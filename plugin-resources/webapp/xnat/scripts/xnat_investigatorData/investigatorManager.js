function InvestigatorManager(){
	this.investigatorsLoaded=new YAHOO.util.CustomEvent("investigators-loaded",this);
	this.init=function(){
		//load from search xml from server
		this.initCallback={
			success:this.completeInit,
			failure:this.initFailure,
			scope:this
		}
		
		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/investigators?XNAT_CSRF=' + window.csrfToken + '&format=json',this.initCallback,null,this);
	};
	
	this.initFailure=function(o){
		this.alert("ERROR " + o.status+ ": Failed to load investigator list.");
	};
	
	this.completeInit=function(o){
		try{
		    this.investigatorResultSet=ResultSet= eval("(" + o.responseText +")");
		    this.investigators=this.investigatorResultSet.ResultSet.Result;
		    this.investigatorsLoaded.fire();
		}catch(e){
			this.alert("ERROR " + o.status+ ": Failed to parse investigator list.");
		}
	}
	
	this.insert=function(investigator){
		var parasm="investigator_xml="+investigator.toXML("");
		YAHOO.util.Connect.asyncRequest('POST',serverRoot +'/REST/investigators/' + investigator.lastname +'?format=json&XNAT_CSRF=' + csrfToken,this.initCallback,null,this);
	}
	
	this.populateSelect=function(_select,_selectedID){
	  	_select.options[0]=new Option("SELECT","");
	  	
	  	for(var investCounter=0;investCounter<this.investigators.length;investCounter++){
	  		var tempInvestigator=this.investigators[investCounter];
	  		var investString=tempInvestigator.lastname+", "+tempInvestigator.firstname;
	  		var investID=tempInvestigator.xnat_investigatordata_id;
	  		_select.options[(investCounter+1)]=new Option(investString,investID,(_selectedID!=undefined && investID==_selectedID)?true:false,(_selectedID!=undefined && investID==_selectedID)?true:false);
	  		if(_selectedID!=undefined && investID==_selectedID){
	  			_select.selectedIndex=investCounter+1;
	  		}
	  	}
	}
}