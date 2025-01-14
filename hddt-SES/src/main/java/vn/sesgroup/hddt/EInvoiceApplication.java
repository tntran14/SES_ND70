package vn.sesgroup.hddt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
//https://stackjava.com/spring-boot/spring-boot-tuy-chinh-trang-whitelabel-error-page.html
//https://stackjava.com/spring/spring-mvc-exception-handling-xu-ly-exception-trong-spring-mvc.html
//@EnableAutoConfiguration(exclude = {
//	ErrorMvcAutoConfiguration.class
//})


@SpringBootApplication(scanBasePackages = "vn.sesgroup.hddt")

public class EInvoiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EInvoiceApplication.class, args);
	}

	
//	public static void check() {
//		int a = 245 % 1000;
//		 
//		String[] units = new String[]{"", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín", "mười", "mười một", "mười hai", "mười ba", "mười bốn", "mười lăm", "mười sáu", "mười bảy", "mười tám", "mười chín"};
//		int hundreds = a / 100;
//		int tens = (a % 100) / 10;
//		int unitsDigit = a % 10;
//
//		String partWords = "";
//
//		if (hundreds > 0) {
//		    partWords += units[hundreds] + " trăm ";
//		}
//		if (tens == 0 && unitsDigit == 0) {
//		    partWords += "";
//		} else if (tens == 0) {
//		    partWords += "linh " + units[unitsDigit];
//		} else if (tens == 1) {
//		    partWords += "mười " + units[unitsDigit];
//		} else {
//		    partWords += units[tens] + " mươi ";
//		    if (unitsDigit == 1) {
//		        partWords += "mốt";
//		    } else if (unitsDigit == 5) {
//		    	partWords += "lăm";
//		    } else if (unitsDigit > 1) {
//		        partWords += units[unitsDigit];
//		    }
//		}
//		
//		
//		System.out.println("hundreds:" + hundreds);
//		System.out.println("tens:" + tens);
//		System.out.println("unitsDigit:" + unitsDigit);
//		System.out.println("Result:" + partWords);
//	}
}
