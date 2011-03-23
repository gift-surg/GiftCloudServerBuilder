function ProjectSubjectSelector(_proj_select, _subj_select,_submit_button, _defaultProject, _defaultSubject){
	this.projectSelect=_proj_select;
	this.subjSelect=_subj_select;
	this.submitButton=_submit_button;
	this.defaultProject=_defaultProject;
	this.defaultSubject=_defaultSubject;
  
	this.init=function(){
		
		//load from search xml from server
		this.initCallback={
			success:this.completeInit,
			failure:this.initFailure,
			scope:this
		}
		
		var params="";
		
		params += "&owner=true";
		params += "&member=true";
				
		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/projects?format=json&timestamp=' + (new Date()).getTime() + params,this.initCallback,null,this);
	};
		
	this.initFailure=function(o){
		this.displayError("ERROR " + o.status+ ": Failed to load project list.");
	};
	
	this.completeInit=function(o){
		try{
		    this.projectResultSet= eval("(" + o.responseText +")");
		}catch(e){
			this.displayError("ERROR " + o.status+ ": Failed to parse project list.");
		}
		try{
		    this.renderProjects();
		}catch(e){
			this.displayError("ERROR : Failed to render project list: " + e.toString());
		}
	};
	
	this.renderProjects=function(){
		if(this.projectResultSet.ResultSet.Result.length==0){
			
		}else{
			this.projBox=document.getElementById(this.projectSelect);
			this.projBox.options[0]=new Option("SELECT","");
				
			for(var pC=0;pC<this.projectResultSet.ResultSet.Result.length;pC++){
				var defaultSelected=(this.projectResultSet.ResultSet.Result[pC].id==this.defaultProject)?true:false;
				var opt=new Option(this.projectResultSet.ResultSet.Result[pC].secondary_id,this.projectResultSet.ResultSet.Result[pC].id,defaultSelected,defaultSelected);
				this.projBox.options[pC+1]=opt;
				if(defaultSelected){
					this.projBox.selectedIndex=(this.projBox.options.length-1);
				}
			}
			
			this.projBox.disabled=false;
			
			this.projBox.manager=this;
			
			this.projBox.onchange=function(o){
				if(this.selectedIndex>0){
					this.manager.projID=this.options[this.selectedIndex].value;
					this.manager.loadSubjects();
					this.manager.loadExpts();
				}
			}
					
			if(this.projBox.selectedIndex>0){
				this.projBox.onchange();
			}
		}
	}
	
	this.loadSubjects=function(o){
	  try{
		var subjCallback={
			success:function(o){
				try{
				    o.argument.subjectResultSet= eval("(" + o.responseText +")");
				    o.argument.subjectResultSet.ResultSet.Result.sort(function(a,b){
				    	if(a["label"]<b["label"]){
				    		return -1;
				    	}else if(b["label"]<a["label"]){
				    		return 1;
				    	}else{
				    		return 0;
				    	}
				    });
				   
				}catch(e){
					o.argument.displayError("ERROR " + o.status+ ": Failed to parse subject list.");
				}
				try{
				    o.argument.renderSubjects();
				}catch(e){
					o.argument.displayError("ERROR : Failed to render subject list.");
				}
			},
			failure:function(o){alert("Failed to load subjects.")},
			argument:this
		}

		if(this.subjBox!=undefined){
			this.subjBox.disabled=true;
			
			while(this.subjBox.length>0){
				this.subjBox.remove(0);
			}
		}
		
		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/projects/' + this.projID +'/subjects?format=json&timestamp=' + (new Date()).getTime(),subjCallback);
	  }catch(e){
	  	alert('failed to load subjects');
	  }
	}
	
	
	this.renderSubjects=function(o){
		this.subjBox=document.getElementById(this.subjSelect);
		this.subjBox.options[0]=new Option("SELECT","");
			this.subjBox.options[0].style.color="black";
			
		var matched=false;
		for(var sC=0;sC<this.subjectResultSet.ResultSet.Result.length;sC++){
			var defaultSelected=(this.subjectResultSet.ResultSet.Result[sC].ID==this.defaultSubject || this.subjectResultSet.ResultSet.Result[sC]["label"]==this.defaultSubject)?true:false;
			if(defaultSelected)matched=true;
			var _label=this.subjectResultSet.ResultSet.Result[sC]["label"];
			_label=(_label==undefined || _label=="")?this.subjectResultSet.ResultSet.Result[sC].ID:_label;
			var opt=new Option(_label,this.subjectResultSet.ResultSet.Result[sC].ID,defaultSelected,defaultSelected);
			this.subjBox.options[sC+1]=opt;
			this.subjBox.options[sC+1].style.color="black";
			if(defaultSelected){
				this.subjBox.selectedIndex=(this.subjBox.options.length-1);
			}
		}
		this.subjBox.disabled=false;
		
		if(!matched && (this.defaultSubject!="NULL" && this.defaultSubject!="null" && this.defaultSubject!="" && this.defaultSubject!=null)){
			var opt=new Option(this.defaultSubject,this.defaultSubject,true,true);
			this.subjBox.options[sC+1]=opt;
			this.subjBox.options[sC+1].newValue=true;
			this.subjBox.options[sC+1].style.color="red";
			this.subjBox.selectedIndex=(this.subjBox.options.length-1);
			if (YAHOO.env.ua.gecko > 0)this.subjBox.style.color="red";
		}
		
		if(eval("window.confirmValues")!=undefined){
			this.subjBox.onchange=function(){
				if (YAHOO.env.ua.gecko > 0)this.style.color=this.options[this.selectedIndex].style.color;
				confirmValues(false);
			}
			
			confirmValues(false);
		}else{
			this.subjBox.onchange=function(){
				if (YAHOO.env.ua.gecko > 0)this.style.color=this.options[this.selectedIndex].style.color;
			}
		}
	}
	
	this.loadExpts=function(o){
	  try{
		var subjCallback={
			success:function(o){
				try{
					var resultset=(eval("(" + o.responseText +")")).ResultSet;
					if(resultset.totalRecords=="0"){
						window.psm.exptResultSet= new Array();
					}else{
						window.psm.exptResultSet= resultset.Result;
					}
				}catch(e){
					if(window.psm!=undefined)window.psm.exptResultSet=new Array();
					if(o.argument.displayError!=undefined)o.argument.displayError("ERROR " + o.status+ ": Failed to parse expt list.");
				}
					
				if(verifyExptId!=undefined && verifyExptId!=null){
					verifyExptId();
			}
			},
			failure:function(o){alert("Failed to load expts.")},
			argument:this
		}
		
		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/projects/' + this.projID +'/experiments?format=json&timestamp=' + (new Date()).getTime(),subjCallback);
	  }catch(e){
	  	alert('failed to load expts');
	  }
	}
}

