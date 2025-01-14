
		$(function(){
			dateInputFormat($('#f-quantity').find('#from-date'));
			dateInputFormat($('#f-quantity').find('#to-date'));
			
			_gridMain.kendoGrid({
				dataSource: new kendo.data.DataSource({
					transport: {
						read: {
							type: 'POST',
							url: ROOT_PATH + '/main/quantitys/search',
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
//		 		filterable: { mode: "row"},
				filterable: false, resizable: true,
				serverSorting: false,
//				height: kendoGridHeight,
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
		  			{field: 'func', title: '', width: '100px', encoded: false
		  				, headerTemplate: '&nbsp;'
						, attributes: {'class': 'table-cell', style: 'text-align: left;'}, sortable: false
						, headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
						, template: '#= window.setTemplateForGridMAIN("func", data) #'
					},
				
					{field: 'KHMSHDon', width: '180px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Kí hiệu mẫu số HD</a>',
						attributes: {'class': 'table-cell text-center'}, sortable: false, 
						headerAttributes: {'class': 'table-header-cell text-center'},
					},
					{field: 'Quantity', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Số lượng</a>',
						attributes: {'class': 'table-cell text-left'}, sortable: false, 
						headerAttributes: {'class': 'table-header-cell text-center'},
					},
					{field: 'TSo', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Từ số</a>',
						attributes: {'class': 'table-cell text-left'}, sortable: false, 
						headerAttributes: {'class': 'table-header-cell text-center'},
					},
					{field: 'DSo', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Đến số</a>',
						attributes: {'class': 'table-cell text-center'}, sortable: false, 
						headerAttributes: {'class': 'table-header-cell text-center'},
					},
					{field: 'CLai', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Còn lại</a>',
						attributes: {'class': 'table-cell text-center'}, sortable: false, 
						headerAttributes: {'class': 'table-header-cell text-center'},
					},
					{field: 'FileName', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tên mẫu</a>',
						attributes: {'class': 'table-cell text-center'}, sortable: false, 
						headerAttributes: {'class': 'table-header-cell text-center'},
					},
					{field: 'IsActive', width: '140px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Trạng thái phát hành</a>',
						attributes: {'class': 'table-cell text-center'}, sortable: false, 
						headerAttributes: {'class': 'table-header-cell text-center'},
						
					},
		    	],
				dataBound: function(e) {
//					_gridMain.find('div table tbody tr td').each(function(idx, obj){
//						$(obj).attr('title', $(obj).html())
//					});
					
					$("#f-quantity").find('button[data-action="quantity-detail"], button[data-action="quantity-edit"]').prop('disabled', true);
					
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
						case 'delete':
						case 'deactive':
						case 'active':					
							if('delete' == subAction){
								objURL['check'] = ROOT_PATH + '/main/quantity-del/check-data';
								objURL['exec'] = ROOT_PATH + '/main/quantity-del/exec-data';
							}else if('active' == subAction){
								objURL['check'] = ROOT_PATH + '/main/quantity-active/check-data';
								objURL['exec'] = ROOT_PATH + '/main/quantity-active/exec-data';
							}
							else {
								objURL['check'] = ROOT_PATH + '/main/quantity-deactive/check-data';
								objURL['exec'] = ROOT_PATH + '/main/quantity-deactive/exec-data';
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
				$("#f-quantity").find('button[data-action="quantity-detail"]').prop('disabled', false);		
				$("#f-quantity").find('button[data-action="quantity-edit"]').prop('disabled', false);		
				var $tr = $(this).closest("tr");
				var rowData = _gridMain.data("kendoGrid").dataItem($tr);
				
			});
			

			$("#f-quantity").undelegate('a.download-plugin', 'click');
			$("#f-quantity").delegate('a.download-plugin', 'click', function(event){
				event.preventDefault();/*event.stopPropagation();*/
				
				window.open(ROOT_PATH + '/main/common/download-plugin', '_blank');
			});
			
			$("#f-quantity").find('button[data-action]').click(function (event) {
				event.preventDefault();/*event.stopPropagation();*/
				var dataAction = $(this).data('action');
				
				var $obj = $(this);
				
				var rowData = null;
				var actionCheck = '|quantity-edit|quantity-detail|';

				
				var entityGrid = _gridMain.data("kendoGrid");
				var selectedItem = entityGrid.dataItem(entityGrid.select());
				if(actionCheck.indexOf('|' + dataAction + '|') != -1 && selectedItem == null){
					alertDLSuccess('<span class="required">Vui lòng chọn dòng dữ liệu để thực hiện.</span>', function(){});
					return;
				}
				
				var objData = {};
				switch (dataAction) {
				case 'quantity-edit':
					objData['_id'] = selectedItem['_id'];
					$('#divSubContent').show();$('#divMainContent').hide();
					submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
					break;
				case 'quantity-detail':
					objData['_id'] = selectedItem['_id'];
					$('#divSubContent').show();$('#divMainContent').hide();
					submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
					break;
				case 'quantity-cre':
					$('#divSubContent').show();$('#divMainContent').hide();
					submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
					break;
				case 'search':
					_gridMain.data("kendoGrid").dataSource.page(1);
					break;		
				default:
					break;
				}
			});
			
		});

		function getDataSearch(){
			var dataPost = {};
			
			dataPost['mau-so-hdon'] = $('#f-quantity #mau-so-hdon').val() == null? '': $('#f-quantity #mau-so-hdon').val();
			dataPost['from-date'] = $('#f-quantity #from-date').val() == null? '': $('#f-quantity #from-date').val();
			dataPost['to-date'] = $('#f-quantity #to-date').val() == null? '': $('#f-quantity #to-date').val();
				
			return dataPost;
		}

		function disableEnabledAllButton(){
			var checkRows = _gridMain.find(' tbody tr input[type="checkbox"]:checked');

		}

		function setTemplateForGridMAIN(key, data){
			var signStatusCode = data['SignStatusCode'];
			var eInvoiceStatus = data['EInvoiceStatus'];
			var MCCQT = data['MCCQT'] == null? '': data['MCCQT'];
			var text = '';
			var acti = data['IsActive'];
			switch (key) {
			case 'func':
				if('Chưa Hoạt động' == acti){
					text += '<i title="Kích hoạt" class="mdi mdi-checkbox-marked-circle-outline fs-25 text-blue123 c-pointer" data-sub-action="active" ></i>';						
					
			/* 		text += '<i title="Xóa" class="mdi mdi-close-box fs-25 text-danger c-pointer" data-sub-action="delete" ></i>';		
			 */	}
				else{
					text += '<i title="Hủy kích hoạt" class="mdi mdi-close-circle fs-25 text-blue123 c-pointer" data-sub-action="deactive" ></i>';						
				}
				break;
			default:
				break;
			}
			
			return text;
		}