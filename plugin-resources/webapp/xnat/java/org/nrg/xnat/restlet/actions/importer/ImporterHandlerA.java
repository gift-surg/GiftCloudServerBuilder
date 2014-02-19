/*
 * org.nrg.xnat.restlet.actions.importer.ImporterHandlerA
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.restlet.actions.importer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.dcm.DicomFileNamer;
import org.nrg.framework.services.ContextService;
import org.nrg.status.StatusProducer;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.DicomObjectIdentifier;
import org.nrg.xnat.archive.DicomZipImporter;
import org.nrg.xnat.archive.GradualDicomImporter;
import org.nrg.xnat.restlet.actions.PrearcBlankSession;
import org.nrg.xnat.restlet.actions.SessionImporter;
import org.nrg.xnat.restlet.actions.XarImporter;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.turbine.utils.PropertiesHelper;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class ImporterHandlerA  extends StatusProducer implements Callable<List<String>>{


    public ImporterHandlerA(final Object listenerControl, final XDATUser u,  final FileWriterWrapperI fw, final Map<String,Object> params) {
        super((listenerControl==null)?u:listenerControl);
    }

    public abstract List<String> call() throws ClientException, ServerException;

    static Logger logger = Logger.getLogger(ImporterHandlerA.class);

    public static final String IMPORT_HANDLER_ATTR = "import-handler";

    public static String SESSION_IMPORTER="SI";
    public static String XAR_IMPORTER="XAR";
    public static String GRADUAL_DICOM_IMPORTER="gradual-DICOM";
    public static String DICOM_ZIP_IMPORTER="DICOM-zip";
    public static String BLANK_PREARCHIVE_ENTRY="blank";

    static String DEFAULT_HANDLER=SESSION_IMPORTER;
    final static Map<String,Class<? extends ImporterHandlerA>> IMPORTERS=new HashMap<String,Class<? extends ImporterHandlerA>>();

    private static final String PROP_OBJECT_IDENTIFIER = "org.nrg.import.handler.impl";
    private static final String IMPORTER_PROPERTIES = "importer.properties";
    private static final String CLASS_NAME = "className";
    private static final String[] PROP_OBJECT_FIELDS = new String[]{CLASS_NAME};
    static{
        //EXAMPLE PROPERTIES FILE 
        //org.nrg.import.handler=NIFTI
        //org.nrg.import.handler.impl.NIFTI.className=org.nrg.import.handler.CustomNiftiImporter
        try {
            IMPORTERS.putAll((new PropertiesHelper<ImporterHandlerA>()).buildClassesFromProps(IMPORTER_PROPERTIES, PROP_OBJECT_IDENTIFIER, PROP_OBJECT_FIELDS, CLASS_NAME));

            if(!IMPORTERS.containsKey(SESSION_IMPORTER))IMPORTERS.put(SESSION_IMPORTER, SessionImporter.class);
            if(!IMPORTERS.containsKey(XAR_IMPORTER))IMPORTERS.put(XAR_IMPORTER, XarImporter.class);
            if(!IMPORTERS.containsKey(GRADUAL_DICOM_IMPORTER))IMPORTERS.put(GRADUAL_DICOM_IMPORTER, GradualDicomImporter.class);
            if(!IMPORTERS.containsKey(DICOM_ZIP_IMPORTER))IMPORTERS.put(DICOM_ZIP_IMPORTER, DicomZipImporter.class);
	    if(!IMPORTERS.containsKey(BLANK_PREARCHIVE_ENTRY))IMPORTERS.put(BLANK_PREARCHIVE_ENTRY, PrearcBlankSession.class);
        } catch (Exception e) {
            logger.error("",e);
        }
    }

    public static ImporterHandlerA buildImporter(String format,final Object uID, final XDATUser u, final FileWriterWrapperI fi, Map<String,Object> params) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException, ImporterNotFoundException {
        if(StringUtils.isEmpty(format)){
            format=DEFAULT_HANDLER;
        }

        Class<? extends ImporterHandlerA> importerImpl=IMPORTERS.get(format);
        if(importerImpl==null){

            throw new ImporterNotFoundException("Unknown importer implementation specified: " + format,new IllegalArgumentException());
        }

        final Constructor con=importerImpl.getConstructor(Object.class, XDATUser.class, FileWriterWrapperI.class, Map.class);
        final ImporterHandlerA handler = (ImporterHandlerA)con.newInstance(uID, u, fi, params);

        /* Abuse Spring to inject some additional parameters. Please fix this. */
        if (GradualDicomImporter.class.equals(importerImpl)) {
            final ContextService context = XDAT.getContextService();
            final GradualDicomImporter gdi = (GradualDicomImporter)handler;
            gdi.setIdentifier(context.getBean("dicomObjectIdentifier", DicomObjectIdentifier.class));
            try {
                final DicomFileNamer namer = context.getBean("dicomFileNamer", DicomFileNamer.class);
                if (null != namer) {
                    gdi.setNamer(namer);
                }
            } catch (NoSuchBeanDefinitionException e) {
                logger.debug("no DicomFileNamer specified", e);
            }
        } else if (DicomZipImporter.class.equals(importerImpl)) {
            final ContextService context = XDAT.getContextService();
            final DicomZipImporter dzi = (DicomZipImporter)handler;
            dzi.setIdentifier(context.getBean("dicomObjectIdentifier", DicomObjectIdentifier.class));
            try {
                final DicomFileNamer namer = context.getBean("dicomFileNamer", DicomFileNamer.class);
                if (null != namer) {
                    dzi.setNamer(namer);
                }
            } catch (NoSuchBeanDefinitionException e) {
                logger.debug("no DicomFileNamer specified", e);
            }
        }

        /* Abuse Spring to inject some additional parameters. Please fix this. */
        if (GradualDicomImporter.class.equals(importerImpl)) {
            final ContextService context = XDAT.getContextService();
            final GradualDicomImporter gdi = (GradualDicomImporter)handler;
            gdi.setIdentifier(context.getBean("dicomObjectIdentifier", DicomObjectIdentifier.class));
            try {
                final DicomFileNamer namer = context.getBean("dicomFileNamer", DicomFileNamer.class);
                if (null != namer) {
                    gdi.setNamer(namer);
                }
            } catch (NoSuchBeanDefinitionException e) {
                logger.debug("no DicomFileNamer specified", e);
            }
        } else if (DicomZipImporter.class.equals(importerImpl)) {
            final ContextService context = XDAT.getContextService();
            final DicomZipImporter dzi = (DicomZipImporter)handler;
            dzi.setIdentifier(context.getBean("dicomObjectIdentifier", DicomObjectIdentifier.class));
            try {
                final DicomFileNamer namer = context.getBean("dicomFileNamer", DicomFileNamer.class);
                if (null != namer) {
                    dzi.setNamer(namer);
                }
            } catch (NoSuchBeanDefinitionException e) {
                logger.debug("no DicomFileNamer specified", e);
            }
        }

        return handler;

    }

    /**
     * This method was added to allow other developers to manually add importers to the list, without adding a configuration file.  However, this would some how need to be done before the import is executed (maybe as a servlet?).
     * @return
     */
    public static Map<String,Class<? extends ImporterHandlerA>> getImporters(){
        return IMPORTERS;
    }
}
