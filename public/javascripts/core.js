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
    if(mBox.html() === "") {
        mBox.html(handle + " ");
    }
});

var createGruntSuccessAction = function(data) {
	var route = jsRoutes.controllers.ApplicationAPI.retrieveSingleGrunt();

	if(data.status === "ok") {
		$.ajax({
            url: route.url,
            type: "POST",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({ id: data.content }),
            success: getOneGruntSuccessAction
        });
	}
};

var getOneGruntSuccessAction = function(data) {
	if(data.status === "ok") {
		$('#mainGruntEntry').after(data.content);
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
            success: createGruntSuccessAction
        });
    }
    messageBox.html("");
});