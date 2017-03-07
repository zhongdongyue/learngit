/*  
 * 文件名：JsonUtil.java
 * 版权：<版权>
 * 描述：<描述>
 * 修改人：<修改人>
 * 修改时间：2016年10月22日
 * 修改单号：<修改单号>
 * 修改内容：<修改内容>
 * 
 */
package com.trendytech.tds.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * [一句话功能描述]
 * [详细功能描述]
 * @version [版本号 2016年10月22日]
 * @author Robin
 * @since <起始版本>
 *
 */
public class JsonUtil {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(JsonUtil.class);

	public static String readFromFile(String filePath) {
        
        File file = new File(filePath);
        Scanner scanner = null;
        StringBuilder buffer = new StringBuilder();
        try {
            scanner = new Scanner(file, "utf-8");
            while (scanner.hasNextLine()) {
                buffer.append(scanner.nextLine());
            }
 
        } catch (FileNotFoundException e) {
        	LOGGER.error("", e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        
        return buffer.toString();
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, String> getKeys(String username, String password) {
		String classpath = JsonUtil.class.getResource("/").getPath();
		ObjectMapper mapper = new ObjectMapper();
		try {
			List<Map<String, String>> list = (List<Map<String, String>>) mapper
					.readValue(new File(classpath + "/users.json"), List.class);
			for (Map<String, String> user : list) {
				if (username.equals(user.get("name")) && password.equals(user.get("password"))) {
					return user;
				}
			}
		} catch (JsonParseException e) {
			LOGGER.error("", e);
		} catch (JsonMappingException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		
		return null;
	}
	
	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		System.out.println(getKeys("robin01", "111111"));
	}
}
