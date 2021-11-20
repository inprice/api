package io.inprice.api.app.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestWorkspaces;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of AlarmController.insert(AlarmDTO)
 * 
 * @author mdpinar
 * @since 2021-07-12
 */
@RunWith(JUnit4.class)
public class InsertTest {

	private static final String SERVICE_ENDPOINT = "/alarm";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(createBody("LINK", "POSITION", "CHANGED"))
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		JSONObject json = callTheService(null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Topic_cannot_be_empty() {
		JSONObject json = callTheService(createBody(null, "POSITION", "CHANGED"));

		assertEquals(400, json.getInt("status"));
		assertEquals("Topic cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Subject_cannot_be_empty() {
		JSONObject json = callTheService(createBody("LINK", null, "CHANGED"));

		assertEquals(400, json.getInt("status"));
		assertEquals("Subject cannot be empty!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_when_the_subject_should_be_considered() {
		JSONObject json = callTheService(createBody("LINK", "POSITION", null));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify when the subject should be considered!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_a_certain_position() {
		JSONObject json = callTheService(createBody("LINK", "POSITION", "EQUAL"));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify a certain position!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for() {
		JSONObject json = callTheService(createBody("LINK", "PRICE", "OUT_OF_LIMITS"));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for_WITH_values_less_than_1() {
		JSONObject json = callTheService(createBody("LINK", "PRICE", "OUT_OF_LIMITS", null, BigDecimal.ZERO, BigDecimal.ZERO));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for_WITH_values_greater_than_or_equals_10_million() {
		JSONObject json = callTheService(createBody("LINK", "PRICE", "OUT_OF_LIMITS", null, new BigDecimal(100_000_000), new BigDecimal(100_000_000)));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		//this user has two roles; one is admin and the other is viewer. so, we need to specify the session number as second to pick viewer session!
		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER(), createBody("LINK", "PRICE", "CHANGED"), 0); //attention!

		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, createBody("LINK", "PRICE", "CHANGED"), 0);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void You_have_already_set_an_alarm_for_this_record() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_no_extra_users.ADMIN());

		JSONArray alarmList = TestFinder.searchAlarms(cookies, "PRODUCT");

		assertNotNull(alarmList);
		assertEquals(1, alarmList.length());

		//get the first link
		JSONObject found = alarmList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(found)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(880, json.getInt("status"));
		assertEquals("You have already defined this alarm previously!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody("LINK", "POSITION", "CHANGED"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");

		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
	}

	private JSONObject createBody(String topic, String subject, String when) {
		return createBody(topic, subject, when, null, null, null);
	}

	private JSONObject createBody(String topic, String subject, String when, String certainPosition, BigDecimal amountLowerLimit, BigDecimal amountUpperLimit) {
		JSONObject body = new JSONObject();

		if (topic != null) body.put("topic", topic);
		if (subject != null) body.put("subject", subject);
		if (when != null) body.put("subjectWhen", when);
		if (certainPosition != null) body.put("certainPosition", certainPosition);
		if (amountLowerLimit != null) body.put("amountLowerLimit", amountLowerLimit);
		if (amountUpperLimit != null) body.put("amountUpperLimit", amountUpperLimit);

		return body;
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(TestWorkspaces.Basic_plan_but_no_extra_user.ADMIN(), body, 0);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
