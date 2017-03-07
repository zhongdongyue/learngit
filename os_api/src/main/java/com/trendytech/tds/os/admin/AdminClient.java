/**
 * 
 */
package com.trendytech.tds.os.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.Signer;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.Headers;
import com.trendytech.tds.os.admin.model.Bucket;
import com.trendytech.tds.os.admin.model.User;
import com.trendytech.tds.util.PropertiesUtil;

/**
 * @author Robin
 *
 */
public class AdminClient extends AmazonS3Client implements AdminOperations {

	private Log log = LogFactory.getLog(getClass());
	
	private ObjectMapper mapper = new ObjectMapper();
	public static final String CHARSET_UTF8 = "UTF-8";
	public static final String API_URL = PropertiesUtil.getString("os.endpoint");
//	public static final String API_URL = "http://192.168.2.24";
	public static final String BUCKET_ADMIN_USER = "admin/user";
	public static final String BUCKET_ADMIN_METADATA = "admin/metadata";
	public static final String BUCKET_ADMIN_USEAGE = "admin/usage";
	public static final String BUCKET_ADMIN_BUCKET = "admin/bucket";
	
	public static final String KEY_USER = "user";
	public static final String KEY_BUCKET = "bucket";
	
	private AWSCredentials awsCredentials = null;
	
	public AdminClient(AWSCredentials awsCredentials) {
		super(awsCredentials);
		setEndpoint(API_URL);
		this.awsCredentials = awsCredentials;
	}
	
	@Override
	public User createUser(User user) throws Exception {
		//try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("uid", user.getId());
			param.put("display-name", user.getDisplayName());
			param.put("email", user.getEmail());
			//param.put("user-caps", "usage=read");
			param.put("max-buckets", user.getMaxBuckets());
			String resp = doRequest(BUCKET_ADMIN_USER, null, HttpMethodName.PUT, param);
			if (resp == null) {
				// 如果创建失败，则尝试不设置email进行创建，防止因为email冲突而导致创建用户失败
				param.remove("email");
				resp = doRequest(BUCKET_ADMIN_USER, null, HttpMethodName.PUT, param);
			}
			user = mapper.readValue(resp, User.class);
		/*} catch (Exception e) {
			log.error("AdminClient createUser", e);
		}*/
		return user;
	}

