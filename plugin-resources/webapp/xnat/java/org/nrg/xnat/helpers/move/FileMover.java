package org.nrg.xnat.helpers.move;

import java.io.File;

import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.helpers.file.StoredFile;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveURI;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.utils.UserUtils;

public class FileMover {
	final boolean overwrite;
	final XDATUser user;
	
	public FileMover(Boolean overwrite, XDATUser user){
		if(overwrite==null){
			this.overwrite=false;
		}else{
			this.overwrite=overwrite;
		}
		
		this.user=user;
	}
	
	public Boolean call(org.nrg.xnat.helpers.uri.URIManager.UserCacheURI src,ArchiveURI dest) throws Exception {
		File srcF;		
		if(src.getProps().containsKey(UriParserUtils._REMAINDER)){
			srcF=UserUtils.getUserCacheFile(user, (String)src.getProps().get(URIManager.XNAME), (String)src.getProps().get(UriParserUtils._REMAINDER));
		}else{
			srcF=UserUtils.getUserCacheFile(user, (String)src.getProps().get(URIManager.XNAME));
		}
		
		File destF;
		if(dest.getProps().containsKey(UriParserUtils._REMAINDER)){
			destF=UserUtils.getUserCacheFile(user, (String)dest.getProps().get(URIManager.XNAME), (String)dest.getProps().get(UriParserUtils._REMAINDER));
		}else{
			destF=UserUtils.getUserCacheFile(user, (String)dest.getProps().get(URIManager.XNAME));
		}
						
		FileWriterWrapperI fw=new StoredFile(srcF,overwrite);
		
		
		
		
		return Boolean.TRUE;
	}

}
