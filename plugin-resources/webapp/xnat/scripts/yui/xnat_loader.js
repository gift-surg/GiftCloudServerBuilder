function XNATLoadingGIF(div_id){
	if(div_id.id==undefined){
		this.div=document.getElementById(div_id);
	}else{
		this.div=div_id;
	}
	this.div.style.display="none";
	
	this.msg=this.div.innerHTML;
	
	this.img = document.createElement("img");
	this.img.src=serverRoot+"/scripts/yui/build/assets/skins/xnat/wait.gif";
	this.div.appendChild(this.img);
	
	this.render=function(){
		this.div.style.display="block";
	};
	
	this.show=function(){
		this.div.style.display="block";
	};
	
	this.hide=function(){
		this.div.style.display="none";
	};
	
	this.close=function(){
		if (this.div.parentNode!=undefined)this.div.parentNode.removeChild(this.div);
	};
}