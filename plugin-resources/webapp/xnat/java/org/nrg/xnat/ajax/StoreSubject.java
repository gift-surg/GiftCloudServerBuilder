//Copyright Washington University School of Medicine All Rights Reserved
/*
 * Created on Dec 15, 2006
 *
 */
package org.nrg.xnat.ajax;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.log4j.Logger;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.model.XnatProjectparticipantI;
import org.nrg.xdat.model.XnatSubjectdataAddidI;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.collections.ItemCollection;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXWriter;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.ItemSearch;
import org.xml.sax.InputSource;

public class StoreSubject{
    static org.apache.log4j.Logger logger = Logger.getLogger(StoreSubject.class);
    public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException{
        final XDATUser user = XDAT.getUserDetails();
        final String xmlString = req.getParameter("subject");

        final StringReader sr = new StringReader(xmlString);
        final InputSource is = new InputSource(sr);
        XnatSubjectdata subject=null;
        response.setContentType("text/xml");
        response.setHeader("Cache-Control", "no-cache");
        
        boolean successful=false;
        try {
            final SAXReader reader = new SAXReader(user);
            final XFTItem item = reader.parse(is);
            
            subject = new XnatSubjectdata(item);
            final CriteriaCollection cc = new CriteriaCollection("OR");
            
            if (subject.getId()!=null){
                cc.addClause("xnat:subjectData/ID",subject.getId());
            }
            
            if (subject.getLabel()!=null && subject.getProject()!=null){
                final CriteriaCollection subcc = new CriteriaCollection("AND");
                subcc.addClause("xnat:subjectData/project",subject.getProject());
                subcc.addClause("xnat:subjectData/label",subject.getLabel());
                cc.add(subcc);
            }
    
            if (!subject.getSharing_share().isEmpty()){
                for (XnatProjectparticipantI pp : subject.getSharing_share()){
                    final CriteriaCollection subcc = new CriteriaCollection("AND");
                    subcc.addClause("xnat:subjectData/sharing/share/project",pp.getProject());
                    subcc.addClause("xnat:subjectData/sharing/share/label",pp.getLabel());
                    cc.add(subcc);
                }
            }
    
            if (!subject.getAddid().isEmpty()){
                for (XnatSubjectdataAddidI pp : subject.getAddid()){
                    final CriteriaCollection subcc = new CriteriaCollection("AND");
                    subcc.addClause("xnat:subjectData/addID/name",pp.getName());
                    subcc.addClause("xnat:subjectData/addID/addID",pp.getAddid());
                    cc.add(subcc);
                }
            }
        
            if (cc.numClauses()>0)
            {
                final ItemCollection items = ItemSearch.GetItems("xnat:subjectData",cc,null,false);
                if (items.size()>0){
                    final StringBuffer sb = new StringBuffer();

                    response.getWriter().write("<matchingResults message=\"Matched pre-existing subject. Save Aborted.\">");
                    try {
                        Iterator itemIter = items.getItemIterator();
                        while (itemIter.hasNext())
                        {
                            ItemI i = (ItemI)itemIter.next();
                            if (i.getXSIType().equals("xnat:subjectData"))
                            {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                
                                try {
                                    SAXWriter writer = new SAXWriter(baos,false);
                                    
                                    writer.write((XFTItem)i);
                                } catch (TransformerConfigurationException e) {
                                    logger.error("",e);
                                } catch (TransformerFactoryConfigurationError e) {
                                    logger.error("",e);
                                } catch (FieldNotFoundException e) {
                                    logger.error("",e);
                                }
                                response.getWriter().write(baos.toString());
                            }
                        }
                    } catch (Exception e) {
                    }
                    response.getWriter().write("</matchingResults>");
                    response.setContentType("text/xml");
                    response.setHeader("Cache-Control", "no-cache");
                    return;
                }
            }      
            
            String newID=null;
            if (subject.getId()==null)
            {
                //ASSIGN A PARTICIPANT ID
                newID = XnatSubjectdata.CreateNewID();
            }
            
            subject.setId(newID);
            
            subject.save(user, false, true);
            successful=true;

        } catch (Exception e1) {
            logger.error("",e1);
        }
        
        StringBuffer sb = new StringBuffer();

        response.getWriter().write("<matchingResults message=\"Subject Stored.\">");
        
        if (successful){
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                
                try {
                    SAXWriter writer = new SAXWriter(baos,false);
                    
                    writer.write(subject.getItem());
                } catch (TransformerConfigurationException e) {
                    logger.error("",e);
                } catch (TransformerFactoryConfigurationError e) {
                    logger.error("",e);
                } catch (FieldNotFoundException e) {
                    logger.error("",e);
                }
                response.getWriter().write(baos.toString());
            } catch (Exception e) {
            }
        }
        response.getWriter().write("</matchingResults>");
        
    }
}
