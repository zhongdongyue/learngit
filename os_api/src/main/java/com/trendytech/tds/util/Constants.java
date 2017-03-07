package com.trendytech.tds.util;

public class Constants {
	public static String CONFIG_RUNTIME = "runtime.properties";
	public static final String OS_ENDPOINT = PropertiesUtil.getString(CONFIG_RUNTIME, "os.endpoint");
	public static final String FAIL = "fail";
	public static final String SUCCESS = "success";
	public static final String AUTH_FALUE = "auth failure, access denied.";
}
