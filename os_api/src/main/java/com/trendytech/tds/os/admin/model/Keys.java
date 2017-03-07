package com.trendytech.tds.os.admin.model;

import java.io.Serializable;

public class Keys implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String user;
	
	private String access_key;
	
	private String secret_key;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getAccess_key() {
		return access_key;
	}

	public void setAccess_key(String access_key) {
		this.access_key = access_key;
	}

	public String getSecret_key() {
		return secret_key;
	}

	public void setSecret_key(String secret_key) {
		this.secret_key = secret_key;
	}
	
	
}
