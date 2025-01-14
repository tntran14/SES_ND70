		$(function(){
			if(vIsEdit){
				
				if($('#f-issu-crud').find('#tinh-thanh').length > 0)
					initComboSearchLocal('#f-issu-crud', '#tinh-thanh');
				if($('#f-issu-crud').find('#CQTQLy').length > 0)
					initComboSearchLocal('#f-issu-crud', '#CQTQLy');
			}
			$('#f-issu-crud').find('button[data-action]').click(function (event) {
				event.preventDefault();/*event.stopPropagation();*/
				var dataAction = $(this).data('action');
				
				var $obj = $(this);
				var objDataSend = null;
				
				switch (dataAction) {
				case 'add-to-grid':
					var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
					objDataJson.push({});
					_gridSub01.data("kendoGrid").dataSource.data(objDataJson);
					break;
				case 'back':
					$('#divMainContent').show();
					$('#divSubContent').hide(function(){$(this).empty();});
					try{
						if($('#f-issu').find('#grid').length > 0)
							$('#f-issu').find('#grid').data("kendoGrid").dataSource.read();
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
														$('#f-issu-crud').find('button[data-action="back"]').trigger('click');
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
							$obj.prop('disabled', false);
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
			dataPost['t'] = $('#f-issu-crud').find('#t').val();	
			dataPost['n'] = $('#f-issu-crud').find('#n').val();
			dataPost['a'] = $('#f-issu-crud').find('#a').val();
			dataPost['p'] = $('#f-issu-crud').find('#p').val();
			dataPost['f'] = $('#f-issu-crud').find('#f').val();	
			dataPost['e'] = $('#f-issu-crud').find('#e').val();
			dataPost['w'] = $('#f-issu-crud').find('#w').val();
			dataPost['ac'] = $('#f-issu-crud').find('#ac').val();
			dataPost['an'] = $('#f-issu-crud').find('#an').val();	
			dataPost['bn'] = $('#f-issu-crud').find('#bn').val();
			dataPost['tinh-thanh'] = $('#f-issu-crud').find('#tinh-thanh').val();
			dataPost['CQTQLy'] = $('#f-issu-crud').find('#CQTQLy').val();
			dataPost['boss'] = $('#f-issu-crud').find('#boss').val();	
			dataPost['cv'] = $('#f-issu-crud').find('#cv').val();
			dataPost['ng'] = $('#f-issu-crud').find('#ng').val();
			dataPost['eng'] = $('#f-issu-crud').find('#eng').val();
			dataPost['png'] = $('#f-issu-crud').find('#png').val();	
			dataPost['englh'] = $('#f-issu-crud').find('#englh').val();
			dataPost['acti'] = $('#f-issu-crud').find('#acti').val();
			
			
			return dataPost;
		}
					
																

		$('#f-issu-crud #tinh-thanh').change(function (event) {
			event.preventDefault();/*event.stopPropagation();*/
			var _val = $(this).val();
			
			$('#f-issu-crud').find('#CQTQLy').empty();
			$('#f-issu-crud').find('#CQTQLy').append($("<option></option>").text('').val(''));
			
			if('' == _val || _val == undefined) return;
			$.ajax({
				type: "POST",
				datatype: "json",
				url: ROOT_PATH + '/common/get-chi-cuc-thue',
				data: {"tinhthanh_ma": _val},
				beforeSend: function(req) {
					initAjaxJsonArrayRequest(req);
				},
				success:function(res) {
					if(res && $.isArray(res)) {
						$.each(res, function(index, item) {
							$('#f-issu-crud').find('#CQTQLy').append(
								$("<option></option>").text(item['name']).val(item['code'])
							);
						});
					}
				},
				error:function (xhr, ajaxOptions, thrownError){
		        }
			});
		});
