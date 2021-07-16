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
 * Tests the functionality of AlarmService.insert(AlarmDTO)
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
			.body(createBody("LINK", 1L, "STATUS", "CHANGED"))
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
		JSONObject json = callTheService(createBody(null, 1L, "STATUS", "CHANGED"));

		assertEquals(400, json.getInt("status"));
		assertEquals("Topic cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Topic_id_cannot_be_empty() {
		JSONObject json = callTheService(createBody("LINK", null, "STATUS", "CHANGED"));

		assertEquals(400, json.getInt("status"));
		assertEquals("Topic id cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Subject_cannot_be_empty() {
		JSONObject json = callTheService(createBody("LINK", 1L, null, "CHANGED"));

		assertEquals(400, json.getInt("status"));
		assertEquals("Subject cannot be empty!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_when_the_subject_should_be_considered() {
		JSONObject json = callTheService(createBody("LINK", 1L, "STATUS", null));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify when the subject should be considered!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_a_certain_status() {
		JSONObject json = callTheService(createBody("LINK", 1L, "STATUS", "EQUAL"));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify a certain status!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for() {
		JSONObject json = callTheService(createBody("LINK", 1L, "PRICE", "OUT_OF_LIMITS"));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for_WITH_values_less_than_1() {
		JSONObject json = callTheService(createBody("LINK", 1L, "PRICE", "OUT_OF_LIMITS", null, BigDecimal.ZERO, BigDecimal.ZERO));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void You_are_expected_to_specify_either_lower_or_upper_limit_for_WITH_values_greater_than_or_equals_10_million() {
		JSONObject json = callTheService(createBody("LINK", 1L, "PRICE", "OUT_OF_LIMITS", null, new BigDecimal(10_000_000), new BigDecimal(10_000_000)));

		assertEquals(400, json.getInt("status"));
		assertEquals("You are expected to specify either lower or upper limit for price!", json.getString("reason"));
	}

	@Test
	public void You_havent_picked_a_plan_yet() {
		JSONObject json = callTheService(TestAccounts.Without_a_plan_and_extra_user.ADMIN(), createBody("LINK", 1L, "PRICE", "CHANGED"), 0);

		assertEquals(903, json.getInt("status"));
		assertEquals("You haven't picked a plan yet!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		//this user has two roles; one is admin and the other is viewer. so, we need to specify the session number as second to pick viewer session!
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.VIEWER(), createBody("LINK", 1L, "PRICE", "CHANGED"), 1); //attention!

		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_super_user() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, createBody("LINK", 1L, "PRICE", "CHANGED"), 0);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void You_have_already_set_an_alarm_for_this_record() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_no_extra_users.ADMIN());

		JSONArray alarmedGroupList = TestFinder.searchAlarms(cookies, "GROUP");

		assertNotNull(alarmedGroupList);
		assertEquals(1, alarmedGroupList.length());

		//get the first link
		JSONObject alarmedGroup = alarmedGroupList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody("GROUP", alarmedGroup.getLong("groupId"), "STATUS", "CHANGED"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(880, json.getInt("status"));
		assertEquals("You have already set an alarm for this record!", json.getString("reason"));
	}

	@Test
	public void You_have_reached_max_alarm_number_of_your_plan() {
		JSONObject json = callTheService(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), createBody("LINK", 1L, "PRICE", "CHANGED"), 0);

		assertEquals(910, json.getInt("status"));
		assertEquals("You have reached max alarm number of your plan!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_FOR_a_link_WITH_admin() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray linkList = TestFinder.searchLinks(cookies, "WAITING");

		assertNotNull(linkList);
		assertEquals(1, linkList.length());

		//get the first alarm for a link
		JSONObject link = linkList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody("LINK", link.getLong("id"), "STATUS", "CHANGED"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");

		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
		assertEquals(link.getLong("id"), data.getLong("linkId"));
	}

	@Test
	public void Everything_must_be_ok_FOR_a_group_WITH_editor() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray groupList = TestFinder.searchGroups(cookies, "");

		assertNotNull(groupList);
		assertEquals(2, groupList.length());

		//get the first alarm for a group
		JSONObject group = groupList.getJSONObject(1); //since first group is already alarmed!

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody("GROUP", group.getLong("id"), "STATUS", "CHANGED"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");

		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
		assertEquals(group.getLong("id"), data.getLong("groupId"));
	}

	private JSONObject createBody(String topic, Long id, String subject, String when) {
		return createBody(topic, id, subject, when, null, null, null);
	}

	private JSONObject createBody(String topic, Long topicId, String subject, String when, String certainStatus, BigDecimal amountLowerLimit, BigDecimal amountUpperLimit) {
		JSONObject body = new JSONObject();

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
		return callTheService(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), body, 0);
	}
	
	public JSONObject callTheService(JSONObject user, JSONObject body, int session) {
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
