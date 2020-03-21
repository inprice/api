package io.inprice.scrapper.api.helpers;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.Application;
import io.inprice.scrapper.api.config.Props;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.common.meta.Role;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;

public class TestHelper {

   private static final Logger log = LoggerFactory.getLogger(TestHelper.class);

   private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

   public static void setup(boolean defaultCompanyAndUser, boolean extraUser) {
      if (Global.isApplicationRunning) {
         dbUtils.reset();
      } else {
         Application.main(null);
      }

      RestAssured.port = Props.getAPP_Port();

      if (defaultCompanyAndUser) {
         // insert a default company
         given().body(TestHelper.getCompanyDTO()).when().post(Consts.Paths.Auth.REGISTER).then()
               .statusCode(HttpStatus.OK_200).assertThat();

         loginAsAdmin();

         // insert a user to use him
         if (extraUser) {
            given().body(TestHelper.getUserDTO()).when().post(Consts.Paths.AdminUser.BASE).then()
                  .statusCode(HttpStatus.OK_200).assertThat();
         }
      }
   }

   public static void runScript(String fileName) {
      try {
         InputStream in = TestHelper.class.getClassLoader().getResourceAsStream(fileName);
         if (in != null) {
            Connection con = dbUtils.getConnection();
            RunScript.execute(con, new InputStreamReader(in));
            con.close();
         } else {
            log.error("Failed to run " + fileName + " script file!");
         }
      } catch (SQLException e) {
         log.error("Failed to run " + fileName + " script file!", e);
      }
   }

   public static LoginDTO getLoginDTO() {
      LoginDTO login = new LoginDTO();
      login.setEmail("sample@inprice.io");
      login.setPassword("p4ssw0rd");
      return login;
   }

   public static UserDTO getUserDTO() {
      UserDTO user = new UserDTO();
      user.setId(2L);
      user.setRole(Role.READER);
      user.setFullName("John Doe");
      user.setEmail("jdoe@inprice.io");
      user.setPassword("p4ssw0rd");
      user.setRepeatPassword("p4ssw0rd");
      return user;
   }

   public static ProductDTO getProductDTO() {
      ProductDTO product = new ProductDTO();
      product.setCode("AX-123");
      product.setName("120x150 Dinner Table");
      product.setBrand("Wooden");
      product.setPrice(BigDecimal.valueOf(420.00));
      return product;
   }

   public static LinkDTO getLinkDTO(Long prodId) {
      LinkDTO link = new LinkDTO(prodId);
      link.setUrl("https://www.amazon.com/dp/1234567890");
      return link;
   }

   public static WorkspaceDTO getWorkspaceDTO() {
      WorkspaceDTO ws = new WorkspaceDTO();
      ws.setName("SECONDARY WS");
      return ws;
   }

   public static CompanyDTO getCompanyDTO() {
      CompanyDTO company = new CompanyDTO();
      company.setCompanyName("inprice");
      company.setWebsite("www.inprice.io");
      company.setFullName("John Doe");

      LoginDTO loginDTO = getLoginDTO();
      company.setEmail(loginDTO.getEmail());
      company.setPassword(loginDTO.getPassword());
      company.setRepeatPassword(loginDTO.getPassword());

      company.setCountry("United States");
      company.setSector("Music");
      return company;
   }

   public static void loginAsAdmin() {
      RestAssured.requestSpecification = null;

      // dont forget, the id 1 is reserved for the admin only during testing
      Response res = given().body(TestHelper.getLoginDTO()).when().post(Consts.Paths.Auth.LOGIN).then()
            .statusCode(HttpStatus.OK_200).assertThat().extract().response();

      RestAssured.requestSpecification = new RequestSpecBuilder()
            .addHeader(Consts.Auth.AUTHORIZATION_HEADER, res.header(Consts.Auth.AUTHORIZATION_HEADER)).build();
   }

   public static void loginAsUser() {
      RestAssured.requestSpecification = null;

      // dont forget, the id 1 is reserved for the admin only during testing
      Response res = given().body(TestHelper.getUserDTO()).when().post(Consts.Paths.Auth.LOGIN).then()
            .statusCode(HttpStatus.OK_200).assertThat().extract().response();

      RestAssured.requestSpecification = new RequestSpecBuilder()
            .addHeader(Consts.Auth.AUTHORIZATION_HEADER, res.header(Consts.Auth.AUTHORIZATION_HEADER)).build();
   }

   public static void logout() {
      when().post(Consts.Paths.Auth.LOGOUT).then().statusCode(HttpStatus.OK_200).assertThat();
   }

   public static File loadFileFromResources(String fileName) {
      try {
         InputStream in = TestHelper.class.getClassLoader().getResourceAsStream("files/import/" + fileName);
         final File tempFile = File.createTempFile(fileName, "");
         tempFile.deleteOnExit();
         try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
         }
         return tempFile;
      } catch (IOException e) {
         log.error("Failed to load " + fileName, e);
      }
      return null;
   }

}
