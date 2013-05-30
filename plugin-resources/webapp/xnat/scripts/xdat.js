var supported = (document.getElementById || document.all);

if (supported)
{
	document.write("<STYLE TYPE='text/css'>");
	document.write(".para {display: none}");
	document.write("</STYLE>");

	var max = 7;
	var shown = new Array();
	for (var i=1;i<=max;i++)
	{
		shown[i+1] = false;
	}
}

function popupWSize(url,h,w,name)
{
	if (! window.focus)return true;
    if (!name) {
        name="";
    }
	window.open(url, name, "width=" + w + ",height=" + h + ",status=yes,resizable=yes,scrollbars=yes,toolbar=yes");
	return false;
}

function popupWithProperties(mylink, windowname, WinProperties)
{
	if (! window.focus)return true;
	var href;
	if (typeof(mylink) == 'string')
	{
   		href=mylink;
	}else
	{
  	 	href=mylink.href;
	}

   	if (href.indexOf('popup_params/') != -1)
   	{
   		var index = href.indexOf('popup_params/') + 13;
   		WinProperties = href.substring(index);
   		href = href.substring(0,href.indexOf('popup_params/'));
   	}
	window.open(href, '', WinProperties);
	return false;
}


function popup(mylink, windowname)
{
	if (! window.focus)return true;
	var href;
	if (typeof(mylink) == 'string')
   		href=mylink;
	else
   		href=mylink.href;
	window.open(href, windowname, 'width=725,height=800,status=yes,resizable=yes,scrollbars=yes,toolbar=yes');
	return false;
}


function popupViewer(mylink, windowname)
{
	if (! window.focus)return true;
	var href;
	if (typeof(mylink) == 'string')
   		href=mylink;
	else
   		href=mylink.href;
	window.open(href, '', 'width=320,height=420,status=yes,resizable=yes,scrollbars=no');
	return false;
}

function IsNumeric(sText)
{
   var ValidChars = "0123456789.";
   var IsNumber=true;
   var Char;


   for (i = 0; i < sText.length && IsNumber == true; i++)
      {
      Char = sText.charAt(i);
      if (ValidChars.indexOf(Char) == -1)
         {
         IsNumber = false;
         }
      }
   return IsNumber;

}


//FUNCTION USED TO TOGGLE SPANS VISIBILITY
function blocking(i)
{
	if (!supported)
	{
        showMessage("page_body", "Notification", "This link does not work in your browser.");
		return;
	}
	var plusLocation = serverRoot+ "/images/plus.jpg";
	var minusLocation = serverRoot+ "/images/minus.jpg";

	imgShow = (shown[i]) ? minusLocation : plusLocation;

	var imgCode = 'IMG' + i;

	if (document.getElementById)
	{
	 	//COMMON
		var current = document.getElementById('span'+i).style.display;
		if (current == 'block')
		{
			document.getElementById('span'+i).style.display = 'none';
			document.images[imgCode].src= plusLocation;
		}else{
			document.getElementById('span'+i).style.display = 'block';
			document.images[imgCode].src= minusLocation;
		}
	}
	else if (document.all)
	{
		var current = document.all['span'+i].style.display;
		if (current == 'block')
		{
			document.all['span'+i].style.display = 'none';
			document.images[imgCode].src= plusLocation;
		}else{
			document.all['span'+i].style.display = 'block';
			document.images[imgCode].src= minusLocation;
		}
	}

	return false;
}

//REDIRECTION SCRIPTS
//SHOW ITEM REPORT
function rpt(exptId,displayElement,searchField)
{
	if (! window.focus)return true;
	var link = serverRoot+ "/app/action/DisplayItemAction";
	link = link + "/search_value/" + exptId.toString() + "/search_element/" + displayElement.toString() + "/search_field/" + searchField;
	if(window.projectScope){
		link=link +"/project/" + window.projectScope;
	}
	if(window.isPopup){
		link=link + "/popup/true";
		window.location=link;
	}else{
		//window.open(link, '','width=800,height=800,status=yes,resizable=yes,scrollbars=yes,toolbar=yes');
		window.location=link;
	}
	return false;
}

