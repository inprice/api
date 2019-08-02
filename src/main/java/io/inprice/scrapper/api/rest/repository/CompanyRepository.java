package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.api.utils.CodeGenerator;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.Company;
import jodd.util.BCrypt;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class CompanyRepository {

    private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);

    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);
    private final CodeGenerator codeGenerator = Beans.getSingleton(CodeGenerator.class);

    public Response findById(Long id) {
        Response response = Responses.CRUD_ERROR;

        Company model = dbUtils.findSingle("select * from company where id="+ id, CompanyRepository::map);
        if (model != null) {
            response.setStatus(HttpStatus.OK_200);
            response.setModel(model);
        }

        return response;
    }

    public Response insert(CompanyDTO companyDTO) {
        Response response = Responses.CRUD_ERROR;

        Connection con = null;
        try {
            con = dbUtils.getTransactionalConnection();

            Long companyId = null;

            //company is inserted
            try (PreparedStatement pst =
                     con.prepareStatement("insert into company (name, website, country_id) values (?, ?, ?) ",
                             Statement.RETURN_GENERATED_KEYS)) {
                int i = 0;
                pst.setString(++i, companyDTO.getCompanyName());
                pst.setString(++i, companyDTO.getWebsite());
                pst.setLong(++i, companyDTO.getCountryId());

                if (pst.executeUpdate() > 0) {
                    try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            companyId = generatedKeys.getLong(1);
                        }
                    }
                }
            }

            if (companyId != null) {
                final String salt = codeGenerator.generateSalt();
                final String q2 =
                        "insert into user " +
                        "(user_type, name, email, password_salt, password_hash, company_id) " +
                        "values " +
                        "(?, ?, ?, ?, ?, ?) ";

                //owner is inserted
                try (PreparedStatement pst = con.prepareStatement(q2, Statement.RETURN_GENERATED_KEYS)) {
                    int i = 0;
                    pst.setString(++i, UserType.OWNER.name());
                    pst.setString(++i, companyDTO.getFullName());
                    pst.setString(++i, companyDTO.getEmail());
                    pst.setString(++i, salt);
                    pst.setString(++i, BCrypt.hashpw(companyDTO.getPassword(), salt));
                    pst.setLong(++i, companyId);

                    if (pst.executeUpdate() > 0) {
                        try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                long ownerId = generatedKeys.getLong(1);

                                //company's owner is set
                                try (PreparedStatement subPst = con.prepareStatement("update company set owner_id=? where id=? ")) {
                                    subPst.setLong(1, ownerId);
                                    subPst.setLong(2, companyId);
                                    if (subPst.executeUpdate() > 0) {
                                        response = Responses.OK;
                                    }
                                }
                            }
                        }
                    }

                } catch (SQLIntegrityConstraintViolationException ie) {
                    log.error("Failed to insert user!", ie);
                    response = Responses.SERVER_ERROR;
                } catch (Exception e) {
                    log.error("Failed to insert user", e);
                    response = Responses.SERVER_ERROR;
                }

            }

            if (Responses.OK.equals(response)) {
                dbUtils.commit(con);
            } else {
                dbUtils.rollback(con);
            }

        } catch (SQLException e) {
            if (con != null) dbUtils.rollback(con);
            log.error("Failed to insert a new company. " + companyDTO, e);
        } finally {
            if (con != null) dbUtils.close(con);
        }

        return response;
    }

    public Response update(CompanyDTO companyDTO) {
        Response response;

        try (Connection con = dbUtils.getConnection();
            PreparedStatement pst = con.prepareStatement("update company set name=?, website=?, country_id=? where id=?")) {

            int i = 0;
            pst.setString(++i, companyDTO.getCompanyName());
            pst.setString(++i, companyDTO.getWebsite());
            pst.setLong(++i, companyDTO.getCountryId());
            pst.setLong(++i, companyDTO.getId());

            if (pst.executeUpdate() > 0) {
                response = Responses.OK;
            } else {
                response = Responses.NOT_FOUND("Company");
            }

        } catch (SQLException e) {
            log.error("Failed to update company. " + companyDTO, e);
            response = Responses.SERVER_ERROR;
        }

        return response;
    }

    private static Company map(ResultSet rs) {
        try {
            Company model = new Company();
            model.setId(rs.getLong("id"));
            model.setName(rs.getString("name"));
            model.setWebsite(rs.getString("website"));
            model.setOwnerId(rs.getLong("owner_id"));
            model.setCountryId(rs.getLong("country_id"));
            model.setInsertAt(rs.getDate("insert_at"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set company's properties", e);
        }
        return null;
    }

}
