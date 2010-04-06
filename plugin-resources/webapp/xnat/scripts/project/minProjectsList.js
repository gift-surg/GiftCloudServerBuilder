function MinProjectsList(_div, _options){
  this.options=_options;
  this.div=_div;
  
  if(this.options==undefined){
  	this.options=new Object();
  	this.options.accessible=true;
  }
  
	this.init=function(){
		this.initLoader=prependLoader(this.div,"Loading projects");
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
		
		if(this.options.owner!=undefined){
			params += "&owner=true";
		}
		
		if(this.options.member!=undefined){
			params += "&member=true";
		}
		
		if(this.options.collaborator!=undefined){
			params += "&collaborator=true";
		}
		
		if(this.options.accessible!=undefined){
			params += "&accessible=true";
		}
		
		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/projects?format=json&stamp='+ (new Date()).getTime() + params,this.initCallback,null,this);
	};
	
	this.initFailure=function(o){
		//displayError("ERROR " + o.status+ ": Failed to load project list.");
		this.initLoader.close();
	};
	
	this.completeInit=function(o){
		try{
		    this.projectResultSet= eval("(" + o.responseText +")");
		}catch(e){
			//displayError("ERROR " + o.status+ ": Failed to parse project list.");
		}
		this.initLoader.close();
		try{
		    this.render();
		}catch(e){
			//displayError("ERROR : Failed to render project list.");
		}
	};
	
	this.render=function(){
		var display=document.getElementById(this.div);
		display.innerHTML="";
		var items=new Array();
		
		window.sort_field="last_accessed_"+this.projectResultSet.ResultSet.xdat_user_id;
		
		this.projectResultSet.ResultSet.Result=this.projectResultSet.ResultSet.Result.sort(function(a,b){
			if(a[window.sort_field]>b[window.sort_field])return -1;
			if(b[window.sort_field]>a[window.sort_field])return 1;
			return 0;
		});
		
		for(var pC=0;pC<this.projectResultSet.ResultSet.Result.length;pC++){
			var p=this.projectResultSet.ResultSet.Result[pC];
					
		
			var newDisplay = document.createElement("div");
			
			if(pC%2==0){
			  newDisplay.className="even";
			}else{
			  newDisplay.className="odd";
			}
			
			var row=document.createElement("div");
			row.innerHTML="<h3 style='margin-bottom:0px'><a href='"+ serverRoot + "/app/template/XDATScreen_report_xnat_projectData.vm/search_element/xnat:projectData/search_field/xnat:projectData.ID/search_value/" + p.id + "'>"+ p.name + "</a></h3>";
			newDisplay.appendChild(row);
			
			row=document.createElement("div");
			row.innerHTML="<b>Project ID: " + p.id +"</b>";
			if(p.pi!=undefined && p.pi!=""){
				row.innerHTML+="&nbsp;&nbsp;&nbsp;<b>PI: "+ p.pi +"</b>";
			}
			newDisplay.appendChild(row);
			
			row=document.createElement("div");
			if(p.description.length>160){
				row.innerHTML=p.description.substring(0,157) + "&nbsp;...";
			}else{
				row.innerHTML=p.description;
			}
			newDisplay.appendChild(row);
			
			row=document.createElement("div");
			if(p["user_role_"+this.projectResultSet.ResultSet.xdat_user_id]==""){
				if(p.project_access=="public"){
					row.innerHTML="This is an <b>open access</b> project.";
				}else{
					row.innerHTML="<a href='" + serverRoot + "/app/template/RequestProjectAccess.vm/project/" + p.id + "'>Request access</a> to this project."
				}
			}else{
				if(p["user_role_"+this.projectResultSet.ResultSet.xdat_user_id]=="Owners"){
					row.innerHTML="You are an <b>owner</b> for this project.";
				}else if(p["user_role_"+this.projectResultSet.ResultSet.xdat_user_id]=="Members"){
					row.innerHTML="You are a <b>member</b> for this project.";
				}else if(p["user_role_"+this.projectResultSet.ResultSet.xdat_user_id]=="Collaborators"){
					row.innerHTML="You are a <b>collaborator</b> for this project.";
				}
			}
			newDisplay.appendChild(row);
			
			display.appendChild(newDisplay);
		}
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