/**
 * xmodal jQuery plugin
 * written for XNAT (xnat.org)
 * to ease the transition away from YUI dialogs
 * by Mark M. Florida (floridam@wustl.edu)
 */

var xmodal = xmodal || {};
xmodal.version = '1.0.1';

if (typeof jQuery == 'undefined') {
    throw new Error('jQuery is required for the xmodal plugin.');
}

(function() {

    var $ = jQuery;
    var _xmodal={}, $html, $body, $mask, $modal;


    _xmodal.topZ = 1000;


    function getScriptDir() {
        var src, path;
        src = $('script[src]').last().attr('src');
        if (src.indexOf('/') !== -1) {
            path = src.split('/');
            path.splice(path.length - 1, 1);
            return path.join('/') + '/';
        }
        else {
            return '';
        }
    }


    function appendCSS(_path, _filename) {
        _path = (_path !== null) ? _path : getScriptDir();
        if (!$('link[href*="' + _filename + '"]').length) {
            $('head').append('<link type="text/css" rel="stylesheet" href="' + _path + _filename + '">');
        }
    }


    function appendScript(_path, _filename) {
        _path = (_path !== null) ? _path : getScriptDir();
        if (!$('script[src*="' + _filename + '"]').length) {
            $('head').append('<script type="text/javascript" src="' + _path + _filename + '"></script>');
        }
    }


    // make the modal draggable
    // usage:
    // $('#element_id').drags();  // <- drag the element that's clicked on
    // $('#element_id').drags({handle:'.drag_handle'});
    $.fn.drags = function (opt) {

        opt = $.extend({handle: '', cursor: 'move'}, opt);

        var $el;

        if (opt.handle === '') {
            $el = this;
        }
        else {
            $el = this.find(opt.handle);
        }

        return $el.css('cursor', opt.cursor).on('mousedown', function (e) {
            var $drag;
            if (opt.handle === '') {
                $drag = $(this).addClass('draggable');
            }
            else {
                $drag = $(this).addClass('active-handle').parent().addClass('draggable');
            }
            var z_idx = $drag.css('z-index')-0,
                drg_h = $drag.outerHeight(),
                drg_w = $drag.outerWidth(),
                pos_y = $drag.offset().top + drg_h - e.pageY,
                pos_x = $drag.offset().left + drg_w - e.pageX;
            $drag.parents().on('mousemove', function (e) {
                //xmodal.topZ = z_idx+1;
                $('.draggable').css({ 'right': 'auto', 'bottom': 'auto' }).offset({
                    top: e.pageY + pos_y - drg_h,
                    left: e.pageX + pos_x - drg_w
                }).on('mouseup', function () {
                    $(this).removeClass('draggable')/*.css('z-index', z_idx)*/;
                });
            });
            e.preventDefault(); // disable selection
        }).on('mouseup', function () {
            if (opt.handle === "") {
                $(this).removeClass('draggable');
            }
            else {
                $(this).removeClass('active-handle').parent().removeClass('draggable');
            }
        });

    };
    // end draggable


    _xmodal.path = getScriptDir();


    _xmodal.count = 0;
    _xmodal.presets = {};
    _xmodal.modals = {};
    _xmodal.modals._ids = [];


    // commonly re-used HTML class names - set here as a convenience
    _xmodal.strings = {};
    _xmodal.strings.base = 'xmodal';
    _xmodal.strings.mask = 'xmodal-mask';
    _xmodal.strings.version = 'v1'; // this is here to deal with the 'old' xModal


    // these properties are added to 'xmodal.dialog' object
    _xmodal.dialog = (function( strings ){

        var dialog={};

        dialog.element = 'div';
        dialog.v       = strings.version;
        dialog.box     = dialog.element + '.' + strings.base + '.' + dialog.v;
        dialog.mask    = dialog.element + '.' + strings.mask + '.' + dialog.v;
        dialog.open    = dialog.box + '.' + 'open';
        dialog.top     = dialog.box + '.' + 'top';

        return dialog;

    })( _xmodal.strings );


    _xmodal.bodyClassName = _xmodal.strings.base + ' ' + _xmodal.strings.base + '-' + _xmodal.strings.version;

    // override local '_xmodal' properties with
    // properties attached to global 'xmodal' object
    xmodal = $.extend(true, _xmodal, xmodal);


    //////////////////////////////////////////////////
    // after DOM load (just do everything then)
    //////////////////////////////////////////////////
    $(function () {

        $html = $('html');
        $body = $(document.body);
        $body.addClass(xmodal.bodyClassName);
        appendCSS(xmodal.path, 'xmodal.css');
        //$body.on('click','div.xmodal .close',function(){xmodal.close(this)});


        // add 'loading_bar.gif' to the page (but off-canvas) to preload it
        // so it's displayed immediately when opening a loading dialog
        $body.append('' +
            '<div id="xmodal-loading" style="position:fixed;left:-9999px;top:-9999px;">' +
            '<img src="' + xmodal.path + 'loading_bar.gif" alt="loading">' +
            '</div>' +
            '');


        $body.on('keydown', function (e) {
            var keyCode = (window.event) ? e.which : e.keyCode;
            var $top_modal = $(xmodal.dialog.top).last();
            if (keyCode === 27) {  // key 27 = 'esc'
                if ($body.hasClass('open')) {
                    //xmodal.close($top_modal);
                    $top_modal.find('.title .close').trigger('click');
                }
            }
            if (keyCode === 13) {  // key 13 = 'enter'
                if ($body.hasClass('open') && xmodal.closeModal === true) {
                    e.preventDefault();
                    $top_modal.find('.buttons .default').trigger('click');
                    xmodal.closeModal = false;
                }
            }
        });


        $body.on('mousedown', 'div.xmodal.open:not(.top)', function(e){
            // click anywhere in an xmodal to move it to the front
            // if there is more than one open
            var $open = $('div.xmodal.open');
            if ( $open.length > 1 ) {
                $open.removeClass('top');
                //xmodal.topZ = $modal.css('z-index')+10;
                $(this).css('z-index',++xmodal.topZ).addClass('top');
            }
        });


        $body.on('click', 'div.xmodal.open .title .maximize', function(){
            var $modal = $(this).closest(xmodal.dialog.box);
            $modal.toggleClass('maxxed');
            xmodal.resize($modal);
        });


        // option-click on close button to close all modals
        // with a delay so users can see what's going on
        $body.on('click', 'div.xmodal.open .title .close', function(e){
            if (e.altKey) {
                xmodal.closeAll();
            }
        });




        //////////////////////////////////////////////////
        // delete the 'xmodal.modals[_id]' object
        //////////////////////////////////////////////////
        xmodal.deleteModal = function(_id){

            delete xmodal.modals[_id];

            var i = xmodal.modals._ids.indexOf(_id);
            xmodal.modals._ids.splice( i, 1 );

            // we don't want to decrement the count
            // this helps ensure there won't be
            // duplicate modal ids.
            //
            //if (xmodal.count > 0) { xmodal.count-- }
            //xmodal.count--;

        };
        //////////////////////////////////////////////////




        //////////////////////////////////////////////////
        // set the xmodal body element's size
        //////////////////////////////////////////////////
        xmodal.resize = function ($modal, modal) {
            //console.log(modal.title);
            function resizeBody(_$modal, _modal) {
                var $title = _$modal.find('.title');
                var modal_height  = _$modal.height();
                var title_height  = $title.height();
                var title_width   = $title.width();
                var footer_height = _$modal.find('.footer').height();
                var body_height   = 200; // set an initial value

                if (_modal.footer && $.isPlainObject(_modal.footer)) {
                    body_height = Math.round(modal_height - title_height - footer_height - 1);
                }
                else {
                    body_height = Math.round(modal_height - title_height);
                }

                _$modal.find('div.body').css({
                    'height': body_height - 1
                });

                if (modal.closeBtn){
                    title_width = title_width - 48;
                    $title.find('.inner').css({
                        'width': title_width
                    });
                }

                if (modal.maximize){
                    title_width = title_width - 34;
                    $title.find('.inner').css({
                        'width': title_width
                    });
                }

            }

            modal = modal || xmodal.modals[$modal.attr('id')];

            resizeBody($modal, modal);

            $(window).resize(function () {
                resizeBody($modal, modal);
            });

        };
        //////////////////////////////////////////////////




        //////////////////////////////////////////////////
        // define some default buttons
        //////////////////////////////////////////////////
        xmodal.defaultButtons = function(_opts){

            _opts = _opts || {};
            _opts.buttons = _opts.buttons || {};

            //console.log(_opts.buttons);
            var buttons = {};

            function newDefaultButton(){
                var btn={};
                btn.label = 'OK';
                btn.action = false;
                btn.close = true; // do we close the modal when this button is clicked?
                return btn;
            }

            buttons.ok = newDefaultButton();
            buttons.ok.label = _opts.okLabel || 'OK';
            buttons.ok.action = _opts.okAction || $.noop;
            buttons.ok.close = (_opts.okClose !== false);
            buttons.ok.isDefault = true;
            //buttons.ok.className = '';
            //buttons.ok = {
            //    label: _opts.okLabel || 'OK',
            //    className: '',
            //    action: _opts.okAction || $.noop(),
            //    close: (_opts.okClose !== false), // do we close the modal when this button is clicked?
            //    isDefault: true
            //};

            buttons.cancel = newDefaultButton();
            buttons.cancel.label = _opts.cancelLabel || 'Cancel';
            buttons.cancel.action = _opts.cancelAction || $.noop;
            //buttons.cancel= {
            //    label: _opts.cancelLabel || 'Cancel',
            //    className: '',
            //    action: _opts.cancelAction || $.noop(),
            //    close: true // do we close the modal when this button is clicked?
            //};

            // shortcuts to hide the ok and cancel buttons
            if (_opts.ok === false || _opts.ok === 'hide') {
                buttons.ok = false
            }
            if (_opts.cancel === false || _opts.cancel === 'hide') {
                buttons.cancel = false
            }
            //this.defaultButton = this.buttons.ok;

            //buttons = $.extend(true, buttons, _opts.buttons);

            //console.log(buttons);

            return buttons;

        };
        //xmodal.defaultButtons = xmodal.defaultButtonSetup();
        //////////////////////////////////////////////////




        //////////////////////////////////////////////////
        // HTML output factory
        //////////////////////////////////////////////////
        xmodal.output = function (_opts) {

            // make sure we've AT LEAST got an empty object
            var modal = $.extend(true, {}, _opts);

            function renderButtons() {

                var html = '';

                modal.buttons = ($.isPlainObject(modal.buttons)) ? modal.buttons : {};

                $.each(modal.buttons, function (_prop, _value) {
                    // we NEED an object for the button value for proper configuration
                    // use false, null or '' to remove a default 'ok' or 'cancel' button
                    if (!$.isPlainObject(_value)) return;
                    var button = this;
                    button.className = button.classes;
                    var button_id = modal.id + '-' + _prop + '-button';
                    // 'button' and the name of this button's property
                    // are automatically added to the class attribute
                    var classNames = [_prop];
                    if (button.link) {
                        classNames.push('link');
                    }
                    else {
                        classNames.push('button');
                    }
                    if (button.className && button.className > '') { classNames.push(button.className) }
                    if (button.close === true) { classNames.push('close') }
                    if (button.isDefault === true) { classNames.push('default') }

                    html += '' +
                        '<a href="javascript:" id="' + button_id + '"' +
                        ' class="' + classNames.join(' ') + '">' + button.label + '</a> ';

                    var button_ = modal.buttons[_prop];
                    var button_action = button_.action;
                    // bind onclick events to THESE buttons
                    $body.on('click', '#' + button_id, function () {
                        if ($.isFunction(button_action)) {
                            button_action(modal);
                        }
                        if ($(this).hasClass('close')) {
                            xmodal.close(modal.id);
                        }
                    });
                });

                return html;
            }
            //
            var buttons = renderButtons();

            // create object to collect the output
            var output={};

            output.container={};

            // HTML to put BEFORE the xmodal stuff
            output.container._pre = '';

            // HTML for the mask
            output.container._pre +=
                // only generate the mask if not false (true is default)
                (modal.mask !== false) ?
                    '<div class="' + [xmodal.strings.mask, xmodal.strings.version, modal.className].join(' ') + '"' +
                        ' id="' + modal.id + '-mask" data-xmodal-id="' + modal.id + '"' +
                        ' data-xmodal-x="' + modal.count + '"></div>' :
                    '';

            // HTML for the modal container
            output.container._pre +=
                '<div class="' + [xmodal.strings.base, xmodal.strings.version, modal.className].join(' ') + '"' +
                    ' id="' + modal.id + '" data-xmodal-x="' + modal.count + '">';

            output.container.post_ = '</div>';

            if (modal.title !== false){
                modal.titleStyle = '';
            }
            else {
                modal.titleStyle = ' style="height:15px;"';
                modal.title = '';
                modal.closeBtn = false;
            }
            output.title  = '<div title="' + modal.title + '" class="title"' + modal.titleStyle + '>';
            output.title += '<span class="inner">' + modal.title + '</span>';
            output.title += (modal.maximize === true) ?
                '<b class="maximize" title="maximize/minimize this dialog">&ndash;</b>':
                '';
            output.title += (modal.closeBtn === true) ?
                '<b class="close" title="(alt-click to close all modals)">&times;</b>' :
                '';
            output.title += '</div>';


            output.body =
                '<div class="body content">' +
                '' + '<div class="inner" style="padding:'+modal.padding+';">' + modal.content + '</div>' +
                '</div>';


            if (modal.footer !== false) {
                output.footerStyle =
                    'height:' + modal.footer.height + ';' +
                    'background-color:' + modal.footer.background + ';' +
                    'border-color:' + modal.footer.border + ';' +
                    '';
                output.footer =
                    '<div class="footer" style="' + output.footerStyle + '">' +
                    '<div class="inner">' +
                    '<span class="content">' + modal.footer.content + '</span>' +
                    '<span class="buttons">' + buttons + '</span>' +
                    '</div>' + // end .inner
                    '</div>'; // end .footer
            }
            else {
                output.footer = '';
            }

            // if we want to TOTALLY overwrite the HTML,
            // or force any other property, put those
            // properties in a 'custom' property of
            // the object passed to the 'xmodal.Modal' constructor
            var custom = (_opts && _opts.custom) ? _opts.custom : {};
            $.extend(true, output, custom);

            output.html =
                output.html ||
                output.container._pre + output.title + output.body + output.footer + output.container.post_;
            output.opts = _opts; // pass along the original _opts if needed later
            output.config = modal; // the combined config object

            // assign the 'close' onclick handler to THIS modal
            $body.on('click', '#' + modal.id + ' .close', function () {
                xmodal.close(modal.id);
            });

            return output;

        };
        //////////////////////////////////////////////////




        //////////////////////////////////////////////////
        // xmodal.Modal constructor - magic starts here
        //////////////////////////////////////////////////
        xmodal.Modal = function (_opts) {

            // extending _opts with an empty object SHOULD make sure
            // we're working with an object, even if it's empty
            _opts = _opts || {};

            //xmodal.count++;
            this.count = _opts.count || ++xmodal.count;
            this.id = 'xmodal' + this.count;
            // we can use 'className', 'classNames' or 'classes'
            this.className = this.classNames = this.classes =
                _opts.className || _opts.classNames || _opts.classes || '' ;
            this.kind = 'dialog'; // is this used now? let's use 'preset' objects to set default properties for different types of modals
            this.width = 600;
            this.height = 400;
            this.position = false; // top: css property - false is vertically centered
            this.animation = 'fade'; // false, 'fade' or 'slide'
            this.duration = _opts.speed || _opts.duration || 100; // if we're passing duration as 'speed'
            this.speed = this.duration;
            this.modal = (typeof _opts.modal != 'undefined') ? _opts.modal : true ;
            this.mask = _opts.mask || this.modal ; // is this dialog modal? defaults to 'true' so mask shows
            this.title = 'Message';
            this.closeBtn = true;
            this.maximize = false;
            //this.padding = _opts.padding; // amount of padding - in pixels
            this.template = false; // don't use a template by default (set a value if a condition below is met)

            if (_opts.template){
                if (_opts.template.jquery) {
                    this.template = this.$template = _opts.template
                }
                else if (_opts.template.charAt(0) === '#') {
                    this.template = this.$template = $(_opts.template)
                }
                else {
                    this.template = this.$template = $('#'+_opts.template);
                }
            }

            // what is the mess in the value of this.content?
            // 1) set this.content and _opts.content to _opts.content or "(no content)" if we're NOT using a template
            // 2) BUT... if we ARE using a template (if there's a value set for _opts.template), grab the HTML from that
            this.content = _opts.content =
                (this.template !== false) ?
                    this.template.html() :
                    _opts.content || '(no content)';
            this.scroll = true;
            this.footer = { // this.footer = false -- will not render footer
                //show: true, // probably don't need this - just pass "false" to "footer" property to not render footer
                content: '',
                height: '50px',
                background: '#f0f0f0',
                border: '#e0e0e0',
                buttons: true
            };
            // okLabel, okAction, okClose, cancelLabel, cancelAction included for easy config of default buttons

            this.buttons = ($.isPlainObject(_opts.buttons)) ? this.buttons = _opts.buttons : this.buttons = xmodal.defaultButtons(_opts);

            this.isDraggable = _opts.draggable || true;

            if (this.template !== false && this.template.jquery) {
                this.template.empty()
            }

            this.original = this.inputObject = _opts;

            // merge defaults with config object
            // properties in '_opts' override 'this'
            //var modal = $.extend(true, this, _opts);
            //
            $.extend(true, this, _opts);

            // private property to let xmodal.open() know if
            // this constructor has been called yet
            this.newModal = true;

        };
        //////////////////////////////////////////////////





        //////////////////////////////////////////////////
        // xmodal GO!
        //////////////////////////////////////////////////
        xmodal.open = function (obj) {

            var modal, output, modal_id;
            // set xmodal.modals.max before calling this script
            // to set a different value for max number of xmodal modals
            var max = xmodal.modals.max || 100;
            // limit to 100 open modals
            // if there are more than that,
            // there is likely a problem somewhere else
            if ($(xmodal.dialog.box).length > (max-1)) { return }

            // also limit to 200 total modals (open or not)
            //if ($(xmodal.dialog.box).length > 199) { return }

            // if 'obj' has ( newModal: true ) then 'obj' was
            // created with ( new xmodal.Modal() ) constructor
            // and we can pass obj as-is
            if (obj.newModal === true){
                modal = obj;
            }
            // otherwise pass 'obj' to xmodal.Modal(obj) constructor
            else {
                modal = new xmodal.Modal(obj);
            }

            output = xmodal.output(modal);
            modal.html = output.html;

            // put the html on the page, but don't show it yet
            $body.append(modal.html);

            modal_id = '#' + modal.id;
            modal.$modal = $modal = $(modal_id);

            var top_ = modal.top || 0,
                left_ = modal.left || 0;

            if ($.isPlainObject(modal.position)) {
                top_ = modal.position.top || top_;
                left_ = modal.position.left || left_;
            }

            // adjust height if there's no text in the title bar
            if (modal.title === false) {
                modal.height = modal.height-10
            }

            modal.$modal.css({
                'top': top_,
                'bottom': (top_ !== 0) ? 'auto' : 0,
                'left': left_,
                'right': (left_ !== 0) ? 'auto' : 0,
                'width': modal.width,
                'height': modal.height,
                'z-index': ++xmodal.topZ + 2
            });

            if (modal.scroll === true) {
                modal.$modal.find('.body').addClass('scroll');
            }

            if ($.isFunction(modal.beforeShow)) {
                modal.beforeShow(modal);
            }

            modal.$modal.find('.body .inner').fadeIn(modal.speed);

            function afterRender($modal) {

                xmodal.resize($modal, modal);

                $('body').addClass('open');
                $('html').addClass('noscroll');

                if ($.isFunction(modal.afterShow)) {
                    modal.afterShow(modal);
                }

                xmodal.modals._ids.push(modal.id);

            }

            if (modal.mask !== false){
                //modal.$mask = modal.$modal.closest('xmodal-mask');
                modal.$mask = $mask = $(modal_id + '-mask');
                modal.$mask.css({
                    'z-index': ++xmodal.topZ
                });
                modal.$mask.show().addClass('open');
            }

            if (modal.animation === 'fade') {
                //$mask.fadeIn(modal.speed / 2);
                modal.$modal.fadeIn(modal.speed, function() {
                    afterRender($(this));
                });
            }
            else if (modal.animation === 'slide') {
                //$mask.fadeIn(modal.speed / 2);
                modal.$modal.slideDown(modal.speed, function() {
                    afterRender($(this));
                });
            }
            else {
                //$mask.show();
                if (modal.animation !== 'hide'){
                    modal.$modal.show();
                    afterRender($modal);
                }
            }

            $(xmodal.dialog.box).removeClass('top'); // strip 'top' class from ALL xmodal modals
            modal.$modal.addClass('open top'); // add 'top' (and 'open') class to only top one

            if (modal.isDraggable !== false) {
                if (typeof $.fn.drags != 'undefined') {
                    modal.$modal.drags({ handle: '.title' });
                }
            }

            // copy to Velocity-safe var names
            modal.__mask  = modal.$mask;
            modal.__modal = modal.$modal;

            // save a reference to this modal
            xmodal.modals[modal.id] = modal;

            return modal;

        };
        //////////////////////////////////////////////////
        xmodal.show = xmodal.open;
        // preference is to call xmodal.open(),
        // but xmodal.show() should work too





        //////////////////////////////////////////////////
        // close the xmodal and delete its object
        //////////////////////////////////////////////////
        xmodal.close = function (_id) {

            var $top, $modal, modal_id, close_modal=true;

            // if there is no argument passed,
            // we'll use the last (top) open modal
            if (typeof _id == 'undefined'){
                $top = $(xmodal.dialog.top);
                $modal = ($top.length) ? $top.last() : $(xmodal.dialog.open).last() ;
                modal_id = $modal.attr('id');
            }
            else {
                // if a jQuery DOM object is passed in,
                // use it as-is, then get the id
                if (_id.jquery){
                    $modal = _id;
                    modal_id = $modal.attr('id');
                }
                // the only other option is to pass
                // the element's id
                else {
                    $modal = $('#'+_id);
                    modal_id = _id;
                }
            }

            var $mask = $('#' + modal_id + '-mask');

            // fade out then remove the modal
            $modal.fadeOut(0, function(){
                $(this).remove();
                // then if there's a mask that goes with it
                // get rid of that too
                if ($mask.length){
                    $mask.fadeOut(0, function(){
                        $(this).remove();
                    });
                }
                if (!$(xmodal.dialog.open).length) {
                    $body.removeClass('open');
                    $html.removeClass('noscroll');
                    return ; // be done if there are no more open modals
                }
                $(xmodal.dialog.open).last().addClass('top');
            });

            // NONE OF THE CODE BELOW THIS WILL EXECUTE
            // IF NO MATCHING 'xmodal.modals[id]' OBJECT IS FOUND
            // (it's for your own good, kid)
            if (!xmodal.modals[modal_id]) { return } // should prevent errors if this xmodal.modals object doesn't exist

            // not gonna decrement the count 'cause it doesn't really matter
            //if (xmodal.count > 0) { xmodal.count-- }

            // localize the object for this modal
            var modal = xmodal.modals[modal_id];

            if ($.isFunction(modal.onClose)) {
                close_modal = modal.onClose(modal);
            }

            // Return the template content to its original location
            // (if we used a template)
            //
            // IMPORTANT NOTE:
            // Any event listeners attached to template elements
            // will be removed because the original DOM elements
            // do not exist anymore - there are NEW elements that
            // match the old ones EXACTLY, but they're not the same.
            //
            // Attach listeners with jQuery's 'on' method, like:
            // $('body').on('click', '#element-id', function(e){ doSomething() });
            //
            // of course the lovely 'onclick' attributes will work too
            // :-/
            if (modal.$template) {
                modal.$template.html(modal.original.content);
            }

            // not sure why we'd want to delete an xmodal.modals[modal] object
            // for a modal that isn't the current one, but we can here.
            // I guess you could override the .deleteModal() method to not delete
            // anything at all - but why?
            if (modal.deleteModal !== false){

                // one last chance to do something with
                // this modal's object
                if ($.isFunction(modal.onDelete)){
                    modal.onDelete(modal);
                }

                xmodal.deleteModal(modal.id);

            }

            if ($.isFunction(modal.afterClose)) {
                modal.afterClose(modal);
            }

            // set the local 'modal' object to xmodal.closed
            // if we need a reference to the one we JUST closed
            return xmodal.closed = modal;

        };
        //////////////////////////////////////////////////




        //////////////////////////////////////////////////
        // close all open xmodal modals
        //////////////////////////////////////////////////
        xmodal.closeAll = function( _$modals, _delete ){
            _$modals = _$modals || $(xmodal.dialog.open);
            var $modals = (_$modals.jquery) ? _$modals : $(_$modals);
            var timeout = 0;
            $($modals.get().reverse()).each(function(){
                var $modal = $(this);
                var id = $modal.attr('id');
                if (typeof _delete !== 'undefined'){
                    xmodal.modals[id].deleteModal = _delete;
                }
                setTimeout(function(){
                    xmodal.close($modal);
                }, (timeout += 10) );
            });
        };
        //////////////////////////////////////////////////




        //////////////////////////////////////////////////
        // maximize this modal
        //////////////////////////////////////////////////
        xmodal.maximize = function(modal){
            var $modal;
            if (!modal) { return }
            if (modal.jquery){
                $modal = modal;
            }
            else {
                $modal = $('#'+modal);
            }
            $modal.toggleClass('maxxed');
        };
        //////////////////////////////////////////////////




        //////////////////////////////////////////////////
        // preset for simple messages with one button
        //////////////////////////////////////////////////
        xmodal.alerts={};
        //xmodal.alerts.count = 0;
        //
        xmodal.presets.Message = function (opts) {
            opts = opts || {};
            //xmodal.alerts.count = ++xmodal.count;
            this.count = opts.count || ++xmodal.count;
            this.kind = 'alert';
            this.id = 'xmodal' + this.count + '-alert';
            this.width = 420;
            this.height = 220;
            this.animation = 'fade';
            this.speed = 100;
            this.title = false;
            this.content = opts.content || opts.message || '(no message - must set value for "content" property)';
            this.scroll = false;
            this.closeBtn = false;
            this.buttons = {};
            this.buttons.ok = {};
            this.buttons.ok.label = opts.label || opts.button || opts.okLabel || opts.buttonLabel || 'OK';
            this.buttons.ok.action = opts.action || opts.okAction || $.noop;
            this.buttons.ok.close = true;
            this.buttons.ok.isDefault = true;
            this.buttons.cancel = false; // prevents default "Cancel" button from rendering
            this.closeModal = xmodal.closeModal = opts.closeModal || true ; // alert messages should be dismissed by pressing the enter key
        };
        //
        xmodal.message = function( /* _title/_msg/_opts , _msg/_buttonText/_opts , _buttonText/_opts , _opts */ ) {

            //_opts = $.extend(true, {}, _opts);

            var opts = {};
            var arg1 = arguments[0];
            var arg2 = arguments[1];
            var arg3 = arguments[2];
            var arg4 = arguments[3];

            if (arguments.length === 0){
                throw new Error('Message text and/or configuration object required.');
            }


            // figure out what to do based on the number of arguments that are passed
            switch (arguments.length){
                //
                case 1:
                    // if there's one argument and it's not an object
                    // it must be a string for the message text
                    if (!$.isPlainObject(arg1)){
                        opts.content = arg1;
                    }
                    // if it's a plain object, set 'opts' to that
                    else {
                        opts = arg1;
                    }
                    break;
                //
                case 2:
                    // if there are two arguments,
                    // and the second one is NOT an object
                    // the first must be the title
                    // and the second the message text
                    if (!$.isPlainObject(arg2)){
                        opts.title   = arg1;
                        opts.content = arg2;
                    }
                    // if the second arg is an object,
                    // the first must be the message text
                    // and the second a config object
                    else {
                        opts = arg2;
                        opts.content = arg1;
                    }
                    break;
                //
                case 3:
                    // if there are three arguments,
                    // and the third is NOT an object,
                    // the third must be a custom button label
                    if (!$.isPlainObject(arg3)){
                        opts.buttonLabel = arg3;
                    }
                    // if the third arg is an object,
                    // it must be a config object
                    else {
                        opts = arg3;
                    }
                    // in all cases when there three args,
                    // the first must be the title,
                    // and the second must be the message text
                    opts.title = arg1;
                    opts.content = arg2;
                    break;
                //
                default:
                    // if all four arguments are present...
                    opts             = arg4;  // fourth is a config object
                    opts.title       = arg1;  // first is the title
                    opts.content     = arg2;  // second is the message content
                    opts.buttonLabel = arg3;  // third is a custom button label
            }

            // seems redundant to pass _opts to the constructor
            // as well as merging with the alert var
            var alert = new xmodal.presets.Message(opts);

            // or maybe not since we alias some properties (like okLabel)
            opts = $.extend(true, {}, alert, opts);

            opts.className = [(opts.className || opts.classNames || opts.classes || ''), 'xmodal-message'].join(' ');

            xmodal.open(opts);

            return opts;

        };
        // xmodal.alert('message text') may be more intuitive to call
        // as a replacement to the standard JavaScript alert() function.
        xmodal.alert = xmodal.message;
        // maybe "alert" is more descriptive than "message"?
        // but either will work
        //
        //sample usage:
        //    xmodal.alert('This is an alert.');
        //    xmodal.alert({
        //         title: 'Error',
        //         content: 'There was an error. Please fix it.',
        //         label: 'OK',
        //         action: function(){ console.log('There was an error with the data') },
        //         scroll: true
        //    });
        //////////////////////////////////////////////////




        xmodal.confirm = function(_opts){
            _opts = _opts || {};
            //xmodal.closeModal = true;
            if (!$.isPlainObject(_opts)) {
                throw new Error('Please pass a configuration object as the argument for the xmodal.confirm(opts) function.');
            }
            //var confirmation = new xmodal.presets.Message(_opts);
            // pretty much the same as the Alert/Message, but with a cancel button.
            var confirmation={};
            confirmation.title = 'Confirmation';
            confirmation.buttons={};
            confirmation.buttons.cancel={};
            confirmation.buttons.cancel.label = _opts.cancelLabel || 'Cancel';
            confirmation.buttons.cancel.action = _opts.cancelAction || $.noop;
            confirmation.buttons.cancel.close = true;
            var opts = $.extend(true, {}, confirmation, _opts);
            xmodal.message(opts);
            //return opts;
        };




        //////////////////////////////////////////////////
        // preset for simple 'loading' dialog
        //////////////////////////////////////////////////
        xmodal.loading={};
        xmodal.loading.count = 0;
        //
        xmodal.loading.open = function( _title, _id, _opts ){

            _opts = _opts || {};

            var loading={};
            loading.id = _id || 'xmodal-loading-' + (++xmodal.loading.count);
            loading.title = _title || false;
            loading.width = 260;
            loading.height = 90;
            //loading.template = 'xmodal-loading';
            loading.content = $('#xmodal-loading').html();
            loading.animation = false;
            loading.closeBtn = false;
            loading.footer = false;
            var opts = $.extend(true, {}, loading, _opts);
            opts.className = [(_opts.className || _opts.classNames || _opts.classes || ''), 'loader loading'].join(' ');
            xmodal.open(opts);
        };
        //
        xmodal.loading.close = function(_id){
            var $loaders = $('div.xmodal.loading.open');
            if (!$loaders.length) { return } // stop if there are no loading modals
            var $top_loader = $loaders.filter('.top');
            var id = _id || $top_loader.attr('id');
            //xmodal.loading.count--;
            xmodal.close(id)
        };
        //////////////////////////////////////////////////




    });
    // end $(function(){ });
    //////////////////////////////////////////////////




})();



