package com.trendytech.tds.s3.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import com.amazonaws.services.s3.transfer.Upload;
import com.thoughtworks.xstream.XStream;
import com.trendytech.tds.util.PropertiesUtil;
import com.trendytech.tds.util.Constants;

public class ObjectUtil {
	/**
	 * 上传文件
	 * @param client
	 * @param bucketName
	 * @param key
	 * @param request
	 * @param response
	 * @return
	 */
    public Object putObject(AmazonS3 client, String bucketName, String key, HttpServletRequest request, HttpServletResponse response) {
    	try {
    		String contentLength = request.getHeader("Content-length");
    		if (contentLength != null) {
    			long size = Long.parseLong(contentLength);
    			ServletInputStream inputStream = request.getInputStream();
    			String result = this.uploadFromInputStream(client, bucketName, key, size, inputStream);
    			if (!"complete".equals(result)) {
					throw new Exception();
				}
			} else {
				response.setStatus(400);
				return Constants.FAIL;
			}
		} catch (Exception e) {
			response.setStatus(500);
			return Constants.FAIL;
		}

		return Constants.SUCCESS;
    }
    
    /**
	 * 上传文件
	 * @param client
	 * @param bucketName
	 * @param key
	 * @param request
	 * @param response
	 * @return
	 */
    public Object putMultiObject(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response) {
    	try {
			ServletInputStream inputStream = request.getInputStream();
			boolean isEnd = false;
			while (true) {
				// 获取文件名长度字节(4字节)
				byte[] nameLenBytes = new byte[4];
				inputStream.read(nameLenBytes);
				// 中间有未读到数据的，则数据流结束
			    for (byte b : nameLenBytes) {
					if (b == 0) {
						isEnd = true;
						break;
					}
				}
			    // 如果数据流结束，则跳出循环
			    if (isEnd) {
					break;
				}
			    
			    int nameLen = Integer.parseInt(new String(nameLenBytes, "utf-8"));
			    // 根据获取到的文件名长度取到文件名
			    byte[] nameBytes = new byte[nameLen];
			    inputStream.read(nameBytes);
			    String name = new String(nameBytes, "utf-8");
			    // 获取文件长度(16字节)
			    byte[] fileLenBytes = new byte[16];
			    inputStream.read(fileLenBytes);
			    long fileLen = Long.parseLong(new String(fileLenBytes, "utf-8"));
			    // 获取文件内容并写入到临时文件
			    File tmpFile = File.createTempFile("upload", null);
			    BufferedWriter out = new BufferedWriter(new FileWriter(tmpFile));
			    int index = 0;
			    int data = 0;
			    while(index < fileLen) {
			    	data = inputStream.read();
			    	if (data == -1) {
						break;
					}
			    	out.write(data);
			    	index ++;
			    }
			    out.close();
			    // 从临时文件上传文件
				this.multipartUpload(client, bucketName, tmpFile, name);
			}
			
		} catch (Exception e) {
			response.setStatus(400);
			return Constants.FAIL;
		}
    	
    	return Constants.SUCCESS;
    }
    
