//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
import java.io.File;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Iterator;

import org.nrg.xdat.XDATTool;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.collections.ItemCollection;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.DBItemCache;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.utils.FileUtils;

/*
 * Created on Jan 11, 2006
 *
 */

/**
 * @author Tim
 *
 */
public class FilePathUpdater {
    public static void main(String[] args) {
		//elementName simple/select-grand file/console
			Hashtable hash = new Hashtable();
	
			if (args.length <1){
				showUsage();
				return;
			}
			
			for(int i=0; i<args.length; i++){
							
				if (args[i].equalsIgnoreCase("-u")  || args[i].equalsIgnoreCase("-username")) {
					if (i+1 < args.length) 
					    hash.put("username",args[i+1]);
				}		
				if (args[i].equalsIgnoreCase("-instance")) {
					if (i+1 < args.length) 
					    hash.put("instance",args[i+1]);
				}			
				if (args[i].equalsIgnoreCase("-project")) {
					if (i+1 < args.length) 
					    hash.put("project",args[i+1]);
				}			
				if (args[i].equalsIgnoreCase("-xdir")) {
					if (i+1 < args.length) 
					    hash.put("xdir",args[i+1]);
				}		
				if (args[i].equalsIgnoreCase("-correct")) {
					if (i+1 < args.length) 
					    hash.put("correct",args[i+1]);
				}			

				if (args[i].equalsIgnoreCase("-p")  || args[i].equalsIgnoreCase("-password")) {
					if (i+1 < args.length) 
					    hash.put("password",args[i+1]);
				}				

				if (args[i].equalsIgnoreCase("-h")  || args[i].equalsIgnoreCase("-help")) {
					showUsage();
					return;
				}			

				if (args[i].equalsIgnoreCase("-quiet") ) {
					XFT.VERBOSE=false;
				}	
		
			}
						
			if (hash.get("username") == null || hash.get("password") == null)
			{
				System.out.println("No specified username and password.");
				System.exit(0);
			}
			
			Process(hash);
			return;
	}	
	
	public static void showUsage()
	{
		System.out.println("Search");
		System.out.println("Function used to access particular sub-sets of the data in the database.\n");

		System.out.println("Options:");
		System.out.println("-xdir, File path to the xnat root directory (required with -project)");
		System.out.println("-project, Name of project to use. (required with -xdir)");
		System.out.println("-correct, true or false");
		System.out.println("-u,username (required) : username");
		System.out.println("-p,password (required) : password");
		System.out.println("-quiet (optional) : Disables un-necessary output.");
	}
	
