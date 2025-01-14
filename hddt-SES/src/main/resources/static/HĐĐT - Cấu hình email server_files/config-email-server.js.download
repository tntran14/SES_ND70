$(function(){
	inputFilterInteger($('#f-config-email-server').find('#smtp-port'))
	
	$('#f-config-email-server').find('button[data-action]').click(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var dataAction = $(this).data('action');
		
		var $obj = $(this);
		var objDataSend = null;
		
		switch (dataAction) {
		case 'accept':
			objDataSend = getDataToSave();
			$.ajax({
				type: "POST",
				datatype: "json",
				url: ROOT_PATH + '/main/' + transactionMain + '/check-data-save',
				data: objDataSend,
				beforeSend: function(req) {
					initAjaxJsonRequest(req);
		        	showLoading();
				},
				success:function(res) {
					hideLoading();
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
									url: ROOT_PATH + '/main/' + transactionMain + '/save-data',
									data: objDataSend,
									beforeSend: function(req) {
										initAjaxJsonRequest(req);
							        	showLoading();
									},
									success:function(res) {
										hideLoading();
										if(res) {
											if(res.errorCode == 0) {
												$('#f-einvoice-crud').find('button[data-action="back"]').trigger('click');
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
				},
				error:function (xhr, ajaxOptions, thrownError){
					alertDLSuccess(xhr.status + " - " + xhr.responseText, function(){});
		            hideLoading();
		        }
			});
			break;

		default:
			break;
		}
	});
	
});

function getDataToSave(){
	var dataPost = {};
	var _tmp = '';
	
	dataPost['_id'] = $('#f-config-email-server').find('input[name="_id"]').val();
	
	_tmp = $('#f-config-email-server').find('input[type="checkbox"][name="check-auto-send"]').prop('checked')? 'Y': 'N';
	dataPost['check-auto-send'] = _tmp;
	_tmp = $('#f-config-email-server').find('input[type="checkbox"][name="check-ssl"]').prop('checked')? 'Y': 'N';
	dataPost['check-ssl'] = _tmp;
	_tmp = $('#f-config-email-server').find('input[type="checkbox"][name="check-tls"]').prop('checked')? 'Y': 'N';
	dataPost['check-tls'] = _tmp;
	dataPost['smtp-server'] = $('#f-config-email-server').find('#smtp-server').val();
	dataPost['smtp-port'] = $('#f-config-email-server').find('#smtp-port').val();
	dataPost['email-address'] = $('#f-config-email-server').find('#email-address').val();
	dataPost['email-password'] = $('#f-config-email-server').find('#email-password').val();
	
	return dataPost;
}