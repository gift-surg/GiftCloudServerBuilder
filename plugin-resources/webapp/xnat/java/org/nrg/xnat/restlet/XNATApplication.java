// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.framework.logging.Analytics;
import org.nrg.framework.utilities.Reflection;
import org.nrg.xnat.helpers.dicom.DicomDump;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.restlet.guard.XnatSecureGuard;
import org.nrg.xnat.restlet.resources.*;
import org.nrg.xnat.restlet.resources.files.CatalogResource;
import org.nrg.xnat.restlet.resources.files.CatalogResourceList;
import org.nrg.xnat.restlet.resources.files.DIRResource;
import org.nrg.xnat.restlet.resources.files.FileList;
import org.nrg.xnat.restlet.resources.protocols.ProjectSubjectVisitsRestlet;
import org.nrg.xnat.restlet.services.Archiver;
import org.nrg.xnat.restlet.services.Importer;
import org.nrg.xnat.restlet.services.RemoteLoggingRestlet;
import org.nrg.xnat.restlet.services.SettingsRestlet;
import org.nrg.xnat.restlet.services.mail.MailRestlet;
import org.nrg.xnat.restlet.services.prearchive.PrearchiveBatchDelete;
import org.nrg.xnat.restlet.services.prearchive.PrearchiveBatchMove;
import org.nrg.xnat.restlet.transaction.monitor.SQListenerRepresentation;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.resource.Resource;
import org.restlet.util.Template;

import java.util.List;

/**
 * @author tolsen01
 *
 * To add additional URIs to this file (non-xnat developers), build a class which extends this class.  Override the addRoutes method (calling the super.addRoutes(router)) to add your new URIs.  Then modify the XNATRestletFactory to load your class as the default application.
 */
public class XNATApplication extends Application {
    private static final Log _log = LogFactory.getLog(XNATApplication.class);
    public static final String PREARC_PROJECT_URI = "/prearchive/projects/{PROJECT_ID}";
    public static final String PREARC_SESSION_URI = PREARC_PROJECT_URI + "/{SESSION_TIMESTAMP}/{SESSION_LABEL}";

	public XNATApplication(Context parentContext) {
        super(parentContext);

    }
    @Override
    public synchronized Restlet createRoot() {
        Router router = new Router(getContext());

        addRoutes(router);
        addExtensionRoutes(router);

        XnatSecureGuard guard = new XnatSecureGuard();
        guard.setNext(router);

        return guard;
    }

	private void attachArchiveURI(final Router router,final String uri,final Class<? extends Resource> clazz){
		router.attach(uri.intern(),clazz);
		router.attach(("/archive"+uri).intern(),clazz);
	}

