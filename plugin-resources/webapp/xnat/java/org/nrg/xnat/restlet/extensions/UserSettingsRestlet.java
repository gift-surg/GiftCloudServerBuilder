/*
 * org.nrg.xnat.restlet.extensions.UserSettingsRestlet
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 1/30/14 3:58 PM
 */
package org.nrg.xnat.restlet.extensions;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.nrg.mail.services.EmailRequestLogService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperField;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.restlet.XnatRestlet2;
import org.nrg.xnat.restlet.XnatRestletURI;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.restlet.util.Template;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@XnatRestlet2({@XnatRestletURI(matchingMode=Template.MODE_EQUALS,value="/user"), @XnatRestletURI(matchingMode=Template.MODE_EQUALS,value="/user/{USER_ID}"), @XnatRestletURI("/user/actions/{ACTION}"), @XnatRestletURI("/user/actions/{USER_ID}/{ACTION}")})
public class UserSettingsRestlet extends SecureResource {
    public static final String PARAM_USER_ID = "USER_ID";
    public static final String PARAM_ACTION = "ACTION";

    public UserSettingsRestlet(Context context, Request request, Response response) throws ResourceException {
        super(context, request, response);

        if (_builder == null) {
            synchronized (UserSettingsRestlet.class) {
                XPath xpath = XPathFactory.newInstance().newXPath();
                try {
                    _builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    _propertyMappings = new HashMap<UserProperty, XPathExpression>();
                    for (UserProperty property : UserProperty.values()) {
                        _propertyMappings.put(property, xpath.compile(XPATH_EXPRESSIONS.containsKey(property) ? XPATH_EXPRESSIONS.get(property) : String.format("/user/%s", property.toString())));
                    }
                } catch (XPathExpressionException exception) {
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error in initialization", exception);
                } catch (ParserConfigurationException exception) {
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error in initialization", exception);
                }
            }
        }

        if (!user.isSiteAdmin()) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User does not have privileges to access this project.");
            _action = null;
            _auths = null;
            _isJsonRequested = false;
            _userId = null;
        } else {
            setModifiable(true);
            this.getVariants().add(new Variant(MediaType.ALL));

            if (StringUtils.isBlank(requested_format)) {
                requested_format = "json";
            }

            _isJsonRequested = getRequestedMediaType().equals(MediaType.APPLICATION_JSON);

            _userId = (String) getRequest().getAttributes().get(PARAM_USER_ID);
            if (!StringUtils.isBlank(_userId)) {
                _auths = XDAT.getXdatUserAuthService().getUsersByXdatUsername(_userId);
            if (_auths == null || _auths.size() == 0) {
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "As of this release, you must specify a user on which to perform.");
            }
            } else {
                _auths = null;
            }

            _action = UserAction.action((String)getRequest().getAttributes().get(PARAM_ACTION));

            final Method method = request.getMethod();
            try {
                _payload = request.getEntity().getText();
            } catch (IOException exception) {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Error when retrieving form body", exception);
            }

