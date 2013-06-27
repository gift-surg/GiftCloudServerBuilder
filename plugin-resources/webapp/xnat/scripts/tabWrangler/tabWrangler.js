/*
 * The tabWrangler will try to wrangleTabs() if the YUI tabs get out of control
 */

$(function(){
    // dynamically add the css for the tabWrangler
    if (!$('link[href*="tabWrangler.css"]').length){
        $('head').append('<link rel="stylesheet" type="text/css" href="'+serverRoot+'/scripts/tabWrangler/tabWrangler.css">');
    }
});


function debugMe(stuff){
    if (console.debug) {
        console.debug('debug: ' + stuff);
    }
    else {
        console.log('log: ' + stuff);
    }
}


function wrangleTabs(tabs){

    debugMe('Wrangling...');

    // changes to true if tabs are too wide
    var show_filppers = false ;

    if (typeof tabs == 'undefined'){
        tabs = {};
    }

    if (typeof tabs.wrapper == 'undefined'){
        tabs.wrapper = '.yui-navset';
    }
    if (typeof tabs.force == 'undefined'){
        tabs.force = false;
    }

    var _tabs = tabs.wrapper + ' ul.yui-nav';
    var _content = 'div.yui-content';

    debugMe(_tabs+', '+_content);

    var $tabs = $(_tabs);
    var $content = $tabs.next(_content);

    var $selector_tab = $tabs.find('#search_selector').closest('li');
    $selector_tab.addClass('selector');

    // if there's a selector tab, get the width of it
    var selector_width = $selector_tab.length ? $selector_tab.outerWidth() : 0 ;

    // let's store the tab_flipper jQuery object here
    var $tab_flipper ;








    // if they've already been wrangled, don't do it again
    //if (!$tabs.hasClass('wrangled')){

        var content_width = $content.outerWidth();
        var tab_count = 0 ;
        var tabs_total_width = 0 ;
        var width_limit = parseInt(content_width - 185 - selector_width - 5) ;

        debugMe('selector_width: '+selector_width+', width_limit: '+width_limit);

//        var $tab_flipper ;

        //var tabs_offset_left = $(this).offsetLeft;
        //var tabs_offset_top = $(this).offsetTop;

        // if there is more than one tab, force the flippers to show
        if ($tabs.find('li'))

        // add up total width of tabs
        $tabs.find('li').each(function(i){
            i++ ;
            debugMe('tab'+i);
            tab_count = i ;
            $(this).attr('data-tab',i).removeClass('last');
            tabs_total_width += $(this).outerWidth();
        });

        $tabs.find('li').first().addClass('first');
        var $last_tab = $tabs.find('li:last');
        // if there's a <select> element in the last tab, ignore it
//        if ($last_tab.has('select').length){
//            $last_tab.prev('li').addClass('last');
//        }
//        else {
//            $last_tab.addClass('last');
//        }

        debugMe(show_filppers);

        // if the width of  all tabs plus 50px is greater than the content_width
        // then do the stuff
        if (((tabs_total_width+50) > content_width) || (tabs.force === true) /*|| ((tabs_total_width+50) <= content_width) */) {

            show_filppers = true ;

            $tabs.parent().css({
                position: 'relative'
            });
            $tabs.css({
//                width: content_width,
                overflow: 'hidden',
                whiteSpace: 'nowrap',
                position: 'relative'
            });

            debugMe('content_width: '+content_width);

            if (!$tabs.hasClass('wrangled')){

                $content.after('<div class="flipper_box"></div>');

                // $content is the jQuery object,
                // .next is the next DOM object that matches the selector
                $tab_flipper = $content.next('div.flipper_box');
                $tab_flipper.html('' +
                    '<span class="flippers">' +
//                '<a href="javascript:;" name="first_tab" class="tab_pager"><img src="/xnat/images/left_end.gif" alt="|&lt;"></a><a href="javascript:;" name="tab_left" class="tab_pager"><img src="/xnat/images/left.gif" alt="&lt;"></a>' +
//                //' tabs ' +
//                '<a href="javascript:;" name="tab_right" class="tab_pager"><img src="/xnat/images/right.gif" alt="&gt;"></a><a href="javascript:;" name="last_tab" class="tab_pager"><img src="/xnat/images/right_end.gif" alt="&gt;|"></a>' +
                    //'<a href="javascript:;" class="flipper first disabled"><b>&laquo;</b></a>' +
                    '<a href="javascript:;" class="flipper left round disabled"><b>&lsaquo;</b> prev </a>' +
                    //'<b>Navigate Tabs</b>' +
                    '<a href="javascript:;" class="flipper right round">next <b>&rsaquo;</b></a>' +
                    //'<a href="javascript:;" class="flipper last"><b>&raquo;</b></a>' +
                    '</span>' +
                    '');

                $tab_flipper.find('a.flipper').css({
                    //display: 'block',
                    //width: '185px',
                    height: $tabs.height()-2,
                    //position: 'absolute',
                    //paddingLeft: '30px',
                    //paddingRight: 0,
                    //left: content_width-180-3, // subtract the same value as width + total padding in this css
                    //right: 0,
                    //top: 0,
                    lineHeight: ($tabs.height()-8) + 'px'//,
                    //verticalAlign: 'middle' //,
                    //textAlign: 'right',
                    //fontWeight: 'bold',
                    //color: '#fff',
                    //background: '#084FAB'
                });

            }

            $tabs.addClass('wrangled');

            $('body').on('click','a.flipper:not(.disabled)',function(){

                var prev_tabs_width = 0 ;

                var
                    $flipper = $(this),
                //$tab_wrapper = $this,
                    $active_tab = $tabs.find('li.selected'),
                    $next_tab = $active_tab.next('li'),
                    $prev_tab = $active_tab.prev('li');

                var active_tab_width = $active_tab.outerWidth();

                //$active_tab.addClass('visible');
                $active_tab.prevAll('li:visible').each(function(){
                    var width = $(this).outerWidth();
                    prev_tabs_width += parseInt(width);
                    prev_tabs_width += 2 ;
                    //$(this).addClass('visible');
                });

                prev_tabs_width += parseInt(active_tab_width) ;
                prev_tabs_width += parseInt($next_tab.outerWidth());
                prev_tabs_width += 1 ; // an extra pixel?

                // search tabs need more space for zelektor
                if ($flipper.closest('.yui-navset').is('#search_tabs')){
                    prev_tabs_width += 150 ;
                }

                debugMe(prev_tabs_width);

                if ($flipper.hasClass('right')){

                    var nextTab = function(){
                        //$next_tab.attr('title','active').addClass('selected');
                        if (!$next_tab.has('select').length){
                            $active_tab.attr('title','').removeClass('selected');
                            $next_tab.addClass('selected');
                        }
                        $next_tab.trigger('click');
                    };

                    if (prev_tabs_width <= width_limit){
                        nextTab();
                    }

                    if (prev_tabs_width > width_limit){
                        $tabs.find('li:visible:first').hide();
                        nextTab();
                    }

                    // after the tab has been 'clicked' and 'selected'
                    // if the one just clicked is the last tab
                    if ($tabs.find('li:last').hasClass('selected') || $tabs.find('li.selected').next('li').has('select').length){
                        $tab_flipper.find('a.right').addClass('disabled');
                        $tab_flipper.find('a.last').addClass('disabled');
                    }
//                else {
//                    $tab_flipper.find('a.right').removeClass('disabled');
//                }
                    $tab_flipper.find('a.left').removeClass('disabled');
                    $tab_flipper.find('a.first').removeClass('disabled');

                }

                if ($flipper.hasClass('last')){

                    debugMe('prev_tabs_width = '+prev_tabs_width);

                    $tabs.find('li').last().trigger('click');
                    $tabs.find('li').not('.selector');
                    //var last_tab_width = $tabs.find('li.last').outerWidth();


                }

                if ($flipper.hasClass('left')){
                    if (!$active_tab.hasClass('first')){
                        //moveTabsRight($tab_wrapper,$prev_tab);
                        $active_tab.attr('title','').removeClass('selected');
                        //$prev_tab.attr('title','active').addClass('selected').show();
                        $prev_tab.show().trigger('click');
                    }

                    // after the tab has been 'clicked' and 'selected'
                    // if the tab just clicked is the first tab
                    if ($tabs.find('li').first().hasClass('selected')){
                        $tab_flipper.find('a.left').addClass('disabled');
                        $tab_flipper.find('a.first').addClass('disabled');
                    }
                    $tab_flipper.find('a.right').removeClass('disabled');
                    $tab_flipper.find('a.last').removeClass('disabled');
                }

                if ($flipper.hasClass('first')){
                    $tabs.find('li').show();
                    $tabs.find('li').first().trigger('click').addClass('selected');
                }

                //wrangleTabs(tabs.wrapper);

                debugMe('tabs_total_width: '+tabs_total_width+', content_width: '+content_width+', width_limit: '+width_limit+', prev_tabs_width: '+prev_tabs_width);

            });
        }
    //}
    //else {
    //    debugMe('Already wrangled.');
    //}

    // opaque-ify the elements
//    $tabs.animate({
//        opacity:1
//    },500);
//    $content.animate({
//        opacity:1
//    },500);
    $tabs.css('opacity',1);
    $content.css('opacity',1);

    debugMe('show_filppers: '+show_filppers);

    // clicking on a tab directly sets flipper 'disabled' class
    $('.yui-navset').on('click','ul.yui-nav.wrangled > li:not(.selector)',function(){
        $tab_flipper = $(this).closest('.yui-navset').find('.flipper_box');
        $tab_flipper.find('a.flipper').removeClass('disabled');
        if ($(this).is(':first-child')){
            $tab_flipper.find('a.left').addClass('disabled');
            $tab_flipper.find('a.first').addClass('disabled');
        }
        if ($(this).is(':last-child')) {
            $tab_flipper.find('a.right').addClass('disabled');
            $tab_flipper.find('a.last').addClass('disabled');
        }
    });

    //$tabs.find('li[title="active"]').trigger('click');

}
// end wrangleTabs()

function tabFlipper(flipper,tab){

    var
        $this_flipper = $(flipper),
        $this_tab = $(tab),
        $next_tab,
        $prev_tab
    ;

    this.nextTab = function(){
        if ($this_tab.next('li').find('select').length) {

        }
//        $this_tab.next('li') : null ;
//        $next_tab.trigger('click');
    }
}

function nextTab(_this){
}

function prevTab(_this){

}

function lastTab(_this){

}

function firstTab(_this){

}

// call the first wrangler after page load
//$(window).load(function(){
//    var tabs = 'ul.yui-nav' ;
//    $(tabs).each(function(){
//        wrangleTabs(tabs,'div.yui-content');
//    });
//});
//
// or don't and only call it when needed.