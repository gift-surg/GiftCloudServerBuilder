package org.nrg.xnat.restlet.representations;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;

public class JSONObjectRepresentation extends OutputRepresentation {
	final JSONObject o;
	
	public JSONObjectRepresentation(MediaType mediaType,final JSONObject o) {
		super(mediaType);
		this.o=o;		
	}

	@Override
	public void write(OutputStream os) throws IOException {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
			o.write(writer);
			writer.flush();
		} catch (JSONException e) {
			new IOException(e);
		}
	}

}
