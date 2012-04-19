function restLister(_info){
	this.info=_info;
	this.loaded=0;

	this.init=function(){
		if(this.loaded==0){
			this.loaded=1;
			var scanCallback={
				success:this.processScans,
				failure:this.handleFailure,
				scope:this
			};

			YAHOO.util.Connect.asyncRequest('GET',info.uri + '/resources?XNAT_CSRF=' + window.csrfToken + '&all=true&format=json&file_stats=true&timestamp=' + (new Date()).getTime(),scanCallback,null,this);
		}
	}

	this.handleFailure=function(o){
		alert("Error loading resources");
		this.loaded=0;
	}

	this.processScans=function(o){
		this.info.scans = new Array();
		var contents = eval("(" + o.responseText +")").ResultSet.Result;
		for(var i=0;i<contents.length;i++){
			if (contents[i].category==this.info.category) {
				this.info.scans.push(contents[i]);
			}
		}

		for(var i=0;i<this.info.scans.length;i++){
			var fileCallback={
				success:this.processFiles,
			    failure:this.handleFailure,
			    scope:this
			};
			var fileuri=this.info.uri + "/" + this.info.category + "/" + this.info.scans[i].cat_id + "/out/resources/" + this.info.scans[i].xnat_abstractresource_id;
			YAHOO.util.Connect.asyncRequest('GET',fileuri + '/files?XNAT_CSRF=' + window.csrfToken + '&all=true&format=json&timestamp=' + (new Date()).getTime(),fileCallback,null,this);
		}
	}

	this.getScan=function(cid) {
		for(var j=0;j<this.info.scans.length;j++){
			if (this.info.scans[j].xnat_abstractresource_id==cid) {
				return this.info.scans[j];
			}
		}
		return null;
	}

	this.processFiles=function(o){
		var allFiles = eval("(" + o.responseText +")").ResultSet.Result;
		var cid = allFiles[0].cat_ID;//same for each element in allFiles
//		if (cid==null && allFiles[0].URI!=null) {
//			var tmp = allFiles[0].URI.replace(this.info.uri + "/" + this.info.category + "/", "");
//			var scanID = tmp.substring(0,tmp.indexOf("/"));
//			cid = getCID(scanID);
//		}
		var scan = this.getScan(cid);
		if (scan!=null) {
			if (scan.files==null){
				scan.files = new Array();
			}
			for(var q=0;q<allFiles.length;q++){
				scan.files.push(allFiles[q]);
			}
		} else {
			alert("Null " + cid);
		}
	}
}
