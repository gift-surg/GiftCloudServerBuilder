/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.helpers.resource.cat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.zip.TarUtils;
import org.nrg.xft.utils.zip.ZipI;
import org.nrg.xft.utils.zip.ZipUtils;
import org.nrg.xnat.helpers.FileWriterWrapper;
import org.nrg.xnat.helpers.resource.XnatResourceInfo;
import org.nrg.xnat.utils.CatalogUtils;

/**
 * @author timo
 * 
 */
public class CatalogResourceModifier {
	public static boolean storeCatalogEntry(final FileWriterWrapper fi, final String dest, final XnatResourcecatalog catResource, final XnatProjectdata proj, final boolean extract, final XnatResourceInfo info) throws IOException, Exception {
		final File catFile = catResource.getCatalogFile(proj.getRootArchivePath());
		final String parentPath = catFile.getParent();
		final CatCatalogBean cat = catResource.getCleanCatalog(proj.getRootArchivePath(), false);

		String filename = fi.getName();

		int index = filename.lastIndexOf('\\');
		if (index < filename.lastIndexOf('/')) {
			index = filename.lastIndexOf('/');
		}
		if (index > 0) {
			filename = filename.substring(index + 1);
		}

		String compression_method = ".zip";
		if (filename.indexOf(".") != -1) {
			compression_method = filename.substring(filename.lastIndexOf("."));
		}

		if (extract && (compression_method.equalsIgnoreCase(".tar") || compression_method.equalsIgnoreCase(".gz") || compression_method.equalsIgnoreCase(".zip") || compression_method.equalsIgnoreCase(".zar"))) {
			final File destinationDir = catFile.getParentFile();
			final InputStream is = fi.getInputStream();

			ZipI zipper = null;
			if (compression_method.equalsIgnoreCase(".tar")) {
				zipper = new TarUtils();
			} else if (compression_method.equalsIgnoreCase(".gz")) {
				zipper = new TarUtils();
				zipper.setCompressionMethod(ZipOutputStream.DEFLATED);
			} else {
				zipper = new ZipUtils();
			}

			final List<File> files = zipper.extract(is, destinationDir.getAbsolutePath());

			for (final File f : files) {
				if (!f.isDirectory()) {
					final String relative = destinationDir.toURI().relativize(f.toURI()).getPath();

					final CatEntryI e = CatalogUtils.getEntryByURI(cat, relative);

					if (e == null) {
						final CatEntryBean newEntry = new CatEntryBean();
						newEntry.setUri(relative);
						newEntry.setName(f.getName());

						CatalogUtils.configureEntry(newEntry, info);

						cat.addEntries_entry(newEntry);
					}
				}
			}
		} else {
			final File saveTo = new File(parentPath, dest);

			saveTo.getParentFile().mkdirs();
			fi.write(saveTo);

			final CatEntryI e = CatalogUtils.getEntryByURI(cat, dest);

			if (e == null) {
				final CatEntryBean newEntry = new CatEntryBean();
				newEntry.setUri(dest);
				newEntry.setName(saveTo.getName());
				
				CatalogUtils.configureEntry(newEntry, info);

				cat.addEntries_entry(newEntry);
			}
		}

		final FileOutputStream fos = new FileOutputStream(catFile);
		OutputStreamWriter fw;
		try {
			final FileLock fl = fos.getChannel().lock();
			try {
				fw = new OutputStreamWriter(fos);
				cat.toXML(fw, true);
				fw.flush();
			} finally {
				fl.release();
			}
		} finally {
			fos.close();
		}

		return true;
	}
}
