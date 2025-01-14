				$(function(){
					_gridMain.kendoGrid({
						dataSource: new kendo.data.DataSource({
							transport: {
								read: {
									type: 'POST',
									url: ROOT_PATH + '/main/issu/search',
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
						filterable: false, resizable: true,
						serverSorting: false,
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
				  			
							{field: 'Name', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tên</a>',
								attributes: {'class': 'table-cell text-center'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							{field: 'Address', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Địa Chỉ</a>',
								attributes: {'class': 'table-cell text-center'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							{field: 'Phone', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Điện Thoại</a>',
								attributes: {'class': 'table-cell text-center'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
					
							{field: 'TaxCode', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã số thuế</a>',
								attributes: {'class': 'table-cell text-left'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							{field: 'IsActive', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Trạng Thái</a>',
								attributes: {'class': 'table-cell text-left'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							{field: 'UserCreated', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người lập</a>',
								attributes: {'class': 'table-cell text-left'}, sortable: false, 
								headerAttributes: {'class': 'table-header-cell text-center'},
							},
							
				    	],
						dataBound: function(e) {
							
							$("#f-issu").find('button[data-action="issu-detail"], button[data-action="issu-edit"]').prop('disabled', true);
							
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
				
								case 'refresh':
									rowData = _gridMain.data("kendoGrid").dataItem($tr);
									objData['_id'] = rowData['_id'];
									$.ajax({
										type: "POST",
										datatype: "json",
										url: ROOT_PATH + '/main/issu/refresh-status-cqt',
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
								case 'delete':
								case 'send-cqt':					
									if('delete' == subAction){
										objURL['check'] = ROOT_PATH + '/main/issu-del/check-data';
										objURL['exec'] = ROOT_PATH + '/main/issu-del/exec-data';
									}else{
										objURL['check'] = ROOT_PATH + '/main/issu-send-cqt/check-data';
										objURL['exec'] = ROOT_PATH + '/main/issu-send-cqt/exec-data';
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
						$("#f-issu").find('button[data-action="issu-detail"]').prop('disabled', false);		
						$("#f-issu").find('button[data-action="issu-edit"]').prop('disabled', false);	
				});
					
					$("#f-issu").find('button[data-action]').click(function (event) {
						event.preventDefault();/*event.stopPropagation();*/
						var dataAction = $(this).data('action');
						
						var $obj = $(this);
						
						var rowData = null;
						var actionCheck = '|issu-edit|issu-sign|issu-detail|';
						
						var entityGrid = _gridMain.data("kendoGrid");
						var selectedItem = entityGrid.dataItem(entityGrid.select());
						if(actionCheck.indexOf('|' + dataAction + '|') != -1 && selectedItem == null){
							alertDLSuccess('<span class="required">Vui lòng chọn dòng dữ liệu để thực hiện.</span>', function(){});
							return;
						}
						
						var objData = {};
						switch (dataAction) {
						case 'issu-sign':
							objData['_id'] = selectedItem['_id'];
							$('#divSubContent').show();$('#divMainContent').hide();
							submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
							break;
						case 'issu-edit':
						case 'issu-detail':
							objData['_id'] = selectedItem['_id'];
							$('#divSubContent').show();$('#divMainContent').hide();
							submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
							break;
						case 'issu-cre':
							$('#divSubContent').show();$('#divMainContent').hide();
							submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
							break;
						case 'search':
							_gridMain.data("kendoGrid").dataSource.page(1);
							break;
						case 'issu-sign---':
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
								url: ROOT_PATH + '/main/issu-sign/check-data-sign',
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
																						
																						var urlPost = ROOT_PATH + '/main/issu-sign/signFile';
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
					
					dataPost['mau-so-hdon'] = $('#f-issu #mau-so-hdon').val() == null? '': $('#f-issu #mau-so-hdon').val();
					dataPost['so-hoa-don'] = $('#f-issu #so-hoa-don').val() == null? '': $('#f-issu #so-hoa-don').val();
					dataPost['status'] = $('#f-issu #status').val() == null? '': $('#f-issu #status').val();
					dataPost['nban-mst'] = $('#f-issu #nban-mst').val() == null? '': $('#f-issu #nban-mst').val();
					dataPost['nban-ten'] = $('#f-issu #nban-ten').val() == null? '': $('#f-issu #nban-ten').val();
					
					return dataPost;
				}

				function disableEnabledAllButton(){
					var checkRows = _gridMain.find(' tbody tr input[type="checkbox"]:checked');
					$("#f-issu").find('button[data-action="issu-sign"]').prop('disabled', checkRows.length == 0);
					}

				function setTemplateForGridMAIN(key, data){
					var signStatusCode = data['SignStatusCode'];
					var eInvoiceStatus = data['EInvoiceStatus'];
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
						text += '<i title="In hóa đơn" class="mdi mdi-printer fs-25 text-black c-pointer" data-sub-action="print" ></i>';
						break;
					case 'StatusDesc':
						if('DELETED' == data['EInvoiceStatus']){
							text = '<div style="background: red;color: white;">' + data['StatusDesc'] + '</div>';
						}else{
							text = data['StatusDesc'];
						}
						break;
					default:
						break;
					}
					
					
					return text;
				}