package com.mod.loan.util.jinyuntong;

import java.util.HashMap;
import java.util.Map;


/**
 * 证件枚举
 * <p> Copyright: Copyright (c) 2014 </p>
 * <p> Create Date: 2014年8月27日 </p>
 * @version $Id: CertTypeEnums.java,v 1.0 Exp $
 */
public enum CertTypeEnums{
	
	IDCARD("01","身份证"),
	ArmyIdentityCard("02","军官证"),
	Passport("03","护照"),
	HomeVisitingCert("04","回乡证"),
	MTPs("05","台胞证"),
	OfficersCard("06","警官证"),
	MilitaryID("07","士兵证"),
	RESIDENCE_BOOKLET("08","户口本"),
	OrgCertNo("30","组织机构代码证号"),
	BUSINESS_LICENCE("31","营业执照"),
	OthersId("99","其它证件");
	
	private String code;
	private String desc;
	private CertTypeEnums(String code,String desc){
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
		for(CertTypeEnums single : CertTypeEnums.values()){
			keyValueMap.put(single.code, single.desc);
		}
		return keyValueMap;
	}
	
	public static boolean isIn(String code){
		if(code==null)return false;
		CertTypeEnums[] types = CertTypeEnums.values();
		for(CertTypeEnums t:types){
			if(t.code.equals(code))
				return true;
		}
		return false;
	}

}
