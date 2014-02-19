

/*
 * ValidateXML
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class ValidateXML {

    public static void main(String[] args) {
        List argsAL = Arrays.asList(args);

        int index = argsAL.indexOf("-f");
        if (index==-1){
            System.out.println("Please specify the file to be validated using the '-f' tag.");
            System.exit(1);
        }

        String path = (String)argsAL.get(index + 1);


        File data = new File(path);

        if (!data.exists())
        {
            System.out.println("Unable to locate file: " + path);
            System.exit(1);
        }


        System.out.print("\nValidating...");
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setValidating(true);
            spf.setFeature("http://apache.org/xml/features/validation/schema", true);

            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();

            ValidationHandler validator= new ValidationHandler();
            //parse the file and also register this class for call backs
            sp.parse(data,validator);

            if (validator.assertValid()){
                System.out.println("done.");
                System.exit(0);
            }else{
                System.out.println("FAILED\n");
                System.out.println("Parsing failed due to the following exception(s).");
                for (int i=0;i<validator.getErrors().size();i++){
                    SAXParseException e = (SAXParseException)validator.getErrors().get(i);
                    System.out.println(e.getMessage());
                }
                System.exit(1);
            }
        } catch (ParserConfigurationException e) {
            System.out.println("FAILED\n\nERROR Unable to parse file: " + path);
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (SAXException e) {
            System.out.println("FAILED\n\nERROR Parsing file: " + path);
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("FAILED\n\nUnable to parse file: " + path);
            System.out.println(e.getMessage());
            System.exit(1);
        }

    }

    public static class ValidationHandler extends DefaultHandler{
        private ArrayList errors = new ArrayList();
        private boolean isValid = true;
        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
         */
        @Override
        public void error(SAXParseException e) throws SAXException {
            errors.add(e);
            isValid = false;
        }

        public boolean assertValid(){
            return isValid;
        }

        public ArrayList getErrors(){
            return errors;
        }

        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
         */
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            errors.add(e);
            isValid = false;
        }


    }
}