//SHOW ITEM EDIT PAGE
function edit(exptId,displayElement,searchField)
{
	if (! window.focus)return true;
	var link = serverRoot+ "/app/action/EditItemAction/popup/true";
	link = link + "/search_value/" + exptId.toString() + "/search_element/" + displayElement.toString() + "/search_field/" + searchField;
	if(window.projectScope){
		link=link +"/project/" + window.projectScope;
	}
	window.open(link, '','width=600,height=800,status=yes,resizable=yes,scrollbars=yes,toolbar=yes');
	return false;
}

//SHOW ITEM IMAGE VIEWER
function viewer(sessionId)
{
	if (! window.focus)return true;
	var link = serverRoot+ "/app/action/ShowViewerAction/popup/true";
	link = link + "/search_element/xnat:mrSessionData/search_field/xnat:mrSessionData.ID/search_value/" + sessionId.toString();
	window.open(link, '', 'width=320,height=420,status=yes,resizable=yes,scrollbars=no');
	return false;
}

//SHOW ITEM IMAGE VIEWER WITH PRESET DEFUALT EXPERIMENT
function view(sessionId,exptCode) {
	if (exptCode==''){
		viewer(sessionId);
	}
	else {
		var link = serverRoot+ "/app/action/ShowViewerAction/popup/true";
		link = link + "/search_element/xnat:mrSessionData/search_field/xnat:mrSessionData.ID/search_value/" + sessionId.toString() + "/startDisplayWith/" + exptCode.toString();
		window.open(link, '', 'width=320,height=420,status=yes,resizable=yes,scrollbars=no');
	}
	return false;
}

//SHOW ITEM IMAGE VIEWER FOR SUBJECT
function viewPart(part_id) {
	var link = serverRoot+ "/app/action/ShowViewerAction/popup/true";
	link = link + "/skipq/true/id/" + part_id.toString();
	window.open(link, '', 'width=320,height=420,status=yes,resizable=yes,scrollbars=no');
	return false;
}

//SHOW EMAIL SCREEN
function email(toAddress)
{
	if (! window.focus)return true;
	var link = serverRoot+ "/app/template/XDATScreen_email.vm";
	link = link + "/emailTo/" + toAddress;
	window.location = link;
	return false;
}

//SHOW ITEM XML
function displayXML(searchValue,displayElement,searchField)
{
	if (! window.focus)return true;
	var link = serverRoot+ "/app/action/DisplayXMLAction/popup/true";
	link = link + "/search_value/" + searchValue.toString() + "/search_element/" + displayElement.toString() + "/search_field/" + searchField;
	window.open(link, '','width=600,height=800,status=yes,resizable=yes,scrollbars=yes,toolbar=yes');
	return false;
}

/**
 * DHTML date validation script. Courtesy of SmartWebby.com (http://www.smartwebby.com/dhtml/)
 */
// Declaring valid date character, minimum year and maximum year
var dtCh= "/";
var minYear=1900;
var maxYear=2100;

function isInteger(s){
	var i;
    for (i = 0; i < s.length; i++){
        // Check that current character is number.
        var c = s.charAt(i);
        if (((c < "0") || (c > "9"))) return false;
    }
    // All characters are numbers.
    return true;
}

function stripCharsInBag(s, bag){
	var i;
    var returnString = "";
    // Search through string's characters one by one.
    // If character is not in bag, append to returnString.
    for (i = 0; i < s.length; i++){
        var c = s.charAt(i);
        if (bag.indexOf(c) == -1) returnString += c;
    }
    return returnString;
}

function daysInFebruary (year){
	// February has 29 days in any year evenly divisible by four,
    // EXCEPT for centurial years which are not also divisible by 400.
    return (((year % 4 == 0) && ( (!(year % 100 == 0)) || (year % 400 == 0))) ? 29 : 28 );
}
function DaysArray(n) {
	for (var i = 1; i <= n; i++) {
		this[i] = 31
		if (i==4 || i==6 || i==9 || i==11) {this[i] = 30}
		if (i==2) {this[i] = 29}
   }
   return this
}

