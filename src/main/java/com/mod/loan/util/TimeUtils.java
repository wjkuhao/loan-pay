package com.mod.loan.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtils {

	public final static String dateformat0 = "yyyy-MM-dd HH:mm";
	public final static String dateformat1 = "yyyy-MM-dd HH:mm:ss";
	public final static String dateformat2 = "yyyy-MM-dd";
	public final static String dateformat3 = "EEE MMM dd HH:mm:ss zzzZZZ yyyy";
	public final static String dateformat4 = "yyyyMMdd";
	public final static String dateformat5 = "yyyyMMddHHmmss";
	public final static String dateformat6 = "yyyyMMddHHmmssSSS";
    public final static String dateformat7 = "HHmm";
    public final static String dateformat8 = "HHmmss";

	private TimeUtils() {
		throw new Error("can't instance this tool class");
	}

	public static String getTime() {
		SimpleDateFormat sdf = new SimpleDateFormat(dateformat1);
		return sdf.format(new Date());
	}

	public static String parseTime(Date date, String dateformat) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateformat);
		return sdf.format(date);
	}

	public static Date parseTime(String str, String dateformat) {
		SimpleDateFormat sf = new SimpleDateFormat(dateformat);
		Date d = null;
		try {
			d = sf.parse(str);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return d;
	}

	/**
	 * 获取昨日时间
	 *
	 * @return
	 */
	public static Date getYesterday() {
		Date now = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(now);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_YEAR);
		calendar.clear();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DAY_OF_YEAR, day - 1);
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 00);
		Date yesterday = calendar.getTime();
		return yesterday;
	}

	/**
	 * 获取明日时间
	 *
	 * @return
	 */
	public static Date getTomorrow() {
		Date now = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(now);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_YEAR);
		calendar.clear();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DAY_OF_YEAR, day + 1);
		calendar.set(Calendar.HOUR, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		Date yesterday = calendar.getTime();
		return yesterday;
	}
}
