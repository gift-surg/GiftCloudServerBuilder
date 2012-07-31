/**
 * Copyright (c) 2012 Washington University
 */
package org.nrg.xnat.helpers.dicom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dcm4che2.data.ElementDictionary;
import org.nrg.action.ClientException;
import org.nrg.xdat.bean.CatDcmentryBean;
import org.nrg.xdat.model.CatCatalogI;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.model.XnatReconstructedimagedataI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilder;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.utils.CatalogUtils;
import org.nrg.xnat.utils.CatalogUtils.CatEntryFilterI;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.restlet.util.Template;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;


public final class DicomDump extends SecureResource {
    // "src" attribute the contains the uri to the desired resources
    private static final String SRC_ATTR = "src";
    private static final String FIELD_PARAM = "field";
    // image type supported.
    private static final String imageType = "DICOM";

    private static final ElementDictionary TAG_DICTIONARY = ElementDictionary.getDictionary();

    // The global environment
    private final Env env;

    /**
     * A global environment that contains the type of request has made and a
     * map of the parsed "src" uri.
     * @author aditya
     *
     */
    class Env {
        Map<String,Object> attrs = new HashMap<String, Object>();
        HeaderType h;
        ArchiveType a;
        ResourceType r;
        final String uri; 
        final Map<Integer,Set<String>> fields;

        Env(String uri, Map<Integer,Set<String>> fields){
            this.uri = uri;
            this.a = ArchiveType.UNKNOWN;
            this.h = HeaderType.UNKNOWN;
            this.r = ResourceType.UNKNOWN;
            this.fields = fields;
            this.determineArchiveType();
            this.determineHeaderType();
            this.determineResourceType();
        }

        ArchiveType getArchiveType() {
            return this.a;
        }
        HeaderType getHeaderType() {
            return this.h;
        }
        ResourceType getResourceType() {
            return this.r;
        }
        void determineArchiveType () {
            if (this.uri.startsWith("/prearchive/")){
                this.a = ArchiveType.PREARCHIVE;
            }
            else if (this.uri.startsWith("/archive/")) {
                this.a = ArchiveType.ARCHIVE;
            }
            else {
                this.a = ArchiveType.UNKNOWN;
            }
        }

        /**
         * If a summary is requested then the resource type defaults to SCAN.
         */
        void determineResourceType() {
            if (this.a != ArchiveType.UNKNOWN && this.h != HeaderType.UNKNOWN) {
                this.r = ResourceType.SCAN;
            }
        }

        /**
         * Find a matching template and update the global environment
         * @param _h
         */
        void visit (HeaderType _h) {
            List<Template> ts = _h.getTemplates();
            Iterator<Template> i = ts.iterator();
            while (i.hasNext()) {
                Template t = i.next();
                if (t.match(this.uri) != -1) {
                    t.parse(this.uri, this.attrs);
                    this.h = _h;
                    this.r = _h.getResourceType(t);
                    break;
                }
            }
        }

        void determineHeaderType() {
            for (HeaderType h : HeaderType.values()) {
                if (this.h == HeaderType.UNKNOWN) {
                    this.visit(h);
                }
                else {
                    // the uri has been parsed so quit looking
                }
            }
        }
    }

