function FileViewer(_obj){
	//categories.object
	this.loading=0;
	this.requestRender=false;
	this.obj=_obj;
	if(this.obj.categories==undefined){
		this.obj.categories=new Object();
		this.obj.categories.ids=new Array();
	}
	
	if(this.obj.categories["misc"]==undefined || this.obj.categories["misc"]==null){
		this.obj.categories["misc"]=new Object();
		this.obj.categories["misc"].cats=new Array();
	}
	
	
	this.init=function(){
		if(this.loading==0){
			this.loading=1;
			this.resetCounts();
			var catCallback={
				success:this.processCatalogs,
				failure:this.handleFailure,
				scope:this
			}
		
			YAHOO.util.Connect.asyncRequest('GET',this.obj.uri + '/resources?all=true&format=json&file_stats=true&sortBy=category,cat_id,label&timestamp=' + (new Date()).getTime(),catCallback,null,this);
		}else if(this.loading==1){
			//in process
		}else{
			//loaded
			if(this.requestRender){
				this.render();
			}
		}
//			var countCallback={
//				success:this.processResults,
//				failure:this.handleFailure,
//				scope:this
//			}
//			
//			if(this.obj.msg==undefined){
//				this.obj.msg="Loading file information.";
//			}
//		
//			this.clearFiles();
//			
//			openModalPanel("refresh_file",this.obj.msg);
//			YAHOO.util.Connect.asyncRequest('GET',this.obj.uri + '/files?all=true&format=json&timestamp=' + (new Date()).getTime(),countCallback,null,this);
	}
	
	this.handleFailure=function(o){		
		closeModalPanel("refresh_file");
		alert("Error loading files");
	}
	   
	this.removeFile=function(item){		
		if(showReason){
			var justification=new XNAT.app.requestJustification("file","File Deletion Dialog",this._removeFile,this);
			justification.item=item;	
		}else{
			var passthrough= new XNAT.app.passThrough(this._removeFile,this);
			passthrough.item=item;
			passthrough.fire();
		}
	}
   
   this._removeFile=function(arg1,arg2,container){	   
		var event_reason=(container==undefined || container.dialog==undefined)?"":container.dialog.event_reason;
		this.initCallback={
			success:function(obj1){
	    		this.refreshCatalogs("file");
			},
			failure:function(o){
	    		closeModalPanel("file");
				displayError("ERROR " + o.status+ ": Failed to delete file.");
			},
			scope:this
		}
		
		openModalPanel("file","Deleting '" + container.item.file_name +"'");

		var params="";		
		params+="event_reason="+event_reason;
		params+="&event_type=WEB_FORM";
		params+="&event_action=File Deleted";
		
		YAHOO.util.Connect.asyncRequest('DELETE',container.item.uri+'?XNAT_CSRF=' + csrfToken + '&'+params,this.initCallback,null,this);
   }
   
   this.removeCatalog=function(item){	
		if(showReason){
			var justification=new XNAT.app.requestJustification("file","Folder Deletion Dialog",this._removeCatalog,this);
			justification.item=item;	
		}else{
			var passthrough= new XNAT.app.passThrough(this._removeCatalog,this);
			passthrough.item=item;
			passthrough.fire();
		}
   }
   
   this._removeCatalog=function(arg1,arg2,container){
		var event_reason=(container==undefined || container.dialog==undefined)?"":container.dialog.event_reason;
		this.initCallback={
			success:function(obj1){
  				this.refreshCatalogs("file");
			},
			failure:function(o){
	    		closeModalPanel("file");
				displayError("ERROR " + o.status+ ": Failed to delete file.");
			},
			scope:this
		}
		
		openModalPanel("file","Deleting folder '" + container.item.file_name +"'");
		var params="";		
		params+="event_reason="+event_reason;
		params+="&event_type=WEB_FORM";
		params+="&event_action=Folder Deleted";
		YAHOO.util.Connect.asyncRequest('DELETE',container.item.uri+ '?XNAT_CSRF=' + csrfToken + '&'+params,this.initCallback,null,this);
   }
   
   this.getScan=function(sc, sid){
   		var gsScans=this.obj.categories[sc];
   		if(gsScans!=undefined && gsScans!=null){
			for(var gssC=0;gssC<gsScans.length;gssC++){
				if(gsScans[gssC].id==sid){
					return gsScans[gssC];
				}
			}
   		}
		
		gsScans=null;
   		return null;
   }
   
   this.clearCatalogs=function(o){
   		var scans;
   		//clear catalogs
   		for(var catC=0;catC<this.obj.categories.ids.length;catC++)
   		{
   			scans=this.obj.categories[this.obj.categories.ids[catC]];
   			for(var sC=0;sC<scans.length;sC++){
   				scans[sC].cats =new Array();
   			}
   			
   			scans=null;
   		}
   		this.obj.categories["misc"].cats=new Array();
   }
   
   this.processCatalogs=function(o){
   		closeModalPanel("catalogs");
   		this.clearCatalogs();
   		
    	var catalogs= eval("(" + o.responseText +")").ResultSet.Result;
    	
    	for(var catC=0;catC<catalogs.length;catC++){
    		var scan=this.getScan(catalogs[catC].category,catalogs[catC].cat_id);
    		if(scan!=null){
    			if(scan.cats==null || scan.cats==undefined){
    				scan.cats=new Array();
    			}
    			scan.cats.push(catalogs[catC]);
    		}else{
    			if(this.obj.categories["misc"].cats==null || this.obj.categories["misc"].cats==undefined){
    				this.obj.categories["misc"].cats=new Array();
    			}
    			this.obj.categories["misc"].cats.push(catalogs[catC]);
    		}
    	}
    	
    	this.showCounts();
    	
    	this.loading=3;
    	
    	if(this.requestRender){
    		this.render();
    	}
   }
   
   this.resetCounts=function(){
   		var scans,sCount,sSize;
   		for(var catC=0;catC<this.obj.categories.ids.length;catC++)
   		{
   			var catName=this.obj.categories.ids[catC];
   			scans=this.obj.categories[this.obj.categories.ids[catC]];
   			for(var sC=0;sC<scans.length;sC++){
   				var dest=document.getElementById(catName + "_" + scans[sC].id + "_stats");
   				if(dest!=null && dest !=undefined){
	   				dest.innerHTML="Loading...";
   				}
   			}
   			
   			scans=null;
   		}
   }
   
   this.showCounts=function(){
   		var scans,sCount,sSize;
   		
   		var scan_counts=new Object();
   		var scan_resources=new Array();
   		
   		for(var catC=0;catC<this.obj.categories.ids.length;catC++)
   		{
   			var catName=this.obj.categories.ids[catC];
   			scans=this.obj.categories[catName];
   			for(var sC=0;sC<scans.length;sC++){
   				var dest=document.getElementById(catName + "_" + scans[sC].id + "_stats");
   				if(dest!=null && dest !=undefined){
	   				sCount=0;
	   				sSize=0;
	   				dest.innerHTML="";
	   				for(var scSC=0;scSC<scans[sC].cats.length;scSC++){
	   					dest.innerHTML+=scans[sC].cats[scSC].label
	   					dest.innerHTML+=" (";
	   					dest.innerHTML+=scans[sC].cats[scSC].file_count;
	   					dest.innerHTML+=" files, "
   						dest.innerHTML+=size_format(scans[sC].cats[scSC].file_size)
   						dest.innerHTML+=") ";
	   					
	   					if(catName=="scans"){
	   						if(scan_counts[scans[sC].cats[scSC].label]==undefined){
	   							scan_counts[scans[sC].cats[scSC].label]=new Object();
	   							scan_counts[scans[sC].cats[scSC].label].label=scans[sC].cats[scSC].label;
	   							scan_counts[scans[sC].cats[scSC].label].count=0;
	   							scan_counts[scans[sC].cats[scSC].label].size=0;
	   							scan_resources.push(scans[sC].cats[scSC].label);
	   						}
   							scan_counts[scans[sC].cats[scSC].label].count+=parseInt(scans[sC].cats[scSC].file_count);
   							scan_counts[scans[sC].cats[scSC].label].size+=parseInt(scans[sC].cats[scSC].file_size);
	   					}
	   				}
   				}
   			}

			var dest=document.getElementById("total_dicom_files");
			if(dest!=null && dest !=undefined){
   				dest.innerHTML="Totals: ";
				for(var sC2=0;sC2<scan_resources.length;sC2++){
					dest.innerHTML+=scan_counts[scan_resources[sC2]].label+" (";
					dest.innerHTML+=scan_counts[scan_resources[sC2]].count;
					dest.innerHTML+=" files, ";
					dest.innerHTML+=size_format(scan_counts[scan_resources[sC2]].size);
					dest.innerHTML+=") ";
				}
				
			}
   			
   			scans=null;
   		}
   }
   
   this.refreshCatalogs=function(msg_id){
		closeModalPanel(msg_id);
		openModalPanel("catalogs","Refreshing Catalog Information");
		var catCallback={
			success:this.processCatalogs,
			failure:this.handleFailure,
			scope:this
		}
	
		this.requestRender=true;
		YAHOO.util.Connect.asyncRequest('GET',this.obj.uri + '/resources?all=true&format=json&file_stats=true&timestamp=' + (new Date()).getTime(),catCallback,null,this);
   }
	
	this.render=function(){
		if(this.loading==0){
			this.requestRender=true;
			openModalPanel("catalogs","Loading File Summaries");
			this.init();
		}else if(this.loading==1){
			//in process
			this.requestRender=true;
		}else{
	   		if(this.panel!=undefined){
	   			this.panel.destroy();
	   		}
	   	
	   	    this.panel=new YAHOO.widget.Dialog("fileListing",{close:true,
			   width:"780px",height:"550px",underlay:"shadow",modal:true,fixedcenter:true,visible:false});
			this.panel.setHeader("File Manager");
		
			this.catalogClickers=new Array();
					
			var bd = document.createElement("div");
			
			var treediv=document.createElement("div");
			treediv.id="fileTree";
			treediv.style.overflow="auto";
			treediv.style.height="460px";
			treediv.style.width="740px";
			bd.appendChild(treediv);
			try{
				var tree = new YAHOO.widget.TreeView(treediv);
				var root = tree.getRoot();
									
				var total_size=0;
				
				if(this.obj.categories["misc"].cats.length>0){
				
					var parent = new YAHOO.widget.TaskNode({label: "Resources", expanded: true,checked:true}, root);
					parent.labelStyle = "icon-cf"; 
				
					for(var rCatC=0;rCatC<this.obj.categories["misc"].cats.length;rCatC++){
						var cat=this.obj.categories["misc"].cats[rCatC];
						var catNode=null;
					
						var lbl;
						if(cat.label!=""){
							lbl=cat.label;
						}else{
							lbl="NO LABEL";
						}
						cat.uri=this.obj.uri + "/resources/" + cat.xnat_abstractresource_id;
						cat.canEdit=this.obj.canEdit;
						cat.canDelete=this.obj.canDelete;
						catNode=new YAHOO.widget.CatalogNode({label: lbl}, parent,cat);
						this.catalogClickers.push(catNode);
					}
				}
				
				for(var cC=0;cC<this.obj.categories.ids.length;cC++){
					var catName=this.obj.categories.ids[cC];
					if(this.obj.categories[catName].length>0){
				
						var parent = new YAHOO.widget.TaskNode({label: catName, expanded: true,checked:true}, root);
						parent.labelStyle = "icon-cf"; 
						
						var scans=this.obj.categories[catName];
						for(var rScanC=0;rScanC<scans.length;rScanC++){
							var scan=scans[rScanC];
							var scanNode=null;
							if(scan.cats!=null && scan.cats!=undefined && scan.cats.length>0){
								var scanNode=new YAHOO.widget.TaskNode({label:(scan.label!=undefined)?scan.label:scan.id, expanded: true,checked:true}, parent);
								scanNode.labelStyle = "icon-cf";
								
								for(var scanCC=0;scanCC<scan.cats.length;scanCC++){
									var cat = scan.cats[scanCC];
									cat.uri=this.obj.uri + "/" + catName+ "/" + scan.id + "/resources/" + cat.xnat_abstractresource_id;
									cat.canEdit=this.obj.canEdit;
									cat.canDelete=this.obj.canDelete;
									catNode=new YAHOO.widget.CatalogNode({label: (cat.label!="")?cat.label:"NO LABEL"}, scanNode,cat);
									this.catalogClickers.push(catNode);

								}
							}else{
								scanNode=new YAHOO.widget.TextNode({label:(scan.label!=undefined)?scan.label:scan.id, expanded: false}, parent);
							}
						}
						scans=null;
					}
				}
				
				this.panel.setBody(bd);
					  
				var foot = document.createElement("div");
				foot.style.textAlign="right";
				
				var fTable=document.createElement("table");
				var fTbody=document.createElement("tbody");
				var fTr=document.createElement("tr");
				fTable.appendChild(fTbody);
				fTable.width="95%";
				fTable.align="center";
				fTbody.appendChild(fTr);
				foot.appendChild(fTable);
				
				var fTd1=document.createElement("td");
				fTd1.align="left";
				fTr.appendChild(fTd1);
				
				var fTd2=document.createElement("td");
				fTd2.align="right";
				fTr.appendChild(fTd2);
				
				
				if(this.obj.canEdit){
					var dButton3=document.createElement("input");
					dButton3.type="button";
					dButton3.value="<b>Add Folder</b>";
					fTd1.appendChild(dButton3);
					
					var oPushButtonD3 = new YAHOO.widget.Button(dButton3);
		  		    oPushButtonD3.subscribe("click",function(o){
		  		    	var upload=new AddFolderForm(this.obj);
		  		    	upload.render();
		  		    },this,true);
					
					var dButton1=document.createElement("input");
					dButton1.type="button";
					dButton1.value="<b>Upload Files</b>";
					fTd1.appendChild(dButton1);
					
					var oPushButtonD1 = new YAHOO.widget.Button(dButton1);
		  		    oPushButtonD1.subscribe("click",function(o){
		  		    	try{
			  		    	var upload=new UploadFileForm(this.obj);
			  		    	upload.render();
		  		    	}catch(e){
		  		    		alert(e.toString());
		  		    	}
		  		    },this,true);
				}
							
				var dType=document.createElement("select");
				dType.id="download_type_select";
				dType.options[0]=new Option("zip","zip",true,true);
				dType.options[1]=new Option("tar","tar");
				dType.options[2]=new Option("tar.gz","tar.gz");
				dType.style.marginRight="10px";
				fTd2.appendChild(dType);
				
				var dButton2=document.createElement("input");
				dButton2.type="button";
				dButton2.value="<b>Download</b>";
				fTd2.appendChild(dButton2);
				
				
				var dButton4=document.createElement("input");
				dButton4.type="button";
				dButton4.value="<b>Close</b>";
				fTd2.appendChild(dButton4);
				
				var oPushButtonD4 = new YAHOO.widget.Button(dButton4);
	  		    oPushButtonD4.subscribe("click",function(o){
	  		    	this.panel.hide();
	  		    },this,true);
				
				var oPushButtonD2 = new YAHOO.widget.Button(dButton2);
	  		    oPushButtonD2.subscribe("click",function(o){
	  		    	var dType=document.getElementById("download_type_select");
	  		    	var resources="";
	  		    	for(var ccC=0;ccC<this.catalogClickers.length;ccC++){
	  		    		if(this.catalogClickers[ccC].checked){
	  		    			if(resources!="")resources+=",";
	  		    			resources+=this.catalogClickers[ccC].xnat_abstractresource_id;
	  		    		}
	  		    	}
	  		    	if(resources==""){
	  		    		alert("No files found.");
	  		    		return;
	  		    	}
	  		    	var destination=this.obj.uri + "/resources/"+resources + "/files?format="+ dType.options[dType.selectedIndex].value;
					
	  		    	this.panel.hide();
	  		    	mySimpleDialog = new YAHOO.widget.SimpleDialog("dlg", { 
						width: "20em", 
						fixedcenter:true,
						modal:true,
	    				visible:false,
						draggable:false });
					mySimpleDialog.setHeader("Preparing Download");
					mySimpleDialog.setBody("Your download should begin within 30 seconds.  If you encounter technical difficulties, you can restart the download using this <a href='" + destination +"'>link</a>.");
					
	  		    	window.location=destination;
	  		    },this,true);
	  		    
				this.panel.setFooter(foot);
					  
				this.panel.selector=this;
				
				tree.render();   
							
				this.loading=3;
				this.requestRender=false;
			}catch(o){				
				alert(o.toString());
			}
			this.panel.render("page_body");
			this.panel.show();
		}
   }	
}

