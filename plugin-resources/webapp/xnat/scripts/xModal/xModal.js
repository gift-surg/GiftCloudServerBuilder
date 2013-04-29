
$.fn.extend({
    hasClasses: function(selectors) {
        var self = this;
        for (var i in selectors) {
            if ($(self).hasClass(selectors[i])) {
                return true;
            }
        }
        return false;
    }
});

/*
    Javascript for xModal
*/

//alert('xModal!');

/* *********************************
    xModal Usage
********************************** */

// reference the xModal.html file in this folder for examples

// this requires jQuery

// call the "xModalOpen" function passing in the parameters below
// use the 'on' method to work with dynamic elements
//
//$('body').on('event','#selector',function(){
//    xModalOpen({
//        kind:     'fixed',  // kind - 'dialog','loading','fixed','large','med','small','custom'
//        width:     550,  // box width when used with 'fixed' size, horizontal margin when used with 'custom' size
//        height:    350,  // box height when used with 'fixed' size, vertical margin when used with 'custom' size
//        scroll:   'yes', // does the content need to scroll? (yes/no)
//        box:      'confirm',  // class name of this modal box
//        title:    'Confirm Download', // text for modal title bar - if empty will be "Alert" - use space for blank title
//        content:   '(put the content here)', // body content of modal window - pull from existing or dynamic element $('#source').html();
//        footer:    {
//            show: 'yes', // self-explanatory, right? anything but 'yes' will hide the footer
//            height: '' , // height in px - blank for default - 52px
//            background: '', // css for footer background - white if blank
//            border: '', // color for footer top border - white if blank
//            content: '' // empty string renders default footer with two buttons (ok/cancel) using values specified below
//        },
//        // by default the buttons will go in the footer, this can be overridden by adding elements with 'class="ok default button"' and 'class="cancel button"' to the body content
//        ok:       'OK', // text for submit button - if blank uses "OK"
//        cancel:   'Cancel' // text for cancel button - if blank uses "Cancel"
//    });
//    xModalSubmit = function(){
//        // doSomething();
//    };
//    xModalCancel = function(){
//        // if you want to do something else if someone cancels, put it here
//        // it's ok if this is blank -
//        xModalClose();
//    };
//});
//

var
    $html,
    $body,
    $modal,
    //$this_box,
    box,
    window_width,
    window_height,
    x_kind,
    x_title,
    x_content,
    box_width,
    box_height,
    h_margin,
    v_margin,
    top_margin,
    x_ok,
    x_cancel,
    x_close
    ;


function xModalSizes(kind,width,height,$this_box){

    window_width = $(window).width();
    window_height = $(window).height();

    var
        //$this_box = box,
        box_width,
        box_height,
        h_margin,
        v_margin,
        top_margin
        ;

// fixed size for kind = 'dialog'
    if (kind === 'dialog'){
        box_width = 400 ;
        box_height = 200 ;
    }
    // fixed size for kind = 'loading'
    else if (kind === 'loading'){
        box_width = 260 ;
        box_height = 100 ;
    }
    // if kind = 'fixed' use the numbers for width and height
    else if (kind === 'fixed'){
        box_width = width ;
        box_height = height ;
    }
    // otherwise ignore those numbers and set window size dynamically
    else {

        if (kind === 'custom'){
            h_margin = width ;
            v_margin = height;
        }
        if (kind === 'large'){
            h_margin = 100 ;
            v_margin = 100 ;
        }
        if (kind === 'med'){
            h_margin = 200 ;
            v_margin = 200 ;
        }
        if (kind === 'small'){
            h_margin = 300 ;
            v_margin = 300 ;
        }

        if (v_margin <= 0){
            v_margin = 0 ;
        }

        box_width = parseInt(window_width - (h_margin * 2));
        box_height = parseInt(window_height - (v_margin * 2));

        // keep width at least 800px
        if (box_width <= 800) {
            box_width = 800 ;
        }
        // width no larger than 1200px
        if (box_width >= 1200){
            box_width = 1200 ;
        }

        // keep height at least 500px
        if (box_height <= 500){
            box_height = 500 ;
        }

    }

    var
        //body_height = $this_box.find('.body').height() ,
        title_height = $this_box.find('.title').height() ,
        footer_height = $this_box.find('.footer').height()
        ;

    top_margin = parseFloat((-box_height/1.25));

//    if (top_margin < 100){
//        top_margin = 100 ;
//    }
//
    // doing my best to put the dialog in a good spot in the window
    // a bit too tricky? just center the thing
    /*
    if ((box_height < 700)){
        top_margin = parseFloat((-box_height/2)-(box_height*0.1));
    }
    if ((box_height < 500) && (window_height > 900)){
        top_margin = parseFloat((-box_height/2)-(box_height*0.2));
    }
    if ((box_height < 300) && (window_height > 900)){
        top_margin = parseFloat((-box_height/2)-(box_height*0.6));
    }
    */

    // another attempt
    /*
    //if (size === 'fixed'){
        if (box_height < 800){
            if (box_height < 600) {
                top_margin = parseFloat((-box_height/2)-(box_height*0.5));
            }
            //top_margin = 200 ;
            else {
                top_margin = parseFloat((-box_height/2)-(box_height*0.3));
            }
        }
        else {
            top_margin = parseFloat((-box_height/2)-(box_height*0.2));
        }
    //}
    */

    $this_box.css({
        width: box_width,
        height: box_height,
        //minHeight: '400px',
        //minWidth: '600px',
        marginLeft: parseFloat((-box_width/2)-2), // another 2 pixels to account for the border width
        marginTop: top_margin
        //marginLeft: -h_margin ,
        //marginTop: -v_margin
    });

    $this_box.find('.body').css({
        //width: box_width,
        height: parseInt(box_height - title_height - footer_height)
    });

    // if it's an iframe
    if ($this_box.find('iframe').length){
        $this_box.find('.body, .body .inner').css('padding',0);
        $this_box.find('iframe').css({
            width: '100%' ,
            height: parseInt(box_height - title_height - footer_height),
            border: 'none'
        });
    }
}

