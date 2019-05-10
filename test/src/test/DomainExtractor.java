package test;

public class DomainExtractor {

	public static void main(String[] args) {
		System.out.println(getHostName("http://www.git-getir.COM.tr"));
		System.out.println(getHostName("https://git-getir.COM"));
		System.out.println(getHostName("www.git-getir.COM.tr"));
		System.out.println(getHostName("http://www.git-getir.COM"));
		System.out.println(getHostName("https://www.git-getir.COM.tr"));

		System.out.println(getHostName("http://urun.git-getir.COM"));
		System.out.println(getHostName("https://urun.git-getir.COM"));

		System.out.println(getHostName("www.urun.git-getir.COM"));
		System.out.println(getHostName("http://www.urun.git-getir.COM.tr"));
		System.out.println(getHostName("https://www.urun.git-getir.COM"));

		System.out.println(getHostName("www.GOOGLE.COM"));
		System.out.println(getHostName("www.GOOGLE.COM.tr:8080"));
		System.out.println(getHostName("www.hepsiburada.com/aaa/bbb/ccc-ddd"));
		System.out.println(getHostName("http://urun.hepsiburada.com.tr:7070/aaa/bbb/ccc-ddd"));
		System.out.println(getHostName("https://urun.hepsiburada.com/aaa/bbb/ccc-ddd"));
		System.out.println(getHostName("HTTP://www.hepsiburada.com.tr/aaa/bbb/ccc-ddd"));
		System.out.println(getHostName("https://www.hepsiburada.com/aaa/bbb/ccc-ddd"));
		System.out.println(getHostName("HTTPS://WWW.HEPSIBURADA.COM.TR:9090/URUN-DETAY/123"));
		System.out.println(getHostName("hepsiburada.com/aaa/bbb/ccc-ddd"));
		System.out.println(getHostName("urun.hepsiburada.com/aaa/bbb/ccc-ddd"));
		System.out.println(getHostName("https://incoming-sms.vkk.io/aaa/bbb/ccc-ddd"));
		System.out.println(getHostName("https://incoming.sms.vkk.io/aaa/bbb/ccc-ddd"));
	}

	public static String getHostName(String url) {
		String decent = url.toLowerCase();

		if (! decent.contains("www.")) {
			if (! decent.startsWith("http")) {
				decent = "www." + decent;
			} else {
				decent = decent.replaceFirst("//", "//www.");
			}
		}

		String domainName = decent.replaceFirst("^(?i)(http|www).*?\\.", "");

		int index = domainName.indexOf('/');
		if (index != -1) {
			domainName = domainName.substring(0, index);
		}

		index = domainName.indexOf(':');
		if (index != -1) {
			domainName = domainName.substring(0, index);
		}

		return domainName;
	}
}