YAHOO.widget.TaskNode = function(oData, oParent, expanded, checked) {
	YAHOO.widget.TaskNode.superclass.constructor.call(this,oData,oParent,expanded);
    this.setUpCheck(checked || oData.checked);

};

YAHOO.extend(YAHOO.widget.TaskNode, YAHOO.widget.TextNode, {

    /**
     * True if checkstate is 1 (some children checked) or 2 (all children checked),
     * false if 0.
     * @type boolean
     */
    checked: false,

    /**
     * checkState
     * 0=unchecked, 1=some children checked, 2=all children checked
     * @type int
     */
    checkState: 0,

	/**
     * The node type
     * @property _type
     * @private
     * @type string
     * @default "TextNode"
     */
    _type: "TaskNode",
	
	taskNodeParentChange: function() {
        //this.updateParent();
    },
	
    setUpCheck: function(checked) {
        // if this node is checked by default, run the check code to update
        // the parent's display state
        if (checked && checked === true) {
            this.check();
        // otherwise the parent needs to be updated only if its checkstate 
        // needs to change from fully selected to partially selected
        } else if (this.parent && 2 === this.parent.checkState) {
             this.updateParent();
        }

        // set up the custom event on the tree for checkClick
        /**
         * Custom event that is fired when the check box is clicked.  The
         * custom event is defined on the tree instance, so there is a single
         * event that handles all nodes in the tree.  The node clicked is 
         * provided as an argument.  Note, your custom node implentation can
         * implement its own node specific events this way.
         *
         * @event checkClick
         * @for YAHOO.widget.TreeView
         * @param {YAHOO.widget.Node} node the node clicked
         */
        if (this.tree && !this.tree.hasEvent("checkClick")) {
            this.tree.createEvent("checkClick", this.tree);
        }

		this.tree.subscribe('clickEvent',this.checkClick);
        this.subscribe("parentChange", this.taskNodeParentChange);


    },

    /**
     * The id of the check element
     * @for YAHOO.widget.TaskNode
     * @type string
     */
    getCheckElId: function() { 
        return "ygtvcheck" + this.index; 
    },

    /**
     * Returns the check box element
     * @return the check html element (img)
     */
    getCheckEl: function() { 
        return document.getElementById(this.getCheckElId()); 
    },

    /**
     * The style of the check element, derived from its current state
     * @return {string} the css style for the current check state
     */
    getCheckStyle: function() { 
        return "ygtvcheck" + this.checkState;
    },


   /**
     * Invoked when the user clicks the check box
     */
    checkClick: function(oArgs) { 
		var node = oArgs.node;
		var target = YAHOO.util.Event.getTarget(oArgs.event);
		if (YAHOO.util.Dom.hasClass(target,'ygtvspacer')) {
	        if (node.checkState === 0) {
	            node.check();
	        } else {
	            node.uncheck();
	        }

	        node.onCheckClick(node);
	        this.fireEvent("checkClick", node);
		    return false;
		}
    },

    /**
     * Override to get the check click event
     */
    onCheckClick: function() { 
        
    },

    /**
     * Refresh the state of this node's parent, and cascade up.
     */
    updateParent: function() { 
        var p = this.parent;

        if (!p || !p.updateParent) {
            return;
        }

        var somethingChecked = false;
        var somethingNotChecked = false;

        for (var i=0, l=p.children.length;i<l;i=i+1) {

            var n = p.children[i];

            if ("checked" in n) {
                if (n.checked) {
                    somethingChecked = true;
                    // checkState will be 1 if the child node has unchecked children
                    if (n.checkState === 1) {
                        somethingNotChecked = true;
                    }
                } else {
                    somethingNotChecked = true;
                }
            }
        }

        if (somethingChecked) {
            p.setCheckState( (somethingNotChecked) ? 1 : 2 );
        } else {
            p.setCheckState(0);
        }

        p.updateCheckHtml();
        p.updateParent();
    },

    /**
     * If the node has been rendered, update the html to reflect the current
     * state of the node.
     */
    updateCheckHtml: function() { 
        if (this.parent && this.parent.childrenRendered) {
            this.getCheckEl().className = this.getCheckStyle();
        }
    },

    /**
     * Updates the state.  The checked property is true if the state is 1 or 2
     * 
     * @param the new check state
     */
    setCheckState: function(state) { 
        this.checkState = state;
        this.checked = (state > 0);
    },

    /**
     * Check this node
     */
    check: function() { 
        this.setCheckState(2);
        for (var i=0, l=this.children.length; i<l; i=i+1) {
            var c = this.children[i];
            if (c.check) {
                c.check();
            }
        }
        this.updateCheckHtml();
        this.updateParent();
    },

    /**
     * Uncheck this node
     */
    uncheck: function() { 
        this.setCheckState(0);
        for (var i=0, l=this.children.length; i<l; i=i+1) {
            var c = this.children[i];
            if (c.uncheck) {
                c.uncheck();
            }
        }
        this.updateCheckHtml();
        this.updateParent();
    },
    // Overrides YAHOO.widget.TextNode

    getContentHtml: function() {                                                                                                                                           
        var sb = [];                                                                                                                                                       
        sb[sb.length] = '<td';                                                                                                                                             
        sb[sb.length] = ' id="' + this.getCheckElId() + '"';                                                                                                               
        sb[sb.length] = ' class="' + this.getCheckStyle() + '"';                                                                                                           
        sb[sb.length] = '>';                                                                                                                                               
        sb[sb.length] = '<div class="ygtvspacer"></div></td>';                                                                                                             
                                                                                                                                                                           
        sb[sb.length] = '<td><span';                                                                                                                                       
        sb[sb.length] = ' id="' + this.labelElId + '"';                                                                                                                    
        if (this.title) {                                                                                                                                                  
            sb[sb.length] = ' title="' + this.title + '"';                                                                                                                 
        }                                                                                                                                                                  
        sb[sb.length] = ' class="' + this.labelStyle  + '"';                                                                                                               
        sb[sb.length] = ' >';                                                                                                                                              
        sb[sb.length] = this.label;                                                                                                                                        
        sb[sb.length] = '</span></td>';                                                                                                                                    
        return sb.join("");                                                                                                                                                
    }  
});

    function number_format( number, decimals, dec_point, thousands_sep ) {
      	var n = number, c = isNaN(decimals = Math.abs(decimals)) ? 2 : decimals;
  		var d = dec_point == undefined ? "," : dec_point;
  		var t = thousands_sep == undefined ? "." : thousands_sep, s = n < 0 ? "-" : "";
  		var i = parseInt(n = Math.abs(+n || 0).toFixed(c)) + "", j = (j = i.length) > 3 ? j % 3 : 0;
  		return s + (j ? i.substr(0, j) + t : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : "");
    }

	

   function size_format (filesize) {
      if (filesize >= 1073741824) {
         filesize = number_format(filesize / 1073741824, 2, '.', '') + ' Gb';
      } else {
         if (filesize >= 1048576) {
             filesize = number_format(filesize / 1048576, 2, '.', '') + ' Mb';
         } else {
             if (filesize >= 1024) {
                 filesize = number_format(filesize / 1024, 0) + ' Kb';
             } else {
                 filesize = number_format(filesize, 0) + ' bytes';
             };
         };
      };
      return filesize;
   };
   
   YAHOO.widget.CatalogNode = function(oData, oParent, catalog) {
	YAHOO.widget.CatalogNode.superclass.constructor.call(this,oData,oParent,false,(catalog.file_count>0));
	this.cat=catalog;
	this.renderCatalog(catalog);
};

