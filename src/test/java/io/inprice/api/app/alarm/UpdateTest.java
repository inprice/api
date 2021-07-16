package io.inprice.api.app.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestAccounts;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of AlarmService.update(AlarmDTO)
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
			.body(createBody(1L, "LINK", 1L, "STATUS", "CHANGED"))
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
		JSONObject json = callTheService(createBody(null, "LINK", 1L, "STATUS", "CHANGED"));

		assertEquals(404, json.getInt("status"));
		assertEquals("Alarm not found!", json.getString("reason"));
	}

	@Test
	public void Alarm_not_found_WITH_wrong_id() {
		JSONObject json = callTheService(createBody(0L, "LINK", 1L, "STATUS", "CHANGED"));

		assertEquals(404, json.getInt("status"));
		assertEquals("Alarm not found!", json.getString("reason"));
	}

	@Test
	public void Topic_cannot_be_empty() {
		JSONObject json = callTheService(createBody(1L, null, 1L, "STATUS", "CHANGED"));

		assertEquals(400, json.getInt("status"));
		assertEquals("Topic cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Topic_id_cannot_be_empty() {
		JSONObject json = callTheService(createBody(1L, "LINK", null, "STATUS", "CHANGED"));

		assertEquals(400, json.getInt("status"));
		assertEquals("Topic id cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Subject_cannot_be_empty() {
		JSONObject json = callTheService(createBody(1L, "LINK", 1L, null, "CHANGED"));

		assertEquals(400, json.getInt("status"));
		assertEquals("Subject cannot be empty!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_when_the_subject_should_be_considered() {
		JSONObject json = callTheService(createBody(1L, "LINK", 1L, "STATUS", null));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify when the subject should be considered!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_a_certain_status() {
		JSONObject json = callTheService(createBody(1L, "LINK", 1L, "STATUS", "EQUAL"));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify a certain status!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for() {
		JSONObject json = callTheService(createBody(1L, "LINK", 1L, "PRICE", "OUT_OF_LIMITS"));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for_WITH_values_less_than_1() {
		JSONObject json = callTheService(createBody(1L, "LINK", 1L, "PRICE", "OUT_OF_LIMITS", null, BigDecimal.ZERO, BigDecimal.ZERO));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for_WITH_values_greater_than_or_equals_10_million() {
		JSONObject json = callTheService(createBody(1L, "LINK", 1L, "PRICE", "OUT_OF_LIMITS", null, new BigDecimal(10_000_000), new BigDecimal(10_000_000)));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		//this user has two roles; one is admin and the other is viewer. so, we need to specify the session number as second to pick viewer session!
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.VIEWER(), createBody(1L, "LINK", 1L, "PRICE", "CHANGED"), 1); //attention!

		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_super_user() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, createBody(1L, "LINK", 1L, "PRICE", "CHANGED"));

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_FOR_a_link_WITH_admin() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray alarmedLinkList = TestFinder.searchAlarms(cookies, "LINK");

		assertNotNull(alarmedLinkList);
		assertEquals(1, alarmedLinkList.length());

		//get the first alarm for a link
		JSONObject alarmedLink = alarmedLinkList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(alarmedLink.getLong("id"), "LINK", alarmedLink.getLong("linkId"), "STATUS", "NOT_EQUAL", "MAXIMUM", null, null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertNotNull(json.getJSONObject("data"));
	}

	@Test
	public void Everything_must_be_ok_FOR_a_group_WITH_editor() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.EDITOR());

		JSONArray alarmedGroupList = TestFinder.searchAlarms(cookies, "GROUP");

		assertNotNull(alarmedGroupList);
		assertEquals(1, alarmedGroupList.length());

		//get the first alarm for a group
		JSONObject alarmedGroup = alarmedGroupList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(alarmedGroup.getLong("id"), "GROUP", alarmedGroup.getLong("groupId"), "STATUS", "NOT_EQUAL", "MAXIMUM", null, null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertNotNull(json.getJSONObject("data"));
	}

	private JSONObject createBody(Long id, String topic, Long topicId, String subject, String when) {
		return createBody(id, topic, topicId, subject, when, null, null, null);
	}

	private JSONObject createBody(Long id, String topic, Long topicId, String subject, String when, String certainStatus, BigDecimal amountLowerLimit, BigDecimal amountUpperLimit) {
		JSONObject body = new JSONObject();

		if (id != null) body.put("id", id);
		
		if (topicId != null) {
  		if ("LINK".equals(topic)) body.put("linkId", topicId);
  		if ("GROUP".equals(topic)) body.put("groupId", topicId);
		}

		if (topic != null) body.put("topic", topic);
		if (subject != null) body.put("subject", subject);
		if (when != null) body.put("subjectWhen", when);
		if (certainStatus != null) body.put("certainStatus", certainStatus);
		if (amountLowerLimit != null) body.put("amountLowerLimit", amountLowerLimit);
		if (amountUpperLimit != null) body.put("amountUpperLimit", amountUpperLimit);

		return body;
	}

	public JSONObject callTheService(JSONObject body) {
		return callTheService(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), body);
	}

	public JSONObject callTheService(JSONObject user, JSONObject body) {
		return callTheService(user, body, 0);
	}
	
	public JSONObject callTheService(JSONObject user, JSONObject body, int session) {
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
