/**
 * This is being created in global scope so that it can be used by the callback function for the YUI calendar control.
 */

var oneDay = 24 * 60 * 60 * 1000;

/**
 * @param _defaultProject
 * @param _defaultSubject
 */
function ProjectSubjectVisitSelector(_defaultProject, _defaultSubject) {
    this.defaultProject = _defaultProject;
    this.defaultSubject = _defaultSubject;




    this.init = function () {

        //load from search xml from server
        this.initCallback = {
            success:this.completeInit,
            failure:this.initFailure,
            cache:false, // Turn off caching for IE
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

	this.displayError=function(errorMsg){
		alert(errorMsg);
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
                cache:false, // Turn off caching for IE
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
                
            }
            document.getElementById("session_date").value = '';
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
        	if(activate == null){
        		var sessionDate = document.getElementById("session_date").value;
        		var noSessionDate = document.getElementById("no_session_date").checked;

        		// If any of these are true, what project this is doesn't matter.
        		if (this.projectBox.selectedIndex == 0 || this.subjectBox.selectedIndex == 0 || (!sessionDate && !noSessionDate)) {
        			activate = false;
        		} else {
        			if(sessionDate && !noSessionDate){
            			//check date format to be sure it has a 4 digit year
            			if(!/^\d{1,2}\/\d{1,2}\/\d{4}$/.test(sessionDate)){
            			  activate = false;
            			  this.displayError("ERROR: Session Date must be in mm/dd/yyyy format."); 
            			} else {
            		      activate = true;
            			}
            		        
            		} else {	
        			  activate = true;
            		}
        		}
        		
        	}
            
            button.disabled = !activate;
            
        }
    };

    this.initializeSessionId = function () {
        var project = this.projectBox.value;
        
        document.getElementById('gojuice').disabled = false;
        
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
