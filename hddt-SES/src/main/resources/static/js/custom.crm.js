var _timeout;
var timer = null; var timerInterval = 30 * 1000;
var arraySelected = [];var checkedIds = {};var callback = null;
var _csrf_value = '';var _csrf_name = '';var tokenTransaction = '';
var KENDOUI_PAGESIZE = 10;var KENDOUI_PAGESIZES = [5, 10, 20, 50, 100];var KENDOUI_BUTTONCOUNT = 5;var kendoGridHeight = 350;
var kendoGridHeightSmall = 250;
var KENDOUI_PAGESIZE_NO_SCROLL_Y = 5;
var ROOT_PATH = '';
var transactionMain = '';var transactionSub = '';
var methodSub = '';
var vats = [];
var isSetDataSource_Popup = false;
var kendoGridMessages = {
//		itemsPerPage: 'dòng trên trang',
		itemsPerPage: '',
		previous: 'Trang trước',
		next: 'Trang sau',
		refresh: 'Tải lại trang',
		last: 'Trang cuối',
		first: 'Trang đầu'
//		, empty: 'Không có dữ liệu'
		, empty: ''
//		, display: '<span class="text-lowercase"> {0} - {1} của tổng số {2} dòng</span>'
		, display: '<span class="text-lowercase">{0}-{1}/{2}</span>'
}

var HEADER_RESULT_TYPE_NAME = "RESULT_TYPE";
var HEADER_RESULT_TYPE_JSON = "JSON";
var HEADER_RESULT_TYPE_JSON_ARRAY = "JSON_ARRAY";
var HEADER_RESULT_TYPE_JSON_GRID = "JSON_GRID";
var HEADER_RESULT_TYPE_NORMAL = "PAGE";
var HEADER_RESULT_TYPE_PAGE_POPUP = "PAGE-POPUP";
var HEADER_RESULT_TYPE_PAGE_AREA = "PAGE-AREA";

function initSetCSRF(xhrObj){xhrObj.setRequestHeader('X-CSRF-TOKEN', _csrf_value)};
function initAjaxJsonRequest(xhrObj) {initSetCSRF(xhrObj); xhrObj.setRequestHeader(HEADER_RESULT_TYPE_NAME, HEADER_RESULT_TYPE_JSON);}
function initAjaxJsonArrayRequest(xhrObj) {initSetCSRF(xhrObj); xhrObj.setRequestHeader(HEADER_RESULT_TYPE_NAME, HEADER_RESULT_TYPE_JSON_ARRAY);}
function initAjaxJsonGridRequest(xhrObj) {initSetCSRF(xhrObj); xhrObj.setRequestHeader(HEADER_RESULT_TYPE_NAME, HEADER_RESULT_TYPE_JSON_GRID);}
function initNormalRequest(xhrObj) {initSetCSRF(xhrObj); xhrObj.setRequestHeader(HEADER_RESULT_TYPE_NAME, HEADER_RESULT_TYPE_NORMAL);}
function initPagePopupRequest(xhrObj) {initSetCSRF(xhrObj); xhrObj.setRequestHeader(HEADER_RESULT_TYPE_NAME, HEADER_RESULT_TYPE_PAGE_POPUP);}
function initPageNormalArea(xhrObj) {initSetCSRF(xhrObj); xhrObj.setRequestHeader(HEADER_RESULT_TYPE_NAME, HEADER_RESULT_TYPE_PAGE_AREA);}

function showLoading() {$('.preloader').fadeIn();}
function hideLoading(){$('.preloader').fadeOut();}
$.fn.serializeIncludeDisabled = function () {
    var disabled = this.find(":input:disabled").removeAttr("disabled");
    var serialized = this.serialize();
    disabled.attr("disabled", "disabled");
    return serialized;
};
$.fn.serializeObject = function() {
	var disabled = this.find(":input:disabled").removeAttr("disabled");
	
    var o = {};
    var a = this.serializeArray();
    $.each(a, function() {
        if (o[this.name]) {
            if (!o[this.name].push) {
                o[this.name] = [o[this.name]];
            }
            o[this.name].push(this.value || '');
        } else {
            o[this.name] = this.value || '';
        }
    });
    disabled.attr("disabled", "disabled");
    return o;
};

String.prototype.trim = function() {
	return this.replace(/^\s+|\s+$/g,"");
}

alertify.set('notifier','position', 'top-right');
alertify.defaults.closable = false;
alertify.defaults.transition = "fade";
alertify.defaults.theme.ok = "btn btn-sm btn-primary btn-ses";
alertify.defaults.theme.cancel = "btn btn-sm btn-danger";

function alertDLSuccess(msg, callback){
	if(isRedirectToLogin){
		isRedirectToLogin = false;
		callback = function(){
			window.location.href = ROOT_PATH;	
		}
	}
	
	alertify.alert(
		'<b class="white text-uppercase fw-800">Thông báo </b>'
		, msg
		, undefined == callback? function(e){}: callback
	);
}

function alertConfirm(msg, callbackOK, callbackCancel){
	alertify.confirm('<b class="white text-uppercase fw-800">Xác nhận</b>', msg, undefined == callbackOK? function(e){}: callbackOK, undefined == callbackCancel? function(e){}: callbackCancel);
}

function notificationDLSuccess(msg, callback){
	showNotificationComplete(msg, 'success');
	if(isRedirectToLogin){
		isRedirectToLogin = false;
		setTimeout(() => {
			window.location.href = ROOT_PATH;
		}, 3000);
	}
//	if(isRedirectToLogin){
//		isRedirectToLogin = false;
//		callback = function(){
//			window.location.href = ROOT_PATH;	
//		}
//	}
//	
//	alertify.alert(
//		'<b class="white text-uppercase fw-800">Thông báo </b>'
//		, msg
//		, undefined == callback? function(e){}: callback
//	);
	
}

//jQuery plugin to prevent double click
jQuery.fn.preventDoubleClick = function() {
	$(this).on('click', function(e) {
		var $el = $(this);
		if ($el.data('clicked')) {
			// Previously clicked, stop actions
			e.preventDefault();e.stopPropagation();
		} else {
			// Mark to ignore next click
			$el.data('clicked', true);
			// Unmark after 1 second
			window.setTimeout(function() {
				$el.removeData('clicked');
			}, 1000)
		}
	});
	return this;
};

function isDoubleClicked(element) {
    //if already clicked return TRUE to indicate this click is not allowed
    if (element.data("isclicked")) return true;

    //mark as clicked for 1 second
    element.data("isclicked", true);
    setTimeout(function () {
        element.removeData("isclicked");
    }, 1000);

    //return FALSE to indicate this click was allowed
    return false;
}

function disabledAllControlsInForm(idForm){
	$('#' + idForm + ' input[type="text"]').attr('readonly', 'readonly');
	$('#' + idForm + ' textarea').attr('readonly', 'readonly'); 

//	$('#' + idForm + ' select').attr('disabled', 'disabled');
//	$('#' + idForm + ' .SumoSelect').addClass('disabled');

	$('#' + idForm + ' input[type="file"]').attr('disabled', 'disabled');
	$('#' + idForm + ' input[type="checkbox"]').attr('disabled', 'disabled');
	$('#' + idForm + ' input[type="radio"]').attr('disabled', 'disabled');
	
	$('#' + idForm + ' select:not(.sumo)').attr('disabled', 'disabled');
	$('#' + idForm + ' select.sumo').find('option').attr('disabled', 'disabled');
	$('#' + idForm).find('div.SumoSelect').find('div.multiple').find('ul.options li.opt').addClass('disabled');
	$('#' + idForm).find('div.SumoSelect').find('div.selall').find('p.select-all').addClass('partial');
	
	try {
		setTimeout(function(){
			$('#' + idForm + ' .date').datetimepicker('destroy');
			$('#' + idForm + ' .date').find('input').datetimepicker('destroy');
		}, 1000);
	}catch(err) {}
	
}

function refreshCaptcha(objImg){$.ajax({type: "POST",datatype: "plain/text",url: ROOT_PATH + "/common/generatingCaptcha",cache: false,beforeSend: function(req) {initSetCSRF(req);},success:function(res) {objImg.attr('src', "data:image/jpg;base64, " + res + "");}});}

