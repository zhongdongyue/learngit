/**
 * 
 */
package com.trendytech.tds.os.admin.model;

import java.io.Serializable;

/**
 * @author Robin
 *
 */
public class Cap implements Serializable {

	private static final long serialVersionUID = -2177146083539094192L;
	
	public enum PERM {
		READ, WRITE, ALL
	}

	private String type = null;
	
	private String perm = null;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPerm() {
		return perm;
	}

	public void setPerm(String perm) {
		this.perm = perm;
	}
}
