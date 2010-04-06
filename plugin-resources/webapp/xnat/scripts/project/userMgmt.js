
/*******************************
 * Set of javascript functions to facilitate project-user access via AJAX
 */

var removeButtonFormatter = function(elCell, oRecord, oColumn, oData) {  
	 elCell.innerHTML="<input type=\"button\" ONCLICK=\"window.userManager.removeUser('" + oRecord.getData("login")  + "','" + oRecord.getData("GROUP_ID")  + "');\" value=\"Remove\"/>"; 
 }; 
	
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
     
function UserManager(user_mgmt_div_id, pID){
	this.pID=pID;
	this.user_mgmt_div = document.getElementById(user_mgmt_div_id);
	
	this.project_table_div=document.createElement("DIV");
	this.project_table_div.id="user_table";
	this.user_mgmt_div.appendChild(this.project_table_div);
	

	this.userColumnDefs=[
	  {key:"button",label:"Remove",formatter:removeButtonFormatter},
	  {key:"login",label:"Username",sortable:true},
	  {key:"firstname",label:"Firstname",sortable:true},
	  {key:"lastname",label:"Lastname",sortable:true},
	  {key:"email",label:"Email",sortable:true},
	  {key:"displayname",label:"Group",sortable:true}];

	this.allUserColumnDefs=[
	  {key:"button",label:"Add",formatter:"checkbox"},
	  {key:"login",label:"Username",sortable:true},
	  {key:"firstname",label:"Firstname",sortable:true},
	  {key:"lastname",label:"Lastname",sortable:true},
	  {key:"email",label:"Email",sortable:true}];
		
	this.init=function(){
		this.initLoader=prependLoader(this.project_table_div,"Loading project users");
		this.initLoader.render();
		//load from search xml from server
		this.initCallback={
			success:this.completeInit,
			failure:this.initFailure,
			scope:this
		}
		
		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/projects/'+ pID + '/users?format=json&stamp='+ (new Date()).getTime(),this.initCallback,null,this);
	};
	
	this.initFailure=function(o){
		this.initLoader.close();
		this.displayError("ERROR " + o.status+ ": Failed to load project user list.");
		if(o.status==401){
			alert("WARNING: Your session has expired.  You will need to re-login and navigate to the content.");
			window.location=serverRoot+"/app/template/Login.vm";
		}
	};
	
	this.completeInit=function(o){
		try{
		    this.userResultSet= eval("(" + o.responseText +")");
		    this.render();
		}catch(e){
			this.displayError("ERROR " + o.status+ ": Failed to parse project user list.");
		}
		this.initLoader.close();
		if(this.allUserResultSet==undefined)
     		this.loadAllUsers();
	};
    
		
    this.loadAllUsers=function(){
		this.allLoader=prependLoader("add_invite_user_header","Loading users");
		this.allLoader.render();
    	this.setFormDisabled(true);
		this.allUsersCallback={
			success:this.completeAllUsers,
			failure:this.allUsersFailure,
			scope:this
		}
		YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/REST/users?format=json&stamp='+ (new Date()).getTime(),this.allUsersCallback,null,this);
    }
	
	this.allUsersFailure=function(o){
		this.displayError("ERROR " + o.status+ ": Failed to load complete user list.");
		this.allLoader.close();
	};
	
	this.completeAllUsers=function(o){
		try{
		    this.allUserResultSet= eval("(" + o.responseText +")");
		    this.setFormDisabled(false);
			this.allLoader.close();
		    this.createPopup();
		}catch(e){
			this.displayError("ERROR " + o.status+ ": Failed to parse complete user list.");
			this.allLoader.close();
		}
	};
	
	this.render=function(){
 		this.userDataSource = new YAHOO.util.DataSource(this.userResultSet.ResultSet.Result);   
 		this.userDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;   
 		this.userDataSource.responseSchema = {   
		     fields: ["login","firstname","lastname","email","displayname","GROUP_ID"]            
	    }; 
	    
	    if (this.userResultSet.ResultSet.Result.length>6)
	      var config= {scrollable:true,height:"180"};
     	this.userDataTable = new YAHOO.widget.DataTable("user_table", this.userColumnDefs,this.userDataSource,config);  
   	     	
	}
	
	this.setFormDisabled=function(value){
		var user_invite_div=document.getElementById("user_invite_div");
		var inputs=user_invite_div.getElementsByTagName("input");
		for(var inputCounter in inputs){
			inputs[inputCounter].disabled=value;
		}
		
		var selects=user_invite_div.getElementsByTagName("select");
		for(var selectsCounter in selects){
			selects[selectsCounter].disabled=value;
		}
	}
	
	this.resetForm=function(){
		document.getElementById("invite_email").value='';
		document.getElementById("invite_email").focus();
	}
		
	this.completeInvite=function(o){
		this.completeInit(o);
		this.setFormDisabled(false);
		this.resetForm();
	};
	
	this.inviteFailure=function(o){
		if(o.status==401){
			alert("WARNING: Your session has expired.  You will need to re-login and navigate to the content.");
			window.location=serverRoot+"/app/template/Login.vm";
		}
		alert("ERROR " + o.status +": Operation Failed.");
		this.setFormDisabled(false);
	};
	
	this.inviteUser=function(emails,access_level){
      if (emails!=undefined && access_level!=undefined){
        var post_url = serverRoot + "/REST/projects/" + this.pID + "/users/" + access_level;
        var email_array=emails.split(',');
        var unknownUsers=new Array();
        var knownUsers=new Array();
        for(var email_counter=0;email_counter<email_array.length;email_counter++){
        	var email = email_array[email_counter].trim();
	        if (!this.userExists(email))
	        {
	        	unknownUsers.push(email);
	        }else{
	        	knownUsers.push(email);
	        }
        }
        
        if(unknownUsers.length>0){
	        var confirmMsg=unknownUsers;
	        if(unknownUsers.length>1)
	        	confirmMsg+=" do not correspond to currently registered accounts.  These users ";
	        else
	        	confirmMsg+=" does not correspond to a currently registered accounts.  This user ";
	        
	        if (!confirm(confirmMsg + 'may have an account under another email address.  Select cancel to enter a different account.  Or, press OK to send an email inviting this user.')){
	           document.getElementById("invite_email").value='';
	           return false;
	        }
        }
        
        
       if(knownUsers.length>0){
	        var confirmMsg="Would you like an email to be sent to " + knownUsers;
	        if(knownUsers.length>1)
	        	confirmMsg+=" to inform them of this addition?";
	        else
	        	confirmMsg+=" to inform him/her of this addition?";
	        
	        if (confirm(confirmMsg)){
	            var sendmail=true;
	        }else
	        {
	        	var sendmail=false;
	        }
        }
        
		this.setFormDisabled(true);
       
		this.insertCallback={
			success:this.completeInvite,
			failure:this.inviteFailure,
			scope:this
		}
		
		var params = "format=json";
		if(sendmail){
			params+="&sendmail=true";
		}
		//alert(post_url + "/" + emails);
        YAHOO.util.Connect.asyncRequest('PUT',post_url + "/" + emails,this.insertCallback,params,this);
        return true;
      }
    }
	
	this.inviteUserFromForm=function(){
  	 var invite_email=document.getElementById("invite_email");
  	 var email = invite_email.value;
     if (email!=""){
      	var access_level=document.getElementById("access_level").options[document.getElementById("access_level").selectedIndex].value;
		this.inviteUser(email,access_level);
      }
    }
	
	this.completeRemoval=function(o){
		this.completeInit(o);
		this.setFormDisabled(false);
	};
	
	this.removeUser=function (login,group){
		this.setFormDisabled(true);
		       
		this.deleteCallback={
			success:this.completeRemoval,
			failure:this.inviteFailure,
			scope:this
		}
		var post_url = serverRoot + "/REST/projects/" + this.pID + "/users/" + group;
       YAHOO.util.Connect.asyncRequest('DELETE',post_url + "/" + login + "?format=json",this.deleteCallback,null,this);
	}
	
	this.userExists=function (email){
	  var all_users= window.userManager.allUserResultSet.ResultSet.Result;
	  for(var user_count=0;user_count<all_users.length;user_count++){
	     if(all_users[user_count].email==email || all_users[user_count].login==email){
	        return true;
	     }
	  }
	}
	
	this.displayError=function(errorMsg){
	    var data_table = document.getElementById("user_table");
	    data_table.className="error";
	    data_table.innerHTML=errorMsg;	    
	}
	
	this.createPopup=function(){
		this.popupLoader=prependLoader("user_list_header","Preparing user list");
		this.popupLoader.render();
		var popupDIV = document.createElement("DIV");
		popupDIV.id="all_users_popup";
		var popupHD = document.createElement("DIV");
		popupHD.className="hd";
		popupDIV.appendChild(popupHD);
		var popupBD = document.createElement("DIV");
		popupBD.className="bd";
		popupDIV.appendChild(popupBD);
		
		popupHD.innerHTML="Select User(s) and the desired level of access";
		
		var access_level_select=document.createElement("select");
		access_level_select.id="popupSelect";
		access_level_select.style.marginLeft="5pt";
		access_level_select.options[0]= new Option("SELECT","");
		access_level_select.options[1]= new Option("owner","owner");
		access_level_select.options[2]= new Option("member","member");
		access_level_select.options[3]= new Option("collaborator","collaborator");
		
		var access_label =document.createElement("label");		
		access_label.innerHTML="Access Level:";	
		access_label.appendChild(access_level_select);
		popupBD.appendChild(access_label);
		
		popupBD.appendChild(document.createElement("br"));
		
		var all_users_table = document.createElement("div");
		all_users_table.id="all_users_table";
		all_users_table.style.marginTop="5pt";
		popupBD.appendChild(all_users_table);
		
		//add to page
		var tp_fm=document.getElementById("tp_fm");
		tp_fm.appendChild(popupDIV);
		
		this.allUsersPopup=new YAHOO.widget.Dialog(popupDIV,{zIndex:999,visible:false,width:"520px"});
		this.allUsersPopup.access_level_select=access_level_select;
		
		var handleCancel = function() {
			this.hide();
		}
		
		var handleSubmit = function() {
			if(this.access_level_select.selectedIndex>0){
				var selected_users=this.allUsersDataTable.selected_users;
				if(selected_users.length>0){
					var usernames=new Array();
					for(var user_count=0;user_count<selected_users.length;user_count++){
						if(selected_users[user_count].checkbox.checked){
							if(!usernames.contains(selected_users[user_count].login))
								usernames.push(selected_users[user_count].login);
						}
					}
					
					if(usernames.length==0){
						this.allUsersDataTable.selected_users=new Array();
						this.hide();
						return;
					}
					
					if(window.userManager.inviteUser(usernames.toString(),this.access_level_select.options[this.access_level_select.selectedIndex].value)){
						for(var user_count=0;user_count<selected_users.length;user_count++){
							if(selected_users[user_count].checkbox.checked){
								selected_users[user_count].checkbox.click();
							}
						} 
						
						this.allUsersDataTable.selected_users=new Array();
						this.hide();
					}
				}else{
					this.hide();
				}
			}else{
				alert("Please selected an access level.");
				this.access_level_select.focus();
			}
			
		}
	    var myButtons = [ { text:"Submit", handler:handleSubmit, isDefault:true },
						  { text:"Cancel", handler:handleCancel } ];
		this.allUsersPopup.cfg.queueProperty("buttons", myButtons);
		
		
		//build all users datatable
		this.allUsersPopup.alluserDataSource = new YAHOO.util.DataSource(this.allUserResultSet.ResultSet.Result);   
		this.allUsersPopup.alluserDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;   
		this.allUsersPopup.alluserDataSource.responseSchema = {   
		     fields: ["login","firstname","lastname","email","displayname"]            
    	}; 
 		this.allUsersPopup.allUsersDataTable = new YAHOO.widget.DataTable("all_users_table", this.allUserColumnDefs,this.allUsersPopup.alluserDataSource,{scrollable:true,height:"300px",width:"500px"});  
		
		this.allUsersPopup.allUsersDataTable.subscribe("checkboxClickEvent", function(oArgs){   
             var elCheckbox = oArgs.target;   
             var oRecord = this.getRecord(elCheckbox); 
             if(elCheckbox.checked){
             	this.selected_users.push({login:oRecord.getData("login"),checkbox:elCheckbox});
             }
        }); 
		this.allUsersPopup.allUsersDataTable.selected_users=new Array();
		
		//this.allUsersPopup.allUsersDataTable.render();
		
		this.allUsersPopup.render();
		
		this.allUsersPopup.show();
		this.allUsersPopup.hide();
		
		document.getElementById("popup_all_users_button").disabled=false;
		this.popupLoader.close();
	}
	
	/**************
	 * Popup dialog box which allows existing users to be selected and added to project.
	 */
	this.popupAllUsersBox=function(){
		if(this.allUsersPopup==undefined){
			this.createPopup();
		}
		
		document.getElementById("tp_fm").style.display="block";
		
		this.allUsersPopup.show();
	}
	
	this.init();
}


