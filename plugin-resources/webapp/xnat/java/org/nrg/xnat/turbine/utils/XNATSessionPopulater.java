//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Sep 15, 2006
 *
 */
package org.nrg.xnat.turbine.utils;


/**
 * @author timo
 * 
 */
public class XNATSessionPopulater {
//    private final org.apache.log4j.Logger logger = Logger.getLogger(XNATSessionPopulater.class);
//    private boolean buildXML = true;
//    private final File directory;
//    private final String project;
//
//    public final List<Exception> exceptions = new java.util.ArrayList<Exception>();
//
//    public XNATSessionPopulater(final File dir, final String project) {
//	this.directory = dir;
//	this.project = project;
//    }
//
//    public XnatMrsessiondata populateMR(UserI user) throws IOException, SAXException {
//	logger.debug("Scanning DICOM fileset " + directory.getAbsolutePath());
//	
//	/*
//	 * Define attributes for the mrSession level
//	 */
//	final File xmlFile = new File(directory.getAbsolutePath() + ".xml");
//	if (buildXML){
//	    logger.debug("Building mrSessionData for " + directory.getAbsolutePath());
//	    final MRSessionBuilder mrb = new MRSessionBuilder(new File(directory.getAbsolutePath()), xmlFile,
//		    new ProjectAttrDef(project));
//	    mrb.run();
//	}
//
//	if (xmlFile.exists()){
//	    final SAXReader reader = new SAXReader(user);
//	    final XFTItem item = reader.parse(xmlFile.getAbsolutePath());
//	    item.setUser(user);
//	    if (item.getXSIType().equalsIgnoreCase("xnat:mrSessionData")){
//		return new XnatMrsessiondata(item);
//	    }
//	}
//
//	return null;
//    }
//
//    public XnatPetsessiondata populatePET(UserI user) throws IOException, SAXException{
//	logger.debug("Scanning PET fileset " + directory.getAbsolutePath());
//
//	/*
//	 * Define attributes for the petSession level
//	 */
//	final File xmlFile = new File(directory.getAbsolutePath() + ".xml");
//	if (buildXML){
//	    logger.debug("Building petSessionData for " + directory.getAbsolutePath());
//	    final PETSessionBuilder mrb = new PETSessionBuilder(new File(directory.getAbsolutePath()), xmlFile, project);
//	    mrb.run();
//	}
//
//	if (xmlFile.exists()){
//	    final SAXReader reader = new SAXReader(user);
//	    final XFTItem item = reader.parse(xmlFile.getAbsolutePath());
//	    item.setUser(user);
//	    if (item.getXSIType().equalsIgnoreCase("xnat:petSessionData")){
//		return new XnatPetsessiondata(item);
//	    }
//	}
//
//	return null;
//    }
//
//    /**
//     * @return the buildXML
//     */
//    public boolean buildXML() {
//	return buildXML;
//    }
//
//    /**
//     * @param buildXML the buildXML to set
//     */
//    public void setBuildXML(boolean buildXML) {
//	this.buildXML = buildXML;
//    }

}
