//Author: Tim Olsen <tim@deck5consulting.com>

XNAT.app.fileDialog={
	loaded:false,
	ready:false,
	show:function(url){
		//if its initialized, then show it, otherwise initialize it.
		if(!this.loaded){
			this.loaded=true;
			this.start(url);
		}else if(this.ready){
			XNAT.app.fileDialog.dialog.render(document.body);
			XNAT.app.fileDialog.dialog.show();
		}
	},
	loadScan:function(url, title){
		this.loaded=false;
		this.ready=false;
		this.dialog.hide();
		YUIDOM.get('filesDialog_content').innerHTML="";
		YUIDOM.get('filesDialog_header').innerHTML=title;
		this.show(url);
	},
	start:function(url){		  		
		//triggers the initial load of the resource list for the select box
		openModalPanel("resource_loading","Loading");
		YAHOO.util.Connect.asyncRequest('GET', url, {success : this.handleLoad, failure : function(){
			closeModalPanel("resource_loading");
		}, cache : false, scope : this});
	},
	handleLoad:function(obj){
		//handles the response with the available resources for this project
		closeModalPanel("resource_loading");
		YUIDOM.get('filesDialog_content').innerHTML=obj.responseText;

		this.ready=true;
		this.show();
	}
}


//initialize modal upload dialog
XNAT.app.fileDialog.dialog=new YAHOO.widget.Dialog("files_dialog", { fixedcenter:true, visible:false, width:"700px", height:"500px", modal:true, close:true, draggable:true } ),
XNAT.app.fileDialog.dialog.cfg.queueProperty("buttons", [{ text:"Close", handler:{fn:function(){XNAT.app.fileDialog.dialog.hide();}}, isDefault:true}]);
