/**
 * 
 */
package com.mod.loan.util.jinyuntong;

import java.util.HashMap;
import java.util.Map;

/**
 * 代付业务枚举
 * 功能模块：代收付应用层
 * Copyright: Copyright (c) 2014 
 * Create Date: 2014年9月28日
 * @version $Id: BsntypeCollEnums.java,v 1.0 Eric.Lu Exp $
 */
public enum PayBsnTypeEnums {
	BSN_PAY_00101("00101","卡验证"),   
	BSN_PAY_00600("00600","保险理赔"), 
	BSN_PAY_00601("00601","保险分红"),     
	BSN_PAY_05100("05100","代发佣金"),     
	BSN_PAY_05101("05101","代发工资"),   
	BSN_PAY_05102("05102","代发奖金"), 
	BSN_PAY_05103("05103","代发养老金"),     
	BSN_PAY_09000("09000","基金赎回"),     
	BSN_PAY_09001("09001","基金分红"), 
	BSN_PAY_09100("09100","汇款"),     
	BSN_PAY_09200("09200","商户退款"), 
	BSN_PAY_09300("09300","信用卡还款"), 
	BSN_PAY_09400("09400","虚拟账户取现"), 
	BSN_PAY_09500("09500","货款"), 
	BSN_PAY_09501("09501","退款"); 
	
	private String code;
	private String desc;
	private PayBsnTypeEnums(String code,String desc){
		this.code = code;
		this.desc = desc;
	}

	public String getCode() {
		return code;
	}

	public String getDesc() {
		return desc;
	}
	
	public static boolean isIn(String code){
		if(code==null)return false;
		PayBsnTypeEnums[] types = PayBsnTypeEnums.values();
		for(PayBsnTypeEnums t:types){
			if(t.code.equals(code))
				return true;
		}
		return false;
	}
	
	public static String getBsnTypeDesc( String code ){
		if(code==null)return null;
		PayBsnTypeEnums[] types = PayBsnTypeEnums.values();
		for(PayBsnTypeEnums t:types){
			if(t.code.equals(code))
				return t.desc;
		}
		return null;
	}

	public static Map<String, String> getDataMap() {
		Map<String,String> keyValueMap = new HashMap<String, String>();
		for(PayBsnTypeEnums single : PayBsnTypeEnums.values()){
			keyValueMap.put(single.code, single.desc);
		}
		return keyValueMap;
	}
}
