package org.nrg.xnat.restlet.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.PropertyNotFoundException;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.framework.exceptions.NrgServiceError;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.mail.api.NotificationType;
import org.nrg.notify.api.CategoryScope;
import org.nrg.notify.api.SubscriberType;
import org.nrg.notify.entities.Category;
import org.nrg.notify.entities.Channel;
import org.nrg.notify.entities.Definition;
import org.nrg.notify.entities.Subscriber;
import org.nrg.notify.entities.Subscription;
import org.nrg.notify.exceptions.DuplicateSubscriberException;
import org.nrg.notify.services.NotificationService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.model.ArcArchivespecificationNotificationTypeI;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.om.ArcArchivespecificationNotificationType;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.restlet.representations.ItemXMLRepresentation;
import org.nrg.xnat.restlet.resources.RestMockCallMapRestlet;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.security.FilterSecurityInterceptorBeanPostProcessor;
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
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

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
            else {
            	Object propertyValue = checkForExistenceOfProperty(_property);
            	
                if (mediaType == MediaType.TEXT_XML) {
                    String xml = "<" + _property + ">" + propertyValue.toString() + "</" + _property + ">";
                    return new StringRepresentation(xml, mediaType);
                }
                else {
                    return new StringRepresentation("{\"ResultSet\":{\"Result\":" + new ObjectMapper().writeValueAsString(propertyValue) + ", \"title\": \"" + _property + "\"}}");
               	}
            }
        } catch (PropertyNotFoundException exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
        } catch (JsonGenerationException exception) {
            throw new NrgServiceRuntimeException(NrgServiceError.Unknown, "Something went wrong converting filters to JSON.", exception);
        } catch (JsonMappingException exception) {
            throw new NrgServiceRuntimeException(NrgServiceError.Unknown, "Something went wrong converting filters to JSON.", exception);
        } catch (IOException exception) {
            throw new NrgServiceRuntimeException(NrgServiceError.Unknown, "Something went wrong converting filters to JSON.", exception);
        } catch (ConfigServiceException exception) {
            throw new NrgServiceRuntimeException(NrgServiceError.Unknown, "Something went wrong retrieving site properties.", exception);
        }
        return null;
    }

    private Map<Object, Object> getArcSpecAsMap() throws IOException, ConfigServiceException {
    	Map<Object, Object> settings = new HashMap<Object, Object>();

        settings.putAll(XDAT.getSiteConfiguration());
        settings.put("siteId", _arcSpec.getSiteId());
        settings.put("siteUrl", _arcSpec.getSiteUrl());
        settings.put("siteAdminEmail", _arcSpec.getSiteAdminEmail());
        settings.put("smtpHost", _arcSpec.getSmtpHost());
        settings.put("requireLogin", _arcSpec.getRequireLogin());
        settings.put("enableNewRegistrations", _arcSpec.getEnableNewRegistrations());
        settings.put("emailVerification", XDAT.getSiteConfigurationProperty("emailVerification"));
        settings.put("archivePath", _arcSpec.getGlobalArchivePath());
        settings.put("prearchivePath", _arcSpec.getGlobalPrearchivePath());
        settings.put("cachePath", _arcSpec.getGlobalCachePath());
        settings.put("buildPath", _arcSpec.getGlobalBuildPath());
        settings.put("ftpPath", _arcSpec.getGlobalpaths().getFtppath());
        settings.put("pipelinePath", _arcSpec.getGlobalpaths().getPipelinepath());
        settings.put("dcmPort", _arcSpec.getDcm_dcmPort());
        settings.put("dcmAe", _arcSpec.getDcm_dcmAe());
        settings.put("enableCsrfToken", _arcSpec.getEnableCsrfToken());
        settings.put("error", getSubscribersForEvent(NotificationType.Error));
        settings.put("issue", getSubscribersForEvent(NotificationType.Issue));
        settings.put("newUser", getSubscribersForEvent(NotificationType.NewUser));
        settings.put("update", getSubscribersForEvent(NotificationType.Update));
        settings.put("anonScript", XDAT.getConfigService().getConfigContents("anon", "script"));
        settings.put("appletScript", XDAT.getConfigService().getConfigContents("applet", "settings"));
        settings.put("restMockCallMap", getFormattedRestMockCallMap());

        return settings;
    }

    private String getFormattedRestMockCallMap() {
        Map<String, String> map = RestMockCallMapRestlet.getRestMockCallMap();
        if (map == null || map.size() == 0) {
            return StringUtils.EMPTY;
        }
        StringBuilder formattedMap = new StringBuilder();
        for (Map.Entry<String, String> call : map.entrySet()) {
            formattedMap.append(call.getKey()).append("|").append(call.getValue()).append("\n");
        }
        return formattedMap.toString();
    }

    private String getUnformattedRestMockCallMap(String raw) {
        if (StringUtils.isBlank(raw)) {
            return StringUtils.EMPTY;
        }
        BufferedReader reader = new BufferedReader(new StringReader(raw));
        Map<String, String> map = new HashMap<String, String>();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] atoms = line.split("\\|");
                map.put(atoms[0], atoms[1]);
            }

            return MAPPER.writeValueAsString(map);
        } catch (IOException ignored) {
            // We're not reading from a file, so we shouldn't encounter this.
        }

        return StringUtils.EMPTY;
    }

    // TODO: Gross.
    public static final String ADMIN_USERNAME_FOR_SUBSCRIPTION = "admin";

    /**
     * This returns the current subscriber or subscribers to a particular <i>site-wide</i> event. If the event doesn't
     * already exist, the event will be created with the default user set to the site administrator's email address.
     * @param event    The event to be created or retrieved.
     */
    private String getSubscribersForEvent(NotificationType event) {
        Category category = getNotificationService().getCategoryService().getCategoryByScopeAndEvent(CategoryScope.Site, event.toString());
        if (category == null) {
            category = getNotificationService().getCategoryService().newEntity();
            category.setScope(CategoryScope.Site);
            category.setEvent(event.toString());
            getNotificationService().getCategoryService().create(category);
        }
        Definition definition;
        List<Definition> definitions = getNotificationService().getDefinitionService().getDefinitionsForCategory(category);
        if (definitions == null || definitions.size() == 0) {
            definition = getNotificationService().getDefinitionService().newEntity();
            definition.setCategory(category);
            getNotificationService().getDefinitionService().create(definition);
        } else {
            definition = definitions.get(0);
        }

        Map<Subscriber, Subscription> subscriptions = getNotificationService().getSubscriptionService().getSubscriberMapOfSubscriptionsForDefinition(definition);
        if (subscriptions != null && subscriptions.size() > 0) {
            return createCommaSeparatedList(subscriptions.keySet());
        } else {
            Subscriber adminUser = getNotificationService().getSubscriberService().getSubscriberByName(ADMIN_USERNAME_FOR_SUBSCRIPTION);
            if (adminUser == null) {
                try {
                    adminUser = getNotificationService().getSubscriberService().createSubscriber(ADMIN_USERNAME_FOR_SUBSCRIPTION, _arcSpec.getSiteAdminEmail());
                } catch (DuplicateSubscriberException exception) {
                    // This shouldn't happen, since we just checked for the subscriber's existence.
                }
            }

            getNotificationService().subscribe(adminUser, SubscriberType.User, definition, XDAT.getHtmlMailChannel());
            assert adminUser != null;
            return adminUser.getEmails();
        }
    }

    private String createCommaSeparatedList(final Set<Subscriber> subscribers) {
        if (subscribers == null || subscribers.size() == 0) {
            return "";
        }
        boolean isFirst = true;
        StringBuilder buffer = new StringBuilder();
        for (Subscriber subscriber : subscribers) {
            if (isFirst) {
                isFirst = false;
            } else {
                buffer.append(", ");
            }
            buffer.append(subscriber.getEmails());
        }
        return buffer.toString();
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
                checkNotifications();
                returnDefaultRepresentation();
            } 
            else if (!StringUtils.isBlank(_property)) {
                // We will only enter this if _property is "initialize", so that means we need to set up the arc spec entry.
                initializeArcSpec();
                checkNotifications();
                setPropertiesFromMap(_data);
                // Do not return a representation if we are initializing, it will attempt (and fail) to find a property called "initialize"
            } else {
                setPropertiesFromMap(_data);
                returnDefaultRepresentation();
            }
        } catch (Exception exception) {
            respondToException(exception, Status.CLIENT_ERROR_BAD_REQUEST);
	    } catch (ConfigServiceException exception) {
	        throw new NrgServiceRuntimeException(NrgServiceError.Unknown, "Something went wrong retrieving site properties.", exception);
	    }
    }

    private void setPropertiesFromMap(Map<String, String> map) throws Exception {
        _log.debug("Setting arc spec property from body string: " + _form);
        boolean dirtied = false;
        boolean dirtiedNotifications = false;
        for (String property : map.keySet()) {
            if (property.equals("siteId")) {
                final String siteId = map.get("siteId");
                _arcSpec.setSiteId(siteId);
                XFT.SetSiteID(siteId);
                dirtied = true;
            } else if (property.equals("siteUrl")) {
                final String siteUrl = map.get("siteUrl");
                _arcSpec.setSiteUrl(siteUrl);
                XFT.SetSiteURL(siteUrl);
                dirtied = true;
            } else if (property.equals("siteAdminEmail")) {
                final String siteAdminEmail = map.get("siteAdminEmail");
                _arcSpec.setSiteAdminEmail(siteAdminEmail);
                XFT.SetAdminEmail(siteAdminEmail);
                dirtied = true;
            } else if (property.equals("smtpHost")) {
                final String smtpHost = map.get("smtpHost");
                _arcSpec.setSmtpHost(smtpHost);
                XFT.SetAdminEmailHost(smtpHost);
                dirtied = true;
            } else if (property.equals("requireLogin")) {
                final String requireLogin = map.get("requireLogin");
                _arcSpec.setRequireLogin(requireLogin);
                XFT.SetRequireLogin(requireLogin);
                updateSecurityFilter(Boolean.parseBoolean(requireLogin));
                dirtied = true;
            } else if (property.equals("enableNewRegistrations")) {
                final String enableNewRegistrations = map.get("enableNewRegistrations");
                _arcSpec.setEnableNewRegistrations(enableNewRegistrations);
                XFT.SetUserRegistration(enableNewRegistrations);
                dirtied = true;
            } else if (property.equals("archivePath")) {
                final String archivePath = map.get("archivePath");
                _arcSpec.getGlobalpaths().setArchivepath(archivePath);
                XFT.SetArchiveRootPath(archivePath);
                dirtied = true;
            } else if (property.equals("prearchivePath")) {
                final String prearchivePath = map.get("prearchivePath");
                _arcSpec.getGlobalpaths().setPrearchivepath(prearchivePath);
                XFT.SetPrearchivePath(prearchivePath);
                dirtied = true;
            } else if (property.equals("cachePath")) {
                final String cachePath = map.get("cachePath");
                _arcSpec.getGlobalpaths().setCachepath(cachePath);
                XFT.SetCachePath(cachePath);
                dirtied = true;
            } else if (property.equals("buildPath")) {
                final String buildPath = map.get("buildPath");
                _arcSpec.getGlobalpaths().setBuildpath(buildPath);
                XFT.setBuildPath(buildPath);
                dirtied = true;
            } else if (property.equals("ftpPath")) {
                final String ftpPath = map.get("ftpPath");
                _arcSpec.getGlobalpaths().setFtppath(ftpPath);
                XFT.setFtpPath(ftpPath);
                dirtied = true;
            } else if (property.equals("pipelinePath")) {
                final String pipelinePath = map.get("pipelinePath");
                _arcSpec.getGlobalpaths().setPipelinepath(pipelinePath);
                XFT.SetPipelinePath(pipelinePath);
                dirtied = true;
            } else if (property.equals("dcmPort")) {
                _arcSpec.setDcm_dcmPort(map.get("dcmPort"));
                dirtied = true;
            } else if (property.equals("dcmAe")) {
                _arcSpec.setDcm_dcmAe(map.get("dcmAe"));
                dirtied = true;
            } else if (property.equals("enableCsrfToken")) {
                final String enableCsrfToken = map.get("enableCsrfToken");
                _arcSpec.setEnableCsrfToken(enableCsrfToken);
                XFT.SetEnableCsrfToken(enableCsrfToken);
                dirtied = true;
            } else if (property.equals("error") || property.equals("issue") || property.equals("newUser") || property.equals("update")) {
                if (!dirtiedNotifications) {
                    dirtiedNotifications = true;
                    clearArcSpecNotifications();
                }
                final String userIds = map.get(property);
                configureEventSubscriptions(NotificationType.valueOf(StringUtils.capitalize(property)), userIds);
            } else if (property.equals("anonScript")) {
                final String anonScript = map.get("anonScript");
                try {
                    XDAT.getConfigService().replaceConfig(user.getUsername(), "Updating the site-wide anonymization script", "anon", "script", anonScript);
                } catch (ConfigServiceException exception) {
                    throw new Exception("Error setting the site-wide anonymization script", exception);
                }
                dirtied = true;
            } else if (property.equals("appletScript")) {
                final String appletScript = map.get("appletScript");
                try {
                    XDAT.getConfigService().replaceConfig(user.getUsername(), "Updating the site-wide applet settings", "applet", "settings", appletScript);
                } catch (ConfigServiceException exception) {
                    throw new Exception("Error setting the site-wide applet settings", exception);
                }
                dirtied = true;
            }  else if (property.equals("restMockCallMap")) {
                final String callMap = map.get("restMockCallMap");
                try {
                    XDAT.getConfigService().replaceConfig(user.getUsername(), "Updating the REST service mock call map", "rest", "mockCallMap", getUnformattedRestMockCallMap(callMap));
                } catch (ConfigServiceException exception) {
                    throw new Exception("Error setting the REST service mock call map", exception);
                }
                dirtied = true;
            } else if (property.equals("emailVerification")) {
                final String emailVerification = map.get("emailVerification");
                try {
                    XDAT.setSiteConfigurationProperty("emailVerification", emailVerification);
                } catch (ConfigServiceException exception) {
                    throw new Exception("Error setting the emailVerification site info property", exception);
                }
                dirtied = true;
            } else {
                try {
	                XDAT.setSiteConfigurationProperty(property, map.get(property));
	            } 
                catch (ConfigServiceException exception) {
	                throw new Exception(String.format("Error setting the '%s' site info property", property), exception);
	            }
                dirtied = true;
            }
        }
        if (dirtied || dirtiedNotifications) {            
        SaveItemHelper.unauthorizedSave(_arcSpec, user, false, false,this.newEventInstance(EventUtils.CATEGORY.SIDE_ADMIN, "Modified archive specification"));

        }
    }

    private void updateSecurityFilter(final boolean requireLogin) {
        FilterSecurityInterceptor interceptor = XDAT.getContextService().getBean(FilterSecurityInterceptor.class);
        FilterSecurityInterceptorBeanPostProcessor postProcessor = XDAT.getContextService().getBean(FilterSecurityInterceptorBeanPostProcessor.class);
        assert interceptor != null;
        assert postProcessor != null;

        interceptor.setSecurityMetadataSource(postProcessor.getMetadataSource(requireLogin));
    }

    /**
     * Sets up the subscriptions for the indicated events.
     * @param notificationType    The type of notification for which to configure subscriptions.
     * @param userIds             The IDs of the users to be subscribed to the indicated notification.
     */
    private void configureEventSubscriptions(final NotificationType notificationType, final String userIds) throws Exception {
        Definition definition = retrieveSiteEventDefinition(notificationType);
        List<Subscriber> subscribers = getSubscribersFromAddresses(userIds);
        Map<Subscriber, Subscription> subscriptions = getNotificationService().getSubscriptionService().getSubscriberMapOfSubscriptionsForDefinition(definition);
        Channel channel = XDAT.getHtmlMailChannel();

        for (Subscriber subscriber : subscribers) {
            // If we don't have a subscription for this notification...
            if (!subscriptions.containsKey(subscriber)) {
                // Create one.
                getNotificationService().subscribe(subscriber, SubscriberType.User, definition, channel);
                // But if we do have a subscription for this notification...
            } else {
                // Remove it from the map.
                subscriptions.remove(subscriber);
            }
        }

        // If there are any left-over subscriptions...
        if (subscriptions.size() > 0) {
            // Those are no longer wanted (they weren't specified in the submitted list), so let's remove those subscriptions.
            for (Subscription subscription : subscriptions.values()) {
                getNotificationService().getSubscriptionService().delete(subscription);
            }
        }

        configureArcSpecNotificationType(_arcSpec, notificationType, subscriptions.values());
    }

    /**
     * @param type The type for which the definition and its associated category should be created or retrieved.
     * @return The existing or newly created definition.
     */
    private Definition retrieveSiteEventDefinition(NotificationType type) {
        Category category = getNotificationService().getCategoryService().getCategoryByScopeAndEvent(CategoryScope.Site, type.toString());
        if (category == null) {
            category = initializeSiteEventCategory(type.toString());
        }
        List<Definition> definitions = getNotificationService().getDefinitionService().getDefinitionsForCategory(category);
        Definition definition;
        if (definitions == null || definitions.size() == 0) {
            definition = initializeSiteEventDefinition(category);
        } else {
            definition = definitions.get(0);
        }
        return definition;
    }

    /**
     * @param event The event for which a category should be created.
     * @return The newly created category object.
     */
    private Category initializeSiteEventCategory(String event) {
        Category category = getNotificationService().getCategoryService().newEntity();
        category.setScope(CategoryScope.Site);
        category.setEvent(event);
        getNotificationService().getCategoryService().create(category);
        return category;
    }

    /**
     * @param category The category for which a definition should be created.
     * @return The newly created definition object.
     */
    private Definition initializeSiteEventDefinition(Category category) {
        Definition definition = getNotificationService().getDefinitionService().newEntity();
        definition.setCategory(category);
        getNotificationService().getDefinitionService().create(definition);
        return definition;
    }

    /**
     * Takes a comma-separated list of "email addresses" (which actually may include {@link XDATUser#getEmail() email addresses},
     * {@link XDATUser#getLogin() XDAT user names}, and a combination of the two in the format:
     * <p/>
     * <code><i>username</i> &lt;<i>email</i>&gt;</code>
     * <p/>
     * So for example, you may have something like this:
     * <p/>
     * <code>user1 &lt;user1@@xnat.org&gt;, user2, user3@xnat.org</code>
     * <p/>
     * Note that if any of the users aren't found, this method currently will have no indication other than returning fewer
     * users than are specified in the <b>emailAddresses</b> parameter.
     *
     * @param addressList The comma-separated list of usernames, email addressList, and combined IDs.
     * @return A list of {@link Subscriber} objects representing those users.
     */
    private List<Subscriber> getSubscribersFromAddresses(String addressList) throws Exception {
        final String[] addresses = addressList.split("[\\s]*,[\\s]*");
        if (addresses == null || addresses.length == 0) {
            throw new Exception("Submitted text couldn't be parsed into a list of addresses: " + addressList);
        }

        List<Subscriber> subscribers = new ArrayList<Subscriber>();
        List<String> badAddresses = new ArrayList<String>();
        for (String address : addresses) {
            String username = null;
            String email = null;
            if (PATTERN_USERNAME.matcher(address).matches()) {
                // Handle this as a username.
                XdatUser user = XDATUser.getXdatUsersByLogin(username, null, true);
                if (user != null) {
                    username = address;
                    email = user.getEmail();
                }
            } else if (PATTERN_EMAIL.matcher(address).matches()) {
                // Handle this as an email.
                List<XdatUser> users = XDATUser.getXdatUsersByField("xdat:user/email", address, null, true);
                if (users != null && users.size() > 0) {
                    username = users.get(0).getLogin();
                    email = address;
                }
            } else {
                Matcher combinedMatcher = PATTERN_COMBINED.matcher(address);
                if (combinedMatcher.matches()) {
                    // Handle this as a combined. username will match first capture, email second capture (0 capture in regex is full expression).
                    username = combinedMatcher.group(1);
                    email = combinedMatcher.group(2);
                }
            }

            // If there's no username, this is a bad address, but we'll continue so we can harvest ALL the bad addresses.
            if (username == null) {
                badAddresses.add(address);
                continue;
            }

            // If we don't have any bad addresses, get the subscriber. If we do have bad addresses, we have a valid user
            // but we'll skip getting the subscriber to save ourselves the work.
            if (badAddresses.size() == 0) {
                Subscriber subscriber = getNotificationService().getSubscriberService().getSubscriberByName(username);
                if (subscriber == null) {
                    try {
                        subscriber = getNotificationService().getSubscriberService().createSubscriber(username, email);
                    } catch (DuplicateSubscriberException exception) {
                        // This shouldn't happen, since we just checked for the subscriber's existence.
                    }
                }

                subscribers.add(subscriber);
            }
        }

        if (badAddresses.size() > 0) {
            throw new Exception(getBadAddressErrorMessage(badAddresses));
        }

        return subscribers;
    }

    private String getBadAddressErrorMessage(final List<String> badAddresses) {
        StringBuilder buffer = new StringBuilder("<p>The following addresses were not valid usernames or emails of users of this XNAT server:</p><ul>");
        for (String address : badAddresses) {
            buffer.append("<li>").append(address).append("</li>");
        }
        buffer.append("</ul>");
        return buffer.toString();
    }

    private void initializeArcSpec() throws Exception {
        PopulateItem populator = new PopulateItem(copyDataToXmlPath(), user, "arc:ArchiveSpecification", true);
        XFTItem item = populator.getItem();
        item.setUser(user);
        ArcArchivespecification arc = new ArcArchivespecification(item);
        SaveItemHelper.unauthorizedSave(arc, user, false, false,this.newEventInstance(EventUtils.CATEGORY.SIDE_ADMIN, "Initialized archive specification"));
        ArcSpecManager.Reset();
    }

    private void setDiscreteProperty() throws Exception, ConfigServiceException {
        _log.debug("Setting arc spec property: [" + _property + "] = " + _value);
    	checkForExistenceOfProperty(_property);
        if(null == _value) {
            throw new NullPointerException(String.format("Setting '%s' cannot be set to NULL.", _property));
        }
        else {
            Map<String, String> map = new HashMap<String, String>(1);
            map.put(_property, _value);
            setPropertiesFromMap(map);
        }
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

    /**
     * Checks whether site-side notifications exist and initializes them if not.
     *
     * @throws Exception
     */
    private void checkNotifications() throws Exception {
        // Check whether any notification types already exist and clear them if so.
        clearArcSpecNotifications();

        for (NotificationType type : NotificationType.values()) {
            Definition definition = retrieveSiteEventDefinition(type);
            List<Subscription> subscriptions = getNotificationService().getSubscriptionService().getSubscriptionsForDefinition(definition);
            configureArcSpecNotificationType(_arcSpec, type, subscriptions);
        }
    }

    private void configureArcSpecNotificationType(final ArcArchivespecification arcSpec, final NotificationType type, final Collection<Subscription> subscriptions) throws Exception {
        ArcArchivespecificationNotificationTypeI typeObj = new ArcArchivespecificationNotificationType();
        typeObj.setNotificationType(type.id());
        if (subscriptions == null || subscriptions.size() == 0) {
            typeObj.setEmailAddresses(getSiteAdminAccount(arcSpec) + " <" + arcSpec.getSiteAdminEmail() + ">");
        } else {
            StringBuilder buffer = new StringBuilder();
            boolean isFirst = true;
            for (Subscription subscription : subscriptions) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    buffer.append(", ");
                }
                buffer.append(subscription.getSubscriber());
            }
            typeObj.setEmailAddresses(buffer.toString());
        }
        arcSpec.addNotificationTypes_notificationType(typeObj);
    }

    /**
     * Clears the arc spec notification listings.
     */
    private void clearArcSpecNotifications() {
        List<ArcArchivespecificationNotificationTypeI> notificationTypes;
        while ((notificationTypes = _arcSpec.getNotificationTypes_notificationType()) != null && notificationTypes.size() > 0) {
            _arcSpec.removeNotificationTypes_notificationType(0);
        }
    }

    /**
     * Gets the notification service instance.
     *
     * @return The notification service instance.
     */
    private NotificationService getNotificationService() {
        if (_notificationService == null) {
            _notificationService = XDAT.getNotificationService();
        }
        return _notificationService;
    }

    /**
     * Gets the site admin account name.
     * @param arcSpec    The arc spec from which the admin account can be retrieved.
     * @return The site admin account name.
     */
    private String getSiteAdminAccount(ArcArchivespecification arcSpec) {
        List<XdatUser> users = XDATUser.getXdatUsersByField("xdat:user/email", arcSpec.getSiteAdminEmail(), null, true);
        if (users == null || users.size() == 0) {
            throw new RuntimeException("Can't find anything for the site admin email account! It must be associated with a user account: " + arcSpec.getSiteAdminEmail());
        }
        return users.get(0).getLogin();
    }
    
    private Object checkForExistenceOfProperty(String propertyName) throws PropertyNotFoundException, ConfigServiceException, IOException {
        Object property = getArcSpecAsMap().get(_property);
        if(null == property) {
            throw new PropertyNotFoundException(String.format("Setting '%s' was not found in the system.", _property));
        }
        return property;
    }

    private static final String EXPRESSION_USERNAME = "[a-zA-Z][a-zA-Z0-9_-]{3,15}";
    private static final String EXPRESSION_EMAIL = "[_A-Za-z0-9-]+(?:\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(?:\\.[A-Za-z0-9]+)*(?:\\.[A-Za-z]{2,})";
    private static final String EXPRESSION_COMBINED = "(" + EXPRESSION_USERNAME + ")[\\s]*<(" + EXPRESSION_EMAIL + ")>";
    private static final Pattern PATTERN_USERNAME = Pattern.compile(EXPRESSION_USERNAME);
    private static final Pattern PATTERN_EMAIL = Pattern.compile(EXPRESSION_EMAIL);
    private static final Pattern PATTERN_COMBINED = Pattern.compile(EXPRESSION_COMBINED);
    private final static ObjectMapper MAPPER = new ObjectMapper(new JsonFactory());

    private NotificationService _notificationService;
    private static final Log _log = LogFactory.getLog(SettingsRestlet.class);
    private ArcArchivespecification _arcSpec;
    private String _property;
    private String _value;
    private String _form;
    private Map<String, String> _data;
}
