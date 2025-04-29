package vn.sesgroup.hddt.resources;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Order(value = 11)
public class HttpClientConfig {
	@Bean
    public RestTemplate customRestTemplate() {
    	 HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
         httpRequestFactory.setConnectionRequestTimeout(APIParams.HTTP_CLIENT_REQUEST_TIMEOUT);
         httpRequestFactory.setConnectTimeout(APIParams.HTTP_CLIENT_CONNECT_TIMEOUT);
         httpRequestFactory.setReadTimeout(APIParams.HTTP_CLIENT_READ_TIMEOUT);

         return new RestTemplate(httpRequestFactory);
    }
}
