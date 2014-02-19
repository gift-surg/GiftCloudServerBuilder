/*
 * StoreXML
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */


import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.nrg.xdat.XDATTool;
import org.nrg.xft.XFT;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.exception.ValidationException;
import org.nrg.xft.utils.FileUtils;
import org.xml.sax.SAXParseException;

import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;

import org.apache.axis.AxisFault;
public class StoreXML extends CommandPromptTool{
//	java -Xmx256M -classpath .;XFT.jar;commons-collections-2.0.jar;commons-configuration-1.0-dev.jar;commons-dbcp-1.0-dev-20020806.jar;commons-lang-1.0.jar;commons-pool-1.0.jar;jakarta-regexp-1.2.jar;log4j-1.2.6.jar;parser.jar;pg74.214.jdbc2.jar;stratum-1.0-b3.jar;xerces-J_1.4.0.jar XFTApp
    public StoreXML(String[] args)
	{
	    super(args);
	}
    
    public static void main(String[] args) {
		StoreXML b = new StoreXML(args);	
		return;
	}	
    
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#definePossibleVariables()
     */
    public void definePossibleVariables() {
        addPossibleVariable("location","location of xml file to insert.",new String[]{"l","location"},false);
        addPossibleVariable("dir","directory containing files to insert.",new String[]{"dir"},false);
        addPossibleVariable("r","in combination with the dir tag, this will cause the app to descend into sub folders looking for xml files.",new String[]{"r","recursive"},false);
        addPossibleVariable("activate","Auto-activate all inserted data.",false);
        addPossibleVariable("quarantine","Load all new/updated data into quaratine.",false);
        addPossibleVariable("allowItemOverwrite","(either 'true' or 'false'): Whether or not pre-existing data for this element which has no unique indentifiers specified, should be overwritten.  If 'true' the pre-existing rows will be removed before the new rows are inserted.  If 'false', then the new rows will be added (appended) without affect to the pre-existing rows.",new String[]{"allowDataDeletion","allowItemOverwrite"},true);

        addPossibleVariable("stopAtException","stop At Exception (Defaults to true)");
    }
    

    
	public String getService(){
	  return "axis/StoreXML.jws";
	}
	
	public void service()
	{
	    Hashtable hash= variables;
	    
	    String s = (String)hash.get("allowItemOverwrite");
	    if (s.equalsIgnoreCase("true"))
	    {
	        hash.put("allowItemOverwrite",new Boolean(true));
	    }else{
	        hash.put("allowItemOverwrite",new Boolean(false));
	    }
	    
	    s = (String)hash.get("stopAtException");
	    if (s!=null)
	    {
		    if (s.equalsIgnoreCase("true"))
		    {
		        hash.put("stopAtException",new Boolean(true));
		    }else{
		        hash.put("stopAtException",new Boolean(false));
		    }
	    }

        
        s = (String)hash.get("quarantine");
        if (s!=null)
        {
            if (s.equalsIgnoreCase("true"))
            {
                hash.put("quarantine",new Boolean(true));
            }else{
                hash.put("quarantine",new Boolean(false));
            }
        }
        
	    Boolean quarantine = (Boolean)hash.get("quarantine");
		Boolean allowItemOverwrite = (Boolean)hash.get("allowItemOverwrite");
		
		boolean stopAtException = true;
		if (hash.get("stopAtException")!=null)
		{
		    stopAtException =((Boolean)hash.get("stopAtException")).booleanValue();
		}

	    Service service = new Service();
        String user = (String)variables.get("username");
        String pass = (String)variables.get("password");
		
		try {
            if (hash.get("dir") ==null)
            {
            	File f = new File((String)hash.get("location"));
            	if (!f.exists())
            	{
            	    System.out.println("Unable to find file: " + f.getAbsolutePath());
        			System.exit(1);
            	}
                sendFile(f,user,pass,quarantine,allowItemOverwrite,service);
            }else{
            	//tool.info((String)hash.get("dir") +"," + hash.get("r")+ "," + quarantine + ","+allowItemOverwrite.booleanValue());
            	File dir = new File((String)hash.get("dir"));
            	
            	if (hash.get("r") ==null)
            	{
            		storeXMLFolderService(dir,false,service,user,pass,quarantine,allowItemOverwrite,stopAtException);
            	}else{
            		storeXMLFolderService(dir,true,service,user,pass,quarantine,allowItemOverwrite,stopAtException);
            	}
            }
        }catch(AxisFault ex2)
        {
	        System.out.println("Error Storing File.");
            System.out.println(ex2.getFaultString());
			System.exit(1);
        }catch(RemoteException ex2)
        {
	        System.out.println("Error Storing File.");
            System.out.println(ex2.getMessage());
			System.exit(1);
        }catch (ServiceException e) {
	        System.out.println("Error Storing File.");
            System.out.println(e.getMessage());
			System.exit(1);
        }
        System.exit(0);
	}