    /**
     * The dicom dump requested, either a specific file, or a general summary of the session
     * @author aditya
     *
     */
    private static enum HeaderType {
        FILE(){
            @Override
            List<Template> getTemplates() {
                String[] ss = {
                        "/prearchive/projects/{PROJECT_ID}/{TIMESTAMP}/{EXPT_ID}/scans/{SCAN_ID}/resources/DICOM/files",
                        "/archive/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}/scans/{SCAN_ID}/resources/DICOM/files",
                        "/archive/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}/assessors/{SCAN_ID}/resources/DICOM/files",
                        "/archive/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}/recons/{SCAN_ID}/resources/DICOM/files"	
                };
                return toTemplates(ss);
            }
            @Override
            ResourceType getResourceType(Template matchingTemplate) {
                ResourceType r = ResourceType.UNKNOWN;
                if (matchingTemplate.getPattern().contains("scans")) {
                    r = ResourceType.SCAN;
                }
                else if (matchingTemplate.getPattern().contains("assessors")) {
                    r = ResourceType.ASSESSOR;
                }
                else if (matchingTemplate.getPattern().contains("recons")) {
                    r = ResourceType.RECON;
                }
                else {
                    // unknown
                }
                return r;
            }
            @Override
            String retrieve(Env env, XDATUser user) throws ClientException, IOException, InvalidPermissionException, Exception {
                final Collection<File> matches = env.r.getFiles(env,user, new CatFilterWithPath() {
                    public boolean accept(CatEntryI entry) {
                        return entry instanceof CatDcmentryBean;
                    }
                });
                for (final Iterator<File> fi = matches.iterator(); fi.hasNext(); ) {
                    final File f = fi.next();
                    if (null != f) {
                        return f.getAbsolutePath();
                    }
                }
                return null;
            }
        },
        SUMMARY(){
            @Override
            List<Template> getTemplates() {
                String[] ss = {
                        "/archive/projects/{PROJECT_ID}/subjects/{SUBJECT_ID}/experiments/{EXPT_ID}",
                        "/archive/projects/{PROJECT_ID}/experiments/{EXPT_ID}",
                        "/prearchive/projects/{PROJECT_ID}/{TIMESTAMP}/{EXPT_ID}"
                };
                return toTemplates(ss);
            }
            @Override
            ResourceType getResourceType (Template t) {
                return ResourceType.UNKNOWN;
            }
            @Override
            String retrieve(Env env, XDATUser user) throws ClientException, IOException, InvalidPermissionException, Exception {
                CatFilterWithPath dummy = new CatFilterWithPath() {
                    @Override
                    public boolean accept(CatEntryI entry) {
                        return true;
                    }
                };

                Collection<File> matches = env.r.getFiles(env,user, dummy);
                List<File> list = new ArrayList<File>(matches);
                if (list.size() > 0) {
                    return list.get(0).getAbsolutePath();
                }
                else {
                    return null;
                }
            }
        },
        UNKNOWN(){
            @Override
            List<Template> getTemplates() {
                return new ArrayList<Template>();
            }
            @Override
            ResourceType getResourceType (Template t) {
                return ResourceType.UNKNOWN;
            }
            @Override
            String retrieve(Env env, XDATUser user) {
                return null;
            }
        };

        // Convert a set of templated string to Template objects
        List<Template> toTemplates (String [] ss) {
            List<Template> ts = new ArrayList<Template>();
            for (String s : ss) {
                ts.add(new Template(s, Template.MODE_STARTS_WITH));
            }
            return ts;
        }

        /**
         * The URI templates associated with type
         * @return
         */
        abstract List<Template> getTemplates ();

        /**
         * Based on the matching template output the correct resource type 
         * @param matchingTemplate
         * @return
         */
        abstract ResourceType getResourceType(Template matchingTemplate);

        /**
         * Retrieve the file path to the specified file. For the summary view the path to the first dicom file found is returned.
         * 
         * Returns null if no DICOM file is found.
         * 
         * @param env
         * @param user
         * @return
         * @throws ClientException
         * @throws IOException
         * @throws InvalidPermissionException
         * @throws Exception
         */

        abstract String retrieve(Env env, XDATUser user) throws ClientException, IOException, InvalidPermissionException, Exception;
    };

    /**
     * The location of the requested session.
     *  
     * This class does some slightly unkosher things with global variables.
     * 
     * There are two global class variables, one holding the root path to a resource and the image session object associated
     * with requested session. These variables are updated *implicitly* by functions in the class, so the call order of the methods
     * in this class is important.
     *    
     * Please read the method comments for more details. 
     * @author aditya
     *
     */
    private static enum ArchiveType {
        PREARCHIVE(){
            @Override
            CatCatalogI getCatalog(XnatResourcecatalogI r) {
                this.rootPath=CatalogUtils.getCatalogFile(this.x.getPrearchivepath(), ((XnatResourcecatalogI)r)).getParentFile().getAbsolutePath();			
                final CatCatalogI catalog=CatalogUtils.getCleanCatalog(this.x.getPrearchivepath(),r, false);
                return catalog;
            }

            @Override
            XnatImagesessiondataI retrieve(Env env, XDATUser user) throws Exception, IOException, InvalidPermissionException{
                String project = (String) env.attrs.get("PROJECT_ID");
                String experiment = (String)env.attrs.get("EXPT_ID");
                String timestamp = (String) env.attrs.get("TIMESTAMP");
                File sessionDIR;
                File srcXML;
                sessionDIR = PrearcUtils.getPrearcSessionDir(user, project, timestamp, experiment,false);
                srcXML=new File(sessionDIR.getAbsolutePath()+".xml");
                XnatImagesessiondataI x = PrearcTableBuilder.parseSession(srcXML);
                this.x = x;
                return x;
            }
        },
        ARCHIVE(){
            @Override
            CatCatalogI getCatalog(XnatResourcecatalogI r) {
                this.rootPath = (new File(r.getUri())).getParent();
                final CatCatalogI catalog=CatalogUtils.getCleanCatalog(this.rootPath, r, true);
                return catalog;
            }

            @Override
            XnatImagesessiondataI retrieve(Env env, XDATUser user) throws ClientException,IOException,InvalidPermissionException,Exception {
                String project = (String) env.attrs.get("PROJECT_ID");
                String experiment = (String)env.attrs.get("EXPT_ID");
                XnatImagesessiondata x = (XnatImagesessiondata) XnatExperimentdata.GetExptByProjectIdentifier(project, experiment,user, false);
                if (x == null || null == x.getId()) {
                    x = (XnatImagesessiondata) XnatExperimentdata.getXnatExperimentdatasById(experiment, user, false);
                    if (x != null && !x.hasProject(project)) {
                        x = null;
                    }
                }
                if (x == null) {
                    throw new ClientException(Status.CLIENT_ERROR_NOT_FOUND, 
                            "Experiment or project not found", 
                            new Exception ("Experiment or project not found"));
                }
                this.x = x;
                return x;
            }
        },

