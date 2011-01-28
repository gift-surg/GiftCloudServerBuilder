/**
 * 
 */
package org.nrg.xnat.helpers.merge;

import java.util.concurrent.Callable;

import org.apache.plexus.util.StringUtils;
import org.nrg.xdat.model.CatCatalogI;
import org.nrg.xdat.model.CatDcmentryI;
import org.nrg.xdat.model.CatEntryI;
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
public class MergeCatCatalog implements Callable<Boolean> {
	@SuppressWarnings("serial")
	public static class DCMEntryConflict extends Exception {
		public DCMEntryConflict(String string, Exception exception) {
			super(string,exception);
		}

	}

	final CatCatalogI src,dest;
	final boolean overwrite;
	
	public MergeCatCatalog(final CatCatalogI src, final CatCatalogI dest, final boolean overwrite){
		this.src=src;
		this.dest=dest;
		this.overwrite=overwrite;
	}

	public Boolean call() throws DCMEntryConflict,Exception {
		return merge(src,dest,overwrite);
	}
	
	private static boolean merge(final CatCatalogI src, final CatCatalogI dest, final boolean overwrite) throws DCMEntryConflict,Exception {
		boolean merge=false;
		for(final CatCatalogI subCat:src.getSets_entryset()){
			if(merge(subCat,dest,overwrite)){
				merge=true;
			}
		}
		
		for(final CatEntryI entry: src.getEntries_entry()){
			if(entry instanceof CatDcmentryI && !StringUtils.isEmpty(((CatDcmentryI)entry).getUid())){
				final CatDcmentryI destEntry=CatalogUtils.getDCMEntryByUID(dest, ((CatDcmentryI)entry).getUid());
				if(destEntry!=null){
					throw new DCMEntryConflict("Duplicate DCM UID cannot be merged at this time.",new Exception());
				}
			}
			
			final CatEntryI destEntry=CatalogUtils.getEntryByURI(dest, entry.getUri());
			
			if(destEntry==null || overwrite){
				dest.addEntries_entry(entry);
				merge=true;
			}
		}
		
		return merge;
	}
	
}
