/**
 * Copyright 2013 Washington University
 */
package org.nrg.xnat.utils;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.nrg.action.ActionException;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.ResourceURII;
import org.nrg.xnat.turbine.utils.ArchivableItem;
import org.restlet.data.Status;

/**
 * @author Tim Olsen <tim@deck5consulting.com>
 * 
 * Helper methods for interacting with XnatResource's
 */
public class ResourceUtils {
	
	/**
	 * Refresh values in resource catalog 
	 * @param resource
	 * @return true if the catalog was modified
	 * @throws Exception Failures stem from the save operation.
	 */
	public static boolean refreshResourceCatalog(final XnatAbstractresource resource,final String projectPath,final boolean checksums,final UserI user,final EventMetaI now) throws Exception{
		if(resource instanceof XnatResourcecatalog){
			final XnatResourcecatalog catRes=(XnatResourcecatalog)resource;
			
			final CatCatalogBean cat=CatalogUtils.getCatalog(projectPath, catRes);
			final File catFile=CatalogUtils.getCatalogFile(projectPath, catRes);
			
			if(cat!=null){
				if(CatalogUtils.formalizeCatalog(cat, catFile.getParent(), user, now, checksums)){
					CatalogUtils.writeCatalogToFile(cat, catFile,checksums);
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Refresh all of the catalogs for a given archivable item.
	 * @param resourceURI
	 * @param user
	 * @param now
	 * @throws ActionException
	 */
	public static void refreshResourceCatalog(final ArchiveItemURI resourceURI,final XDATUser user,final EventDetails details) throws ActionException{
		try {
			if(resourceURI instanceof ResourceURII){//if we are referencing a specific catalog, make sure it doesn't actually reference an individual file.
				if(StringUtils.isNotEmpty(((ResourceURII)resourceURI).getResourceFilePath()) && !((ResourceURII)resourceURI).getResourceFilePath().equals("/")){
					throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,new Exception("This operation cannot be performed directly on a file URL"));
				}
			}
			
			final ArchivableItem security=resourceURI.getSecurityItem();
			if(!user.canEdit(security)){
				throw new ClientException(Status.CLIENT_ERROR_FORBIDDEN, new Exception("Unauthorized attempt to add a file to "+ resourceURI.getUri()));
			}
			
			final PersistentWorkflowI wrk = PersistentWorkflowUtils.getOrCreateWorkflowData(null, user, resourceURI.getSecurityItem().getItem(), details);
			
			for(XnatAbstractresourceI res:resourceURI.getResources(true)){
				refreshResourceCatalog((XnatAbstractresource)res, resourceURI.getSecurityItem().getArchiveRootPath(), true, user, wrk.buildEvent());
			}
			
			WorkflowUtils.complete(wrk, wrk.buildEvent());
		} catch (InvalidItemException e) {
			throw new ServerException(e);
		} catch (UnknownPrimaryProjectException e) {
			throw new ServerException(e);
		} catch (Exception e) {
			throw new ServerException(e);
		}
		
	}
}
