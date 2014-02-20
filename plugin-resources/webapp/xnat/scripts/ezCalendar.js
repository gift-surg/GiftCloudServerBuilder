/*
 * D:/Development/XNAT/1.6/xnat_builder_1_6dev/plugin-resources/webapp/xnat/scripts/ezCalendar.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 1/21/14 9:53 AM
 */

if (typeof XNAT == 'undefined') XNAT={};
// if the XNAT.app namespace object isn't defined yet, we've STILL got some problems
if (typeof XNAT.app == 'undefined') XNAT.app={};
if (typeof XNAT.app.defaults == 'undefined') XNAT.app.defaults={};
if (typeof XNAT.app.datePicker == 'undefined') XNAT.app.datePicker={};
// making sure there's an XNAT.data object
if (typeof XNAT.data == 'undefined') XNAT.data={};


XNAT.app.datePicker.count = 0;


XNAT.app.datePicker.reveal = function(_$cal){
    _$cal.find('.calendar').fadeIn(100);
};


XNAT.app.datePicker.selected = XNAT.data.selectedDate = {};


function closeModal_pickDate(_this,$input){
    var modal_id = $(_this).closest('.xmodal').attr('id');
    xModalCloseNew(modal_id);
    //_$input.focus();
    XNAT.app.datePicker.reveal($input.closest('.ez_cal_wrapper'));
}


function focusInput($input){
    if ($input.is('input.single')){
        $input.focus();
        $input.select();
    }
    // if 'multi', the class will be on element (div, span, etc.)
    // CONTAINING the mm, dd, and yyyy input fields
    if ($input.is('.multi')){
        $input.find('input.date').first().focus();
    }
}


XNAT.app.maskDateInput = function( _$input , _filler , _format ){

    if ( !$.mask.dataName || !_$input) return ; // exit if the jquery.maskedinput plugin is not loaded

    var $input, filler, format ;

    $input = (_$input.jquery) ? _$input : $(_$input); // we prefer a jQuery object, but we'll transmogrify a regular selector if necessary

    if ($input.hasClass('us')){ format='us' }
    if ($input.hasClass('iso')){ format='iso' }

    format = _format || format || 'us';
    format = format.toLowerCase() ;

    if (format === 'us' || format === 'eu' || format === 'euro'){
        filler = _filler || '_';
        $input.mask('99/99/9999',{ placeholder: filler }); // requires the jquery.maskedinput plugin
    }
    else if (format === 'iso'){
        filler = _filler || ' ';
        $input.mask('9999-99-99',{ placeholder: filler }); // requires the jquery.maskedinput plugin
    }
    else {
        // nothing?
    }
};