function concealContent(message){
	if(!message) {
		message = "Submitting... Please wait.";
	}
    var layout2 = document.getElementById("layout_content2");
    if (layout2) {
        //layout2.className="warning";
        layout2.className="message"; // "message" class presents more 'friendly' display of status
        layout2.innerHTML=message;
        layout2.style.display="block";
    }
    var layout1 = document.getElementById("layout_content");
    if (layout1) {
        layout1.style.display="none";
    }
}

function showContent(){
    var layout2 = document.getElementById("layout_content2");
    if (layout2) {
    document.getElementById("layout_content2").style.display="none";
    }
    var layout1 = document.getElementById("layout_content");
    if (layout1) {
    document.getElementById("layout_content").style.display="block";
}
}

function ArrayIndexOf(array,item){
	var index =-1;
	for(var i=0;i<array.length;i++){
		if (array[i]==item){
			index=i;
			break;
		}
	}
	return index;
}

function ArrayIndexById(array,id){
	var index =-1;
	for(var i=0;i<array.length;i++){
		if (array[i].getId()==item){
			index=i;
			break;
		}
	}
	return index;
}

function ArrayIndexByName(array,name){
	var index =-1;
	for(var i=0;i<array.length;i++){
		if (array[i].getName()==item){
			index=i;
			break;
		}
	}
	return index;
}

function stringTrim(str)
{
   return str.replace(/^\s*|\s*$/g,"");
}
function stringCamelCaps(val)
{
        var temp = val.replace(/^\s*|\s*$/g,"");
        temp = temp.replace(/[&]/," ");
        temp = temp.replace(/[?]/," ");
        temp = temp.replace(/[<]/," ");
        temp = temp.replace(/[>]/," ");
        temp = temp.replace(/[(]/," ");
        temp = temp.replace(/[)]/," ");
        var newVal = '';
        temp = temp.split(' ');
        for(var c=0; c < temp.length; c++) {
              if (c==0)
                newVal += temp[c].substring(0,1) +
temp[c].substring(1,temp[c].length);
              else
                newVal += temp[c].substring(0,1).toUpperCase() +
temp[c].substring(1,temp[c].length);
        }

        return newVal;
}
String.prototype.trim = function () {
	return this.replace(/^\s*|\s*$/g,"");
}


String.prototype.startsWith = function(str) {
	if (this.indexOf(str)==0){
		return true;
	}else{
		return false;
	}
}

String.prototype.endsWith = function(str) {
	if (this.lastIndexOf(str)==(this.length-str.length)){
		return true;
	}else{
		return false;
	}
}

String.prototype.getExcerpt = function(desiredLength, excerptIndicator) {
	if(this.length <= desiredLength) {
		return this;
	}
	else {
		return this.substr(0, desiredLength - 1).concat(excerptIndicator || "...");
	}
}

Array.prototype.contains = function(str) {
	for(var containsCount=0;containsCount<this.length;containsCount++){
		if (this[containsCount]==str){
			return true;
		}
	}

	return false;
}


function emptyChildNodes(element) {
	while(element.childNodes.length>0){
		element.removeChild(element.childNodes[0]);
	}
}

function log(msg){
	if (DEBUG){
		var logBox = document.getElementById("DEBUG_OUT");
		if (logBox==undefined){
			logBox = document.createElement("DIV");
			logBox.Id="DEBUG_OUT";
			logBox.style.position="absolute";
			logBox.style.right="0px";
			logBox.style.bottom="0px";
			logBox.style.color="FFFFFF";
			document.childNodes.push(logBox);
		}
		logBox.innerHTML=logBox.innerHTML + "<BR>" +msg;
	}
}

