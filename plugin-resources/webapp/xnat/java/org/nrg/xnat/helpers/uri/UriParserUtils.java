package org.nrg.xnat.helpers.uri;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.restlet.resources.ExperimentListResource;
import org.nrg.xnat.restlet.resources.ExperimentResource;
import org.nrg.xnat.restlet.resources.ExptAssessmentResource;
import org.nrg.xnat.restlet.resources.ProjSubExptAsstList;
import org.nrg.xnat.restlet.resources.ProjSubExptList;
import org.nrg.xnat.restlet.resources.ProjectAccessibilityResource;
import org.nrg.xnat.restlet.resources.ProjectArchive;
import org.nrg.xnat.restlet.resources.ProjectListResource;
import org.nrg.xnat.restlet.resources.ProjectMemberResource;
import org.nrg.xnat.restlet.resources.ProjectResource;
import org.nrg.xnat.restlet.resources.ProjectSearchResource;
import org.nrg.xnat.restlet.resources.ProjectSubjectList;
import org.nrg.xnat.restlet.resources.ProjectUserListResource;
import org.nrg.xnat.restlet.resources.ProtocolResource;
import org.nrg.xnat.restlet.resources.ReconList;
import org.nrg.xnat.restlet.resources.ReconResource;
import org.nrg.xnat.restlet.resources.ScanDIRResource;
import org.nrg.xnat.restlet.resources.ScanList;
import org.nrg.xnat.restlet.resources.ScanResource;
import org.nrg.xnat.restlet.resources.ScanTypeListing;
import org.nrg.xnat.restlet.resources.ScannerListing;
import org.nrg.xnat.restlet.resources.SubjAssessmentResource;
import org.nrg.xnat.restlet.resources.SubjectListResource;
import org.nrg.xnat.restlet.resources.SubjectResource;
import org.restlet.Router;
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

	/**
	 * A uri parser that reads prearchive uri that requests an action on multiple projects.
	 * @author aditya
	 *
	 */
	static class PrearcProjectsParser implements UriParserI<List<String>> {
		final UriParserUtils.UriParser i;
		public PrearcProjectsParser(UriParserUtils.UriParser i) {
			this.i = i;
		}
		
		/**
		 * Read multiple projects and throw an error if the session label or timestamp are specified.
		 */
		public List<String> readUri (String uri) throws java.util.MissingFormatArgumentException {
			Map<String,Object> so = i.readUri(uri);
			if (null == so.get(PROJECT_ID)) {
				throw new java.util.MissingFormatArgumentException("No projects specified in uri " + uri);
			}
			if (null != so.get("SESSION_LABEL")) {
				throw new java.util.MissingFormatArgumentException("Cannot specify a session label when querying for multiple projects in uri " + uri);
			} 
			if (null != so.get("SESSION_TIMESTAMP")) {
				throw new java.util.MissingFormatArgumentException("Cannot specify a timestamp when querying for multiple projects in uri  " + uri);
			}
			List<String> ls = new ArrayList<String>();
			String[] sa = StringUtils.split((String)so.get(PROJECT_ID),',');
			for (int i = 0; i < sa.length ; i++) {
				if (StringUtils.upperCase(sa[i]).equals(StringUtils.upperCase(PrearcUtils.COMMON))) {
					ls.add(null);
				}
				else {
					ls.add(sa[i]);
				}
			}
			return ls;
		}
	}

	/**
	 * Parse a uri that requests an action on a single session 
	 * @author aditya
	 *
	 */
	static class PrearcSessionParser implements UriParserI<Map<String,String>> {
		final UriParserUtils.UriParser i;
		PrearcSessionParser(UriParserUtils.UriParser i) {
			this.i=i;
		}
		/**
		 * Read a single session from the uri. If the project name field contains more than one
		 * project an error is thrown.
		 */
		public Map<String,String> readUri (String uri) throws java.util.MissingFormatArgumentException {
			Map <String,Object> so = i.readUri(uri);
			if (null == so.get(PROJECT_ID)) {
				throw new java.util.MissingFormatArgumentException("Unable to parse PROJECT_ID using template from uri: " + uri);
			}
			if (null == so.get("SESSION_LABEL")) {
				throw new java.util.MissingFormatArgumentException("Unable to parse SESSION_LABEL using template from uri: " + uri);
			}
			if (null == so.get("SESSION_TIMESTAMP")) {
				throw new java.util.MissingFormatArgumentException("Unable to parse TIMESTAMP using template from uri: " + uri);
			}
			Map<String,String> ss = new HashMap<String,String>();
			Iterator<String> i = ss.keySet().iterator();
			while(i.hasNext()) {
				String key = i.next();
				if (so.get(key) == PrearcUtils.COMMON) {
					ss.put(key, null);
				}
				else {
					ss.put(key, (String) so.get(key));
				}
			}
			return ss;
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
	
	static List<String> prearcTemplates=new ArrayList<String>(){
		{
			add("/prearchive/projects/{" + PROJECT_ID + "}/{" +PrearcUtils.PREARC_TIMESTAMP + "}/{" + PrearcUtils.PREARC_SESSION_FOLDER + "}".intern());
			add("/prearchive/projects/{" + PROJECT_ID + "}/{" + PrearcUtils.PREARC_TIMESTAMP + "}".intern());
			add("/prearchive/projects/{" + PROJECT_ID + "}".intern());
			add("/prearchive".intern());
		}};
	
	public static DataURIA parseURI(String s) throws MalformedURLException{
		if(s.startsWith("/prearchive")){
			if(s.equals("/prearchive"))return new PrearchiveURI(new HashMap<String,Object>(),s);
			
			for(String template: prearcTemplates){
				Map<String,Object> map=new UriParser(template,Template.MODE_EQUALS).readUri(s);
				if(map.size()>0){
					return new PrearchiveURI(map,s);
				}
			}
		}else if(s.startsWith("/archive")){
			if(s.equals("/archive"))return new ArchiveURI(new HashMap<String,Object>(),s);
			
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
	static class UriParser implements UriParserI<Map <String, Object>> {
		String template;
		int mode=Template.MODE_STARTS_WITH;
		UriParser (String template) {
			this.template = template;
		}
		UriParser (String template, int mode) {
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
