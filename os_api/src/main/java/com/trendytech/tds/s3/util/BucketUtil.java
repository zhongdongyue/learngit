package com.trendytech.tds.s3.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.LambdaConfiguration;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.util.FakeIOException;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.thoughtworks.xstream.XStream;
import com.trendytech.tds.model.BucketInfo;
import com.trendytech.tds.os.admin.AdminClient;
import com.trendytech.tds.os.admin.CephParser;
import com.trendytech.tds.util.Constants;
import com.trendytech.tds.util.JsonUtil;

public class BucketUtil {
	/**
	 * 设置bucket
	 * @param client
	 * @param bucketName
	 * @param request
	 * @param response
	 * @return
	 */
    public String putBucket(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response) {
    	String contentType = request.getContentType();
    	String acl = request.getHeader("x-oss-acl");
    	String region = null;
    	
		/* 从用户传过来的数据中解析出地区信息
		 * <?xmlversion="1.0"encoding="UTF-8"?>   	
		 * <CreateBucketConfiguration >
		 *     <LocationConstraint>oss-cn-hangzhou</LocationConstraint>
		 * </CreateBucketConfiguration >
		*/
    	if (contentType != null &&contentType.toLowerCase().contains("xml")) {
			try {
				InputStream inputStream = request.getInputStream();
				String xmlStr = getStrFromInputSteam(inputStream);
				Document doc = (Document) DocumentHelper.parseText(xmlStr);
				Element root = doc.getRootElement();
				if (!"CreateBucketConfiguration".equals(root.getName())) {
					throw new Exception();
				}
				@SuppressWarnings("rawtypes")
				Iterator Elements = root.elementIterator();
				while (Elements.hasNext()) {
					Element regionElem = (Element) Elements.next();
					if (!"LocationConstraint".equals(regionElem.getName())) {
						throw new Exception();
					}
					region = regionElem.getText();
					break;
				}
			} catch (Exception e) {
				response.setStatus(400);
				return Constants.FAIL;
			}
		}
    	
    	try {
    		// 创建bucket时，如果有设置地区，则设置地区
        	if (region != null) {
        		client.createBucket(bucketName, region);
        		
    		} else {
    			client.createBucket(bucketName);
    		}
        	// 创建bucket时，如果有设置acl，则设置acl
        	if (acl != null) {
        		if ("public-read-write".equals(acl.toLowerCase())) {
    				client.setBucketAcl(bucketName, CannedAccessControlList.PublicReadWrite);
    			} else if ("public-read".equals(acl.toLowerCase())) {
    				client.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
    			} else if ("private".equals(acl.toLowerCase())) {
    				client.setBucketAcl(bucketName, CannedAccessControlList.Private);
    			}
    		}
    	} catch(Exception e){
    		response.setStatus(406);
			return Constants.FAIL;
    	}
    	
    	return Constants.SUCCESS;
    }
    /**
     * 设置bucket acl
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public String setBucketAcl(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response){
    	String aclStr = request.getHeader("x-oss-acl");
    	String contentType = request.getContentType();
    	String username = request.getHeader("User-Name");
    	
    	/* 从用户传过来的数据中解析出acl信息
		*/
    	List<Grant> grantList = new ArrayList<Grant>();
    	if (contentType != null &&contentType.toLowerCase().contains("xml")) {
			try {
				InputStream inputStream = request.getInputStream();
				String xmlStr = getStrFromInputSteam(inputStream);
				Document doc = (Document) DocumentHelper.parseText(xmlStr);
				Element root = doc.getRootElement();
				@SuppressWarnings("rawtypes")
				Iterator Elements = root.elementIterator();
				while (Elements.hasNext()) {
					Element grant = (Element) Elements.next();
					
					Element grantee = grant.element("Grantee");
					Element id = grantee.element("ID");
					
					Element permission = grant.element("Permission");
					
					Grant grantsVarArg = null;
					if ("FULL_CONTROL".equals(permission.getText())) {
						grantsVarArg = new Grant(new CanonicalGrantee(id.getText()), Permission.FullControl);
					} else if ("WRITE".equals(permission.getText())) {
						grantsVarArg = new Grant(new CanonicalGrantee(id.getText()), Permission.Write);
					} else if ("WRITE_ACP".equals(permission.getText())) {
						grantsVarArg = new Grant(new CanonicalGrantee(id.getText()), Permission.WriteAcp);
					} else if ("READ".equals(permission.getText())) {
						grantsVarArg = new Grant(new CanonicalGrantee(id.getText()), Permission.Read);
					} else if ("READ_ACP".equals(permission.getText())) {
						grantsVarArg = new Grant(new CanonicalGrantee(id.getText()), Permission.ReadAcp);
					}
					
					if (grantsVarArg != null) {
						grantList.add(grantsVarArg);
					}
				}	
			} catch (Exception e) {
				response.setStatus(400);
				return Constants.FAIL;
			}
		}
    	
