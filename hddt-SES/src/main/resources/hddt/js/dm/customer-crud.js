$(function(){
	if(vIsEdit){
		if($('#f-cus-crud').find('#province').length > 0)
			initComboSearchLocalWithParent('#f-cus-crud', '#province', '#modelPopupMedium');
	}

	$('div.modal-footer').find('button[data-action="accept"]').click(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var $obj = $(this);
		var objDataSend = getPopupDataToSave();
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
												$obj.prop('disabled', true);
												disabledAllControlsInForm('f-cus-crud');
												
												if($('#f-customer').find('#grid').length > 0){
													try{
														_gridMain.data("kendoGrid").dataSource.read();	
													}catch(err){}
												}
												$('#f-cus-crud').closest("div.modal").modal("hide");
												$('#f-cus-crud').closest("div.modal").find('.modal-content').empty();
												
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
	
	dataPost['_id'] = $('#f-cus-crud').find('input[name="_id"]').val();
	dataPost['tax-code'] = $('#f-cus-crud').find('#tax-code').val();
	dataPost['customer-code'] = $('#f-cus-crud').find('#customer-code').val();
	dataPost['roleId'] = $('#f-cus-crud').find('#roleId').val();
	dataPost['company-name'] = $('#f-cus-crud').find('#company-name').val();
	dataPost['customer-name'] = $('#f-cus-crud').find('#customer-name').val();
	dataPost['address'] = $('#f-cus-crud').find('#address').val();
	dataPost['email'] = $('#f-cus-crud').find('#email').val();
	dataPost['emailcc'] = $('#f-cus-crud').find('#emailcc').val();
	dataPost['phone'] = $('#f-cus-crud').find('#phone').val();
	dataPost['fax'] = $('#f-cus-crud').find('#fax').val();
	dataPost['website'] = $('#f-cus-crud').find('#website').val();
	dataPost['province'] = $('#f-cus-crud').find('#province').val();
	dataPost['province-name'] = $('#f-cus-crud').find('#province').find('option:selected').text();
	dataPost['LHDSDung_HDBTSCong'] = $('#f-cus-crud').find('input[name="LHDSDung_HDBTSCong"]:checked').val();
	dataPost['account-number'] = $('#f-cus-crud').find('#account-number').val();
	dataPost['account-bank-name'] = $('#f-cus-crud').find('#account-bank-name').val();
	dataPost['customer-group-1'] = $('#f-cus-crud').find('#customer-group-1').val();
	dataPost['customer-group-1-name'] = $('#f-cus-crud').find('#customer-group-1').find('option:selected').text();
	
	dataPost['customer-group-2'] = $('#f-cus-crud').find('#customer-group-2').val();
	dataPost['customer-group-2-name'] = $('#f-cus-crud').find('#customer-group-2').find('option:selected').text();
	
	dataPost['customer-group-3'] = $('#f-cus-crud').find('#customer-group-3').val();
	dataPost['customer-group-3-name'] = $('#f-cus-crud').find('#customer-group-3').find('option:selected').text();
	
	dataPost['remark'] = $('#f-cus-crud').find('#remark').val();
	
	return dataPost;
}