// set modal_count to 0 when script loads
// will be incremented each time xModalOpen() is called
var modal_count = 0 ;

// this is the main function that draws the x_modal guy
function xModalOpen(xModal){

    modal_count++ ;
    var this_modal = 'xmodal'+modal_count ;

    //alert(this_modal);

    $body.append(
        '<div class="xmodal xmodal_mask">' +
            //'<div class="box round"></div>' +
        '</div>'
    );

    $body.addClass('open');

    // setup modal title, contents, and buttons
    // use values passed in or use defaults if empty string
    x_title   = (xModal.title && xModal.title > '') ? xModal.title : 'Alert' ;
    x_content = (xModal.content && xModal.content > '') ? xModal.content : '(no content)' ;
    x_ok      = (xModal.ok && xModal.ok > '') ? xModal.ok : 'OK' ;
    x_cancel  = (xModal.cancel && xModal.cancel > '') ? xModal.cancel : 'Cancel' ;
    x_close   = (xModal.close && xModal.close > '') ? xModal.close : 'Close' ;


//    $body.css({
//        top: -(document.documentElement.scrollTop)+'px'//,
//        //position: 'fixed'
//    });

//    if ($(document).height() > $(window).height()) {
        var scrollTop = ($html.scrollTop()) ? $html.scrollTop() : $body.scrollTop(); // Works for Chrome, Firefox, IE...
        $html.css('top',-scrollTop).addClass('noscroll'); // css needs to go first to make Safari happy
//    }

    if (xModal.kind === 'loading'){
        box = 'loading';
    }
    else {
        box = xModal.box ;
    }

    if (xModal.content === 'static'){
//        $modal = $('div#'+box+'_modal.xmodal.static');
        $modal = $('div#'+box+'.xmodal.static');
    }
    else {
        $modal = $('div.xmodal').last() ;
        $modal.html('');
        // make a new box
        $modal.append('<div class="box round"></div>');
        // tell me what it is
    }

    $this_box = $modal.find('.box');
    $this_box.addClass(box);

    // if there's no xModal with 'static' class,
    // clear the contents and make a new box
    if (!($modal.hasClass('static'))){

        // set up the modal contents
        //var x_title, x_content ;
        if (xModal.kind === 'loading'){
            x_title = 'Loading...' ;
            x_content =
            '<div style="margin-top:4px;text-align:center;">' +
                //'loading...' +
            '<img src="' + scripts_dir + '/xModal/images/loading_anim.gif" alt="loading...">' +
            '</div>' ;
        }
//        else {
//            x_title = (xModal.title > '') ? xModal.title : 'Alert' ;
//            x_content = (xModal.content > '') ? xModal.content : '(no content)' ;
//        }

        // fill up the modal box
        $this_box.append(
            '<div class="title round"><span class="inner">' + x_title + '</span><a href="javascript:;" class="close cancel button"></a></div>' +
            '<div class="body"><div class="inner"></div></div>'
        );

        $this_box.find('.body .inner').append(x_content);

        // if there's a footer, put it in
        if (xModal.footer && xModal.footer.show === 'yes'){
            // if there's footer content, show it
            if (xModal.footer.content && xModal.footer.content > ''){
                $this_box.append('<div class="footer"><div class="inner">' + xModal.footer.content + '</div></div>');
            }
            // if no content is specified, show default buttons
            else {
                $this_box.append(
                '<div class="footer"><div class="inner">' +
                '<span class="buttons">'+
                //'<a class="cancel button" href="javascript:;">' + x_cancel + '</a> ' +
                //'<a class="ok default button" href="javascript:;">' + x_ok + '</a>' +
                    //'<button type="submit" class="ok default button" value="ok"' +
                    //' style="position:absolute;left:-9999;top:-9999;"' +
                    //'>ok</button>' +
                '</span>' +
                '</div></div>'
                );
                if (xModal.cancel && xModal.cancel > ''){
                    $this_box.find('span.buttons').append('<a class="cancel button" href="javascript:;">' + x_cancel + '</a>');
                }
                if (xModal.ok && xModal.ok > ''){
                    $this_box.find('span.buttons').append('<a class="ok default button" href="javascript:;">' + x_ok + '</a>');
                }
            }
            // set up the footer style
            if (xModal.footer.height && xModal.footer.height > ''){
                $modal.find('.footer').css({height: xModal.footer.height});
            }
            if (xModal.footer.background && xModal.footer.background > ''){
                $modal.find('.footer').css({background: xModal.footer.background});
            }
            if (xModal.footer.border && xModal.footer.border > ''){
                $modal.find('.footer').css({borderColor: xModal.footer.border});
            }
        }
        // if 'footer' is not defined, just show buttons in the default footer
        else {
            //x_ok = (typeof xModal.ok !== 'undefined' && xModal.ok > '') ? xModal.ok : 'OK' ;
            x_cancel = (xModal.cancel && xModal.cancel > '') ? xModal.cancel : 'Cancel' ;
            $this_box.append(
                '<div class="footer"><div class="inner">' +
                    '<span class="buttons">'+
                    '</span>' +
                '</div></div>'
            );
            if (xModal.ok && xModal.ok > ''){
                $this_box.find('.footer .buttons').append('<a class="ok default button" href="javascript:;">' + xModal.ok + '</a>')
            }
            else {
                x_ok = 'OK'
            }
        }

    }
    // otherwise use the one with a 'static' class
    else {
        $this_box.find('.title .inner').text(x_title);
        $this_box.find('.footer .buttons .cancel.button').text(x_cancel);
        $this_box.find('.footer .buttons .ok.button').text(x_ok);
    }

    // scroll the body?
    if (xModal.scroll === 'yes'){
        $this_box.addClass('scroll');
    }

    x_kind = xModal.kind ;

    // size the stuff
    xModalSizes(xModal.kind, xModal.width, xModal.height, $this_box);

    // if draggable:true then make it draggable by the title bar
    if (xModal.draggable && xModal.draggable === true){
        $this_box.drags({handle:$this_box.find('div.title')});
    }

    $this_box.show();
    $modal.addClass('open').fadeIn(100);

}

