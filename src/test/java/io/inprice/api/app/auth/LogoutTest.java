package io.inprice.api.app.auth;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.app.utils.Fixtures;
import io.inprice.api.app.utils.TestHelper;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * 
 * @author mdpinar
 * @since 2021-07-01
 */
@RunWith(JUnit4.class)
public class LogoutTest {

	private static final String SERVICE_ENDPOINT = "/logout";

	@BeforeClass
	public static void setup() {
		TestHelper.initTestServers();
	}
	
	@Test
	public void Seems_that_you_are_already_logged_out_WITH_no_cookie() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT).asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 801);
		assertEquals(json.getString("reason"), "Seems that you are already logged out!");
	}
	
	@Test
	public void Seems_that_you_are_already_logged_out_FOR_multiple_logout() {
		//in order to get a valid cookied, user logins first
		Cookies cookies = Unirest.post("/login")
			.body(Fixtures.EDITOR_USER)
			.asEmpty()
			.getCookies();
		
		//first logout
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT).cookie(cookies).asJson();
		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 200);
		
		//second logout
		res = Unirest.post(SERVICE_ENDPOINT).cookie(cookies).asJson();
		json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 801);
		assertEquals(json.getString("reason"), "Seems that you are already logged out!");
	}
	
	@Test
	public void Everything_must_be_ok_WITH_superuser_cookie() {
		//in order to get a valid cookied, super user logins first
		Cookies cookies = Unirest.post("/login")
			.body(Fixtures.SUPER_USER)
			.asEmpty()
			.getCookies();
		
		//handled cookie is used here
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT).cookie(cookies).asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 200);
		assertEquals(json.getString("reason"), "OK");
	}

	@Test
	public void Everything_must_be_ok_WITH_normal_user_cookie() {
		//in order to get a valid cookied, user logins first
		Cookies cookies = Unirest.post("/login")
			.body(Fixtures.EDITOR_USER)
			.asEmpty()
			.getCookies();
		
		//handled cookie is used here
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT).cookie(cookies).asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 200);
    assertEquals(json.getString("reason"), "OK");
	}

}
