package org.nrg.xnat.helpers.prearchive;

import java.io.IOException;
import java.sql.SQLException;

import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.SessionException;
import org.xml.sax.SAXException;

public final class PrearcDatabaseApp {

	/**
	 * @param args
	 * @throws SessionException 
	 * @throws SAXException 
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IllegalStateException, Exception, SQLException, SessionException, IOException {
		PrearcDatabase.initDatabase("/home/aditya/Java/PRE_ARCHIVE_NEW/");
	    System.out.println(System.getenv("PWD"));
	}
}
