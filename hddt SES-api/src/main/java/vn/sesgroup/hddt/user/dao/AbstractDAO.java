package vn.sesgroup.hddt.user.dao;

import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.api.message.MsgPage;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.utility.Commons;

public class AbstractDAO {
	public Commons commons = new Commons();
	
	public List<Document> createFacetForSearchNotSort(MsgPage page){
		List<Document> r = new ArrayList<Document>();
		
		int pageNo = commons.calcPageNo(page.getPageNo());
		int size = commons.calcPageSize(page.getSize());
		
//		if(null == page.getFieldSort() || "".equals(page.getFieldSort()))
//			r.add(new Document("$sort", new Document("_id", -1)));
//		else
//			r.add(new Document("$sort", new Document(page.getFieldSort(), page.getTypeSort())));

		r.add(
			new Document("$facet", 
				new Document("meta", 
					Arrays.asList(
						new Document("$count", "total")
					)
				)
				.append("data", 
					Arrays.asList(
						new Document("$skip", size * (pageNo - 1))
						, new Document("$limit", size)
					)
				)
			)
		);
		r.add(new Document("$unwind", "$meta"));
		r.add(new Document("$project", new Document("total", "$meta.total").append("data", 1)));
		
		return r;
	}
	
	public List<Document> createFacetForSearch(MsgPage page){
		List<Document> r = new ArrayList<Document>();
		
		int pageNo = commons.calcPageNo(page.getPageNo());
		int size = commons.calcPageSize(page.getSize());
		
		if(null == page.getFieldSort() || "".equals(page.getFieldSort()))
			r.add(new Document("$sort", new Document("_id", -1)));
		else
			r.add(new Document("$sort", new Document(page.getFieldSort(), page.getTypeSort())));

		r.add(
			new Document("$facet", 
				new Document("meta", 
					Arrays.asList(
						new Document("$count", "total")
					)
				)
				.append("data", 
					Arrays.asList(
						new Document("$skip", size * (pageNo - 1))
						, new Document("$limit", size)
					)
				)
			)
		);
		r.add(new Document("$unwind", "$meta"));
		r.add(new Document("$project", new Document("total", "$meta.total").append("data", 1)));
		
		return r;
	}
	
	public List<Document> createFacetForSearch(MsgPage page, String defaultField){
		List<Document> r = new ArrayList<Document>();
		
		int pageNo = commons.calcPageNo(page.getPageNo());
		int size = commons.calcPageSize(page.getSize());
		
		if(null == page.getFieldSort() || "".equals(page.getFieldSort()))
			r.add(new Document("$sort", new Document(defaultField, -1)));
		else
			r.add(new Document("$sort", new Document(page.getFieldSort(), page.getTypeSort())));

		r.add(
			new Document("$facet", 
				new Document("meta", 
					Arrays.asList(
						new Document("$count", "total")
					)
				)
				.append("data", 
					Arrays.asList(
						new Document("$skip", size * (pageNo - 1))
						, new Document("$limit", size)
					)
				)
			)
		);
		r.add(new Document("$unwind", "$meta"));
		r.add(new Document("$project", new Document("total", "$meta.total").append("data", 1)));
		
		return r;
	}
	
	public List<Document> createFacetForSearchNotSortNotPage(){
		List<Document> r = new ArrayList<Document>();

		r.add(
				new Document("$facet", 
					new Document("meta", 
						Arrays.asList(
							new Document("$count", "total")
						)
					)
					.append("data", 
						Arrays.asList(
//							new Document("$skip", size * (pageNo - 1))
//							, 
//								new Document("$limit", "$meta.total")
						)
					)
				)
			);
		r.add(new Document("$unwind", "$meta"));
		r.add(new Document("$project", new Document("total", "$meta.total").append("data", 1)));
		
		return r;
	}
	
	
	public String removeAccent(String s) {
		  
		  String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
		  Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		  return pattern.matcher(temp).replaceAll("");
		 }
	
	//CHUYEN SO SANG CHU
	public static HashMap<String, String> hm_tien = new HashMap<String, String>() {
		/**
		* 
		*/
		private static final long serialVersionUID = 1L;

		{
			put("0", "không");
			put("1", "một");
			put("2", "hai");
			put("3", "ba");
			put("4", "bốn");
			put("5", "năm");
			put("6", "sáu");
			put("7", "bảy");
			put("8", "tám");
			put("9", "chín");
		}
	};
	public static HashMap<String, String> hm_hanh = new HashMap<String, String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			put("1", "đồng");
			put("2", "mươi");
			put("3", "trăm");
			put("4", "nghìn");
			put("5", "mươi");
			put("6", "trăm");
			put("7", "triệu");
			put("8", "mươi");
			put("9", "trăm");
			put("10", "tỷ");
			put("11", "mươi");
			put("12", "trăm");
			put("13", "nghìn");
			put("14", "mươi");
			put("15", "trăm");

		}
	};

	public String ChuyenSangChu(String x) {
		String kq = "";
		x = x.replace(".", "");
		String arr_temp[] = x.split(",");
		if (!NumberUtils.isNumber(arr_temp[0])) {
			return "";
		}
		String m = arr_temp[0];
		int dem = m.length();
		String dau = "";
		int flag10 = 1;
		while (!m.equals("")) {
			if (m.length() <= 3 && m.length() > 1 && Long.parseLong(m) == 0) {

			} else {
				dau = m.substring(0, 1);
				if (dem % 3 == 1 && m.startsWith("1") && flag10 == 0) {
					kq += "mốt ";
					flag10 = 0;
				} else if (dem % 3 == 2 && m.startsWith("1")) {
					kq += "mười ";
					flag10 = 1;
				} else if (dem % 3 == 2 && m.startsWith("0") && m.length() >= 2 && !m.substring(1, 2).equals("0")) {
					// System.out.println("a "+m.substring(1, 2));
					kq += "lẻ ";
					flag10 = 1;
				} else {
					if (!m.startsWith("0")) {
						kq += hm_tien.get(dau) + " ";
						flag10 = 0;
					}
				}
				if (dem % 3 != 1 && m.startsWith("0") && m.length() > 1) {
				} else {
					if (dem % 3 == 2 && (m.startsWith("1") || m.startsWith("0"))) {// mười
					} else {
						kq += hm_hanh.get(dem + "") + " ";
					}
				}
			}
			m = m.substring(1);
			dem = m.length();
		}
		kq = kq.substring(0, kq.length() - 1);
		return kq;
	}

	public static String currencyFormat(String curr) {
		try {
			double vaelue = Double.parseDouble(curr);
			String pattern = "###,###";
			DecimalFormat myFormatter = new DecimalFormat(pattern);
			String output = myFormatter.format(vaelue);
			return output;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return "";

	}
	
	public int getValueForNextSequenceAdmin(MongoTemplate mongoTemplate, String issuerId, String key, String maCQT) throws Exception{
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(true);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		Document docR = mongoTemplate.getCollection("LogNextSequence").findOneAndUpdate(
				new Document("IssuerId", issuerId)
					.append("Key", key)
					.append("MaCQT", maCQT)
				, new Document("$inc", new Document("seq", 1))
				, options);
		
		return docR.getInteger("seq", 0);
	}
}
