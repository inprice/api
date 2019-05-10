package io.inprice.scrapper.api.repository;

import io.inprice.crawler.common.logging.Logger;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.NamedStatement;
import io.inprice.scrapper.api.helpers.RedisClient;
import io.inprice.scrapper.api.models.Customer;
import jodd.util.BCrypt;
import org.eclipse.jetty.http.HttpStatus;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerRepository {

    private static final Logger log = new Logger(CustomerRepository.class);

    private static final
        String SIGNUP_QUERY =
            "insert into customer " +
                "(user_type, email, password_hash, password_salt, company_name, contact_name, website, sector_id, country_id) " +
            "values " +
                "(:user_type, :email, :password_hash, :password_salt, :company_name, :contact_name, :website, :sector_id, :country_id) ";


    public Customer signup(Customer customer) {
        Customer exists = findByEmail(customer.getEmail(), false);
        if (exists != null) {
            return new Customer(HttpStatus.CONFLICT_409, "There is already a customer with the same email address!");
        }

        try (Connection con = DBUtils.getConnection();
             NamedStatement pst = new NamedStatement(con, SIGNUP_QUERY, true)) {

            pst.setString("user_type", customer.getUserType().name());
            pst.setString("email", customer.getEmail());
            pst.setString("company_name", customer.getCompanyName());
            pst.setString("contact_name", customer.getContactName());
            pst.setString("website", customer.getWebsite());
            pst.setLong("sector_id", customer.getSectorId());
            pst.setLong("country_id", customer.getCountryId());

            String salt = BCrypt.gensalt(12);

            pst.setString("password_salt", salt);
            pst.setString("password_hash", BCrypt.hashpw(customer.getPassword(), salt));

            int result = pst.executeUpdate();
            if (result != 1) {
                log.error("Something went wrong during inserting new customer!");
                log.error(customer.toString());
                return new Customer(HttpStatus.INTERNAL_SERVER_ERROR_500, "Internal server error!");
            }

            customer.setId(DBUtils.getGeneratedId(pst));
            if (customer.getId() != null) {
                customer.setToken(RedisClient.createCSRFToken());
                customer.setHttpStatus(HttpStatus.OK_200);
            } else {
                return new Customer(HttpStatus.INTERNAL_SERVER_ERROR_500, "Customer is added but could not get an id!");
            }
            customer.setPassword(null);
            customer.setPasswordSalt(null);
            customer.setPasswordHash(null);

        } catch (Exception e) {
            log.error("Error in inserting Customer", e);
            return new Customer(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
        }

        return customer;
    }

    public Customer findByEmail(String email, boolean hasToken) {
        Customer customer = findByQuery(String.format("select * from customer where email = '%s'", email));
        if (customer != null && hasToken) {
            customer.setToken(RedisClient.createCSRFToken());
            customer.setHttpStatus(HttpStatus.OK_200);
        }

        return customer;
    }

    public Customer findByApiKey(String apiKey) {
        Customer customer = findByQuery(String.format("select * from customer where password_hash = '%s'", apiKey));
        if (customer != null) {
            customer.setHttpStatus(HttpStatus.OK_200);
        }

        return customer;
    }

    private Customer findByQuery(String query) {
        Customer result = null;
        try (Connection con = DBUtils.getConnection();
             NamedStatement pst = new NamedStatement(con, query);
             ResultSet rs = pst.executeQuery()) {

            if (rs.next()) {
                result = map(rs);
            }
        } catch (Exception e) {
            log.error("Error in getting Customer");
            log.error(query);
            e.printStackTrace();
        }

        return result;
    }

    private Customer map(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setId(rs.getLong("id"));
        customer.setEmail(rs.getString("email"));
        customer.setPasswordHash(rs.getString("password_hash"));
        customer.setPasswordSalt(rs.getString("password_salt"));
        customer.setCompanyName(rs.getString("company_name"));
        customer.setContactName(rs.getString("contact_name"));
        customer.setWebsite(rs.getString("website"));
        customer.setInsertAt(rs.getDate("insert_at"));

        customer.setSectorId(rs.getLong("sector_id"));
        customer.setCountryId(rs.getLong("country_id"));

        return customer;
    }

}
