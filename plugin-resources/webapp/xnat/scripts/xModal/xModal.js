
// Not part of xModal, but needed to find stuff.
// This would normally already be defined before xModal.js is called.
// Modify as needed to reflect path of "scripts" directory, from site root (no trailing slash).
//var serverRoot, scripts_dir ;
//if (!scripts_dir){
    scripts_dir = serverRoot+'/scripts' ;
//}


/*
 Javascript for xModal
 */

// Global vars we're going to use
var
    $html,
    $body,
    xmodal_count = 0
    ;

// init xModal global object
var xModal = {} ;


/* *********************************
 xModal Usage
 ********************************** */

// reference the xModal.html file in this folder for examples

// this requires jQuery

// call the "xModalOpenNew" function passing in the parameters below
// use the 'on' method to work with dynamic elements

// example of xModal object with all settings
xModal_sampleObject = {
    //id: 'unique_id_for_this_modal', // id to give to new xModal 'window' - if omitted, will be generated dynamically
    kind: 'fixed',  // options: 'dialog','fixed','large','med','small','custom' - defaults to 'fixed'
    width: 600, // width in px - used for 'fixed','custom','static' - defaults to 600
    height: 400, // height in px - used for 'fixed','custom','static' - defaults to 400
    scroll: 'yes', // does content need to scroll? - defaults to yes, any other value prevents scrolling
    title: 'Message', // text for title bar - you should really customize this, but will show "Message" if omitted
    content: ' ', // REQUIRED - 'Put the content here. Alternatively, pull content from a variable or an existing element.', // use 'static' (or 'existing') to put existing content in a modal (use for forms)
    footer: 'show', // optional - defaults to 'show' if omitted - use 'hide' to supress footer
    footerButtons: 'show' , // optional - defaults to 'show' if omitted - use 'hide' to supress buttons
    footerContent: ' ', // optional - defaults to '' (empty string) if omitted
    footerHeight: 52, // optional - footer height in px - defaults to 52 if omitted
    footerBackground: '#f0f0f0', // optional - footer background color - defaults to #f0f0f0
    footerBorder: '#e0e0e0', // optional - footer top border color - defaults to #e0e0e0
    ok: 'show', // optional - show the 'ok' button? defaults to 'show' - use 'hide' to suppress ok button
    okLabel: 'OK', // optional - label for the 'ok' button - defaults to 'OK'
    okAction: function(){}, // optional - if omitted, will do nothing (empty function)
    okClose: 'yes', // optional - modal closes by default when clicking 'ok' - use 'no' if not closing on 'ok' click (useful to open an xModal from an xModal)
    cancel: 'show', // optional - defaults to 'show' - use 'hide' to suppress cancel button
    cancelLabel: 'Cancel', // optional - label for 'cancel' button - defaults to 'Cancel'
    cancelAction: function(){}, // optional - if omitted, will do nothing (empty function)
    cancelClose: 'yes', // optional - modal closes by default when clicking 'cancel' button - use 'no' to not close
    defaultButton: 'ok' // optional - defaults to 'ok - 'ok' or 'cancel' - which button is the default?
    // if you want more than 2 buttons, you'll need to use the 'existing' value for the 'content' property and include the proper markup
};

var xModalMessageCount = 0 ;

// 'Preset' for generic 'Message' modal
function xModalMessage(_title,_message,_label,_options){

    xModalMessageCount++ ;

    var msgWidth, msgHeight, msgAction ;

    msgWidth  = (_options && _options.width)  ? _options.width  : 420 ;
    msgHeight = (_options && _options.height) ? _options.height : 220 ;
    msgAction = (_options && _options.action) ? _options.action : function(){} ;

    var message = {
        //id: 'xmodal'+(xmodal_count++),  // REQUIRED - id to give to new xModal 'window'
        id: 'message'+xModalMessageCount,
        kind: 'fixed',  // REQUIRED - options: 'dialog','fixed','large','med','small','xsmall','custom'
        width: msgWidth, // width in px - used for 'fixed','custom','static'
        height: msgHeight, // height in px - used for 'fixed','custom','static'
        scroll: 'yes', // true/false - does content need to scroll?
        title: _title || 'Message', // text for title bar
        content: _message || ' ',
        // footer not specified, will use default footer
        ok: 'show',  // show the 'ok' button
        okLabel: _label || 'OK',
        okAction: msgAction ,
        cancel: 'hide' // do NOT show the 'Cancel' button
    };
    xModalOpenNew(message);
}