	public static void Process(Hashtable hash)
	{
	    int _return = 0;
		try {
			//System.out.print(elementName + ":" + selectType + ":" + output);
		    if ((hash.get("xdir")!=null) && (hash.get("project")!=null))
			{
			    String xdir = (String)hash.get("xdir");
			    String project = (String)hash.get("project");
			    if (!xdir.endsWith(File.separator))
			    {
			        xdir += File.separator;
			    }
			    hash.put("instance",xdir + "deployments" + File.separator + project);
			}
		    
		    String directory = null;
		    if (hash.get("instance") == null)
		    {
				if (! (new File("InstanceSettings.xml")).exists())
				{
					System.out.println("\nERROR:  Missing instance document 'InstanceSettings.xml'");
					System.out.println("Use the -xdir and -project variables to specify the installation and project dir.");
					System.exit(0);
				}
				File f = new File("");
				directory = f.getAbsolutePath();
		    }else{
		        directory = (String) hash.get("instance");
		        if (directory.endsWith("InstanceSettings.xml"))
		        {
		            directory = directory.substring(0,directory.length()-21);
		        }
		    }
		    
		    boolean correct = false;
		    if (hash.get("correct")!=null)
		    {
		        if (((String)hash.get("correct")).equalsIgnoreCase("true")){
		            correct = true;
		        }
		    }

			String username =(String)hash.get("username");
			String password = (String)hash.get("password");
			if (username==null || password==null)
			{
			    throw new Exception("Requires a username and password.");
			}
			
			XDATTool tool = new XDATTool(directory,username,password);
			String dir = tool.getWorkDirectory();
			
			ItemCollection coll= ItemSearch.GetAllItems("xnat:file",tool.getUser(),false);

            DBItemCache cache =  new DBItemCache(null,EventUtils.DEFAULT_EVENT(tool.getUser(),"File Path Updater"));
            String dbname = null;
			StringBuffer sb = new StringBuffer();
			sb.append("SESSION,OLD,NEW\n");
			Iterator iter = coll.getItemIterator();
			int i = 1;
//			while (iter.hasNext())
//			{
//			    System.out.print("Checking " + (i++) + " of " + coll.size() + "... ");
//			    XFTItem item = (XFTItem)iter.next();
//			    dbname = item.getDBName();
//			    String path = item.getStringProperty("xnat:file.path");
//			    String initialPath = path;
////			    if (initialPath.startsWith("arc006"))
////			    {
//				    path = FileUtils.AppendRootPath(path);
//				    File file = new File(path);
//				    tool.info("Checking: " + path);
//				    if (!file.exists())
//				    {
//				        System.out.println("MISSING");
//				        tool.info("Missing");
//				        String s = null;
//				        try {
//	                        s = FileUtils.ParseSessionIDFromPath(path);
//	                        tool.info(s);
//	                        String arcFind = FileUtils.ArcFind(s);
//	                        tool.info("FOUND");
//	                        String newPath = arcFind;
//	                        if (!initialPath.startsWith(File.separator))
//	                        {
//	                            String newRelativePath = FileUtils.RemoveRootPath(arcFind);
//	                            newPath = newRelativePath + initialPath.substring(initialPath.indexOf(File.separator));
//	                        }else{
//	                            String relativePath = initialPath.substring(initialPath.indexOf(File.separator + s));
//	                            newPath += relativePath;
//	                        }
//	                        
//	                        try {
//	                            item.setProperty("xnat:file.path",newPath);
//
//	                            if (correct)
//	                            {
//	                                DBAction.StoreItem(item,tool.getUser(),false,false,false,false,SecurityManager.GetInstance());
//	                                sb.append(s).append(",").append(initialPath).append(",").append(newPath).append(",CORRECTED").append("\n");
//	                            }else{
//	                                DBAction.StoreItem(item,tool.getUser(),false,false,false,false,SecurityManager.GetInstance(),cache);
//	                                sb.append(s).append(",").append(initialPath).append(",").append(newPath).append("\n");
//	                            }
//	                        } catch (Exception e2) {
//	                            sb.append(s).append(",").append(initialPath).append(",").append(newPath).append(",").append(e2.toString()).append("\n");
//	                            tool.info(e2);
//	                            e2.printStackTrace();
//	                        }
//	                        
//	                        
//	                    } catch (Exception e1) {
//	                        sb.append(s).append(",").append(path).append(",").append(e1.toString()).append("\n");
//	                        tool.info(e1);
//	                        e1.printStackTrace();
//	                    }
//				    }else{
//				        System.out.println("OK");
//				        tool.info("OK");
//				    }
//			    }
//			//}
//
//			
			if (!correct)
			{
			    XFT.LogInsert(cache.getSQL(),"file_update");
			}
			
			FileUtils.OutputToFile(sb.toString(),tool.getWorkDirectory()+ "missing.csv");
			
		} catch (ElementNotFoundException e) {
			e.printStackTrace();
			_return= 4;
		} catch (XFTInitException e) {
			e.printStackTrace();
			_return= 5;
		} catch (SQLException e) {
			e.printStackTrace();
			_return= 6;
		} catch (DBPoolException e) {
			e.printStackTrace();
			_return= 7;
		} catch (Exception e) {
			e.printStackTrace();
			_return= 9;
		}finally{
		    try {
	            XFT.closeConnections();
	        } catch (SQLException e1) {
	        }
		}
		System.exit(_return);
	}
}
