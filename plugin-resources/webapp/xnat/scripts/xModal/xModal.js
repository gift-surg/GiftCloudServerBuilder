
// debug/log function - so IE doesn't freakout on console.debug
function debugMe(info){
    if (console.debug){
        console.debug('debug: '+info);
    }
    console.log('log: '+info);
}

// Not part of xModal, but needed to find stuff.
// This would normally already be defined before xModal.js is called.
// Modify as needed to reflect path of "scripts" directory, from site root (no trailing slash).
var serverRoot, scripts_dir ;
if (!scripts_dir){
    scripts_dir = serverRoot+'/scripts' ;
    debugMe('"scripts_dir" is now defined');
}
else {
    debugMe('"scripts_dir" was already defined');
}


/*
 Javascript for xModal
 */

// Global vars we're going to use
var
    $html,
    $body,
    xmodal_count = 0
    ;

// init xModal object
var xModal = {} ;


/* *********************************
 xModal Usage
 ********************************** */

// reference the xModal.html file in this folder for examples

// this requires jQuery

// call the "xModalOpen" function passing in the parameters below
// use the 'on' method to work with dynamic elements

// define your modal here
xModal.default = {
    //id: 'xmodal'+xmodal_count,  // REQUIRED - id to give to new xModal 'window'
    kind: 'fixed',  // REQUIRED - options: 'dialog','fixed','large','med','small','custom'
    width: 500, // width in px - used for 'fixed','custom','static'
    height: 300, // width in px - used for 'fixed','custom','static'
    scroll: true, // true/false - does content need to scroll?
    title: 'Message', // text for title bar
    content: ' ', //'Put the content here. Alternatively, pull content from a variable or an existing element.', // use 'static' to put existing content in a modal (use for forms)
    footer: { // if omitted, defaults to true w/buttons below
        buttons: true,  // true (or omitted) renders buttons as defined below, false renders no buttons - must be in content
        content: '', //'Put content for custom footer here. (and buttons too)',
        height: 52, // desired height in px (probably only necessary for footer with custom content (optional - default if omitted)
        border: '#e0e0e0', // css color for top border of footer (optional - default if omitted)
        background: '#f0f0f0' // css color for footer background (optional - default if omitted)
    },
    ok: { // REQUIRED if 'footer' param is omitted - if 'footer.buttons: true' - must have at least one button
        label: 'OK', // text to appear on 'ok' button
        action: function(){
            //alert('You clicked the "OK" button.');
            //doSomethingCool(); // if custom function is needed on 'ok'
            xModalClose(xModal.default.id);  // if you want the modal to close on 'ok'
        }
    },
    cancel: {  // pass this if you want a 'cancel' button - if omitted, no 'cancel' button will render
        label: 'Cancel', // text to appear on 'cancel' button
        action: function(){
            //doSomethingOnCancel();
            xModalClose(xModal.default.id);  // if you want the modal to close on 'cancel'
        }
    }
};

// 'Preset' for generic 'Message' modal
xModal.message = {
    //id: 'xmodal'+(xmodal_count++),  // REQUIRED - id to give to new xModal 'window'
    kind: 'fixed',  // REQUIRED - options: 'dialog','fixed','large','med','small','xsmall','custom'
    width: 420, // width in px - used for 'fixed','custom','static'
    height: 240, // width in px - used for 'fixed','custom','static'
    scroll: true, // true/false - does content need to scroll?
    title: 'Message', // text for title bar
    content: ' ', // content for xModal body - use 'static' to put existing content in a modal (use for forms)
    // uses default footer
    footer : {
        render: true
    },
    ok: { // REQUIRED if 'footer' param is omitted - if 'footer.buttons: true' - must have at least one button
        label: 'OK', // text to appear on 'ok' button
        action: function(){
            var this_modal = xModal.message.id ;
            debugMe(this_modal);
            // define 'OK' function when calling xModal
            // don't forget xModalClose() if you want to close your modal
        },
        close: true // does pressing 'OK' cause this modal to close? defalts to 'true'
    }
    // no 'Cancel' button by default
    // if you want a 'Cancel' button,
    // define it in the function call
};

