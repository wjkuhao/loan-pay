package com.mod.loan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Constant {

	public static String ENVIROMENT;

	/**
	 * oss
	 * */
	public static String OSS_STATIC_BUCKET_NAME;
	public static String OSS_ACCESSKEY_ID;
	public static String OSS_ACCESS_KEY_SECRET;
	public static String OSS_ENDPOINT_OUT_URL;
	public static String OSS_ENDPOINT_IN_URL;

	/**
	 * 合利宝委托代付
	 * */
	public static String HELIPAY_ENTRUSTED_URL;
	public static String HELIPAY_ENTRUSTED_FILE_URL;

	@Value("${environment:}")
	public void setENVIROMENT(String environment) {
		Constant.ENVIROMENT = environment;
	}

	@Value("${oss.static.bucket.name:}")
	public void setOSS_STATIC_BUCKET_NAME(String oSS_STATIC_BUCKET_NAME) {
		OSS_STATIC_BUCKET_NAME = oSS_STATIC_BUCKET_NAME;
	}

	@Value("${oss.accesskey.id:}")
	public void setOSS_ACCESSKEY_ID(String oSS_ACCESSKEY_ID) {
		OSS_ACCESSKEY_ID = oSS_ACCESSKEY_ID;
	}

	@Value("${oss.accesskey.secret:}")
	public void setOSS_ACCESS_KEY_SECRET(String oSS_ACCESS_KEY_SECRET) {
		OSS_ACCESS_KEY_SECRET = oSS_ACCESS_KEY_SECRET;
	}

	@Value("${oss.endpoint.out:}")
	public void setOssEndpointOutUrl(String ossEndpointOutUrl) {
		Constant.OSS_ENDPOINT_OUT_URL = ossEndpointOutUrl;
	}

	@Value("${oss.endpoint.in:}")
	public void setOssEndpointInUrl(String ossEndpointInUrl) {
		Constant.OSS_ENDPOINT_IN_URL = ossEndpointInUrl;
	}

	@Value("${helipay.entrusted.url:}")
	public void setHelipayEntrustedUrl(String helipayEntrustedUrl) {
		Constant.HELIPAY_ENTRUSTED_URL = helipayEntrustedUrl;
	}

	@Value("${helipay.entrusted.file.url:}")
	public void setHelipayEntrustedFileUrl(String helipayEntrustedFileUrl) {
		Constant.HELIPAY_ENTRUSTED_FILE_URL = helipayEntrustedFileUrl;
	}

}
