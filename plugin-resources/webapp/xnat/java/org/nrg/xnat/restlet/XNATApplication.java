// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet;

import org.nrg.xnat.restlet.guard.XnatSecureGuard;
import org.nrg.xnat.restlet.resources.ExperimentListResource;
import org.nrg.xnat.restlet.resources.ExperimentResource;
import org.nrg.xnat.restlet.resources.ExptAssessmentResource;
import org.nrg.xnat.restlet.resources.InvestigatorListResource;
import org.nrg.xnat.restlet.resources.ProjSubExptAsstList;
import org.nrg.xnat.restlet.resources.ProjSubExptList;
import org.nrg.xnat.restlet.resources.ProjectAccessibilityResource;
import org.nrg.xnat.restlet.resources.ProjectArchive;
import org.nrg.xnat.restlet.resources.ProjectListResource;
import org.nrg.xnat.restlet.resources.ProjectMemberResource;
import org.nrg.xnat.restlet.resources.ProjectPipelineListResource;
import org.nrg.xnat.restlet.resources.ProjectResource;
import org.nrg.xnat.restlet.resources.ProjectSearchResource;
import org.nrg.xnat.restlet.resources.ProjectSubjectList;
import org.nrg.xnat.restlet.resources.ProjectUserListResource;
import org.nrg.xnat.restlet.resources.ProjtExptPipelineResource;
import org.nrg.xnat.restlet.resources.ProtocolResource;
import org.nrg.xnat.restlet.resources.ReconList;
import org.nrg.xnat.restlet.resources.ReconResource;
import org.nrg.xnat.restlet.resources.ScanDIRResource;
import org.nrg.xnat.restlet.resources.ScanList;
import org.nrg.xnat.restlet.resources.ScanResource;
import org.nrg.xnat.restlet.resources.ScanTypeListing;
import org.nrg.xnat.restlet.resources.ScannerListing;
import org.nrg.xnat.restlet.resources.SubjAssessmentResource;
import org.nrg.xnat.restlet.resources.SubjectListResource;
import org.nrg.xnat.restlet.resources.SubjectResource;
import org.nrg.xnat.restlet.resources.UserCacheResource;
import org.nrg.xnat.restlet.resources.VersionRepresentation;
import org.nrg.xnat.restlet.resources.files.CatalogResource;
import org.nrg.xnat.restlet.resources.files.CatalogResourceList;
import org.nrg.xnat.restlet.resources.files.DIRResource;
import org.nrg.xnat.restlet.resources.files.FileList;
import org.nrg.xnat.restlet.services.Archiver;
import org.nrg.xnat.restlet.services.Importer;
import org.nrg.xnat.restlet.services.prearchive.PrearchiveBatchDelete;
import org.nrg.xnat.restlet.services.prearchive.PrearchiveBatchMove;
import org.nrg.xnat.restlet.transaction.monitor.SQListenerRepresentation;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.resource.Resource;

/**
 * @author tolsen01
 *
 * To add additional URIs to this file (non-xnat developers), build a class which extends this class.  Override the addRoutes method (calling the super.addRoutes(router)) to add your new URIs.  Then modify the XNATRestletFactory to load your class as the default application.
 */
public class XNATApplication extends Application {
     public static String PREARC_PROJECT_URI = "/prearchive/projects/{PROJECT_ID}",
    PREARC_SESSION_URI = PREARC_PROJECT_URI + "/{SESSION_TIMESTAMP}/{SESSION_LABEL}";
    
	public XNATApplication(Context parentContext) {
        super(parentContext);
    }

	public void attachArchiveURI(final Router router,final String uri,final Class<? extends Resource> clazz){
		router.attach(uri.intern(),clazz);
		router.attach(("/archive"+uri).intern(),clazz);
	}
	