function xModalClose() {
    // closest the topmost modal
    var $modal = $('div.xmodal').last();
    $modal.fadeOut(100).removeClass('open');
    if (!($modal.hasClass('static'))){
        //$modal.html('');
        $modal.detach();
    }
    var scrollTop = parseInt($html.css('top'));
    $html.removeClass('noscroll');
    $('html,body').scrollTop(-scrollTop);
    $body.removeClass('open');
}

$(document).ready(function(){

    $html = $('html');
    $body = $('body');

    $body.addClass('xmodal');

    // make sure the xModal.css is loaded
    if (!($('link[href*="xModal.css"]').length)){
        $('head').append('<link type="text/css" rel="stylesheet" href="'+ serverRoot + '/scripts/xModal/xModal.css">');
    }

    // if no div.xmodal or if there's already a div.xmodal.static
    // put a new empty xmodal div on the page
//    var $xmodal_static = $('div.xmodal.static') ;
//    if (!($('div.xmodal').length) || !($xmodal_static.length)){
//        $body.append(
//        '<div class="xmodal xmodal_mask">' +
//            //'<div class="box round"></div>' +
//        '</div>'
//        );
//    }

    // what happens when clicking a button (close, cancel, default)
    $body.on('click','div.xmodal.open .button',function(){

        // there's no escape from loading!
        if (!($('div.xmodal').last().find('.box').hasClass('loading'))){

            if ($(this).hasClass('cancel')){
                xModalCancel();
            }
            else if ($(this).hasClass('close')){
                xModalClose();
                // use xModalClose(); in xModalCancel() where xModalOpen() is called
            }
            else {
                xModalSubmit();
                // use xModalClose(); in xModalSubmit() where xModalOpen() is called
            }
        }
    });

    // press 'esc' key to close
    $body.keydown(function(esc) {
        if (esc.keyCode === 27) {  // key 27 = 'esc'
            if ($body.hasClasses(['xmodal open','nothing'])){
                $('div.xmodal').last().find('.cancel.button').trigger('click');
                //xModalCancel();
                //xModalClose();
            }
        }
    });

    // press 'enter' key to choose default
    $body.keydown(function(e) {
        var keyCode = (window.event) ? e.which : e.keyCode;
        if (keyCode === 13) {  // key 13 = 'enter'
            if ($body.hasClasses(['xmodal open','nothing'])){
                e.preventDefault();
                $('div.xmodal').last().find('.default.button').trigger('click');
                //xModalSubmit();
                //xModalClose();
            }
        }
    });

});

$(window).resize(function(){
    if ($('body').hasClasses(['xmodal open','nothing'])){
        var width, height ;
        if (x_kind === 'dialog'){
            width = 400 ;
            height = 200 ;
        }
        else if (x_kind === 'loading'){
            width = 260 ;
            height = 100 ;
        }
        else if (x_kind === 'fixed'){
            width = box_width ;
            height = box_height ;
        }
        else {
            width = h_margin ;
            height = v_margin ;
        }
        xModalSizes(x_kind,width,height,$this_box);
    }
});