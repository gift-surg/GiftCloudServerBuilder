/*******************************************************************************
 * Set of functions to facilitate settings management via AJAX
 */

// trying to override the showMessage function
// xModal is in a state of flux, so don't know
// if this will work right now

$.getScript(serverRoot+'/scripts/xModal/xModal.js');

function showMessage(divId, title, body) {

    //alert('showMessage!');

    xModalOpen({
        kind: 'dialog',
        // width and height not necessary with kind:'dialog'
        //width: 500,
        //height: 300,
        scroll: true,
        //draggable: true, // the xModal will not be draggable by default, add draggable:true for draggability
        box: divId,
        title: title,
        content: body,
//        footer:    {
//            show: 'yes', // self-explanatory, right? anything but 'yes' will hide the footer
//            height: '' , // height in px - blank for default - 52px
//            background: '', // css for footer background - white if blank
//            border: '', // color for footer top border - white if blank
//            content: '' // empty string renders default footer with two buttons (ok/cancel) using values specified below
//        },
        // by default the buttons will go in the footer, this can be overridden by adding elements with 'class="ok default button"' and 'class="cancel button"' to the body content
        ok:       'OK', // text for submit button - if blank button doesn't render
        cancel:   '' // text for cancel button - if empty string button doesn't render
    });
    xModalSubmit = function(){
        // requires xModalClose() function to close the xModal!
        xModalClose();
    };
    xModalCancel = function(){
        // if you want to do something else if someone cancels, put it here
        // requires xModalClose() function to close the xModal!
        xModalClose();
    };

    /*
     var dialog = new YAHOO.widget.SimpleDialog("dialog", {
     width:"20em",
     close:false,
     fixedcenter:true,
     constraintoviewport:true,
     modal:true,
     icon:YAHOO.widget.SimpleDialog.ICON_WARN,
     visible:true,
     draggable:false,
     buttons: [{ text:'OK', isDefault:true, handler: function() { this.hide(); } }]
     });

     dialog.render(document.getElementById(divId));
     dialog.setHeader(title);
     dialog.setBody(body);
     dialog.bringToTop();
     dialog.show();
     */
}


function configurationIndexChanged() {
    var activeIndex = this.get("activeIndex");
    YAHOO.util.Cookie.set("configuration.tab.index", activeIndex);
}

function fullConfigHandler() {
    if (!document.getElementById('siteId').value) {
        showMessage('page_body', 'Site ID Required', 'You must specify a value for the site ID!');
        return;
    }

    var missing = [];

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
    if(window.appletManager==undefined) {
        missing.push('Applet');
    }
    if(window.dicomReceiverManager==undefined) {
        missing.push('DICOM Receiver');
    }
    if (missing.length > 0) {
        var message = 'You need to review the contents of the following panels before saving: <ul>';
        for (var index = 0; index < missing.length; index++) {
            message += "<li>" + missing[index] + "</li>";
        }
        message += "</ul>";
        showMessage('page_body', 'Required', message);
    } else {
        this.fullConfigCallback = {
            success : function() {
                showMessage('page_body', 'Welcome!', 'Your settings were saved. You will now be redirected to the main XNAT page.');
                var destination;
                if (serverRoot) {
                    destination = serverRoot;
                } else {
                    destination = "/";
                }
                window.location.replace(destination);
            },
            failure : function(o) {
                showMessage('page_body', 'Error', 'Your settings were not successfully saved: ' + o.responseText);
            },
            cache : false, // Turn off caching for IE
            scope : this
        };

        var allControls = [
            'siteId', 'siteUrl', 'siteAdminEmail', 'showapplet', 'enableCsrfToken'
            , 'archivePath', 'checksums', 'prearchivePath', 'cachePath', 'ftpPath', 'buildPath', 'pipelinePath'
            , 'requireLogin', 'enableNewRegistrations', 'emailVerification'
            , 'error', 'issue', 'newUser', 'update'
            , 'anonScript'
            , 'applet'
            , 'dcmPort', 'dcmAe', 'enableDicomReceiver'
        ];
        var data = buildSettingsUpdateRequestBody(allControls);

        var putUrl = serverRoot + '/data/services/settings/initialize?XNAT_CSRF=' + window.csrfToken + '&stamp=' + (new Date()).getTime();
        YAHOO.util.Connect.asyncRequest('PUT', putUrl, this.fullConfigCallback, data, this);

        //reset buttons to use standard save mechanism.  The system is initialized after the first attempted additional saves will fail if they use the initialize method in fullConfigHandler
        //this SHOULD be safe.  The ArcSpec.isComplete() is the method the Restlet uses to see if the arc spec is built.  All the properties that isComplete() checks are populated by default except SITE_ID.  But site_id is checked at the beginning of this method.
        document.getElementById('siteInfo_save_button').onclick = saveSettings;
        document.getElementById('fileSystem_save_button').onclick = saveSettings;
        document.getElementById('registration_save_button').onclick = saveSettings;
        document.getElementById('notifications_save_button').onclick = saveSettings;
        document.getElementById('anonymization_save_button').onclick = saveSettings;
        document.getElementById('applet_save_button').onclick = saveSettings;
        document.getElementById('dicomReceiver_save_button').onclick = saveSettings;
    }
}

