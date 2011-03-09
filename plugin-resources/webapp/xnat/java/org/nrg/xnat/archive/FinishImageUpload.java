/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.xnat.archive;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.SyncFailedException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.nrg.action.ActionException;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.status.ListenerUtils;
import org.nrg.status.StatusProducer;
import org.nrg.status.StatusPublisherI;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.prearchive.SessionException;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.nrg.xnat.restlet.actions.PrearcImporterA.PrearcSession;
import org.nrg.xnat.restlet.util.RequestUtil;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;

/**
 * @author Timothy R Olsen
 *
 */
public class FinishImageUpload extends StatusProducer implements Callable<String>,StatusPublisherI {
    private final org.apache.log4j.Logger logger = Logger.getLogger(FinishImageUpload.class);
	private final PrearcSession session;
	private final UriParserUtils.DataURIA destination;
	private final boolean allowDataDeletion, overwrite,inline;
	private final XDATUser user;
	
	static List<String> prearc_variables=Lists.newArrayList(RequestUtil.AA,RequestUtil.AUTO_ARCHIVE,PrearcUtils.PREARC_SESSION_FOLDER,PrearcUtils.PREARC_TIMESTAMP);

	public FinishImageUpload(Object control, XDATUser user,final PrearcSession session,final UriParserUtils.DataURIA destination, final boolean allowDataDeletion, final boolean overwrite, final boolean inline) {
		super(control);
		this.session=session;
		this.destination=destination;
		this.allowDataDeletion=allowDataDeletion;
		this.overwrite=overwrite;
		this.user=user;
		this.inline=inline;
	}
	
	@Override
	public String call() throws ActionException {
		if(isAutoArchive(session,destination)){
			try {
				if(inline){
					//This is being done as part of a parent transaction and should not manage prearc cache state.
					return ListenerUtils.addListeners(this, new PrearcSessionArchiver(session, user, removePrearcVariables(session.getAdditionalValues()), allowDataDeletion,overwrite))
					.call();
				}else{
					if (PrearcDatabase.setStatus(session.getFolderName(), session.getTimestamp(), session.getProject(), PrearcUtils.PrearcStatus.ARCHIVING)) {
						return PrearcDatabase.archive(session, allowDataDeletion, overwrite, user, getListeners());
					}else{
						throw new ServerException("Unable to lock session for archiving.");
					}
				}
			} catch (SyncFailedException e) {
				logger.error("",e);
				throw new ServerException(e);
			} catch (IOException e) {
				logger.error("",e);
				throw new ServerException(e);
			} catch (SAXException e) {
				logger.error("",e);
				throw new ServerException(e);
			} catch (SessionException e) {
				logger.error("",e);
				throw new ServerException(e);
			} catch (SQLException e) {
				logger.error("",e);
				throw new ServerException(e);
			} catch (Exception e){
				logger.error("",e);
				throw new ServerException(e);
			}
		}else{
			populateAdditionalFields(session.getSessionDir());
			return session.getUrl().toString();
		}
	}

	/**
	 * This method will allow users to pass xml path as parameters.  The values supplied will be copied into the loaded session.
	 *
	 * @throws ElementNotFoundException
	 * @throws FieldNotFoundException
	 * @throws InvalidValueException
	 */
	private void populateAdditionalFields(final File sessionDIR) throws ActionException{
		//prepare params by removing non xml path names
		final Map<String,Object> cleaned=XMLPathShortcuts.identifyUsableFields(session.getAdditionalValues(),XMLPathShortcuts.EXPERIMENT_DATA,false);

		if(cleaned.size()>0){
			final SAXReader reader = new SAXReader(user);
			final File xml=new File(sessionDIR.getParentFile(),sessionDIR.getName()+".xml");
	        
			try {
				XFTItem item = reader.parse(xml.getAbsolutePath());
			
				try {
					item.setProperties(cleaned, true);
				} catch (Exception e) {
					failed("unable to map parameters to valid xml path: " + e.getMessage());
					throw new ClientException("unable to map parameters to valid xml path: ", e);
				}
				
				FileWriter fw=null;
				try {
					fw = new FileWriter(xml);
					item.toXML(fw, false);
				} catch (IllegalArgumentException e) {
					throw new ServerException(e);
				} catch (IOException e) {
					throw new ServerException(e);
				} catch (SAXException e) {
					throw new ServerException(e);
				}finally{
					try {if(fw!=null)fw.close();} catch (IOException e) {}
				}
			} catch (IOException e1) {
				throw new ServerException(e1);
			} catch (SAXException e1) {
				throw new ServerException(e1);
			}
		}
	}
	
	private static boolean isAutoArchive(final Map<String,Object> params){
		String aa = (String)params.get(RequestUtil.AA);
		
		if(aa==null){
			aa = (String)params.get(RequestUtil.AUTO_ARCHIVE);
		}
		
		
		if(aa!=null && aa.toString().equalsIgnoreCase(RequestUtil.TRUE)){
			return true;
		}else{
			return false;
		}
	}
	
	public boolean isAutoArchive() throws IOException, SAXException{
		return isAutoArchive(session,destination);
	}
	
	private static boolean isAutoArchive(final PrearcSession session, final UriParserUtils.DataURIA destination){
		//determine auto-archive setting
		if(session.getProject()==null){
			return false;
		}
				
		if(destination !=null && destination instanceof UriParserUtils.ArchiveURI){
			return true;
		}
		
		if(isAutoArchive(session.getAdditionalValues())){
			return true;
		}
						
		final Integer code=ArcSpecManager.GetInstance().getPrearchiveCodeForProject(session.getProject());
		if(code!=null && code.equals(4)){
			return true;
		}
		
		return false;
	}
	
	public static Map<String,Object> removePrearcVariables(final Map<String,Object> params){
		for(String param: prearc_variables){
			params.remove(param);
		}
		return params;
	}
}
