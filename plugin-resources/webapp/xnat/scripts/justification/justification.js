
XNAT.app.ConfirmWJustification=function(_yuioptions){
	this.yuioptions=_yuioptions;
	this.onResponse=new YAHOO.util.CustomEvent("response",this);

	if(this.yuioptions.width==undefined){
		this.yuioptions.width="400px";
	}	
	
	if(this.yuioptions.height==undefined){
		this.yuioptions.height="150px";
	}	
	
	if(this.yuioptions.close==undefined){
		this.yuioptions.close=true;
	}	
	this.yuioptions.underlay="shadow";
	this.yuioptions.modal=true;
	this.yuioptions.fixedcenter=true;
	this.yuioptions.visible=false;
	
	this.render=function(){
		this.panel=new YAHOO.widget.Dialog("justificationDialog",this.yuioptions);
		this.panel.setHeader(this.yuioptions.header);

		var bd = document.createElement("form");

		var table = document.createElement("table");
		var tb = document.createElement("tbody");
		table.appendChild(tb);
		bd.appendChild(table);

		//delete files
		tr=document.createElement("tr");
		td1=document.createElement("th");
		td2=document.createElement("td");		

		td1.innerHTML="Justification:";
		td1.align="left";
		var sel = document.createElement("textarea");
		sel.cols="24";
		sel.rows="4";
		sel.id="event_reason";
		sel.name="event_reason";
		td2.appendChild(sel);
		tr.appendChild(td1);
		tr.appendChild(td2);
		tb.appendChild(tr);

		this.panel.setBody(bd);

		this.panel.form=bd;

		this.panel.selector=this;
		var buttons=[{text:"Confirm",handler:{fn:function(){
				this.selector.event_reason = this.form.event_reason.value;
				if(this.selector.event_reason==""){
					alert("Please enter a justification!");
					return;
				}
				this.cancel();
				this.selector.onResponse.fire();
			}},isDefault:true},
			{text:"Cancel",handler:{fn:function(){
				this.cancel();
			}}}];
		this.panel.cfg.queueProperty("buttons",buttons);


		this.panel.render("page_body");
		this.panel.show();
	}
}

XNAT.app.requestJustification=function(_id,_header,_function,scope){
	this.id=_id;
	this.onCompletion=new YAHOO.util.CustomEvent("complete",this);
	
	this.options=new Object();
	this.options.header=_header;
	
	this.dialog=new XNAT.app.ConfirmWJustification(this.options);
	this.dialog.id=_id;
	this.dialog.header=_header;
	this.dialog.onResponse.subscribe(_function,this,scope);

	this.dialog.render();
}

//example pass-through function
function sample(){
	var event_reason=this.dialog.event_reason;

	this.initCallback={
		success:function(obj1){
			closeModalPanel(this.id);
			this.onCompletion.fire();
		},
		failure:function(o){
			closeModalPanel(this.id);
			this.displayError("ERROR " + o.status);
		},
		scope:this
	}
	
	var params="";
	
	params+="&event_reason="+event_reason;
	params+="&event_type=WEB_FORM";
	params+="&event_action="+this.header;

	openModalPanel(this.id,this.header);
	YAHOO.util.Connect.asyncRequest('DELETE',serverRoot +'/REST/experiments/' + this.options.session_id +'/scans/' + this.options.scan.getProperty("ID") +'?format=json'+params,this.initCallback,null,this);
}