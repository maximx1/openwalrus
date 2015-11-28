var currentHandler = 1;

var profileImageUpdateHandler = function(data) { //Handler 0
	var route = jsRoutes.controllers.ApplicationAPI.updateProfileImage();
		
	$.ajax({
		url: route.url,
        type: "POST",
        contentType: "application/json",
        dataType: "json",
        data: JSON.stringify({ id: data.result.content }),
        success: function(results) {
        	$('#mainProfileImage').attr('src', jsRoutes.controllers.Application.lookUpImageThumb(results.content).url);
        	$('.playsHideAndSeek').hide('slow');
        }
    });
};

var gruntImageUpdateHandler = function(data) { //Handler 1
	var mBox = $('div[name=grunt]');
	var originalVal = mBox.html();
	var newImageUrl = jsRoutes.controllers.Application.lookUpImage(data.result.content).url;
	var markdown = "![" + data.result.content + "](" + newImageUrl + ")";
	mBox.html(originalVal + markdown);
	$('.playsHideAndSeek').hide('slow');
};

var bannerImageUpdateHandler = function(data) { //Handler 3
	var route = jsRoutes.controllers.ApplicationAPI.updateBannerImage();
		
	$.ajax({
		url: route.url,
        type: "POST",
        contentType: "application/json",
        dataType: "json",
        data: JSON.stringify({ id: data.result.content }),
        success: function(results) {
        	$('#bannerImage').attr('style', 'background-image: url("' + jsRoutes.controllers.Application.lookUpImage(results.content).url + '");');
        	$('.playsHideAndSeek').hide('slow');
        }
    });
};

var setFileOverlayHandler = function(id) {
	if(id === "mainProfileImage") {
		currentHandler = 0;
	}
	else if(id === "gruntAddImgButton") {
		currentHandler = 1;
	}
	else if(id === "bannerImage") {
		currentHandler = 2;
	}
}

var imageHandler = function(handlerToUseId) {
	if(handlerToUseId === 0) {
		return profileImageUpdateHandler;
	}
	else if(handlerToUseId === 1) {
		return gruntImageUpdateHandler;
	}
	else if(handlerToUseId === 2) {
		return bannerImageUpdateHandler;
	}
	throw "No handler found";
};

var imageHook = function (e, data) {
	if(data.result.status === "success") {
		imageHandler(currentHandler)(data);
	}
	else {
		console.log("error" + data.result.content);
	}
};

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
		$('#gruntsCount').text(Number($('#gruntsCount').text()) + 1)
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