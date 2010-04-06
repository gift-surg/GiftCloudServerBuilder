
INSERT INTO xnat_reconstructedImageData_scanID_meta_data (status 
, activation_date 
, insert_date 
, activation_user_xdat_user_id 
, insert_user_xdat_user_id 
, modified 
, meta_data_id
, shareable) (SELECT status 
, activation_date 
, insert_date 
, activation_user_xdat_user_id 
, insert_user_xdat_user_id 
, modified 
, meta_data_id
, shareable FROM xnat_scanid_meta_data);

INSERT INTO xnat_reconstructedImageData_scanID (scanid 
, reconstructedImageData_scanID_info  
, xnat_reconstructedimagedata_scanid_id  
, inscans_scanid_xnat_reconstruct_xnat_reconstructedimagedata_id) 
(SELECT DISTINCT ON (inscans_scanid_xnat_reconstruct_xnat_reconstructedimagedata_id,scanid) scanid ,
  scanid_info ,
  xnat_scanid_id ,
  inscans_scanid_xnat_reconstruct_xnat_reconstructedimagedata_id  FROM xnat_scanid);

INSERT INTO xnat_volumetricRegion_subregion_meta_data (status 
, activation_date 
, insert_date 
, activation_user_xdat_user_id 
, insert_user_xdat_user_id 
, modified 
, meta_data_id
, shareable) (SELECT status 
, activation_date 
, insert_date 
, activation_user_xdat_user_id 
, insert_user_xdat_user_id 
, modified 
, meta_data_id
, shareable FROM xnat_subregion_meta_data);

INSERT INTO xnat_volumetricRegion_subregion (name  
, voxels
, volumetricRegion_subregion_info
, xnat_volumetricregion_subregion_id 
, subregions_subregion_xnat_volum_xnat_volumetricregion_id) (SELECT name,
  voxels,
  subregion_info,
  xnat_subregion_id,
  subregions_subregion_xnat_volum_xnat_volumetricregion_id FROM xnat_subregion);