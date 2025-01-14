$(function(){
	$('div.modal-footer').find('button[data-action="send-mail"]').click(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var $obj = $(this);
		var objDataSend = getPopupDataToSave();
		$.ajax({
			type: "POST",
			datatype: "json",
			url: ROOT_PATH + '/main/einvoice-send-mail/check-data-send',
			data: objDataSend,
			beforeSend: function(req) {
				initAjaxJsonRequest(req);
	        	showLoading();
			},
			success:function(res) {
				hideLoading();
				if(res) {
					if(res.errorCode == 0) {
						var responseData = res.responseData;
						
						var confirmText = responseData['CONFIRM'];
						tokenTransaction = responseData['TOKEN'];
						
						objDataSend['tokenTransaction'] = tokenTransaction;
						
						alertConfirm(confirmText,
							function(e){
								$.ajax({
									type: "POST",
									datatype: "json",
									url: ROOT_PATH + '/main/einvoice-send-mail/send-mail',
									data: objDataSend,
									beforeSend: function(req) {
										initAjaxJsonRequest(req);
							        	showLoading();
									},
									success:function(res) {
										hideLoading();
										if(res) {
											if(res.errorCode == 0) {
												$('#f-einvoice-send-mail').closest("div.modal").modal("hide");
												$('#f-einvoice-send-mail').closest("div.modal").find('.modal-content').empty();
											}else{
												alertDLSuccess(createObjectError(res).html(), function(){});
											}
										}else{
											alertDLSuccess('unknown error!!!', function(){});
											hideLoading();
										}
									},
									error:function (xhr, ajaxOptions, thrownError){
										alertDLSuccess(xhr.status + " - " + xhr.responseText, function(){});
							            hideLoading();
							        }
								});
							},
							function(e){}
						);
					}else{
						alertDLSuccess(createObjectError(res).html(), function(){});
					}
				}else{
					alertDLSuccess('unknown error!!!', function(){});
					hideLoading();
				}
			},
			error:function (xhr, ajaxOptions, thrownError){
				alertDLSuccess(xhr.status + " - " + xhr.responseText, function(){});
	            hideLoading();
	        }
		});
	});
	
	
});
function getPopupDataToSave(){
	var dataPost = {};
	
	dataPost['_id'] = $('#f-einvoice-send-mail').find('input[name="_id"]').val();
	dataPost['_title'] = $('#f-einvoice-send-mail').find('#title').val();
	dataPost['_email'] = $('#f-einvoice-send-mail').find('#email-receive').val();
	dataPost['_content'] = encodeURIComponent(tinymce.get('emailContent').getContent());
	
	return dataPost;
}