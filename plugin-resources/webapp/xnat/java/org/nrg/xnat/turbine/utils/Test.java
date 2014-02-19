/*
 * org.nrg.xnat.turbine.utils.Test
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.utils;

import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;

/**
 * @author Tim
 *
 */
public class Test {

    public static void main(String[] args) {
        try {
			//String appDir = "C:\\xdat\\projects\\sample";
			String appDir = "C:\\xdat\\projects\\oasis\\";
			
			//XDATTool tool = new XDATTool(appDir,"admin","");
			XDAT.init(appDir,false,true);
			
			
			XDATUser user = new XDATUser("admin","admin");
			
//			XFTItem item = ItemSearch.GetItem("cnda:atlasScalingFactorData/ID","990413_97427_ASF_9501161738",user,false);
//			CndaAtlasscalingfactordata asf = new CndaAtlasscalingfactordata(item);
//			XnatMrsessiondata mr = asf.getMrSessionData();
			
			

			//Hashtable hash =XNATUtils.getInvestigatorsForCreate("xnat:subjectData",user);
			
			//org.nrg.xft.XFTTool.StoreXMLFileToDB(appDir + "\\work\\000510_vc4309.xml",user,Boolean.FALSE);
			
//			XFTItem item = XFTItem.NewItem("xnat:subjectData",user);
//			XFTItem demo = XFTItem.NewItem("xnat:demographicData",user);
//			XFTItem me = XFTItem.NewItem("xnat:subjectMetadata",user);
//			item.setProperty("demographics",demo);
//			item.setProperty("metadata",me);
//			
//			item.setProperty("ID","XNAT001");
//			item.setProperty("investigator.xnat_investigatorData_id",new Integer(1));
//			item.setProperty("demographics.gender","female");
			
//			item.setProperty("xnat:subjectdata.demographics.gender","female");
//			item.setProperty("xnat:subjectdata/addid[0]","123456");
//			item.setProperty("xnat:subjectdata/addid[0]/name","map");
//			item.setProperty("xnat:subjectdata/demographics/dob.date","2");
//			item.setProperty("xnat:subjectdata/demographics/dob.month","2");
//			item.setProperty("xnat:subjectdata/demographics/dob.year","1903");
//			item.setProperty("xnat:subjectdata/demographics/education","12");
//			item.setProperty("xnat:subjectdata/demographics/handedness","left");
//			item.setProperty("xnat:subjectdata/demographics/ses","1");
//			item.setProperty("xnat:subjectdata/id","XNAT001");
//			item.setProperty("xnat:subjectdata/investigator_xnat_investigatordata_id","1");
//			item.setProperty("xnat:subjectdata/metadata/cohort","cohort");
			
//			item.save(user,true);
			
//			DisplaySearch ds = user.getSearch("xnat:subjectData","listing");
////			
//			XFTTableI t = ds.execute(new HTMLPresenter(""));
			
			
//			Document doc = XFTTool.FindXML("xnat:Project.ID","CNDA0",null);
//			XMLUtils.DOMToFile(doc,appDir + "\\work\\CNDA.xml");
			System.out.println("");
		}catch(ElementNotFoundException e)
		{
			e.printStackTrace();
		}catch(XFTInitException e)
		{
			e.printStackTrace();
		}catch(Exception e)
		{
			e.printStackTrace();
		}
			
    }
}
