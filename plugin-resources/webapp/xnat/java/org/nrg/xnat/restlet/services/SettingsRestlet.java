package org.nrg.xnat.restlet.services;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.nrg.framework.exceptions.NrgServiceError;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.utils.SaveItemHelper;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class SettingsRestlet extends SecureResource {

    public SettingsRestlet(Context context, Request request, Response response) throws IOException {
        super(context, request, response);
        setModifiable(true);
        this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        this.getVariants().add(new Variant(MediaType.TEXT_XML));

        _arcSpec = ArcSpecManager.GetInstance();
        _property = (String) getRequest().getAttributes().get("PROPERTY");
        if (!StringUtils.isBlank(_property)) {
            if (_property.equals("initialize")) {
                if (_arcSpec != null && _arcSpec.isComplete()) {
                    throw new RuntimeException("You can't initialize an already initialized system!");
                }
            } else {
                if (_arcSpec == null) {
                    throw new RuntimeException("You haven't yet initialized the system, so I can't return any values!");
                }
                _value = (String) getRequest().getAttributes().get("VALUE");
            }
        } else {
            if (_arcSpec == null) {
                throw new RuntimeException("You haven't yet initialized the system, so I can't return any values!");
            }
        }
        if (request.isEntityAvailable()) {
            convertFormDataToMap(request.getEntity().getText());
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        final MediaType mediaType = overrideVariant(variant);

        try {
            if (StringUtils.isBlank(_property)) {
                return mediaType == MediaType.TEXT_XML ?
                    new ItemXMLRepresentation(_arcSpec.getItem(), mediaType) :
                    new StringRepresentation("{\"ResultSet\":{\"Result\":" + new ObjectMapper().writeValueAsString(getArcSpecAsMap()) + ", \"title\": \"Settings\"}}");
            }

            Object property = _arcSpec.getProperty(_property);
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
        settings.put("enableCsrfToken", _arcSpec.getEnableCsrfToken());

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
        try {
            if (!StringUtils.isBlank(_property) && !_property.equals("initialize")) {
                setDiscreteProperty();
            } else
                // We will only enter this if _property is "initialize", so that means we need to set up the arc spec entry.
                if (!StringUtils.isBlank(_property)) {
                    initializeArcSpec();
                } else {
                    setPropertiesFromFormData();
            }
        } catch (XFTInitException exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        } catch (ElementNotFoundException exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        } catch (FieldNotFoundException exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        } catch (InvalidValueException exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        } catch (Exception exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        }
        returnDefaultRepresentation();
    }

    private void setPropertiesFromFormData() throws Exception {
        _log.debug("Setting arc spec property from body string: " + _form);
        boolean dirtied = false;
        for (String property : _data.keySet()) {
            if (property.equals("siteId")) {
                final String siteId = _data.get("siteId");
                _arcSpec.setSiteId(siteId);
                XFT.SetSiteID(siteId);
                dirtied = true;
            } else if (property.equals("siteUrl")) {
                final String siteUrl = _data.get("siteUrl");
                _arcSpec.setSiteUrl(siteUrl);
                XFT.SetSiteURL(siteUrl);
                dirtied = true;
            } else if (property.equals("siteAdminEmail")) {
                final String siteAdminEmail = _data.get("siteAdminEmail");
                _arcSpec.setSiteAdminEmail(siteAdminEmail);
                XFT.SetAdminEmail(siteAdminEmail);
                dirtied = true;
            } else if (property.equals("smtpHost")) {
                final String smtpHost = _data.get("smtpHost");
                _arcSpec.setSmtpHost(smtpHost);
                XFT.SetAdminEmailHost(smtpHost);
                dirtied = true;
            } else if (property.equals("requireLogin")) {
                final String requireLogin = _data.get("requireLogin");
                _arcSpec.setRequireLogin(requireLogin);
                XFT.SetRequireLogin(requireLogin);
                dirtied = true;
            } else if (property.equals("enableNewRegistrations")) {
                final String enableNewRegistrations = _data.get("enableNewRegistrations");
                _arcSpec.setEnableNewRegistrations(enableNewRegistrations);
                XFT.SetUserRegistration(enableNewRegistrations);
                dirtied = true;
            } else if (property.equals("archivePath")) {
                final String archivePath = _data.get("archivePath");
                _arcSpec.getGlobalpaths().setArchivepath(archivePath);
                XFT.SetArchiveRootPath(archivePath);
                dirtied = true;
            } else if (property.equals("prearchivePath")) {
                final String prearchivePath = _data.get("prearchivePath");
                _arcSpec.getGlobalpaths().setPrearchivepath(prearchivePath);
                XFT.SetPrearchivePath(prearchivePath);
                dirtied = true;
            } else if (property.equals("cachePath")) {
                final String cachePath = _data.get("cachePath");
                _arcSpec.getGlobalpaths().setCachepath(cachePath);
                XFT.SetCachePath(cachePath);
                dirtied = true;
            } else if (property.equals("buildPath")) {
                final String buildPath = _data.get("buildPath");
                _arcSpec.getGlobalpaths().setBuildpath(buildPath);
                XFT.setBuildPath(buildPath);
                dirtied = true;
            } else if (property.equals("ftpPath")) {
                final String ftpPath = _data.get("ftpPath");
                _arcSpec.getGlobalpaths().setFtppath(ftpPath);
                XFT.setFtpPath(ftpPath);
                dirtied = true;
            } else if (property.equals("pipelinePath")) {
                final String pipelinePath = _data.get("pipelinePath");
                _arcSpec.getGlobalpaths().setPipelinepath(pipelinePath);
                XFT.SetPipelinePath(pipelinePath);
                dirtied = true;
            } else if (property.equals("dcmPort")) {
                _arcSpec.setDcm_dcmPort(_data.get("dcmPort"));
                dirtied = true;
            } else if (property.equals("dcmAe")) {
                _arcSpec.setDcm_dcmAe(_data.get("dcmAe"));
                dirtied = true;
            } else if (property.equals("dcmAppletLink")) {
                _arcSpec.setDcm_appletLink(_data.get("dcmAppletLink"));
                dirtied = true;
            } else if (property.equals("enableCsrfToken")) {
                final String enableCsrfToken = _data.get("enableCsrfToken");
                _arcSpec.setEnableCsrfToken(enableCsrfToken);
                XFT.SetEnableCsrfToken(enableCsrfToken);
                dirtied = true;
            } else if (property.equals("enableCsrfToken")) {
                final String enableCsrfToken = _data.get("enableCsrfToken");
                _arcSpec.setEnableCsrfToken(enableCsrfToken);
                XFT.SetEnableCsrfToken(enableCsrfToken);
                dirtied = true;
            }
        }
        if (dirtied) {
            SaveItemHelper.unauthorizedSave(_arcSpec, user, false, false,EventUtils.ADMIN_EVENT(user));
        }
    }

    private void initializeArcSpec() throws Exception {
        PopulateItem populator = new PopulateItem(copyDataToXmlPath(), user, "arc:ArchiveSpecification", true);
        XFTItem item = populator.getItem();
        item.setUser(user);
        ArcArchivespecification arc = new ArcArchivespecification(item);
        SaveItemHelper.unauthorizedSave(arc, user, false, false,EventUtils.ADMIN_EVENT(user));
        ArcSpecManager.Reset();
    }

    private void setDiscreteProperty() throws XFTInitException, ElementNotFoundException, FieldNotFoundException, InvalidValueException {
        _log.debug("Setting arc spec property: [" + _property + "] = " + _value);
        _arcSpec.setProperty(_property, _value);
    }

    private Map<String, String> copyDataToXmlPath() {
        Map<String, String> data = new HashMap<String, String>(_data.size());
        addSpecifiedProperty(data, "arc:archivespecification/site_id", "siteId");
        addSpecifiedProperty(data, "arc:archivespecification/site_url", "siteUrl");
        addSpecifiedProperty(data, "arc:archivespecification/site_admin_email", "siteAdminEmail");
        addSpecifiedProperty(data, "arc:archivespecification/smtp_host", "smtpHost");
        addSpecifiedProperty(data, "arc:archivespecification/globalpaths/archivepath", "archivePath");
        addSpecifiedProperty(data, "arc:archivespecification/globalpaths/prearchivepath", "prearchivePath");
        addSpecifiedProperty(data, "arc:archivespecification/globalpaths/cachepath", "cachePath");
        addSpecifiedProperty(data, "arc:archivespecification/globalpaths/ftppath", "ftpPath");
        addSpecifiedProperty(data, "arc:archivespecification/globalpaths/buildpath", "buildPath");
        addSpecifiedProperty(data, "arc:archivespecification/globalpaths/pipelinepath", "pipelinePath");
        addSpecifiedProperty(data, "arc:archivespecification/require_login", "requireLogin");
        addSpecifiedProperty(data, "arc:archivespecification/enable_new_registrations", "enableNewRegistrations");
        addSpecifiedProperty(data, "arc:archivespecification/dcm/dcm_ae", "dcmAe");
        addSpecifiedProperty(data, "arc:archivespecification/dcm/applet_link", "dcmAppletLink");
        addSpecifiedProperty(data, "arc:archivespecification/dcm/dcm_port", "dcmPort");
        addSpecifiedProperty(data, "arc:archivespecification/enable_csrf_token", "enableCsrfToken");
        return data;
    }

    /**
     * Checks whether the {@link #_data} map contains the specified key and whether the corresponding value is blank.
     * If the key exists and the value is not blank, this method puts the value into the submitted data map using the
     * submitted <b>xmlPath</b> as the key.
     * @param data       The data map to be populated with existing non-blank values.
     * @param xmlPath    The key to use for storage in the data map.
     * @param key        The key to check in the parsed data map.
     */
    private void addSpecifiedProperty(final Map<String, String> data, final String xmlPath, final String key) {
        if (_data.containsKey(key)) {
            String value = _data.get(key);
            if (!StringUtils.isBlank(value)) {
                data.put(xmlPath, value);
            }
        }
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
                try {
                    _data.put(atoms[0], URLDecoder.decode(atoms[1], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // This is the dumbest exception in the history of humanity: the form of this method that doesn't
                    // specify an encoding is deprecated, so you have to specify an encoding. But the form of the method
                    // that takes an encoding (http://bit.ly/yX56fe) has an note that emphasizes that you should only
                    // use UTF-8 because "[n]ot doing so may introduce incompatibilities." Got it? You have to specify
                    // it, but it should always be the same thing. Oh, and BTW? You have to catch an exception for
                    // unsupported encodings because you may specify that one acceptable encoding or... something.
                    //
                    // I hate them.
                }
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
