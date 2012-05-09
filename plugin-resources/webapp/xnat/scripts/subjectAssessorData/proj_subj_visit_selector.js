/**
 * This is being created in global scope so that it can be used by the callback function for the YUI calendar control.
 */
var visits = new Array("1", "2", "3");
var oneDay = 24 * 60 * 60 * 1000;

/**
 * @param _defaultProject
 * @param _defaultSubject
 */
function ProjectSubjectVisitSelector(_defaultProject, _defaultSubject) {
    this.defaultProject = _defaultProject;
    this.defaultSubject = _defaultSubject;
    this.visitSpan = document.getElementById('visit_entry');
    this.visitSelect = document.getElementById('visit_id');
    this.sessionTypeSpan = document.getElementById('session_type_entry');
    this.sessionTypeSelect = document.getElementById('session_type');

    this.init = function () {

        //load from search xml from server
        this.initCallback = {
            success:this.completeInit,
            failure:this.initFailure,
            scope:this
        };

        var params = "";

        params += "&owner=true";
        params += "&member=true";

        //noinspection JSUnresolvedVariable
        YAHOO.util.Connect.asyncRequest('GET', serverRoot + '/data/projects?format=json&timestamp=' + (new Date()).getTime() + params, this.initCallback, null, this);
    };

    this.initFailure = function (o) {
        this.displayError("ERROR " + o.status + ": Failed to load project list.");
    };

    this.completeInit = function (o) {
        try {
            this.projectResultSet = eval("(" + o.responseText + ")");
        } catch (e) {
            this.displayError("ERROR " + o.status + ": Failed to parse project list.");
        }
        try {
            this.renderProjects();
            this.rigDateControls();
        } catch (e) {
            this.displayError("ERROR : Failed to render project list: " + e.toString());
        }
    };

    this.renderProjects = function () {
        //noinspection JSUnresolvedVariable
        if (this.projectResultSet.ResultSet.Result.length == 0) {

        } else {
            this.projectBox = document.getElementById("project");
            this.projectBox.options[0] = new Option("SELECT", "");

            //noinspection JSUnresolvedVariable
            for (var pC = 0; pC < this.projectResultSet.ResultSet.Result.length; pC++) {
                //noinspection JSUnresolvedVariable
                var defaultSelected = !!(this.projectResultSet.ResultSet.Result[pC].id == this.defaultProject);
                //noinspection JSUnresolvedVariable
                this.projectBox.options[pC + 1] = new Option(this.projectResultSet.ResultSet.Result[pC].secondary_id, this.projectResultSet.ResultSet.Result[pC].id, defaultSelected, defaultSelected);
                if (defaultSelected) {
                    this.projectBox.selectedIndex = (this.projectBox.options.length - 1);
                }
            }

            this.projectBox.disabled = false;

            this.projectBox.manager = this;

            this.projectBox.onchange = function () {
                if (this.selectedIndex > 0) {
                    this.manager.projectID = this.options[this.selectedIndex].value;
                    this.manager.loadSubjects();
                    this.manager.checkVisitAndSessionType();
                }
            };

            if (this.projectBox.selectedIndex > 0) {
                this.projectBox.onchange();
            }
        }

        // This whole construct is so that the calendar can find its way back to the manager object.
        var sessionDateCalendar = document.getElementById("cal_session_date");
        sessionDateCalendar.calendar.selectEvent.subscribe(function () {
            //noinspection JSUnresolvedVariable
            window.projectSubjectVisitManager.manageLaunchUploaderButton();
        }, sessionDateCalendar, false);
    };

    this.rigDateControls = function () {
        var noSessionDate = document.getElementById("no_session_date");
        noSessionDate.manager = this;
        noSessionDate.onclick = function() {
            document.getElementById("session_date").disabled = this.checked;
            this.manager.manageLaunchUploaderButton();
        }
    };

    this.checkVisitAndSessionType = function () {
        if (this.projectID != undefined && this.projectID.indexOf("DIAN") == 0) {
            this.sessionTypeSpan.style.display = '';
            this.sessionTypeSelect.disabled = false;
            this.visitSpan.style.display = '';
            this.visitSelect.options.length = 1;
            this.visitSelect.options[0] = new Option("(Select a subject)", "");
            this.visitSelect.options[0].style.color = "black";
            this.visitSelect.disabled = true;
        } else {
            this.sessionTypeSpan.style.display = 'none';
            this.sessionTypeSelect.disabled = true;
            this.visitSpan.style.display = 'none';
            this.visitSelect.disabled = true;
        }
    };

    this.loadSubjects = function () {
        try {
            var subjCallback = {
                success:function (o) {
                    try {
                        o.argument.subjectResultSet = eval("(" + o.responseText + ")");
                        //noinspection JSUnresolvedVariable
                        o.argument.subjectResultSet.ResultSet.Result.sort(function (a, b) {
                            if (a["label"] < b["label"]) {
                                return -1;
                            } else if (b["label"] < a["label"]) {
                                return 1;
                            } else {
                                return 0;
                            }
                        });

                    } catch (e) {
                        o.argument.displayError("ERROR " + o.status + ": Failed to parse subject list.");
                    }
                    try {
                        o.argument.renderSubjects();
                    } catch (e) {
                        o.argument.displayError("ERROR : Failed to render subject list.");
                    }
                },
                failure:function () {
                    alert("Failed to load subjects.")
                },
                argument:this
            };

            if (this.subjectBox != undefined) {
                this.subjectBox.disabled = true;

                while (this.subjectBox.length > 0) {
                    this.subjectBox.remove(0);
                }
            }

            //noinspection JSUnresolvedVariable
            YAHOO.util.Connect.asyncRequest('GET', serverRoot + '/data/projects/' + this.projectID + '/subjects?format=json&timestamp=' + (new Date()).getTime(), subjCallback);
        } catch (e) {
            alert('failed to load subjects');
        }
    };

    this.renderSubjects = function () {
        this.subjectBox = document.getElementById("part_id");
        this.subjectBox.options[0] = new Option("SELECT", "");
        this.subjectBox.options[0].style.color = "black";

        //noinspection JSUnresolvedVariable
        if (this.subjectResultSet.ResultSet.Result.length == 0) {
            this.showMessage('No subjects found', 'The selected project has no subjects recorded yet. You should create a subject with which to associate uploaded session data.', true);
            return;
        }

        var matched = false;
        //noinspection JSUnresolvedVariable
        for (var sC = 0; sC < this.subjectResultSet.ResultSet.Result.length; sC++) {
            //noinspection JSUnresolvedVariable
            var result = this.subjectResultSet.ResultSet.Result[sC];
            var defaultSelected = !!(result.ID == this.defaultSubject || result["label"] == this.defaultSubject);
            if (defaultSelected) {
                matched = true;
            }
            var _label = result["label"];
            _label = (_label == undefined || _label == "") ? result.ID : _label;
            this.subjectBox.options[sC + 1] = new Option(_label, result.ID, defaultSelected, defaultSelected);
            this.subjectBox.options[sC + 1].style.color = "black";
            if (defaultSelected) {
                this.subjectBox.selectedIndex = (this.subjectBox.options.length - 1);
                this.subjectID = this.subjectBox.options[this.subjectBox.selectedIndex].value;
                if (this.projectID.indexOf("DIAN") == 0) {
                    this.loadVisits();
                }
            }
        }

        this.subjectBox.disabled = false;

        if (!matched && (this.defaultSubject != "NULL" && this.defaultSubject != "null" && this.defaultSubject != "" && this.defaultSubject != null)) {
            this.subjectBox.options[sC + 1] = new Option(this.defaultSubject, this.defaultSubject, true, true);
            this.subjectBox.options[sC + 1].newValue = true;
            this.subjectBox.options[sC + 1].style.color = "red";
            this.subjectBox.selectedIndex = (this.subjectBox.options.length - 1);
            if (YAHOO.env.ua.gecko > 0) {
                this.subjectBox.style.color = "red";
            }
        }

        this.subjectBox.manager = this;
        this.subjectBox.onchange = function () {
            if (YAHOO.env.ua.gecko > 0) {
                this.style.color = this.options[this.selectedIndex].style.color;
            }
            if (this.selectedIndex > 0) {
                this.manager.subjectID = this.options[this.selectedIndex].value;
                if (this.manager.projectID.indexOf("DIAN") == 0) {
                    this.manager.loadVisits();
                }
            }
            document.getElementById("session_date").value = '';
            this.manager.manageLaunchUploaderButton();
        }
    };

    this.loadVisits = function () {
        try {
            var visitCallback = {
                success:function (o) {
                    try {
                        o.argument.visitResultSet = eval("(" + o.responseText + ")");
                    } catch (e) {
                        o.argument.displayError("ERROR " + o.status + ": Failed to parse visit list.");
                    }
                    try {
                        o.argument.renderVisits();
                    } catch (e) {
                        o.argument.displayError("ERROR : Failed to render visit list.");
                    }
                },
                failure:function (o) {
                    alert("Failed to load visits: " + o);
                },
                argument:this
            };

            var visitList = document.getElementById('visit_id');
            if (visitList != undefined) {
                visitList.disabled = true;

                while (visitList.length > 0) {
                    visitList.remove(0);
                }
            }

            //noinspection JSUnresolvedVariable
            var visitUri = serverRoot + '/data/services/protocols/project/' + this.projectID + '/subject/' + this.subjectID + '/visits?format=json&timestamp=' + (new Date()).getTime();
            YAHOO.util.Connect.asyncRequest('GET', visitUri, visitCallback);
        } catch (e) {
            alert('Failed to load visits: ' + e.status);
        }
    };

    this.renderVisits = function () {
        this.sessionTypeSpan.style.display = '';
        this.visitSpan.style.display = '';

        this.visitSelect.options[0] = new Option("SELECT", "");
        this.visitSelect.options[0].style.color = "black";

        //noinspection JSUnresolvedVariable
        var result = this.visitResultSet.ResultSet.Result;
        for (var sC = 0; sC < result["available"].length; sC++) {
            var label = result["available"][sC];
            this.visitSelect.options[sC + 1] = new Option(label, label);
            this.visitSelect.options[sC + 1].style.color = "black";
        }
        this.visitSelect.selectedIndex = 0;

        // Copy the visit results into the global visits array as an associative array.
        visits = new Array();
        var visitData = result["visit_data"];
        if (!visitData || visitData.length == 0) {
            this.showMessage('No visits found', 'The selected subject has no visits recorded yet. You should create a visit entry.', true);
        } else {
            for (var i = 0; i < visitData.length; i++) {
                var visit = visitData[i];
                visits[visit['visit_id']] = visit;
            }
        }

        this.visitSelect.disabled = false;
        this.visitSelect.manager = this;
        this.visitSelect.onchange = function () {
            if (YAHOO.env.ua.gecko > 0) {
                this.style.color = this.options[this.selectedIndex].style.color;
            }
            this.manager.manageLaunchUploaderButton();
        };
        this.sessionTypeSelect.disabled = false;
        this.sessionTypeSelect.manager = this;
        this.sessionTypeSelect.onchange = function () {
            if (YAHOO.env.ua.gecko > 0) {
                this.style.color = this.options[this.selectedIndex].style.color;
            }
            this.manager.manageLaunchUploaderButton();
        }
    };

    /**
     * This function takes an activate parameter. If the value of that parameter is null, the function will run through
     * a series of validity test of the criteria currently set in the input form. If the criteria is valid, the launch
     * uploader button will be activated. Otherwise, if the value of the activate parameter is set to a boolean, the
     * button is activated if the value is true, and deactivated otherwise.
     * @param activate Indicates whether to activate the launch uploader button. Set to null to force validity tests.
     */
    this.manageLaunchUploaderButton = function (activate) {
        var button = document.getElementById("gojuice");
        if (button) {
            if (activate == null) {
                activate = this.validateVisitCriteria();
            }
            if (activate && this.projectBox.value.indexOf("DIAN") == 0) {
                this.initializeSessionId();
            } else {
                button.disabled = !activate;
            }
        }
    };

    this.initializeSessionId = function () {
        var project = this.projectBox.value;
        if (project != undefined && project.indexOf("DIAN") == 0) {
            var launchCallback = {
                success:function (o) {
                    try {
                        o.argument.subjectResultSet = eval("(" + o.responseText + ")");
                        //noinspection JSUnresolvedVariable
                        document.getElementById('session_id').value = o.argument.subjectResultSet.sessionId;
                        document.getElementById('gojuice').disabled = false;
                    } catch (e) {
                        o.argument.displayError("ERROR " + o.status + ": Failed to initialize session ID according to protocol.");
                    }
                },
                failure:function (o) {
                    alert('Encountered an error trying to initialize session ID according to project protocol: ' + o)
                },
                argument:this
            };

            var parameters = '{"visitId":"' + this.visitSelect.value + '","sessionType":"' + this.sessionTypeSelect.value + '"}';
            //noinspection JSUnresolvedVariable
            YAHOO.util.Connect.asyncRequest('POST', serverRoot + '/data/services/protocols/project/' + this.projectBox.value + '/subject/' + this.subjectBox.value + '/generate/sessionId?XNAT_CSRF='+csrfToken, launchCallback, parameters);
        } else {
            document.getElementById('gojuice').disabled = false;
        }
    };

    /**
     * Everything in this handler is hard-coded because it's a hassle to get the submitted parameter values from the
     * containing object when the function is called as a handler from the calendar selectEvent. This should be fixed
     * later, but requires adding new members to the calendar control, I think.
     */
    this.validateVisitCriteria = function () {
        var sessionDate = document.getElementById("session_date").value;
        var noSessionDate = document.getElementById("no_session_date").checked;

        // If any of these are true, what project this is doesn't matter.
        if (this.projectBox.selectedIndex == 0 || this.subjectBox.selectedIndex == 0 || (!sessionDate && !noSessionDate)) {
            return false;
        }
        if (this.projectBox.value.indexOf("DIAN") == 0) {
            // We only do any of the other DIAN validation if a visit is selected.
            if (this.visitSelect.selectedIndex == 0) {
                return false;
            }

            // Get the selected visit and compare it to the date.
            var visit = visits[this.visitSelect.value];
            if (!this.isValidVisit(sessionDate, visit)) {
                // If that fails, go ahead and fail without checking the visit/session compatibility.
                return false;
            }

            if (this.sessionTypeSelect.selectedIndex == 0) {
                return false;
            }

            var sessionType = this.sessionTypeSelect.value;

            // For valid session check, we need a selected session type and visit.
            return this.isValidSession(sessionType, visit);
        } else {
            return true;
        }
    };

    this.isValidVisit = function (submittedDate, visit) {
        if (visit == null) {
            return this.userAcceptsNoVisitState();
        }
        return this.isValidDateRangeForVisit(submittedDate, visit['date']);
    };

    this.userAcceptsNoVisitState = function () {
        return this.showMessage('No visit available', 'There is no visit recorded for that particular visit ID. Are you sure you want to upload this session to the indicated visit?');
    };

    this.isValidDateRangeForVisit = function (submittedDate, comparableDate) {
        var submittedDateObj = this.convertDate(submittedDate);
        var comparableDateObj = this.convertDate(comparableDate);
        if (submittedDateObj.getTime() < comparableDateObj.getTime()) {
            return this.showMessage('Invalid date', 'The experiment date is before the visit date. Are you sure you want to upload this session to this visit?');
        } else {
            var elapsedDays = Math.floor((submittedDateObj.getTime() - comparableDateObj.getTime()) / (oneDay));
            if (elapsedDays > 90) {
                return this.showMessage('Over visit limit', 'Too much time has passed since the visit date! Are you sure you want to upload this session to this visit?');
            }
        }
        return true;
    };

    this.convertDate = function (dateText) {
        var splitChar;
        if (dateText.indexOf("-") >= 0) {
            splitChar = "-";
        } else
        if (dateText.indexOf("/") >= 0) {
            splitChar = "/";
        }
        var dateArray = dateText.split(splitChar);
        var date = new Date();
        var year;
        var month;
        var day;
        // If the year is first, we assume yyyy-mm-dd. If not, we assume mm-dd-yyyy. This should be
        // localized, but for now is being handled only for U.S. date (yyyy-mm-dd is PostgreSQL format).
        if (dateArray[0] > 1900) {
            year = dateArray[0];
            month = dateArray[1] - 1;
            day = dateArray[2];
        } else {
            month = dateArray[0] - 1;
            day = dateArray[1];
            year = dateArray[2];
        }
        date.setFullYear(year, month, day);
        return date;
    };

    this.isValidSession = function (sessionType, visit) {
        if (!sessionType || !visit) {
            return false;
        }

        var sessions = visit['sessions'];
        if (sessions == null || sessions.length == 0 || sessions.indexOf(sessionType) == -1) {
            return true;
        }
        this.showMessage("Session already exists", "A session of the indicated type already exists for this visit. You can only add one session each of type MR, PET (FDG tracer), and PET (PIB tracer).", true);
        return false;
    };

    this.showMessage = function (title, message, confirmOnly) {
        var buttonArray;
        if (!confirmOnly) {
             buttonArray = [{ text:'Yes', handler:handleYes, isDefault:true },
                 { text:'No', handler:handleNo }];
        } else {
            buttonArray = [{ text:'OK', handler:handleOK, isDefault:true }];
        }
        var dialog = new YAHOO.widget.SimpleDialog("dialog", {
            width:"20em",
            close:false,
            fixedcenter:true,
            constraintoviewport:true,
            modal:true,
            icon:YAHOO.widget.SimpleDialog.ICON_WARN,
            visible:true,
            draggable:false,
            buttons: buttonArray
        });

        dialog.manager = this;
        dialog.render(document.getElementById('layout_content'));
        dialog.setHeader(title);
        dialog.setBody(message);
        dialog.bringToTop();
        dialog.show();

        function handleYes() {
            this.hide();
            this.manager.manageLaunchUploaderButton(true);
        }

        function handleNo() {
            this.hide();
            this.manager.manageLaunchUploaderButton(false);
        }

        function handleOK() {
            this.hide();
        }
    }
}
