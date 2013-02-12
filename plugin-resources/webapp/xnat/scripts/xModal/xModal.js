/*
    Javascript for xModal
*/

console.log('xModal loaded');

/* *********************************
    xModal Usage
********************************** */

// reference the xModal.html file in this folder for examples

// this requires jQuery

// call the "xModalOpen" function passing in the parameters below
    // use the 'on' method to work with dynamic elements
    /*
    $('body').on('event','#selector',function(){
        xModalOpen({
            size:     'fixed',  // size - 'fixed','large','med','small','custom'
            width:     550,  // box width when used with 'fixed' size, horizontal margin when used with 'custom' size
            height:    350,  // box height when used with 'fixed' size, vertical margin when used with 'custom' size
            scroll:   'yes', // does the content need to scroll? (yes/no)
            box:      'confirm',  // class name of this modal box
            title:    'Confirm Download', // text for modal title bar - if empty will be "Alert" - use space for blank title
            content:   '(put the content here)', // body content of modal window - pull from existing or dynamic element $('#source').html();
            footer:    {
                show: 'yes', // self-explanatory, right? anything but 'yes' will hide the footer
                height: '' , // height in px - blank for default - 52px
                background: '', // css for footer background - white if blank
                border: '', // color for footer top border - white if blank
                content: '' // empty string renders default footer with two buttons (ok/cancel) using values specified below
            },
            // by default the buttons will go in the footer, this can be overridden by adding elements with 'class="ok default button"' and 'class="cancel button"' to the body content
            ok:       'OK', // text for submit button - if blank uses "OK"
            cancel:   'Cancel' // text for cancel button - if blank uses "Cancel"
        });
        xModalSubmit = function(){
            // doSomething();
        };
        xModalCancel = function(){
            // if you want to do something else if someone cancels, put it here
            // it's ok if this is blank -
        };
    });
    */


var
    $body,
    $modal,
    $this_box,
    box,
    window_width,
    window_height,
    size_type,
    box_width,
    box_height,
    h_margin,
    v_margin,
    top_margin,
    ok_btn,
    cancel_btn
;


