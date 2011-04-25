/**
 * Copyright (c) 2006-2011 Washington University
 */
package org.nrg.dcm;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.PDVInputStream;
import org.dcm4che2.net.service.CStoreSCP;
import org.dcm4che2.net.service.DicomService;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.archive.GradualDicomImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 */
public class CStoreService extends DicomService implements CStoreSCP, Closeable {
    private static final String PhilipsPrivateCXImageStorage = "1.3.46.670589.2.4.1.1";
    private static final String PhilipsPrivateVolumeStorage = "1.3.46.670589.5.0.1";
    private static final String PhilipsPrivate3DObjectStorage = "1.3.46.670589.5.0.2";
    private static final String PhilipsPrivate3DObject2Storage = "1.3.46.670589.5.0.2.1";
    private static final String PhilipsPrivateSurfaceStorage = "1.3.46.670589.5.0.3";
    private static final String PhilipsPrivateSurface2Storage = "1.3.46.670589.5.0.3.1";
    private static final String PhilipsPrivateCompositeObjectStorage = "1.3.46.670589.5.0.4";
    private static final String PhilipsPrivateMRCardioProfile = "1.3.46.670589.5.0.7";
    private static final String PhilipsPrivateMRCardio = "1.3.46.670589.5.0.8";
    private static final String PhilipsPrivateCTSyntheticImageStorage = "1.3.46.670589.5.0.9";
    private static final String PhilipsPrivateMRSyntheticImageStorage = "1.3.46.670589.5.0.10";
    private static final String PhilipsPrivateMRCardioAnalysisStorage = "1.3.46.670589.5.0.11";
    private static final String PhilipsPrivateCXSyntheticImageStorage = "1.3.46.670589.5.0.12";
    private static final String PhilipsPrivateGyroscanMRSpectrum = "1.3.46.670589.11.0.0.12.1";
    private static final String PhilipsPrivateGyroscanMRSerieData = "1.3.46.670589.11.0.0.12.2";
    private static final String PhilipsPrivateMRExamcardStorage = "1.3.46.670589.11.0.0.12.4";
    private static final String PhilipsPrivateSpecializedXAStorage = "1.3.46.670589.2.3.1.1";

    private static final String[] CUIDS = { // Full list in PS 3.4, Annex B.5
        UID.ComputedRadiographyImageStorage,
        UID.DigitalXRayImageStorageForPresentation,
        UID.DigitalXRayImageStorageForProcessing,
        UID.DigitalMammographyXRayImageStorageForPresentation,
        UID.DigitalMammographyXRayImageStorageForProcessing,
        UID.DigitalIntraoralXRayImageStorageForPresentation,
        UID.DigitalIntraoralXRayImageStorageForProcessing,
        UID.CTImageStorage, UID.EnhancedCTImageStorage,
        UID.UltrasoundMultiframeImageStorage, UID.MRImageStorage,
        UID.EnhancedMRImageStorage,
        UID.MRSpectroscopyStorage,
        UID.UltrasoundImageStorage,
        UID.SecondaryCaptureImageStorage,
        UID.MultiframeSingleBitSecondaryCaptureImageStorage,
        UID.MultiframeGrayscaleByteSecondaryCaptureImageStorage,
        UID.MultiframeGrayscaleWordSecondaryCaptureImageStorage,
        UID.MultiframeTrueColorSecondaryCaptureImageStorage,
        UID._12leadECGWaveformStorage,
        UID.GeneralECGWaveformStorage,
        UID.AmbulatoryECGWaveformStorage,
        UID.HemodynamicWaveformStorage,
        UID.CardiacElectrophysiologyWaveformStorage,
        UID.BasicVoiceAudioWaveformStorage,
        UID.GrayscaleSoftcopyPresentationStateStorageSOPClass,
        UID.ColorSoftcopyPresentationStateStorageSOPClass,
        UID.PseudoColorSoftcopyPresentationStateStorageSOPClass,
        UID.BlendingSoftcopyPresentationStateStorageSOPClass,
        UID.XRayAngiographicImageStorage,
        UID.EnhancedXAImageStorage,
        UID.XRayRadiofluoroscopicImageStorage,
        UID.EnhancedXRFImageStorage,
        UID.XRay3DAngiographicImageStorage,
        UID.XRay3DCraniofacialImageStorage,
        UID.NuclearMedicineImageStorage,
        UID.RawDataStorage,
        UID.SpatialRegistrationStorage,
        UID.SpatialFiducialsStorage,
        UID.DeformableSpatialRegistrationStorage,
        UID.SegmentationStorage,
        UID.RealWorldValueMappingStorage,
        UID.VLEndoscopicImageStorage,
        UID.VideoEndoscopicImageStorage,
        UID.VLMicroscopicImageStorage,
        UID.VideoMicroscopicImageStorage,
        UID.VLSlideCoordinatesMicroscopicImageStorage,
        UID.VLPhotographicImageStorage,
        UID.VideoPhotographicImageStorage,
        UID.OphthalmicPhotography8BitImageStorage,
        UID.OphthalmicPhotography16BitImageStorage,
        UID.OphthalmicTomographyImageStorage,
        UID.StereometricRelationshipStorage,
        UID.BasicTextSRStorage,
        UID.EnhancedSRStorage,
        UID.ComprehensiveSRStorage,
        UID.ProcedureLogStorage,
        UID.MammographyCADSRStorage,
        UID.KeyObjectSelectionDocumentStorage,
        UID.ChestCADSRStorage,
        UID.XRayRadiationDoseSRStorage,
        UID.EncapsulatedPDFStorage,
        UID.PositronEmissionTomographyImageStorage,
        UID.RTImageStorage,
        UID.RTDoseStorage,
        UID.RTStructureSetStorage,
        UID.RTBeamsTreatmentRecordStorage,
        UID.RTPlanStorage,
        UID.RTBrachyTreatmentRecordStorage,
        UID.RTTreatmentSummaryRecordStorage,
        UID.RTIonPlanStorage,
        UID.RTIonBeamsTreatmentRecordStorage,
        UID.SiemensCSANonImageStorage, // Siemens proprietary; we get this sometimes
        PhilipsPrivateCXImageStorage, // Philips proprietary. Thanks, Philips.
        PhilipsPrivateVolumeStorage, PhilipsPrivate3DObjectStorage,
        PhilipsPrivate3DObject2Storage, PhilipsPrivateSurfaceStorage,
        PhilipsPrivateSurface2Storage,
        PhilipsPrivateCompositeObjectStorage,
        PhilipsPrivateMRCardioProfile, PhilipsPrivateMRCardio,
        PhilipsPrivateCTSyntheticImageStorage,
        PhilipsPrivateMRSyntheticImageStorage,
        PhilipsPrivateMRCardioAnalysisStorage,
        PhilipsPrivateCXSyntheticImageStorage,
        PhilipsPrivateGyroscanMRSpectrum,
        PhilipsPrivateGyroscanMRSerieData, PhilipsPrivateMRExamcardStorage,
        PhilipsPrivateSpecializedXAStorage, };

