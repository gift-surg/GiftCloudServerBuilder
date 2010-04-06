//Copyright Washington University School of Medicine All Rights Reserved
/*
 * Created on Feb 9, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.xml.sax.SAXException;

public class VerifyMRUpload extends SecureReport {

    static org.apache.log4j.Logger logger = Logger.getLogger(VerifyMRUpload.class);
    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void finalProcessing(RunData data, Context context) {
        String uploadID= data.getParameters().getString("uploadID");
        context.put("uploadID",uploadID);
        
        String cache_path = ArcSpecManager.GetInstance().getGlobalCachePath();
        
        cache_path +="uploads" + File.separator + uploadID + File.separator;
        File dir = new File(cache_path);
        if (dir.exists())
        {
            File[] listFiles = dir.listFiles();
            for (int i=0;i<listFiles.length;i++)
            {
                if(!listFiles[i].isDirectory())
                {
                    if (listFiles[i].getName().endsWith(".xml"))
                    {
                        File xml = listFiles[i];
                        if (xml.exists())
                        {
                            try {
                                SAXReader reader = new SAXReader(TurbineUtils.getUser(data));
                                XFTItem temp = reader.parse(xml.getAbsolutePath());
                                XnatMrsessiondata pet = new XnatMrsessiondata(temp);
                                pet.fixScanTypes();
                                context.put("xml", temp);
                                context.put("xmlOM", pet);
                            } catch (IOException e) {
                                logger.error("",e);
                            } catch (SAXException e) {
                                logger.error("",e);
                            }
                        }
                    }
                }
            }
        }
    }

}
