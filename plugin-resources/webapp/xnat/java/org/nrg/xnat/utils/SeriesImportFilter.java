/*
 * org.nrg.xnat.utils.SeriesImportFilter
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 1/3/14 9:54 AM
 */
package org.nrg.xnat.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.framework.exceptions.NrgServiceError;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.model.XnatImagescandataI;

import com.google.common.base.Joiner;

public class SeriesImportFilter {

	public static final String SERIES_IMPORT_TOOL = "seriesImportFilter";
	public static final String SERIES_IMPORT_PATH = "config";
	public static final int LAST_TAG = Tag.SeriesDescription;

	private boolean _isDirty = false;
	private boolean _enabled;
	private Long _projectId = null;
	private Mode _mode;
	private List<String> _filters;
	private List<Pattern> _patterns;

	public SeriesImportFilter() {
		this(XDAT.getConfigService().getConfig(SERIES_IMPORT_TOOL,
				SERIES_IMPORT_PATH));
	}

	public SeriesImportFilter(long projectId) {
		this(XDAT.getConfigService().getConfig(SERIES_IMPORT_TOOL,
				SERIES_IMPORT_PATH, projectId));
		_projectId = projectId;
	}

	public SeriesImportFilter(Configuration configuration) {
		this(getSeriesFilterAsMap(configuration));
		if (configuration != null) {
			_enabled = configuration.getStatus().equals("enabled");
			_projectId = configuration.getProject();
		} else {
			_isDirty = true;
		}
	}

	private SeriesImportFilter(final Map<String, String> values) {
		_enabled = true;
		if (values.containsKey("projectId")) {
			_projectId = Long.parseLong(values.get("projectId"));
		}
		_mode = Mode.mode(values.get("mode"));
		_filters = Arrays.asList(values.get("list").split("\\n+"));
		_patterns = compileFilterList(_filters);
	}

	public static List<Pattern> compileFilterList(final String list) {
		final String[] candidates = list.split("\\n+");
		return compileFilterList(candidates);
	}

	public static List<Pattern> compileFilterList(final String[] candidates) {
		return compileFilterList(Arrays.asList(candidates));
	}

	public static List<Pattern> compileFilterList(final List<String> candidates) {
		final List<String> failed = new ArrayList<String>();
		final List<Pattern> patterns = new ArrayList<Pattern>();
		for (String candidate : candidates) {
			try {
				patterns.add(Pattern.compile(candidate));
			} catch (PatternSyntaxException ignored) {
				failed.add(candidate);
			}
		}
		if (failed.size() > 0) {
			String failedPatterns;
			if (failed.size() == 1) {
				failedPatterns = failed.get(0);
			} else {
				StringBuilder buffer = new StringBuilder();
				for (String failedPattern : failed) {
					buffer.append(failedPattern).append(", ");
				}
				failedPatterns = buffer.toString().substring(0,
						buffer.length() - 2);
			}
			throw new NrgServiceRuntimeException(
					NrgServiceError.UnsupportedFeature,
					"The series import filter contains the following invalid pattern(s): "
							+ failedPatterns);
		}
		return patterns;
	}

	public static Map<String, String> getSeriesFilterAsMap(
			Configuration configuration) {
		if (configuration == null) {
			return getDefaultSeriesFilterMap();
		}
		return getSeriesFilterAsMap(configuration.getContents());
	}

	public static Map<String, String> getSeriesFilterAsMap(String contents) {
		if (StringUtils.isBlank(contents)) {
			return getDefaultSeriesFilterMap();
		}
		try {
			return MAPPER.readValue(contents, MAP_TYPE_REFERENCE);
		} catch (IOException exception) {
			throw new NrgServiceRuntimeException(
					NrgServiceError.Unknown,
					"Something went wrong unmarshalling the series import filter configuration.",
					exception);
		}
	}

	public boolean isDirty() {
		return _isDirty;
	}

	public boolean isEnabled() {
		return _enabled;
	}

	public void setEnabled(boolean enabled) {
		if (enabled != _enabled) {
			_enabled = enabled;
			_isDirty = true;
		}
	}

	public Long getProjectId() {
		return _projectId;
	}

	public void setProjectId(Long projectId) {
		if (!((projectId == null && _projectId == null) || (projectId != null && projectId
				.equals(_projectId)))) {
			_projectId = projectId;
			_isDirty = true;
		}
	}

