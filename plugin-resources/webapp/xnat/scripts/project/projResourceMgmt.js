//Copyright 2014 Washington University
//Author: Tim Olsen <tim@deck5consulting.com>

/* 
 * javascript for ResourceManagement.  Used to configure the expected resources for a project
 */
XNAT.app.pResources={
	configs:new Array(),
	settingsDialog:new YAHOO.widget.Dialog("pResource_settings_dialog", { fixedcenter:true, visible:false, width:"800px", height:"600px", modal:true, close:true, draggable:true,resizable:true}),
	begin:function(){
		this.load();

		this.settingsDialog.render(document.body);
		this.settingsDialog.show();
	},
	reset:function(){
		$("#pResource_form").html("");
		$("#pResource_form").hide();
		$("#pResource_exist").html("");
		$("#pResource_exist").height(430);
	},
	menu:function(level){
		$("#pResource_form").html("");
		var temp_html="";
		temp_html="<div class='row1'><span><label for='pResource.name'>Title: </label> <input class='pResourceField' required='true' data-prop-name='name' type='text' id='pResource.name' value='' placeholder='Name user sees'/></span>";
		if(level=="proj"){
			temp_html+="<input class='pResourceField' data-prop-name='type' type='hidden' id='pResource.type' value='xnat:projectData'/>";
		}else if(level=="subj"){
			temp_html+="<input class='pResourceField' data-prop-name='type' type='hidden' id='pResource.type' value='xnat:subjectData'/>";
		}else if(level=="sa"){
			temp_html+=" <span><label for='pResource.type'>Select data-type: </label> <select id='pResource.type' class='pResourceField' data-prop-name='type'>" +
					"<option value='xnat:subjectAssessorData'>All</option>" +
					"<option value='xnat:imageSessionData'>Image Sessions</option>";
			$.each(window.available_elements,function( index, value ) {
				if(value.isSubjectAssessor){
					temp_html+="<option value='" + value.element_name + "'>"+ value.singular+"</option>";
				}
			});
			temp_html+="</select></span>";
		}else if(level=="scan"){
			temp_html+="<input class='pResourceField' data-prop-name='type' type='hidden' id='pResource.type' value='xnat:imageScanData'/>";
		}else if(level=="ia"){
			temp_html+=" <span><label for='pResource.type'>Select data-type: </label> <select id='pResource.type' class='pResourceField' data-prop-name='type'>" +
					"<option value='xnat:imageAssessorData'>All</option>";
			$.each(window.available_elements,function( index, value ) {
				if(value.isImageAssessor){
					temp_html+="<option value='" + value.element_name + "'>"+ value.singular+"</option>";
				}
			});
			temp_html+="</select></span>";
			temp_html+="<span><label for='pResource.level'>Level: </label> <select id='pResource.level' class='pResourceField' data-prop-name='level'>";
			temp_html+="<option value='default'>DEFAULT (resources)</option>";
			temp_html+="<option value='out'>outputs dir (out)</option>";
			temp_html+="<option value='in'>inputs dir (in)</option>";
			temp_html+="</select></span>";
		}
		temp_html+=" <span><label for='pResource.label'>Label: </label> <input class='pResourceField' required='true' data-prop-name='label' size='10' type='text' id='pResource.label' required=true placeholder='ex. DICOM' data-regex='^[a-zA-Z0-9_-]+$' /></span>";
		temp_html+=" <span><label for='pResource.subdir'>Sub-directory: </label> <input class='pResourceField' data-prop-name='subdir' type='text' id='pResource.subdir' placeholder='(optional) ex. data/sub/dir' size='24' data-regex='^[a-zA-Z0-9_\\-\\/]+$'/></span>";
		temp_html+=" <span><label for='pResource.overwrite'>Allow overwrite</label><input class='pResourceField' data-prop-name='overwrite' type='checkbox' id='pResource.overwrite'/></div>";
		temp_html+=" <div class='row2'><label for='pResource.desc'>Description: </label> <textarea class='pResourceField' style='width:500px' data-prop-name='description' id='pResource.desc' placeholder='(optional) Descriptive text explaining details to the user about this resource.' /></div>";
		temp_html+=" <div class='row3'><button id='cruCancelBut' onclick='$(\"#pResource_form\").html(\"\");$(\"#pResource_form\").hide();$(\"#pResource_exist\").height(430)'>Cancel</button><button class='default' id='cruAddBut' onclick='XNAT.app.pResources.add();'>Add</button></div>";
		temp_html+=" <div class='row4'></div>";
		$("#pResource_form").html(temp_html);
		$("#pResource_form").show();
		$("#pResource_exist").height(430-$("#pResource_form").height());
	},
	add:function(){		
		var valid=true;
		
		var props=new Object();
		
		//iterate over the fields in the form
		$(".pResourceField").each(function(){
			if($(this).attr('required')=='required' && $(this).val()==""){
				if($(this).attr('required-msg'))
				{
					xModalMessage("Required field",$(this).attr('required-msg'));
				}else{
					xModalMessage("Required field",$(this).attr('data-prop-name') + " is required.");
				}
				valid=false;
			}
			
			//check if this form field has a regex defined
			if($(this).attr('data-regex') !=undefined && $(this).val()!=""){
				if(! (new RegExp($(this).attr('data-regex'))).test($(this).val())){
					if($(this).attr('data-regex-msg'))
					{
						xModalMessage("Invalid value",$(this).attr('data-regex-msg'));
					}else{
						xModalMessage("Invalid value",$(this).attr('data-prop-name') + " has an invalid character.");
					}
					valid=false;
				}
			}
			
			if($(this).attr('type')=="checkbox"){
				props[$(this).attr('data-prop-name')]=$(this).is(':checked');
			}else{
				props[$(this).attr('data-prop-name')]=$(this).val();
			}
		});
		
		if(valid){
			this.configs.push(props);

			this.save();
		}
	},
	save:function(){
		openModalPanel("saveResource","Saving resource configurations.");
		YAHOO.util.Connect.asyncRequest('PUT',serverRoot+"/data/projects/" + this.id +"/config/resource_config/script?inbody=true&XNAT_CSRF=" + window.csrfToken,
        {	
        	success : function()
        	{
        		closeModalPanel('saveResource');
        		XNAT.app.pResources.load();
        	},
        	 failure: function(){
        	 	closeModalPanel('saveResource');
        	 	xModalMessage("Exception","Failed to store configuration.");
        	 },
             cache:false, // Turn off caching for IE
        	 scope: this
        },
         YAHOO.lang.JSON.stringify(this.configs));
	},
	load:function(){
		this.reset();
		
		YAHOO.util.Connect.asyncRequest('GET', serverRoot+'/data/projects/' + this.id +'/config/resource_config/script?format=json', {success : this.handleLoad, failure : function(){}, cache : false, scope : this});
	},
	handleLoad:function(obj){
		var parsedResponse = YAHOO.lang.JSON.parse(obj.responseText);
	    var script = "";
	    if (parsedResponse.ResultSet.Result.length !== 0) {
	    	script = parsedResponse.ResultSet.Result[0].script;
			//sort of a hack to get this code to work with generic nrg_config return values
			if(script == undefined ){
				script = parsedResponse.ResultSet.Result[0].contents;
			}
			
			this.configs=YAHOO.lang.JSON.parse(script);
		}
	    this.render();
	},
	render:function(){
		//identify columns
		var tmpHtml="<dl class='header'><dl><dd class='col1'>&nbsp;</dd><dd class='colL'>Type</dd><dd class='colM'>Name</dd><dd class='colM'>Label</dd><dd class='colL'>Sub-directory</dd><dd class='colM'>Overwrite?</dd><dd class='colS'>Level</dd></dl></dl>	";
		jq.each(this.configs,function(i1,v1){
			tmpHtml+="<dl class='item'><dd class='col1'><button onclick='XNAT.app.pResources.remove(\"" + i1 +"\");'>Remove</button></dd><dd class='colL'>"+v1.type +"</dd><dd class='colM'>"+v1.name +"</dd><dd class='colM'>"+v1.label +"</dd><dd class='colL'>"+v1.subdir +"&nbsp;</dd><dd class='colM'>"+((v1.overwrite)?v1.overwrite:"") +"</dd><dd class='colS'>"+((v1.level)?v1.level:"") +"</dd>";
			if(v1.description){
				tmpHtml+="<dd class='colX'><b>Description:</b> "+v1.description +"</dd></dl>";
			}else{
				tmpHtml+="</dl>";
			}
		});
		$("#pResource_exist").html(tmpHtml);
	},
	remove:function(index){
		this.configs.splice(index,1);
		this.save();
	}
}
//implements button functionaly
XNAT.app.pResources.settingsDialog.cfg.queueProperty("buttons", [
   { text:"Close", handler:{fn:function(){
	   	XNAT.app.pResources.reset();
	   	XNAT.app.pResources.settingsDialog.hide();
   }},isDefault:true}
   ]);