    /**
	 * 下载文件
	 * @param client
	 * @param bucketName
	 * @param key
	 * @param request
	 * @param response
	 * @return
	 */
    public Object downloadObject(AmazonS3 client, String bucketName, String key, HttpServletRequest request, HttpServletResponse response) {
    	try {
    		String fileName = key.substring(key.lastIndexOf("/")+1);
    		S3ObjectSummary object = this.getObjectSummary(client, bucketName, key);
    		long size = object.getSize();
    		
    		fileName = this.getBrowserCompatibleFileName(request, fileName);
    		
    		// 清空response
    		response.reset();
    		// 设置response的Header
    		response.addHeader("Content-Disposition", "attachment;filename=\"" 
    				+ fileName + "\"");
    		response.addHeader("Content-Length", "" + size);
    		response.setContentType("application/octet-stream;charset=UTF-8");
      	
    		S3Object s3Object = client.getObject(bucketName, key);
    		S3ObjectInputStream inputStream = s3Object.getObjectContent();
    		
    		OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
    		
    		int bufferSize = 102400; // 每次传输的字节数
    		int readSize = 0;
    		byte[] buffer = new byte[bufferSize];    // 缓存区
    		
    		// 分块传输
    		while((readSize = inputStream.read(buffer)) != -1) {
    			toClient.write(buffer, 0 , readSize);
    	        toClient.flush();
    		}
    		
    		//将最后剩余的未传输字节传输到客户端
    		toClient.write(buffer);
    		toClient.flush();
    		    
    		toClient.close();
    		s3Object.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
    	
		return Constants.SUCCESS;
    }
    
    /**
     * 删除文件
     * @param client
     * @param bucketName
     * @param key
     * @param request
     * @param response
     * @return
     */
    public Object deleteObject(AmazonS3 client, String bucketName, String key, HttpServletRequest request, HttpServletResponse response) {
    	try {
			client.deleteObject(bucketName, key);
		} catch (Exception e) {
			response.setStatus(500);
			return Constants.FAIL;
		}
		return Constants.SUCCESS;
    }
    
    /**
     * 删除多文件
     * @param client
     * @param bucketName
     * @param key
     * @param request
     * @param response
     * @return
     */
    public Object deleteMutiObject(AmazonS3 client, String bucketName, HttpServletRequest request, HttpServletResponse response) {
    	boolean quiet = true;
		List<KeyVersion> delObjects = new ArrayList<KeyVersion>();
		try {
			InputStream inputStream = request.getInputStream();
			String xmlStr = getStrFromInputSteam(inputStream);
			Document doc = (Document) DocumentHelper.parseText(xmlStr);
			Element delete = doc.getRootElement();
			if (!"Delete".equals(delete.getName())) {
				throw new Exception();
			}
			@SuppressWarnings("rawtypes")
			Iterator Elements = delete.elementIterator();
			while (Elements.hasNext()) {
				Element object = (Element) Elements.next();
				if ("quiet".equals(object.getName().toLowerCase())) {
					if ("false".equals(object.getText().toLowerCase())) {
						quiet = false;
					}
				} else if ("object".equals(object.getName().toLowerCase())) {
					Element objectKey = object.element("Key");
					if (objectKey != null) {
						KeyVersion keyVersion = new KeyVersion(objectKey.getText());
						delObjects.add(keyVersion);
					}
				}
			}
			DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
			deleteObjectsRequest.setQuiet(quiet);
			deleteObjectsRequest.setKeys(delObjects);
			client.deleteObjects(deleteObjectsRequest);
			return Constants.SUCCESS;
		} catch (Exception e) {
			response.setStatus(400);
			return Constants.FAIL;
		}
    }
    
    /**
     * 获取文件head信息
     * @param client
     * @param bucketName
     * @param key
     * @param request
     * @param response
     * @return
     */
    public Object getObjectHead(AmazonS3 client, String bucketName, String key, HttpServletRequest request, HttpServletResponse response) {
    	try {
			GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
			S3Object result = client.getObject(getObjectRequest);
			response.setHeader("Etag", result.getObjectMetadata().getETag());
			response.setDateHeader("Last-Modified", result.getObjectMetadata().getLastModified().getTime());
			response.setHeader("Connection", result.getObjectMetadata().getContentDisposition());
			response.setHeader("x-aws-object-type", result.getObjectMetadata().getContentType());
			response.setHeader("Size", String.valueOf(result.getObjectMetadata().getContentLength()));
		} catch (Exception e) {
			response.setStatus(500);
			return Constants.FAIL;
		}

		return Constants.SUCCESS;
    }
    
    /**
     * 设置object acl
     * @param client
     * @param bucketName
     * @param key
     * @param request
     * @param response
     * @return
     */
    public String setObjectAcl(AmazonS3 client, String bucketName, String key, HttpServletRequest request, HttpServletResponse response){
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
        		if ("public-read-write".equals(aclStr)) {
					client.setObjectAcl(bucketName, key, CannedAccessControlList.PublicReadWrite);
				} else if ("public-read".equals(aclStr)) {
					client.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);
				} else if ("private".equals(aclStr)) {
					client.setObjectAcl(bucketName, key, CannedAccessControlList.Private);
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
				client.setObjectAcl(bucketName, key, acl);
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
     * 设置object acl
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object getObjectAcl(AmazonS3 client, String bucketName, String key, HttpServletRequest request, HttpServletResponse response){
    	AccessControlList acl = null;
		try {
			acl = client.getObjectAcl(bucketName, key);
			response.setContentType("json");
		} catch (Exception e) {
			response.setStatus(500);
			return Constants.FAIL;
		}
		/**
		 * AccessControlList [owner=S3Owner [name=zhon,id=zhon],
		 *  grants=[Grant [grantee=com.amazonaws.services.s3.model.CanonicalGrantee@39098d, permission=FULL_CONTROL]]]
		 */
		XStream xStream = new XStream();
		xStream.alias("AccessControlList", AccessControlList.class);
		xStream.alias("Grant", Grant.class);
		xStream.alias("CanonicalGrantee", CanonicalGrantee.class);
		xStream.alias("owner", Owner.class);
		String xml = xStream.toXML(acl);
		return xml;
    }
    
    /**
     * 初始化分块上传
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object initMutiPartUpload(AmazonS3 client, String bucketName, String key, HttpServletRequest request, HttpServletResponse response){

		InitiateMultipartUploadResult result = null;
		try {
			// 初始化分片上传
			InitiateMultipartUploadRequest imuRequest = new InitiateMultipartUploadRequest(bucketName, key);
			result = client.initiateMultipartUpload(imuRequest);
		} catch (Exception e) {
			response.setStatus(500);
			return Constants.FAIL;
		}
		
		//将结果转换为xml
		XStream xStream = new XStream();
		String xml = xStream.toXML(result);
		xStream.alias("InitiateMultipartUploadResult", InitiateMultipartUploadResult.class);
		return xml;
    }
    
    /**
     * 初始化分块上传
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object mutiPartUpload(AmazonS3 client, String bucketName, String key, HttpServletRequest request, HttpServletResponse response){
    	UploadPartResult result = null;
		try {
			ServletInputStream inputStream = request.getInputStream();
			Integer partNumber = Integer.parseInt(request.getParameter("position"));
	        Long length = Long.parseLong(request.getHeader("Content-Length"));
	        String uploadId = request.getParameter("uploadId");
			// 上传分片
			UploadPartRequest uploadPartRequest = new UploadPartRequest();
			uploadPartRequest.setInputStream(inputStream);
			uploadPartRequest.setKey(key);
			uploadPartRequest.setBucketName(bucketName);
			uploadPartRequest.setObjectMetadata(new ObjectMetadata());
			uploadPartRequest.setFileOffset(0);
			uploadPartRequest.setPartSize(length);
			uploadPartRequest.setPartNumber(partNumber);
			uploadPartRequest.setUploadId(uploadId);

			result = client.uploadPart(uploadPartRequest);
		} catch (Exception e) {
			response.setStatus(500);
			return Constants.FAIL;
		}

		//将结果转换为xml
		XStream xStream = new XStream();
		String xml = xStream.toXML(result);
		xStream.alias("UploadPartResult", UploadPartResult.class);
		xStream.alias("partETag", PartETag.class);
		return xml;
    }
    
    /**
     * 完成分块上传
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object finishMutiPartUpload(AmazonS3 client, String bucketName, String key, HttpServletRequest request, HttpServletResponse response){
    	try {
    		String uploadId = request.getParameter("uploadId");
        	//查找分片的数据列表
    		ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
    		// 设置找到的parts数量，不然大文件parts会不全，最大支持文件大小为10000 * 5 M
    		listPartsRequest.withMaxParts(10000);
    		PartListing listResult = client.listParts(listPartsRequest);
    		List<PartSummary> parts = listResult.getParts();
    		List<PartETag> partETags = new ArrayList<PartETag>();
    		for (PartSummary partSummary : parts) {
    			PartETag partETag = new PartETag(partSummary.getPartNumber(), partSummary.getETag());
    			partETags.add(partETag);
    		}
    		
    		// 合并分片文件，完成分片上传
    		CompleteMultipartUploadRequest cmuRequest = new CompleteMultipartUploadRequest();
    		cmuRequest.setBucketName(bucketName);
    		cmuRequest.setKey(key);
    		cmuRequest.setUploadId(uploadId);
    		cmuRequest.setPartETags(partETags);
    		client.completeMultipartUpload(cmuRequest);
		} catch (Exception e) {
			response.setStatus(400);
			return Constants.FAIL;
		}
    	
		return Constants.SUCCESS;
    }
    
    /**
     * 初始化分块上传
     * @param client
     * @param bucketName
     * @param request
     * @param response
     * @return
     */
    public Object abortMutiPartUpload(AmazonS3 client, String bucketName, String key, HttpServletRequest request, HttpServletResponse response){
    	try {
    		String uploadId = request.getParameter("uploadId");
    		client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, key, uploadId));
		} catch (Exception e) {
			response.setStatus(400);
			return Constants.FAIL;
		}
    	
		return Constants.SUCCESS;
    }
    