var isRedirectToLogin = false;
function createObjectError(objJson){
	isRedirectToLogin = false;
	if(jQuery.type(objJson) == 'object'){
		if(objJson.errorCode == 401)
			isRedirectToLogin = true;
	}
	var objDiv = $('<div>');if(objJson.responseData != null)objDiv.append($('<div>').addClass('bootbox-body').html(0 != objJson.errorCode? '<span class="text-danger">' + objJson.responseData + '</span>': objJson.responseData));if(objJson.errorMessages != null && objJson.errorMessages.length > 0){objDiv.append($('<hr>').css('margin-bottom', '5px').css('margin-top', '5px'));var objDivSub = $('<div>').addClass('required');objDivSub.append('<ul class="p-l-20">');for(var i=0; i<objJson.errorMessages.length; i++){objDivSub.find('ul').eq(0).append($('<li class="small">').html(objJson.errorMessages[i]));}objDiv.append(objDivSub);}return objDiv;
}
function showPopupWithURLAndData(url, data, isSmall, callback){var idDlg = 'modelPopup'; if(isSmall) idDlg = 'modelPopupMedium'; this.callback = null; if(callback) this.callback = callback;$('#' + idDlg + ' .modal-content').empty();var dataPost = '';if(data != null){Object.keys(data).forEach(function(key, index) {dataPost += key + '=' + data[key] + '&';});}$.ajax({type: "POST",dataType: "html",url: url,data: dataPost,beforeSend: function(req) {initPagePopupRequest(req);/*showLoading();*/},success : function(data) {$('#' + idDlg + ' .modal-content').html(data);hideLoading();},error:function (xhr, ajaxOptions, thrownError){$('#' + idDlg + ' .modal-content').html('<div class="modal-header" style="background-color: #ececec;"><h4 class="modal-title text-uppercase fw-700" id="dlg_title">SYSTEM-ERROR</h4></div><div class="modal-body"><div class="row"><div class="col-sm-12"><div class="col-xs-11 margin-top-only margin-bottom-only center-block"><label class="col-form-label control-label fw-700 text-uppercase text-red-important">Lỗi hệ thống...</label></div></div></div></div><div class="modal-footer"><button type="button" id="btDlgClose" class="btn btn-danger btn-sm " data-dismiss="modal"><i class="fa fa-power-off mR-5"></i> Đóng</button></div>');hideLoading();} });$('#' + idDlg).modal("show");}
function submitFormRenderArea(url, formData, objRender, callback){
	objRender.empty();
	this.callback = null; if(callback) this.callback = callback
	$.ajax({
		url: url,
		type: 'POST',
		data: formData,
//		async: true,
		cache: false,		
//		contentType: 'application/json; charset=utf-8',
//		contentType: 'text/plain',
//		contentType: false,		
//		enctype: 'multipart/form-data',		
//		processData: false,
		dataType : 'html',
		traditional: true,
		beforeSend: function(req){
			initPageNormalArea(req);
			showLoading();
		},
		beforeSubmit: function(req){
			initPageNormalArea(req);
			showLoading();
		},
		success: function(res){
			hideLoading();			
			objRender.html(res);
		},
		error:function (xhr, ajaxOptions, thrownError){
            hideLoading();
        }
	});
}

function encodeObjJsonBase64UTF8(obj){
	return base64.encode(base64._utf8_encode(JSON.stringify(obj))).replace(/\+/g, "@");
}
function decodeBase64UTF8ToObj(strBase64){
	return JSON.parse(base64._utf8_decode(base64.decode(base64._utf8_encode(strBase64))));
}

//https://stackoverflow.com/questions/17727884/range-out-of-order-in-character-class-in-javascript/17728040
$.fn.inputFilter = function(inputFilter) {
	return this.on(
			"input keydown keyup mousedown mouseup select contextmenu drop",
			function() {
				if (inputFilter(this.value)) {
					this.oldValue = this.value;
					this.oldSelectionStart = this.selectionStart;
					this.oldSelectionEnd = this.selectionEnd;
				} else if (this.hasOwnProperty("oldValue")) {
					this.value = this.oldValue;
					this.setSelectionRange(this.oldSelectionStart,
							this.oldSelectionEnd);
				}
			});
};

function inputFilterPosition(obj) {
	obj.inputFilter(function(value) {
		  return /^[A-Za-z]{0}[A-Za-z0-9_\-\.]*$/.test(value); });
}

function inputFilterServiceProductCode(obj) {
	obj.inputFilter(function(value) {
		  return /^[A-Za-z]{0}[A-Za-z0-9_-]*$/.test(value); });
}

function inputFilterCodeNormal(obj) {
	obj.inputFilter(function(value) {
		  return /^[A-Za-z0-9_-]{0}[A-Za-z0-9_-]*$/.test(value); });
}

function inputFilterInteger(obj) {
	obj.inputFilter(function(value) {
		return /^[-]?\d*$/.test(value); 
	});
}
function inputFilterDouble(obj) {
	obj.inputFilter(function(value) {
//		value = value.replace(/,/g, "")
		return /^\d{0,20}(\.\d{0,5}){0,1}$/.test(value); 
	});
}
function inputFilterUsername(obj) {
	obj.inputFilter(function(value) {
		  return /^[A-Za-z]{0}[A-Za-z0-9_\-\.]*$/.test(value); });
}

function FormatNumberPercent(obj){
	var value = obj.value;
	if(null == value || '' == value) return;
	try{
		obj.value = numeral(value).format('#,###.0000');
	}catch(err){
		obj.value = '';
	}
}

function FormatCurrency(obj, curr) {
	var value = obj.value;
	if(null == value || '' == value) return;
	
	if(curr == null || curr == '')
		curr = 'VND';
	
	var number = numeral(value);
	var nFinal = number.value();
	
	if (curr == 'VND' || curr == "JPY" || curr == "CCC") {
		nFinal = Math.round(nFinal);
		obj.value = numeral(nFinal).format('#,###');
	}else{
		obj.value = numeral(nFinal).format('#,###.00');
	}
}

function FormatNumberWithPrecision(obj){
	var value = obj.value;
	if(null == value || '' == value) return;
	try{
		obj.value = numeral(value).format('#,###.[00000]');
	}catch(err){
		obj.value = '';
	}
}

function initComboSearchLocalNotPercent(jqObj, idSelect) {
	$(jqObj).find(idSelect).select2({
		allowClear: true,
		language : {
			noResults : function() {
				return "Không tìm thấy dữ liệu";
			}	
		},
		placeholder :'',
//		width: '100%',
//		width: null == widthPercent || undefined == widthPercent? '100%': widthPercent
	});
}

function initComboSearchLocal(jqObj, idSelect, widthPercent) {
	$(jqObj).find(idSelect).select2({
		allowClear: true,
		language : {
			noResults : function() {
				return "Không tìm thấy dữ liệu";
			}	
		},
		placeholder :'',
		width: '100%',
//		width: null == widthPercent || undefined == widthPercent? '100%': widthPercent
	});
}

function initComboSearchLocalWithParent(jqObj, idSelect, idParentContainer) {
	$(jqObj).find(idSelect).select2({
		allowClear: true,
		language : {
			noResults : function() {
				return "Không tìm thấy dữ liệu";
			}	
		},
		placeholder :'',
		dropdownParent: $(idParentContainer),
		width: '100%',
	});
}

//$.datetimepicker.setLocale('vi');
function dateInputFormat(ids){
	$(ids).datetimepicker({
		timepicker:false,
		format:'d/m/Y',
		formatDate:'d/m/Y',
		value: $(ids)[0].tagName === 'DIV' ? $(ids).find('input').val() : $(ids).val(),
		onChangeDateTime:function(dp, $input){
			if(!$input.is('input')){
				$input.find('input').val($input.val());
			}
		},
//		lang: 'vi'
		
		scrollMonth : false,
	    scrollInput : false
	});
}
//$('.date').datetimepicker('destroy');

