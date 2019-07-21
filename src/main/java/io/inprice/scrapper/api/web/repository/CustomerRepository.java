package io.inprice.scrapper.api.web.repository;

import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.api.framework.Repository;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.Customer;
import jodd.util.BCrypt;

import java.sql.*;

@Repository
public class CustomerRepository {

    private static final Logger log = new Logger(CustomerRepository.class);

    public Response insert(Customer customer) {
        final String salt = BCrypt.gensalt(12);
        final String query =
            "insert into customer " +
            "(user_type, company_name, contact_name, email, website, password_hash, password_salt, country_id) " +
            "values " +
            "(?, ?, ?, ?, ?, ?, ?, ?) ";

        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int i = 0;
            pst.setString(++i, customer.getUserType().name());
            pst.setString(++i, customer.getCompanyName());
            pst.setString(++i, customer.getContactName());
            pst.setString(++i, customer.getEmail());
            pst.setString(++i, customer.getWebsite());
            pst.setString(++i, salt);
            pst.setString(++i, BCrypt.hashpw(customer.getPassword(), salt));
            pst.setLong(++i, customer.getCountryId());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.CRUD_ERROR;

        } catch (SQLIntegrityConstraintViolationException ie) {
            log.error("Failed to insert customer: " + ie.getMessage());
            return Responses.SERVER_ERROR;
        } catch (Exception e) {
            log.error("Failed to insert customer", e);
            return Responses.SERVER_ERROR;
        }
    }

    public Response update(Customer customer) {
        final String query =
            "update customer " +
            "set user_type=?, company_name=?, contact_name=?, website=?, country_id=? " +
            "where email=?";

        try (Connection con = DBUtils.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            int i = 0;
            pst.setString(++i, customer.getUserType().name());
            pst.setString(++i, customer.getCompanyName());
            pst.setString(++i, customer.getContactName());
            pst.setString(++i, customer.getWebsite());
            pst.setLong(++i, customer.getCountryId());
            pst.setString(++i, customer.getEmail());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.CRUD_ERROR;

        } catch (Exception e) {
            log.error("Failed to update customer", e);
            return Responses.SERVER_ERROR;
        }
    }

    public Response findById(Long id, boolean passwordFields) {
        Customer model = DBUtils.findSingle(String.format("select * from customer where id = %d", id), this::map);
        if (model != null) {
            if (! passwordFields) {
                model.setPasswordSalt(null);
                model.setPasswordHash(null);
            }
            return new Response(model);
        } else {
            return Responses.NOT_FOUND("Customer");
        }
    }

    public Response findByEmail(String email, boolean passwordFields) {
        Customer model = DBUtils.findSingle(String.format("select * from customer where email = '%s'", email), this::map);
        if (model != null) {
            if (! passwordFields) {
                model.setPasswordSalt(null);
                model.setPasswordHash(null);
            }
            return new Response(model);
        } else {
            return Responses.NOT_FOUND("Customer");
        }
    }

    private Customer map(ResultSet rs) {
        try {
            Customer customer = new Customer();
            customer.setId(rs.getLong("id"));
            customer.setUserType(UserType.valueOf(rs.getString("user_type")));
            customer.setCompanyName(rs.getString("company_name"));
            customer.setContactName(rs.getString("contact_name"));
            customer.setEmail(rs.getString("email"));
            customer.setWebsite(rs.getString("website"));
            customer.setPasswordHash(rs.getString("password_hash"));
            customer.setPasswordSalt(rs.getString("password_salt"));
            customer.setCountryId(rs.getLong("country_id"));
            customer.setInsertAt(rs.getDate("insert_at"));
            return customer;
        } catch (SQLException e) {
            log.error("Failed to set user's properties", e);
        }
        return null;
    }

}
