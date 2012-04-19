function ProjectLoader(_options){
  this.options=_options;

  if(this.options==undefined){
  	this.options=new Object();
  	this.options.owner=true;
  	this.options.member=true;
  }

  this.onLoadComplete=new YAHOO.util.CustomEvent("load-complete",this);

	this.init=function(){

		//load from search xml from server
		this.initCallback={
			success:this.completeInit,
			failure:this.initFailure,
			scope:this
		}

		var params="";

		if(this.options.recent!=undefined){
			params += "&recent=true";
		}

		if(this.options.owner!=undefined){
			params += "&owner=true";
		}

		if(this.options.member!=undefined){
			params += "&member=true";
		}

		if(this.options.collaborator!=undefined){
			params += "&collaborator=true";
		}

		params += "&prearc_code=true";

		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/projects?XNAT_CSRF=' + window.csrfToken + '&format=json&timestamp=' + (new Date()).getTime()+ params,this.initCallback,null,this);
	};

	this.initFailure=function(o){
		this.displayError("ERROR " + o.status+ ": Failed to load project list.");
	};

	this.completeInit=function(o){
		try{
		    this.list= eval("(" + o.responseText +")").ResultSet.Result;

		    if(this.options.selects!=undefined){
		    	for(var selectC=0;selectC<this.options.selects.length;selectC++){
		    		var selectBox=this.options.selects[selectC];
		    		if(this.options.defaultValue!=undefined){
		    			renderProjects(selectBox,this.list,this.options.defaultValue);
		    		}else{
		    			renderProjects(selectBox,this.list,"");
		    		}
		    	}
		    }

			this.onLoadComplete.fire();
		}catch(e){
			this.displayError("ERROR " + o.status+ ": Failed to parse project list.");
		}

	};

	this.displayError=function(errorMsg){
		alert(errorMsg);
	}
}

function SubjectLoader(_options){
  this.onLoadComplete=new YAHOO.util.CustomEvent("load-complete",this);
  this.options=_options;

	this.load=function(_project,_options){
		if(_project!=undefined)
			this.project=_project;

		if(_options!=undefined)
			this.options=_options;

		//load from search xml from server
		this.initCallback={
			success:this.completeInit,
			failure:this.initFailure,
			scope:this
		}

		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/projects/' +_project +'/subjects?XNAT_CSRF=' + window.csrfToken + '&format=json&timestamp=' + (new Date()).getTime(),this.initCallback,null,this);
	};

	this.initFailure=function(o){
		this.displayError("ERROR " + o.status+ ": Failed to load subject list.");
	};

	this.completeInit=function(o){
		try{
		    this.list= eval("(" + o.responseText +")").ResultSet.Result;
			this.onLoadComplete.fire();

		    if(this.options != undefined && this.options.selects != undefined) {
		    	for(var selectC=0;selectC<this.options.selects.length;selectC++){
		    		var selectBox=this.options.selects[selectC];
		    		if(this.options.defaultValue!=undefined){
		    			renderSubjects(selectBox,this.list,this.options.defaultValue,this.project);
		    		}else{
		    			renderSubjects(selectBox,this.list,"",this.project);
		    		}
		    	}
		    }
		}catch(e){
			if (o.status != 200) {
			this.displayError("ERROR " + o.status+ ": Failed to parse subject list.");
            } else {
                this.displayError("EXCEPTION: " + e.toString());
            }
		}

	};

	this.displayError=function(errorMsg){
		alert(errorMsg);
	};
}

function ExptLoader(){
  this.onLoadComplete=new YAHOO.util.CustomEvent("load-complete",this);

	this.load=function(_project){
		//load from search xml from server
		this.initCallback={
			success:this.completeInit,
			failure:this.initFailure,
			scope:this
		}

		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/projects/' +_project +'/experiments?XNAT_CSRF=' + window.csrfToken + '&format=json&timestamp=' + (new Date()).getTime(),this.initCallback,null,this);
	};

	this.initFailure=function(o){
		this.displayError("ERROR " + o.status+ ": Failed to load experiment list.");
	};

	this.completeInit=function(o){
		try{
		    this.list= eval("(" + o.responseText +")").ResultSet.Result;
			this.onLoadComplete.fire();
		}catch(e){
			this.displayError("ERROR " + o.status+ ": Failed to parse experiment list.");
		}

	};

	this.displayError=function(errorMsg){
		alert(errorMsg);
	};
}

