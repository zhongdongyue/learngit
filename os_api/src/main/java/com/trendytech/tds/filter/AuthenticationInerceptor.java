package com.trendytech.tds.filter;

import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.trendytech.tds.util.Constants;
import com.trendytech.tds.util.JsonUtil;

public class AuthenticationInerceptor implements HandlerInterceptor {
	protected Logger LOGGER = LoggerFactory.getLogger(getClass());
	
	@Override
	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object arg2, Exception arg3)
			throws Exception {
	}

	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object arg2, ModelAndView arg3)
			throws Exception {
	}

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		String userName = request.getHeader("User-Name");
		String password = request.getHeader("Password");
		Map<String, String> user = JsonUtil.getKeys(userName, password);
		if (user == null) {
			response.setStatus(403);
			ServletOutputStream out = response.getOutputStream();
		    out.write(Constants.AUTH_FALUE.getBytes());
		    out.close();
			return false;
		}
	    
		return true;
	}
}