    public static final int SUCCESS = 0;
    public static final int REFUSED_OUT_OF_RESOURCES = 0xA700;
    public static final int ERROR_DATA_SET_SOP_CLASS_MISMATCH = 0xA900;
    public static final int ERROR_CANNOT_UNDERSTAND = 0xC000;
    public static final int WARNING_COERCION_DATA_ELEMENTS = 0xB000;
    public static final int WARNING_DATA_SET_SOP_CLASS_MISMATCH = 0xB007;
    public static final int WARNING_ELEMENTS_DISCARDED = 0xB006;

    private final Logger logger = LoggerFactory.getLogger(CStoreService.class);
    private final NetworkApplicationEntity ae;
    private final XDATUser user;

    public CStoreService(final NetworkApplicationEntity ae, final XDATUser user)
    throws IOException {
        super(CUIDS);
        this.ae = ae;
        ae.register(this);
        this.user = user;

        logger.info("Starting C-STORE service {} for user {} on port {}",
                new Object[] { ae.getAETitle(), user.getUsername(),
                ae.getNetworkConnection()[0].getPort() });
    }

    /**
     * Release resources held by this object and clear the class singleton
     * handle. A subsequent call to getInstance() will create a new object.
     * 
     * @throws IOException
     */
    @Override
    public void close() {
        logger.info("Stopping C-STORE service");
        ae.unregister(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dcm4che2.net.service.CStoreSCP#cstore(org.dcm4che2.net.Association,
     * int, org.dcm4che2.data.DicomObject, org.dcm4che2.net.PDVInputStream,
     * java.lang.String)
     */
    /**
     * Adapted from StorageService, with a few tweaks for better compliance with
     * the standard: (see PS 3.7, section 9.3.1.2) + standard requires UID
     * fields in C-STORE-RSP + standard requires Data Set Type (Null) in
     * response
     */
    @Override
    public void cstore(final Association as, final int pcid,
            final DicomObject rq, final PDVInputStream dataStream,
            final String tsuid) throws DicomServiceException,IOException {
        final boolean includeUIDs = CommandUtils.isIncludeUIDinRSP();
        CommandUtils.setIncludeUIDinRSP(true);
        final DicomObject rsp = CommandUtils.mkRSP(rq, CommandUtils.SUCCESS);
        rsp.putInt(Tag.DataSetType, VR.US, 0x0101);
        doCStore(as, pcid, rq, dataStream, tsuid, rsp);
        as.writeDimseRSP(pcid, rsp);
        CommandUtils.setIncludeUIDinRSP(includeUIDs);
    }

    private final Object identifySender(final Association association) {
            return new StringBuilder()
            .append(association.getRemoteAET()).append("@")
            .append(association.getSocket().getRemoteSocketAddress());
    }
    
    private void doCStore(final Association as, final int pcid,
            final DicomObject rq, final PDVInputStream dataStream,
            final String tsuid, final DicomObject rsp)
    throws DicomServiceException {
        try {
            final DicomObject dataset;
            try {
                dataset = dataStream.readDataset();
            } catch (final IOException e) {
                logger.error("C-STORE operation failed", e);
                throw new DicomServiceException(rq, ERROR_CANNOT_UNDERSTAND,
                        e.getMessage());
            }
            dataset.putString(Tag.TransferSyntaxUID, VR.UI, tsuid);
            try {
                new GradualDicomImporter(this, user, dataset,
                        Collections.singletonMap(GradualDicomImporter.SENDER_ID_PARAM,
                                identifySender(as))).call();
            } catch (final ClientException e) {
                logger.error("C-STORE operation failed", e);
                throw new DicomServiceException(rq, ERROR_CANNOT_UNDERSTAND,
                        e.getMessage());
            } catch (final ServerException e) {
                logger.error("C-STORE operation failed", e);
                throw new DicomServiceException(rq, REFUSED_OUT_OF_RESOURCES,
                        e.getMessage());
            }
        } catch (DicomServiceException e) {
            throw e;
        } catch (final Throwable e) {
            // Don't let mysterious unchecked exceptions and errors through.
            logger.error("C-STORE operation failed", e);
            throw new DicomServiceException(rq, REFUSED_OUT_OF_RESOURCES,
                    e.getMessage());
        }
    }
}
