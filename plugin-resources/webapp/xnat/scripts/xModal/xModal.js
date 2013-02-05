/*
    Javascript for xModal
*/

var
    $modal,
    $this_box,
    window_width,
    window_height,
    size_type,
    box_width,
    box_height,
    h_margin,
    v_margin
;


function xModalSizes(size,width,height,box){

    window_width = $(window).width();
    window_height = $(window).height();

    // if size = 'fixed' use the numbers for width and height
    if (size === 'fixed'){
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

    box.css({
        width: box_width,
        height: box_height,
        //minHeight: '400px',
        //minWidth: '600px',
        marginLeft: parseFloat(-box_width/2) ,
        marginTop: parseFloat(-box_height/2)
        //marginLeft: -h_margin ,
        //marginTop: -v_margin
    });

    var
        title_height = box.find('.title').height(),
        footer_height = box.find('.footer').height()
        ;

    box.find('.body').css({
        //width: box_width,
        height: parseInt(box_height - title_height - footer_height)
    });

}


function xModalOpen(size,width,height,box,title,html,footer){

    $('body').addClass('modal');

    $modal = $('#x_modal') ;

    $modal.find('.box').hide().removeClass('box');

    size_type = size ;

    if (box === ''){
        if ($modal.find('.box').length){
            $this_box = $modal.find('.box');
        }
        else {
            alert("There is no box.");
        }
    }
    else {
        $this_box = $modal.find(box);
        $this_box.addClass('box');
    }

    // build the modal box
    $this_box.append(
        '<div class="title"><span class="inner"></span><div class="close"></div></div>' +
        '<div class="body"><div class="inner"></div></div>' +
        '<div class="footer"><div class="inner"></div></div>'
    );

    // size the stuff
    xModalSizes(size,width,height,$this_box);

    if (title > ''){
        $this_box.find('.title .inner').text(title);
    }
    else {
        if ($this_box.find('.title .inner').text() === ''){
            $this_box.find('.title .inner').text('Alert');
        }
    }

    if (html > ''){
        $this_box.find('.body .inner').html(html);
    }

    if (footer > ''){
        $this_box.find('.footer .inner').html(footer);
    }

    $this_box.show();
    $modal.fadeIn(200);

}


function xModalClose() {
    $('body').removeClass('modal');
    $modal.fadeOut(100);
}


$(document).ready(function(){

    var $body = $('body');

    $body.on('click','#x_modal .close',function(){
        xModalClose();
    });

    // press 'esc' key to close
    $body.keyup(function(e) {
        if (e.keyCode == 27) {  // key 27 = 'esc'
            xModalClose();
        }
    });

});


$(window).resize(function(){
    if ($('body').hasClass('modal')){
        var width, height ;
        if (size_type === 'fixed'){
            width = box_width ;
            height = box_height ;
        }
        else {
            width = h_margin ;
            height = v_margin ;
        }
        xModalSizes(size_type,width,height,$this_box);
    }
});