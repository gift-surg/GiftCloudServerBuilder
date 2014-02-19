import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XnatDatatypeprotocol;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

/*
 * ManageProjectAccessories
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */



public class ManageProjectAccessories  extends CommandPromptTool{
//  java -Xmx256M -classpath .;XFT.jar;commons-collections-2.0.jar;commons-configuration-1.0-dev.jar;commons-dbcp-1.0-dev-20020806.jar;commons-lang-1.0.jar;commons-pool-1.0.jar;jakarta-regexp-1.2.jar;log4j-1.2.6.jar;parser.jar;pg74.214.jdbc2.jar;stratum-1.0-b3.jar;xerces-J_1.4.0.jar XFTApp
    public ManageProjectAccessories(String[] args)
    {
        super(args);
    }

    public static void main(String[] args) {
        ManageProjectAccessories b = new ManageProjectAccessories(args);
        return;
    }

    public boolean requireLogin()
    {
        return true;
    }


    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#definePossibleVariables()
     */
    public void definePossibleVariables() {
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
        return "Function used to insert meta data rows for any table entries which may be missing them.\n";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "ManageProjectAccessories";
    }

    public void process()
    {
        try {
            //System.out.print(elementName + ":" + selectType + ":" + output);
            Hashtable<String,ElementSecurity> usable = new Hashtable<String,ElementSecurity>();
            Collection<ElementSecurity> all =ElementSecurity.GetElementSecurities().values();
            for (ElementSecurity es: all){
                try {
                    if (es.getAccessible()){
                        GenericWrapperElement g= es.getSchemaElement().getGenericXFTElement();

                        if (g.instanceOf("xnat:subjectData") || g.instanceOf("xnat:experimentData")){
                            usable.put(es.getElementName(),es);
                        }else{
                            System.out.println(es.getElementName()+" not an experiment");
                        }
                    }else{
                        System.out.println(es.getElementName()+" not accessible");
                    }
                } catch (Throwable e) {
                }
            }

            ArrayList<XnatProjectdata> al = XnatProjectdata.getAllXnatProjectdatas(null, false);
            System.out.println("" + al.size() + " projects");
            
            EventMetaI c=EventUtils.DEFAULT_EVENT(getUser(),"src/Manage Project Accessories");

            for (XnatProjectdata p:al){
                System.out.println("Reviewing " + p.getId());
                if (p.getStudyprotocol().size()==0){
                    XnatDatatypeprotocol protocol = new XnatDatatypeprotocol((UserI)getUser());
                    String elementName = "xnat:subjectData";
                    protocol.setDataType(elementName);
                    ElementSecurity es = usable.get(elementName);
                    if (es==null){
                        throw new Exception("Unknown type:" + elementName + "\n" + usable.toString());

                    }
                    protocol.setName(es.getPluralDescription());
                    protocol.setProperty("ID", p.getProperty("ID") + "_" + usable.get(elementName).getSchemaElement().getSQLName());
                    protocol.setProperty("xnat:datatypeProtocol/xnat_projectdata_id",p.getId());
                    protocol.setProperty("xnat:datatypeProtocol/definitions/definition[ID=default]/data-type", protocol.getProperty("data-type"));
                    protocol.setProperty("xnat:datatypeProtocol/definitions/definition[ID=default]/project-specific", "false");
                    SaveItemHelper.authorizedSave(protocol,getUser(), false, false,c);

                    p.setStudyprotocol(protocol);

                    Hashtable<String,Long> counts = p.getExperimentCountByXSIType();
                    for(Map.Entry<String, Long> entry: counts.entrySet()){
                        if (entry.getValue()>0){
                            elementName=entry.getKey();

                            protocol = new XnatDatatypeprotocol((UserI)getUser());
                            protocol.setDataType(elementName);
                            es = usable.get(elementName);
                            if (es==null){
                                throw new Exception("Unknown type:" + elementName + "\n" + usable.toString());
                            }
                            protocol.setName(es.getPluralDescription());
                            protocol.setProperty("ID", p.getProperty("ID") + "_" + usable.get(elementName).getSchemaElement().getSQLName());

                            protocol.setProperty("xnat:datatypeProtocol/xnat_projectdata_id",p.getId());
                            protocol.setProperty("xnat:datatypeProtocol/definitions/definition[ID=default]/data-type", protocol.getProperty("data-type"));
                            protocol.setProperty("xnat:datatypeProtocol/definitions/definition[ID=default]/project-specific", "false");
                            p.setStudyprotocol(protocol);

                            SaveItemHelper.authorizedSave(protocol,getUser(), false, false,c);

                        }
                    }

                }

                p.getItem().setUser(this.getUser());

                System.out.println(p.getId()+" initGroups();");
                p.initGroups();

                System.out.println(p.getId()+" initBundles();");
                //p.initBundles((XDATUser)getUser());
                String projectAccess = p.getPublicAccessibility();
                if(projectAccess==null || projectAccess.equals("")){
                	projectAccess="private";
                }

                p.initAccessibility(projectAccess, true,getUser(),c);

                ArcProject arcP = new ArcProject((UserI)getUser());
                arcP.setId(p.getId());
                arcP.setProperty("projects_project_arc_archivespe_arc_archivespecification_id", ArcSpecManager.GetInstance().getArcArchivespecificationId());
                arcP.setCurrentArc("arc001");

                if (arcP.getItem().getUniqueMatches(false).size()==0){
                    if (arcP.getPaths()==null || arcP.getPaths().getArchivepath() ==null || arcP.getPaths().getArchivepath().equals(""))
                    {
                        arcP.setProperty("arc:project/paths/archivePath", ArcSpecManager.GetInstance().getGlobalArchivePath() + p.getId()+"/");
                    }else if(arcP.getPaths().getArchivepath().equals(ArcSpecManager.GetInstance().getGlobalArchivePath())){
                        arcP.setProperty("arc:project/paths/archivePath", ArcSpecManager.GetInstance().getGlobalArchivePath() + p.getId()+"/");
                    }

                    if (arcP.getPaths()==null || arcP.getPaths().getPrearchivepath() ==null || arcP.getPaths().getPrearchivepath().equals(""))
                    {
                        arcP.setProperty("arc:project/paths/prearchivePath", ArcSpecManager.GetInstance().getGlobalPrearchivePath() + p.getId()+"/");
                    }else if(arcP.getPaths().getPrearchivepath().equals(ArcSpecManager.GetInstance().getGlobalPrearchivePath())){
                        arcP.setProperty("arc:project/paths/prearchivePath", ArcSpecManager.GetInstance().getGlobalPrearchivePath() + p.getId()+"/");
                    }



                    if (arcP.getPaths()==null || arcP.getPaths().getCachepath() ==null || arcP.getPaths().getCachepath().equals(""))
                    {
                        arcP.setProperty("arc:project/paths/cachePath", ArcSpecManager.GetInstance().getGlobalCachePath() + p.getId()+"/");
                    }else if(arcP.getPaths().getCachepath().equals(ArcSpecManager.GetInstance().getGlobalCachePath())){
                        arcP.setProperty("arc:project/paths/cachePath", ArcSpecManager.GetInstance().getGlobalCachePath() + p.getId()+"/");
                    }

                    if (arcP.getPaths()==null || arcP.getPaths().getBuildpath() ==null || arcP.getPaths().getBuildpath().equals(""))
                    {
                        arcP.setProperty("arc:project/paths/buildPath", ArcSpecManager.GetInstance().getGlobalBuildPath() + p.getId()+"/");
                    }

                    SaveItemHelper.authorizedSave( arcP,getUser(), true, false,c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            try {
                XFT.closeConnections();
            } catch (SQLException e1) {
            }
        }

        return;
    }
}

