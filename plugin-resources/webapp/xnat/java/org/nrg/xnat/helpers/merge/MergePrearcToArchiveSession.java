package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatResourceI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.model.XnatResourceseriesI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xnat.restlet.actions.PrearcImporterA.PrearcSession;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.turbine.utils.XNATUtils;
import org.nrg.xnat.utils.CatalogUtils;
import org.restlet.data.Status;


public  class  MergePrearcToArchiveSession extends  MergeSessionsA<XnatImagesessiondata> {
	public MergePrearcToArchiveSession(Object control,final File srcDIR, final XnatImagesessiondata src, final String srcRootPath, final File destDIR, final XnatImagesessiondata existing, final String destRootPath, boolean addFilesToExisting, boolean overwrite_files,SaveHandlerI<XnatImagesessiondata> saver,final UserI u, final EventMetaI now) {
		super(control, srcDIR, src, srcRootPath, destDIR, existing, destRootPath, addFilesToExisting, overwrite_files,saver,u,now);
		super.setAnonymizer(new PrearcSessionAnonymizer(src, src.getProject(), srcDIR.getAbsolutePath()));
	}

	public String getCacheBKDirName() {
		return "merge";
	}

	public void finalize(XnatImagesessiondata session) throws ClientException,
			ServerException {
		final String root = destRootPath.replace('\\','/') + "/";
		for(XnatImagescandataI scan:session.getScans_scan()){
			for (final XnatAbstractresourceI file : scan.getFile()) {
				((XnatAbstractresource)file).prependPathsWith(root);
				
				if (XNATUtils.isNullOrEmpty(((XnatAbstractresource)file).getContent())) {
					((XnatResource)file).setContent("RAW");
				}
				
				if(file instanceof XnatResourcecatalog){
					CatalogUtils.populateStats((XnatResourcecatalog)file, root);
				}
			}
		}
	}
	
	public void postSave(XnatImagesessiondata session){
		final String root = destRootPath.replace('\\','/') + "/";
		boolean modified=false;
		for(XnatImagescandataI scan:session.getScans_scan()){
			for (final XnatAbstractresourceI file : scan.getFile()) {
				if(file instanceof XnatResourcecatalog){
					XnatResourcecatalog res=(XnatResourcecatalog)file;
					File f=CatalogUtils.getCatalogFile(root, res);
					CatCatalogBean cat=CatalogUtils.getCatalog(root, res);
					if(CatalogUtils.formalizeCatalog(cat, f.getParentFile().getAbsolutePath(), user, c)){
						try {
							CatalogUtils.writeCatalogToFile(cat, f);
						} catch (Exception e) {
							logger.error("",e);
						}
					}
				}
			}
		}
	}

	public org.nrg.xnat.helpers.merge.MergeSessionsA.Results<XnatImagesessiondata> mergeSessions(
			XnatImagesessiondata src, String srcRootPath,
			XnatImagesessiondata dest, String destRootPath,final File rootbackup)
			throws ClientException, ServerException {
		if(dest==null)return new Results<XnatImagesessiondata>(src);

		final Results<XnatImagesessiondata> results=new Results<XnatImagesessiondata>();
		final List<XnatImagescandataI> srcScans=src.getScans_scan();
		final List<XnatImagescandataI> destScans=dest.getScans_scan();
		
		final List<File> toDelete=new ArrayList<File>();
		processing("Merging new meta-data into existing meta-data.");
		try {
			for(final XnatImagescandataI srcScan: srcScans){
				final XnatImagescandataI destScan = MergeUtils.getMatchingScan(srcScan,destScans);
				if(destScan==null){
					dest.addScans_scan(srcScan); 
				}else{
					final List<XnatAbstractresourceI> srcRess=srcScan.getFile();
					final List<XnatAbstractresourceI> destRess=destScan.getFile();
					
					for(final XnatAbstractresourceI srcRes:srcRess){
						final XnatAbstractresourceI destRes=MergeUtils.getMatchingResource(srcRes,destRess);
						if(destRes==null){
							destScan.addFile(srcRes);
						}else{
							if(destRes instanceof XnatResourcecatalogI){
								MergeSessionsA.Results<File> r=mergeCatalogs(srcRootPath,(XnatResourcecatalogI)srcRes,destRootPath,(XnatResourcecatalogI)destRes);
								if(r!=null){
									toDelete.add(r.result);
									results.addAll(r);
								}else{
									CatalogUtils.populateStats((XnatAbstractresource)srcRes, srcRootPath);
								}
							}else if(destRes instanceof XnatResourceseriesI){
								srcRes.setLabel(srcRes.getLabel()+"2");
								srcScan.addFile(destRes);
								
								destScan.addFile(srcRes);
							}else if(destRes instanceof XnatResourceI){
								srcRes.setLabel(srcRes.getLabel()+"2");
								srcScan.addFile(destRes);
								
								destScan.addFile(srcRes);
							}
						}
					}
				}
			}
			
			
		} catch (MergeCatCatalog.DCMEntryConflict e) {
			failed("Duplicate DCM UID cannot be merged at this time.");
			throw new ClientException(Status.CLIENT_ERROR_CONFLICT,e.getMessage(), e);
		} catch (Exception e) {
			failed("Failed to merge upload into existing data.");
			throw new ServerException(e.getMessage(), e);
		}

		final File backup = new File(rootbackup,"catalog_bk");
		backup.mkdirs();
		
		final List<Callable<Boolean>> followup=new ArrayList<Callable<Boolean>>();
		followup.add(new Callable<Boolean>(){
			@Override
			public Boolean call() throws Exception {
				try {
					int count=0;
					for(File f:toDelete){
						File catBkDir=new File(backup,""+count++);
						catBkDir.mkdirs();
						
						FileUtils.MoveFile(f, new File(catBkDir,f.getName()), false);
					}
					return Boolean.TRUE;
				} catch (Exception e) {
					throw new ServerException(e.getMessage(),e);
				}
			}});

		if(src.getXSIType().equals(dest.getXSIType())){
			try {
				src.copyValuesFrom(dest);
			} catch (Exception e) {
				failed("Failed to merge upload into existing data.");
				throw new ServerException(e.getMessage(), e);
			}
			
			results.setResult(src);
		}else{
			results.setResult(dest);
		}
		
		results.getBeforeDirMerge().addAll(followup);
		return results;
	}

}
