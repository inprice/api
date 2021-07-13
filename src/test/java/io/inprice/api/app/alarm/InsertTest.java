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
	public void Request_body_is_invalid_WITH_no_body() {
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
	public void You_have_reached_max_alarm_number_of_your_plan() {
		JSONObject json = callTheService(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), createBody("LINK", 1L, "PRICE", "CHANGED"));

		assertEquals(910, json.getInt("status"));
		assertEquals("You have reached max alarm number of your plan!", json.getString("reason"));
	}

	@Test
	public void You_havent_picked_a_plan_yet() {
		JSONObject json = callTheService(TestAccounts.Without_a_plan_and_extra_user.ADMIN(), createBody("LINK", 1L, "PRICE", "CHANGED"));

		assertEquals(903, json.getInt("status"));
		assertEquals("You haven't picked a plan yet!", json.getString("reason"));
	}
	
	@Test
	public void Everything_must_be_ok() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray linkList = TestFinder.searchLinks(cookies, "WAITING");

		assertNotNull(linkList);
		assertEquals(1, linkList.length());

		//get the first link
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

	private JSONObject createBody(String topic, Long id, String subject, String when) {
		return createBody(topic, id, subject, when, null, null, null);
	}

	private JSONObject createBody(String topic, Long id, String subject, String when, String certainStatus, BigDecimal amountLowerLimit, BigDecimal amountUpperLimit) {
		JSONObject body = new JSONObject();

		if (id != null) {
  		if ("LINK".equals(topic)) body.put("linkId", id);
  		if ("GROUP".equals(topic)) body.put("groupId", id);
		}

		if (topic != null) body.put("topic", topic);;
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
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
