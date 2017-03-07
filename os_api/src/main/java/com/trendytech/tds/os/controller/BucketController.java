package com.trendytech.tds.os.controller;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.Owner;
import com.thoughtworks.xstream.XStream;
import com.trendytech.tds.base.controller.BaseController;
import com.trendytech.tds.s3.util.BucketUtil;
import com.trendytech.tds.s3.util.ObjectUtil;
import com.trendytech.tds.util.Constants;

@Controller
@RequestMapping("/")
public class BucketController extends BaseController{
	/**
	 * 获取对应用户下所有的存储桶信息
	 * @param username
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "", method = RequestMethod.GET)
	public Object listBuckets(@RequestHeader(value = "User-Name") String username
			, @RequestHeader(value = "Password") String password
			, @RequestParam(value = "format", required = false) String format
			, HttpServletResponse response) {
		AmazonS3 client = this.client(username, password);
		List<Bucket> bucketList = client.listBuckets();
		Owner owner = null;
		List<com.trendytech.tds.model.Bucket> myBuckets = new ArrayList<>();
        for (Bucket bucket : bucketList) {
        	com.trendytech.tds.model.Bucket myBucket = new com.trendytech.tds.model.Bucket();
        	myBucket.setCreateDate(bucket.getCreationDate());
        	myBucket.setName(bucket.getName());
        	myBuckets.add(myBucket);
        	owner = bucket.getOwner();
		}
        LinkedList<Object> listAllMyBucketsResult = new LinkedList<>();
        listAllMyBucketsResult.add(owner);
        listAllMyBucketsResult.add(myBuckets);
		XStream xStream = new XStream();
		xStream.alias("Bucket", com.trendytech.tds.model.Bucket.class);
		xStream.alias("Buckets", List.class);
		xStream.alias("Owner", Owner.class);
		xStream.alias("ListAllMyBucketsResult", LinkedList.class);
		String xml = xStream.toXML(listAllMyBucketsResult);
		return xml;
	}
	/**
	 * bucket 类方法的处理
	 * @param username
	 * @param bucketName
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "{bucketName}", method = RequestMethod.POST)
	public Object postBucketMethod(@RequestHeader(value = "User-Name") String username
			, @RequestHeader(value = "Password") String password
			, @PathVariable(value = "bucketName") String bucketName
			, @RequestParam(value = "method") String method
			, HttpServletRequest request
			, HttpServletResponse response) {
		
		AmazonS3 client = this.client(username, password);
		BucketUtil bucketUtil = new BucketUtil();
		ObjectUtil objectUtil = new ObjectUtil();
		try {
			if ("PUT".equals(method.toUpperCase())) {
				return bucketUtil.putBucket(client, bucketName, request, response);
			} else if ("PUTACL".equals(method.toUpperCase())) {
				return bucketUtil.setBucketAcl(client, bucketName, request, response);
			} else if ("LIFECYCLE".equals(method.toUpperCase())){
				return bucketUtil.setBucketLifeCycle(client, bucketName, request, response);
			} else if ("QUOTA".equals(method.toUpperCase())) {
				return bucketUtil.setBucketQuota(client, bucketName, request, response);
			} else if ("MUTIDELETE".equals(method.toUpperCase())) {
				return objectUtil.deleteMutiObject(client, bucketName, request, response);
			} else if ("MULTIPUT".equals(method.toUpperCase())) {
				return objectUtil.putMultiObject(client, bucketName, request, response);
			}
		} catch (Exception e) {
			response.setStatus(400);
		}
		
		return Constants.FAIL;
	}
	
	@ResponseBody
	@RequestMapping(value = "{bucketName}", method = RequestMethod.GET)
	public Object getBucketMethod(@RequestHeader(value = "User-Name") String username
			, @RequestHeader(value = "Password") String password
			, @PathVariable(value = "bucketName") String bucketName
			, @RequestParam(value = "method", required=false) String method
			, HttpServletRequest request
			, HttpServletResponse response) {
		
		AmazonS3 client = this.client(username, password);
		BucketUtil bucketUtil = new BucketUtil();
		try {
			if (method == null || "GET".equals(method.toUpperCase())){
				return bucketUtil.getBucketObjectList(client, bucketName, request, response);
			}else if ("GETACL".equals(method.toUpperCase())) {
				return bucketUtil.getBucketAcl(client, bucketName, request, response);
			} else if ("LIFECYCLE".equals(method.toUpperCase())) {
				return bucketUtil.getBucketLifeCycle(client, bucketName, request, response);
			}  else if ("GETBUCKETINFO".equals(method.toUpperCase())) {
				return bucketUtil.getBucketInfo(client, bucketName, request, response);
			} else if ("QUOTA".equals(method.toUpperCase())) {
				return bucketUtil.getBucketQuota(client, bucketName, request, response);
			}
		} catch (Exception e) {
			response.setStatus(400);
		}
		
		return Constants.FAIL;
	}
	
	@ResponseBody
	@RequestMapping(value = "{bucketName}", method = RequestMethod.DELETE)
	public Object deleteBucketMethod(@RequestHeader(value = "User-Name") String username
			, @RequestHeader(value = "Password") String password
			, @PathVariable(value = "bucketName") String bucketName
			, @RequestParam(value = "method" ,required=false) String method
			, HttpServletRequest request
			, HttpServletResponse response) {
		
		AmazonS3 client = this.client(username, password);
		BucketUtil bucketUtil = new BucketUtil();
		try {
			if (method == null || "DELETE".equals(method.toUpperCase())) {
			    return bucketUtil.deleteBucket(client, bucketName, request, response);
			} else if ("FORCEDELETE".equals(method.toUpperCase())) {
				return bucketUtil.forceDeleteBucket(client, bucketName, request, response);
			}
		} catch (Exception e) {
			response.setStatus(400);
		}
		
		return Constants.FAIL;
	}
}
