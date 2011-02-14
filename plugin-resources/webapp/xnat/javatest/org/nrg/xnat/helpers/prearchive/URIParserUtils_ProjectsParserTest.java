package org.nrg.xnat.helpers.prearchive;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.MissingFormatArgumentException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nrg.xnat.restlet.XNATApplication;

public class URIParserUtils_ProjectsParserTest {	
	@Test
	public final void testSessionParser() {
		final PrearcUriParserUtils.SessionParser parser = new PrearcUriParserUtils.SessionParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_SESSION_URI));
		this.missingLabel(parser);
		this.missingProject(parser);
		this.missingTimestamp(parser);
		this.unassignedProjectToNull(parser);
		this.wellFormedSession(parser);
	}
	
	@Test
	public final void testProjectsParser () {
		final PrearcUriParserUtils.ProjectsParser parser = new PrearcUriParserUtils.ProjectsParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_PROJECT_URI));
		this.singleProject(parser);
		this.multipleProjects(parser);
		this.unassignedProjectToNull(parser);
		this.sessionUriToProjectsParser(parser);
	}
	
	public final void sessionUriToProjectsParser (PrearcUriParserUtils.ProjectsParser parser) {
		String uri = "prearchive/projects/proj/timestamp/label";
		try {
			parser.readUri(uri);
			fail("Should have thrown a MissingFormatArgumentException");
		}
		catch (MissingFormatArgumentException e){			
		}
	}
	

	public final void singleProject (PrearcUriParserUtils.ProjectsParser parser) {
		String uri = "prearchive/projects/proj";
		final List<String> ls = parser.readUri(uri);
		assert(ls.size() == 1 && ls.contains((String) "proj"));
	}
	
	public final void multipleProjects (PrearcUriParserUtils.ProjectsParser parser) {
		String uri = "prearchive/projects/proj1,proj2,proj3";
		final List<String> ls = parser.readUri(uri);
		assert(ls.size() == 3 && ls.contains((String) "proj1") && ls.contains((String) "proj2") && ls.contains((String) "proj3"));
	}
	
	public final void unassignedProjectToNull (PrearcUriParserUtils.ProjectsParser parser) {
		String uri = "prearchive/projects/Unassigned";
		final List<String> ls = parser.readUri(uri);
		assert(ls.size() == 1 && null == ls.get(0));
	}
	
	public final void missingLabel (PrearcUriParserUtils.SessionParser parser) {
		String uri = "prearchive/projects/proj/timestamp/";
		try {
			final Map<String,String> sess = parser.readUri(uri);
			fail ("Should have thrown MissingFormatArgumentException");
		}
		catch (MissingFormatArgumentException e){			
		}		
	}
	
	public final void missingProject (PrearcUriParserUtils.SessionParser parser) {
		String uri = "prearchive/projects//timestamp/label";
		try {
			parser.readUri(uri);
			fail ("Should have thrown MissingFormatArgumentException");
		}
		catch (MissingFormatArgumentException e){			
		}		
	}
	
	public final void missingTimestamp (PrearcUriParserUtils.SessionParser parser) {
		String uri = "prearchive/projects/proj//label";
		try {
			final Map<String,String> sess = parser.readUri(uri);
			fail ("Should have thrown MissingFormatArgumentException");
		}
		catch (MissingFormatArgumentException e){			
		}		
	}
	
	public final void unassignedProjectToNull (PrearcUriParserUtils.SessionParser parser) {
		String uri = "prearchive/projects/Unassigned/timestamp/label";
		final Map<String,String> sess = parser.readUri(uri);
		assertNull(sess.get("PROJECT_ID"));
	}
	
	public final void wellFormedSession (PrearcUriParserUtils.SessionParser parser) {
		String uri = "prearchive/projects/proj/timestamp/sess";
		final Map<String,String> sess = parser.readUri(uri);
		assertEquals(sess.get("SESSION_LABEL"), "sess");
		assertEquals(sess.get("PROJECT_ID"), "proj");
		assertEquals(sess.get("SESSION_TIMESTAMP"), "timestamp");
	}
}
