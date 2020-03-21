package io.inprice.scrapper.api.app.member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.app.user.UserStatus;
import io.inprice.scrapper.api.component.UserInfo;
import io.inprice.scrapper.api.dto.MemberChangeStatusDTO;
import io.inprice.scrapper.api.dto.MemberDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;

public class MemberRepository {

   private static final Logger log = LoggerFactory.getLogger(MemberRepository.class);

   private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

   public ServiceResponse findById(long id) {
      Member model = dbUtils.findSingle("select * from member where id=" + id, this::map);
      if (model != null)
         return new ServiceResponse(model);
      else
         return Responses.NotFound.MEMBER;
   }

   public ServiceResponse findByEmail(String email) {
      Member model = dbUtils.findSingle("select * from member where email='" + email + "'", this::map);
      if (model != null)
         return new ServiceResponse(model);
      else
         return Responses.NotFound.MEMBER;
   }

   public ServiceResponse getList() {
      List<Member> members = dbUtils
            .findMultiple(String.format("select * from member where company_id = %d order by name created_at desc",
                  UserInfo.getCompanyId()), this::map);

      // since there must be at least admin in the member list
      return new ServiceResponse(members);
   }

   public ServiceResponse findASuitableCompanyId(String email) {
      List<Member> members = dbUtils.findMultiple(String
            .format("select * from member where email = %s and status = %s order by role, company_id", email, UserStatus.JOINED),
            this::map);

      if (members != null && members.size() > 0) {
         Member member = members.get(0);
         return new ServiceResponse(member.getCompanyId());
      }
      return Responses.NotFound.COMPANY;
   }

   public ServiceResponse findPreviousStatusById(long memberId) {
      List<MemberHistory> members = dbUtils.findMultiple(String
            .format("select * from member_history where id = %d and company_id = %d", memberId, UserInfo.getCompanyId()),
            this::mapHistory);

      if (members != null && members.size() > 0) {
         Member member = members.get(0);
         return new ServiceResponse(member.getCompanyId());
      }
      return Responses.NotFound.COMPANY;
   }

   public ServiceResponse insert(MemberDTO memberDTO) {
      ServiceResponse response = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      // company is inserted
      try (Connection con = dbUtils.getConnection();
            PreparedStatement pst = con
                  .prepareStatement("insert into member (email, role, company_id) values (?, ?, ?) ")) {
         int i = 0;
         pst.setString(++i, memberDTO.getEmail());
         pst.setString(++i, memberDTO.getRole().name());
         pst.setLong(++i, UserInfo.getCompanyId());

         if (pst.executeUpdate() > 0) {
            response = Responses.OK;
         }
      } catch (SQLException e) {
         log.error("Failed to insert a new member. " + memberDTO, e);
      }

      return response;
   }

   public ServiceResponse changeStatus(MemberChangeStatusDTO changeStatusDTO) {
      ServiceResponse response = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      Connection con = null;

      // company is inserted
      try {
         con = dbUtils.getTransactionalConnection();

         if (changeStatusDTO.isPaused()) {
            ServiceResponse found = findById(ch)
         }

         try (PreparedStatement pstUpdate = con
               .prepareStatement("update member set status = ? where id = ? and company_id = ?")) {
            int i = 0;
            pstUpdate.setLong(++i, changeStatusDTO.getId());
            pstUpdate.setString(++i, changeStatusDTO.getStatus().name());
            pstUpdate.setLong(++i, UserInfo.getCompanyId());

            if (pstUpdate.executeUpdate() > 0) {
               try (PreparedStatement pstInsert = con
                     .prepareStatement("insert into member_history (member_id, company_id, status) values (?, ?, ?)")) {
                  int j = 0;
                  pstInsert.setLong(++j, changeStatusDTO.getId());
                  pstInsert.setLong(++j, UserInfo.getCompanyId());
                  pstInsert.setString(++j, changeStatusDTO.getStatus().name());

                  if (pstInsert.executeUpdate() > 0) {
                     response = Responses.OK;
                     dbUtils.commit(con);
                  } else {
                     dbUtils.rollback(con);
                  }
               }
            }
         }
      } catch (SQLException e) {
         if (con != null) {
            dbUtils.rollback(con);
         }
         log.error("Failed to change status of a member. " + changeStatusDTO, e);
      } finally {
         if (con != null) {
            dbUtils.close(con);
         }
      }

      return response;
   }

   public ServiceResponse increaseSendingCount(long memberId) {
      ServiceResponse response = new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!");

      // company is inserted
      try (Connection con = dbUtils.getConnection();
            PreparedStatement pst = con.prepareStatement(
                  "update member set retry = retry + 1 where id = ? and retry < 3 and status = ? and company_id = ?")) {
         int i = 0;
         pst.setLong(++i, memberId);
         pst.setString(++i, MemberStatus.PENDING.name());
         pst.setLong(++i, UserInfo.getCompanyId());

         if (pst.executeUpdate() > 0) {
            response = Responses.OK;
         }
      } catch (SQLException e) {
         log.error("Failed to increase retry count. Member Id: " + memberId, e);
      }

      return response;
   }

   private Member map(ResultSet rs) {
      try {
         Member model = new Member();
         model.setId(rs.getLong("id"));
         model.setEmail(rs.getString("email"));
         model.setCompanyId(rs.getLong("company_id"));
         model.setRole(UserRole.valueOf(rs.getString("role")));
         model.setStatus(MemberStatus.valueOf(rs.getString("status")));
         model.setRetry(rs.getInt("retry"));
         model.setCreatedAt(rs.getDate("created_at"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set member's properties", e);
      }
      return null;
   }

   private MemberHistory mapHistory(ResultSet rs) {
      try {
         MemberHistory model = new MemberHistory();
         model.setId(rs.getLong("id"));
         model.setMemberId(rs.getLong("member_id"));
         model.setCompanyId(rs.getLong("company_id"));
         model.setStatus(MemberStatus.valueOf(rs.getString("status")));
         model.setCreatedAt(rs.getDate("created_at"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set member's history properties", e);
      }
      return null;
   }

}
