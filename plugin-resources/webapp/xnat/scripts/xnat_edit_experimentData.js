// Copyright (c) 2007 Washington University School of Medicine
// Author: Tim Olsen <timo@npg.wustl.edu>
var submitHistory=false;

var matchedExpts = new Array();
var exptChecked=false;
var mainDisplayDIV=null;
var verifyExptIdreq=null;

dynamicJSLoad("SAXDriver","xmlsax-min.js");
dynamicJSLoad("SAXEventHandler","SAXEventHandler-min.js");
dynamicJSLoad("xdat_stored_search","generated/xdat_stored_search.js");
dynamicJSLoad("xdat_search_field","generated/xdat_search_field.js");
dynamicJSLoad("xdat_criteria_set","generated/xdat_criteria_set.js");
dynamicJSLoad("xdat_criteria","generated/xdat_criteria.js");

function verifyExptId(expt_id,server){
	matchedExpts = new Array();
	exptChecked=false;
   var verifyExptURL = "remote-class=org.nrg.xdat.ajax.XMLSearch";
   verifyExptURL = verifyExptURL + "&remote-method=execute";
   verifyExptURL = verifyExptURL + "&search="+escape(expt_id);
   verifyExptURL = verifyExptURL + "&XNAT_CSRF="+csrfToken;
   if (window.XMLHttpRequest) {
       var req = new XMLHttpRequest();
   } else if (window.ActiveXObject) {
       var req = new ActiveXObject("Microsoft.XMLHTTP");
   }
   req.open("POST", server, false);
   req.setRequestHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
  
   req.send(verifyExptURL);
   
    if (req.readyState == 4) {
        if (req.status == 200) {
            // handle response 
            var xmlText = req.responseText;
            if (xmlText.startsWith("<html>")){
            	alert("A horrible exception has occurred. \r\nserver:" + server + "\r\n url:"+ verifyExptURL);
            	return;
            }
            exptChecked=true;
            
            var arr,src='',parser = new SAXDriver();
			var handler = new SAXEventHandler();
			
			parser.setDocumentHandler(handler);
			parser.setErrorHandler(handler);
			parser.setLexicalHandler(handler);
			
			parser.parse(xmlText);// start parsing                        
						
			if (handler.items.length>0){
				matchedExpts=handler.items;
			}else{
				if (handler.root){
					matchedExpts[0]=handler.root;
				}
			}
            submitParentForm();
       }else{
       	 alert("A horrible exception has occurred. \r\nserver:" + server + "\r\n url:"+ verifyExptURL);
       }
    }   
}

function fixSpaces(val)
{
        var temp = stringTrim(val);
        var newVal = '';
        temp = temp.split(' ');
        for(var c=0; c < temp.length; c++) {
                newVal += '' + temp[c];
        }
        
        newVal = newVal.replace(/[&]/,"_");
        newVal = newVal.replace(/[?]/,"_");
        newVal = newVal.replace(/[<]/,"_");
        newVal = newVal.replace(/[>]/,"_");
        newVal = newVal.replace(/[(]/,"_");
        newVal = newVal.replace(/[)]/,"_");
        return newVal;
}

function stringTrim(str)
{
   return str.replace(/^\s*|\s*$/g,"");
}

