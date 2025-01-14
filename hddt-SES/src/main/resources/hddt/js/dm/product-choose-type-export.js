$(function(){
	dateInputFormat($('#f-prd-type-exp').find('#from-date'));
	dateInputFormat($('#f-prd-type-exp').find('#to-date'));
	
	$('#f-prd-type-exp').find('input[type="radio"][name="optType"]').change(function() {
		var _val = $(this).val();
		
		$('#f-prd-type-exp').find('div.type-date').addClass('d-none');
		if('DATE' == _val){
			$('#f-prd-type-exp').find('div.type-date').removeClass('d-none');	
		}
	});
	
	$('div.modal-footer').find("button[data-action='accept']").click(function (event) {
		event.preventDefault();
		
		var _type = $('#f-prd-type-exp').find('input[type="radio"][name="optType"]:checked').val();
		var obj = {};
		obj['type'] = _type;
		if('DATE' == _type){
			obj['from-date'] = $('#f-prd-type-exp').find('#from-date').val();
			obj['to-date'] = $('#f-prd-type-exp').find('#to-date').val();
		}
		
		if(callback) callback(obj);
		$('#f-prd-type-exp').closest("div.modal").modal("hide");
		$('#f-prd-type-exp').closest("div.modal").find('.modal-content').empty();
	});
});