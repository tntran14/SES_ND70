$(function(){
	if(vIsEdit){
		initInputNumber('#f-prd-crud .text-number');
		inputFilterCode($('#f-prd-crud').find('#code'))
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
												disabledAllControlsInForm('f-prd-crud');
												
												if($('#f-prd').find('#grid').length > 0){
													_gridMain.data("kendoGrid").dataSource.read();
												}
												$('#f-prd-crud').closest("div.modal").modal("hide");
												$('#f-prd-crud').closest("div.modal").find('.modal-content').empty();
												
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
	
	dataPost['_id'] = $('#f-prd-crud').find('input[name="_id"]').val();
	dataPost['code'] = $('#f-prd-crud').find('#code').val();
	dataPost['name'] = $('#f-prd-crud').find('#name').val();
	dataPost['stock'] = $('#f-prd-crud').find('#stock').val();
	dataPost['unit'] = $('#f-prd-crud').find('#unit').val();	
	dataPost['price'] = $('#f-prd-crud').find('#price').val();
	dataPost['vat-rate'] = $('#f-prd-crud').find('#vat-rate').val();
	dataPost['description'] = $('#f-prd-crud').find('#description').val();
	dataPost['thdoi_tonkho'] = $('#f-prd-crud').find('input[name="thdoi_tonkho"]').prop('checked')? 'Y': 'N';	
	dataPost['remark'] = $('#f-prd-crud').find('#remark').val();
	
	dataPost['tkvt'] = $('#f-prd-crud').find('#tkvt').val();
	dataPost['tkgv'] = $('#f-prd-crud').find('#tkgv').val();	
	dataPost['tkdt'] = $('#f-prd-crud').find('#tkdt').val();
	dataPost['loaivt'] = $('#f-prd-crud').find('#loaivt').val();
	dataPost['nh_vt1'] = $('#f-prd-crud').find('#nh_vt1').val();
	dataPost['nh_vt2'] = $('#f-prd-crud').find('#nh_vt2').val();
	dataPost['nh_vt3'] = $('#f-prd-crud').find('#nh_vt3').val();
	dataPost['sua_tk_tonkho'] = $('#f-prd-crud').find('input[name="sua_tk_tonkho"]').prop('checked')? 'Y': 'N';
	dataPost['cach_tinh_gia_ton'] = '1';
	dataPost['tk_cl_vt'] = $('#f-prd-crud').find('#tk_cl_vt').val();
	dataPost['tk_dtnb'] = $('#f-prd-crud').find('#tk_dtnb').val();
	
	return dataPost;
}