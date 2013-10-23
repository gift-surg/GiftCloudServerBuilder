/**
 (hopefully) simpler implementation of YUI calendar for XNAT
 probably not simpler, but maybe more robust?
 */

// if the XNAT namespace object isn't defined yet, we've got some problems
if (typeof XNAT == 'undefined') XNAT={};
// if the XNAT.app namespace object isn't defined yet, we've STILL got some problems
if (typeof XNAT.app == 'undefined') XNAT.app={};
if (typeof XNAT.app.datePicker == 'undefined') XNAT.app.datePicker={};
// making sure there's an XNAT.data object
if (typeof XNAT.data == 'undefined') XNAT.data={};

XNAT.app.datePicker.count = 0;

XNAT.app.datePicker.show = function(_$cal){
    _$cal.find('.calendar').fadeIn(100);
};


// initialize todaysDate object
// and put the stuff in there
XNAT.data.todaysDate = {} ;
(function(){
    var dateObj = XNAT.data.todaysDate ;
    dateObj.date = new Date();
    dateObj.gotMonth = dateObj.date.getMonth();
    dateObj.m = (dateObj.gotMonth + 1).toString() ;
    dateObj.mm = (dateObj.m.length === 1) ? '0'+ dateObj.m : dateObj.m ;
    dateObj.d = dateObj.date.getDate().toString();
    dateObj.dd = (dateObj.d.length === 1) ? '0'+ dateObj.d : dateObj.d ;
    dateObj.yyyy = dateObj.date.getFullYear().toString();
    dateObj.ISO = dateObj.yyyy + '-' +  dateObj.mm + '-' + dateObj.dd;
    dateObj.US = dateObj.mm + '/' +  dateObj.dd + '/' + dateObj.yyyy;
})();

XNAT.data.selectedDate = {};

function splitDate(_date,_format){
    var date={}, valid_format ;
    if (_format === 'us'){
        valid_format = /^\d{2}\/\d{2}\/\d{4}$/ ; //Basic check for format validity
        date.arr = _date.split('/');
        date.mm = date.arr[0];
        date.dd = date.arr[1];
        date.yyyy = date.arr[2];
        date.format = '01/31/2013';
    }
    if (_format === 'iso'){
        valid_format = /^\d{4}\-\d{2}\-\d{2}$/ ; //Basic check for format validity
        date.arr = _date.split('-');
        date.yyyy = date.arr[0];
        date.mm = date.arr[1];
        date.dd = date.arr[2];
        date.format = '2013-01-31';
    }
    if (valid_format) return date ;
    else return _date ;
}

function closeModal_pickDate(_this,$input){
    xModalCloseNew('',$(_this).closest('div.xmask.top'));
    //_$input.focus();
    XNAT.app.datePicker.show($input.closest('.ez_cal_wrapper'));
}

function focusInput($input){
    if ($input.is('input.single')){
        $input.focus();
        $input.select();
    }
    // if 'multi', class will be on element (div, span, etc.)
    // CONTAINING the mm, dd, and yyyy input fields
    if ($input.is('.multi')){
        $input.find('input.date').first().focus();
    }
}


// hate to admit I put a copy-paste script in here...
// but here it is... HIGHLY modified...
// (but seriously, how are they gonna know without the notice below?)
/**--------------------------
 //* Validate Date Field script- By JavaScriptKit.com
 //* For this script and 100s more, visit http://www.javascriptkit.com
 //* This notice must stay intact for usage
 ---------------------------**/
