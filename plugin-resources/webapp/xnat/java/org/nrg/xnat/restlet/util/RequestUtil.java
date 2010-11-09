// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
	
	public static String deriveFileName(final String prefix,final Representation entity){
		if(entity==null || entity.getMediaType()==null){
			return null;
		}
		
		if(supported_upload_types.containsKey(entity.getMediaType())){
			return prefix+supported_upload_types.get(entity.getMediaType());
		}
		
		return null;
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
		}else{
			return false;
		}
	}
}
