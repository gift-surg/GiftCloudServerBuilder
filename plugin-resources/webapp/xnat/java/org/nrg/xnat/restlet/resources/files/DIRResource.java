// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources.files;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.oro.io.GlobFilenameFilter;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.restlet.representations.ZipRepresentation;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.FileRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.restlet.util.Series;

import com.noelios.restlet.http.HttpConstants;

public class DIRResource extends SecureResource {
	XnatProjectdata proj=null;

	XnatExperimentdata expt = null;
	
	public DIRResource(Context context, Request request, Response response) {
		super(context, request, response);

		if(user==null){
			response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			return;
		}
		
		final String pID = (String) request.getAttributes().get("PROJECT_ID");
		if (pID != null) {
			proj = XnatProjectdata.getXnatProjectdatasById(pID, user, false);
		}

		if (proj == null) {
			ArrayList<XnatProjectdata> matches = XnatProjectdata
					.getXnatProjectdatasByField(
							"xnat:projectData/aliases/alias/alias", pID, user,
							false);
			if (matches.size() > 0) {
				proj = matches.get(0);
			}
		}

		final String exptID = (String) request.getAttributes().get("EXPT_ID");
		if (exptID != null) {
			if(exptID!=null){
				expt=XnatExperimentdata.getXnatExperimentdatasById(exptID, user, false);
				
				if(expt==null && proj!=null){
					expt=(XnatSubjectassessordata)XnatExperimentdata.GetExptByProjectIdentifier(proj.getId(), exptID,user, false);
				}
			}
			
		}
		
		if(expt==null) {
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}else{
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
		}
	}

	public final static String[] FILE_HEADERS = {"Name","DIR","Size","URI"};
	
	@Override
	public Representation getRepresentation(Variant variant) {
		MediaType mt = overrideVariant(variant);
		
		if(user==null){
			this.getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
					return null;
		}
		
		if(expt instanceof XnatSubjectassessordata){
			if(filepath==null){
				filepath="";
			}
			
			final File session_dir=expt.getSessionDir();
			if(session_dir==null){
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
				"Session directory doesn't exist in standard location for this experiment.");
				return null;
			}
		
			try {
				final List<File> src;
				if(filepath.equals("")){
					src=new ArrayList<File>();
					src.add(session_dir);
				}else{
					src=getFiles(session_dir,filepath,true);
				}

				if(src.size()==0){
					this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Specified request didn't match any stored files.");
					return null;
				}else if (src.size()==1 && !src.get(0).isDirectory()){
					final File f=src.get(0);
					
					if((mt.equals(MediaType.APPLICATION_ZIP)) || (mt.equals(MediaType.APPLICATION_GNU_TAR) )){
						ZipRepresentation rep=new ZipRepresentation(mt,(expt).getArchiveDirectoryName());
						rep.addEntry(f);
						
						this.setContentDisposition(String.format("attachment; filename=\"%s.zip\";",f.getName()));
						
						return rep;
					}else{
						return this.representFile(f, mt);
					}
				}else{
					final List<FileSet> dest= new ArrayList<FileSet>();
					
					for(File f: src){
						final FileSet set=new FileSet(f);
						if(f.isDirectory()){
							if(this.isQueryVariableTrue("recursive")){
								set.addAll(FileUtils.listFiles(f, null, true));
							}else{
								File[] children=f.listFiles();
								if(children!=null){
									set.addAll(Arrays.asList(children));
								}
							}
						}else{
							set.add(f);
						}
						dest.add(set);
					}
					
					
					if((mt.equals(MediaType.APPLICATION_ZIP)) || (mt.equals(MediaType.APPLICATION_GNU_TAR) )){
						
						final ZipRepresentation rep=new ZipRepresentation(mt,(expt).getArchiveDirectoryName());
						
						for(FileSet fs:dest){
							rep.addAll(fs.getMatches());
						}
						
						this.setContentDisposition(String.format("attachment; filename=\"%s\";",rep.getDownloadName()));
						
						return rep;
					}else{

						final Hashtable<String,Object> params=new Hashtable<String,Object>();
						params.put("title", "Files");
						
						final XFTTable table = new XFTTable();
						table.initTable(FILE_HEADERS);
						
						String qsParams="";
						if(this.getQueryVariable("format")!=null){
							if(qsParams.equals(""))qsParams+="?";else qsParams+="&";
							qsParams+=String.format("format=%s",this.getQueryVariable("format"));
						}
						
						for(final FileSet fs:dest){
							final File parent=fs.getParent();
							for(final File f:fs.getMatches()){
								final Object[] row = new Object[8];
								row[0]=((parent.toURI().relativize(f.toURI())).getPath());
								row[1]=f.isDirectory();
					            row[2]=(f.length());
					           
					            final String rel=(session_dir.toURI().relativize(f.toURI())).getPath();
					            final String qs=(f.isDirectory())?qsParams:"";
					            row[3]=String.format("/REST/experiments/%1s/DIR/%2s%3s", new Object[]{expt.getId(),rel,qs});
					       				            
					            table.rows().add(row);
							}
						}
						
						
						final Map<String,Map<String,String>> cp = new Hashtable<String,Map<String,String>>();
						cp.put("URI", new Hashtable<String,String>());
						
						String rootPath = this.getRequest().getRootRef().getPath();
						if(rootPath.endsWith("/REST")){
							rootPath=rootPath.substring(0,rootPath.indexOf("/REST"));
						}
						cp.get("URI").put("serverRoot", rootPath);
						
						return this.representTable(table, mt, params,cp);
					}
				}
				
				
			} catch (InvalidFileCharacters e) {
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
				String.format("'%s' is not allowed in this resource URI.",e.characters));
				return null;
			}
		}else{
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
			"Resource only available for extensions of the xnat:subjectAssessorData type.");
			return null;
		}
	}
	
	public static List<File> getFiles(File dir,String path,boolean recursive) throws InvalidFileCharacters{
		final List<File> files=new ArrayList<File>();
		final int slash=path.indexOf("/");
		if(slash>-1){
			final String local=path.substring(0,slash);
			
			if(path.length()>(slash+1)){
				path=path.substring(slash+1);
			}else{
				recursive=false;
			}
			
			if(path.trim().equals("..")){
				throw new InvalidFileCharacters("..");
			}
			
			final GlobFilenameFilter glob = new GlobFilenameFilter(local);
			final String[] children=dir.list(glob);
			for(final String child:children){
				final File f=new File(dir,child);
				if(recursive && f.isDirectory()){
					files.addAll(getFiles(f,path,true));
				}else{
					files.add(f);
				}
			}
			
		}else{
			if(path.trim().equals("..")){
				throw new InvalidFileCharacters("..");
			}
			final GlobFilenameFilter glob = new GlobFilenameFilter((path.equals(""))?"*":path);
			final String[] children=dir.list(glob);
			for(final String child:children){
				files.add(new File(dir,child));
			}
		}
		
		return files;
	}
	
	public static class InvalidFileCharacters extends Exception{
		public String characters=null;
		public InvalidFileCharacters(String chars){
			characters=chars;
		}
	}
	
	public static class FileSet{
		final File parent;
		final List<File> matches=new ArrayList<File>();
		public FileSet(File p){
			parent=p;
		}
		public List<File> getMatches() {
			return matches;
		}
		
		public void add(File f){
			matches.add(f);
		}
		
		public void addAll(Collection files){
			matches.addAll(files);
		}
		
		public File getParent(){
			return parent;
		}
	}
}
