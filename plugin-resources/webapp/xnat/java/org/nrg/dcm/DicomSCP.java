/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.dcm;

import static org.dcm4che2.data.UID.ExplicitVRBigEndian;
import static org.dcm4che2.data.UID.ExplicitVRLittleEndian;
import static org.dcm4che2.data.UID.ImplicitVRLittleEndian;
import static org.dcm4che2.data.UID.JPEG2000;
import static org.dcm4che2.data.UID.JPEG2000LosslessOnly;
import static org.dcm4che2.data.UID.JPEG2000Part2Multicomponent;
import static org.dcm4che2.data.UID.JPEG2000Part2MulticomponentLosslessOnly;
import static org.dcm4che2.data.UID.JPEGBaseline1;
import static org.dcm4che2.data.UID.JPEGExtended24;
import static org.dcm4che2.data.UID.JPEGLSLossless;
import static org.dcm4che2.data.UID.JPEGLSLossyNearLossless;
import static org.dcm4che2.data.UID.JPEGLossless;
import static org.dcm4che2.data.UID.JPEGLosslessNonHierarchical14;
import static org.dcm4che2.data.UID.JPIPReferenced;
import static org.dcm4che2.data.UID.JPIPReferencedDeflate;
import static org.dcm4che2.data.UID.MPEG2;
import static org.dcm4che2.data.UID.RFC2557MIMEencapsulation;
import static org.dcm4che2.data.UID.RLELossless;
import static org.dcm4che2.data.UID.VerificationSOPClass;
import static org.dcm4che2.data.UID.XMLEncoding;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.dcm4che2.net.Device;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.DicomService;
import org.dcm4che2.net.service.VerificationService;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 * 
 */
public class DicomSCP {
    private static final String DICOM_SCP_PROPS = "DICOM-SCP.properties";
    private static final String DICOM_SCP_USER = "DICOM.user";
    private static final String DICOM_SCP_DEFAULT_USER = "admin";
    private static final String DICOM_SCP_PORT = "DICOM.port";
    
    private static final String SERVICE_NAME = "XNAT_DICOM";

    // Verification service can only use LE encoding
    private static final String[] VERIFICATION_SOP_TS = {
        ImplicitVRLittleEndian, ExplicitVRLittleEndian };

    // Accept just about anything. Some of these haven't been tested and
    // might not actually work correctly (e.g., XML encoding); some probably
    // can be received but will give the XNAT processing pipeline fits
    // (e.g., anything compressed).
    private static final String[] TSUIDS = { ExplicitVRLittleEndian,
        ExplicitVRBigEndian, ImplicitVRLittleEndian, JPEGBaseline1,
        JPEGExtended24, JPEGLosslessNonHierarchical14, JPEGLossless,
        JPEGLSLossless, JPEGLSLossyNearLossless, JPEG2000LosslessOnly,
        JPEG2000, JPEG2000Part2MulticomponentLosslessOnly,
        JPEG2000Part2Multicomponent, JPIPReferenced, JPIPReferencedDeflate,
        MPEG2, RLELossless, RFC2557MIMEencapsulation, XMLEncoding };

    private final Logger logger = LoggerFactory.getLogger(DicomSCP.class);

    private final Executor executor;
    private final Device device;
    private final XDATUser user;

    public DicomSCP(final XDATUser user, final Executor executor,
            final Device device, final NetworkApplicationEntity ae)
    throws IOException {
        this.executor = executor;
        this.device = device;
        this.user = user;
        final CStoreService cstore = new CStoreService(ae, user);
        initTransferCapability(ae, cstore);
    }

    private static Logger slog() {
        return LoggerFactory.getLogger(DicomSCP.class);
    }
    
    public static Properties getProperties() {
        final File propsfile = new File(XFT.GetConfDir(), DicomSCP.DICOM_SCP_PROPS);
        final Properties properties = new Properties();
        try {
            properties.load(new FileReader(propsfile));
        } catch (IOException e) {
            slog().debug("no DICOM SCP properties file " + propsfile + " found", e);
        }
        return properties;
    }
    
    public static XDATUser getUser(final Properties properties) throws Exception {
        return new XDATUser(properties.getProperty(DicomSCP.DICOM_SCP_USER, DicomSCP.DICOM_SCP_DEFAULT_USER));
    }
    
    public static XDATUser getUser() throws Exception {
        return getUser(getProperties());
    }
    
    public static int getPort(final Properties properties, final String defaultPort) {
        final String sp = properties.getProperty(DicomSCP.DICOM_SCP_PORT, defaultPort);
        return Integer.parseInt(sp);
    }
    
    public static DicomSCP makeSCP(final XDATUser user, final String aeTitle,
            final int port) throws IOException {
        final NetworkConnection nc = new NetworkConnection();
        nc.setPort(port);

        final NetworkApplicationEntity ae = new NetworkApplicationEntity();
        ae.setNetworkConnection(nc);
        ae.setAssociationAcceptor(true);
        ae.register(new VerificationService());
        ae.setAETitle(aeTitle);

        final Device device = new Device(SERVICE_NAME);
        device.setNetworkConnection(nc);
        device.setNetworkApplicationEntity(ae);

        final Executor executor = new NewThreadExecutor(SERVICE_NAME);

        return new DicomSCP(user, executor, device, ae);
    }

    public void start() throws IOException {
        logger.info("starting DICOM SCP {} on port {} as user {}",
                new Object[] {
                device.getNetworkApplicationEntity(),
                device.getNetworkConnection()[0].getPort(),device,
                user.getUsername()
        });
        device.startListening(executor);
    }

    public void stop() {
        logger.info("stopping DICOM SCP");
        device.stopListening();
    }

    private void initTransferCapability(final NetworkApplicationEntity ae,
            final DicomService... services) {
        final List<TransferCapability> tcs = Lists.newArrayList();
        tcs.add(new TransferCapability(VerificationSOPClass,
                VERIFICATION_SOP_TS, TransferCapability.SCP));
        for (final DicomService service : services) {
            for (final String sopClass : service.getSopClasses()) {
                tcs.add(new TransferCapability(sopClass, TSUIDS,
                        TransferCapability.SCP));
            }
        }

        ae.setTransferCapability(tcs.toArray(new TransferCapability[0]));
    }
}
