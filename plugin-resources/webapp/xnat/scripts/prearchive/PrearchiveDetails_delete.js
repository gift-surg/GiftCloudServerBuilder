XNAT.app.scanDeleter={
	requestDelete:function(scan_id){
		this.lastScan=scan_id;
		
		xModalConfirm({
          content: "Are you sure you want to delete scan "+scan_id+ "?",
          okAction: function(){
              XNAT.app.scanDeleter.doDelete();
          },
          cancelAction: function(){
          }
        });
	},
	doDelete:function(){
		this.delCallback={
            success:this.handleSuccess,
            failure:this.handleFailure,
            cache:false, // Turn off caching for IE
            scope:this
        };
		openModalPanel("delete_scan","Deleting scan " + this.lastScan);
		
		this.tempURL=serverRoot+"/REST" + this.url +"/scans/" + this.lastScan;
        YAHOO.util.Connect.asyncRequest('DELETE',this.tempURL+"?XNAT_CSRF=" + csrfToken,this.delCallback,null,this);
	},
	handleSuccess:function(o){
		closeModalPanel("delete_scan");
		$('#scanTR'+this.lastScan).remove();
	},
	handleFailure:function(o){
		closeModalPanel("delete_scan");
	    showMessage("page_body", "Error", "Failed to delete scan. ("+ e.message + ")");
	}
}