// will need to map old parameters to new for backwards compatibility

// example of (old) xmodal object with all settings
/*
xmodal.defaultsOld = {
    //id: 'unique_id_for_this_modal', // id to give to new xmodal 'window' - if omitted, will be generated dynamically
    kind: 'fixed',  // options: 'dialog','fixed','large','med','small','custom' - defaults to 'fixed'
    width: 600, // optional - width in px - used for 'fixed','custom','static' - defaults to 600
    height: 400, // optional - height in px - used for 'fixed','custom','static' - defaults to 400
    scroll: 'yes', // optional - does content need to scroll? - defaults to yes, any other value prevents scrolling
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
    okClose: 'yes', // optional - modal closes by default when clicking 'ok' - use 'no' if not closing on 'ok' click (useful to open an xmodal from an xmodal)
    cancel: 'show', // optional - defaults to 'show' - use 'hide' to suppress cancel button
    cancelLabel: 'Cancel', // optional - label for 'cancel' button - defaults to 'Cancel'
    cancelAction: function(){}, // optional - if omitted, will do nothing (empty function)
    cancelClose: 'yes', // optional - modal closes by default when clicking 'cancel' button - use 'no' to not close
    defaultButton: 'ok' // optional - defaults to 'ok - 'ok' or 'cancel' - which button is the default?
    // if you want more than 2 buttons, you'll need to use the 'existing' value for the 'content' property and include the proper markup
};
*/
