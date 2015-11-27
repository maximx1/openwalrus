var setFileOverlayHandler = function(id) {}

var imageHook = function (e, data) {
	if(data.result.status === "success") {
		$('input[name=profileImage').val(data.result.content);
		$('.playsHideAndSeek').hide('slow');
	}
	else {
		console.log("error" + data.result.content);
	}
};
