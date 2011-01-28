package org.nrg.xnat.helpers.prearchive;

import java.io.IOException;
import java.sql.SQLException;

import org.xml.sax.SAXException;

public final class PrearcDatabaseApp {

	/**
	 * @param args
	 * @throws SessionException 
	 * @throws SAXException 
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IllegalStateException, SQLException, SessionException, IOException {
		PrearcDatabase.initDatabase();
	    System.out.println(System.getenv("PWD"));
	}
}
