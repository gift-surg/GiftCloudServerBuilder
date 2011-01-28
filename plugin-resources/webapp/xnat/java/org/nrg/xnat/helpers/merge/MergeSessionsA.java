package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.status.StatusProducer;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.helpers.merge.MergeCatCatalog.DCMEntryConflict;
import org.nrg.xnat.utils.CatalogUtils;
import org.restlet.data.Status;

public abstract class MergeSessionsA<A extends XnatImagesessiondataI> extends StatusProducer implements Callable<A> {
	public static final String CAT_ENTRY_MATCH = "Session already exists with the same resources.  Retry with overwrite=delete to force an overwrite of the pre-existing data.";
	public static final String HAS_FILES = "Session already exists with matching files, retry with overwrite=delete enabled";
	protected final File srcDIR,destDIR;
	protected final A src,dest;
	protected final String destRootPath,srcRootPath;	
	protected final boolean overwrite,allowDataDeletion;
	protected final SaveHandlerI<A> saver;
	protected final Object control;

	static org.apache.log4j.Logger logger = Logger.getLogger(MergeSessionsA.class);
	
	public MergeSessionsA(Object control,final File srcDIR, final A src, final String srcRootPath, final File destDIR, final A existing, final String destRootPath, boolean overwrite, boolean allowDataDeletion,SaveHandlerI<A> saver) {
		super(control);
		this.control=control;
		this.srcDIR=srcDIR;
		this.overwrite=overwrite;
		this.allowDataDeletion=allowDataDeletion;
		this.dest=existing;
		this.src=src;
		this.destDIR=destDIR;
		this.srcRootPath=srcRootPath;
		this.destRootPath=destRootPath;
		this.saver=saver;
	}
	
	public interface SaveHandlerI<A>{
		public void save(A session) throws Exception;
	}
	
	public void checkForConflict() throws ClientException,ServerException{
		if(destDIR.exists() || dest!=null){
			if(!overwrite){
				failed(HAS_FILES);
				throw new ClientException(Status.CLIENT_ERROR_CONFLICT,HAS_FILES, new Exception());
			}
			
			if(!allowDataDeletion){
				if((new SessionOverwriteCheck(src, dest,src.getPrearchivepath(),dest.getPrearchivepath())).call()){
					failed(CAT_ENTRY_MATCH);
					throw new ClientException(Status.CLIENT_ERROR_CONFLICT,CAT_ENTRY_MATCH,new IOException());
				}
			}
		}
		
		if(destDIR.exists() && !allowDataDeletion){
			try {
				if(FileUtils.FindFirstMatch(srcDIR, destDIR, new FileFilter(){
					public boolean accept(File pathname) {
						return !pathname.getName().endsWith(".xml");
					}})!= null){
					failed(HAS_FILES);
					throw new ClientException(Status.CLIENT_ERROR_CONFLICT,HAS_FILES, new Exception());
				}
			} catch (IOException e) {
				failed("Error accessing file system.");
				throw new ServerException(Status.SERVER_ERROR_INTERNAL,e.getMessage(), new Exception());
			}
		}
	}

	public A call() throws ClientException, ServerException {
		processing("Preparing to move uploaded resources into destination directory.");
		this.checkForConflict();
		
		File rootBackup=createPrimaryBackupDirectory(this.getCacheBKDirName(),src.getProject(),destDIR.getName());
		
		File backupDIR=null;
		if(destDIR.exists()){
			backupDIR=backupDestDIR(destDIR,rootBackup);
		}
		
		if(dest!=null){
			backupXML(dest,rootBackup);
		}

		
		final UpdatedSession<A> update=mergeSessions(src,srcRootPath,dest,destRootPath);
		
		A merged= update.getSession();
		//If we wrote to the src directory's catalogs, would the overwrite persist them into the new space (overwriting the old ones).
		//What if the same catalog had two different catalog file names.  This would cause duplicate catalogs.
		//Could merge the catalogs based on label, delete the old one, write the new one to the src space, then copy it all in.
		//DONE
		
		finalize(merged);

		deleteCatalogFiles(update.getToDelete(),rootBackup);
		
		mergeDirectories(srcDIR,destDIR,allowDataDeletion,false);
		
		try {
			this.processing("Updating stored meta-data.");
			saver.save(merged);
		} catch (Throwable e) {
			if(backupDIR!=null){
				rollback(backupDIR,destDIR,rootBackup);
			}else{
				rollback(destDIR,srcDIR,rootBackup);
			}
			failed("Error updating existing meta-data");
			throw new ServerException(Status.SERVER_ERROR_INTERNAL,e.getMessage(), new Exception());
		}
		
		return merged;
	}
	