var xModalLoaderCount = 0 ;

function xModalLoadingOpen(_options){

    // let's only have ONE 'generic' loading window open at a time
    if (!$('#loader1.xmodal.loading.open').length){

        xModalLoaderCount++;

        var thisLoader = (_options && _options.id) ? _options.id : 'loader'+xModalLoaderCount;
        var thisClass = (_options && _options.class) ? _options.class+' loading' : 'loading';
        var thisTitle = (_options && _options.title) ? _options.title : 'Please wait...';
        var thisContent = (_options && _options.content) ? _options.content : '<img src="'+serverRoot+'/images/loading_bar.gif" alt="loading">';
        var loader = {
            id: thisLoader,
            class: thisClass,
            kind: 'fixed',
            width: 260,
            height: 92,
            scroll: 'no',
            title: thisTitle,
            content: thisContent,
            footer: 'hide'
        };
        xModalOpenNew(loader);
    }
}
function xModalLoadingClose(_id){
    var thisLoader = (_id && _id > '') ? '#'+_id : 'div.xmodal.loading.open' ;
    xModalCloseNew($(thisLoader).closest('div.xmask.open.top'));
}

// 'Preset' for generic fixed-size modal
xModal.fixed = {
    //id: 'xmodal'+(xmodal_count++),  // REQUIRED - id to give to new xModal 'window'
    kind: 'fixed',  // REQUIRED - options: 'dialog','fixed','large','med','small','xsmall','custom'
    width: 800, // width in px - used for 'fixed','custom','static'
    height: 600, // height in px - used for 'fixed','custom','static'
    scroll: 'yes', // true/false - does content need to scroll?
    title: 'Information', // text for title bar
    content: ' ', // content for xModal body - use 'static' to put existing content in a modal (use for forms)
    // uses default footer
    ok: 'show',
    okLabel: 'OK',
    // okAction: (pass in function call)
    okClose: 'yes',
    cancel: 'show',
    cancelLabel: 'Cancel',
    cancelClose: 'yes'
};

// 'Preset' for generic no footer modal
// footer.render: false doesn't seem to work
xModal.noFooter = {
    //id: 'xmodal'+(xmodal_count++),
    kind: 'fixed',
    width: 500,
    height: 300,
    scroll: 'yes',
    title: ' ',
    content: ' ',
    footer: 'hide'
    // since footer='hide', ok and cancel properties are not needed because those buttons aren't rendered
};

xModal.message = {
    //id: 'xmodal'+(xmodal_count++),  // REQUIRED - id to give to new xModal 'window'
    kind: 'fixed',  // REQUIRED - options: 'dialog','fixed','large','med','small','xsmall','custom'
    width: 420, // width in px - used for 'fixed','custom','static'
    height: 240, // height in px - used for 'fixed','custom','static'
    scroll: 'yes', // yes/no - does content need to scroll?
    title: 'Message', // text for title bar
    content: '', // content MUST be specified for this before calling xModalOpen()
    cancel: 'hide' // do NOT show the 'Cancel' button
};

// 'Preset' for dialog (smaller than message)
xModal.dialog = {
    //id: 'xmodal_dialog'+(xmodal_count++),
    kind: 'fixed',
    width: 420,
    height: 240,
    scroll: 'no',
    title: ' ',
    content: '', // content MUST be specified for this before calling xModalOpen()
    ok: 'show',
    okLabel: 'OK'
};

