/*  
 * 文件名：User.java
 * 版权：<版权>
 * 描述：<描述>
 * 修改人：<修改人>
 * 修改时间：2016年10月22日
 * 修改单号：<修改单号>
 * 修改内容：<修改内容>
 * 
 */
package com.trendytech.tds.model;

import java.io.Serializable;

/**
 * 用户信息
 * @author Robin
 *
 */
public class User implements Serializable {

	private static final long serialVersionUID = -4956765048721745762L;
	
	private String name;
	private String password;
	private String accessKey;
	private String secretKey;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getAccessKey() {
		return accessKey;
	}
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}
	public String getSecretKey() {
		return secretKey;
	}
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	@Override
	public String toString() {
		return "User [name=" + name + ", password=" + password + ", accessKey=" + accessKey + ", secretKey=" + secretKey
				+ "]";
	}
	
}
