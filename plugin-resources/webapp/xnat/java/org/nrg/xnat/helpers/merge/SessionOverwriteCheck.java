package org.nrg.xnat.helpers.merge;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.model.CatCatalogI;
import org.nrg.xdat.model.CatDcmentryI;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.model.XnatResourceI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.model.XnatResourceseriesI;
import org.nrg.xnat.utils.CatalogUtils;

public class SessionOverwriteCheck implements Callable<Boolean> {
	final XnatImagesessiondataI src,dest;
	final String srcRootPath,destRootPath;
	
	public SessionOverwriteCheck(XnatImagesessiondataI src, XnatImagesessiondataI dest,String srcRootPath,String destRootPath){
		this.src=src;
		this.dest=dest;
		this.srcRootPath=srcRootPath;
		this.destRootPath=destRootPath;
	}
	
	@Override
	public Boolean call(){
		final List<XnatImagescandataI> srcScans=src.getScans_scan();
		final List<XnatImagescandataI> destScans=dest.getScans_scan();

		for(final XnatImagescandataI srcScan: srcScans){
			final XnatImagescandataI destScan = MergeUtils.getMatchingScan(srcScan,destScans);
			if(destScan==null){
			}else{
				final List<XnatAbstractresourceI> srcRess=srcScan.getFile();
				final List<XnatAbstractresourceI> destRess=destScan.getFile();
				
				for(final XnatAbstractresourceI srcRes:srcRess){
					final XnatAbstractresourceI destRes=MergeUtils.getMatchingResource(srcRes,destRess);
					if(destRes==null){
					}else{
						if(destRes instanceof XnatResourcecatalogI){
							final CatCatalogBean srcCat=CatalogUtils.getCleanCatalog(srcRootPath, (XnatResourcecatalogI)srcRes, false);

							final CatCatalogBean destCat=CatalogUtils.getCleanCatalog(destRootPath, (XnatResourcecatalogI)destRes, false);
							
							if(detectOverwrite(srcCat,destCat)){
								return true;
							}
						}else if(destRes instanceof XnatResourceseriesI){
							return true;
						}else if(destRes instanceof XnatResourceI){
							return true;
						}
					}
				}
			}
		}
		
		return false;
	}

	
	private static boolean detectOverwrite(final CatCatalogI src, final CatCatalogI dest)  {
		boolean merge=false;
		for(final CatCatalogI subCat:src.getSets_entryset()){
			if(detectOverwrite(subCat,dest)){
				return true;
			}
		}
		
		for(final CatEntryI entry: src.getEntries_entry()){
			if(entry instanceof CatDcmentryI && !StringUtils.isEmpty(((CatDcmentryI)entry).getUid())){
				final CatDcmentryI destEntry=CatalogUtils.getDCMEntryByUID(dest, ((CatDcmentryI)entry).getUid());
				if(destEntry!=null){
					return true;
				}
			}
			
			final CatEntryI destEntry=CatalogUtils.getEntryByURI(dest, entry.getUri());
			
			if(destEntry==null){
				return true;
			}
		}
		
		return merge;
	}
}