function DefaultAccessibilityManager(_dom,_pID){
	this.dom=_dom;
	this.pID=_pID;
	
	this.disableDOM=function(_val){
		if(_val){
		    if(this.popupLoader==undefined){
		    	this.popupLoader=prependLoader("accessibility_save","Saving");
				this.popupLoader.render();
		    }
			this.dom.style.color="#DEDEDE";
		}else{
		    if(this.popupLoader!=undefined){
				this.popupLoader.close();
				this.popupLoader=null;
		    }
			this.dom.style.color="";
		}
		document.getElementById("private_access").disabled=_val;
		document.getElementById("protected_access").disabled=_val;
		document.getElementById("public_access").disabled=_val;
		document.getElementById("accessibility_save").disabled=_val;
	}
	
	this.changeSuccess=function(o){
		this.disableDOM(false);
		document.getElementById('current_accessibility').value=this._level;
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
	
	this.set=function(){
		if(document.getElementById("private_access").checked){
			this._level=document.getElementById("private_access").value;
		}else if(document.getElementById("protected_access").checked){
			this._level=document.getElementById("protected_access").value;
		}else if(document.getElementById("public_access").checked){
			this._level=document.getElementById("public_access").value;
		}
		this.accessibilityCallback={
			success:this.changeSuccess,
			failure:this.changeFailure,
			scope:this
		}
		this.disableDOM(true);
		
       YAHOO.util.Connect.asyncRequest('PUT',serverRoot + "/REST/projects/" + this.pID + "/accessibility/" + this._level,this.accessibilityCallback,null,this);
	}
	
}