XNAT.app.checkDateInput = function(_$input,_format,_value) {

    if (!_$input && !_format && !_value) return false ; // don't be dumb - pass the args

    var $input, format, value ;
    var valid_format, kind, example, $focus, date={}, message_opts={}, future=false ;

    format = (_format && _format !== '') ? _format.toLowerCase() : '';

    // pass null or '' for first argument if checking with _value
    if (_$input && _$input !== ''){

        $input = (_$input.jquery) ? _$input : $(_$input); // we prefer a jQuery object, but we'll transmogrify a regular selector if necessary

        if (format === '') { // there's no 'format'? use the class
            if ($input.hasClass('us')){ format='us' }
            if ($input.hasClass('iso')){ format='iso' }
        }

        if ($input.hasClass('multi')){
            kind = 'multi' ;
            var YYYY = $input.find('input.year').val();
            var MM = $input.find('input.month').val();
            var DD = $input.find('input.day').val();
            var DATE_IN = YYYY + '-' + MM + '-' + DD; // put in 'iso' format
            valid_format = /^\d{4}\-\d{2}\-\d{2}$/ ; //Basic check for format validity
            example = '(use multiple input fields)';
            date = new SplitDate(DATE_IN,'iso');
            $focus = $input.find('input:first');
        }
        else {
            kind = 'single' ;
            date = new SplitDate($input.val(),format);
            $focus = $input ;
        }

        // if it has the 'future' class, allow future date selection
        if ($input.hasClass('future')) future = true ;

    }
    // if there's a value passed, use that instead of the value from the input
    else if (_value && _value !== ''){
        // add '-future' string to date value to allow future dates
        if (_value.indexOf('-future') !== -1) {
            future = true ;
            _value = _value.replace(/-future/gi,'');
        }
        date = new SplitDate(_value);
        format = date.format ;
    }
    else {
        return false ;
    }

    // if still no format, use 'iso'
    format = (format !== '') ? format : ((date) ? date.format : 'iso') ;

    if (format === 'us' || format === 'euro'){
        valid_format = /^\d{2}\/\d{2}\/\d{4}$/ ; //Basic check for format validity
        example = (format === 'euro') ? 'DD/MM/YYYY' : 'MM/DD/YYYY' ;
    }
    if (format === 'iso'){
        valid_format = /^\d{4}\-\d{2}\-\d{2}$/ ; //Basic check for format validity
        example = 'YYYY-MM-DD';
    }

    var return_val = false ;
    if ((date.mm === '  ' || date.mm === '' || date.mm === '13') && (date.dd === '  ' || date.dd === '' || date.dd === '32') && (date.yyyy === '    ' || date.yyyy === '') && $input.hasClass('onblur')){
        // don't freak out *just* because it's empty if it's validated onblur
        // go ahead and freak out if it's onsubmit
        return false ;
    }

    // concatenate today's year, month and day to represent a value for today (hacky? yes? effective? also yes.)
    // this will be used to compare with the selected date to prevent selecting a future date
    var max_date_string = XNAT.data.todaysDate.yyyy + XNAT.data.todaysDate.mm + XNAT.data.todaysDate.dd ;
    // convert concatenated string to a number for comparison
    var max_date_num = parseInt(max_date_string);

    date.max_future = max_date_num+1000000 ;
    date.min_past = max_date_num-1200000 ;

    //message_opts.action = function(){closeModal_pickDate(this,$input)};
    //message_opts.id = 'invalid_date' ;
    message_opts.height = 175 ;
    message_opts.okClose = false ;
    message_opts.action = function(){
        var modal_id = $(this).closest('.xmodal').attr('id');
        xModalCloseNew(modal_id);
        //focusInput($focus);
    };

    var useDatePicker = function(){
        if ($input && $input.closest('.ez_cal_wrapper').find('.calendar').length){
            return '' +
                ' or <a class="use_date_picker" href="javascript:" ' +
                //'onclick="$(this).closest(\'.xmodal\').find(\'.ok.button\').click()" ' +
                //'onclick="closeModal_pickDate();" ' +
                'style="text-decoration:underline;">use the date picker to select a date</a>'
        }
        else { return '' }
    };

    if (hasNumber(date.arr) && !valid_format.test(date.val)) {
        if (typeof xModalMessage != 'undefined') {
            xModalMessage(
                'Invalid Date Format',
                'Please enter the date in the format: ' + example + useDatePicker() + '.',
                'OK',
                message_opts
            );
        }
        else {
            alert('Invalid Date Format. Please enter the date in the format: ' + example + '.');
        }
    }
    else if (future === false && date.date_num > max_date_num && date.date_num < date.max_future){
        if (typeof xModalMessage != 'undefined') {
            xModalMessage(
                'Invalid Date',
                'You may not select a date in the future. Please correct' + useDatePicker() + ' and continue.',
                'OK',
                message_opts
            );
        }
        else {
            alert("You may not select a date in the future. Please enter a valid date and continue.");
        }
    }
    // check for sane date ranges - between 120 years ago and 100 years from now
    else if (date.date_num < date.min_past || date.date_num > date.max_future || date.date_string === '0000-00-00') {
        if (typeof xModalMessage != 'undefined') {
            xModalMessage(
                'Invalid Date',
                'Invalid Day, Month, or Year range detected. Please correct'  + useDatePicker() +  ' and continue.',
                'OK',
                message_opts
            );
        }
        else {
            alert("Invalid Day, Month, or Year range detected. Please enter a valid date and continue.");
        }
    }
    else {
        return_val = true;
    }

    $('body').on('click','.use_date_picker',function(){
        closeModal_pickDate(this,$input);
    });

//    if (return_val == false) {
//        $input.focus(); // this was causing issues with Chrome, but is not needed for this calendar
//        $input.select();
//    }
//    else {
//        $input.removeClass('invalid');
//    }

//    if (return_val === false){
//        if (console.log) console.log('invalid date');
//    }
//    else {
//        if (console.log) console.log('valid date')
//    }

    return { valid: return_val, date: date } ;

};


