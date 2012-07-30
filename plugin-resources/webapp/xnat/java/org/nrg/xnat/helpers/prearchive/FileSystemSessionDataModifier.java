package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomObject;
import org.nrg.config.entities.Configuration;
import org.nrg.dcm.Anonymize;
import org.nrg.dcm.xnat.DICOMSessionBuilder;
import org.nrg.dcm.xnat.XnatAttrDef;
import org.nrg.session.SessionBuilder;
import org.nrg.xdat.bean.XnatImagesessiondataBean;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.helpers.merge.AnonUtils;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase.SyncFailedException;
import org.nrg.xnat.helpers.prearchive.PrearcUtils.PrearcStatus;
import org.xml.sax.SAXException;

import com.google.common.base.Function;
/**
 * Modify the session on the filesystem 
 * @author aditya
 *
 */
public class FileSystemSessionDataModifier implements SessionDataModifierI {
	static Logger logger = Logger.getLogger(FileSystemSessionDataModifier.class);
	private final String basePath;
	public FileSystemSessionDataModifier(String basePath) {
		this.basePath = basePath;
	}
	
	/**
	 * Represents each step of the operation can either be run
	 * or rolled back in case of exception. 
	 * 
	 * There is no recovery from a rollback.
	 * @author aditya
	 *
	 * @param <T> the type of value returned from a running the op.
	 */
	interface Transaction<T> {
		T run() throws SyncFailedException;
		void rollback() throws IllegalStateException;
	}
			
	static class Move {
		final String basePath, sess, uri, subject, newProj;
		final File f, tsdir, newTsdir, xml;
		XnatImagesessiondataBean doc = null;
		
		/**
		 * Each step of the move is a Transaction that
		 * can be rolled back. It is expected that the steps
		 * are executed in the order in which they appear below
		 * because each step depends on the success of the previous one.
		 * So an error in 'setXml' step for example invokes a rollback on the
		 * 'copy' step.  
		 */
		Transaction<java.lang.Void> copy;
		Transaction<XnatImagesessiondataBean> setXml;
		Transaction<java.lang.Void>writeXml;
		
		class Copy implements Transaction<java.lang.Void>{
			File tsdir;
			File newTsdir;
			String sess;
			public Copy(File tsdir, File newTsdir, String sess) {
				super();
				this.tsdir = tsdir;
				this.newTsdir = newTsdir;
				this.sess = sess;
			}
			public java.lang.Void run () throws SyncFailedException {
				try {
					org.apache.commons.io.FileUtils.copyDirectoryToDirectory(new File(tsdir, sess), newTsdir);
				}
				catch (IOException e) {
					throwSync(e.getMessage());
				}
				return null;
			}
			public void rollback() {
				try {
					org.apache.commons.io.FileUtils.deleteDirectory(new File(newTsdir, sess));
				} catch (IOException e) {
					throw new IllegalStateException(e.getMessage());
				}
			}
		}
		
		class SetXml implements Transaction<XnatImagesessiondataBean>{
			final File xml;
			final String newProj,newDirPath;
			public SetXml(File xml, String newProj, String newDirPath) {
				this.xml = xml;
				this.newProj = newProj;
				this.newDirPath = newDirPath;
			}
			public XnatImagesessiondataBean run() throws SyncFailedException {
				XnatImagesessiondataBean doc = null;
				try {
					XnatProjectdata xpd = XnatProjectdata.getXnatProjectdatasById(newProj, null, false);
					Long projectId = DicomEdit.getDBId(xpd);
					Configuration c = AnonUtils.getService().getScript(DicomEdit.buildScriptPath(DicomEdit.ResourceScope.PROJECT, newProj), projectId);
					if (c != null) {
						final String anonScript = c.getContents();
						final XnatAttrDef[] params = {new XnatAttrDef.Constant("project", newProj)};
						final DICOMSessionBuilder db = new DICOMSessionBuilder(tsdir, params,
						        new Function<DicomObject,DicomObject>() {
						    public DicomObject apply(final DicomObject o) {
						        try {
						            Anonymize.anonymize(o, newProj, subject, sess, anonScript);
						        } catch (RuntimeException e) {
						            throw e;
						        } catch (Exception e) {
						            throw new RuntimeException(e);
						        }
						        return o;
						    }
						});
						XnatImagesessiondataBean i = db.call();
						doc = i;
					}
					else {
						doc=PrearcTableBuilder.parseSession(xml);
						doc.setProject(newProj);
					}
					//modified to also set the new prearchive path.
					doc.setPrearchivepath(newDirPath);	
				} catch (SAXException e) {
					throwSync(e);
				} catch (IOException e) {
					throwSync(e);
				} catch (SQLException e) {
					throwSync(e);
				} catch (SessionBuilder.NoUniqueSessionException e) {
					throwSync(e);
				}
				
				return doc;
			}
			public void rollback() throws IllegalStateException {
				// this operation is in memory there is no need to rollback
			}
		}
		