	@Override
	public User modifyUser(User user) {
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("uid", user.getId());
			if(StringUtils.isNotEmpty(user.getEmail())){
				param.put("email", user.getEmail());
			}
			if(StringUtils.isNotEmpty(user.getDisplayName())){
				param.put("display-name", user.getDisplayName());
			}
		
			if(StringUtils.isNotEmpty(String.valueOf(user.getMaxBuckets()))&&user.getMaxBuckets()!=0){
				param.put("max-buckets", user.getMaxBuckets());
			}
			
			if(StringUtils.isNotEmpty(String.valueOf(user.isSuspended()))){
				param.put("suspended", user.isSuspended());
			}
			//param.put("user-caps", "usage=read");
		
			String resp = doRequest(BUCKET_ADMIN_USER, null, HttpMethodName.POST, param);
			user = mapper.readValue(resp, User.class);
		} catch (Exception e) {
			log.error("AdminClient modifyUser", e);
		}
		return user;
	}
	@Override
	public void removeUser(String uid) {
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("uid", uid);
			String resp = doRequest(BUCKET_ADMIN_USER, null, HttpMethodName.DELETE, param);
		} catch (Exception e) {
			log.error("AdminClient removeUser", e);
		}
	}

	@Override
	public void linkBucket(String bucketName, String uid,String bucketId) {
	
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("bucket", bucketName);
			param.put("uid", uid);
			param.put("bucket-id", bucketId);
			String resp = doRequest(BUCKET_ADMIN_BUCKET, null, HttpMethodName.PUT, param);
			//bucket = mapper.readValue(resp, Bucket.class);
		} catch (Exception e) {
			log.error("AdminClient linkBucket", e);
		}
	}

	@Override
	public void unlinkBucket(String bucketName, String uid) {
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("bucket", bucketName);
			param.put("uid", uid);
			String resp = doRequest(BUCKET_ADMIN_BUCKET, null, HttpMethodName.POST, param);
		} catch (Exception e) {
			log.error("AdminClient unlinkBucket", e);
		}
	}
	@Override
	public void addUserCapability(String uid, String caps) {
		//User user = null;
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("uid", uid);
			param.put("user-caps", caps);
			String resp = doRequest(BUCKET_ADMIN_USER, null, HttpMethodName.PUT, param);
		    List<Map> cap = mapper.readValue(resp, ArrayList.class);
		} catch (JsonParseException e) {
			log.error("AdminClient addUserCapability", e);
		} catch (JsonMappingException e) {
			log.error("AdminClient addUserCapability", e);
		} catch (IOException e) {
			log.error("AdminClient addUserCapability", e);
		}
		//return user;
	}

	@Override
	public void removeUserCapability(String uid, String caps) {
		User user = null;
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("uid", uid);
			param.put("user-caps", caps);
			String resp = doRequest(BUCKET_ADMIN_USER, null, HttpMethodName.DELETE, param);
			List<Map> capMap = mapper.readValue(resp, ArrayList.class);
		} catch (JsonParseException e) {
			log.error("AdminClient removeUserCapability", e);
		} catch (JsonMappingException e) {
			log.error("AdminClient removeUserCapability", e);
		} catch (IOException e) {
			log.error("AdminClient removeUserCapability", e);
		}
		
	}

	@Override
	public User getUserQuota(String uid, String quotatype) {
		User user = null;
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("uid", uid);
			param.put("quota-type", quotatype);
			String resp = doRequest(BUCKET_ADMIN_USER, "quota", HttpMethodName.GET, param);
			user = mapper.readValue(resp, User.class);
		} catch (JsonParseException e) {
			log.error("AdminClient getUserQuota", e);
		} catch (JsonMappingException e) {
			log.error("AdminClient getUserQuota", e);
		} catch (IOException e) {
			log.error("AdminClient getUserQuota", e);
		}
		return user;
	}
	@Override
	public void setQuota(String uid, String quotatype, long maximumObjects,
			long maximumSize) {
		
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("uid", uid);
			param.put("quota-type", quotatype);
			param.put("max-objects", maximumObjects);
			param.put("max-size-kb", maximumSize);
			String resp = doRequest(BUCKET_ADMIN_USER, "quota", HttpMethodName.PUT, param);
			
		} catch (Exception e) {
			log.error("AdminClient setQuota", e);
		}
		
	}

	@Override
	public User getQuota(String uid, String quotatype) {
		User user = null;
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("uid", uid);
			param.put("quota-type", quotatype);
			String resp = doRequest(BUCKET_ADMIN_USER, "quota", HttpMethodName.GET, param);
			user = mapper.readValue(resp, User.class);
		} catch (JsonParseException e) {
			log.error("AdminClient getQuota", e);
		} catch (JsonMappingException e) {
			log.error("AdminClient getQuota", e);
		} catch (IOException e) {
			log.error("AdminClient getQuota", e);
		}
		return user;
	}
	
    /**
     * 用于存储api接口，获取配额
     * @param uid
     * @param param
     * @return
     * @throws IOException 
     * @throws ClientProtocolException 
     */
	public String getQuota(String uid, Map<String, Object> param) throws ClientProtocolException, IOException{
		param.put("quota", "");
		return doRequest("admin", "user", HttpMethodName.GET, param);
	}
	
	/**
     * 用于存储api接口，设置配额
     * @param uid
     * @param param
     * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
     */
	public void setQuota(String uid, String quotatype, boolean enabled, long maxObjects,
			long maxSizeKb) throws ClientProtocolException, IOException {
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("uid", uid);
		param.put("quota-type", quotatype);
		param.put("enabled", enabled);
		param.put("max-objects", maxObjects);
		param.put("max-size-kb", maxSizeKb);
		doRequest(BUCKET_ADMIN_USER, "quota", HttpMethodName.PUT, param);
	}

	@Override
	public User setBucketQuota(String uid, String quotatype,
			long maximumObjects, long maximumSize) throws ClientProtocolException, IOException {
		User user = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("uid", uid);
		param.put("quota-type", quotatype);
		param.put("max-objects", maximumObjects);
		param.put("max-size", maximumSize);
		String resp = doRequest(BUCKET_ADMIN_USER, "quota", HttpMethodName.PUT, param);
		user = mapper.readValue(resp, User.class);
		return user;
	}
	/*@Override
	public User setQuota(String uid, String quotatype, long maximumObjects,
			long maximumSize) {
		
			User user = null;
			try {
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("uid", uid);
				param.put("quota-type", quotatype);
				param.put("max-objects", maximumObjects);
				param.put("max-size-kb", maximumSize);
				String resp = doRequest(BUCKET_ADMIN_USER, "quota", HttpMethodName.PUT, param);
				user = mapper.readValue(resp, User.class);
			} catch (JsonParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return user;
		
		
	}
*/
	
	@SuppressWarnings("unchecked")
	@Override
	public List<User> listUsers() {
		List<User> rtnUsers = new ArrayList<User>();
		try {
			String resp = doRequest(BUCKET_ADMIN_METADATA, KEY_USER, HttpMethodName.GET, null);
			List<String> userIds = mapper.readValue(resp, ArrayList.class);
			
			for (String userId : userIds) {
				if(userId.startsWith("_")){
					userId = userId.substring(1);
				}
				User user = getUser(userId);
//				User user = new User();
//				user.setId(userId);
				rtnUsers.add(user);
			}
		} catch (Exception e) {
			log.error("AdminClient listUsers", e);
		}
		return rtnUsers;
	}

	@Override
	public User getUser(String userId) throws Exception {
		User user = null;
		//try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("uid", userId);
			String resp = doRequest(BUCKET_ADMIN_USER, null, HttpMethodName.GET, param);
			if(resp == null){
				return user;
			}
			//System.out.println("===="+resp);
			user = mapper.readValue(resp, User.class);
		/*} catch (JsonParseException e) {
			log.error("AdminClient getUser", e);
		} catch (JsonMappingException e) {
			log.error("AdminClient getUser", e);
		} catch (IOException e) {
			log.error("AdminClient getUser", e);
		}*/
		return user;
	}
	
	@Override
	public Bucket getBucket(String bucketName) {
		Bucket bucket = null;
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("bucket", bucketName);
			String resp = doRequest(BUCKET_ADMIN_BUCKET, null, HttpMethodName.GET, param);
			bucket = mapper.readValue(resp, Bucket.class);
		} catch (JsonParseException e) {
			log.error("AdminClient getBucket", e);
		} catch (JsonMappingException e) {
			log.error("AdminClient getBucket", e);
		} catch (IOException e) {
			log.error("AdminClient getBucket", e);
		}
		return bucket;
	}
	@Override
	public void removeBucket(String bucketName) {
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("bucket", bucketName);
			param.put("purge-objects", true);
			String resp = doRequest(BUCKET_ADMIN_BUCKET, null, HttpMethodName.DELETE, param);
		} catch (Exception e) {
			log.error("AdminClient removeBucket", e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Bucket> listAvailableBuckets() {
		List<Bucket> buckets = new ArrayList<Bucket>();
		try {
			List<String> bucketNames = null;
			Map<String, Object> param = new HashMap<String, Object>();
			String resp = doRequest(BUCKET_ADMIN_BUCKET, null, HttpMethodName.GET, param);
			bucketNames = (List<String>) mapper.readValue(resp, List.class);
			
			
			
			String resp2 = doRequest(BUCKET_ADMIN_METADATA, KEY_USER, HttpMethodName.GET, null);
			List<String> userIds = mapper.readValue(resp2, ArrayList.class);
			
			//List<String> bucketNamesByUserId = null;
			for (String uid : userIds) {
				//List<Bucket> bucketsByUserId = listBuckets(uid);
				//List<String> bucketNamesByUserId = null;
				Map<String, Object> param2 = new HashMap<String, Object>();
				param2.put("uid", uid);
				String resp3 = doRequest(BUCKET_ADMIN_BUCKET, null, HttpMethodName.GET, param2);
				List<String> bucketNamesByUserId = (List<String>) mapper.readValue(resp3, List.class);
				
				
				Iterator<String> bucketListIterator = bucketNames.iterator();
				while(bucketListIterator.hasNext()){
					String bucket = bucketListIterator.next();
					for (String cachePool : bucketNamesByUserId) {
						
						if(bucket.equals(cachePool)){
							bucketListIterator.remove();
						}
					}
				
				}
				
				
			}
			
			
			for (String bucketName : bucketNames) {
				Bucket bucket = getBucket(bucketName);
				buckets.add(bucket);
			}
		} catch (Exception e) {
			log.error("AdminClient listAvailableBuckets", e);
		}
		return buckets;
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public List<Bucket> listBuckets(String uid) {
		List<Bucket> buckets = new ArrayList<Bucket>();
		try {
			List<String> bucketNames = null;
			Map<String, Object> param = new HashMap<String, Object>();
			if (StringUtils.isNotEmpty(uid)) {
				param.put("uid", uid);
			}
			String resp = doRequest(BUCKET_ADMIN_BUCKET, null, HttpMethodName.GET, param);
			bucketNames = (List<String>) mapper.readValue(resp, List.class);
			
			for (String bucketName : bucketNames) {
				Bucket bucket = getBucket(bucketName);
				buckets.add(bucket);
			}
		} catch (Exception e) {
			log.error("AdminClient listBuckets", e);
		}
		return buckets;
	}
	@Override
	public List<Bucket> listAllBuckets() {
		return this.listBuckets("");
	}
	/*public User getUsage(String userId) {
		User user = null;
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			//param.put("uid", userId);
			param.put("show-entries", true);
			String resp = doRequest(BUCKET_ADMIN_USEAGE, null, HttpMethodName.GET, param);
			//System.out.println("===="+resp);
			user = mapper.readValue(resp, User.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return user;
	}*/
	public String doRequest(String bucketName, String key, HttpMethodName method
			, Map<String, Object> params) throws ClientProtocolException, IOException {
		/*
		 * 根据bucketName, key, endpoint, accessKey, secretKey生成AWS签名，
		 * 请求接口时鉴权使用  
		 */
		Request<AmazonWebServiceRequest> request = this.createRequest(
				bucketName, key, null, method, this.endpoint);
		if (!request.getHeaders().containsKey(Headers.CONTENT_TYPE)) {
            request.addHeader(Headers.CONTENT_TYPE,
                "application/octet-stream");
        }
		Signer signer = this.createSigner(request, bucketName, key);
		signer.sign(request, this.awsCredentials);
		
		// 根据bucketName和key生成请求地址
		String resourcePath = this.getResourceUrl(bucketName, key);
		
		/*
		 * 根据请求方法封装请求内容
		 */
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpRequestBase httpReq = null;
		resourcePath = buildResourcePath(resourcePath, params);
		System.out.println("resourcePath:"+resourcePath);
		if (HttpMethodName.POST.equals(method)) {
			httpReq = new HttpPost(resourcePath);
		} else if (HttpMethodName.PUT.equals(method)) {
			httpReq = new HttpPut(resourcePath);
		} else if (HttpMethodName.DELETE.equals(method)) {
			httpReq = new HttpDelete(resourcePath);
		} else {
			httpReq = new HttpGet(resourcePath);
		}
		
		/*
		 * 设置请求头（header），主要是aws的鉴权信息 
		 */
		Map<String, String> headers = request.getHeaders();
		for (String hk : headers.keySet()) {
			httpReq.setHeader(hk, headers.get(hk));
		}
		
		/*
		 * 发起请求，并处理响应结果
		 */
		String response = null;
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            @Override
            public String handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };
        
		response = httpClient.execute(httpReq, responseHandler);
		httpClient.close();
		
		return response;
	}
	
	
	/**
	 * 设置请求参数
	 * @param httpReq
	 * @param params
	 */
	private String buildResourcePath(String resourcePath, Map<String, Object> params) {
		/*if (params != null && params.keySet().size() > 0) {
			resourcePath += "?1=1";
			for (String hk : params.keySet()) {
				resourcePath += "&" + hk + "=" + params.get(hk);
			}}*/
		if (params != null && params.keySet().size() > 0 && params.containsKey("user-caps")) {
			resourcePath += "?caps";
			for (String hk : params.keySet()) {
				resourcePath += "&" + hk + "=" + params.get(hk);
			}
		}else if(params != null && params.keySet().size() > 0 && params.containsKey("quota-type")){
			resourcePath += "?quota";
			for (String hk : params.keySet()) {
				resourcePath += "&" + hk + "=" + params.get(hk);
			}
		}else if(params != null && params.keySet().size() > 0){
			resourcePath += "?1=1";
			for (String hk : params.keySet()) {
				resourcePath += "&" + hk + "=" + params.get(hk);
			}
		}
		return resourcePath;
	}
	
	/*private String buildResourcePath(String resourcePath, Map<String, String> params) {
		if (params != null && params.keySet().size() > 0) {
			resourcePath += "?1=1";
			if(params.containsKey("user-caps")){
				resourcePath +="caps";
			}else{
				for (String hk : params.keySet()) {
					resourcePath += "&" + hk + "=" + params.get(hk);
				}
			}
		}
		return resourcePath;
	}*/
	
	
	public static void main(String[] args) throws Exception {
		/*String accessKey = "MEO8LEYNEK859RO6AMI5";
		String secretKey = "sExMJIur8VwYLwj8VQFBnhUCU9k8aMYYb91o1ouv";
		
		*/
		/*String accessKey = "LWOB04T40K5K4E4H58WE";
		String secretKey = "LutW+XBP9pOou3Eq+K4T2AxNsFeb60+Xi973kXVw";
		*/
		String accessKey = "RG9EBFYGGPU1A59N0ACV";
		String secretKey = "3VxXdj3iJ2/PWI4ZgZ9TnsTclcQSEU6eRPqTJ6V5";
//		String bucketName = "bucket96784904";
//		String recycleName = "recycle-bin--bucket96784904";
		
//		String accessKey = "7NZBS9DTBP6WCR5I42A0";
//		String secretKey = "hPTSji8WJQVnZQ6DL6RIeOpXrmsrpNNntfo06uGG";
//		String bucketName = "bucket1131396643";
//		String recycleName = "recycle-bin--bucket1131396643";
		
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		AdminClient s3Client = new AdminClient(credentials);
//		Bucket bucket = s3Client.getBucket(bucketName);
//		System.out.println(bucket.getUsage().getRgwMain().getSize_kb());
//		for(com.amazonaws.services.s3.model.Bucket bucket: s3Client.listBuckets()){
//			System.out.println(bucket.getName());
//			System.out.println(bucket.getOwner());
//		}
		
//		AmazonS3Client s3Client = new AmazonS3Client(credentials);
		
//		BucketLifecycleConfiguration.Rule rule1 = 
//	            new BucketLifecycleConfiguration.Rule()
//	            .withId("Archive immediately rule")
//	            .withPrefix("glacierobjects/")
//	            .addTransition(new Transition()
//	                                .withDays(0)
//	                                .withStorageClass(StorageClass.Glacier))
//	            .withStatus(BucketLifecycleConfiguration.ENABLED.toString());
//
//	            BucketLifecycleConfiguration.Rule rule2 = 
//	                new BucketLifecycleConfiguration.Rule()
//	                .withId("Archive and then delete rule")
//	                .withPrefix("projectdocs/")
//	                .addTransition(new Transition()
//	                                      .withDays(30)
//	                                      .withStorageClass(StorageClass.StandardInfrequentAccess))
//	                .addTransition(new Transition()
//	                                      .withDays(365)
//	                                      .withStorageClass(StorageClass.Glacier))
//	                .withExpirationInDays(3650)
//	                .withStatus(BucketLifecycleConfiguration.ENABLED.toString());
//
//	            BucketLifecycleConfiguration configuration = 
//	            new BucketLifecycleConfiguration()
//	            .withRules(Arrays.asList(rule1, rule2));
//	            
//	            // Save configuration.
//	            s3Client.setBucketLifecycleConfiguration(bucketName, configuration);
//	            
//	            // Retrieve configuration.
//	            configuration = s3Client.getBucketLifecycleConfiguration(bucketName);
//	            
//	            // Add a new rule.
//	            configuration.getRules().add(
//	                    new BucketLifecycleConfiguration.Rule()
//	                        .withId("NewRule")
//	                        .withPrefix("YearlyDocuments/")
//	                        .withExpirationInDays(3650)
//	                        .withStatus(BucketLifecycleConfiguration.
//	                            ENABLED.toString())
//	                       );
//	            // Save configuration.
//	            s3Client.setBucketLifecycleConfiguration(bucketName, configuration);
//	            
//	            // Retrieve configuration.
//	            configuration = s3Client.getBucketLifecycleConfiguration(bucketName);
//	            
//	            // Verify there are now three rules.
//	            configuration = s3Client.getBucketLifecycleConfiguration(bucketName);
//	            System.out.format("Expected # of rules = 3; found: %s\n", 
//	                configuration.getRules().size());
//
//	            System.out.println("Deleting lifecycle configuration. Next, we verify deletion.");
//	            // Delete configuration.
//	            s3Client.deleteBucketLifecycleConfiguration(bucketName);
//	            
//	            // Retrieve nonexistent configuration.
//	            configuration = s3Client.getBucketLifecycleConfiguration(bucketName);
//	            String s = (configuration == null) ? "No configuration found." : "Configuration found.";
//	            System.out.println(s);
        
		
//		User user1 = new User();
//		user1.setId("zhon");
//		user1.setDisplayName("zhonzhon");
//	    user1.setEmail("zhon@qq.com");
//		user1.setMaxBuckets(3);
//		user1.setSuspended(false);
//		client.modifyUser(user1);
		/*User user = client.getUser("adminadmin");
		System.out.println(user);*/
		//client.getUsage("admin");
//		client.setEndpoint("http://192.168.2.187");
//		String resp = client.doRequest("admin/metadata", "user", HttpMethodName.GET);
		
//		List<User> users = client.listUsers();
//		for (User user : users) {
//			System.out.println(user.getId() + ">>>" + user.getDisplayName());
//		}
		//client.unlinkBucket("pppppp", "robin");
		/*String bucketId = (String) client.getBucket("balabala").getParams().get("id");
		System.out.println("=========="+bucketId);
		
		client.linkBucket("balabala", "robin",bucketId);
	*/
//		User user2 = new User();
//		user2.setId("zdy8886");
//		user2.setDisplayName("zdy8885");
//		user2.setEmail("zdy8888.qq.com");
//		user2.setMaxBuckets(2);
//		
//		User user = s3Client.createUser(user2);
//		List<Keys> Keys = user.getKeys();
//		for (Keys keys2 : Keys) {
//			System.out.println(keys2.getAccess_key());
//			System.out.println(keys2.getSecret_key());
//		}
		
		//client.removeUser("wei1889fg（iiffiggg）");
		//client.createBucket("bucketwei18i88");
		
		//client.getBucket("objtgd");
		//client.getBucket("objecttest");
		
		//client.removeBucket("objtgddddddd");
		//System.out.println("=========="+client.getBucket("new").getParams().get("id"));
		//client.addUserCapability("yyyyy", "users=read");
		//client.removeUserCapability("z", "usage=*");
		//client.setQuota("nnnnnn", "user",77,88);
		//client.setBucketQuota("yyyyy", "user", 111, 222);
		//client.getQuota("admin", "user");
		//client.listBuckets("admin");
		//client.listAllBuckets();
	}
	
}
