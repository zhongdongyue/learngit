package com.trendytech.tds.os.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.amazonaws.services.s3.AmazonS3;
import com.trendytech.tds.base.controller.BaseController;
import com.trendytech.tds.s3.util.ObjectUtil;
import com.trendytech.tds.util.Constants;

@Controller
@RequestMapping("/")
public class ObjectController  extends BaseController{
	/**
	 * 文件post方法
	 */
	@ResponseBody
	@RequestMapping(value = "{bucketName}/**", method = RequestMethod.POST)
	public Object postObjectMethod(@RequestHeader(value = "User-Name") String username
			, @RequestHeader(value = "Password") String password
			, @PathVariable(value = "bucketName") String bucketName
			, @RequestParam(value = "method", required=false) String method
			, HttpServletRequest request
			, HttpServletResponse response) {

		AmazonS3 client = this.client(username, password);
		ObjectUtil objectUtil = new ObjectUtil();
		
		String key = objectUtil.getKey(bucketName, request);

		try {
			if (method == null || "PUT".equals(method.toUpperCase())) {
				return objectUtil.putObject(client, bucketName, key, request, response);
			} else if ("PUTACL".equals(method.toUpperCase())) {
				return objectUtil.setObjectAcl(client, bucketName, key, request, response);
			} else if ("APPEND".equals(method.toUpperCase())) {
				return objectUtil.mutiPartUpload(client, bucketName, key, request, response);
			}
		} catch (Exception e) {
			response.setStatus(400);
		}

		return Constants.FAIL;
	}
	
	/**
	 * 文件get方法
	 */
	@ResponseBody
	@RequestMapping(value = "{bucketName}/**", method = RequestMethod.GET)
	public Object getObjectMethod(@RequestHeader(value = "User-Name") String username
			, @RequestHeader(value = "Password") String password
			, @PathVariable(value = "bucketName") String bucketName
			, @RequestParam(value = "method", required=false) String method
			, HttpServletRequest request
			, HttpServletResponse response) {

		AmazonS3 client = this.client(username, password);
		ObjectUtil objectUtil = new ObjectUtil();
		
		String key = objectUtil.getKey(bucketName, request);

		try {
			if (method == null || "GET".equals(method.toUpperCase())) {
				return objectUtil.downloadObject(client, bucketName, key, request, response);
			} else if ("GETACL".equals(method.toUpperCase())) {
				return objectUtil.getObjectAcl(client, bucketName, key, request, response);
			} else if ("INITAPPEND".equals(method.toUpperCase())){
				return objectUtil.initMutiPartUpload(client, bucketName, key, request, response);
			} else if ("FINISHAPPEND".equals(method.toUpperCase())) {
				return objectUtil.finishMutiPartUpload(client, bucketName, key, request, response);
			} else if ("ABORTAPPEND".equals(method.toUpperCase())){
				return objectUtil.abortMutiPartUpload(client, bucketName, key, request, response);
			}
		} catch (Exception e) {
			response.setStatus(400);
		}

		return Constants.FAIL;
	}
	
	/**
	 * 文件delete方法
	 */
	@ResponseBody
	@RequestMapping(value = "{bucketName}/**", method = RequestMethod.DELETE)
	public Object deleteObjectMethod(@RequestHeader(value = "User-Name") String username
			, @RequestHeader(value = "Password") String password
			, @PathVariable(value = "bucketName") String bucketName
			, @RequestParam(value = "method" ,required=false) String method
			, HttpServletRequest request
			, HttpServletResponse response) {

		AmazonS3 client = this.client(username, password);
		ObjectUtil objectUtil = new ObjectUtil();
		
		String key = objectUtil.getKey(bucketName, request);

		try {
			if (method == null || "DELETE".equals(method.toUpperCase())) {
				return objectUtil.deleteObject(client, bucketName, key, request, response);
			}
		} catch (Exception e) {
			response.setStatus(400);
		}

		return Constants.FAIL;
	}
	
	/**
	 * 文件head方法
	 */
	@ResponseBody
	@RequestMapping(value = "{bucketName}/**", method = RequestMethod.HEAD)
	public Object headObjectMethod(@RequestHeader(value = "User-Name") String username
			, @RequestHeader(value = "Password") String password
			, @PathVariable(value = "bucketName") String bucketName
			, @RequestParam(value = "method" ,required=false) String method
			, HttpServletRequest request
			, HttpServletResponse response) {

		AmazonS3 client = this.client(username, password);
		ObjectUtil objectUtil = new ObjectUtil();
		
		String key = objectUtil.getKey(bucketName, request);

		try {
			if (method == null || "HEAD".equals(method.toUpperCase())) {
				return objectUtil.getObjectHead(client, bucketName, key, request, response);
			}
		} catch (Exception e) {
			response.setStatus(400);
		}

		return Constants.FAIL;
	}
}
