/*
 * org.nrg.xnat.services.ArcGetService
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.services;

import org.apache.log4j.Logger;

/**
 * @author timo
 *
 */
public class ArcGetService {
static org.apache.log4j.Logger logger = Logger.getLogger(ArcGetService.class);
//public String search(String _id, ArrayList rawScanTypes,ArrayList processedScanTypes,String zippedFileType) throws RemoteException
//{
//    String _username= AxisEngine.getCurrentMessageContext().getUsername();
//    String _password= AxisEngine.getCurrentMessageContext().getPassword();
//    AccessLogger.LogServiceAccess(_username,"","ArcGetService",_id);
//    try {
//        XDATUser user = new XDATUser(_username,_password);
//        if (user == null)
//        {
//            throw new Exception("Invalid User: "+_username);
//        }
//        return search(user,_id,rawScanTypes,processedScanTypes,zippedFileType);
//    } catch (RemoteException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (XFTInitException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (ElementNotFoundException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (DBPoolException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (SQLException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (FieldNotFoundException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (FailedLoginException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (Exception e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    }
//}
//
//public String search(String session_id,String _id, ArrayList rawScanTypes,ArrayList processedScanTypes,String zippedFileType) throws RemoteException
//{
//    AccessLogger.LogServiceAccess(session_id,"","ArcGetService",_id);
//    try {
//        XDATUser user = UserCache.GetUser(session_id);
//        if (user == null)
//        {
//            throw new Exception("Invalid Session: "+session_id);
//        }
//        return search(user,_id,rawScanTypes,processedScanTypes,zippedFileType);
//    } catch (RemoteException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (XFTInitException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (ElementNotFoundException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (DBPoolException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (SQLException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (FieldNotFoundException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (FailedLoginException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (Exception e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    }
//}
//
//private String search(XDATUser user,String id, ArrayList rawScanTypes,ArrayList processedScanTypes,String zippedFileType) throws RemoteException
//{
//    try {
//        ArrayList al = XnatMrsessiondata.getXnatMrsessiondatasByField("xnat:mrSessionData.ID",id,user,true);
//        if (al.size()== 0)
//        {
//            throw new Exception("Unknown MR Session: " + id);
//        }
//
//        XnatMrsessiondata mr = (XnatMrsessiondata)al.get(0);
//        long startTime = Calendar.getInstance().getTimeInMillis();
//        
//        String service_session = UserCache.CreateUserSession(user);
//        String urlString = TurbineUtils.GetServer() + "/app/template/ArcGet.vm/session/" + service_session + "/id/" + id;
//        System.out.println(urlString);
//        URL url = new URL(urlString);
////        OutputStream outStream = null;
////        File temp = File.createTempFile("xnat","zip");
////        outStream = new BufferedOutputStream(new FileOutputStream(temp));
////        System.out.println("Creating Temporary File: " + temp.getAbsolutePath());
////       
////        String archive = mr.getArchivePath();
////        File archiveF = new File(archive);
////        if (archiveF.exists())
////        {
////            mr.loadFiles();
////            Hashtable files = mr.getFileTracker().createHash(mr.getId());
////            
////            ZipI zip = null;
////            if (zippedFileType.equals("zip")){
////                zip = new ZipUtils();
////            }else{
////                zip = new TarUtils();
////                outStream = new GZIPOutputStream(outStream);
////            }
////            zip.setOutputStream(outStream);
////            Enumeration enumer = files.keys();
////            while(enumer.hasMoreElements())
////            {
////                  String key = (String)enumer.nextElement();
////                  File f = new File((String)files.get(key));
////                  if (!f.isDirectory())
////                  {
////                      zip.write(key,f);
////                  }
////            }
////              
////            // Complete the ZIP file
////            zip.close();
////        }
////        
////        outStream.close();
////        long serviceDuration = Calendar.getInstance().getTimeInMillis() - startTime;
////        System.out.println("Temp File Complete (" + serviceDuration + " ms). ");
//        
//        MessageContext mContext = AxisEngine.getCurrentMessageContext();
//		Message rspmsg =mContext.getResponseMessage();
//		
//		rspmsg.getAttachmentsImpl().setSendType(org.apache.axis.attachments.Attachments.SEND_TYPE_DIME);
//		
//		DataHandler dh = new DataHandler(url);
////		DataHandler dh = new DataHandler(new FileDataSource(temp));
//		if (dh == null ) System.err.println("dhSource is null");
//		
////		temp.deleteOnExit();
//				
//		AttachmentPart ap = new AttachmentPart(dh);
//		ap.setContentId("File1");
//		
//		MessageContext context=MessageContext.getCurrentContext();
//		Message responseMessage=context.getResponseMessage();
//		
//		responseMessage.addAttachmentPart(ap);
//        
//		//String summary = mr.output();
//        return "Loading " +id + ".";
//    } catch (Exception e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    }
//}
//
//private DataHandler fromXML(String _session, String xml)throws RemoteException{
//    AccessLogger.LogServiceAccess(_session,"","ArcGetService (FROM XML)","");
//    try {
//        XDATUser user = UserCache.GetUser(_session);
//        if (user == null)
//        {
//            throw new Exception("Invalid Session: "+_session);
//        }
//        return fromXML(user,xml);
//    } catch (RemoteException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (XFTInitException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (ElementNotFoundException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (DBPoolException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (SQLException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (FieldNotFoundException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (FailedLoginException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (Exception e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    }
//}
//private DataHandler fromXML(String xml)throws RemoteException{
//String _username= AxisEngine.getCurrentMessageContext().getUsername();
//String _password= AxisEngine.getCurrentMessageContext().getPassword();
//AccessLogger.LogServiceAccess(_username,"","ArcGetService","");
//try {
//    XDATUser user = new XDATUser(_username,_password);
//    if (user == null)
//    {
//        throw new Exception("Invalid User: "+_username);
//    }
//    return fromXML(user,xml);
//} catch (RemoteException e) {
//    logger.error("",e);
//    throw new RemoteException("",e);
//} catch (XFTInitException e) {
//    logger.error("",e);
//    throw new RemoteException("",e);
//} catch (ElementNotFoundException e) {
//    logger.error("",e);
//    throw new RemoteException("",e);
//} catch (DBPoolException e) {
//    logger.error("",e);
//    throw new RemoteException("",e);
//} catch (SQLException e) {
//    logger.error("",e);
//    throw new RemoteException("",e);
//} catch (FieldNotFoundException e) {
//    logger.error("",e);
//    throw new RemoteException("",e);
//} catch (FailedLoginException e) {
//    logger.error("",e);
//    throw new RemoteException("",e);
//} catch (Exception e) {
//    logger.error("",e);
//    throw new RemoteException("",e);
//}
//}
//
//private DataHandler fromXML(XDATUser user, String xml) throws RemoteException{
//    SAXReader reader = new SAXReader(user);
//    try {
//        org.nrg.xft.XFTItem item = reader.parse(new StringReader(xml));
//        if (item != null)
//        {
//            item.setPreLoaded(true);
//            ItemI om = org.nrg.xdat.base.BaseElement.GetGeneratedItem(item);
//            if (om instanceof XnatMrsessiondata)
//            {
//                XnatMrsessiondata mr = (XnatMrsessiondata)om;
//                
//                File f = File.createTempFile("arc-get.",".zip");
//                try {
//                    FileOutputStream fos = new FileOutputStream(f);
//                    ZipI zip = new ZipUtils();
//                    int compressionMethod = ZipOutputStream.STORED;
//                    zip.setOutputStream(fos,compressionMethod);
//        
//                    File temp = File.createTempFile("arc-get.log.","");
//                    FileWriter fw = new FileWriter(temp);
//                               
//                    fw.write(mr.getId() +" Archive Download Summary \n");
//                    int successful = 0;
//                    int failed = 0;
//                    if (mr.hasSRBData()){
//                        mr.loadSRBFiles();
//                        
//                        ArrayList files = new ArrayList();
//                            XNATDirectory srbDIR = mr.getSRBDirectory();
//                            files.add(srbDIR);
//                        
//
//                        File tempFile = File.createTempFile("srbTransfer", "");
//                        tempFile.delete();
//                        File tempSession = new File(tempFile.getAbsolutePath() + File.separator + mr.getId());
//                        tempSession.mkdirs();
//                        
//                        Iterator enumer = files.iterator();
//                        while(enumer.hasNext())
//                        {
//                            XNATDirectory key = (XNATDirectory)enumer.next();
//                            key.importFiles(tempSession);
//                        }
//                        
//
//                        try {
//                            zip.writeDirectory(tempSession);
//                            successful++;
//                        } catch (Throwable e1) {
//                            logger.error("",e1);
//                            throw new RemoteException("",e1);
//                        }
//                        
//                        fw.write("\nFiles loaded successfully.\n");
//                        if (failed>0)
//                        {
//                            fw.write(failed +" file(s) failed.\n");
//                        }
//                        fw.flush();
//                        fw.close();
//                        
//                        zip.write("README.txt",temp);
//                        
//                        // Complete the ZIP file
//                        zip.close();
//
//                        tempFile.delete();
//                    }else{
//                        mr.loadLocalFiles(false);
//                        Hashtable files = new Hashtable();
//                        files = mr.getFileTracker().createHash(mr.getId());
//                                          
//                        
//                        //ZipUtils.ZipToStream(response.getOutputStream(),files);
//                        Enumeration enumer = files.keys();
//                        while(enumer.hasMoreElements())
//                        {
//                              String key = (String)enumer.nextElement();
//                              try {
//                                File t = new File((String)files.get(key));
//                                  if (!t.isDirectory())
//                                  {
//                                      zip.write(key,t);
//                                  }
//                                  successful++;
//                            } catch (FileNotFoundException e) {
//                                failed++;
//                                fw.write("cannot access " +key + ": " + e.getMessage() +"\n");
//                            } catch (IOException e) {
//                                failed++;
//                                fw.write("cannot access " +key + ": " + e.getMessage() +"\n");
//                            } catch (RuntimeException e) {
//                                failed++;
//                                fw.write("cannot access " +key + ": " + e.getMessage() +"\n");
//                            }
//                        }
//                        
//                        fw.write("\n" + successful +" file(s) loaded successfully.\n");
//                        if (failed>0)
//                        {
//                            fw.write(failed +" file(s) failed.\n");
//                        }
//                        fw.flush();
//                        fw.close();
//                        
//                        zip.write("README.txt",temp);
//                        
//                        // Complete the ZIP file
//                        zip.close();
//                     }
//                    fos.flush();
//                    fos.close();
//                    temp.delete();
//                    
//                    //MessageContext mContext = AxisEngine.getCurrentMessageContext();
//                    //Message rspmsg =mContext.getResponseMessage();
//                    //AttachmentPart a = rspmsg.createAttachmentPart();
//                    //rspmsg.getAttachmentsImpl().setSendType(org.apache.axis.attachments.Attachments.SEND_TYPE_DIME);
//                    DataHandler dh = new DataHandler(new FileDataSource(f));
//                    if (dh == null ) System.err.println("dhSource is null");
//                    //AttachmentPart ap = new AttachmentPart(dh);
//                    //ap.setContentId("File1");
//                    
////                    MessageContext context=MessageContext.getCurrentContext();
////                    Message responseMessage=context.getResponseMessage();
//                    
//                    //rspmsg.addAttachmentPart(ap);
//                    System.out.println("Created file " + f.getAbsolutePath()  + " " + f.length());
//                   // f.delete();
//                    return dh;
//                    //return "File Created";
//                } catch (IOException e) {
//                    logger.error("",e);
//                    throw new RemoteException("",e);
//                }
//            }
//        }
//    } catch (IOException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    } catch (SAXException e) {
//        logger.error("",e);
//        throw new RemoteException("",e);
//    }
//    
//    return null;
//}
//
//public static DataHandler FromXML(String xml) throws RemoteException
//{
//    return (new ArcGetService()).fromXML(xml);
//}
//
//public static DataHandler FromXML(String _session,String xml) throws RemoteException
//{
//    return (new ArcGetService()).fromXML(_session,xml);
//}
//
//
//public static String Search(String _id,String zippedFileType) throws RemoteException
//{
//    return (new ArcGetService()).search(_id,null,null,zippedFileType);
//}
//
//public static String Search(String _id, ArrayList rawScanTypes,ArrayList processedScanTypes,String zippedFileType) throws RemoteException
//{
//    return (new ArcGetService()).search(_id,rawScanTypes,processedScanTypes,zippedFileType);
//}
//
//public static String Search(String _session,String _id, ArrayList rawScanTypes,ArrayList processedScanTypes,String zippedFileType) throws RemoteException
//{
//    return (new ArcGetService()).search(_session,_id,rawScanTypes,processedScanTypes,zippedFileType);
//}
}
