$(function(){
	if($('#f-search').find('#from-date').length > 0)
		dateInputFormat($('#f-search').find('#from-date'));
	if($('#f-search').find('#to-date').length > 0)
		dateInputFormat($('#f-search').find('#to-date'));
	
	_gridMainSearch.kendoGrid({
		dataSource: new kendo.data.DataSource({
			transport: {
				read: {
					type: 'POST',
					url: ROOT_PATH + '/common/list-einvoice_mtt-signed',
                    dataType: 'json',
                    data: function(){return getDataSearchPopup();},
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
  			{field: 'isCheck', title: '', width: '40px', encoded: false, headerTemplate: '<input type="checkbox" class="Check-All checkbox-in-grid"/>',
  				attributes: {'class': 'table-cell', style: 'text-align: center;'}, sortable: false, 
  				headerAttributes: {'class': 'table-header-cell', style: 'text-align: center;',}
  				, template:'<input type="checkbox" class="Check-Item checkbox-in-grid"/>'
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
  			{field: 'MCCQT', width: '300px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Mã CQT</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
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
			{field: 'HVTNMHang', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người mua hàng</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'UserCreated', width: '150px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Người lập</a>',
				attributes: {'class': 'table-cell text-left text-nowrap'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
		]
	});
	
	_gridMainSearch.find('table[role="grid"]').find('thead[role="rowgroup"]').undelegate('input[type="checkbox"].Check-All', 'click');
	_gridMainSearch.find('table[role="grid"]').find('thead[role="rowgroup"]').delegate('input[type="checkbox"].Check-All', 'click', function(e){
		var $obj = $(this);
		var isCheck = $obj.prop('checked');
		
		_gridMainSearch.find('div.k-grid-content tbody input[type="checkbox"].Check-Item').prop('checked', isCheck);
		var arrRow = _gridMainSearch.find('table[role="grid"]').find('tbody tr').find("input[type='checkbox'].Check-Item");
		if(isCheck){
			_gridMainSearch.find('table[role="grid"]').find('tbody tr').addClass("k-state-selected")
		}else{
			_gridMainSearch.find('table[role="grid"]').find('tbody tr').removeClass("k-state-selected")
		}
	});
	
	_gridMainSearch.find('table[role="grid"]').find('tbody[role="rowgroup"]').undelegate('input[type="checkbox"].Check-Item', 'click');
	_gridMainSearch.find('table[role="grid"]').find('tbody[role="rowgroup"]').delegate('input[type="checkbox"].Check-Item', 'click', function(e){
		var $obj = $(this);
		var isCheck = $obj.prop('checked');
		if(isCheck){
			$obj.closest("tr").addClass("k-state-selected");
		}else{
			$obj.closest("tr").removeClass("k-state-selected");
		}
		_gridMainSearch.find('table[role="grid"]').find('thead input[type="checkbox"]').prop('checked', _gridMainSearch.find('tbody tr input[type="checkbox"]:not(:checked)').length == 0);
	});
	
	$("#f-search").find('button[data-action]').click(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var dataAction = $(this).data('action');
		
		switch (dataAction) {
		case 'search':
			_gridMainSearch.data("kendoGrid").dataSource.page(1);
			break;

		default:
			break;
		}
	});
	
	$('div.modal-footer').find('button[data-action="select"]').click(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		
		var arrayDataSelect = [];
		var rowData = null;
		var checkRows = _gridMainSearch.find(' tbody tr input[type="checkbox"]:checked');
		checkRows.each(function(i, v) {
			rowData = _gridMainSearch.data("kendoGrid").dataItem(_gridMainSearch.find(' tbody tr').eq($(checkRows[i].closest("tr")).index()));
			arrayDataSelect.push(rowData);
		});
		if(callback) callback(arrayDataSelect);
		$('#f-search').closest("div.modal").modal("hide");
		$('#f-search').closest("div.modal").find('.modal-content').empty();
	});
	
});

function getDataSearchPopup(){
	var dataPost = {};
	
	dataPost['mau-so-hdon'] = $('#f-search #mau-so-hdon').val() == null? '': $('#f-search #mau-so-hdon').val();
	dataPost['so-hoa-don'] = $('#f-search #so-hoa-don').val() == null? '': $('#f-search #so-hoa-don').val();
	dataPost['from-date'] = $('#f-search #from-date').val() == null? '': $('#f-search #from-date').val();
	dataPost['to-date'] = $('#f-search #to-date').val() == null? '': $('#f-search #to-date').val();
	dataPost['nban-mst'] = $('#f-search #nban-mst').val() == null? '': $('#f-search #nban-mst').val();
	dataPost['nban-ten'] = $('#f-search #nban-ten').val() == null? '': $('#f-search #nban-ten').val();
	
	return dataPost;
}