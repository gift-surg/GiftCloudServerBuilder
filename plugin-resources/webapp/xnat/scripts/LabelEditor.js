<!-- used in xnat:subjectData/share.vm -->
if(XNAT.app._label==undefined)XNAT.app._label=new Object();

XNAT.app._label.LabelEditorP=function(_config,uri,currentLabel){
	this.config=_config;
	this.uri=uri;
	XNAT.app._label.currentLabel=currentLabel;
	
	if(this.config.header==undefined){
		this.config.header="Session";
	}
	
    this.onModification=new YAHOO.util.CustomEvent("modification",this);
    this.onError=new YAHOO.util.CustomEvent("modification-error",this);
	
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
		
		this.labelInput = document.createElement("input");
		this.labelInput.id="new_label";
		this.labelInput.value=XNAT.app._label.currentLabel;
		this.labelInput.name="new_label";
		
		this.labelContainer=document.createElement("div");
		this.labelContainer.id="complete_container";
		this.labelContainer.width="100px";
		td2.appendChild(this.labelContainer);
		
		this.labelContainer.appendChild(this.labelInput);
		
		var labelToggler=document.createElement("span");
		labelToggler.id="toggleLabels";
		
		this.labelContainer.appendChild(labelToggler);
		
		this.label_auto=document.createElement("div");
		this.label_auto.id="label_auto";
		this.labelContainer.appendChild(this.label_auto);
		
		tr.appendChild(td1);		
		tr.appendChild(td2);
		tb.appendChild(tr);
		
		 XNAT.app._label.oPushButtonD = new YAHOO.widget.Button({container:"toggleLabels"});   
		  YAHOO.util.Dom.setStyle("toggleLabels","display","none");
		
		this.panel.setBody(bd);
		
		this.panel.selector=this;
		var buttons=[{text:"Modify",handler:{fn:function(){
				var labelBox = document.getElementById("new_label");
				  XNAT.app._label.selectedLabel=labelBox.value.trim();
				  if(XNAT.app._label.selectedLabel==""){
				    alert("Please specify a new " + this.selector.config.header + ".");
				  }else if(XNAT.app._label.selectedLabel==XNAT.app._label.currentLabel){
				    alert("No modification found.");
				  }else{
				    var validatedLabel=cleanLabel(XNAT.app._label.selectedLabel);
				    if(validatedLabel!=XNAT.app._label.selectedLabel){
				       labelBox.value=validatedLabel;
				       alert("Invalid characters in new " + this.selector.config.header + ".  Review modified value and resubmit.");
				       labelBox.focus();
				       return;
				    }
				    
				    var matchedExisting=false;
				    for(var lC=0;lC<XNAT.app._label.labelLoader.list.length;lC++){
				       if(XNAT.app._label.selectedLabel==XNAT.app._label.labelLoader.list[lC].label){
				          matchedExisting=true;
				          break;
				       }
				    }
				    
				    if(matchedExisting){
				       alert("This " + this.selector.config.header + " is already in use in this project.  Please modify and resubmit.");
				       labelBox.focus();
				       return;
				    }
				    
				    
				    if(this.selector.uri==undefined){
			               		XNAT.app._label.currentLabel=XNAT.app._label.selectedLabel;
			        	        closeModalPanel("modify_new_label");	
			        	        this.selector.onModification.fire();
			        	        this.cancel();
					}else{
					    if(confirm("Modifying the " + this.selector.config.header + " of an imaging session will result in the moving of files on the file server within the project's storage space.  Are you sure you want to make this change?")){
					    	if(showReason){
                    			var justification=new XNAT.app.requestJustification("label_change","Label Modification Justification",XNAT.app.modifyLabel,this);
                    		}else{
                    			var passthrough= new XNAT.app.passThrough(XNAT.app.modifyLabel,this);
                    			passthrough.fire();
                    		}
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
			
      openModalPanel("labels_loading","Loading " + this.config.header + "s...");
      XNAT.app._label.labelLoader=new XNAT.app._label.labelLoaderP();
      XNAT.app._label.labelLoader.load(uri);
	}
	
	
}


XNAT.app.modifyLabel=function(arg1,arg2,container){
	openModalPanel("modify_new_label","Modifying " + this.selector.config.header +", please wait...");

	var settingsCallback={
    	success:function(o){
       		XNAT.app._label.currentLabel=XNAT.app._label.selectedLabel;
	        closeModalPanel("modify_new_label");	
	        this.selector.onModification.fire();
	        this.cancel();
    	},
        failure:function(o){
       		alert("ERROR (" +o.status +"): Failed to modify label.");
	        this.selector.onError.fire();
	        closeModalPanel("modify_new_label");	
    	},
        cache:false, // Turn off caching for IE
        scope:this
    }

	var event_reason=(container==undefined || container.dialog==undefined)?"":container.dialog.event_reason;
	
    var params="";		
	   	params+="event_reason="+event_reason;
	   	params+="&event_type=WEB_FORM";
	   	params+="&event_action=Renamed shared item";
    	
	YAHOO.util.Connect.asyncRequest('PUT',this.selector.uri +"/" + XNAT.app._label.currentLabel +"/projects/" + this.selector.config.project +"?label=" + XNAT.app._label.selectedLabel +"&format=json&XNAT_CSRF=" + csrfToken + "&"+ params,settingsCallback);
}

XNAT.app._label.labelLoaderP=function(){
    this.load=function(uri){
		this.list=undefined;
		//load from search xml from server
		this.initCallback={
			success:this.completeInit,
			failure:this.initFailure,
            cache:false, // Turn off caching for IE
			scope:this
		}
		
		YAHOO.util.Connect.asyncRequest('GET',uri+'?format=json&columns=label&timestamp=' + (new Date()).getTime(),this.initCallback,null,this);
	};
	
	this.initFailure=function(o){
		this.displayError("ERROR " + o.status+ ": Failed to load current label list.");
	};
	
	this.completeInit=function(o){
		try{
		    this.list= eval("(" + o.responseText +")").ResultSet.Result;
			this.onload();
		}catch(e){
			this.displayError("ERROR " + o.status+ ": Failed to parse current label  list.");
		}
		
	};
	
	this.displayError=function(errorMsg){
		alert(errorMsg);
	};
	
	this.onload=function(obj){
		closeModalPanel("labels_loading");
		
		document.getElementById("new_label").disabled=false;
	    var oDS=new YAHOO.util.LocalDataSource(XNAT.app._label.labelLoader.list);
	    oDS.responseSchema = {fields : ["label"]};  
	    
	    XNAT.app._label.oAC= new YAHOO.widget.AutoComplete("new_label","label_auto",oDS);
	    XNAT.app._label.oAC.prehighlightClassName = "yui-ac-prehighlight";   
	    XNAT.app._label.oAC.useShadow = true;   
	    XNAT.app._label.oAC.minQueryLength = 0;
	    
		if(XNAT.app._label.labelLoader.list.length>0){
	       //show label button     
	       var toggleD = function(e) {   
	          //YAHOO.util.Event.stopEvent(e);   
	          if(!YAHOO.util.Dom.hasClass("toggleLabels", "open")) {   
	             YAHOO.util.Dom.addClass("toggleLabels", "open")   
	          }   
	            
	          // Is open   
	          if(XNAT.app._label.oAC.isContainerOpen()) {   
	             XNAT.app._label.oAC.collapseContainer();   
	          }     
	          else {   
	             // Is closed 
	             XNAT.app._label.oAC.getInputEl().focus(); // Needed to keep widget active   
	             setTimeout(function() { // For IE   
	                 XNAT.app._label.oAC.sendQuery("");   
	             },0);   
	          }
	       }
	       XNAT.app._label.oPushButtonD.on("click", toggleD);   
	       XNAT.app._label.oAC.containerCollapseEvent.subscribe(function(){YAHOO.util.Dom.removeClass("toggleLabels", "open")});   
	       YAHOO.util.Dom.setStyle("toggleLabels","display","");
	    }else{
	       YAHOO.util.Dom.setStyle("toggleLabels","display","none");
	    }
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