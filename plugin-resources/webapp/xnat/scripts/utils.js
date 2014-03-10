/*
 * D:/Development/XNAT/1.6/xnat_builder_1_6dev/plugin-resources/webapp/xnat/scripts/utils.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 1/13/14 11:49 AM
 */

// indexOf method for IE8
if (!Array.prototype.indexOf) {
    Array.prototype.indexOf = function (searchElement /*, fromIndex */ ) {
        "use strict";
        if (this == null) {
            throw new TypeError();
        }
        var t = Object(this);
        var len = t.length >>> 0;
        if (len === 0) {
            return -1;
        }
        var n = 0;
        if (arguments.length > 1) {
            n = Number(arguments[1]);
            if (n != n) { // shortcut for verifying if it's NaN
                n = 0;
            } else if (n != 0 && n != Infinity && n != -Infinity) {
                n = (n > 0 || -1) * Math.floor(Math.abs(n));
            }
        }
        if (n >= len) {
            return -1;
        }
        var k = n >= 0 ? n : Math.max(len - Math.abs(n), 0);
        for (; k < len; k++) {
            if (k in t && t[k] === searchElement) {
                return k;
            }
        }
        return -1;
    }
}

// map method for IE8
// Production steps of ECMA-262, Edition 5, 15.4.4.19
// Reference: http://es5.github.com/#x15.4.4.19
if (!Array.prototype.map) {
    Array.prototype.map = function(callback, thisArg) {
        var T, A, k;
        if (this == null) {
            throw new TypeError(" this is null or not defined");
        }
        var O = Object(this);
        var len = O.length >>> 0;
        if (typeof callback !== "function") {
            throw new TypeError(callback + " is not a function");
        }
        if (thisArg) {
            T = thisArg;
        }
        A = new Array(len);
        k = 0;
        while(k < len) {
            var kValue, mappedValue;
            if (k in O) {
                kValue = O[ k ];
                mappedValue = callback.call(T, kValue, k, O);
                A[ k ] = mappedValue;
            }
            k++;
        }
        return A;
    };
}

// add commas to numbers
function addCommas(nStr) {
    nStr += '';
    var
        x = nStr.split('.'),
        x1 = x[0],
        x2 = x.length > 1 ? '.' + x[1] : ''
    ;
    var rgx = /(\d+)(\d{3})/;
    while (rgx.test(x1)) {
        x1 = x1.replace(rgx, '$1' + ',' + '$2');
    }
    return x1 + x2;
}

// convert number to file size in KB, MB, GB
// rounded to 2 decimal places
function roundNumber(num, dec) {
    return Math.round(num * Math.pow(10, dec)) / Math.pow(10, dec);
}

function sizeFormat(fs) {
    if (fs >= 1073741824) {
        return roundNumber(fs / 1073741824, 2) + ' GB';
    }
    if (fs >= 1048576) {
        return roundNumber(fs / 1048576, 2) + ' MB';
    }
    if (fs >= 1024) {
        return roundNumber(fs / 1024, 0) + ' KB';
    }
    return fs + ' B';
}

// make sure the ajax calls are NOT cached
$.ajaxSetup({cache:false});


// where is the 'scripts' directory?
//if (typeof scripts_dir === 'undefined') {
//    scripts_dir = '/xnat/scripts' ;
//}
//else {
//    if (scripts_dir === ''){
//        scripts_dir = '/xnat/scripts' ;
//    }
//}

jQuery.loadScript = function (url, arg1, arg2) {
    var cache = false, callback = null;
    //arg1 and arg2 can be interchangable
    if ($.isFunction(arg1)){
        callback = arg1;
        cache = arg2 || cache;
    } else {
        cache = arg1 || cache;
        callback = arg2 || callback;
    }

    var load = true;
    //check all existing script tags in the page for the url
    jQuery('script[type="text/javascript"]')
        .each(function () {
            return load = (url != $(this).attr('src'));
        });
    if (load){
        //didn't find it in the page, so load it
        jQuery.ajax({
            type: 'GET',
            url: url,
            success: callback,
            dataType: 'script',
            cache: cache
        });
    } else {
        //already loaded so just call the callback
        if (jQuery.isFunction(callback)) {
            callback.call(this);
        }
    }
};

