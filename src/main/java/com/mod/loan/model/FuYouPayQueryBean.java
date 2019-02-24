package com.mod.loan.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "qrytransreq")
public class FuYouPayQueryBean {

	private String ver;
	private String busicd;
	private String orderno;
	private String startdt;
	private String enddt;

	public String getVer() {
		return ver;
	}

	public void setVer(String ver) {
		this.ver = ver;
	}

	public String getBusicd() {
		return busicd;
	}

	public void setBusicd(String busicd) {
		this.busicd = busicd;
	}

	public String getOrderno() {
		return orderno;
	}

	public void setOrderno(String orderno) {
		this.orderno = orderno;
	}

	public String getStartdt() {
		return startdt;
	}

	public void setStartdt(String startdt) {
		this.startdt = startdt;
	}

	public String getEnddt() {
		return enddt;
	}

	public void setEnddt(String enddt) {
		this.enddt = enddt;
	}

}
