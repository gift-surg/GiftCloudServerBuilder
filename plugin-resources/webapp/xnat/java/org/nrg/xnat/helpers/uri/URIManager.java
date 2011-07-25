package org.nrg.xnat.helpers.uri;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.restlet.services.MoveFiles;
import org.restlet.util.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class URIManager {
	private final static Logger logger = LoggerFactory.getLogger(URIManager.class);
	public static enum TEMPLATE_TYPE {ARC,PREARC,CACHE};
	
	public static final String XNAME = "XNAME";
	public static final String FILE = "FILE";
	public static final String RECON_ID = "RECON_ID";
	public static final String SCAN_ID = "SCAN_ID";
	public static final String ASSESSED_ID = "ASSESSED_ID";
	public static final String SUBJECT_ID = "SUBJECT_ID";
	public static final String EXPT_ID = "EXPT_ID";
	public static final String EXPT_LABEL = "EXPT_LABEL";
	public static final String PROJECT_ID = "PROJECT_ID";

	public static final String RES_NAME = "RES_NAME";
	
	private static URIManager manager=null;
	
	public synchronized static URIManager getInstance(){
		if(manager==null){
			manager=new URIManager();
		}
		
		return manager;
	}
	
	private URIManager(){
		add(TEMPLATE_TYPE.CACHE,"/user/cache/resources/{" + XNAME + "}/files",Template.MODE_STARTS_WITH,URIManager.UserCacheURI.class);
		
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/projects/{" + URIManager.PROJECT_ID + "}/experiments/{" + URIManager.EXPT_ID + "}".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/projects/{" + URIManager.PROJECT_ID + "}/subjects/{" + URIManager.SUBJECT_ID + "}".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/projects/{" + URIManager.PROJECT_ID + "}/subjects/{" + URIManager.SUBJECT_ID + "}/experiments/{" + URIManager.EXPT_ID + "}".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/projects/{" + URIManager.PROJECT_ID + "}/subjects/{" + URIManager.SUBJECT_ID + "}/experiments/{" + URIManager.ASSESSED_ID + "}/assessors/{" + URIManager.EXPT_ID + "}".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/projects/{" + URIManager.PROJECT_ID + "}/subjects/{" + URIManager.SUBJECT_ID + "}/experiments/{" + URIManager.ASSESSED_ID + "}/scans/{" + URIManager.SCAN_ID + "}".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/projects/{" + URIManager.PROJECT_ID + "}/subjects/{" + URIManager.SUBJECT_ID + "}/experiments/{" + URIManager.ASSESSED_ID + "}/reconstructions/{" + URIManager.RECON_ID + "}".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/projects/{" + URIManager.PROJECT_ID + "}".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/experiments/{" + URIManager.EXPT_ID + "}".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/experiments/{" + URIManager.ASSESSED_ID + "}/scans/{" + URIManager.SCAN_ID + "}".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/experiments/{" + URIManager.ASSESSED_ID + "}/reconstructions/{" + URIManager.RECON_ID + "}".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/experiments/{" + URIManager.ASSESSED_ID + "}/assessors/{" + URIManager.EXPT_ID + "}".intern(),URIManager.ArchiveURI.class);
		addArcWithFiles(TEMPLATE_TYPE.ARC,"/archive/subjects/{SUBJECT_ID}",URIManager.ArchiveURI.class);
		
		add(TEMPLATE_TYPE.PREARC,"/prearchive/projects/{" + URIManager.PROJECT_ID + "}/{" +PrearcUtils.PREARC_TIMESTAMP + "}/{" + PrearcUtils.PREARC_SESSION_FOLDER + "}".intern(),Template.MODE_EQUALS,URIManager.PrearchiveURI.class);
		add(TEMPLATE_TYPE.PREARC,"/prearchive/projects/{" + URIManager.PROJECT_ID + "}/{" + PrearcUtils.PREARC_TIMESTAMP + "}".intern(),Template.MODE_EQUALS,URIManager.PrearchiveURI.class);
		add(TEMPLATE_TYPE.PREARC,"/prearchive/projects/{" + URIManager.PROJECT_ID + "}".intern(),Template.MODE_EQUALS,URIManager.PrearchiveURI.class);
		add(TEMPLATE_TYPE.PREARC,"/prearchive".intern(),Template.MODE_EQUALS,URIManager.PrearchiveURI.class);
	}
	
	public static Collection<TemplateInfo> getTemplates(TEMPLATE_TYPE type){
		return getInstance().TEMPLATES.get(type);
	}
	
	public static class TemplateInfo<A extends DataURIA>{
		public final String key;
		public final int MODE;
		public final Class<A> clazz;
		
		public TemplateInfo(final String key, final Integer i, final Class<A> clazz){
			this.key=key;
			this.MODE=i;
			this.clazz=clazz;
		}
		
		private final static Class[] CONS=new Class[]{Map.class,String.class};
		
		public A wrap(Map<String,Object> props, final String uri){
			try {
				return clazz.getConstructor(CONS).newInstance(new Object[]{props,uri});
			} catch (Exception e) {
				logger.error("",e);
				return null;
			}
			
		}
	}
	
	public static abstract class DataURIA{
		final Map<String,Object> props;
		final String uri;
		
		public DataURIA(final Map<String,Object> props, final String uri){
			this.props=props;
			this.uri=uri.intern();
		}
		
		public Map<String,Object> getProps(){
			return props;
		}
		
		public String getUri(){
			return uri;
		}
	}

	public static class UserCacheURI extends DataURIA{
	
		public UserCacheURI(Map<String, Object> props, String uri) {
			super(props,uri);
		}
	}

	public static class ArchiveURI extends DataURIA{
	
		public ArchiveURI(Map<String, Object> props, String uri) {
			super(props,uri);
		}
	}

	public static class PrearchiveURI extends DataURIA{
	
		public PrearchiveURI(Map<String, Object> props, String uri) {
			super(props,uri);
		}
	}
	
	final Multimap<TEMPLATE_TYPE, TemplateInfo> TEMPLATES=ArrayListMultimap.create();
	
	private void add(final TEMPLATE_TYPE type,final String template,final int MODE,final Class<? extends URIManager.DataURIA> clazz){
		TEMPLATES.put(type,new TemplateInfo(template.intern(),MODE,clazz));		
	}
	
	private void addArcWithFiles(final TEMPLATE_TYPE type,final String template,final Class<? extends URIManager.DataURIA> clazz){
		TEMPLATES.put(type,new TemplateInfo(template.intern(),Template.MODE_EQUALS,clazz));		

		//TEMPLATES.put(type,new TemplateInfo((template+"/resources/{" + XNAME + "}").intern(),Template.MODE_EQUALS,clazz));	
		TEMPLATES.put(type,new TemplateInfo((template+"/resources/{" + XNAME + "}/files/").intern(),Template.MODE_STARTS_WITH,clazz));	
	}
	
}
