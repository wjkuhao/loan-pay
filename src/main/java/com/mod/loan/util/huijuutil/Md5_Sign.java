package com.mod.loan.util.huijuutil;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * 类MD5_Sign：MD5签名和验签
 * 
 * @author Lori 2018年6月04日 下午16:10:04
 */
public class Md5_Sign {

	/**
	 * MD5签名
	 * 
	 * @param requestSign
	 *            请求签名串
	 * @param merchantKey
	 *            商户秘钥
	 */
	public static String SignByMD5(String requestSign, String merchantKey) {
		String reqHmac = "";
		try {
			reqHmac = DigestUtils.md5Hex(requestSign + merchantKey).toUpperCase();
		} catch (Exception e) {
		}
		return reqHmac;
	}
}
