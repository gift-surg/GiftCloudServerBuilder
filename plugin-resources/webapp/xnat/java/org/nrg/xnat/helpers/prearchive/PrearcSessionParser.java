package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.IOException;

import org.nrg.xdat.bean.reader.XDATXMLReader;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class PrearcSessionParser extends XDATXMLReader{
	private static final String stopAtPath="prearchivePath";
	boolean stopRecording=false;
	
	
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if(!stopRecording)super.characters(ch, start, length);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(!stopRecording)super.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(stopRecording)return;
		
		super.endElement(uri, localName, qName);
		
		if (localName.equals(stopAtPath)){
           // stopRecording=true;
        }
	}

	public final XnatImagesessiondataI parseSession(final File s) throws IOException, SAXException {
		return (XnatImagesessiondataI) this.parse(s);
	}
}
