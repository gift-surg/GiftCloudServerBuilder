/*
 * ValidateJavaBean
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.nrg.xdat.bean.base.BaseElement;
import org.nrg.xdat.bean.base.BaseElement.UnknownFieldException;
import org.nrg.xdat.bean.reader.XDATXMLReader;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;



public class ValidateJavaBean {

    public static void main(String[] args) {
        List argsAL = Arrays.asList(args);

        int index = argsAL.indexOf("-f");
        if (index==-1){
            System.out.println("Please specify the file to be validated using the '-f' tag.");
            System.exit(1);
        }

        String fpath = (String)argsAL.get(index + 1);


        File data = new File(fpath);

        if (!data.exists())
        {
            System.out.println("Unable to locate file: " + fpath);
            System.exit(1);
        }

        try {
			FileInputStream fis = new FileInputStream(data);
			XDATXMLReader reader = new XDATXMLReader();
			BaseElement base1 = reader.parse(fis);
			
			outputProperties(base1.getSchemaElementName() +"/",base1);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

    }
    
    public static void outputProperties(String header, BaseElement base1){
    	for(int i=0;i<base1.getAllFields().size();i++){
			try {
				String path=(String)base1.getAllFields().get(i);
				String ft=base1.getFieldType(path);
				if(ft.equals(BaseElement.field_data) || ft.equals(BaseElement.field_LONG_DATA)){
					Object v1=base1.getDataFieldValue(path);
					System.out.println(header + path + "=" + v1);
				}else{
					//reference
					Object o1=base1.getReferenceField(path);
					
					if(o1 instanceof ArrayList){
						ArrayList<BaseElement> children1=(ArrayList<BaseElement>)o1;
							for(int j=0;j<children1.size();j++){
								outputProperties(header + path + "/",children1.get(j));
							}
					}else if(o1!=null){
						BaseElement child1=(BaseElement)o1;
						outputProperties(header + path + "/",child1);
					}
				}
			} catch (UnknownFieldException e) {
				e.printStackTrace();
			}
			
    	}
    }

}