// 'Preset' for generic fixed-size modal
xModal.fixed = {
    //id: 'message'+xmodal_count++,  // REQUIRED - id to give to new xModal 'window'
    kind: 'fixed',  // REQUIRED - options: 'dialog','fixed','large','med','small','xsmall','custom'
    width: 500, // width in px - used for 'fixed','custom','static'
    height: 300, // width in px - used for 'fixed','custom','static'
    scroll: true, // true/false - does content need to scroll?
    title: 'Information', // text for title bar
    content: ' ', // content for xModal body - use 'static' to put existing content in a modal (use for forms)
    // uses default footer
    ok: { // REQUIRED if 'footer' param is omitted - if 'footer.buttons: true' - must have at least one button
        label: 'OK', // text to appear on 'ok' button
        action: function(){
            // define 'ok' function when calling xModal
        },
        close: true // (optional) does pressing 'OK' cause this modal to close? defalts to 'true' if omitted,
    },
    cancel: {  // pass this if you want a 'cancel' button - if omitted, no 'cancel' button will render
        label: 'Cancel', // text to appear on 'cancel' button
        action: function(){
            // define 'cancel' function when calling xModal
        },
        close: true // (optional) we usually want the 'Cancel' button to close - defalts to 'true' if omitted,
    }
};


// make sure jQuery and the DOM have loaded
$(function(){

    $html = $('html');
    $body = $('body');
    $body.addClass('xmodal');

    // make sure the xModal.css is loaded
    if (!($('link[href*="xModal.css"]').length)){
        $('head').append('<link type="text/css" rel="stylesheet" href="'+ scripts_dir + '/xModal/xModal.css">');
        debugMe('xModal.css has been added.');
    }
    else {
        debugMe('xModal.css was already there.');
    }

});



function xModalSizes($this_modal,width,height){

    var
        window_width  = $(window).width(),
        window_height = $(window).height()//,
        ;

    var
        this_width,
        this_height,
        h_margin,
        v_margin,
        top_margin
        ;

    if ($this_modal.hasClass('dialog')){
        this_width = 420 ;
        this_height = 240 ;
    }
    // fixed size for kind = 'loading'
    else if ($this_modal.hasClass('loading')){
        this_width = 260 ;
        this_height = 100 ;
    }
    // if kind = 'fixed' use the numbers for width and height
    else if ($this_modal.hasClass('fixed')){
        this_width = width ;
        this_height = height ;
    }
    // otherwise ignore those numbers and set window size dynamically
    else {

        if ($this_modal.hasClass('custom')){
            h_margin = width ;
            v_margin = height ;
        }
        if ($this_modal.hasClass('large')){
            h_margin = 100 ;
            v_margin = 100 ;
        }
        if ($this_modal.hasClass('med')){
            h_margin = 200 ;
            v_margin = 200 ;
        }
        if ($this_modal.hasClass('small')){
            h_margin = 500 ;
            v_margin = 300 ;
        }
        if ($this_modal.hasClass('xsmall')){
            h_margin = 600 ;
            v_margin = 400 ;
        }

        if (v_margin <= 0){
            v_margin = 0 ;
        }

        this_width = parseInt(window_width - (h_margin * 2));
        this_height = parseInt(window_height - (v_margin * 2));

        // keep width at least 800px
        // (only for large, med, small, xsmall)
        if (this_width <= 800) {
            this_width = 800 ;
        }
        // width no larger than 1200px
        if (this_width >= 1200){
            this_width = 1200 ;
        }

        // keep height at least 500px
        if (this_height <= 500){
            this_height = 500 ;
        }

    }

    var
        title_height  = $this_modal.find('.title').height() ,
        footer_height = $this_modal.find('.footer').height()
        ;

    if (this.kind === 'fixed'){
        top_margin = parseFloat((-this_height/1.25));
    }
    else if (this.kind === 'dialog'){
        if (window_height < 1000){
            top_margin = parseFloat((-this_height/1.15));
        }
        else {
            top_margin = parseFloat((-this_height/0.85));
        }
    }
    else {
        top_margin = parseFloat((-this_height/2));
    }

    if (this.kind === 'small'){
        top_margin = top_margin-60;
    }
    if (this.kind === 'xsmall'){
        top_margin = top_margin-80;
    }


    $this_modal.css({
        width: this_width,
        height: this_height,
        marginLeft: parseFloat((-this_width/2)-3) ,
        marginTop: parseInt(top_margin-3)
    });

    $this_modal.find('.body').css({
        height: parseInt(this_height - title_height - footer_height)
    });

    // if it's an iframe
    if ($this_modal.find('iframe').length){
        $this_modal.find('.body, .body .inner').css('padding',0);
        $this_modal.find('iframe').css({
            width: '100%' ,
            height: parseInt(this_height - title_height - footer_height),
            border: 'none'
        });
    }
}