            validateParameters(method);
        }
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        try {
            if (StringUtils.isBlank(_userId)) {
                Collection logins = XDATUser.getAllLogins();
                return new StringRepresentation(_isJsonRequested ? _serializer.writeValueAsString(logins) : createLoginListXml(logins));
    }

            XdatUser requestedUser = XDATUser.getXdatUsersByLogin(_userId, user, true);
            return simpleRepresentation(requestedUser, getRequestedMediaType());
        } catch (Exception exception) {
            _log.error("There was an error rendering the list of user IDs", exception);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "There was an error rendering the list of user IDs", exception);
    }
    }

    @Override
    public void handlePut() {
        if (_action != null) {
           handleAction();
        }
    }

    @Override
    public void handlePost() {
        if (_action != null) {
           handleAction();
        }

        try {
            Map<UserProperty, String> attributes = translateBody();
            XDATUser xdatUser = new XDATUser();
            final String login = attributes.get(UserProperty.login);
            if (!StringUtils.isBlank(login)) {
                xdatUser.setLogin(login);
            }
            final String email = attributes.get(UserProperty.email);
            if (!StringUtils.isBlank(email)) {
                xdatUser.setEmail(email);
            }
            final String firstname = attributes.get(UserProperty.firstname);
            if (!StringUtils.isBlank(firstname)) {
                xdatUser.setFirstname(firstname);
            }
            final String lastname = attributes.get(UserProperty.lastname);
            if (!StringUtils.isBlank(lastname)) {
                xdatUser.setLastname(lastname);
            }
            final String password = attributes.get(UserProperty.password);
            if (!StringUtils.isBlank(password)) {
                String salt = XDATUser.createNewSalt();
                xdatUser.setPrimaryPassword(new ShaPasswordEncoder(256).encodePassword(password, salt));
                xdatUser.setPrimaryPassword_encrypt(true);
                xdatUser.setSalt(salt);
            }
            final String enabled = attributes.get(UserProperty.enabled);
            if (!StringUtils.isBlank(enabled)) {
                xdatUser.setEnabled(enabled);
            }
            final String verified = attributes.get(UserProperty.verified);
            if (!StringUtils.isBlank(verified)) {
                xdatUser.setVerified(verified);
            }

            // Find all auths before saving primary record. This way, if there are any format errors, we'll error out before committing the new user record.
            List<Map<UserProperty, String>> auths = translateAuths();

            SaveItemHelper.authorizedSave(xdatUser, user, true, false, true, false, EventUtils.newEventInstance(EventUtils.CATEGORY.SIDE_ADMIN, EventUtils.TYPE.WEB_SERVICE, "Registered User"));

            // If there are no auths specified...
            if (auths == null || auths.size() == 0) {
                // Create the default database auth record.
                XdatUserAuth newUserAuth = new XdatUserAuth(login, XdatUserAuthService.LOCALDB);
                XDAT.getXdatUserAuthService().create(newUserAuth);
            } else {
                // Otherwise, iterate the auths and create one for each.
                for (Map<UserProperty, String> auth : auths) {
                    XdatUserAuth newUserAuth = new XdatUserAuth();
                    String authId = auth.get(UserProperty.authId);
                    newUserAuth.setAuthUser(authId);
                    newUserAuth.setXdatUsername(login);
                    newUserAuth.setAuthMethod(auth.get(UserProperty.method));
                    if (auth.containsKey(UserProperty.methodId)) {
                        newUserAuth.setAuthMethodId(auth.get(UserProperty.methodId));
                    }
                    if (auth.containsKey(UserProperty.enabled)) {
                        newUserAuth.setEnabled(Boolean.parseBoolean(auth.get(UserProperty.enabled)));
                    }
                    XDAT.getXdatUserAuthService().create(newUserAuth);
                }
            }
        } catch (Exception exception) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, exception, "Error handling POST operation");
        }
    }

    @Override
    public void handleDelete() {
        if (_action != null) {
            handleAction();
        }
    }

    private void handleAction() {
        if (_action == UserAction.Reset) {
            for (XdatUserAuth auth : _auths) {
                auth.setFailedLoginAttempts(0);
                XDAT.getXdatUserAuthService().update(auth);
            }
        } else if(_action == UserAction.ResetEmailRequests){
           try{
              final XDATUser u = new XDATUser(_userId);
              final EmailRequestLogService requests = XDAT.getContextService().getBean(EmailRequestLogService.class);
              requests.unblockEmail(u.getEmail());
           }catch(Exception e){ 
              getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e, "Error reseting email requests for " + _userId);
           } 
        } else {
            throw new RuntimeException("Unknown action: " + _action);
        }
    }

    private void validateParameters(final Method method) throws ResourceException {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating parameters for method call: " + method.toString());
        }
        if (_action == null && _userId == null && _payload == null && method.equals(Method.POST)) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "You can't do a " + method.toString() + " operation without specifying a user ID, action, or payload.");
        }
    }

    private String createLoginListXml(final Collection<String> logins) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\"?>\n<logins>\n");
        for (String login : logins) {
            xml.append("    <login>").append(login).append("</login>\n");
        }
        return xml.append("</logins>\n").toString();
    }

    private boolean isXml(final String body) {
        return body.startsWith("<?xml");
    }

    private Map<UserProperty, String> translateBody() throws IOException, SAXException, XPathExpressionException {
        return isXml(_payload) ? translateXmlBody() : translateJsonBody();
    }

    private Map<UserProperty, String> translateXmlBody() throws IOException, SAXException, XPathExpressionException {
        Map<UserProperty, String> properties = new HashMap<UserProperty, String>();
        Document document = getXmlDocument();
        for (UserProperty property : UserProperty.values()) {
            if (!XPATH_EXPRESSIONS.containsKey(property)) {
                Object value = _propertyMappings.get(property).evaluate(document, XPathConstants.STRING);
                if (value != null) {
                    properties.put(property, value.toString());
                }
            }
        }
        return properties;
    }

    private Map<UserProperty, String> translateJsonBody() throws IOException {
        Map<UserProperty, String> properties = new HashMap<UserProperty, String>();
        JsonNode node = getJsonNode();
        for (UserProperty property : UserProperty.values()) {
            if (!XPATH_EXPRESSIONS.containsKey(property)) {
                Object value = node.get(property.toString()).getTextValue();
                if (value != null) {
                    properties.put(property, value.toString());
                }
            }
        }
        return properties;
    }

    private List<Map<UserProperty, String>> translateAuths() throws IOException, SAXException, XPathExpressionException {
        return isXml(_payload) ? translateXmlAuths() : translateJsonAuths();
    }

    private List<Map<UserProperty, String>> translateXmlAuths() throws IOException, SAXException, XPathExpressionException {
        List<Map<UserProperty, String>> auths = new ArrayList<Map<UserProperty, String>>();
        Document document = getXmlDocument();
        NodeList authNodes = (NodeList) _propertyMappings.get(UserProperty.userAuths).evaluate(document, XPathConstants.NODESET);
        if (authNodes != null && authNodes.getLength() > 0) {
            for (int i = 0; i < authNodes.getLength(); i++) {
                Map<UserProperty, String> auth = new HashMap<UserProperty, String>();
                Node node = authNodes.item(i);
                auth.put(UserProperty.enabled, node.hasAttributes() && node.getAttributes().getNamedItem("enabled") != null
                        ? node.getAttributes().getNamedItem("enabled").getNodeValue()
                        : "true");
                auth.put(UserProperty.authId, (String) _propertyMappings.get(UserProperty.authId).evaluate(node, XPathConstants.STRING));
                auth.put(UserProperty.method, (String) _propertyMappings.get(UserProperty.method).evaluate(node, XPathConstants.STRING));
                String methodId = (String) _propertyMappings.get(UserProperty.methodId).evaluate(node, XPathConstants.STRING);
                if (!StringUtils.isBlank(methodId)) {
                    auth.put(UserProperty.methodId, methodId);
                }
                auths.add(auth);
            }
        }
        return auths;
    }

    private List<Map<UserProperty, String>> translateJsonAuths() throws IOException {
        List<Map<UserProperty, String>> auths = new ArrayList<Map<UserProperty, String>>();
        JsonNode authNodes = getJsonNode().get("userAuths");
        if (authNodes != null && authNodes.size() > 0) {
            for (int i = 0; i < authNodes.size(); i++) {
                Map<UserProperty, String> auth = new HashMap<UserProperty, String>();
                JsonNode node = authNodes.get(i);
                auth.put(UserProperty.authId, node.get(UserProperty.authId.toString()).getTextValue());
                auth.put(UserProperty.method, node.get(UserProperty.method.toString()).getTextValue());
                JsonNode methodId = node.get(UserProperty.methodId.toString());
                if (methodId != null) {
                    auth.put(UserProperty.methodId, methodId.getTextValue());
                }
                JsonNode enabled = node.get(UserProperty.enabled.toString());
                if (enabled != null) {
                    auth.put(UserProperty.enabled, Boolean.toString(enabled.getBooleanValue()));
                }
                auths.add(auth);
            }
        }
        return auths;
    }

    private Document getXmlDocument() throws SAXException, IOException {
        if (_document == null) {
            _document = _builder.parse(new InputSource(new StringReader(_payload)));
        }
        return _document;
    }

    private JsonNode getJsonNode() throws IOException {
        if (_node == null) {
            _node = _serializer.readValue(_payload, JsonNode.class);
        }
        return _node;
    }

    private Representation simpleRepresentation(final XdatUser xdatUser, final MediaType requestedMediaType) {
        StringBuilder output = new StringBuilder();
        String login = xdatUser.getLogin();
        List<XdatUserAuth> auths = XDAT.getXdatUserAuthService().getUsersByXdatUsername(login);
        if (requestedMediaType.equals(MediaType.APPLICATION_JSON)) {
            output.append("{ \"login\": \"").append(login).append("\", ");
            output.append("\"email\": \"").append(xdatUser.getEmail()).append("\", ");
            output.append("\"firstname\": \"").append(xdatUser.getFirstname()).append("\", ");
            output.append("\"lastname\": \"").append(xdatUser.getLastname()).append("\", ");
            output.append("\"enabled\": \"").append(xdatUser.getEnabled()).append("\", ");
            output.append("\"verified\": \"").append(xdatUser.getVerified()).append("\"");
            if (auths.size() > 0) {
                output.append(", \"userAuths\": [ ");
                boolean isFirstAuth = true;
                for (XdatUserAuth auth : auths) {
                    if (!isFirstAuth) {
                        output.append(", ");
                    } else {
                        isFirstAuth = false;
                    }
                    output.append("{ \"authId\": \"").append(auth.getAuthUser()).append("\", ");
                    output.append("\"method\": \"").append(auth.getAuthMethod()).append("\"");
                    if (!StringUtils.isBlank(auth.getAuthMethodId())) {
                        output.append(", \"methodId\": \"").append(auth.getAuthMethodId()).append("\"");
                    }
                    if (!auth.isEnabled()) {
                        output.append(", \"enabled\": \"false\"");
                    }
                    output.append(" }");
                }
                output.append(" ]");
            }
            output.append(" }");
        } else if (requestedMediaType.equals(MediaType.TEXT_XML) || requestedMediaType.equals(MediaType.APPLICATION_XML)) {
            output.append("<?xml version=\"1.0\"?>\n<user>\n");
            output.append("    <login>").append(login).append("</login>\n");
            output.append("    <email>").append(xdatUser.getEmail()).append("</email>\n");
            output.append("    <firstname>").append(xdatUser.getFirstname()).append("</firstname>\n");
            output.append("    <lastname>").append(xdatUser.getLastname()).append("</lastname>\n");
            output.append("    <enabled>").append(xdatUser.getEnabled()).append("</enabled>\n");
            output.append("    <verified>").append(xdatUser.getVerified()).append("</verified>\n");
            if (auths.size() > 0) {
                output.append("    <userAuths>\n");
                for (XdatUserAuth auth : auths) {
                    output.append("        <userAuth");
                    if (!auth.isEnabled()) {
                        output.append(" enabled=\"false\"");
                    }
                    output.append(">\n");
                    output.append("            <authId>").append(auth.getAuthUser()).append("</authId>\n");
                    output.append("            <method>").append(auth.getAuthMethod()).append("</method>\n");
                    if (!StringUtils.isBlank(auth.getAuthMethodId())) {
                        output.append("            <method>").append(auth.getAuthMethodId()).append("</method>\n");
                    }
                    output.append("        </userAuth>\n");
                }
                output.append("    </userAuths>\n");
            }
            output.append("</user>");
        }
        return new StringRepresentation(output.toString());
    }

    enum UserAction {
        Reset,
        ResetEmailRequests;

        public static UserAction action(String action) {
            if (StringUtils.isBlank(action)) {
                return null;
            }
            if (_actions.isEmpty()) {
                synchronized (UserAction.class) {
                    for (UserAction userAction : values()) {
                        _actions.put(userAction.toString(), userAction);
                    }
                }
            }
            return _actions.get(action);
        }

        @Override
        public String toString() {
            return this.name();
        }

        private static Map<String, UserAction> _actions = new TreeMap<String, UserAction>(String.CASE_INSENSITIVE_ORDER);
    }

    enum UserProperty {
        email,
        enabled,
        firstname,
        lastname,
        login,
        password,
        verified,
        userAuths,
        authId,
        method,
        methodId
    }

    abstract class IgnoreSetValueMixIn {
        @JsonIgnore
        public abstract Boolean isPrimaryKey();
    }

    private static final Log _log = LogFactory.getLog(UserSettingsRestlet.class);
    private static final ObjectMapper _serializer = new ObjectMapper() {{
        getSerializationConfig().addMixInAnnotations(XdatUser.class, IgnoreSetValueMixIn.class);
        getSerializationConfig().addMixInAnnotations(GenericWrapperField.class, IgnoreSetValueMixIn.class);
    }};
    private static final Map<UserProperty, String> XPATH_EXPRESSIONS = new HashMap<UserProperty, String>() {{
        put(UserProperty.userAuths, "//userAuths/userAuth");
        put(UserProperty.authId, "authId");
        put(UserProperty.method, "method");
        put(UserProperty.methodId, "methodId");
    }};

    private static DocumentBuilder _builder;
    private static Map<UserProperty, XPathExpression> _propertyMappings;

    private final boolean _isJsonRequested;

    private final String _userId;
    private final UserAction _action;
    private final List<XdatUserAuth> _auths;
    private String _payload;
    private Document _document;
    private JsonNode _node;
}
