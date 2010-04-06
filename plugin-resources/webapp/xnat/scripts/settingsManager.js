//requires settings object
// {"URI":"","radio_ids":["id1","id2"],"current_selected":"id2","msg":"Saving","color":"#DEDEDE","button":"idy"}

function RadioSettingsManager(_dom,_obj){
	this.dom=_dom;
	this.obj=_obj;
	
	if(this.obj.msg==undefined){
		this.obj.msg="Saving";
	}
	if(this.obj.color==undefined){
		this.obj.color="#DEDEDE";
	}
	
	this.disableDOM=function(_val){
		if(_val){
		    if(this.popupLoader==undefined){
		    	if(this.obj.divID){
			    	this.popupLoader=prependLoader(this.dom,this.obj.msg);
					this.popupLoader.render();
		    	}
		    }
			this.dom.style.color=this.obj.color;
		}else{
		    if(this.popupLoader!=undefined){
				this.popupLoader.close();
				this.popupLoader=null;
		    }
			this.dom.style.color="";
		}
		
		for(var settingsC=0;settingsC<this.obj.radio_ids.length;settingsC++){
			document.getElementById(this.obj.radio_ids[settingsC]).disabled=_val;
		}
		
		document.getElementById(this.obj.button).disabled=_val;
	}
	
	this.changeSuccess=function(o){
		this.obj.current_value=this._level;
		this.disableDOM(false);
		this.configButton();
	}
	
	this.changeFailure=function(o){
		if(o.status==401){
			alert("WARNING: Your session has expired.  You will need to re-login and navigate to the content.");
			window.location=serverRoot+"/app/template/Login.vm";
		}else{
			this.disableDOM(false);
			alert("ERROR " + o.status + ": Change failed.")
		}
	}
	
	this.setDefault=function(){
		for(var settingsC=0;settingsC<this.obj.radio_ids.length;settingsC++){
			if(this.obj.current_value==document.getElementById(this.obj.radio_ids[settingsC]).value){
				document.getElementById(this.obj.radio_ids[settingsC]).checked=true;
				break;
			}
		}
	}
	
	this.configButton=function(){
		for(var settingsC=0;settingsC<this.obj.radio_ids.length;settingsC++){
			if(document.getElementById(this.obj.radio_ids[settingsC]).checked){
				this._level=document.getElementById(this.obj.radio_ids[settingsC]).value;
				break;
			}
		}
		
		if(this._level==undefined){
			this.setDefault();
			document.getElementById(this.obj.button).disabled=true;
		}else{
			if(this.obj.current_value==this._level){
				document.getElementById(this.obj.button).disabled=true;
			}else{
				document.getElementById(this.obj.button).disabled=false;
			}
		}
	}
	
	this.set=function(){
		for(var settingsC=0;settingsC<this.obj.radio_ids.length;settingsC++){
			if(document.getElementById(this.obj.radio_ids[settingsC]).checked){
				this._level=document.getElementById(this.obj.radio_ids[settingsC]).value;
				break;
			}
		}
		this.settingsCallback={
			success:this.changeSuccess,
			failure:this.changeFailure,
			scope:this
		}
		this.disableDOM(true);
		
        YAHOO.util.Connect.asyncRequest('PUT',this.obj.URI + this._level,this.settingsCallback);
	}
	
	//on init
	this.configButton();
	for(var settingsC=0;settingsC<this.obj.radio_ids.length;settingsC++){
		document.getElementById(this.obj.radio_ids[settingsC]).manager=this;
		document.getElementById(this.obj.radio_ids[settingsC]).onclick=function (){
			this.manager.configButton();
		}
	}
	
	document.getElementById(this.obj.button).manager=this;
	document.getElementById(this.obj.button).onclick=function (){
		this.manager.set();
	}
}