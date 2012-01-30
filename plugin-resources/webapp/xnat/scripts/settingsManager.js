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
	};
	
	this.changeSuccess=function(o){
		this.obj.current_value=this._level;
		this.disableDOM(false);
		this.configButton();
	};
	
	this.changeFailure=function(o){
		if(o.status==401){
			alert("WARNING: Your session has expired.  You will need to re-login and navigate to the content.");
			window.location=serverRoot+"/app/template/Login.vm";
		}else{
			this.disableDOM(false);
			alert("ERROR " + o.status + ": Change failed.")
		}
	};
	
	this.setDefault=function(){
		for(var settingsC=0;settingsC<this.obj.radio_ids.length;settingsC++){
			if(this.obj.current_value==document.getElementById(this.obj.radio_ids[settingsC]).value){
				document.getElementById(this.obj.radio_ids[settingsC]).checked=true;
				break;
			}
		}
	};
	
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
	};
	
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
		};
		this.disableDOM(true);
		
        YAHOO.util.Connect.asyncRequest('PUT',this.obj.URI + this._level,this.settingsCallback);
	};
	
	//on init
	this.configButton();
	for(var settingsC=0;settingsC<this.obj.radio_ids.length;settingsC++){
		document.getElementById(this.obj.radio_ids[settingsC]).manager=this;
		document.getElementById(this.obj.radio_ids[settingsC]).onclick=function (){
			this.manager.configButton();
		};
	}
	
	document.getElementById(this.obj.button).manager=this;
	document.getElementById(this.obj.button).onclick=function (){
		this.manager.set();
	};
}

function scriptGet (_dom,_obj) {
  this.dom=_dom;
  /**
   * {
   *   getStatus : uri,
   *   getScript : uri,
   *   project : "",
   *   enable_radio : "",
   *   disable_radio : "",
   *   script_text_area : "",
   *   save_button : ""
   * }
   */
  
  this.obj=_obj;
  this.initial = {status : null, script : null};
  this.current = {status : null, script : null};
  this.onFailure=function(o) {
    if(o.status==401){
      alert("WARNING: Your session has expired.  You will need to re-login and navigate to the content.");
      window.location=serverRoot+"/app/template/Login.vm";
    }else{
      // this.disableDOM(false);
      alert("ERROR " + o.status + ": Get failed.");
    }
  };
  this.getDifferences=function() {
    var changed = [];
    for (var k in this.initial) {
      if (this.initial[k] !== this.current[k]) {
	changed.push(k);
      }
    }
    return changed;
  };
  this.determineDifference=function () {
    var changed = this.getDifferences();
    if (changed.length !== 0) {
      document.getElementById(this.obj.save_button).disabled=false;

    }
    else {
      document.getElementById(this.obj.save_button).disabled=true;
    }
  };
  this.addListeners=function() {
    var that = this;
    document.getElementById(that.obj.enable_radio).onclick=function() {
      that.current.status=true;
      that.determineDifference();
    };
    document.getElementById(that.obj.disable_radio).onclick=function() {
      that.current.status=false;
      that.determineDifference();
    };
    document.getElementById(that.obj.script_text_area).onkeyup=function() {
      that.current.script = document.getElementById(that.obj.script_text_area).value;
      that.determineDifference();
    };
    var resetInitial = function () {
      that.initial.status = that.current.status;
      that.initial.script = that.current.script;
      document.getElementById(that.obj.save_button).disabled=true;
    };
    var statusPut = function () {
      var uri = that.obj.putStatus+that.current.status;
      YAHOO.util.Connect.asyncRequest('PUT',uri,{success : resetInitial, failure : that.onFailure,scope : that});
    };
    var scriptPut = function (f) {
      YAHOO.util.Connect.asyncRequest('PUT',that.obj.putScript,{success : f,
                                                                 failure: that.onFailure,
								 scope: that},
				     that.current.script);
    };
    var contains = function(a,v) {
      var found = false;
      for (var i = 0; i < a.length; i++) {
	if (a[i] === v) {
	  found = true;
	  break;
	}
      }
      return found;
    };
    document.getElementById(that.obj.save_button).onclick=function() {
      var changes = that.getDifferences();
      if (contains(changes, "script") && contains(changes, "status")) {
	scriptPut(statusPut);
      }
      else if (contains(changes, "script")) {
	scriptPut(null);
      }
      else {
	statusPut();
      }
    };
  };
  this.get=function() {
    var parseResponse = function (o) {
      var resp = o.responseText;
      var parsedResponse = YAHOO.lang.JSON.parse(resp);
      return parsedResponse;
    };
    var statusGet = function (o) {
      var parsedResponse = parseResponse(o);
      var status = false;
      if (parsedResponse.ResultSet.Result.length !== 0) {
	status = parsedResponse.ResultSet.Result[0].edit === "true" ? true : false;
      }
      this.initial.status=status;
      this.current.status=status;
      document.getElementById(this.obj.enable_radio).checked=status;
      document.getElementById(this.obj.disable_radio).checked=!status;
    };
    var scriptGet = function (o) {
      var parsedResponse = parseResponse(o);
      var script = "";
      if (parsedResponse.ResultSet.Result.length !== 0) {
	script = parsedResponse.ResultSet.Result[0].script;
	YAHOO.util.Connect.asyncRequest('GET', this.obj.getStatus, {success : statusGet, failure : this.onFailure, scope : this});
      }
      this.initial.script = script;
      this.current.script = script;
            
      document.getElementById(this.obj.script_text_area).value=script;
    };
    
    YAHOO.util.Connect.asyncRequest('GET', this.obj.getScript, {success : scriptGet, failure : this.onFailure, scope : this});
  };
  this.addListeners();
}
