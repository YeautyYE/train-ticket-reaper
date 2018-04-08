package com.yeauty.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public class JsonUtils {

    // 定义jackson对象
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    /**
     * 
     * @Description:将对象转换成json字符串
     * @param data
     * @return
     * @author:Yeauty
     * @time:2017年8月18日 下午5:50:03
     */
    public static String objectToJson(Object data) {
    	try {
			String string = MAPPER.writeValueAsString(data);
			return string;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    /**
     * 
     * @Description:将json结果集转化为对象
     * @param jsonData
     * @param beanType
     * @return
     * @author:Yeauty
     * @time:2017年8月18日 下午5:50:14
     */
    public static <T> T jsonToPojo(String jsonData, Class<T> beanType) throws IOException {
		T t = MAPPER.readValue(jsonData, beanType);
		return t;
    }
    
    /**
     * 
     * @Description:将json数据转换成pojo对象list
     * @param jsonData
     * @param beanType
     * @return
     * @author:Yeauty
     * @time:2017年8月18日 下午5:55:22
     */
    public static <T>List<T> jsonToList(String jsonData, Class<T> beanType) {
    	JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, beanType);
    	try {
    		List<T> list = MAPPER.readValue(jsonData, javaType);
    		return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return null;
    }
    
    /**
     * 
     * @Description:将json数据转换成Map
     * @param jsonData
     * @param beanType1
     * @param beanType2
     * @return
     * @author:Yeauty
     * @time:2017年8月18日 下午5:55:27
     */
    public static <T,V> Map<T,V> jsonToMap(String jsonData, Class<T> beanType1,Class<V> beanType2) {
    	JavaType javaType = MAPPER.getTypeFactory().constructParametricType(Map.class, beanType1 ,beanType2);
    	try {
    		Map<T,V> map = MAPPER.readValue(jsonData, javaType);
    		return map;
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	return null;
    }
    
    /**
     * 
     * @Description:将json数据转换成JsonNode对象，可以获取子节点
     * @param json
     * @return
     * @author:Yeauty
     * @time:2017年8月18日 下午5:55:39
     */
    public static JsonNode jsonToJsonNode(String json) {
    	JsonNode jsonNode;
		try {
			jsonNode = MAPPER.readTree(json);
			return jsonNode;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
    }

	/**
	 * @Description: 	从jsonNode中获取某个字段的值
	 * @param jsonNode
	 * @param fieldName
	 * @return: 		无论是null，还是空字符串，都返回null
	 * @author:Yeauty
	 * @time: 2017/9/6 19:14
	 */
    public static String getStringFromNode(JsonNode jsonNode, String fieldName){
		JsonNode node = jsonNode.get(fieldName);
		if(node!=null){
			String textValue = node.asText();
			if("".equals(textValue.trim())){
				return null;
			}else{
				return textValue;
			}
		}else {
			return null;
		}
	}
    
}
