// initialize a global var for the <body> element
var $body ;


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




$(function(){

    $body = $('body');
    var notLinks = 'a[href^="/##"], a[href^="##"], a[href^="!"], a[href^="*"]';

    $body.on('click',notLinks,function(e){
        e.preventDefault();
    });

});







function prevTabsWidth(_$tab,_n){
    var tab_number = _$tab.data('tab');
    var prev_tabs_width = 0 ;
    _$tab.prevAll('li').each(function(){
        if ($(this).data('tab') <= tab_number){
            prev_tabs_width += $(this).outerWidth();
            prev_tabs_width += _n || 1 ;
        }
    });
    return prev_tabs_width ;
}







function renderFlippers(_wrapper){
    var $wrapper = $(_wrapper) /* || $('.yui-navset') */ ;
    var $tabs_ul = $wrapper.find('ul.yui-nav');
    var $tabs = $tabs_ul.find('li');
    if ($tabs.length > 1){
        var has_flippers = !!$wrapper.find('.flipper_box').length;
        var $flipper_box ;
        var $content_wrapper = $wrapper.find('.yui-content');
        if (has_flippers === false){
            $content_wrapper.after('<div class="flipper_box"></div>');
            $flipper_box = $content_wrapper.next('div.flipper_box');
            $flipper_box.html('' +
                '<span class="flippers">' +
                '<a href="##" class="flipper first"><b>&laquo;</b></a>' +
                '<a href="##" class="flipper left"><b>&lsaquo;</b> prev </a>' +
                '<a href="##" class="flipper right">next <b>&rsaquo;</b></a>' +
                '<a href="##" class="flipper last"><b>&raquo;</b></a>' +
                '</span>' +
                '');
//            $flipper_box.find('a.flipper').css({
//                height: ($tabs_ul.height()-2),
//                lineHeight: ($tabs_ul.height()-8) + 'px'
//            });
        }
        // remove 'disabled' class from all flippers
        $('.flippers > .flipper').removeClass('disabled');
        // disable the left flippers if we're on the first tab
        if ($tabs_ul.find('li.first[title="active"]').length){
            $('.flipper.first,.flipper.left').addClass('disabled');
        }
        // if the first tab is also the last tab, disable right flippers flippers
        // actually, this may not be necessary if we're checking for more than one tab
        if (!$tabs_ul.find('li.first.last').length){
            if ($tabs_ul.find('li:last').not('.selector').attr('title') === 'active'){
                $('.flipper.last,.flipper.right').addClass('disabled');
            }
        }
    }
}








// show the content for the selected tab
function showTabContent(_$wrapper,_n){
    _$wrapper.find('.yui-nav li[data-tab="'+_n+'"]').trigger('click');
}


function moveToTab(_$wrapper,_n,_x){

    // the tab we're moving to
    var $the_tab = _$wrapper.find('li[data-tab="' + _n + '"]');

    // width of said tab
    var the_tab_width = $the_tab.outerWidth();

    // width of all tabs BEFORE this one
    var prev_tabs_width = prevTabsWidth($the_tab);

    var content_width = _$wrapper.find('.yui-content').outerWidth();

    // if there's a 'selector' tab, get the width, otherwise set to 0
    var selector_tab_width = ($the_tab.next('li').hasClass('selector')) ? parseInt(_$wrapper.find('li.selector').outerWidth()-20) : 40 ;

    // how much visible space do we have to show the tabs? (need to ensure they are viewable inside this space)
    var width_limit = parseInt(content_width - 220);

    var move_x ;
    if (parseInt(prev_tabs_width + selector_tab_width) > parseInt(width_limit - selector_tab_width)){
        move_x = _x || parseInt(-prev_tabs_width - the_tab_width + width_limit - selector_tab_width);
    }
    else {
        move_x = 0;
    }

    _$wrapper.find('ul.yui-nav').animate({
        //left: '-' + parseInt(prev_tabs_width-width_limit-50)
        left: move_x
    },200);
}