function renderProjects(selectBox,list,defaultValue){
	while(selectBox.length>0){
		selectBox.remove(0);
	}

	selectBox.options[0]=new Option("SELECT","");

	for(var pC=0;pC<list.length;pC++){
		var defaultSelected=(list[pC].id==defaultValue)?true:false;
		var opt=new Option(list[pC].secondary_id,list[pC].id,defaultSelected,defaultSelected);
		selectBox.options[pC+1]=opt;
		selectBox.options[pC+1].pc=list[pC].proj_prearchive_code;
		selectBox.options[pC+1].qc=list[pC].proj_quarantine;
		if(defaultSelected){
			selectBox.selectedIndex=(selectBox.options.length-1);
			var pc=document.getElementById("pc_0");
			if(pc!=undefined && pc!=null){
				if(list[pC].proj_prearchive_code=="4"){
					if(list[pC].proj_quarantine=="0"){
					  	document.getElementById("pc_2").click();
					}else{
					  	document.getElementById("pc_1").click();
					}
				}else{
					  	document.getElementById("pc_0").click();
				}
			}
		}
	}

	if(window.projectPostLoadDisabled){
		selectBox.disabled=window.projectPostLoadDisabled;
	}else{
	selectBox.disabled=false;
}

}

function renderSubjects(selectBox,list,defaultValue,projectID){
	while(selectBox.length>0){
		selectBox.remove(0);
	}

	if(projectID==undefined){
		projectID="";
	}

	selectBox.options[0]=new Option("SELECT","");

	for(var sC=0;sC<list.length;sC++){
		var _label=list[sC]["label"];
		var defaultSelected=(list[sC].ID==defaultValue || _label==defaultValue)?true:false;
		_label=(_label==undefined || _label=="")?list[sC].ID:_label;
		var opt=new Option(_label,list[sC].ID,defaultSelected,defaultSelected);
		selectBox.options[sC+1]=opt;
		if(defaultSelected){
			selectBox.selectedIndex=(selectBox.options.length-1);
		}
	}

	selectBox.disabled=false;
}

function ProjectEditor(_config){
	this.config=_config;

    this.onModification=new YAHOO.util.CustomEvent("modification",this);

	this.render=function(){
      if(this.panel==undefined){
		this.panel=new YAHOO.widget.Dialog("projectDialog",{close:true,
		   width:"350px",height:"100px",underlay:"shadow",modal:true,fixedcenter:true,visible:false});
		this.panel.setHeader("Project modification");

		var bd = document.createElement("form");

		var table = document.createElement("table");
		var tb = document.createElement("tbody");
		table.appendChild(tb);
		bd.appendChild(table);

		//modality
		tr=document.createElement("tr");
		td1=document.createElement("th");
		td2=document.createElement("td");

		td1.innerHTML="Project:";
		td1.align="left";
		this.selectBox = document.createElement("select");
		this.selectBox.id="new_project";
		this.selectBox.name="new_project";
		td2.appendChild(this.selectBox);
		tr.appendChild(td1);
		tr.appendChild(td2);
		tb.appendChild(tr);

		this.panel.setBody(bd);

		this.panel.form=bd;

		this.panel.selector=this;
		var buttons=[{text:"Modify",handler:{fn:function(){
				this.selector.new_project = this.form.new_project.options[this.form.new_project.selectedIndex].value;
				this.selector.new_project_name = this.form.new_project.options[this.form.new_project.selectedIndex].text;

				if(this.selector.new_project==window.currentProject){
				    alert("No project modification found.");
				    this.cancel();
				}else if(this.form.new_project.selectedValue==0){
				    alert("Please select a project");
				}else{
					var settingsCallback={
				            	success:function(o){
				                    window.currentProject=this.selector.new_project;
				        	        closeModalPanel("modify_project");
				        	        this.selector.onModification.fire();
				        	        this.cancel();
				            	},
					            failure:function(o){
				                    alert("ERROR (" +o.status +"): Failed to modify project.");
				        	        closeModalPanel("modify_project");
				            	},
				            	scope:this
					   }

					   if(this.selector.config.uri==undefined){
				                    window.currentProject=this.selector.new_project;
				        	        closeModalPanel("modify_project");
				        	        this.selector.onModification.fire();
				        	        this.cancel();
					   }else{
						  if(confirm("Modifying the primary project of an imaging session will result in the moving of files on the file server into the new project's storage space.  Are you sure you want to make this change?")){
						       openModalPanel("modify_project","Modifying project, please wait...");

				               YAHOO.util.Connect.asyncRequest('PUT',this.selector.config.uri + "/projects/" + this.selector.new_project + "?primary=true&format=json&XNAT_CSRF=" + csrfToken,settingsCallback);
					      }else{
					    	   this.cancel();
					      }
					   }
				}
			}},isDefault:true},
			{text:"Cancel",handler:{fn:function(){
				this.cancel();
			}}}];
		this.panel.cfg.queueProperty("buttons",buttons);
		this.panel.render("page_body");

		this.panel.show();

		if(window.projectLoader==undefined)
		{
			window.projectLoader=new ProjectLoader({selects:[this.selectBox],defaultValue:window.currentProject,member:true,owner:true});
			openModalPanel("projects_loading","Loading projects...");
			window.projectLoader.onLoadComplete.subscribe(function(obj){
				closeModalPanel("projects_loading");
			})
			window.projectLoader.init();
		}
      }
	}
}