function xModalOpen(xx){

    $html = $('html');
    $body = $('body');

    xmodal_count++ ;

    this.id      = xx.id || 'xmodal'+xmodal_count ;
    this.kind    = xx.kind || 'fixed' ;
    this.width   = xx.width || false ;
    this.height  = xx.height || false ;
    this.scroll  = xx.scroll || true ;
    this.title   = xx.title || 'Message' ;
    this.content = xx.content || 'This message has no content.' ;

    if (xx.footer){
        this.footer = xx.footer || {} ;
        if (this.footer.render && this.footer.render !== false) {
            this.footer.render = true ;
        }
        else {
            this.footer.render = true ;
        }
        this.footer.buttons = this.footer.buttons || true ;
        this.footer.content = this.footer.content || false ;
        this.footer.height = this.footer.height || 'default';
        this.footer.border = this.footer.border || 'default';
        this.footer.background = this.footer.background || 'default';
    }

    debugMe('blah');

    this.ok = (xx.ok) ? xx.ok : {} ;
    this.ok.label = (xx.ok.label) ? xx.ok.label : 'OK' ;
    //this.ok.action = null ;
    if (xx.ok.action){
        this.ok.action = xx.ok.action ;
    }
    this.ok.close = (xx.ok.close) ? xx.ok.close : true ;

    if (xx.cancel){
        this.cancel = xx.cancel || {} ;
        this.cancel.label = (this.cancel.label && this.cancel.label !== false) ? this.cancel.label || 'Cancel' : this.cancel = false ;
        this.cancel.action = this.cancel.action || function(){xModalClose()} ;
        this.cancel.close = this.cancel.close || true ;
    }
    else {
        this.cancel = false ;
    }

    $body.addClass('open');


    //$body.append('<div id="mask'+ xmodal_count +'" class="xmask"></div>');

    if (this.kind === 'loading'){
        this.id = 'loading'+xmodal_count ;
    }

    debugMe(this.id+', '+xmodal_count);

    var
        this_modal_id,
        $this_modal,
        $this_mask,
        $this_content
        ;

    debugMe('this.content: '+this.content);

    if (this.content === 'static'){
        debugMe('static id: '+this.id);
        $this_content = $('#' + this.id);
        $this_content.wrap('<div id="mask'+xmodal_count+'" class="xmask" />');
        $this_content.wrap('<div id="'+this.id+'_xmodal" class="xmodal static" />');
        $this_content.wrap('<div class="body content" />');
        $this_content.wrap('<div class="inner" />');
        $this_content.show();

        debugMe('this_content id: '+$this_content.find('.xmodal').attr('id'));

        $this_mask  = $('#mask'+xmodal_count);
        this_modal_id = '#'+this.id+'_xmodal';
        $this_modal = $(this_modal_id);

    }
    else {
        $body.append('' +
            '<div id="mask'+xmodal_count+'" class="xmask">' +
            '   <div id="'+this.id+'_xmodal" class="xmodal"></div>' +
            '</div>' +
            '');

        $this_mask  = $('#mask'+xmodal_count);
        this_modal_id = '#'+this.id+'_xmodal';
        $this_modal = $(this_modal_id);

        // set up the modal contents
        //var x_title, x_content ;
        if (this.kind === 'loading'){
            this.title = 'Loading...' ;
            this.content = '' +
                '<div style="margin-top:4px;text-align:center;">' +
                //'loading...' +
                '   <img src="' + scripts_dir + '/xModal/images/loading_anim.gif" alt="loading...">' +
                '</div>' ;
        }

        // fill up the modal box
        $this_modal.append(''+
            //'<div class="title"><span class="inner">' + this.title + '</span><div class="close button"></div></div>' +
            '<div class="body content"><div class="inner">' +
            this.content +
            '</div></div>' +
            '');
    }

    // the title
    $this_modal.prepend('' +
        '<div class="title">' +
        '   <span class="inner">' + this.title + '</span>' +
        '   <div class="close button"></div>' +
        '</div>' +
        '');



    // footer stuff
    // set up classes for 'ok' and 'cancel' buttons
    var ok_class = (this.ok.close === true) ? 'ok default close button' : 'ok default button' ;
    var cancel_class = (this.cancel.close === true) ? 'cancel close button' : 'cancel button' ;

    // if the footer isn't defined or if it's defined with buttons: true
    if (this.footer.render === true){

        // default buttons
        $this_modal.append('' +
            '<div class="footer"><div class="inner">' +
            '   <span class="buttons">'+
            '       <a class="'+ok_class+'" href="javascript:;">' + this.ok.label + '</a>' +
            '   </span>' +
            '</div></div>' +
            '');
    }

    debugMe('cancel: '+this.cancel);

    if (this.cancel !== false){
        $this_modal.find('.footer .buttons').prepend('' +
            '<a class="'+cancel_class+'" href="javascript:;">' + this.cancel.label + '</a> ' +
            '');
    }

    if (this.footer && this.footer.content && this.footer.content !== false){
        $this_modal.find('.footer > .inner').prepend('<span class="content">' + this.footer.content + '</span>');
    }

    // if there's a 'height' defined
    if (this.footer && this.footer.height && this.footer.height !== 'default'){
        $this_modal.find('.footer').css('height',this.footer.height);
    }
    // if there's a custom border color defined
    if (this.footer && this.footer.border && this.footer.border !== 'default'){
        $this_modal.find('.footer').css('border-color',this.footer.border);
    }
    // if there's a custom background color defined
    if (this.footer && this.footer.background && this.footer.background !== 'default'){
        $this_modal.find('.footer').css('background-color',this.footer.background);
    }

    // scroll the body?
    if (this.scroll === true){
        $this_modal.find('.body').addClass('scroll');
    }

    $('div.xmask').not($this_mask).removeClass('top');
    $this_mask.css('z-index',parseInt(1000000000+xmodal_count)).addClass('open top').fadeIn(100);

    $this_modal.addClass('open').fadeIn(100);
    $this_modal.addClass(this.kind);

    xModalSizes($this_modal, this.width, this.height);

    // set up vars for 'ok' and 'cancel' buttons
    this.ok_button = '#'+this_modal_id+' .ok.button' ;
    this.cancel_button = '#'+this_modal_id+' .cancel.button' ;

//    var the_xmodal = this ;
//
//    $body.on('click',this_modal_id+' .ok',function(){
//        the_xmodal.ok.action();
//        xx.ok.action = null ;
//    });
//
//    $body.on('click',this_modal_id+' .cancel',function(){
//        the_xmodal.cancel.action();
//        xx.cancel.action = null ;
//    });


    var that = this ;



    // press 'esc' key to close
    $body.keydown(function(esc) {
        if (esc.keyCode === 27) {  // key 27 = 'esc'
            if ($body.hasClass('open')){
                //$('div.xmodal').last().find('.cancel.button').trigger('click');
                $this_modal.find('.close.button').trigger('click');
                //$('div.xmask.top').find('.cancel.button').trigger('click');
                //that.cancel.action();
                //xModalClose();
            }
        }
    });

    // press 'enter' key to choose default
    $body.keydown(function(e) {
        var keyCode = (window.event) ? e.which : e.keyCode;
        if (keyCode === 13) {  // key 13 = 'enter'
            if ($body.hasClass('open')){
                e.preventDefault();
                $('div.xmask.top').find('.default.button').trigger('click');
                //that.ok.action();
                //xModalClose();
            }
        }
    });

}