var _vArraysImage = [];
function initTinyMCEForMail(objId){
	_vArraysImage = [];
//	tinymce.remove(objId);
	tinymce.init({
        selector: 'textarea#' + objId,
//        mode: "textareas",
//         content_css: '//www.tiny.cloud/css/codepen.min.css',
        height: 400,
        forced_root_block: 'div',
        plugins: [
            "advlist anchor autolink codesample fullscreen image ",	/*help*/
            " lists link media noneditable preview",
            " searchreplace table template visualblocks wordcount paste  code imagetools "	/*image*/
        ],
        toolbar: "fullscreen preview | insertfile a11ycheck undo redo | fontselect fontsizeselect | bold italic | forecolor backcolor | template codesample | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | removeformat | link code image ",	/*image*/
//        mobile: {
//        	menubar: true
//        },
        fontsize_formats: '8pt 9pt 10pt 11pt 12pt 14pt 16pt 18pt 20pt 22pt 24pt 26pt 36pt',
        
        statusbar: true,
        branding: false,	// To disable "Powered by TinyMCE"
        editor_selector : "mceEditor",
        language : "vi",
        mode : "textareas"
//        theme : "simple",
        
        /* without images_upload_url set, Upload tab won't show up*/
//        images_upload_url: 'postAcceptor.php',
//        images_upload_url: ROOT_PATH + '/Main?transaction=ImagesManageCreate&method=processUploadFile',
        /* we override default upload handler to simulate successful upload*/
        , relative_urls: true
        , remove_script_host: false
        , convert_urls : 0
        , images_upload_handler: function (blobInfo, success, failure) {
        	var xhr, formData;

        	xhr = new XMLHttpRequest();
        	xhr.withCredentials = false;
//        	xhr.open('POST', ROOT_PATH + '/Main?transaction=ImagesManageCreate&method=processUploadFile');
        	xhr.open('POST', ROOT_PATH + '/common/processUploadFileForMail');
        	  
        	xhr.setRequestHeader('X-CSRF-TOKEN', _csrf_value);
        	xhr.setRequestHeader(HEADER_RESULT_TYPE_NAME, HEADER_RESULT_TYPE_JSON);
        	xhr.responseType = 'json';
//        	  xhr.upload.onprogress = function (e) {
//        	    progress(e.loaded / e.total * 100);
//        	  };

        	xhr.onload = function() {
        		var json;

        		if (xhr.status === 403) {
//        			failure('HTTP Error: ' + xhr.status, { remove: true });
        			alertDLSuccess('HTTP Error: ' + xhr.status);
        			return;
        		}

        		if (xhr.status < 200 || xhr.status >= 300) {
//        	      failure('HTTP Error: ' + xhr.status);
        			alertDLSuccess('HTTP Error: ' + xhr.status);
        	      return;
        	    }
        	    var json = xhr.response;
        	    if(typeof json === 'object'){
//        	    	if($.isObject(json.responseData)){
        	    		var obj = json.responseData;
        	    		success(window.location.origin + ROOT_PATH + '/common/viewImageForMail/' + obj['OriginalPath'] + "/" + obj['SystemFilename']);
        	    		
        	    		_vArraysImage.push(obj['SystemFilename']);
//        	    	}
        	    		
//        	    	if($.isArray(json.responseData) && json.responseData.length > 0){
//        	    		var obj = json.responseData[0];
//        	    		success(ROOT_PATH + '/common/viewImageForMail/' + obj['SystemFilename']);
//        	    	}
//        	    	success(json.location);
        	    }
//        	   alert(JSON.parse(json))
        	    
//        	    json = JSON.parse(xhr.responseText);
//
//        	    if (!json || typeof json.location != 'string') {
//        	      failure('Invalid JSON: ' + xhr.responseText);
//        	      return;
//        	    }

//        	    success(json.location);
        	  };

        	  xhr.onerror = function () {
//        	    failure('Image upload failed due to a XHR Transport error. Code: ' + xhr.status);
        		  alertDLSuccess('HTTP Error: ' + xhr.status);
        	  };

        	  formData = new FormData();
        	  formData.append('file', blobInfo.blob(), blobInfo.filename());

        	  xhr.send(formData);
              
//        	setTimeout(function () {
//	            /* no matter what you upload, we will turn it into TinyMCE logo :)*/
//	            success('http://moxiecode.cachefly.net/tinymce/v9/images/logo.png');
//        	}, 2000);
        },
        
        setup: function (editor) {
            editor.on('init', function (e) {
//            	editor.setContent('<p>Hello world!asd sa</p>');
//            	editor.setMode("readonly")
            });
        }
        
        
    });
	
//	https://stackoverflow.com/questions/32519169/why-tinymce-fails-to-load-second-time
//	var textArea_id = objId;
//	textArea_id = 'mytextarea';
	if (tinymce.editors.length > 0) {
//	    tinymce.execCommand('mceFocus', true, textArea_id );       
	    tinymce.execCommand('mceRemoveEditor',true, objId);        
	    tinymce.execCommand('mceAddEditor',true, objId);
	}
}

//tinymce.init({
//    selector: '#emailContent',
//    menubar: false,
//    readonly: 1, 
//    branding: false,
//    height: 100,
//    max_height: 400,
//    //theme: 'modern',
//    toolbar: false,
//	toolbar_items_size: 'small',
//	plugins: ['advlist autolink lists link image charmap print preview hr anchor pagebreak','searchreplace wordcount visualblocks visualchars code fullscreen','insertdatetime media nonbreaking save table contextmenu directionality','emoticons template paste textcolor colorpicker textpattern imagetools codesample toc help'],
//	toolbar1: 'insert | undo redo |  styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image print preview media | forecolor backcolor emoticons | fullscreen | table',
//    image_advtab: true,
//    templates: [{
//            title: 'Test template 1',
//            content: 'Test 1'
//        },
//        {
//            title: 'Test template 2',
//            content: 'Test 2'
//
//        }
//    ],
//    content_css: [
//
//    ]
//	setup: function (editor) {
//    editor.setMode("readonly")
//    },
//});

//https://stackoverflow.com/questions/26888276/tinymce-enable-button-while-in-read-only-mode
function initTinyMCEOnlyView(objId){
	tinymce.init({
	    selector: '#' + objId,
	    menubar: false,
	   // readonly: 1, 
	    branding: false,
	    height: 500,
	    max_height: 400,
	    //theme: 'modern',
	    toolbar: false,
	    //toolbar_items_size: 'small',
	    plugins: [' fullscreen'],		//preview
	    toolbar: ' fullscreen',		//preview
	    image_advtab: true,
	    templates: [
	    ],
	    content_css: [

	    ],
		setup: function (editor) {
			editor.on('init', function (e) {
				editor.setMode("readonly");
			});
		}
		, relative_urls: true
	    , remove_script_host: false
	});
	
	if (tinymce.editors.length > 0) {       
	    tinymce.execCommand('mceRemoveEditor',true, objId);        
	    tinymce.execCommand('mceAddEditor',true, objId);
	}
}

function initTinyMCEForMailHideMenuBar(objId){
//	tinymce.remove(objId);
	tinymce.init({
        selector: '#' + objId,
        height: 400,
        forced_root_block: 'div',
        plugins: [
            "advlist anchor autolink codesample fullscreen ",	/*help*/
            " lists link media noneditable preview",
            " searchreplace table template visualblocks wordcount paste  code imagetools "	/*image*/
        ],
        toolbar: "fullscreen preview | insertfile a11ycheck undo redo | fontselect fontsizeselect | bold italic | forecolor backcolor | template codesample | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | removeformat | link code",	/*image*/
        fontsize_formats: '8pt 9pt 10pt 11pt 12pt 14pt 16pt 18pt 20pt 22pt 24pt 26pt 36pt',
        menubar:false,
        statusbar: true,
        branding: false,	// To disable "Powered by TinyMCE"
        editor_selector : "mceEditor",
        language : "vi",
        mode : "textareas",
        images_upload_url: 'postAcceptor.php',
        images_upload_handler: function (blobInfo, success, failure) {
        	setTimeout(function () {
	            success('http://moxiecode.cachefly.net/tinymce/v9/images/logo.png');
        	}, 2000);
        },
        
        setup: function (editor) {
            editor.on('init', function (e) {
            });
        }
        
        
    });
	
	if (tinymce.editors.length > 0) {   
	    tinymce.execCommand('mceRemoveEditor',true, objId);        
	    tinymce.execCommand('mceAddEditor',true, objId);
	}
}