        UNKNOWN() {
            @Override
            CatCatalogI getCatalog(XnatResourcecatalogI r) {
                return null;
            }
            @Override
            XnatImagesessiondataI retrieve(Env env, XDATUser user) { 
                return null;
            }
        };

        XnatImagesessiondataI x = null;
        String rootPath=null;

        /**
         * Retrieve the catalog for this resource. Additionally this also updates the 
         * "rootPath" global class variable. This function is dependent on the 
         * {@link XnatImageassessordataI} having been populated.
         * @param r
         * @return
         */
        abstract CatCatalogI getCatalog(XnatResourcecatalogI r);

        /**
         * Retrieve the image session object for this session. Additionally this also updates the
         * XnatImagesessiondataI global. 
         * @param env
         * @param user
         * @return
         * @throws ClientException
         * @throws IOException
         * @throws InvalidPermissionException
         * @throws Exception
         */
        abstract XnatImagesessiondataI retrieve(Env env, XDATUser user) throws ClientException, IOException, InvalidPermissionException, Exception;
    };

    /**
     * The type of resource requested.
     * @author aditya
     *
     */
    private static enum ResourceType {
        SCAN {
            @Override
            Collection<File> getFiles(Env env, XDATUser user, CatFilterWithPath filter) throws Exception {
                final XnatImagesessiondataI x = env.a.retrieve(env, user);
                final List<File> files = new ArrayList<File>();
                final Object scanID = env.attrs.get(URIManager.SCAN_ID);
                for (final XnatImagescandataI scan : x.getScans_scan()){
                    if (null == scanID || scanID.equals(scan.getId())) {
                        final List<XnatResourcecatalogI> resources = scan.getFile();
                        files.addAll(this.findMatchingFile(env, resources, filter));
                    }
                }
                return files;
            }
        },

        ASSESSOR {
            @Override
            Collection<File> getFiles(Env env, XDATUser user, CatFilterWithPath filter) throws Exception {
                XnatImagesessiondataI x = env.a.retrieve(env, user);
                List<XnatImageassessordataI> assessors = x.getAssessors_assessor();
                Collection<File> files = new ArrayList<File>();
                for (XnatImageassessordataI assessor : assessors) {
                    List<XnatResourcecatalogI> resources = assessor.getResources_resource();
                    files.addAll(this.findMatchingFile(env, resources, filter));
                    List<XnatResourcecatalogI> in_resources = assessor.getIn_file();
                    files.addAll(this.findMatchingFile(env, in_resources, filter));
                    List<XnatResourcecatalogI> out_resources = assessor.getOut_file();
                    files.addAll(this.findMatchingFile(env, out_resources, filter));
                }
                return files;
            }
        },

        RECON {
            @Override
            Collection<File> getFiles(Env env, XDATUser user, CatFilterWithPath filter) throws Exception {
                XnatImagesessiondataI x = env.a.retrieve(env,user);
                List<XnatReconstructedimagedataI> recons = x.getReconstructions_reconstructedimage();
                Collection<File> files = new ArrayList<File>();
                for (XnatReconstructedimagedataI recon : recons) {
                    List<XnatResourcecatalogI> in_resources = recon.getIn_file();
                    files.addAll(this.findMatchingFile(env, in_resources, filter));
                    List<XnatResourcecatalogI> out_resources = recon.getOut_file();
                    files.addAll(this.findMatchingFile(env, out_resources, filter));
                }
                return files;
            }
        },