// 'Preset' for confirmation
xModal.confirm = {
    //id: 'xmodal'+(xmodal_count++),  // REQUIRED - id to give to new xModal 'window'
    kind: 'fixed',  // REQUIRED - options: 'dialog','fixed','large','med','small','xsmall','custom'
    width: 800, // width in px - used for 'fixed','custom','static'
    height: 600, // height in px - used for 'fixed','custom','static'
    scroll: 'yes', // true/false - does content need to scroll?
    title: 'Confirm', // text for title bar
    //content: ' ', // content for xModal body - use 'static' to put existing content in a modal (use for forms)
    // uses default footer
    ok: 'show',
    okLabel: 'OK',
    okAction: function(){alert('Please add a function for the "OK" button')}, // REQUIRED - that's the point of using the xModal.confirm object
    okClose: 'yes',
    cancel: 'show',
    cancelLabel: 'Cancel',
    cancelAction: function(){alert('Please add a function for the "Cancel" button if custom "cancel" action is desired.')},
    cancelClose: 'yes'
};

// use for testing
xModal.test = {
    //id: 'xmodal'+(xmodal_count++),
    kind: 'fixed',
    width: 400,
    height: 300,
    title: 'Test',
    content: 'This is only a test.',
    ok: 'show',
    okLabel: 'OK',
    okAction: function(){alert('Test OK success.')},
    okClose: 'yes',
    cancel: 'show',
    cancelLabel: 'Cancel',
    cancelAction: function(){alert('Test Cancel success.')},
    cancelClose: 'yes'
};

