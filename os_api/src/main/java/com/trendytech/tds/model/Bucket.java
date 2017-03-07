package com.trendytech.tds.model;

import java.util.Date;

import com.amazonaws.services.s3.model.Owner;

public class Bucket {
	private String name;
    private Date createDate;
    
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Date date) {
		this.createDate = date;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
    
}
