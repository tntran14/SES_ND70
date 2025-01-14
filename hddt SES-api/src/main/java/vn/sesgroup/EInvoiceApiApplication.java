package vn.sesgroup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EInvoiceApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EInvoiceApiApplication.class, args);
	}

}