XNAT.app.datePicker.createInputs = function(_$e,_kind,_layout,_format,_opts){

    // Increment the number of calendars.
    // This helps to know how many there are
    // when there are multiple date inputs
    // on one page
    XNAT.app.datePicker.count++;
    var this_cal = 'cal'+ XNAT.app.datePicker.count;

    //var kind = _kind || 'multi' ; // make multi the default
    var kind = (_kind && _kind > '') ? _kind : 'multi' ; // make multi the default
    var layout = (_layout && _layout > '') ? _layout : 'inline' ; // make inline the default for multi
    var format = (_format && _format > '') ? _format : 'iso' ; // make ISO the default
    format = format.toLowerCase();

    var the_html='';
    var input_class='';

    if (_opts && _opts.validate){
        if (_opts.validate === 'onblur') input_class += ' validate onblur';
        if (_opts.validate === 'onsubmit') input_class += ' validate onsubmit';
    }

    var input_id='';
    if (_opts && _opts.input_id) input_id = _opts.input_id ;
    else if (_$e.data('input-id') && _$e.data('input-id') > '') input_id = _$e.data('input-id');
    else input_id = 'datepicker'+XNAT.app.datePicker.count;

    var input_name='';
    if (_opts && _opts.name) input_name = _opts.name ;
    else if (_$e.data('name') && _$e.data('name') > '') input_name = _$e.data('name');
    else if (_$e.data('input-name') && _$e.data('input-name') > '') input_name = _$e.data('input-name');
    else input_name = 'date';

    if (kind === 'single'){
        the_html += '\n' +
            '<input type="text" size="10" maxlength="10"';
        if (format === 'iso'){
            the_html +=
                ' placeholder="'+ 'YYYY-MM-DD' +'"' +
                    //' placeholder="'+ XNAT.data.todaysDate.ISO +'"' +
                    //' value="'+ XNAT.data.todaysDate.ISO  + '"' +
                    '';
            input_class += ' iso';
        }
        if (format === 'us'){
            the_html +=
                ' placeholder="'+ 'MM/DD/YYYY' +'"' +
                    //' placeholder="'+ XNAT.data.todaysDate.US +'"' +
                    //' value="'+ XNAT.data.todaysDate.US  + '"' +
                    '';
            input_class += ' us';
        }
        the_html +=
            ' id="' + input_id + '"' +
                ' name="'+ input_name +'"' +
                ' style="width:auto;font-family:Courier,monospace;"' +
                ' class="ez_cal single date' +
                input_class +
                '">' + '\n' +
                '';
    }

    var multi_labels='', multi_inputs='' ;
    if (kind === 'multi'){

        var month_input = '' +
            '<input class="ez_cal month validate" type="text" size="2" maxlength="2"' +
            //' placeholder="'+ XNAT.data.todaysDate.mm +'"' +
            ' placeholder="'+ 'MM' +'"' +
            //' value="'+ XNAT.data.todaysDate.mm + '"' +
            ' style="width:auto;font-family:Courier,monospace;">' +
            '';
        var day_input   = '' +
            '<input class="ez_cal day validate" type="text" size="2" maxlength="2"' +
            //' placeholder="'+ XNAT.data.todaysDate.dd +'"' +
            ' placeholder="'+ 'DD' +'"' +
            //' value="'+ XNAT.data.todaysDate.dd + '"' +
            ' style="width:auto;font-family:Courier,monospace;">' +
            '';
        var year_input  = '' +
            '<input class="ez_cal year validate" type="text" size="4" maxlength="4"' +
            //' placeholder="'+ XNAT.data.todaysDate.yyyy +'"' +
            ' placeholder="'+ 'YYYY' +'"' +
            //' value="'+ XNAT.data.todaysDate.yyyy + '"' +
            ' style="width:auto;font-family:Courier,monospace;">' +
            '';

        if (layout === 'stacked'){
            if (format === 'iso'){
                multi_labels = '\n' +
                    '   <tr>' + '\n' +
                    '       <td>year</td>' + '\n' +
                    '       <td>month</td>' + '\n' +
                    '       <td>day</td>' + '\n' +
                    '   </tr>' + '\n' +
                    '';
                multi_inputs = '\n' +
                    '   <tr>' + '\n' +
                    '       <td>'+ year_input +'</td>' + '\n' +
                    '       <td>'+ month_input +'</td>' + '\n' +
                    '       <td>'+ day_input +'</td>' + '\n' +
                    '   </tr>' + '\n' +
                    '';
            }
            if (format === 'us'){
                multi_labels = '\n' +
                    '   <tr>' + '\n' +
                    '       <td>month</td>' + '\n' +
                    '       <td>day</td>' + '\n' +
                    '       <td>year</td>' + '\n' +
                    '   </tr>' + '\n' +
                    '';
                multi_inputs = '\n' +
                    '   <tr>' + '\n' +
                    '       <td>'+ month_input +'</td>' + '\n' +
                    '       <td>'+ day_input +'</td>' + '\n' +
                    '       <td>'+ year_input +'</td>' + '\n' +
                    '   </tr>' + '\n' +
                    '';
            }
            the_html = '\n' +
                '<table class="date-input multi stacked" style="display:inline-block;vertical-align:middle;">' + '\n' +
                multi_labels +
                multi_inputs +
                '</table>' + '\n' +
                '';
        }
        if (layout === 'inline'){
            if (format === 'iso'){
                the_html = '\n' +
                    '<table class="date-input multi inline iso" style="display:inline-block;vertical-align:middle;">' + '\n' +
                    '   <tr>' + '\n' +
                    '       <td>'+ year_input +'</td>' + '\n' +
                    '       <td> - </td>' + '\n' +
                    '       <td>'+ month_input +'</td>' + '\n' +
                    '       <td> - </td>' + '\n' +
                    '       <td>'+ day_input +'</td>' + '\n' +
                    '   </tr>' + '\n' +
                    '</table>' + '\n' +
                    '';
            }
            if (format === 'us'){
                the_html = '\n' +
                    '<table class="date-input multi inline us" style="display:inline-block;vertical-align:middle;">' + '\n' +
                    '   <tr>' + '\n' +
                    '       <td>'+ month_input +'</td>' + '\n' +
                    '       <td> / </td>' + '\n' +
                    '       <td>'+ day_input +'</td>' + '\n' +
                    '       <td> / </td>' + '\n' +
                    '       <td>'+ year_input +'</td>' + '\n' +
                    '   </tr>' + '\n' +
                    '</table>' + '\n' +
                    '';
            }
        }
    }

    the_html += '<input type="hidden" class="date month" name="date-month" value="">';
    the_html += '<input type="hidden" class="date day" name="date-day" value="">';
    the_html += '<input type="hidden" class="date year" name="date-year" value="">';

    the_html += ' <button class="ez_cal insert-date" type="button" style="font-size:12px;">select date</button>' ;
    if (_opts && _opts.todayButton === true){
        the_html += ' &nbsp <a href="#" class="btn today use-todays-date" style="font-size:11px;text-decoration:underline;">use today\'s date</a>';
    }
    the_html += ' <div class="calendar" id="' + this_cal + '-container" style="display:none;position:absolute;right:0;float:none;"></div>' ;

    _$e.addClass('ez_cal_wrapper yui-skin-sam').css({position:'relative',whiteSpace:'nowrap'}).append(the_html);

    var $new_cal = _$e.find('input.ez_cal');

    if (_opts && _opts.required && _opts.required === true){
        $new_cal.addClass('required');
    }

    var default_value='', default_date={} ;
    if (_opts && _opts.defaultValue) default_value = _opts.defaultValue ;
    else if (_$e.data('default') && _$e.data('default') > '') default_value = _$e.data('default');

    if (default_value > ''){
        default_date = new SplitDate(default_value);
    }

    if (kind === 'single'){
        if (format === 'iso'){
            $new_cal.mask("9999-99-99",{placeholder:" "});
        }
        else {
            $new_cal.mask("99/99/9999",{placeholder:"_"});
        }
    }

    if (typeof default_date[format] != 'undefined'){
        if (kind === 'single'){
            $new_cal.val(default_date[format]);
        }
        if (kind === 'multi'){
            _$e.find('input.ez_cal.year').val(default_date.yyyy);
            _$e.find('input.ez_cal.month').val(default_date.mm);
            _$e.find('input.ez_cal.day').val(default_date.dd);
        }
    }

    var cal_config = {
        close: true,
        navigator: true
    };

    cal_config.selected = XNAT.data.todaysDate.US;

    // if the 'future' option is not specified, stop at today
    if (typeof _opts.future == 'undefined') cal_config.maxdate = XNAT.data.todaysDate.US;

    var ezCal = new YAHOO.widget.Calendar(this_cal+'-calendar', this_cal+'-container', cal_config);
    //var myEzCal = this.ezCal ;

    var ezCalSelect = function(type,args,obj) {

        var dates = args[0];
        var date = dates[0];

        var year = date[0];
        var month = zeroPad(date[1]);
        var day = zeroPad(date[2]);

        _$e.find('input:hidden.date.month').val(month);
        _$e.find('input:hidden.date.day').val(day);
        _$e.find('input:hidden.date.year').val(year);

        if (kind === 'single'){
            var $single = _$e.find('.ez_cal.single.date');
            if (format === 'iso'){
                $single.val(year + '-' + month + '-' + day);
            }
            if (format === 'us'){
                $single.val(month +'/'+ day +'/'+ year);
            }
        }

        if (kind === 'multi'){
            _$e.find('.ez_cal.month').val(month);
            _$e.find('.ez_cal.day').val(day);
            _$e.find('.ez_cal.year').val(year);
        }

        _$e.find('#'+this_cal+'-container').hide();

        // not used - additional function to fire on date select
        //_config.action(_$e);

    };

    ezCal.selectEvent.subscribe(ezCalSelect, ezCal, true);

    ezCal.render();

    //_$e.find('table.yui-calendar').css('width','180px'); // not using this because the width will change when changing months

};