		class WriteXml implements Transaction<java.lang.Void>{
			File tsdir;
			String sess;
			File xml;
			public WriteXml(File tsdir, String sess) {
				super();
				this.tsdir = tsdir;
				this.sess = sess;
				xml = new File(tsdir, sess + ".xml");
			}
			public Void run() throws SyncFailedException {
				FileWriter fw=null;
			    try {
			    	fw=new FileWriter(xml);
			    	doc.toXML(fw);
				} catch (Exception e) {
					throwSync(e.getMessage());
				}finally{
					if(fw!=null)
						try {
							fw.close();
						} catch (IOException e) {}
				}
			    return null;
			}
			public void rollback() throws IllegalStateException {
				xml.delete();
			}
		}
				
		public Move(String basePath, final String sess, String uri, String subject, final String newProj) {
			super();
			this.basePath = basePath;
			this.sess = sess;
			this.uri = uri;
			this.subject = subject;
			this.newProj = newProj;
			this.f = new File(this.uri);
			this.tsdir = f.getParentFile();
			this.newTsdir = new File(this.basePath + this.newProj, this.tsdir.getName());
			this.xml =  new File(tsdir, sess + ".xml");
			copy = new Copy(tsdir, newTsdir, sess);
			setXml = new SetXml(xml,newProj,(new File(newTsdir,sess)).getAbsolutePath());
			writeXml = new WriteXml(newTsdir,sess);
		}

		void throwSync (String msg) throws SyncFailedException {
			throw new SyncFailedException(msg);
		}
		
		void throwSync (Throwable cause) throws SyncFailedException {
			throw new SyncFailedException(cause);
		}
		
		public Transaction<java.lang.Void> getCopy() {
			return copy;
		}

		public Transaction<java.lang.Void> getWriteXml() {
			return writeXml;
		}

		public Transaction<XnatImagesessiondataBean> getSetXml() {
			return setXml;
		}

		public void setCopy(Transaction<java.lang.Void> copy) {
			this.copy = copy;
		}

		public void setWriteXml(Transaction<java.lang.Void> writeXml) {
			this.writeXml = writeXml;
		}

		public void setSetXml(Transaction<XnatImagesessiondataBean> setXml) {
			this.setXml = setXml;
		}

		void run() throws SyncFailedException {
			try {
				this.copy.run();
			}
			catch (SyncFailedException e) {
				this.copy.rollback();
				throw e;
			}
			
			try {
				this.doc = this.setXml.run();
			} catch (SyncFailedException e) {
				this.setXml.rollback();
				this.copy.rollback();
				throw e;
			}
			
			try {
				this.writeXml.run();
			} catch (SyncFailedException e) {
				this.writeXml.rollback();
				this.setXml.rollback();
				this.copy.rollback();
				throw e;
			}
			// If everything was moved, we can remove the session and timestamp directories.
			FileUtils.deleteDirQuietly(f);
			if (f.exists()) {
				logger.warn("moved session " + sess + " to " + newProj + ", but unable to delete original directory.");
			}
			// timestamp directory might contain another session, so no warning if deletion fails.
			if(tsdir.list().length==0){
				tsdir.delete();	
			}
		}
	}
	
	public void move(final SessionData sd, final String newProj) throws SyncFailedException {
		this._move(new Move (this.basePath, sd.getFolderName(), sd.getUrl(), sd.getSubject(), newProj){});
	}
	
	protected void _move(Move move) throws SyncFailedException {
		move.run();
	}
	
	public void delete(SessionData sd) {
		File f = new File(sd.getUrl());
		try {
			FileUtils.MoveToCache(new File(f.getPath() + ".xml"));
		} catch (Exception e) {
			logger.error("",e);
		}
		final File tsdir = f.getParentFile();
		try {
			FileUtils.MoveToCache(f);
		} catch (Exception e) {
			logger.error("",e);
		}
		
		if(!FileUtils.HasFiles(tsdir)){
			FileUtils.deleteDirQuietly(tsdir);	// delete timestamp parent only if empty.
		}
	}
	
	public void setStatus(SessionData sd, PrearcStatus status) {
		// TODO Auto-generated method stub
	}
}
