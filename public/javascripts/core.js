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

$(document).ready(function() {
	$.ajax({
		url: jsRoutes.controllers.ApplicationAPI.fileUploadMenuPartial().url,
		type: "GET",
		success: function(data) {
			$('#uploadPlaceholder').append(data)
			$('.playsHideAndSeek').hide();
			$('#closeFileOverlay').click(function() {
				$('.playsHideAndSeek').hide('slow');
			});

			$('#fileOverlayButton').click(function() {
				$('.playsHideAndSeek').show('slow');
			});
			
			$(function () {
			    $('#fileupload').fileupload({
			        dataType: 'json',
			        progressall: function (e, data) {
			            var progress = parseInt(data.loaded / data.total * 100, 10);
			            $('#progress .bar').css(
			                'width',
			                progress + '%'
			            );
			        },
			        done: function (e, data) {
			        	if(data.result.status === "success") {
			        		$('input[name=profileImage').val(data.result.content)
			        		$('.playsHideAndSeek').hide('slow');
			        	}
			        	else {
			        		console.log("error" + data.result.content);
			        	}
			        }
			    });
			});
		}
	});
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

//$('.mainProfileImage').click()