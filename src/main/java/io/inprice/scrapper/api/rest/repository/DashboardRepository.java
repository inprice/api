package io.inprice.scrapper.api.rest.repository;

import com.google.gson.JsonObject;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

//TODO: data must be cached via redis and expired per 5 mins at max
public class DashboardRepository {

    private static final Logger log = LoggerFactory.getLogger(DashboardRepository.class);

    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public JsonObject getReport() {
        JsonObject dashboard = emptyDashboard();
        JsonObject user = new JsonObject();
        JsonObject workspace = new JsonObject();

        try (Connection con = dbUtils.getConnection()) {
        	
        	//user
        	addUserInfo(con, user);
        	dashboard.add("user", user);

            //workspace
            addWorkspaceInfo(con, workspace);
            dashboard.add("workspace", workspace);

            if (workspace.get("lastCollectingTime") != null) {

                //product
                JsonObject product = new JsonObject();
                addProductCounts(con, product);
                addProductPositionDistributions(con, product);
                addMinMaxProductNumbersOf_YouAsTheSeller(con, product);
                addMRUTenProducts(con, product);
                dashboard.add("product", product);

                //link
                JsonObject link = new JsonObject();
                addLinkCounts(con, link);
                addLinkStatusDistributions(con, link);
                addMRUTenLinks(con, link);
                dashboard.add("link", link);
            }

        } catch (Exception e) {
            log.error("Dashboard error", e);
        }

        return dashboard;
    }

