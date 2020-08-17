package io.inprice.api;

import java.net.URI;
import java.net.URISyntaxException;

public class Test {

  private static String extractHostname(String url) {
    try {
      URI uri = new URI(url);
      String domain = uri.getHost();
      if (domain == null) domain = url;
      if (domain.indexOf("/") > 0) {
        domain = domain.substring(0, domain.indexOf("/"));
      }
      return domain.startsWith("www.") ? domain.substring(4) : domain;
    } catch (URISyntaxException e) {
      //ignored
    }
    return null;
  }

  public static void main(String[] args) {

    System.out.println(extractHostname("http://hasan.bey"));
    System.out.println(extractHostname("https://www.hasan.bey"));
    System.out.println(extractHostname("www.hasan.bey"));
    System.out.println(extractHostname("hasan.bey"));
    System.out.println(extractHostname("http://hasan.bey/sdfsfsd"));
    System.out.println(extractHostname("https://www.hasan.bey/dfc"));
    System.out.println(extractHostname("www.hasan.bey/dsdf/sdfsdf"));
    System.out.println(extractHostname("hasan.bey/sdcs/sdfsd/sfsd"));

  }
  
}