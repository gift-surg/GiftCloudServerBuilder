package org.nrg.xnat.archive;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.model.XnatReconstructedimagedataI;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourceseries;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.DBItemCache;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.turbine.utils.ArchivableItem;
import org.nrg.xnat.utils.WorkflowUtils;

public class Rename  implements Callable<File>{
	enum STEP{PREPARING,PREPARE_SQL,COPY_DIR,EXECUTE_SQL,DELETE_OLD_DIR,COMPLETE};
	static org.apache.log4j.Logger logger = Logger.getLogger(Rename.class);
	private static final String SUCCESSFUL_RENAMES = "successful_renames";
	private static final String FAILED_RENAME = "failed_rename";
	private final ArchivableItem i;
	private final XnatProjectdata proj;
	
	private final String newLabel;
	private final XDATUser user;
	
	private STEP step=STEP.PREPARING;
	/**
	 * Only for use in the JUNIT tests
	 */
	public Rename(){
		proj=null;
		i=null;
		newLabel=null;
		user=null;
	}
	
	public Rename(final XnatProjectdata p,final ArchivableItem e, final String lbl, final XDATUser u){
		proj=p;
		i=e;
		newLabel=lbl;
		user=u;
		
		if(i==null){
			throw new NullPointerException();
		}
		
		if(newLabel==null){
			throw new NullPointerException();
		}
		
		if(StringUtils.isEmpty(newLabel)){
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Rename the label for the corresponding session and modify the file URIs for the adjusted path.
	 * 
	 * @throws FieldNotFoundException
	 * @throws ProcessingInProgress
	 * @throws DuplicateLabelException
	 * @throws IllegalAccessException
	 * @throws LabelConflictException
	 * @throws FolderConflictException
	 * @throws InvalidArchiveStructure
	 * @throws URISyntaxException
	 * @throws Exception
	 */
	public File call() throws FieldNotFoundException, ProcessingInProgress, DuplicateLabelException, IllegalAccessException, LabelConflictException, FolderConflictException, InvalidArchiveStructure, URISyntaxException,Exception{
		final File newSessionDir = new File(new File(proj.getRootArchivePath(),proj.getCurrentArc()),newLabel);
		
		try {
			final String id=i.getStringProperty("ID");
			final String lbl=i.getStringProperty("label");
			final String current_label=(StringUtils.isEmpty(lbl))?id:lbl;	
						
			if(newLabel.equals(current_label)){
				throw new DuplicateLabelException();
			}
			
			//confirm if user has permission
			if(!checkPermissions(i, user)){
				throw new IllegalAccessException("Invalid Edit permissions for project: " + proj.getId());
			}

			//confirm if new label is already in use
			if(i instanceof XnatSubjectdata){
				final XnatSubjectdata match=XnatSubjectdata.GetSubjectByProjectIdentifier(proj.getId(), newLabel,null, false);
				if(match!=null){
					throw new LabelConflictException();
				}
			}else{
				final XnatExperimentdata match=XnatExperimentdata.GetExptByProjectIdentifier(proj.getId(), newLabel,null, false);
				if(match!=null){
					throw new LabelConflictException();
				}
			}
			
			//confirm if new directory already exists
			if(newSessionDir.exists()){		
				throw new FolderConflictException();
			}else{
				newSessionDir.mkdir();
			}
			
			//identify existing directory
			final File oldSessionDir = i.getExpectedCurrentDirectory();
				

			final Collection<WrkWorkflowdata> open=WorkflowUtils.getOpenWorkflows(user, id);
			if(!open.isEmpty()){		
				throw new ProcessingInProgress(((WrkWorkflowdata)CollectionUtils.get(open, 0)).getPipelineName());
			}
			
			//add workflow entry    		
			final WrkWorkflowdata workflow = WorkflowUtils.buildOpenWorkflow(user, i.getXSIType(), i.getStringProperty("ID"), proj.getId());
			workflow.setPipelineName(String.format("Renamed from %s to %s", current_label,newLabel));
			workflow.setStepDescription(getStep().toString());
			workflow.save(user, false, false);
			
			
			final URI expected=oldSessionDir.toURI();
			final String newArchive=newSessionDir.getAbsolutePath();
			
			final boolean moveFiles=oldSessionDir.exists();
			
			this.updateStep(workflow, setStep(STEP.PREPARE_SQL));	
			
			try {
				//Copy files to new location    		
				
				//Generate SQL to update URIs
				final DBItemCache cache=new DBItemCache();
				generateLabelSQL(i, newLabel, cache, user);
				generateURISQL(i, expected, newArchive, cache, user);

				this.updateStep(workflow, setStep(STEP.COPY_DIR));
				
				if(moveFiles)copy(oldSessionDir, newSessionDir);	

				this.updateStep(workflow, setStep(STEP.EXECUTE_SQL));
				//Execute SQL
				executeSQL(cache,user,XFT.buildLogFileName(i));

				this.updateStep(workflow, setStep(STEP.DELETE_OLD_DIR));
				//if successful, move old directory to cache)
				if(moveFiles)moveToCache(proj,SUCCESSFUL_RENAMES,oldSessionDir);
				
				//close workflow entry
				workflow.setStepDescription(setStep(STEP.COMPLETE).toString());
				workflow.setStatus(WorkflowUtils.COMPLETE);
			} catch (final Exception e) {
				if(!getStep().equals(STEP.DELETE_OLD_DIR)){
					try {
						if(moveFiles)moveToCache(proj,FAILED_RENAME,newSessionDir);
					} catch (IOException e1) {
						logger.error("", e1);
					}
					
					//close workflow
					workflow.setStatus(WorkflowUtils.FAILED);
					
					throw e;
				}else{
					workflow.setStatus(WorkflowUtils.COMPLETE);
				}
			}finally{
				try {
					workflow.save(user, false, false);
				} catch (Exception e1) {
					logger.error("", e1);
				}				
			}
		} catch (XFTInitException e) {
			logger.error("", e);
		} catch (ElementNotFoundException e) {
			logger.error("", e);
		}
		
		return newSessionDir;
	}
	
	public void updateStep(final WrkWorkflowdata wrk, final STEP step){
		wrk.setStepDescription(step.toString());
		try {
			wrk.save(user, false, false);
		} catch (Exception e1) {
			logger.error("", e1);
		}
	}
	
	public static boolean checkPermissions(final ArchivableItem i, final XDATUser user) throws Exception{
		return user.canEdit(i);
	}
	
	public static void moveToCache(final XnatProjectdata proj, final String subdir, final File src) throws IOException{
		//should include a timestamp in folder name
		if(src.exists()){
			final File cache=(StringUtils.isEmpty(subdir))?new File(proj.getCachePath()):new File(proj.getCachePath(),subdir);
			
			final File dest= new File(cache,org.nrg.xft.utils.FileUtils.renameWTimestamp(src.getName()));
						
			move(src,dest);
		}
	}
	
	public static void move(final File src, final File dest) throws IOException{
		//Started out using Ant Move, but that is a big pain in the you know what with ant 1.5.  This should not be more then one line of code.
		FileUtils.moveDirectory(src, dest);
	}
	
	public static void copy(final File src, final File dest) throws IOException{
		FileUtils.copyDirectory(src,dest);
	}
	
	/**
	 * Generate the SQL update logic for all of the items resources.  
	 * Checks permissions for assessments, if they were modified.
	 * 
	 * @param current_label
	 * @return
	 * @throws UnsupportedResourceType 
	 * @throws Exception 
	 * @throws SQLException 
	 * @throws FieldNotFoundException 
	 * @throws XFTInitException 
	 * @throws ElementNotFoundException 
	 */
	public static void generateURISQL(final ItemI i, final URI expected, final String newArchive, final DBItemCache cache,final XDATUser user) throws UnsupportedResourceType, SQLException, Exception{
		final SecurityManager sm= SecurityManager.GetInstance();
		//set label and modify URI
		if(i instanceof XnatSubjectdata){
			for(final XnatAbstractresourceI res: ((XnatSubjectdata)i).getResources_resource()){
				modifyResource((XnatAbstractresource)res,expected,newArchive,user,sm,cache);
			}
		}else{
			for(final XnatAbstractresourceI res: ((XnatExperimentdata)i).getResources_resource()){
				modifyResource((XnatAbstractresource)res,expected,newArchive,user,sm,cache);
			}
			
			if(i instanceof XnatImagesessiondata){
				for(final XnatImagescandataI scan: ((XnatImagesessiondataI)i).getScans_scan()){
					for(final XnatAbstractresourceI res: scan.getFile()){
						modifyResource((XnatAbstractresource)res,expected,newArchive,user,sm,cache);
					}
				}

				for(final XnatReconstructedimagedataI recon: ((XnatImagesessiondataI)i).getReconstructions_reconstructedimage()){
					for(final XnatAbstractresourceI res: recon.getIn_file()){
						modifyResource((XnatAbstractresource)res,expected,newArchive,user,sm,cache);
					}
					
					for(final XnatAbstractresourceI res: recon.getOut_file()){
						modifyResource((XnatAbstractresource)res,expected,newArchive,user,sm,cache);
					}
				}

				for(final XnatImageassessordataI assess: ((XnatImagesessiondataI)i).getAssessors_assessor()){
					boolean checkdPermissions =false;
					for(final XnatAbstractresourceI res: assess.getResources_resource()){
						if(modifyResource((XnatAbstractresource)res,expected,newArchive,user,sm,cache)){
							if(!checkdPermissions){
								if(checkPermissions((XnatImageassessordata)assess, user)){
									checkdPermissions=true;
								}else{
									throw new IllegalAccessException("Invalid Edit permissions for assessor in project: " + assess.getProject());
								}
							}
						}
					}
					
					for(final XnatAbstractresourceI res: assess.getIn_file()){
						if(modifyResource((XnatAbstractresource)res,expected,newArchive,user,sm,cache)){
							if(!checkdPermissions){
								if(checkPermissions((XnatImageassessordata)assess, user)){
									checkdPermissions=true;
								}else{
									throw new IllegalAccessException("Invalid Edit permissions for assessor in project: " + assess.getProject());
								}
							}
						}
					}
					
					for(final XnatAbstractresourceI res: assess.getOut_file()){
						if(modifyResource((XnatAbstractresource)res,expected,newArchive,user,sm,cache)){
							if(!checkdPermissions){
								if(checkPermissions((XnatImageassessordata)assess, user)){
									checkdPermissions=true;
								}else{
									throw new IllegalAccessException("Invalid Edit permissions for assessor in project: " + assess.getProject());
								}
							}
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Modifies the resource to point to the new path, if the old path is in the expected place.
	 * @param res
	 * @param expected
	 * @return
	 * @throws UnsupportedResourceType 
	 * @throws Exception 
	 * @throws SQLException 
	 * @throws FieldNotFoundException 
	 * @throws XFTInitException 
	 * @throws ElementNotFoundException 
	 */
	protected static boolean modifyResource(final XnatAbstractresource res, final URI expected, final String newArchive,final XDATUser user, final SecurityManager sm, final DBItemCache cache) throws UnsupportedResourceType, ElementNotFoundException, XFTInitException, FieldNotFoundException, SQLException, Exception{
		final String path=getPath(res);
		final URI current= new File(path).toURI();
		
		final URI relative=expected.relativize(current);
		
		if(relative.equals(current)){
			//not within expected path
			final File oldSessionDir=new File(expected);
			if(path.replace('\\', '/').contains("/"+oldSessionDir.getName()+"/")){
				//session contains resource which is not in the standard format, but is in a directory with the old label.
				throw new UnsupportedResourceType();
			}else{
			return false;
			}
		}else{
			//properly in place
			setPath(res,(new File(newArchive,relative.getPath())).getAbsolutePath());
			DBAction.StoreItem(res.getItem(),user,false,false,false,false,sm,cache);
			
			return true;
		}
	}
	
	/**
	 * Generate update logic for modifying the label of the given item.
	 * @param i
	 * @param newLabel
	 * @param cache
	 * @param user
	 * @throws SQLException
	 * @throws Exception
	 */
	protected static void generateLabelSQL(final ArchivableItem i, final String newLabel, final DBItemCache cache,final XDATUser user) throws SQLException, Exception{
		i.getItem().setProperty("label", newLabel);

		DBAction.StoreItem(i.getItem(),user,false,false,false,false,SecurityManager.GetInstance());
	}
	
	/**
	 * Executes the given cached logic against the database.  
	 * @param cache
	 * @return
	 * @throws Exception 
	 */
	protected static void executeSQL(final DBItemCache cache, final XDATUser user, final String logFileName) throws Exception{
		DBAction.executeCache(cache, user, user.getDBName(), logFileName);
	}
	
	/**
	 * Gets the path or URI of the given resource
	 * @param res
	 * @return
	 * @throws UnsupportedResourceType
	 */
	protected static String getPath(final XnatAbstractresource res) throws UnsupportedResourceType{
		if(res instanceof XnatResource){
			return ((XnatResource)res).getUri();
		}else if(res instanceof XnatResourceseries){
			return ((XnatResourceseries)res).getPath();
		}else{
			throw new UnsupportedResourceType();
		}
	}
	
	/**
	 * Sets the path or URI of the given resource to the newPath.  
	 * @param res
	 * @param newPath
	 * @throws UnsupportedResourceType
	 */
	protected static void setPath(final XnatAbstractresource res, final String newPath) throws UnsupportedResourceType{
		if(res instanceof XnatResource){
			((XnatResource)res).setUri(newPath);
		}else if(res instanceof XnatResourceseries){
			((XnatResourceseries)res).setPath(newPath);
		}else{
			throw new UnsupportedResourceType();
		}
	}
	
	public STEP getStep() {
		return step;
	}

	public STEP setStep(STEP step) {
		return (this.step = step);
	}

	//EXCEPTION DECLARATIONS
	public class LabelConflictException extends Exception{
		public LabelConflictException(){
			super();
		}
	}
	
	public class DuplicateLabelException extends Exception{
		public DuplicateLabelException(){
			super();
		}
	}
	
	public class FolderConflictException extends Exception{
		public FolderConflictException(){
			super();
		}
	}
	
	public static class UnsupportedResourceType extends Exception{
		public UnsupportedResourceType(){
			super();
		}
	}
	
	public static class ProcessingInProgress extends Exception{
		private final String pipeline_name;
		public ProcessingInProgress(final String s){
			super();
			pipeline_name=s;
		}
		public String getPipeline_name() {
			return pipeline_name;
		}
	}
}
