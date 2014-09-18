XNAT.app.featureMgr={
	features:[],
	groupLoadSuccess:function(resp){
		//once the existing settings are loaded from the server for these groups, the UI needs to be generated
		var parsedResponse = YAHOO.lang.JSON.parse(resp.responseText);
		var _html="";
		$(parsedResponse).each(function(i1,v1){
			var proj=parsedResponse[i1];
			$(proj.groups).each(function(i2,v2){
    			var group=proj.groups[i2];
    			_html+='<dl class="featureItem" id="">';
				if(XNAT.app.featureMgr.level=="group"){
    				_html+='<dd class="featureProject">' + proj.id +'</dd>';
    			}
				_html+='<dd class="featureGroup">' + group.display +'</dd>';
    			$(XNAT.app.featureMgr.features).each(function(i3,v3){
    				var feature=XNAT.app.featureMgr.features[i3];
    				_html+='<dd class="featureEnabled"><input type="checkbox"';
					_html+=' data-feature="' + feature.key + '" data-group="' + group.id + '" ';
    				
					if(proj.banned!=undefined && proj.banned.contains(feature.key)){
						_html+=' DISABLED ';
					}else if(XNAT.app.featureMgr.level=="group" && proj.onByDefault!=undefined && proj.onByDefault.contains(feature.key)){
    					//only used in project level view
						if(group.inherited_banned!=undefined && group.inherited_banned.contains(feature.key)){
    						if((group.features!=undefined && group.features.contains(feature.key))) {
            					_html+=' class="standard featureToggle" CHECKED '; 
            				}else{
        						_html+=' class="standard featureToggle"';
        					}
    					}else{
							if((group.blocked!=undefined && group.blocked.contains(feature.key))) {
            					_html+=' class="inherited inherited-global featureToggle"'; 
            				}else{
    							_html+=' class="inherited inherited-global featureToggle half-check"';
    						}
						}
					}else if(XNAT.app.featureMgr.level=="group" && group.inherited_features!=undefined && group.inherited_features.contains(feature.key)){
						//only used in project level view
						if((group.blocked!=undefined && group.blocked.contains(feature.key))) {
        					_html+=' class="inherited inherited-group featureToggle"'; 
        				}else{
							_html+=' class="inherited inherited-group featureToggle half-check"';
						}
					}else if(XNAT.app.featureMgr.level=="type" && proj.onByDefault!=undefined && proj.onByDefault.contains(feature.key)){
						//only used in site-wide (group-type) view
						if((group.blocked!=undefined && group.blocked.contains(feature.key))) {
        					_html+=' class="inherited inherited-type featureToggle"'; 
        				}else{
							_html+=' class="inherited inherited-type featureToggle half-check"';
						}
					}else if((group.features!=undefined && group.features.contains(feature.key))) {
    					_html+=' class="standard featureToggle" CHECKED '; 
    				}else{
						_html+=' class="standard featureToggle"';
					};
    				_html+='/></dd>';
    			});
    			_html+='</dl>';
			});
		});
		$("#featureBody").html(_html);
		$("input.half-check").each(function(i1,v1){
			//class=inherited implies that this feature was set to OnByDefault at a higher level.  So, it defaults to being on here, but not because it was specifically set here.
			$(v1).prop('indeterminate',true);
		});
		$("input.inherited.featureToggle").click(function(i1,v1){
			//had to set this ourselves because IE handles the onchange event weirdly for intermediate boxes.
			//so we are taking over the marking of enabled/disabled with our own variable to track it.
			var boxCh=this.checked;
			$(this).toggleClass("half-check")
			
			manageBlocked(this);
			return false;
		});
		$("input.standard.featureToggle").change(function(i1,v1){
			return manageEnable(this);
		});
	},
	groupFailure:function(resp){
		alert(resp);
	},
	init:function(){
		if(XNAT.app.featureMgr.level=="group"){
			//project level view
    		YAHOO.util.Connect.asyncRequest('GET',serverRoot+"/REST/services/features?tag=" + XNAT.app.featureMgr.project,{success : XNAT.app.featureMgr.groupLoadSuccess, failure : XNAT.app.featureMgr.groupFailure, cache : false, scope : this}, null, this);
		}else{
			//site wide (group-type) view
			YAHOO.util.Connect.asyncRequest('GET',serverRoot+"/REST/services/features?type=admin" ,{success : XNAT.app.featureMgr.groupLoadSuccess, failure : XNAT.app.featureMgr.groupFailure, cache : false, scope : this}, null, this);
		}
    }
};

function manageEnable(check){
	//enable/disable this feature/group combo
	var key=$(check).attr("data-feature");
	var tag=$(check).attr("data-group");
	
	YAHOO.util.Connect.asyncRequest('POST',serverRoot+"/REST/services/features?" + XNAT.app.featureMgr.level +"=" + tag + "&XNAT_CSRF=" + csrfToken,
		{success : featureSuccess, failure : featureFailure, cache : false, scope : check},
		"{'key':'" + key + "','enabled':" + check.checked + "}",
		check);
		
	$("input.featureToggle").attr("disabled", true);
}
function manageBlocked(check){
	//features that are on by default, need to be blocked rather than enabled.
	var key=$(check).attr("data-feature");
	var tag=$(check).attr("data-group");
	
	YAHOO.util.Connect.asyncRequest('POST',serverRoot+"/REST/services/features?" + XNAT.app.featureMgr.level +"=" + tag + "&XNAT_CSRF=" + csrfToken,
		{success : featureSuccess, failure : featureFailure, cache : false, scope : check},
		"{'key':'" + key + "','banned':" + !($(check).hasClass("half-check")) + "}", 
		check);
		
	$("input.featureToggle").attr("disabled", true);
}
function featureSuccess(res){
	$("input.featureToggle").removeAttr("disabled");
	if($(this).hasClass("inherited")){
		if($(this).hasClass("half-check")){
			$(this).prop('indeterminate',true);
		}else{
			$(this).prop('indeterminate',false);
		}
	}
}
function featureFailure(res){
	if($(this).hasClass("inherited")){
		$(this).toggleClass("half-check")
	}

	$("input.featureToggle").removeAttr("disabled");
}