function initTinyMCEOnlyView($objId){
	var quill = new Quill($objId, {
		modules: {
			toolbar: false
	    },
		theme: 'snow',
		readOnly: true
	});
//	quill.enable(false);
}

//function initTrumbowygForEdit($objId){
//	$objId.trumbowyg();
//}

function initTrumbowygForEdit($objId){
	$objId.trumbowyg({
	    btnsDef: {
	        align: {
	            dropdown: ['justifyLeft', 'justifyCenter', 'justifyRight', 'justifyFull'],
	            ico: 'justifyLeft'
	        },
	        image: {
	            dropdown: ['insertImage', 'base64'],
	            ico: 'insertImage'
	        }
	    },
	    btns: [
	        ['viewHTML'],
//	        ['undo', 'redo'],
	        ['historyUndo','historyRedo'], // Only supported in Blink browsers
	        ['formatting', 'lineheight'],
	        ['strong', 'em', 'del'],
	        ['superscript', 'subscript'],
	        ['link'],
//	        ['insertImage'],
	        ['image'], ['align'],
//	        ['justifyLeft', 'justifyCenter', 'justifyRight', 'justifyFull'],
	        ['foreColor', 'backColor'], ['fontfamily', 'fontsize'],
	        ['table'],
	        ['unorderedList', 'orderedList'],
	        ['horizontalRule'],
	        ['removeformat'],
	        ['fullscreen']
	    ],
	    lang: 'vi'
	    , changeActiveDropdownIcon: true
	    , hideButtonTexts: false
	    , semantic: true				/* (i.e. <em> instead of <i>, <strong> instead of <b>, etc.).  */
	    , resetCss: false
	    , removeformatPasted: false        //default
	    , tagsToRemove: ['script']
	    , tagsToKeep: ['i']
	    , autogrow: false                //scroll - default
//	    , autogrowOnEnter: true
	    , imageWidthModalEdit: true
	    , urlProtocol: true
	    , minimalLinks: true
	    , defaultLinkTarget: '_blank'
	    , disabled: false
	    , plugins: {
	    	//trumbowyg.resizimg.js
	        resizimg: {
	        	minSize: 32,
	        	step: 4
	        },
	        allowTagsFromPaste: true
	/*
	        allowTagsFromPaste: {
	            allowedTags: ['h4', 'p', 'br']
	        }
	*/
	        , fontfamily: {
	        	fontList: [
	        		  {name: 'Arial', family: 'Arial, Helvetica, sans-serif'},
	        		  {name: 'Arial Black', family: 'Arial Black, Gadget, sans-serif'},
	        		  {name: 'Comic Sans', family: 'Comic Sans MS, Textile, cursive, sans-serif'},
	        		  {name: 'Courier New', family: 'Courier New, Courier, monospace'},
	        		  {name: 'Georgia', family: 'Georgia, serif'},
	        		  {name: 'Impact', family: 'Impact, Charcoal, sans-serif'},
	        		  {name: 'Lucida Console', family: 'Lucida Console, Monaco, monospace'},
	        		  {name: 'Lucida Sans', family: 'Lucida Sans Uncide, Lucida Grande, sans-serif'},
	        		  {name: 'Palatino', family: 'Palatino Linotype, Book Antiqua, Palatino, serif'},
	        		  {name: 'Tahoma', family: 'Tahoma, Geneva, sans-serif'},
	        		  {name: 'Times New Roman', family: 'Times New Roman, Times, serif'},
	        		  {name: 'Trebuchet', family: 'Trebuchet MS, Helvetica, sans-serif'},
	        		  {name: 'Verdana', family: 'Verdana, Geneva, sans-serif'}
	        		]
//	            fontList: [
//	                {name: 'Arial', family: 'Arial, Helvetica, sans-serif'},
//	                {name: 'Arial Black', family: '\'Arial Black\', Gadget, sans-serif'},
//	                {name: 'Comic Sans', family: '\'Comic Sans MS\', Textile, cursive, sans-serif'},
//	                {name: 'Courier New', family: '\'Courier New\', Courier, monospace'},
//	                {name: 'Georgia', family: 'Georgia, serif'},
//	                {name: 'Impact', family: 'Impact, Charcoal, sans-serif'},
//	                {name: 'Lucida Console', family: '\'Lucida Console\', Monaco, monospace'},
//	                {name: 'Lucida Sans', family: '\'Lucida Sans Uncide\', \'Lucida Grande\', sans-serif'},
//	                {name: 'Palatino', family: '\'Palatino Linotype\', \'Book Antiqua\', Palatino, serif'},
//	                {name: 'Tahoma', family: 'Tahoma, Geneva, sans-serif'},
//	                {name: 'Times New Roman', family: '\'Times New Roman\', Times, serif'},
//	                {name: 'Trebuchet', family: '\'Trebuchet MS\', Helvetica, sans-serif'},
//	                {name: 'Verdana', family: 'Verdana, Geneva, sans-serif'}
//	            ]
	        }
	    }
	});
	
	$objId.trumbowyg('execCmd',{'cmd': 'insertHTML', 'param': '<p></p>', 'forceCss': false});
	
	setTimeout(function(){ 
		$('.trumbowyg-box .trumbowyg-editor').click(function () {
	    	if($(".vscomp-ele.pop-comp-ele").hasClass("pop-comp-active")){
	    		$(".vscomp-ele.pop-comp-ele").removeClass("pop-comp-active")
	    	}
	    	if($(".vscomp-ele-wrapper").hasClass('focused')){
	    		$(".vscomp-ele-wrapper").removeClass("focused")
	    		$(".vscomp-ele-wrapper").addClass("closed")
	    	}
	    	$(".vscomp-dropbox-container.pop-comp-wrapper").css({
		    	"display" :"none",
		    	"transform": "translate3d(0px,9px,0px)",
		    	"max-width": "196px",
		    	"opacity": "0"
	    	});
		});
	}, 3000);
}

function initTrumbowygOnlyView_TMP($objId){
	$objId.trumbowyg({
		disabled: true,
		imageResize: false
	});
}

