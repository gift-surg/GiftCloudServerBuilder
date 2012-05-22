// Copyright (c) 2012 Radiologics, Inc
// Author: Timothy R. Olsen <tim@radiologics.com>
//
// general js included in footer of page to execute javascript after page load.
// initially, this focused on injecting new form submit handling like required fields, null fields, etc


if(XNAT==undefined)XNAT=new Object();
if(XNAT.forms==undefined)XNAT.forms=new Object();

/********************
 Add support for in page validation specification
 */
var forms=0;

YAHOO.util.Event.onDOMReady(function(){
	var myforms = document.getElementsByTagName("form");
	for(var iFc=0;iFc<myforms.length;iFc++){
		for(var fFc=0;fFc<myforms[iFc].length;fFc++){
			if(YAHOO.util.Dom.hasClass(myforms[iFc][fFc],'required')){
				if(myforms[iFc][fFc].nodeName=="INPUT" || myforms[iFc][fFc].nodeName=="TEXTAREA"){
					_addValidation(myforms[iFc][fFc],new TextboxValidator(myforms[iFc][fFc]));
				}else if(myforms[iFc][fFc].nodeName=="SELECT"){
					_addValidation(myforms[iFc][fFc],new SelectValidator(myforms[iFc][fFc]));
				}
			}
		}
	}
	
});

function addValidator(_element,_validator){
	YAHOO.util.Event.onDOMReady(function(){
		_addValidation(_element,_validator);
	});
}
function _addValidation(_element,_validator){
	var element=_element;
	if(_element.nodeName==undefined){
		_element=document.getElementById(_element);
	}
	
	var _form=_element.form;
	if(_form.ID==undefined){
		_form.ID="form" + forms++;
	}
	
	if(XNAT.forms[_form.ID]==undefined){
		XNAT.forms[_form.ID]=new Array();
	}
	
	XNAT.forms[_form.ID].push(_validator);
}

function validateBox(box){
	if(box.value=="")
	{
		appendImage(box,"/images/checkmarkRed.gif");
		return false;
	}else{
		removeAppendImage(box);
		return true;
	}
}

//declaring the constructor  
function TextboxValidator(box) {  
    this.box = box;  
}  
// declaring instance methods  
TextboxValidator.prototype = {  
    validate: function () {
    	if(this.box.monitored == undefined){
        	this.box.monitored=true;
    		YAHOO.util.Event.on(this.box,"change",function(env,var2){
    			return validateBox(this);
    		});
    	}
    	
		
    	if(!validateBox(this.box)){
  			this.box.focus();
  			return false;
    	}else{
    		return true;
    	}
    }
};  

function validateSelect(sel){
	if(sel.options[sel.selectedIndex].value==""){
		appendImage(sel,"/images/checkmarkRed.gif");
		return false;
	}else{
		removeAppendImage(sel);
		return true;
	}
}


//declaring the constructor  
function SelectValidator(box) {  
  this.box = box;  
}  
//declaring instance methods  
SelectValidator.prototype = {  validate: function () {
	if(this.box.monitored == undefined){
    	this.box.monitored=true;
		YAHOO.util.Event.on(this.box,"change",function(env,var2){
			return validateSelect(this);
		});
	}
	
	
	if(!validateSelect(this.box)){
			this.box.focus();
			return false;
	}else{
		return true;
	}
}
};  

YAHOO.util.Event.onDOMReady( function()
{
	var myforms = document.getElementsByTagName("form");
	for (var i=0; i<myforms.length; i++) {
        var myForm = myforms[i];
        if(!myForm.ID) {
            myForm.ID = "form" + forms++;
        }

        //function to hide forms while they are being submitted, unless they have a noHide class
		if(!YAHOO.util.Dom.hasClass(myForm,'noHide')){
			YAHOO.util.Event.on(myForm,"submit",function(env,var2){
    			concealContent("Submitting... Please wait.");
    		});
		}
		
		//function to add validation to any form elements with specific classes (required, etc)
        YAHOO.util.Event.on(myForm, "submit", function (env, var2) {
            var validators = XNAT.forms[this.ID];
            XNAT.forms[this.ID]._ok = true;
            for (var iVc = 0; iVc < validators.length; iVc++) {
                if (!validators[iVc].validate()) {
                    XNAT.forms[this.ID]._ok = false;
                    this.focus = validators[iVc].box;
                }
            }

            if (!XNAT.forms[this.ID]._ok) {
                YAHOO.util.Event.stopEvent(env);
                showContent();
                this.focus.focus();
            }
        });

        //take the statically defined onsubmit action and add it as a yui event instead
		var subFunction = myForm.onsubmit;
		myForm.onsubmit = null;
		YAHOO.util.Event.on(myForm,"submit",function(env,var2){
            var result = subFunction();
            if (result == undefined) {
                result = true;
            }
            if(!result){
				YAHOO.util.Event.stopEvent(env);
				showContent();
			}
		});
		
		//function to replace empty strings with NULL in form elements with a nullable class
		YAHOO.util.Event.on(myForm,"submit",function(env,var2){
            if (this.ID) {
			if(XNAT.forms[this.ID]._ok){
				for(var iFc=0;iFc<this.length;iFc++){
					if(YAHOO.util.Dom.hasClass(this[iFc],'nullable')){
						if((this[iFc].nodeName=="INPUT" || this[iFc].nodeName=="TEXTAREA") && this[iFc].value==""){
							this[iFc].value="NULL";
						}
					}
				}
			}
            }
		});
	}
});


XNAT.app.toggle=function (_name){
	var elements = document.getElementsByName(_name);
	for(var trI=0;trI<elements.length;trI++){
		if(elements[trI].style.display=="none"){
			elements[trI].style.display="block";
		}else{
			elements[trI].style.display="none";
		}
	}	
}