	public void addRoutes(final Router router){
        attachArchiveURI(router,"/investigators",InvestigatorListResource.class);
        
        //BEGIN ---- Pipelines section
        attachArchiveURI(router,"/projects/{PROJECT_ID}/pipelines",ProjectPipelineListResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/pipelines/{STEP_ID}/experiments/{EXPT_ID}",ProjtExptPipelineResource.class);
        //END ---- Pipelines section

        attachArchiveURI(router,"/projects/{PROJECT_ID}/archive_spec",ProjectArchive.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/experiments",ProjSubExptList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/experiments/{EXPT_ID}",ExperimentResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/users",ProjectUserListResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/users/{GROUP_ID}/{USER_ID}",ProjectMemberResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/searches/{SEARCH_ID}",ProjectSearchResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects",ProjectSubjectList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}",SubjectResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments",ProjSubExptList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}",SubjAssessmentResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors",ProjSubExptAsstList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}",ExptAssessmentResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans",ScanList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans/{SCAN_ID}",ScanResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/DICOMDIR",ScanDIRResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions",ReconList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}",ReconResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/accessibility",ProjectAccessibilityResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/accessibility/{ACCESS_LEVEL}",ProjectAccessibilityResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}",ProjectResource.class);
        attachArchiveURI(router,"/projects",ProjectListResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/scan_types",ScanTypeListing.class);
        attachArchiveURI(router,"/scan_types",ScanTypeListing.class);
        attachArchiveURI(router,"/scanners",ScannerListing.class);

        attachArchiveURI(router,"/projects/{PROJECT_ID}/protocols/{PROTOCOL_ID}",ProtocolResource.class);

        attachArchiveURI(router,"/experiments",ExperimentListResource.class);
        attachArchiveURI(router,"/experiments/{EXPT_ID}",ExperimentResource.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/scans",ScanList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/scans/{SCAN_ID}",ScanResource.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/DICOMDIR",ScanDIRResource.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/reconstructions",ReconList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}",ReconResource.class);
		attachArchiveURI(router,"/experiments/{ASSESSED_ID}/assessors",ProjSubExptAsstList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}",ExptAssessmentResource.class);

        attachArchiveURI(router,"/subjects/{SUBJECT_ID}",SubjectResource.class);
        attachArchiveURI(router,"/subjects",SubjectListResource.class);
        
        //resources
        attachArchiveURI(router,"/projects/{PROJECT_ID}/resources",CatalogResourceList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/resources",CatalogResourceList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}/resources",CatalogResourceList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources",CatalogResourceList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources",CatalogResourceList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources",CatalogResourceList.class);
        attachArchiveURI(router,"/experiments/{EXPT_ID}/resources",CatalogResourceList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/resources",CatalogResourceList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/resources",CatalogResourceList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources",CatalogResourceList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources",CatalogResourceList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources",CatalogResourceList.class);

        attachArchiveURI(router,"/subjects/{SUBJECT_ID}/resources",CatalogResourceList.class);

        //resources (catalogs)
        attachArchiveURI(router,"/projects/{PROJECT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources/{RESOURCE_ID}",CatalogResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources/{RESOURCE_ID}",CatalogResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        attachArchiveURI(router,"/experiments/{EXPT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources/{RESOURCE_ID}",CatalogResource.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources/{RESOURCE_ID}",CatalogResource.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources/{RESOURCE_ID}",CatalogResource.class);

        attachArchiveURI(router,"/subjects/{SUBJECT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);

        //resource files
        attachArchiveURI(router,"/projects/{PROJECT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{EXPT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources/{RESOURCE_ID}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources/{RESOURCE_ID}/files",FileList.class);

        attachArchiveURI(router,"/subjects/{SUBJECT_ID}/resources/{RESOURCE_ID}/files",FileList.class);

        //file short-cut
        attachArchiveURI(router,"/projects/{PROJECT_ID}/files", FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/files",FileList.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{EXPT_ID}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/files",FileList.class);
        attachArchiveURI(router,"/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/files",FileList.class);

        attachArchiveURI(router,"/subjects/{SUBJECT_ID}/files",FileList.class);

        router.attach("/users",org.nrg.xnat.restlet.resources.UserListResource.class);
        router.attach("/users/favorites/{DATA_TYPE}",org.nrg.xnat.restlet.resources.UserFavoritesList.class);
        router.attach("/users/favorites/{DATA_TYPE}/{PROJECT_ID}",org.nrg.xnat.restlet.resources.UserFavoriteResource.class);

        router.attach("/search",org.nrg.xnat.restlet.resources.search.SearchResource.class);
        router.attach("/search/elements",org.nrg.xnat.restlet.resources.search.SearchElementListResource.class);
        router.attach("/search/elements/{ELEMENT_NAME}",org.nrg.xnat.restlet.resources.search.SearchFieldListResource.class);
        router.attach("/search/elements/{ELEMENT_NAME}/versions",org.nrg.xnat.restlet.resources.search.SearchFieldsVersionListResource.class);
        router.attach("/search/saved",org.nrg.xnat.restlet.resources.search.SavedSearchListResource.class);
        router.attach("/search/saved/{SEARCH_ID}",org.nrg.xnat.restlet.resources.search.SavedSearchResource.class);
        router.attach("/search/{CACHED_SEARCH_ID}",org.nrg.xnat.restlet.resources.search.CachedSearchResource.class);
        router.attach("/search/{CACHED_SEARCH_ID}/{COLUMN}",org.nrg.xnat.restlet.resources.search.CachedSearchColumnResource.class);

        router.attach("/pars",org.nrg.xnat.restlet.resources.PARList.class);
        router.attach("/pars/{PAR_ID}",org.nrg.xnat.restlet.resources.PARResource.class);
        router.attach("/projects/{PROJECT_ID}/pars",org.nrg.xnat.restlet.resources.ProjectPARListResource.class);

        router.attach("/JSESSION",org.nrg.xnat.restlet.resources.UserSession.class);
        
        router.attach("/prearchive",org.nrg.xnat.restlet.resources.prearchive.PrearcSessionListResource.class);
        router.attach(PREARC_PROJECT_URI,org.nrg.xnat.restlet.resources.prearchive.PrearcSessionListResource.class);
        router.attach(PREARC_SESSION_URI, org.nrg.xnat.restlet.resources.prearchive.PrearcSessionResource.class);
        router.attach("/prearchive/projects/{PROJECT_ID}/{SESSION_TIMESTAMP}/{SESSION_LABEL}/scans", org.nrg.xnat.restlet.resources.prearchive.PrearcScansListResource.class);
        router.attach("/prearchive/projects/{PROJECT_ID}/{SESSION_TIMESTAMP}/{SESSION_LABEL}/scans/{SCAN_ID}/resources", org.nrg.xnat.restlet.resources.prearchive.PrearcSessionResourcesList.class);
        router.attach("/prearchive/projects/{PROJECT_ID}/{SESSION_TIMESTAMP}/{SESSION_LABEL}/scans/{SCAN_ID}/resources/{RESOURCE_ID}/files", org.nrg.xnat.restlet.resources.prearchive.PrearcSessionResourceFiles.class);

        attachArchiveURI(router,"/experiments/{EXPT_ID}/DIR",DIRResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/experiments/{EXPT_ID}/DIR",DIRResource.class);
        attachArchiveURI(router,"/experiments/{EXPT_ID}/XAR",DIRResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/experiments/{EXPT_ID}/XAR",DIRResource.class);

        // Users Cache Space
        router.attach("/user/cache/resources",UserCacheResource.class);
        router.attach("/user/cache/resources/{XNAME}",UserCacheResource.class);
        router.attach("/user/cache/resources/{XNAME}/files",UserCacheResource.class);
        router.attach("/user/cache/resources/{XNAME}/files/{FILE}",UserCacheResource.class);


        router.attach("/services/import",Importer.class);
        router.attach("/services/archive",Archiver.class);
        router.attach("/services/prearchive/move",PrearchiveBatchMove.class);
        router.attach("/services/prearchive/delete",PrearchiveBatchDelete.class);
        
        router.attach("/status/{TRANSACTION_ID}",SQListenerRepresentation.class);
        
        router.attach("/version",VersionRepresentation.class);
	}
        
    @Override
    public synchronized Restlet createRoot() {
        Router router = new Router(getContext());

        addRoutes(router);
        
        XnatSecureGuard guard = new XnatSecureGuard();
        guard.setNext(router);
        
        return guard;
    }
}
