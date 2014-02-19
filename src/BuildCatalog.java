/*
 * BuildCatalog
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xnat.turbine.utils.XNATUtils;


public class BuildCatalog extends CommandPromptTool {

	public BuildCatalog(String[] args)
	{
	    super(args);
	}
    
    public static void main(String[] args) {
    	BuildCatalog b = new BuildCatalog(args);	
		return;
	}	
    
	@Override
	public void definePossibleVariables() {
        addPossibleVariable("folder","folder to look for files",new String[]{"folder"},true);
	}

	@Override
	public String getAdditionalUsageInfo() {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}
	
	

	@Override
	public boolean requireLogin() {
		return false;
	}

	@Override
	public void process() {
		String folder = (String)this.arguments.get("folder");
		
		File dir = new File(folder);
		
		CatCatalogBean cat = new CatCatalogBean();
    	if (dir.exists())
        {
            XNATUtils.populateCatalogBean(cat, "", dir);
        }
    	File dest = new File(folder +"_catalog.xml");
    	try {
			FileWriter fw = new FileWriter(dest);
			cat.toXML(fw, true);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
