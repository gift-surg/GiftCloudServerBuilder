package org.nrg.xnat.restlet.actions.importer;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import org.nrg.xnat.restlet.actions.SessionImporter;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.turbine.utils.PropertiesHelper;

public abstract class ImporterHandlerA  extends StatusProducer implements Callable<List<String>>{


	public ImporterHandlerA(final Object listenerControl, final XDATUser u,  final FileWriterWrapperI fw, final Map<String,Object> params) {
		super((listenerControl==null)?u:listenerControl);
	}
	
	public abstract List<String> call() throws ClientException, ServerException;
	
	static Logger logger = Logger.getLogger(ImporterHandlerA.class);

	public static final String IMPORT_HANDLER_ATTR = "import-handler";
	
	static String SESSION_IMPORTER="SI";
	static String DEFAULT_HANDLER=SESSION_IMPORTER;
	final static Map<String,Class<? extends ImporterHandlerA>> IMPORTERS=new HashMap<String,Class<? extends ImporterHandlerA>>();

	private static final String PROP_OBJECT_IDENTIFIER = "org.nrg.import.handler.impl";
	private static final String IMPORTER_PROPERTIES = "importer.properties";
	private static final String ORG_NRG_IMPORTER_DEFAULT = "org.nrg.import.handler.default";
	private static final String CLASS_NAME = "className";
	private static final String[] PROP_OBJECT_FIELDS = new String[]{CLASS_NAME};
	static{
		//EXAMPLE PROPERTIES FILE 
		//org.nrg.import.handler=NIFTI
		//org.nrg.import.handler.impl.NIFTI.className=org.nrg.import.handler.CustomNiftiImporter
		try {
			final File props=new File(XFT.GetConfDir(),IMPORTER_PROPERTIES);
			final Map<String,Map<String,Object>> confBuilders=PropertiesHelper.RetrievePropertyObjects(props, PROP_OBJECT_IDENTIFIER, PROP_OBJECT_FIELDS);
			for(final String key:confBuilders.keySet()){
				final String className=(String)confBuilders.get(key).get(CLASS_NAME);
				final String seqS=(String)confBuilders.get(key).get(CLASS_NAME);
				
				if(className!=null){
					try {
						final Class c=Class.forName(className);
						IMPORTERS.put(key,c);
					} catch (NumberFormatException e) {
						logger.error("",e);
					} catch (ClassNotFoundException e) {
						logger.error("",e);
					}
				}
			}
			
			if(!IMPORTERS.containsKey(SESSION_IMPORTER))IMPORTERS.put(SESSION_IMPORTER, SessionImporter.class);
			
			String newDefault=PropertiesHelper.GetProperty(props, ORG_NRG_IMPORTER_DEFAULT);
			if(!StringUtils.isEmpty(newDefault)&& IMPORTERS.containsKey(newDefault)){
				DEFAULT_HANDLER=newDefault;
			}
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
		return (ImporterHandlerA)con.newInstance(uID, u, fi, params);
	
	}
	
	/**
	 * This method was added to allow other developers to manually add importers to the list, without adding a configuration file.  However, this would some how need to be done before the import is executed (maybe as a servlet?).
	 * @return
	 */
	public static Map<String,Class<? extends ImporterHandlerA>> getImporters(){
		return IMPORTERS;
	}
}
