/**
 * 
 */
package com.trendytech.tds.os.admin.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Robin
 *
 */
public class User implements Serializable {

	private static final long serialVersionUID = 8060288090375670121L;

	@JsonProperty("user_id")
	private String id = null;			// 唯一标识
	
	@JsonProperty("display_name")
	private String displayName = null;	// 名称
	
	private String email = null;		// 邮箱
	
	//private int suspended = 0;			// 是否挂起
	private boolean suspended = false;
	@JsonProperty("max_buckets")
	private long maxBuckets = 0;		// 最大桶数量
	
	private List<Cap> caps = null;		// 权限
	
	private List<Keys> keys = null;
	
	private Map<String, Object> params = new HashMap<String, Object>();

	
	
	
	public boolean isSuspended() {
		return suspended;
	}

	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}

	public List<Keys> getKeys() {
		return keys;
	}

	public void setKeys(List<Keys> keys) {
		this.keys = keys;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public long getMaxBuckets() {
		return maxBuckets;
	}

	public void setMaxBuckets(long maxBuckets) {
		this.maxBuckets = maxBuckets;
	}

	public List<Cap> getCaps() {
		return caps;
	}

	public void setCaps(List<Cap> caps) {
		this.caps = caps;
	}

	/*public int getSuspended() {
		return suspended;
	}

	public void setSuspended(int suspended) {
		this.suspended = suspended;
	}*/

	public Map<String, Object> getParams() {
		return params;
	}
	@JsonAnySetter
	public void setParams(String key, Object value) {
		params.put(key, value);
	}
}
