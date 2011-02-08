package org.nrg.xnat.helpers.uri;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.restlet.util.Template;
import org.restlet.util.Variable;

public final class UriParserUtils {
		
	private static final String RECON_ID = "RECON_ID";

	private static final String SCAN_ID = "SCAN_ID";

	public static final String ASSESSED_ID = "ASSESSED_ID";

	public static final String SUBJECT_ID = "SUBJECT_ID";

	public static final String EXPT_ID = "EXPT_ID";

	public static final String EXPT_LABEL = "EXPT_LABEL";

	public static final String PROJECT_ID = "PROJECT_ID";
	
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
		
	
	@SuppressWarnings("serial")
	static List<String> arcTemplates=new ArrayList<String>(){{
		add("/archive/projects/{" + PROJECT_ID + "}/experiments/{" + EXPT_ID + "}".intern());
		add("/archive/projects/{" + PROJECT_ID + "}/subjects/{" + SUBJECT_ID + "}".intern());
		add("/archive/projects/{" + PROJECT_ID + "}/subjects/{" + SUBJECT_ID + "}/experiments/{" + EXPT_ID + "}".intern());
		add("/archive/projects/{" + PROJECT_ID + "}/subjects/{" + SUBJECT_ID + "}/experiments/{" + ASSESSED_ID + "}/assessors/{" + EXPT_ID + "}".intern());
		add("/archive/projects/{" + PROJECT_ID + "}/subjects/{" + SUBJECT_ID + "}/experiments/{" + ASSESSED_ID + "}/scans/{" + SCAN_ID + "}".intern());
		add("/archive/projects/{" + PROJECT_ID + "}/subjects/{" + SUBJECT_ID + "}/experiments/{" + ASSESSED_ID + "}/reconstructions/{" + RECON_ID + "}".intern());
		add("/archive/projects/{" + PROJECT_ID + "}".intern());
		add("/archive".intern());

		add("/archive/experiments/{" + EXPT_ID + "}".intern());
		add("/archive/experiments/{" + ASSESSED_ID + "}/scans/{" + SCAN_ID + "}".intern());
		add("/archive/experiments/{" + ASSESSED_ID + "}/reconstructions/{" + RECON_ID + "}".intern());
		add("/archive/experiments/{" + ASSESSED_ID + "}/assessors/{" + EXPT_ID + "}".intern());

		add("/archive/subjects/{SUBJECT_ID}".intern());
	}};
	
	@SuppressWarnings("serial")
	static List<String> prearcTemplates=new ArrayList<String>(){
		{
			add("/prearchive/projects/{" + PROJECT_ID + "}/{" +PrearcUtils.PREARC_TIMESTAMP + "}/{" + PrearcUtils.PREARC_SESSION_FOLDER + "}".intern());
			add("/prearchive/projects/{" + PROJECT_ID + "}/{" + PrearcUtils.PREARC_TIMESTAMP + "}".intern());
			add("/prearchive/projects/{" + PROJECT_ID + "}".intern());
			add("/prearchive".intern());
		}};
	
	public static DataURIA parseURI(String s) throws MalformedURLException{
		if(s.startsWith("/prearchive")){
			if(s.equals("/prearchive")){
				final Map<String,Object> t=Collections.emptyMap();
				return new PrearchiveURI(t,s);
			}
			
			for(String template: prearcTemplates){
				Map<String,Object> map=new UriParser(template,Template.MODE_EQUALS).readUri(s);
				if(map.size()>0){
					return new PrearchiveURI(map,s);
				}
			}
		}else if(s.startsWith("/archive")){
			if(s.equals("/archive")){
				final Map<String,Object> t=Collections.emptyMap();
				return new ArchiveURI(t,s);
			}
			
			for(String template: arcTemplates){
				Map<String,Object> map=new UriParser(template,Template.MODE_EQUALS).readUri(s);
				if(map.size()>0){
					return new ArchiveURI(map,s);
				}
			}
		}
			
		throw new MalformedURLException();
	}

	/**
	 * A base parser that reads a uri using the given template.
	 * @author aditya
	 *
	 */
	public static class UriParser implements UriParserI<Map <String, Object>> {
		String template;
		int mode=Template.MODE_STARTS_WITH;
		UriParser (String template) {
			this.template = template;
		}
		public UriParser (String template, int mode) {
			this.template = template;
			this.mode=mode;
		}
		/**
		 * Parse the uri with the given template. No errors are thrown
		 * for fields, instead they are set to null. Users of this object beware.
		 */
		public Map<String,Object> readUri (String uri) {
			Template t = new Template(template, mode, Variable.TYPE_URI_SEGMENT, "", true, false);
			Map<String,Object> so = new HashMap<String,Object>();
			t.parse(uri,so);
			return so;
		}
	}
	
	
}
