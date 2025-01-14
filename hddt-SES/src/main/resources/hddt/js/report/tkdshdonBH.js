$(function(){
	dateInputFormat($('#f-einvoices').find('#from-date'));
	dateInputFormat($('#f-einvoices').find('#to-date'));
	
	_gridMain.kendoGrid({
		dataSource: new kendo.data.DataSource({
			transport: {
				read: {
					type: 'POST',
					url: ROOT_PATH + '/main/tkdshdonBH/search',
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
			{field: 'StatusDesc', width: '100px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Trạng thái</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
				headerAttributes: {'class': 'table-header-cell text-center'},
			},
			{field: 'SignStatusDesc', width: '80px', encoded: false, headerTemplate: '<a class="k-link" href="javascript:void(0);">Đã ký</a>',
				attributes: {'class': 'table-cell text-center'}, sortable: false, 
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
			
    	],
		dataBound: function(e) {
			
		}
	});
	
	$("#f-einvoices").find('button[data-action]').click(function (event) {
		event.preventDefault();/*event.stopPropagation();*/
		var dataAction = $(this).data('action');
		
		var objData = {};
		switch (dataAction) {
		case 'search':
			_gridMain.data("kendoGrid").dataSource.page(1);
			break;
		case 'export-excel-fast':
		case 'export-excel-detail':
		case 'export-excel-general':	
			objData = getDataSearch();
			$.ajax({
				type: "POST",
				datatype: "json",
				url: ROOT_PATH + '/main/tkdshdonBH/check-data-export',
				data: objData,
				beforeSend: function(req) {
					initAjaxJsonRequest(req);
		        	showLoading();
				},
				success:function(res) {
					hideLoading();
					if(res.errorCode == 0) {
						var responseData = res.responseData;
						
						tokenTransaction = responseData['TOKEN'];
						if(dataAction == 'export-excel-fast')
							window.open(ROOT_PATH + '/main/tkdshdonBH/export-excel-to-fast/' + tokenTransaction,'_blank');
						else if(dataAction == 'export-excel-general')
							window.open(ROOT_PATH + '/main/tkdshdonBH/export-excel-general/' + tokenTransaction,'_blank');
						else
							window.open(ROOT_PATH + '/main/tkdshdonBH/export-excel-dshdon-ctiet/' + tokenTransaction,'_blank');
					}else{
						alertDLSuccess(createObjectError(res).html(), function(){});
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
	dataPost['nmua-mst'] = $('#f-einvoices #nmua-mst').val() == null? '': $('#f-einvoices #nmua-mst').val();
	dataPost['nmua-ten'] = $('#f-einvoices #nmua-ten').val() == null? '': $('#f-einvoices #nmua-ten').val();
	
	return dataPost;
}