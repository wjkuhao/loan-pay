package com.mod.loan.util.jinyuntong;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
/**
 * TC1002接口请求bean
 */
public class TC1002_ReqBodyBean {	

	@Size(min = 0, max = 30,message="虚拟账号不能超过30个数字")
	private String merViralAcct;
	
	@Size(min = 0, max = 30,message="协议号不能超过30个字符")
	private String agrtNo;
	
	@NotNull(message="银行名称不能为空")
	@Size(min = 1, max = 60,message="银行名称为空或超过60个字符")
	private String bankName;
	
	@NotNull(message="银行账号不能为空")
	@Size(min = 1, max = 32,message="银行账号不能超过32个字符")
	private String accountNo;
	
	@NotNull(message="银行账户名称不能为空")
	@Size(min = 1, max = 60,message="银行账户名称不能超过60个字符")
	private String accountName;
	
	@NotNull(message="账户类型不能为空")
	@Size(min = 2, max = 2,message="账户类型必须2个字符")
	private String accountType;
	
	@Size(min = 0, max = 20,message="开户行所在省不能超过20个字符")
	private String brachBankProvince;
	
	@Size(min = 0, max = 30,message="开户行所在市不能超过30个字符")
	private String brachBankCity;
	
	@Size(min = 0, max = 60,message="开户行名称不能超过60个字符")
	private String brachBankName;
	
	@Size(min = 0, max = 12,message="开户行行号不能超过12个字符")
	private String brachBankCode;
	
	@NotNull(message="交易金额不能为空")
	@DecimalMin(value="0.01",message="交易金额最小金额不能为0.00")
	@DecimalMax(value="9999999999999.99",message="交易最大金额不能超过9999999999999.99")
	@Digits(integer=13,fraction=2,message="交易金额整数部分不能超过13位，且小数部分不能超过2位")
	private BigDecimal tranAmt;
	
	@NotNull(message="币种不能为空")
	@Size(min = 3, max = 3,message="币种必须为3个字符")
	private String currency;
	
	@NotNull(message="代收业务类型不能为空")
	@Size(min = 5, max = 5,message="代收业务类型必须为5个字符")
	private String bsnCode;
	
	@Size(min = 0, max = 2,message="证件类型不能超过2个字符")
	private String certType;
	
	
	@Size(min = 0, max = 20,message="证件号不能超过20个字符")
	private String certNo;
	
	@Pattern(regexp="[0-9]*",message="手机号必须为数字")
	@Size(min = 0, max = 13,message="手机号码长度不能超过13个字符")
	private String mobile;
	
	@Size(min = 0, max = 100,message="摘要信息不能超过100个字符")
	private String remark;
	
	@Size(min = 0, max = 100,message="预留字段不能超过100个字符")
	private String reserve;

	public String getMerViralAcct() {
		return merViralAcct;
	}

	public void setMerViralAcct(String merViralAcct) {
		this.merViralAcct = merViralAcct;
	}

	public String getAgrtNo() {
		return agrtNo;
	}

	public void setAgrtNo(String agrtNo) {
		this.agrtNo = agrtNo;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getAccountNo() {
		return accountNo;
	}

	public void setAccountNo(String accountNo) {
		this.accountNo = accountNo;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public String getBrachBankProvince() {
		return brachBankProvince;
	}

	public void setBrachBankProvince(String brachBankProvince) {
		this.brachBankProvince = brachBankProvince;
	}

	public String getBrachBankCity() {
		return brachBankCity;
	}

	public void setBrachBankCity(String brachBankCity) {
		this.brachBankCity = brachBankCity;
	}

	public String getBrachBankName() {
		return brachBankName;
	}

	public void setBrachBankName(String brachBankName) {
		this.brachBankName = brachBankName;
	}

	public BigDecimal getTranAmt() {
		return tranAmt;
	}

	public void setTranAmt(BigDecimal tranAmt) {
		this.tranAmt = tranAmt;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getBsnCode() {
		return bsnCode;
	}

	public void setBsnCode(String bsnCode) {
		this.bsnCode = bsnCode;
	}

	public String getCertType() {
		return certType;
	}

	public void setCertType(String certType) {
		this.certType = certType;
	}

	public String getCertNo() {
		return certNo;
	}

	public void setCertNo(String certNo) {
		this.certNo = certNo;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getReserve() {
		return reserve;
	}

	public void setReserve(String reserve) {
		this.reserve = reserve;
	}

	public String getBrachBankCode() {
		return brachBankCode;
	}

	public void setBrachBankCode(String brachBankCode) {
		this.brachBankCode = brachBankCode;
	}
	
}
