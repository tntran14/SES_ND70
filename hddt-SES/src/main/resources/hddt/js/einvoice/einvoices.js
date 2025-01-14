

				$(function(){
					dateInputFormat($('#f-einvoices').find('#from-date'));
					dateInputFormat($('#f-einvoices').find('#to-date'));
					var  timeoutID;
					var  timeoutID2;
					_gridMain.kendoGrid({
						dataSource: new kendo.data.DataSource({
							transport: {
								read: {
									type: 'POST',
									url: ROOT_PATH + '/main/einvoices/search',
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
//				 		filterable: { mode: "row"},
						filterable: false, resizable: true,
						serverSorting: false,
//						height: kendoGridHeight,
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
				  			{field: 'isCheck', title: '', width: '60px', encoded: false
								, headerTemplate: '<label class="custom-control custom-checkbox p-l-30 m-b-0"><input type="checkbox" class="custom-control-input Check-All" data-check-all ><span class="custom-control-label"></span></label>'
								, attributes: {'class': 'table-cell', style: 'text-align: center;'}, sortable: false
								, headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
								, template: '<label class="custom-control custom-checkbox p-l-30 m-b-3"><input type="checkbox" class="custom-control-input Check-Item" data-check-item ><span class="custom-control-label"></span></label>'
							},
				  			{field: 'func', title: '', width: '150px', encoded: false
				  				, headerTemplate: '&nbsp;'
								, attributes: {'class': 'table-cell', style: 'text-align: left;'}, sortable: false
								, headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
//								, template: '<i title="In hóa đơn" class="mdi mdi-file-pdf-outline fs-25 text-danger c-pointer"></i>'
								, template: '#= window.setTemplateForGridMAIN("func", data) #'
							},
							{field: 'StatusDesc', width: '140px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Trạng thái hóa đơn</a>',
								attributes: {'class': 'table-cell text-center'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
								template: '#= window.setTemplateForGridMAIN("StatusDesc", data) #'
							},
							{field: 'EInvoiceNumber', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Số hóa đơn</a>',
								attributes: {'class': 'table-cell text-center'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							{field: 'MauSoHD', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mẫu số HĐ</a>',
								attributes: {'class': 'table-cell text-center'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							{field: 'NLap', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Ngày lập</a>',
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
						
							{field: 'TgTTTBSo', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tổng cộng</a>',
								attributes: {'class': 'table-cell text-right'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							{field: 'TgTCThue', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tổng tiền</a>',
								attributes: {'class': 'table-cell text-right'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							{field: 'TgTThue', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tiền thuế</a>',
								attributes: {'class': 'table-cell text-right'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},

							{field: 'MCCQT', width: '300px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã CQT</a>',
								attributes: {'class': 'table-cell text-left'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							{field: 'CQTMTLoi', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Lỗi từ CQT</a>',
								attributes: {'class': 'table-cell text-left'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
								
											
							{field: 'SignStatusDesc', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Trạng thái ký HĐ</a>',
								attributes: {'class': 'table-cell text-center'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},															
						
							{field: 'MaHD', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã HĐ</a>',
								attributes: {'class': 'table-cell text-center'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							{field: 'HVTNMHang', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người mua hàng</a>',
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
							{field: 'UserCreated', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người lập</a>',
								attributes: {'class': 'table-cell text-left text-nowrap'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							
				    	],
						dataBound: function(e) {
//							_gridMain.find('div table tbody tr td').each(function(idx, obj){
//								$(obj).attr('title', $(obj).html())
//							});
							
							$("#f-einvoices").find('button[data-action="einvoice-detail"], button[data-action="einvoice-edit"],button[data-action="einvoice-copy"], button[data-action="einvoice-sign"], button[data-action="einvoice-cre-dc-tt"]').prop('disabled', true);
							
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
									showPopupWithURLAndData(ROOT_PATH + '/main/einvoice-send-mail/init', objData, false, function(e){
									});
									break;
								case 'print':
									rowData = _gridMain.data("kendoGrid").dataItem($tr);
									window.open(ROOT_PATH + '/common/print-einvoice/' + rowData['_id'],'_blank');
									break;
									
								case 'print-convert':
									rowData = _gridMain.data("kendoGrid").dataItem($tr);
									window.open(ROOT_PATH + '/common/print-einvoice-convert/' + rowData['_id'],'_blank');
									break;
								case 'refresh':
									rowData = _gridMain.data("kendoGrid").dataItem($tr);
									objData['_id'] = rowData['_id'];
									$.ajax({
										type: "POST",
										datatype: "json",
										url: ROOT_PATH + '/main/einvoices/refresh-status-cqt',
										data: objData,
										beforeSend: function(req) {
											initAjaxJsonRequest(req);
								        	showLoading();
										},
										success:function(res) {
											hideLoading();
//											if(res) {
//												if(res.errorCode == 0) {
//													_gridMain.data("kendoGrid").dataSource.read();
//												}else{
//													alertDLSuccess(createObjectError(res).html(), function(){});
//												}
//											}else{
//												alertDLSuccess('unknown error!!!', function(){});
//												hideLoading();
//											}
//										},
											if(res) {
												if(res.errorCode == 0) {
													_gridMain.data("kendoGrid").dataSource.read();
													objURL['check'] = ROOT_PATH + '/main/einvoice-send-emailauto/check-data-send';
													objURL['exec'] = ROOT_PATH + '/main/einvoice-send-emailauto/send-mail';
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
																	tokenTransaction = responseData['TOKEN'];
																	
																	objData['tokenTransaction'] = tokenTransaction;

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
					                    objURL['check'] = ROOT_PATH + '/main/einvoice-del/check-data';
					                    objURL['exec'] = ROOT_PATH + '/main/einvoice-del/exec-data';
					                  }else{
					                    objURL['check'] = ROOT_PATH + '/main/einvoice-send-cqt/check-data';
					                    objURL['exec'] = ROOT_PATH + '/main/einvoice-send-cqt/exec-data';
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
					
					
					_gridMain.find('table[role="grid"]').find('thead').undelegate('input[type="checkbox"][data-check-all]', 'click');
					_gridMain.find('table[role="grid"]').find('thead').delegate('input[type="checkbox"][data-check-all]', 'click', function(e){
						var _obj = this;
						
						_gridMain.find('table[role="grid"] tbody input[type="checkbox"][data-check-item]').prop('checked', $(_obj).prop('checked'));
						if ($(_obj).prop('checked')) {
							_gridMain.find(' tbody tr').addClass("k-state-selected");
						}else{
							_gridMain.find(' tbody tr').removeClass("k-state-selected");
						}
						
						isDisabledEditDel();
					});
								
					
					
					
					
					
					_gridMain.find('table[role="grid"]').find('tbody').undelegate('tr', 'click');
					_gridMain.find('table[role="grid"]').find('tbody').delegate('tr', 'click', function(e){
						$("#f-einvoices").find('button[data-action="einvoice-detail"]').prop('disabled', false);
						$("#f-einvoices").find('button[data-action="einvoice-copy"]').prop('disabled', false);	
						var $tr = $(this).closest("tr");

						var rowData = _gridMain.data("kendoGrid").dataItem($tr);
						$("#f-einvoices").find('button[data-action="einvoice-delete"]').prop('disabled', 'NOSIGN' == rowData['SignStatusCode']? false: true);
						$("#f-einvoices").find('button[data-action="einvoice-sign"]').prop('disabled', 'NOSIGN' == rowData['SignStatusCode']? false: true);
						$("#f-einvoices").find('button[data-action="einvoice-edit"]').prop('disabled', 'NOSIGN' == rowData['SignStatusCode']? false: true);
						$("#f-einvoices").find('button[data-action="einvoice-signAll"]').prop('disabled', 'NOSIGN' == rowData['SignStatusCode']? false: true);

						if('Đã phát hành' == rowData['StatusDesc'] || 'Đã thay thế' == rowData['StatusDesc'] || 'Đã điều chỉnh' == rowData['StatusDesc'] || 'Đã xóa bỏ' == rowData['StatusDesc']){
				              $("#f-einvoices").find('button[data-action="einvoice-pdfAll"]').prop('disabled', false);
				              $("#f-einvoices").find('button[data-action="einvoice-xml"]').prop('disabled', false);
				            }else{
				              $("#f-einvoices").find('button[data-action="einvoice-pdfAll"]').prop('disabled', true);
				              $("#f-einvoices").find('button[data-action="einvoice-xml"]').prop('disabled', false);
				            }		
						
						
						if('2' == rowData['HDSS_TCTBao'] || '3' == rowData['HDSS_TCTBao']){
				              $("#f-einvoices").find('button[data-action="einvoice-cre-dc-tt"]').prop('disabled', false);
				            }else{
				              $("#f-einvoices").find('button[data-action="einvoice-cre-dc-tt"]').prop('disabled', true);
				            }
				            
				          });
					
					_gridMain.find('table[role="grid"]').find('thead').undelegate('input[type="checkbox"][data-check-all]', 'click');
					_gridMain.find('table[role="grid"]').find('thead').delegate('input[type="checkbox"][data-check-all]', 'click', function(e){
						var _obj = this;
						
						_gridMain.find('table[role="grid"] tbody input[type="checkbox"][data-check-item]').prop('checked', $(_obj).prop('checked'));
						if ($(_obj).prop('checked')) {
							_gridMain.find(' tbody tr').addClass("k-state-selected");
						}else{
							_gridMain.find(' tbody tr').removeClass("k-state-selected");
						}
						
						disableEnabledAllButton();
					});
	
					_gridMain.find('table[role="grid"]').find('tbody').undelegate('input[type="checkbox"][data-check-item]', 'click');
					_gridMain.find('table[role="grid"]').find('tbody').delegate('input[type="checkbox"][data-check-item]', 'click', function(e){
						var checked = $(this).prop('checked');
						if(checked){
							$(this).closest("tr").addClass("k-state-selected");
						}else{
							$(this).closest("tr").removeClass("k-state-selected");
						}
						_gridMain.find('table[role="grid"]').find('thead input[type="checkbox"]').prop('checked', _gridMain.find(' tbody tr input[type="checkbox"]:not(:checked)').length == 0);
						disableEnabledAllButton();
					});
	
					_gridMain.find('table[role="grid"]').find('tbody').undelegate('tr td:not(:eq(1))', 'click');
					_gridMain.find('table[role="grid"]').find('tbody').delegate('tr td:not(:eq(1))', 'click', function(e){
						var _obj = $(this).closest("tr");
						
						var _oldChecked = $(_obj).find('input[type=checkbox][data-check-item]').prop('checked');
						$(_obj).find('input[type=checkbox][data-check-item]').prop('checked', !_oldChecked);
						if(!_oldChecked){
							$(this).closest("tr").addClass("k-state-selected");
						}else{
							$(this).closest("tr").removeClass("k-state-selected");
						}
						_gridMain.find('table[role="grid"]').find('thead input[type="checkbox"]').prop('checked', _gridMain.find(' tbody tr input[type="checkbox"]:not(:checked)').length == 0);
						disableEnabledAllButton();
					});
					
					$("#f-einvoices").undelegate('a.download-plugin', 'click');
					$("#f-einvoices").delegate('a.download-plugin', 'click', function(event){
						event.preventDefault();/*event.stopPropagation();*/
						
						window.open(ROOT_PATH + '/main/common/download-plugin', '_blank');
					});
					
					$("#f-einvoices").find('button[data-action]').click(function (event) {
						event.preventDefault();/*event.stopPropagation();*/
						var dataAction = $(this).data('action');
						var $obj = $(this);
						var objData = {};
						///////////////////////////////////
						var tokenSignApproveWithToken = '';
							var rowsSign = null;
							var serialNumber = '';
							var checkSignStatus = false;
							var signDate = '';
						//////////////////////////////
						var rowData = null;
						var actionCheck = '|einvoice-edit|einvoice-copy|einvoice-delete|einvoice-pdfAll|einvoice-xml|einvoice-signAll|einvoice-sign|einvoice-detail|';
						var checkRows = _gridMain.find(' tbody tr input[type="checkbox"]:checked');
						var ids = null;
						var idx = -1;
						if(actionCheck.indexOf('|' + dataAction + '|') != -1 && 0 == checkRows.length){
							alertDLSuccess('<span class="required">Vui lòng chọn dòng dữ liệu để thực hiện.</span>', function(){});
							return;
						}
						switch (dataAction) {
					
						case 'einvoice-import':
							showPopupWithURLAndData(ROOT_PATH + '/main/einvoice-import/init', objData, true, function(e){
							});
							break;
						case 'einvoice-import-auto':
				              showPopupWithURLAndData(ROOT_PATH + '/main/einvoice-import-auto/init', objData, true, function(e){
				              });
				              break;
						
						case 'einvoice-delete':
							ids = [];
							checkRows.each(function(i, v) {
							    idx = $(checkRows[i].closest("tr")).index();
							    rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
							    ids.push(rowData['_id']);
							});
							
							objData = {_token: encodeObjJsonBase64UTF8(ids)};
							$.ajax({
								type: "POST",
								datatype: "json",
								url: ROOT_PATH + '/main/einvoice-deleteAll/check-data-save',
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
														url: ROOT_PATH + '/main/einvoice-deleteAll/save-data',
														data: objData,
														beforeSend: function(req) {
															initAjaxJsonRequest(req);
												        	showLoading();
														},
														success:function(res) {
															hideLoading();
															if(res) {
																if(res.errorCode == 0) {
																	if($('#f-einvoices').find('#grid').length > 0){
																		_gridMain.data("kendoGrid").dataSource.read();
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
				
						case 'einvoice-sign':
							objDataSend = {};
							idx = $(checkRows[0].closest("tr")).index();
							rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
							objDataSend['_id'] = rowData['_id'];
							$.ajax({
								type: "POST",
								datatype: "json",
								url: ROOT_PATH + '/main/einvoice-sign/check-data-sign',
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
//															        	hideLoading();
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
																						
																						var urlPost = ROOT_PATH + '/main/einvoice-sign/signFile';
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
										  objURL['check'] = ROOT_PATH + '/main/einvoice-send-cqt/check-data';
						                    objURL['exec'] = ROOT_PATH + '/main/einvoice-send-cqt/exec-data';
						               
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
											url: ROOT_PATH + '/main/einvoices/refresh-status-cqt',
											data: objDataSend,
											beforeSend: function(req) {
												initAjaxJsonRequest(req);
									        	showLoading();
											},
											success:function(res) {
												hideLoading();
											/*	if(res) {
													if(res.errorCode == 0) {
														_gridMain.data("kendoGrid").dataSource.read();
														hideLoading();
													}else{
														alertDLSuccess(createObjectError(res).html(), function(){});
													}
												}else{
													alertDLSuccess('unknown error!!!', function(){});
													hideLoading();
												}*/
												if(res) {
													if(res.errorCode == 0) {
														var objURL1 = {};
														objURL1['check'] = ROOT_PATH + '/main/einvoice-send-emailauto/check-data-send';
														objURL1['exec'] = ROOT_PATH + '/main/einvoice-send-emailauto/send-mail';
													
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
						case 'einvoice-edit':
						case 'einvoice-copy':
						case 'einvoice-detail':
							objData = {};
							idx = $(checkRows[0].closest("tr")).index();
							rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
							objData['_id'] = rowData['_id'];
							$('#divSubContent').show();$('#divMainContent').hide();
							submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
							break;
							
						
						case 'einvoice-cre-dc-tt':
							objData = {};
							idx = $(checkRows[0].closest("tr")).index();
							rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
							objData['_id'] = rowData['_id'];
							$('#divSubContent').show();$('#divMainContent').hide();
							submitFormRenderArea(ROOT_PATH + '/main/einvoice-cre/init-dc-tt', objData, $('#divSubContent'));
							break;
						case 'einvoice-cre':
							$('#divSubContent').show();$('#divMainContent').hide();
							submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
							break;
						case 'search':
							_gridMain.data("kendoGrid").dataSource.page(1);
							break;
						case 'einvoice-sign---':
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
								url: ROOT_PATH + '/main/einvoice-sign/check-data-sign',
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
//																    		var xmlText = null;
//																    		try{
//																    			xmlText = new XMLSerializer().serializeToString(data);
//															    			}catch(error){
//															    				console.log(error);
//															    				alertDLSuccess('Lỗi trong quá trình ký file.', function(){});
//															    				return;
//															    			}												    		
//																			blob = new Blob([xmlText], { type: 'octet/stream' });
																    		
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
																						
																						var urlPost = ROOT_PATH + '/main/einvoice-sign/signFile';
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
																    		
//																    		var formData = new FormData();
//																			formData.append("SerialNumber", serialNumber);
//																			formData.append("xmlFile", blob);
//																			$.ajax({
//																			 type: 'POST',
//																			    url: urlPluginSign + signXML,
//																			    contentType: "multipart/form-data",
//																			    data: formData,
//																			    processData: false,
//																			    contentType: false,
//																			    beforeSend: function(req) {
//																			    	showLoading();
//																			    },
//																			    success:function(data, textStatus, xhr) {
//																			    	hideLoading();
//																			    	
//																			    	alert('o');
//																			    },
//																				error:function (xhr, ajaxOptions, thrownError){
//																					alertDLSuccess("Lỗi: Không ký được dữ liệu hóa đơn.", function(){});
//																		            hideLoading();
//																		        }
//																			});
																    		
																    		
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
						case 'einvoice-pdfAll':
							var objData = null;
							var ids = null;
							ids = [];
							checkRows.each(function(i, v) {
							    idx = $(checkRows[i].closest("tr")).index();
							    rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
							    ids.push(rowData['_id']);
							});	
							objData =  encodeObjJsonBase64UTF8(ids);
							window.open(ROOT_PATH + '/common/print-einvoiceAll/' + objData,'_blank');
							break;
						case 'einvoice-xml':
							var objData = null;
							var ids = null;
							ids = [];
							checkRows.each(function(i, v) {
							    idx = $(checkRows[i].closest("tr")).index();
							    rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
							    ids.push(rowData['_id']);
							});	
							objData =  encodeObjJsonBase64UTF8(ids);
							window.open(ROOT_PATH + '/common/einvoiceXml/' + objData,'_blank');
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
					
					return dataPost;
				}

				function disableEnabledAllButton(){
			
					var checkRows = _gridMain.find(' tbody tr input[type="checkbox"]:checked');
					$("#f-einvoices").find('button[data-action="einvoice-pdfAll"]').prop('disabled', checkRows.length == 0);
					$('#f-einvoices').find('button[data-action="einvoice-delete"]').prop('disabled', checkRows.length == 0);
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
							text += '<i title="In hóa đơn chuyển đổi" class="mdi mdi-file-pdf fs-25 text-info c-pointer" data-sub-action="print-convert" ></i>';
							//text += '<i  title="Tự động gửi email" class="mdi mdi-email-send-outline fs-25 text-info c-pointer" id="send-emailauto" name="send-emailauto" data-sub-action="send-emailauto" ></i>';	
							text += '<i title="Gửi email hóa đơn" class="mdi mdi-gmail fs-25 text-info c-pointer" id="send-email" name="send-email" data-sub-action="send-email" ></i>';	
							
						}
						break;
					case 'StatusDesc':
						if('DELETED' == data['EInvoiceStatus']){
							text = '<div style="background: red;color: white;">' + data['StatusDesc'] + '</div>';
						}else if('REPLACED' == data['EInvoiceStatus']){
							text = '<div style="background: yellow;color: black;">' + data['StatusDesc'] + '</div>';
						}
						else if('ADJUSTED' == data['EInvoiceStatus']){
							text = '<div style="background: blue;color: white;">' + data['StatusDesc'] + '</div>';
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
			
