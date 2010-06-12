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
import org.nrg.xnat.restlet.resources.ScanList;
import org.nrg.xnat.restlet.resources.ScanResource;
import org.nrg.xnat.restlet.resources.ScanTypeListing;
import org.nrg.xnat.restlet.resources.ScannerListing;
import org.nrg.xnat.restlet.resources.SubjAssessmentResource;
import org.nrg.xnat.restlet.resources.SubjectListResource;
import org.nrg.xnat.restlet.resources.SubjectResource;
import org.nrg.xnat.restlet.resources.files.CatalogResource;
import org.nrg.xnat.restlet.resources.files.CatalogResourceList;
import org.nrg.xnat.restlet.resources.files.DIRResource;
import org.nrg.xnat.restlet.resources.files.FileList;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.Router;

public class XNATApplication extends Application {
	public XNATApplication(Context parentContext) {
        super(parentContext);
    }

    @Override
    public synchronized Restlet createRoot() {
        Router router = new Router(getContext());

        router.attach("/investigators",InvestigatorListResource.class);
        
        //BEGIN ---- Pipelines section
        router.attach("/projects/{PROJECT_ID}/pipelines",ProjectPipelineListResource.class);
        router.attach("/projects/{PROJECT_ID}/pipelines/{STEP_ID}/experiments/{EXPT_ID}",ProjtExptPipelineResource.class);
        //END ---- Pipelines section

        router.attach("/projects/{PROJECT_ID}/archive_spec",ProjectArchive.class);
        router.attach("/projects/{PROJECT_ID}/experiments",ProjSubExptList.class);
        router.attach("/projects/{PROJECT_ID}/experiments/{EXPT_ID}",ExperimentResource.class);
        router.attach("/projects/{PROJECT_ID}/users",ProjectUserListResource.class);
        router.attach("/projects/{PROJECT_ID}/users/{GROUP_ID}/{USER_ID}",ProjectMemberResource.class);
        router.attach("/projects/{PROJECT_ID}/searches/{SEARCH_ID}",ProjectSearchResource.class);
        router.attach("/projects/{PROJECT_ID}/subjects",ProjectSubjectList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}",SubjectResource.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments",ProjSubExptList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}",SubjAssessmentResource.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors",ProjSubExptAsstList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}",ExptAssessmentResource.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans",ScanList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans/{SCAN_ID}",ScanResource.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions",ReconList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}",ReconResource.class);
        router.attach("/projects/{PROJECT_ID}/accessibility",ProjectAccessibilityResource.class);
        router.attach("/projects/{PROJECT_ID}/accessibility/{ACCESS_LEVEL}",ProjectAccessibilityResource.class);
        router.attach("/projects/{PROJECT_ID}",ProjectResource.class);
        router.attach("/projects",ProjectListResource.class);
        router.attach("/projects/{PROJECT_ID}/scan_types",ScanTypeListing.class);
        router.attach("/scan_types",ScanTypeListing.class);
        router.attach("/scanners",ScannerListing.class);

        router.attach("/projects/{PROJECT_ID}/protocols/{PROTOCOL_ID}",ProtocolResource.class);

        router.attach("/experiments",ExperimentListResource.class);
        router.attach("/experiments/{EXPT_ID}",ExperimentResource.class);
        router.attach("/experiments/{ASSESSED_ID}/scans",ScanList.class);
        router.attach("/experiments/{ASSESSED_ID}/scans/{SCAN_ID}",ScanResource.class);
        router.attach("/experiments/{ASSESSED_ID}/reconstructions",ReconList.class);
        router.attach("/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}",ReconResource.class);
		router.attach("/experiments/{ASSESSED_ID}/assessors",ProjSubExptAsstList.class);
        router.attach("/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}",ExptAssessmentResource.class);

        router.attach("/subjects/{SUBJECT_ID}",SubjectResource.class);
        router.attach("/subjects",SubjectListResource.class);
        
        //resources
        router.attach("/projects/{PROJECT_ID}/resources",CatalogResourceList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/resources",CatalogResourceList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}/resources",CatalogResourceList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources",CatalogResourceList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources",CatalogResourceList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources",CatalogResourceList.class);
        router.attach("/experiments/{EXPT_ID}/resources",CatalogResourceList.class);
        router.attach("/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/resources",CatalogResourceList.class);
        router.attach("/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/resources",CatalogResourceList.class);
        router.attach("/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources",CatalogResourceList.class);
        router.attach("/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources",CatalogResourceList.class);
        router.attach("/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources",CatalogResourceList.class);

        router.attach("/subjects/{SUBJECT_ID}/resources",CatalogResourceList.class);

        //resources (catalogs)
        router.attach("/projects/{PROJECT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources/{RESOURCE_ID}",CatalogResource.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources/{RESOURCE_ID}",CatalogResource.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        router.attach("/experiments/{EXPT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        router.attach("/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        router.attach("/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/resources/{RESOURCE_ID}",CatalogResource.class);
        router.attach("/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources/{RESOURCE_ID}",CatalogResource.class);
        router.attach("/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources/{RESOURCE_ID}",CatalogResource.class);
        router.attach("/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources/{RESOURCE_ID}",CatalogResource.class);

        router.attach("/subjects/{SUBJECT_ID}/resources/{RESOURCE_ID}",CatalogResource.class);

        //resource files
        router.attach("/projects/{PROJECT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/experiments/{EXPT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/resources/{RESOURCE_ID}/files",FileList.class);
        router.attach("/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/resources/{RESOURCE_ID}/files",FileList.class);

        router.attach("/subjects/{SUBJECT_ID}/resources/{RESOURCE_ID}/files",FileList.class);

        //file short-cut
        router.attach("/projects/{PROJECT_ID}/files", FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/files",FileList.class);
        router.attach("/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/files",FileList.class);
        router.attach("/experiments/{EXPT_ID}/files",FileList.class);
        router.attach("/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/{TYPE}/files",FileList.class);
        router.attach("/experiments/{ASSESSED_ID}/assessors/{EXPT_ID}/files",FileList.class);
        router.attach("/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/{TYPE}/files",FileList.class);
        router.attach("/experiments/{ASSESSED_ID}/reconstructions/{RECON_ID}/files",FileList.class);
        router.attach("/experiments/{ASSESSED_ID}/scans/{SCAN_ID}/files",FileList.class);

        router.attach("/subjects/{SUBJECT_ID}/files",FileList.class);

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
        
        router.attach("/projects/{PROJECT_ID}/prearchive",org.nrg.xnat.restlet.resources.PrearcSessionListResource.class);
        router.attach("/projects/{PROJECT_ID}/prearchive/sessions/{SESSION_TIMESTAMP}/{SESSION_LABEL}", org.nrg.xnat.restlet.resources.PrearcSessionResource.class);

        router.attach("/experiments/{EXPT_ID}/DIR",DIRResource.class);
        router.attach("/projects/{PROJECT_ID}/experiments/{EXPT_ID}/DIR",DIRResource.class);


        XnatSecureGuard guard = new XnatSecureGuard();
        guard.setNext(router);
        
        return guard;
    }
}
