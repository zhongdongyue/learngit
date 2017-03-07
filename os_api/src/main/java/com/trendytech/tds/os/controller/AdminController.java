package com.trendytech.tds.os.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.trendytech.tds.base.controller.BaseController;
import com.trendytech.tds.os.admin.AdminClient;
import com.trendytech.tds.util.Constants;
import com.trendytech.tds.util.JsonUtil;

@Controller
@RequestMapping("admin")
public class AdminController extends BaseController{
	/**
	 * 获取指定用户存储桶的配额信息
	 * @param username
	 * @param bucketName
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "user/quota", method = RequestMethod.GET)
	public Object getUserQuota(@RequestHeader(value = "User-Name") String username
			, @RequestHeader(value = "Password") String password
			, @RequestParam(value = "quota-type", required = false) String quotaType
			, HttpServletResponse response) {
		
		Map<String, String> user = JsonUtil.getKeys(username, password);
		AWSCredentials credentials = new BasicAWSCredentials(
				user.get("accessKey"), user.get("secretKey"));
		AdminClient adminClient = new AdminClient(credentials);
		
		String result = null;
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("uid", username);
			if (quotaType != null) {
				param.put("quota-type", quotaType);
			}
			String resp = adminClient.getQuota(username, param);
			result = new JSONObject(resp).toString(4);
		} catch (JSONException e) {
			response.setStatus(400);
			return Constants.FAIL;
		} catch (ClientProtocolException e) {
			response.setStatus(403);
			return Constants.FAIL;
		} catch (IOException e) {
			response.setStatus(403);
			return Constants.FAIL;
		}
		
		return result;
	}
	
	/**
	 * 设置指定用户存储桶的配额信息
	 * @param username
	 * @param bucketName
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "user/quota", method = RequestMethod.PUT)
	public Object setUserQuota(@RequestHeader(value = "User-Name") String username
			, @RequestHeader(value = "Password") String password
			, @RequestParam(value = "quota-type", required = false) String quotaType
			, @RequestBody String jsonObjects
			, HttpServletResponse response) {
		
		Map<String, String> user = JsonUtil.getKeys(username, password);
		AWSCredentials credentials = new BasicAWSCredentials(
				user.get("accessKey"), user.get("secretKey"));
		AdminClient adminClient = new AdminClient(credentials);
		
		try {
			JSONObject quota = new JSONObject(jsonObjects);
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("uid", username);
			if (quotaType != null) {
				boolean enabled = quota.getBoolean("enabled");
				long maxObjects = quota.getLong("max_objects");
				long maxSizeKb = quota.getLong("max_size_kb");
				param.put("quota-type", quotaType);
				adminClient.setQuota(username, quotaType, enabled, maxObjects, maxSizeKb);
			} else {
				//设置bucket配额
				JSONObject bucketQuota = quota.getJSONObject("bucket_quota");
				boolean enabledBucket = bucketQuota.getBoolean("enabled");
				long maxObjectsBucket = bucketQuota.getLong("max_objects");
				long maxSizeKbBucket = bucketQuota.getLong("max_size_kb");
				adminClient.setQuota(username, "bucket", enabledBucket, maxObjectsBucket, maxSizeKbBucket);
				//设置user配额
				JSONObject userQuota = quota.getJSONObject("user_quota");
				boolean enabledUser = userQuota.getBoolean("enabled");
				long maxObjectsUser = userQuota.getLong("max_objects");
				long maxSizeKbUser = userQuota.getLong("max_size_kb");
				adminClient.setQuota(username, "user", enabledUser, maxObjectsUser, maxSizeKbUser);
			}
		}
		catch (JSONException e) {
			response.setStatus(400);
			return Constants.FAIL;
		} catch (ClientProtocolException e) {
			response.setStatus(403);
			return Constants.FAIL;
		} catch (IOException e) {
			response.setStatus(403);
			return Constants.FAIL;
		}
		
		return SUCCESS;
	}
}
