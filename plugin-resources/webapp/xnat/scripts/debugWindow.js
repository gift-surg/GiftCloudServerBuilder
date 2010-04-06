
function writeConsole(content) {
 if(top.consoleRef){
 	
 }else{
 	top.consoleRef=window.open('','myconsole',
  'width=350,height=250'
   +',menubar=0'
   +',toolbar=1'
   +',status=0'
   +',scrollbars=1'
   +',resizable=1');
 }
 top.consoleRef.document.writeln(content);
 
}