        UNKNOWN {
            @Override
            Collection<File> getFiles(Env env, XDATUser user, CatFilterWithPath filter) {
                return new ArrayList<File>();
            }
        };

        Collection<File> findMatchingFile(Env env, List<XnatResourcecatalogI> resources, CatFilterWithPath filter) {
            Collection<File> files = new ArrayList<File>();
            for (XnatResourcecatalogI resource: resources) {
                String type = resource.getLabel();
                if (type.equals(DicomDump.imageType)){
                    // The "retrieve" method should have populated the global "XnatImagesessiondataI" object.
                    CatCatalogI catalog = env.a.getCatalog(resource);
                    // the getCatalog function updates the global "rootPath" variable so 
                    // it's safe to use it here.
                    filter.setPath(env.a.rootPath);
                    Collection<CatEntryI> matches = CatalogUtils.getEntriesByFilter(catalog, filter);
                    for (CatEntryI match : matches) {
                        files.add(CatalogUtils.getFile(match, env.a.rootPath));
                    }
                }
                else {
                    // ignore non-dicom files
                }
            }
            return files;
        }

        /**
         * Retrieve the DICOM files at this resource level.
         * @param uri
         * @param user
         * @param filter
         * @return
         * @throws ClientException
         * @throws IOException
         * @throws InvalidPermissionException
         * @throws Exception
         */
        abstract Collection<File> getFiles(Env env, 
                XDATUser user, 
                CatFilterWithPath filter) 
                throws ClientException, 
                IOException, 
                InvalidPermissionException, 
                Exception;
    }

    private static ImmutableMap<Integer,Set<String>> getFields(String[] fieldVals) {
        ImmutableMap.Builder<Integer,Set<String>> fieldsb = ImmutableMap.builder();
        for (final String field : fieldVals) {
            final String[] parts = field.split(":");
            final String tag_s = parts[0];
            final Set<String> subs = Sets.newHashSet();
            for (int i = 1; i < parts.length; i++) {
                subs.add(parts[i]);
            }

            int tag;
            try {
                tag = TAG_DICTIONARY.tagForName(tag_s);
            } catch (IllegalArgumentException e) {
                try {
                    tag = Integer.parseInt(tag_s, 16);
                } catch (NumberFormatException e1) {
                    throw new IllegalArgumentException("not a valid DICOM attribute tag: " + tag_s, e1);
                }
            }
            fieldsb.put(tag, subs);
        }
        return fieldsb.build();
    }

    public DicomDump(Context context, Request request, Response response) {
        super(context, request, response);

        if (!this.containsQueryVariable(DicomDump.SRC_ATTR)) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Please set the src parameter");
            env = null;
            return;
        }

        final Map<Integer,Set<String>> fields;
        try {
            fields = getFields(getQueryVariables(FIELD_PARAM));
        } catch (IllegalArgumentException e) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e);
            env = null;
            return;
        }

        this.env = new Env(this.getQueryVariable(DicomDump.SRC_ATTR), fields);   

        getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        getVariants().add(new Variant(MediaType.TEXT_HTML));
        getVariants().add(new Variant(MediaType.TEXT_XML));
    }

    public boolean allowPost() { return false; }
    public boolean allowPut() { return false; }

    /**
     * Enhance {@link CatEntryFilterI} to include a path
     * in it's environment. 
     * 
     * This is required because by the time we get down to 
     * iterating through the catalog entries we've lost
     * access to the absolute path to the resource. 
     * @author aditya
     *
     */
    static abstract class CatFilterWithPath implements CatEntryFilterI {
        String path;
        public void setPath (String path) {this.path = path;}
        public abstract boolean accept(CatEntryI entry);
    }

    public Representation represent(final Variant variant) {
        final MediaType mt = overrideVariant(variant);
        XFTTable t = new XFTTable();
        try {
            String file = this.env.h.retrieve(this.env, this.user);
            DicomHeaderDump d = new DicomHeaderDump(file, env.fields);
            t = d.render();
        }
        catch (FileNotFoundException e){
            this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, e);
            return null;
        }
        catch (IOException e){
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e);
            return null;
        }
        catch (ClientException e) {
            this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e);
            return null;
        }
        catch (Throwable e) {
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e);
            return null;
        }

        return this.representTable(t, mt, new Hashtable<String,Object>());
    }
}