XNAT.app.checkDateInput = function($input,format) {
    // $input parameter REQUIRED - jQuery object

    // if format parameter isn't specified
    // and input doesn't have 'us' or 'iso' class,
    // 'iso' will be default:
    format = format || 'iso';
    // 'us' or 'iso' class will override format parameter
    if ($input.hasClass('us')){ format='us' }
    if ($input.hasClass('iso')){ format='iso' }
    format = format.toLowerCase();

    var valid_format, kind, $focus, date={}, message_opts={} ;

    var days = ['01','02','03','04','05','06','07','08','09','10','11','12','13','14','15','16','17','18','19','20','21','22','23','24','25','26','27','28','29','30','31'];
    var months = ['01','02','03','04','05','06','07','08','09','10','11','12'];
    var year_min = 1900 ;
    var year_max = XNAT.data.todaysDate.yyyy ;

    if ($input.hasClass('multi')){
        kind = 'multi' ;
        date.yyyy = $input.find('input.year').val();
        date.mm = $input.find('input.month').val();
        date.dd = $input.find('input.day').val();
        $focus = $input.find('input:first');
        date.val = date.yyyy+'-'+date.mm+'-'+date.dd;
        valid_format = /^\d{4}\-\d{2}\-\d{2}$/ ; //Basic check for format validity
        date.format = '2013-01-31';
    }
//    if ($input.is('input.single')){
    else {
        kind = 'single' ;
        date.val = $input.val();
        if (format === 'us'){
            valid_format = /^\d{2}\/\d{2}\/\d{4}$/ ; //Basic check for format validity
            date.arr = date.val.split('/');
            date.mm = date.arr[0];
            date.dd = date.arr[1];
            date.yyyy = date.arr[2];
            date.format = '01/31/2013';
        }
        if (format === 'iso'){
            valid_format = /^\d{4}\-\d{2}\-\d{2}$/ ; //Basic check for format validity
            date.arr = date.val.split('-');
            date.yyyy = date.arr[0];
            date.mm = date.arr[1];
            date.dd = date.arr[2];
            date.format = '2013-01-31';
        }
        $focus = $input ;
    }

    var return_val = false ;
    if (date.mm === '  ' && date.dd === '  ' && date.yyyy === '    ' && $input.hasClass('onblur')){
        // don't freak out just' because it's empty
        // but only if it's validated onblur - go ahead and freak out if it's onsubmit
        return false ;
    }
    var selected_date = new Date(date.yyyy, date.mm - 1, date.dd);
    var todays_date = new Date();
    //message_opts.action = function(){closeModal_pickDate(this,$input)};
    message_opts.height = 175 ;
    message_opts.action = function(){
        focusInput($focus);
    };
    if (!valid_format.test(date.val)) {
        if (typeof xModalMessage != 'undefined'){
            xModalMessage(
                'Invalid Date Format',
                'Please enter the date in the format: '+ date.format +' or <a class="use_date_picker" href="javascript:" ' +
                    //'onclick="$(this).closest(\'.xmodal\').find(\'.ok.button\').click()" ' +
                    //'onclick="closeModal_pickDate();" ' +
                    'style="text-decoration:underline;">use the date picker to select a date</a>.',
                'OK',
                message_opts
            );
        }
        else {
            alert('Invalid Date Format. Please enter the date in the format: '+ date.format +'.');
        }
    }
    //Detailed check for valid date ranges
    else if ((selected_date.getMonth() + 1 != date.mm) || (selected_date.getDate() != date.dd) || (selected_date.getFullYear() != date.yyyy)) {
        if (
        // as soon as I figure out how to make sure the date is not in the future
            selected_date > todays_date
//            window
//            && date.yyyy < year_min &&
//                date.yyyy > XNAT.data.todaysDate.yyyy &&
//                date.mm > XNAT.data.todaysDate.mm &&
//                date.dd > XNAT.data.todaysDate.dd &&
//                (parseInt(date.yyyy + date.mm + date.dd) > parseInt(XNAT.data.todaysDate.yyyy + XNAT.data.todaysDate.mm + XNAT.data.todaysDate.dd))
            ) {
            if (typeof xModalMessage != 'undefined') {
                xModalMessage(
                    'Invalid Date',
                    'Invalid Day, Month, or Year range detected. Please correct or <a class="use_date_picker" href="javascript:" ' +
                        //'onclick="$(this).closest(\'.xmodal\').find(\'.ok.button\').click()" ' +
                        'style="text-decoration:underline;">use the date picker to select a valid date</a> and continue.',
                    'OK',
                    message_opts
                );
            }
            else {
                alert("Invalid Day, Month, or Year range detected. Please correct and submit again.");
            }
        }
        //alert(todays_date)
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


    if (return_val === false){
        if (console.log) console.log('invalid date');
    }
    else {
        if (console.log) console.log('valid date')
    }

    return return_val ;

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

    var the_html = '' ;
    var input_class = '' ;

    if (_opts && _opts.validate){
        if (_opts.validate === 'onblur') input_class += ' validate onblur';
        if (_opts.validate === 'onsubmit') input_class += ' validate onsubmit';
    }

    var input_name;
    if (_opts && _opts.name) input_name = _opts.name ;
    else if (_$e.data('name') && _$e.data('name') > '') input_name = _$e.data('name');
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
        the_html += '' +
            ' name="'+ input_name +'"' +
            ' style="width:auto;font-family:Courier,monospace;"' +
            ' class="ez_cal single date' +
            input_class +
            '">' + '\n' +
            '';
    }

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

    var multi_labels='', multi_inputs='' ;
    if (kind === 'multi'){
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

    if (_opts && _opts.required && _opts.required === true){
        _$e.find('input.ez_cal').addClass('required');
    }

    var cal_config = {
        close: true,
        //maxdate: XNAT.data.todaysDate.US, // don't go past today
        navigator: true
    };

    // if the 'future' option is not specified, stop at today
    if (typeof _opts.future == 'undefined') cal_config.maxdate = XNAT.data.todaysDate.US;

    this.ezCal = new YAHOO.widget.Calendar(this_cal+'-calendar', this_cal+'-container', cal_config);
    //var myEzCal = this.ezCal ;

    this.handleSelect = function(type,args,obj) {

        var dates = args[0];
        var date = dates[0];
        var
            year = date[0],
            month = date[1].toString(),
            day = date[2].toString();

        if (month.length === 1){
            month = '0'+month;
        }
        if (day.length === 1){
            day = '0'+day;
        }

        _$e.find('input:hidden.date.month').val(month);
        _$e.find('input:hidden.date.day').val(day);
        _$e.find('input:hidden.date.year').val(year);

        if (kind === 'single'){
            if (format === 'iso'){
                _$e.find('.ez_cal.single.date').val(year + '-' + month + '-' + day);
            }
            if (format === 'us'){
                _$e.find('.ez_cal.single.date').val(month +'/'+ day +'/'+ year);
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

    this.ezCal.selectEvent.subscribe(this.handleSelect, this.ezCal, true);

    this.ezCal.render();

    _$e.find('table.yui-calendar').css('width','180px');

};


// element(s) matching '_container' selector should already exist on the page
XNAT.app.datePicker.init = function($container,_config){

    // I guess we can make this work without any parameters
    // on any element with 'xnat-date' class?
    $container = $container || $('.xnat-date') ;

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

        var opts = _config || {} ;
        opts.kind = (_config && _config.kind) ? _config.kind : _kind ; // 'single' or 'multi'
        opts.layout = (_config && _config.layout) ? _config.layout : _layout ; // 'inline' or 'stacked' - for 'multi' only
        opts.format = (_config && _config.format) ? _config.format : _format ; // 'iso' or 'us'
        opts.validate = (_config && _config.validate) ? _config.validate : _validate ; // validate onblur or onsubmit - false if neither
        opts.todayButton = (_config && _config.todayButton) ? _config.todayButton : _today ; // show 'today' link/button - true or false
        opts.future = (_config && _config.future) ? _config.future : _future ; // if future==true, show future dates

        XNAT.app.datePicker.createInputs($this_container, opts.kind, opts.layout, opts.format, opts);
        //XNAT.app.datePicker.init.createInputs($(this));

    });

};


$(function(){

    $body = $('body');

    $body.on('click','.insert-date',function(){
        XNAT.app.datePicker.show($(this).closest('.ez_cal_wrapper'));
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

    //
    // validate date inputs
    //
    // onblur
    $body.on('blur','input.date.single.validate.onblur',function(){
        XNAT.app.checkDateInput($(this));
    });
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
