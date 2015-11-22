var wipePlaceholderBlur = function() {
    var mBox = $('div[name=grunt]')
    var handle = $('#userProfileHandle').text()
	if(mBox.html() === "<br>" || mBox.html() === handle + " " || mBox.html() === handle) {
		mBox.html("");
	}
}

$('#markedOtherProfile').focus(function() {
    var mBox = $('div[name=grunt]')
    var handle = $('#userProfileHandle').text()
    console.log(handle);
    if(mBox.html() == "") {
        mBox.html(handle + " ");
    }
});

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