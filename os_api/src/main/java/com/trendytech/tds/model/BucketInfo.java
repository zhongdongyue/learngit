package com.trendytech.tds.model;

public class BucketInfo {
	String creationDate;
	String owner;
	String Permission;
	public String getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getPermission() {
		return Permission;
	}
	public void setPermission(String permission) {
		Permission = permission;
	}
	@Override
	public String toString() {
		return "Bucket [creationDate=" + creationDate + ", owner=" + owner + ", Permission=" + Permission + "]";
	}
}