function isValidDate(dateStr, format) {
   if (format == null) { format = "MDY"; }
   format = format.toUpperCase();
   if (format.length != 3) { format = "MDY"; }
   if ( (format.indexOf("M") == -1) || (format.indexOf("D") == -1) || (format.indexOf("Y") == -1) ) { format = "MDY"; }
   if (format.substring(0, 1) == "Y") { // If the year is first
      var reg1 = /^\d{2}(\-|\/|\.)\d{1,2}\1\d{1,2}$/;
      var reg2 = /^\d{4}(\-|\/|\.)\d{1,2}\1\d{1,2}$/;
   } else if (format.substring(1, 2) == "Y") { // If the year is second
      var reg1 = /^\d{1,2}(\-|\/|\.)\d{2}\1\d{1,2}$/;
      var reg2 = /^\d{1,2}(\-|\/|\.)\d{4}\1\d{1,2}$/;
   } else { // The year must be third
      var reg1 = /^\d{1,2}(\-|\/|\.)\d{1,2}\1\d{2}$/;
      var reg2 = /^\d{1,2}(\-|\/|\.)\d{1,2}\1\d{4}$/;
   }
   // If it doesn't conform to the right format (with either a 2 digit year or 4 digit year), fail
   if ( (reg1.test(dateStr) == false) && (reg2.test(dateStr) == false) ) { return false; }
   var parts = dateStr.split(RegExp.$1); // Split into 3 parts based on what the divider was
   // Check to see if the 3 parts end up making a valid date
   if (format.substring(0, 1) == "M") { var mm = parts[0]; } else if (format.substring(1, 2) == "M") { var mm = parts[1]; } else { var mm = parts[2]; }
   if (format.substring(0, 1) == "D") { var dd = parts[0]; } else if (format.substring(1, 2) == "D") { var dd = parts[1]; } else { var dd = parts[2]; }
   if (format.substring(0, 1) == "Y") { var yy = parts[0]; } else if (format.substring(1, 2) == "Y") { var yy = parts[1]; } else { var yy = parts[2]; }
   if (parseFloat(yy) <= 50) { yy = (parseFloat(yy) + 2000).toString(); }
   if (parseFloat(yy) <= 99) { yy = (parseFloat(yy) + 1900).toString(); }
   var dt = new Date(parseFloat(yy), parseFloat(mm)-1, parseFloat(dd), 0, 0, 0, 0);
   if (parseFloat(dd) != dt.getDate()) { return false; }
   if (parseFloat(mm)-1 != dt.getMonth()) { return false; }
   return true;
}


