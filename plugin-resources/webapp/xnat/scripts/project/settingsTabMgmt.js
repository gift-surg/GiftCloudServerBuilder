/*******************************************************************************
 * Set of functions to facilitate settings management via AJAX
 */

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
	this.resetButton = document.getElementById("reset_button");
	if (this.resetButton) {
		this.resetButton.disabled = true;
	}

    this.dirtyFlag = false;

	this.init = function() {
		this.initLoader = prependLoader(this.settings_tab_table_div, "Loading site information...");
		this.initLoader.render();
		// load from search xml from server
		this.initCallback = {
			success : this.completeInit,
			failure : this.initFailure,
			scope : this
		};

		var getUrl = this.settings_svc_url + '?format=json&stamp=' + (new Date()).getTime();
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
	};

	this.resetForm = function() {
		this.setFormDisabled(false);
        this.dirtyFlag = false;
		if (this.resetButton) {
			this.resetButton.disabled = true;
		}
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
		if (this.resetButton) {
			this.resetButton.disabled = false;
		}
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
				YAHOO.util.Connect.asyncRequest('POST', this.settings_svc_url, this.updateCallback, data, this);
			} else {
				alert("None of the site information appears to have changed.");
			}
		} else {
			alert("You need to enter a value into all of the site information settings boxes to save the site settings.");
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
		alert("Your settings have been successfully updated.");
	};

	this.saveFailure = function(o) {
		if (o.status == 401) {
			alert("WARNING: Your session has expired.  You will need to re-login and navigate to the content.");
			window.location = serverRoot + "/app/template/Login.vm";
		}
		alert("ERROR " + o.status + ": Operation Failed.");
		this.setFormDisabled(false);
	};

	this.displayError = function(errorMsg) {
		this.settings_tab_table_div.className = "error";
		this.settings_tab_table_div.innerHTML = errorMsg;
	};

	this.init();
}
