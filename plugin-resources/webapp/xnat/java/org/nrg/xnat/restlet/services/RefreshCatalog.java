/**
 *
 */
package org.nrg.xnat.restlet.services;

import java.util.List;

import org.nrg.action.ActionException;
import org.nrg.action.ClientException;
import org.nrg.xft.event.EventUtils;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.RequestUtil;
import org.nrg.xnat.utils.ResourceUtils;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

/**
 * @author Tim Olsen
 *
 *	The refresh catalog restlet is a generic restlet for refreshing catalog entries.
 *
 *  It will review the query string parameters and a multi-part form body.  For each parameter named 'resource' it will refresh the associated catalogs
 *
 *  Options:
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT/subjects/SUBJECT/experiments/EXPT/scans/SCAN/resources/LABEL
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT/subjects/SUBJECT/experiments/EXPT/scans/SCAN
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT/subjects/SUBJECT/experiments/EXPT/reconstructions/RECON/resources/LABEL
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT/subjects/SUBJECT/experiments/EXPT/reconstructions/RECON
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT/subjects/SUBJECT/experiments/EXPT/assessors/ASSESSOR/resources/LABEL
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT/subjects/SUBJECT/experiments/EXPT/assessors/ASSESSOR
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT/subjects/SUBJECT/experiments/EXPT/resources/LABEL
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT/subjects/SUBJECT/experiments/EXPT
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT/subjects/SUBJECT/resources/LABEL
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT/subjects/SUBJECT -- will not cascade to children (experiments)
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT/resources/LABEL
 *  /services/refresh/catalog?resource=/archive/projects/PROJECT-- will not cascade to children (subjects or experiments)
 *  /services/refresh/catalog?resource=/archive/experiments/EXPT/scans/SCAN/resources/DICOM
 *  /services/refresh/catalog?resource=/archive/experiments/EXPT/scans/SCAN
 *  /services/refresh/catalog?resource=/archive/experiments/EXPT/reconstructions/RECON/resources/LABEL
 *  /services/refresh/catalog?resource=/archive/experiments/EXPT/reconstructions/RECON
 *  /services/refresh/catalog?resource=/archive/experiments/EXPT/assessors/ASSESSOR/resources/LABEL
 *  /services/refresh/catalog?resource=/archive/experiments/EXPT/assessors/ASSESSOR
 *  /services/refresh/catalog?resource=/archive/experiments/EXPT/resources/LABEL
 *  /services/refresh/catalog?resource=/archive/experiments/EXPT
 *  /services/refresh/catalog?resource=/archive/subjects/SUBJECT/resources/LABEL
 *  /services/refresh/catalog?resource=/archive/subjects/SUBJECT -- will not cascade to children (experiments)
 *
 *  Multiple resource paths can be specified in a single submit using multiple resource parameters on the query string or in a multipart form body
 */
public class RefreshCatalog extends SecureResource {

	public RefreshCatalog(Context context, Request request, Response response) {
		super(context, request, response);
	}

	@Override
	public boolean allowGet() {
		return true;
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	List<String> resources=Lists.newArrayList();
	ListMultimap<String,Object> otherParams=ArrayListMultimap.create();

	public void handleParam(final String key,final Object value) throws ClientException{
		if(value!=null){
			if(key.equals("resource")){
				resources.add((String)value);
			}else{
				otherParams.put(key, value);
			}
		}
	}

	@Override
	public void handlePost() {
		try {
			final Representation entity = this.getRequest().getEntity();

			//parse body to identify resources if its multi-part form data
			//TODO: Handle JSON body.
			if (RequestUtil.isMultiPartFormData(entity)) {
				loadParams(new Form(entity));
			}
			loadQueryVariables();//parse query string to identify resources

			for(final String resource:resources){
				//parse passed URI parameter
				URIManager.DataURIA uri=UriParserUtils.parseURI(resource);

				ArchiveItemURI resourceURI=null;
				if(uri instanceof ArchiveItemURI){
					resourceURI=(ArchiveItemURI)uri;
				}else{
					throw new ClientException("Invalid Resource URI:"+ resource);
				}

				//call refresh operation
				ResourceUtils.refreshResourceCatalog(resourceURI, user,this.newEventInstance(EventUtils.CATEGORY.DATA, "Catalog(s) Refreshed"));
			}

			this.getResponse().setStatus(Status.SUCCESS_OK);
		} catch (ActionException e) {
			this.getResponse().setStatus(e.getStatus(), e.getMessage());
			logger.error("",e);
			return;
		} catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
			logger.error("",e);
			return;
		}
	}


}
