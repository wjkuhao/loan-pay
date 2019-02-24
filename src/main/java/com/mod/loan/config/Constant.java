package com.mod.loan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Constant {

	public static String ENVIROMENT;

	@Value("${environment:}")
	public void setENVIROMENT(String environment) {
		Constant.ENVIROMENT = environment;
	}

}
