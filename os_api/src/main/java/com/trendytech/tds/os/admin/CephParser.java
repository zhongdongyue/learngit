package com.trendytech.tds.os.admin;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.trendytech.tds.util.PropertiesUtil;
import com.trendytech.tds.util.SshClient;

public class CephParser {
	
	private SshClient client;
	
	public CephParser() {
		client = new SshClient();
		client.connect(
				PropertiesUtil.getString("os.endpoint.ip"), 
				PropertiesUtil.getString("os.endpoint.user"), 
				PropertiesUtil.getString("os.endpoint.password")
				);
	}
	
	public int setBucketQuota(String uid, String bucketName, boolean enabled, Long maxObjects,
			Long maxSize) {
		try {
			String bucketQuota = getBucketQuota(uid, bucketName);
			if (bucketQuota == null) {
				return -1;
			}
		} catch (JSONException e) {
			return 0;
		}
		String cmd = null;
		if (enabled) {
			cmd = "radosgw-admin quota enable --quota-scope=bucket --uid=" + uid + " --bucket=" + bucketName + " --max-objects=" + maxObjects + " --max-size=" + maxSize;
		} else {
			cmd = "radosgw-admin quota disable --quota-scope=bucket --uid=" + uid + " --bucket=" + bucketName;
		}
		int resultInt = 0;
		String retStr;
		int value = client.exec(cmd);
		if (value == 0) {
			retStr = client.getOutput();
			if ("".equals(retStr)) {
				resultInt = 1;
			}
		}
		return resultInt;
	}
	
	public String getBucketQuota(String userName, String bucketName) throws JSONException {
		String cmd = "radosgw-admin bucket stats --bucket=" + bucketName;
		String retStr;
		int value = client.exec(cmd);
		if (value == 0) {
			retStr = client.getOutput();
			JSONObject jsonObject = new JSONObject(retStr);
			String uid = jsonObject.getString("owner");
			if (uid != null && uid.equals(userName)) {
				String bucketQuota = jsonObject.getJSONObject("bucket_quota").toString(4);
				return bucketQuota;
			}
		}
		return null;
	}
	
//	public static void main(String[] args) throws JSONException {
//		CephParser cephParser = new CephParser();
//	}

}
