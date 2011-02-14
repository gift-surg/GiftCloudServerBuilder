// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.util;

import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.resource.Representation;

import com.noelios.restlet.ext.servlet.ServletCall;

public class RequestUtil {
	public HttpServletRequest getHttpServletRequest(Request request) {
		return ServletCall.getRequest(request);
	}

	private static Map<MediaType,String> supported_upload_types=new Hashtable<MediaType,String>();
	
	static{
		supported_upload_types.put(MediaType.APPLICATION_ALL,"");
		supported_upload_types.put(MediaType.APPLICATION_ZIP,".zip");
		supported_upload_types.put(SecureResource.APPLICATION_XAR,".xar");
		supported_upload_types.put(MediaType.APPLICATION_GNU_TAR,".tar.gz");
		supported_upload_types.put(MediaType.APPLICATION_GNU_ZIP,".gzip");
		supported_upload_types.put(MediaType.APPLICATION_OCTET_STREAM,"");
		supported_upload_types.put(MediaType.APPLICATION_PDF,".pdf");
		supported_upload_types.put(MediaType.APPLICATION_EXCEL,".xls");
		supported_upload_types.put(MediaType.APPLICATION_POWERPOINT,".ppt");
		supported_upload_types.put(MediaType.APPLICATION_TAR,".tar");
		supported_upload_types.put(MediaType.APPLICATION_WORD,".doc");
		supported_upload_types.put(MediaType.IMAGE_ALL,".img");
		supported_upload_types.put(MediaType.IMAGE_BMP,".bmp");
		supported_upload_types.put(MediaType.IMAGE_GIF,".gif");
		supported_upload_types.put(MediaType.IMAGE_ICON,".icon");
		supported_upload_types.put(MediaType.IMAGE_JPEG,".jpeg");
		supported_upload_types.put(MediaType.IMAGE_PNG,".png");
		supported_upload_types.put(MediaType.IMAGE_SVG,".svg");
		supported_upload_types.put(MediaType.IMAGE_TIFF,".tiff");
		supported_upload_types.put(MediaType.TEXT_XML,".xml");
		supported_upload_types.put(MediaType.VIDEO_ALL,".video");
		supported_upload_types.put(MediaType.VIDEO_AVI,".avi");
		supported_upload_types.put(MediaType.VIDEO_MP4,".mp4");
		supported_upload_types.put(MediaType.VIDEO_MPEG,".mpg");
		supported_upload_types.put(MediaType.VIDEO_QUICKTIME,".mov");
		supported_upload_types.put(MediaType.VIDEO_WMV,".wmv");
}
	
	// method with boolean flag to allow curl processing where media types are not specified
	public static String deriveFileName(final String prefix,final Representation entity, boolean returnNullForUnsupported){
		if(!(entity==null || entity.getMediaType()==null)){
			if(supported_upload_types.containsKey(entity.getMediaType())){
				return prefix+supported_upload_types.get(entity.getMediaType());
			}
		}
		if (returnNullForUnsupported) {
			return null;
		} else {
			return prefix+".unsuppported";
		}
	}
	
	// original deriveFileName method returned null filename for null/unsupported types
	public static String deriveFileName(final String prefix,final Representation entity){
		return deriveFileName(prefix,entity,true);
	}
	
	public static boolean isFileInBody(Representation entity){
		if(entity==null || entity.getMediaType()==null){
			return false;
		}
		
		final MediaType mt=entity.getMediaType();
		if(supported_upload_types.containsKey(mt))
		{
			return true;
		}else{
			return false;
		}
	}
	
	public  static boolean isMultiPartFormData(Representation entity){
		if(entity==null || entity.getMediaType()==null){
			return false;
		}
		
		final MediaType mt=entity.getMediaType();
		if(mt.equals(MediaType.MULTIPART_ALL))
		{
			return true;
		}else if(mt.equals(MediaType.MULTIPART_FORM_DATA))
		{
			return true;
		}else if(mt.equals(MediaType.APPLICATION_WWW_FORM))
		{
			return true;
		// check string values of main/subtypes where doesn't resolve to one of the above media types.
		// Happens for some Curl requests
		}else if (mt.getMainType().equalsIgnoreCase("multipart") && mt.getSubType().equalsIgnoreCase("form-data")){
			return true;
		}else{
			return false;
		}
	}
}
