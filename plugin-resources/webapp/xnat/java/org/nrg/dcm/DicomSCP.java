/**
 * Copyright (c) 2011,2012 Washington University
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.inject.Provider;

import org.dcm4che2.net.Device;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.DicomService;
import org.dcm4che2.net.service.VerificationService;
import org.nrg.dcm.CStoreService.Specifier;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.DicomObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 * 
 */
public class DicomSCP {
    private static final String DEVICE_NAME = "XNAT_DICOM";

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

    public static DicomSCP create(final Executor executor, final int port,
            final CStoreService.Specifier...cStoreSpecs) {
        return create(executor, port, Arrays.asList(cStoreSpecs));
    }

    public static DicomSCP create(final Executor executor, final int port,
            final Iterable<CStoreService.Specifier> cStoreSpecs) {
        final NetworkConnection nc = new NetworkConnection();
        nc.setPort(port);

        final Device device = new Device(DEVICE_NAME);
        device.setNetworkConnection(nc);
        
        final DicomSCP scp = new DicomSCP(executor, device);
        for (final CStoreService.Specifier spec : cStoreSpecs) {
            scp.setCStoreService(spec);
        }
        return scp;
    }
    
    /**
     * Creates a new DICOM C-STORE SCP.
     * @param executor thread provider for server
     * @param port TCP port number
     * @param userProvider provides XNAT user who owns the service
     * @param cstores Map of AE title to DicomObjectIdentifier for C-STORE services
     * @return
     * @throws IOException
     */
    public static DicomSCP create(final Executor executor,
            final int port,
            final Provider<XDATUser> userProvider,
            final Map<String,DicomObjectIdentifier<XnatProjectdata>> cstores)
    throws IOException {
        return create(executor, port, userProvider, cstores, Collections.<String,DicomFileNamer>emptyMap());
    }
    
    /**
     * Creates a new DICOM C-STORE SCP.
     * @param executor thread provider for server
     * @param port TCP port number
     * @param userProvider provides XNAT user who owns the service
     * @param cstores Map of AE title to DicomObjectIdentifier for C-STORE services
     * @param namers Map of AE title to DicomFileNamer assigning name policy
     *                (if no entry for an AE title, uses default policy)
     * @return
     * @throws IOException
     */
    public static DicomSCP create(final Executor executor,
            final int port,
            final Provider<XDATUser> userProvider,
            final Map<String,DicomObjectIdentifier<XnatProjectdata>> cstores,
            final Map<String,DicomFileNamer> namers)
    throws IOException {
        final Logger logger = LoggerFactory.getLogger(DicomSCP.class);
        final List<CStoreService.Specifier> specs = Lists.newArrayList();
        for (final Map.Entry<String,DicomObjectIdentifier<XnatProjectdata>> me : cstores.entrySet()) {
            logger.trace("preparing C-STORE service specifier for {}", me);
            final String aeTitle = me.getKey();
            final DicomFileNamer namer = namers.get(aeTitle);
            final Specifier specifier = new Specifier(aeTitle, userProvider, me.getValue(), namer);
            specs.add(specifier);
        }
        return create(executor, port, specs);
    }

    /**
     * Creates a new DICOM C-STORE SCP using the default name policy.
     * @param executor thread provider for server
     * @param port TCP port number
     * @param userProvider provides XNAT user who owns the service
     * @param aeTitle application entity title
     * @param identifier DICOM object identifier
     * @return
     * @throws IOException
     */
    public static DicomSCP create(final Executor executor, final int port,
            final Provider<XDATUser> userProvider,
            final String aeTitle,
            final DicomObjectIdentifier<XnatProjectdata> identifier)
                    throws IOException {
        return create(executor, port, userProvider, aeTitle, identifier, null);        
    }
                   

    /**
     * Creates a new DICOM C-STORE SCP. This can be a little easier to use
     * than the constructor because it wraps creating some of the DICOM
     * networking infrastructure
     * @param executor thread provider for server
     * @param port TCP port number
     * @param userProvider provides XNAT user who owns the service
     * @param aeTitle application entity title
     * @param identifier DICOM object identifier
     * @param namer name policy implementation
     * @return
     * @throws IOException
     */
    public static DicomSCP create(final Executor executor, final int port,
            final Provider<XDATUser> userProvider,
            final String aeTitle,
            DicomObjectIdentifier<XnatProjectdata> identifier,
            final DicomFileNamer namer)
    throws IOException {
        return create(executor, port, new Specifier(aeTitle, userProvider, identifier, namer));
    }
    