function fixSessionID(val)
{
        var temp = val.trim();
        var newVal = '';
        temp = temp.split(' ');
        for(var c=0; c < temp.length; c++) {
                newVal += '' + temp[c];
        }
        
        newVal = newVal.replace(/[&]/,"_");
        newVal = newVal.replace(/[?]/,"_");
        newVal = newVal.replace(/[<]/,"_");
        newVal = newVal.replace(/[>]/,"_");
        newVal = newVal.replace(/[(]/,"_");
        newVal = newVal.replace(/[)]/,"_");
        newVal = newVal.replace(/[.]/,"_");
        newVal = newVal.replace(/[,]/,"_");
        newVal = newVal.replace(/[\^]/,"_");
        newVal = newVal.replace(/[@]/,"_");
        newVal = newVal.replace(/[!]/,"_");
        newVal = newVal.replace(/[%]/,"_");
        newVal = newVal.replace(/[*]/,"_");
        newVal = newVal.replace(/[#]/,"_");
        newVal = newVal.replace(/[$]/,"_");
        newVal = newVal.replace(/[\\]/,"_");
        newVal = newVal.replace(/[|]/,"_");
        newVal = newVal.replace(/[=]/,"_");
        newVal = newVal.replace(/[+]/,"_");
        newVal = newVal.replace(/[']/,"_");
        newVal = newVal.replace(/["]/,"_");
        newVal = newVal.replace(/[~]/,"_");
        newVal = newVal.replace(/[`]/,"_");
        newVal = newVal.replace(/[:]/,"_");
        newVal = newVal.replace(/[;]/,"_");
        newVal = newVal.replace(/[\/]/,"_");
        newVal = newVal.replace(/[\[]/,"_");
        newVal = newVal.replace(/[\]]/,"_");
        newVal = newVal.replace(/[{]/,"_");
        newVal = newVal.replace(/[}]/,"_");
        
        if(newVal!=temp){
      	  alert("Removing invalid characters in session.");
        }
        return newVal;
}

function verifyExptId(obj){        
 try{
 	if(elementName!=undefined){
	   	var pS=document.getElementById(elementName+"/project");
	   	if(pS.selectedIndex>0){
	     	var p = pS.options[pS.selectedIndex].value;
	     	var match=null,veid=false;
	     	if(document.getElementById(elementName+"/label")!=null){
			
		     	var temp_label=document.getElementById(elementName+"/label").value.trim();
		     	temp_label=fixSessionID(temp_label);
		     	document.getElementById(elementName+"/label").value=temp_label;
		   
		    	if(temp_label!='' && window.psm.exptResultSet!=undefined){
		     		for(var aSc=0;aSc<window.psm.exptResultSet.length;aSc++)
		     		{
		       
		       			if(window.psm.exptResultSet[aSc].label==temp_label){
		         			match=window.psm.exptResultSet[aSc];
		         			break;
		       			}
		     		}
		    	}
		   
			    if(match!=null){
			    	
			         document.getElementById(elementName+"/ID").value=match.id;
			         document.getElementById(elementName+"/label").verified=true;
			         
			         document.getElementById("label_msg").innerHTML="* Matches existing session.  Continuing could modify that session.  <ul><li>Select append to only add new content to existing session.</li><li>Select overwrite to overwrite existing content.</li></ul>";
			         if(document.getElementById("label_opts").innerHTML=="")document.getElementById("label_opts").innerHTML="<select name='overwrite' ID='session_overwrite'><option value='append' SELECTED>APPEND</option><option value='delete'>OVERWRITE</option></select>";
			         veid=true;
			    }else{
			         document.getElementById(elementName+"/ID").value="";
			         document.getElementById(elementName+"/label").verified=true;
			         document.getElementById("label_msg").innerHTML="";
			         document.getElementById("label_opts").innerHTML="";
			         
			         veid=true;
			    }
		   
		    	return veid;
		}
	   	}else{
	     	return true;
	   	}
 	}
 }catch(e){
	   alert('Failed to validate expt id:' + e.message);
	}
}