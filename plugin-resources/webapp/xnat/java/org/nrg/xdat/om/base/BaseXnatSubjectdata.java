//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 16 15:08:17 CDT 2005
 *
 */
package org.nrg.xdat.om.base;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatAbstractdemographicdataI;
import org.nrg.xdat.om.XnatAbstractprotocol;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatAbstractsubjectmetadataI;
import org.nrg.xdat.om.XnatDatatypeprotocol;
import org.nrg.xdat.om.XnatDemographicdata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatExperimentdataShare;
import org.nrg.xdat.om.XnatFielddefinitiongroup;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatProjectdataI;
import org.nrg.xdat.om.XnatProjectparticipant;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourceseries;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.XnatSubjectdataAddid;
import org.nrg.xdat.om.XnatSubjectdataField;
import org.nrg.xdat.om.XnatSubjectdataFieldI;
import org.nrg.xdat.om.XnatSubjectmetadata;
import org.nrg.xdat.om.base.auto.AutoXnatSubjectdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.SecurityValues;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.identifier.IDGeneratorFactory;
import org.nrg.xft.identifier.IDGeneratorI;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.TableSearch;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.turbine.utils.ArchivableItem;
import org.nrg.xnat.turbine.utils.XNATUtils;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatSubjectdata extends AutoXnatSubjectdata implements ArchivableItem{
    protected ArrayList<ItemI> minLoadAssessors = null;

	public BaseXnatSubjectdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatSubjectdata(UserI user)
	{
		super(user);
	}

	public BaseXnatSubjectdata()
	{}

	public BaseXnatSubjectdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}
    
    public String getArchiveDirectoryName(){
    	if(this.getLabel()!=null)
    		return this.getLabel();
    	else 
    		return this.getId();
    }

    public String getAddIdString(){
        StringBuffer sb = new StringBuffer();
        for(int j=0;j<getAddid().size();j++)
        {
            XnatSubjectdataAddid addid =(XnatSubjectdataAddid) getAddid().get(j);
            if (j==0)
            {
                sb.append(addid.getAddid() + " (" + addid.getName() + ")");
            }else{
                sb.append(", " + addid.getAddid() + " (" + addid.getName() + ")");
            }
        }
        return sb.toString();
    }

	public String getGenderText()
	{
	    String s = null;
        try {
            XnatAbstractdemographicdataI ame = this.getDemographics();
    	    if (ame instanceof XnatDemographicdata)
    	    {
    	        s= ((XnatDemographicdata)ame).getGender();
    	    }
        } catch (Exception e) {
            logger.error("",e);
        }
        if (s==null)
	    {
	        return "";
	    }else{
	        if (s.equalsIgnoreCase("m"))
	        {
	            return "Male";
	        }else if (s.equalsIgnoreCase("f"))
	        {
	            return "Female";
	        }else if (s.equalsIgnoreCase("o"))
	        {
	            return "Other";
	        }else if (s.equalsIgnoreCase("u"))
	        {
	            return "Unknown";
	        }else{
	            return StringUtils.CapitalFirstLetter(s.toLowerCase());
	        }
	    }
	}

	public Date getDOB()
	{
	    XnatAbstractdemographicdataI ame = this.getDemographics();
	    if (ame instanceof XnatDemographicdata)
	    {
	        return (Date)((XnatDemographicdata)ame).getDob();
	    }
	    return null;
	}

	public String getGender()
	{
	    XnatAbstractdemographicdataI ame = this.getDemographics();
	    if (ame instanceof XnatDemographicdata)
	    {
	        return ((XnatDemographicdata)ame).getGender();
	    }
	    return null;
	}

	public String getHandedness()
	{
	    XnatAbstractdemographicdataI ame = this.getDemographics();
	    if (ame instanceof XnatDemographicdata)
	    {
	        return ((XnatDemographicdata)ame).getHandedness();
	    }
	    return null;
	}

	public Integer getYOB()
	{
	    XnatAbstractdemographicdataI ame = this.getDemographics();
	    if (ame instanceof XnatDemographicdata)
	    {
	        return ((XnatDemographicdata)ame).getYob();
	    }
	    return null;
	}

	public String getHandedText()
	{
	    String s = null;
	    try {
            XnatAbstractdemographicdataI ame = this.getDemographics();
    	    if (ame instanceof XnatDemographicdata)
    	    {
    	        s= ((XnatDemographicdata)ame).getHandedness();
    	    }
        } catch (Exception e) {
            logger.error("",e);
        }
        if (s==null)
	    {
	        return "";
	    }else{
	        if (s.equalsIgnoreCase("l"))
	        {
	            return "Left";
	        }else if (s.equalsIgnoreCase("r"))
	        {
	            return "Right";
	        }else if (s.equalsIgnoreCase("a"))
	        {
	            return "Ambidextrous";
	        }else if (s.equalsIgnoreCase("u"))
	        {
	            return "Unknown";
	        }else{
	            return StringUtils.CapitalFirstLetter(s.toLowerCase());
	        }
	    }
	}

    @SuppressWarnings("deprecation")
	public String getDOBDisplay()
	{
	    try {
	        if (this.getYOB()!=null)
            {
                return (this.getYOB().intValue()) + "";
            }else if(this.getDOB()!=null){
                return (((Date)this.getDOB()).getYear() + 1900) + "";
            }else{
                return "--";
            }
        } catch (Exception e) {
            logger.error("",e);
            return "--";
        }
	}

	public String getLongCreateTime()
	{
	    if (((XFTItem)this.getItem()).getInsertDate() == null)
		{
			return "--";
		}else
		{
		    Date date = ((XFTItem)this.getItem()).getInsertDate();
			return DateFormat.getDateInstance(DateFormat.LONG).format(date);
		}
	}

	public String getAge(Date experimentDate) {
		if (experimentDate == null) {
			return failSafeAge();
		}
		try {
			Date dob = getDOB();
			if (dob != null) {
				return formatAge(calculateAge(experimentDate, dob));
			} else {
				Integer year = getYOB();
				if (year == null) {
					return failSafeAge();
				} else {
					Calendar cal = Calendar.getInstance();
					cal.set(year.intValue(), 1, 1);
					dob = cal.getTime();
					return formatAge(calculateAge(experimentDate, dob));
				}
			}
		} catch (Exception e) {
			logger.error("",e);
			return failSafeAge();
		}
	}

	private Calendar makeCalendar(Date date) {
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		return calendar;
	}

	private int calculateAge(Date current, Date dob) {
		Calendar currentCalendar = makeCalendar(current);
		Calendar dobCalendar = makeCalendar(dob);
		int age = currentCalendar.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR);
		if (currentCalendar.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
			age--;
		}
		return age;
	}

	private String formatAge(double age) {
		NumberFormat formatter = NumberFormat.getInstance();
		formatter.setGroupingUsed(false);
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		return formatter.format(age);
	}

	private String failSafeAge() {
		if (getAge() != null) {
			return getAge().toString();
		} else {
			return "--";
		}
	}
	
	public String getRace(){
	    try {
            XnatAbstractdemographicdataI ame = this.getDemographics();
    	    if (ame instanceof XnatDemographicdata)
    	    {
    	        return ((XnatDemographicdata)ame).getRace();
    	    }
        } catch (Exception e) {
            logger.error("",e);
        }
        return null;
	}

	public String getEthnicity(){
	    try {
            XnatAbstractdemographicdataI ame = this.getDemographics();
    	    if (ame instanceof XnatDemographicdata)
    	    {
    	        return ((XnatDemographicdata)ame).getEthnicity();
    	    }
        } catch (Exception e) {
            logger.error("",e);
        }
        return null;
	}

	public Integer getAge(){
	    try {
            XnatAbstractdemographicdataI ame = this.getDemographics();
    	    if (ame instanceof XnatDemographicdata)
    	    {
    	        return ((XnatDemographicdata)ame).getAge();
    	    }
        } catch (Exception e) {
            logger.error("",e);
        }
        return null;
	}

	public Integer getEducation(){
	    try {
            XnatAbstractdemographicdataI ame = this.getDemographics();
    	    if (ame instanceof XnatDemographicdata)
    	    {
    	        return ((XnatDemographicdata)ame).getEducation();
    	    }
        } catch (Exception e) {
            logger.error("",e);
        }
        return null;
	}

    public String getEducationDesc(){
        try {
            XnatAbstractdemographicdataI ame = this.getDemographics();
            if (ame instanceof XnatDemographicdata)
            {
                return ((XnatDemographicdata)ame).getEducationdesc();
            }
        } catch (Exception e) {
            logger.error("",e);
        }
        return null;
    }

	public Integer getSes(){
	    try {
            XnatAbstractdemographicdataI ame = this.getDemographics();
    	    if (ame instanceof XnatDemographicdata)
    	    {
    	        return ((XnatDemographicdata)ame).getSes();
    	    }
        } catch (Exception e) {
            logger.error("",e);
        }
        return null;
	}

	public String getCohort(){
	    try {
	        XnatAbstractsubjectmetadataI ame = this.getMetadata();
		    if (ame instanceof XnatSubjectmetadata)
		    {
		        return ((XnatSubjectmetadata)ame).getCohort();
		    }
        } catch (Exception e) {
            logger.error("",e);
        }
        return null;
	}


	public ArrayList<ItemI> getMinimalLoadAssessors()
	{
	    if (minLoadAssessors==null)
	    {
	        minLoadAssessors = new ArrayList<ItemI>();

	        try {
                XFTTable table = TableSearch.Execute("SELECT ex.id,ex.date,me.element_name AS type,ex.project,me.element_name,ex.note AS note,projects,label FROM xnat_subjectAssessorData assessor LEFT JOIN xnat_experimentData ex ON assessor.ID=ex.ID LEFT JOIN xdat_meta_element me ON ex.extension=me.xdat_meta_element_id LEFT JOIN (SELECT xs_a_concat(project || ':' || label || ',') AS PROJECTS, sharing_share_xnat_experimentda_id FROM xnat_experimentData_share GROUP BY sharing_share_xnat_experimentda_id) PROJECT_SEARCH ON ex.id=PROJECT_SEARCH.sharing_share_xnat_experimentda_id WHERE assessor.subject_id='" + this.getId() +"'  ORDER BY ex.date ASC",getDBName(),null);
                table.resetRowCursor();
                while (table.hasMoreRows())
                {
                	final Hashtable row = table.nextRowHash();
                    final String element = (String)row.get("element_name");
                    try {
                    	final XFTItem child = XFTItem.NewItem(element,this.getUser());
                        final ItemI om = BaseElement.GetGeneratedItem(child);

                        final Object date = row.get("date");
                        final Object id = row.get("id");
                        final Object note = row.get("note");
                        final Object project = row.get("project");
                        final Object label = row.get("label");

                        if (date!=null)
                        {
                            try {
                                child.setProperty("date",date);
                            } catch (XFTInitException e) {
                                logger.error("",e);
                            } catch (ElementNotFoundException e) {
                                logger.error("",e);
                            } catch (FieldNotFoundException e) {
                                logger.error("",e);
                            } catch (InvalidValueException e) {
                                logger.error("",e);
                            }
                        }
                        if (id!=null)
                        {
                            try {
                                child.setProperty("ID",id);
                            } catch (XFTInitException e) {
                                logger.error("",e);
                            } catch (ElementNotFoundException e) {
                                logger.error("",e);
                            } catch (FieldNotFoundException e) {
                                logger.error("",e);
                            } catch (InvalidValueException e) {
                                logger.error("",e);
                            }
                        }
                        if (label!=null)
                        {
                            try {
                                child.setProperty("label",label);
                            } catch (XFTInitException e) {
                                logger.error("",e);
                            } catch (ElementNotFoundException e) {
                                logger.error("",e);
                            } catch (FieldNotFoundException e) {
                                logger.error("",e);
                            } catch (InvalidValueException e) {
                                logger.error("",e);
                            }
                        }
                        if (note!=null)
                        {
                            try {
                                child.setProperty("note",note);
                            } catch (XFTInitException e) {
                                logger.error("",e);
                            } catch (ElementNotFoundException e) {
                                logger.error("",e);
                            } catch (FieldNotFoundException e) {
                                logger.error("",e);
                            } catch (InvalidValueException e) {
                                logger.error("",e);
                            }
                        }
                        if (project!=null)
                        {
                            try {
                                child.setProperty("project",project);
                            } catch (XFTInitException e) {
                                logger.error("",e);
                            } catch (ElementNotFoundException e) {
                                logger.error("",e);
                            } catch (FieldNotFoundException e) {
                                logger.error("",e);
                            } catch (InvalidValueException e) {
                                logger.error("",e);
                            }
                        }

                        final String projects = (String)row.get("projects");
                        if (projects!=null)
                        {
                            for(final String projectName:StringUtils.CommaDelimitedStringToArrayList(projects, true))
                            {
                               if(projectName.indexOf(":")>-1){
                            	   XnatExperimentdataShare es = new XnatExperimentdataShare(this.getUser());
                            	   es.setProject(projectName.substring(0,projectName.indexOf(":")));
                            	   if(!projectName.endsWith(":")){
                            		   es.setLabel(projectName.substring(projectName.indexOf(":")+1));
                            	   }
                            	   child.setChild("xnat:experimentData/sharing/share",es.getItem(),false);
                               }else{
                                   child.setProperty("sharing.share.project", projectName);
                               }
                            }
                        }


                        if (child.instanceOf("xnat:imageSessionData"))
                        {
                            minLoadAssessors.add(BaseElement.GetGeneratedItem(child));
                            try {
                                XFTTable table2 = TableSearch.Execute("SELECT ex.id,ex.date,me.element_name AS type,ex.project,me.element_name,ex.note AS note,projects,label,assessor.imagesession_id FROM xnat_imageAssessorData assessor LEFT JOIN xnat_experimentData ex ON assessor.ID=ex.ID LEFT JOIN xdat_meta_element me ON ex.extension=me.xdat_meta_element_id LEFT JOIN (SELECT xs_a_concat(project || ':' || label || ',') AS PROJECTS, sharing_share_xnat_experimentda_id FROM xnat_experimentData_share GROUP BY sharing_share_xnat_experimentda_id) PROJECT_SEARCH ON ex.id=PROJECT_SEARCH.sharing_share_xnat_experimentda_id WHERE assessor.imagesession_id='" + id +"'  ORDER BY ex.date ASC",getDBName(),null);
                                table2.resetRowCursor();
                                while (table2.hasMoreRows())
                                {
                                	final Hashtable row2 = table2.nextRowHash();
                                	final String element2 = (String)row2.get("element_name");
                                    try {
                                    	final XFTItem child2 = XFTItem.NewItem(element2,this.getUser());

                                        final Object date2 = row2.get("date");
                                        final  Object id2 = row2.get("id");
                                        final Object project2 = row2.get("project");
                                        final Object note2 = row2.get("note");
                                        final Object label2 = row2.get("label");
                                        final Object imgsession2 = row2.get("imagesession_id");


                                        if (imgsession2!=null)
                                        {
                                            try {
                                                child2.setProperty("imageSession_ID",imgsession2);
                                            } catch (XFTInitException e) {
                                                logger.error("",e);
                                            } catch (ElementNotFoundException e) {
                                                logger.error("",e);
                                            } catch (FieldNotFoundException e) {
                                                logger.error("",e);
                                            } catch (InvalidValueException e) {
                                                logger.error("",e);
                                            }
                                        }
                                        if (date2!=null)
                                        {
                                            try {
                                                child2.setProperty("date",date2);
                                            } catch (XFTInitException e) {
                                                logger.error("",e);
                                            } catch (ElementNotFoundException e) {
                                                logger.error("",e);
                                            } catch (FieldNotFoundException e) {
                                                logger.error("",e);
                                            } catch (InvalidValueException e) {
                                                logger.error("",e);
                                            }
                                        }
                                        if (id2!=null)
                                        {
                                            try {
                                                child2.setProperty("ID",id2);
                                            } catch (XFTInitException e) {
                                                logger.error("",e);
                                            } catch (ElementNotFoundException e) {
                                                logger.error("",e);
                                            } catch (FieldNotFoundException e) {
                                                logger.error("",e);
                                            } catch (InvalidValueException e) {
                                                logger.error("",e);
                                            }
                                        }
                                        if (label2!=null)
                                        {
                                            try {
                                                child2.setProperty("label",label2);
                                            } catch (XFTInitException e) {
                                                logger.error("",e);
                                            } catch (ElementNotFoundException e) {
                                                logger.error("",e);
                                            } catch (FieldNotFoundException e) {
                                                logger.error("",e);
                                            } catch (InvalidValueException e) {
                                                logger.error("",e);
                                            }
                                        }
                                        if (note2!=null)
                                        {
                                            try {
                                                child2.setProperty("note",note2);
                                            } catch (XFTInitException e) {
                                                logger.error("",e);
                                            } catch (ElementNotFoundException e) {
                                                logger.error("",e);
                                            } catch (FieldNotFoundException e) {
                                                logger.error("",e);
                                            } catch (InvalidValueException e) {
                                                logger.error("",e);
                                            }
                                        }
                                        if (project2!=null)
                                        {
                                            try {
                                                child2.setProperty("project",project2);
                                            } catch (XFTInitException e) {
                                                logger.error("",e);
                                            } catch (ElementNotFoundException e) {
                                                logger.error("",e);
                                            } catch (FieldNotFoundException e) {
                                                logger.error("",e);
                                            } catch (InvalidValueException e) {
                                                logger.error("",e);
                                            }
                                        }

                                        final String projects2 = (String)row2.get("projects");
                                        if (projects2!=null)
                                        {
                                        	for(final String projectName:StringUtils.CommaDelimitedStringToArrayList(projects2, true))
                                            {
                                               if(projectName.indexOf(":")>-1){
                                            	   final XnatExperimentdataShare es = new XnatExperimentdataShare(this.getUser());
                                            	   es.setProject(projectName.substring(0,projectName.indexOf(":")));
                                            	   if(!projectName.endsWith(":")){
                                            		   es.setLabel(projectName.substring(projectName.indexOf(":")+1));
                                            	   }
                                            	   child2.setChild("xnat:experimentData/sharing/share",es.getItem(),false);
                                               }else{
                                                   child2.setProperty("sharing.share.project", projectName);
                                               }
                                            }
                                        }

                                        minLoadAssessors.add(BaseElement.GetGeneratedItem(child2));
                                    } catch (XFTInitException e) {
                                        logger.error("",e);
                                    } catch (ElementNotFoundException e) {
                                        logger.error("",e);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("",e);
                            }
                        }else{
                            minLoadAssessors.add(BaseElement.GetGeneratedItem(child));
                        }
                    } catch (XFTInitException e) {
                        logger.error("",e);
                    } catch (ElementNotFoundException e) {
                        logger.error("",e);
                    }
                }
            } catch (Exception e) {
                logger.error("",e);
            }
	    }

	    return minLoadAssessors;
	}

	public int getMinimalLoadAssessorsCount(String elementName)
	{
	    return getMinimalLoadAssessors(elementName).size();
	}

	public Map<String,List<MinLoadExptByP>> getMinimalLoadAssessorsByProject()
	{
		final Map<String,List<MinLoadExptByP>> al = new TreeMap<String,List<MinLoadExptByP>>();
		final Map<String,String> projects=new Hashtable<String,String>();
         for (final ItemI assessor:this.getMinimalLoadAssessors())
         {
        	 try{
        		 final XnatExperimentdata expt=(XnatExperimentdata)assessor;
        		 if(projects.get(expt.getProject())==null){
            		 final String pAlias=expt.getProjectDisplayID();
        			 projects.put(expt.getProject(), pAlias);
        			 al.put(pAlias, new ArrayList<MinLoadExptByP>());
        		 }
        		 
        		 al.get(projects.get(expt.getProject())).add(new MinLoadExptByP(expt.getProject(), expt.getIdentifier(expt.getProject()),expt));
        		 
        		 for(final XnatExperimentdataShare share:expt.getSharing_share()){
            		 if(projects.get(share.getProject())==null){
                		 final String pAlias=share.getProjectDisplayID();
            			 projects.put(share.getProject(), pAlias);
            			 al.put(pAlias, new ArrayList<MinLoadExptByP>());
            		 }

            		 al.get(projects.get(share.getProject())).add(new MinLoadExptByP(share.getProject(), expt.getIdentifier(share.getProject()),expt));
        		 }
        	 }catch (Throwable e){
        		 logger.error("", e);
        	 }
         }

	     return al;
	}
	
	public class MinLoadExptByP{
		String project=null;
		String label=null;
		ItemI item =null;
		
		public MinLoadExptByP(String p, String l, ItemI i){
			project=p;
			label=l;
			item=i;
		}

		public ItemI getItem() {
			return item;
		}

		public void setItem(ItemI item) {
			this.item = item;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getProject() {
			return project;
		}

		public void setProject(String project) {
			this.project = project;
		}
		
		
	}

	public ArrayList getMinimalLoadAssessors(String elementName)
	{
	    ArrayList al = new ArrayList();
	    try {
            SchemaElement e = SchemaElement.GetElement(elementName);
             Iterator min = this.getMinimalLoadAssessors().iterator();
             while (min.hasNext())
             {
                 ItemI assessor = (ItemI)min.next();
                 if (assessor.getXSIType().equalsIgnoreCase(e.getFullXMLName()))
                 {
                      al.add(assessor);
                 }
             }
        } catch (XFTInitException e) {
            logger.error("",e);
        } catch (ElementNotFoundException e) {
            logger.error("",e);
        }

	     al.trimToSize();
	     return al;
	}


	public ArrayList<XnatSubjectassessordata> getExperiments_experiment(String type)
	{
	    ArrayList<XnatSubjectassessordata> al = new ArrayList<XnatSubjectassessordata>();
	    Iterator expts = getExperiments_experiment().iterator();
	    while (expts.hasNext())
	    {
            XnatSubjectassessordata expt = (XnatSubjectassessordata)expts.next();
	        if (expt.getXSIType().equalsIgnoreCase(type))
	        {
	            al.add(expt);
	        }
	    }

	    return al;
	}


    public ArrayList getSessionsByType(String type)
    {
        ArrayList al = new ArrayList();
        Iterator expts = getExperiments_experiment("xnat:mrSessionData").iterator();
        while (expts.hasNext())
        {
            XnatMrsessiondata expt = (XnatMrsessiondata)expts.next();
            if (expt.getSessionType()!=null){
                if (expt.getSessionType().equalsIgnoreCase(type))
                {
                    al.add(expt);
                }
            }else{
                if(type==null)
                    al.add(expt);
            }
        }

        return al;
    }

	public int getExperiments_experiment_Count(String type)
	{
	    return getExperiments_experiment(type).size();
	}

	public boolean isMrAssessor(ItemI i)
	{
	    if (i instanceof BaseXnatMrassessordata)
	    {
	        return true;
	    }else{
	        return false;
	    }
	}

	public boolean isImageAssessor(ItemI i)
	{
	    if (i instanceof BaseXnatImageassessordata)
	    {
	        return true;
	    }else{
	        return false;
	    }
	}

	public XnatMrsessiondata getLastSession()
	{
	    return XNATUtils.getLastSessionForParticipant(this.getId(),(XDATUser)this.getUser());
	}



	public ArrayList getScannerSortedSessions()
	{
	    ArrayList al = this.getExperiments_experiment("xnat:mrSessionData");
	    Collections.sort(al,BaseXnatMrsessiondata.GetScannerDelayComparator());
	    return al;
	}

//    public static String generateGenericID()
//    {
//        String s = org.nrg.xft.XFT.CreateGenericID();
//        ArrayList temp = XnatSubjectdata.getXnatSubjectdatasByField("xnat:subjectData.ID",s,null,false);
//        while (temp.size()>0)
//        {
//            s = org.nrg.xft.XFT.CreateGenericID();
//            temp = XnatSubjectdata.getXnatSubjectdatasByField("xnat:subjectData.ID",s,null,false);
//        }
//
//        return s;
//    }

    public String getAddFieldValueByName(String name)
    {
        return (String)this.getFieldByName(name);
    }

    public static XnatSubjectdata GetSubjectByProjectIdentifier(String project, String identifier,XDATUser user,boolean preLoad){
        CriteriaCollection cc=new CriteriaCollection("OR");
            	
    	CriteriaCollection subcc1 = new CriteriaCollection("AND");
        subcc1.addClause("xnat:subjectData/project", project);
        subcc1.addClause("xnat:subjectData/label", identifier);
        
        cc.add(subcc1);
            	
    	CriteriaCollection subcc2 = new CriteriaCollection("AND");
    	subcc2.addClause("xnat:subjectData/sharing/share/project", project);
    	subcc2.addClause("xnat:subjectData/sharing/share/label", identifier);
        
        cc.add(subcc2);

        ArrayList al =  XnatSubjectdata.getXnatSubjectdatasByField(cc, user, preLoad);
        if (al.size()>0){
           return new XnatSubjectdata((ItemI)al.get(0));
        }else{
            return null;
        }

    }

    public String getIdentifier(String project){
    	 return this.getIdentifier(project, true);
    }

    public String getIdentifiers(){
        final Hashtable<String,String> ids = new Hashtable<String,String>();
        
        if (this.getProject()!=null){
        	if (this.getLabel()!=null){
        		ids.put(this.getLabel(), this.getProject());
        	}else{
        		ids.put(this.getId(), this.getProject());
        	}
        }
        for (final XnatProjectparticipant pp:this.getSharing_share())
        {
            if (pp.getLabel()!=null){
                if (ids.containsKey(pp.getLabel()))
                {
                    ids.put(pp.getLabel(), ids.get(pp.getLabel()) + "," + pp.getProject());
                }else{
                    ids.put(pp.getLabel(), pp.getProject());
                }
            }else{
                if (ids.containsKey(this.getId()))
                {
                    ids.put(this.getId(), ids.get(this.getId()) + "," + pp.getProject());
                }else{
                    ids.put(this.getId(), pp.getProject());
                }
            }
        }

        String identifiers = new String();

        Enumeration enumer = ids.keys();
        int counter=0;
        for (String key: ids.keySet()){
            if (counter++>0)identifiers=identifiers + ", ";
            identifiers=identifiers + key + " ("+ ids.get(key) + ")";
        }

        return identifiers;
    }

    public Hashtable<XnatProjectdataI,String> getProjectDatas(){
        final Hashtable<XnatProjectdataI,String> hash = new Hashtable<XnatProjectdataI,String>();
        for (final XnatProjectparticipant pp:this.getSharing_share())
        {
            if (pp.getLabel()==null)
                hash.put(pp.getProjectData(), this.getId());
            else
                hash.put(pp.getProjectData(), pp.getLabel());
        }

        return hash;
    }

    Hashtable fieldsByName = null;
    public Hashtable getFieldsByName(){
        if (fieldsByName == null){
            fieldsByName=new Hashtable();
            for (final XnatSubjectdataField field : this.getFields_field()){
                fieldsByName.put(field.getName(), field);
            }
        }

        return fieldsByName;
    }

    public Object getFieldByName(String s){
        final XnatSubjectdataFieldI field = (XnatSubjectdataFieldI)getFieldsByName().get(s);
        if (field!=null){
            return field.getField();
        }else{
            return null;
        }
    }


    public XnatProjectdataI getProject(String projectID, boolean preLoad)
    {
        XnatProjectparticipant ep = null;
        for (final XnatProjectparticipant tempep: this.getSharing_share())
        {
            if (tempep.getProject().equals(projectID))
            {
                ep=tempep;
                break;
            }
        }

        try {
            if (ep!=null){
                return XnatProjectdata.getXnatProjectdatasById(ep.getProject(), this.getUser(), preLoad);
            }else if (this.getProject().equals(projectID)){
                return XnatProjectdata.getXnatProjectdatasById(this.getProject(), this.getUser(), preLoad);
            }
        } catch (RuntimeException e) {
            logger.error("",e);
        }

        return null;
    }



    public XnatProjectdata getPrimaryProject(boolean preLoad){
        if (this.getProject()!=null){
            return (XnatProjectdata)XnatProjectdata.getXnatProjectdatasById(getProject(), this.getUser(), preLoad);
        }else{
            return (XnatProjectdata)getFirstProject();
        }
    }


    public XnatProjectdataI getFirstProject()
    {
        XnatProjectparticipant ep = null;
        if (!this.getSharing_share().isEmpty()){
        	ep = this.getSharing_share().get(0);
        }

        try {
            if (ep!=null){
                return XnatProjectdata.getXnatProjectdatasById(ep.getProject(), this.getUser(), false);
            }
        } catch (RuntimeException e) {
            logger.error("",e);
        }

        return null;
    }

    public String getIdentifier(String project,boolean returnNULL){
        if (project!=null){
        	if (this.getProject().equals(project)){
        		if (this.getLabel()!=null){
        			return this.getLabel();
        		}
        	}
        	
            for (final XnatProjectparticipant pp: this.getSharing_share())
            {
                if (pp.getProject().equals(project))
                {
                    if (pp.getLabel()!=null){
                        return pp.getLabel();
                    }
                }
            }
        }

        if (returnNULL){
            return null;
        }else{
            return getId();
        }
    }

    public int getSubjectAssessorCount(){
        try {
           final  XFTTable table = XFTTable.Execute("SELECT COUNT(*) FROM xnat_subjectassessordata WHERE subject_id='" + getId() + "';", getDBName(), null);

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


//    public String createNewAssessorId(String xsiType) throws SQLException{
//        String newID= "";
//        String prefix= "";
//        long i = this.getExperiments_experiment_Count(xsiType)+1;
//        prefix+=this.getId();
//
//        NumberFormat nf = NumberFormat.getInstance();
//        nf.setMinimumIntegerDigits(3);
//        
//        String code =ElementSecurity.GetCode(xsiType);
//        if(code!=null && !code.equals(""))
//            prefix+="_" + code;
//        
//        newID+=prefix +"_"+ StringUtils.ReplaceStr(nf.format(i), ",", "");
//        
//        String query = "SELECT count(ID) AS id_count FROM xnat_experimentdata WHERE ID='";
//
//        String login = null;
//        if (this.getUser()!=null){
//            login=this.getUser().getUsername();
//        }
//        try {
//            Long idCOUNT= (Long)PoolDBUtils.ReturnStatisticQuery(query + newID + "';", "id_count", this.getDBName(), login);
//            while (idCOUNT > 0){
//                i++;
//                newID=prefix + "_"+ StringUtils.ReplaceStr(nf.format(i), ",", "");
//                idCOUNT= (Long)PoolDBUtils.ReturnStatisticQuery(query + newID + "';", "id_count", this.getDBName(), login);
//            }
//        } catch (Exception e) {
//            logger.error("",e);
//        }
//
//        return newID;
//    }

    private String name = null;
    private String description = null;
    private String secondaryID = null;
    private boolean initd = false;

    public void loadProjectDetails(){
        if (!initd)
        {
            initd=true;
            try {
                XFTTable table = XFTTable.Execute("SELECT name,description,secondary_ID FROM xnat_projectData WHERE ID ='" + this.getProject() + "';", this.getDBName(), null);

                if (table.size()>0)
                {
                    Object[] row = (Object[])table.rows().get(0);
                    name = (String)row[0];
                    description = (String)row[1];
                    secondaryID = (String)row[2];
                }
            } catch (SQLException e) {
                logger.error("",e);
            } catch (DBPoolException e) {
                logger.error("",e);
            }
        }
    }

    public XnatProjectdata getProjectData(){
        return XnatProjectdata.getXnatProjectdatasById(this.getProject(), this.getUser(), false);
    }


    /**
     * @return the description
     */
    public String getDescription() {
        loadProjectDetails();
        return description;
    }

    /**
     * @return the name
     */
    public String getProjectName() {
        loadProjectDetails();
        return name;
    }

    /**
     * @return the secondaryID
     */
    public String getProjectSecondaryID() {
        loadProjectDetails();
        return secondaryID;
    }



    /**
     * @return the secondaryID
     */
    public String getProjectDisplayID() {
        loadProjectDetails();
        if (secondaryID!=null){
            return secondaryID;
        }else{
           return getProject();
        }
    }


    public Collection<XnatFielddefinitiongroup> getFieldDefinitionGroups(String dataType){
        Hashtable<String,XnatFielddefinitiongroup> groups = new Hashtable<String,XnatFielddefinitiongroup>();
        Hashtable<XnatProjectdataI,String> projects = getProjectDatas();

        if (projects.size()==0){
            if (this.getProject()!=null){
                projects.put(this.getPrimaryProject(false), "");
            }
        }

        for(Map.Entry<XnatProjectdataI,String> entry : projects.entrySet()){
            XnatAbstractprotocol prot = ((XnatProjectdata)entry.getKey()).getProtocolByDataType(dataType);
            if (prot!=null && prot instanceof XnatDatatypeprotocol){
                XnatDatatypeprotocol dataProt=(XnatDatatypeprotocol)prot;
                for(XnatFielddefinitiongroup group : dataProt.getDefinitions_definition()){
                    groups.put(group.getId(), group);
                }
            }
        }



        return groups.values();
    }
    
    public static String CreateNewID() throws Exception{
    	IDGeneratorI generator = IDGeneratorFactory.GetIDGenerator("org.nrg.xnat.turbine.utils.IDGenerator");
    	generator.setTable("xnat_subjectData");
    	generator.setDigits(5);
    	generator.setColumn("id");
    	return generator.generateIdentifier();
    }
    
    public void moveToProject(XnatProjectdata newProject,String newLabel,XDATUser user) throws Exception{
    	if(!this.getProject().equals(newProject.getId()))
    	{
    		if(!user.canEdit(this)){
    			throw new InvalidPermissionException(this.getXSIType());
    		}
    		
    		String existingRootPath=this.getProjectData().getRootArchivePath();
    		
    		if(newLabel==null)newLabel = this.getLabel();
    		if(newLabel==null)newLabel = this.getId();
    		
    		File newSessionDir = new File(new File(newProject.getRootArchivePath(),newProject.getCurrentArc()),newLabel);
    		
    		String current_label=this.getLabel();
    		if(current_label==null)current_label=this.getId();
    		
    		for(XnatAbstractresource abstRes:this.getResources_resource()){
    			String uri= null;
    			if(abstRes instanceof XnatResource){
    				uri=((XnatResource)abstRes).getUri();
    			}else{
    				uri=((XnatResourceseries)abstRes).getPath();
    			}
    			
    			if(FileUtils.IsAbsolutePath(uri)){
    				int lastIndex=uri.lastIndexOf(File.separator + current_label + File.separator);
    				if(lastIndex>-1)
    				{
    					lastIndex+=1+current_label.length();
    				}
    				if(lastIndex==-1){
    					lastIndex=uri.lastIndexOf(File.separator + this.getId() + File.separator);
        				if(lastIndex>-1)
        				{
        					lastIndex+=1+this.getId().length();
        				}
    				}
    				String existingSessionDir=null;
    				if(lastIndex>-1){
        				//in session_dir
        				existingSessionDir=uri.substring(0,lastIndex);
        			}else{
        				//outside session_dir
        				newSessionDir = new File(newSessionDir,"RESOURCES");
        				newSessionDir = new File(newSessionDir,"RESOURCES/"+abstRes.getXnatAbstractresourceId());
        				int lastSlash=uri.lastIndexOf("/");
        				if(uri.lastIndexOf("\\")>lastSlash){
        					lastSlash=uri.lastIndexOf("\\");
        				}
        				existingSessionDir=uri.substring(0,lastSlash);
        			}
        			abstRes.moveTo(newSessionDir,existingSessionDir,existingRootPath,user);
    			}else{
        			abstRes.moveTo(newSessionDir,null,existingRootPath,user);
    			}
    		}
    		
    		XFTItem current=this.getCurrentDBVersion(false);
    		current.setProperty("project", newProject.getId());
    		current.setProperty("label", newLabel);    		
    		current.save(user, true, false); 
    		
    		this.setProject(newProject.getId());
    		this.setLabel(newLabel);
    	}
    }
    
    public boolean hasProject(String proj_id){
		if(this.getProject().equals(proj_id)){
		    return true;
		}else{
		    for(XnatProjectparticipant pp: this.getSharing_share()){
			if(pp.getProject().equals(proj_id)){
			    return true;
			}
		    }
		}
		
		return false;
    }
    
    public String canDelete(BaseXnatProjectdata proj, XDATUser user) {
    	BaseXnatSubjectdata subj=this;
    	if(this.getItem().getUser()!=null){
    		subj=new XnatSubjectdata(this.getCurrentDBVersion(true));
    	}
    	if(!subj.hasProject(proj.getId())){
    		return "Subject is not assigned to specified project " + proj.getId();
    	}else {

			try {
				SecurityValues values = new SecurityValues();
				values.put(this.getXSIType() + "/project", proj.getId());
				
				if (!user.canDeleteByXMLPath(this.getSchemaElement(),values))
				{
					return "User cannot delete subjects for project " + proj.getId();
				}
			} catch (Exception e1) {
				return "Unable to delete subject.";
			}
			
    		for(XnatSubjectassessordata sad: subj.getExperiments_experiment()){
    			String msg=sad.canDelete(proj,user);
    			if(msg!=null){
    				return msg;
    			}
    		}
			
			return null;
    	}
    }
    
    public String delete(BaseXnatProjectdata proj, XDATUser user, boolean removeFiles){
    	BaseXnatSubjectdata sub=this;
    	if(this.getItem().getUser()!=null){
    		sub=new XnatSubjectdata(this.getCurrentDBVersion(true));
    	}
    	
    	String msg=sub.canDelete(proj,user);
    	if(msg!=null){
    		logger.error(msg);
    		return msg;
    	}
    	
    	if(!sub.getProject().equals(proj.getId())){
			try {
				SecurityValues values = new SecurityValues();
				values.put(this.getXSIType() + "/project", proj.getId());
				
				if (!user.canDelete(sub) && !user.canDeleteByXMLPath(this.getSchemaElement(),values))
				{
					return null;
				}
				
				int index = 0;
				int match = -1;
				for(XnatProjectparticipant pp : sub.getSharing_share()){
					if(pp.getProject().equals(proj.getId())){
						DBAction.RemoveItemReference(sub.getItem(), "xnat:subjectData/sharing/share", pp.getItem(), user);
						match=index;
						break;
					}
					index++;
				}
				
				if(match==-1)return null;
				
				this.removeSharing_share(match);
				
				final  ArrayList<XnatSubjectassessordata> expts = sub.getExperiments_experiment();
		        for (XnatSubjectassessordata exptI : expts){
		        	final XnatSubjectassessordata expt = (XnatSubjectassessordata)exptI;
		            expt.delete(proj,user,false);
		        }
				
				return null;
			} catch (SQLException e) {
				logger.error("",e);
				return e.getMessage();
			} catch (Exception e) {
				logger.error("",e);
				return e.getMessage();
			}
		}else{
			try {
			
				if(!user.canDelete(this)){
					return "User account doesn't have permission to delete this subject.";
				}
							
				if(removeFiles){
					this.deleteFiles();
				}

				final  ArrayList<XnatSubjectassessordata> expts = sub.getExperiments_experiment();
		        for (XnatSubjectassessordata exptI : expts){
		        	final XnatSubjectassessordata expt = (XnatSubjectassessordata)exptI;
		            msg=expt.delete(proj,user,removeFiles);
		            if(msg!=null)return msg;
		        }
		        
		        DBAction.DeleteItem(sub.getItem().getCurrentDBVersion(), user);
				
			    user.clearLocalCache();
				MaterializedView.DeleteByUser(user);
			} catch (SQLException e) {
				logger.error("",e);
				return e.getMessage();
			} catch (Exception e) {
				logger.error("",e);
				return e.getMessage();
			}
		}
    	return null;
    }
    
    public void deleteFiles() throws IOException{
    	XnatProjectdata proj = this.getPrimaryProject(false);
    	String archive=proj.getRootArchivePath();
    	File dir=new File(archive,"subjects/"+ this.getArchiveDirectoryName());
    	if(dir.exists()){
    		FileUtils.MoveToCache(dir);
    	}
    	
    	for(XnatAbstractresource abstRes:this.getResources_resource()){
    		abstRes.deleteFromFileSystem(archive);
    	}
    }
    
    public int getAssessmentCount(String project){
    	int count=0;
    	for(int i=0;i<this.getMinimalLoadAssessors().size();i++){
    		XnatExperimentdata expt=(XnatExperimentdata)this.getMinimalLoadAssessors().get(i);
    		if(expt.getProject().equals(project)){
    			count++;
    		}
    	}
    	return count;
    }

    public static String cleanValue(String v){
    	v= StringUtils.ReplaceStr(v, " ", "_");
    	v= StringUtils.ReplaceStr(v, "`", "_");
    	v= StringUtils.ReplaceStr(v, "~", "_");
    	v= StringUtils.ReplaceStr(v, "@", "_");
    	v= StringUtils.ReplaceStr(v, "#", "_");
    	v= StringUtils.ReplaceStr(v, "$", "_");
    	v= StringUtils.ReplaceStr(v, "%", "_");
    	v= StringUtils.ReplaceStr(v, "^", "_");
    	v= StringUtils.ReplaceStr(v, "&", "_");
    	v= StringUtils.ReplaceStr(v, "*", "_");
    	v= StringUtils.ReplaceStr(v, "(", "_");
    	v= StringUtils.ReplaceStr(v, ")", "_");
    	v= StringUtils.ReplaceStr(v, "+", "_");
    	v= StringUtils.ReplaceStr(v, "=", "_");
    	v= StringUtils.ReplaceStr(v, "[", "_");
    	v= StringUtils.ReplaceStr(v, "]", "_");
    	v= StringUtils.ReplaceStr(v, "{", "_");
    	v= StringUtils.ReplaceStr(v, "}", "_");
    	v= StringUtils.ReplaceStr(v, "|", "_");
    	v= StringUtils.ReplaceStr(v, "\\", "_");
    	v= StringUtils.ReplaceStr(v, "/", "_");
    	v= StringUtils.ReplaceStr(v, "?", "_");
    	v= StringUtils.ReplaceStr(v, ":", "_");
    	v= StringUtils.ReplaceStr(v, ";", "_");
    	v= StringUtils.ReplaceStr(v, "\"", "_");
    	v= StringUtils.ReplaceStr(v, "'", "_");
    	v= StringUtils.ReplaceStr(v, ",", "_");
    	v= StringUtils.ReplaceStr(v, ".", "_");
    	v= StringUtils.ReplaceStr(v, "<", "_");
    	v= StringUtils.ReplaceStr(v, ">", "_");
    	
    	return v;
    }
    


	@Override
	public void preSave() throws Exception{
		super.preSave();
		
		final XnatProjectdata proj = this.getPrimaryProject(false);
		if(proj==null){
			throw new Exception("Unable to identify project for:" + this.getProject());
		}
		
		final String expectedPath=getExpectedCurrentDirectory().getAbsolutePath().replace('\\', '/');
		
		for(final XnatAbstractresource res: this.getResources_resource()){
			final String uri;
			if(res instanceof XnatResource){
				uri=((XnatResource)res).getUri();
			}else if(res instanceof XnatResourceseries){
				uri=((XnatResourceseries)res).getPath();
			}else{
				continue;
			}
			
			FileUtils.ValidateUriAgainstRoot(uri,expectedPath,"URI references data outside of the project: " + uri);
		}
		
		for(final XnatSubjectassessordata expt:this.getExperiments_experiment()){
			expt.preSave();
		}
	}

	@Override
	public File getExpectedCurrentDirectory() throws InvalidArchiveStructure {
		return new File(this.getPrimaryProject(false).getRootArchivePath(),"subjects/"+ this.getArchiveDirectoryName());
	}
}
