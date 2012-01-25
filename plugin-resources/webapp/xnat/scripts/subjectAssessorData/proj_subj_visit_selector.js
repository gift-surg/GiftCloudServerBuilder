/**
 * This is being created in global scope so that it can be used by the callback function for the YUI calendar control.
 */
var visits = new Array("1", "2", "3");

/**
 * @param _proj_select
 * @param _subj_select
 * @param _submit_button
 * @param _defaultProject
 * @param _defaultSubject
 */
function ProjectSubjectVisitSelector(_proj_select, _subj_select, _submit_button, _defaultProject, _defaultSubject) {
    this.projectSelect = _proj_select;
    this.subjSelect = _subj_select;
    this.submitButton = _submit_button;
    this.defaultProject = _defaultProject;
    this.defaultSubject = _defaultSubject;

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

        YAHOO.util.Connect.asyncRequest('GET', serverRoot + '/REST/projects?format=json&timestamp=' + (new Date()).getTime() + params, this.initCallback, null, this);
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
        } catch (e) {
            this.displayError("ERROR : Failed to render project list: " + e.toString());
        }
    };

    this.renderProjects = function () {
        if (this.projectResultSet.ResultSet.Result.length == 0) {

        } else {
            this.projectBox = document.getElementById(this.projectSelect);
            this.projectBox.options[0] = new Option("SELECT", "");

            for (var pC = 0; pC < this.projectResultSet.ResultSet.Result.length; pC++) {
                var defaultSelected = !!(this.projectResultSet.ResultSet.Result[pC].id == this.defaultProject);
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
                    this.manager.checkVisitAndScanType();
                }
            };

            if (this.projectBox.selectedIndex > 0) {
                this.projectBox.onchange();
            }
        }

        this.scanDateCalendar = document.getElementById("cal_scan_date");
        this.scanDateCalendar.manager = this;
        this.scanDateCalendar.calendar.selectEvent.subscribe(this.checkSubmitButton, this.scanDateCalendar, false);
    };

    this.checkVisitAndScanType = function () {
        var scanTypeSpan = document.getElementById('scan_type_entry');
        var scanTypeSelect = document.getElementById('scan_type');
        var visitSpan = document.getElementById('visit_entry');
        var visitSelect = document.getElementById('visit_id');
        var projectID = this.projectBox.options[this.projectBox.selectedIndex].value;
        if (projectID != undefined && projectID.indexOf("DIAN") == 0) {
            scanTypeSpan.style.display = '';
            scanTypeSelect.disabled = false;
            visitSpan.style.display = '';
            visitSelect.options.length = 1;
            visitSelect.options[0] = new Option("(Select a subject)", "");
            visitSelect.options[0].style.color = "black";
            visitSelect.disabled = true;
        } else {
            scanTypeSpan.style.display = 'none';
            scanTypeSelect.disabled = true;
            visitSpan.style.display = 'none';
            visitSelect.disabled = true;
        }
    };

    this.loadSubjects = function () {
        try {
            var subjCallback = {
                success:function (o) {
                    try {
                        o.argument.subjectResultSet = eval("(" + o.responseText + ")");
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

            YAHOO.util.Connect.asyncRequest('GET', serverRoot + '/REST/projects/' + this.projectID + '/subjects?format=json&timestamp=' + (new Date()).getTime(), subjCallback);
        } catch (e) {
            alert('failed to load subjects');
        }
    };

    this.renderSubjects = function () {
        this.subjectBox = document.getElementById(this.subjSelect);
        this.subjectBox.options[0] = new Option("SELECT", "");
        this.subjectBox.options[0].style.color = "black";

        var matched = false;
        for (var sC = 0; sC < this.subjectResultSet.ResultSet.Result.length; sC++) {
            var defaultSelected = !!(this.subjectResultSet.ResultSet.Result[sC].ID == this.defaultSubject || this.subjectResultSet.ResultSet.Result[sC]["label"] == this.defaultSubject);
            if (defaultSelected)matched = true;
            var _label = this.subjectResultSet.ResultSet.Result[sC]["label"];
            _label = (_label == undefined || _label == "") ? this.subjectResultSet.ResultSet.Result[sC].ID : _label;
            var opt = new Option(_label, this.subjectResultSet.ResultSet.Result[sC].ID, defaultSelected, defaultSelected);
            this.subjectBox.options[sC + 1] = opt;
            this.subjectBox.options[sC + 1].style.color = "black";
            if (defaultSelected) {
                this.subjectBox.selectedIndex = (this.subjectBox.options.length - 1);
            }
        }

        this.subjectBox.disabled = false;

        if (!matched && (this.defaultSubject != "NULL" && this.defaultSubject != "null" && this.defaultSubject != "" && this.defaultSubject != null)) {
            var opt = new Option(this.defaultSubject, this.defaultSubject, true, true);
            this.subjectBox.options[sC + 1] = opt;
            this.subjectBox.options[sC + 1].newValue = true;
            this.subjectBox.options[sC + 1].style.color = "red";
            this.subjectBox.selectedIndex = (this.subjectBox.options.length - 1);
            if (YAHOO.env.ua.gecko > 0) {
                this.subjectBox.style.color = "red";
            }
        }

        this.subjectBox.submitButton = this.submitButton;
        this.subjectBox.manager = this;
        if (eval("window.confirmValues") != undefined) {
            this.subjectBox.onchange = function () {
                if (YAHOO.env.ua.gecko > 0) {
                    this.style.color = this.options[this.selectedIndex].style.color;
                }
                confirmValues(false);
                if (this.selectedIndex > 0) {
                    this.manager.subjectID = this.options[this.selectedIndex].value;
                }
                this.manager.checkSubmitButton();
            }

            confirmValues(false);
            if (this.selectedIndex > 0) {
                this.manager.subjectID = this.options[this.selectedIndex].value;
            }
            this.manager.checkSubmitButton();
        } else {
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
                this.manager.checkSubmitButton();
            }
        }
    }

    this.loadVisits = function (o) {
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
            }

            if (this.visitBox != undefined) {
                this.visitBox.disabled = true;

                while (this.visitBox.length > 0) {
                    this.visitBox.remove(0);
                }
            }

            YAHOO.util.Connect.asyncRequest('GET', serverRoot + '/REST/projects/' + this.projectID + '/subjects/' + this.subjectID + '/visits?format=json&timestamp=' + (new Date()).getTime(), visitCallback);
        } catch (e) {
            alert('failed to load visits');
        }
    }


    this.renderVisits = function (o) {
        document.getElementById('scan_type_entry').style.display = '';
        document.getElementById('visit_entry').style.display = '';
        this.visitBox = document.getElementById('visit_id');

        var defaultVisit = this.visitResultSet.ResultSet.Result["last_visit_id"];
        for (var sC = 0; sC < this.visitResultSet.ResultSet.Result["available"].length; sC++) {
            var label = this.visitResultSet.ResultSet.Result["available"][sC];
            var opt = new Option(label, label);
            this.visitBox.options[sC] = opt;
            this.visitBox.options[sC].style.color = "black";
            if (label == defaultVisit) {
                this.visitBox.selectedIndex = (this.visitBox.options.length - 1);
            }
        }

        // Copy the visit results into the global visits array as an associative array.
        visits = new Array();
        var visitData = this.visitResultSet.ResultSet.Result["visit_data"];
        for (var i = 0; i < visitData.length; i++) {
            var visit = visitData[i];
            visits[visit['visit_id']] = visit;
        }

        this.visitBox.disabled = false;

        this.visitBox.submitButton = this.submitButton;
        if (eval("window.confirmValues") != undefined) {
            this.visitBox.onchange = function () {
                if (YAHOO.env.ua.gecko > 0) {
                    this.style.color = this.options[this.selectedIndex].style.color;
                }
                confirmValues(false);
                this.manager.checkSubmitButton();
            }

            confirmValues(false);
            this.manager.checkSubmitButton();
        } else {
            this.visitBox.onchange = function () {
                if (YAHOO.env.ua.gecko > 0) {
                    this.style.color = this.options[this.selectedIndex].style.color;
                }
                this.manager.checkSubmitButton();
            }
        }
    };

    /**
     * Everything in this handler is hard-coded because it's a hassle to get the submitted parameter values from the
     * containing object when the function is called as a handler from the calendar selectEvent. This should be fixed
     * later, but requires adding new members to the calendar control, I think.
     */
    this.checkSubmitButton = function() {
        var button = document.getElementById("gojuice");
        if (button) {
            var projectBox = document.getElementById("project");
            var subjectBox = document.getElementById("part_id");
            var scanDateValue = document.getElementById("scan_date").value;

            // If any of these are true, what project this is doesn't matter.
            if (projectBox.selectedIndex == 0 || subjectBox.selectedIndex == 0 || !scanDateValue) {
                button.disabled = true;
                return;
            }
            if (projectBox.value.indexOf("DIAN") == 0) {
                var visitId = document.getElementById("visit_id").value;
                var visit = visits[visitId];
                button.disabled = !isValidDateRangeForVisit(scanDateValue, visit['date']);
            } else {
                button.disabled = false;
            }
        }
    }
}

var oneDay = 24*60*60*1000;

function isValidDateRangeForVisit(submittedDate, comparableDate) {
    var submittedDateObj = convertDate(submittedDate);
    var comparableDateObj = convertDate(comparableDate);
    if (submittedDateObj.getTime() < comparableDateObj.getTime()) {
        alert('The experiment date is before the visit date!');
        return false;
    } else {
        var elapsedDays = Math.floor((submittedDateObj.getTime() - comparableDateObj.getTime()) / (oneDay));
        if (elapsedDays > 90) {
            alert('Too much time has passed since the visit date!');
            return false;
        }
    }
    return true;
}

function convertDate(dateText) {
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
}

