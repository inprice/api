package io.inprice.api.app.company;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.dto.CreateDTO;
import io.inprice.api.app.company.dto.RegisterDTO;
import io.inprice.api.app.token.TokenService;
import io.inprice.api.app.token.TokenType;
import io.inprice.api.app.user.UserRepository;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.SubsStatus;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.User;
import jodd.util.BCrypt;

class CompanyRepository {

  private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);

  Company findById(Long id) {
    try (Handle handle = Database.getHandle()) {
      CompanyDao dao = handle.attach(CompanyDao.class);
      return dao.findById(id);
    }
  }

  boolean hasUserDefinedTheCompany(RegisterDTO dto) {
    try (Handle handle = Database.getHandle()) {
      CompanyDao dao = handle.attach(CompanyDao.class);
      User user = dao.findUserByEmail(dto.getEmail());
      if (user != null) {
        Company found = dao.findByNameAndAdminId(dto.getCompanyName(), user.getId());
        return (found != null);
      }
      return false;
    }
  }

  ServiceResponse insert(RegisterDTO dto, Map<String, String> clientInfo, String token) {
    final ServiceResponse[] res = { new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!") };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(h -> {
        CompanyDao dao = h.attach(CompanyDao.class);

        User user = dao.findUserByEmail(dto.getEmail());
        if (user == null) {
          user = new User();
          user.setEmail(dto.getEmail());
          user.setName(dto.getEmail().split("@")[0]);
          user.setTimezone(clientInfo.get(Consts.TIMEZONE));
          user.setCreatedAt(new Date());

          final String salt = BCrypt.gensalt(Props.APP_SALT_ROUNDS());
          final String hash = BCrypt.hashpw(dto.getPassword(), salt);

          user.setId(
            dao.insertUser(
              user.getEmail(), 
              user.getName(), 
              clientInfo.get(Consts.TIMEZONE), 
              salt, hash
            )
          );
        }
  
        if (user.getId() != null) {
          res[0] = 
            createCompany(
              dao,
              user.getId(),
              user.getEmail(),
              dto.getCompanyName(),
              clientInfo.get(Consts.CURRENCY_CODE),
              clientInfo.get(Consts.CURRENCY_FORMAT)
            );
        } else {
          res[0] = Responses.NotFound.USER;
        }

        return res[0].isOK();
      });
    }

    return res[0];
  }

  ServiceResponse create(CreateDTO dto) {
    final ServiceResponse[] res = { new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!") };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(h -> {
        CompanyDao dao = h.attach(CompanyDao.class);

        res[0] = 
          createCompany(
            dao,
            CurrentUser.getUserId(),
            CurrentUser.getEmail(),
            dto.getName(),
            dto.getCurrencyCode(),
            dto.getCurrencyFormat()
          );
        return true;
      });
    }

    return res[0];
  }

  boolean update(CreateDTO dto) {
    try (Handle handle = Database.getHandle()) {
      CompanyDao dao = handle.attach(CompanyDao.class);
      return 
        dao.updateCompany(
          dto.getName(),
          dto.getCurrencyCode(),
          dto.getCurrencyFormat(),
          CurrentUser.getCompanyId(),
          CurrentUser.getUserId()
        );
    }
  }

  ServiceResponse deleteEverything(String password) {
    final ServiceResponse[] res = { new ServiceResponse(Responses.DataProblem.DB_PROBLEM.getStatus(), "Database error!") };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(h -> {
        CompanyDao dao = h.attach(CompanyDao.class);

        User user = dao.findUserById(CurrentUser.getUserId());
        String phash = BCrypt.hashpw(password, user.getPasswordSalt());
        if (phash.equals(user.getPasswordHash())) {

          Company company = dao.findByAdminId(CurrentUser.getCompanyId());
          if (company != null) {
            String where = "where company_id=" + CurrentUser.getCompanyId();

            Batch batch = h.createBatch();
            batch.add("SET FOREIGN_KEY_CHECKS=0");
            batch.add("delete from competitor_price " + where);
            batch.add("delete from competitor_history " + where);
            batch.add("delete from competitor_spec " + where);
            batch.add("delete from competitor " + where);
            batch.add("delete from product_price " + where);
            batch.add("delete from product " + where);
            batch.add("delete from lookup " + where);
            batch.add("delete from user_session " + where);
            batch.add("delete from membership " + where);
            batch.add("delete from subs_trans " + where);
            batch.add("delete from user where id in (select admin_id from company where id="+CurrentUser.getCompanyId()+")");
            batch.add("delete from company where id="+CurrentUser.getCompanyId());
            batch.add("SET FOREIGN_KEY_CHECKS=1");
            batch.execute();

            List<String> hashList = dao.getSessionHashesByCompanyId(CurrentUser.getCompanyId());
            if (hashList != null && hashList.size() > 0) {
              for (String hash : hashList) {
                RedisClient.removeSesion(hash);
              }
            }
            res[0] = Responses.OK;
          } else {
            res[0] = Responses.Invalid.COMPANY;
          }
        } else {
          res[0] = Responses.Invalid.USER;
        }

        return res[0].isOK();
      });
    }

    return res[0];
  }

  ServiceResponse createCompany(CompanyDao dao, Long userId, String userEmail, String companyName, String currencyCode, String currencyFormat) {
    Company company = dao.findByNameAndAdminId(companyName, userId);
    if (company == null) {
      Long companyId = 
        dao.insertCompany(
          userId, 
          companyName,
          currencyCode,
          currencyFormat
        );

      if (companyId != null) {
        long membershipId = 
          dao.insertMembership(
            userId,
            userEmail,
            companyId,
            UserRole.ADMIN.name(),
            UserStatus.JOINED.name()
          );

        if (membershipId > 0) {
          log.info("A new user just registered a new company. C.Name: {}, U.Email: {} ", companyName, email);
          return Responses.OK;
        }
      }
      return Responses.DataProblem.DB_PROBLEM;
    } else {
      return Responses.Already.Defined.COMPANY;
    }
  }

}