YAHOO.extend(YAHOO.widget.CatalogNode, YAHOO.widget.TaskNode, {
	renderCatalog:function(cat){
		this.xnat_abstractresource_id=cat.xnat_abstractresource_id;
		this.labelStyle = "icon-cf"; 
		
		if(cat.files!=undefined && cat.files!=null){
			this.renderFiles();				
		}else if(cat.file_count>0){
			this.setDynamicLoad(function(node, fnLoadComplete){
		 		var callback={
			      success:function(oResponse){
			        oResponse.argument.catNode.cat.files = (eval("(" + oResponse.responseText + ")")).ResultSet.Result; 
			        oResponse.argument.catNode.renderFiles();   
			        oResponse.argument.fnLoadComplete();
			      },
			      failure:function(oResponse){
			        oResponse.argument.fnLoadComplete();
			      },
			      argument:{"fnLoadComplete":fnLoadComplete,catNode:this}
			    };
			    
				YAHOO.util.Connect.asyncRequest('GET',this.cat.uri + '/files?format=json&timestamp=' + (new Date()).getTime(),callback,null);
			},this);
		}

		if(cat.file_count!=undefined && cat.file_count!=null){
		  this.label+="&nbsp;&nbsp;" + cat.file_count + " files, "+ size_format(cat.file_size);
		}else{
		  this.label+="&nbsp;&nbsp;" + size_format(cat.file_size);
		}
		if(this.cat.format!=""){
		   this.label +="&nbsp;"+ this.cat.format +"";
		}
		if(this.cat.content!=""){
		   this.label +="&nbsp;"+ this.cat.content +"";
		}
		if(this.cat.tags!=""){
		   this.label +="&nbsp;("+ this.cat.tags +")";
		}
		if(this.cat.canDelete)
			this.label +="&nbsp;&nbsp;<a onclick=\"window.viewer.removeCatalog({file_name:'" + cat.label +"',uri:'" + this.cat.uri + "',id:'" + cat.xnat_abstractresource_id + "'});\"><img style='height:14px' border='0' src='" +serverRoot+"/images/delete.gif'/></a>";
	},
	renderFiles:function(){
			for(var fC=0;fC<this.cat.files.length;fC++){
				var file=this.cat.files[fC];
				var size=parseInt(file.Size);
				var path=file.URI.substring(file.URI.indexOf("/files/")+7);
				var _lbl="<a style='font-size:9px' target='_blank' onclick=\"location.href='" +serverRoot + file.URI + "';\">" + path + "</a>"
				if(file.file_format!=""){
				   _lbl +="&nbsp;"+ file.file_format +"";
				}
				if(file.file_content!=""){
				   _lbl +="&nbsp;"+ file.file_content +"";
				}
				if(file.file_tags!=""){
				   _lbl +="&nbsp;("+ file.file_tags +")";
				}
				_lbl +="&nbsp;&nbsp;"+size_format(size) +"&nbsp;";
				
				if(this.cat.canDelete)
					_lbl +="&nbsp;&nbsp;<a onclick=\"window.viewer.removeFile({file_name:'" + path +"',uri:'" + serverRoot + file.URI + "'});\"><img style='height:14px' border='0' src='" +serverRoot+"/images/delete.gif'/></a>";
				
				var fileNode=new YAHOO.widget.TextNode({label: _lbl, expanded: false}, this);
				fileNode.labelStyle = "icon-f"; 
			}
		}
});
   
