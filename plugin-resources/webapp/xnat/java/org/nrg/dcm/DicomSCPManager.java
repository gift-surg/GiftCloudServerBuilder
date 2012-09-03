package org.nrg.dcm;

import java.io.IOException;

import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.xdat.XDAT;

public class DicomSCPManager {
	
	private DicomSCP dicomSCP;
	
	@SuppressWarnings("unused")
	private DicomSCPManager() {}
	
	public DicomSCPManager(DicomSCP dicomSCP) {
		this.dicomSCP = dicomSCP;
	}

	public void startOrStopDicomSCPAsDictatedByConfiguration() {
		
        try {
        	Boolean enableDicomReceiver = Boolean.valueOf(XDAT.getSiteConfigurationProperty("enableDicomReceiver"));
        	if(enableDicomReceiver) {
                dicomSCP.start();
        	}
        	else {
        		dicomSCP.stop();
        	}
        } catch (ConfigServiceException e) {
            throw new RuntimeException("unable to lookup enableDicomReceiver property from config service", e);
        } catch (IOException e) {
            throw new RuntimeException("unable to start DICOM SCP", e);
        }
    }
	
	public void startDicomSCP() {
		try {
			dicomSCP.start();
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void stopDicomSCP() {
		dicomSCP.stop();
	}
	
	public boolean isDicomSCPStarted() {
		return dicomSCP.isStarted();
	}
	
	public int getDicomSCPPort() {
		return dicomSCP.getPort();
	}

	public Iterable<String> getDicomSCPAEs() {
		return dicomSCP.getAEs();
	}
}
