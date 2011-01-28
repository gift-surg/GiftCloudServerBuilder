package org.nrg.xnat.restlet.actions;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.status.StatusProducer;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFT;
import org.nrg.xnat.helpers.PrearcImporterHelper;
import org.nrg.xnat.restlet.actions.PrearcImporterA.PrearcSession;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.turbine.utils.PropertiesHelper;

/**
 * @author tolsen01
 *
 *	Developers can add their own implementations for the PrearcImporterA.  This can also be done using the PrearcImporterFactory, though I think this might be easier.
 *
 *  Implementations should return a list of URIs that will identify the resources created. (/prearchive/projects/X/timestamp/session)
 *
 *	Developers should add a conf file to their project called prearc-importer.properties with the structure defined below.  The name given in the configuration file, can then be passed to the Importer action, and will be used to get the proper prearc importer.
 */
public abstract class PrearcImporterA extends StatusProducer implements Callable<Iterable<PrearcSession>>{
	@SuppressWarnings("serial")
	public static class UnknownPrearcImporterException extends Exception {

		public UnknownPrearcImporterException(String string,
				IllegalArgumentException illegalArgumentException) {
			super(string,illegalArgumentException);
		}

	}

	static Logger logger = Logger.getLogger(PrearcImporterA.class);

	public static final String ECAT = "ECAT";
	public static final String DICOM = "DICOM";
	
	public static final String PREARC_IMPORTER_ATTR= "prearc-importer";

	static String DEFAULT_HANDLER=DICOM;
	final static Map<String,Class<? extends PrearcImporterA>> PREARC_IMPORTERS=new HashMap<String,Class<? extends PrearcImporterA>>();

	private static final String PROP_OBJECT_IDENTIFIER = "org.nrg.PrearcImporter.impl";
	private static final String SESSION_BUILDER_PROPERTIES = "prearc-importer.properties";
	private static final String CLASS_NAME = "className";
	private static final String[] PROP_OBJECT_FIELDS = new String[]{CLASS_NAME};
	static{
		//EXAMPLE PROPERTIES FILE 
		//org.nrg.PrearcImporter=NIFTI
		//org.nrg.PrearcImporter.impl.NIFTI.className=org.nrg.prearc.importers.CustomNiftiImporter
		try {
			final File props=new File(XFT.GetConfDir(),SESSION_BUILDER_PROPERTIES);
			final Map<String,Map<String,Object>> confBuilders=PropertiesHelper.RetrievePropertyObjects(props, PROP_OBJECT_IDENTIFIER, PROP_OBJECT_FIELDS);
			for(final String key:confBuilders.keySet()){
				final String className=(String)confBuilders.get(key).get(CLASS_NAME);
				
				if(className!=null){
					try {
						final Class<? extends PrearcImporterA> c=(Class<? extends PrearcImporterA>)Class.forName(className);
						PREARC_IMPORTERS.put(key,c);
					} catch (NumberFormatException e) {
						logger.error("",e);
					} catch (ClassNotFoundException e) {
						logger.error("",e);
					}
				}
			}
			
			if(!PREARC_IMPORTERS.containsKey(DICOM))PREARC_IMPORTERS.put(DICOM, PrearcImporterHelper.class);
			if(!PREARC_IMPORTERS.containsKey(ECAT))PREARC_IMPORTERS.put(ECAT, PrearcImporterHelper.class);
			
			String newDefault=PropertiesHelper.GetProperty(props, "org.nrg.PrearcImporter.default");
			if(!StringUtils.isEmpty(newDefault)&& PREARC_IMPORTERS.containsKey(newDefault)){
				DEFAULT_HANDLER=newDefault;
			}
		} catch (Exception e) {
			logger.error("",e);
		}
	}
	
	public static <A extends PrearcImporterA> A buildImporter(String format,Object uID, final XDATUser u, final FileWriterWrapperI fi, Map<String,Object> params,boolean overwrite, boolean allowDataDeletion) throws ClientException, ServerException, SecurityException, NoSuchMethodException, UnknownPrearcImporterException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException{
		if(StringUtils.isEmpty(format)){
			format=DEFAULT_HANDLER;
		}
		
		Class<A> importerImpl=(Class<A>)PREARC_IMPORTERS.get(format);
		if(importerImpl==null){
			throw new UnknownPrearcImporterException("Unknown prearc-importer implementation specified: " + format,new IllegalArgumentException());
		}
		
		final Constructor<A> con=importerImpl.getConstructor(Object.class, XDATUser.class, FileWriterWrapperI.class, Map.class, boolean.class, boolean.class);
		return con.newInstance(uID, u, fi, params,overwrite,allowDataDeletion);
		
	}
	
	/**
	 * This method was added to allow other developers to manually add importers to the list, without adding a configuration file.  However, this would some how need to be done before the import is executed (maybe as a servlet?).
	 * @return
	 */
	public static Map<String,Class<? extends PrearcImporterA>> getPrearcImporters(){
		return PREARC_IMPORTERS;
	}
	
	
	public PrearcImporterA(Object control, final XDATUser u, final FileWriterWrapperI fi, Map<String,Object> params, boolean overwrite, boolean allowDataDeletion) {
		super(control);
	}
	
	public abstract List<PrearcSession> call() throws ClientException, ServerException;
	
	public static class PrearcSession{
		private final File sessionDIR;
		private final File sessionXML;
		private final URI url;
		
		public PrearcSession(final File dir, final File xml, final URI u){
			sessionDIR=dir;
			sessionXML=xml;
			url=u;
		}

		public File getSessionDIR() {
			return sessionDIR;
		}

		public File getSessionXML() {
			return sessionXML;
		}

		public URI getUrl() {
			return url;
		}
	}
}