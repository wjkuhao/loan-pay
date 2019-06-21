package com.mod.loan.util.jinyuntong;




/**
 * 报文头信息
 */
public class MsgHeadInfoV2 {
	
	/**报文参数，报文体密文*/
	public static final String  MSG_FIELD_MERCHANTID = "merchant_id";
	
	/**报文参数，报文体密文*/
	public static final String  MSG_FIELD_XML_ENC = "xml_enc";
    public static final String MSG_FIELD_MSG_ENC = "msg_enc";
	/**报文参数，报文密钥*/
	public static final String  MSG_FIELD_KEY_ENC = "key_enc";
	/**报文参数，报文签名*/
	public static final String  MSG_FIELD_SIGN = "sign";
	/**报文参数，xml明文*/
	public static final String MSG_FILE_XML = "xml";

    public static final String MSG_FIELD_MERCHANT_ORDER_ID = "mer_order_id";

    public static final String MSG_FIELD_MSG_TYPE = "msg_type"; // xml,json

    public static final String MSG_FIELD_MSG_TYPE_JSON = "JSON";
    
	public static final String MSG_HEAD_TRAN_TYPE_REQ_VALUE = "01";//请求报文类型
	public static final String MSG_HEAD_TRAN_TYPE_RSP_VALUE = "02";//响应报文类型
	
	/**报文头字段名称-版本*/
	public static final String MSG_HEAD_FILED_VERSION = "version";
	public static final String MSG_HEAD_FILED_TRAN_TYPE = "tran_type";
	public static final String MSG_HEAD_FILED_MERCHANT_ID = "merchant_id";
	public static final String MSG_HEAD_FILED_TRAN_DATE = "tran_date";
	public static final String MSG_HEAD_FILED_TRAN_TIME = "tran_time";
	public static final String MSG_HEAD_FILED_TRAN_FLOWID = "tran_flowid";
	public static final String MSG_HEAD_FILED_TRAN_CODE = "tran_code";
	public static final String MSG_HEAD_FILED_RESP_CODE = "resp_code";
	public static final String MSG_HEAD_FILED_RESP_DESC = "resp_desc";
	
    /** 报文XML路径，报文头 */
    public static final String MSG_JSON_HEAD = "head";
    /** 报文XML路径，报文体 */
    public static final String MSG_JSON_BODY = "body";
    
	private String version;//信息格式版本号
	private String tranType;//报文类型
	private String merchantId;//商户号 
	private String tranDate;//交易日期YYYYMMDD
	private String tranTime;//交易时间HHMMSS
	private String tranFlowid;//交易流水号
	private String tranCode;//交易代码
	private String respCode;//返回码
	private String respDesc;//返回信息描述
	
	public MsgHeadInfoV2(){
		//channelno = "99";
	}
	
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getTranType() {
		return tranType;
	}
	public void setTranType(String tranType) {
		this.tranType = tranType;
	}
	public String getMerchantId() {
		return merchantId;
	}
	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}
	public String getTranDate() {
		return tranDate;
	}
	public void setTranDate(String tranDate) {
		this.tranDate = tranDate;
	}
	public String getTranTime() {
		return tranTime;
	}
	public void setTranTime(String tranTime) {
		this.tranTime = tranTime;
	}
	public String getTranFlowid() {
		return tranFlowid;
	}
	public void setTranFlowid(String tranFlowid) {
		this.tranFlowid = tranFlowid;
	}
	public String getTranCode() {
		return tranCode;
	}
	public void setTranCode(String tranCode) {
		this.tranCode = tranCode;
	}
	public String getRespCode() {
		return respCode;
	}
	public void setRespCode(String respCode) {
		this.respCode = respCode;
	}
	public String getRespDesc() {
		return respDesc;
	}
	public void setRespDesc(String respDesc) {
		this.respDesc = respDesc;
	}
}
