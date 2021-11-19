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
 * Tests the functionality of AlarmController.update(AlarmDTO)
 * 
 * @author mdpinar
 * @since 2021-07-14
 */
@RunWith(JUnit4.class)
public class UpdateTest {

	private static final String SERVICE_ENDPOINT = "/alarm";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(createBody(1L, "LINK", "POSITION", "CHANGED"))
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
	public void Alarm_not_found_WITH_null_id() {
		JSONObject json = callTheService(createBody(null, "LINK", "POSITION", "CHANGED"));

		assertEquals(404, json.getInt("status"));
		assertEquals("Alarm not found!", json.getString("reason"));
	}

	/**
	 * Consists of four steps;
	 *	a) to gather other workspace's alarms, admin is logged in
	 *	b) searches some specific alarms
	 *  c) picks one of those alarms
	 *  d) evil user tries to update other workspace's alarm
	 */
	@Test
	public void Alarm_not_found_WHEN_trying_to_update_someone_elses_alarm() {
		//to gather other workspace's links, admin is logged in
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN());

		//searches some specific links
		JSONArray alarmList = TestFinder.searchAlarms(cookies, "LINK");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(alarmList);

		//picks one of those alarms
		JSONObject alarm = alarmList.getJSONObject(0);

		//evil user tries to update the alarm
		JSONObject json = callTheService(createBody(alarm.getLong("id"), "LINK", "POSITION", "EQUAL", "Lowest", null, null));

		assertEquals(404, json.getInt("status"));
		assertEquals("Alarm not found!", json.getString("reason"));
	}

	@Test
	public void Topic_cannot_be_empty() {
		JSONObject json = callTheService(createBody(1L, null, "POSITION", "CHANGED"));

		assertEquals(400, json.getInt("status"));
		assertEquals("Topic cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Subject_cannot_be_empty() {
		JSONObject json = callTheService(createBody(1L, "LINK", null, "CHANGED"));

		assertEquals(400, json.getInt("status"));
		assertEquals("Subject cannot be empty!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_when_the_subject_should_be_considered() {
		JSONObject json = callTheService(createBody(1L, "LINK", "POSITION", null));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify when the subject should be considered!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_a_certain_position() {
		JSONObject json = callTheService(createBody(1L, "LINK", "POSITION", "EQUAL"));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify a certain position!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for() {
		JSONObject json = callTheService(createBody(1L, "LINK", "PRICE", "OUT_OF_LIMITS"));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for_WITH_values_less_than_1() {
		JSONObject json = callTheService(createBody(1L, "LINK", "PRICE", "OUT_OF_LIMITS", null, BigDecimal.ZERO, BigDecimal.ZERO));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for_WITH_values_greater_than_or_equals_10_million() {
		JSONObject json = callTheService(createBody(1L, "LINK", "PRICE", "OUT_OF_LIMITS", null, new BigDecimal(10_000_000), new BigDecimal(10_000_000)));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		//this user has two roles; one is admin and the other is viewer. so, we need to specify the session number as second to pick viewer session!
		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER(), createBody(1L, "LINK", "PRICE", "CHANGED"), 0); //attention!

		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, createBody(1L, "LINK", "PRICE", "CHANGED"));

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void You_have_already_defined_this_alarm_previously() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray alarmedLinkList = TestFinder.searchAlarms(cookies, "LINK");

		assertNotNull(alarmedLinkList);
		assertEquals(2, alarmedLinkList.length());

		//get the first alarm for a link
		JSONObject alarmedLink = alarmedLinkList.getJSONObject(0);
		
		JSONObject json = callTheService(createBody(alarmedLink.getLong("id"), "LINK", "POSITION", "EQUAL", "Average", null, null));

		assertEquals(880, json.getInt("status"));
		assertEquals("You have already defined this alarm previously!", json.getString("reason"));
	}
	
	@Test
	public void Everything_must_be_ok_FOR_a_link_WITH_admin() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray alarmedLinkList = TestFinder.searchAlarms(cookies, "LINK");

		assertNotNull(alarmedLinkList);
		assertEquals(2, alarmedLinkList.length());

		//get the first alarm for a link
		JSONObject alarmedLink = alarmedLinkList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(alarmedLink.getLong("id"), "LINK", "POSITION", "NOT_EQUAL", "MAXIMUM", null, null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertNotNull(json.getJSONObject("data"));
	}

	@Test
	public void Everything_must_be_ok_FOR_a_product_WITH_editor() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.EDITOR());

		JSONArray alarmedProductList = TestFinder.searchAlarms(cookies, "PRODUCT");

		assertNotNull(alarmedProductList);
		assertEquals(1, alarmedProductList.length());

		//get the first alarm for a product
		JSONObject alarmedProduct = alarmedProductList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(alarmedProduct.getLong("id"), "PRODUCT", "POSITION", "NOT_EQUAL", "Highest", null, null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertNotNull(json.getJSONObject("data"));
	}

	private JSONObject createBody(Long id, String topic, String subject, String when) {
		return createBody(id, topic, subject, when, null, null, null);
	}

	private JSONObject createBody(Long id, String topic, String subject, String when, String certainPosition, BigDecimal amountLowerLimit, BigDecimal amountUpperLimit) {
		JSONObject body = new JSONObject();

		if (id != null) body.put("id", id);
		
		if (topic != null) body.put("topic", topic);
		if (subject != null) body.put("subject", subject);
		if (when != null) body.put("subjectWhen", when);
		if (certainPosition != null) body.put("certainPosition", certainPosition);
		if (amountLowerLimit != null) body.put("amountLowerLimit", amountLowerLimit);
		if (amountUpperLimit != null) body.put("amountUpperLimit", amountUpperLimit);

		return body;
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(TestWorkspaces.Basic_plan_but_no_extra_user.ADMIN(), body);
	}

	private JSONObject callTheService(JSONObject user, JSONObject body) {
		return callTheService(user, body, 0);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