// make sure jQuery and the DOM have loaded
$(function(){

    $html = $('html');
    $body = $('body');
    $body.addClass('xmodal');

    // make sure the xModal.css is loaded
    if (!($('link[href*="xModal.css"]').length)){
        $('head').append('<link type="text/css" rel="stylesheet" href="'+ scripts_dir + '/xModal/xModal.css">');
    }
    else {
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

//    if ($this_modal.hasClass('dialog')){
//        this_width = 420 ;
//        this_height = 240 ;
//    }
    // fixed size for kind = 'loading'
//    else if ($this_modal.hasClass('loading')){
//        this_width = 260 ;
//        this_height = 100 ;
//    }
    // if kind = 'fixed' use the numbers for width and height
    /*else*/
    if ($this_modal.hasClass('fixed')){
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

        // keep width at least 600px
        // (only for large, med, small, xsmall)
        if (this_width <= 600) {
            this_width = 600 ;
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

    if (this.kind === 'fixed' || this.kind === 'dialog' || this.kind === 'message'){
        top_margin = parseFloat((-this_height/2)-20);
    }
    // not sure if this is needed anymore
    /*
    else if (this.kind === 'dialog'){
        if (window_height < 1000){
            top_margin = parseFloat((-this_height/1.15));
        }
        else {
            top_margin = parseFloat((-this_height/1.05));
        }
    }
    */
    else {
        top_margin = parseFloat((-this_height/2));
    }

    if (this.kind === 'small'){
        top_margin = top_margin-20;
    }
    if (this.kind === 'xsmall'){
        top_margin = top_margin-30;
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
    /*
    // this isn't ready
    if ($this_modal.find('iframe').length){
        $this_modal.find('.body, .body .inner').css('padding',0);
        $this_modal.find('iframe').css({
            width: '100%' ,
            height: parseInt(this_height - title_height - footer_height),
            border: 'none'
        });
    }
    */
}


function xModalOpenNew(xx){

    $html = $('html');
    $body = $('body');

    xmodal_count++ ;

    this.id =  xx.id || 'xmodal'+xmodal_count ;
    //this.kind = xx.size || 'fixed' ; // should change 'kind' to 'size' but will need to check if 'kind' is used first
    this.kind = xx.kind || 'fixed' ;
    this.width = xx.width || 600 ;
    this.height = xx.height || 400 ;
    this.scroll = xx.scroll|| 'yes' ;
    this.title = xx.title || 'Message' ;
    this.content = xx.content || '(no content)' ;

    this.footer = xx.footer || 'show' ;
    this.footerButtons = xx.footerButtons || 'show' ;
    this.footerContent = xx.footerContent || '' ;
    this.footerHeight = xx.footerHeight || 52 ;
    this.footerBackground = xx.footerBackground || '#f0f0f0' ;
    this.footerBorder = xx.footerBorder || '#e0e0e0' ;

    this.ok = xx.ok || 'show' ;
    this.okLabel = xx.okLabel || 'OK' ;
    this.okAction = xx.okAction || function(){} ;
    this.okClose = xx.okClose || 'yes' ;
    var xModalOkAction = this.okAction ;

    this.cancel = xx.cancel || 'show' ;
    this.cancelLabel = xx.cancelLabel || 'Cancel';
    this.cancelAction = xx.cancelAction || function(){};
    this.cancelClose = xx.cancelClose || 'yes' ;
    var xModalCancelAction = this.cancelAction ;

    this.defaultButton = xx.defaultButton || 'ok' ;

    $body.addClass('open');

    if (this.kind === 'loading'){
        this.id = 'loading'+xmodal_count ;
    }

    var
        this_mask_id,
        this_modal_id,
        this_modal_class,
        $this_modal,
        $this_mask,
        $this_content
        ;

    if (this.content === 'static'){

        this_mask_id = this.id+'_xmask' ;
        this_modal_id = this.id+'_xmodal';
        this_modal_class = (xx.class && xx.class > '') ? xx.class+' xmodal static' : 'xmodal static' ;

        $this_content = $('#' + this.id);
        $this_content.wrap('<div id="'+this_mask_id+'" data-xmodal-x="'+xmodal_count+'" class="xmask" />');
        $this_content.wrap('<div id="'+this_modal_id+'" class="'+this_modal_class+'" />');
        $this_content.wrap('<div class="body content" />');
        $this_content.wrap('<div class="inner" />');
        $this_content.show();

        $this_mask  = $('#'+this_mask_id);
        $this_modal = $('#'+this_modal_id);
    }
    else if (this.content === 'existing'){
        this.footer = 'hide';
        this_mask_id = this.id+'_xmask' ;
        this_modal_id = this.id;
        this_modal_class = (xx.class && xx.class > '') ? xx.class+' xmodal existing' : 'xmodal existing' ;

        $this_modal = $('#' + this.id);
        $this_modal.wrap('<div id="'+this_mask_id+'" data-xmodal-x="'+xmodal_count+'" class="xmask" />');
        $this_modal.addClass(this_modal_class).show();
        $this_mask  = $('#'+this_mask_id);
    }
    else {

        this_mask_id = this.id+'_xmask' ;
        this_modal_id = this.id ;
        this_modal_class = (xx.class && xx.class > '') ? xx.class+' xmodal' : 'xmodal' ;

        $body.append('' +
            '<div id="'+this_mask_id+'" data-xmodal-x="'+xmodal_count+'" class="xmask">' +
            '   <div id="'+this_modal_id+'" class="'+this_modal_class+'"></div>' +
            '</div>' +
            '');

        $this_mask  = $('#'+this_mask_id);
        $this_modal = $('#'+this_modal_id);

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
    var ok_class = (this.okClose === 'yes') ? 'ok button close' : 'ok button' ;
    ok_class += (this.defaultButton === 'ok') ? ' default' : '' ;
    var cancel_class = (this.cancelClose === 'yes') ? 'cancel button close' : 'cancel button' ;
    cancel_class += (this.defaultButton === 'cancel') ? ' default' : '' ;

    // if the footer isn't defined or if it's defined with buttons: true
    if (this.footer === 'show'){
        // default buttons
        $this_modal.append('' +
            '<div class="footer"><div class="inner">' +
            '</div></div>' +
            '');

        if (this.footerButtons === 'show'){
            $this_modal.find('.footer .inner').append('' +
                '   <span class="buttons">'+
                '   </span>' +
                '');

            if (this.ok === 'show'){
                $this_modal.find('.footer .buttons').append('' +
                    '       <a class="'+ok_class+'" href="javascript:;">' + this.okLabel + '</a>' +
                    '');
            }

            if (this.cancel === 'show'){
                $this_modal.find('.footer .buttons').prepend('' +
                    '<a class="'+cancel_class+'" href="javascript:;">' + this.cancelLabel + '</a> ' +
                    '');
            }
        }
        //if (this.footer.content > ''){
        $this_modal.find('.footer > .inner').prepend('<span class="content">' + this.footerContent + '</span>');
        //}
    }



    // we've already checked and set defaults for the footer stuff
    // let's use those values here
    $this_modal.find('.footer').css({
        height: this.footerHeight,
        borderColor: this.footerBorder,
        backgroundColor: this.footerBackground
    });

    // scroll the body?
    if (this.scroll === 'yes'){
        $this_modal.find('.body').addClass('scroll');
    }

    $('div.xmask').not($this_mask).removeClass('top');
    $this_mask.css('z-index',parseInt(1000000000+xmodal_count)).show().addClass('open top');

    $this_modal.fadeIn(100).addClass('open');
    $this_modal.addClass(this.kind);

    xModalSizes($this_modal, this.width, this.height);

    // set up vars for 'ok' and 'cancel' buttons
    this.ok_button = '#'+this_modal_id+' .ok.button' ;
    this.cancel_button = '#'+this_modal_id+' .cancel.button' ;

    // set up functions for 'ok' and 'cancel' buttons
    // named to match the modal they're attached to
    if (this.okAction){
        var this_ok_action = this_modal_id+'_ok';
        window[this_ok_action] = function(){
            xModalOkAction();
        };
    }
    if (this.cancelAction){
        var this_cancel_action = this_modal_id+'_cancel';
        window[this_cancel_action] = function(){
            xModalCancelAction();
        };
    }
}


function xModalCloseNew(_$this) {
    // closes the topmost modal
    var $mask = _$this ?  _$this : $('div.xmask.top');
    var $modal = $mask.find('div.xmodal');
    xmodal_count-- ;
    var prev_xmodal = xmodal_count ;
    $modal.hide().removeClass('open');
    $mask.fadeOut(100).removeClass('open top');
    $('div.xmask[data-xmodal-x="'+prev_xmodal+'"]').addClass('top');
    if ($modal.hasClass('static')){
        $modal.find('.title').remove();
        $modal.find('.footer').remove();
        var $static_content = $modal.find('.inner > div');
        $static_content.hide();
        $static_content.unwrap('<div />');
        $static_content.unwrap('<div />');
        $static_content.unwrap('<div />');
        $static_content.unwrap('<div />');
    }
    else if ($modal.hasClass('existing')){
        $modal.hide();
        $modal.find('.title').remove();
        $modal.unwrap('<div />');
    }
    else {
        $mask.remove();
    }
    if (!$('div.xmask.open').length){
        $body.removeClass('open');
    }
}


function xModalActions(_this){

    var $clicked_button = $(_this) ;
    var $my_modal = $clicked_button.closest('div.xmodal');
    var $my_mask = $clicked_button.closest('div.xmask');
    var xmodal_id = $my_modal.attr('id');

    // there's no escape from loading!
    if (!($my_modal.hasClass('loading'))){

        // don't allow clicks if 'disabled'
        if (!$clicked_button.hasClass('disabled')){

            // don't allow clicks if 'hidden'
            if (!$clicked_button.hasClass('hidden')){

                if ($clicked_button.hasClass('ok')){
                    if (window[xmodal_id+'_ok'] != undefined){
                        window[xmodal_id+'_ok']();
                    }
                    if ($clicked_button.hasClass('close')){
                        xModalCloseNew($my_mask);
                    }
                }
                else if ($clicked_button.hasClass('cancel')){
                    if (window[xmodal_id+'_cancel'] != undefined){
                        window[xmodal_id+'_cancel']();
                    }
                    if ($clicked_button.hasClass('close')){
                        xModalCloseNew($my_mask);
                    }
                }
                else if ($clicked_button.hasClass('close')){
                    xModalCloseNew($my_mask);
                }
            }
        }
    }
}


$(function(){

    $body.on('click','div.xmodal .button',function(){
        xModalActions(this);
    });

    // press 'esc' key to close
    $body.keydown(function(esc) {
        if (esc.keyCode === 27) {  // key 27 = 'esc'
            if ($body.hasClass('open')){
                var $top_modal = $('div.xmask.top');
                $top_modal.find('.title .close.button').trigger('click');
            }
        }
    });

    // press 'enter' key to choose default
    $body.keydown(function(e) {
        var keyCode = (window.event) ? e.which : e.keyCode;
        if (keyCode === 13) {  // key 13 = 'enter'
            if ($body.hasClass('open')){
                e.preventDefault();
                var $top_modal = $('div.xmask.top');
                $top_modal.find('.default.button').trigger('click');
            }
        }
    });
});


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

