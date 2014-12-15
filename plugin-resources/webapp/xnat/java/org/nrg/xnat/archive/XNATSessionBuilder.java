/*
 * org.nrg.xnat.archive.XNATSessionBuilder
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/17/13 2:15 PM
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.configuration.ConfigurationException;
import org.nrg.dcm.xnat.DICOMSessionBuilder;
import org.nrg.dcm.xnat.XnatAttrDef;
import org.nrg.dcm.xnat.XnatImagesessiondataBeanFactory;
import org.nrg.ecat.xnat.PETSessionBuilder;
import org.nrg.framework.services.ContextService;
import org.nrg.xdat.XDAT;
import org.nrg.xft.XFT;
import org.nrg.xdat.turbine.utils.PropertiesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class XNATSessionBuilder implements Callable<Boolean> {
	private final Logger logger = LoggerFactory
			.getLogger(XNATSessionBuilder.class);

	// config params for loading injecting a different executor for pooling the
	// session builders.
	private static final String exec_fileName = "session-builder.properties";
	private static final String exec_identifier = "org.nrg.SessionBuilder.executor.impl";

	// config params for session builder specification
	private static final String SEQUENCE = "sequence";
	private static final String CLASS_NAME = "className";
	private static final String[] PROP_OBJECT_FIELDS = new String[] {
			CLASS_NAME, SEQUENCE };
	private static final String PROP_OBJECT_IDENTIFIER = "org.nrg.SessionBuilder.impl";
	private static final String SESSION_BUILDER_PROPERTIES = "session-builder.properties";

	private static final String PROJECT_PARAM = "project";

	private static final String DICOM = "DICOM";
	private static final BuilderConfig DICOM_BUILDER = new BuilderConfig(DICOM,
			DICOMSessionBuilder.class, 0);

	private static final String ECAT = "ECAT";
	private static final BuilderConfig ECAT_BUILDER = new BuilderConfig(ECAT,
			PETSessionBuilder.class, 1);

	private static List<BuilderConfig> builderClasses;

	private static final Class<?>[] PARAMETER_TYPES = new Class[] { File.class,
			Writer.class };

	private static final List<Class<? extends XnatImagesessiondataBeanFactory>> sessionDataFactoryClasses = Lists
			.newArrayList();
	private static ContextService contextService = null;

	private final File dir, xml;
	private final boolean isInPrearchive;
	private final Map<String, String> params;

	static {
		builderClasses = new ArrayList<BuilderConfig>();

		// EXAMPLE PROPERTIES FILE
		// org.nrg.SessionBuilder.impl=NIFTI
		// org.nrg.SessionBuilder.impl.NIFTI.className=org.nrg.builders.CustomNiftiBuilder
		// org.nrg.SessionBuilder.impl.NIFTI.sequence=3
		try {
			final File props = new File(XFT.GetConfDir(),
					SESSION_BUILDER_PROPERTIES);
			final Map<String, Map<String, Object>> confBuilders = PropertiesHelper
					.RetrievePropertyObjects(props, PROP_OBJECT_IDENTIFIER,
							PROP_OBJECT_FIELDS);
			for (final String key : confBuilders.keySet()) {
				final String className = (String) confBuilders.get(key).get(
						CLASS_NAME);
				final String seqS = (String) confBuilders.get(key)
						.get(SEQUENCE);

				if (className != null) {
					try {
						final Class<?> c = Class.forName(className);
						Integer seq = 3;// default
						if (seqS != null) {
							seq = Integer.valueOf(seqS);
						}
						builderClasses.add(new BuilderConfig(key, c, seq));
					} catch (NumberFormatException e) {
						LoggerFactory.getLogger(XNATSessionBuilder.class)
								.error("", e);
					} catch (ClassNotFoundException e) {
						LoggerFactory.getLogger(XNATSessionBuilder.class)
								.error("", e);
					}
				}
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(XNATSessionBuilder.class).error("", e);
		}

		if (!CollectionUtils.exists(builderClasses, new Predicate() {
			public boolean evaluate(Object bc) {
				return ((BuilderConfig) bc).getCode().equals(DICOM);
			}
		})) {
			builderClasses.add(DICOM_BUILDER);
		}

		if (!CollectionUtils.exists(builderClasses, new Predicate() {
			public boolean evaluate(Object bc) {
				return ((BuilderConfig) bc).getCode().equals(ECAT);
			}
		})) {
			builderClasses.add(ECAT_BUILDER);
		}
	}

	/**
	 * @param dir
	 * @param xml
	 * @param isInPrearchive
	 * @param params
	 */
	public XNATSessionBuilder(final File dir, final File xml,
			final boolean isInPrearchive, final Map<String, String> params) {
		if (null == dir || null == xml) {
			throw new NullPointerException();
		}
		this.dir = dir;
		this.xml = xml;
		this.isInPrearchive = isInPrearchive;
		this.params = ImmutableMap.copyOf(params);
	}

	/**
	 * @param dir
	 * @param xml
	 * @param project
	 */
	public XNATSessionBuilder(final File dir, final File xml,
			final String project, final boolean isInPrearchive) {
		this(dir, xml, isInPrearchive, Collections.singletonMap(PROJECT_PARAM,
				project));
	}

	public Boolean execute() {
		final ExecutorService executor = getExecutor();
		try {
			return executor.submit(this).get();
		} catch (InterruptedException e) {
			logger.error("session build interrupted", e);
			return false;
		} catch (ExecutionException e) {
			logger.error("session build failed", e);
			return false;
		}
	}

	/**
	 * Add session data bean factory classes to the chain used to map DICOM SOP
	 * classes to XNAT session types
	 * 
	 * @param classes
	 *            session bean factory classes
	 * @return this
	 */
	public XNATSessionBuilder setSessionDataFactoryClasses(
			final Iterable<Class<? extends XnatImagesessiondataBeanFactory>> classes) {
		sessionDataFactoryClasses.clear();
		Iterables.addAll(sessionDataFactoryClasses, classes);
		return this;
	}

	/**
	 * Iterate over the available Builders to try to generate an xml for the
	 * files in this directory.
	 * 
	 * The iteration will stop once it successfully builds an xml (or runs out
	 * of builder configs).
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public Boolean call() throws IOException {
		xml.getParentFile().mkdirs();
		final FileWriter fw = new FileWriter(xml);

		if (null == contextService && sessionDataFactoryClasses.isEmpty()) {
			contextService = XDAT.getContextService();
			sessionDataFactoryClasses.addAll(contextService.getBean(
					"sessionDataFactoryClasses", Collection.class));
		}

		for (final BuilderConfig bc : builderClasses) {
			if (bc.getCode().equals(DICOM)) {
				// hard coded implementation for DICOM.
				try {
					// Turn the parameters into an array of XnatAttrDef.Constant
					// attribute definitions
					final XnatAttrDef attrdefs[] = Lists
							.newArrayList(
									Iterables.transform(
											params.entrySet(),
											new Function<Map.Entry<String, String>, XnatAttrDef>() {
												public XnatAttrDef apply(
														final Map.Entry<String, String> me) {
													return new XnatAttrDef.Constant(
															me.getKey(), me
																	.getValue());
												}
											})).toArray(new XnatAttrDef[0]);
					final DICOMSessionBuilder builder = new DICOMSessionBuilder(
							dir, fw, attrdefs);
					if (!sessionDataFactoryClasses.isEmpty()) {
						builder.setSessionBeanFactories(sessionDataFactoryClasses);
					}

					if (!isInPrearchive) {
						builder.setIsInPrearchive(isInPrearchive);
					}

					try {
						builder.run();
					} finally {
						builder.close();
					}
				} catch (IOException e) {
					logger.warn("unable to process session directory " + dir, e);
					throw e;
				} catch (SQLException e) {
					logger.error("unable to process session directory " + dir,
							e);
				} catch (Throwable e) {
					logger.error("", e);
				}
			} else if (bc.getCode().equals(ECAT)) {
				// hard coded implementation for ECAT
				PETSessionBuilder builder = new PETSessionBuilder(dir, fw,
						params.get(PROJECT_PARAM));
				logger.debug(
						"assigning session params for ECAT session builder from {}",
						params);

				builder.setSessionLabel(params.get("label"));
				builder.setSubject(params.get("subject_ID"));
				builder.setTimezone(params.get("TIMEZONE"));
				if (!isInPrearchive) {
					builder.setIsInPrearchive(isInPrearchive);
				}

				builder.run();
			} else {
				// this is currently unused... and probably should be
				// re-written. It was a first pass.
				try {
					Constructor con = bc.c.getConstructor(PARAMETER_TYPES);
					try {
						org.nrg.session.SessionBuilder builder = (org.nrg.session.SessionBuilder) con
								.newInstance(new Object[] { dir.getPath(), fw });

						if (!isInPrearchive) {
							builder.setIsInPrearchive(isInPrearchive);
						}

						builder.run();
					} catch (IllegalArgumentException e) {
						logger.error("", e);
					} catch (InstantiationException e) {
						logger.error("", e);
					} catch (IllegalAccessException e) {
						logger.error("", e);
					} catch (InvocationTargetException e) {
						logger.error("", e);
					}

				} catch (SecurityException e) {
					logger.error("", e);
				} catch (NoSuchMethodException e) {
					logger.error("", e);
				}
			}

			if (xml.exists() && xml.length() > 0) {
				break;
			}
		}

		return Boolean.TRUE;
	}

	private static ExecutorService exec = null;

	public static ExecutorService getExecutor() {
		if (exec == null) {
			PropertiesHelper.ImplLoader<ExecutorService> loader = new PropertiesHelper.ImplLoader<ExecutorService>(
					exec_fileName, exec_identifier);
			try {
				exec = loader.buildNoArgs(Executors
						.newFixedThreadPool(PropertiesHelper
								.GetIntegerProperty(exec_fileName,
										exec_identifier + ".size", 2)));
			} catch (IllegalArgumentException e) {
				LoggerFactory.getLogger(XNATSessionBuilder.class).error("", e);
			} catch (SecurityException e) {
				LoggerFactory.getLogger(XNATSessionBuilder.class).error("", e);
			} catch (ConfigurationException e) {
				LoggerFactory.getLogger(XNATSessionBuilder.class).error("", e);
			} catch (InstantiationException e) {
				LoggerFactory.getLogger(XNATSessionBuilder.class).error("", e);
			} catch (IllegalAccessException e) {
				LoggerFactory.getLogger(XNATSessionBuilder.class).error("", e);
			} catch (InvocationTargetException e) {
				LoggerFactory.getLogger(XNATSessionBuilder.class).error("", e);
			} catch (NoSuchMethodException e) {
				LoggerFactory.getLogger(XNATSessionBuilder.class).error("", e);
			}
		}

		return exec;
	}

	private static class BuilderConfig implements Comparable {
		protected String code;
		protected Class c;
		protected Integer order;

		public BuilderConfig(final String code, final Class c,
				final Integer order) {
			if (code == null)
				throw new NullPointerException();
			if (c == null)
				throw new NullPointerException();

			this.code = code;
			this.c = c;
			this.order = (order == null) ? 0 : order;
		}

		public BuilderConfig(final String code, final String c,
				final Integer order) throws ClassNotFoundException {
			this(code, Class.forName(c), order);
		}

		public int compareTo(Object o) {
			return this.getOrder().compareTo(((BuilderConfig) o).getOrder());
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