// element(s) matching '_container' selector should already exist on the page
XNAT.app.datePicker.init = function($container,_config){

    // I guess we can make this work without any parameters
    // on any element with 'xnat-date' class?
    $container = $container || $('.xnat-date') ;

    $container = ($container.jquery) ? $container : $($container); // don't freak out if it's a selector instead of a jQuery object

    $container.each(function(){

        var $this_container = $(this);

        $this_container.empty();

        var _kind, _layout, _format, _validate, _today, _future ;

        // since the class matches the value we want to pass...
        // return the match (_val1 or _val2)
        var containerClass = function(_val1,_val2){
            var val ;
            if ($this_container.hasClass(_val1)){ val = _val1 }
            if ($this_container.hasClass(_val2)){ val = _val2 }
            return val ;
        };
        //
        _kind = containerClass('multi','single');
        _layout = containerClass('stacked','inline');
        _format = containerClass('iso','us');
        _validate = containerClass('onblur','onsubmit');
        //

        if ($this_container.hasClass('today')){ _today = true }

        if ($this_container.hasClass('future')){ _future = true }

        if ($this_container.data('validate') === 'onblur'){ _validate = 'onblur' }
        if ($this_container.data('validate') === 'onsubmit'){ _validate = 'onsubmit' }

        var opts = {} ;
        opts.kind = _kind ; // 'single' or 'multi'
        opts.layout = _layout ; // 'inline' or 'stacked' - for 'multi' only
        opts.format = _format ; // 'iso' or 'us'
        opts.validate = _validate ; // validate onblur or onsubmit - false if neither
        opts.todayButton = _today ; // show 'today' link/button - true or false
        opts.future = _future ; // if future==true, show future dates

        var _opts = $.extend( true, {}, opts, _config ); // values passed via the _config parameter overrides all

        XNAT.app.datePicker.createInputs($this_container, _opts.kind, _opts.layout, _opts.format, _opts);

    });

};


