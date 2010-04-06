import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.nrg.xft.XFT;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.zip.ZipUtils;

/**
 * @author timo
 *
 */
public class ArcGet extends CommandPromptTool {

    
    public static void main(String[] args) {
        ArcGet b = new ArcGet(args);	
		return;
	}	
    /**
     * @param args
     */
    public ArcGet(String[] args) {
        super(args);
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "ArcGet";
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
        return "Function used to obtain a local copy of the image files for a MRSession from the XNAT archive.";
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#process()
     */
    public void process() {

    }
    
    public void service(){
	    int _return = 0;
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
		
		String zipped = (String)variables.get("z");
		boolean unzip= true;
		if (zipped!=null)
		{
		   if (zipped.equalsIgnoreCase("true"))
		   {
		       unzip=false;
		   }
		}
		
        String user = (String)variables.get("username");
        String pass = (String)variables.get("password");

        try {
	        ArrayList sessions = (ArrayList)variables.get("session_id");
	        if (sessions == null || sessions.size()==0)
	        {
	            String file = (String)variables.get("f");
	            if (file==null)
	            {
                    if (variables.get("xml")!=null)
                    {
                        processPartialDoc((String)variables.get("xml"),user,pass);
                        return;
                    }else{
                        System.out.println("Missing required fields -s or -f.");
                        showUsage();
                        System.exit(1);
                        return;
                    }
	            }else{
	                File sessionFile = new File(file);
	                if (sessionFile.exists())
	                {
	                    sessions = FileUtils.FileLinesToArrayList(sessionFile);

	        	        if (sessions.size()==0){
	        	            System.out.println("Unable to load session ids from file: " + f.getAbsolutePath());
		            		System.exit(1);
		            		return;
	        	        }
	                }else{
	                    System.out.println("Unable to access file: " + f.getAbsolutePath());
	            		System.exit(1);
	            		return;
	                }
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
            String service_session = (String)call.invoke(params);
            
            for (int i=0;i<sessions.size();i++)
            {
                String session_id = (String)sessions.get(i);
                int counter =0;
                File outFile = new File(dir + session_id + ".zip");
                while (outFile.exists())
                {
                    outFile = new File(dir + session_id+"_" + counter++ + ".zip");
                }
            	
            	try {
                    System.out.println("Requesting data for " + session_id + "");
            	    long startTime = Calendar.getInstance().getTimeInMillis();
                    URL url = new URL(this.getSiteURL() + "app/template/ArcGet.vm/session/" + service_session + "/id/" + session_id);
//   			 Use Buffered Stream for reading/writing.
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
                    
                    if (unzip)
                    {
                        long serviceDuration = Calendar.getInstance().getTimeInMillis() - startTime;
                        System.out.println("Zipped Archive Received (" + serviceDuration + " ms)");
                        
                        System.out.println("Unzipping Archive");
                        ZipUtils.Unzip(outFile);
                        serviceDuration = Calendar.getInstance().getTimeInMillis() - startTime;
                        System.out.println("Archive Loaded (" + serviceDuration + " ms): " + dir + session_id);
                    }else{
                        long serviceDuration = Calendar.getInstance().getTimeInMillis() - startTime;
                        System.out.println("Zipped Archive Received (" + serviceDuration + " ms) : "+ outFile.getAbsolutePath());
                    }
                    
                } catch (MalformedURLException e) {
                    System.out.println("Error retrieving data for " + session_id);
                    System.out.println(e.getMessage());
                } catch (FileNotFoundException e) {
                    System.out.println("Error retrieving data for " + session_id);
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    System.out.println("Error retrieving data for " + session_id);
                    System.out.println(e.getMessage());
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
            ex.printStackTrace();
			_return= 11;
        } catch (Exception ex) {
            ex.printStackTrace();
			_return= 11;
        }
		System.exit(_return);
    }
    
    public void processPartialDoc(String fileLocation, String user, String pass) throws Exception
    {
        File f = new File(fileLocation);
        if (!f.exists())
        {
            throw new java.io.FileNotFoundException(fileLocation);
        }
        
        String xml = FileUtils.GetContents(f);
        
        Service service = new Service();
        
        //REQUEST SESSION ID
        Call call = (Call)service.createCall();
        call.setUsername(user);
        call.setPassword(pass);
        URL requestSessionURL = new URL(this.getSiteURL() + "axis/ArcGet.jws");
        call.setTargetEndpointAddress(requestSessionURL);
        call.setOperationName("fromXML");
        
        Object[] params = {xml};  
        String service_session = (String)call.invoke(params);
        MessageContext context =call.getMessageContext();
        Message rspMessage = context.getResponseMessage();
        Iterator attachments = rspMessage.getAttachments();
        while (attachments.hasNext())
        {
            AttachmentPart part = (AttachmentPart)attachments.next();
            DataHandler handler = part.getDataHandler();
            File out = new File(part.getContentId() + ".zip");
            FileOutputStream fos = new FileOutputStream(out);
            handler.writeTo(fos);
        }
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#definePossibleVariables()
     */
    public void definePossibleVariables() {
        addPossibleVariable("session_id","MR Session ID for the desired session(s)\nFor multiple sessions, use multiple -s tags.",new String[]{"s","session_id"},false,true);
        addPossibleVariable("f","File containing MRSession IDs seperated by line breaks.",new String[]{"f","file_name"},false);
        addPossibleVariable("dir","Directory to store files.",new String[]{"o","dir_name"});
        addPossibleVariable("z","Bundles each requested session in a zipped file.",new String[]{"z","zip"});
        addPossibleVariable("xml","Path to Partial XML document",new String[]{"xml"});
    }

	public String getService(){
	  return "axis/CreateServiceSession.jws";
	}
}