function UploadFileForm(_obj){
  	this.obj=_obj;
    this.onResponse=new YAHOO.util.CustomEvent("response",this);
  
	this.render=function(){	
		this.panel=new YAHOO.widget.Dialog("fileUploadDialog",{close:true,
		   width:"440px",height:"400px",underlay:"shadow",modal:true,fixedcenter:true,visible:false});
		this.panel.setHeader("Upload File");
				
		var div = document.createElement("form");
		
		
		var table,tbody,tr,td,input;
   	  
   	  var title=document.createElement("div");
   	  title.style.marginTop="3px";
   	  title.style.marginLeft="1px";
   	  title.innerHTML="<font size='+1' style='weight:700'>File Upload Form</font>";
   	  div.appendChild(title);
   	  
   	  div.appendChild(document.createElement("br"));
   	  
   	  var collection_form=document.createElement("div");
   	  div.appendChild(collection_form);
   	  collection_form.style.border="1px solid #DEDEDE";
   	  collection_form.appendChild(document.createElement("div"));
   	  collection_form.childNodes[0].innerHTML="<strong>Destination</strong>";
   	  
   	  table=document.createElement("table");
   	  tbody=document.createElement("tbody");
   	  table.appendChild(tbody);
   	  collection_form.appendChild(table);
   	  
   	  
   	  if(this.obj.categories!=undefined){
	   	  //collection
	   	  tr=document.createElement("tr");
			tr.style.height="20px";
	   	  
	   	  td=document.createElement("th");
	   	  td.align="left";
	   	  td.innerHTML="Level";
	   	  tr.appendChild(td);
	   	  
	   	  td=document.createElement("td");
	   	  input=document.createElement("select");
	   	  input.id="upload_level";
	   	  input.manager=this;
	   	  input.options[0]=new Option("SELECT","");
	   	  for(var cIdC=0;cIdC<this.obj.categories.ids.length;cIdC++){
	   	    input.options[input.options.length]=new Option(this.obj.categories.ids[cIdC],this.obj.categories.ids[cIdC]);
	   	    input.options[(input.options.length -1)].category=this.obj.categories[this.obj.categories.ids[cIdC]];
	   	  }
	   	  input.options[input.options.length]=new Option("resources","resources");
	   	  input.options[(input.options.length -1)].category=this.obj.categories["misc"];
	   	  
	   	  input.onchange=function(o){
	   	  	var item_select=document.getElementById("upload_item");
	   	  	var coll_select=document.getElementById("upload_collection");
	   	  	
	   	  	if(this.selectedIndex==0){
	   	  		while(item_select.options.length>0){
	   	  			item_select.remove(0);
	   	  		}
	   	  		while(coll_select.options.length>0){
	   	  			coll_select.remove(0);
	   	  		}
	   	  		item_select.disabled=true;
	   	  		coll_select.disabled=true;
	   	  	}else{
	   	  		while(item_select.options.length>0){
	   	  			item_select.remove(0);
	   	  		}
	   	  		while(coll_select.options.length>0){
	   	  			coll_select.remove(0);
	   	  		}
	   	  		var _sOption=this.options[this.selectedIndex];
	   	  		
	   	  		if(_sOption.value=="resources"){
	   	  			item_select.disabled=true;
	   	  			coll_select.disabled=false;
	   	  			
	   	  			while(coll_select.options.length>0){
		   	  			coll_select.remove(0);
		   	  		}
	   	  		
	   	  			for(var cC=0;cC<_sOption.category.cats.length;cC++){
						var cat=_sOption.category.cats[cC];
						if(cat.label==""){
							coll_select.options[coll_select.options.length]=new Option("NO LABEL",cat.xnat_abstractresource_id);
						}else{
							coll_select.options[coll_select.options.length]=new Option(cat.label,cat.xnat_abstractresource_id);
						}
	   	  			}
						if(coll_select.options.length==0){
							coll_select.options[coll_select.options.length]=new Option("NO LABEL","");
						}
	   	  		}else{
	   	  			coll_select.disabled=true;
	   	  			item_select.disabled=false;
		   	  		var scans=_sOption.category;
		   	  		
		   	  		item_select.options[item_select.options.length]=new Option("SELECT","");
		   	  		for(var catC=0;catC<scans.length;catC++){
		   	  			item_select.options[item_select.options.length]=new Option(scans[catC].id,scans[catC].id);
		   	  			item_select.options[(item_select.options.length -1)].scan=scans[catC];
		   	  		}
	   	  		}
	   	  	}
	   	  };
	   	  td.appendChild(input);
	   	  tr.appendChild(td);
	   	  
	   	  tbody.appendChild(tr);
	   	  
	   	  
	   	  //collection
	   	  tr=document.createElement("tr");
			tr.style.height="20px";
	   	  
	   	  td=document.createElement("th");
	   	  td.align="left";
	   	  td.innerHTML="Item";
	   	  tr.appendChild(td);
	   	  
	   	  td=document.createElement("td");
	   	  input=document.createElement("select");
	   	  input.id="upload_item";
	   	  input.disabled=true;
	   	  input.manager=this;
	   	  input.onchange=function(o){
	   	  	var coll_select=document.getElementById("upload_collection");
	   	  	if(this.selectedIndex>0){
	   	  		coll_select.disabled=false;
	   	  		while(coll_select.options.length>0){
	   	  			coll_select.remove(0);
	   	  		}
	   	  		var _selectedO=this.options[this.selectedIndex];
   	  		
   	  			for(var cC=0;cC<_selectedO.scan.cats.length;cC++){
					var cat=_selectedO.scan.cats[cC];
					if(cat.label==""){
						coll_select.options[coll_select.options.length]=new Option("NO LABEL",cat.xnat_abstractresource_id);
					}else{
						coll_select.options[coll_select.options.length]=new Option(cat.label,cat.xnat_abstractresource_id);
					}
   	  			}
				if(coll_select.options.length==0){
					alert("Please create a folder (using the Add Folder dialog) before attempting to add files at this level.");
					coll_select.disabled=true;
				}
   	  			
	   	  	}else{
	   	  		coll_select.disabled=true;
	   	  		while(coll_select.options.length>0){
	   	  			coll_select.remove(0);
	   	  		}
	   	  	}

	   	  };
	   	  td.appendChild(input);
	   	  tr.appendChild(td);
	   	  
	   	  tbody.appendChild(tr);
	   	  
	   	  
	   	  //collection
	   	  tr=document.createElement("tr");
			tr.style.height="20px";
	   	  
	   	  td=document.createElement("th");
	   	  td.align="left";
	   	  td.innerHTML="Folder";
	   	  tr.appendChild(td);
	   	  
	   	  td=document.createElement("td");
	   	  input=document.createElement("select");
	   	  input.id="upload_collection";
	   	  input.manager=this;
	   	  input.disabled=true;
	   	  td.appendChild(input);
	   	  tr.appendChild(td);
	   	  
	   	  tbody.appendChild(tr);
   	  }else{    
	   	  //collection
	   	  tr=document.createElement("tr");
			tr.style.height="20px";
	   	  
	   	  td=document.createElement("th");
	   	  td.align="left";
	   	  td.innerHTML="Folder";
	   	  tr.appendChild(td);
	   	  
	   	  td=document.createElement("td");
	   	  input=document.createElement("select");
	   	  input.id="upload_collection";
	   	  input.manager=this;
	   	  td.appendChild(input);
	   	  tr.appendChild(td);
	   	  
	   	  tbody.appendChild(tr);
	   	  
	   	  for(var cC=0;cC<this.obj.categories["misc"].cats.length;cC++){
				var cat=this.obj.categories["misc"].cats[catID];
				if(cat.label==""){
					input.options[input.options.length]=new Option("NO LABEL",cat.xnat_abstractresource_id);
				}else{
					input.options[input.options.length]=new Option(cat.label,cat.xnat_abstractresource_id);
				}
			}
				if(input.options.length==0){
					input.options[input.options.length]=new Option("NO LABEL","");
				}
   	  }
   	  
   	  div.appendChild(document.createElement("br"));
   	  var file_form=document.createElement("div");
   	  div.appendChild(file_form);
   	  file_form.style.border="1px solid #DEDEDE";
   	  file_form.appendChild(document.createElement("div"));
   	  file_form.childNodes[0].innerHTML="<strong>File Information</strong>";
   	  table=document.createElement("table");
   	  tbody=document.createElement("tbody");
   	  table.appendChild(tbody);
   	  file_form.appendChild(table);
   	  
   	  //collection
   	  tr=document.createElement("tr");
		tr.style.height="20px";
   	  
   	  td=document.createElement("th");
   	  td.align="left";
   	  td.innerHTML="Rename";
   	  tr.appendChild(td);
   	  
   	  td=document.createElement("td");
   	  input=document.createElement("input");
   	  input.size=50;
   	  input.id="file_name";
   	  input.style.fontSize = "99%";
   	  input.manager=this;
   	  td.appendChild(input);
   	  tr.appendChild(td);
   	  
   	  tr.appendChild(td);
   	  tbody.appendChild(tr);
   	  
   	  
   	  //format
   	  tr=document.createElement("tr");
		tr.style.height="20px";
   	  
   	  td=document.createElement("th");
   	  td.align="left";
   	  td.innerHTML="Format";
   	  tr.appendChild(td);
   	  
   	  td=document.createElement("td");
   	  input=document.createElement("input");
   	  input.id="file_format";
   	  input.style.fontSize = "99%";
   	  td.appendChild(input);
   	  
   	  tr.appendChild(td);
   	  tbody.appendChild(tr);
   	  
   	  
   	  //content
   	  tr=document.createElement("tr");
		tr.style.height="20px";
   	  
   	  td=document.createElement("th");
   	  td.align="left";
   	  td.innerHTML="Content";
   	  tr.appendChild(td);
   	  
   	  td=document.createElement("td");
   	  input=document.createElement("input");
   	  input.style.fontSize = "99%";
   	  input.id="file_content";
   	  td.appendChild(input);
   	  
   	  tr.appendChild(td);
   	  tbody.appendChild(tr);
   	  div.appendChild(file_form);
   	  
   	  
   	  //tags
   	  tr=document.createElement("tr");
		tr.style.height="20px";
   	  
   	  td=document.createElement("th");
   	  td.align="left";
   	  td.innerHTML="Tags";
   	  tr.appendChild(td);
   	  
   	  td=document.createElement("td");
   	  input=document.createElement("input");
   	  input.style.fontSize = "99%";
   	  input.id="file_tags";
   	  td.appendChild(input);
   	  
   	  tr.appendChild(td);
   	  tbody.appendChild(tr);
   	  
   	  
   	  div.appendChild(document.createTextNode("File To Upload:"));
   	  
   	  var form = document.createElement("form");
   	  form.id="file_upload";
   	  form.name="file_upload";
   	  div.appendChild(form);
   	  
   	  input =document.createElement("input");
   	  input.type="file";
   	  input.id="local_file";
   	  input.name="local_file";
   	  input.size=50;
   	  input.style.fontSize = "99%";
   	  
   	  form.appendChild(input);
		
		this.panel.setBody(div);
		
		this.panel.selector=this;
		var buttons=[{text:"Upload",handler:{fn:this.uploadFile},isDefault:true},
			{text:"Close",handler:{fn:function(){
				this.cancel();
			}}}];
		this.panel.cfg.queueProperty("buttons",buttons);
		
		
		this.panel.render("page_body");
		this.panel.show();
	}

	this.uploadFile=function(){
		var coll_select=document.getElementById("upload_collection");
  	    if(coll_select.disabled==true){
  	    	alert("Please select a folder for this file.");
  	    	return;
  	    }
  	    
  	    if(document.getElementById("local_file").value==""){
  	    	alert("Please select a file to upload.");
  	    	return;
  	    }
  	    
		var collection_name=coll_select.options[coll_select.selectedIndex].value;
		
		var file_tags=document.getElementById("file_tags").value.trim();
		var file_format=document.getElementById("file_format").value.trim();
		var file_content=document.getElementById("file_content").value.trim();
		var file_name=document.getElementById("file_name").value.trim();
		if(file_name[0]=="/"){
			file_name=file_name.substring(1);
		}
		
		var file_params="?file_upload=true&XNAT_CSRF=" + csrfToken;
		
		if(file_content!=""){
			file_params+="&content="+file_content;
		}
		if(file_format!=""){
			file_params+="&format="+file_format;
		}
		if(file_tags!=""){
			file_params+="&tags="+file_tags;
		}
			
		var file_dest = this.selector.obj.uri;
		if(collection_name==""){
			file_dest=this.selector.obj.uri+"/files";
		}else{
			file_dest=this.selector.obj.uri+"/resources/"+ collection_name + "/files";
		}
		
		if(file_name!=""){
			file_dest +="/"+ file_name;
		}
		
		if(document.getElementById("local_file").value.endsWith(".zip")
		  || document.getElementById("local_file").value.endsWith(".gz")
		  || document.getElementById("local_file").value.endsWith(".xar")){
			if(confirm("Would you like the contents of this archive file to be extracted on the server?")){
				file_params+="&extract=true";
			}
		}
		
		file_dest+=file_params;
		if(showReason){
			var justification=new XNAT.app.requestJustification("add_file","File Upload Dialog",XNAT.app._uploadFile,this);
			justification.file_dest=file_dest;
			justification.file_name=file_name;
		}else{
			var passthrough= new XNAT.app.passThrough(XNAT.app._uploadFile,this);
			passthrough.file_dest=file_dest;
			passthrough.file_name=file_name;
			passthrough.fire();
		}
		
	}
}
   