function saveSettings(){
    window.siteInfoManager.saveTabSettings();
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
        document.getElementById('applet_save_button').onclick = fullConfigHandler;
        document.getElementById('dicomReceiver_save_button').onclick = fullConfigHandler;
        showMessage('page_body', 'Welcome!', 'Your XNAT installation has not yet been initialized. Please review each panel on this configuration screen before saving the system settings.');
    }
}

function prependLoader(div_id, msg) {
    var div ;
    if (div_id.id == undefined) {
        div = document.getElementById(div_id);
    } else {
        div = div_id;
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
    //this.settings_tab_table_div.id = "settings_tab_table";
    this.settings_tab_table_div.className = this.settings_tab_table_div.className + " settings_tab_table";
    this.settings_tab_mgmt_div.appendChild(this.settings_tab_table_div);
    this.settings_svc_url = serverRoot + '/data/services/settings/';

    this.dirtyFlag = false;

    var
        resetButtons =
            '#siteInfo_reset_button, ' +
            '#fileSystem_reset_button, ' +
            '#registration_reset_button, ' +
            '#notifications_reset_button, ' +
            '#anonymization_reset_button, ' +
            '#applet_reset_button, ' +
            '#dicomReceiver_reset_button'
        ;

    this.enableResetButtons = function() {
        $(resetButtons).prop('disabled',false);
    };
    this.disableResetButtons = function() {
        $(resetButtons).prop('disabled',true);
    };

    this.disableResetButtons();
    this.init = function() {
        this.initLoader = prependLoader(this.settings_tab_table_div, "Loading site information...");
        this.initLoader.render();
        // load from search xml from server
        this.initCallback = {
            success : this.completeInit,
            failure : this.initFailure,
            cache : false, // Turn off caching for IE
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
                control.defaultValue = eval('resultSet.ResultSet.Result.' + setting);
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
            if (control.type == 'text' || control.type == 'textarea') {
                control.value = control.defaultValue;
            } else if (control.type == 'checkbox') {
                control.checked = control.defaultValue.toLowerCase() === 'true';
            } else if (control.type == 'hidden') {
                control.value = control.defaultValue;
                jq('#' + control.id + 'Label').html(control.defaultValue);
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
                    cache : false, // Turn off caching for IE
                    scope : this
                };
                var data = buildSettingsUpdateRequestBody(this.controls);
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

    /**
     * Combines the wasDirty() and hasBeenDirtied() calls to tell you if the resource had been dirtied previously or is
     * in a dirty state currently. Note that this can return true even if the form isn't truly dirty, i.e. it was
     * changed then reverted manually.
     * @returns True if previously or currently dirty.
     */
    this.isDirty = function() {
        return this.wasDirty() || this.hasBeenDirtied();
    };

    /**
     * Indicates whether the resource was ever dirtied. Note that this can return true even if the form isn't truly
     * dirty, i.e. it was changed then reverted manually. The only thing that should clear this flag is the reset
     * button.
     * @returns True if the resource was previously dirtied.
     */
    this.wasDirty = function() {
        return this.dirtyFlag;
    };

    /**
     * Indicates whether the form is currently dirtied.
     * @returns {boolean}
     */
    this.hasBeenDirtied = function() {
        for (var index = 0; index < this.controls.length; index++) {
            var control = this.controls[index];
            if (control.type === 'text' || control.type === 'textarea') {
                if (control.value != control.defaultValue) {
                    return true;
                }
            } else if (control.type === 'checkbox') {
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

function buildSettingsUpdateRequestBody(controls) {
    var data = '';
    for (var index = 0; index < controls.length; index++) {
        var control;
        if('string' == typeof controls[index]) {
            control = document.getElementById(controls[index]);
        }
        else {
            control = controls[index];
        }
        // Filter out null control: happens when some text controls have no content.
        if (control != null) {
            if (data) {
                data += '&';
            }
            var value = (control.type == 'checkbox' ? control.checked : control.value);
            data += control.id + '=' + encodeURIComponent(value);
        }
    }

    return data;
}