	public Mode getMode() {
		return _mode;
	}

	public void setMode(final Mode mode) {
		if (mode != _mode) {
			_mode = mode;
			_isDirty = true;
		}
	}

	public List<String> getFilters() {
		return _filters;
	}

	public String getFiltersAsString() {
		return Joiner.on("\n").join(_filters);
	}

	public void setFilters(final String filters) {
		setFilters(Arrays.asList(filters.split("\n")));
	}

	public void setFilters(final List<String> filters) {
		if (!CollectionUtils.isEqualCollection(filters, _filters)) {
			_filters = filters;
			_patterns = compileFilterList(filters);
			_isDirty = true;
		}
	}

	public List<Pattern> getPatterns() {
		return _patterns;
	}

	public boolean shouldIncludeDicomObject(final DicomObject dicomObject) {
		final String seriesDescription = dicomObject
				.getString(Tag.SeriesDescription);
		return shouldIncludeDicomObject(seriesDescription);
	}

	public boolean shouldIncludeDicomObject(final XnatImagescandataI scan) {
		return StringUtils.isEmpty(scan.getSeriesDescription())
				|| shouldIncludeDicomObject(scan.getSeriesDescription());
	}

	public boolean shouldIncludeDicomObject(final String seriesDescription) {
		for (String filter : _filters) {
			// Finding a match is insufficient, we need to check the mode.
			if (StringUtils.isNotEmpty(filter)
					&& StringUtils.isNotBlank(seriesDescription)
					&& seriesDescription.matches(filter)) {
				// So if we matched, then this should be included if this is a
				// whitelist. If
				// it's a blacklist, this will return false and indicate that
				// this DicomObject
				// should not be included.
				return _mode == Mode.Whitelist;
			}
		}

		// We didn't match anything. That means that, if this is a blacklist, we
		// should include
		// this DicomObject, but if it's a whitelist, we should not.
		return _mode == Mode.Blacklist;
	}

	public void commit(String username) {
		commit(username, "No reason given");
	}