function AddFolderForm(_obj){
  	this.obj=_obj;
    this.onResponse=new YAHOO.util.CustomEvent("response",this);
  
	this.render=function(){	
		this.panel=new YAHOO.widget.Dialog("fileUploadDialog",{close:true,
		   width:"440px",height:"400px",underlay:"shadow",modal:true,fixedcenter:true,visible:false});
		this.panel.setHeader("Add Folder");
				
		var div = document.createElement("form");
		
		
		var table,tbody,tr,td,input;
   	  
   	  var title=document.createElement("div");
   	  title.style.marginTop="3px";
   	  title.style.marginLeft="1px";
   	  title.innerHTML="<font size='+1' style='weight:700'>New Folder</font>";
   	  div.appendChild(title);
   	  
   	  div.appendChild(document.createElement("br"));
   	  
   	  var collection_form=document.createElement("div");
   	  div.appendChild(collection_form);
   	  collection_form.style.border="1px solid #DEDEDE";
   	  
   	  table=document.createElement("table");
   	  tbody=document.createElement("tbody");
   	  table.appendChild(tbody);
   	  collection_form.appendChild(table);
   	  
   	  
   	  if(this.obj.categories!=undefined){
	   	  //collection
	   	  tr=document.createElement("tr");
			tr.style.height="20px";
	   	  
	   	  td=document.createElement("th");
	   	  td.align="left";
	   	  td.innerHTML="Level";
	   	  tr.appendChild(td);
	   	  
	   	  td=document.createElement("td");
	   	  input=document.createElement("select");
	   	  input.id="folder_level";
   	  		input.style.fontSize = "99%";
	   	  input.manager=this;
	   	  input.options[0]=new Option("SELECT","");
	   	  for(var cIdC=0;cIdC<this.obj.categories.ids.length;cIdC++){
	   	    input.options[input.options.length]=new Option(this.obj.categories.ids[cIdC],this.obj.categories.ids[cIdC]);
	   	  }
	   	  input.options[input.options.length]=new Option("resources","resources");
	   	  
	   	  input.onchange=function(o){
	   	  	var item_select=document.getElementById("folder_item");
	   	  	var coll_select=document.getElementById("folder_collection");
	   	  	
	   	  	if(this.selectedIndex==0){
	   	  		while(item_select.options.length>0){
	   	  			item_select.remove(0);
	   	  		}
	   	  		coll_select.value="";
	   	  		item_select.disabled=true;
	   	  		coll_select.disabled=true;
	   	  	}else{
	   	  		while(item_select.options.length>0){
	   	  			item_select.remove(0);
	   	  		}
	   	  		coll_select.value="";
	   	  		var _v=this.options[this.selectedIndex].value;
	   	  		
	   	  		if(_v=="resources"){
	   	  			item_select.disabled=true;
	   	  			
	   	  			coll_select.value="";
	   	  		
	   	  			coll_select.disabled=false;
	   	  		}else{
	   	  			coll_select.disabled=true;
	   	  			item_select.disabled=false;
		   	  		var cat=this.manager.obj.categories[_v];
		   	  		
		   	  		item_select.options[item_select.options.length]=new Option("SELECT","");
		   	  		for(var catC=0;catC<cat.length;catC++){
		   	  			item_select.options[item_select.options.length]=new Option(cat[catC].id,cat[catC].id);
		   	  		}
	   	  		}
	   	  	}
	   	  };
	   	  td.appendChild(input);
	   	  tr.appendChild(td);
	   	  
	   	  tbody.appendChild(tr);
	   	  
	   	  
	   	  //collection
	   	  tr=document.createElement("tr");
			tr.style.height="20px";
	   	  
	   	  td=document.createElement("th");
	   	  td.align="left";
	   	  td.innerHTML="Item";
	   	  tr.appendChild(td);
	   	  
	   	  td=document.createElement("td");
	   	  input=document.createElement("select");
	   	  input.id="folder_item";
	   	  input.disabled=true;
	   	  input.manager=this;
   	  input.style.fontSize = "99%";
	   	  input.onchange=function(o){
	   	  	var coll_select=document.getElementById("folder_collection");
	   	  	if(this.selectedIndex>0){
	   	  		coll_select.disabled=false;
	   	  		coll_select.value="";
	   	  		var _v=this.options[this.selectedIndex].value;
   	  		  	  			
	   	  	}else{
	   	  		coll_select.disabled=true;
	   	  		coll_select.value="";
	   	  	}

	   	  };
	   	  td.appendChild(input);
	   	  tr.appendChild(td);
	   	  
	   	  tbody.appendChild(tr);
   	  }
   	  //collection
   	  tr=document.createElement("tr");
		tr.style.height="20px";
   	  
   	  td=document.createElement("th");
   	  td.align="left";
   	  td.innerHTML="Folder";
   	  tr.appendChild(td);
   	  
   	  td=document.createElement("td");
   	  input=document.createElement("input");
   	  input.id="folder_collection";
   	  input.style.fontSize = "99%";
   	  input.manager=this;
   	  td.appendChild(input);
   	  tr.appendChild(td);
   	  
   	  tbody.appendChild(tr);
   	  
   	  div.appendChild(document.createElement("br"));
   	  var file_form=document.createElement("div");
   	  div.appendChild(file_form);
   	  file_form.style.border="1px solid #DEDEDE";
   	  file_form.appendChild(document.createElement("div"));
   	  file_form.childNodes[0].innerHTML="<strong>Format Information</strong>";
   	  table=document.createElement("table");
   	  tbody=document.createElement("tbody");
   	  table.appendChild(tbody);
   	  file_form.appendChild(table);
   	     	  
   	  
   	  //format
   	  tr=document.createElement("tr");
		tr.style.height="20px";
   	  
   	  td=document.createElement("th");
   	  td.align="left";
   	  td.innerHTML="Format";
   	  tr.appendChild(td);
   	  
   	  td=document.createElement("td");
   	  input=document.createElement("input");
   	  input.id="folder_format";
   	  input.style.fontSize = "99%";
   	  td.appendChild(input);
   	  
   	  tr.appendChild(td);
   	  tbody.appendChild(tr);
   	  
   	  
   	  //content
   	  tr=document.createElement("tr");
		tr.style.height="20px";
   	  
   	  td=document.createElement("th");
   	  td.align="left";
   	  td.innerHTML="Content";
   	  tr.appendChild(td);
   	  
   	  td=document.createElement("td");
   	  input=document.createElement("input");
   	  input.id="folder_content";
   	  input.style.fontSize = "99%";
   	  td.appendChild(input);
   	  
   	  tr.appendChild(td);
   	  tbody.appendChild(tr);
   	  div.appendChild(file_form);
   	  
   	  
   	  //tags
   	  tr=document.createElement("tr");
		tr.style.height="20px";
   	  
   	  td=document.createElement("th");
   	  td.align="left";
   	  td.innerHTML="Tags";
   	  tr.appendChild(td);
   	  
   	  td=document.createElement("td");
   	  input=document.createElement("input");
   	  input.id="folder_tags";
   	  input.style.fontSize = "99%";
   	  td.appendChild(input);
   	  
   	  tr.appendChild(td);
   	  tbody.appendChild(tr);
   	  
   	  		
		this.panel.setBody(div);
		
		this.panel.selector=this;
		var buttons=[{text:"Create",handler:{fn:this.addFolder},isDefault:true},
			{text:"Close",handler:{fn:function(){
				this.cancel();
			}}}];
		this.panel.cfg.queueProperty("buttons",buttons);
		
		
		this.panel.render("page_body");
		this.panel.show();
	}
	
	this.addFolder=function (){
		var coll_select=document.getElementById("folder_collection");
	    if(coll_select.disabled==true){
	    	alert("Please select a folder for this file.");
	    	return;
	    }
	    
	    if(coll_select.value==""){
	    	alert("Please identify a folder name.");
	    	return;
	    }
	    
		var collection_name=coll_select.value;
		
		var file_tags=document.getElementById("folder_tags").value.trim();
		var file_format=document.getElementById("folder_format").value.trim();
		var file_content=document.getElementById("folder_content").value.trim();
		var folder_level=document.getElementById("folder_level");
		if(folder_level!=null && folder_level.selectedIndex==0){
			alert("Please select a level");
			return;
		}
		
		var file_params="?n=1&XNAT_CSRF=" + csrfToken;
		
		if(file_content!=""){
			file_params+="&content="+file_content;
		}
		if(file_format!=""){
			file_params+="&format="+file_format;
		}
		if(file_tags!=""){
			file_params+="&tags="+file_tags;
		}
			
			
		if(folder_level==null || folder_level.options[folder_level.selectedIndex].value=="resources"){
			var file_dest = this.selector.obj.uri+"/resources/"+ collection_name;
		}else{
			var folder_item=document.getElementById("folder_item");
			if(folder_item.selectedIndex==0){
				alert("Please select an item.");
				return;
			}
			file_dest =this.selector.obj.uri+"/" +
				 folder_level.options[folder_level.selectedIndex].value+ "/"+ 
				 folder_item.options[folder_item.selectedIndex].value+ "/"+ 
				 "resources/"+ 
				 collection_name;
		}				
		
		file_dest+=file_params;
		if(showReason){
			var justification=new XNAT.app.requestJustification("add_folder","Folder Creation Dialog",XNAT.app._addFolder,this);
			justification.file_dest=file_dest;
		}else{
			var passthrough= new XNAT.app.passThrough(XNAT.app._addFolder,this);
			passthrough.file_dest=file_dest;
			passthrough.fire();
		}
		
	}
	
	
}


