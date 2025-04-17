$(function(){
	_gridMain.kendoGrid({
		dataSource: new kendo.data.DataSource({
			transport: {
				read: {
					type: 'POST',
					url: ROOT_PATH + '/main/tkhai/search',
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
			pageSize: 9999,
			serverPaging: false,
			serverSorting: false,
           	serverFiltering: false,
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
			pageSizes: false,
			numeric: false,
			previousNext: false
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
  			{field: 'func', title: '', width: '40px', encoded: false
  				, headerTemplate: '&nbsp;'
				, attributes: {'class': 'table-cell', style: 'text-align: center;'}, sortable: false
				, headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
//				, template: '<i title="Lấy kết quả từ CQT" class="mdi mdi-refresh-circle fs-25 text-info c-pointer"></i>'
				, template: '#= window.setTemplateForGridMAIN("func", data) #'
			},
  			{field: 'MST', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã số thuế</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'TenNnt', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tên người nộp thuế</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'MTDiep', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã thông điệp</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'MSo', width: '130px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mẫu số</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'StatusDesc', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Trạng thái</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'NLap', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Ngày lập</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'MTa', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Chi tiết lỗi</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'Ten', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tên tờ khai</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'CoQuanThue', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Cơ quan thuế</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{
				field: 'StatusCQTDesc', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Trạng thái CQT</a>',
				attributes: { 'class': 'table-cell text-center' }, sortable: false,
				headerAttributes: { 'class': 'table-header-cell text-center' },
			},
			
    	],
    	dataBound: function(e) {
    		$("#f-tkhai").find('button[data-action="tkhai-detail"], button[data-action="tkhai-sign"], button[data-action="tkhai-edit"], button[data-action="tkhai-del"], button[data-action="tkhai-copy"]').prop('disabled', true);
    		
    		_gridMain.find('tbody[role="rowgroup"]').find('tr').undelegate('i[data-sub-action]', 'click');
    		_gridMain.find('tbody[role="rowgroup"]').find('tr').delegate('i[data-sub-action]', 'click', function(e){
    			e.preventDefault();/*e.stopPropagation();*/
				
				var $obj = $(this);
				var $tr = $obj.closest('tr');
				var subAction = $obj.attr('data-sub-action');
				
				var indexRow = $tr.index();
				var rowData = null;
				var objData = {};
				
				switch (subAction) {
				case 'delete':
					rowData = _gridMain.data("kendoGrid").dataItem($tr);
					objData['_id'] = rowData['_id'];
					$.ajax({
						type: "POST",
						datatype: "json",
						url: ROOT_PATH + '/main/tkhai-del/check-data-save',
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
												url: ROOT_PATH + '/main/tkhai-del/save-data',
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
//															$("#f-tkhai-crud").find('button[data-action="back"]').trigger('click');
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
				case 'refresh':
					rowData = _gridMain.data("kendoGrid").dataItem($tr);
					objData['_id'] = rowData['_id'];
					$.ajax({
						type: "POST",
						datatype: "json",
						url: ROOT_PATH + '/main/tkhai/refresh-status-cqt',
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
		$("#f-tkhai").find('button[data-action="tkhai-detail"]').prop('disabled', false);
		$("#f-tkhai").find('button[data-action="tkhai-copy"]').prop('disabled', false);
		
		var $tr = $(this);
		var rowData = _gridMain.data("kendoGrid").dataItem($tr);
		
		$("#f-tkhai").find('button[data-action="tkhai-sign"]').prop('disabled', !('CREATED' == rowData['Status']));
		$("#f-tkhai").find('button[data-action="tkhai-edit"]').prop('disabled', !('CREATED' == rowData['Status']));
		$("#f-tkhai").find('button[data-action="tkhai-del"]').prop('disabled', !('CREATED' == rowData['Status']));

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
	$("#f-tkhai").find('button[data-action]').click(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var dataAction = $(this).data('action');
		
		var entityGrid = _gridMain.data("kendoGrid");
		var selectedItem = entityGrid.dataItem(entityGrid.select());
		
		var actionCheck = '|tkhai-edit|tkhai-detail|tkhai-sign|tkhai-copy|';
		var checkRows = _gridMain.find(' tbody tr input[type="checkbox"]:checked');
			var ids = null;
			var idx = -1;
			if(actionCheck.indexOf('|' + dataAction + '|') != -1 && 0 == checkRows.length){
			alertDLSuccess('<span class="required">Vui lòng chọn dòng dữ liệu để thực hiện.</span>', function(){});
			return;
		}
		
		var $obj = $(this);
		var objData = {};
		switch (dataAction) {
		case 'tkhai-cre':
			$('#divSubContent').show();$('#divMainContent').hide();
			submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
			break;
		case 'tkhai-detail':
		objData = {};
			idx = $(checkRows[0].closest("tr")).index();
			rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
			objData['_id'] = rowData['_id'];
			$('#divSubContent').show();$('#divMainContent').hide();
			submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
		case 'tkhai-copy':
		case 'tkhai-edit':
			objData = {};
			idx = $(checkRows[0].closest("tr")).index();
			rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
			objData['_id'] = rowData['_id'];
			$('#divSubContent').show();$('#divMainContent').hide();
			submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
			break;
		case 'tkhai-sign':
			objData = {};
			idx = $(checkRows[0].closest("tr")).index();
			rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
			objData['_id'] = rowData['_id'];
			$('#divSubContent').show();$('#divMainContent').hide();
			submitFormRenderArea(ROOT_PATH + '/main/' + dataAction + '/init', objData, $('#divSubContent'));
			break;
		default:
			break;
		}
	});
	
});

function getDataSearch(){
	var dataPost = {};
	
	return dataPost;
}

	function disableEnabledAllButton(){
					var checkRows = _gridMain.find(' tbody tr input[type="checkbox"]:checked');
				}

function setTemplateForGridMAIN(key, data){
	var status = data['Status'];
	var text = '';
	switch (key) {
	case 'func':
		switch (status) {
		case 'PROCESSING':
			text = '<i title="Lấy kết quả từ CQT" class="mdi mdi-refresh-circle fs-25 text-info c-pointer" data-sub-action="refresh" ></i>';
			break;
		case 'CREATED':
			text = '<i title="Xóa" class="mdi mdi-account-remove-outline fs-25 text-warning c-pointer" data-sub-action="delete" ></i>';
			break;
		default:
			text = '';
			break;
		}
		
		break;
	default:
		return data[key];
		break;
	}
	return text;
	
}

