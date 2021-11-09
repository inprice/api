package io.inprice.api.app.exim.imbort;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestUtils;
import io.inprice.api.utils.TestWorkspaces;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of LinkController.upload(String csvContent) under exim
 * 
 * @author mdpinar
 * @since 2021-11-07
 */
@RunWith(JUnit4.class)
public class LinkTest {

	private static final String SERVICE_ENDPOINT = "/exim/link/upload";
	
	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body("empty.csv")
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER(), "empty.csv");

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	@Test
	public void Request_body_is_invalid_WITH_empty_content_FOR_editor() {
		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR(), "empty.csv");

		assertEquals(400, json.getInt("status"));
		assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Wrong_content_WITH_admin() {
		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR(), "products_wrong.csv");

		assertEquals(200, json.getInt("status"));
		assertEquals(0, json.getJSONObject("data").getInt("successCount"));
	}

	@Test
	public void Everything_must_be_ok_WITH_editor() {
		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR(), "links_1.csv");

		assertEquals(200, json.getInt("status"));
		
		JSONObject data = json.getJSONObject("data");
		assertEquals(data.getInt("total"), data.getInt("successCount"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		JSONObject json = callTheService(TestWorkspaces.Pro_plan_with_no_user.ADMIN(), "links_2.csv");

		assertEquals(200, json.getInt("status"));
		
		JSONObject data = json.getJSONObject("data");
		assertEquals(data.getInt("total"), data.getInt("successCount"));
	}

	private JSONObject callTheService(JSONObject user, String fileName) {
		Cookies cookies = TestUtils.login(user);
		
		String body = null;
		try {
			body = IOUtils.toString(this.getClass().getResourceAsStream("/files/"+fileName), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.contentType("text/csv")
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