function SubjectEditor(_config){
	this.config=_config;

    this.onModification=new YAHOO.util.CustomEvent("modification",this);

	this.render=function(){
      if(this.panel==undefined){
		this.panel=new YAHOO.widget.Dialog("subjectDialog",{close:true,
		   width:"350px",height:"100px",underlay:"shadow",modal:true,fixedcenter:true,visible:false});
		this.panel.setHeader("Subject modification");

		var bd = document.createElement("form");

		var table = document.createElement("table");
		var tb = document.createElement("tbody");
		table.appendChild(tb);
		bd.appendChild(table);

		//modality
		tr=document.createElement("tr");
		td1=document.createElement("th");
		td2=document.createElement("td");
		td3=document.createElement("td");

		td1.innerHTML="Subject:";
		td1.align="left";

		window.subjectBox = document.createElement("select");
		window.subjectBox.id="new_subject";
		window.subjectBox.name="new_subject";
		td2.appendChild(window.subjectBox);

		tr.appendChild(td1);
		tr.appendChild(td2);

		if(this.config.create_subject_link){
			td3.appendChild(document.createTextNode("Or, "));
			this.chs=document.createElement("input");
			this.chs.id="create_subject_button";
			this.chs.type="button";
			this.chs.value="CREATE SUBJECT";
			this.chs.project=window.currentProject;
			this.chs.create_subject_link=this.config.create_subject_link;
			this.chs.onclick=function ()
		          {
		             if(this.project!=undefined){
			  	   	     if (window.subjectForm!=undefined){
	  	     	            window.subjectForm.close();
	  	     	            window.subjectForm=null;
		   	  	         }

		   	 	         window.subjectForm=window.open(this.create_subject_link, '','width=500,height=550,status=yes,resizable=yes,scrollbars=yes,toolbar=no');
		   	             	if (window.subjectForm.opener == null) window.subjectForm.opener = self;
					  	              return window.subjectForm;
			          }else{
			                alert("Please select a project.");
			          }
		          }
			td3.appendChild(this.chs);
		    tr.appendChild(td3);
		}

		tb.appendChild(tr);

		this.panel.setBody(bd);

		this.panel.form=bd;

		this.panel.selector=this;
		var buttons=[{text:"Modify",handler:{fn:function(){
				this.selector.new_subject = this.form.new_subject.options[this.form.new_subject.selectedIndex].value;
				this.selector.new_subject_name = this.form.new_subject.options[this.form.new_subject.selectedIndex].text;

				if(this.selector.new_subject==window.currentSubject){
				    alert("No subject modification found.");
				    this.cancel();
				}else if(this.form.new_subject.selectedValue==0){
				    alert("Please select a subject");
				}else{
					var settingsCallback={
				            	success:function(o){
				                    window.currentSubject=this.selector.new_subject;
				        	        closeModalPanel("modify_subject");
				        	        this.selector.onModification.fire();
				        	        this.cancel();
				            	},
					            failure:function(o){
				                    alert("ERROR (" +o.status +"): Failed to modify subject.");
				        	        closeModalPanel("modify_subject");
				            	},
				            	scope:this
					   }

					  if(this.selector.config.uri==undefined){
				                    window.currentSubject=this.selector.new_subject;
				        	        closeModalPanel("modify_subject");
				        	        this.selector.onModification.fire();
				        	        this.cancel();
					  }else{
						  if(confirm("Modifying the subject of an experiment may result in the moving of files on the file server into the new subject's storage space.  Are you sure you want to make this change?")){
						       openModalPanel("modify_subject","Modifying subject, please wait...");

				               YAHOO.util.Connect.asyncRequest('PUT',serverRoot +"/REST/projects/" + window.currentProject +"/subjects/" + this.selector.new_subject + "/experiments/" + window.currentLabe + "?format=json&XNAT_CSRF=" + csrfToken,settingsCallback);
					      }else{
					    	   this.cancel();
					      }
					  }
				}
			}},isDefault:true},
			{text:"Cancel",handler:{fn:function(){
				this.cancel();
			}}}];
		this.panel.cfg.queueProperty("buttons",buttons);
		this.panel.render("page_body");

		this.panel.show();

		if(window.subjectLoader==undefined)
		{
			window.subjectLoader=new SubjectLoader({selects:[window.subjectBox],defaultValue:window.currentSubject});


		}
      }

      this.refresh=function(){
	      window.subjectLoader.onLoadComplete.subscribe(function(obj){
				closeModalPanel("subjects_loading");
			});

	      openModalPanel("subjects_loading","Loading subject...");
	      window.subjectLoader.load(window.currentProject,{selects:[window.subjectBox],defaultValue:window.currentSubject});
      }

      this.refresh();
	}
}