function xModalActions(action){

    this.button = action.button ;
    this.id = action.id ;
    this.ok = action.ok ;
    this.cancel = action.cancel ;

    //$body.on('click','div.xmodal .button',function(){
    var $clicked_button = $(this.button) ;
    var $top_modal = $clicked_button.closest('div.xmodal');

    // there's no escape from loading!
    if (!($top_modal.hasClass('loading'))){

        if ($clicked_button.hasClass('ok')){
            // do something on OK?
            if (action.ok){
                this.ok();
            }
            //that.ok.action = null ;
            //xx.ok.action();
            if ($clicked_button.hasClass('close')){
                xModalClose($top_modal);
            }
        }
        else if ($clicked_button.hasClass('cancel')){
            // do something when cancelling?
            if (action.cancel){
                this.cancel();
            }
            if ($clicked_button.hasClass('close')){
                xModalClose($top_modal);
            }
        }
        else if ($clicked_button.hasClass('close')){
            xModalClose($top_modal);
        }
    }
}





function xModalOk(id,action){
    $body.on('click','#'+id+' .ok.button',function(){
        action();
    });
}






$(function(){
// default actions
// just closes the thing
// define custom 'ok' or 'cancel' actions
// on page where invoking the xModalOpen();
    $body.on('click','div.xmodal .button',function(){
        var actions = {} ;
        actions.button = this ;
        actions.id = $(this).attr('id');
        //actions.ok = function(){alert('Doing this.')};
        //actions.cancel = function(){alert('NOT doing this.')};
        xModalActions(actions);
    });
});













