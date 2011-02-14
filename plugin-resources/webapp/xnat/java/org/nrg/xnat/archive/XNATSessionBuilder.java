/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.archive;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;
import org.nrg.dcm.xnat.DICOMSessionBuilder;
import org.nrg.dcm.xnat.XnatAttrDef;
import org.nrg.ecat.xnat.PETSessionBuilder;
import org.nrg.xft.XFT;
import org.nrg.xnat.turbine.utils.PropertiesHelper;



/**
 * @author Timothy R. Olsen <olsent@wustl.edu>
 *
 * Helper class to execute all of the available SessionBuilders. 
 * Initially this only supports DICOM and ECAT.  But, it is a step towards allowing other implementations.
 */
@SuppressWarnings("rawtypes")
public class XNATSessionBuilder implements Callable<Boolean>{
    private static final String SEQUENCE = "sequence";

	private static final String CLASS_NAME = "className";

	private static final String[] PROP_OBJECT_FIELDS = new String[]{CLASS_NAME,SEQUENCE};

	private static final String PROP_OBJECT_IDENTIFIER = "org.nrg.SessionBuilder.impl";

	private static final String SESSION_BUILDER_PROPERTIES = "session-builder.properties";

	static Logger logger = Logger.getLogger(XNATSessionBuilder.class);
    
	private static final String DICOM = "DICOM";

	private static final BuilderConfig DICOM_BUILDER = new BuilderConfig(DICOM,DICOMSessionBuilder.class,0);
	private static final String ECAT = "ECAT";

	private static final BuilderConfig ECAT_BUILDER = new BuilderConfig(ECAT,PETSessionBuilder.class,1);

	private static List<BuilderConfig> builderClasses;
	
	private static final Class[] PARAMETER_TYPES=new Class[]{File.class,Writer.class};
	
	private final File dir;
	private final File xml;
	private final String project;
	
	static{
		builderClasses=new ArrayList<BuilderConfig>();
		
		//EXAMPLE PROPERTIES FILE 
		//org.nrg.SessionBuilder.impl=NIFTI
		//org.nrg.SessionBuilder.impl.NIFTI.className=org.nrg.builders.CustomNiftiBuilder
		//org.nrg.SessionBuilder.impl.NIFTI.sequence=3
		try {
			final File props=new File(XFT.GetConfDir(),SESSION_BUILDER_PROPERTIES);
			final Map<String,Map<String,Object>> confBuilders=PropertiesHelper.RetrievePropertyObjects(props, PROP_OBJECT_IDENTIFIER, PROP_OBJECT_FIELDS);
			for(final String key:confBuilders.keySet()){
				final String className=(String)confBuilders.get(key).get(CLASS_NAME);
				final String seqS=(String)confBuilders.get(key).get(SEQUENCE);
				
				if(className!=null){
					try {
						final Class c=Class.forName(className);
						Integer seq=3;//default
						if(seqS!=null){
							seq=Integer.valueOf(seqS);
						}
						builderClasses.add(new BuilderConfig(key,c,seq));
					} catch (NumberFormatException e) {
						logger.error("",e);
					} catch (ClassNotFoundException e) {
						logger.error("",e);
					}
				}
			}
		} catch (Exception e) {
			logger.error("",e);
		}
		
		if(!CollectionUtils.exists(builderClasses, new Predicate(){
			public boolean evaluate(Object bc) {
				return ((BuilderConfig)bc).getCode().equals(DICOM);
			}
		})){
			builderClasses.add(DICOM_BUILDER);
		}
		
		if(!CollectionUtils.exists(builderClasses, new Predicate(){
			public boolean evaluate(Object bc) {
				return ((BuilderConfig)bc).getCode().equals(ECAT);
			}
		})){
			builderClasses.add(ECAT_BUILDER);
		}
	}
	
	/**
	 * @param dir
	 * @param xml
	 * @param project
	 */
	public XNATSessionBuilder(final File dir, final File xml, final String project){
		if(dir==null)throw new NullPointerException();
		if(xml==null)throw new NullPointerException();
		
		this.dir=dir;
		this.xml=xml;
		this.project=project;
	}
	
	/**
	 * Iterate over the available Builders to try to generate an xml for the files in this directory.
	 * 
	 * The iteration will stop once it successfully builds an xml (or runs out of builder configs).
	 * @throws IOException
	 */
	public Boolean call() throws IOException {
		xml.getParentFile().mkdirs();
		final FileWriter fw = new FileWriter(xml);
		
		for(final BuilderConfig bc: builderClasses){
			if(bc.getCode().equals(DICOM)){
				//hard coded implementation for DICOM. 
				try {
					final DICOMSessionBuilder builder = new DICOMSessionBuilder(dir,
							fw,
							new XnatAttrDef.Constant("project", project));
					try {
						builder.run();
					} finally {
						builder.close();
					}
				} catch (IOException e) {
					logger.warn("unable to process session directory " + dir, e);
					throw e;
				} catch (SQLException e) {
					logger.error("unable to process session directory " + dir, e);
				} catch (Throwable e) {
					logger.error("",e);
				}
			}else if(bc.getCode().equals(ECAT)){
				//hard coded implementation for ECAT
				new PETSessionBuilder(dir,fw,project).run();
			}else{
				//this is currently unused... and probably should be re-written.  It was a first pass.
				try {
					Constructor con=bc.c.getConstructor(PARAMETER_TYPES);
					try {
						org.nrg.session.SessionBuilder builder=(org.nrg.session.SessionBuilder) con.newInstance(new Object[]{dir.getPath(),fw});
						builder.run();
					} catch (IllegalArgumentException e) {
						logger.error("",e);
					} catch (InstantiationException e) {
						logger.error("",e);
					} catch (IllegalAccessException e) {
						logger.error("",e);
					} catch (InvocationTargetException e) {
						logger.error("",e);
					}
					
				} catch (SecurityException e) {
					logger.error("",e);
				} catch (NoSuchMethodException e) {
					logger.error("",e);
				}
			}
			
			if (xml.exists() && xml.length()>0) {
				break;
			}
		}
		
		return Boolean.TRUE;
	}
		
	
	private static class BuilderConfig implements Comparable{
		protected String code;
		protected Class c;
		protected Integer order;
		
		public BuilderConfig(final String code, final Class c,final Integer order){
			if(code==null)throw new NullPointerException();
			if(c==null)throw new NullPointerException();
			
			this.code=code;
			this.c=c;
			this.order=(order==null)?0:order;
		}

		public BuilderConfig(final String code, final String c,final Integer order) throws ClassNotFoundException{
			this(code,Class.forName(c),order);
		}

		public int compareTo(Object o) {
			return this.getOrder().compareTo(((BuilderConfig)o).getOrder());
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public Class getC() {
			return c;
		}

		public void setC(Class c) {
			this.c = c;
		}

		public Integer getOrder() {
			return order;
		}

		public void setOrder(Integer order) {
			this.order = order;
		}
	}
}