function validateDate(sel){
   var childNodes = sel.parentNode.getElementsByTagName("SELECT");
   var img_div = sel.parentNode.getElementsByTagName("DIV")[0];
   if(img_div!=undefined)img_div.innerHTML="";
   var month =0;
   var day = 0;
   var year=0;
   for(var childNodeCount=0;childNodeCount<childNodes.length;childNodeCount++)
   {
      if (childNodes[childNodeCount].name.indexOf(".month")>0){
       if(childNodes[childNodeCount].selectedIndex==0)
       {
        return;
       }else{
        month= childNodes[childNodeCount].options[childNodes[childNodeCount].selectedIndex].value;
		monthInt = parseInt(month) + 1;
		month = monthInt;
	   }
      }else if (childNodes[childNodeCount].name.indexOf(".date")>0){
       if(childNodes[childNodeCount].selectedIndex==0)
       {
        return;
       }else{
        day= childNodes[childNodeCount].options[childNodes[childNodeCount].selectedIndex].value;
       }
      }else if (childNodes[childNodeCount].name.indexOf(".year")>0){
       if(childNodes[childNodeCount].selectedIndex==0)
       {
        return;
       }else{
        year= childNodes[childNodeCount].options[childNodes[childNodeCount].selectedIndex].value;
       }
      }
   }

   if(!isValidDate(month+"/" + day + "/" + year))
   {
     for(var childNodeCount=0;childNodeCount<childNodes.length;childNodeCount++)
     {
       childNodes[childNodeCount].selectedIndex=0;
       if (childNodes[childNodeCount].name.indexOf(".date")>0){
        childNodes[childNodeCount].focus();
       }
     }
       showMessage("page_body", "Notification", "Please select a valid date.");
   }else{
     if(img_div!=undefined)img_div.innerHTML="<img src=\"" + serverRoot+ "/images/checkmarkGreen.gif\"/>";
   }

  }

  /**********************************************
   * Inserts a YUI Calendar to handle date validation & selection
   */
  function insertCalendar(input,_title){
  	//CREATE BUTTON
  	var button = document.createElement("input");
  	button.type="button";
  	button.value=">";
  	button.id="cal_"+input.id;

  	//CREATE CALENDAR CONTAINER
	var calendarContainer = document.createElement("DIV");
	calendarContainer.className="yui-skin-sam";
	calendarContainer.style.display="inline";

	//INSERT INTO DOM
	if(input.nextSibling==undefined){
		input.parentNode.appendChild(calendarContainer);
	}else{
		input.parentNode.insertBefore(calendarContainer,input.nextSibling);
	}

	calendarContainer.appendChild(button);

	var calendarDIV= document.createElement("DIV");
	calendarContainer.appendChild(calendarDIV);
	calendarDIV.style.position="absolute";

      try {
          var cal1 = new YAHOO.widget.Calendar("cal1", calendarDIV, {context:["cal_" + input.id, "tr", "tl"], title:_title, navigator:true, close:true, visible:false});
      } catch (e) {
          showMessage("page_body", "Notification", "Found exception creating calendar: " + e);
      }

    cal1.text_input = input;
    input.calendar=cal1;

    button.calendar=cal1;
    button.onclick=function(){
    	this.calendar.show();
    	return false;
    }

    cal1.hider=button;

    cal1.handleSelect = function(type,args,obj) {
        var dates = args[0];
		var date = dates[0];
		var year = date[0], month = date[1].toString(), day = date[2].toString();

		if(month !=null && month!=undefined && month.length==1){
			month="0"+month;
		}

		if(day !=null && day!=undefined && day.length==1){
			day="0"+day;
		}
		
		this.text_input.value = month + "/" + day + "/" + year;
		this.text_input.calendar.hide();
		try{this.text_input.onblur();}catch(e){};
    }


   input.onchange=function(){
   	   if(this.value!=""){
   	   	this.value=this.value.replace(/[-]/,"/");
   	   	this.value=this.value.replace(/[.]/,"/");
   	   	if(isValidDate(this.value)){
   	   	 this.calendar.select(this.value);
         var selectedDates = this.calendar.getSelectedDates();
         if (selectedDates.length > 0) {
            var firstDate = selectedDates[0];
            this.calendar.cfg.setProperty("pagedate", (firstDate.getMonth()+1) + "/" + firstDate.getFullYear());
            this.calendar.render();
         } else {
            showMessage("page_body", "Notification", "Invalid date. MM/DD/YYYY");
         }
   	    }else if(this.value == "NULL"){
   	    	//don't do anything, they're trying to clear out the value.
   	   	}else{
                  showMessage("page_body", "Notification", "Invalid date. MM/DD/YYYY");
   	   		this.value="";
   	   		this.focus();
   	   	}
   	   }
	}

 	cal1.selectEvent.subscribe(cal1.handleSelect, cal1, true);

    cal1.render();
    cal1.hide();

    if(input.value!=""){
   	   	 input.onchange();
	}
  }


  function parseForm(formId){
  	var oForm,oElement, oName, oValue, oDisabled,
            data='', item = 0,
            i,len,j,jlen,opt;
  	//TAKEN FROM yui/build/connection/connection.js
  		if(typeof formId == 'string'){
			// Determine if the argument is a form id or a form name.
			// Note form name usage is deprecated by supported
			// here for legacy reasons.
			oForm = (document.getElementById(formId) || document.forms[formId]);
		}
		else if(typeof formId == 'object'){
			// Treat argument as an HTML form object.
			oForm = formId;
		}
		else{
			return;
		}

  		// Iterate over the form elements collection to construct the
		// label-value pairs.
		for (i=0,len=oForm.elements.length; i<len; ++i){
			oElement  = oForm.elements[i];
			oDisabled = oElement.disabled;
            oName     = oElement.name;

			// Do not submit fields that are disabled or
			// do not have a name attribute value.
			if(!oDisabled && oName)
			{
                oName  = encodeURIComponent(oName)+'=';
                oValue = encodeURIComponent(oElement.value);

				switch(oElement.type)
				{
                    // Safari, Opera, FF all default opt.value from .text if
                    // value attribute not specified in markup
					case 'select-one':
                        if (oElement.selectedIndex > -1) {
                            opt = oElement.options[oElement.selectedIndex];
                            if(item++ >0)
                              data+='&';
                            data += oName + encodeURIComponent(
                                (opt.attributes.value && opt.attributes.value.specified) ? opt.value : opt.text);
                        }
                        break;
					case 'select-multiple':
                        if (oElement.selectedIndex > -1) {
                            for(j=oElement.selectedIndex, jlen=oElement.options.length; j<jlen; ++j){
                                opt = oElement.options[j];
                                if (opt.selected) {
		                            if(item++ >0)
		                              data+='&';
                                    data += oName + encodeURIComponent(
                                        (opt.attributes.value && opt.attributes.value.specified) ? opt.value : opt.text);
                                }
                            }
                        }
						break;
					case 'radio':
					case 'checkbox':
						if(oElement.checked){
                            if(item++ >0)
                              data+='&';
                            data += oName + oValue;
						}
						break;
					case 'file':
						// stub case as XMLHttpRequest will only send the file path as a string.
					case undefined:
						// stub case for fieldset element which returns undefined.
					case 'reset':
						// stub case for input type reset button.
					case 'button':
						// stub case for input type button elements.
						break;
					case 'submit':
						break;
					default:
                        if(item++ >0)
                          data+='&';
                        data += oName + oValue;
				}
			}
		}

		return data.toString();
  }


