

$(document).ready(function() {
	
	makeTheBalloonMatchTheCheckedButton();
});


function checkAccessibilityRadioButton(buttonDiv, buttonId) {
	
	$("#" + buttonId).prop('checked', true);

	$("#balloon").html(buttonDiv.title);
}


function makeTheBalloonMatchTheCheckedButton() {
	
	var checkedButton = $("input[name='accessibility']:checked").val();
	
	$("#" + checkedButton + "_access_div").click();
}
