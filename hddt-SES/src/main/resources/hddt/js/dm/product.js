$(function(){
	_gridMain.kendoGrid({
		dataSource: new kendo.data.DataSource({
			transport: {
				read: {
					type: 'POST',
					url: ROOT_PATH + '/main/dmproduct/search',
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
		selectable: false, scrollable: true, 
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
  			{field: 'isCheck', title: '', width: '60px', encoded: false
				, headerTemplate: '<label class="custom-control custom-checkbox p-l-30 m-b-0"><input type="checkbox" class="custom-control-input Check-All" data-check-all ><span class="custom-control-label"></span></label>'
				, attributes: {'class': 'table-cell', style: 'text-align: center;'}, sortable: false
				, headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
				, template: '<label class="custom-control custom-checkbox p-l-30 m-b-3"><input type="checkbox" class="custom-control-input Check-Item" data-check-item ><span class="custom-control-label"></span></label>'
			},
  			{field: 'Code', width: '130px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mẫu sản phẩm</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'Name', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tên sản phẩm</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'Stock', width: '80px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã kho</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'Slsx', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Số lô SX</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'Price', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Đơn giá</a>',
				attributes: {'class': 'table-cell text-right'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'Unit', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Đơn vị tính</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'VatRate', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Thuế VAT</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'UserCreated', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người lập</a>',
				attributes: {'class': 'table-cell text-left text-nowrap'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'DateCreated', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Ngày lập</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'UserUpdated', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người cập nhật</a>',
				attributes: {'class': 'table-cell text-left text-nowrap'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'DateUpdated', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Ngày cập nhật</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
				{field: 'thdoi_tonkho', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Hạn sử dụng</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
    	],
    	dataBound: function(e) {
			_gridMain.find('table[role="grid"]').find('thead input[type="checkbox"]').prop('checked', 
				_gridMain.find(' tbody tr input[type="checkbox"]:not(:checked)').length > 0 
				&& _gridMain.find(' tbody tr input[type="checkbox"]:not(:checked)').length == 0
			);
			isDisabledEditDel();
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
	
	_gridMain.find('table[role="grid"]').find('tbody').undelegate('input[type="checkbox"][data-check-item]', 'click');
	_gridMain.find('table[role="grid"]').find('tbody').delegate('input[type="checkbox"][data-check-item]', 'click', function(e){
		var checked = $(this).prop('checked');
		if(checked){
			$(this).closest("tr").addClass("k-state-selected");
		}else{
			$(this).closest("tr").removeClass("k-state-selected");
		}
		_gridMain.find('table[role="grid"]').find('thead input[type="checkbox"]').prop('checked', _gridMain.find(' tbody tr input[type="checkbox"]:not(:checked)').length == 0);
		isDisabledEditDel();
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
		isDisabledEditDel();
	});
	
	$("#f-prd").find('button[data-action]').click(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var dataAction = $(this).data('action');
		
		var $obj = $(this);
		var objData = {};
		
		var rowData = null;
		var actionCheck = '|prd-detail|prd-edit|prd-delete|prd-copy|';
		var grid = _gridMain.data("kendoGrid");
		var checkRows = _gridMain.find(' tbody tr input[type="checkbox"]:checked');
		var ids = null;
		var idx = -1;
		if(actionCheck.indexOf('|' + dataAction + '|') != -1 && 0 == checkRows.length){
			alertDLSuccess('<span class="required">Vui lòng chọn dòng dữ liệu để thực hiện.</span>', function(){});
			return;
		}
		
		switch (dataAction) {
		case 'search':
			_gridMain.data("kendoGrid").dataSource.page(1);
			break;
		case 'prd-import':
			showPopupWithURLAndData(ROOT_PATH + '/main/dmproduct-import/init', objData, true, function(e){
			});
			break;
		case 'prd-exp':
			showPopupWithURLAndData(ROOT_PATH + '/main/dmproduct-exp/choose-type-export', objData, true, function(e){
				if(typeof e == 'object'){
					$.ajax({
						type: "POST",
						datatype: "json",
						url: ROOT_PATH + '/main/dmproduct-exp/check-data-export',
						data: e,
						beforeSend: function(req) {
							initAjaxJsonRequest(req);
				        	showLoading();
						},
						success:function(res) {
							hideLoading();
							if(res.errorCode == 0) {
								var responseData = res.responseData;
								
								tokenTransaction = responseData['TOKEN'];
								window.open(ROOT_PATH + '/main/dmproduct-exp/export-excel/' + tokenTransaction,'_blank');
							}else{
								alertDLSuccess(createObjectError(res).html(), function(){});
							}
						},
						error:function (xhr, ajaxOptions, thrownError){
							alertDLSuccess(xhr.status + " - " + xhr.responseText, function(){});
				            hideLoading();
				        }
					});
				}
			});
			break;
		case 'prd-cre':
			showPopupWithURLAndData(ROOT_PATH + '/main/dmproduct-cre/init', objData, true, function(e){
			});
			break;
		case 'prd-copy':
			objData = {};
			idx = $(checkRows[0].closest("tr")).index();
			rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
			
			objData['_id'] = rowData['_id'];
			showPopupWithURLAndData(ROOT_PATH + '/main/dmproduct-cre/init', objData, true, function(e){
			});
			break;
		case 'prd-detail':
			objData = {};
			idx = $(checkRows[0].closest("tr")).index();
			rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
			
			objData['_id'] = rowData['_id'];
			showPopupWithURLAndData(ROOT_PATH + '/main/dmproduct-detail/init', objData, true, function(e){
			});
			break;
		case 'prd-edit':
			objData = {};
			idx = $(checkRows[0].closest("tr")).index();
			rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
			
			objData['_id'] = rowData['_id'];
			showPopupWithURLAndData(ROOT_PATH + '/main/dmproduct-edit/init', objData, true, function(e){
			});
			break;
		case 'prd-delete':
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
				url: ROOT_PATH + '/main/dmproduct-del/check-data-save',
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
										url: ROOT_PATH + '/main/dmproduct-del/save-data',
										data: objData,
										beforeSend: function(req) {
											initAjaxJsonRequest(req);
								        	showLoading();
										},
										success:function(res) {
											hideLoading();
											if(res) {
												if(res.errorCode == 0) {
													if($('#f-prd').find('#grid').length > 0){
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
		default:
			break;
		}
	});
	
});

function getDataSearch(){
	var dataPost = {};
	
	dataPost['code'] = $('#f-prd #code').val() == null? '': $('#f-prd #code').val();
	dataPost['name'] = $('#f-prd #name').val() == null? '': $('#f-prd #name').val();
	dataPost['stock'] = $('#f-prd #stock').val() == null? '': $('#f-prd #stock').val();
	
	return dataPost;
}

function isDisabledEditDel(){
	var checkRows = _gridMain.find(' tbody tr input[type="checkbox"]:checked');
	$('#f-prd').find('button[data-action="prd-edit"],button[data-action="prd-detail"],button[data-action="prd-copy"]').prop('disabled', checkRows.length != 1);
	$('#f-prd').find('button[data-action="prd-delete"]').prop('disabled', checkRows.length == 0);
}