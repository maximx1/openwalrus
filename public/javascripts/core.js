$(document).ready(function() {
	$.ajax({
		url: jsRoutes.controllers.ApplicationAPI.fileUploadMenuPartial().url,
		type: "GET",
		success: function(data) {
			$('#uploadPlaceholder').append(data)
			$('.playsHideAndSeek').hide();
			
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

$('body').on('click', '.closeFileOverlay', function() {
	$('.playsHideAndSeek').hide('slow');
});

$('body').on('click', '.opensFileOverlay', function() {
	setFileOverlayHandler($(this).attr("id"))
	$('.playsHideAndSeek').show('slow');
});

$('.timestamp').each(function() {
	var timestamp = moment.unix(Number($(this).text()) / 1000);
	$(this).text(timestamp.format("H:mm M/D/YYYY"));
});