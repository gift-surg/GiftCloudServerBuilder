import java.sql.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
/*
 * TestDB
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */



/**
 * @author timo
 *
 */
public class TestDB extends Task {
    private String connectionstring =null;
    private String driver =null;
    private String user =null;
    private String password =null;

    /**
     * @param connectionString The connectionString to set.
     */
    public void setConnectionstring(String connectionstring) {
        this.connectionstring = connectionstring;
    }
    /**
     * @param driver The driver to set.
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }
    /**
     * @param password The password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }
    /**
     * @param user The user to set.
     */
    public void setUser(String user) {
        this.user = user;
    }
    
    /* (non-Javadoc)
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        try {
            Class.forName(driver).newInstance();
            con = DriverManager.getConnection(connectionstring,
              user, password);
        } catch (InstantiationException e) {
            System.out.println("Unable to connected to '" + connectionstring + "'");
            throw new BuildException("Unable to instantiate Driver. (InstantiationException)");
        } catch (IllegalAccessException e) {
            System.out.println("Unable to connected to '" + connectionstring + "'");
            throw new BuildException("Illegal Access Exception.");
        } catch (ClassNotFoundException e) {
            System.out.println("Unable to connected to '" + connectionstring + "'");
            throw new BuildException("Unable to instantiate Driver. (ClassNotFoundException)");
        } catch (SQLException e) {
            String message = e.getMessage();
            if ((message.indexOf("FATAL: database")!=-1) && (message.indexOf("does not exist")!=-1))
            {
                String host = connectionstring.substring(0,connectionstring.lastIndexOf("/"));
                String db = connectionstring.substring(connectionstring.lastIndexOf("/")+1);
                System.out.println("\n\nDatabase '"+ db +"' does not exist at '" + host + "'.");
                System.out.println("Please create the database and re-execute this procedure.");
                throw new BuildException(e.getMessage());
            }else{
                System.out.println("Unable to connected to '" + connectionstring + "'");
                System.out.println("Please resolve the PostgreSQL issue (specified below).");
                throw new BuildException(e.getMessage());
            }
        }finally{
            if (con!=null)
            {
                try {
                    con.close();
                } catch (SQLException e1) {
                }
            }
        }
        
        
    }
}
