$(function(){


	_gridSub01.kendoGrid({
		dataSource: {
			data: rowsCert,
			pageSize: 999999,
			serverPaging: false,
			serverSorting: false,
           	serverFiltering: false
		},
		selectable: true, scrollable: true, 
		sortable: false,
		filterable: false, resizable: true,
		serverSorting: false,
		pageable: {
			refresh: false,
			pageSizes: false,
			numeric: false,
			previousNext: false
		},
		dataBinding: function () {
            record = (this.dataSource.page() - 1) * this.dataSource.pageSize();
        },
		columns: [
			{field: 'STT', title: 'STT', width: '50px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">STT</a>',
  				attributes: {'class': 'table-cell', style: 'text-align: right;'}, sortable: false, 
  				headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
  				, template: '#: ++record #',
  			},
  			{field: 'func', title: '', width: '60px', encoded: false, hidden: !vIsEdit
  				, headerTemplate: '&nbsp;'
				, attributes: {'class': 'table-cell', style: 'text-align: center;'}, sortable: false
				, headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
				, template: '<i class="mdi mdi-close-box fs-25 text-danger c-pointer" data-sub-action="remove" ></i>'
			},
  			{field: 'TTChuc', title: '', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Nhà cung cấp</a>',
				attributes: {'class': 'table-cell text-left text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'Seri', title: '', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Seri</a>',
				attributes: {'class': 'table-cell text-center text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'TNgay', title: '', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Từ ngày</a>',
				attributes: {'class': 'table-cell text-center text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'DNgay', title: '', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Đến ngày</a>',
				attributes: {'class': 'table-cell text-center text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'HThuc', title: '', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Hình thức</a>',
				attributes: {'class': 'table-cell text-center text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
				template: '#= window.setTemplateForGrid("HThuc", data) #'
			},
		],
		dataBound: function(e) {
			_gridSub01.find('tbody[role="rowgroup"]').find('tr').undelegate('i[data-sub-action]', 'click');
			_gridSub01.find('tbody[role="rowgroup"]').find('tr').delegate('i[data-sub-action]', 'click', function(e){
				e.preventDefault();/*e.stopPropagation();*/
				
				var $obj = $(this);
				var $tr = $obj.closest('tr');
				var subAction = $obj.attr('data-sub-action');
				
				var indexRow = $tr.index();
				
				switch (subAction) {
				case 'remove':
					alertConfirm('Bạn có chắc chắn muốn xóa không?',
						function(e){
							var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
							if(indexRow < objDataJson.length && indexRow > -1){
								objDataJson.splice(indexRow, 1);
								_gridSub01.data("kendoGrid").dataSource.data(objDataJson);	
							}
								
						},
						function(e){}
					)
					break;

				default:
					break;
				}
				
			});
			
		}
	});
	
	/*LAY THONG TIN CKS*/
	$("#f-tkhai-crud").find('button[data-action]').click(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var dataAction = $(this).data('action');
		
		var $obj = $(this);
		var objDataSend = null;
		
		switch (dataAction) {
		case 'add-cert':
			getCert(function(e){
				if(null == e) {
					alertDLSuccess('Lấy chữ ký số không thành công.', function(){});
					hideLoading();
					return;
				}

				$.ajax({
					type: "POST",
					datatype: "json",
					url: ROOT_PATH + '/main/common/check-cert-full',
					data: {'cert': base64Cert.replace(/\+/g, "@")},
					beforeSend: function(req) {
						initAjaxJsonRequest(req);
			        	showLoading();
					},
					success:function(res) {
						hideLoading();
						if(res) {
							if(res.errorCode == 0) {
								var res = res.responseData;
								var seri = res['Seri'];
								var check = false;
								var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
								for(var i = 0; i < objDataJson.length; i++){
									if(seri == objDataJson[i]['Seri']){
										check = true;
										break;
									}
								}
								
								if(!check){
									objDataJson.push(res);
									_gridSub01.data("kendoGrid").dataSource.data(objDataJson);	
								}
								
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
			break;
		case 'back':
			$('#divMainContent').show();
			$('#divSubContent').hide(function(){$(this).empty();});
			try{
				if($('#f-tkhai').find('#grid').length > 0)
					$('#f-tkhai').find('#grid').data("kendoGrid").dataSource.read();
			}catch(err){}
			break;
		case 'sign':
			objDataSend = {};
			objDataSend['_id'] = $('#f-tkhai-crud').find('input[name="_id"]').val();
			
			$.ajax({
				type: "POST",
				datatype: "json",
				url: ROOT_PATH + '/main/tkhai-sign/check-data-sign',
				data: objDataSend,
				beforeSend: function(req) {
					initAjaxJsonRequest(req);
		        	showLoading();
				},
				success:function(res) {
					if(res) {
						if(res.errorCode == 0) {
							var responseData = res.responseData;
							
							tokenTransaction = responseData['TOKEN'];
							objDataSend['tokenTransaction'] = tokenTransaction;
							
							getCert(function(e){
								if(null == e) {
									alertDLSuccess('Lấy chữ ký số không thành công.', function(){});
									hideLoading();
									return;
								}
								serialNumber = '';
								$.ajax({
									type: "POST",
									datatype: "json",
									url: ROOT_PATH + '/main/common/check-cert',
									data: {'cert': base64Cert.replace(/\+/g, "@")},
									beforeSend: function(req) {
										initAjaxJsonRequest(req);
							        	showLoading();
									},
									success:function(res) {
										if(res) {
											if(res.errorCode == 0) {
												serialNumber = res.responseData;
												//LAY NOI DUNG FILE XML
												jQuery.ajax({
													url: ROOT_PATH + '/main/common/get-file-to-sign/' + tokenTransaction,
											        cache:false,
											        xhr:function(){// Seems like the only way to get access to the xhr object
											            var xhr = new XMLHttpRequest();
											            xhr.responseType= 'blob'
											            return xhr;
											        },
											        success:function(data, textStatus, xhr) {
											        	if(xhr.status == 200){
											        		var blob = new Blob([data], { type: 'octet/stream' });
												    		var postEnc = new FormData();
												    		postEnc.append('SerialNumber', serialNumber);
												    		postEnc.append('xmlFile', blob);
												    		
												    		var xhrSign = new XMLHttpRequest();
												    		xhrSign.onreadystatechange = function(e) {
												    			if(4 == this.readyState) {
												    				if(this.status == 200){
												    					/*NOI DUNG XML DA KY - SEND TO SERVER*/
												    					blob = new Blob( [this.response], { type : "octet/stream" } );
												    					showLoading();
												    					
												    					formData = new FormData();
																		formData.append("XMLFileSigned", blob);
																		formData.append('certificate', base64Cert.replace(/\+/g, "@"));
																		formData.append('_id', $('#f-tkhai-crud').find('input[name="_id"]').val())
												    					
																		xhrSign = new XMLHttpRequest();
																		xhrSign.upload.addEventListener('progress', function(e) {
																			
																		});
																		if(xhrSign.upload) {
																			
																		};
																		xhrSign.onreadystatechange = function(e) {
																			if(4 == this.readyState && this.status == 200) {
																				var res = xhrSign.response;
																				if(res){
																					if(res.errorCode == 0) { 
																						hideLoading();
																						
																						$("#f-tkhai-crud").find('button[data-action="back"]').trigger('click');
																					}else{
																            			alertDLSuccess(createObjectError(res).html(), function(){});
																            			hideLoading();
																            		}
																				}else{
																					alertDLSuccess('unknown error!!!', function(){});
																					hideLoading();
																            	}
																			}
																		}
																		xhrSign.onerror = function() {
																			
																		}
																		xhrSign.ontimeout = function() {
																			
																		}
																		
																		var urlPost = ROOT_PATH + '/main/tkhai-sign/signFile';
																		xhrSign.open("POST", urlPost, true);
																		xhrSign.responseType = 'json';
																		xhrSign.setRequestHeader('X-CSRF-TOKEN', _csrf_value)
																		xhrSign.setRequestHeader(HEADER_RESULT_TYPE_NAME, HEADER_RESULT_TYPE_JSON);
																		
																		xhrSign.setRequestHeader("Cache-Control", "no-cache");
																		xhrSign.setRequestHeader("X-Requested-With", "XMLHttpRequest");
																		xhrSign.send(formData);
												    				}else{
																		hideLoading();
																		alertDLSuccess('<span class="text-danger">Lỗi trong quá trình ký tập tin.</span>', function(){});
																	}
												    			}
												    		}
												    		xhrSign.open('POST', urlPluginSign + signDLTK, true);
															xhrSign.timeout = 5 * 60 * 1000;
															xhrSign.responseType = 'blob';	//or arraybuffer
															xhrSign.send(postEnc);
											        	}else{
												    		hideLoading();
												    		alertDLSuccess("Lỗi: Lấy dữ liệu tờ khai không thành công.", function(){});
														}
											        },
											        error:function(){
											            
											        }
												});
											}else{
												alertDLSuccess(createObjectError(res).html(), function(){});
												hideLoading();
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
							
						}else{
							hideLoading();
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
												$("#f-tkhai-crud").find('button[data-action="back"]').trigger('click');
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
	
	$('#f-tkhai-crud #tinh-thanh').change(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var _val = $(this).val();
		
		$('#f-tkhai-crud').find('#CQTQLy').empty();
		$('#f-tkhai-crud').find('#CQTQLy').append($("<option></option>").text('').val(''));
		
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
						$('#f-tkhai-crud').find('#CQTQLy').append(
							$("<option></option>").text(item['name']).val(item['code'])
						);
					});
				}
			},
			error:function (xhr, ajaxOptions, thrownError){
	        }
		});
	});
	
});

function setTemplateForGrid(key, data){
	if(!vIsEdit)
		return data[key] == null? '': data[key];
		
	var text = '';
	text = '<div class="form-row m-l-1 m-r-1">';
	switch (key) {
	case 'HThuc':
		text += '<select class="input-grid form-control form-control-sm input-grid-feature"  name="' + key + '" style="height: 100%;" >';
		text += '<option value="1">Thêm mới</option>';
		text += '<option value="2">Gia hạn</option>';
		text += '<option value="3">Ngừng sử dụng</option>';
		text += '</select>';
		break;

	default:
		break;
	}
	text += '</div>';
	return text;
}

function getDataToSave(){
	var dataPost = {};
	
	dataPost['_id'] = $('#f-tkhai-crud').find('input[name="_id"]').val();
	dataPost['ten-nnt'] = $('#f-tkhai-crud').find('#ten-nnt').val();
	dataPost['mau-so'] = $('#f-tkhai-crud').find('#mau-so').val();
	dataPost['ten'] = $('#f-tkhai-crud').find('#ten').val();	
	dataPost['mst'] = $('#f-tkhai-crud').find('#mst').val();
	dataPost['tinh-thanh'] = $('#f-tkhai-crud').find('#tinh-thanh').val();
	dataPost['CQTQLy'] = $('#f-tkhai-crud').find('#CQTQLy').val();
	dataPost['NLHe'] = $('#f-tkhai-crud').find('#NLHe').val();
	dataPost['DCLHe'] = $('#f-tkhai-crud').find('#DCLHe').val();
	dataPost['DCTDTu'] = $('#f-tkhai-crud').find('#DCTDTu').val();
	dataPost['DTLHe'] = $('#f-tkhai-crud').find('#DTLHe').val();
	dataPost['NLap'] = $('#f-tkhai-crud').find('#NLap').val();
	dataPost['HThuc'] = $('#f-tkhai-crud').find('#HThuc').val();
	
	/*HINH THUC HOA DON*/
	dataPost['HTHDon'] = $('#f-tkhai-crud').find('input[name="optHTHDon"]:checked').val();
	/*PHƯƠNG THỨC CHUYỂN DỮ LIỆU HÓA ĐƠN ĐIỆN TỬ*/
	dataPost['PThuc'] = $('#f-tkhai-crud').find('input[name="optPThuc"]:checked').val();
	/*LOẠI HÓA ĐƠN SỬ DỤNG*/
	dataPost['LHDSDung_HDGTGT'] = $('#f-tkhai-crud').find('input[name="LHDSDung_HDGTGT"]:checked').val();
	dataPost['LHDSDung_HDBHang'] = $('#f-tkhai-crud').find('input[name="LHDSDung_HDBHang"]:checked').val();
	dataPost['LHDSDung_HDBTSCong'] = $('#f-tkhai-crud').find('input[name="LHDSDung_HDBTSCong"]:checked').val();
	dataPost['LHDSDung_HDBHDTQGia'] = $('#f-tkhai-crud').find('input[name="LHDSDung_HDBHDTQGia"]:checked').val();
	dataPost['LHDSDung_HDKhac'] = $('#f-tkhai-crud').find('input[name="LHDSDung_HDKhac"]:checked').val();
	dataPost['LHDSDung_CTu'] = $('#f-tkhai-crud').find('input[name="LHDSDung_CTu"]:checked').val();
	
	var arrRows = [];
	var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
	jQuery.each(objDataJson, function(index, item) {
		item['HThuc'] = $('#f-tkhai-crud').find('#grid_cert').find('tbody[role="rowgroup"]').find('tr:eq(' + index + ')').find('select[name="HThuc"]').val();
		arrRows.push(item);
	});
	dataPost['DSCTSSDung'] = encodeObjJsonBase64UTF8(arrRows);
	
	return dataPost;
}