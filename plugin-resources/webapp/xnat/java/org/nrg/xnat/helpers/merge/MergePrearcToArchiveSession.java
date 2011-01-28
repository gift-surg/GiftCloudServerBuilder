package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatResourceI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.model.XnatResourceseriesI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xnat.turbine.utils.XNATUtils;
import org.restlet.data.Status;


public  class  MergePrearcToArchiveSession extends  MergeSessionsA<XnatImagesessiondata> {
	public MergePrearcToArchiveSession(Object control,final File srcDIR, final XnatImagesessiondata src, final String srcRootPath, final File destDIR, final XnatImagesessiondata existing, final String destRootPath, boolean overwrite, boolean allowDataDeletion,SaveHandlerI<XnatImagesessiondata> saver) {
		super(control, srcDIR, src, destRootPath, destDIR, existing, destRootPath, overwrite, allowDataDeletion,saver);
	}


	public String getCacheBKDirName() {
		return "merge";
	}

	public void finalize(XnatImagesessiondata session) throws ClientException,
			ServerException {
		final String root = destRootPath.replace('\\','/') + "/";
		for(XnatImagescandataI scan:session.getScans_scan()){
			for (final XnatAbstractresourceI file : scan.getFile()) {
				((XnatAbstractresource)file).prependPathsWith(root);
				
				if (XNATUtils.isNullOrEmpty(((XnatAbstractresource)file).getContent())) {
					((XnatResource)file).setContent("RAW");
				}
			}
		}
	}

	public org.nrg.xnat.helpers.merge.MergeSessionsA<XnatImagesessiondata>.UpdatedSession<XnatImagesessiondata> mergeSessions(
			XnatImagesessiondata src, String srcRootPath,
			XnatImagesessiondata dest, String destRootPath)
			throws ClientException, ServerException {
		if(dest==null)return new UpdatedSession<XnatImagesessiondata>(src,new ArrayList<File>());
		
		final List<XnatImagescandataI> srcScans=src.getScans_scan();
		final List<XnatImagescandataI> destScans=dest.getScans_scan();
		
		final List<File> toDelete=new ArrayList<File>();
		processing("Merging new meta-data into existing meta-data.");
		try {
			for(final XnatImagescandataI srcScan: srcScans){
				final XnatImagescandataI destScan = MergeUtils.getMatchingScan(srcScan,destScans);
				if(destScan!=null){
					final List<XnatAbstractresourceI> srcRess=srcScan.getFile();
					final List<XnatAbstractresourceI> destRess=destScan.getFile();
					
					for(final XnatAbstractresourceI srcRes:srcRess){
						final XnatAbstractresourceI destRes=MergeUtils.getMatchingResource(srcRes,destRess);
						if(destRes!=null){
							if(destRes instanceof XnatResourcecatalogI){
								File del=mergeCatalogs(srcRootPath,(XnatResourcecatalogI)srcRes,destRootPath,(XnatResourcecatalogI)destRes);
								if(del!=null)toDelete.add(del);
							}else if(destRes instanceof XnatResourceseriesI){
								srcRes.setLabel(srcRes.getLabel()+"2");
								srcScan.addFile(destRes);
							}else if(destRes instanceof XnatResourceI){
								srcRes.setLabel(srcRes.getLabel()+"2");
								srcScan.addFile(destRes);
							}
						}
					}
				}
			}
			
			src.copyValuesFrom(dest);
		} catch (MergeCatCatalog.DCMEntryConflict e) {
			failed("Duplicate DCM UID cannot be merged at this time.");
			throw new ClientException(Status.CLIENT_ERROR_CONFLICT,e.getMessage(), e);
		} catch (Exception e) {
			failed("Failed to merge upload into existing data.");
			throw new ServerException(e.getMessage(), e);
		}
		
		return new UpdatedSession<XnatImagesessiondata>(src, toDelete);
	}

}
