package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.SyncFailedException;

import org.apache.log4j.Logger;
import org.nrg.xdat.bean.XnatImagesessiondataBean;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.helpers.prearchive.PrearcUtils.PrearcStatus;
import org.xml.sax.SAXException;
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
		final String basePath, sess, uri, newProj;
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
			File xml;
			String newProj;
			public SetXml(File xml, String newProj) {
				this.xml = xml;
				this.newProj = newProj;
			}
			public XnatImagesessiondataBean run() throws SyncFailedException {
				XnatImagesessiondataBean doc = null;
				try {
					doc=PrearcTableBuilder.parseSession(xml);
					doc.setProject(newProj);
				} catch (SAXException e) {
					throwSync(e.getMessage());
				} catch (IOException e) {
					throwSync(e.getMessage());
				}
				return doc;
			}
			public void rollback() throws IllegalStateException {
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
				
		public Move(String basePath, final String sess, String uri, final String newProj) {
			super();
			this.basePath = basePath;
			this.sess = sess;
			this.uri = uri;
			this.newProj = newProj;
			this.f = new File(this.uri);
			this.tsdir = f.getParentFile();
			this.newTsdir = new File(this.basePath + this.newProj, this.tsdir.getName());
			this.xml =  new File(tsdir, sess + ".xml");
			copy = new Copy(tsdir, newTsdir, sess);
			setXml = new SetXml(xml,newProj);
			writeXml = new WriteXml(newTsdir,sess);
		}

		void throwSync (String msg) throws SyncFailedException {
			throw new SyncFailedException(msg);
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
		this._move(new Move (this.basePath, sd.getName(), sd.getUrl(), newProj){});
	}
	
	protected void _move(Move move) throws SyncFailedException {
		move.run();
	}
	
	public void delete(SessionData sd) {
		File f = new File(sd.getUrl());
		new File(f.getPath() + ".xml").delete();	// remove the session XML
		final File tsdir = f.getParentFile();
		FileUtils.DeleteFile(f);
		tsdir.delete();	// delete timestamp parent only if empty.
	}
	
	public void resetStatus(SessionData sd) {
		
	}
	
	public void setStatus(SessionData sd, PrearcStatus status) {
		// TODO Auto-generated method stub
	}
}
