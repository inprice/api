package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.utils.CodeGenerator;
import io.inprice.scrapper.common.meta.Role;
import io.inprice.scrapper.common.models.Company;
import jodd.util.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class CompanyRepository {

    private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);

    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);
    private final CodeGenerator codeGenerator = Beans.getSingleton(CodeGenerator.class);
    private final Properties props = Beans.getSingleton(Properties.class);

    public ServiceResponse<Company> findById(Long id) {
        Company model = dbUtils.findSingle("select * from company where id="+ id, CompanyRepository::map);
        if (model != null)
            return new ServiceResponse<>(model);
        else
            return Responses.DataProblem.DB_PROBLEM;
    }

    /**
     * Three insert operations happen during a new company creation
     *  - company
     *  - workspace
     *  - admin user
     */
    public ServiceResponse insert(CompanyDTO companyDTO) {
        ServiceResponse response = new ServiceResponse<>(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

        Connection con = null;

        //company is inserted
        try {
            con = dbUtils.getTransactionalConnection();
            Long companyId = null;

            try (PreparedStatement pst =
                 con.prepareStatement("insert into company (name, website, country, sector) values (?, ?, ?, ?) ",
                     Statement.RETURN_GENERATED_KEYS)) {
                int i = 0;
                pst.setString(++i, companyDTO.getCompanyName());
                pst.setString(++i, companyDTO.getWebsite());
                pst.setString(++i, companyDTO.getCountry());
                pst.setString(++i, companyDTO.getSector());

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
                     con.prepareStatement("insert into workspace (master, name, plan_id, company_id) values (true, ?, ?, ?) ",
                         Statement.RETURN_GENERATED_KEYS)) {
                    int i = 0;
                    pst.setString(++i, "DEFAULT WORKSPACE");
                    if (props.isRunningForTests()) {
                        pst.setLong(++i, 1L);
                    } else {
                        pst.setNull(++i, Types.BIGINT);
                    }
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
                        "(role, full_name, email, password_salt, password_hash, company_id, workspace_id) " +
                        "values " +
                        "(?, ?, ?, ?, ?, ?, ?) ";

                    try (PreparedStatement pst = con.prepareStatement(q2, Statement.RETURN_GENERATED_KEYS)) {
                        int i = 0;
                        pst.setString(++i, Role.admin.name());
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
                                            response = Responses.OK;
                                        }
                                    }
                                }
                            }
                        }

                    } catch (SQLIntegrityConstraintViolationException ie) {
                        log.error("Failed to insert user!", ie);
                        response = Responses.DataProblem.INTEGRITY_PROBLEM;
                    } catch (Exception e) {
                        log.error("Failed to insert user", e);
                        response = Responses.ServerProblem.EXCEPTION;
                    }
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

    public ServiceResponse update(CompanyDTO companyDTO) {
        try (Connection con = dbUtils.getConnection();
            PreparedStatement pst =
                con.prepareStatement(
                "update company " +
                    "set name=?, website=?, country=?, sector=? " +
                    "where id=? " +
                    "  and admin_id=?")) {

            int i = 0;
            pst.setString(++i, companyDTO.getCompanyName());
            pst.setString(++i, companyDTO.getWebsite());
            pst.setString(++i, companyDTO.getCountry());
            pst.setString(++i, companyDTO.getSector());
            pst.setLong(++i, companyDTO.getId());
            pst.setLong(++i, Context.getAuthUser().getId());

            if (pst.executeUpdate() <= 0) {
                return Responses.NotFound.COMPANY;
            }

            return Responses.OK;

        } catch (SQLException e) {
            log.error("Failed to update company. " + companyDTO, e);
            return Responses.ServerProblem.EXCEPTION;
        }
    }

    private static Company map(ResultSet rs) {
        try {
            Company model = new Company();
            model.setId(rs.getLong("id"));
            model.setName(rs.getString("name"));
            model.setWebsite(rs.getString("website"));
            model.setAdminId(rs.getLong("admin_id"));
            model.setCountry(rs.getString("country"));
            model.setSector(rs.getString("sector"));
            model.setCreatedAt(rs.getDate("created_at"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set company's properties", e);
        }
        return null;
    }

}
