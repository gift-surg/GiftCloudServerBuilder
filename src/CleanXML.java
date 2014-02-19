import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * CleanXML
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
public class CleanXML {

    /**
     * 
     */
    public CleanXML() {
        super();
    }
    
    public static void process(List argsAL){
        int index = argsAL.indexOf("-dir");
        String directory = null;
        String file =null;
        if (index==-1)
        {
            index = argsAL.indexOf("-file");
            if (index==-1)
            {
               System.err.println("No directory or file supplied. (-file or -dir)");
               return;
            }else{
                file = (String)argsAL.get(index+1);
            }
        }else{
            directory = (String)argsAL.get(index+1);
        }
        String appDir = "C:\\xdat\\deployments\\cnda_xnat";
        
        try {            
            
            boolean removeNotes=false;
            if (argsAL.contains("-removeNotes"))
            {
                removeNotes=true;
            } 
            
            boolean removeEmptys=false;
            if (argsAL.contains("-removeEmptyTags"))
            {
                removeEmptys=true;
            }
            
            boolean removeStatus=false;
            if (argsAL.contains("-removeStatus"))
            {
                removeStatus=true;
            }
            
            boolean removeTime=false;
            if (argsAL.contains("-removeTime"))
            {
                removeTime=true;
            }
            
            boolean removeProvenance=false;
            if (argsAL.contains("-removeProvenance"))
            {
                removeProvenance=true;
            }
            
            boolean fixFile=false;
            if (argsAL.contains("-fixFile"))
            {
                fixFile=true;
            }
            
            boolean fixSchemaLocation=false;
            if (argsAL.contains("-fixSchemaLocation"))
            {
                fixSchemaLocation=true;
            }
            
            boolean deleteSchemaLocation=true;
            if (argsAL.contains("-leaveSchemaLocation"))
            {
                deleteSchemaLocation=false;
            }
            
            boolean validate=false;
            if (argsAL.contains("-validate"))
            {
                validate=true;
            }
            
            if (directory==null)
            {
                File f = new File(file);
                if (!f.exists())
                {
                    System.err.println("File (" + file +") Not Found.");
                    return;
                }
                System.out.print("Starting " + f.getAbsolutePath());
                long startTime = Calendar.getInstance().getTimeInMillis();
                String s = FileUtils.GetContents(f);
                if (fixSchemaLocation){
                    s = StringUtils.ReplaceStr(s, "http://cnda.wustl.edu:80/cnda_xnat/schemas", "C:/xdat/deployments/cnda_xnat/src/schemas");
                    if (validate){
                        try {
                            parse(f);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }else if (deleteSchemaLocation){
                    if (validate){
                        try {
                            parse(f);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    int schemaLocatin = s.indexOf("xsi:schemaLocation=\"");
                    if (schemaLocatin>=0)
                    {
                        String before = s.substring(0,schemaLocatin);
                        s= before + s.substring(s.indexOf("\"",schemaLocatin+20)+1);
                    }
                }else if (validate){
                    if (validate){
                        try {
                            parse(f);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
                
                if (fixFile){
                    s = StringUtils.ReplaceStr(s, "xsi:type=\"xnat:imageSeries\"", "xsi:type=\"xnat:imageResourceSeries\"");

                    s = StringUtils.ReplaceStr(s, " fileCount=", " count=");
                    s = StringUtils.ReplaceStr(s, "xsi:type=\"xnat:imageFile\"", "xsi:type=\"xnat:imageResource\"");
                    s = StringUtils.ReplaceStr(s, "xsi:type=\"xnat:file\"", "xsi:type=\"xnat:resource\"");

                    int fileIndex = s.indexOf("<xnat:file ");
                    while (fileIndex != -1){
                       int end= s.indexOf(">", fileIndex);
                       String sub = s.substring(fileIndex, end);
                       
                       if (sub.indexOf("xsi:type")==-1){
                           String start = s.substring(0,fileIndex +11);
                           s= start + " xsi:type=\"xnat:resource\" " + s.substring(fileIndex+11);
                           fileIndex = s.indexOf("<xnat:file ",end);
                       }else{
                           int pattern = sub.indexOf(" pattern=\"");
                           if (pattern==-1){
                               int nOpen= sub.indexOf(" name=\"");
                               if (nOpen!=-1){
                                   int nClose = sub.indexOf('\"',nOpen+7);
                                   String nString= sub.substring(nOpen+7, nClose);

                                   int pOpen= sub.indexOf(" path=\"");
                                   int pClose = sub.indexOf('\"',pOpen+7);
                                   String pString= sub.substring(pOpen+7, pClose);

                                   if (!pString.endsWith("/") && !pString.endsWith("\\")){
                                       pString+="/";
                                   }
                                   
                                   if (pOpen<nOpen){
                                       sub = sub.substring(0,nOpen) + sub.substring(nClose+1);
                                       sub = sub.substring(0,pOpen) + sub.substring(pClose+1);
                                       sub += " URI=\""+ pString + nString + "\"";
                                   }else{
                                       sub = sub.substring(0,pOpen) + sub.substring(pClose+1);
                                       sub = sub.substring(0,nOpen) + sub.substring(nClose+1);
                                       sub += " URI=\""+ pString + nString + "\"";
                                   }
                                   String start = s.substring(0,fileIndex);
                                   String last = s.substring(end);
                                   s = start + sub + last;
                               }
                           }
                           
                           fileIndex = s.indexOf("<xnat:file ",fileIndex +1);
                       }
                    }
                }
                
                if (removeNotes)
                {
                    while (s.indexOf("<xnat:notes>")!=-1)
                    {
                        int startIndex=s.indexOf("<xnat:notes>");
                        
                        int endIndex= s.indexOf("xnat:notes>",startIndex+4)+11;
                        
                        String before= s.substring(0,startIndex);
                        s= before + s.substring(endIndex+1);
                        
                    }
                    
                    s = StringUtils.ReplaceStr(s,"<xnat:note/>","");
                    s = StringUtils.ReplaceStr(s,"<xnat:notes/>","");
                    
                    while (s.indexOf("<xnat:note>")!=-1)
                    {
                        int startIndex=s.indexOf("<xnat:note>");
                        
                        int endIndex= s.indexOf("xnat:note>",startIndex+4)+10;
                        
                        String before= s.substring(0,startIndex);
                        s= before + s.substring(endIndex+1);
                        
                    }
                }
                
                if(removeEmptys){
                    s = StringUtils.ReplaceStr(s,"<xnat:note/>","");
                    s = StringUtils.ReplaceStr(s,"<xnat:notes/>","");
                    s = StringUtils.ReplaceStr(s,"<xnat:provenance/>","");
                    s = StringUtils.ReplaceStr(s," status=\"\"","");
                }
                
                if (removeProvenance)
                {
                    while (s.indexOf("<xnat:provenance>")!=-1)
                    {
                        int startIndex=s.indexOf("<xnat:provenance>");
                        
                        int endIndex= s.indexOf("xnat:provenance>",startIndex+4)+11;
                        
                        String before= s.substring(0,startIndex);
                        s= before + s.substring(endIndex+1);
                        
                    }
                    
                    s = StringUtils.ReplaceStr(s,"<xnat:provenance/>","");
                    
                }
                
                if (removeStatus){
                    s = StringUtils.ReplaceStr(s," status=\"\"","");
                }
                
                if (removeTime)
                {
                    while (s.indexOf("<xnat:time>")!=-1)
                    {
                        int startIndex=s.indexOf("<xnat:time>");
                        
                        int endIndex= s.indexOf("xnat:time>",startIndex+5)+10;
                        
                        String before= s.substring(0,startIndex);
                        s= before + s.substring(endIndex+1);
                        
                    }
                }
                
                FileUtils.OutputToFile(s,f.getAbsolutePath());
                
                org.nrg.xft.utils.XMLUtils.PrettyPrintDOM(f);
               System.out.println(":" + ((float) (Calendar.getInstance().getTimeInMillis()-startTime)/1000) + "s ");
            }else{
                File dir = new File(directory);
                if (!dir.exists())
                {
                    System.err.println("Directory (" + file +") Not Found.");
                    return;
                }
                File[] files = dir.listFiles();
                for(int i=0;i<files.length;i++)
                {
                    File f = files[i];
                    if (f.getName().endsWith(".xml"))
                    {
                        try {
                            System.out.println("Starting " + f.getAbsolutePath());
                            long startTime = Calendar.getInstance().getTimeInMillis();
                            String s = FileUtils.GetContents(f);
                            if (fixSchemaLocation){
                                s = StringUtils.ReplaceStr(s, "http://cnda.wustl.edu:80/cnda_xnat/schemas", "C:/xdat/deployments/cnda_xnat/src/schemas");
                                if (validate){
                                    try {
                                        parse(f);
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                    }
                                }
                            }else if (deleteSchemaLocation){
                                if (validate){
                                    try {
                                        parse(f);
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                    }
                                }
                                int schemaLocatin = s.indexOf("xsi:schemaLocation=\"");
                                if (schemaLocatin>=0)
                                {
                                    String before = s.substring(0,schemaLocatin);
                                    s= before + s.substring(s.indexOf("\"",schemaLocatin+20)+1);
                                }
                            }else if (validate){
                                if (validate){
                                    try {
                                        parse(f);
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                    }
                                }
                            }
                            
                            if (fixFile){
                                s = StringUtils.ReplaceStr(s, "xsi:type=\"xnat:imageSeries\"", "xsi:type=\"xnat:imageResourceSeries\"");

                                s = StringUtils.ReplaceStr(s, " fileCount=", " count=");
                                s = StringUtils.ReplaceStr(s, "xsi:type=\"xnat:imageFile\"", "xsi:type=\"xnat:imageResource\"");
                                s = StringUtils.ReplaceStr(s, "xsi:type=\"xnat:file\"", "xsi:type=\"xnat:resource\"");
                                
                                int fileIndex = s.indexOf("<xnat:file ");
                                while (fileIndex != -1){
                                   int end= s.indexOf(">", fileIndex);
                                   String sub = s.substring(fileIndex, end);
                                   
                                   if (sub.indexOf("xsi:type")==-1){
                                       String start = s.substring(0,fileIndex +11);
                                       s= start + " xsi:type=\"xnat:resource\" " + s.substring(fileIndex+11);
                                       fileIndex = s.indexOf("<xnat:file ",end);
                                   }else{
                                       int pattern = sub.indexOf(" pattern=\"");
                                       if (pattern==-1){
                                           int nOpen= sub.indexOf(" name=\"");
                                           if (nOpen!=-1){
                                               int nClose = sub.indexOf('\"',nOpen+7);
                                               String nString= sub.substring(nOpen+7, nClose);

                                               int pOpen= sub.indexOf(" path=\"");
                                               int pClose = sub.indexOf('\"',pOpen+7);
                                               String pString= sub.substring(pOpen+7, pClose);

                                               if (!pString.endsWith("/") && !pString.endsWith("\\")){
                                                   pString+="/";
                                               }
                                               
                                               if (pOpen<nOpen){
                                                   sub = sub.substring(0,nOpen) + sub.substring(nClose+1);
                                                   sub = sub.substring(0,pOpen) + sub.substring(pClose+1);
                                                   sub += " URI=\""+ pString + nString + "\"";
                                               }else{
                                                   sub = sub.substring(0,pOpen) + sub.substring(pClose+1);
                                                   sub = sub.substring(0,nOpen) + sub.substring(nClose+1);
                                                   sub += " URI=\""+ pString + nString + "\"";
                                               }
                                               String start = s.substring(0,fileIndex);
                                               String last = s.substring(end);
                                               s = start + sub + last;
                                           }
                                       }
                                       
                                       fileIndex = s.indexOf("<xnat:file ",end);
                                   }
                                }
                                
                                s = StringUtils.ReplaceStr(s, "<prov:cvs/>", "");
                                s = StringUtils.ReplaceStr(s, "<cnda:diagnosis/>", "");
                                
                            }
                            
                            if (removeNotes)
                            {
                                while (s.indexOf("<xnat:notes>")!=-1)
                                {
                                    int startIndex=s.indexOf("<xnat:notes>");
                                    
                                    int endIndex= s.indexOf("xnat:notes>",startIndex+4)+11;
                                    
                                    String before= s.substring(0,startIndex);
                                    s= before + s.substring(endIndex+1);
                                    
                                }
                                
                                s = StringUtils.ReplaceStr(s,"<xnat:note/>","");
                                s = StringUtils.ReplaceStr(s,"<xnat:notes/>","");
                                
                                while (s.indexOf("<xnat:note>")!=-1)
                                {
                                    int startIndex=s.indexOf("<xnat:note>");
                                    
                                    int endIndex= s.indexOf("xnat:note>",startIndex+4)+10;
                                    
                                    String before= s.substring(0,startIndex);
                                    s= before + s.substring(endIndex+1);
                                    
                                }
                            }
                            
                            if(removeEmptys){
                                s = StringUtils.ReplaceStr(s,"<xnat:note/>","");
                                s = StringUtils.ReplaceStr(s,"<xnat:notes/>","");
                                s = StringUtils.ReplaceStr(s,"<xnat:provenance/>","");
                                s = StringUtils.ReplaceStr(s," status=\"\"","");
                            }
                            
                            if (removeProvenance)
                            {
                                while (s.indexOf("<xnat:provenance>")!=-1)
                                {
                                    int startIndex=s.indexOf("<xnat:provenance>");
                                    
                                    int endIndex= s.indexOf("xnat:provenance>",startIndex+4)+11;
                                    
                                    String before= s.substring(0,startIndex);
                                    s= before + s.substring(endIndex+1);
                                    
                                }
                                
                                s = StringUtils.ReplaceStr(s,"<xnat:provenance/>","");
                                
                            }
                            
                            if (removeStatus){
                                s = StringUtils.ReplaceStr(s," status=\"\"","");
                            }
                            
                            if (removeTime)
                            {
                                while (s.indexOf("<xnat:time>")!=-1)
                                {
                                    int startIndex=s.indexOf("<xnat:time>");
                                    
                                    int endIndex= s.indexOf("xnat:time>",startIndex+5)+10;
                                    
                                    String before= s.substring(0,startIndex);
                                    s= before + s.substring(endIndex+1);
                                    
                                }
                            }
                            
                            FileUtils.OutputToFile(s,f.getAbsolutePath());
                            
                            org.nrg.xft.utils.XMLUtils.PrettyPrintDOM(f);
                            System.out.println(":" + ((float) (Calendar.getInstance().getTimeInMillis()-startTime)/1000) + "s ");
                        } catch (RuntimeException e) {
                            
                            e.printStackTrace();
                        }
                    }
                    
                }
                
                
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    
    public static void parse(java.io.File data) throws IOException, SAXException, ParserConfigurationException{
        System.out.print("Validating...");
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        
        //get a new instance of parser
        SAXParser sp = spf.newSAXParser();
        //parse the file and also register this class for call backs
        sp.parse(data,new DefaultHandler());

        System.out.println("done.");
        return ;
    }
    
    public static void main(String[] args) {

        List argsAL = Arrays.asList(args);
        process(argsAL);
    }
}