    /**
	 * 从用户数据中解析出key
	 * @param bucketName
	 * @param request
	 * @return
	 */
	public String getKey(String bucketName, HttpServletRequest request) {
		String prefix = null;
		String requestURI = request.getRequestURI();
		String contextPath = request.getContextPath();
		try {
			requestURI = URLDecoder.decode(requestURI, "utf-8");
		} catch (UnsupportedEncodingException e) {
			requestURI = URLDecoder.decode(requestURI);
		}
		if ("/".equals(contextPath)) {
			prefix = "/" + bucketName + "/";
		} else {
			prefix = request.getContextPath() + "/" + bucketName + "/";
		}
		String key = requestURI.replaceFirst(prefix, "");
		return key;
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
     * 获取文件的相关信息
     * @param client
     * @param bucketName
     * @param key
     * @return
     */
   private S3ObjectSummary getObjectSummary(AmazonS3 client, String bucketName,String key){
		S3ObjectSummary summary = null;
		try{
			GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
			S3Object result = client.getObject(getObjectRequest);
			if (result != null) {
				summary = new S3ObjectSummary();
				summary.setBucketName(result.getBucketName());
				summary.setKey(result.getKey());
				summary.setETag(result.getObjectMetadata().getETag());
				summary.setLastModified(result.getObjectMetadata().getLastModified());
				summary.setSize(result.getObjectMetadata().getContentLength());
				summary.setStorageClass(result.getObjectMetadata().getStorageClass());
				summary.setOwner(client.getS3AccountOwner());
			}
			result.close();
		}catch (Exception e) {
			return null;
		}
		return summary;
	}
   
   /**
    * 获取兼容浏览器的文件名称
    * @param request
    * @param fileName
    * @return
    * @throws UnsupportedEncodingException
    */
    private String getBrowserCompatibleFileName(HttpServletRequest request, String fileName)
			throws UnsupportedEncodingException {
		String agent = request.getHeader("User-Agent");
		// 是否ie浏览器
		boolean isMSIE = (agent != null && (agent.indexOf("MSIE") != -1
				|| agent.indexOf("rv:11") != -1));

		if (isMSIE) {
		    fileName = URLEncoder.encode(fileName, "UTF-8");
		    if (fileName.contains(".")) {
		    	fileName=fileName.replaceAll("\\+",  "%20"); //处理空格
		    } else {
		    	fileName=fileName.replaceAll("\\+",  " "); //处理空格
		    }
		} else {
		    fileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
		}
		return fileName;
	}
    
    /**
     * 通过流的方式上传文件
     * @param client
     * @param bucketName
     * @param objectKey
     * @param size
     * @param inputStream
     * @return
     */
    private String uploadFromInputStream(AmazonS3 client, String bucketName, String objectKey, Long size, InputStream inputStream){
		String uploadId = null;
		try {
			// 分片上传块大小
			int bufferSize = 1024 * 1024 * 5;  //5M
			byte[] buffer = new byte[bufferSize];
			int index = 0;
			byte[] unitBuffer = new byte[1024];
			int partNumber = 1;
			int readSize = 0;
			
			// 小于10M的文件直接上传
			if (size < new Long(bufferSize * 2)) {
				client.putObject(bucketName, objectKey, inputStream, new ObjectMetadata());
				return "complete";
			}
			
			// 初始化分片上传
			InitiateMultipartUploadRequest imuRequest = new InitiateMultipartUploadRequest(bucketName, objectKey);
			InitiateMultipartUploadResult initResult = client.initiateMultipartUpload(imuRequest);
			uploadId = initResult.getUploadId();
			// 分块传输
			while((readSize = inputStream.read(unitBuffer)) != -1) {
				for(int i = 0; i < readSize; i++){
					buffer[index] = unitBuffer[i];
					index++;
					if (index == bufferSize) {
						// 上传分片
						UploadPartRequest uploadPartRequest = new UploadPartRequest();
						uploadPartRequest.setInputStream(new ByteArrayInputStream(buffer));
						uploadPartRequest.setKey(objectKey);
						uploadPartRequest.setBucketName(bucketName);
						uploadPartRequest.setObjectMetadata(new ObjectMetadata());
						uploadPartRequest.setFileOffset(0);
						uploadPartRequest.setPartSize(bufferSize);
						uploadPartRequest.setPartNumber(partNumber);
						uploadPartRequest.setUploadId(uploadId);
						client.uploadPart(uploadPartRequest);
						
						partNumber ++;
						index = 0;
					}
				}
			}
			
			inputStream.close();
			
			byte[] availBytes = Arrays.copyOfRange(buffer, 0, index);
			
			// 上传分片
			UploadPartRequest uploadPartRequest = new UploadPartRequest();
			uploadPartRequest.setInputStream(new ByteArrayInputStream(availBytes));
			uploadPartRequest.setKey(objectKey);
			uploadPartRequest.setBucketName(bucketName);
			uploadPartRequest.setObjectMetadata(new ObjectMetadata());
			uploadPartRequest.setFileOffset(0);
			uploadPartRequest.setPartSize(index);
			uploadPartRequest.setPartNumber(partNumber);
			uploadPartRequest.setUploadId(uploadId);
			client.uploadPart(uploadPartRequest);
			
			//查找分片的数据列表
			ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, objectKey, uploadId);
			// 设置找到的parts数量，不然大文件parts会不全，最大支持文件大小为10000 * 5 M
			listPartsRequest.withMaxParts(10000);
			PartListing listResult = client.listParts(listPartsRequest);
			List<PartSummary> parts = listResult.getParts();
			List<PartETag> partETags = new ArrayList<PartETag>();
			for (PartSummary partSummary : parts) {
				PartETag partETag = new PartETag(partSummary.getPartNumber(), partSummary.getETag());
				partETags.add(partETag);
			}
			
			// 合并分片文件，完成分片上传
			CompleteMultipartUploadRequest cmuRequest = new CompleteMultipartUploadRequest();
			cmuRequest.setBucketName(bucketName);
			cmuRequest.setKey(objectKey);
			cmuRequest.setUploadId(uploadId);
			cmuRequest.setPartETags(partETags);
			client.completeMultipartUpload(cmuRequest);
			return "complete";
		} catch (Exception e) {
			if (uploadId != null) {
				client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, objectKey, uploadId));
			}
		}
		
		return "fail";
	}
    
    public String multipartUpload(AmazonS3 client, String bucketName,File file,String objectName){
		String result = "";
		try {
			PutObjectRequest request =  new PutObjectRequest(bucketName, objectName, file);
			String uploadThresholed = PropertiesUtil.getString("netdisk.upload_threshold");
			TransferManagerConfiguration tmc = new TransferManagerConfiguration();
			tmc.setMultipartUploadThreshold(Long.parseLong(uploadThresholed));
			TransferManager tx = new TransferManager(client);
			tx.setConfiguration(tmc);
//			TransferManager.appendMultipartUserAgent(request);
			Upload myUpload = tx.upload(request);
			myUpload.waitForUploadResult();
			result = "complete";
		} catch (Exception e) {
			result = "fail";
		} 
		return result;
	}
}
