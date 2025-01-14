	$(function(){
		if(vIsEdit){
			initInputNumber('#f-quantity-crud .text-number');
		}
		
		
		/*LAY THONG TIN CKS*/
		$("#f-quantity-crud").find('button[data-action]').click(function (event) {
			event.preventDefault();/*event.stopPropagation();*/
			var dataAction = $(this).data('action');
			
			var $obj = $(this);
			var objDataSend = null;
			
			switch (dataAction) {	
			case 'back':
				$('#divMainContent').show();
				$('#divSubContent').hide(function(){$(this).empty();});
				try{
					if($('#f-quantity').length > 0)
						$('#f-quantity').data("kendoGrid").dataSource.read();
				}catch(err){}
				break;
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
													$("#f-quantity-crud").find('button[data-action="back"]').trigger('click');
														location.reload();
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
//		dataPost['THDon'] = $('#f-quantity-crud').find('input[name="THDon"]').val();
//		dataPost['MSo'] = $('#f-quantity-crud').find('input[name="MSo"]').val();
//		dataPost['Quantity'] = $('#f-quantity-crud').find('input[name="Quantity"]').val();
//		dataPost['TSo'] = $('#f-quantity-crud').find('input[name="TSo"]').val();
//		dataPost['DSo'] = $('#f-quantity-crud').find('input[name="DSo"]').val();
		dataPost['_id'] = $('#f-quantity-crud').find('input[name="_id"]').val();
		dataPost['mausohdon'] = $('#f-quantity-crud').find('#mausohdon').val();	
		dataPost['quantity'] = $('#f-quantity-crud').find('#quantity').val();
		return dataPost;
	}
