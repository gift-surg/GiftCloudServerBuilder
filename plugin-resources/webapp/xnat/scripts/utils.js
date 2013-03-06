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
    x = nStr.split('.');
    x1 = x[0];
    x2 = x.length > 1 ? '.' + x[1] : '';
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