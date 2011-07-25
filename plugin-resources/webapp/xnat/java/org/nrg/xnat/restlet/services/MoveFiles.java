package org.nrg.xnat.restlet.services;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;

import org.apache.jcs.engine.CacheUtils;
import org.nrg.action.ActionException;
import org.nrg.action.ClientException;
import org.nrg.xnat.helpers.move.FileMover;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveURI;
import org.nrg.xnat.helpers.uri.URIManager.DataURIA;
import org.nrg.xnat.helpers.uri.URIManager.UserCacheURI;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.UriParserUtils.UriParser;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.restlet.util.RequestUtil;
import org.nrg.xnat.utils.UserUtils;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.util.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class MoveFiles extends SecureResource {

	
	private static final String OVERWRITE = "overwrite";
	private final static Logger logger = LoggerFactory.getLogger(MoveFiles.class);
	
	public MoveFiles(Context context, Request request, Response response) {
		super(context, request, response);
				
	}
	
	@Override
	public boolean allowPost() {
		return true;
	}

	Map<URIManager.UserCacheURI,URIManager.ArchiveURI> moves=Maps.newHashMap();
	Boolean overwrite=null;

	public void handleParam(final String key,final Object value) throws ClientException{
		if(value!=null){
			if(key.contains("/")){
				moves.put(convertKey(key), convertValue((String)value));
			}else if(key.equals(OVERWRITE)){
				overwrite=Boolean.valueOf((String)value);
			}
		}
	}
	
	public URIManager.UserCacheURI convertKey(final String key) throws ClientException{
		try {
			URIManager.DataURIA uri=UriParserUtils.parseURI(key);
			
			if(uri instanceof URIManager.UserCacheURI){
				return (URIManager.UserCacheURI)uri;
			}else{
				throw new ClientException("Invalid Source:"+ key);
			}
		} catch (MalformedURLException e) {
			throw new ClientException("Invalid Source:"+ key,e);
		}
	}
	
	public URIManager.ArchiveURI convertValue(final String key) throws ClientException{
		try {
			URIManager.DataURIA uri=UriParserUtils.parseURI(key);
			
			if(uri instanceof URIManager.ArchiveURI){
				return (URIManager.ArchiveURI)uri;
			}else{
				throw new ClientException("Invalid Destination:"+ key);
			}
		} catch (MalformedURLException e) {
			throw new ClientException("Invalid Destination:"+ key,e);
		}
	}

	@Override
	public void handlePost() {		
		//build fileWriters
		try {
			final Representation entity = this.getRequest().getEntity();
													
			if (RequestUtil.isMultiPartFormData(entity)) {
				loadParams(new Form(entity));
			}
			
			loadParams(getQueryVariableForm());			
			
			//this should allow injection of a different implementation- TO
			final FileMover mover =new FileMover(overwrite,user);
			
			for(Map.Entry<URIManager.UserCacheURI,URIManager.ArchiveURI> entry: moves.entrySet()){
				mover.call(entry.getKey(),entry.getValue());
			}
		} catch (ActionException e) {
			this.getResponse().setStatus(e.getStatus(), e.getMessage());
			return;
		} catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
			return;
		}
	}
}
