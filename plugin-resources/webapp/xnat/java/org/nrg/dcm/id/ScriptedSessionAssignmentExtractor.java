package org.nrg.dcm.id;

import org.dcm4che2.data.DicomObject;
import org.nrg.automation.services.ScriptRunnerService;
import org.nrg.dcm.ChainExtractor;
import org.nrg.dcm.Extractor;
import org.nrg.framework.constants.Scope;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xnat.DicomObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ScriptedSessionAssignmentExtractor extends ChainExtractor {

    public ScriptedSessionAssignmentExtractor(final String scriptId, Collection<Integer> tags, final Extractor... extractors) {
        this(scriptId, null, tags, Arrays.asList(extractors));
    }

    public ScriptedSessionAssignmentExtractor(final String scriptId, Collection<Integer> tags, final List<Extractor> extractors) {
        this(scriptId, null, tags, extractors);
    }

    public ScriptedSessionAssignmentExtractor(final String scriptId, final String path, Collection<Integer> tags, final Extractor... extractors) {
        this(scriptId, path, tags, Arrays.asList(extractors));
    }

    public ScriptedSessionAssignmentExtractor(final String scriptId, final String path, Collection<Integer> tags, final Iterable<Extractor> extractors) {
        super(extractors);
        _service = XDAT.getContextService().getBean(ScriptRunnerService.class);
        _scriptId = scriptId;
        _path = path;
        _tags.addAll(tags);
    }

    public void setIdentifier(DicomObjectIdentifier<XnatProjectdata> identifier) {
        _identifier = identifier;
    }

    @Override
    public String extract(final DicomObject object) {
        // Get the project ID if available.
        XnatProjectdata project = _identifier.getProject(object);
        final Object projectData = project == null ? null : project.getItem().getProps().get("projectdata_info");
        final String projectId = (projectData == null || !(projectData instanceof Integer)) ? null : projectData.toString();

        // If we have a project ID, then
        final Scope scope = projectId == null ? Scope.Site : Scope.Project;

        final String defaultValue = super.extract(object);

        // If there's no script associated with the indicated ID, just return the default.
        if (!_service.hasScript(scope, projectId, _scriptId, _path)) {
            return defaultValue;
        }

        Map<Integer, String> values = new HashMap<Integer, String>();
        for (final Integer tag : _tags) {
            values.put(tag, object.getString(tag));
        }

        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("defaultValue", defaultValue);
        parameters.put("values", values);
        parameters.put("logger", _log);

        final Object results = _service.runScript("admin", scope, projectId, _scriptId, _path, parameters);
        return results == null ? defaultValue : results.toString();
    }

    @Override
    public SortedSet<Integer> getTags() {
        return _tags;
    }

    private static final Logger _log = LoggerFactory.getLogger(ScriptedSessionAssignmentExtractor.class);

    private final SortedSet<Integer> _tags = new TreeSet<Integer>();
    private final ScriptRunnerService _service;
    private DicomObjectIdentifier<XnatProjectdata> _identifier;
    private final String _scriptId;
    private final String _path;
}
