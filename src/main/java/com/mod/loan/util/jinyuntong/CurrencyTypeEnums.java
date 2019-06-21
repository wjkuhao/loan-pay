package com.mod.loan.util.jinyuntong;

import java.util.HashMap;
import java.util.Map;

/**
 * 币种枚举
 * <p> Copyright: Copyright (c) 2014 </p>
 * <p> Create Date: 2014年8月27日 </p>
 * @version $Id: CurrencyTypeEnums.java,v 1.0 zq Exp $
 */
public enum CurrencyTypeEnums{
	
	CNY("CNY","人民币");
	
	private String code;
	private String desc;
	private CurrencyTypeEnums(String code,String desc){
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
		for(CurrencyTypeEnums single : CurrencyTypeEnums.values()){
			keyValueMap.put(single.code, single.desc);
		}
		return keyValueMap;
	}
	
	public static boolean isIn(String code){
		if(code==null)return false;
		CurrencyTypeEnums[] types = CurrencyTypeEnums.values();
		for(CurrencyTypeEnums t:types){
			if(t.code.equals(code))
				return true;
		}
		return false;
	}

	/**
	 * 获取ISO 4217标准中的货币代码，其中人民币为 156
	 * <p> Create Date: 2014年9月10日 </p>
	 * @param curCode
	 * @return
	 */
	public static Integer getISOCode( String curCode ){
		Integer isoCode = 0 ;
		if(curCode.equals(CNY.code))
			isoCode = 156 ;
		
		return isoCode ;
	}
}
