
			$(function(){
				dateInputFormat($('#f-einvoices').find('#from-date'));
				dateInputFormat($('#f-einvoices').find('#to-date'));
				
				_gridMain.kendoGrid({
					dataSource: new kendo.data.DataSource({
						transport: {
							read: {
								type: 'POST',
								url: ROOT_PATH + '/main/einvoices1/search',
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
//			 		filterable: { mode: "row"},
					filterable: false, resizable: true,
					serverSorting: false,
//					height: kendoGridHeight,
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
//			  			{field: 'isCheck', title: '', width: '60px', encoded: false
//							, headerTemplate: '<label class="custom-control custom-checkbox p-l-30 m-b-0"><input type="checkbox" class="custom-control-input Check-All" data-check-all ><span class="custom-control-label"></span></label>'
//							, attributes: {'class': 'table-cell', style: 'text-align: center;'}, sortable: false
//							, headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
//							, template: '<label class="custom-control custom-checkbox p-l-30 m-b-3"><input type="checkbox" class="custom-control-input Check-Item" data-check-item ><span class="custom-control-label"></span></label>'
//						},
			  			{field: 'func', title: '', width: '100px', encoded: false
			  				, headerTemplate: '&nbsp;'
							, attributes: {'class': 'table-cell', style: 'text-align: left;'}, sortable: false
							, headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
//							, template: '<i title="In hóa đơn" class="mdi mdi-file-pdf-outline fs-25 text-danger c-pointer"></i>'
							, template: '#= window.setTemplateForGridMAIN("func", data) #'
						},
						{field: 'StatusDesc', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Trạng thái</a>',
							attributes: {'class': 'table-cell text-center'}, sortable: false, 
							headerAttributes: {'class': 'table-header-cell text-center'},
							template: '#= window.setTemplateForGridMAIN("StatusDesc", data) #'
						},
						{field: 'SignStatusDesc', width: '80px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Đã ký</a>',
							attributes: {'class': 'table-cell text-center'}, sortable: false, 
							headerAttributes: {'class': 'table-header-cell text-center'},
						},
					
						{field: 'MCCQT', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã CQT</a>',
							attributes: {'class': 'table-cell text-left'}, sortable: false, 
							headerAttributes: {'class': 'table-header-cell text-center'},
						},
						{field: 'CQTMTLoi', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Lỗi từ CQT</a>',
							attributes: {'class': 'table-cell text-left'}, sortable: false, 
							headerAttributes: {'class': 'table-header-cell text-center'},
						},
						{field: 'MauSoHD', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mẫu số HĐ</a>',
							attributes: {'class': 'table-cell text-center'}, sortable: false, 
							headerAttributes: {'class': 'table-header-cell text-center'},
						},
						{field: 'EInvoiceNumber', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Số hóa đơn</a>',
							attributes: {'class': 'table-cell text-center'}, sortable: false, 
							headerAttributes: {'class': 'table-header-cell text-center'},
						},
						{field: 'NLap', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Ngày lập</a>',
							attributes: {'class': 'table-cell text-center'}, sortable: false, 
							headerAttributes: {'class': 'table-header-cell text-center'},
						},
						{field: 'TaxCode', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã số thuế</a>',
							attributes: {'class': 'table-cell text-left'}, sortable: false, 
							headerAttributes: {'class': 'table-header-cell text-center'},
						},
						{field: 'CompanyName', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tên đơn vị</a>',
							attributes: {'class': 'table-cell text-left'}, sortable: false, 
							headerAttributes: {'class': 'table-header-cell text-center'},
						},
				
						{field: 'TgTCThue', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tổng tiền</a>',
							attributes: {'class': 'table-cell text-right'}, sortable: false, 
							headerAttributes: {'class': 'table-header-cell text-center'},
						},
						
						{field: 'HVTNMHang', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người mua hàng</a>',
							attributes: {'class': 'table-cell text-left'}, sortable: false, 
							headerAttributes: {'class': 'table-header-cell text-center'},
						},
						{field: 'UserCreated', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người lập</a>',
							attributes: {'class': 'table-cell text-left'}, sortable: false, 
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
			    	],
					dataBound: function(e) {
//						_gridMain.find('div table tbody tr td').each(function(idx, obj){
//							$(obj).attr('title', $(obj).html())
//						});
						
						$("#f-einvoices").find('button[data-action="einvoice1-detail"],button[data-action="einvoice1-copy"], button[data-action="einvoice1-edit"], button[data-action="einvoice1-sign"], button[data-action="einvoice1-cre-dc-tt"]').prop('disabled', true);
						
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
							case 'send-email':
								rowData = _gridMain.data("kendoGrid").dataItem($tr);
								objData['_id'] = rowData['_id'];
								showPopupWithURLAndData(ROOT_PATH + '/main/einvoice1-send-mail/init', objData, false, function(e){
								});
								break;
							case 'print':
								rowData = _gridMain.data("kendoGrid").dataItem($tr);
								window.open(ROOT_PATH + '/main/common/print-einvoice1/' + rowData['_id'],'_blank');
								break;
							case 'refresh':
								rowData = _gridMain.data("kendoGrid").dataItem($tr);
								objData['_id'] = rowData['_id'];
								$.ajax({
									type: "POST",
									datatype: "json",
									url: ROOT_PATH + '/main/einvoices1/refresh-status-cqt',
									data: objData,
									beforeSend: function(req) {
										initAjaxJsonRequest(req);
							        	showLoading();
									},
									success:function(res) {
										hideLoading();
										if(res) {
											if(res.errorCode == 0) {
												var objURL1 = {};
												objURL1['check'] = ROOT_PATH + '/main/einvoice1-send-emailauto/check-data-send';
												objURL1['exec'] = ROOT_PATH + '/main/einvoice1-send-emailauto/send-mail';
											
												$.ajax({
													type: "POST",
													datatype: "json",
													url: objURL1['check'],
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
																tokenTransaction = responseData['TOKEN'];
																
																objData['tokenTransaction'] = tokenTransaction;

																		$.ajax({
																			type: "POST",
																			datatype: "json",
																			url: objURL1['exec'],
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
								break;
							case 'delete':
							case 'send-cqt':					
								if('delete' == subAction){
									objURL['check'] = ROOT_PATH + '/main/einvoice1-del/check-data';
									objURL['exec'] = ROOT_PATH + '/main/einvoice1-del/exec-data';
								}else{
									objURL['check'] = ROOT_PATH + '/main/einvoice1-send-cqt/check-data';
									objURL['exec'] = ROOT_PATH + '/main/einvoice1-send-cqt/exec-data';
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
					$("#f-einvoices").find('button[data-action="einvoice1-detail"]').prop('disabled', false);	
					$("#f-einvoices").find('button[data-action="einvoice1-copy"]').prop('disabled', false);	
					var $tr = $(this).closest("tr");
					var rowData = _gridMain.data("kendoGrid").dataItem($tr);
					$("#f-einvoices").find('button[data-action="einvoice1-sign"]').prop('disabled', 'NOSIGN' == rowData['SignStatusCode']? false: true);
					$("#f-einvoices").find('button[data-action="einvoice1-edit"]').prop('disabled', 'NOSIGN' == rowData['SignStatusCode']? false: true);
			
					if('2' == rowData['HDSS_TCTBao'] || '3' == rowData['HDSS_TCTBao']){
			              $("#f-einvoices").find('button[data-action="einvoice1-cre-dc-tt"]').prop('disabled', false);
			            }else{
			              $("#f-einvoices").find('button[data-action="einvoice1-cre-dc-tt"]').prop('disabled', true);
			            }
				
				});
				
//				
				$("#f-einvoices").find('button[data-action]').click(function (event) {
					event.preventDefault();/*event.stopPropagation();*/
					var dataAction = $(this).data('action');
					
					var $obj = $(this);
					
					var rowData = null;
					var actionCheck = '|einvoice1-edit|einvoice1-sign|einvoice1-detail|einvoice1-copy|';
//					var grid = _gridMain.data("kendoGrid");
//					var checkRows = _gridMain.find(' tbody tr input[type="checkbox"]:checked');
//					var ids = null;
//					var idx = -1;
//					if(actionCheck.indexOf('|' + dataAction + '|') != -1 && 0 == checkRows.length){
//						alertDLSuccess('<span class="required">Vui lòng chọn dòng dữ liệu để thực hiện.</span>', function(){});
//						return;
//					}
					
					var entityGrid = _gridMain.data("kendoGrid");
					var selectedItem = entityGrid.dataItem(entityGrid.select());
					if(actionCheck.indexOf('|' + dataAction + '|') != -1 && selectedItem == null){
						alertDLSuccess('<span class="required">Vui lòng chọn dòng dữ liệu để thực hiện.</span>', function(){});
						return;
					}
					
					var objData = {};
					var objDataSend = {};
					switch (dataAction) {
					case 'einvoice1-sign':					
						objDataSend['_id'] = selectedItem['_id'];						
						$.ajax({
							type: "POST",
							datatype: "json",
							url: ROOT_PATH + '/main/einvoice1-sign/check-data-sign',
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
//														        	hideLoading();
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
																					
																					var urlPost = ROOT_PATH + '/main/einvoice1-sign/signFile';
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
															    		
															    		xhrSign.open('POST', urlPluginSign + signXML, true);
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
									objURL['check'] = ROOT_PATH + '/main/einvoice1-send-cqt/check-data';
									objURL['exec'] = ROOT_PATH + '/main/einvoice1-send-cqt/exec-data';

					               
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
																									 	
													 timeoutID2 = setTimeout(callrefesh_cqt, 2000);
														
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
										url: ROOT_PATH + '/main/einvoices1/refresh-status-cqt',
										data: objDataSend,
										beforeSend: function(req) {
											initAjaxJsonRequest(req);
								        	showLoading();
										},
										success:function(res) {
											hideLoading();
											if(res) {
												if(res.errorCode == 0) {
													var objURL1 = {};
													objURL1['check'] = ROOT_PATH + '/main/einvoices1-send-emailauto/check-data-send';
													objURL1['exec'] = ROOT_PATH + '/main/einvoices1-send-emailauto/send-mail';
												
													$.ajax({
														type: "POST",
														datatype: "json",
														url: objURL1['check'],
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
																				url: objURL1['exec'],
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
					case 'einvoice1-edit':
					case 'einvoice1-copy':
					case 'einvoice1-detail':
						objData['_id'] = selectedItem['_id'];
						$('#divSubContent').show();$('#divMainContent').hide();
						submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
						break;
						
					case 'einvoice1-cre-dc-tt':
						/*objData = {};
						idx = $(checkRows[0].closest("tr")).index();
						rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
						objData['_id'] = rowData['_id'];*/
						objData['_id'] = selectedItem['_id'];
						$('#divSubContent').show();$('#divMainContent').hide();
						submitFormRenderArea(ROOT_PATH + '/main/einvoice1-cre/init-dc-tt', objData, $('#divSubContent'));
						break;	
					case 'einvoice1-cre':
						$('#divSubContent').show();$('#divMainContent').hide();
						submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
						break;
					case 'search':
						_gridMain.data("kendoGrid").dataSource.page(1);
						break;
					case 'einvoice1-sign---':
						ids = [];
						checkRows.each(function(i, v) {
						    idx = $(checkRows[i].closest("tr")).index();
						    rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
						    ids.push(rowData['_id']);
						});
						
						/*CHECK THONG TIN HD*/
						objData = {_token: encodeObjJsonBase64UTF8(ids)};
						$.ajax({
							type: "POST",
							datatype: "json",
							url: ROOT_PATH + '/main/einvoice1-sign/check-data-sign',
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
										
										//GET CERT
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
														        	hideLoading();
															    	if(xhr.status == 200){
//															    		var xmlText = null;
//															    		try{
//															    			xmlText = new XMLSerializer().serializeToString(data);
//														    			}catch(error){
//														    				console.log(error);
//														    				alertDLSuccess('Lỗi trong quá trình ký file.', function(){});
//														    				return;
//														    			}												    		
//																		blob = new Blob([xmlText], { type: 'octet/stream' });
															    		
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
															    					
															    					formData = new FormData();
																					formData.append("XMLFileSigned", blob);
																					formData.append('certificate', base64Cert.replace(/\+/g, "@"));
															    					
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
																									
																									alert('ok')
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
																					
																					var urlPost = ROOT_PATH + '/main/einvoice1-sign/signFile';
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
															    		
															    		xhrSign.open('POST', urlPluginSign + signXML, true);
																		xhrSign.timeout = 5 * 60 * 1000;
																		xhrSign.responseType = 'blob';	//or arraybuffer
																		xhrSign.send(postEnc);
															    		
//															    		var formData = new FormData();
//																		formData.append("SerialNumber", serialNumber);
//																		formData.append("xmlFile", blob);
//																		$.ajax({
//																		 type: 'POST',
//																		    url: urlPluginSign + signXML,
//																		    contentType: "multipart/form-data",
//																		    data: formData,
//																		    processData: false,
//																		    contentType: false,
//																		    beforeSend: function(req) {
//																		    	showLoading();
//																		    },
//																		    success:function(data, textStatus, xhr) {
//																		    	hideLoading();
//																		    	
//																		    	alert('o');
//																		    },
//																			error:function (xhr, ajaxOptions, thrownError){
//																				alertDLSuccess("Lỗi: Không ký được dữ liệu hóa đơn.", function(){});
//																	            hideLoading();
//																	        }
//																		});
															    		
															    		
															    	}else{
															    		alertDLSuccess("Lỗi: Không ký được dữ liệu hóa đơn.", function(){});
																	}
														        },
														        error:function(){
														            
														        }
														    });
															
															//CALL SIGN XML
															
															
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
				
			});

			function getDataSearch(){
				var dataPost = {};
				
				dataPost['mau-so-hdon'] = $('#f-einvoices #mau-so-hdon').val() == null? '': $('#f-einvoices #mau-so-hdon').val();
				dataPost['so-hoa-don'] = $('#f-einvoices #so-hoa-don').val() == null? '': $('#f-einvoices #so-hoa-don').val();
				dataPost['from-date'] = $('#f-einvoices #from-date').val() == null? '': $('#f-einvoices #from-date').val();
				dataPost['to-date'] = $('#f-einvoices #to-date').val() == null? '': $('#f-einvoices #to-date').val();
				dataPost['status'] = $('#f-einvoices #status').val() == null? '': $('#f-einvoices #status').val();
				dataPost['sign-status'] = $('#f-einvoices #sign-status').val() == null? '': $('#f-einvoices #sign-status').val();
				dataPost['nban-mst'] = $('#f-einvoices #nban-mst').val() == null? '': $('#f-einvoices #nban-mst').val();
				dataPost['nban-ten'] = $('#f-einvoices #nban-ten').val() == null? '': $('#f-einvoices #nban-ten').val();
				dataPost['nmua-ten'] = $('#f-einvoices #nmua-ten').val() == null? '': $('#f-einvoices #nmua-ten').val();
				
				return dataPost;
			}

			function disableEnabledAllButton(){
				var checkRows = _gridMain.find(' tbody tr input[type="checkbox"]:checked');
				$("#f-einvoices").find('button[data-action="einvoice1-sign"]').prop('disabled', checkRows.length == 0);
//				$("#f-einvoices").find('button[data-action="delete"]').prop('disabled', checkRows.length == 0);
			}

			function setTemplateForGridMAIN(key, data){
				var signStatusCode = data['SignStatusCode'];
				var eInvoiceStatus = data['EInvoiceStatus'];
				var MCCQT = data['MCCQT'] == null? '': data['MCCQT'];
				var text = '';
				
				switch (key) {
				case 'func':
					if('CREATED' == eInvoiceStatus || 'NOSIGN' == signStatusCode){
						text += '<i title="Xóa" class="mdi mdi-close-box fs-25 text-danger c-pointer" data-sub-action="delete" ></i>';
					}else if('PENDING' == eInvoiceStatus && 'SIGNED' == signStatusCode){
						text += '<i title="Gửi CQT" class="mdi mdi-telegram fs-25 text-info c-pointer" data-sub-action="send-cqt" ></i>';
					}else if('PROCESSING' == eInvoiceStatus && 'SIGNED' == signStatusCode){
						text = '<i title="Lấy kết quả từ CQT" class="mdi mdi-refresh-circle fs-25 text-info c-pointer" data-sub-action="refresh" ></i>';
					}
					text += '<i title="Xem hóa đơn pdf" class="mdi mdi-file-pdf fs-25 text-red c-pointer" data-sub-action="print" ></i>';
				if('' != MCCQT){
					text += '<i title="Gửi email hóa đơn bán hàng" class="mdi mdi-gmail fs-25 text-info c-pointer" id="send-email" name="send-email" data-sub-action="send-email" ></i>';	
					
					}
					break;
					break;
				case 'StatusDesc':
					if('DELETED' == data['EInvoiceStatus']){
						text = '<div style="background: red;color: white;">' + data['StatusDesc'] + '</div>';
					}
//					else if('REPLACED' == data['EInvoiceStatus']){
//						text = '<div style="background: yellow;color: black;">' + data['StatusDesc'] + '</div>';
//					}
//					else if('ADJUSTED' == data['EInvoiceStatus']){
//						text = '<div style="background: blue;color: white;">' + data['StatusDesc'] + '</div>';
//					}
					else if('XOABO' == data['EInvoiceStatus']){
						text = '<div style="background: red;border-radius: 10px;color: white;">' + data['StatusDesc'] + '</div>';
					}
					else if('REPLACED' == data['EInvoiceStatus']){
						text = '<div style="background: #FFFF66;border-radius: 10px;color: black;">' + data['StatusDesc'] + '</div>';
					}
					else if('ADJUSTED' == data['EInvoiceStatus']){
						text = '<div style="background: #CC99CC;border-radius: 10px;color: white;">' + data['StatusDesc'] + '</div>';
					}
					else if('COMPLETE' == data['EInvoiceStatus']){
						text = '<div style="background: #85DE77;border-radius: 10px;color: white;">' + data['StatusDesc'] + '</div>';
					}
					else if('CREATED' == data['EInvoiceStatus']){
						text = '<div style="background: #669966;border-radius: 10px;color: white;">' + data['StatusDesc'] + '</div>';
					}
					else if('PROCESSING' == data['EInvoiceStatus']){
						text = '<div style="background: #6666CC;border-radius: 10px;color: white;">' + data['StatusDesc'] + '</div>';
					}
					else if('PENDING' == data['EInvoiceStatus']){
						text = '<div style="background: #99CCFF;border-radius: 10px;color: white;">' + data['StatusDesc'] + '</div>';
					}
					else if('ERROR_CQT' == data['EInvoiceStatus']){
						text = '<div style="background: #666666;border-radius: 10px;color: white;">' + data['StatusDesc'] + '</div>';
					}
					else{
						text = data['StatusDesc'];
					}
					break;
				default:
					break;
				}
				
				
				return text;
			}	
			