// non-YUI way to make elements draggable
// usage:
// $('#element_id').drags();  // <- drag the element that's clicked on
// $('#element_id').drags({handle:'.drag_handle'});
(function($) {

    $.fn.drags = function(opt) {

        opt = $.extend({handle:"",cursor:"move"}, opt);

        var $el ;

        if(opt.handle === "") {
            $el = this;
        } else {
            $el = this.find(opt.handle);
        }

        return $el.css('cursor', opt.cursor).on("mousedown", function(e) {
            var $drag ;
            if(opt.handle === "") {
                $drag = $(this).addClass('draggable');
            } else {
                $drag = $(this).addClass('active-handle').parent().addClass('draggable');
            }
            var z_idx = $drag.css('z-index'),
                drg_h = $drag.outerHeight(),
                drg_w = $drag.outerWidth(),
                pos_y = $drag.offset().top + drg_h - e.pageY,
                pos_x = $drag.offset().left + drg_w - e.pageX;
            $drag.css('z-index', 1000).parents().on("mousemove", function(e) {
                $('.draggable').offset({
                    top:e.pageY + pos_y - drg_h,
                    left:e.pageX + pos_x - drg_w
                }).on("mouseup", function() {
                        $(this).removeClass('draggable').css('z-index', z_idx);
                    });
            });
            e.preventDefault(); // disable selection
        }).on("mouseup", function() {
            if(opt.handle === "") {
                $(this).removeClass('draggable');
            } else {
                $(this).removeClass('active-handle').parent().removeClass('draggable');
            }
        });

    }
})(jQuery);
//
// make ".draggable" elements draggable
// seems to only work on elements that
// are on the page on DOM ready
// must use the .drags() method on
// dynamically generated elements
//
// sample usage:
//$(function(){
//    $('.draggable').drags();
//});
//
// end draggable


// utility for getting URL query string value
function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return (results == null) ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}


// utility for sorting DOM elements
// usage: sortElements('ul#list','li');
function sortElements(_parent,_child){
    //console.log('sorting...');
    var mylist = $(_parent);
    var listitems = mylist.children(_child).get();
    listitems.sort(function(a, b) {
        var compA = $(a).text().toUpperCase();
        var compB = $(b).text().toUpperCase();
        return (compA < compB) ? -1 : (compA > compB) ? 1 : 0;
    });
    $.each(listitems, function(idx, itm) { mylist.append(itm); });
}


// returns single-digit number as a string with leading zero
function zeroPad (x) {
    var y = parseInt(x,10) ; // make sure it's a number
    return (y < 10) ? '0'+y : ''+y ; // make it a string again
}


// feed an array of numbers to make sure ALL of them are numbers
function allNumbers(_array){
    var is_num = true ;
    if ($.isArray(_array)){
        $.each(_array,function(){
            if (is_num === true){
                is_num = !!($.isNumeric(this));
            }
        });
        return is_num ;
    }
    else {
        return false ;
    }
}


// feed an array of numbers to make check for at least one number
function hasNumber(_array){
    var is_num = false ;
    if ($.isArray(_array)){
        $.each(_array,function(){
            if (is_num === false){
                is_num = !!($.isNumeric(this));
            }
        });
    }
    return is_num ;
}


