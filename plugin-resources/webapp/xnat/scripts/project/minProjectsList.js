/*
 * D:/Development/XNAT/1.6/xnat_builder_1_6dev/plugin-resources/webapp/xnat/scripts/project/minProjectsList.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */
function MinProjectsList(_div, _options){

    this.options = _options;
    this.div     = _div;

    if ( this.options == undefined ){
        this.options = { accessible: true };
    }

	this.init = function(){

        this.initLoader = prependLoader(this.div, "Loading " + XNAT.app.displayNames.plural.project.toLowerCase());
		this.initLoader.render();
		
		//load from search xml from server
        this.initCallback = {
            success: this.completeInit,
            failure: this.initFailure,
            cache: false, // Turn off caching for IE
            scope: this
        };
		
		var params="";

        if (this.options.recent != undefined) {
            params += "&recent=true";
        }

        if (this.options.owner != undefined) {
            params += "&owner=true";
        }

        if (this.options.member != undefined) {
            params += "&member=true";
        }

        if (this.options.collaborator != undefined) {
            params += "&collaborator=true";
        }

        if (this.options.accessible != undefined) {
            params += "&accessible=true";
        }
		
		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/projects?format=json&stamp='+ (new Date()).getTime() + params,this.initCallback,null,this);

	};

        this.initFailure = function (o) {
            this.initLoader.close();
        };

        this.completeInit = function (o) {
            try {
                this.projectResultSet = eval("(" + o.responseText + ")");
            }
            catch (e) {
            }

            this.initLoader.close();

            try {
                this.render();
            }
            catch (e) {
            }
        };

	this.render=function(){

        var display = document.getElementById(this.div);
		display.innerHTML = "";

        //var items = [];
        var projects = this.projectResultSet.ResultSet.Result;
		
		window.sort_field = "last_accessed_" + this.projectResultSet.ResultSet.xdat_user_id;

        projects = projects.sort(function(a,b){
			if( a[window.sort_field] > b[window.sort_field] ) return -1;
			if( b[window.sort_field] > a[window.sort_field] ) return 1;
			return 0;
		});
		
		var projectsLength = projects.length;

        for ( var pC = 0; pC < projectsLength; pC++ ){

			var p = projects[pC];

            var project_name = p.name;

            // if there are no spaces in the first 42 characters, then chop it off
            if (project_name.length > 42 && project_name.substring(0,41).indexOf(' ') === -1){
                project_name = project_name.substring(0,39) + "&hellip;";
            }

			var newDisplay = document.createElement("div");
            newDisplay.title = p.name;
            newDisplay.className = ( pC%2 === 0 ) ? 'even' : 'odd';

			var row = document.createElement("div");
			row.innerHTML =
                '<h3>' +
                '<a href="' + serverRoot +
                    '/app/template/XDATScreen_report_xnat_projectData.vm' +
                    '/search_element/xnat:projectData' +
                    '/search_field/xnat:projectData.ID' +
                    '/search_value/' + p.id + '">' + project_name + '</a>' +
                    '</h3>';

            newDisplay.appendChild(row);
			
			row = document.createElement("div");
			row.innerHTML =
                "<b>" + XNAT.app.displayNames.singular.project + " ID: " + p.id +"</b>";

            if ( p.pi != undefined && p.pi != ""){
				row.innerHTML+="&nbsp;&nbsp;&nbsp;<b>PI: "+ p.pi +"</b>";
			}

            newDisplay.appendChild(row);
			
			row = document.createElement("div");
			if (p.description.length > 160){
				row.innerHTML = p.description.substring(0,157) + "&nbsp;&hellip;";
			}
            else {
				row.innerHTML = p.description;
			}
			newDisplay.appendChild(row);
			
			row=document.createElement("div");

            if(p["user_role_"+this.projectResultSet.ResultSet.xdat_user_id]==""){
				if(p.project_access && p.project_access.toLowerCase()=="public"){
					row.innerHTML="This is a <b>public</b> " + XNAT.app.displayNames.singular.project.toLowerCase() + "." + "<br/>"
					+ "<a href='" + serverRoot + "/app/template/RequestProjectAccess.vm/project/" + p.id + "'>Request write access</a> to this " + XNAT.app.displayNames.singular.project.toLowerCase() + ".";
				}else{
					row.innerHTML="<a href='" + serverRoot + "/app/template/RequestProjectAccess.vm/project/" + p.id + "'>Request access</a> to this " + XNAT.app.displayNames.singular.project.toLowerCase() + ".";
				}
			}else{
				if(p["user_role_"+this.projectResultSet.ResultSet.xdat_user_id]=="Owners"){
					row.innerHTML="You are an <b>owner</b> for this " + XNAT.app.displayNames.singular.project.toLowerCase() + ".";
				}else if(p["user_role_"+this.projectResultSet.ResultSet.xdat_user_id]=="Members"){
					row.innerHTML="You are a <b>member</b> for this " + XNAT.app.displayNames.singular.project.toLowerCase() + ".";
				}else if(p["user_role_"+this.projectResultSet.ResultSet.xdat_user_id]=="Collaborators"){
					row.innerHTML="You are a <b>collaborator</b> for this " + XNAT.app.displayNames.singular.project.toLowerCase() + ".";
				}
			}
			newDisplay.appendChild(row);
			
			display.appendChild(newDisplay);
		}
		//this.menu=new YAHOO.widget.Menu(this.div_id,{itemdata:items,visible:true, scrollincrement:5,position:"static"});
	};
}

function prependLoader(div_id,msg){
    if(div_id.id==undefined){
        var div=document.getElementById(div_id);
    }else{
        var div=div_id;
    }
    var loader_div = document.createElement("div");
    loader_div.innerHTML=msg;
    div.parentNode.insertBefore(loader_div,div);
    return new XNATLoadingGIF(loader_div);
}