function wrangleTabs(_wrapper){  // initialize the wrangler

    var tabs_wrapper = _wrapper || '.yui-navset' ;
    var tabs_ul = _wrapper + ' .yui-nav' ;
    var $tabs_wrapper = $(tabs_wrapper);
    var $tabs_ul = $(tabs_ul);

    if (!$tabs_ul.parent('div.wrangler').length){
        $tabs_ul.wrap('<div class="wrangler" style="width:100%;overflow:hidden;border-bottom:5px solid #084FAB"></div>');
    }

    $tabs_wrapper.addClass('wrangled');

    debugMe(tabs_wrapper);

    // var the tab <li>s -- probably have to update this var if tabs are added/subtracted
    var tabs = tabs_ul + ' > li';

    // all tabs INCLUDING the selector tab (underscore at end)
    var $tabs_ = $(tabs);

    // var the <select>
    // jQuery gracefully handles this - won't freak out if there's no <select>
    var $select = $tabs_.find('select');
    var $select_tab = $select.closest('li');
    var select_tab_width = $select_tab.outerWidth();

    // remove href from <a> (to prevent click event)
    // give 'selector' class to tab with <select>
    $select.closest('a').removeAttr('href');
    $select_tab.addClass('selector').attr('data-width',select_tab_width);

    // all tabs EXCEPT the selector tab (remove trailing underscore)
    var $tabs = $tabs_.not('.selector');

    $tabs.removeClass('last');
    // var first and last tabs and add classes
    var $first_tab = $tabs.first().addClass('first');
    var $last_tab = $tabs.last().addClass('last');

    // var to hold 'selected' tab number
    // tab/content #1 selected by default
    var n_selected = 1 ;
    // boolean if there's at least one tab that's 'active'
    var has_active = !!$(tabs+'[title="active"]').length;

    // number the tab <li>s and set '.selected' and title="active"
    // also set 'selected' var to value of i for 'selected'/'active' tab
    $tabs.each(function(i){
        i++ ;
        var tab_width = $(this).outerWidth();
        $(this).attr('data-tab',i).attr('data-width',tab_width);
        // if there's already an 'active' tab (overrides '.selected' tab)
        if (has_active === true){
            // if THIS tab is the 'active' tab
            // if there's more than one that's 'active' (this should not happen),
            // then the last 'active' tab will be 'selected'
            if ($(this).is('[title="active"]')){
                n_selected = i ;
                $tabs.removeClass('selected');
                $(this).addClass('selected');
            }
        }
        // else if there's no tab already flagged as 'active'
        // maybe it's '.selected'?
        else {
            if ($(this).hasClass('selected')){
                n_selected = i ;
                // probably don't need to remove the 'title' attr
                // since we've already checked for that
                // but it doesn't hurt to remove it anyway
                $tabs.removeAttr('title');
                $(this).attr('title','active');
            }
        }
    });

    // if nothing is selected by now... select the first tab
    if (!$(tabs+'.selected').length){
        $tabs.first().addClass('selected whut').attr('title','active');
    }

    renderFlippers(tabs_wrapper);

    showTabContent($tabs_wrapper,n_selected);

    moveToTab($tabs_wrapper,n_selected);

}


