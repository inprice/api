package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.info.InstantResponses;
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

    public ServiceResponse<Company> findById(Long id) {
        ServiceResponse<Company> response = InstantResponses.CRUD_ERROR;

        Company model = dbUtils.findSingle("select * from company where id="+ id, CompanyRepository::map);
        if (model != null) {
            response.setStatus(HttpStatus.OK_200);
            response.setModel(model);
        }

        return response;
    }

    /**
     * When a company is created three insert operations happen
     *  - company
     *  - workspace
     *  - and admin
     *
     */
    public ServiceResponse insert(CompanyDTO companyDTO) {
        ServiceResponse response = InstantResponses.CRUD_ERROR;

        Connection con = null;

        //company is inserted
        try {
            con = dbUtils.getTransactionalConnection();
            Long companyId = null;

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

            //workspace is inserted
            if (companyId != null) {

                Long workspaceId = null;
                try (PreparedStatement pst =
                         con.prepareStatement("insert into workspace (name, company_id) values (?, ?) ",
                             Statement.RETURN_GENERATED_KEYS)) {
                    int i = 0;
                    pst.setString(++i, "DEFAULT WORKSPACE");
                    pst.setLong(++i, companyId);

                    if (pst.executeUpdate() > 0) {
                        try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                workspaceId = generatedKeys.getLong(1);
                            }
                        }
                    }
                }

                //admin is inserted
                if (workspaceId != null) {

                    final String salt = codeGenerator.generateSalt();
                    final String q2 =
                            "insert into user " +
                                "(user_type, full_name, email, password_salt, password_hash, company_id, default_workspace_id) " +
                                "values " +
                                "(?, ?, ?, ?, ?, ?, ?) ";

                    try (PreparedStatement pst = con.prepareStatement(q2, Statement.RETURN_GENERATED_KEYS)) {
                        int i = 0;
                        pst.setString(++i, UserType.ADMIN.name());
                        pst.setString(++i, companyDTO.getFullName());
                        pst.setString(++i, companyDTO.getEmail());
                        pst.setString(++i, salt);
                        pst.setString(++i, BCrypt.hashpw(companyDTO.getPassword(), salt));
                        pst.setLong(++i, companyId);
                        pst.setLong(++i, workspaceId);

                        if (pst.executeUpdate() > 0) {
                            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                                if (generatedKeys.next()) {
                                    long adminId = generatedKeys.getLong(1);

                                    //company's admin is set
                                    try (PreparedStatement subPst = con.prepareStatement("update company set admin_id=? where id=? ")) {
                                        subPst.setLong(1, adminId);
                                        subPst.setLong(2, companyId);
                                        if (subPst.executeUpdate() > 0) {
                                            response = InstantResponses.OK;
                                        }
                                    }
                                }
                            }
                        }

                    } catch (SQLIntegrityConstraintViolationException ie) {
                        log.error("Failed to insert user!", ie);
                        response = InstantResponses.SERVER_ERROR;
                    } catch (Exception e) {
                        log.error("Failed to insert user", e);
                        response = InstantResponses.SERVER_ERROR;
                    }
                }
            }

            if (InstantResponses.OK.equals(response)) {
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

    public ServiceResponse update(AuthUser authUser, CompanyDTO companyDTO) {
        ServiceResponse response;

        try (Connection con = dbUtils.getConnection();
            PreparedStatement pst =
                con.prepareStatement(
                    "update company " +
                        "set name=?, website=?, country_id=? " +
                        "where id=? " +
                        "  and admin_id=?")) {

            int i = 0;
            pst.setString(++i, companyDTO.getCompanyName());
            pst.setString(++i, companyDTO.getWebsite());
            pst.setLong(++i, companyDTO.getCountryId());
            pst.setLong(++i, companyDTO.getId());
            pst.setLong(++i, authUser.getId());

            if (pst.executeUpdate() > 0) {
                response = InstantResponses.OK;
            } else {
                response = InstantResponses.NOT_FOUND("Company");
            }

        } catch (SQLException e) {
            log.error("Failed to update company. " + companyDTO, e);
            response = InstantResponses.SERVER_ERROR;
        }

        return response;
    }

    private static Company map(ResultSet rs) {
        try {
            Company model = new Company();
            model.setId(rs.getLong("id"));
            model.setName(rs.getString("name"));
            model.setWebsite(rs.getString("website"));
            model.setAdminId(rs.getLong("admin_id"));
            model.setCountryId(rs.getLong("country_id"));
            model.setInsertAt(rs.getDate("insert_at"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set company's properties", e);
        }
        return null;
    }

}
