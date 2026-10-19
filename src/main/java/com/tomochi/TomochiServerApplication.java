package com.tomochi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TomochiServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TomochiServerApplication.class, args);
	}

}