$(function(){

    $body = $('body');

    $body.on('click','.yui-nav li:not(.selector)',function(){
        //var $this_tab = $(this).closest('li');
        var $this_tab = $(this);
        var $this_navset = $(this).closest('.yui-navset');
        var $tabs = $this_navset.find('.yui-nav > li');
        var n_selected = $this_tab.data('tab');
        $tabs.removeClass('selected').removeAttr('title');
        $this_tab.addClass('selected').attr('title','active');
        //showTabContent($this_navset,n_selected);
        var $these_flippers = $this_navset.find('.flippers');
        $these_flippers.find('.flipper').removeClass('disabled');
        if ($this_tab.hasClass('first')){
            $these_flippers.find('.first,.left').addClass('disabled');
        }
        if ($this_tab.hasClass('last')){
            $these_flippers.find('.last,.right').addClass('disabled');
        }
        var prev_tabs_width = prevTabsWidth($(this).next('li'));
        moveToTab($this_navset,n_selected);
        //moveToTab($this_navset,n_selected,prev_tabs_width);
        //moveToTab($this_navset,$next_tab.data('tab'),parseInt(-prev_tabs_width + width_limit - move_right));

    });


    // we need to be able to move to the previous tab when closing one
    // won't work for some reason
//    $body.on('click','.close',function(){
//        //showTabContent($(this).closest('.yui-navset'),$(this).closest('li').prev('li').data('tab'));
//        var $this_tab = $(this).closest('li');
//        $this_tab.prev('li').trigger('click');
//    });




    $body.on('click','a.flipper:not(.disabled)',function(){

        var
            $flipper = $(this),
            $flippers = $flipper.closest('.flippers'),
            $this_navset = $flippers.closest('.yui-navset'),
            $this_tab_ul = $this_navset.find('ul.yui-nav'),
            $these_tabs = $this_tab_ul.find('li:not(.selector)'),
            $active_tab = $this_tab_ul.find('li[title="active"]') || $this_tab_ul.find('li.selected'), // check title="active" first
            $prev_tab = $active_tab.prev('li'),
            $next_tab = $active_tab.next('li'),
            $first_tab = $this_tab_ul.find('li.first'),
            //$last_tab = $this_tab_ul.find('li.last'),
            $last_tab = $this_tab_ul.find('li:not(.selector)').last(),
            $this_content = $this_navset.find('.yui-content'),
            $selector_tab = $this_tab_ul.find('li.selector')
            ;

        var active_tab_number = $active_tab.data('tab');

        var
            navset_width = $this_navset.outerWidth(),
            content_width = $this_content.outerWidth(),
            prev_tabs_width = 0,
            tabs_count = 0,
            tabs_total_width = 0,
            active_tab_width = $active_tab.outerWidth(),
        //selector_tab_width = $selector_tab.outerWidth(),
            selector_tab_width = ($selector_tab.length) ? $selector_tab.outerWidth() : 0,
            width_limit = parseInt(content_width - 200 - selector_tab_width - 5),
            these_tabs_width = 0 //$active_tab.outerWidth()
            ;

        $these_tabs.each(function(){
            these_tabs_width += $(this).outerWidth();
        });

        debugMe(these_tabs_width);

        //$flippers.find('.flipper').removeClass('disabled');

        // click 'left' flipper
        if ($flipper.hasClass('left')){
            $prev_tab.trigger('click');
        }
        // click 'right' flipper
        if ($flipper.hasClass('right')){
            $next_tab.trigger('click');
        }

        // click 'first' flipper
        if ($flipper.hasClass('first')){
            $first_tab.trigger('click');
        }

        // click 'last' flipper
        if ($flipper.hasClass('last')){
            $last_tab.trigger('click');
        }
    });








// old flipper clicker code
    function doNotClick(){
        $('body').on('click','a.flipper:not(.disabled)',function(){

            var
                $flipper = $(this),
                $flippers = $flipper.closest('.flippers'),
                $this_navset = $flippers.closest('.yui-navset'),
                $this_tab_ul = $this_navset.find('ul.yui-nav'),
                $these_tabs = $this_tab_ul.find('li:not(.selector)'),
                $active_tab = $this_tab_ul.find('li[title="active"]') || $this_tab_ul.find('li.selected'), // check title="active" first
                $prev_tab = $active_tab.prev('li'),
                $next_tab = $active_tab.next('li'),
                $first_tab = $this_tab_ul.find('li.first'),
                $last_tab = $this_tab_ul.find('li.last'),
                $this_content = $this_navset.find('.yui-content'),
                $selector_tab = $this_tab_ul.find('li.selector')
                ;

            var active_tab_number = $active_tab.data('tab');

            var
                navset_width = $this_navset.outerWidth(),
                content_width = $this_content.outerWidth(),
                prev_tabs_width = 0,
                tabs_count = 0,
                tabs_total_width = 0,
                active_tab_width = $active_tab.outerWidth(),
            //selector_tab_width = $selector_tab.outerWidth(),
                selector_tab_width = ($selector_tab.length) ? $selector_tab.outerWidth() : 0,
                width_limit = parseInt(content_width - 200 - selector_tab_width - 5),
                these_tabs_width = 0 //$active_tab.outerWidth()
                ;

            $these_tabs.each(function(){
                these_tabs_width += $(this).outerWidth();
            });

            debugMe(these_tabs_width);

            $flippers.find('.flipper').removeClass('disabled');

            // click 'left' flipper
            if ($flipper.hasClass('left')){
                $prev_tab.show().trigger('click');
                //prev_tabs_width = prevTabsWidth($prev_tab);
                prev_tabs_width = prevTabsWidth($active_tab);
                if (parseInt(prev_tabs_width+100) > parseInt(width_limit/*-$first_tab.outerWidth()*/)){
                    moveToTab($this_navset,$prev_tab.data('tab'),parseInt(-prev_tabs_width + width_limit));

//                $this_tab_ul.animate({
//                    left: '+=' + parseInt($prev_tab.outerWidth())
//                },200);
                }
                else {
                    $this_tab_ul.animate({
                        left: 0
                    },200);
                }
            }

            // click 'right' flipper
            if ($flipper.hasClass('right')){
                $next_tab.trigger('click');
                prev_tabs_width = prevTabsWidth($next_tab);
                var move_right = parseInt($next_tab.outerWidth());
                if ($('li.selected.last').next('li').hasClass('selector')){
                    move_right += selector_tab_width ;
                    move_right -= 50;
                }
                moveToTab($this_navset,$next_tab.data('tab'),parseInt(-prev_tabs_width + width_limit - move_right));
//            if (parseInt(prev_tabs_width+100) > parseInt(width_limit)){
//                $this_tab_ul.animate({
//                    //left: '-=' + move_right
//                    //left: parseInt(-prev_tabs_width+move_right-100)
//                    left: parseInt( -prev_tabs_width + width_limit - move_right)
//                },200);
//            }
            }

            // click 'first' flipper
            if ($flipper.hasClass('first')){
                $this_tab_ul.animate({
                    left: 0
                },200);
                $first_tab.delay(200).trigger('click');
            }

            // click 'last' flipper
            if ($flipper.hasClass('last')){
                prev_tabs_width = prevTabsWidth($last_tab);
                if (parseInt(prev_tabs_width+100) > parseInt(width_limit)){
                    $this_tab_ul.animate({
                        //left: parseInt(-prev_tabs_width+width_limit-selector_tab_width)
                        //left: parseInt(-prev_tabs_width + width_limit - selector_tab_width - 69)
                        left: parseInt(-prev_tabs_width + width_limit - selector_tab_width - 50 /* - $last_tab.outerWidth()*/)
                    },200);
                }
                $last_tab.delay(200).trigger('click');
            }

            //$this_navset.find('li[data-tab="'+n_selected+'"]').trigger('click');

        });
    }
// end flipper clicker
});
