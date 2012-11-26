// Copyright (c) 2012 Radiologics, Inc
// Author: Timothy R. Olsen <tim@radiologics.com>
//
// general js included in footer of page to execute javascript after page load.
// initially, this focused on injecting new form submit handling like required fields, null fields, etc


if(XNAT==undefined)XNAT=new Object();
if(XNAT.validators==undefined)XNAT.validators=new Object();
if(XNAT.app.validatorImpls==undefined)XNAT.app.validatorImpls=new Object();
 
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
                    _addValidation(myforms[iFc][fFc],new TextboxValidator(myforms[iFc][fFc],XNAT.app.validatorImpls.RequiredTextBox));
                }else if(myforms[iFc][fFc].nodeName=="SELECT"){
                    _addValidation(myforms[iFc][fFc],new SelectValidator(myforms[iFc][fFc],XNAT.app.validatorImpls.RequiredSelect));
                }
            }

            if(YAHOO.util.Dom.hasClass(myforms[iFc][fFc],'float')){
                if(myforms[iFc][fFc].nodeName=="INPUT"){
                    _addValidation(myforms[iFc][fFc],new TextboxValidator(myforms[iFc][fFc],XNAT.app.validatorImpls.FloatTextBox));
                }
            }
        }
    }

});

//this delays the call to add validatoin until after the dom is loaded
function addValidator(_element,_validator){
    YAHOO.util.Event.onDOMReady(function(){
        _addValidation(_element,_validator);
    });
}

//this method should only be called once the dom is loaded
function _addValidation(_element,_validator){
    var element=_element;
    if(_element.nodeName==undefined){
        _element=document.getElementById(_element);
    }

    var _form=_element.form;
    if(_form.ID==undefined){
        _form.ID="form" + forms++;
    }

    if(XNAT.validators[_form.ID]==undefined){
        XNAT.validators[_form.ID]=new Object();
        XNAT.validators[_form.ID].keys=new Array();
    }

    if(XNAT.validators[_form.ID][_element.id]==undefined){
        XNAT.validators[_form.ID][_element.id]=new Array();
        XNAT.validators[_form.ID][_element.id].box=_element;
        XNAT.validators[_form.ID].keys.push(_element.id);
    }
    
    XNAT.validators[_form.ID][_element.id].push(_validator);
}

function validateBox(box,_checkFunction){
    if(!_checkFunction.isValid(box))
    {
        appendImage(box,"/images/checkmarkRed.gif");
        return false;
    }else{
        removeAppendImage(box);
        return true;
    }
}

XNAT.app.validatorImpls.RequiredTextBox={
	message:"Required field.",
	isValid:function(_box){
		return (_box.value!="");
	}
}

XNAT.app.validatorImpls.FloatTextBox={
	message:"Value must be a floating point decimal.",
	isValid:function(_box){
		if(_box.value!=""){
			var temp=_box.value.trim().replace(/\s+/g,"");
			if(temp!=_box.value){
				return false;
			}
			if(!isNaN(parseFloat(temp)) && isFinite(temp)){
				return true;
			}else{
				return false;
			}
		}else{
			return true;
		}
	}
}