function xModalSizes(size,width,height /*,box*/){

    window_width = $(window).width();
    window_height = $(window).height();

    // fixed size for size = 'dialog'
    if (size === 'dialog'){
        box_width = 350 ;
        box_height = 200 ;
    }
    // if size = 'fixed' use the numbers for width and height
    else if ((size === 'fixed')){
        box_width = width ;
        box_height = height ;
    }
    // otherwise ignore those numbers and set window size dynamically
    else {

        if (size === 'custom'){
            h_margin = width ;
            v_margin = height;
        }
        if (size === 'large'){
            h_margin = 100 ;
            v_margin = 100 ;
        }
        if (size === 'med'){
            h_margin = 200 ;
            v_margin = 200 ;
        }
        if (size === 'small'){
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
        // height no larger than 900px
        // or not?
        //if (box_height >= 900){
        //    box_height = 900 ;
        //}

    }

    var body_height = $this_box.find('.body').height() ,
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


// this is the main function that draws the modal guy
function xModalOpen(xModal /* size,width,height,scroll,box,title,content,footer,ok,cancel,action */){

    //var $body = $('body');

    $body.addClass('modal');

    $modal = $('#x_modal') ;

    box = xModal.box ;

    // throw out the boxes
    $modal.html('');

    // make a new box
    $modal.append('<div class="box round"></div>');

    // tell me what it is
    $this_box = $modal.find('.box');

    //$this_box.html('');

    $this_box.addClass(xModal.box);

    if (xModal.scroll === 'yes'){
        $this_box.addClass('scroll');
    }

    // set up the modal contents
    var _title = (xModal.title > '') ? xModal.title : 'Alert' ;
    var _content = (xModal.content > '') ? xModal.content : '(no content)' ;

    // fill up the modal box
    $this_box.append(
        '<div class="title round"><span class="inner">' + _title + '</span><div class="close cancel button"></div></div>' +
        '<div class="body"><div class="inner">' + _content + '</div></div>'
    );

    // if there's a footer, put it in
    if (xModal.footer.show === 'yes'){
        // if there's footer content, show it
        if (xModal.footer.content > ''){
            $this_box.append('<div class="footer"><div class="inner">' + xModal.footer.content + '</div></div>');
        }
        // if no content is specified, show default buttons
        else {
			ok_btn = (xModal.ok > '') ? xModal.ok : 'OK' ;
			cancel_btn = (xModal.cancel > '') ? xModal.cancel : 'Cancel' ;
			$this_box.append(
				'<div class="footer"><div class="inner">' +
					'<a class="cancel btn2 button" href="#!">' + cancel_btn + '</a> ' +
                    '<a class="ok default btn1 button" href="#!">' + ok_btn + '</a>' +
                    //'<button type="submit" class="ok default button" value="ok"' +
                    //' style="position:absolute;left:-9999;top:-9999;"' +
                    //'>ok</button>' +
				'</div></div>'
			);
        }

        if (xModal.footer.height > ''){
            $modal.find('.footer').css({
                height: xModal.footer.height
            });
        }

        if (xModal.footer.background > ''){
            $modal.find('.footer').css({
                background: xModal.footer.background
            });
        }

        if (xModal.footer.border > ''){
            $modal.find('.footer').css({
                borderColor: xModal.footer.border
            });
        }

        //$modal.find('button.ok.default').focus();

    }

    size_type = xModal.size ;

    // size the stuff
    xModalSizes(xModal.size, xModal.width, xModal.height /* ,box_elem */);

    $this_box.show();
    $modal.fadeIn(100);

}

function xModalClose() {
    $('body').removeClass('modal');
    $modal.fadeOut(100);
    $this_box.html('');
}

$(document).ready(function(){

    $head = $('head');
    $body = $('body');

    // make sure the xModal.css is loaded
    if (!($('link[href*="xModal.css"]').length)){
        $head.append('<link type="text/css" rel="stylesheet" href="'+ scripts_dir + '/xModal/xModal.css">');
        console.log('xModal.css added');
    }

    // prevent default clicks on these:
    var script_links =
        'a[href="#"],' +
        'a[href="#!"],' +
        'a[href="#*"],' +
        'a[href="*"],' +
        'a[href=""],' +
        'a.script' +
        '';
    $body.on('click',script_links,function(s){
        s.preventDefault();
        return false ;
    });

    if (!($('#x_modal').length)){
        $body.append(
            '<div id="x_modal"></div>'
        );
    }

//    if (typeof xModalSubmit !== 'function'){
//        var xModalSubmit = null ; // nullify the function if it doesn't exist
//    }
//
//    if (typeof xModalCancel !== 'function'){
//        var xModalCancel = null ;
//    }


    // what happens when clicking the default button
    $body.on('click','#x_modal .button',function(){
        if ($(this).hasClass('close')){
            xModalClose();
        }
        else if ($(this).hasClass('cancel')){
            xModalCancel();
            xModalClose();
        }
        else {
            xModalSubmit();
            xModalClose();
        }
    });

    // press 'esc' key to close
    $body.keydown(function(esc) {
        if (esc.keyCode === 27) {  // key 27 = 'esc'
            if ($body.hasClass('modal')){
                //$modal.find('.cancel.button').trigger('click');
                xModalCancel();
                xModalClose();
            }
        }
    });

    // press 'enter' key to choose default
    $body.keydown(function(e) {
        var keyCode = (window.event) ? e.which : e.keyCode;
        if (keyCode === 13) {  // key 13 = 'enter'
            if ($body.hasClass('modal')){
                e.preventDefault();
                $modal.find('.default.button').trigger('click');
                //xModalSubmit();
                //xModalClose();
            }
        }
    });

});


$(window).resize(function(){
    if ($('body').hasClass('modal')){
        var width, height ;
        if (size_type === 'dialog'){
            width = 350 ;
            height = 200 ;
        }
        else if (size_type === 'fixed'){
            width = box_width ;
            height = box_height ;
        }
        else {
            width = h_margin ;
            height = v_margin ;
        }
        xModalSizes(size_type,width,height/*,$this_box*/);
    }
});