function initTrumbowygOnlyView($objId){
	$objId.trumbowyg({
	    btnsDef: {
	        align: {
	            dropdown: ['justifyLeft', 'justifyCenter', 'justifyRight', 'justifyFull'],
	            ico: 'justifyLeft'
	        },
	        image: {
	            dropdown: ['insertImage', 'base64'],
	            ico: 'insertImage'
	        }
	    },
	    btns: [
	        ['viewHTML'],
//	        ['undo', 'redo'],
	        ['historyUndo','historyRedo'], // Only supported in Blink browsers
	        ['formatting', 'lineheight'],
	        ['strong', 'em', 'del'],
	        ['superscript', 'subscript'],
	        ['link'],
//	        ['insertImage'],
	        ['image'], ['align'],
//	        ['justifyLeft', 'justifyCenter', 'justifyRight', 'justifyFull'],
	        ['foreColor', 'backColor'], ['fontfamily', 'fontsize'],
	        ['table'],
	        ['unorderedList', 'orderedList'],
	        ['horizontalRule'],
	        ['removeformat'],
	        ['fullscreen']
	    ]
	    , lang: 'vi'
	    , changeActiveDropdownIcon: true
	    , hideButtonTexts: false
	    , semantic: true
	    , resetCss: false
	    , removeformatPasted: false        //default
	    , tagsToRemove: ['script']
	    , tagsToKeep: ['i']
	    , autogrow: false                //scroll - default
//	    , autogrowOnEnter: true
//	    , imageWidthModalEdit: true
	    , urlProtocol: true
	    , minimalLinks: true
	    , defaultLinkTarget: '_blank'
	    , disabled: true
	    , plugins: {
//	        resizimg: {
//	            minSize: 64,
//	            step: 16,
//	        },
//	        allowTagsFromPaste: true,
	/*
	        allowTagsFromPaste: {
	            allowedTags: ['h4', 'p', 'br']
	        }
	*/
	        fontfamily: {
	        	fontList: [
	        		  {name: 'Arial', family: 'Arial, Helvetica, sans-serif'},
	        		  {name: 'Arial Black', family: 'Arial Black, Gadget, sans-serif'},
	        		  {name: 'Comic Sans', family: 'Comic Sans MS, Textile, cursive, sans-serif'},
	        		  {name: 'Courier New', family: 'Courier New, Courier, monospace'},
	        		  {name: 'Georgia', family: 'Georgia, serif'},
	        		  {name: 'Impact', family: 'Impact, Charcoal, sans-serif'},
	        		  {name: 'Lucida Console', family: 'Lucida Console, Monaco, monospace'},
	        		  {name: 'Lucida Sans', family: 'Lucida Sans Uncide, Lucida Grande, sans-serif'},
	        		  {name: 'Palatino', family: 'Palatino Linotype, Book Antiqua, Palatino, serif'},
	        		  {name: 'Tahoma', family: 'Tahoma, Geneva, sans-serif'},
	        		  {name: 'Times New Roman', family: 'Times New Roman, Times, serif'},
	        		  {name: 'Trebuchet', family: 'Trebuchet MS, Helvetica, sans-serif'},
	        		  {name: 'Verdana', family: 'Verdana, Geneva, sans-serif'}
	        		]
	        }
	    }
	});
	
	setTimeout(function(){ 
		$('.trumbowyg-box .trumbowyg-editor').click(function () {
	    	if($(".vscomp-ele.pop-comp-ele").hasClass("pop-comp-active")){
	    		$(".vscomp-ele.pop-comp-ele").removeClass("pop-comp-active")
	    	}
	    	if($(".vscomp-ele-wrapper").hasClass('focused')){
	    		$(".vscomp-ele-wrapper").removeClass("focused")
	    		$(".vscomp-ele-wrapper").addClass("closed")
	    	}
	    	$(".vscomp-dropbox-container.pop-comp-wrapper").css({
		    	"display" :"none",
		    	"transform": "translate3d(0px,9px,0px)",
		    	"max-width": "196px",
		    	"opacity": "0"
	    	});
		});
	}, 2000);
	
}

function initVirtualSelect($objEle, data, isMultiple, selectedValue){
	VirtualSelect.init({
		ele: $objEle,
		options: data,
		multiple: isMultiple,
		selectedValue: selectedValue,
		
		placeholder: '',
		
		// No.of options to show on viewport
		optionsCount: 5,
		// if has options descriptions
		hasOptionDescription: false,
		// top, bottom, auto
		position: 'auto',
		// Text to show when no options to show
		noOptionsText: 'No results found',
		// Text to show when no results on search
		noSearchResultsTex: 'No results found',
		// Text to show near select all checkbox when search is disabled
		selectAllText: 'Chọn tất cả',
		 // Text to show as placeholder for search input
		searchPlaceholderText: 'Tìm kiếm', 
		// Text to use when displaying all values selected text (i.e. All (10))
		allOptionsSelectedText: 'Tất cả',
		zIndex: 100,
	});
}

//sumoSelectAllowSearch('#sel-02','Nhập để tìm kiếm', 'Không tìm thấy {0}')
//var num = $('option').length;
//for(var i=num; i>=1; i--){
//   $('.SlectBox')[0].sumo.remove(i-1);
//}
//$('#customerTypes')[0].sumo.add('aa','sadasd');
function sumoSelect(ids){
	$(ids).SumoSelect({
		search: false, searchText: '',selectAll: false
//		, nativeOnDevice: ['Android', 'BlackBerry', 'iPhone', 'iPad', 'iPod', 'Opera Mini', 'IEMobile', 'Silk'],
	});
	
	$(ids).on('sumo:closed', function(sumo) {
		var _idSumo = $(sumo.currentTarget).attr('id');
//		var _objSelect = $('#fCustomerTracking').find('#' + _idSumo);
		var _objSelect = $(sumo.currentTarget);
		
		var rows = [];
		var idxSelect = -1;
		var val = '';
		var li = _objSelect.closest('div.SumoSelect').find('ul.options').find('li.selected').each(function(i, o){
		    idxSelect = $(o).index();
		    val = _objSelect.find('option').eq(idxSelect).val();
		    rows.push(val);
		});
		_objSelect[0].sumo.unSelectAll();
//		console.log(rows);

		for (var j = 0; j < rows.length; j++) {
			_objSelect[0].sumo.selectItem(rows[j]);
		}
	});
	
}

function sumoSelectAllowSearch(ids, textSearch, textNoMatch){
//	console.log($(ids).prop('disabled'))
//	search: true
	$(ids).SumoSelect({
		search:true, searchText: textSearch, noMatch: textNoMatch, okCancelInMulti: false, selectAll: true
		, placeholder: ' '
//		, placeholder: 'Chọn dữ liệu ...'
//		, nativeOnDevice: ['Android', 'BlackBerry', 'iPhone', 'iPad', 'iPod', 'Opera Mini', 'IEMobile', 'Silk'], 
	});
	$(ids).on('sumo:closed', function(sumo) {
		var _idSumo = $(sumo.currentTarget).attr('id');
//		var _objSelect = $('#fCustomerTracking').find('#' + _idSumo);
		var _objSelect = $(sumo.currentTarget);
		
		var rows = [];
		var idxSelect = -1;
		var val = '';
		var li = _objSelect.closest('div.SumoSelect').find('ul.options').find('li.selected').each(function(i, o){
		    idxSelect = $(o).index();
		    val = _objSelect.find('option').eq(idxSelect).val();
		    rows.push(val);
		});
		_objSelect[0].sumo.unSelectAll();
//		console.log(rows);

		for (var j = 0; j < rows.length; j++) {
			_objSelect[0].sumo.selectItem(rows[j]);
		}
	});
	
}
function sumoSelectAllowSearchOkCancel(ids, textSearch, textNoMatch){
	$(ids).SumoSelect({
		search: true, searchText: textSearch, noMatch: textNoMatch, selectAll: true, okCancelInMulti: true
		, placeholder: 'Chọn dữ liệu ...'
	});
	
	$(ids).on('sumo:closed', function(sumo) {
		var _idSumo = $(sumo.currentTarget).attr('id');
//		var _objSelect = $('#fCustomerTracking').find('#' + _idSumo);
		var _objSelect = $(sumo.currentTarget);
		
		var rows = [];
		var idxSelect = -1;
		var val = '';
		var li = _objSelect.closest('div.SumoSelect').find('ul.options').find('li.selected').each(function(i, o){
		    idxSelect = $(o).index();
		    val = _objSelect.find('option').eq(idxSelect).val();
		    rows.push(val);
		});
		_objSelect[0].sumo.unSelectAll();
//		console.log(rows);

		for (var j = 0; j < rows.length; j++) {
			_objSelect[0].sumo.selectItem(rows[j]);
		}
	});
	
}