    @SuppressWarnings("deprecation")
	public static String GetContents(File f)
	{
	    try {
            FileInputStream in = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(in);
            StringBuffer sb = new StringBuffer();
            while (dis.available() !=0)
			{
                                    // Print file line to screen
				sb.append(dis.readLine()).append("\n");
			}

            dis.close();

            return sb.toString();
        } catch (Exception e) {
            return "";
        }
	}
	
	private void sendFile(File f, String user, String pass, Boolean quarantine, Boolean allowItemOverwrite, Service service) throws RemoteException, ServiceException
	{
        Call call = (Call)service.createCall();
        call.setTargetEndpointAddress(url);
        call.setUsername(user);
        call.setPassword(pass);
        
        call.setOperationName("store");
        Object[] params = {GetContents(f),quarantine,allowItemOverwrite};
        
        if (XFT.VERBOSE)System.out.println("\nFound Document: " + f.getAbsolutePath());
	    if (XFT.VERBOSE)System.out.println("Sending Request...");
	    long startTime = Calendar.getInstance().getTimeInMillis();
        String o = (String)call.invoke(params);
	    long duration = Calendar.getInstance().getTimeInMillis() - startTime;
	    if (XFT.VERBOSE)System.out.println("Response Received (" + duration + " ms)");
	    
	    System.out.println(o);
	    call = null;
	}
	
	public void process()
	{
	    Hashtable hash= variables;
	    
	    String s = (String)hash.get("allowItemOverwrite");
	    if (s.equalsIgnoreCase("true"))
	    {
	        hash.put("allowItemOverwrite",new Boolean(true));
	    }else{
	        hash.put("allowItemOverwrite",new Boolean(false));
	    }
	    
	    s = (String)hash.get("stopAtException");
	    if (s!=null)
	    {
		    if (s.equalsIgnoreCase("true"))
		    {
		        hash.put("stopAtException",new Boolean(true));
		    }else{
		        hash.put("stopAtException",new Boolean(false));
		    }
	    }
	    
        s = (String)hash.get("quarantine");
        if (s!=null)
        {
            if (s.equalsIgnoreCase("true"))
            {
                hash.put("quarantine",new Boolean(true));
            }else{
                hash.put("quarantine",new Boolean(false));
            }
        }
	    
		try {
			//System.out.print(elementName + ":" + selectType + ":" + output);
			
			Boolean quarantine = (Boolean)hash.get("quarantine");
			Boolean allowItemOverwrite = (Boolean)hash.get("allowItemOverwrite");
			
			boolean stopAtException = true;
			if (hash.get("stopAtException")!=null)
			{
			    stopAtException =((Boolean)hash.get("stopAtException")).booleanValue();
			}
			
			if (tool.getUser()!=null)
		    {
				tool.info("\n\nStoreXML (" + tool.getUser().getUsername() + ")");
		    }
		    
			if (hash.get("dir") ==null)
			{
				tool.info((String)hash.get("location") + "," + quarantine + ","+allowItemOverwrite.booleanValue());
				tool.storeXML(((String)hash.get("location")),quarantine,allowItemOverwrite.booleanValue());
			}else{
				tool.info((String)hash.get("dir") +"," + hash.get("r")+ "," + quarantine + ","+allowItemOverwrite.booleanValue());
				File dir = new File((String)hash.get("dir"));
				if (hash.get("r") ==null)
				{
					StoreXMLFolder(dir,false,tool,quarantine,allowItemOverwrite,hash,stopAtException);
				}else{
					StoreXMLFolder(dir,true,tool,quarantine,allowItemOverwrite,hash,stopAtException);
				}
			}
			
		} catch (ValidationException e) {
			XFT.LogError(e.VALIDATION_RESULTS.toString(),e);
			System.out.println("ERROR:  See log for details (logs/xdat.log).");
			System.out.println(e.VALIDATION_RESULTS.toString());
			System.exit(1);
		} catch (InvalidItemException e) {
			XFT.LogError(e.getMessage());
			System.out.println("ERROR:  See log for details (logs/xdat.log).");
			System.exit(1);
		} catch (Exception e) {
			XFT.LogError("",e);
			System.out.println(e.getMessage());
			System.out.println("ERROR:  See log for details (logs/xdat.log).");
			System.exit(1);
		}finally{
		    try {
                XFT.closeConnections();
            } catch (SQLException e1) {
            }
		}
		System.exit(0);
	}
	
