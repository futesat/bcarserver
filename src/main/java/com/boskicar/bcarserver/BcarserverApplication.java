package com.boskicar.bcarserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@PropertySource(name="application", value="classpath:application.properties")
@Import({
    DispatcherServletAutoConfiguration.class,
	ServletWebServerFactoryAutoConfiguration.class,
	WebMvcAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    WebSocketServletAutoConfiguration.class,
    IntegrationAutoConfiguration.class,
    PropertyPlaceholderAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class
})
@EnableScheduling
@ComponentScan(basePackages={"com.boskicar.bcarserver"})
public class BcarserverApplication
{
	public static void main(String[] args) 
	{
		SpringApplication.run(BcarserverApplication.class, args);
	}
	
	@Bean
    public IntegrationFlow processUniCastUdpMessage() 
	{
	    return IntegrationFlows
	      .from(new UnicastReceivingChannelAdapter(4444))
	      .handle("BoskicarServerUDP", "handleMessage")
	      .get();
    }
}