function appendImage(obj,img_name,msg){
	if(obj!=null){
	  if(typeof obj == 'string'){
			// Determine if the argument is a form id or a form name.
			// Note form name usage is deprecated by supported
			// here for legacy reasons.
			obj = (document.getElementById(obj));
	  }

	  if(obj.appendedImage==undefined){
	    obj.appendedImage = document.createElement("img");
	    obj.appendedImage.style.marginLeft="5pt";
	    if(obj.nextSibling==null)
	    {
	    	obj.parentNode.insertBefore(obj.appendedImage,obj.nextSibling);
	    }else{
	    	obj.parentNode.appendChild(obj.appendedImage);
	    }
	  }
	  
	  if(msg!=undefined)obj.appendedImage.title=msg;
	  
	  obj.appendedImage.src=serverRoot + img_name;
	}
}

function removeAppendImage(obj){
if(obj!=null){
	  if(typeof obj == 'string'){
			// Determine if the argument is a form id or a form name.
			// Note form name usage is deprecated by supported
			// here for legacy reasons.
			obj = (document.getElementById(obj));
	  }

	  if(obj.appendedImage!=undefined){
	  	obj.appendedImage.parentNode.removeChild(obj.appendedImage);
	    obj.appendedImage=null;
	  }
	}
}

window.modals=new Object();

function openModalPanel(id,msg,parentPanel,options){
	  if(id==undefined)id="wait";
	  if(window.modals[id]==undefined){
		  if(options==undefined){
			  options=new Object();
		  }
		  //set defaults
		  if(options.width==undefined){
				  options.width="240px";
		  }
		  if(options.fixedcenter==undefined){
			  options.fixedcenter=true;
		  }
		  if(options.close==undefined){
			  options.close=true;
		  }
		  if(options.draggable==undefined){
			  options.draggable=false;
		  }
		  if(options.zindex==undefined){
			  options.zindex=4;
		  }
		  if(options.modal==undefined){
			  options.modal=true;
		  }
		  if(options.visible==undefined){
			  options.visible=false
          }
		  window.modals[id]=new YAHOO.widget.Panel(id,options);
	      if(parentPanel!=undefined){
	   	      parentPanel.hide();
	          window.modals[id].parentPanel=parentPanel;
	      }
	      window.modals[id].setHeader(msg);
	      if(options.body==undefined){
	    	  window.modals[id].setBody('<img src="' + serverRoot + '/images/rel_interstitial_loading.gif" />');
	      }else{
	    	  window.modals[id].setBody(options.body);
	      }
	      window.modals[id].render(document.body);
	  }

	  window.modals[id].show();
}