XNAT.app._uploadFile=function(arg1,arg2,container){	 
	var event_reason=(container==undefined || container.dialog==undefined)?"":container.dialog.event_reason;
	var form = document.getElementById("file_upload");
	YAHOO.util.Connect.setForm(form,true);
	
	var callback={
		upload:function(obj1){
			window.viewer.refreshCatalogs("add_file");
			this.cancel();
		},
		scope:this
	}
	openModalPanel("add_file","Uploading File.")
	
	var method = 'POST';
	if(container.file_name!=""){
		method='PUT';
	}
	

	var params="&event_reason="+event_reason;
	params+="&event_type=WEB_FORM";
	params+="&event_action=File(s) uploaded";
	YAHOO.util.Connect.asyncRequest(method,container.file_dest+params,callback);
}

XNAT.app._addFolder=function(arg1,arg2,container){	   
	var event_reason=(container==undefined || container.dialog==undefined)?"":container.dialog.event_reason;
	var callback={
		success:function(obj1){
			closeModalPanel("add_folder");
			window.viewer.refreshCatalogs("add_folder");
			this.cancel();
		},
		failure:function(obj1){
			closeModalPanel("add_folder");
			if(obj1.status==409){
				alert('Specified resource already exists.');
			}else{
				alert(obj1.toString());
			}
			this.cancel();
		},
		scope:this
	}
	openModalPanel("add_folder","Creating folder.");
	
	var params="&event_reason="+event_reason;
	params+="&event_type=WEB_FORM";
	params+="&event_action=Folder Created";
	
	YAHOO.util.Connect.asyncRequest('PUT',container.file_dest+params,callback);
}