    private void addUserInfo(Connection con, JsonObject parent) throws SQLException {
        final String query =
            "select full_name, email, role " +
            "from user as u " +
            "where u.id=?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setLong(1, Context.getUserId());

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                	parent.addProperty("fullName", rs.getString(1));
                	parent.addProperty("email", rs.getString(2));
                    parent.addProperty("role", rs.getString(3));
                }
            }
        }
    }

    private void addWorkspaceInfo(Connection con, JsonObject parent) throws SQLException {
        final String query =
            "select w.last_collecting_time, w.last_collecting_status, p.name, w.name, w.due_date " +
            "from workspace as w " +
            "left join plan as p on w.plan_id = p.id " +
            "where w.id=?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setLong(1, Context.getWorkspaceId());

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Date lastCollectionTime = rs.getDate(1);

                    if (lastCollectionTime != null) {
                        parent.addProperty("lastCollectingTime", DateUtils.formatLongDateForDB(lastCollectionTime));
                        parent.addProperty("lastCollectingStatus", (rs.getBoolean(2) ? "Successful" : "Failed"));
                        parent.addProperty("planName", rs.getString(3));
                    } else {
                        parent.addProperty("lastCollectingStatus", "Waiting");
                        parent.addProperty("planName", "Please select one!");
                    }

                    parent.addProperty("name", rs.getString(4));
                    parent.addProperty("dueDate", DateUtils.formatLongDateForDB(rs.getDate(5)));
                }
            }

        }
    }

    private void addLinkCounts(Connection con, JsonObject parent) throws SQLException {
        final String query = "select count(1) from link where workspace_id=?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setLong(1, Context.getWorkspaceId());

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    parent.addProperty("counts", rs.getInt(1));
                }
            }

        }
    }

    private void addLinkStatusDistributions(Connection con, JsonObject parent) throws SQLException {
        final String query = "select status, count(1) from link where workspace_id=? group by status";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setLong(1, Context.getWorkspaceId());

            JsonObject sd = new JsonObject();
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    sd.addProperty(rs.getString(1), rs.getInt(2));
                }
            }
            if (sd.size() > 0) parent.add("statuses", sd);
        }
    }

    /**
     * MRU - Most Recently Updated
     *
     */
    private void addMRUTenLinks(Connection con, JsonObject parent) throws SQLException {
        final String query =
                "select sku, name, seller, price, status, last_update, website_class_name " +
                "from link " +
                "where workspace_id=? " +
                "order by last_update desc " +
                "limit 10";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setLong(1, Context.getWorkspaceId());

            JsonObject mruTenLinks = new JsonObject();
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int index = 2;
                    JsonObject link = new JsonObject();
                    link.addProperty("name", rs.getString(index++));
                    link.addProperty("seller", rs.getString(index++));
                    link.addProperty("price", rs.getBigDecimal(index++));
                    link.addProperty("status", rs.getString(index++));
                    link.addProperty("lastUpdate", DateUtils.formatLongDateForDB(rs.getDate(index++)));
                    link.addProperty("website", rs.getString(index));
                    mruTenLinks.add(rs.getString(1), link);
                }
            }
            if (mruTenLinks.size() > 0) parent.add("mruTen", mruTenLinks);
        }
    }

    private void addProductCounts(Connection con, JsonObject parent) throws SQLException {
        final String query = "select active, count(1) from product where workspace_id=? group by active";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setLong(1, Context.getWorkspaceId());

            JsonObject pc = new JsonObject();
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    pc.addProperty(rs.getBoolean(1) ? "active" : "passive", rs.getInt(2));
                }
            }
            if (pc.size() > 0) parent.add("counts", pc);
        }
    }

    /**
     * MRU - Most Recently Updated
     *
     */
    private void addMRUTenProducts(Connection con, JsonObject parent) throws SQLException {
        final String query =
                "select code, name, price, position, last_update, min_seller, max_seller, min_price, avg_price, max_price " +
                "from product " +
                "where workspace_id=? " +
                "order by last_update desc " +
                "limit 10";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setLong(1, Context.getWorkspaceId());

            JsonObject mruTenProducts = new JsonObject();
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int index = 2;
                    JsonObject prod = new JsonObject();
                    prod.addProperty("name", rs.getString(index++));
                    prod.addProperty("price", rs.getBigDecimal(index++));
                    prod.addProperty("position", rs.getInt(index++));
                    prod.addProperty("lastUpdate", DateUtils.formatLongDateForDB(rs.getDate(index++)));
                    prod.addProperty("minSeller", rs.getString(index++));
                    prod.addProperty("maxSeller", rs.getString(index++));
                    prod.addProperty("minPrice", rs.getBigDecimal(index++));
                    prod.addProperty("avgPrice", rs.getBigDecimal(index++));
                    prod.addProperty("maxPrice", rs.getBigDecimal(index));
                    mruTenProducts.add(rs.getString(1), prod);
                }
            }
            if (mruTenProducts.size() > 0) parent.add("mruTen", mruTenProducts);
        }
    }

    private void addProductPositionDistributions(Connection con, JsonObject parent) throws SQLException {
        final String query = "select position, count(1) from product where workspace_id=? and active=true group by position";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setLong(1, Context.getWorkspaceId());

            JsonObject pd = new JsonObject();
            for (int i = 1; i < 8; i++) {
                pd.addProperty(""+i, 0);
            }
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    pd.addProperty(""+rs.getInt(1), rs.getInt(2));
                }
            }
            if (pd.size() > 0) parent.add("positions", pd);
        }
    }

    /**
     * finding product counts which You are either the cheapest or the most expensive
     *
     */
    private void addMinMaxProductNumbersOf_YouAsTheSeller(Connection con, JsonObject parent) throws SQLException {
        JsonObject sellerCounts = new JsonObject();
        addMinMaxProductNumbersOfYou(con, sellerCounts, "min");
        addMinMaxProductNumbersOfYou(con, sellerCounts, "max");
        if (sellerCounts.size() > 0) parent.add("sellerCounts", sellerCounts);
    }

    private void addMinMaxProductNumbersOfYou(Connection con, JsonObject sellerCounts, String indicator) throws SQLException {
        final String query = "select count(1) from product where workspace_id=? and active=true and " + indicator + "_seller='You'";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setLong(1, Context.getWorkspaceId());

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    sellerCounts.addProperty(indicator, rs.getInt(1));
                }
            }
        }
    }

    private JsonObject emptyDashboard() {
        final String val =
        "{" +
            "'product': {" +
                "'counts': {" +
                    "'active': 0," +
                    "'passive': 0" +
                "}," +
                "'positions': {" +
                    "'1': 0," +
                    "'2': 0," +
                    "'3': 0," +
                    "'4': 0," +
                    "'5': 0," +
                    "'6': 0," +
                    "'7': 0" +
                "}," +
                "'sellerCounts': {" +
                    "'min': 0," +
                    "'max': 0" +
                "}," +
                "'mruTen': {}" +
            "}," +
            "'link': {" +
                "'counts': 0," +
                "'statuses': {" +
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
                "}," +
                "'mruTen': {}" +
            "}" +
        "}";

        return Global.gson.fromJson(val, JsonObject.class);
    }

}