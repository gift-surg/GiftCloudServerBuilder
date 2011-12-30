/**
 * 
 */
package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.model.CatCatalogI;
import org.nrg.xdat.model.CatDcmentryI;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.utils.CatalogUtils;

/**
 * @author tolsen01
 *
 * Merges src Catalog into dest Catalog.  Returns true if something was modified.  
 * 
 * if duplicate URI, then (if overwrite=true, then copy, else throw ClientException)
 * 
 * if duplicate DICOM UID, then throw exception, until we can compare Class-UID too.
 */
public class MergeCatCatalog implements Callable<MergeSessionsA.Results<Boolean>> {
	@SuppressWarnings("serial")
	public static class DCMEntryConflict extends Exception {
		public DCMEntryConflict(String string, Exception exception) {
			super(string,exception);
		}

	}
	@SuppressWarnings("serial")
	public static class EntryConflict extends Exception {
		public EntryConflict(String string, Exception exception) {
			super(string,exception);
		}

	}

	final CatCatalogI src,dest;
	final boolean overwrite;
	final EventMetaI ci;
	final File destCatFile;
	
	public MergeCatCatalog(final CatCatalogI src, final CatCatalogI dest, final boolean overwrite, final EventMetaI ci, final File destCatFile){
		this.src=src;
		this.dest=dest;
		this.overwrite=overwrite;
		this.ci=ci;
		this.destCatFile=destCatFile;
	}

	public MergeSessionsA.Results<Boolean> call() throws DCMEntryConflict,Exception {
		return merge(src,dest,overwrite,ci,destCatFile);
	}
	
	private static MergeSessionsA.Results<Boolean> merge(final CatCatalogI src, final CatCatalogI dest, final boolean overwrite,final EventMetaI ci, final File destCatFile) throws DCMEntryConflict,Exception {
		boolean merge=false;
		MergeSessionsA.Results<Boolean> result=new MergeSessionsA.Results<Boolean>();
		for(final CatCatalogI subCat:src.getSets_entryset()){
			final MergeSessionsA.Results<Boolean> r=merge(subCat,dest,overwrite,ci,destCatFile);
			if(r.result){
				merge=true;
			}
			result.addAll(r);
		}
		
		for(final CatEntryI entry: src.getEntries_entry()){
			if(!overwrite){
				if(entry instanceof CatDcmentryI && !StringUtils.isEmpty(((CatDcmentryI)entry).getUid())){
					final CatEntryI destEntry=CatalogUtils.getDCMEntryByUID(dest, ((CatDcmentryI)entry).getUid());
					if(destEntry!=null){
						throw new DCMEntryConflict("Duplicate DCM UID cannot be merged at this time.",new Exception());
					}
				}
			}
			
			final CatEntryI destEntry=CatalogUtils.getEntryByURI(dest, entry.getUri());
			
			if(destEntry==null){
				dest.addEntries_entry(entry);
				merge=true;
			}else if(!overwrite){
				throw new EntryConflict("Duplicate file uploaded.",new Exception());
			}else if(ci!=null){
				//backup existing file
				//the entry should be copied to the .history catalog, the file will be moved separately
				result.getAfter().add(new Callable<Boolean>(){
					@Override
					public Boolean call() throws Exception {
						File f=FileUtils.BuildHistoryFile(CatalogUtils.getFile(destEntry, destCatFile.getParentFile().getAbsolutePath()),EventUtils.getTimestamp(ci));
						CatalogUtils.addCatHistoryEntry(destCatFile, (CatCatalogBean)dest, f.getAbsolutePath(), (CatEntryBean)entry, ci);
						return true;
					}});
			}
		}
		
		return result.setResult(merge);
	}
	
}