	private void storeXMLFolderService(File dir, boolean recursive, Service service, String user, String pass, Boolean quarantine, Boolean allowItemOverwrite, boolean stopAtException) throws RemoteException, ServiceException
	{
	    if (!dir.exists())
		{
		}else{
		    ArrayList dirs = new ArrayList();
			File[] list = dir.listFiles();
			for (int i=0;i<list.length;i++)
			{
				if (list[i].getName().endsWith(".xml"))
				{
				    try {
                        sendFile(list[i],user,pass,quarantine,allowItemOverwrite,service);
				    }catch(AxisFault ex2)
			        {
				        System.out.println("Error Storing " + list[i].getAbsolutePath());
			            System.out.println(ex2.getFaultString());
			            
			            if (stopAtException)
                        {
                            throw ex2;
                        }else{
                            FileUtils.OutputToFile("Error Storing " + list[i].getName() + " " + ex2.getFaultString(),"storeXMLexceptions.txt",true);
                        }
			        }catch(RemoteException ex2)
			        {
				        System.out.println("Error Storing " + list[i].getAbsolutePath());
			            System.out.println(ex2.getMessage());
			            
			            if (stopAtException)
                        {
                            throw ex2;
                        }else{
                            FileUtils.OutputToFile("Error Storing (XNAT - Validation) " + list[i].getName() + " " + ex2.getMessage(),"storeXMLexceptions.txt",true);
                        }
			        }
				}else{
					if (list[i].isDirectory() && recursive)
					{
						dirs.add(list[i]);
					}
				}
			}
			
			if (recursive)
			{
			    Iterator iter = dirs.iterator();
			    while (iter.hasNext())
			    {
			        File f = (File)iter.next();

					storeXMLFolderService(f,recursive,service,user,pass,quarantine,allowItemOverwrite,stopAtException);
			    }
			}
		}
	}
	
	private static void StoreXMLFolder(File dir, boolean recursive, XDATTool tool, Boolean quarantine, Boolean allowItemOverwrite, Hashtable hash, boolean stopAtException) throws Exception
	{
		if (!dir.exists())
		{
		}else{
		    ArrayList dirs = new ArrayList();
			File[] list = dir.listFiles();
			for (int i=0;i<list.length;i++)
			{
				if (list[i].getName().endsWith(".xml"))
				{
				    try {
				        if (!XFT.VERBOSE)System.out.println("\nFound Document: " + list[i].getAbsolutePath());
				        long startTime = Calendar.getInstance().getTimeInMillis();
                        tool.storeXML(list[i],quarantine,allowItemOverwrite.booleanValue());
                        if (XFT.VERBOSE)System.out.print("  " + ((float) (Calendar.getInstance().getTimeInMillis()-startTime)/1000) + "s ");
				    } catch (ValidationException e) {
						XFT.LogError(e.VALIDATION_RESULTS.toString(),e);
						System.out.println("ERROR:  See log for details (logs/xdat.log).");
						System.out.println(e.VALIDATION_RESULTS.toFullString());
						
                        if (stopAtException)
                        {
                            throw e;
                        }else{
                            FileUtils.OutputToFile("Error Storing (XNAT - Validation) " + list[i].getName() + " " + e.VALIDATION_RESULTS.toFullString(),tool.getSettingsDirectory() + "/logs/storeXMLexceptions.txt",true);
                        }
					} catch (InvalidItemException e) {
						XFT.LogError(e.getMessage());
						System.out.println("ERROR:  See log for details (logs/xdat.log).");

                        if (stopAtException)
                        {
                            throw e;
                        }else{
                            FileUtils.OutputToFile("Error Storing (Invalid permissions) " + list[i].getName() + " " + e.getMessage(),tool.getSettingsDirectory() + "/logs/storeXMLexceptions.txt",true);
                        }
                        
					} catch (SAXParseException e) {
						XFT.LogError(e.getMessage());
						System.out.println(e.getMessage());
						System.out.println("ERROR:  See log for details (logs/xdat.log).");

                        if (stopAtException)
                        {
                            throw e;
                        }else{
                            FileUtils.OutputToFile("Error Storing (XERCES - Validation) " + list[i].getName() + " " + e.getMessage(),tool.getSettingsDirectory() + "/logs/storeXMLexceptions.txt",true);
                        }
                        
					} catch (Exception e) {
						XFT.LogError("",e);
						System.out.println(e.getMessage());
						System.out.println("ERROR:  See log for details (logs/xdat.log).");

                        if (stopAtException)
                        {
                            throw e;
                        }else{
                            FileUtils.OutputToFile("Error Storing " + list[i].getName() + " \n" + e.toString(),tool.getSettingsDirectory() + "/logs/storeXMLexceptions.txt",true);
                        }
					}
				}else{
					if (list[i].isDirectory() && recursive)
					{
						dirs.add(list[i]);
					}
				}
			}
			
			if (recursive)
			{
			    Iterator iter = dirs.iterator();
			    while (iter.hasNext())
			    {
			        File f = (File)iter.next();

					StoreXMLFolder(f,recursive,tool,quarantine,allowItemOverwrite,hash,stopAtException);
			    }
			}
		}
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
        return "Function insert data from a xml document into the database.";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "StoreXML";
    }
    
    public boolean requireLogin()
    {
        String location = (String)this.arguments.get("location");
        
        if (location==null)
        {
            location = (String)this.arguments.get("l");
        }
        
        if (location == null)
        {
            return true;
        }else{
            if (location.endsWith("security.xml"))
            {
                return false;
            }else{
                return true;
            }
        }
    }
}