	public void commit(String username, String reason) {
		// Remove enabled and project ID, since those are not actually stored as
		// the config contents.
		final Map<String, String> map = toMap();
		map.remove("enabled");
		map.remove("projectId");
		final String filter;
		try {
			filter = MAPPER.writeValueAsString(map);
		} catch (IOException exception) {
			throw new NrgServiceRuntimeException(NrgServiceError.Unknown,
					"Error occurred trying to marshall filter values",
					exception);
		}

		// Get the config if it exists.
		final Configuration existing = _projectId == null ? XDAT
				.getConfigService().getConfig(SERIES_IMPORT_TOOL,
						SERIES_IMPORT_PATH) : XDAT.getConfigService()
				.getConfig(SERIES_IMPORT_TOOL, SERIES_IMPORT_PATH, _projectId);

		// If the config is null, we can't very well enable or disable it.
		if (existing != null) {
			final Map<String, String> existingContents = getSeriesFilterAsMap(existing);
			final boolean isModeChanged = !existingContents.get("mode").equals(
					map.get("mode"));
			final boolean isListChanged = !existingContents.get("list").equals(
					map.get("list"));
			if (isModeChanged || isListChanged) {
				StringBuilder message = new StringBuilder("Updated ");
				if (_projectId == null) {
					message.append("site-wide series import filter ");
				} else {
					message.append("series import filter for project ")
							.append(_projectId).append(" ");
				}
				if (isModeChanged) {
					message.append("mode to ").append(map.get("mode"));
				}
				if (isModeChanged && isListChanged) {
					message.append(" and ");
				}
				if (isListChanged) {
					message.append("list to ").append(
							map.get("list").trim().replaceAll("\n", ", "));
				}
				try {
					if (_projectId == null) {
						XDAT.getConfigService().replaceConfig(username,
								message.toString(), SERIES_IMPORT_TOOL,
								SERIES_IMPORT_PATH, filter);
					} else {
						XDAT.getConfigService().replaceConfig(username,
								message.toString(), SERIES_IMPORT_TOOL,
								SERIES_IMPORT_PATH, filter, _projectId);
					}
				} catch (ConfigServiceException exception) {
					throw new NrgServiceRuntimeException(
							NrgServiceError.Unknown,
							"Error updating configuration for the series import filter",
							exception);
				}
			}
			if (_enabled && !existing.getStatus().equals("enabled")
					&& !isModeChanged && !isListChanged) { // if mode or list
															// changed, the
															// updated version
															// is already
															// enabled
				try {
					if (_projectId == null) {
						XDAT.getConfigService().enable(username, reason,
								SERIES_IMPORT_TOOL, SERIES_IMPORT_PATH);
					} else {
						XDAT.getConfigService().enable(username, reason,
								SERIES_IMPORT_TOOL, SERIES_IMPORT_PATH,
								_projectId);
					}
				} catch (ConfigServiceException exception) {
					final String message = _projectId == null ? "Error enabling the site-wide series import filter"
							: "Error enabling the series import filter for project "
									+ _projectId;
					throw new NrgServiceRuntimeException(
							NrgServiceError.Unknown, message, exception);
				}
			} else if (!_enabled
					&& (existing.getStatus().equals("enabled") || isModeChanged || isListChanged)) { // if
																										// we
																										// are
																										// disabling
																										// a
																										// filter,
																										// or
																										// need
																										// to
																										// disable
																										// a
																										// newly
																										// updated
																										// filter
				try {
					if (_projectId == null) {
						XDAT.getConfigService().disable(username, reason,
								SERIES_IMPORT_TOOL, SERIES_IMPORT_PATH);
					} else {
						XDAT.getConfigService().disable(username, reason,
								SERIES_IMPORT_TOOL, SERIES_IMPORT_PATH,
								_projectId);
					}
					return;
				} catch (ConfigServiceException exception) {
					throw new NrgServiceRuntimeException(
							NrgServiceError.Unknown,
							"Error disabling the site-wide series import filter",
							exception);
				}
			}
		} else {
			try {
				if (_projectId == null) {
					XDAT.getConfigService().replaceConfig(username, reason,
							SERIES_IMPORT_TOOL, SERIES_IMPORT_PATH, true,
							filter);
				} else {
					XDAT.getConfigService().replaceConfig(username, reason,
							SERIES_IMPORT_TOOL, SERIES_IMPORT_PATH, true,
							filter, _projectId);
				}
				// In reality, this shouldn't ever really happen. You can't
				// disable the filter and send filter values, but just in
				// case...
				if (!_enabled) {
					if (_projectId == null) {
						XDAT.getConfigService().disable(username,
								"Disabled on creation", SERIES_IMPORT_TOOL,
								SERIES_IMPORT_PATH);
					} else {
						XDAT.getConfigService().disable(username,
								"Disabled on creation", SERIES_IMPORT_TOOL,
								SERIES_IMPORT_PATH, _projectId);
					}
				}
			} catch (ConfigServiceException exception) {
				throw new NrgServiceRuntimeException(
						NrgServiceError.Unknown,
						"Error creating new configuration for the series import filter",
						exception);
			}
		}
	}

	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("enabled", Boolean.toString(_enabled));
		if (_projectId != null) {
			map.put("projectId", Long.toString(_projectId));
		}
		map.put("mode", _mode.getValue());
		map.put("list", getFiltersAsString());
		return map;
	}

	public Map<String, String> toQualifiedMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("seriesImportFilterEnabled", Boolean.toString(_enabled));
		if (_projectId != null) {
			map.put("seriesImportFilterProjectId", Long.toString(_projectId));
		}
		map.put("seriesImportFilterMode", _mode.getValue());
		map.put("seriesImportFilterList", getFiltersAsString());
		return map;
	}

	private static Map<String, String> getDefaultSeriesFilterMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("enabled", "false");
		map.put("mode", "blacklist");
		map.put("list", "");
		return map;
	}

	public enum Mode {
		Blacklist("blacklist"), Whitelist("whitelist");

		private final String _value;
		private static final Map<String, Mode> _modes = new HashMap<String, Mode>();

		Mode(String value) {
			_value = value;
		}

		public String getValue() {
			return _value;
		}

		public static Mode mode(String value) {
			if (_modes.isEmpty()) {
				synchronized (Mode.class) {
					for (Mode mode : values()) {
						_modes.put(mode.getValue(), mode);
					}
				}
			}
			return _modes.get(value);
		}

		@Override
		public String toString() {
			return this.name();
		}
	}

	private static final ObjectMapper MAPPER = new ObjectMapper(
			new JsonFactory());
	private static final TypeReference<HashMap<String, String>> MAP_TYPE_REFERENCE = new TypeReference<HashMap<String, String>>() {
	};
}
