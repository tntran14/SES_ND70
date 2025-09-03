package vn.sesgroup.hddt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "vn.sesgroup.hddt")
@EnableScheduling
public class EInvoiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EInvoiceApplication.class, args);
	}

}
