package com.mod.loan.util.huijuutil;

/**
 * 类HttpClientUtil
 * 
 * @author Lori 2018年6月04日 下午16:10:04
 */

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HttpClientUtil {
	public static String sendHttpPost(String url, String body) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(url);
		httpPost.addHeader("Content-Type", "application/json;charset=UTF-8");
		// UrlEncodedFormEntity setEntity = new UrlEncodedFormEntity(body, HTTP.UTF_8);
		StringEntity setEntity = new StringEntity(body, "utf-8");
		setEntity.setContentType("application/json");
		setEntity.setContentEncoding("UTF-8");
		httpPost.setEntity(setEntity);

		CloseableHttpResponse response = httpClient.execute(httpPost);
		HttpEntity entity = response.getEntity();
		String responseContent = EntityUtils.toString(entity, "UTF-8");

		response.close();
		httpClient.close();
		return responseContent;
	}
}
