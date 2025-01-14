$(function(){
	_gridMain.kendoGrid({
		dataSource: new kendo.data.DataSource({
			transport: {
				read: {
					type: 'POST',
					url: ROOT_PATH + '/main/dmcustomer/search',
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
			{field: 'TaxCode', width: '130px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã số thuế</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'CustomerCode', width: '130px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã khách hàng</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'CompanyName', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tên đơn vị</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'CustomerName', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người mua hàng</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'Address', width: '250px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Địa chỉ</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'Email', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Email</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'EmailCC', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">EmailCC</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'ProvinceName', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Tỉnh/Thành phố</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'CustomerGroup1Name', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Nhóm KH 1</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'CustomerGroup2Name', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Nhóm KH 2</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'CustomerGroup3Name', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Nhóm KH 3</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'UserCreated', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người lập</a>',
				attributes: {'class': 'table-cell text-left text-nowrap'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'DateCreated', width: '120px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Ngày lập</a>',
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

	$("#f-customer").find('button[data-action]').click(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var dataAction = $(this).data('action');
		
		var $obj = $(this);
		var objData = {};
		
		var rowData = null;
		var actionCheck = '|detail|';
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
		case 'import':
			showPopupWithURLAndData(ROOT_PATH + '/main/dmcustomer-import/init', objData, true, function(e){
			});
			break;
		case 'cre':
			showPopupWithURLAndData(ROOT_PATH + '/main/dmcustomer-cre/init', objData, true, function(e){
			});
			break;
		case 'detail':
			objData = {};
			idx = $(checkRows[0].closest("tr")).index();
			rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
			
			objData['_id'] = rowData['_id'];
			showPopupWithURLAndData(ROOT_PATH + '/main/dmcustomer-detail/init', objData, true, function(e){
			});
			break;
		case 'edit':
			objData = {};
			idx = $(checkRows[0].closest("tr")).index();
			rowData = _gridMain.data("kendoGrid").dataItem(_gridMain.find(' tbody tr').eq(idx));
			
			objData['_id'] = rowData['_id'];
			showPopupWithURLAndData(ROOT_PATH + '/main/dmcustomer-edit/init', objData, true, function(e){
			});
			break;
		case 'delete':
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
				url: ROOT_PATH + '/main/dmcustomer-del/check-data-save',
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
										url: ROOT_PATH + '/main/dmcustomer-del/save-data',
										data: objData,
										beforeSend: function(req) {
											initAjaxJsonRequest(req);
								        	showLoading();
										},
										success:function(res) {
											hideLoading();
											if(res) {
												if(res.errorCode == 0) {
													if($('#f-customer').find('#grid').length > 0){
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
	
	dataPost['tax-code'] = $('#f-customer #tax-code').val() == null? '': $('#f-customer #tax-code').val();
	dataPost['company-name'] = $('#f-customer #company-name').val() == null? '': $('#f-customer #company-name').val();
	dataPost['customer-name'] = $('#f-customer #customer-name').val() == null? '': $('#f-customer #customer-name').val();
	
	return dataPost;
}

function isDisabledEditDel(){
	var checkRows = _gridMain.find(' tbody tr input[type="checkbox"]:checked');
	$('#f-customer').find('button[data-action="edit"],button[data-action="detail"]').prop('disabled', checkRows.length != 1);
	$('#f-customer').find('button[data-action="delete"]').prop('disabled', checkRows.length == 0);
}