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

			$('.opensFileOverlay').click(function() {
				setFileOverlayHandler($(this).id)
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
			        done: imageHook
			    });
			});
		}
	});
});