function xModalClose(_this) {
    // closest the topmost modal
    var $modal = _this ?  $(_this) : $(this).closest('div.xmodal');
    var $mask = $modal.closest('div.xmask');
    //var $modal = ((_$modal && _$modal > '') || (_$modal === 'top')) ? _$modal : $('div.xmask').last() ;
    $modal.fadeOut(100).removeClass('open');
    $mask.fadeOut(100).removeClass('open top');
    if (!($modal.hasClass('static'))){
        $mask.remove();
    }
    else {
        debugMe("it's static");
    }
    if (!$('div.xmask.open').length){
        $body.removeClass('open');
    }
//    if (!($('div.xmodal').length)){
//        $modal.closest('div.xmask').fadeOut(100).removeClass('open');
//    }
}




















function xModalCloseTop() {
    // closest the topmost modal
    var $modal = $('div.xmodal').last();
    $modal.fadeOut(100).removeClass('open');
    if (!($modal.hasClass('static'))){
        $modal.html('');
    }
    $body.removeClass('open');
}





































































$(window).resize(function(){

    if ($body.hasClass('open')){

        var window_width = $(window).width();
        var window_height = $(window).height();

        $('div.xmodal').each(function(){

            var width, height ;
            var $this_modal = $(this);

            if ($this_modal.hasClass('dialog') || $this_modal.hasClass('loading') || $this_modal.hasClass('fixed')) {
                width = $this_modal.width();
                height = $this_modal.height();
            }
            else {
                width = parseInt((window_width - $this_modal.width())/2) ;
                height = parseInt((window_height - $this_modal.height())/2) ;
            }

            xModalSizes($this_modal,width,height);

        });
    }
});

