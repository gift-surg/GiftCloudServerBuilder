function MinExptList(_div, _options){
  this.options=_options;
  this.div=_div;

  if(this.options==undefined){
  	this.options=new Object();
  	this.options.recent=true;
  }

	this.init=function(){
		this.initLoader=prependLoader(this.div,"Loading recent data");
		this.initLoader.render();
		//load from search xml from server
		this.initCallback={
			success:this.completeInit,
			failure:this.initFailure,
			scope:this
		}

		var params="";

		if(this.options.recent!=undefined){
			params += "&recent=true";
		}

		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/experiments?XNAT_CSRF=' + window.csrfToken + '&format=json' + params,this.initCallback,null,this);
	};

	this.initFailure=function(o){
		this.displayError("ERROR " + o.status+ ": Failed to load experiment list.");
		this.initLoader.close();
	};

	this.completeInit=function(o){
		try{
		    this.exptResultSet= eval("(" + o.responseText +")");
		}catch(e){
			this.displayError("ERROR " + o.status+ ": Failed to parse experiment list.");
		}
		this.initLoader.close();
		try{
		    this.render();
		}catch(e){
			this.displayError("ERROR : Failed to render experiment list.");
		}
	};

	this.render=function(){
		var items=new Array();

		var display=document.getElementById(this.div);
		var t = document.createElement("table");
		t.width="100%";
		t.cellSpacing="0px";
		var tb = document.createElement("tbody");

		for(var eC=0;eC<this.exptResultSet.ResultSet.Result.length;eC++){
			var e=this.exptResultSet.ResultSet.Result[eC];

			var tr = document.createElement("tr");

			if(eC%2==0){
			  tr.className="even";
			}else{
			  tr.className="odd";
			}

			var td = document.createElement("td");
			td.align="left";
			if(e.project.length>10){
			   td.innerHTML="<a title='" + e.project + "' href='" + serverRoot + "/REST/projects/" + e.project + "?format=html'>" + e.project.substring(0,7) + "...</a>";
			}else{
			   td.innerHTML="<a href='" + serverRoot + "/REST/projects/" + e.project + "?format=html'>" + e.project + "</a>";
			}
			tr.appendChild(td);

			td = document.createElement("td");
			td.align="left";
			td.innerHTML=e.type_desc;
			tr.appendChild(td);

			td = document.createElement("td");
			td.align="left";

			if(e.label==""){
				var tempLabel=e.id;
			}else{
				var tempLabel=e.label;
			}

			var labelLink="<a";
			labelLink+=" href='"+ serverRoot + "/app/action/DisplayItemAction/search_element/" + e.element_name + "/search_field/" + e.element_name + ".ID/search_value/" + e.id + "/project/" + e.project + "'";

			if(tempLabel.length>18){
				labelLink+=" title='" + tempLabel + "'>"+tempLabel.substring(0,15) + "...";
			}else{
				labelLink+=">"+tempLabel;
			}
			labelLink+="</a>";

			td.innerHTML=labelLink;

			tr.appendChild(td);


			td = document.createElement("td");
			td.align="right";

			if(e.action_date==""){
				e.action_date=e.insert_date;
			}

			switch(e.action_date){
				case e.workflow_date:
				  if(e.pipeline_name.indexOf('Transfer')==-1 && e.pipeline_name.indexOf('AutoRun')==-1){
					td.innerHTML="<A title='" + e.pipeline_name + " at " + e.workflow_date + "'>PROC<a>";
				  }else{
					td.innerHTML="<A title='Archived at " + e.workflow_date + "'>ARC<a>";
				  }
				  break;
				case e.last_modified:
				  td.innerHTML="<A title='Modified at " + e.last_modified + "'>MOD<a>";
				  break;
				case e.activation_date:
				  if(e.activation_date==e.insert_date)
				  	td.innerHTML="<A title='Created at " + e.insert_date + "'>NEW<a>";
				  else
				  	td.innerHTML="<A title='Released from Quarantine at " + e.activation_date + "'>REL<a>";
				  break;
				default:
				  td.innerHTML="NEW";
				  break;
			}
			tr.appendChild(td);
			tb.appendChild(tr);

//			tr.extension=eC+"_rExpt_tr";
//			tr.onclick=function(){
//				var extension=document.getElementById(this.extension);
//				extension.style.display=(extension.style.display=="none")?"":"none";
//			}
//			tr.style.cursor="pointer";

			tr= document.createElement("tr");
			tr.id=eC+"_rExpt_tr";
			tr.style.display="none";
			if(eC%2==0){
			  tr.className="even";
			}else{
			  tr.className="odd";
			}
			td = document.createElement("td");
			td.colSpan="4";
			td.innerHTML="&nbsp;";

			tr.appendChild(td);
			tb.appendChild(tr);
		}
		t.appendChild(tb);
		display.appendChild(t);
		//this.menu=new YAHOO.widget.Menu(this.div_id,{itemdata:items,visible:true, scrollincrement:5,position:"static"});

	}
}

	function prependLoader(div_id,msg){
		if(div_id.id==undefined){
			var div=document.getElementById(div_id);
		}else{
			var div=div_id;
		}
		var loader_div = document.createElement("div");
		loader_div.innerHTML=msg;
		div.parentNode.insertBefore(loader_div,div);
		return new XNATLoadingGIF(loader_div);
	}
