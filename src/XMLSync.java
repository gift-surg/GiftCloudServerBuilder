import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.nrg.xdat.security.XDATUser.FailedLoginException;
import org.nrg.xft.XFT;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.utils.DateUtils;
import org.nrg.xft.utils.FileUtils;

/*
 * XMLSync
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 11/18/13 9:35 AM
 */



/**
 * @author timo
 *
 */
public class XMLSync extends CommandPromptTool{
//	java -Xmx256M -classpath .;XFT.jar;commons-collections-2.0.jar;commons-configuration-1.0-dev.jar;commons-dbcp-1.0-dev-20020806.jar;commons-lang-1.0.jar;commons-pool-1.0.jar;jakarta-regexp-1.2.jar;log4j-1.2.6.jar;parser.jar;pg74.214.jdbc2.jar;stratum-1.0-b3.jar;xerces-J_1.4.0.jar XFTApp
	public XMLSync(String[] args)
	{
	    super(args);
	}
    
    public static void main(String[] args) {
        XMLSync b = new XMLSync(args);	
		return;
	}	
	
	public void definePossibleVariables()
	{
        addPossibleVariable("elementName","element to browse.",new String[]{"e","dataType"},true);
        addPossibleVariable("insert_date","Get new items since this date. \"yyyy-MM-dd HH:mm:ss\"",false);
        addPossibleVariable("duration","Number of days used to generate insert_date",false);
        addPossibleVariable("remoteURL","URL of remote server (http://localhost:8080/cnda_xnat)",true);
        addPossibleVariable("remoteUsername","Username for connection to remote server",true);
        addPossibleVariable("remotePassword","Password for connection to remote server",true);
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
        return "Function used to sync the data in two separate XNAT installations.";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "XMLSync";
    }
    
