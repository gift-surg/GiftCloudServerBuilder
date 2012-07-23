/*******************************************************************************
 * Set of functions to facilitate settings management via AJAX
 */

function configurationIndexChanged() {
    var activeIndex = this.get("activeIndex");
    YAHOO.util.Cookie.set("configuration.tab.index", activeIndex);
}

function fullConfigHandler() {
    if (!document.getElementById('siteId').value) {
        showMessage('page_body', 'Site ID Required', 'You must specify a value for the site ID!');
        return;
    }

    var missing = new Array();

    if(window.registrationManager==undefined) {
        missing.push('Registration');
    }
    if(window.fileSystemManager==undefined) {
        missing.push('File System');
    }
    if(window.siteInfoManager==undefined) {
        missing.push('Site Information');
    }
    if(window.notificationsManager==undefined) {
        missing.push('Notifications');
    }
    if(window.anonymizationManager==undefined) {
        missing.push('Anonymization');
    }
    if (missing.length > 0) {
        var message = 'You need to review the contents of the following panels before saving: <ul>';
        for (var index = 0; index < missing.length; index++) {
            message += "<li>" + missing[index] + "</li>";
        }
        message += "</ul>";
        showMessage('page_body', 'Required', message);
    } else {
        this.initArcSpecCallback = {
            success : function() {
                showMessage('page_body', 'Welcome!', 'Your settings were saved. You will now be redirected to the main XNAT page.');
                window.location.replace(serverRoot);
            },
            failure : function(o) {
                showMessage('page_body', 'Error', 'Your settings were not successfully saved: ' + o);
            },
            scope : this
        };

        var arcSpecControls = [ 'siteId', 'siteUrl', 'siteAdminEmail', 'dcmAppletLink', 'enableCsrfToken', 'archivePath', 'prearchivePath', 'cachePath', 'ftpPath', 'buildPath', 'pipelinePath', 'requireLogin', 'enableNewRegistrations' ];
        var data = '';
        for (var index = 0; index < arcSpecControls.length; index++) {
            var control = document.getElementById(arcSpecControls[index]);
            if (data) {
                data += '&';
            }
            var value = (control.type == 'checkbox' ? (control.checked ? 1 : 0) : control.value);
            data += control.id + '=' + encodeURIComponent(value);
        }

        var putUrl = serverRoot + '/data/services/settings/initialize?XNAT_CSRF=' + window.csrfToken + '&stamp=' + (new Date()).getTime();
        YAHOO.util.Connect.asyncRequest('PUT', putUrl, this.initArcSpecCallback, data, this);
    }
}

function configurationTabManagerInit(initialize) {
    window.configurationTabView = new YAHOO.widget.TabView('configurationTabs');
    window.configuration_tabs_module = new YAHOO.widget.Module("configuration_tabs_module", {visible:false, zIndex:5});
    window.configuration_tabs_module.show();
    window.configurationTabView.subscribe("activeTabChange", configurationIndexChanged);
    if (initialize) {
        // If we're initializing, divert all of the save handlers to centralized handling.
        document.getElementById('siteInfo_save_button').onclick = fullConfigHandler;
        document.getElementById('fileSystem_save_button').onclick = fullConfigHandler;
        document.getElementById('registration_save_button').onclick = fullConfigHandler;
        document.getElementById('notifications_save_button').onclick = fullConfigHandler;
        document.getElementById('anonymization_save_button').onclick = fullConfigHandler;
        showMessage('page_body', 'Welcome!', 'Your XNAT installation has not yet been initialized. Please review each panel on this configuration screen before saving the system settings.');
    }
}

function prependLoader(div_id, msg) {
	if (div_id.id == undefined) {
		var div = document.getElementById(div_id);
	} else {
		var div = div_id;
	}
	var loader_div = document.createElement("div");
	loader_div.innerHTML = msg;
	div.parentNode.insertBefore(loader_div, div);
	return new XNATLoadingGIF(loader_div);
}

