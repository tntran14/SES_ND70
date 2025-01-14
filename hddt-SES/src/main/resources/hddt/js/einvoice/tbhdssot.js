
$(function(){
	dateInputFormat($('#f-tbhdssot').find('#from-date'));
	dateInputFormat($('#f-tbhdssot').find('#to-date'));
	
	_gridMain.kendoGrid({
		dataSource: new kendo.data.DataSource({
			transport: {
				read: {
					type: 'POST',
					url: ROOT_PATH + '/main/tbhdssot/search',
                    dataType: 'json',
                    data: function(){return getDataSearch();},
                    beforeSend: function(req){
                    	initAjaxJsonGridRequest(req);
                	},
				}
			},
			requestEnd: function (e) {
               	if (e.type === "read" && e.response) {
               		if(e.response.errorCode == 0){
               		}else{
               			notificationDLSuccess(createObjectError(e.response).html(), function(){});
               		}
               	}
           	},
            schema: {
				data: "rows",
                total: "total",
                model: {
					fields: {
					}
				}
			},
			pageSize: KENDOUI_PAGESIZE_NO_SCROLL_Y,
			serverPaging: true,
			serverSorting: true,
           	serverFiltering: true,
           	change: function(e) {
            },
		}),
		selectable: true, scrollable: true, 
 		sortable: {mode: "single", allowUnsort: true},
		sortable: true,
// 		filterable: { mode: "row"},
		filterable: false, resizable: true,
		serverSorting: false,
//		height: kendoGridHeight,
		pageable: {
			refresh: true,
			pageSizes: true,
			buttonCount: KENDOUI_BUTTONCOUNT,
			messages: {
				itemsPerPage: kendoGridMessages.itemsPerPage,
				previous: kendoGridMessages.previous,
				next: kendoGridMessages.next,
				refresh: kendoGridMessages.refresh,
				last: kendoGridMessages.last,
				first: kendoGridMessages.first,
				empty: kendoGridMessages.empty,
				display: kendoGridMessages.display
			},
			pageSizes: KENDOUI_PAGESIZES,
			numeric: true
		},
		dataBinding: function () {
            record = (this.dataSource.page() - 1) * this.dataSource.pageSize();
        },
        columns: [
        	{field: 'STT', width: '60px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">STT</a>',
  				attributes: {'class': 'table-cell text-center'}, sortable: false, 
  				headerAttributes: {'class': 'table-header-cell text-center'}, template: "#= ++record #",
  			},
  			{field: 'func', title: '', width: '50px', encoded: false
  				, headerTemplate: '&nbsp;'
				, attributes: {'class': 'table-cell', style: 'text-align: center;'}, sortable: false
				, headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
//				, template: '<i title="In hóa đơn" class="mdi mdi-file-pdf-outline fs-25 text-danger c-pointer"></i>'
				, template: '#= window.setTemplateForGridMAIN("func", data) #'
			},
			{field: 'StatusDesc', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Trạng thái</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'SignStatusDesc', width: '80px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Đã ký</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
  			{field: 'TenMSo', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mẫu số</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			
		
			{field: 'LoaiTB', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Loại thông báo</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'DSLoi', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Lỗi từ CQT</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'NTBao', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Ngày thông báo</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'MTDiep', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã thông điệp</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},	
			{field: 'MTDTChieu', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã tham chiếu</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'UserCreated', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người lập</a>',
				attributes: {'class': 'table-cell text-left text-nowrap'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
    	],
    	dataBound: function(e) {
    		$("#f-tbhdssot").find('button[data-action="tbhdssot-detail"], button[data-action="tbhdssot-edit"], button[data-action="tbhdssot-sign"]').prop('disabled', true);
    		
    		_gridMain.find('tbody[role="rowgroup"]').find('tr').undelegate('i[data-sub-action]', 'click');
			_gridMain.find('tbody[role="rowgroup"]').find('tr').delegate('i[data-sub-action]', 'click', function(e){
				e.preventDefault();/*e.stopPropagation();*/
				
				var $obj = $(this);
				var $tr = $obj.closest('tr');
				var subAction = $obj.attr('data-sub-action');
				
				var indexRow = $tr.index();
				var rowData = null;
				var objData = {};
				var objURL = {};
				
				switch (subAction) {
				case 'print04':
					rowData = _gridMain.data("kendoGrid").dataItem($tr);
					window.open(ROOT_PATH + '/common/print04/' + rowData['_id'],'_blank');
					break;
				case 'refresh':
					rowData = _gridMain.data("kendoGrid").dataItem($tr);
					objData['_id'] = rowData['_id'];
					$.ajax({
						type: "POST",
						datatype: "json",
						url: ROOT_PATH + '/main/tbhdssot/refresh-status-cqt',
						data: objData,
						beforeSend: function(req) {
							initAjaxJsonRequest(req);
				        	showLoading();
						},
						success:function(res) {
							hideLoading();
							if(res) {
								if(res.errorCode == 0) {
									_gridMain.data("kendoGrid").dataSource.read();
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
					break;
				case 'send-cqt':
				case 'delete':
					if('send-cqt' == subAction){
						objURL['check'] = ROOT_PATH + '/main/tbhdssot-send-cqt/check-data';
						objURL['exec'] = ROOT_PATH + '/main/tbhdssot-send-cqt/exec-data';
					}else if('delete' == subAction){
						objURL['check'] = ROOT_PATH + '/main/tbhdssot-del/check-data';
						objURL['exec'] = ROOT_PATH + '/main/tbhdssot-del/exec-data';
					}
					
					rowData = _gridMain.data("kendoGrid").dataItem($tr);
					objData['_id'] = rowData['_id'];
					$.ajax({
						type: "POST",
						datatype: "json",
						url: objURL['check'],
						data: objData,
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
									
									objData['tokenTransaction'] = tokenTransaction;
									
									alertConfirm(confirmText,
										function(e){
											$.ajax({
												type: "POST",
												datatype: "json",
												url: objURL['exec'],
												data: objData,
												beforeSend: function(req) {
													initAjaxJsonRequest(req);
										        	showLoading();
												},
												success:function(res) {
													hideLoading();
													if(res) {
														if(res.errorCode == 0) {
															_gridMain.data("kendoGrid").dataSource.read();
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
					
					break;

				default:
					break;
				}
				
			});
			
    	}
	});
	
	_gridMain.find('table[role="grid"]').find('tbody').undelegate('tr', 'click');
	_gridMain.find('table[role="grid"]').find('tbody').delegate('tr', 'click', function(e){
		$("#f-tbhdssot").find('button[data-action="tbhdssot-detail"]').prop('disabled', false);		
		var $tr = $(this).closest("tr");
		var rowData = _gridMain.data("kendoGrid").dataItem($tr);
		$("#f-tbhdssot").find('button[data-action="tbhdssot-sign"]').prop('disabled', 'NOSIGN' == rowData['SignStatusCode']? false: true);
		$("#f-tbhdssot").find('button[data-action="tbhdssot-edit"]').prop('disabled', 'NOSIGN' == rowData['SignStatusCode']? false: true);
	});
	
	$("#f-tbhdssot").find('button[data-action]').click(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var dataAction = $(this).data('action');
		
		var $obj = $(this);
		
		var rowData = null;
		var actionCheck = '|tbhdssot-edit|tbhdssot-sign|tbhdssot-detail|';
		
		var entityGrid = _gridMain.data("kendoGrid");
		var selectedItem = entityGrid.dataItem(entityGrid.select());
		if(actionCheck.indexOf('|' + dataAction + '|') != -1 && selectedItem == null){
			alertDLSuccess('<span class="required">Vui lòng chọn dòng dữ liệu để thực hiện.</span>', function(){});
			return;
		}
		
		var objData = {};
		var objDataSend = {};
		switch (dataAction) {
		case 'search':
			_gridMain.data("kendoGrid").dataSource.page(1);
			break;
		case 'tbhdssot-cre':
			$('#divSubContent').show();$('#divMainContent').hide();
			submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
			break;
		case 'tbhdssot-edit':
		case 'tbhdssot-detail':
			objData['_id'] = selectedItem['_id'];
			$('#divSubContent').show();$('#divMainContent').hide();
			submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
			break;
		case 'tbhdssot-sign':
			objDataSend['_id'] = selectedItem['_id'];	
		
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
																		formData.append('_id',  selectedItem['_id']);
																		
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
																						   _gridMain.data("kendoGrid").dataSource.read();
																							  timeoutID = setTimeout(callsend_cqt, 1000);
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
					function callsend_cqt() {
						var objURL = {};
						objURL['check'] = ROOT_PATH + '/main/tbhdssot-send-cqt/check-data';
						objURL['exec'] = ROOT_PATH + '/main/tbhdssot-send-cqt/exec-data';
		                  $.ajax({
		                    type: "POST",
		                    datatype: "json",
		                    url: objURL['check'],
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
		                          tokenTransaction = responseData['TOKEN'];
		                          objDataSend['tokenTransaction'] = tokenTransaction;         
		                              $.ajax({
		                                type: "POST",
		                                datatype: "json",
		                                url: objURL['exec'],
		                                data: objDataSend,
		                                beforeSend: function(req) {
		                                  initAjaxJsonRequest(req);
		                                      showLoading();
		                                },
		                                success:function(res) {
		                                  hideLoading();
		                                  if(res) {
		                                    if(res.errorCode == 0) {
		                                     _gridMain.data("kendoGrid").dataSource.read();
																						 	
										 timeoutID2 = setTimeout(callrefesh_cqt, 4000);
											
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
						}

					
					
					function callrefesh_cqt() {
					
						$.ajax({
							type: "POST",
							datatype: "json",
							url: ROOT_PATH + '/main/tbhdssot/refresh-status-cqt',
							data: objDataSend,
							beforeSend: function(req) {
								initAjaxJsonRequest(req);
					        	showLoading();
							},
							success:function(res) {
								hideLoading();
								if(res) {
									if(res.errorCode == 0) {
										_gridMain.data("kendoGrid").dataSource.read();
										hideLoading();
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
						}	
					
						function clearAlert() {
						  clearTimeout(timeoutID);
						}
						function clearAlert() {
							  clearTimeout(timeoutID2);
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

function getDataSearch(){
	var dataPost = {};
	
	dataPost['from-date'] = $('#f-tbhdssot #from-date').val() == null? '': $('#f-tbhdssot #from-date').val();
	dataPost['to-date'] = $('#f-tbhdssot #to-date').val() == null? '': $('#f-tbhdssot #to-date').val();
	
	return dataPost;
}

function setTemplateForGridMAIN(key, data){
	var signStatusCode = data['SignStatusCode'];
	var eInvoiceStatus = data['Status'];
	var text = '';
	
	switch (key) {
	case 'func':
		if('CREATED' == eInvoiceStatus || 'NOSIGN' == signStatusCode){
			text += '<i title="Xóa" class="mdi mdi-close-box fs-25 text-danger c-pointer" data-sub-action="delete" ></i>';
		}else if('PENDING' == eInvoiceStatus && 'SIGNED' == signStatusCode){
			text += '<i title="Gửi CQT" class="mdi mdi-telegram fs-25 text-info c-pointer" data-sub-action="send-cqt" ></i>';
			text += '<i title="Xóa" class="mdi mdi-close-box fs-25 text-danger c-pointer" data-sub-action="delete" ></i>';
		}else if('PROCESSING' == eInvoiceStatus && 'SIGNED' == signStatusCode){
			text = '<i title="Lấy kết quả từ CQT" class="mdi mdi-refresh-circle fs-25 text-info c-pointer" data-sub-action="refresh" ></i>';
		}else if('COMPLETE' == eInvoiceStatus && 'SIGNED' == signStatusCode){

			text += '<i title="In PDF" class="mdi mdi-file-pdf fs-25 text-red c-pointer" data-sub-action="print04" ></i>';
			
		}
		break;

	default:
		break;
	}
	return text;
}