    public void run() throws DBPoolException,SQLException,FailedLoginException,Exception{

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
            _service();
        }else{
            System.out.println("Local Server is not available. " + url.toString());
            System.exit(1);
        }
    }
	
	public void process()
	{
	    System.out.println("Services Unavailable." + url.toString());
        System.exit(1);
	}

    
	public String getService(){
	  return "axis/StoreXML.jws";
	}
	
	public void service()
	{
	    String remoteURL = (String)variables.get("remoteURL");
        if (!remoteURL.endsWith("/"))
        {
            remoteURL += "/";
        }

        String dir = getDirectory();
        if (!dir.endsWith(File.separator))
        {
            dir +=File.separator;
        }
        
        File syncDir = new File(dir + "work" + File.separator + "sync");
        if (!syncDir.exists())
        {
            syncDir.mkdir();
        }
        long currentTime = Calendar.getInstance().getTimeInMillis();
        
        File todaysDir = new File(dir + "work" + File.separator + "sync" + File.separator + currentTime);
        if (!todaysDir.exists())
        {
            todaysDir.mkdir();
        }
        
        File summaryFile = new File(dir + "work" + File.separator + "sync" + File.separator + currentTime + File.separator + "summary.log");        
        FileUtils.OutputToFile("XML Sync:" + Calendar.getInstance().getTime().toString() + "\n\n",summaryFile.getAbsolutePath(),true);
        
        
        URL remote =null;
        try {
            remote = new URL(remoteURL + "axis/Browse.jws");
            remote.getContent();
        } catch (Exception e1) {
    	    System.out.println("Remote Service Unavailable." + remoteURL);
            System.exit(1);
    	    
        }
		
	    int _return = 0;
        try {
            Service service = new Service();
            Call call = (Call)service.createCall();
            call.setTargetEndpointAddress(remote);
            
            call.setOperationName("search");
            
            String user = (String)variables.get("remoteUsername");
            String pass = (String)variables.get("remotePassword");
            String dataType = (String)variables.get("elementName");
            String insertDateSt = (String)variables.get("insert_date");
            String duration = (String)variables.get("duration");

            call.setUsername(user);
            call.setPassword(pass);
            
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
            
            Object[] params = {dataType,insert_date};
            
            FileUtils.OutputToFile("Parameters:\nDATA_TYPE:" + dataType +"\nINSERT_DATE:" + insert_date +"\n\n",summaryFile.getAbsolutePath(),true);
            
            try {
                if (XFT.VERBOSE)System.out.println("Sending Request to " + call.getTargetEndpointAddress() +"...");
                long startTime = Calendar.getInstance().getTimeInMillis();
                Object[] o = (Object[])call.invoke(params);
                long serviceDuration = Calendar.getInstance().getTimeInMillis() - startTime;
                if (XFT.VERBOSE)System.out.println("Response Received (" + serviceDuration + " ms)");

                String pk = (String)o[0];
                Object[] ids = (Object[])o[1];
                if (XFT.VERBOSE)System.out.println(ids.length + " Item(s) Found.");
                FileUtils.OutputToFile(ids.length + " Item(s) Found.\n",summaryFile.getAbsolutePath(),true);
                
                if (ids.length > 0)
                {
                    for (int i =0;i<ids.length;i++)
                    {
                        
                        String id = (String)ids[i];

                	    if (XFT.VERBOSE)System.out.println("Requesting xml for " + id + "");
                        FileUtils.OutputToFile("Requesting xml for " + id + "\n",summaryFile.getAbsolutePath(),true);
                        try {
                            call = (Call)service.createCall();
                            call.setTargetEndpointAddress(remoteURL + "axis/XMLSearch.jws");
                            
                            call.setOperationName("search");
                            
                            user = (String)variables.get("remoteUsername");
                            pass = (String)variables.get("remotePassword");
                            String field =pk;
                            String comparison = "=";
                            String value = id;
                            
                            Boolean limited = Boolean.FALSE;

                            call.setUsername(user);
                            call.setPassword(pass);
                            params = new Object[]{field,comparison,value,dataType,limited};

                            if (XFT.VERBOSE)System.out.println("Sending Request to " + call.getTargetEndpointAddress() +"...");
                            startTime = Calendar.getInstance().getTimeInMillis();
                            o = (Object[])call.invoke(params);
                            serviceDuration = Calendar.getInstance().getTimeInMillis() - startTime;
                            if (XFT.VERBOSE)System.out.println("Response Received (" + serviceDuration + " ms)");
                            FileUtils.OutputToFile("Search (" + serviceDuration + " ms)\n",summaryFile.getAbsolutePath(),true);
                            
                            for (int j =0;j<o.length;j++)
                            {
                                Object[] item = (Object[])o[j];
                               
                                String name = (String)item[0];
                                String content = (String)item[1];
                                                        
                                call = (Call)service.createCall();
                                call.setTargetEndpointAddress(url);

                                FileUtils.OutputToFile(content,dir + "work" + File.separator + "sync" + File.separator + currentTime +File.separator + name +".xml",true);

                                call.setUsername(user);
                                call.setPassword(pass);
                                call.setOperationName("store");
                                params = new Object[]{content,Boolean.FALSE,Boolean.TRUE};
                                
                                user=(String)variables.get("username");
                                pass=(String)variables.get("password");

                                if (XFT.VERBOSE)System.out.println("Sending Request to " + call.getTargetEndpointAddress() +"...");
                                startTime = Calendar.getInstance().getTimeInMillis();
                                String _store = (String)call.invoke(params);
                                serviceDuration = Calendar.getInstance().getTimeInMillis() - startTime;
                                if (XFT.VERBOSE)System.out.println("Response Received (" + serviceDuration + " ms)\n\n");

                                FileUtils.OutputToFile(name + " StoreXML (" + serviceDuration + " ms)\n" + _store + "\n",summaryFile.getAbsolutePath(),true);
                                System.out.println(_store);
                                call = null;
                            }
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
                        }
                    }
                }
            }catch(AxisFault ex2)
            {
                System.out.println(ex2.getFaultString());
                _return= 10;
            }catch (RemoteException ex) {
                Throwable e = ex.getCause();
               
            	ex.printStackTrace();
                _return= 10;
            }
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
        } catch (ServiceException ex) {
            ex.printStackTrace();
			_return= 11;
        } catch (ParseException e) {
            System.out.println("Unable to parse date.");
        }
		System.exit(_return);
	}
	
	
}
