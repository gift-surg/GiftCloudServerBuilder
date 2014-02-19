/*
 * Browse
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.security.XDATUser.FailedLoginException;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTool;
import org.nrg.xft.collections.ItemCollection;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.utils.DateUtils;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
/**
 * @author Tim
 */
public class Browse extends CommandPromptTool{
//	java -Xmx256M -classpath .;XFT.jar;commons-collections-2.0.jar;commons-configuration-1.0-dev.jar;commons-dbcp-1.0-dev-20020806.jar;commons-lang-1.0.jar;commons-pool-1.0.jar;jakarta-regexp-1.2.jar;log4j-1.2.6.jar;parser.jar;pg74.214.jdbc2.jar;stratum-1.0-b3.jar;xerces-J_1.4.0.jar XFTApp
	public Browse(String[] args)
	{
	    super(args);
	}
    
    public static void main(String[] args) {
		Browse b = new Browse(args);	
		return;
	}	
	
	public void definePossibleVariables()
	{
        addPossibleVariable("elementName","element to browse.",new String[]{"e","dataType"},true);
        addPossibleVariable("output","output Format (i.e. 'xml','csv','text', or 'console') - default is output to xml.",new String[]{"f","format"});

        addPossibleVariable("backup","whether or not to duplicate files.",new String[]{"duplicate"});
        addPossibleVariable("dir","Directory to store files.",new String[]{"dir"});
        addPossibleVariable("insert_date","Get new items since this date. \"yyyy-MM-dd HH:mm:ss\"",false);
        addPossibleVariable("duration","Number of days used to generate insert_date",false);
        addPossibleVariable("pp"," (true or false) Whether or not to 'pretty-print' the xml (not recommended for large files).",false);
	}
	
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getAdditionalUsageInfo()
     */
    public String getAdditionalUsageInfo() {
		return "";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getDescription()
     */
    public String getDescription() {
        return "Function used to access the data stored in the database from a command prompt.";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "Browse";
    }
    
    public void run() throws DBPoolException,SQLException,FailedLoginException,Exception{
        if (getService()==null)
        {
            _process();
        }else{
            
            boolean URLExists = true;
            try {
                url =new URL(getServiceURL());
                url.getContent();
            } catch (Exception e1) {
                URLExists = false;
    		    if (XFT.VERBOSE)System.out.println("Unable to connect to web service.");
            }
            
            if (URLExists)
            {
                String output = "xml";
    			if (variables.get("output") != null)
    				output = (String)variables.get("output");
    			
    			if (output.equalsIgnoreCase("xml") || output.equalsIgnoreCase("text"))
    			{
    			    _service();
    			}else{
    			    _process();
    			}
            }else{
                _process();
            }
        }
    }
	
	public void process()
	{
	    Hashtable hash = variables;
	    String elementName=(String)hash.get("elementName");
	    
	    String dir=(String)hash.get("dir");
	    String username=(String)hash.get("username");
	    String password=(String)hash.get("password");
	    String instance=(String)hash.get("instance");
	    String pp = (String)variables.get("pp");
        
        Boolean pretty = Boolean.FALSE;
        if (pp!=null)
        {
            if (pp.equalsIgnoreCase("true"))
            {
                pretty = Boolean.TRUE;
            }
        }
        String output=(String)hash.get("output");
	    if (output==null)
	    {
	        output="xml";
	    }
	    String backup=(String)hash.get("backup");
	    if (backup==null)
	    {
	        backup="true";
	    }
		try {
		    
			if (dir == null || dir.equals(""))
			{
			    File f = new File("");
			   dir = f.getAbsolutePath();
			}
			if (! dir.endsWith(File.separator))
			{
				dir += File.separator;			
			}
			
			boolean valid = XFTTool.ValidateElementName(elementName);
			if (! valid)
			{
				System.out.println("\nERROR:  Invalid Element '" + elementName + "'");
				System.exit(0);
			}
	
			if (output.equalsIgnoreCase("csv"))
			{
			    tool.CSVSearch(elementName);
				System.out.println("Create CSV file.");
			}else if (output.equalsIgnoreCase("xml")){
			    if (backup.equalsIgnoreCase("true"))
			    {
				    tool.XMLSearch(elementName,true,dir,false,pretty.booleanValue());
				}else{
				    tool.XMLSearch(elementName,false,dir,false,pretty.booleanValue());
			    }
			}else if (output.equalsIgnoreCase("text")){
                String field =elementName + ".ID";
                
                field = StringUtils.StandardizeXMLPath(field);
                ItemSearch search = ItemSearch.GetItemSearch(elementName,tool.getUser());
                search.setAllowMultiples(false);
                search.addCriteria(field, "NULL", " IS NOT " );
                
                ItemCollection items= search.exec(false);   
                
                if (items.size()>0)
                {
                    Iterator iter = items.getItemIterator();
                    System.out.println(items.size()+ " Matching Items Found.\n\n");
                    while (iter.hasNext())
                    {
                        XFTItem item =(XFTItem)iter.next();
                        Object id = item.getProperty(field);
                        ItemI bo = BaseElement.GetGeneratedItem(item);
                        String text = bo.output();

                        File dest = new File(dir);
                        String finalName = id + ".txt";
                        FileUtils.OutputToFile(text, dir + finalName, false);
                        System.out.println("Created File: " + dir + finalName);
                    }
                }else{
                    System.out.println("No Matches Found.");
                }
            }else
			{
			    tool.HTMLSearch(elementName);
				System.out.println("Create HTML file.");
			}
		} catch (ElementNotFoundException e) {
			e.printStackTrace();
		} catch (XFTInitException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (DBPoolException e) {
			e.printStackTrace();
		} catch (FieldNotFoundException e) {
			e.printStackTrace();
        } catch (FailedLoginException e) {
            System.out.println("Invalid login and/or password.");
			e.printStackTrace();
        } catch (Exception e) {
			e.printStackTrace();
        }finally{
		    try {
                XFT.closeConnections();
            } catch (SQLException e1) {
            }
		}
		return;
	}

    
	public String getService(){
	  return "axis/Browse.jws";
	}
	
	public void service()
	{
	    if (XFT.VERBOSE)System.out.println("Using Web Service");
	    long ultimateStartTime = Calendar.getInstance().getTimeInMillis();
	    File f = new File("");
	    String dir = f.getAbsolutePath();
		
		if (variables.get("dir") != null)
			dir = (String)variables.get("dir");
		
		if (! dir.endsWith(File.separator))
			dir += File.separator;
		
		f = new File(dir);
		if (!f.exists())
		{
		    f.mkdir();
		}
	    int _return = 0;
	    

        String user = (String)variables.get("username");
        String pass = (String)variables.get("password");
        String dataType = (String)variables.get("elementName");
        String insertDateSt = (String)variables.get("insert_date");
        String duration = (String)variables.get("duration");

	    String backupS=(String)variables.get("backup");
	    boolean duplicate = false;
	    if (backupS==null || backupS.equalsIgnoreCase("false"))
	    {
	        duplicate=false;
	    }else{
	        duplicate= true;
	    }

        String output=(String)variables.get("output");
        if (output==null)
        {
            output="xml";
        }
	    try {
	        Date insert_date = null;
	        if (insertDateSt!=null)
	        {
	            insert_date = DateUtils.parseDateTime(insertDateSt);
	        }else{
	            if (duration !=null)
	            {
	                Integer amount = Integer.valueOf(duration);
	                Calendar calendar = Calendar.getInstance();
	                calendar.add(Calendar.DATE,-amount.intValue());
	                insert_date=calendar.getTime();
	            }
	        }
	        
            Service service = new Service();
            
            //REQUEST SESSION ID
            Call call = (Call)service.createCall();
            call.setUsername(user);
            call.setPassword(pass);
            URL requestSessionURL = new URL(this.getSiteURL() + "axis/CreateServiceSession.jws");
            call.setTargetEndpointAddress(requestSessionURL);
            call.setOperationName("execute");
            
            Object[] params = {};  
            String session = (String)call.invoke(params);
            
            //SEND SEARCH
            call = (Call)service.createCall();
            call.setTargetEndpointAddress(url);
            call.setUsername(user);
            call.setPassword(pass);
            call.setOperationName("search");
            params = new Object[]{session,dataType,insert_date};
            

    	    if (XFT.VERBOSE)System.out.println("Sending Request...");
    	    long startTime = Calendar.getInstance().getTimeInMillis();
            Object[] o = (Object[])call.invoke(params);
    	    long serviceDuration = Calendar.getInstance().getTimeInMillis() - startTime;
    	    if (XFT.VERBOSE)System.out.println("Response Received (" + serviceDuration + " ms)");

    	    String pk = (String)o[0];
    	    Object[] ids = (Object[])o[1];
    	    if (XFT.VERBOSE)System.out.println(ids.length + " Item(s) Found.");
    	    
    	    if (ids.length > 0)
    	    {
        	    for (int i =0;i<ids.length;i++)
                {
        	        
                    Object id = (Object)ids[i];
                    
                    user = (String)variables.get("username");
                    pass = (String)variables.get("password");     
                    
                    if (output.equalsIgnoreCase("text"))
                    {
                        try {
                            service = new Service();
                            call = (Call)service.createCall();
                            call.setTargetEndpointAddress(this.getSiteURL() + "axis/VelocitySearch.jws");
                            
                            call.setOperationName("search");
                            
                            String field =dataType + ".ID";
                            String comparison = "=";
                            Object value = id;

                            call.setUsername(user);
                            call.setPassword(pass);
                            params = new Object[]{field,comparison,value,dataType};
                            

                            if (XFT.VERBOSE)System.out.print("Sending Request For " + id + "...");
                            startTime = Calendar.getInstance().getTimeInMillis();
                            String text = (String)call.invoke(params);
                            if (text.indexOf("\r\n")==-1){
                                text = text.replaceAll("\n", System.getProperty("line.separator")); 
                            }
                            long time = Calendar.getInstance().getTimeInMillis() - startTime;
                            if (XFT.VERBOSE)System.out.println(" Response Received (" + time + " ms)\n\n");
                            File dest = new File(dir);
                            String finalName = id + ".txt";
                            FileUtils.OutputToFile(text, dir + finalName, false);
                            System.out.println("Created File: " + dir + finalName);
                        } catch (Throwable e) {
                            System.out.println("Error retrieving text for " + id);
                            System.out.println(e.getMessage());
                        }
                    }else{
                        File dest = new File(dir);
                        String finalName = id + ".xml";
                        boolean exists = FileUtils.SearchFolderForChild(dest,finalName);
                        if (exists && !duplicate)
                        {
                            if (XFT.VERBOSE)
                                System.out.println(id + ".xml already exists.");
                        }else{
                            
                            int counter = 0;
                            while(exists)
                            {
                                finalName = id + "_v" + (counter++) + ".xml";
                                exists =FileUtils.SearchFolderForChild(dest,finalName);
                            }
                            
                            File outFile = new File(dir + finalName);
                            
                            try {
                                if (XFT.VERBOSE)System.out.println("Requesting xml for " + id + "");
                                startTime = Calendar.getInstance().getTimeInMillis();
                                URL url = new URL(this.getSiteURL() + "app/template/XMLSearch.vm/session/" + session + "/id/" + id + "/data_type/" + dataType);
//                           Use Buffered Stream for reading/writing.
                                BufferedInputStream  bis = null; 
                                BufferedOutputStream bos = null;
                                
                                FileOutputStream out = new FileOutputStream(outFile);

                                bis = new BufferedInputStream(url.openStream());
                                bos = new BufferedOutputStream(out);

                                byte[] buff = new byte[2048];
                                int bytesRead;
                                
                                while(-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
                                    bos.write(buff, 0, bytesRead);

                                }
                                
                                bos.flush();
                                bos.close();
                                
                                serviceDuration = Calendar.getInstance().getTimeInMillis() - startTime;
                                if (XFT.VERBOSE)System.out.println("Response Received (" + serviceDuration + " ms)");

                                System.out.println("Created File: " + dir + finalName);
                            } catch (MalformedURLException e) {
                                System.out.println("Error retrieving xml for " + id);
                                System.out.println(e.getMessage());
                            } catch (FileNotFoundException e) {
                                System.out.println("Error retrieving xml for " + id);
                                System.out.println(e.getMessage());
                            } catch (IOException e) {
                                System.out.println("Error retrieving xml for " + id);
                                System.out.println(e.getMessage());
                            }
                        }
                    }
                }
    	    }

    	    if (XFT.VERBOSE)System.out.println("Total Time: " + (Calendar.getInstance().getTimeInMillis() - ultimateStartTime) + " ms");
        }catch(AxisFault ex2)
        {
            System.out.println(ex2.getFaultString());
            _return= 10;
        }catch (RemoteException ex) {
            Throwable e = ex.getCause();
           
			ex.printStackTrace();
            _return= 10;
        } catch (ServiceException ex) {
            ex.printStackTrace();
			_return= 11;
        } catch (MalformedURLException ex) {
            System.out.println("Invalid date format: Try \"yyyy-MM-dd HH:mm:ss\"");
			_return= 11;
        } catch (java.text.ParseException ex) {
            System.out.println("Invalid date format: Try \"yyyy-MM-dd HH:mm:ss\"");
			_return= 11;
        }
		System.exit(_return);
	}
	
	
}
