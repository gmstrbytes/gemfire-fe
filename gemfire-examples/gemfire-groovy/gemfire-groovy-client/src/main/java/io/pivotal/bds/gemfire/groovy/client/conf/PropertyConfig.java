package io.pivotal.bds.gemfire.groovy.client.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PropertyConfig {

	@Value("${locators:localhost:10334}")
	public String locators;
	
    @Value("${rabbitHost:localhost}")
	public String rabbitHost;
    
    @Value("${rabbitPort:5672}")
	public int rabbitPort;
    
    @Value("${rabbitUsername:admin}")
    public String rabbitUsername;
    
    @Value("${rabbitPassword:admin}")
    public String rabbitPassword;
}
