RestDeleter = function(_array,_config) {
	RestDeleter.superclass.constructor.call(this,"rest_deleter",_config);
	this.a=_array;
	_config.title="Deletion Manager";
	_config.footer="Are you sure you want to permanently remove this data from the archive?&nbsp;&nbsp;(Data shared into this project will be un-shared, rather than deleted)";
	this.trArray=new Array();

	
	this.drawContents=function(_div){
		var t=_div.appendChild(document.createElement("table"));
		t.width="100%";
		var tb=t.appendChild(document.createElement("tbody"));
  	    for(var aC=0;aC<this.a.length;aC++){
  	    	if(this.a[aC].canRead && (this.a[aC].allowDelete==undefined||this.a[aC].allowDelete==true)){
	  	   	  	var tr=tb.appendChild(document.createElement("tr"));
	  	   	  	tr.entry=this.a[aC];
	  	   	  	
	  	   	  	var td1=tr.appendChild(document.createElement("td"));
	  	   	  	var td2=tr.appendChild(document.createElement("td"));
	  	   	  	tr.td1=td1;
	  	   	  	tr.td2=td2;
	  	   	  	
	  	   	  	td1.innerHTML=this.a[aC].label;
	  	   	  	tr.pDivColor=td2.appendChild(document.createElement("div"));
	  	   	  	tr.pDivColor.style.width="100%";
	  	   	  	tr.pDivColor.style.backgroundColor="gray";
	  	   	  	tr.pDivColor.style.color="white";
	  	   	  	tr.pDivColor.innerHTML="&nbsp;waiting...";
	  	   	  	this.trArray.push(tr);
  	    	}
  	    } 
		var NUMSPACES=this.config.defaultHeight/25;
		for (var j=0; j<NUMSPACES; j++){
			var tr=tb.appendChild(document.createElement("tr"));
			var td1=tr.appendChild(document.createElement("td"));
			td1.innerHTML="&nbsp;"
		}
		var tr=tb.appendChild(document.createElement("tr"));
		var td1=tr.appendChild(document.createElement("td"));
		td1.innerHTML="Are you sure you want to permanently remove this data from the archive?<br />(Data shared into this project will be un-shared, rather than deleted.)";
		td1.style.color="red";
		
	    var myButtons = [ { text:"Cancel", handler:this.handleCancel, isDefault:true }, { text:"Delete", handler:{fn:this.handleDelete, scope:this} } ];
		this.popup.cfg.queueProperty("buttons", myButtons);
    }
	
	
	this.handleDelete=function(){

		this.process();
	}
	
    this.handleCancel=function(){
    	if(this.manager.stopped){
    		window.location.reload();
    	}else if(this.manager.complete){
    		if(this.manager.redirect){
    			window.location=this.manager.redirect;
    		}else if(this.manager.redirectHome){
    			window.location=serverRoot + "";
    		}else{
	    		window.location.reload();
    		}
    	}else if(this.manager.processing){
    		this.manager.stopped=true;
			alert("Please wait for current process to complete.  Futher actions are cancelled.");
    	}else{
			window.location.reload();
		}
    }
    
    this.stopped=false;
    this.processing=false;

    this.process=function(){
    	if(!this.stopped){
			this.processing=true;
    		var params="";
    		var rF=document.getElementById("removeFiles");
  			if(rF==null || rF.checked){
    			params +="?removeFiles=true"
    		}
    		var matched=false;

    		for(var traC=0;traC<this.trArray.length;traC++){
    			this.currentTR=this.trArray[traC];
    			if(this.currentTR.processed==undefined){
    				if(this.currentTR.entry.redirectHome)this.redirectHome=true;
    				if(this.currentTR.entry.redirect)this.redirect=this.currentTR.entry.redirect;
		  	   	  	this.currentTR.pDivColor.style.backgroundColor="yellow";
		  	   	  	this.currentTR.pDivColor.style.color="black";
		  	   	  	this.currentTR.pDivColor.innerHTML="&nbsp;deleting...";
		  	   	  	
		     		deleteCB={
						success:function(obj1){
							this.currentTR.processed=true;
				  	   	  	this.currentTR.pDivColor.style.backgroundColor="green";
				  	   	  	this.currentTR.pDivColor.style.color="white";
				  	   	  	this.currentTR.pDivColor.innerHTML="&nbsp;complete&nbsp;";
				    		this.process();
						},
						failure:function(o){
							this.stopped=true;
							this.currentTR.pDivColor.style.backgroundColor="red";
				  	   	  	this.currentTR.pDivColor.style.color="black";
				  	   	  	this.currentTR.pDivColor.innerHTML="&nbsp;error&nbsp;";
				    		alert("ERROR " + o.status+ ": Failed to delete " + this.currentTR.entry.label);
							//this.popup.firstButton.textContent="Close";
						},
						scope:this
					}
					matched=true;
					YAHOO.util.Connect.asyncRequest('DELETE',serverRoot + this.currentTR.entry.ru + params,deleteCB,null,this);
		    		break;	
    			}
    		}
    		
    		if(!matched){
    			alert("All items were successfully deleted.");
    			if(this.redirect){
    				window.location=this.redirect;
	    		}else if(this.redirectHome){
	    			window.location=serverRoot + "";
	    		}else{
		    		window.location.reload();
	    		}
			//this.popup.firstButton.textContent="Close";
    		}
    	}else{
    		closeModalPanel("stopAction");
			//this.popup.firstButton.textContent="Close";
    	}
    }
    
     
  /*this.beforeInit=function(obj){
  	var msg="Are you sure you want to permanently remove this data ";
  	var rF=document.getElementById("removeFiles");
  	if(rF==null || rF.checked){
  		msg+="(including files) ";
  	}
  	msg+="from the archive?\r\n\r\n(Data shared into this project will be un-shared, rather than deleted)";
 	if(confirm(msg)){
 		return true;
 	}else{
 		return false;
 	}	
  }*/
};

YAHOO.extend(RestDeleter, BasePopup, {
});