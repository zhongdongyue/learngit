package com.trendytech.tds.base.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.trendytech.tds.util.Constants;
import com.trendytech.tds.util.JsonUtil;

public class BaseController {

	protected Logger log = LoggerFactory.getLogger(this.getClass());

	protected static final String DATA = "data";
	protected static final String RESULT = "result";
	protected static final String SUCCESS = "success";
	protected static final String FAILURE = "failure";
	protected static final String MESSAGE = "message";

	protected static final String PAGER = "pager";
	protected static final String CONTENT = "content";
	protected static final String TITLE = "title";
	protected static final String MAIN_VIEW = "/common/portal";
	protected static final String LIST = "list";
	protected static final String ALARM = "alarm";
	protected static final String NO_AUTH = "noAuth";
	
	protected static final String ATTR_USER = "user";
	
	protected static final String DAYS="days ago";
	protected static final String HOURS="hours ago";
	protected static final String MINUTES="minutes ago";
	
    protected AmazonS3 client(String username, String password) {
    	
    	Map<String, String> user = JsonUtil.getKeys(username, password);
    	if (user != null) {
    		AWSCredentials credentials = new BasicAWSCredentials(
    				user.get("accessKey"), user.get("secretKey"));
    		
    		ClientConfiguration clientConfig = new ClientConfiguration();
    		clientConfig.setSignerOverride("S3SignerType");
    		
    		AmazonS3 conn = new AmazonS3Client(credentials,clientConfig);
    		conn.setEndpoint(Constants.OS_ENDPOINT);
    		
    		return conn;
		}
		
		return null;
	}
	
	/*@ExceptionHandler
	public ModelAndView exp(HttpServletRequest request, Exception ex) {
		
		log.error("Abnormal operation of the system", ex);
		
		ModelMap map = new ModelMap();
        String code = "default";	// 默认异常
        String errorUrl = "/error/error";
        
        // Ajax异常页面
        if(isAjaxRequest(request)) {
            errorUrl = "/error/error-ajax";
        }
        
        // 系统业务异常
        if(ex instanceof BusinessException) {
            String codeTemp = ((BusinessException) ex).getCode();
            if(codeTemp != null && !"".equals(codeTemp)) {
                code = codeTemp;
            }
        }

        // 异常信息封装
        String cause = getMessage(request, code + ".cause", new String[0], "");
        String description = getMessage(request, code + ".description", new String[0], "");
        String solution = getMessage(request, code + ".solution", new String[0], "");
        map.put("code", code);
        map.put("cause", cause);
        map.put("description", description);
        map.put("solution", solution);

        String publicUrl = PropertiesUtil.getString("url.public");
        request.setAttribute("publicUrl", publicUrl);
		return new ModelAndView(errorUrl, map);
	}*/
	
}
