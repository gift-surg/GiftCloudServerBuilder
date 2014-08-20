XNAT.app.fileDialog = XNAT.app.fileDialog || {};

XNAT.app.fileDialog.loadScan = function( url, title ){

    openModalPanel("resource_loading","Loading");

    var modalOpts={};
    modalOpts.width = 700;
    modalOpts.height = 500;
    modalOpts.okLabel = 'Close';
    modalOpts.cancel = 'hide';

    var getData = $.ajax({
        type: 'GET',
        url: url,
        cache: false ,
        dataType: 'html'
    });

    getData.done(function(data){
        modalOpts.title = title;
        modalOpts.content = data;
        xModalOpenNew(modalOpts);
        closeModalPanel("resource_loading");
    });

    getData.fail(function(jqXHR, textStatus, errorThrown){
        modalOpts.title = 'Error - ' + title;
        modalOpts.content = 'Error: ' + textStatus;
        xModalOpenNew(modalOpts);
        closeModalPanel("resource_loading");
    });

};