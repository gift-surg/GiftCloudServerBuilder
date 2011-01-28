package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XMLSetAttributeApp {

	/**
	 * @param args
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		// TODO Auto-generated method stub
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(new File("/tmp/test.xml"));
		Element e = doc.getDocumentElement();
		e.normalize();
		e.setAttribute("proj", "newProj");
		  // use specific Xerces class to write DOM-data to a file:
	    XMLSerializer serializer = new XMLSerializer();
	    serializer.setOutputCharStream(
	      new java.io.FileWriter("/tmp/test.xml"));
	    serializer.serialize(doc);
	}
}
