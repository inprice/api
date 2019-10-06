package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.dto.TicketDTO;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.helpers.TestHelper;
import io.inprice.scrapper.common.meta.TicketSource;
import io.inprice.scrapper.common.meta.TicketType;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

public class TicketTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true);
    }

    @Test
    public void source_cannot_be_null() {
        TicketDTO ticket = getTicketDTO();
        ticket.setSource(null);

        given()
            .body(ticket).
        when()
            .post(Consts.Paths.Ticket.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Source cannot be null!"));
    }

    @Test
    public void type_cannot_be_null() {
        TicketDTO ticket = getTicketDTO();
        ticket.setType(null);

        given()
            .body(ticket).
        when()
            .post(Consts.Paths.Ticket.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Type cannot be null!"));
    }

    @Test
    public void sourceId_cannot_be_null() {
        TicketDTO ticket = getTicketDTO();
        ticket.setSourceId(null);

        given()
            .body(ticket).
        when()
            .post(Consts.Paths.Ticket.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Unknown source id!"));
    }

    @Test
    public void description_cannot_be_null() {
        TicketDTO ticket = getTicketDTO();
        ticket.setDescription(null);

        given()
            .body(ticket).
        when()
            .post(Consts.Paths.Ticket.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Description cannot be null!"));
    }

    @Test
    public void description_length_is_out_of_range_if_less_than_5() {
        TicketDTO ticket = getTicketDTO();
        ticket.setDescription("AAAA");

        given()
            .body(ticket).
        when()
            .post(Consts.Paths.Ticket.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Description must be between 5 and 255 chars!"));
    }

    @Test
    public void description_length_is_out_of_range_if_greater_than_255() {
        TicketDTO ticket = getTicketDTO();
        ticket.setDescription(StringUtils.repeat('a', 256));

        given()
            .body(ticket).
        when()
            .post(Consts.Paths.Ticket.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Description must be between 5 and 255 chars!"));
    }

    @Test
    public void invalid_data_for_ticket() {
        given()
            .body("wrong body!").
        when()
            .post(Consts.Paths.Ticket.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.Invalid.TICKET.getStatus()));
    }

    @Test
    public void ticket_not_found_when_delete_with_a_wrong_id() {
        when()
            .delete(Consts.Paths.Ticket.BASE + "/0").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.Invalid.TICKET.getStatus()));
    }

    private TicketDTO getTicketDTO() {
        TicketDTO ticket = new TicketDTO();
        ticket.setSource(TicketSource.LINK);
        ticket.setType(TicketType.COMPLAIN);
        ticket.setDescription("Doesn't collect data, please help!");
        ticket.setSourceId(1L);
        return ticket;
    }
}
