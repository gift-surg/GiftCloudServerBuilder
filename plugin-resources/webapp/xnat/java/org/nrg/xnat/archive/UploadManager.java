/*
 * org.nrg.xnat.archive.UploadManager
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.archive;

import org.apache.log4j.Logger;
import org.nrg.xdat.security.XDATUser;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class UploadManager extends Thread{
    static Logger logger = Logger.getLogger(UploadManager.class);
    private String projectID = null;
    private ArrayList<File> files = new ArrayList<File>();
    private XDATUser user = null;
    private String outputDir = null;
    private String server = null;

    public UploadManager(XDATUser user, String project, String outputPath, String server)
    {
        this.user=user;
        projectID= project;
        outputDir=outputPath;
        if (server.trim().endsWith(":80"))
        {
            server = server.trim().substring(0,server.trim().indexOf(":80"));
        }
        this.server=server;
    }
    
    public void addFile(File file)
    {
        files.add(file);
    }
    
    public int size(){
        return files.size();
    }

    public void execute(){
        Date d = Calendar.getInstance().getTime();
//        ArrayList<String> successful = new ArrayList<String>();
//        ArrayList<ArrayList> error = new ArrayList<ArrayList>();
//        File prearc = new File(outputDir);
//        
//        for(File f : files){
//            System.out.println("Upload Manager: Starting " + f.getName());
//            String compression_method = ".zip";
//            String extension= null;
//            if (f.getName().indexOf(".")!=-1){
//                extension = f.getName().substring(f.getName().lastIndexOf("."));
//                compression_method = extension;
//            }
//            
//            
//            if (extension.equalsIgnoreCase(".zip") 
//                    || extension.equalsIgnoreCase(".tar") 
//                    || extension.equalsIgnoreCase(".gz")){
//                
//                File tempFile = null;
//                try {
//                    tempFile = File.createTempFile("extraction", "");
//                    tempFile.delete();
//                    ZipI zipper = null;
//                    if (compression_method.equalsIgnoreCase(".tar")){
//                        zipper = new TarUtils();
//                    }else if (compression_method.equalsIgnoreCase(".gz")){
//                        zipper = new TarUtils();
//                        zipper.setCompressionMethod(ZipOutputStream.DEFLATED);
//                    }else{
//                        zipper = new ZipUtils();
//                    }
//                                
//                    zipper.extract(f,tempFile.getAbsolutePath() + File.separator,true);
//                } catch (Throwable e) {
//                    System.out.println("Extraction Failed. " + e.getMessage());
//                    logger.error("",e);
//                    ArrayList al = new ArrayList();
//                    al.add(f.getName());
//                    al.add("Extraction failed.  Please verify that your file is compressed into either a .zip or .tar.gz file and is uncorrupt.  Then, re-upload the file.");
//                    error.add(al);
//                    System.out.println("Upload Manager: Error " + f.getName());
//                    continue;
//                }finally{
//                    //f.delete();
//                }
//                
//                final String[] command = {"archiveIma"};   // or maybe {"/usr/bin/perl", "/data/cninds01/data2/arc-tools/archiveIma"}
//
//                final String[] env = {"LD_LIBRARY_PATH=/usr/local/lib"};
//
//
//                final PrearcImporter pw = new PrearcImporter(tempFile, prearc, command, env);
//
//                pw.run();
//                
//                
//                FileUtils.DeleteFile(tempFile);
//            }else{
//
//                File[] listFiles = f.listFiles();
//                for(int i=0;i<listFiles.length;i++)
//                {
//                    File child = listFiles[i];
//                    if (child.isDirectory())
//                    {
//                        File newDir = new File(prearc.getAbsolutePath() + File.separator + child.getName());
//                        while (newDir.exists())
//                        {
//                            newDir = new File(newDir.getAbsolutePath() + "_1");
//                        }
//                        try {
//                            FileUtils.MoveDir(child, newDir , true);
//                        } catch (FileNotFoundException e) {
//                            logger.error("",e);
//                            continue;
//                        } catch (IOException e) {
//                            logger.error("",e);
//                            continue;
//                        }
//                    }else{
//                        String fileToFolder = child.getName();
//                        if (fileToFolder.indexOf(".")!=-1)
//                        {
//                            fileToFolder = fileToFolder.substring(0, fileToFolder.indexOf("."));
//                        }
//                        
//                        File images = new File(prearc.getAbsolutePath() + File.separator + fileToFolder);
//
//                        while (images.exists())
//                        {
//                            images = new File(images.getAbsolutePath() + "_1");
//                        }
//                        
//                        images.mkdirs();
//                        try {
//                            FileUtils.MoveFile(child, new File(images.getAbsolutePath() + File.separator + child.getName()), true);
//                        } catch (FileNotFoundException e) {
//                            logger.error("",e);
//                            continue;
//                        } catch (IOException e) {
//                            logger.error("",e);
//                            continue;
//                        }
//                    }
//                }
//            }
//            
//
//
//            
//
//            successful.add(f.getName());
//            System.out.println("Upload Manager: Ending " + f.getName());
//            FileUtils.DeleteFile(f);
//        }
//
//        for (int i=0;i<prearc.listFiles().length;i++){
//            File child = prearc.listFiles()[i];
//            if (child.isDirectory()){
//                XNATSessionPopulater populater = new XNATSessionPopulater(child);
//                if (projectID.equalsIgnoreCase("DICOM"))
//                {
//                    try {
//                        XnatMrsessiondata mrNEW = populater.populateMR(user);
//                        
//                        if (mrNEW.getDcmpatientname()!=null)
//                        {
//                            mrNEW.setId(mrNEW.getDcmpatientname());
//                        }
//                        mrNEW.setSubjectId("NULL");
//                        mrNEW.fixScanTypes();
//                        
//                        
//                        File xml = new File(prearc.getAbsolutePath() + File.separator + child.getName() + ".xml");
//                        FileOutputStream fos = new FileOutputStream(xml);
//                        
//                        mrNEW.toXML(fos,false);
//                        
//                        fos.close();
//                    } catch (IOException e) {
//                        logger.error(e);
//                        continue;
//                    }catch(Throwable e){
//                        logger.error(e);
//                        continue;
//                    }
//                }else if (projectID.equals("ECAT")) {
//                    try {
//                        XnatPetsessiondata mrNEW = populater.populatePET(user);
//                        //XnatPetsessiondata mrNEW = new XnatPetsessiondata(user);
//                        //mrNEW.setId("TEST");
//                        
//                        if (mrNEW.getStudytype()!=null)
//                        {
//                            mrNEW.setId(mrNEW.getStudytype());
//                        }
//                        
//                        File xml = new File(prearc.getAbsolutePath() + File.separator + child.getName() + ".xml");
//                        FileOutputStream fos = new FileOutputStream(xml);
//                        
//                        mrNEW.toXML(fos,false);
//                        
//                        fos.close();
//                    } catch (IOException e) {
//                        logger.error(e);
//                        continue;
//                    }catch(Throwable e){
//                        logger.error(e);
//                        continue;
//                    }
//                }
//            }
//            
//        }
//
//        String message = null;
//        try {
//            
//            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat ("MM/dd/yyyy HH:mm:ss");
//            ArrayList<InternetAddress> to = new ArrayList();
//            to.add(new InternetAddress(AdminUtils.getAdminEmailId()));
//            to.add(new InternetAddress(user.getEmail()));
//            String from = AdminUtils.getAdminEmailId();
//            String subject = "Upload Completed";
//
//            VelocityContext context = new VelocityContext();
//            context.put("date",formatter.format(d));
//            context.put("successfuls",successful);
//            context.put("project",projectID);
//            context.put("server",server);
//            context.put("errors",error);
//            StringWriter sw = new StringWriter();
//            Template template =Velocity.getTemplate("/screens/BatchUploadEmail.vm");
//            template.merge(context,sw);
//            AdminUtils.sendEmail(to, from, subject, sw.toString());
//        } catch (Exception e) {
//            logger.error("",e);
//            StringBuffer sb = new StringBuffer();
//            sb.append("Uploads Completed.<BR>Email to user failed.<BR>");
//            sb.append(e.getMessage());
//            sb.append("<br><br>TEXT:<br>");
//            sb.append(message);
//            AdminUtils.sendAdminEmail((XDATUser)user,"Uploads Completed. Email to user failed.",sb.toString());
//        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        super.run();
        try {
            execute();
        } catch (Exception e) {
            logger.error("",e);
        }
    }
}
