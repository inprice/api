package io.inprice.scrapper.api.app.invitation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.app.user.UserStatus;
import io.inprice.scrapper.api.app.user_company.UserCompany;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.InvitationAcceptDTO;
import io.inprice.scrapper.api.dto.InvitationDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.RepositoryHelper;
import io.inprice.scrapper.api.helpers.SqlHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;

public class InvitationRepository {

   private static final Logger log = LoggerFactory.getLogger(InvitationRepository.class);

   private final Database db = Beans.getSingleton(Database.class);
   private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);

   public ServiceResponse findById(long userCompanyId) {
      UserCompany model = db.findSingle(String.format("select * from user_company where id=%d", userCompanyId), this::map);

      if (model != null) {
         return new ServiceResponse(model);
      } else {
         return Responses.NotFound.INVITATION;
      }
   }

   public ServiceResponse findByEmailAndCompanyId(String email, long companyId) {
      UserCompany model = db
            .findSingle(String.format("select * from user_company where email='%s' and company_id=%d",
                  SqlHelper.clear(email), companyId), this::map);
      if (model != null) {
         return new ServiceResponse(model);
      } else {
         return Responses.NotFound.INVITATION;
      }
   }

   public ServiceResponse invite(InvitationDTO dto) {
      ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      // member is inserted
      try (Connection con = db.getConnection();
            PreparedStatement pst = con
                  .prepareStatement("insert into user_company (email, role, company_id) values (?, ?, ?) ")) {
         int i = 0;
         pst.setString(++i, dto.getEmail());
         pst.setString(++i, dto.getRole().name());
         pst.setLong(++i, CurrentUser.getCompanyId());

         if (pst.executeUpdate() > 0) {
            res = Responses.OK;
         }
      } catch (SQLException e) {
         log.error("Failed to insert a new member. " + dto, e);
      }

      return res;
   }

   public ServiceResponse acceptNewUser(InvitationAcceptDTO acceptDTO, InvitationDTO invitationDTO) {
      ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      Connection con = null;

      try {
         con = db.getTransactionalConnection();

         UserCompany userCompany = 
            db.findSingle(
               con,
               String.format("select * from user_company where email='%s' and company_id=%d",
               invitationDTO.getEmail(), invitationDTO.getCompanyId()), this::map);

         res = checkUserCompany(userCompany);
         if (res.isOK()) {

            try (PreparedStatement pst = con.prepareStatement("update user_company set status=? where email=? and company_id=?")) {
               int i = 0;
               pst.setString(++i, UserStatus.JOINED.name());
               pst.setString(++i, invitationDTO.getEmail());
               pst.setLong(++i, invitationDTO.getCompanyId());
      
               if (pst.executeUpdate() > 0) {

                  res = userRepository.findByEmail(con, invitationDTO.getEmail());
                  if (Responses.NotFound.USER.equals(res)) {
                     UserDTO dto = new UserDTO();
                     dto.setName(acceptDTO.getName());
                     dto.setEmail(invitationDTO.getEmail());
                     dto.setPassword(acceptDTO.getPassword());
                     dto.setCompanyId(invitationDTO.getCompanyId());
                     res = userRepository.insert(con, dto);
                  } else {
                     res = Responses.OK;
                  }

                  if (Responses.OK.equals(res)) {
                     db.commit(con);
                  } else {
                     db.rollback(con);
                  }
               }
            }
         }

      } catch (SQLException e) {
         if (con != null) {
            db.rollback(con);
         }
         log.error("Failed to accept a new member. " + invitationDTO, e);
      } finally {
         if (con != null) {
            db.close(con);
         }
      }

      return res;
   }

   public ServiceResponse handleExisting(Long userCompanyId, boolean isAcceptance) {
      ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      try (Connection con = db.getConnection()) {

         UserCompany userCompany = 
            db.findSingle(
               con,
               String.format("select * from user_company where id=%d and email='%s'", userCompanyId, CurrentUser.getEmail()), this::map);

         res = checkUserCompany(userCompany);
         if (res.isOK()) {

            try (PreparedStatement pst = con.prepareStatement("update user_company set status=? where id=?")) {
               int i = 0;
               if (isAcceptance) {
                  pst.setString(++i, UserStatus.JOINED.name());
               } else {
                  pst.setString(++i, UserStatus.LEFT.name());
               }
               pst.setLong(++i, userCompanyId);
      
               if (pst.executeUpdate() > 0) {
                  res = Responses.OK;
               }
            }
         }

      } catch (SQLException e) {
         log.error("Failed to " + (isAcceptance ? " accept " : " reject ") + " an existing member. UserCompany Id: " + userCompanyId, e);
      }

      return res;
   }

   public ServiceResponse increaseSendingCount(long userCompanyId) {
      ServiceResponse res = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      // company is inserted
      try (Connection con = db.getConnection();
            PreparedStatement pst = con.prepareStatement(
                  "update user_company set retry = retry + 1 where active = true and id = ? and retry < 3 and status = ? and company_id = ?")) {
         int i = 0;
         pst.setLong(++i, userCompanyId);
         pst.setString(++i, UserStatus.PENDING.name());
         pst.setLong(++i, CurrentUser.getCompanyId());

         if (pst.executeUpdate() > 0) {
            res = Responses.OK;
         }
      } catch (SQLException e) {
         log.error("Failed to increase retry count. UserCompany Id: " + userCompanyId, e);
      }

      return res;
   }

   private ServiceResponse checkUserCompany(UserCompany userCompany) {
      ServiceResponse res = Responses.OK;
      if (userCompany != null) {
         if (userCompany.getActive() && UserStatus.PENDING.equals(userCompany.getStatus())) {
            res = Responses.OK;
         } else {
            res = Responses.NotActive.INVITATION;
         }
      } else {
         res = Responses.NotFound.INVITATION;
      }
      return res;
   }

   private UserCompany map(ResultSet rs) {
      try {
         UserCompany model = new UserCompany();
         model.setId(RepositoryHelper.nullLongHandler(rs, "id"));
         model.setActive(rs.getBoolean("active"));
         model.setEmail(rs.getString("email"));
         model.setCompanyId(RepositoryHelper.nullLongHandler(rs, "company_id"));
         model.setRole(UserRole.valueOf(rs.getString("role")));
         model.setStatus(UserStatus.valueOf(rs.getString("status")));
         model.setRetry(rs.getInt("retry"));
         model.setCreatedAt(rs.getTimestamp("created_at"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set member's properties", e);
      }
      return null;
   }

}
