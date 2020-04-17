package io.inprice.scrapper.api.app.dashboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.consts.Global;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.session.CurrentUser;

//TODO: data must be cached via redis and expired per 5 mins at max
public class DashboardRepository {

   private static final Logger log = LoggerFactory.getLogger(DashboardRepository.class);

   private final Database db = Beans.getSingleton(Database.class);

   public JSONObject getReport() {
      JSONObject dashboard = emptyDashboard();
      JSONObject user = new JSONObject();
      JSONObject company = new JSONObject();

      try (Connection con = db.getConnection()) {

         // user
         addSession(con, user);
         dashboard.put("user", user);

         // company
         addCompanyInfo(con, company);
         dashboard.put("company", company);

         if (company.get("lastCollectingTime") != null) {

            // product
            JSONObject product = new JSONObject();
            addProductCounts(con, product);
            addProductPositionDistributions(con, product);
            addMinMaxProductNumbersOf_YouAsTheSeller(con, product);
            addMRUTenProducts(con, product);
            dashboard.put("product", product);

            // link
            JSONObject link = new JSONObject();
            addLinkCounts(con, link);
            addLinkStatusDistributions(con, link);
            addMRUTenLinks(con, link);
            dashboard.put("link", link);
         }

      } catch (Exception e) {
         log.error("Dashboard error", e);
      }

      return dashboard;
   }

