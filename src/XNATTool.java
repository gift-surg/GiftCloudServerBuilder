import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * XNATTool
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */



public class XNATTool{
    String host=null;
    String u=null;
    String p=null;
    String session=null;
    
    public XNATTool(String host,String user,String pass) throws ServiceException,MalformedURLException, RemoteException{
        this.host=host;
        if (!this.host.endsWith("/"))
        {
            this.host += "/";
        }
        u=user;
        p=pass;
        
        open();
    }
    
    /**
     * @param map
     * @return
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     */
    public String getSubjectIdForMap(String map) throws ServiceException,MalformedURLException, RemoteException     {
        URL url = new URL(host + "axis/GetIdentifiers.jws");
        Service service = new Service();
        Call call = (Call)service.createCall();
        call.setTargetEndpointAddress(url);
        
        call.setOperationName("search");
        
        String user = u;
        String pass = p;
        String field ="cnda:cndaSubjectMetadata.map";
        
        String comparison = "=";
        
        String value = map;
        
        String dataType = "xnat:subjectData";
        Object[] params=new Object[]{session,field,comparison,value,dataType};

        System.out.println("Requesting matching IDs...");
        long startTime = Calendar.getInstance().getTimeInMillis();
        Object[] o = (Object[])call.invoke(params);
        long duration = Calendar.getInstance().getTimeInMillis() - startTime;
        System.out.println("Response Received (" + duration + " ms)");

        System.out.println(o.length + " Matching Item(s) Found.");
        
        if (o.length==0)
        {
            return null;
        }else{
            return o[0].toString();
        }
    }

    /**
     * @param map
     * @param dir
     * @return
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public File getSubjectXMLForMap(String map, String dir) throws ServiceException,MalformedURLException, RemoteException, FileNotFoundException,IOException ,SAXException,ParserConfigurationException   {
        
        
        String id= getSubjectIdForMap(map);
        return writeXMLToFile(id, "xnat:subjectData", dir, false);
    }
    
    private void open()  throws ServiceException,MalformedURLException, RemoteException{
        session = createServiceSession();
    }
    
    public void close() throws ServiceException,MalformedURLException, RemoteException{
        closeServiceSession(session);
    }
    


    /**
     * @return Session id for use in subsequent requests.
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     */
    public String createServiceSession() throws ServiceException,
            MalformedURLException, RemoteException {
        Service service = new Service();
        Call call = (Call) service.createCall();

        //  REQUEST SESSION ID
        call.setUsername(u);
        call.setPassword(p);
        URL requestSessionURL = new URL(host + "axis/CreateServiceSession.jws");
        call.setTargetEndpointAddress(requestSessionURL);
        call.setOperationName("execute");
        Object[] params = {};
        return (String) call.invoke(params);
    }

    /**
     * @return Session id for use in subsequent requests.
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     */
    public void closeServiceSession(String service_session)
            throws ServiceException, MalformedURLException, RemoteException {
        if (service_session != null) {
            Service service = new Service();
            Call call = (Call) service.createCall();
            URL requestSessionURL = new URL(host
                    + "axis/CloseServiceSession.jws");
            call.setTargetEndpointAddress(requestSessionURL);
            call.setOperationName("execute");
            Object[] params = new Object[] { service_session };
            call.invoke(params);
        }
    }

    
    /**
     * @param host
     * @param service_session
     * @param id
     * @param dataType
     * @param dir
     * @param quiet
     * @param outputStream to write content to
     * @throws FileNotFoundException
     * @throws MalformedURLException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * Retrieves XML from specified host and writes it to passed output stream.
     */
    public static void writeXMLtoOS(String host, String service_session,Object id, String dataType, String dir, boolean quiet,OutputStream out)throws FileNotFoundException, MalformedURLException, IOException, SAXException, ParserConfigurationException{
        
            if (!quiet)System.out.println("Requesting xml for " + id + "");
            long startTime = Calendar.getInstance().getTimeInMillis();
            URL url = new URL(host + "app/template/XMLSearch.vm/session/" + service_session + "/id/" + id + "/data_type/" + dataType);
//       Use Buffered Stream for reading/writing.
            BufferedInputStream  bis = null; 
            BufferedOutputStream bos = null;
            

            bis = new BufferedInputStream(url.openStream());
            bos = new BufferedOutputStream(out);

            byte[] buff = new byte[2048];
            int bytesRead;
            
            while(-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
                bos.write(buff, 0, bytesRead);

            }
            
            bos.flush();
            
            if (!quiet)System.out.println("Response Received (" + (Calendar.getInstance().getTimeInMillis() - startTime) + " ms)");

    }
    
    public String[] getNewSubjectIds(Integer num) throws ServiceException,MalformedURLException, RemoteException
    {
        URL url = new URL(host + "axis/RequestSubjectId.jws");
        Service service = new Service();
        Call call = (Call)service.createCall();
        call.setTargetEndpointAddress(url);
        
        call.setOperationName("execute");
        
        Object[] params=new Object[]{num};

        System.out.println("Requesting IDs...");
        long startTime = Calendar.getInstance().getTimeInMillis();
        String[] o = (String[])call.invoke(params);
        long duration = Calendar.getInstance().getTimeInMillis() - startTime;
        System.out.println("Response Received (" + duration + " ms)");

        System.out.println(o.length + " IDs returned.");
        
        return o;
    }   
   

    /**
     * @param host
     * @param service_session
     * @param id
     * @param dataType
     * @param dir_path Output directory
     * @param quiet
     * @return Path to created XML File
     * @throws FileNotFoundException
     * @throws MalformedURLException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public File writeXMLToFile(Object id, String dataType, String dir_path, boolean quiet)throws FileNotFoundException, MalformedURLException, IOException, SAXException, ParserConfigurationException{
        int counter = 0;
        String finalName = id + ".xml";
        File outFile = new File(dir_path,finalName);
        while(outFile.exists())
        {
            finalName = id + "_v" + (counter++) + ".xml";
            outFile = new File(dir_path,finalName);
        }
        FileOutputStream out = new FileOutputStream(outFile);
        
        writeXMLtoOS(host, session, id, dataType, dir_path, quiet, out);
            
            out.close();
            
            parse(outFile);
        
            return outFile;
    }
    
    public static void parse(java.io.File data) throws IOException, SAXException, ParserConfigurationException{
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        
        //get a new instance of parser
        SAXParser sp = spf.newSAXParser();
        //parse the file and also register this class for call backs
        sp.parse(data,new DefaultHandler());

        
        return ;
    }

    
    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            XNATTool tool = new XNATTool("http://cninds03:8070/cnda_xnat/","tolsen","mysql");
         
            String[] ids = tool.getNewSubjectIds(new Integer(10));
                        
            String id = tool.getSubjectIdForMap("12345");
            System.out.println(id);
            
            try {
                File xml = tool.getSubjectXMLForMap("12345", ".");
                System.out.println(xml.getPath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            
            tool.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }
}
