//DO NOT EDIT THIS FILE, IT MAY BE OVER-WRITTEN.

var exptId=null;
var exptCreateDate=null;
var exptCreateUser=null;
var exptElement=null;
var exptChecked=false;
var mainDisplayDIV=null;
var verifyExptIdreq=null;

function verifyExptId(expt_id,server){
	exptId=null;
	exptCreateDate=null;
	exptCreateUser=null;
	exptElement=null;
	exptChecked=false;
   var url = server + "?remote-class=org.nrg.xnat.ajax.CheckExptId";
   url = url + "&remote-method=execute";
   url = url + "&id="+expt_id;
   if (window.XMLHttpRequest) {
       verifyExptIdreq = new XMLHttpRequest();
   } else if (window.ActiveXObject) {
       verifyExptIdreq = new ActiveXObject("Microsoft.XMLHTTP");
   }
   verifyExptIdreq.open("GET", url, true);
   verifyExptIdreq.onreadystatechange = verifyExptIdCallback;
  
   verifyExptIdreq.send(null);
}


function verifyExptIdCallback() {
  if (verifyExptIdreq !=null){
    if (verifyExptIdreq.readyState == 4) {
        if (verifyExptIdreq.status == 200) {
            // handle response 
            var xmlDoc = verifyExptIdreq.responseXML;
            exptChecked=true;
            if (xmlDoc)
            {
               var root = xmlDoc.getElementsByTagName("matchingExperiments")[0];
               if (root)
               {
                 if (root.childNodes.length>0)
                 {
                   var expt = root.childNodes[0];
                   var exptAttributes = expt.attributes;
                   exptId = exptAttributes.getNamedItem("id").value;
                   exptCreateDate = exptAttributes.getNamedItem("create_date").value;
                   exptCreateUser = exptAttributes.getNamedItem("create_user").value;
                   exptElement = exptAttributes.getNamedItem("element").value;
                   
                 }
               }
            }
            
            submitParentForm();
       }
    }   
   }
   
 
}