function LabelEditor(_config){
	this.config=_config;

	if(this.config.header==undefined){
		this.config.header="Session";
	}

    this.onModification=new YAHOO.util.CustomEvent("modification",this);

	this.render=function(){
      if(this.panel==undefined){
		this.panel=new YAHOO.widget.Dialog("labelDialog",{close:true,
		   width:"350px",height:"100px",underlay:"shadow",modal:true,fixedcenter:true,visible:false});
		this.panel.setHeader("Label modification");

		var bd = document.createElement("form");

		var table = document.createElement("table");
		var tb = document.createElement("tbody");
		table.appendChild(tb);
		bd.appendChild(table);

		//modality
		tr=document.createElement("tr");
		td1=document.createElement("th");
		td2=document.createElement("td");
		td3=document.createElement("td");

		td1.innerHTML=this.config.header +":";
		td1.align="left";

		window.labelInput = document.createElement("input");
		window.labelInput.id="new_label";
		window.labelInput.value=window.currentLabel;
		window.labelInput.name="new_label";

		this.labelContainer=document.createElement("div");
		this.labelContainer.id="complete_container";
		this.labelContainer.width="100px";
		td2.appendChild(this.labelContainer);

		this.labelContainer.appendChild(window.labelInput);

		window.labelToggler=document.createElement("span");
		window.labelToggler.id="toggleLabels";

		this.labelContainer.appendChild(window.labelToggler);

		this.label_auto=document.createElement("div");
		this.label_auto.id="label_auto";
		this.labelContainer.appendChild(this.label_auto);

		tr.appendChild(td1);
		tr.appendChild(td2);
		tb.appendChild(tr);

		  var oPushButtonD = new YAHOO.widget.Button({container:window.labelToggler});
		  window.labelToggler.style.display="none";

		this.panel.setBody(bd);

		this.panel.form=bd;

		this.panel.selector=this;
		var buttons=[{text:"Modify",handler:{fn:function(){
				var label = this.form.new_label;
				  window.selectedLabel=label.value.trim();
				  if(window.selectedLabel==""){
				    alert("Please specify a new " + this.selector.config.header + ".");
				  }else if(window.selectedLabel==window.currentLabel){
				    alert("No modification found.");
				  }else{
				    var validatedLabel=cleanLabel(window.selectedLabel);
				    if(validatedLabel!=window.selectedLabel){
				       label.value=validatedLabel;
				       alert("Invalid characters in new " + this.selector.config.header + ".  Review modified value and resubmit.");
				       label.focus();
				       return;
				    }

				    var matchedExisting=false;
				    for(var lC=0;lC<window.exptLoader.list.length;lC++){
				       if(window.selectedLabel==window.exptLoader.list[lC].label){
				          matchedExisting=true;
				          break;
				       }
				    }

				    if(matchedExisting){
				       alert("This " + this.selector.config.header + " is already in use in this project.  Please modify and resubmit.");
				       label.focus();
				       return;
				    }

				    	var settingsCallback={
			            	success:function(o){
			               		window.currentLabel=window.selectedLabel;
			        	        closeModalPanel("modify_new_label");
			        	        this.selector.onModification.fire();
			        	        this.cancel();
			            	},
				            failure:function(o){
			               		alert("ERROR (" +o.status +"): Failed to modify session ID.");
			        	        closeModalPanel("modify_new_label");
			            	},scope:this
				        }

				    if(this.selector.config.uri==undefined){
			               		window.currentLabel=window.selectedLabel;
			        	        closeModalPanel("modify_new_label");
			        	        this.selector.onModification.fire();
			        	        this.cancel();
					}else{
					    if(confirm("Modifying the " + this.selector.config.header + " of an imaging session will result in the moving of files on the file server within the project's storage space.  Are you sure you want to make this change?")){
							openModalPanel("modify_new_label","Modifying " + this.selector.config.header +", please wait...");

				        	YAHOO.util.Connect.asyncRequest('PUT',this.selector.config.uri +"?label=" + window.selectedLabel +"&format=json&XNAT_CSRF=" + csrfToken,settingsCallback);
					    }
				    }
				  }
			}},isDefault:true},
			{text:"Cancel",handler:{fn:function(){
				this.cancel();
			}}}];
		this.panel.cfg.queueProperty("buttons",buttons);
		this.panel.render("page_body");

		this.panel.show();

      }
      window.exptLoader.onLoadComplete.subscribe(function(obj){
			closeModalPanel("labels_loading");

			window.labelInput.disabled=false;
		    var oDS=new YAHOO.util.LocalDataSource(window.exptLoader.list);
		    oDS.responseSchema = {fields : ["label"]};

		    window.oAC= new YAHOO.widget.AutoComplete(window.labelInput,"label_auto",oDS);
		    window.oAC.prehighlightClassName = "yui-ac-prehighlight";
		    window.oAC.useShadow = true;
		    window.oAC.minQueryLength = 0;

			if(window.exptLoader.list.length>0){
		       //show label button
		       var toggleD = function(e) {
		          //YAHOO.util.Event.stopEvent(e);
		          if(!YAHOO.util.Dom.hasClass(window.labelToggler, "open")) {
		             YAHOO.util.Dom.addClass(window.labelToggler, "open")
		          }

		          // Is open
		          if(window.oAC.isContainerOpen()) {
		             window.oAC.collapseContainer();
		          }
		          else {
		             // Is closed
		             window.oAC.getInputEl().focus(); // Needed to keep widget active
		             setTimeout(function() { // For IE
		                 window.oAC.sendQuery("");
		             },0);
		          }
		       }
		       oPushButtonD.on("click", toggleD);
		       window.oAC.containerCollapseEvent.subscribe(function(){YAHOO.util.Dom.removeClass(window.labelToggler, "open")});
		       window.labelToggler.style.display="";
		    }else{
		       window.labelToggler.style.display="none";
		    }
		});

      openModalPanel("labels_loading","Loading " + this.config.header + "s...");
      window.exptLoader.load(window.currentProject);
	}


}