function validateExperimentForm()
{   
   if(submitHistory==true)
   {
      alert("Submit already in progress.  Please wait for process to complete.");
      return false;
   }
   
   if (elementName==null || elementName=="")
   {
     alert("ERROR: Unknown 'elementName'");
     return false;
   }
   
   if (serverRoot==null)
   {
     alert("ERROR: Unknown 'serverRoot'");
     return false;
   }
      
    var rootProject = document.getElementById(elementName+"/project");
    var rootProjectName = null;
    if (rootProject.options){
      rootProjectName= rootProject.options[rootProject.selectedIndex].value;
    }else{
      rootProjectName=rootProject.value;
    }
    
    if (rootProjectName==null){
      document.getElementById(elementName+"/project").focus();
      alert("Please select a " + XNAT.app.displayNames.singular.project.toLowerCase() + ".");
      return false;
    }
        
   
      var xss = new xdat_stored_search();
	 xss.setRootElementName("xnat:experimentData");
  	 var critset = new xdat_criteria_set();
	 critset.setMethod("OR");
	 	 
	 //ID
   if(document.getElementById(elementName+"/ID").value!=""){
  	 var crit= new xdat_criteria();
	 crit.setSchemaField("xnat:experimentData/ID");
	 crit.setComparisonType("=");
	 crit.setValue(document.getElementById(elementName+"/ID").value);	
	 critset.addCriteria(crit);
   }
   
	 //label
   if(document.getElementById(elementName+"/label").value!=""){
	   var subset = new xdat_criteria_set();
		 subset.setMethod("AND");
	   
  	 var crit= new xdat_criteria();
	 crit.setSchemaField("xnat:experimentData/label");
	 crit.setComparisonType("=");
	 crit.setValue(document.getElementById(elementName+"/label").value);	
	 subset.addCriteria(crit);
	 
	 crit= new xdat_criteria();
	 crit.setSchemaField("xnat:experimentData/project");
	 crit.setComparisonType("=");
	 crit.setValue(rootProjectName);	
	 subset.addCriteria(crit);
	 
	 critset.addChildSet(subset);
   }
	 
	 if(critset.Criteria.length==0){
		 if(confirm("The experiment ID is blank.  Is this correct?")){
	            submitParentForm();
	            return;
		 }else{
		 	return false;
		 }
	 }
	 
	 //ID
//   if(document.getElementById(elementName+"/label").value!=""){
//   	 var subset = new xdat_criteria_set();
//   	 subset.setMethod("AND");
   	 
//	 var crit= new xdat_criteria();
//	 crit.setSchemaField("xnat:experimentData/project");
//	 crit.setComparisonType("=");
//	 crit.setValue(rootProjectName);	
//	 subset.addCriteria(crit);
	 
//  	 crit= new xdat_criteria();
//	 crit.setSchemaField("xnat:experimentData/label");
//	 crit.setComparisonType("=");
//	 crit.setValue(document.getElementById(elementName+"/label").value);	
//	 subset.addCriteria(crit);
	 
//	 critset.addChildSet(subset);
//   }
	 
 
	 xss.addSearchWhere(critset);
	
	 var search_xml = xss.toXML("");
         
   return verifyExptId(search_xml,serverRoot+"/servlet/AjaxServlet");

}

function submitParentForm(){
   if (matchedExpts.length>1)
   {
      var matchAlert = "The specified data label is in use by multiple stored experiments.  Please use a unique label for this item.";
      matchAlert+="";
      alert(matchAlert);
      submitHistory=false;
      return false;
   }else if(matchedExpts.length>0){
      var matchedExpt=matchedExpts[0];
      if (matchedExpt.xsiType!=elementName)
      {
        alert("ERROR:  This ID is already in use for a different experiment.  Please use a different ID.");
        submitHistory=false;
        return false;
      }else{
       var primaryProject = matchedExpt.getProperty("sharing/share[project=" +matchedExpt.getProperty("project") + "]/label");
       if (primaryProject == undefined || primaryProject==null || primaryProject=="")
       {
         primaryProject=matchedExpt.getProperty("ID");
       }
       
       if(confirm("WARNING: " + primaryProject + " already exists. Storing this entry may result in modifications to that entry. Do you want to proceed?"))
       {
         document.getElementById(elementName+"/ID").value=matchedExpt.getProperty("ID");
         submitHistory=true;
         //submit
         document.getElementById("form1").submit();
       }else{
         submitHistory=false;
         return false;
       }
      }
   }else{
         //alert("NO MATCHES FOUND");
         submitHistory=true;
         //submit
         document.getElementById("form1").submit();
   }
}