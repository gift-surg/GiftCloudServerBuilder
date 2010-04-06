//Copyright Washington University School of Medicine All Rights Reserved
/*
 * Created on Apr 2, 2007
 *
 */
package org.nrg.xnat.turbine.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.nrg.PrearcImporter;
import org.nrg.StatusListener;
import org.nrg.StatusMessage;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xnat.archive.PrearcImporterFactory;

public class ImageUploadHelper {
    static org.apache.log4j.Logger logger = Logger.getLogger(ImageUploadHelper.class);
    private final String uploadID;
    private final HttpSession session;
    private final String project;
    
    private Map<String,Object> hash = new Hashtable<String,Object>();
    
    public ImageUploadHelper(final String upload_identifier, final HttpSession http_session, final String project)
    {
        uploadID = upload_identifier;
        session = http_session;
        this.project = project;
    }
    
    public void addBatchVariable(String xmlPath, Object value)
    {
        hash.put(xmlPath, value);
    }
    
    public Map getBatchVariables()
    {
        return hash;
    }
    
    public void clearBatchVariables()
    {
        hash =new Hashtable<String,Object>();
    }
    
    public HelperResults run(File src, File dest)
    {        
        HelperResults results=new HelperResults();
        results.listener = null;
        final PrearcImporter pw = PrearcImporterFactory.getFactory().getPrearcImporter(project, dest, src);
        if (uploadID!=null){
            session.setAttribute(uploadID + "status", new ArrayList());
            results.listener= new PrearcListener(session,uploadID + "status");
        }else{
        	results.listener= new PrearcListener(null,null);
        }
        pw.addStatusListener(results.listener);
        pw.run();
        
        System.out.println("Done with PrearcImporter.");
        
        results.sessions=pw.getSessions();
        
        for(File f : results.sessions){
            if (f.isDirectory())
            {
                String s = f.getAbsolutePath() + ".xml";
                File xml = new File(s);
                if (xml.exists())
                {
                	results.listener.addMessage("PROCESSING", "Setting security field(s) for '" + f.getName() + "'");
                    SAXReader reader = new SAXReader(null);
                    try {   
                        org.nrg.xft.XFTItem item = reader.parse(s);
                        for (Map.Entry<String, Object> entry :hash.entrySet())
                        {
                            try {
                                item.setProperty(entry.getKey(), entry.getValue());
                            } catch (Throwable e) {
                                logger.error("",e);
                                results.listener.addMessage("FAILED", "failed to set appropriate field for '" + f.getName() + "'.  Data may be publicly accessible until archived.");
                            }
                        }
                                             
                        FileOutputStream fos=new FileOutputStream(xml);
                        OutputStreamWriter fw;
            			try {
            				FileLock fl=fos.getChannel().lock();
            				try{
            					fw = new OutputStreamWriter(fos);
                                item.toXML(fos,false);
            					fw.flush();
            				}finally{
            					fl.release();
            				}
            			}finally{
            				fos.close();
            			}
                    } catch (Throwable e) {
                        logger.error("",e);
                        results.listener.addMessage("FAILED", "failed to set appropriate field(s) for '" + f.getName() + "'.  Data may be publicly accessible until archived.");
                    }
                }else{
                	results.listener.addMessage("FAILED", "failed to load generated xml file ('" + xml.getName() + "').  Data may be publicly accessible until archived.");
                }
            }
        }

        
        for (String[] s : results.listener.messages){
            if (s[0].equals("FAILED")){
                logger.error(s[1]);
            }else{
                logger.info(s[1]);
            }
        }
        
        return results;
    }
    
    public class HelperResults{
    	public Collection<File> sessions=null;
    	public PrearcListener listener=null;
    }
    
    public class PrearcListener implements StatusListener{
        final HttpSession session;
        final String sessionAttribute;
        final ArrayList<String[]> messages = new ArrayList<String[]>();
        
        public PrearcListener(HttpSession s, String sa)
        {
            session = s;
            sessionAttribute = sa;
        }
        
        public void notify(StatusMessage sm){
//            Object src = sm.getSource();
            String message = sm.getMessage();
            StatusMessage.Status status = sm.getStatus();
            
            String text = "";
//            if (src instanceof File){
//                text += ((File)src).getName() + " ";
//            }
            text +=message;
            
            if (status.equals(StatusMessage.Status.COMPLETED)){
                messages.add(new String[]{"COMPLETED",text});
            }else if (status.equals(StatusMessage.Status.PROCESSING)){
                messages.add(new String[]{"PROCESSING",text});
            }else if (status.equals(StatusMessage.Status.WARNING)){
                messages.add(new String[]{"WARNING",text});
            }else if (status.equals(StatusMessage.Status.FAILED)){
                messages.add(new String[]{"FAILED",text});
            }else{
                messages.add(new String[]{"UNKNOWN",text});
            }
            
            if (session !=null){
                session.setAttribute(sessionAttribute, messages);
            }
            
        
        }
        
        public ArrayList<String[]> getMessages(){
            return messages;
        }
        
        public void addMessage(String level, String message){
            messages.add(new String[]{level,message});
        }
    };
}
