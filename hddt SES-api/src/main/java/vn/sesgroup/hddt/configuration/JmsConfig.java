package vn.sesgroup.hddt.configuration;

import javax.jms.ConnectionFactory;

import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import vn.sesgroup.hddt.resources.JmsParams;

@Configuration
@EnableJms
@ComponentScan(basePackages = { "vn.sesgroup.hddt" })
public class JmsConfig
{
    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(JmsParams.BROKER_URL);
        connectionFactory.setUserName(JmsParams.BROKER_USERNAME);
        connectionFactory.setPassword(JmsParams.BROKER_PASSWORD);
        return connectionFactory;
    }
    
    @Bean
    public JmsTemplate jmsTemplate() {
        final JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory((ConnectionFactory)this.connectionFactory());
        return template;
    }
    
    @Bean(name = { "queue" })
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory((ConnectionFactory)this.connectionFactory());
        factory.setConcurrency("1-3");
        factory.setPubSubDomain(Boolean.valueOf(false));
        final FixedBackOff fbo = new FixedBackOff();
        fbo.setInterval(30000L);
        factory.setBackOff((BackOff)fbo);
        return factory;
    }
}

