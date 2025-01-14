	$(function(){
				if(vIsEdit){
					initInputNumber('#f-agent-crud .text-number');
					dateInputFormat($('#f-agent-crud').find('#ngay-lap'));
					dateInputFormat($('#f-agent-crud').find('#kh-hd-kt-ngay'));
				}
				
				_gridSub01.kendoGrid({
					dataSource: {
						data: rowsTMP,
						pageSize: 999999,
						serverPaging: false,
						serverSorting: false,
			           	serverFiltering: false
					},
					selectable: !vIsEdit, scrollable: true, 
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
						{field: 'STT', title: 'STT', width: '80px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">STT</a>',
			  				attributes: {'class': 'table-cell', style: 'text-align: right;'}, sortable: false, 
			  				headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
//			  				, template: '<input type="text" name="STT" value="' + '#: ++record #' + '" class="input-grid k-input form-control form-control-sm text-right input-grid-number" style="border: none;" >',
			  				, template: '#= window.setTemplateForGrid("STT", data) #'
			  				
			  			},
			  			{field: 'func', title: '', width: '60px', encoded: false, hidden: !vIsEdit
//							, headerTemplate: '<label class="custom-control custom-checkbox p-l-30 m-b-0"><input type="checkbox" class="custom-control-input Check-All" data-check-all ><span class="custom-control-label"></span></label>'
			  				, headerTemplate: '&nbsp;'
							, attributes: {'class': 'table-cell', style: 'text-align: center;'}, sortable: false
							, headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
//							, template: '<label class="custom-control custom-checkbox p-l-30 m-b-3"><input type="checkbox" class="custom-control-input Check-Item" data-check-item ><span class="custom-control-label"></span></label>'
//							, template: '<i class="mdi mdi-close-box fs-25 text-black"></i>'
							, template: '#= window.setTemplateForGrid("func", data) #'
						},
						{field: 'ProductName', title: '', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tên sản phẩm , hàng hóa</a>',
							attributes: {'class': 'table-cell text-left text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
							template: '#= window.setTemplateForGrid("ProductName", data) #'
						},
						{field: 'ProductCode', title: '', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã Số</a>',
							attributes: {'class': 'table-cell text-left text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
							template: '#= window.setTemplateForGrid("ProductCode", data) #'
						},
						{field: 'Unit', title: '', width: '70px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Đơn vị<br>tính</a>',
							attributes: {'class': 'table-cell text-left text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
							template: '#= window.setTemplateForGrid("Unit", data) #'
						},
						{field: 'Quantity', title: '', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Số lượng thực xuất</a>',
							attributes: {'class': 'table-cell text-right text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
							template: '#= window.setTemplateForGrid("Quantity", data) #'
						},	
							{field: 'Quantity1', title: '', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Số lượng thực nhập</a>',
							attributes: {'class': 'table-cell text-right text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
							template: '#= window.setTemplateForGrid("Quantity1", data) #'
						},		
						{field: 'Price', title: '', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Đơn giá</a>',
							attributes: {'class': 'table-cell text-right text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
							template: '#= window.setTemplateForGrid("Price", data) #'
						},
					
						{field: 'Total', title: '', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Thành tiền</a>',
							attributes: {'class': 'table-cell text-right text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
							template: '#= window.setTemplateForGrid("Total", data) #'
						},
					
						{field: 'Amount', title: '', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tổng tiền</a>',
							attributes: {'class': 'table-cell text-right text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
							template: '#= window.setTemplateForGrid("Amount", data) #'
						},
						{field: 'Feature', title: '', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tính chất</a>',
							attributes: {'class': 'table-cell text-center text-nowrap'}, headerAttributes: {'class': 'table-header-cell text-center'},
							template: '#= window.setTemplateForGrid("Feature", data) #'
						},
					],
					dataBound: function(e) {
						initInputNumber('#f-agent-crud #grid .input-grid-number');
						
						autoCompleteProducts(_gridSub01.find('tbody[role="rowgroup"]').find('tr[data-uid]').find('input[name="ProductName"]'), function(e){
							var $tr = _gridSub01.find('tbody[role="rowgroup"]').find('tr:eq(' + rowSelect + ')');
							$tr.find('input[name="ProductCode"]').val(e.Code);
							$tr.find('input[name="Unit"]').val(e.Unit);
							$tr.find('input[name="Price"]').val(e.Price);		
							$tr.find('input[name="Quantity"]').trigger('keyup');
						});
						
//						rowSelect
						
						/*ACTION IN GRID*/
						_gridSub01.find('tbody[role="rowgroup"]').find('tr[data-uid]').undelegate('i[data-sub-action]', 'click');
						_gridSub01.find('tbody[role="rowgroup"]').find('tr[data-uid]').delegate('i[data-sub-action]', 'click', function(e){
							event.preventDefault();/*event.stopPropagation();*/
							
							var $obj = $(this);
							var $tr = $obj.closest('tr');
							var subAction = $obj.attr('data-sub-action');

							var indexRow = $tr.index();
							switch (subAction) {
							case 'delete':
								alertConfirm('Bạn có muốn xóa dòng ' + (indexRow + 1) + ' không?',
									function(e){
										var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
										if(indexRow < objDataJson.length && indexRow > -1){
											objDataJson.splice(indexRow, 1);
											_gridSub01.data("kendoGrid").dataSource.data(objDataJson);	
										}
													calcTotalAmount();
									},
									function(e){}
								)
								break;
							default:
								break;
							}
						});
						
						_gridSub01.find('tbody[role="rowgroup"]').undelegate('input[type="checkbox"][name="chkSTT"]', 'click');
						_gridSub01.find('tbody[role="rowgroup"]').delegate('input[type="checkbox"][name="chkSTT"]', 'click', function(e){
							setSTTinGrid();
								calcAmountInGrid($(this));
						});
						
						_gridSub01.find('tbody[role="rowgroup"]').undelegate('.input-grid-number, .input-grid-feature, select[name="Feature"], input[name="ProductName"], input[name="ProductCode"], input[name="Unit"]', 'change');
						_gridSub01.find('tbody[role="rowgroup"]').delegate('.input-grid-number, .input-grid-feature, select[name="Feature"], input[name="ProductName"], input[name="ProductCode"], input[name="Unit"]', 'change', function(e){
							//những sự kiện này thay đổi thì thực hiện 2 hàm ở dưới
							calcAmountInGrid($(this));
							calcTotalAmount();
						});
						
						_gridSub01.find('tbody[role="rowgroup"]').undelegate('.input-grid-number, .input-grid-feature', 'keyup');
						_gridSub01.find('tbody[role="rowgroup"]').delegate('.input-grid-number, .input-grid-feature', 'keyup', function(e){
							var code = e.keyCode || e.which;
//							console.log(code)
							//9: Tab
							//16: Shift Tab
							if(code == 9 || code == 16) return;
							calcAmountInGrid($(this));
							calcTotalAmount();
						});
						/*END - ACTION IN GRID*/
						
					}
			        
				});
				
				$('#f-agent-crud').find('button[data-action]').click(function (event) {
					event.preventDefault();/*event.stopPropagation();*/
					var dataAction = $(this).data('action');
					
					var $obj = $(this);
					var objDataSend = null;
					
					switch (dataAction) {
					case 'kh-refresh':
			$('#f-export-crud').find('#kh-mst,#kh-makhachhang,#kh-ho-ten-nguoi-mua,#kh-ten-don-vi').val('');
			$('#f-export-crud').find('#kh-dia-chi,#kh-email,#kh-emailcc,#kh-so-dt,#kh-so-tk,#kh-tk-tai-ngan-hang').val('');
			break;
		case 'kh-check-mst':
			
			var dataMST = {};				
			dataMST['mst'] = $('#f-export-crud').find('#kh-mst').val();
			var mst = dataMST['mst'];			
			$.ajax({
				type: "POST",
				datatype: "json",
				url: ROOT_PATH + '/main/export_check_mst/check-mst',
				data: dataMST,
				beforeSend: function(req) {
					initAjaxJsonRequest(req);
		        	showLoading();
				},
				success:function(res) {
					hideLoading();
					if(res) {
						if(res.errorCode == 0) {
						
							var responseData = res.responseData;								
							$('#f-export-crud').find('#kh-mst').val(mst);
							$('#f-export-crud').find('#kh-makhachhang').val(responseData['ma_kh']);
							$('#f-export-crud').find('#kh-ten-don-vi').val(responseData['tendv']);
							$('#f-export-crud').find('#kh-ho-ten-nguoi-mua').val(responseData['hvtnmh']);
							$('#f-export-crud').find('#kh-dia-chi').val(responseData['dchi']);
							$('#f-export-crud').find('#kh-email').val(responseData['email']);
							$('#f-export-crud').find('#kh-emailcc').val(responseData['emailcc']);
							$('#f-export-crud').find('#kh-so-dt').val(responseData['sdt']);
							$('#f-export-crud').find('#kh-so-tk').val(responseData['stk']);
							$('#f-export-crud').find('#kh-tk-tai-ngan-hang').val(responseData['tnh']);
						}else{
							alertDLSuccess("Không tìm thấy khách hàng", function(){});
							$('#f-export-crud').find('#kh-makhachhang,#kh-ho-ten-nguoi-mua,#kh-ten-don-vi').val('');
							$('#f-export-crud').find('#kh-dia-chi,#kh-email,#kh-emailcc,#kh-so-dt,#kh-so-tk,#kh-tk-tai-ngan-hang').val('');
						}
					}else{
						alertDLSuccess('unknown error!!!', function(){});
						hideLoading();
					}
				},
				error:function (xhr, ajaxOptions, thrownError){
					alertDLSuccess("Không tìm thấy khách hàng", function(){});
		            hideLoading();
		        }
			});
			break;		
		case 'kh-add':				
			var dataMST = {};				
			dataMST['mst'] = $('#f-export-crud').find('#kh-mst').val();	
			dataMST['kh-makhachhang'] = $('#f-export-crud').find('#kh-makhachhang').val();
			dataMST['kh-ho-ten-nguoi-mua'] = $('#f-export-crud').find('#kh-ho-ten-nguoi-mua').val();
			dataMST['kh-ten-don-vi'] = $('#f-export-crud').find('#kh-ten-don-vi').val();
			dataMST['kh-dia-chi'] = $('#f-export-crud').find('#kh-dia-chi').val();
			dataMST['kh-email'] = $('#f-export-crud').find('#kh-email').val();
			dataMST['kh-emailcc'] = $('#f-export-crud').find('#kh-emailcc').val();
			dataMST['kh-so-dt'] = $('#f-export-crud').find('#kh-so-dt').val();
			$.ajax({
				type: "POST",
				datatype: "json",
				url: ROOT_PATH + '/main/export_save_nmua/save-nmua',
				data: dataMST,
				beforeSend: function(req) {
					initAjaxJsonRequest(req);
		        	showLoading();
				},
				success:function(res) {
					hideLoading();
					if(res) {
						if(res.errorCode == 0) {
							alertDLSuccess("Cập nhật thành công", function(){});	 						
						}else{
							alertDLSuccess("Không thành công", function(){});								
						}
					}else{
						alertDLSuccess('unknown error!!!', function(){});
						hideLoading();
					}
				},
				error:function (xhr, ajaxOptions, thrownError){
					alertDLSuccess("Không thành công", function(){});
		            hideLoading();
		        }
			});
			break;		
		case 'kh_searchol':
			var dataMST = {};				
			dataMST['mst'] = $('#f-export-crud').find('#kh-mst').val();	
	       // objDataSend =$('#f-export-crud').find('#kh-mst').val();
	        $.ajax({
	          type: "POST",
	          datatype: "json",
	      	url: ROOT_PATH + '/main/export_online_mst/online-mst',
			data: dataMST,
	          beforeSend: function(req) {
	            initAjaxJsonRequest(req);
	                showLoading();
	          },
	          success:function(res) {
	      
	            hideLoading();
	            if(res) {		           
	              if(res.errorCode == 0) {
	            	var responseData = res.responseData;	  
	                $('#f-export-crud').find('#kh-ten-don-vi').val(res.responseData['Title']);
	                $('#f-export-crud').find('#kh-dia-chi').val(res.responseData['DiaChiCongTy']);
	               /*  $('#f-export-crud').find('#kh-ho-ten-nguoi-mua').val(res.responseData['ChuSoHuu']); */
	              }else{
	            		$('#f-export-crud').find('#kh-makhachhang,#kh-ho-ten-nguoi-mua,#kh-ten-don-vi').val('');
	    				$('#f-export-crud').find('#kh-dia-chi,#kh-email,#kh-emailcc,#kh-so-dt,#kh-so-tk,#kh-tk-tai-ngan-hang').val('');
	                alertDLSuccess("Không tìm thấy thông tin khách hàng!!!");
	              }
	            }else{
	            	$('#f-export-crud').find('#kh-makhachhang,#kh-ho-ten-nguoi-mua,#kh-ten-don-vi').val('');
					$('#f-export-crud').find('#kh-dia-chi,#kh-email,#kh-emailcc,#kh-so-dt,#kh-so-tk,#kh-tk-tai-ngan-hang').val('');
	              alertDLSuccess('unknown error!!!');
	              hideLoading();
	            }
	          },
	          error:function (xhr, ajaxOptions, thrownError){
	            alertDLSuccess(xhr.status + " - " + xhr.responseText, function(){});
	                  hideLoading();
	              }
	        });
	        break;	
		case 'kh_search':
			objDataSend = {};
			showPopupWithURLAndData(ROOT_PATH + '/common/show-search-customer', objDataSend, false, function(e){
				if('object' ==jQuery.type(e)){
					var mst = e['TaxCode'] == null? '': e['TaxCode'];
					if(mst.startsWith('CN') || mst.length < 10)
						mst = '';
					$('#f-export-crud').find('#kh-mst').val(mst);
					$('#f-export-crud').find('#kh-makhachhang').val(e['CustomerCode'] == null? '': e['CustomerCode']);
					$('#f-export-crud').find('#kh-ho-ten-nguoi-mua').val(e['CustomerName'] == null? '': e['CustomerName']);
					$('#f-export-crud').find('#kh-ten-don-vi').val(e['CompanyName'] == null? '': e['CompanyName']);
					$('#f-export-crud').find('#kh-dia-chi').val(e['Address'] == null? '': e['Address']);
					$('#f-export-crud').find('#kh-email').val(e['Email'] == null? '': e['Email']);
					$('#f-export-crud').find('#kh-emailcc').val(e['EmailCC'] == null? '': e['EmailCC']);
					$('#f-export-crud').find('#kh-so-dt').val(e['Phone'] == null? '': e['Phone']);
					$('#f-export-crud').find('#kh-so-tk').val(e['AccountNumber'] == null? '': e['AccountNumber']);
					$('#f-export-crud').find('#kh-tk-tai-ngan-hang').val(e['AccountBankName'] == null? '': e['AccountBankName']);
				}
			});
			break;	
					case 'add-to-grid':
						var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
						objDataJson.push({});
						_gridSub01.data("kendoGrid").dataSource.data(objDataJson);
						break;
					case 'back':
						$('#divMainContent').show();
						$('#divSubContent').hide(function(){$(this).empty();});
						try{
							if($('#f-agent').find('#grid').length > 0)
								$('#f-agent').find('#grid').data("kendoGrid").dataSource.read();
						}catch(err){}
						break;
					case 'sign':
						objDataSend = {};
						objDataSend['_id'] = $('#f-agent-crud').find('input[name="_id"]').val();
						$.ajax({
							type: "POST",
							datatype: "json",
							url: ROOT_PATH + '/main/agent-sign/check-data-sign',
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
																									$('#f-agent-crud').find('button[data-action="back"]').trigger('click');
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
																					
																					var urlPost = ROOT_PATH + '/main/agent-sign/signFile';
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
															$('#f-agent-crud').find('button[data-action="back"]').trigger('click');
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

			function setTemplateForGrid(key, data){
				if(!vIsEdit) return data[key] == null? '': data[key];
				
				var text = '';
				if('func' == key){
					text = '<div>';
					text += '<i class="mdi mdi-close-box fs-25 text-black" data-sub-action="delete"></i>';
					text += '</div>';
					return text;
				}
				
				var _valTmp = '';
				text = '<div class="form-row m-l-1 m-r-1">';
				switch (key) {
				case 'Quantity':
				case 'Price':
				case 'Total':
				case 'Amount':		
					text = '<input type="text" name="' + key + '" value="' + (null == data[key]? '': data[key]) + '" class="input-grid k-input form-control form-control-sm text-right input-grid-number" >';
					break;	
				case 'Feature':
					_valTmp = null == data[key]? '': data[key];
					if('' == _valTmp) _valTmp = '';
					
					text += '<select class="input-grid form-control form-control-sm input-grid-feature" name="' + key + '" style="height: 100%;" >';
					$.each(objFeature, function(k, v){
						text += '<option value="' + k + '" ' + (k == _valTmp? 'selected="selected" ': '') + ' >' + v + '</option>';
					});
					text += '</select>';
					break;
				case 'STT':
					text += '<div class="input-group">';
					text += '	<div class="input-group-prepend">';		
					text += '		<label class="custom-control custom-checkbox">';
					text += '			<input type="checkbox" name="chkSTT" class="custom-control-input" ' + (null != data[key] && '' != data[key]? 'checked="checked"': '') + ' >';
					text += '			<span class="custom-control-label">&nbsp;</span>';
					text += '		</label>';		
					text += '	</div>';
					text += '	<input type="text" name="' + key + '" value="' + (null == data[key]? '': data[key]) + '" class="input-grid k-input form-control form-control-sm text-right" style="border: none;" >';
					text += '</div>';
					break;
				default:
					text = '<input type="text" name="' + key + '" value="' + (null == data[key]? '': data[key]) + '" class="input-grid k-input form-control form-control-sm" >';
					break;
				}
				text += '</div>';
				return text;
			}

			function calcAmountInGrid($obj){
				var $tr = $obj.closest('tr');
				var _name = $obj.prop('name');
				var _val = '';
				var _numeral = null;
				var loai_tien_tt = $('#f-agent-crud').find('#loai-tien-tt').val();
				
				var dblQuantity = 0;
				var dblPrice = 0;
				var dblTotal = 0;
				var dblAmount = 0;
				
				switch (_name) {
				case 'Quantity':	
					_numeral = numeral($tr.find('input[name="price"]').val());
					dblPrice = _numeral == null? 0: _numeral.value();
				case 'Price':
					_numeral = numeral($tr.find('input[name="Quantity"]').val());
					dblQuantity = _numeral == null? 0: _numeral.value();
					_numeral = numeral($tr.find('input[name="Price"]').val());
					dblPrice = _numeral == null? 0: _numeral.value();
					
					
					dblTotal = dblQuantity * dblPrice;
					$tr.find('input[name="Total"]').val(dblTotal.toFixed(4))
					FormatCurrency($tr.find('input[name="Total"]')[0], loai_tien_tt);
					
					break;
				case 'Total':
					$tr.find('input[name="Quantity"],input[name="Price"]').val('');
					
					_numeral = numeral($tr.find('input[name="Total"]').val());
					dblTotal = _numeral == null? 0: _numeral.value();
					
					break;
				
				
				case 'Feature':
					$tr.find('input[name="Unit"], input[name="Quantity"], input[name="Price"], input[name="Total"], input[name="Amount"]').prop('readonly', false);
					
					_val = $obj.val();
					switch (_val) {
					case '1':
						$tr.find('input[name="Amount"]').val('');
						break;
					case '4':
						$tr.find('input[name="Unit"], input[name="Quantity"], input[name="Price"]').val('');
						$tr.find('input[name="Total"]').val('');
						
						$tr.find('input[name="Unit"], input[name="Quantity"], input[name="Price"], input[name="Total"], input[name="Amount"]').prop('readonly', true);
						
						
						break;
					default:
						break;
					}
					break;
				case 'ProductName':
					_val = $obj.val();
					if('' == _val){
						$tr.find('input[type="checkbox"][name="chkSTT"]').prop('checked', false);
					}else{
						$tr.find('input[type="checkbox"][name="chkSTT"]').prop('checked', true);
					}
					setSTTinGrid();
					break;
				default:
					break;
				}
				
				_numeral = numeral($tr.find('input[name="Total"]').val());
				dblTotal = _numeral == null? 0: _numeral.value();
				
				dblAmount = dblTotal+ 0;
				$tr.find('input[name="Amount"]').val(dblAmount.toFixed(4))
				FormatCurrency($tr.find('input[name="Amount"]')[0], loai_tien_tt);
				
				/*LAY THONG TIN UPDATE LAI DATASOURCE*/
				var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
				try{
					var indexRow = $tr.index();
					var rowData = objDataJson[indexRow];
					
					rowData['STT'] = $tr.find('input[name="STT"]').val();
					rowData['ProductName'] = $tr.find('input[name="ProductName"]').val();
					rowData['ProductCode'] = $tr.find('input[name="ProductCode"]').val();
					rowData['Unit'] = $tr.find('input[name="Unit"]').val();
					rowData['Quantity'] = $tr.find('input[name="Quantity"]').val();
					rowData['Price'] = $tr.find('input[name="Price"]').val();
					rowData['Total'] = $tr.find('input[name="Total"]').val();
					rowData['Amount'] = $tr.find('input[name="Amount"]').val();
					rowData['Feature'] = $tr.find('select[name="Feature"]').val();
					
					_gridSub01.data("kendoGrid").dataSource.data()[indexRow] = rowData;
				}catch(err){
					console.log(err);
				}
			}

			function setSTTinGrid(){
				var $tr = null;
				var stt = 0;
				_gridSub01.find('table[role="grid"]').find('tbody[role="rowgroup"]').find('tr').each(function(i, v) {
					$tr = $(this);
					if($tr.find('input[type="checkbox"][name="chkSTT"]').prop('checked')
						&& $tr.find('input[type="text"][name="ProductName"]').val() != ''
					){
						$tr.find('input[type="text"][name="STT"]').val(++stt);
					
					}else{
						$tr.find('input[type="text"][name="STT"]').val('');
					}
				});
			}

			function calcTotalAmount(){
				var loai_tien_tt = $('#f-agent-crud').find('#loai-tien-tt').val();
				var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
				
				/*TINH TIEN TRUOC THUE - TIEN THUE - TONG TIEN SAU THUE*/
				var sumAmount = 0;
				var sumAmountVAT = 0;
				var sumAmountAfterTax = 0;
				var tmp = '';
				var tmp2 = '';
				jQuery.each(objDataJson, function(index, item) {
					tmp = item['ProductName'] == null? '': item['ProductName'].trim();
					if('' != tmp){
						_val = item['Feature'] == null? '': item['Feature'].trim();
						_numeral = numeral(item['Total']);
						sumAmount += _numeral.value() == null? 0: _numeral.value();
						
						switch (_val) {
						case '4':		//GHI CHU - KHONG XU LY TIEN
							break;
						case '3':		//CK: TRU TIEN THUE VA TONG TIEN
				
							_numeral = numeral(item['Amount']);
							sumAmountAfterTax -= _numeral.value() == null? 0: _numeral.value();
							break;
						case '2':		//KM: CHI CONG TONG TIEN
							break;
						default:
				
							_numeral = numeral(item['Amount']);
							sumAmountAfterTax += _numeral.value() == null? 0: _numeral.value();
							break;
						}
					}
				});
				
				$('#f-agent-crud').find('#tong-tien-truoc-thue').val(sumAmount.toFixed(4))
				FormatCurrency($('#f-agent-crud').find('#tong-tien-truoc-thue')[0], loai_tien_tt);
				$('#f-agent-crud').find('#tong-tien-thue-gtgt').val(sumAmountVAT.toFixed(4))
				FormatCurrency($('#f-agent-crud').find('#tong-tien-thue-gtgt')[0], loai_tien_tt);
				$('#f-agent-crud').find('#tong-tien-da-co-thue').val(sumAmountAfterTax.toFixed(4))
				FormatCurrency($('#f-agent-crud').find('#tong-tien-da-co-thue')[0], loai_tien_tt);
				
				$('#f-agent-crud').find('#tien-bang-chu').val(readMoneyInWords(sumAmountAfterTax.toFixed(4), loai_tien_tt));
			}



			function getDataToSave(){
				var dataPost = {};
				
				dataPost['_id'] = $('#f-agent-crud').find('input[name="_id"]').val();
				dataPost['mau-so-hdon'] = $('#f-agent-crud').find('#mau-so-hdon').val();	
				dataPost['ten-loai-hd'] = $('#f-agent-crud').find('#ten-loai-hd').val();
				dataPost['hinh-thuc-thanh-toan'] = $('#f-agent-crud').find('#hinh-thuc-thanh-toan').val();
				dataPost['hinh-thuc-thanh-toan-text'] = $('#f-agent-crud').find('#hinh-thuc-thanh-toan').find('option:selected').text();
				dataPost['ngay-lap'] = $('#f-agent-crud').find('#ngay-lap').val();
				
				dataPost['kh-mst'] = $('#f-agent-crud').find('#kh-mst').val();
				dataPost['kh-ho-ten-ng-xuat'] = $('#f-agent-crud').find('#kh-ho-ten-ng-xuat').val();
					dataPost['nban-dchi'] = $('#f-agent-crud').find('#nban-dchi').val();
				
				dataPost['kh-ho-ten-ng-vc'] = $('#f-agent-crud').find('#kh-ho-ten-ng-vc').val();
				dataPost['kh-ho-ten-ng-dd'] = $('#f-agent-crud').find('#kh-ho-ten-ng-dd').val();
				dataPost['kh-ve-viec'] = $('#f-agent-crud').find('#kh-ve-viec').val();
				dataPost['kh-ten-don-vi'] = $('#f-agent-crud').find('#kh-ten-don-vi').val();
				dataPost['kh-dia-chi'] = $('#f-agent-crud').find('#kh-dia-chi').val();
				
				dataPost['kh-hd-kt-so'] = $('#f-agent-crud').find('#kh-hd-kt-so').val();
				dataPost['kh-hd-kt-ngay'] = $('#f-agent-crud').find('#kh-hd-kt-ngay').val();
				dataPost['kh-pt-vc'] = $('#f-agent-crud').find('#kh-pt-vc').val();
				dataPost['kh-email'] = $('#f-agent-crud').find('#kh-email').val();
				dataPost['kh-so-dt'] = $('#f-agent-crud').find('#kh-so-dt').val();
				dataPost['kh-so-tk'] = $('#f-agent-crud').find('#kh-so-tk').val();
				dataPost['kh-tk-tai-ngan-hang'] = $('#f-agent-crud').find('#kh-tk-tai-ngan-hang').val();
				
				dataPost['tong-tien-truoc-thue'] = $('#f-agent-crud').find('#tong-tien-truoc-thue').val();
				dataPost['loai-tien-tt'] = $('#f-agent-crud').find('#loai-tien-tt').val();
				dataPost['ty-gia'] = $('#f-agent-crud').find('#ty-gia').val();
				dataPost['tong-tien-thue-gtgt'] = $('#f-agent-crud').find('#tong-tien-thue-gtgt').val();
				dataPost['tong-tien-da-co-thue'] = $('#f-agent-crud').find('#tong-tien-da-co-thue').val();
				dataPost['tien-bang-chu'] = $('#f-agent-crud').find('#tien-bang-chu').val();
				
				var arrRows = [];
				var objDataJson = _gridSub01.data("kendoGrid").dataSource.data();
				jQuery.each(objDataJson, function(index, item) {
					tmp = item['ProductName'] == null? '': item['ProductName'].trim();
					if('' != tmp){
						arrRows.push(item);
					}
				});
				dataPost['ds-san-pham'] = encodeObjJsonBase64UTF8(arrRows);
				
				
				return dataPost;
			}