	private void addRoutes(final Router router){
        attachArchiveURI(router,"/investigators",InvestigatorListResource.class);

        //BEGIN ---- Pipelines section
        attachArchiveURI(router,"/projects/{PROJECT_ID}/pipelines",ProjectPipelineListResource.class);
        attachArchiveURI(router,"/projects/{PROJECT_ID}/pipelines/{STEP_ID}/experiments/{EXPT_ID}",ProjtExptPipelineResource.class);
        //END ---- Pipelines section
        attachArchiveURI(router,"/config/edit/image/dicom/{RESOURCE}", DicomEdit.class);
        attachArchiveURI(router,"/config/edit/projects/{PROJECT_ID}/image/dicom/{RESOURCE}", DicomEdit.class);
        attachArchiveURI(router,"/config/{PROJECT_ID}/archive_spec",ProjectArchive.class);
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
        router.attach("/prearchive/experiments", org.nrg.xnat.restlet.resources.prearchive.RecentPrearchiveSessions.class);
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

        // Configuration Service
        router.attach("/config",ConfigResource.class);
        router.attach("/config/{TOOL_NAME}",ConfigResource.class);
        router.attach("/config/{TOOL_NAME}/{PATH_TO_FILE}",ConfigResource.class).setMatchingMode(Template.MODE_STARTS_WITH);
        router.attach("/projects/{PROJECT_ID}/config",ConfigResource.class);
        router.attach("/projects/{PROJECT_ID}/config/{TOOL_NAME}",ConfigResource.class);
        router.attach("/projects/{PROJECT_ID}/config/{TOOL_NAME}/{PATH_TO_FILE}",ConfigResource.class).setMatchingMode(Template.MODE_STARTS_WITH);
        
        // System services
        router.attach("/services/import",Importer.class);
        router.attach("/services/archive",Archiver.class);
        router.attach("/services/prearchive/move",PrearchiveBatchMove.class);
        router.attach("/services/prearchive/delete",PrearchiveBatchDelete.class);
        router.attach("/services/settings", SettingsRestlet.class);
        router.attach("/services/dicomdump", DicomDump.class);
        router.attach("/services/settings/{PROPERTY}", SettingsRestlet.class);
        router.attach("/services/settings/{PROPERTY}/{VALUE}", SettingsRestlet.class);
        router.attach("/services/logging/{" + Analytics.EVENT_KEY + "}", RemoteLoggingRestlet.class);
        router.attach("/services/mail/send", MailRestlet.class);

        router.attach("/status/{TRANSACTION_ID}",SQListenerRepresentation.class);

        router.attach("/version",VersionRepresentation.class);

        // TODO: These are placeholders for the protocol REST services to come.
        router.attach("/services/protocols/project/{PROJECT_ID}/subject/{SUBJECT_ID}/visits", ProjectSubjectVisitsRestlet.class);
        router.attach("/services/protocols/project/{PROJECT_ID}/subject/{SUBJECT_ID}/visits/{VISIT_ID}", ProjectSubjectVisitsRestlet.class);
        router.attach("/services/protocols/project/{PROJECT_ID}/subject/{SUBJECT_ID}/generate/{TYPE}", ProjectSubjectVisitsRestlet.class);
        
        attachArchiveURI(router,"/projects/{PROJECT_ID}/visits/{VISIT_ID}",VisitResource.class); //use this to get or delete a visit. Deletion will automatically dis-associate all experiments associated with the deleted visit.
        attachArchiveURI(router,"/visits/{VISIT_ID}",VisitResource.class); //for consistency with the URI result returned by ProjSubVisitList. only GET. DELETE on this URI does not work (you need to pass the project to delete)
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/visits",ProjSubVisitList.class); ///GET returns a list of the subject's visits. POST will create a new visit and define the new ID and label.
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/visits/{VISIT_ID}",SubjVisitResource.class); //GET returns the visit. PUT to creates or updates a visit using the passed in Label. DELETE removes the visit as in VisitResource.
        attachArchiveURI(router,"/projects/{PROJECT_ID}/visits/{VISIT_ID}/experiments",ExptVisitListResource.class);  //GET to return a list of experiments.
        attachArchiveURI(router,"/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/visits/{VISIT_ID}/experiments",ExptVisitListResource.class);  //GET to return a list of experiments.

	}

    /**
     * This method walks the <b>org.nrg.xnat.restlet.extensions</b> package and attempts to find extensions for the
     * set of available REST services.
     * @param router The URL router for the restlet servlet.
     */
    private void addExtensionRoutes(Router router) {

        List<Class<?>> classes;
        try {
            classes = Reflection.getClassesForPackage("org.nrg.xnat.restlet.extensions");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(XnatRestlet.class)) {
                XnatRestlet annotation = clazz.getAnnotation(XnatRestlet.class);
                boolean required = annotation.required();
                if (!Resource.class.isAssignableFrom(clazz)) {
                    String message = "You can only apply the XnatRestlet annotation to classes that subclass the org.restlet.resource.Resource class: " + clazz.getName();
                    if (required) {
                        throw new NrgServiceRuntimeException(message);
                    } else {
                        _log.error(message);
    }
}
                String[] paths = annotation.value();
                if(paths == null || paths.length == 0) {
                    String message = "You must specify a value for the XnatRestlet annotation to indicate the hosting path for the restlet extension in class: " + clazz.getName();
                    if (required) {
                        throw new NrgServiceRuntimeException(message);
                    } else {
                        _log.error(message);
                    }
                } else {
                    for (String path : paths) {
                        router.attach(path, (Class<? extends Resource>) clazz);
                    }
                }
            }
        }
    }
}