window.success=function(subject_id){
  if(window.subjectForm!=undefined){
    window.subjectForm.close();
    window.subjectForm=null;
  }
  window.subjectLoader.load(window.currentProject,{selects:[window.subjectBox],defaultValue:subject_id});
}

window.failure=function(msg){
  //window.ProjectSubjectManager.message(msg);
  if(window.subjectForm!=undefined){
    window.subjectForm.close();
    window.subjectForm=null;
  }
}

function cleanLabel(val)
{
        var temp = val.replace(/^\s*|\s*$/g,"");
        var newVal = '';
        temp = temp.split(' ');
        for(var c=0; c < temp.length; c++) {
                newVal += '' + temp[c];
        }

        newVal = newVal.replace(/[-]/,"_");
        newVal = newVal.replace(/[&]/,"_");
        newVal = newVal.replace(/[?]/,"_");
        newVal = newVal.replace(/[<]/,"_");
        newVal = newVal.replace(/[>]/,"_");
        newVal = newVal.replace(/[(]/,"_");
        newVal = newVal.replace(/[)]/,"_");
        newVal = newVal.replace(/[#]/,"_");
        newVal = newVal.replace(/[%]/,"_");
        newVal = newVal.replace(/[=]/,"_");
        newVal = newVal.replace(/[{]/,"_");
        newVal = newVal.replace(/[}]/,"_");
        newVal = newVal.replace(/[|]/,"_");
        newVal = newVal.replace(/[,]/,"_");
        newVal = newVal.replace(/[`]/,"_");
        newVal = newVal.replace(/[~]/,"_");
        newVal = newVal.replace(/[;]/,"_");
        newVal = newVal.replace(/[:]/,"_");
   	     return newVal;
}