    	try {
        	// 创建bucket时，如果有设置acl，则设置acl
        	if (aclStr != null) {
        		if ("public-read-write".equals(aclStr.toLowerCase())) {
    				client.setBucketAcl(bucketName, CannedAccessControlList.PublicReadWrite);
    			} else if ("public-read".equals(aclStr.toLowerCase())) {
    				client.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
    			} else if ("private".equals(aclStr.toLowerCase())) {
    				client.setBucketAcl(bucketName, CannedAccessControlList.Private);
    			}
    		}
        	
        	if (grantList.size() > 0) {
				AccessControlList acl = new AccessControlList();
				Grant[] grants = new Grant[grantList.size()];
				grantList.toArray(grants);
				acl.grantAllPermissions(grants);
				Owner owner = new Owner();
				owner.setId(username);
				acl.setOwner(owner);
				client.setBucketAcl(bucketName, acl);
			}
        	
        	//如果两种acl方式都没有的话，则为错误的请求
        	if (aclStr == null && grantList.size() <= 0) {
				throw new Exception();
			}
    	} catch(Exception e){
    		response.setStatus(400);
			return Constants.FAIL;
    	}
    	
    	return Constants.SUCCESS;
    }
    
    /**
     * 设置bucket lifecycle
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public String setBucketLifeCycle(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response){
    	String contentType = request.getContentType();
    	
    	/* 从用户传过来的数据中解析出地区信息
		 * <?xml version="1.0" encoding="UTF-8"?>
		 * <LifecycleConfiguration>
		 *     <Days>n</Days>
		 * </LifecycleConfiguration>
		*/
		try {
			if (contentType != null &&contentType.toLowerCase().contains("xml")) {
				List<Rule> rules = new ArrayList<BucketLifecycleConfiguration.Rule>();
				InputStream inputStream = request.getInputStream();
				String xmlStr = getStrFromInputSteam(inputStream);
				Document doc = (Document) DocumentHelper.parseText(xmlStr);
				Element lifecycle = doc.getRootElement();
				if (!"LifecycleConfiguration".equals(lifecycle.getName())) {
					throw new Exception();
				}
				@SuppressWarnings("rawtypes")
				Iterator Elements = lifecycle.elementIterator();  
	            while(Elements.hasNext()){   
	                Element object = (Element)Elements.next();
	                if ("days".equals(object.getName().toLowerCase())) {
	                	Rule rule = new Rule();
	                	rule.setExpirationInDays(Integer.parseInt(object.getText().trim()));
	                	rule.setId("Expiration Rule");
	                	rule.setStatus("Enabled");
	                	rule.setExpiredObjectDeleteMarker(true);
	                	rules.add(rule);
					}
	            }
	            
	            BucketLifecycleConfiguration config = new BucketLifecycleConfiguration(rules);
	            client.setBucketLifecycleConfiguration(bucketName, config);
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			response.setStatus(400);
			return Constants.FAIL;
		}
		
		return Constants.SUCCESS;
    }
    
    /**
     * 获取bucket acl
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object getBucketAcl(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response){
    	AccessControlList acl = null;
		try {
			 acl = client.getBucketAcl(bucketName);
		} catch (Exception e) {
			response.setStatus(403);
			return Constants.FAIL;
		}
		XStream xStream = new XStream();
		xStream.alias("AccessControlList", AccessControlList.class);
		xStream.alias("Grant", Grant.class);
		xStream.alias("CanonicalGrantee", CanonicalGrantee.class);
		xStream.alias("owner", Owner.class);
		xStream.alias("GroupGrantee", GroupGrantee.class);
		String xml = xStream.toXML(acl);
		return xml;
    }
    
    /**
     * 获取bucket lifecycle
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object getBucketLifeCycle(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response){
    	BucketLifecycleConfiguration config = null;
		try {
			config = client.getBucketLifecycleConfiguration(bucketName);
		} catch (Exception e) {
			response.setStatus(404);
			return Constants.FAIL;
		}
		XStream xStream = new XStream();
		String xml = xStream.toXML(config);
		xStream.alias("LifecycleConfiguration", BucketLifecycleConfiguration.class);
		return xml;
    }
    
    /**
     * 获取bucket object list
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object getBucketObjectList(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response){
    	String prefix = request.getParameter("prefix");
    	String  maxKeysStr = request.getParameter("maxKeys");
    	String encodingType = request.getParameter("encodingType");
    	String delimiter = request.getParameter("delimiter");
    	String marker = request.getParameter("marker");
    	int maxKeys = 1000;
    	
    	if (prefix == null) {
			prefix = "";
		}
		if (maxKeysStr != null) {
			maxKeys = Integer.parseInt(maxKeysStr);
		}
		if (encodingType == null) {
			encodingType = "url";
		}
		
		ObjectListing list = null ;
		ListObjectsRequest objectsRequest = new ListObjectsRequest()
				.withBucketName(bucketName)
				.withPrefix(prefix)
				.withMaxKeys(maxKeys)
				.withMarker(marker)
				.withDelimiter(delimiter)
				.withEncodingType(encodingType);
		try {
			list = client.listObjects(objectsRequest);
		} catch (Exception e) {
			response.setStatus(400);
			return Constants.FAIL;
		}
		XStream xStream = new XStream();
		xStream.alias("ListBucketResult", ObjectListing.class);
		xStream.alias("Contents", S3ObjectSummary.class);
//		xStream.alias("owner", Owner.class);
		String xml = xStream.toXML(list);
		return xml;
    }
    
    /**
     * 获取bucket info
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object getBucketInfo(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response){
    	String username = request.getHeader("User-Name");
    	String password = request.getHeader("Password");
    	
		String result = null;
		BucketInfo bucket = new BucketInfo();
		try {
			Map<String, String> user = JsonUtil.getKeys(username, password);
			AWSCredentials credentials = new BasicAWSCredentials(
					user.get("accessKey"), user.get("secretKey"));
			AdminClient adminClient = new AdminClient(credentials);
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("bucket", bucketName);
			String resp = adminClient.doRequest("admin/bucket", null, HttpMethodName.GET, param);
			JSONObject bucketJson = new JSONObject(resp);
			bucket.setCreationDate(bucketJson.getString("mtime"));
			bucket.setOwner(bucketJson.getString("owner"));
			bucket.setPermission(this.getGlobalAcl(adminClient, bucketName));
			if (resp == null) {
				response.setStatus(404);
			}
		} catch (Exception e) {
			response.setStatus(500);
			return Constants.FAIL;
		}
		
		XStream xStream = new XStream();
		xStream.alias("Bucket", BucketInfo.class);
		String xml = xStream.toXML(bucket);
		return xml;
    }
    
    /**
     * 删除bucket
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object deleteBucket(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response){
    	try {
			client.deleteBucket(bucketName);
		} catch (Exception e) {
			response.setStatus(400);
			return Constants.FAIL;
		}
		return Constants.SUCCESS;
    }
    
    /**
     * 强制bucket
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object forceDeleteBucket(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response){
    	try {
			ObjectListing objectListing = client.listObjects(bucketName);
			List<S3ObjectSummary> objectList = objectListing.getObjectSummaries();
			for (S3ObjectSummary s3ObjectSummary : objectList) {
				client.deleteObject(bucketName, s3ObjectSummary.getKey());
			}
			client.deleteBucket(bucketName);
		} catch (Exception e) {
			response.setStatus(500);
			return Constants.FAIL;
		}
		return Constants.SUCCESS;
    }
    
    /**
     * 获取bucket配额信息
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object getBucketQuota(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response) {
    	String result = null;
    	String username = request.getHeader("User-Name");
		try {
			CephParser cephParser = new CephParser();
			result = cephParser.getBucketQuota(username, bucketName);
			if (result == null) {
				throw new Exception();
			}
		} catch (Exception e) {
			response.setStatus(400);
			return Constants.FAIL;
		}
		
		return result;
    }
    
    /**
     * 设置用户配额
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object setBucketQuota(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response){
		String contentType = request.getContentType();
		String username = request.getHeader("User-Name");
		try {
			if (contentType != null &&contentType.toLowerCase().contains("json")) {
				InputStream inputStream = request.getInputStream();
				String jsonObjects = getStrFromInputSteam(inputStream);
				CephParser cephParser = new CephParser();
				JSONObject bucketQuota = new JSONObject(jsonObjects);
				boolean enabled = bucketQuota.getBoolean("enabled");
				long maxObjects = bucketQuota.getLong("max_objects");
				long maxSize = bucketQuota.getLong("max_size_kb") * 1024; // 要设置的值的单位为B
				int retInt = cephParser.setBucketQuota(username, bucketName, enabled, maxObjects, maxSize);
				if (retInt <= 0) {
					throw new Exception();
				}
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			response.setStatus(400);
			return Constants.FAIL;
		}
		
		return Constants.SUCCESS;
    }
    
    /**
     * 从inputstream中获取字符串
     * @param in
     * @return
     * @throws IOException
     */
    public String getStrFromInputSteam(InputStream in) throws IOException{  
        BufferedReader bf=new BufferedReader(new InputStreamReader(in,"UTF-8"));  
        //最好在将字节流转换为字符流的时候 进行转码  
        StringBuffer buffer=new StringBuffer();  
        String line="";  
        while((line=bf.readLine())!=null){  
            buffer.append(line);  
        }  
          
       return buffer.toString();  
   }  
    
    /**
     * 获取bucket的全局acl
     * @param client
     * @param bucketName
     * @return
     */
    private String getGlobalAcl(AmazonS3 client, String bucketName){
	    String result = "private";
    	AccessControlList acl = client.getBucketAcl(bucketName);
		List<Grant> grants = acl.getGrantsAsList();
		for (Grant grant : grants) {
			String className = grant.getGrantee().getClass().getName();
			String name = grant.getGrantee().getIdentifier();
			if (className.contains("GroupGrantee") && name.contains("AllUsers")) {
				result = grant.getPermission().name();
			}
		}
		
		if ("Read".equals(result)) {
			result = "public-read";
		} else if ("Write".equals(result)) {
			result = "public-read-write";
		}
	   return result;
   }
}
