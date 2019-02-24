package com.mod.loan.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "payforreq")
public class FuYouPayBean {

	private String ver;
	private String merdt;
	private String orderno;
	private String bankno;
	private String cityno;
	private String accntno;
	private String accntnm;
	private Long amt;

	public String getVer() {
		return ver;
	}

	public void setVer(String ver) {
		this.ver = ver;
	}

	public String getMerdt() {
		return merdt;
	}

	public void setMerdt(String merdt) {
		this.merdt = merdt;
	}

	public String getOrderno() {
		return orderno;
	}

	public void setOrderno(String orderno) {
		this.orderno = orderno;
	}

	public String getBankno() {
		return bankno;
	}

	public void setBankno(String bankno) {
		this.bankno = bankno;
	}

	public String getCityno() {
		return cityno;
	}

	public void setCityno(String cityno) {
		this.cityno = cityno;
	}

	public String getAccntno() {
		return accntno;
	}

	public void setAccntno(String accntno) {
		this.accntno = accntno;
	}

	public String getAccntnm() {
		return accntnm;
	}

	public void setAccntnm(String accntnm) {
		this.accntnm = accntnm;
	}

	public Long getAmt() {
		return amt;
	}

	public void setAmt(Long amt) {
		this.amt = amt;
	}

}