//declaring the constructor
function TextboxValidator(box,_validator) {
	this._validator=_validator;
	if(this._validator.message){
		this.message=this._validator.message;
	}
    this.box = box;
}
// declaring instance methods
TextboxValidator.prototype = {
    validate: function () {
        if(this.box.monitored == undefined){
            this.box.monitored=true;
            YAHOO.util.Event.on(this.box,"change",function(env,var2){
                return validateBox(this.box,this._validator);
            },this,true);
        }


        if(!validateBox(this.box,this._validator)){
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

XNAT.app.validatorImpls.RequiredSelect={
	message:"Required field.",
	isValid:function(sel){
		return (sel.options[sel.selectedIndex].value!="");
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

function RadioButtonValidator(name,box){
	this.name=name;
	
	this.box=box;
}

RadioButtonValidator.prototype = {  validate: function () {
	var passedBoxes=document.getElementsByName(this.name);
    var valid=false;
    for(var stoppedBoxI=0; stoppedBoxI<passedBoxes.length;stoppedBoxI++){
  	  var stoppedBox=passedBoxes[stoppedBoxI];
  	  if(stoppedBox.checked){
  		valid=true;
  	  }
    }
    
    if(!valid){
    	stoppedBox.focus();
        return false;
    }else{
        return true;
    }
}}

YAHOO.util.Event.onDOMReady( function()
{
    var myforms = document.getElementsByTagName("form");
    for (var i=0; i<myforms.length; i++) {
        var myForm = myforms[i];
        if(!myForm.ID) {
            myForm.ID = "form" + forms++;
        }

        //function to hide forms while they are being submitted, unless they have a noHide class
        if(!YUIDOM.hasClass(myForm,'noHide')){
            YAHOO.util.Event.on(myForm,"submit",function(env,var2){
            	if(!YUIDOM.hasClass(this,'noHide')){//check again in case class was added after build
            		concealContent("Submitting... Please wait.");
            	}
            });
        }

        //function to add validation to any form elements with specific classes (required, etc)
        //an array of validator functions is stored in XNAT.validators.  They are tied to the form by the form's ID.
        //this function iterates over those validators and tracks the overall validation outcome in a variable called _ok which is attached the the array of validators.
        //the _ok may be checked by other functions
        YAHOO.util.Event.on(myForm, "submit", function (env, var2) {
            var validators = XNAT.validators[this.ID];
            if(validators!=undefined){
            	try{
	                validators._ok = true;
	                for(var elementIdI=0;elementIdI<validators.keys.length;elementIdI++){
	                	var elementId=validators.keys[elementIdI];
	                	if(validators[elementId] instanceof Array){
		                	try{
		                		validators[elementId]._ok=true;
		                		this.message=undefined;
		                		for (var iVc = 0; iVc < validators[elementId].length; iVc++) {
		                			var tempValidator=validators[elementId][iVc];
		    	                    if (!tempValidator.validate()) {
		    	                        validators[elementId]._ok = false;
		    	                        validators._ok = false;
		    	                        this.focus = validators[elementId][iVc].box;
		    	                        this.message=tempValidator.message;
		    	                    }
		    	                }
		                		
		                		if(validators[elementId]._ok){
		                	        removeAppendImage(validators[elementId].box);
		                		}else{
		                	        appendImage(validators[elementId].box,"/images/checkmarkRed.gif",this.message);
		                		}
		                	}catch(e){
		                		alert("Error performing validation")
		                		validators._ok=false;
		                	}
	                	}
	                }
            	}catch(e){
            		alert("Error performing validation")
            		validators._ok=false;
            	}

                if (!validators._ok) {
                    YAHOO.util.Event.stopEvent(env);
                    showContent();
                    if(this.focus!=undefined)
                        this.focus.focus();
                }
            }
        });

        //take the statically defined onsubmit action and add it as a yui event instead
        if (!myForm.userDefinedSubmit) {
            myForm.userDefinedSubmit = myForm.onsubmit;
        }
        myForm.onsubmit = null;
        YAHOO.util.Event.on(myForm,"submit",function(env,var2){
            var result = (this.userDefinedSubmit) ? this.userDefinedSubmit() : undefined;
            if (result == undefined) {
                result = true;
            }
            if(!result){
                YAHOO.util.Event.stopEvent(env);
                showContent();
            }
        },null,myForm);

        //function to replace empty strings with NULL in form elements with a nullable class
        YAHOO.util.Event.on(myForm,"submit",function(env,var2){
            if (this.ID) {
                if(XNAT.validators[this.ID]==undefined || XNAT.validators[this.ID]._ok){
                    for(var iFc=0;iFc<this.length;iFc++){
                        if(YUIDOM.hasClass(this[iFc],'nullable')){
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


jq(window).load(function(){

    // trying to make the text readable
    jq('[style*="font-size:8px"]').addClass('smallest_text');
    jq('[style*="font-size: 8px"]').addClass('smallest_text');
    jq('[style*="font-size:9px"]').addClass('smallest_text');
    jq('[style*="font-size: 9px"]').addClass('smallest_text');
    jq('[style*="font-size:10px"]').addClass('smaller_text');
    jq('[style*="font-size: 10px"]').addClass('smaller_text');
    jq('[style*="font-size:11px"]').addClass('small_text');
    jq('[style*="font-size: 11px"]').addClass('small_text');

    // ridding <font> tags of their meaning
    jq('font').attr('face','')/*.attr('size','')*/.css('font-family','Arial, Helvetica, sans-serif');

    // this is not necessary now that z-index issues are resolved
    /*
    window.timeLeft_dialog = setInterval(function(){
        if((jq('#session_timeout_dialog_mask').length > 0) && (jq('#session_timeout_dialog_c').length > 0)){ //check if selected options are loaded
            jq('body').append(jq('#session_timeout_dialog_mask, #session_timeout_dialog_c'));
            //jq('#timeout_dialog_wrapper').append(jq('#session_timeout_dialog_c'));
            clearInterval(window.timeLeft_dialog);
        }
    },100);
    */

    jq('#actionsMenu ul ul').addClass('shadowed');

});