function callCommonFunction(url, dataPost, calback){
	$.ajax({
		type: "POST",
		datatype: "json",
		url: url,
		data: dataPost,
		beforeSend: function(req) {
			initAjaxJsonRequest(req);
        	showLoading();
		},
		success:function(res) {
			hideLoading();
			if(res) {
				calback(res);
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
}

/*DOC TIEN*/
var ChuSo=new Array(" không "," một "," hai "," ba "," bốn "," năm "," sáu "," bảy "," tám "," chín ");
var Tien=new Array( "", " nghìn", " triệu", " tỷ", " nghìn tỷ", " triệu tỷ");

//1. Hàm đọc số có ba chữ số;
function DocSo3ChuSo(baso)
{
    var tram;
    var chuc;
    var donvi;
    var KetQua="";
    tram=parseInt(baso/100);
    chuc=parseInt((baso%100)/10);
    donvi=baso%10;
    if(tram==0 && chuc==0 && donvi==0) return "không";
    if(tram!=0)
    {
        KetQua += ChuSo[tram] + " trăm ";
        if ((chuc == 0) && (donvi != 0)) KetQua += " linh ";
    }
    if ((chuc != 0) && (chuc != 1))
    {
            KetQua += ChuSo[chuc] + " mươi";
            if ((chuc == 0) && (donvi != 0)) KetQua = KetQua + " linh ";
    }
    if (chuc == 1) KetQua += " mười ";
    switch (donvi)
    {
        case 1:
            if ((chuc != 0) && (chuc != 1))
            {
                KetQua += " mốt ";
            }
            else
            {
                KetQua += ChuSo[donvi];
            }
            break;
        case 5:
            if (chuc == 0)
            {
                KetQua += ChuSo[donvi];
            }
            else
            {
                KetQua += " lăm ";
            }
            break;
        default:
            if (donvi != 0)
            {
                KetQua += ChuSo[donvi];
            }
            break;
        }
    return KetQua;
}

function readMoneyDBLInWords(SoTien, TyGia){
   if (TyGia === ''){
       SoTien = Math.round(SoTien);       
   }else{
       SoTien = Math.floor(SoTien);
   }   
   if(SoTien<0) return "Số tiền âm !";
   if(SoTien===0) return "Không đồng !"; 
   var DOCSO = function(){var t=["không","một","hai","ba","bốn","năm","sáu","bảy","tám","chín"],r=function(r,n){var o="",a=Math.floor(r/10),e=r%10;return a>1?(o=" "+t[a]+" mươi",1==e&&(o+=" mốt")):1==a?(o=" mười",1==e&&(o+=" một")):n&&e>0&&(o=" lẻ"),5==e&&a>=1?o+=" lăm":4==e&&a>=1?o+=" bốn":(e>1||1==e&&0==a)&&(o+=" "+t[e]),o},n=function(n,o){var a="",e=Math.floor(n/100),n=n%100;return o||e>0?(a=" "+t[e]+" trăm",a+=r(n,!0)):a=r(n,!1),a},o=function(t,r){var o="",a=Math.floor(t/1e6),t=t%1e6;a>0&&(o=n(a,r)+" triệu",r=!0);var e=Math.floor(t/1e3),t=t%1e3;return e>0&&(o+=n(e,r)+" nghìn",r=!0),t>0&&(o+=n(t,r)),o};return{doc:function(r){if(0==r)return t[0];var n="",a="";do ty=r%1e9,r=Math.floor(r/1e9),n=r>0?o(ty,!0)+a+n:o(ty,!1)+a+n,a=" tỷ";while(r>0);return n.trim()}}}();   
   var KetQua = DOCSO.doc(SoTien);   
   var LoaiTienTe = " đồng.";
   switch (TyGia) {
    case "":
        LoaiTienTe = " đồng.";
        break; 
    case "USD":
        LoaiTienTe = " Đô la Mỹ.";
        break; 
    case "AUD":
        LoaiTienTe = " Đô la Úc.";
        break;
    case "CAD":
        LoaiTienTe = " Đô la Canada.";
        break;
    case "CHF":
        LoaiTienTe = " Đồng Frank Thụy Sĩ.";
        break;
    case "DKK":
        LoaiTienTe = " Đồng Đan Mạch.";
        break;
    case "EUR":
        LoaiTienTe = " Đồng Euro.";
        break;
    case "GBP":
        LoaiTienTe = " Đồng Bảng Anh.";
        break;
    case "HKD":
        LoaiTienTe = " Đô la Hồng Kông.";
        break;
    case "INR":
        LoaiTienTe = " Đồng Rupee Ấn độ.";
        break;
    case "JPY":
        LoaiTienTe = " Đồng Yên Nhật.";
        break;
    case "KRW":
        LoaiTienTe = " Đồng Won Hàn Quốc.";
        break;
    case "KWD":
        LoaiTienTe = " Đồng Dinar Kuwait.";
        break;
    case "MYR":
        LoaiTienTe = " Đồng Ringít Malaysia.";
        break;
    case "NOK":
        LoaiTienTe = " Đồng Curon Nauy.";
        break;
    case "RUB":
        LoaiTienTe = " Đồng Rúp Nga.";
        break;
    case "SAR":
        LoaiTienTe = " Đồng Riyal Saudi.";
        break;
    case "SEK":
        LoaiTienTe = " Đồng Curon Thuỵ Điển.";
        break;
    case "SGD":
        LoaiTienTe = " Đô la Singgapore.";
        break;
    case "THB":
        LoaiTienTe = " Đồng Bath Thái.";
        break;
    default: 
        LoaiTienTe = " đồng.";
   }
   KetQua = KetQua.substring(0,1).toUpperCase() + KetQua.substring(1) + LoaiTienTe;
   return KetQua;
}

//2. Hàm đọc số thành chữ (Sử dụng hàm đọc số có ba chữ số)

function readMoneyInWords(SoTien, LoaiTien){
//	var n = numeral(SoTien);
//	return readMoneyDBLInWords(n.value() == null? 0: n.value(), LoaiTien);
	
	var value = 0;
	var valueDecimal = 0;
	var n = numeral(SoTien);
	if(null == n.value()){
		value = 0;
	}else{
		value = n.value();
	}
	if('VND' == LoaiTien){
		value = Math.round(value);
	}else{
		value = value.toFixed(2);
		valueDecimal = value%1*100;
		valueDecimal = valueDecimal.toFixed(2);
	}
	var r = readMoneyDBLInWords(value, LoaiTien);
	if(valueDecimal > 0) {
		var posDot = r.lastIndexOf(".");
		if(posDot == r.length -1) r = r.substr(0, posDot);
		r += ' và ' + DocSo3ChuSo(valueDecimal) + ' cents';
	}
	return r.replace(/\s+/g, ' ');;
	
	
//	if(SoTien==null || SoTien=='') SoTien='0';
//	var loaidonvi  = getCurrencyCodeDes(LoaiTien.trim())[1];
//	var t = SoTien.toString().split('.');
//	var kq = "";
//	if(t.length == 1)
//		return DocTienBangChu(t[0], getCurrencyCodeDes(LoaiTien.trim())[0]);
//	else if(t.length == 2){
//		if(t[1] === '00' || t[1] === '0'){
//			return DocTienBangChu(t[0], getCurrencyCodeDes(LoaiTien.trim())[0]);
//		}else{
//			return DocTienBangChu(t[0], getCurrencyCodeDes(LoaiTien.trim())[0]) + " và " + DocTienBangChu(t[1], getCurrencyCodeDes(LoaiTien.trim())[1]);
//		}
//		
//	}else
//		return "";
}

function DocTienBangChu(SoTien, loaiDonVi)
{
    var lan=0;
    var i=0;
    var so=0;
    var KetQua="";
    var tmp="";
    var ViTri = new Array();
    if(SoTien<0) return "Số tiền âm !";
    if(SoTien==0) return "Không " + loaiDonVi;
    if(SoTien>0)
    {
        so=SoTien;
    }
    else
    {
        so = -SoTien;
    }
    if (SoTien > 8999999999999999)
    {
        //SoTien = 0;
        return "Số quá lớn!";
    }
    ViTri[5] = Math.floor(so / 1000000000000000);
    if(isNaN(ViTri[5]))
        ViTri[5] = "0";
    so = so - parseFloat(ViTri[5].toString()) * 1000000000000000;
    ViTri[4] = Math.floor(so / 1000000000000);
     if(isNaN(ViTri[4]))
        ViTri[4] = "0";
    so = so - parseFloat(ViTri[4].toString()) * 1000000000000;
    ViTri[3] = Math.floor(so / 1000000000);
     if(isNaN(ViTri[3]))
        ViTri[3] = "0";
    so = so - parseFloat(ViTri[3].toString()) * 1000000000;
    ViTri[2] = parseInt(so / 1000000);
     if(isNaN(ViTri[2]))
        ViTri[2] = "0";
    ViTri[1] = parseInt((so % 1000000) / 1000);
     if(isNaN(ViTri[1]))
        ViTri[1] = "0";
    ViTri[0] = parseInt(so % 1000);
  if(isNaN(ViTri[0]))
        ViTri[0] = "0";
    if (ViTri[5] > 0)
    {
        lan = 5;
    }
    else if (ViTri[4] > 0)
    {
        lan = 4;
    }
    else if (ViTri[3] > 0)
    {
        lan = 3;
    }
    else if (ViTri[2] > 0)
    {
        lan = 2;
    }
    else if (ViTri[1] > 0)
    {
        lan = 1;
    }
    else
    {
        lan = 0;
    }
    for (i = lan; i >= 0; i--)
    {
       tmp = DocSo3ChuSo(ViTri[i]);
       KetQua += tmp;
       if (ViTri[i] > 0) KetQua += Tien[i];
       if ((i > 0) && (tmp.length > 0)) KetQua += '';//&& (!string.IsNullOrEmpty(tmp))		','
    }
   if (KetQua.substring(KetQua.length - 1) == ',')
   {
        KetQua = KetQua.substring(0, KetQua.length - 1);
   }
   KetQua = KetQua.substring(1,2).toUpperCase()+ KetQua.substring(2) + " " + loaiDonVi;
   return KetQua;//.substring(0, 1);//.toUpperCase();// + KetQua.substring(1);
}

function getCurrencyCodeDes(CurrencyCode) {

	var CurrencyCodeDes = [ "", "" ];
	if (CurrencyCode.trim() === "AUD") {
		CurrencyCodeDes[0] = "Đô la Úc";
		CurrencyCodeDes[1] = "cents";
	} else if (CurrencyCode.trim() === "JPY") {
		CurrencyCodeDes[0] = "Yên Nhật";
		CurrencyCodeDes[1] = "yen nhat";
	} else if (CurrencyCode.trim() === "LAK") {
		CurrencyCodeDes[0] = "Kip Lào";
		CurrencyCodeDes[1] = "kip lao";
	} else if (CurrencyCode.trim() === "GBP") {
		CurrencyCodeDes[0] = "Bảng Anh";
		CurrencyCodeDes[1] = "pence";
	} else if (CurrencyCode.trim() === "EUR") {
		CurrencyCodeDes[0] = "Euro";
		CurrencyCodeDes[1] = "cents";
	} else if (CurrencyCode.trim() === "USD") {
		CurrencyCodeDes[0] = "Đô la Mỹ";
		CurrencyCodeDes[1] = "cents";
	} else if (CurrencyCode.trim() === "HKD") {
		CurrencyCodeDes[0] = "Đô la Hongkong";
		CurrencyCodeDes[1] = "cents";
	} else if (CurrencyCode.trim() === "CHF") {
		CurrencyCodeDes[0] = "Franc Thụy Sỹ";
		CurrencyCodeDes[1] = "cents";
	} else if (CurrencyCode.trim() === "CAD") {
		CurrencyCodeDes[0] = "Đô la Canada";
		CurrencyCodeDes[1] = "cents";
	} else if (CurrencyCode.trim() === "SGD") {
		CurrencyCodeDes[0] = "Đô la Singapo";
		CurrencyCodeDes[1] = "cents";
	} else if (CurrencyCode.trim() === "THB") {
		CurrencyCodeDes[0] = "Bat Thai";
		CurrencyCodeDes[1] = "satang";
	} else if (CurrencyCode.trim() === "SEK") {
		CurrencyCodeDes[0] = "Cuaron Thụy Điển";
		CurrencyCodeDes[1] = "ore";
	} else if (CurrencyCode.trim() === "DKK") {
		CurrencyCodeDes[0] = "Cuaron Đan Mạch";
		CurrencyCodeDes[1] = "ore";
	} else if (CurrencyCode.trim() === "CNY") {
		CurrencyCodeDes[0] = "Nhân dân tệ";
		CurrencyCodeDes[1] = "Phan";
	} else if (CurrencyCode.trim() === "RUB") {
		CurrencyCodeDes[0] = "Rub Nga";
		CurrencyCodeDes[1] = "kopek";
	} else if (CurrencyCode.trim() === "VND") {
		CurrencyCodeDes[0] = "đồng";
		CurrencyCodeDes[1] = "đồng";
	}else if (CurrencyCode.trim() === "FKP") {
		CurrencyCodeDes[0] = "FKP";
		CurrencyCodeDes[1] = "FKP";
	} else if (CurrencyCode.trim() === "IDR") {
		CurrencyCodeDes[0] = "Rupiah Indonesia";
		CurrencyCodeDes[1] = "Rupiah";
	} else if (CurrencyCode.trim() === "KHR") {
		CurrencyCodeDes[0] = "Riel Campuchia";
		CurrencyCodeDes[1] = "Riel";
	} else if (CurrencyCode.trim() === "KRW") {
		CurrencyCodeDes[0] = "KRW";
		CurrencyCodeDes[1] = "KRW";
	} else if (CurrencyCode.trim() === "MMK") {
		CurrencyCodeDes[0] = "Kyat Myanmar";
		CurrencyCodeDes[1] = "Kyat";
	} else if (CurrencyCode.trim() === "MYR") {
		CurrencyCodeDes[0] = "Ringgit Malaysia";
		CurrencyCodeDes[1] = "Ringgit";
	} else if (CurrencyCode.trim() === "NOK") {
		CurrencyCodeDes[0] = "NOK";
		CurrencyCodeDes[1] = "NOK";
	} else if (CurrencyCode.trim() === "NZD") {
		CurrencyCodeDes[0] = "NZD";
		CurrencyCodeDes[1] = "NZD";
	} else if (CurrencyCode.trim() === "PHP") {
		CurrencyCodeDes[0] = "Peso Philippines";
		CurrencyCodeDes[1] = "Peso";
	} else {
		CurrencyCodeDes[0] = "đồng";
		CurrencyCodeDes[1] = "đồng";
	}

	return CurrencyCodeDes;
}
/*END DOC TIEN*/

function autoCompleteAdminServiceProduces(objId, callback){
	objId.autocomplete({
		source: function( request, response ) {
			$.ajax({
				url: ROOT_PATH + '/Main?transaction=common&method=getServiceProducts',
				dataType: "json",
				type: "POST",
				data: {
					q: request.term,
				},
				beforeSend: function(req) {
					initAjaxJsonArrayRequest(req);
				},
				success: function( data ) {
					response($.map(data, function (item) {
						return {
//							label: (null == item.code? ' ': item.code + ' - ') + item.name,
							label: null == item.name? ' ': item.name,
							value: null == item.name? ' ': item.name,
							
							code: item.code,
							name: item.name,
							UnitPriceNotVat: item.UnitPriceNotVat,
							VATRate: item.VATRate
						};
					}));
				}
			});
		},
		minLength : 2,
		select : function(event, ui) {
//			console.log(JSON.stringify(ui.item))
			callback(ui.item)
		},
		open: function() {
			$( this ).removeClass( "ui-corner-all" ).addClass( "ui-corner-top");
		},
		close: function() {
			$( this ).removeClass( "ui-corner-top" ).addClass( "ui-corner-all" );
		}
	});
}

/*LAY DANH SACH KHACH HANG CHO COMBOBOX - SEARCH AJAX*/
//function _getDataForSearchCustomerInvoice(term){var dataPost = {};dataPost['q'] = term;return dataPost;}
function _CustomerInvoiceFormatResult(obj){
	if(!obj || obj['loading']) return 'Đang xử lý...';
	var markup = obj.IdentificationNumber + " - " + obj.CustomerName;
	return markup;
}
function _CustomerInvoiceFormatSelection(obj) {
	if(!obj || '' == obj['id']) return '';
	return obj.CustomerName;
}
function processData(data) {
	if(data == undefined || data == null) return {results: {}};
	var mapdata = $.map(data, function (obj) {      
		obj.id = obj.id;		//CHU YEU DUA THEO TRUONG ID
		//obj.text = '[' + obj.id + '] ' + obj.name;
		return obj;
	});
	return { results: mapdata };
}
//https://stackoverflow.com/questions/14819865/select2-ajax-method-not-selecting
function _initInputSearchCustomerInvoice(jqObj, smOrLg, dataInit){
	var idParentSelect2 = null;
	
	return jqObj.select2({
		dropdownParent: null == idParentSelect2? null: $(idParentSelect2),  
		placeholder: "Search for a customers",
		minimumInputLength: 2,
		allowClear: true,
		delay: 1000,
		width: '100%',
		language : {
			inputTooShort : function() {
				return 'Vui lòng nhập dữ liệu tìm kiếm...';
			},
			noResults: function (params) {
			     return "Không tìm thấy dữ liệu";
		    }
		},
		ajax: {
			url: ROOT_PATH + '/Main?transaction=common&method=getCustomerForInputInvoice',
			dataType: 'json',
			type: "POST",
			delay: 250,
/*
			data: function (params) {
				return _getDataForSearchProvinces(params['term']);
			},
*/
            data: function(term, page) {
            	return {
            		search: term,
            		page: page || 1
            	}
            },
			beforeSend: function(req) {
				initAjaxJsonArrayRequest(req);
			},
			processResults: function (response) {
/*
 * 	processResults: function (data, page) {
				var results = [];
                $.each(JSON.parse(data.d), function (i, v) {
                        var o = {};
                        o.id = v.key;
                        o.name = v.displayName;
                        o.value = v.key;
                        results.push(o);
                    })

                    return {
                        results: results
                    };
*/
				
				return {
					results: response
				};
			},
			results: function (data) { // parse the results into the format expected by Select2.
				return { results: data };
			},
			cache: true,
		},
		//data: processData([{ "id": "HNA", "name": "Ha Noi" }]).results,
//		data: processData([{id:'aaa', name: 'test'}]).results,
		data: processData(dataInit).results,
		templateResult: _CustomerInvoiceFormatResult,
		templateSelection: _CustomerInvoiceFormatSelection,
		escapeMarkup: function (m) { return m; }
	});
}

var isDragDiv = true;
function dragToScrollDiv(_id){
	const ele = document.getElementById(_id);
    ele.style.cursor = 'grab';

    let pos = { top: 0, left: 0, x: 0, y: 0 };

    const mouseDownHandler = function(e) {
        ele.style.cursor = 'grabbing';
        ele.style.userSelect = 'none';

        pos = {
            left: ele.scrollLeft,
            top: ele.scrollTop,
            // Get the current mouse position
            x: e.clientX,
            y: e.clientY,
        };

        document.addEventListener('mousemove', mouseMoveHandler);
        document.addEventListener('mouseup', mouseUpHandler);
    };

    const mouseMoveHandler = function(e) {
    	if(!isDragDiv) return;
    	
        // How far the mouse has been moved
        const dx = e.clientX - pos.x;
        const dy = e.clientY - pos.y;

        // Scroll the element
        ele.scrollTop = pos.top - dy;
        ele.scrollLeft = pos.left - dx;
    };

    const mouseUpHandler = function() {
        ele.style.cursor = 'grab';
        ele.style.removeProperty('user-select');

        document.removeEventListener('mousemove', mouseMoveHandler);
        document.removeEventListener('mouseup', mouseUpHandler);
    };

    // Attach the handler
    ele.addEventListener('mousedown', mouseDownHandler);
}


//function myFunction() {
//	/* Get the text field */
//	var copyText = document.getElementById("myInput");
//
//	/* Select the text field */
//	copyText.select();
//	copyText.setSelectionRange(0, 99999); /* For mobile devices */
//
//	/* Copy the text inside the text field */
//	document.execCommand("copy");
//
//	/* Alert the copied text */
//	alert("Copied the text: " + copyText.value);
//}

const copyToClipboard = str => {
//function copyToClipboard(str){
	const el = document.createElement('textarea');
	el.value = str;
	el.setAttribute('readonly', '');
	el.style.position = 'absolute';
	el.style.left = '-9999px';
	document.body.appendChild(el);
	el.select();
	document.execCommand('copy');
	document.body.removeChild(el);
	
//	var hiddenClipboard = $('#_hiddenClipboard_');
//    if(!hiddenClipboard.length){
//        $('body').append('<textarea readonly style="position:absolute;top: -9999px;" id="_hiddenClipboard_"></textarea>');
//        hiddenClipboard = $('#_hiddenClipboard_');
//    }
//    hiddenClipboard.html(str);
//    hiddenClipboard.select();
//    document.execCommand('copy');
//    document.getSelection().removeAllRanges();
//    hiddenClipboard.remove();
	
	$.toast({
//      heading: 'Copied...',
      text: 'Copied the text: '+ str,
      position: 'top-right',
      loaderBg:'#ff6849',
      icon: 'success',
      hideAfter: 3000, 
      stack: 6	
    });
};

//https://kamranahmed.info/toast
/*
 * $.toast({
    text: "Don't forget to star the repository if you like it.", // Text that is to be shown in the toast
    
    icon: 'info', // Type of toast icon
    showHideTransition: 'fade', // fade, slide or plain
    allowToastClose: true, // Boolean value true or false
    hideAfter: 3000, // false to make it sticky or number representing the miliseconds as time after which toast needs to be hidden
    stack: 5, // false if there should be only one toast at a time or a number representing the maximum number of toasts to be shown at a time
    position: 'top-right', // bottom-left or bottom-right or bottom-center or top-left or top-right or top-center or mid-center or an object representing the left, right, top, bottom values
    
    
    
    textAlign: 'left',  // Text alignment i.e. left, right or center
    loader: true,  // Whether to show loader or not. True by default
    loaderBg: '#9EC600',  // Background color of the toast loader
    beforeShow: function () {}, // will be triggered before the toast is shown
    afterShown: function () {}, // will be triggered after the toat has been shown
    beforeHide: function () {}, // will be triggered before the toast gets hidden
    afterHidden: function () {}  // will be triggered after the toast has been hidden
});
 * */
function showNotificationComplete(text, iconType){
	iconType = null == iconType || '' == iconType? 'success': iconType
	var showText = '<span class="fw-700">' + (null == text || '' == text? 'Thực hiện thành công.': text) + '</span>'
	$.toast({
      text: showText,
      position: 'top-right',
      loaderBg:'#ff6849',
      icon: iconType,			//warning, success, error, info
      hideAfter: 3000, 
      stack: 6,					//SL Thông báo cùng thời điểm	
      textColor: '#ffffff',
    });
}

/*DRAG IN DIV*/
function startDrag(e) {
//		e.preventDefault();e.stopPropagation();
	// determine event object
	if (!e) {
		var e = window.event;
	}
//     if(e.preventDefault) e.preventDefault();

	// IE uses srcElement, others use target
	targ = e.target ? e.target : e.srcElement;

//		console.log('targ = ' + targ)
//		return true;
	
	if (targ.className != 'dragme') {return};
	// calculate event X, Y coordinates
	offsetX = e.clientX;
	offsetY = e.clientY;
	
	// assign default values for top and left properties
	if(!targ.style.left) { targ.style.left='0px'};
	if (!targ.style.top) { targ.style.top='0px'};

	// calculate integer values for top and left 
	// properties
	coordX = parseInt(targ.style.left);
	coordY = parseInt(targ.style.top);
	drag = true;

	// move div element
	document.onmousemove = dragDiv;
    return false;
	
}
function dragDiv(e) {
	if (!drag) {return};
//		e.preventDefault();e.stopPropagation();
	
	//DUNG TRONG TRUONG HOP DRAG DIV GIONG SCROLL PDF
	isDragDiv = false;
	
	if (!e) { var e= window.event};
	// var targ=e.target?e.target:e.srcElement;
	// move div element
	
	var left = coordX + e.clientX - offsetX;
	var top = coordY + e.clientY - offsetY;
	if(left < 0) left = 0;
	if(top < 0) top = 0;
	
//		targ.style.left=coordX+e.clientX-offsetX+'px';
	targ.style.left = left + 'px';
//		targ.style.top=coordY+e.clientY-offsetY+'px';
	targ.style.top = top + 'px';
	return false;
}
function stopDrag() {
	drag = false;
	isDragDiv = true;
}

/*END - DRAG IN DIV*/

$.fn.waitForImages = function (callback) {
    var $img = $('img', this),
    totalImg = $img.length;

    var waitImgLoad = function () {
    	totalImg--;
    	if (!totalImg) {
    		callback();
    	}
    };

    $img.each(function () {
    	if (this.complete) { 
    		waitImgLoad();
    	}
    });

    $img.load(waitImgLoad).error(waitImgLoad);
};

function getRandomInt(min, max) {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min + 1)) + min;
}