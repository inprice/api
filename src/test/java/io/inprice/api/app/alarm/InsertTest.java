package io.inprice.api.app.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.TestAccounts;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.json.JSONArray;

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
	public void Everything_must_be_OK_WITH_correct_credentials_OF_superuser() {
		Cookies cookies = TestUtils.login(TestAccounts.Basic_plan_but_no_extra_user.ADMIN());

		JSONArray data = TestFinder.searchLinks(cookies, "WAITING");
		
		TestUtils.logout(cookies);

		assertNotNull(data);
		assertEquals(5, data.length());
	}

}
