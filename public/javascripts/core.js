var wipePlaceholderBlur = function() {
	if($('div[name=grunt]').html() === "<br>") {
		$('div[name=grunt]').html("");
	}
}

$('#gruntSubmitButton').click(function() {
    var route = jsRoutes.controllers.ApplicationAPI.postGrunt();
    var messageBox = $('div[name=grunt]');

    if(messageBox.html() !== "") {
        $.ajax({
            url: route.url,
            type: "POST",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({ message: messageBox.html() }),
            success: function(data) {
                console.log(data.content);
                messageBox.html("");
            }
        });
    }
});