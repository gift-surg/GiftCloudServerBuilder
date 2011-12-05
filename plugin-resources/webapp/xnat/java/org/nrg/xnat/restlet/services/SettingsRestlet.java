package org.nrg.xnat.restlet.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.nrg.framework.exceptions.NrgServiceError;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xnat.restlet.representations.ItemXMLRepresentation;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class SettingsRestlet extends SecureResource {

    public SettingsRestlet(Context context, Request request, Response response) throws IOException {
        super(context, request, response);
        setModifiable(true);
        this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        this.getVariants().add(new Variant(MediaType.TEXT_XML));

        _arcSpec = ArcSpecManager.GetInstance();
        _property = (String) getRequest().getAttributes().get("PROPERTY");
        if (!StringUtils.isBlank(_property)) {
            _value = (String) getRequest().getAttributes().get("VALUE");
        }
        if (request.isEntityAvailable()) {
            convertFormDataToMap(request.getEntity().getText());
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        final MediaType mediaType = overrideVariant(variant);
        ArcArchivespecification arcSpec = ArcSpecManager.GetInstance();

        try {
            if (StringUtils.isBlank(_property)) {
                return mediaType == MediaType.TEXT_XML ?
                    new ItemXMLRepresentation(arcSpec.getItem(), mediaType) :
                    new StringRepresentation("{\"ResultSet\":{\"Result\":" + new ObjectMapper().writeValueAsString(getArcSpecAsMap()) + ", \"title\": \"Settings\"}}");
            }

            Object property = arcSpec.getProperty(_property);
            if (mediaType == MediaType.TEXT_XML) {
                String xml = "<" + _property + ">" + property.toString() + "</" + _property + ">";
                return new StringRepresentation(xml, mediaType);
            } else {
                return new StringRepresentation("{\"ResultSet\":{\"Result\":" + new ObjectMapper().writeValueAsString(property) + ", \"title\": \"" + _property + "\"}}");
            }
        } catch (ElementNotFoundException exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        } catch (FieldNotFoundException exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        } catch (JsonGenerationException exception) {
            throw new NrgServiceRuntimeException(NrgServiceError.Unknown, "Something went wrong converting filters to JSON.", exception);
        } catch (JsonMappingException exception) {
            throw new NrgServiceRuntimeException(NrgServiceError.Unknown, "Something went wrong converting filters to JSON.", exception);
        } catch (IOException exception) {
            throw new NrgServiceRuntimeException(NrgServiceError.Unknown, "Something went wrong converting filters to JSON.", exception);
        }
        return null;
    }

    private Map<String, Object> getArcSpecAsMap() {
        Map<String, Object> settings = new HashMap<String, Object>();

        settings.put("siteId", _arcSpec.getSiteId());
        settings.put("siteUrl", _arcSpec.getSiteUrl());
        settings.put("siteAdminEmail", _arcSpec.getSiteAdminEmail());
        settings.put("smtpHost", _arcSpec.getSmtpHost());
        settings.put("requireLogin", _arcSpec.getRequireLogin());
        settings.put("enableNewRegistrations", _arcSpec.getEnableNewRegistrations());
        settings.put("archivePath", _arcSpec.getGlobalArchivePath());
        settings.put("prearchivePath", _arcSpec.getGlobalPrearchivePath());
        settings.put("cachePath", _arcSpec.getGlobalCachePath());
        settings.put("buildPath", _arcSpec.getGlobalBuildPath());
        settings.put("ftpPath", _arcSpec.getGlobalpaths().getFtppath());
        settings.put("pipelinePath", _arcSpec.getGlobalpaths().getPipelinepath());
        settings.put("dcmPort", _arcSpec.getDcm_dcmPort());
        settings.put("dcmAe", _arcSpec.getDcm_dcmAe());
        settings.put("dcmAppletLink", _arcSpec.getDcm_appletLink());

        return settings;
    }

    @Override
    public void handlePost() {
        setProperties();
    }

    @Override
    public void handlePut() {
        setProperties();
    }

    @Override
    public void handleDelete() {
        _log.debug("Got a request to delete property with ID: " + _property);
        returnDefaultRepresentation();
    }

    private void setProperties() {
        ArcArchivespecification arcSpec = ArcSpecManager.GetInstance();
        try {
            if (!StringUtils.isBlank(_property)) {
                _log.debug("Setting arc spec property: [" + _property + "] = " + _value);
                arcSpec.setProperty(_property, _value);
            } else {
                _log.debug("Setting arc spec property from body string: " + _form);
                for (String property : _data.keySet()) {
                    if (property.equals("siteId")) {
                        _arcSpec.setSiteId(_data.get("siteId"));
                    } else if (property.equals("siteUrl")) {
                        _arcSpec.setSiteUrl(_data.get("siteUrl"));
                    } else if (property.equals("siteAdminEmail")) {
                        _arcSpec.setSiteAdminEmail(_data.get("siteAdminEmail"));
                    } else if (property.equals("smtpHost")) {
                        _arcSpec.setSmtpHost(_data.get("smtpHost"));
                    } else if (property.equals("requireLogin")) {
                        _arcSpec.setRequireLogin(_data.get("requireLogin"));
                    } else if (property.equals("enableNewRegistrations")) {
                        _arcSpec.setEnableNewRegistrations(_data.get("enableNewRegistrations"));
                    } else if (property.equals("archivePath")) {
                        _arcSpec.getGlobalpaths().setArchivepath(_data.get("archivePath"));
                    } else if (property.equals("prearchivePath")) {
                        _arcSpec.getGlobalpaths().setPrearchivepath(_data.get("prearchivePath"));
                    } else if (property.equals("cachePath")) {
                        _arcSpec.getGlobalpaths().setCachepath(_data.get("cachePath"));
                    } else if (property.equals("buildPath")) {
                        _arcSpec.getGlobalpaths().setBuildpath(_data.get("buildPath"));
                    } else if (property.equals("ftpPath")) {
                        _arcSpec.getGlobalpaths().setFtppath(_data.get("ftpPath"));
                    } else if (property.equals("pipelinePath")) {
                        _arcSpec.getGlobalpaths().setPipelinepath(_data.get("pipelinePath"));
                    } else if (property.equals("dcmPort")) {
                        _arcSpec.setDcm_dcmPort(_data.get("dcmPort"));
                    } else if (property.equals("dcmAe")) {
                        _arcSpec.setDcm_dcmAe(_data.get("dcmAe"));
                    } else if (property.equals("dcmAppletLink")) {
                        _arcSpec.setDcm_appletLink(_data.get("dcmAppletLink"));
                    }
                }
            }
        } catch (XFTInitException exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        } catch (ElementNotFoundException exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        } catch (FieldNotFoundException exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        } catch (InvalidValueException exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        }
        returnDefaultRepresentation();
    }

    private void convertFormDataToMap(String text) {
        _form = text;
        _data = new HashMap<String, String>();
        String[] entries = text.split("&");
        for (String entry : entries) {
            String[] atoms = entry.split("=", 2);
            if (atoms == null || atoms.length == 0) {
                // TODO: Just ignoring for now, should we do something here?
            } else if (atoms.length == 1) {
                _data.put(atoms[0], "");
            } else {
                _data.put(atoms[0], atoms[1]);
            }
        }
    }

    private static final Log _log = LogFactory.getLog(SettingsRestlet.class);
    private ArcArchivespecification _arcSpec;
    private String _property;
    private String _value;
    private String _form;
    private Map<String, String> _data;
}
