$(function(){	
	_gridMainSearch.kendoGrid({
		dataSource: new kendo.data.DataSource({
			transport: {
				read: {
					type: 'POST',
					url: ROOT_PATH + '/common/search-customer',
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
			{field: 'Email', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Email</a>',
				attributes: {'class': 'table-cell text-left'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'EmailCC', width: '200px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">EmailCC</a>',
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
			}
    	],
    	dataBound: function(e) {
    	}
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
		
		var grid = _gridMainSearch.data("kendoGrid");
    	var rowData = grid.dataItem(grid.select());
    	if(null == rowData){
    		alertDLSuccess('Vui lòng chọn dòng dữ liệu để thực hiện.', function(){});
    		return;
    	}
		
		if(callback) callback(rowData);
		$('#f-search').closest("div.modal").modal("hide");
		$('#f-search').closest("div.modal").find('.modal-content').empty();
	});
	
});

function getDataSearchPopup(){
	var dataPost = {};
	
	dataPost['tax-code'] = $('#f-search #tax-code').val() == null? '': $('#f-search #tax-code').val();
	dataPost['company-name'] = $('#f-search #company-name').val() == null? '': $('#f-search #company-name').val();
	dataPost['customer-name'] = $('#f-search #customer-name').val() == null? '': $('#f-search #customer-name').val();
	
	return dataPost;
}