/*
    Javascript for xModal
*/


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
    $body,
    $modal,
    $this_box,
    modal_count=0,
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
    ok_btn,
    cancel_btn,
    close_btn
    ;


function xModalSizes(kind,width,height /*,box*/){

    window_width = $(window).width();
    window_height = $(window).height();

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

    top_margin = parseFloat((-box_height/2));

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
        marginLeft: parseFloat(-box_width/2) ,
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


// this is the main function that draws the x_modal guy
function xModalOpen(xModal){

    x_title = (typeof xModal.title !== 'undefined' && xModal.title > '') ? xModal.title : 'Alert' ;
    x_content = (typeof xModal.content !== 'undefined' && xModal.content > '') ? xModal.content : '(no content)' ;
    ok_btn = (typeof xModal.ok !== 'undefined' && xModal.ok > '') ? xModal.ok : 'OK' ;
    cancel_btn = (typeof xModal.cancel !== 'undefined' && xModal.cancel > '') ? xModal.cancel : 'Cancel' ;
    close_btn = (typeof xModal.close !== 'undefined' && xModal.close > '') ? xModal.close : 'Close' ;

    modal_count = parseInt(modal_count + 1) ;
    var my_modal = 'modal_'+modal_count ;

    $('body').addClass('x_modal_body');

    if (xModal.kind === 'loading'){
        box = 'loading';
    }
    else {
        box = xModal.box ;
    }

    if (xModal.content === 'static'){
//        $modal = $('div#'+box+'_modal.x_modal.static');
        $modal = $('div#'+box+'.x_modal.static');
    }
    else {
        $modal = $('div.x_modal').last() ;
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
            '<div class="title round"><span class="inner">' + x_title + '</span><div class="close cancel button"></div></div>' +
            '<div class="body"><div class="inner"></div></div>'
        );

        $this_box.find('.body .inner').append(x_content);

        // if there's a footer, put it in
        if (typeof xModal.footer !== 'undefined' && xModal.footer.show === 'yes'){
            // if there's footer content, show it
            if (typeof xModal.footer.content !== 'undefined' && xModal.footer.content > ''){
                $this_box.append('<div class="footer"><div class="inner">' + xModal.footer.content + '</div></div>');
            }
            // if no content is specified, show default buttons
            else {
                $this_box.append(
                '<div class="footer"><div class="inner">' +
                '<span class="buttons">'+
                '<a class="cancel button" href="javascript:">' + cancel_btn + '</a> ' +
                '<a class="ok default button" href="javascript:">' + ok_btn + '</a>' +
                    //'<button type="submit" class="ok default button" value="ok"' +
                    //' style="position:absolute;left:-9999;top:-9999;"' +
                    //'>ok</button>' +
                '</span>' +
                '</div></div>'
                );
            }
            // set up the footer style
            if (typeof xModal.footer.height !== 'undefined' && xModal.footer.height > ''){
                $modal.find('.footer').css({height: xModal.footer.height});
            }
            if (typeof xModal.footer.background !== 'undefined' && xModal.footer.background > ''){
                $modal.find('.footer').css({background: xModal.footer.background});
            }
            if (typeof xModal.footer.border !== 'undefined' && xModal.footer.border > ''){
                $modal.find('.footer').css({borderColor: xModal.footer.border});
            }
        }
        // if 'footer' is not defined, just show buttons in the default footer
        else {
            //ok_btn = (typeof xModal.ok !== 'undefined' && xModal.ok > '') ? xModal.ok : 'OK' ;
            cancel_btn = (typeof xModal.cancel !== 'undefined' && xModal.cancel > '') ? xModal.cancel : 'Cancel' ;
            $this_box.append(
                '<div class="footer"><div class="inner">' +
                    '<span class="buttons">'+
                    '</span>' +
                '</div></div>'
            );
            if (typeof xModal.ok !== 'undefined' && xModal.ok > ''){
                $this_box.find('.footer .buttons').append('<a class="ok default button" href="javascript:">' + xModal.ok + '</a>')
            }
            else {
                ok_btn = 'OK'
            }
        }

    }
    // otherwise use the one with a 'static' class
    else {
        $this_box.find('.title .inner').text(x_title);
        $this_box.find('.footer .buttons .cancel.button').text(cancel_btn);
        $this_box.find('.footer .buttons .ok.button').text(ok_btn);
    }

    // scroll the body?
    if (xModal.scroll === 'yes'){
        $this_box.addClass('scroll');
    }

    x_kind = xModal.kind ;

    // size the stuff
    xModalSizes(xModal.kind, xModal.width, xModal.height /* ,box_elem */);

    $this_box.show();
    $modal.addClass('open').fadeIn(100);

}

function xModalClose() {
    // closest the topmost modal
    //var $modal = $('div.x_modal').last();
    $modal.fadeOut(100).removeClass('open');
    if (!($modal.hasClass('static'))){
        $modal.html('');
    }
    $('body').removeClass('x_modal_body');
}

$(document).ready(function(){

    $body = $('body');

    // make sure the xModal.css is loaded
//    if (!($('link[href*="xModal.css"]').length)){
//        $('head').append('<link type="text/css" rel="stylesheet" href="'+ scripts_dir + '/xModal/xModal.css">');
//    }

    // if no div.x_modal or if there's already a div.x_modal.static
    // put a new empty x_modal div on the page
    var $static_modal_obj = $('div.x_modal.static') ;
    if (!($('div.x_modal').length) || !($static_modal_obj.length)){
        $body.append(
        '<div class="x_modal"><div class="box round"></div></div>'
        );
    }

    // what happens when clicking a button (close, cancel, default)
    $body.on('click','div.x_modal.open .button',function(){

        // there's no escape from loading!
        if (!($('div.x_modal').last().find('.box').hasClass('loading'))){

            if ($(this).hasClass('close')){
                xModalClose();
            }
            else if ($(this).hasClass('cancel')){
                xModalCancel();
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
            if ($body.hasClass('x_modal_body')){
                $('div.x_modal').last().find('.cancel.button').trigger('click');
                //xModalCancel();
                //xModalClose();
            }
        }
    });

    // press 'enter' key to choose default
    $body.keydown(function(e) {
        var keyCode = (window.event) ? e.which : e.keyCode;
        if (keyCode === 13) {  // key 13 = 'enter'
            if ($body.hasClass('x_modal_body')){
                e.preventDefault();
                $('div.x_modal').last().find('.default.button').trigger('click');
                //xModalSubmit();
                //xModalClose();
            }
        }
    });

});


$(window).resize(function(){
    if ($('body').hasClass('x_modal_body')){
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
        xModalSizes(x_kind,width,height/*,$this_box*/);
    }
});