    private final Logger logger = LoggerFactory.getLogger(DicomSCP.class);

    private final Executor executor;
    
    private final Device device;
    
    private final Multimap<NetworkApplicationEntity,DicomService> aes = LinkedHashMultimap.create();
    
    private boolean started;
    

    public DicomSCP(final Executor executor, final Device device) {
        this.executor = executor;
        this.device = device;
        setStarted(false);
    }
    
    public Iterable<String> getAEs() {
        return Iterables.transform(aes.keySet(),
                new Function<NetworkApplicationEntity,String>() {
            public String apply(final NetworkApplicationEntity ae) {
                return ae.getAETitle();
            }
        });
    }
    
    public int getPort() {
        return device.getNetworkConnection()[0].getPort();
    }

    public boolean isStarted() {
		return started;
	}


    public DicomSCP setCStoreService(final CStoreService.Specifier spec) {
        return setService(spec.getAETitle(), spec.build());
    }
    
    public DicomSCP setHostname(final String hostname) {
        return setHostname(hostname, null);
    }

    /**
     * Set the hostname for the SCP. This may be used to
     * distinguish among multiple network interfaces.
     * @param hostname
     * @return this
     */
    public DicomSCP setHostname(final String hostname, final String aeTitle) {
        // TODO: check state. is this possible?
        for (final NetworkApplicationEntity ae : aes.keySet()) {
            if (null == aeTitle || aeTitle.equals(ae.getAETitle())) {
                ae.getNetworkConnection()[0].setHostname(hostname);
            }
        }
        return this;
    }


    public DicomSCP setService(final String aeTitle, final DicomService service) {
        if (Strings.isNullOrEmpty(aeTitle)) {
            throw new IllegalArgumentException("can only add service to named AE");
        }
        NetworkApplicationEntity ae = null;
        for (final NetworkApplicationEntity iae : aes.keySet()) {
            if (aeTitle.equals(iae.getAETitle())) {
                ae = iae;
                break;
            }
        }
        if (null == ae) {
            ae = new NetworkApplicationEntity();
            ae.setNetworkConnection(device.getNetworkConnection());
            ae.setAssociationAcceptor(true);
            ae.setAETitle(aeTitle);
        }
        aes.put(ae, service);
        return this;
    }
    
    private void setStarted(boolean started) {
		this.started = started;
	}

	public void start() throws IOException {
    	if(!isStarted()) {
	        logger.info("starting DICOM SCP on {}:{}",
	                new Object[] {
	                device.getNetworkConnection()[0].getHostname(),
	                device.getNetworkConnection()[0].getPort(),
	        });
	        if (logger.isDebugEnabled()) {
	            logger.debug("Application Entities: ");
	            for (final NetworkApplicationEntity ae : aes.keySet()) {
	                logger.debug("{}: {}", ae.getAETitle(), aes.get(ae));
	            }
	        }
	
	        final VerificationService cecho = new VerificationService();
	
	        for (final NetworkApplicationEntity ae : aes.keySet()) {
	            logger.trace("Setting up AE {}", ae.getAETitle());
	            final List<TransferCapability> tcs = Lists.newArrayList();
	            ae.register(cecho);
	            tcs.add(new TransferCapability(VerificationSOPClass,
	                    VERIFICATION_SOP_TS, TransferCapability.SCP));
	            for (final DicomService service : aes.get(ae)) {
	                logger.trace("adding {}", service);
	                ae.register(service);
	                for (final String sopClass : service.getSopClasses()) {
	                    tcs.add(new TransferCapability(sopClass, TSUIDS,
	                            TransferCapability.SCP));
	                }
	            }
	
	            ae.setTransferCapability(tcs.toArray(new TransferCapability[0]));
	        }
	        device.setNetworkApplicationEntity(aes.keySet().toArray(new NetworkApplicationEntity[0]));
	        device.startListening(executor);
	        setStarted(true);
    	}
    }

	public void stop() {
    	if(isStarted()) {
	        logger.info("stopping DICOM SCP");
	        device.stopListening();
	        for (final NetworkApplicationEntity ae : aes.keySet()) {
	            for (final DicomService service : aes.get(ae)) {
	                ae.unregister(service);
	            }
	            ae.setTransferCapability(new TransferCapability[0]);
	        }
	        setStarted(false);
    	}
    }
}
