/*
 * D:/Development/XNAT/1.6/xnat_builder_1_6dev/plugin-resources/webapp/xnat/scripts/experiments/recentExptList.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 11/18/13 9:35 AM
 */
function MinExptList(_div, _options){
  this.options=_options;
  this.div=_div;
  
  if(this.options==undefined){
  	this.options={};
  	this.options.recent=true;
  }
  
	this.init=function(){
		this.initLoader=prependLoader(this.div,"Loading recent data");
		this.initLoader.render();
		//load from search xml from server
		this.initCallback = {
            success: this.completeInit,
            failure: this.initFailure,
            cache: false, // Turn off caching for IE
            scope: this
        };
		
		var params="";
		
		if(this.options.recent!=undefined){
			params += "&recent=true";
		}
		
		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/experiments?XNAT_CSRF=' + window.csrfToken + '&format=json' + params,this.initCallback,null,this);
	};
	
	this.initFailure=function(o){
        if (!window.leaving) {
            this.displayError("ERROR " + o.status+ ": Failed to load experiment list.");
        }
        this.initLoader.close();
    };
	
	this.completeInit=function(o){
		try{
		    this.exptResultSet= eval("(" + o.responseText +")");
		}catch(e){
			this.displayError("ERROR " + o.status+ ": Failed to parse experiment list.");
		}
		this.initLoader.close();
		try{
		    this.render();
		}catch(e){
			this.displayError("ERROR : Failed to render experiment list.");
		}
	};
	
	this.displayError=function(errorMsg){
        xModalMessage('Experiment List Error', errorMsg);
	};
	
	this.render=function(){
		var display=document.getElementById(this.div);
		var t = document.createElement("table");
		t.width="100%";
		t.cellSpacing="0px";
		var tb = document.createElement("tbody");
		
		for(var eC=0;eC<this.exptResultSet.ResultSet.Result.length;eC++){
			var e=this.exptResultSet.ResultSet.Result[eC];
			
			var tr = document.createElement("tr");
			
			if(eC%2==0){
			  tr.className="even";
			}else{
			  tr.className="odd";
			}
			
			var td = document.createElement("td");
			td.align="left";
			if(e.project.length>10){
			   td.innerHTML="<a title='" + e.project + "' href='" + serverRoot + "/REST/projects/" + e.project + "?format=html'>" + e.project.substring(0,7) + "...</a>";
			}else{
			   td.innerHTML="<a href='" + serverRoot + "/REST/projects/" + e.project + "?format=html'>" + e.project + "</a>";
			}
			tr.appendChild(td);
			
			td = document.createElement("td");
			td.align="left";
			td.innerHTML=e.type_desc;
			tr.appendChild(td);
			
			td = document.createElement("td");
			td.align="left";
			
			if(e.label==""){
				var tempLabel=e.id;
			}else{
				var tempLabel=e.label;
			}
			
			var labelLink="<a";
			labelLink+=" href='"+ serverRoot + "/app/action/DisplayItemAction/search_element/" + e.element_name + "/search_field/" + e.element_name + ".ID/search_value/" + e.id + "/project/" + e.project + "'";
			
			if(tempLabel.length>18){
				labelLink+=" title='" + tempLabel + "'>"+tempLabel.substring(0,15) + "...";
			}else{
				labelLink+=">"+tempLabel;
			}
			labelLink+="</a>";
			
			td.innerHTML=labelLink;
			
			tr.appendChild(td);
			
			td = document.createElement("TD");
			if(e.workflow_date !="" && e.pipeline_name!=""){
        		if(e.workflow_status=="Complete"){
        			td.innerHTML="";
        		}else if(e.workflow_status=="Failed"){
        			td.innerHTML="<img src='"+ serverRoot +"/images/icon-alert-9px.png' title='Failed'/>";
        		}else if(e.workflow_status=="Queued"){
        			td.innerHTML="<img src='"+ serverRoot +"/images/icon-queued-9px.png' title='Queued'/>";
        		}else{
        			td.innerHTML="<img src='"+ serverRoot +"/images/icon-waiting-9px.gif' title='" + e.workflow_status + "'/>";
        		}
			}else{
    			td.innerHTML="";
			}
            tr.appendChild(td);
			
			td = document.createElement("td");
			td.align="right";
			
        	if(e.workflow_date !="" && e.pipeline_name!=""){
        		var tdTmp="<A class='recentDataActivity' title='" + e.pipeline_name;
        		if(e.workflow_status!="Complete"){
        			tdTmp+=" \"" + e.workflow_status + "\"";
        		}
        		tdTmp+=" at " + e.workflow_date + "'>" +e.pipeline_name.replace('_',' ')+ "<a>";
        		td.innerHTML=tdTmp;
        	}else if(e.last_modified!=""){
                td.innerHTML="<A class='recentDataActivity' title='Modified at " + e.last_modified + "'>Modified<a>";
        	}else if(e.insert_date!=""){
                td.innerHTML="<A class='recentDataActivity' title='Created at " + e.insert_date + "'>Created<a>";
        	}else{
                td.innerHTML="<span class='recentDataActivity'>Created</span>";
        	}
        	
            tr.appendChild(td);
			tb.appendChild(tr);
			
//			tr.extension=eC+"_rExpt_tr";
//			tr.onclick=function(){
//				var extension=document.getElementById(this.extension);
//				extension.style.display=(extension.style.display=="none")?"":"none";
//			}
//			tr.style.cursor="pointer";
			
			tr= document.createElement("tr");
			tr.id=eC+"_rExpt_tr";
			tr.style.display="none";
			if(eC%2==0){
			  tr.className="even";
			}else{
			  tr.className="odd";
			}
			td = document.createElement("td");
			td.colSpan="4";
			td.innerHTML="&nbsp;";
			
			tr.appendChild(td);
			tb.appendChild(tr);
		}
		t.appendChild(tb);
		display.appendChild(t);

	}
}

function prependLoader(div_id, msg) {
    var div;
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
