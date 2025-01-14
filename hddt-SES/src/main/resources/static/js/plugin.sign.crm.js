var version = '2.0.0.0';
var urlPluginSign = "http://localhost:11284";
var uriGetCert = "/getCert";
var signXML = "/signXML";
var signDLTK = "/signDLTKhai";
var signMultiXML = "/SignDocuments";
var signXMLMTT = "/signXMLMTT";

var serialNumber = '';
var base64Cert = '';
var callback = null;

function getCert(cb){
	base64Cert = null;
	callback = null; if(cb) callback = cb;

	var request = new XMLHttpRequest();
	
	showLoading();

	request.onreadystatechange = function() {
		if(request.readyState == 4){
//			hideLoading();
			if(request.status == 200){
				base64Cert = request.responseText;
				if(null != callback) callback(base64Cert);
			}else{
				if(null != callback) callback(null);
			}
		}
	};
	
	request.onerror = function() {
		hideLoading();
	}
	request.ontimeout = function() {
		hideLoading();
	}
	
	request.open('POST', urlPluginSign + uriGetCert, true);
	request.send(null);
}