//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 16 15:08:17 CDT 2005
 *
 */
package org.nrg.xdat.om.base;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.display.DisplayField;
import org.nrg.xdat.model.ArcPathinfoI;
import org.nrg.xdat.model.XnatAbstractprotocolI;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatExperimentdataShareI;
import org.nrg.xdat.model.XnatFielddefinitiongroupFieldI;
import org.nrg.xdat.model.XnatFielddefinitiongroupI;
import org.nrg.xdat.model.XnatInvestigatordataI;
import org.nrg.xdat.model.XnatProjectdataFieldI;
import org.nrg.xdat.model.XnatProjectparticipantI;
import org.nrg.xdat.model.XnatPublicationresourceI;
import org.nrg.xdat.model.XnatSubjectassessordataI;
import org.nrg.xdat.om.ArcPathinfo;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XdatSearchField;
import org.nrg.xdat.om.XdatUserGroupid;
import org.nrg.xdat.om.XdatUsergroup;
import org.nrg.xdat.om.XnatAbstractprotocol;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatDatatypeprotocol;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatExperimentdataShare;
import org.nrg.xdat.om.XnatInvestigatordata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatProjectdataField;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourceseries;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.base.auto.AutoXnatProjectdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.search.CriteriaCollection;
import org.nrg.xdat.search.DisplaySearch;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.UserGroup;
import org.nrg.xdat.security.UserGroupManager;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XDATUser.UserNotFoundException;
import org.nrg.xdat.security.XdatStoredSearch;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.event.Event;
import org.nrg.xft.event.EventManager;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.ArchivableItem;
import org.restlet.data.Status;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatProjectdata extends AutoXnatProjectdata  implements ArchivableItem{
    public final static String MEMBER_GROUP = "member";
    public final static String COLLABORATOR_GROUP = "collaborator";
    public final static String OWNER_GROUP = "owner";

	public BaseXnatProjectdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatProjectdata(UserI user)
	{
		super(user);
	}

	public BaseXnatProjectdata()
	{}

	public BaseXnatProjectdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    public int getSubjectCount(){
        try {
            XFTTable table = XFTTable.Execute("SELECT COUNT(*) FROM (SELECT DISTINCT subject_id,project FROM (SELECT pp.subject_id,pp.project FROM xnat_projectparticipant pp LEFT JOIN xnat_subjectData sub ON pp.subject_id=sub.id WHERE sub.id IS NOT NULL UNION SELECT ID,project FROM xnat_subjectdata )SEARCH )SEARCH WHERE project='" + getId() + "';", getDBName(), null);

            Long i = (Long)table.getFirstObject();
            if (i!=null){
                return i.intValue();
            }
        } catch (SQLException e) {
            logger.error("",e);
        } catch (DBPoolException e) {
            logger.error("",e);
        }
        return -1;
    }

    public Hashtable<String,Long> getExperimentCountByName(){
        Hashtable<String,Long> hash = new Hashtable<String,Long>();
        try {
            XFTTable table = XFTTable.Execute("SELECT COUNT(*) AS expt_count,element_name FROM (SELECT DISTINCT project,sharing_share_xnat_experimentda_id,extension FROM (SELECT exs.project, sharing_share_xnat_experimentda_id,extension FROM xnat_experimentdata_share exs LEFT JOIN xnat_experimentData ex ON exs.sharing_share_xnat_experimentda_id=ex.id WHERE ex.id IS NOT NULL UNION SELECT project,ID,extension FROM xnat_experimentdata) SEARCH )SEARCH LEFT JOIN xdat_meta_element ON  SEARCH.extension=xdat_meta_element.xdat_meta_element_id WHERE project='" + getId() + "' GROUP BY element_name;", getDBName(), null);

            table.resetRowCursor();
            while(table.hasMoreRows()){
                Object[] row = table.nextRow();
                Long count = (Long)row[0];
                String elementN = (String)row[1];
                try {
                    SchemaElement se = SchemaElement.GetElement(elementN);
                    elementN = se.getProperName();
                } catch (XFTInitException e) {
                    logger.error("",e);
                } catch (ElementNotFoundException e) {
                    logger.error("",e);
                }
                hash.put(elementN, count);
            }
        } catch (SQLException e) {
            logger.error("",e);
        } catch (DBPoolException e) {
            logger.error("",e);
        }

        return hash;
    }

    Hashtable<String,Long> exptCountsByType=null;
    public Hashtable<String,Long> getExperimentCountByXSIType(){
    	if(exptCountsByType==null){
        	exptCountsByType = new Hashtable<String,Long>();
            try {
                XFTTable table = XFTTable.Execute("SELECT COUNT(*) AS expt_count,element_name FROM (SELECT DISTINCT project,sharing_share_xnat_experimentda_id,extension FROM (SELECT exs.project, sharing_share_xnat_experimentda_id,extension FROM xnat_experimentdata_share exs LEFT JOIN xnat_experimentData ex ON exs.sharing_share_xnat_experimentda_id=ex.id WHERE ex.id IS NOT NULL UNION SELECT project,ID,extension FROM xnat_experimentdata) SEARCH )SEARCH LEFT JOIN xdat_meta_element ON  SEARCH.extension=xdat_meta_element.xdat_meta_element_id WHERE project='" + getId() + "' GROUP BY element_name;", getDBName(), null);

                table.resetRowCursor();
                while(table.hasMoreRows()){
                    Object[] row = table.nextRow();
                    Long count = (Long)row[0];
                    String elementN = (String)row[1];
                    exptCountsByType.put(elementN, count);
                }
            } catch (SQLException e) {
                logger.error("",e);
            } catch (DBPoolException e) {
                logger.error("",e);
            }
    	}

        return exptCountsByType;
    }
//
//    public String outputProjectHTMLMenu(String login,boolean expandable){
//        StringBuffer sb = new StringBuffer();
//         long startTime = Calendar.getInstance().getTimeInMillis();
//        int subjects =this.getSubjectCount();
//
//        String subjectBundle = getId() + "_SUBJECTS";
//        boolean canRead = true;
//        XdatStoredSearch xss = XdatStoredSearch.GetPreLoadedSearch(subjectBundle);
//        String link = Turbine.getContextPath()+"/app/action/BundleAction/bundle/"+subjectBundle;
//        if (xss==null)
//        {
//            link = Turbine.getContextPath()+"/app/action/DisplaySearchAction/displayversion/full/ELEMENT_0/xnat:subjectData/xnat:subjectData.PROJECTS_equals/" + getId();
//        }else{
//            if (!xss.canRead(login)){
//                canRead= false;
//            }
//        }
//        try {
//            XFTTable table = XFTTable.Execute("SELECT COUNT(*) AS expt_count,element_name FROM xnat_subjectAssessorData LEFT JOIN xnat_experimentdata_share ON xnat_subjectAssessorData.ID=xnat_experimentdata_share.sharing_share_xnat_experimentda_id LEFT JOIN xnat_experimentData ON xnat_subjectAssessorData.ID=xnat_experimentData.ID LEFT JOIN xdat_meta_element ON xnat_experimentData.extension=xdat_meta_element.xdat_meta_element_id WHERE xnat_experimentdata_share.project='" + getId() + "' GROUP BY element_name;", getDBName(), null);
//
//            if (table.rows().size()==0){
//                sb.append("<tr><td>&#8226;</td>");
//                sb.append("<td>");
//                if (canRead)
//                    sb.append("<a href=\"" + link + "\">");
//                sb.append("Subjects").append("&nbsp;(" + subjects + ")");
//                if (canRead)
//                    sb.append("</a>");
//                sb.append("</td></tr>");
//
//            }else{
//                sb.append("<tr>");
//                if (expandable){
//                    sb.append("<td>");
//                    sb.append("<A NAME=\"LINK").append(subjectBundle).append("\"");
//                    sb.append(" HREF=\"#LINK").append(subjectBundle).append("\" ");
//                    sb.append("onClick=\" return blocking('").append(subjectBundle).append("');\">");
//                    sb.append("<img ID=\"IMG").append(subjectBundle).append("\" src=\"");
//                    sb.append(Turbine.getContextPath()).append("/images/plus.jpg\" border=0></A></td>");
//                }else{
//                    sb.append("<td>&#8226;</td>");
//                }
//
//                sb.append("<td>");
//                if (canRead)
//                    sb.append("<a href=\"" + link + "\">");
//                sb.append("Subjects");
//                sb.append("&nbsp;(" + subjects + ")");
//                if (canRead)
//                    sb.append("</a>");
//                sb.append("</td></tr>");
//
//
//                sb.append("<tr><td></td><td>");
//                if (expandable)
//                    sb.append("<span ID=\"span").append(subjectBundle).append("\" style=\"position:relative; display:none;\">");
//                sb.append("<TABLE align=\"left\" valign=\"top\">");
//                table.resetRowCursor();
//                while(table.hasMoreRows()){
//                    Object[] row = table.nextRow();
//                    Long count = (Long)row[0];
//                    String displayName = (String)row[1];
//                    String dataType=(String)row[1];
//                    SchemaElement se= null;
//                    try {
//                        se = SchemaElement.GetElement(displayName);
//                        displayName = se.getProperName();
//                    } catch (XFTInitException e) {
//                        logger.error("",e);
//                    } catch (ElementNotFoundException e) {
//                        logger.error("",e);
//                    }
//
//
//                    if (count > 0)
//                    {
//                        String bundleID = getId() + "_" + displayName;
//
//                        canRead = true;
//                        xss = XdatStoredSearch.GetPreLoadedSearch(bundleID);
//                        if (xss==null)
//                        {
//                            link = Turbine.getContextPath()+"/app/action/DisplaySearchAction/displayversion/full/ELEMENT_0/" + dataType + "/" + dataType + ".PROJECTS_equals/" + getId();
//                        }else{
//                            link = Turbine.getContextPath()+"/app/action/BundleAction/bundle/"+bundleID;
//                            if (!xss.canRead(login)){
//                                canRead= false;
//                            }
//                        }
//
//                        if (se==null){
//                            sb.append("<tr><td>&#8226;</td>");
//                            sb.append("<td>");
//                            if (canRead)
//                                sb.append("<a href=\"" + link + "\">");
//                            sb.append(displayName).append("&nbsp;(" + count + ")*");
//                            if (canRead)
//                                sb.append("</a>");
//                            sb.append("</td></tr>");
//                            continue;
//                        }
//                        if (se.getDisplay()==null){
//                            canRead=false;
//                        }
//
//                        if (dataType.equalsIgnoreCase("xnat:mrSessionData"))
//                        {
//                            XFTTable table2 = XFTTable.Execute("SELECT COUNT(*) AS expt_count,element_name FROM xnat_mrAssessorData LEFT JOIN xnat_experimentdata_share ON xnat_mrAssessorData.ID=xnat_experimentdata_share.sharing_share_xnat_experimentda_id LEFT JOIN xnat_experimentData ON xnat_mrAssessorData.ID=xnat_experimentData.ID LEFT JOIN xdat_meta_element ON xnat_experimentData.extension=xdat_meta_element.xdat_meta_element_id WHERE xnat_experimentdata_share.project='" + getId() + "' GROUP BY element_name;", getDBName(), null);
//
//                            if (table2.rows().size()==0){
//                                sb.append("<tr><td>&#8226;</td>");
//                                sb.append("<td>");
//                                if (canRead)
//                                    sb.append("<a href=\"" + link + "\">");
//                                sb.append(displayName).append("&nbsp;(" + count + ")");
//                                if (canRead)
//                                    sb.append("</a>");
//                                sb.append("</td></tr>");
//
//                            }else{
//                                sb.append("<tr>");
//                                if (expandable){
//                                    sb.append("<td><A NAME=\"LINK").append(bundleID).append("\"");
//                                    sb.append("<A NAME=\"LINK").append(bundleID).append("\"");
//                                    sb.append(" HREF=\"#LINK").append(bundleID).append("\" ");
//                                    sb.append("onClick=\" return blocking('").append(bundleID).append("');\">");
//                                    sb.append("<img ID=\"IMG").append(bundleID).append("\" src=\"");
//                                    sb.append(Turbine.getContextPath()).append("/images/plus.jpg\" border=0></A></td>");
//                                }else{
//                                    sb.append("<td>&#8226;</td>");
//                                }
//
//                                sb.append("<td>");
//                                if (canRead)
//                                    sb.append("<a href=\"" + link + "\">");
//                                sb.append(displayName).append("&nbsp;(" + count + ")");
//                                if (canRead)
//                                    sb.append("</a>");
//                                sb.append("</td></tr>");
//
//
//                                sb.append("<tr><td></td><td>");
//                                if (expandable)
//                                    sb.append("<span ID=\"span").append(bundleID).append("\" style=\"position:relative; display:none;\">");
//                                sb.append("<TABLE align=\"left\" valign=\"top\">");
//                                table2.resetRowCursor();
//                                while(table2.hasMoreRows()){
//                                    Object[] row2 = table2.nextRow();
//                                    count = (Long)row2[0];
//                                    displayName = (String)row2[1];
//                                    dataType=(String)row2[1];
//                                    se= null;
//                                    try {
//                                        se = SchemaElement.GetElement(displayName);
//                                        displayName = se.getProperName();
//                                    } catch (XFTInitException e) {
//                                        logger.error("",e);
//                                    } catch (ElementNotFoundException e) {
//                                        logger.error("",e);
//                                    }
//
//
//                                    if (count > 0)
//                                    {
//                                        bundleID = getId() + "_" + displayName;
//
//                                        canRead = true;
//                                        xss = XdatStoredSearch.GetPreLoadedSearch(bundleID);
//                                        if (xss==null)
//                                        {
//                                            link = Turbine.getContextPath()+"/app/action/DisplaySearchAction/displayversion/full/ELEMENT_0/" + dataType + "/" + dataType + ".PROJECTS_equals/" + getId();
//                                        }else{
//                                            link = Turbine.getContextPath()+"/app/action/BundleAction/bundle/"+bundleID;
//                                            if (!xss.canRead(login)){
//                                                canRead= false;
//                                            }
//                                        }
//                                        if (se.getDisplay()==null){
//                                            canRead=false;
//                                        }
//
//                                        sb.append("<tr><td>&#8226;</td>");
//                                        sb.append("<td>");
//                                        if (canRead)
//                                            sb.append("<a href=\"" + link + "\">");
//                                        sb.append(displayName).append("&nbsp;(" + count + ")");
//                                        if (canRead)
//                                            sb.append("</a>");
//                                        sb.append("</td></tr>");
//                                    }
//                                }
//                                sb.append("\n</TABLE>");
//                                if (expandable)
//                                    sb.append("</span>");
//                                sb.append("</td></tr>");
//
//                            }
//                        }else if (dataType.equalsIgnoreCase("xnat:petSessionData"))
//                        {
//                            XFTTable table2 = XFTTable.Execute("SELECT COUNT(*) AS expt_count,element_name FROM xnat_petAssessorData LEFT JOIN xnat_experimentdata_share ON xnat_petAssessorData.ID=xnat_experimentdata_share.sharing_share_xnat_experimentda_id LEFT JOIN xnat_experimentData ON xnat_petAssessorData.ID=xnat_experimentData.ID LEFT JOIN xdat_meta_element ON xnat_experimentData.extension=xdat_meta_element.xdat_meta_element_id WHERE xnat_experimentdata_share.project='" + getId() + "' GROUP BY element_name;", getDBName(), null);
//
//                            if (table2.rows().size()==0){
//                                sb.append("<tr><td>&#8226;</td>");
//                                sb.append("<td>");
//                                if (canRead)
//                                    sb.append("<a href=\"" + link + "\">");
//                                sb.append(displayName).append("&nbsp;(" + count + ")");
//                                if (canRead)
//                                    sb.append("</a>");
//                                sb.append("</td></tr>");
//
//                            }else{
//                                sb.append("<tr>");
//                                if (expandable){
//                                    sb.append("<td><A NAME=\"LINK").append(bundleID).append("\"");
//                                    sb.append("<A NAME=\"LINK").append(bundleID).append("\"");
//                                    sb.append(" HREF=\"#LINK").append(bundleID).append("\" ");
//                                    sb.append("onClick=\" return blocking('").append(bundleID).append("');\">");
//                                    sb.append("<img ID=\"IMG").append(bundleID).append("\" src=\"");
//                                    sb.append(Turbine.getContextPath()).append("/images/plus.jpg\" border=0></A></td>");
//                                }else{
//                                    sb.append("<td>&#8226;</td>");
//                                }
//
//                                sb.append("<td>");
//                                if (canRead)
//                                    sb.append("<a href=\"" + link + "\">");
//                                sb.append(displayName).append("&nbsp;(" + count + ")");
//                                if (canRead)
//                                    sb.append("</a>");
//                                sb.append("</td></tr>");
//
//
//                                sb.append("<tr><td></td><td>");
//                                if (expandable)
//                                    sb.append("<span ID=\"span").append(bundleID).append("\" style=\"position:relative; display:none;\">");
//                                sb.append("<TABLE align=\"left\" valign=\"top\">");
//                                table2.resetRowCursor();
//                                while(table2.hasMoreRows()){
//                                    Object[] row2 = table2.nextRow();
//                                    count = (Long)row2[0];
//                                    displayName = (String)row2[1];
//                                    dataType=(String)row2[1];
//                                    se= null;
//                                    try {
//                                        se = SchemaElement.GetElement(displayName);
//                                        displayName = se.getProperName();
//                                    } catch (XFTInitException e) {
//                                        logger.error("",e);
//                                    } catch (ElementNotFoundException e) {
//                                        logger.error("",e);
//                                    }
//
//
//                                    if (count > 0)
//                                    {
//                                        bundleID = getId() + "_" + displayName;
//
//                                        canRead = true;
//                                        if (se.getDisplay()==null){
//                                            canRead=false;
//                                        }
//                                        xss = XdatStoredSearch.GetPreLoadedSearch(bundleID);
//                                        if (xss==null)
//                                        {
//                                            link = Turbine.getContextPath()+"/app/action/DisplaySearchAction/displayversion/full/ELEMENT_0/" + dataType + "/" + dataType + ".PROJECTS_equals/" + getId();
//                                        }else{
//                                            link = Turbine.getContextPath()+"/app/action/BundleAction/bundle/"+bundleID;
//                                            if (!xss.canRead(login)){
//                                                canRead= false;
//                                            }
//                                        }
//
//                                        sb.append("<tr><td>&#8226;</td>");
//                                        sb.append("<td>");
//                                        if (canRead)
//                                            sb.append("<a href=\"" + link + "\">");
//                                        sb.append(displayName).append("&nbsp;(" + count + ")");
//                                        if (canRead)
//                                            sb.append("</a>");
//                                        sb.append("</td></tr>");
//                                    }
//                                }
//                                sb.append("\n</TABLE>");
//                                if (expandable)
//                                    sb.append("</span>");
//                                sb.append("</td></tr>");
//
//                            }
//                        }else{
//                            sb.append("<tr><td>&#8226;</td>");
//                            sb.append("<td>");
//                            if (canRead)
//                                sb.append("<a href=\"" + link + "\">");
//                            sb.append(displayName).append("&nbsp;(" + count + ")");
//                            if (canRead)
//                                sb.append("</a>");
//                            sb.append("</td></tr>");
//                        }
//                    }
//                }
//                sb.append("\n</TABLE>");
//                if (expandable)
//                    sb.append("</span>");
//                sb.append("</td></tr>");
//            }
//        } catch (SQLException e) {
//            logger.error("",e);
//        } catch (DBPoolException e) {
//            logger.error("",e);
//        }
//
//        System.out.println(getId() + " Menu Create Time: " + (System.currentTimeMillis()-startTime) + "ms");
//
//        return sb.toString();
//    }

    public ArrayList<org.nrg.xdat.om.XnatPublicationresource> getPublicationsByType(String t)
    {
        ArrayList<org.nrg.xdat.om.XnatPublicationresource> pubs = new ArrayList<org.nrg.xdat.om.XnatPublicationresource>();

        List<XnatPublicationresourceI> allPubs = getPublications_publication();
        for (int i =0;i<allPubs.size();i++)
        {
            org.nrg.xdat.om.XnatPublicationresource res = (org.nrg.xdat.om.XnatPublicationresource)allPubs.get(i);
            if (res.getType().equals(t))
            {
                pubs.add(res);
            }
        }

        return pubs;

    }

    public String getShortenedDescription()
    {
        if (getDescription()==null)
        {
            return "";
        }
        if (getDescription().length()>500)
        {
            return getDescription().substring(0,499) + "...";
        }else{
            return getDescription();
        }
    }

    public String createID(String base, int digits) throws Exception{
        String identifier = "";

        if (base!=null)
        {
            identifier = base;
            identifier = StringUtils.ReplaceStr(identifier, " ", "");
            identifier = StringUtils.ReplaceStr(identifier, "-", "_");
            identifier = StringUtils.ReplaceStr(identifier, "\"", "");
            identifier = StringUtils.ReplaceStr(identifier, "'", "");

            identifier= incrementID(identifier,digits);
        }

        return identifier;
    }

    public String createID(String base) throws Exception{
        return createID(base,-1);
    }

    private String incrementID(String s,int digits) throws Exception{
        String temp_id = null;

        if(s == null)
        {
            throw new NullPointerException();
        }

        XFTTable table = org.nrg.xft.search.TableSearch.Execute("SELECT id FROM xnat_projectdata WHERE id LIKE '" + s + "%';", this.getSchemaElement().getDbName(), null);
        ArrayList al =table.convertColumnToArrayList("id");

        NumberFormat nf = NumberFormat.getIntegerInstance();
        nf.setMinimumIntegerDigits(digits);
        if (al.size()>0){
            int count =al.size()+1;
            String full = StringUtils.ReplaceStr(nf.format(count), ",", "");
            temp_id = s+ full;

            while (al.contains(temp_id)){
                count++;
                full =StringUtils.ReplaceStr(nf.format(count), ",", "");
                temp_id = s+ full;
            }

            return temp_id;
        }else{
            int count =1;
            String full = nf.format(count);
            temp_id = s+ full;
            return temp_id;
        }
    }

    public String setId(XnatInvestigatordataI i) throws Exception{

        if(i == null)
        {
            throw new NullPointerException();
        }
        String temp_id = createID(i.getLastname());

        this.setId(temp_id);

        return temp_id;
    }

    private ArrayList<XnatSubjectdata> _participants = null;
    public ArrayList<XnatSubjectdata> getParticipants_participant(){
    	if (_participants==null){
        	final UserI user = this.getUser();
        	final CriteriaCollection cc = new CriteriaCollection("OR");
        	cc.addClause("xnat:subjectData/project", this.getId());
        	cc.addClause("xnat:subjectData/sharing/share/project", this.getId());
        	_participants= XnatSubjectdata.getXnatSubjectdatasByField(cc, user, false);
    	}

    	return _participants;
    }

    private ArrayList<XnatExperimentdata> _experiments = null;
    public ArrayList<XnatExperimentdata> getExperiments(){
    	if (_experiments==null){
        	final UserI user = this.getUser();
        	final CriteriaCollection cc = new CriteriaCollection("OR");
        	cc.addClause("xnat:experimentData/project", this.getId());
        	cc.addClause("xnat:experimentData/sharing/share/project", this.getId());
        	_experiments= XnatExperimentdata.getXnatExperimentdatasByField(cc, user, false);
    	}

    	return _experiments;
    }

    public ArrayList getExperimentsByXSIType(String type) {
        ArrayList<XnatExperimentdata> typed = new ArrayList<XnatExperimentdata>();
    	for(final XnatExperimentdata expt:this.getExperiments()){
    		if (expt.getXSIType().equals(type)){
    			typed.add(expt);
    		}
    	}
    	return typed;
    }


    Hashtable<String,XnatProjectdataField> fieldsByName = null;
    public Hashtable getFieldsByName(){
        if (fieldsByName == null){
            fieldsByName=new Hashtable<String,XnatProjectdataField>();
            for(final XnatProjectdataFieldI field: this.getFields_field()){
                fieldsByName.put(field.getName(), (XnatProjectdataField)field);
            }
        }

        return fieldsByName;
    }
    public Object getFieldByName(String s){
        final XnatProjectdataFieldI field = (XnatProjectdataFieldI)getFieldsByName().get(s);
        if (field!=null){
            return field.getField();
        }else{
            return null;
        }
    }

    public ArrayList<String> getOwnerEmails() throws Exception{
        return this.getOwners();
    }



    public ArrayList<String> getOwners() throws Exception{
        final XFTTable table2 = XFTTable.Execute("SELECT DISTINCT email FROM xdat_element_access ea LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id=fms.permissions_allow_set_xdat_elem_xdat_element_access_id LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id=fm.xdat_field_mapping_set_xdat_field_mapping_set_id LEFT JOIN xdat_userGroup ug ON ea.xdat_usergroup_xdat_usergroup_id=ug.xdat_usergroup_id LEFT JOIN xdat_user_groupid map ON ug.id=map.groupid LEFT JOIN xdat_user u ON map.groups_groupid_xdat_user_xdat_user_id=u.xdat_user_id  WHERE read_element=1 AND delete_element=1 AND login !='guest' AND element_name='xnat:subjectData' AND field_value='" + getId() +  "' ORDER BY email;", getDBName(), null);
        return table2.convertColumnToArrayList("email");
    }

    public ArrayList<String> getMembers() throws Exception{
    	final XFTTable table1 = XFTTable.Execute("SELECT DISTINCT email FROM xdat_element_access ea LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id=fms.permissions_allow_set_xdat_elem_xdat_element_access_id LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id=fm.xdat_field_mapping_set_xdat_field_mapping_set_id LEFT JOIN xdat_userGroup ug ON ea.xdat_usergroup_xdat_usergroup_id=ug.xdat_usergroup_id LEFT JOIN xdat_user_groupid map ON ug.id=map.groupid LEFT JOIN xdat_user u ON map.groups_groupid_xdat_user_xdat_user_id=u.xdat_user_id  WHERE edit_element=1 AND login !='guest' AND element_name='xnat:subjectData' AND field_value='" + getId() +  "' ORDER BY email;", getDBName(), null);
        return table1.convertColumnToArrayList("email");
    }

    public ArrayList<String> getCollaborators() throws Exception{
    	final XFTTable table2 = XFTTable.Execute("SELECT DISTINCT email FROM xdat_element_access ea LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id=fms.permissions_allow_set_xdat_elem_xdat_element_access_id LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id=fm.xdat_field_mapping_set_xdat_field_mapping_set_id LEFT JOIN xdat_userGroup ug ON ea.xdat_usergroup_xdat_usergroup_id=ug.xdat_usergroup_id LEFT JOIN xdat_user_groupid map ON ug.id=map.groupid LEFT JOIN xdat_user u ON map.groups_groupid_xdat_user_xdat_user_id=u.xdat_user_id  WHERE read_element=1 AND edit_element=0 AND login !='guest' AND element_name='xnat:subjectData' AND field_value='" + getId() +  "' ORDER BY email;", getDBName(), null);
        return table2.convertColumnToArrayList("email");
    }
//
//    public ArrayList<XnatProjectparticipant> getParticipants(String field, Object value){
//    	final ArrayList<XnatProjectparticipant> matches = new ArrayList<XnatProjectparticipant>();
//
//        ArrayList<XnatProjectparticipant> participants = this.getSharing_sh();
//        if (participants.size()>0)
//        {
//            for (XnatProjectparticipant pp: participants){
//                try {
//                    if (pp.hasProperty(field, value)){
//                        matches.add(pp);
//                    }
//                } catch (XFTInitException e) {
//                    logger.error("",e);
//                } catch (ElementNotFoundException e) {
//                    logger.error("",e);
//                } catch (FieldNotFoundException e) {
//                    logger.error("",e);
//                }
//            }
//        }
//
//        return matches;
//    }

    public String getSubjectSummary(){
        final StringBuffer sb = new StringBuffer();
            sb.append(this.getSubjectCount());
            sb.append(" Subjects ");

        return sb.toString();
    }

    public String getRootArchivePath(){
        String path = null;

        final ArcProject arcProj = this.getArcSpecification();
        if (arcProj !=null){
            ArcPathinfo pathInfo=arcProj.getPaths();
            if (pathInfo!=null){
                path = pathInfo.getArchivepath();
            }
        }

        if (path==null){
            path=ArcSpecManager.GetInstance().getGlobalArchivePath()+"/" + this.getId();
        }

        if (path==null){
            path ="./" + this.getId();
        }

        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }

        return path;
    }

    public String getCachePath(){
        String path = null;

        final ArcProject arcProj = this.getArcSpecification();
        if (arcProj !=null){
            ArcPathinfoI pathInfo=arcProj.getPaths();
            if (pathInfo!=null){
                path = pathInfo.getCachepath();
            }
        }

        if (path==null){
            path=ArcSpecManager.GetInstance().getGlobalCachePath()+"/" + this.getId();
        }

        if (path==null){
            path ="./" + this.getId();
        }

        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }

        return path;
    }

    public String getPrearchivePath(){
        String path = null;

        final ArcProject arcProj = this.getArcSpecification();
        if (arcProj !=null){
            ArcPathinfoI pathInfo=arcProj.getPaths();
            if (pathInfo!=null){
                path = pathInfo.getPrearchivepath();
            }
        }

        if (path==null){
            path=ArcSpecManager.GetInstance().getGlobalPrearchivePath()+"/" + this.getId();
        }

        if (path==null){
            path ="./" + this.getId();
        }

        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }

        return path;
    }

    public String getBuildPath(){
        String path = null;

        final ArcProject arcProj = this.getArcSpecification();
        if (arcProj !=null){
            ArcPathinfoI pathInfo=arcProj.getPaths();
            if (pathInfo!=null){
                path = pathInfo.getBuildpath();
            }
        }

        if (path==null){
            path=ArcSpecManager.GetInstance().getGlobalBuildPath()+"/" + this.getId();
        }

        if (path==null){
            path ="./" + this.getId();
        }

        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }

        return path;
    }

    public String getCurrentArc(){
        String path = null;

        final ArcProject arcProj = this.getArcSpecification();

		if (arcProj==null) return "arc001";

        path = arcProj.getCurrentArc();
        if (path==null || path.equals("")){
            path="arc001";
        }

        return path;
    }

    public ArrayList<String> getGroupMembers(String level){
        try {
        	final XFTTable table = XFTTable.Execute("SELECT DISTINCT email FROM xdat_user RIGHT JOIN xdat_user_groupid xug ON xdat_user.xdat_user_id=xug.groups_groupid_xdat_user_xdat_user_id WHERE groupid='"+ this.getId() + "_" + level +"';", this.getDBName(), null);
            return table.convertColumnToArrayList("email");
        } catch (SQLException e) {
            logger.error("",e);
        } catch (DBPoolException e) {
            logger.error("",e);

        }

        return null;
    }

    public ArrayList<String> getGroupMembersByGroupID(String groupid){
        try {
        	final XFTTable table = XFTTable.Execute("SELECT DISTINCT email FROM xdat_user RIGHT JOIN xdat_user_groupid xug ON xdat_user.xdat_user_id=xug.groups_groupid_xdat_user_xdat_user_id WHERE groupid='"+ groupid +"';", this.getDBName(), null);
            return table.convertColumnToArrayList("email");
        } catch (SQLException e) {
            logger.error("",e);
        } catch (DBPoolException e) {
            logger.error("",e);

        }

        return null;
    }

    public ArrayList<XdatUsergroup> getGroups(){

    	final CriteriaCollection col = new CriteriaCollection("AND");
        col.addClause(XdatUsergroup.SCHEMA_ELEMENT_NAME +".tag","=", "'" + this.getId() + "'",true);
        final ArrayList<XdatUsergroup> groups = XdatUsergroup.getXdatUsergroupsByField(col, this.getUser(), false);

        return groups;
    }

    public ArrayList<List> getGroupIDs(){
        try {
            final XFTTable groups = XFTTable.Execute("SELECT id,displayname FROM xdat_usergroup WHERE tag='" + this.getId() + "' ORDER BY displayname DESC",this.getDBName(), null);
            return groups.toArrayListOfLists();
        } catch (Exception e) {
            logger.error("",e);
            return new ArrayList();
        }
    }

    public String addGroupMember(String group_id, XDATUser newUser,XDATUser currentUser) throws Exception{
		final String confirmquery = "SELECT * FROM xdat_user_groupid WHERE groupid='" + group_id + "' AND groups_groupid_xdat_user_xdat_user_id=" + newUser.getXdatUserId() + ";";
    	XFTTable t=XFTTable.Execute(confirmquery,currentUser.getDBName(), currentUser.getUsername());
    	if(t.size()==0){
            final XdatUserGroupid map = new XdatUserGroupid((UserI)currentUser);
            map.setProperty(map.getXSIType() +".groups_groupid_xdat_user_xdat_user_id", newUser.getXdatUserId());
            map.setGroupid(group_id);
            map.save(currentUser, false, false);
    	}
        return group_id;
    }

    public void removeGroupMember(String group_id, XDATUser newUser,XDATUser currentUser) throws Exception{
		final String confirmquery = "SELECT * FROM xdat_user_groupid WHERE groupid='" + group_id + "' AND groups_groupid_xdat_user_xdat_user_id=" + newUser.getXdatUserId() + ";";
    	XFTTable t=XFTTable.Execute(confirmquery,currentUser.getDBName(), currentUser.getUsername());
    	if(t.size()>0){
    		final String query = "DELETE FROM xdat_user_groupid WHERE groupid='" + group_id + "' AND groups_groupid_xdat_user_xdat_user_id=" + newUser.getXdatUserId() + ";";

            PoolDBUtils.ExecuteNonSelectQuery(query,getDBName(), currentUser.getLogin());

            PoolDBUtils.PerformUpdateTrigger(newUser.getItem(), currentUser.getLogin());
    	}
    }

    public void initGroups(){
    	final long startTime = Calendar.getInstance().getTimeInMillis();
    	final ArrayList<XdatUsergroup> owner = XdatUsergroup.getXdatUsergroupsByField(XdatUsergroup.SCHEMA_ELEMENT_NAME +".ID", this.getId() + "_" + OWNER_GROUP, this.getUser(), true);

    	final ArrayList<ElementSecurity> ess = new ArrayList<ElementSecurity>();
        try {
        	ess.addAll(ElementSecurity.GetElementSecurities().values());

//            for (final XnatAbstractprotocol protocol: this.getStudyprotocol()){
//                if (securedElements.containsKey(protocol.getDataType())){
//                    ess.add(securedElements.remove(protocol.getDataType()));
//                }
//            }

        } catch (Exception e2) {
            logger.error("",e2);
        }
        if (XFT.VERBOSE)System.out.println("Group init() BEGIN: " + (Calendar.getInstance().getTimeInMillis()-startTime) + "ms");

        for (final ElementSecurity es:(ArrayList<ElementSecurity>)ess.clone())
        {
            try {
				if (es.isSecure() && (es.getSchemaElement().getGenericXFTElement().instanceOf("xnat:subjectData") || es.getSchemaElement().getGenericXFTElement().instanceOf("xnat:experimentData"))){

				    es.initPSF(es.getElementName() + "/project");
				    es.initPSF(es.getElementName() + "/sharing/share/project");
				}else{
				    ess.remove(es);
				}
			} catch (Exception e) {
	            logger.error("",e);
			}
        }

        if (XFT.VERBOSE)System.out.println("Group init() PREP: " + (Calendar.getInstance().getTimeInMillis()-startTime) + "ms");

        //init owners
        XdatUsergroup group = null;
        if (owner.size()==0){
            try {
                group = new XdatUsergroup((UserI)this.getUser());
                group.setId(getId() + "_" + OWNER_GROUP);
                group.setDisplayname("Owners");
                group.setTag(getId());
                group.save(this.getUser(), true, true);
            } catch (Exception e) {
                logger.error("",e);
            }
        }else{
            group = (XdatUsergroup)owner.get(0);
        }

        try {
            group.setPermissions("xnat:projectData", "xnat:projectData/ID", getId(), Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, (XDATUser)this.getUser(),false);

            Iterator iter = ess.iterator();
            while (iter.hasNext())
            {
                ElementSecurity es = (ElementSecurity)iter.next();


                if(group.setPermissions(es.getElementName(),es.getElementName() + "/project", getId(), Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, (XDATUser)this.getUser(),false)){
                	//ownerMod = true;
                }
                     
                if(group.setPermissions(es.getElementName(),es.getElementName() + "/sharing/share/project", getId(), Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, (XDATUser)this.getUser(),false)){
                	//ownerMod = true;
                }
            }
        } catch (Exception e) {
            logger.error("",e);
        }
        if (XFT.VERBOSE)System.out.println("Group init() OWNER: " + (Calendar.getInstance().getTimeInMillis()-startTime) + "ms");

        try {
			group.activate(this.getUser());
		} catch (Exception e2) {
            logger.error("",e2);
		}

        try {
			EventManager.Trigger(XdatUsergroup.SCHEMA_ELEMENT_NAME,group.getId(),Event.UPDATE);
		} catch (Exception e1) {
            logger.error("",e1);
		}

        //init members
        ArrayList member = XdatUsergroup.getXdatUsergroupsByField(XdatUsergroup.SCHEMA_ELEMENT_NAME +".ID", this.getId() + "_" + MEMBER_GROUP, this.getUser(), true);

        group = null;

        if (member.size()==0){
            try {
                group = new XdatUsergroup((UserI)this.getUser());
                group.setId(getId() + "_" + MEMBER_GROUP);
                group.setDisplayname("Members");
                group.setTag(getId());
                group.save(this.getUser(), true, true);
            } catch (Exception e) {
                logger.error("",e);
            }
        }else{
            group = (XdatUsergroup)member.get(0);
        }

        try {
            group.setPermissions("xnat:projectData", "xnat:projectData/ID", getId(), Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, (XDATUser)this.getUser(),false);

            Iterator iter = ess.iterator();
            while (iter.hasNext())
            {
                ElementSecurity es = (ElementSecurity)iter.next();

                group.setPermissions(es.getElementName(),es.getElementName() + "/project", getId(), Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, (XDATUser)this.getUser(),false);
                group.setPermissions(es.getElementName(),es.getElementName() + "/sharing/share/project", getId(), Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, (XDATUser)this.getUser(),false);
            }
        } catch (Exception e) {
            logger.error("",e);
        }

        try {
			EventManager.Trigger(XdatUsergroup.SCHEMA_ELEMENT_NAME,group.getId(),Event.UPDATE);
		} catch (Exception e1) {
            logger.error("",e1);
		}

        try {
			group.activate(this.getUser());
		} catch (Exception e2) {
            logger.error("",e2);
		}

        if (XFT.VERBOSE)System.out.println("Group init() MEMBER: " + (Calendar.getInstance().getTimeInMillis()-startTime) + "ms");

        //init collaborators
        ArrayList collabs = XdatUsergroup.getXdatUsergroupsByField(XdatUsergroup.SCHEMA_ELEMENT_NAME +".ID", this.getId() + "_" + COLLABORATOR_GROUP, this.getUser(), true);

        group = null;
        if (collabs.size()==0){
            try {
                group = new XdatUsergroup((UserI)this.getUser());
                group.setId(getId() + "_" + COLLABORATOR_GROUP);
                group.setDisplayname("Collaborators");
                group.setTag(getId());
                group.save(this.getUser(), true, true);
            } catch (Exception e) {
                logger.error("",e);
            }
        }else{
            group = (XdatUsergroup)collabs.get(0);
        }


        try {
            group.setPermissions("xnat:projectData", "xnat:projectData/ID", getId(), Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, (XDATUser)this.getUser(),false);

            Iterator iter = ess.iterator();
            while (iter.hasNext())
            {
                ElementSecurity es = (ElementSecurity)iter.next();

                group.setPermissions(es.getElementName(),es.getElementName() + "/project", getId(), Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, (XDATUser)this.getUser(),false);
                group.setPermissions(es.getElementName(),es.getElementName() + "/sharing/share/project", getId(), Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, (XDATUser)this.getUser(),false);
            }
        } catch (Exception e) {
            logger.error("",e);
        }

        try {
			EventManager.Trigger(XdatUsergroup.SCHEMA_ELEMENT_NAME,group.getId(),Event.UPDATE);
		} catch (Exception e1) {
            logger.error("",e1);
		}

        try {
			group.activate(this.getUser());
		} catch (Exception e2) {
            logger.error("",e2);
		}

        if (XFT.VERBOSE)System.out.println("Group init(): " + (Calendar.getInstance().getTimeInMillis()-startTime) + "ms");
    }

    public static void quickSave(XnatProjectdata project, XDATUser user,boolean allowDataDeletion,boolean overrideSecurity) throws Exception{
    	project.initNewProject(user,allowDataDeletion,true);

	
		project.save(user,overrideSecurity,false);
		XFTItem item =project.getItem().getCurrentDBVersion(false);
		
		XnatProjectdata postSave = new XnatProjectdata(item);
	    postSave.getItem().setUser(user);
	
	    postSave.initGroups();
	        
	    user.refreshGroup(postSave.getId() + "_" + BaseXnatProjectdata.OWNER_GROUP);
	
	    postSave.initArcProject(null, user);
	
	    user.clearLocalCache();
		MaterializedView.DeleteByUser(user);
	    ElementSecurity.refresh();
    }

    public XnatAbstractprotocol getProtocolByDataType(String elementName){
        Iterator iter = this.getStudyprotocol().iterator();
        while (iter.hasNext()){
            XnatAbstractprotocol protocol = (XnatAbstractprotocol)iter.next();
            if (protocol.getDataType().equals(elementName)){
                return protocol;
            }
        }

        return null;
    }


    public ArrayList<XdatStoredSearch> getBundles(){
    	ArrayList<XdatStoredSearch> searches = XdatStoredSearch.GetSearches("xdat:stored_search/tag", this.getId(), true);

    	Hashtable<String,Long> counts=this.getExperimentCountByXSIType();

    	boolean matched=false;
    	for(XdatStoredSearch xss:searches){
    		if(xss.getRootElementName().equalsIgnoreCase("xnat:subjectData")){
    			matched=true;
    			break;
    		}
    	}

    	if(!matched){
    		XnatAbstractprotocol protocol = this.getProtocolByDataType("xnat:subjectData");
    		XdatStoredSearch xss=null;
    		if(protocol!=null){
        		xss=protocol.getDefaultSearch((XnatProjectdata)this);
    		}else{
        		xss=this.getDefaultSearch("xnat:subjectData");
    		}
    		xss.setId("@xnat:subjectData");
    		searches.add(xss);
    	}

    	for(String key:counts.keySet()){
    		matched=false;
        	for(XdatStoredSearch xss:searches){
        		if(xss.getRootElementName().equalsIgnoreCase(key)){
        			matched=true;
        			break;
        		}
        	}

        	if(!matched){
        		XnatAbstractprotocol protocol = this.getProtocolByDataType(key);
        		XdatStoredSearch xss=null;
        		if(protocol!=null){
            		xss=protocol.getDefaultSearch((XnatProjectdata)this);
        		}else{
            		xss=this.getDefaultSearch(key);
        		}
        		xss.setId("@" + key);
        		searches.add(xss);
        	}
    	}

        return searches;
    }



    public XdatStoredSearch getDefaultSearch(String elementName){
    	XdatStoredSearch xss=null;
        try {
            xss=this.getDefaultSearch(elementName,this.getId()+"_"+elementName);

			xss.setId(this.getId()+"_"+elementName);

			ElementSecurity es=ElementSecurity.GetElementSecurity(elementName);

			if(es!=null)
				xss.setBriefDescription(es.getPluralDescription());
			else{
				xss.setBriefDescription(elementName);
			}
			xss.setSecure(false);
			xss.setAllowDiffColumns(false);
			xss.setTag(this.getId());

            UserI user = this.getUser();

            XnatAbstractprotocol protocol = this.getProtocolByDataType(elementName);
    		if(protocol!=null){
    			if(protocol instanceof XnatDatatypeprotocol)
    			for(XnatFielddefinitiongroupI group : ((XnatDatatypeprotocol)protocol).getDefinitions_definition()){
    	            for(XnatFielddefinitiongroupFieldI field : group.getFields_field()){

    	                XdatSearchField xsf = new XdatSearchField(this.getUser());
    	                xsf.setElementName(((XnatDatatypeprotocol)protocol).getDataType());
    	                String fieldID=null;
    	                if (field.getType().equals("custom"))
    	                {
    	                    fieldID=((XnatDatatypeprotocol)protocol).getDatatypeSchemaElement().getSQLName().toUpperCase() + "_FIELD_MAP="+field.getName().toLowerCase();

    	                }else{
    	                    try {
    	                        SchemaElement se=SchemaElement.GetElement(((XnatDatatypeprotocol)protocol).getDataType());

    	                        try {
    	                            DisplayField df=se.getDisplayFieldForXMLPath(field.getXmlpath());
    	                            if (df!=null){
    	                                fieldID=df.getId();
    	                            }
    	                        } catch (Exception e) {
    	                            logger.error("",e);
    	                        }
    	                    } catch (XFTInitException e) {
    	                        logger.error("",e);
    	                    } catch (ElementNotFoundException e) {
    	                        logger.error("",e);
    	                    }
    	                }

    	                if (fieldID!=null){
    	                    xsf.setFieldId(fieldID);

    	                    xsf.setHeader(field.getName());
    	                    xsf.setType(field.getDatatype());
    	                    xsf.setSequence(xss.getSearchField().size());
    	                    if (field.getType().equals("custom"))xsf.setValue(field.getName().toLowerCase());
    	                    try {
    	                        xss.setSearchField(xsf);
    	                    	System.out.println("LOADED " + field.getXmlpath());
    	                    } catch (Exception e) {
    	                        logger.error("",e);
    	                    	System.out.println("FAILED to load " + field.getXmlpath());
    	                    }
    	                }else{
    	                	System.out.println("FAILED to load " + field.getXmlpath());
    	                }
    	            }
    	        }
    		}

            if (elementName.equals("xnat:subjectData")){
                for(String xsiType:this.getExperimentCountByXSIType().keySet()){
                    try {
                        final GenericWrapperElement e = GenericWrapperElement.GetElement(xsiType);
                        if (e.instanceOf("xnat:subjectAssessorData"))
                        {
                            XdatSearchField xsf = new XdatSearchField(user);
                            xsf.setElementName("xnat:subjectData");
                            xsf.setFieldId("SUB_EXPT_COUNT=" + xsiType);

                            xsf.setHeader(ElementSecurity.GetPluralDescription(xsiType));
                            xsf.setType("integer");
                            xsf.setSequence(xss.getSearchField().size());
                            xsf.setValue(xsiType);
                            xss.setSearchField(xsf);
                        }
                    } catch (XFTInitException e) {
                        logger.error("",e);
                    } catch (ElementNotFoundException e) {
                        logger.error("",e);
                    } catch (Exception e) {
                        logger.error("",e);
                    }
                }
            }

            if (elementName.equals("xnat:mrSessionData")){
                for(String xsiType:this.getExperimentCountByXSIType().keySet()){
                    try {
                        final GenericWrapperElement e = GenericWrapperElement.GetElement(xsiType);
                        if (e.instanceOf("xnat:mrAssessorData"))
                        {
                            XdatSearchField xsf = new XdatSearchField(user);
                            xsf.setElementName("xnat:mrSessionData");
                            xsf.setFieldId("MR_EXPT_COUNT=" + xsiType);

                            xsf.setHeader(ElementSecurity.GetPluralDescription(xsiType));
                            xsf.setType("integer");
                            xsf.setSequence(xss.getSearchField().size());
                            xsf.setValue(xsiType);
                            xss.setSearchField(xsf);
                        }
                    } catch (XFTInitException e) {
                        logger.error("",e);
                    } catch (ElementNotFoundException e) {
                        logger.error("",e);
                    } catch (Exception e) {
                        logger.error("",e);
                    }
                }
            }

            if (elementName.equals("xnat:petSessionData")){
                for(String xsiType:this.getExperimentCountByXSIType().keySet()){
                    try {
                        final GenericWrapperElement e = GenericWrapperElement.GetElement(xsiType);
                        if (e.instanceOf("xnat:petAssessorData"))
                        {
                            XdatSearchField xsf = new XdatSearchField(user);
                            xsf.setElementName("xnat:petSessionData");
                            xsf.setFieldId("PET_EXPT_COUNT=" + xsiType);

                            xsf.setHeader(ElementSecurity.GetPluralDescription(xsiType));
                            xsf.setType("integer");
                            xsf.setSequence(xss.getSearchField().size());
                            xsf.setValue(xsiType);
                            xss.setSearchField(xsf);
                        }
                    } catch (XFTInitException e) {
                        logger.error("",e);
                    } catch (ElementNotFoundException e) {
                        logger.error("",e);
                    } catch (Exception e) {
                        logger.error("",e);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("",e);
        }

        return xss;
    }
//
//    public void initBundles(XDATUser u){
//        long startTime = Calendar.getInstance().getTimeInMillis();
//        try {
//            XFTTable t = XFTTable.Execute("SELECT * FROM xdat_stored_search WHERE tag='" + this.getId() + "'", this.getDBName(), null);
//
//            ArrayList existing = t.convertColumnToArrayList("id");
//            boolean modified = false;
//            Iterator protocols= this.getStudyprotocol().iterator();
//            while(protocols.hasNext()){
//                XnatAbstractprotocol protocol = (XnatAbstractprotocol)protocols.next();
//
//                if (!existing.contains(protocol.getId())){
//                    //CREATE BUNDLE
//                    try {
//                        XdatStoredSearch xss = protocol.getDefaultSearch((XnatProjectdata)this);
//
//
//                        xss.save(this.getUser(), true, true);
//
//
//                        //XdatStoredSearch.GetPreLoadedSearches().add(xss);
//                        modified=true;
//
//                        final String[] groups = {getId() + "_" + OWNER_GROUP,getId() + "_" + MEMBER_GROUP,getId() + "_" + COLLABORATOR_GROUP};
//
//                        for(int i=0;i<groups.length;i++){
//                            String group = groups[i];
//                            XdatUsergroup g =UserGroupManager.GetGroup(group);
//                            if (g!=null && g.getStoredSearches().size()>0)
//                            {
//                                g.getStoredSearches().add(xss);
//                            }
//                        }
//                    } catch (XFTInitException e) {
//                        logger.error("",e);
//                    } catch (ElementNotFoundException e) {
//                        logger.error("",e);
//                    } catch (Exception e) {
//                        logger.error("",e);
//                    }
//                }
//
//            }
//
//        } catch (SQLException e) {
//            logger.error("",e);
//        } catch (DBPoolException e) {
//            logger.error("",e);
//        } catch (Exception e) {
//            logger.error("",e);
//        }
//        if (XFT.VERBOSE)System.out.println("Bundle init(): " + (Calendar.getInstance().getTimeInMillis()-startTime) + "ms");
//    }

    public String getPublicAccessibility() throws Exception{
        String query = "SELECT read_element FROM xdat_element_access ea LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id=fms.permissions_allow_set_xdat_elem_xdat_element_access_id LEFT JOIN xdat_user u ON ea.xdat_user_xdat_user_id=u.xdat_user_id LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id=fm.xdat_field_mapping_set_xdat_field_mapping_set_id WHERE login='guest' AND element_name='xnat:projectData' AND field_value='" + getId() +  "';";

        Integer projectAccess = (Integer)PoolDBUtils.ReturnStatisticQuery(query, "read_element", getDBName(), null);

        if (projectAccess!=null)
        {
            if (projectAccess==1){

                query = "SELECT read_element FROM xdat_element_access ea LEFT JOIN xdat_field_mapping_set fms ON ea.xdat_element_access_id=fms.permissions_allow_set_xdat_elem_xdat_element_access_id LEFT JOIN xdat_user u ON ea.xdat_user_xdat_user_id=u.xdat_user_id LEFT JOIN xdat_field_mapping fm ON fms.xdat_field_mapping_set_id=fm.xdat_field_mapping_set_xdat_field_mapping_set_id WHERE login='guest' AND element_name!='xnat:projectData' AND field_value='" + getId() +  "';";

                Integer subjectAccess = (Integer)PoolDBUtils.ReturnStatisticQuery(query, "read_element", getDBName(), null);
                if (subjectAccess!=null)
                {
                    if (subjectAccess==1){
                        return "public";
                    }else{
                        return "protected";
                    }
                }else{
                    return "protected";
                }
            }else{
                return "private";
            }
        }else{
        	//DEFAULT
            return "private";
        }
    }

    public void initAccessibility(String accessibility, boolean forceInit){
        try {
            if (accessibility!=null){
                if ((!accessibility.equals(this.getPublicAccessibility())) || forceInit)
                {
                    XDATUser guest=null;
                    try {
                        guest = new XDATUser("guest");
                    } catch (UserNotFoundException e) {
                        XFTItem i = XFTItem.NewItem("xdat:user",this.getUser());
                        i.setProperty("login","guest");
                        i.setProperty("enabled", 0);
                        i.save(this.getUser(), true, false);

                        guest = new XDATUser("guest");
                    }
                    ArrayList<ElementSecurity> securedElements = ElementSecurity.GetSecureElements();

                    if (accessibility.equals("public"))
                    {
                        //public
                        guest.setPermissions("xnat:projectData", "xnat:projectData/ID", this.getId(), false, true, false, false, true,true);

                        for (ElementSecurity es: securedElements)
                        {
                        	if (es.hasField(es.getElementName() + "/project") && es.hasField(es.getElementName() + "/sharing/share/project")){
                        		guest.setPermissions(es.getElementName(),es.getElementName() + "/project", this.getId(), false, true, false, false, true,true);
                        		guest.setPermissions(es.getElementName(),es.getElementName() + "/sharing/share/project", this.getId(), false, false, false, false, false,true);
                        	}
                        }
                    }else if (accessibility.equals("protected"))
                    {
                        //protected
                        guest.setPermissions("xnat:projectData", "xnat:projectData/ID", this.getId(), false, true, false, false, false,true);
                        for (ElementSecurity es: securedElements)
                        {
                        	if (es.hasField(es.getElementName() + "/project") && es.hasField(es.getElementName() + "/sharing/share/project")){
                        		guest.setPermissions(es.getElementName(),es.getElementName() + "/project", this.getId(),  false, false, false, false, false,true);
                        		guest.setPermissions(es.getElementName(),es.getElementName() + "/sharing/share/project", this.getId(),  false, false, false, false, false,true);
                        	}
                        }
                    }else
                    {
                        //private
                        guest.setPermissions("xnat:projectData", "xnat:projectData/ID", this.getId(), false, false, false, false, false,true);
                        for (ElementSecurity es: securedElements)
                        {
                        	if (es.hasField(es.getElementName() + "/project") && es.hasField(es.getElementName() + "/sharing/share/project")){
                        		guest.setPermissions(es.getElementName(),es.getElementName() + "/project", this.getId(),  false, false, false, false, false,true);
                        		guest.setPermissions(es.getElementName(),es.getElementName() + "/sharing/share/project", this.getId(),  false, false, false, false, false,true);
                        	}
                        }
                    }
                }
            }
        } catch (UserNotFoundException e) {
            logger.error("",e);
        } catch (XFTInitException e) {
            logger.error("",e);
        } catch (ElementNotFoundException e) {
            logger.error("",e);
        } catch (DBPoolException e) {
            logger.error("",e);
        } catch (SQLException e) {
            logger.error("",e);
        } catch (FieldNotFoundException e) {
            logger.error("",e);
        } catch (InvalidValueException e) {
            logger.error("",e);
        } catch (InvalidItemException e) {
            logger.error("",e);
        } catch (Exception e) {
            logger.error("",e);
        }
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.ItemWrapper#save(org.nrg.xft.security.UserI, boolean, boolean)
     */
    @Override
    public boolean save(UserI user, boolean overrideSecurity, boolean allowItemRemoval) throws Exception {
        XdatUsergroup group = new XdatUsergroup((UserI)this.getUser());
        group.setId(getId() + "_" + OWNER_GROUP);
        group.setDisplayname("Owners");
        group.setTag(getId());

        UserGroup ownerG=UserGroupManager.GetGroup(group.getId());
        if(ownerG==null){
        	group.save(this.getUser(), true, true);

            group.setPermissions("xnat:projectData", "xnat:projectData/ID", getId(), Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, (XDATUser)user,false);

            if(!((XDATUser)user).getGroups().containsKey(group.getId())){
                UserGroup ug= new UserGroup(group.getId());
                ug.init(group);
                ((XDATUser)user).getGroups().put(group.getId(),ug);

                this.addGroupMember(this.getId() + "_" + OWNER_GROUP, (XDATUser)user, (XDATUser)user);
            }
        }

        return super.save(user, overrideSecurity, allowItemRemoval);
    }

    public String getDisplayName(){
        if (this.getSecondaryId()==null)
            return this.getId();
        else
            return this.getSecondaryId();
    }

    public String getDisplayID(){
        if (this.getSecondaryId()==null)
            return this.getId();
        else
            return this.getSecondaryId();
    }

    public ArcProject getArcSpecification(){
        return ArcSpecManager.GetInstance().getProjectArc(getId());
    }


    public static Comparator GetComparator()
    {
        return (new BaseXnatProjectdata()).getComparator();
    }

    public Comparator getComparator()
    {
        return new ProjectIDComparator();
    }
    public class ProjectIDComparator implements Comparator{
        public ProjectIDComparator()
        {
        }
        public int compare(Object o1, Object o2) {
            BaseXnatProjectdata  value1 = (BaseXnatProjectdata)(o1);
            BaseXnatProjectdata value2 = (BaseXnatProjectdata)(o2);

            if (value1 == null){
                if (value2 == null)
                {
                    return 0;
                }else{
                    return -1;
                }
            }
            if (value2== null)
            {
                return 1;
            }

            return value1.getId().compareTo(value2.getId());

        }
    }

    public void initNewProject(XDATUser user,boolean allowDataDeletion,boolean allowMatchingID) throws Exception{
    	if (this.getId()==null){
            String secondaryID = this.getSecondaryId();
            if (secondaryID==null){

                throw new Exception("Please define a project abbreviation.");
            }else{
                setId(secondaryID);

                XFTItem db=getCurrentDBVersion();
                if (db!=null){
                	String msg="Project '" + getId() + "' already exists.";
                	this.setId("");
                	throw new Exception(msg);
                }
            }
        }else{
        	if(!allowMatchingID){
                XFTItem db=getCurrentDBVersion();
                if (db!=null){
                	String msg="Project '" + getId() + "' already exists.";
                	this.setId("");
                	throw new Exception(msg);
                }
        	}
        }

    	if(this.getSecondaryId()==null){
    		this.setSecondaryId(this.getId());
    	}

    	if(this.getName()==null){
    		this.setName(this.getId());
    	}

    	if(this.getStudyprotocol().size()>0){
        	Hashtable<String,ElementSecurity> ess = ElementSecurity.GetElementSecurities();

            int index = 0;
            for (XnatAbstractprotocolI  protocolT:this.getStudyprotocol()){
            	XnatAbstractprotocol protocol=(XnatAbstractprotocol)protocolT;
                if(protocol.getProperty("data-type")==null){
                	if(allowDataDeletion){
                        //NOT REQUESTED
                        if (protocol.getProperty("xnat_abstractProtocol_id")!=null){
                            try {
                            	getItem().getCurrentDBVersion().removeChildFromDB("xnat:projectData/studyProtocol", protocol.getCurrentDBVersion(), user);
                            } catch (SQLException e) {
                                logger.error("",e);
                            } catch (Exception e) {
                                logger.error("",e);
                            }
                        }

                        getItem().removeChild("xnat:projectData/studyProtocol", getItem().getChildItems("xnat:projectData/studyProtocol").indexOf(protocol));
                	}
                }else{
                    //REQUESTED
                    GenericWrapperElement e = GenericWrapperElement.GetElement((String)protocol.getProperty("data-type"));
                    if (protocol.getProperty("ID")==null){
                        try {
                            protocol.setProperty("ID", getItem().getProperty("ID") + "_" + e.getSQLName());
                        } catch (InvalidValueException e1) {
                            logger.error("",e1);
                        }
                    }
                    if (protocol.getProperty("name")==null){
                        protocol.setProperty("name",ess.get(e.getFullXMLName()).getPluralDescription());
                    }

                    if(protocol.getXSIType().equals("xnat:datatypeProtocol")){
                        protocol.setProperty("xnat:datatypeProtocol/definitions/definition[ID=default]/data-type", protocol.getProperty("data-type"));
                        protocol.setProperty("xnat:datatypeProtocol/definitions/definition[ID=default]/project-specific", "false");
                    }
                }
                index++;
            }
    	}

        for(XnatInvestigatordataI inv:this.getInvestigators_investigator()){
        	if(inv.getFirstname()==null){
        		XFTItem temp = ((XnatInvestigatordata)inv).getCurrentDBVersion();
        		((XnatInvestigatordata)inv).setFirstname(temp.getStringProperty("firstname"));
        		((XnatInvestigatordata)inv).setLastname(temp.getStringProperty("lastname"));
        	}
        }

        for(XnatInvestigatordataI inv:this.getInvestigators_investigator()){
        	if(inv.getFirstname()==null){
        		XFTItem temp = ((XnatInvestigatordata)inv).getCurrentDBVersion();
        		((XnatInvestigatordata)inv).setFirstname(temp.getStringProperty("firstname"));
        		((XnatInvestigatordata)inv).setLastname(temp.getStringProperty("lastname"));
        	}
        }
    }

    public void initArcProject(ArcProject arcP, XDATUser user) throws Exception{
    	if(arcP==null){
    		 XFTItem item = XFTItem.NewItem("arc:project", user);
    	     arcP = new ArcProject(item);
    	     arcP.setCurrentArc("arc001");
    	}
    	arcP.setProperty("projects_project_arc_archivespe_arc_archivespecification_id", ArcSpecManager.GetInstance().getArcArchivespecificationId());
        arcP.setId(getId());

        arcP.setProperty("arc:project/paths/archivePath", ArcSpecManager.GetInstance().getGlobalArchivePath() + getId()+"/");
        arcP.setProperty("arc:project/paths/prearchivePath", ArcSpecManager.GetInstance().getGlobalPrearchivePath() + getId()+"/");
        arcP.setProperty("arc:project/paths/cachePath", ArcSpecManager.GetInstance().getGlobalCachePath() + getId()+"/");
        arcP.setProperty("arc:project/paths/buildPath", ArcSpecManager.GetInstance().getGlobalBuildPath() + getId()+"/");

        arcP.save(user, true, false);
        ArcSpecManager.Reset();
    }

    public XdatStoredSearch getDefaultSearch(String dataType,String id){
    	XdatStoredSearch xss=null;
    	try {
			DisplaySearch search = new DisplaySearch();
			search.setDisplay("project_bundle");
			search.setRootElement(dataType);
			CriteriaCollection cc = new CriteriaCollection("OR");
			cc.addClause(dataType+"/sharing/share/project", "=", getId());
			cc.addClause(dataType+".PROJECT", "=", getId());

			search.addCriteria(cc);

			xss = search.convertToStoredSearch(id);

			Iterator fields = xss.getSearchFields().iterator();
			while (fields.hasNext()){
			    XdatSearchField f = (XdatSearchField)fields.next();
			    if (f.getFieldId().endsWith("_PROJECT_IDENTIFIER")){
			        f.setValue(getId());
			        f.setFieldId(f.getFieldId()+"=" + getId());
			    }
			}
		} catch (XFTInitException e) {
            logger.error("",e);
		} catch (ElementNotFoundException e) {
            logger.error("",e);
		} catch (FieldNotFoundException e) {
            logger.error("",e);
		} catch (Exception e) {
            logger.error("",e);
		}
		return xss;
    }

    public static String CleanID(String s){
    	s=s.replace('`', '_');
    	s=s.replace('~', '_');
    	s=s.replace('!', '_');
    	s=s.replace('@', '_');
    	s=s.replace('#', '_');
    	s=s.replace('$', '_');
    	s=s.replace('%', '_');
    	s=s.replace('^', '_');
    	s=s.replace('&', '_');
    	s=s.replace('*', '_');
    	s=s.replace('(', '_');
    	s=s.replace(')', '_');
    	s=s.replace('+', '_');
    	s=s.replace('=', '_');
    	s=s.replace('|', '_');
    	s=s.replace('\\', '_');
    	s=s.replace('{', '_');
    	s=s.replace('[', '_');
    	s=s.replace('}', '_');
    	s=s.replace(']', '_');
    	s=s.replace(':', '_');
    	s=s.replace(';', '_');
    	s=s.replace('"', '_');
    	s=s.replace('\'', '_');
    	s=s.replace('<', '_');
    	s=s.replace('>', '_');
    	s=s.replace('?', '_');
    	s=s.replace(',', '_');
    	s=s.replace('.', '_');
    	s=s.replace('/', '_');
    	return s;
    }



    public String canDelete(XDATUser user) {
    	try {
			if(!user.canDelete(this.getItem())){
				return "Invalid delete permissions for this project.";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "Invalid delete permissions for this project.";
		}

		for(XnatSubjectdata sub: this.getParticipants_participant()){
			String msg=sub.canDelete(this,user);
			if(msg!=null){
				return msg;
			}
		}

		return null;
    }

    public void deleteFiles() throws IOException{
    	String archive=this.getRootArchivePath();
    	File dir=new File(archive);
    	if(dir.exists()){
    		FileUtils.MoveToCache(dir);
    	}

    	for(XnatAbstractresourceI abstRes:this.getResources_resource()){
    		((XnatAbstractresource)abstRes).deleteFromFileSystem(archive);
    	}
    }

//    public String delete(XDATUser user, boolean removeFiles){
//    	String msg=this.canDelete(user);
//    	if(msg!=null){
//    		logger.error(msg);
//    		return msg;
//    	}
//
//		try {
//
//			if(!user.canDelete(this)){
//				return "User account doesn't have permission to delete this project.";
//			}
//
//			if(removeFiles){
//				this.deleteFiles();
//			}
//
//			final  ArrayList<XnatSubjectdata> subs = this.getParticipants_participant();
//	        for (XnatSubjectdata sub : subs){
//	            msg=sub.delete(this,user,removeFiles);
//	            if(msg!=null)return msg;
//	        }
//
//	        user.clearLocalCache();
//			MaterializedView.DeleteByUser(user);
//
//	        DBAction.DeleteItem(getItem().getCurrentDBVersion(), user);
//
//	        //DELETE field mappings
//	        ItemSearch is = ItemSearch.GetItemSearch("xdat:field_mapping", user);
//	        is.addCriteria("xdat:field_mapping.field_value", getId());
//	        Iterator items = is.exec(false).iterator();
//	        while (items.hasNext())
//	        {
//	            XFTItem item = (XFTItem)items.next();
//	            DBAction.DeleteItem(item, user);
//	        }
//
//	        //DELETE user.groupId
//	        CriteriaCollection col = new CriteriaCollection("AND");
//	        col.addClause(XdatUserGroupid.SCHEMA_ELEMENT_NAME +".groupid"," LIKE ", getId() + "_%");
//	        Iterator groups = XdatUserGroupid.getXdatUserGroupidsByField(col, user, false).iterator();
//
//	        while(groups.hasNext()){
//	            XdatUserGroupid g = (XdatUserGroupid)groups.next();
//	            try {
//	                DBAction.DeleteItem(g.getItem(), user);
//	            } catch (Throwable e) {
//	                logger.error("",e);
//	            }
//	        }
//
//	        //DELETE user groups
//	        col = new CriteriaCollection("AND");
//	        col.addClause(XdatUsergroup.SCHEMA_ELEMENT_NAME +".ID"," LIKE ", getId() + "_%");
//	        groups = XdatUsergroup.getXdatUsergroupsByField(col, user, false).iterator();
//
//	        while(groups.hasNext()){
//	            XdatUsergroup g = (XdatUsergroup)groups.next();
//	            try {
//	                DBAction.DeleteItem(g.getItem(), user);
//	            } catch (Throwable e) {
//	                logger.error("",e);
//	            }
//	        }
//
//	        //DELETE storedSearches
//	        Iterator bundles=getBundles().iterator();
//	        while (bundles.hasNext())
//	        {
//	            ItemI bundle = (ItemI)bundles.next();
//	            try {
//	                DBAction.DeleteItem(bundle.getItem(), user);
//	            } catch (Throwable e) {
//	                logger.error("",e);
//	            }
//	        }
//
//	        ArcProject p =getArcSpecification();
//	        try {
//	            if (p!=null)DBAction.DeleteItem(p.getItem(), user);
//	        } catch (Throwable e) {
//	            logger.error("",e);
//	        }
//
//
//	        try {
//				EventManager.Trigger(XdatUsergroup.SCHEMA_ELEMENT_NAME,Event.UPDATE);
//			} catch (Exception e1) {
//	            logger.error("",e1);
//			}
//		} catch (SQLException e) {
//			logger.error("",e);
//			return e.getMessage();
//		} catch (Exception e) {
//			logger.error("",e);
//			return e.getMessage();
//		}
//    	return null;
//    }

    public void delete(boolean removeFiles, XDATUser user)throws SQLException,Exception{
    	boolean preventProjectDelete=false;
        boolean preventProjectDeleteByP=false;
    	for (XnatSubjectdata subject : getParticipants_participant()){
            if (subject!=null){
            	boolean preventSubjectDelete=false;
            	boolean preventSubjectDeleteByP=false;
               final  List<XnatSubjectassessordataI> expts = subject.getExperiments_experiment();

               if(!(preventSubjectDelete || preventSubjectDeleteByP) && expts.size()!=subject.getSubjectAssessorCount()){
               	preventSubjectDelete=true;
               }

                for (XnatSubjectassessordataI exptI : expts){
                    final XnatSubjectassessordata expt = (XnatSubjectassessordata)exptI;

                    if(expt.getProject().equals(getId())){
                        if(user.canDelete(expt)){
                        	expt.deleteWorkflowEntries(expt.getId(), user);
                        	
                            if (removeFiles){
                            	final List<XFTItem> hash = expt.getItem().getChildrenOfType("xnat:abstractResource");

                                for (XFTItem resource : hash){
                                    ItemI om = BaseElement.GetGeneratedItem((XFTItem)resource);
                                    if (om instanceof XnatAbstractresource){
                                        XnatAbstractresource resourceA = (XnatAbstractresource)om;
                                        resourceA.deleteFromFileSystem(getRootArchivePath());
                                    }
                                }
                            }

                        	DBAction.DeleteItem(expt.getItem().getCurrentDBVersion(), user);
                        }else{
                        	preventSubjectDeleteByP=true;
                        }
                    }else{
                    	preventSubjectDelete=true;
                    	for(XnatExperimentdataShareI pp : expt.getSharing_share()){
                    		if(pp.getProject().equals(getId())){
                    			DBAction.DeleteItem(((XnatExperimentdataShare)pp).getItem(),user);
                    		}
                    	}
                    }

                }


            	if(!subject.getProject().equals(getId())){
            		for(XnatProjectparticipantI pp : subject.getSharing_share()){
                		if(pp.getProject().equals(getId())){
                			DBAction.DeleteItem(((XnatExperimentdataShare)pp).getItem(),user);
                		}
                	}
            	}else{
                	if(preventSubjectDelete){
                		preventProjectDelete=true;
                	}else if(preventSubjectDeleteByP){
                		preventProjectDeleteByP=true;
                	}else{
                		if(user.canDelete(subject)){
                            if (removeFiles){
                            	final List<XFTItem> hash = subject.getItem().getChildrenOfType("xnat:abstractResource");

                                for (XFTItem resource : hash){
                                    ItemI om = BaseElement.GetGeneratedItem((XFTItem)resource);
                                    if (om instanceof XnatAbstractresource){
                                        XnatAbstractresource resourceA = (XnatAbstractresource)om;
                                        resourceA.deleteFromFileSystem(getRootArchivePath());
                                    }
                                }
                            }
                    		DBAction.DeleteItem(subject.getItem().getCurrentDBVersion(), user);
                		}else{
                			preventProjectDeleteByP=true;
                		}
                	}
            	}
            }
        }

	    user.clearLocalCache();
		MaterializedView.DeleteByUser(user);

		if (!preventProjectDelete && !preventProjectDeleteByP){
			final File arc=new File(this.getRootArchivePath());
			final File prearc=new File(this.getPrearchivePath());
			
			PrearcUtils.deleteProject(this.getId());
	        DBAction.DeleteItem(getItem().getCurrentDBVersion(), user);

	        //DELETE field mappings
	        ItemSearch is = ItemSearch.GetItemSearch("xdat:field_mapping", user);
	        is.addCriteria("xdat:field_mapping.field_value", getId());
	        Iterator items = is.exec(false).iterator();
	        while (items.hasNext())
	        {
	            XFTItem item = (XFTItem)items.next();
	            DBAction.DeleteItem(item, user);
	        }

	        //DELETE user.groupId
	        CriteriaCollection col = new CriteriaCollection("AND");
	        col.addClause(XdatUserGroupid.SCHEMA_ELEMENT_NAME +".groupid"," LIKE ", getId() + "_%");
	        Iterator groups = XdatUserGroupid.getXdatUserGroupidsByField(col, user, false).iterator();

	        while(groups.hasNext()){
	            XdatUserGroupid g = (XdatUserGroupid)groups.next();
	            try {
	                DBAction.DeleteItem(g.getItem(), user);
	            } catch (Throwable e) {
	                logger.error("",e);
	            }
	        }

	        //DELETE user groups
	        col = new CriteriaCollection("AND");
	        col.addClause(XdatUsergroup.SCHEMA_ELEMENT_NAME +".ID"," LIKE ", getId() + "_%");
	        groups = XdatUsergroup.getXdatUsergroupsByField(col, user, false).iterator();

	        while(groups.hasNext()){
	            XdatUsergroup g = (XdatUsergroup)groups.next();
	            try {
	                DBAction.DeleteItem(g.getItem(), user);
	            } catch (Throwable e) {
	                logger.error("",e);
	            }
	        }

	        //DELETE storedSearches
	        Iterator bundles=getBundles().iterator();
	        while (bundles.hasNext())
	        {
	            ItemI bundle = (ItemI)bundles.next();
	            try {
	                DBAction.DeleteItem(bundle.getItem(), user);
	            } catch (Throwable e) {
	                logger.error("",e);
	            }
	        }

	        ArcProject p =getArcSpecification();
	        try {
	            if (p!=null)DBAction.DeleteItem(p.getItem(), user);
	        } catch (Throwable e) {
	            logger.error("",e);
	        }
	        
	        try {
				if(arc.exists() && removeFiles)FileUtils.MoveToCache(arc);
			} catch (Exception e) {
	            logger.error("",e);
			}

	        try {
				EventManager.Trigger(XdatUsergroup.SCHEMA_ELEMENT_NAME,Event.UPDATE);
			} catch (Exception e1) {
	            logger.error("",e1);
			}
		}
    }


	@Override
	public void preSave() throws Exception{
		super.preSave();

		final String expectedPath=getExpectedCurrentDirectory().getAbsolutePath().replace('\\', '/');

		for(final XnatAbstractresourceI res: this.getResources_resource()){
			final String uri;
			if(res instanceof XnatResource){
				uri=((XnatResource)res).getUri();
			}else if(res instanceof XnatResourceseries){
				uri=((XnatResourceseries)res).getPath();
			}else{
				continue;
			}

			FileUtils.ValidateUriAgainstRoot(uri,expectedPath,"URI references data outside of the project:" + uri);
		}
	}



    public String getArchiveDirectoryName(){
    	return this.getId();
    }

	public File getExpectedCurrentDirectory() throws InvalidArchiveStructure {
		return new File(getRootArchivePath(),"resources");
	}
	
	public boolean isAutoArchive(){
		Integer i= ArcSpecManager.GetInstance().getAutoQuarantineCodeForProject(this.getId());
		if(i==null || i<4){
			return false;
		}else{
			return true;
}
	}
	
	public static XnatProjectdata getProjectByIDorAlias(String pID,XDATUser user,boolean preLoad){
		XnatProjectdata proj=null;
		if(pID!=null){
			proj = XnatProjectdata.getXnatProjectdatasById(pID, user, preLoad);
		}

		if (proj == null && pID!=null) {
			final ArrayList<XnatProjectdata> matches = XnatProjectdata
					.getXnatProjectdatasByField(
							"xnat:projectData/aliases/alias/alias", pID,
							user, preLoad);
			if (matches.size() == 1) {
				proj = matches.get(0);
			}
		}
		
		return proj;
	}
}
