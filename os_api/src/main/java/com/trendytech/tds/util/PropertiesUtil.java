package com.trendytech.tds.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
* Properties文件处理类
*/ 
public class PropertiesUtil {
	private static Logger log = LoggerFactory.getLogger(PropertiesUtil.class);
	private static Properties prop;

	public static Properties loadFromPropertie(String propName){
		prop = new Properties();
		InputStream inputFile = null;
		try {
			URL url = PropertiesUtil.class.getClassLoader().getResource(propName);
			inputFile = new FileInputStream(url.getFile()); 
			prop.load(inputFile);
		} catch (FileNotFoundException ex) {
			log.error("读取属性文件--->失败！- 原因：文件路径错误或者文件不存在", ex);
		} catch (IOException ex) {
			log.error("装载文件--->失败!", ex);
		} finally {
			if (inputFile != null) {
				try {
					inputFile.close();
				} catch (IOException ex) {
					log.error("关闭文件--->失败!", ex);
				}
			}
		}
		return prop;
	}
	
	/**
	 * 读取配置文件中的字符串
	 * @param propName 配置文件名称
	 * @param key 键
	 * @param defaultValue 默认值
	 * @return
	 * @since v1.2.1
	 */
	public static String getString(String propName, String key, String defaultValue){
		prop = loadFromPropertie(propName);
		String value = prop.getProperty(key);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}
	
	public static String getString(String propName, String key){
		prop = loadFromPropertie(propName);
		String value = prop.getProperty(key);
		return value;
	}
	
	public static String getString(String key){
		prop = loadFromPropertie(Constants.CONFIG_RUNTIME);
		String value = prop.getProperty(key);
		return value;
	}
	
	public static int getInt(String propName, String key, int defaultValue){
		prop = loadFromPropertie(propName);
		String value = prop.getProperty(key);
		if (value == null) {
			return defaultValue;
		}
		return Integer.valueOf(value);
	}
	
	public static int getInt(String propName, String key){
		prop = loadFromPropertie(propName);
		String value = prop.getProperty(key);
		if (value == null) {
			return 0;
		}
		return Integer.valueOf(value);
	}
	
	public static int getInt(String key){
		prop = loadFromPropertie(Constants.CONFIG_RUNTIME);
		String value = prop.getProperty(key);
		if (value == null) {
			return 0;
		}
		return Integer.valueOf(value);
	}
	
	public static Boolean getBoolean(String propName, String key, Boolean defaultValue){
		prop = loadFromPropertie(propName);
		String value = prop.getProperty(key);
		if (value == null) {
			return false;
		}
		return Boolean.valueOf(value);
	}
	
	public static Boolean getBoolean(String propName, String key){
		prop = loadFromPropertie(propName);
		String value = prop.getProperty(key);
		if (value == null) {
			return false;
		}
		return Boolean.valueOf(value);
	}
	
	public static Boolean getBoolean(String key){
		prop = loadFromPropertie(Constants.CONFIG_RUNTIME);
		String value = prop.getProperty(key);
		if (value == null) {
			return false;
		}
		return Boolean.valueOf(value);
	}
	
}