// feed this function a date (and optionally a format) and
// it'll spit out month number and name (full or abbreviated), day, and year
function SplitDate(_date, _format) {

    var mm_pos, dd_pos, yyyy_pos, example;

    this.val = _date = (typeof _date != 'undefined' && ('' + _date).length) ? _date : '0000-00-00'; // save it to a variable before removing the spaces

    _date = _date.replace(/\s+/g, ''); // removing spaces?
    _date = _date.replace(/_/g, ''); // removing underscores?

    _format = _format || '';
    _format = _format.toLowerCase();

    var months = {
        '01': ['January', 'Jan'], '02': ['February', 'Feb'], '03': ['March', 'Mar'], '04': ['April', 'Apr'], '05': ['May', 'May'], '06': ['June', 'Jun'], '07': ['July', 'Jul'],
        '08': ['August', 'Aug'], '09': ['September', 'Sep'], '10': ['October', 'Oct'], '11': ['November', 'Nov'], '12': ['December', 'Dec'], '13': ['invalid', 'invalid']
    };

    // accepts either dashes, slashes or periods as a delimeter
    // but there must be SOME delimeter
    if (_date.indexOf('-') !== -1) {
        this.arr = _date.split('-');
    }
    else if (_date.indexOf('/') !== -1) {
        this.arr = _date.split('/');
    }
    else if (_date.indexOf('.') !== -1) {
        this.arr = _date.split('.');
    }
    else {
        this.arr = null;
    }

    // we can't do anything if we don't have the date elements saved in an array
    // or if we're passed a bogus value for _date
    if (this.arr !== null && allNumbers(this.arr)) {
        try {
            // accepts either single-digit or double-digit for month or day
            if (this.arr[0].length === 1 || this.arr[0].length === 2) { // it's probably US format, but could MAYBE be Euro format
                var first_num = parseInt(this.arr[0], 10);
                var second_num = parseInt(this.arr[1], 10);
                if (first_num > 12 && first_num < 32 && second_num < 13) _format = 'eu'; // if the first number is higher than 12 but less than 32, it's *probably* Euro format?
                if (_format === 'eu' || _format === 'euro') { // if it's Euro
                    dd_pos = 0;
                    mm_pos = 1;
                    yyyy_pos = 2;
                    example = '31/01/2001';
                    this.format = 'eu';
                }
                else {
                    mm_pos = 0;
                    dd_pos = 1;
                    yyyy_pos = 2;
                    example = '01/31/2001';
                    this.format = 'us';
                }
            }
            else if (this.arr[0].length === 4 || _format === 'iso') { // it's probably ISO format
                yyyy_pos = 0;
                mm_pos = 1;
                dd_pos = 2;
                example = '2001-01-31';
                this.format = 'iso';
            }

            this.m = this.arr[mm_pos];
            this.mm = zeroPad(this.m);
            if (this.m === '' || parseInt(this.m, 10) < 1 || parseInt(this.m, 10) > 12) this.mm = '13';
            //if (this.mm+'' !== '00'){
            this.month = months[this.mm + ''][0]; // set the full month name
            this.mo = months[this.mm + ''][1]; // set the month abbreviation
            //}
            this.d = this.arr[dd_pos];
            this.dd = zeroPad(this.d);
            if (this.d === '' || parseInt(this.d, 10) > 31) this.dd = '32';
            this.yyyy = this.year = this.arr[yyyy_pos];
            if (this.yyyy !== '') this.yyyy = parseInt(this.year, 10);
            this.example = example;

            this.ISO = this.iso = this.yyyy + '-' + this.mm + '-' + this.dd;
            this.US = this.us = this.mm + '/' + this.dd + '/' + this.yyyy;
            this.EU = this.eu = this.EURO = this.euro = this.dd + '/' + this.mm + '/' + this.yyyy;

            this.date_string = this.yyyy + this.mm + this.dd;
            this.date_num = parseInt(this.date_string, 10);
            this.ms = Date.parse(this.iso);
        }
        catch (e) {
            if (console.log) console.log('Error: ' + e);
            this.val = _date;
            this.format = _format;
        }
    }
    else {
        this.val = _date;
        this.format = _format;
    }
}

/*
 // examples of using the SplitDate function
 var split_date = new SplitDate(XNAT.data.todaysDate.iso);
 if (console.log) console.log('The date is ' + split_date.mo + ' ' + split_date.dd + ', ' + split_date.yyyy + '.');
 //
 var split_date2 = new SplitDate(XNAT.data.todaysDate.us);
 if (console.log) console.log('The date is ' + split_date2.mo + ' ' + split_date2.dd + ', ' + split_date2.yyyy + '.');
 //
 var split_date3 = new SplitDate('22-11-2011','euro');
 if (console.log) console.log('The date is ' + split_date3.mo + ' ' + split_date3.dd + ', ' + split_date3.yyyy + '.');
 //
 var split_date4 = new SplitDate('2001.11.11');
 if (console.log) console.log('The date is ' + split_date4.mo + ' ' + split_date4.dd + ', ' + split_date4.yyyy + '.');
 //
 var split_date5 = new SplitDate('9999-44-55');
 if (console.log) console.log('The date is ' + split_date5.mo + ' ' + split_date5.dd + ', ' + split_date5.yyyy + '.');
 //
 */