	private void backupXML(A dest2, File rootBackup) throws ServerException{
		File backup = new File(rootBackup,"dest.xml");
		try {
			processing("Backing up destination XML Specification document.");
			FileWriter fw = new FileWriter(backup);
			dest2.toXML(fw);
			fw.close();
		} catch (Exception e) {
			failed("Failed to update XML Specification document.");
			throw new ServerException(e.getMessage(),e);
		}
	}

	private File backupDestDIR(File destDIR2, File rootBackup) throws ServerException{
		File backup = new File(rootBackup,"dest_backup");
		backup.mkdirs();
		
		this.processing("Backing up destination directory");
		try {
			FileUtils.CopyDir(destDIR2, backup, false);
		} catch (Exception e) {
			this.failed("Failed to backup destination directory");
			throw new ServerException(e.getMessage(),e);
		}
		
		return backup;
	}

	private File createPrimaryBackupDirectory(String cacheBKDirName,
			String project,String folderName) {
		File f= org.nrg.xnat.utils.FileUtils.buildCachepath(project, cacheBKDirName, folderName);
		f.mkdirs();
		return f;
	}

	private void deleteCatalogFiles(List<File> toDelete, File rootBackup)  throws ServerException{
		File backup = new File(rootBackup,"catalog_bk");
		backup.mkdirs();

		this.processing("Removing pre-existing catalog documents.");
		try {
			int count=0;
			for(File f:toDelete){
				File catBkDir=new File(backup,""+count++);
				catBkDir.mkdirs();
				
				FileUtils.MoveFile(f, new File(catBkDir,f.getName()), false);
			}
		} catch (Exception e) {
			this.failed("Failed to remove pre-existing catalog documents.");
			throw new ServerException(e.getMessage(),e);
		}
	}

	private void rollback(File backupDIR, File destDIR2, File rootBackup)  throws ServerException{
		File backup = new File(rootBackup,"modified_dest");
		backup.mkdirs();

		this.processing("Restoring previous version of destination directory.");
		try {
			FileUtils.MoveDir(destDIR2, backup, true);
		} catch (IOException e) {
			logger.error("",e);
		}
		
		try {
			FileUtils.MoveDir(backupDIR, destDIR2, overwrite);
		} catch (Exception e) {
			this.failed("Failed to restore previous version of destination directory.");
			throw new ServerException(e.getMessage(),e);
		}
	}

	/**
	 * Fix scans
	 * @param session
	 * @throws Exception
	 */
	public abstract void finalize(A session)  throws ClientException, ServerException;
	
	public abstract String getCacheBKDirName();
	
	public abstract UpdatedSession<A> mergeSessions(final A src, final String srcRootPath, final A dest, final String destRootPath) throws ClientException, ServerException;
	
	
	public File mergeCatalogs(final String srcRootPath, final XnatResourcecatalogI srcRes, final String destRootPath, final XnatResourcecatalogI destRes) throws DCMEntryConflict, ServerException, Exception{
		final CatCatalogBean srcCat=CatalogUtils.getCleanCatalog(srcRootPath, (XnatResourcecatalogI)srcRes, false);
		
		//WARNING: this command will create a catalog if it doesn't already exist
		final CatCatalogBean destCat=CatalogUtils.getCleanCatalog(destRootPath, (XnatResourcecatalogI)destRes, false);
		
		MergeCatCatalog merge= new MergeCatCatalog(srcCat, destCat, overwrite);
		if(merge.call()){
			try {
				//write merged destination file to src directory for merge process to move
				CatalogUtils.writeCatalogToFile(destCat, CatalogUtils.getCatalogFile(srcRootPath, (XnatResourcecatalogI)srcRes));
				
				return CatalogUtils.getCatalogFile(destRootPath, destRes);
			} catch (Exception e) {
				failed("Failed to update XML Specification document.");
				throw new ServerException(e.getMessage(),e);
			}
		}
		
		return null;
	}
	
	public class UpdatedSession<A>{
		private final A session;
		private final List<File> toDelete;
		
		public UpdatedSession(A session, List<File> toDelete){
			this.session=session;
			this.toDelete=toDelete;
		}

		public A getSession() {
			return session;
		}

		public List<File> getToDelete() {
			return toDelete;
		}		
	}

	public void mergeDirectories(File srcDIR2, File destDIR2, boolean overwrite, boolean includeXML) throws ClientException, ServerException {
		try {
			org.nrg.xft.utils.FileUtils.MoveDir(srcDIR2,destDIR2,overwrite,(includeXML)?null:new FileFilter(){
				@Override
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".xml");
				}});			
		} catch (IOException e) {
			failed("Unable to merge uploaded data into destination directory.");
			throw new ServerException(e.getMessage(),e);
		}
	}

}