   private void addSession(Connection con, JSONObject parent) throws SQLException {
      final String query = "select name, email from user as u where u.id=?";
      try (PreparedStatement pst = con.prepareStatement(query)) {
         pst.setLong(1, CurrentUser.getUserId());

         try (ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
               parent.put("name", rs.getString(1));
               parent.put("email", rs.getString(2));
            }
         }
      }
   }

   private void addCompanyInfo(Connection con, JSONObject parent) throws SQLException {
      final String query = "select c.last_collecting_time, c.last_collecting_status, p.name, c.name, c.due_date "
            + "from company as c left join plan as p on c.plan_id = p.id where w.id=?";
      try (PreparedStatement pst = con.prepareStatement(query)) {
         pst.setLong(1, CurrentUser.getCompanyId());

         try (ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
               Date lastCollectionTime = rs.getTimestamp(1);

               if (lastCollectionTime != null) {
                  parent.put("lastCollectingTime", lastCollectionTime.toString());
                  parent.put("lastCollectingStatus", (rs.getBoolean(2) ? "Successful" : "Failed"));
                  parent.put("planName", rs.getString(3));
               } else {
                  parent.put("lastCollectingStatus", "Waiting");
                  parent.put("planName", "Please select one!");
               }

               parent.put("name", rs.getString(4));
               parent.put("dueDate", rs.getTimestamp(5).toString());
            }
         }

      }
   }

   private void addLinkCounts(Connection con, JSONObject parent) throws SQLException {
      final String query = "select count(1) from link where company_id=?";
      try (PreparedStatement pst = con.prepareStatement(query)) {
         pst.setLong(1, CurrentUser.getCompanyId());

         try (ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
               parent.put("counts", rs.getInt(1));
            }
         }

      }
   }

   private void addLinkStatusDistributions(Connection con, JSONObject parent) throws SQLException {
      final String query = "select status, count(1) from link where company_id=? group by status";
      try (PreparedStatement pst = con.prepareStatement(query)) {
         pst.setLong(1, CurrentUser.getCompanyId());

         JSONObject sd = new JSONObject();
         try (ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
               sd.put(rs.getString(1), rs.getInt(2));
            }
         }
         if (sd.length() > 0)
            parent.put("statuses", sd);
      }
   }

   /**
    * MRU - Most Recently Updated
    *
    */
   private void addMRUTenLinks(Connection con, JSONObject parent) throws SQLException {
      final String query = "select sku, name, seller, price, status, last_update, website_class_name from link "
            + "where company_id=? order by last_update desc limit 10";
      try (PreparedStatement pst = con.prepareStatement(query)) {
         pst.setLong(1, CurrentUser.getCompanyId());

         JSONObject mruTenLinks = new JSONObject();
         try (ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
               int index = 2;
               JSONObject link = new JSONObject();
               link.put("name", rs.getString(index++));
               link.put("seller", rs.getString(index++));
               link.put("price", rs.getBigDecimal(index++));
               link.put("status", rs.getString(index++));
               link.put("lastUpdate", rs.getTimestamp(index++).toString());
               link.put("website", rs.getString(index));
               mruTenLinks.put(rs.getString(1), link);
            }
         }
         if (mruTenLinks.length() > 0)
            parent.put("mruTen", mruTenLinks);
      }
   }

   private void addProductCounts(Connection con, JSONObject parent) throws SQLException {
      final String query = "select active, count(1) from product where company_id=? group by active";
      try (PreparedStatement pst = con.prepareStatement(query)) {
         pst.setLong(1, CurrentUser.getCompanyId());

         JSONObject pc = new JSONObject();
         try (ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
               pc.put(rs.getBoolean(1) ? "active" : "passive", rs.getInt(2));
            }
         }
         if (pc.length() > 0)
            parent.put("counts", pc);
      }
   }

   /**
    * MRU - Most Recently Updated
    *
    */
   private void addMRUTenProducts(Connection con, JSONObject parent) throws SQLException {
      final String query = "select code, name, position, price, avg_price, min_platform, min_seller, min_price "
            + ", max_platform, max_seller, max_price, updated_at "
            + "from product where company_id=? order by last_update desc limit 10";
      try (PreparedStatement pst = con.prepareStatement(query)) {
         pst.setLong(1, CurrentUser.getCompanyId());

         JSONObject mruTenProducts = new JSONObject();
         try (ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
               int index = 2;
               JSONObject prod = new JSONObject();
               prod.put("name", rs.getString(index++));
               prod.put("price", rs.getBigDecimal(index++));
               prod.put("position", rs.getInt(index++));
               prod.put("lastUpdate", rs.getTimestamp(index++).toString());
               prod.put("minSeller", rs.getString(index++));
               prod.put("maxSeller", rs.getString(index++));
               prod.put("minPrice", rs.getBigDecimal(index++));
               prod.put("avgPrice", rs.getBigDecimal(index++));
               prod.put("maxPrice", rs.getBigDecimal(index));
               mruTenProducts.put(rs.getString(1), prod);
            }
         }
         if (mruTenProducts.length() > 0)
            parent.put("mruTen", mruTenProducts);
      }
   }

   private void addProductPositionDistributions(Connection con, JSONObject parent) throws SQLException {
      final String query = "select position, count(1) from product where company_id=? and active=true group by position";
      try (PreparedStatement pst = con.prepareStatement(query)) {
         pst.setLong(1, CurrentUser.getCompanyId());

         JSONObject pd = new JSONObject();
         for (int i = 1; i < 8; i++) {
            pd.put("" + i, 0);
         }
         try (ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
               pd.put("" + rs.getInt(1), rs.getInt(2));
            }
         }
         if (pd.length() > 0)
            parent.put("positions", pd);
      }
   }

   /**
    * finding product counts which You are either the cheapest or the most
    * expensive
    *
    */
   private void addMinMaxProductNumbersOf_YouAsTheSeller(Connection con, JSONObject parent) throws SQLException {
      JSONObject sellerCounts = new JSONObject();
      addMinMaxProductNumbersOfYou(con, sellerCounts, "min");
      addMinMaxProductNumbersOfYou(con, sellerCounts, "max");
      if (sellerCounts.length() > 0)
         parent.put("sellerCounts", sellerCounts);
   }

   private void addMinMaxProductNumbersOfYou(Connection con, JSONObject sellerCounts, String indicator)
         throws SQLException {
      final String query = "select count(1) from product where company_id=? and active=true and " + indicator + "_seller='You'";
      try (PreparedStatement pst = con.prepareStatement(query)) {
         pst.setLong(1, CurrentUser.getCompanyId());

         try (ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
               sellerCounts.put(indicator, rs.getInt(1));
            }
         }
      }
   }

   private JSONObject emptyDashboard() {
      final String val = "{" + 
         "'product': {" + 
               "'counts': {" + 
                  "'active': 0," + 
                  "'passive': 0" + 
               "},"
            + "'positions': {" + 
                  "'1': 0," + 
                  "'2': 0," + 
                  "'3': 0," + 
                  "'4': 0," + 
                  "'5': 0" + 
               "},"
            + "'sellerCounts': {" + 
                  "'min': 0," + 
                  "'max': 0" + 
               "}," + 
               "'mruTen': {}" + 
            "}," + 
         "'link': {"
            + "'counts': 0," 
            + "'statuses': {" + 
                  "'NEW': 0," + 
                  "'AVAILABLE': 0," + 
                  "'RENEWED': 0," + 
                  "'BE_IMPLEMENTED': 0," + 
                  "'IMPLEMENTED': 0," + 
                  "'DUPLICATE': 0," + 
                  "'WONT_BE_IMPLEMENTED': 0," + 
                  "'IMPROPER': 0," + 
                  "'NOT_A_PRODUCT_PAGE': 0," + 
                  "'NO_DATA': 0," + 
                  "'NOT_AVAILABLE': 0," + 
                  "'READ_ERROR': 0," + 
                  "'SOCKET_ERROR': 0," + 
                  "'NETWORK_ERROR': 0," + 
                  "'CLASS_PROBLEM': 0," + 
                  "'INTERNAL_ERROR': 0," + 
                  "'PAUSED': 0," + 
                  "'RESUMED': 0" + 
               "}," 
            + "'mruTen': {}" + 
            "}" + 
         "}";

      return Global.fromJson(val, JSONObject.class);
   }

}
