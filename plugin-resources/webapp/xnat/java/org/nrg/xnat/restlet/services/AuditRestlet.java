package org.nrg.xnat.restlet.services;

import java.util.List;
import java.util.Map;

import org.nrg.action.ActionException;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xdat.security.Authorizer;
import org.nrg.xft.ItemI;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.restlet.representations.ItemHTMLRepresentation;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.google.common.collect.Maps;

/**
 * @author Tim Olsen
 *
 *	The audit restlet is a generic restlet for accessing the audit trail for any items stored in XNAT.
 *
 *	Use a combination of the xsi:type and the primary key value to access the audit trail
 *
 *  /services/audit/xdat:user/1
 *  /services/audit/xnat:projectData/TEST_PROJECT
 *  
 *  
 */
public class AuditRestlet extends SecureResource {
	ItemI item;
	final String key;

	public AuditRestlet(Context context, Request request, Response response) {
		super(context, request, response);

		String xsiType=this.filepath.substring(0, filepath.indexOf("/"));
		key=this.filepath.substring(filepath.indexOf("/")+1);
		

		List<String> ids=StringUtils.DelimitedStringToArrayList(key, ",");
		
		try {
			item=retrieveItemByIds(xsiType, ids);
		} catch (ActionException e) {
			respondToException(e,e.getStatus());
		}

		if(item!=null){
	        this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	        this.getVariants().add(new Variant(MediaType.TEXT_HTML));
		}
	}
	
	public ItemI retrieveItemByIds(final String xsiType, List<String> ids) throws ActionException{		
		try {
			GenericWrapperElement element=GenericWrapperElement.GetElement(xsiType);
			
			CriteriaCollection cc = new CriteriaCollection("AND");
			
			List<String> pks=element.getPkNames();
			
			if(pks.size()!=ids.size()){
				throw new ClientException("Missing required primary key values");
			}
			
			for(int i=0;i<element.getPkNames().size();i++){
				cc.addClause(element.getXSIType()+"/"+pks.get(i), ids.get(i));
			}
			
			ItemI i=ItemSearch.GetItems(xsiType, cc, this.user, false).getFirst();
			
			if(i !=null){
				Authorizer.getInstance().authorizeRead(i.getItem(), user);
			}
			
			return i;
		} catch (ElementNotFoundException e) {
			throw new ClientException(e);
		}  catch (ActionException e) {
			throw e;
		}catch (Exception e) {
			throw new ServerException(Status.SERVER_ERROR_INTERNAL, e);
		}
	}

	
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		MediaType mt=overrideVariant(variant);
		
		try {
			if(mt.equals(MediaType.TEXT_HTML)){
				String screen=getQueryVariable("requested_screen");
				if(screen==null){
					screen="WorkflowHistorySummary";
				}
				
				Map<String,Object> params=Maps.newHashMap();
				
				params.put("key", key);
				
				if(hasQueryVariable("includeFiles")){
					params.put("includeFiles", getQueryVariable("includeFiles"));
				}
				
				if(hasQueryVariable("includeDetails")){
					params.put("includeDetails", getQueryVariable("includeDetails"));
				}
				
				params.put("hideTopBar",isQueryVariableTrue("hideTopBar"));
				
				return new ItemHTMLRepresentation(item.getItem(), MediaType.TEXT_HTML, getRequest(), user,screen,params);
			}else{
				return buildChangesets(item.getItem(), key, mt);
			}
		} catch (Exception e) {
			logger.error("",e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
			return null;
		}
	}

	
}
