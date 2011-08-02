package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xdat.bean.XnatImagesessiondataBean;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatResourceI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.model.XnatResourceseriesI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.base.BaseXnatImagesessiondata;
import org.restlet.data.Status;


public class MergePrearchiveSessions extends MergeSessionsA<XnatImagesessiondataBean>  {

	public MergePrearchiveSessions(Object control,final File srcDIR, final XnatImagesessiondataBean src, final String srcRootPath, final File destDIR, final XnatImagesessiondataBean existing, final String destRootPath, boolean overwrite, boolean allowDataDeletion, SaveHandlerI<XnatImagesessiondataBean> saver) {
		super(control, srcDIR, src, srcRootPath, destDIR, existing, destRootPath, overwrite, allowDataDeletion, saver);
		super.setAnonymizer(new SiteWideAnonymizer(src, true));
	}

	public String getCacheBKDirName() { 
		return "prearc_merge";
	}
	

	public org.nrg.xnat.helpers.merge.MergeSessionsA<XnatImagesessiondataBean>.UpdatedSession<XnatImagesessiondataBean> mergeSessions(final XnatImagesessiondataBean src, final String srcRootPath, final XnatImagesessiondataBean dest, final String destRootPath) throws ClientException, ServerException {
		if(dest==null)return new UpdatedSession<XnatImagesessiondataBean>(src, new ArrayList<File>());
		
		final List<XnatImagescandataI> srcScans=src.getScans_scan();
		final List<XnatImagescandataI> destScans=dest.getScans_scan();
	
		final List<File> toDelete=new ArrayList<File>();
		processing("Merging new meta-data into existing meta-data.");
		try {
			for(final XnatImagescandataI srcScan: srcScans){
				final XnatImagescandataI destScan = MergeUtils.getMatchingScan(srcScan,destScans);
				if(destScan==null){
					dest.addScans_scan(srcScan); 
				}else{
					final List<XnatAbstractresourceI> srcRess=srcScan.getFile();
					final List<XnatAbstractresourceI> destRess=destScan.getFile();
					
					for(final XnatAbstractresourceI srcRes:srcRess){
						final XnatAbstractresourceI destRes=MergeUtils.getMatchingResource(srcRes,destRess);
						if(destRes==null){
							destScan.addFile(srcRes);
						}else{
							if(destRes instanceof XnatResourcecatalogI){
								File del=mergeCatalogs(srcRootPath,(XnatResourcecatalogI)srcRes,destRootPath,(XnatResourcecatalogI)destRes);
								if(del!=null)toDelete.add(del);
							}else if(destRes instanceof XnatResourceseriesI){
								srcRes.setLabel(srcRes.getLabel()+"2");
								destScan.addFile(srcRes);
							}else if(destRes instanceof XnatResourceI){
								srcRes.setLabel(srcRes.getLabel()+"2");
								destScan.addFile(srcRes);
							}
						}
					}
				}
			}
		} catch (MergeCatCatalog.DCMEntryConflict e) {
			failed("Duplicate DCM UID cannot be merged at this time.");
			throw new ClientException(Status.CLIENT_ERROR_CONFLICT,e.getMessage(), e);
		} catch (Exception e) {
			failed("Failed to merge upload into existing prearchive.");
			throw new ServerException(e.getMessage(), e);
		}
		
		return new UpdatedSession<XnatImagesessiondataBean>(dest, toDelete);
	}



	@Override
	public void finalize(XnatImagesessiondataBean session)
			throws ClientException, ServerException {
		session.setPrearchivepath(this.destRootPath);
	}



}