$(function(){

    $body = $('body');

    $body.on('click','.insert-date',function(){
        XNAT.app.datePicker.reveal($(this).closest('.ez_cal_wrapper'));
    });

    $body.on('click','.use-todays-date',function(){
        var $wrapper = $(this).closest('.ez_cal_wrapper');
        $wrapper.find('input.date.single').each(function(){
            var $date_input = $(this);
            if ($date_input.hasClass('iso')){
                $date_input.val(XNAT.data.todaysDate.ISO);
            }
            if ($date_input.hasClass('us')){
                $date_input.val(XNAT.data.todaysDate.US);
            }
        });
        $wrapper.find('input.year').val(XNAT.data.todaysDate.yyyy);
        $wrapper.find('input.month').val(XNAT.data.todaysDate.mm);
        $wrapper.find('input.day').val(XNAT.data.todaysDate.dd);
    });

    var checkDateOnBlur = function(input){
        if ($('div.xmask.open.top').data('xmodal-x') === xModalMessageCount) return ;
        //if (!$('#invalid_date')) return ;
        var $wrapper = $(input).closest('.ez_cal_wrapper');
        var $year = $wrapper.find('input:hidden.year');
        var $month = $wrapper.find('input:hidden.month');
        var $day = $wrapper.find('input:hidden.day');
        var $input = $(input);
        //var format = ($input.hasClass('iso')) ? 'iso' : 'us' ;
        if ($input.hasClass('single')){
            var dateCheck = XNAT.app.checkDateInput($input);
            //var newDate = new SplitDate($input.val(),format);
            if (dateCheck.valid === true){
                $year.val(dateCheck.date.yyyy);
                $month.val(dateCheck.date.mm);
                $day.val(dateCheck.date.dd);
            }
            else {
                $year.val(0);
                $month.val(0);
                $day.val(0);
            }
        }
        else {
            if ($input.hasClass('year')){
                $year.val($input.val());
            }
            if ($input.hasClass('month')){
                $month.val($input.val());
            }
            if ($input.hasClass('day')){
                $day.val($input.val());
            }
        }
    };

    // mask existing input elements
    $body.on('focus','input.date_mask',function(){
        XNAT.app.maskDateInput($(this));
    });

    // validate existing input elements
    $body.on('blur','input.check_date',function(){
        var format='' ;
        if ($(this).hasClass('us')) format = 'us';
        if ($(this).hasClass('iso')) format = 'iso';
        XNAT.app.checkDateInput('',format,$(this).val());
    });

    // maybe this is redundant with the xmodal check in checkDateOnBlur()?
    $body.on('focus','input.ez_cal.validate.onblur',function(){
        $(this).on('blur',function(){
            checkDateOnBlur(this);
        });
    });

    $body.on('blur','input.ez_cal.validate.onblur',function(){
        checkDateOnBlur(this);
        $(this).off('blur');
    });

    //
    // validate date inputs
    //
    // onblur
//    $body.on('blur','input.date.single.validate.onblur',function(){
//        XNAT.app.checkDateInput($(this));
//    });
    //
    // onsubmit
    $body.on('click','button.validate.onsubmit, a.validate.onsubmit',function(e){
        e.preventDefault();
        var $form = $(this).closest('form');
        var $wrapper = $form.find('.ez_cal_wrapper[data-validate="onsubmit"]');
        var $date_input = ($wrapper.hasClass('single')) ? $wrapper.find('input.date.single') : $wrapper;
        XNAT.app.checkDateInput($date_input);
    });
    //

    //
    // populate hidden date inputs
    //
    //$body.on('blur','input.')
});
