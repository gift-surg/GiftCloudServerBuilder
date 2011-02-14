package org.nrg.schedule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.nrg.xft.XFT;
import org.nrg.xnat.helpers.prearchive.PrearcScheduler;
import org.nrg.xnat.turbine.utils.PropertiesHelper;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tim Olsen
 *
 * This Quartz integration is not a long term solution.  We should be using Spring to configure this.  But, we don't have time to implement that for XNAT 1.5 and need a scheduler.
 */
public abstract class JobBuilderA {
	private final static Logger logger = LoggerFactory.getLogger(JobBuilderA.class);
	private static final String CLASS_NAME = "className";
	private static final String QUARTZ_BUILDER_IDENTIFIER = "org.nrg.scheduler.impl";
	private static final String[] QUARTZ_BUILDER_PROP_OBJECT_FIELDS = new String[]{CLASS_NAME};
	private static final Class[] QUARTZ_BUILDER_PARAMETER_TYPES=new Class[]{};
	private static final String XNAT_QUARTZ_PROPERTIES = "xnat-quartz.properties";
		
	public static final String PREARC_MONITOR = "PREARC_BUILDER";
	
	protected abstract List<XJob> createJobs();
	
	 
	//EXAMPLE PROPERTIES FILE conf/xnat-quartz.properties
	//# add additional JobBuilders using this
	//org.nrg.scheduler.impl=STANDARD
	//org.nrg.scheduler.impl.STANDARD.className=org.some.path.CustomJobBuilder1
	//
	//org.nrg.scheduler.impl=CUSTOM1
	//org.nrg.scheduler.impl.CUSTOM1.className=org.some.path.CustomJobBuilder2
	
	private static Collection<JobBuilderA> getBuilders(){
		Map<String,JobBuilderA> configd=new HashMap<String,JobBuilderA>();
		try {
			
			Configuration conf=PropertiesHelper.RetrieveConfiguration(new File(XFT.GetConfDir(),XNAT_QUARTZ_PROPERTIES));
			if(conf!=null){
				configd.putAll((new PropertiesHelper<JobBuilderA>()).buildObjectsFromProps(conf, QUARTZ_BUILDER_IDENTIFIER, QUARTZ_BUILDER_PROP_OBJECT_FIELDS, CLASS_NAME, QUARTZ_BUILDER_PARAMETER_TYPES, new Object[]{}));
			}
		} catch (ConfigurationException e) {
			logger.error("",e);
		}
		
		if(!configd.containsKey(PREARC_MONITOR)){
			configd.put(PREARC_MONITOR, new PrearcScheduler());
		}
		
		return configd.values();
	}
	
	public static List<XJob> getJobs(){
		List<XJob> jobs=new ArrayList<XJob>();
		for(JobBuilderA jb:getBuilders()){
			jobs.addAll(jb.createJobs());
		}
		
		return jobs;
	}
	
	public static class XJob{
		public JobDetail jobDetail=null;
		public Trigger trigger=null;
		public XJob(JobDetail jd,Trigger trigger){
			this.jobDetail=jd;
			this.trigger=trigger;
		}
		
		public XJob(Trigger trigger){
			this.trigger=trigger;
		}
	}	
}
