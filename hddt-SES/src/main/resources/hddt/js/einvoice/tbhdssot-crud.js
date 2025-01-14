$(function(){
	if($('#f-tbhdssot-crud').find('#NTBCCQT').length > 0)
		dateInputFormat($('#f-tbhdssot-crud').find('#NTBCCQT'));
	if(vIsEdit){
		if($('#f-tbhdssot-crud').find('#tinh-thanh').length > 0)
			initComboSearchLocal('#f-tbhdssot-crud', '#tinh-thanh');
		if($('#f-tbhdssot-crud').find('#CQTQLy').length > 0)
			initComboSearchLocal('#f-tbhdssot-crud', '#CQTQLy');
	}
	
	_gridSub01.kendoGrid({
		dataSource: {
			data: rowsTMP,
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
			{field: 'MSHDon', title: '', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mẫu số HĐ</a>',
				attributes: {'class': 'table-cell text-center text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'SHDon', title: '', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Số HĐ</a>',
				attributes: {'class': 'table-cell text-center text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'Ngay', title: '', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Ngày tạo</a>',
				attributes: {'class': 'table-cell text-center text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'MCQTCap', title: '', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã CQT</a>',
				attributes: {'class': 'table-cell text-center text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'LADHDDT', title: '', width: '180px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Loại áp dụng<br>HĐĐT</a>',
				attributes: {'class': 'table-cell text-left text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
				template: '#= window.setTemplateForGrid("LADHDDT", data) #'
			},
			{field: 'TCTBao', title: '', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tính chất<br>thông báo</a>',
				attributes: {'class': 'table-cell text-left text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
				template: '#= window.setTemplateForGrid("TCTBao", data) #'
			},
			{field: 'LDo', title: '', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Lý do</a>',
				attributes: {'class': 'table-cell text-left text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
				template: '#= window.setTemplateForGrid("LDo", data) #'
			},
		],
		dataBound: function(e) {
			_gridSub01.find('tbody[role="rowgroup"]').find('tr[data-uid]').undelegate('i[data-sub-action]', 'click');
			_gridSub01.find('tbody[role="rowgroup"]').find('tr[data-uid]').delegate('i[data-sub-action]', 'click', function(e){
				event.preventDefault();/*event.stopPropagation();*/
				
				var $obj = $(this);
				var $tr = $obj.closest('tr');
				var subAction = $obj.attr('data-sub-action');
				
				var indexRow = $tr.index();
				switch (subAction) {
				case 'remove':
					alertConfirm('Bạn có muốn xóa dòng ' + (indexRow + 1) + ' không?',
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
	
	_gridSub01.find('tbody[role="rowgroup"]').undelegate('.input-grid', 'change');
	_gridSub01.find('tbody[role="rowgroup"]').delegate('.input-grid', 'change', function(e){
		var $obj = $(this);
		var _name = $obj.attr('name');
		var $tr = $obj.closest('tr');
		
		var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
		try{
			var indexRow = $tr.index();
			var rowData = objDataJson[indexRow];
			rowData[_name] = $obj.val();
		}catch(err){
			console.log(err);
		}
		
	});
	
	readonlyByLoaiTB();
	$('#f-tbhdssot-crud').find('#Loai').change(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		readonlyByLoaiTB();
	});
	
	$('#f-tbhdssot-crud #tinh-thanh').change(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var _val = $(this).val();
		
		$('#f-tbhdssot-crud').find('#CQTQLy').empty();
		$('#f-tbhdssot-crud').find('#CQTQLy').append($("<option></option>").text('').val(''));
		
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
						$('#f-tbhdssot-crud').find('#CQTQLy').append(
							$("<option></option>").text(item['name']).val(item['code'])
						);
					});
				}
			},
			error:function (xhr, ajaxOptions, thrownError){
	        }
		});
	});
	
	$('#f-tbhdssot-crud').find('button[data-action]').click(function (event) {
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
												$('#f-tbhdssot-crud').find('button[data-action="back"]').trigger('click');
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
						)
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
		case 'add-inv':
			objDataSend = {};
			showPopupWithURLAndData(ROOT_PATH + '/common/show-search-dshddky', objDataSend, false, function(e){
				if($.type(e) == 'array'){
					var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
					
					/*
					 * LAY DANH SACH ID HD DA TON TAI; 
					 * CHI ADD ID MOI
					 * */
					var ids = [];
					$.each(objDataJson, function(index, value) {
						ids.push(value['_id']);
					});
					
					var item = null;
					$.each(e, function(index, value) {
						if(ids.indexOf(value['_id']) == -1){
							item = {};
							item['_id'] = value['_id'];
							item['MSHDon'] = value['MauSoHD'];
							item['SHDon'] = value['EInvoiceNumber'];
							item['Ngay'] = value['NLap'];
							item['MCQTCap'] = value['MCCQT'];
							objDataJson.push(item);
						}
					});
					_gridSub01.data("kendoGrid").dataSource.data(objDataJson);
				}
			});
			break;
		case 'sign':
			objDataSend = {};
			objDataSend['_id'] = $('#f-tbhdssot-crud').find('input[name="_id"]').val();
			$.ajax({
				type: "POST",
				datatype: "json",
				url: ROOT_PATH + '/main/tbhdssot-sign/check-data-sign',
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
								//KIEM TRA THONG TIN CERT
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
										hideLoading();
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
																		formData.append('_id', $('#f-tbhdssot-crud').find('input[name="_id"]').val());
																		
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
																						$('#f-tbhdssot-crud').find('button[data-action="back"]').trigger('click');
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
																		
																		var urlPost = ROOT_PATH + '/main/tbhdssot-sign/signFile';
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
												    		alertDLSuccess("Lỗi: Không ký được dữ liệu hóa đơn.", function(){});
														}
											        },
											        error:function(){
											            
											        }
												});
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
		case 'back':
			$('#divMainContent').show();
			$('#divSubContent').hide(function(){$(this).empty();});
			try{
				if($('#f-tbhdssot').find('#grid').length > 0)
					$('#f-tbhdssot').find('#grid').data("kendoGrid").dataSource.read();
			}catch(err){}
			break;

		default:
			break;
		}
	});
	
});

function readonlyByLoaiTB(){
	var _vLoai = $('#f-tbhdssot-crud').find('#Loai').val();
	if(_vLoai == null) _vLoai = '';
	$('#f-tbhdssot-crud').find('#So, #NTBCCQT').val('');
	$('#f-tbhdssot-crud').find('#So, #NTBCCQT').prop('readonly', _vLoai != '2');
	if(_vLoai == '2'){
		$('#f-tbhdssot-crud').find('#NTBCCQT').removeClass('d-none');
		$('#f-tbhdssot-crud').find('#NTBCCQT-TMP').addClass('d-none');
	}else{
		$('#f-tbhdssot-crud').find('#NTBCCQT').addClass('d-none');
		$('#f-tbhdssot-crud').find('#NTBCCQT-TMP').removeClass('d-none');
	}
}

function setTemplateForGrid(key, data){
	if(!vIsEdit) return data[key] == null? '': data[key];
	
	var _valTmp = '';
	var text = '';
	_valTmp = null == data[key]? '': data[key];
	
	text = '<div class="form-row m-l-1 m-r-1">';
	switch (key) {
	case 'LDo':
		text = '<input type="text" name="' + key + '" value="' + (null == data[key]? '': data[key]) + '" class="input-grid k-input form-control form-control-sm" >';
		break;
	case 'TCTBao':
		text += '<select class="input-grid form-control form-control-sm"  name="' + key + '" style="height: 100%;" >';
		text += '<option value="" ></option>';
		$.each(objTCTBao, function(k, v){
			if('|1|2|3|4|'.indexOf(k) != -1)
				text += '<option value="' + k + '" ' + (k == _valTmp? 'selected="selected" ': '') + '>' + v + '</option>';
		});
		text += '</select>';
		break;
	case 'LADHDDT':
		if('' == _valTmp) _valTmp = '1';
		
		text += '<select class="input-grid form-control form-control-sm" disabled="disabled" name="' + key + '" style="height: 100%;" >';
		text += '<option value="" ></option>';
		$.each(objLADHDDT, function(k, v){
			text += '<option value="' + k + '" ' + (k == _valTmp? 'selected="selected" ': '') + '>' + v + '</option>';
		});
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
	
	dataPost['_id'] = $('#f-tbhdssot-crud').find('input[name="_id"]').val();
	dataPost['tinh-thanh'] = $('#f-tbhdssot-crud').find('#tinh-thanh').val();
	dataPost['CQTQLy'] = $('#f-tbhdssot-crud').find('#CQTQLy').val();
	dataPost['Loai'] = $('#f-tbhdssot-crud').find('#Loai').val();
	dataPost['So'] = $('#f-tbhdssot-crud').find('#So').val();
	dataPost['NTBCCQT'] = $('#f-tbhdssot-crud').find('#NTBCCQT').val();
	var arrRows = [];
	var obj = null;
	var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
	jQuery.each(objDataJson, function(index, item) {
		obj = {'MCQTCap': item['MCQTCap']};
		obj['TCTBao'] = undefined == item['TCTBao']? '': item['TCTBao']
		obj['LDo'] = undefined == item['LDo']? '': item['LDo'];
		arrRows.push(obj);
	});
	dataPost['ds-hd'] = encodeObjJsonBase64UTF8(arrRows);
	
	return dataPost;
}