function closeModalPanel(id){
	if(id==undefined)id="wait";
	if(window.modals[id]!=undefined){
		var parentPanel=window.modals[id].parentPanel;
		window.modals[id].destroy();
		window.modals[id]=null;
		if(parentPanel!=undefined){
			parentPanel.show();
		}
	}
}

function displayError(msg){
    showMessage("page_body", "Exception", msg);
}

var msgC=0;

var messageNumber = 0 ;

function showMessage(divId, title, body, options) {

    var thisMessage = xModal.message ;

    messageNumber++ ;

    thisMessage.id = 'message'+messageNumber ;

    // may not need the 'divId' anymore
    if (divId && (divId > '' && divId !== 'page_body')){
        thisMessage.id = divId ;
    }

    thisMessage.title = title ;
    thisMessage.content = body ;

    if (options){
        if (options.width){
            thisMessage.width = options.width ;
        }
    }

    xModalOpen(thisMessage);

    /*
    if (options == undefined) {
        options = {};
    }
    if (options.width == undefined) {
        options.width = "20em";
    }
    options.close = false;
    options.fixedcenter = true;
    options.constraintoviewport = true;
    options.modal = true;
    options.icon = YAHOO.widget.SimpleDialog.ICON_WARN;
    options.visible = true;
    options.draggable = false;
    options.buttons = [
        { text: 'OK', isDefault: true, handler: function () {
            this.hide();
        } }
    ];
    var dialog = new YAHOO.widget.SimpleDialog("dialog" + msgC++, options);

    dialog.render(document.getElementById(divId));
    dialog.setHeader(title);
    dialog.setBody(body);
    dialog.bringToTop();
    dialog.show();
    return dialog;
    */
}



function toggle_ul(n){
	var element = document.getElementById("ul_"+n);
	if (element) { var a = document.getElementById("a_"+n); }
	if(YAHOO.util.Dom.hasClass(element,"hidden")){
		// show hidden items and toggle icon to "hide"
		YAHOO.util.Dom.removeClass(element,"hidden");
		a.style.backgroundPosition="right top";
	}else{
		// hide elements and toggle icon to "expand"
		YAHOO.util.Dom.addClass(element,"hidden");
		a.style.backgroundPosition="left top";
	}
}

function getValueById(id){
	var box=document.getElementById(id);
	if(box==undefined){
		return {"value":""};
	}
	if(box.value==undefined){
		if(box.selectedIndex!=undefined){
			return {"value":box.options[box.selectedIndex].value,obj:box};
		}
	}else{
		return {"value":box.value,obj:box};
	}
}


//this function can be used to execute a function within the scope of a different object (i.e. the 'this' in the fuction will be the second argument passed in).
//not sure if there is an easier way to do this...
XNAT.app.runInScope=function(_function,scope){
	var runScope=new YAHOO.util.CustomEvent("complete",this);
	runScope.subscribe(_function, this, scope);
	runScope.fire();
}
function addSearchMenuOption(menuMap) {
    if (!window.menuOptions) {
        window.menuOptions = [];
    }
    window.menuOptions.push(menuMap);
}

function getSearchMenuOptions() {
    return window.menuOptions;
}

function clearSearchMenuOptions() {
    if (window.menuOptions) {
        window.menuOptions.length = 0;
    }
}


XNAT.utils=new Object();

//find the specified object in the specified array using the specified comparator
XNAT.utils.find=function(array,tofind, comparator){
	for(var findI=0;findI<array.length;findI++){
		if(comparator(array[findI],tofind)){
			return array[findI];
		}
	}
	
	return null;
}

//filter the specified array for objects that match the comparator
XNAT.utils.filter=function(array, comparator){
	var new_array=new Array();
	for(var filterI=0;filterI<array.length;filterI++){
		if(comparator(array[filterI])){
			new_array.push(array[filterI]);
		}
	}
	return new_array;
}
