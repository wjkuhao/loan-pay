package com.mod.loan.util.jinyuntong;

import java.util.HashMap;
import java.util.Map;

public enum AccoutTypeEnums{
	
	PERSON("00","对私"),CORP("01","对公"),PERSON_BOOK("02","对私存折");
	
	private String code;
	private String desc;
	private AccoutTypeEnums(String code,String desc){
		this.code = code;
		this.desc = desc;
	}

	public String getCode() {
		return code;
	}

	public String getDesc() {
		return desc;
	}
	public static Map<String, String> getDataMap() {
		Map<String,String> keyValueMap = new HashMap<String, String>();
		for(AccoutTypeEnums single : AccoutTypeEnums.values()){
			keyValueMap.put(single.code, single.desc);
		}
		return keyValueMap;
	}

}