function SettingsTabManager(settingsTabDivId, settings) {
	this.settings = settings;
    this.controls = [];
	this.settings_tab_mgmt_div = document.getElementById(settingsTabDivId);
	this.settings_tab_table_div = document.createElement("div");
	this.settings_tab_table_div.id = "settings_tab_table";
	this.settings_tab_mgmt_div.appendChild(this.settings_tab_table_div);
	this.settings_svc_url = serverRoot + '/data/services/settings/';

	

    this.dirtyFlag = false;

    this.enableResetButtons = function() {
    	if(document.getElementById("siteInfo_reset_button")){
    		document.getElementById("siteInfo_reset_button").disabled = false;
    	}
    	if(document.getElementById("fileSystem_reset_button")){
    		document.getElementById("fileSystem_reset_button").disabled = false;
    	}
    	if(document.getElementById("registration_reset_button")){
    		document.getElementById("registration_reset_button").disabled = false;
    	}
    	if(document.getElementById("notifications_reset_button")){
    		document.getElementById("notifications_reset_button").disabled = false;
    	}
    	if(document.getElementById("anonymization_reset_button")){
    		document.getElementById("anonymization_reset_button").disabled = false;
    	}
    };
    this.disableResetButtons = function() {
    	if(document.getElementById("siteInfo_reset_button")){
    		document.getElementById("siteInfo_reset_button").disabled = true;
    	}
    	if(document.getElementById("fileSystem_reset_button")){
    		document.getElementById("fileSystem_reset_button").disabled = true;
    	}
    	if(document.getElementById("registration_reset_button")){
    		document.getElementById("registration_reset_button").disabled = true;
    	}
    	if(document.getElementById("notifications_reset_button")){
    		document.getElementById("notifications_reset_button").disabled = true;
    	}
    	if(document.getElementById("anonymization_reset_button")){
    		document.getElementById("anonymization_reset_button").disabled = true;
    	}
    };
    
    this.disableResetButtons();
	this.init = function() {
		this.initLoader = prependLoader(this.settings_tab_table_div, "Loading site information...");
		this.initLoader.render();
		// load from search xml from server
		this.initCallback = {
			success : this.completeInit,
			failure : this.initFailure,
			scope : this
		};

		var getUrl = this.settings_svc_url + '?XNAT_CSRF=' + window.csrfToken + '&format=json&stamp=' + (new Date()).getTime();
		YAHOO.util.Connect.asyncRequest('GET', getUrl, this.initCallback, null, this);
	};

	this.completeInit = function(o) {
		try {
            this.controls.length = 0;
			var resultSet = eval("(" + o.responseText + ")");
			for (var index = 0; index < this.settings.length; index++) {
				var setting = this.settings[index];
                var control = document.getElementById(setting);
                var value = eval('resultSet.ResultSet.Result.' + setting);
                control.defaultValue = value;
                this.controls.push(control);
                if (!this.firstControl) {
                    this.firstControl = control;
                }
			}
			this.render();
		} catch (e) {
			this.displayError("ERROR " + o.status + ": Failed to parse site information.");
		}
		this.initLoader.close();
	};

	this.initFailure = function(o) {
		this.initLoader.close();
		this.displayError("ERROR " + o.status + ": Failed to load site information.");
		if (o.status == 401) {
			alert("WARNING: Your session has expired.  You will need to re-login and navigate to the content.");
			window.location = serverRoot + "/app/template/Login.vm";
		}
	};

	this.render = function() {
		this.resetForm();
	};

	this.setFormDisabled = function(value) {
		var inputs = this.settings_tab_mgmt_div.getElementsByTagName("input");
		for ( var inputCounter in inputs) {
			inputs[inputCounter].disabled = value;
		}

		var selects = this.settings_tab_mgmt_div.getElementsByTagName("select");
		for ( var selectsCounter in selects) {
			selects[selectsCounter].disabled = value;
		}

		var textareas = this.settings_tab_mgmt_div.getElementsByTagName("textarea");
		for (var textareasCounter in textareas) {
			textareas[textareasCounter].disabled = value;
		}
	};

	this.resetForm = function() {
		this.setFormDisabled(false);
        this.dirtyFlag = false;
		this.disableResetButtons();
        for (var index = 0; index < this.controls.length; index++) {
            var control = this.controls[index];
			if (control.type == 'text') {
                control.value = control.defaultValue;
			} else if (control.type == 'checkbox') {
                control.checked = control.defaultValue.toLowerCase() === 'true';
            }
			}
		this.firstControl.focus();
	};

	this.dirtyForm = function() {
        this.dirtyFlag = true;
		this.enableResetButtons();
	};

	this.saveTabSettings = function() {
		if (this.validateSettings()) {
			if (this.isDirty()) {
				this.setFormDisabled(true);
				this.updateCallback = {
					success : this.completeSave,
					failure : this.saveFailure,
					scope : this
				};
				var data = '';
                for (var index = 0; index < this.controls.length; index++) {
                    var control = this.controls[index];
					if (data) {
						data += '&';
					}
                    var value = (control.type == 'checkbox' ? control.checked : control.value);
                    data += control.id + '=' + value;
                }
				YAHOO.util.Connect.asyncRequest('POST', this.settings_svc_url + '?XNAT_CSRF=' + window.csrfToken, this.updateCallback, data, this);
			} else {
				showMessage('page_body', 'Message', 'None of the site information appears to have changed.');
			}
		} else {
			showMessage('page_body', 'Note', 'You need to enter a value into all of the site information settings boxes to save the site settings.');
		}
	};

	this.validateSettings = function() {
        for (var index = 0; index < this.controls.length; index++) {
            var control = this.controls[index];
			if (control.type == 'text') {
				if (!control.value) {
					return false;
				}
			}
		}
        return true;
	};

	this.isDirty = function() {
        if (this.dirtyFlag) {
            return true;
        }
        for (var index = 0; index < this.controls.length; index++) {
            var control = this.controls[index];
			if (control.type == 'text') {
                if (control.value != control.defaultValue) {
					 return true;
				}
			} else if (control.type == 'checkbox') {
                if (control.checked != control.defaultValue.toLowerCase()) {
					return true;
				}
			}
		}
		return false;
	};

	this.completeSave = function(o) {
		this.completeInit(o);
		this.setFormDisabled(false);
		showMessage('page_body', 'Success', 'Your settings have been successfully updated.');
	};

	this.saveFailure = function(o) {
		if (o.status == 401) {
			alert("WARNING: Your session has expired.  You will need to re-login and navigate to the content.");
			window.location = serverRoot + "/app/template/Login.vm";
		}
        showMessage('page_body', 'Error', '<p>There was an error saving your notification settings. Please check that all of the configured usernames and addresses map to valid enabled users on your XNAT system.</p><p><b>Error code:</b> ' + o.status + ' ' + o.statusText + '</p>');
		this.setFormDisabled(false);
	};

	this.displayError = function(errorMsg) {
		this.settings_tab_table_div.className = "error";
		this.settings_tab_table_div.innerHTML = errorMsg;
	};

	this.init();
}
