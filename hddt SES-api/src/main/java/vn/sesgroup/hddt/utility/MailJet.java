//package vn.sesgroup.hddt.utility;
//import com.mailjet.client.errors.MailjetException;
//import com.mailjet.client.errors.MailjetSocketTimeoutException;
//import com.mailjet.client.MailjetClient;
//import com.mailjet.client.MailjetRequest;
//import com.mailjet.client.MailjetResponse;
//import com.mailjet.client.ClientOptions;
//import com.mailjet.client.resource.Emailv31;
//import org.json.JSONArray;
//import org.json.JSONObject;
//public class MailJet {
//    /**
//     * This call sends a message to the given recipient with attachment.
//     */
//    public static void main(String[] args) throws MailjetException, MailjetSocketTimeoutException {
//      MailjetClient client;
//      MailjetRequest request;
//      MailjetResponse response;
//      client = new MailjetClient(System.getenv("MJ_APIKEY_PUBLIC"), System.getenv("MJ_APIKEY_PRIVATE"), new ClientOptions("v3.1"));
//      request = new MailjetRequest(Emailv31.resource)
//			.property(Emailv31.MESSAGES, new JSONArray()
//                .put(new JSONObject()
//                    .put(Emailv31.Message.FROM, new JSONObject()
//                        .put("Email", "pilot@mailjet.com")
//                        .put("Name", "Mailjet Pilot"))
//                    .put(Emailv31.Message.TO, new JSONArray()
//                        .put(new JSONObject()
//                            .put("Email", "passenger1@mailjet.com")
//                            .put("Name", "passenger 1")))
//                    .put(Emailv31.Message.SUBJECT, "Your email flight plan!")
//                    .put(Emailv31.Message.TEXTPART, "Dear passenger 1, welcome to Mailjet! May the delivery force be with you!")
//                    .put(Emailv31.Message.HTMLPART, "<h3>Dear passenger 1, welcome to <a href=\"https://www.mailjet.com/\">Mailjet</a>!</h3><br />May the delivery force be with you!")
//                    .put(Emailv31.Message.ATTACHMENTS, new JSONArray()
//                        .put(new JSONObject()
//                            .put("ContentType", "text/plain")
//                            .put("Filename", "test.txt")
//                            .put("Base64Content", "VGhpcyBpcyB5b3VyIGF0dGFjaGVkIGZpbGUhISEK")))));
//      response = client.post(request);
//      System.out.println(response.getStatus());
//      System.out.println(response.getData());
//    }
//}