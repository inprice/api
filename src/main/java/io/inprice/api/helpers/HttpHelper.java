package io.inprice.api.helpers;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpHelper
 */
public class HttpHelper {

  private static final Logger log = LoggerFactory.getLogger(HttpHelper.class);

  public static String GET(String url) {
    HttpGet request = new HttpGet(url);
    try (CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = httpClient.execute(request)) {

      HttpEntity entity = response.getEntity();
      return EntityUtils.toString(entity);
    } catch (IOException e) {
      log.error("Failed to get", e);
    }
    return null;
  }

  public static String GET(CloseableHttpClient httpClient, String url) {
    HttpGet request = new HttpGet(url);
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      HttpEntity entity = response.getEntity();
      return EntityUtils.toString(entity);
    } catch (IOException e) {
      log.error("Failed to get", e);
    }
    return null;
  }

}