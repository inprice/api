package io.inprice.api.app.lookup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.LookupDTO;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.LookupType;
import io.inprice.common.models.Lookup;

public class LookupRepository {

  private static final Logger log = LoggerFactory.getLogger(LookupRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  public ServiceResponse add(LookupDTO dto) {
    final String query = "insert into lookup (company_id, type, name) values (?, ?, ?)";

    try (Connection con = db.getConnection();
        PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

      Map<String, Object> data = new HashMap<>(3);
      data.put("type", dto.getType());

      Lookup found = findByTypeAndName(con, dto.getType(), dto.getNewValue());
      if (found != null) {
        data.put("selected", found.getId());
      } else {
        int i = 0;
        pst.setLong(++i, CurrentUser.getCompanyId());
        pst.setString(++i, dto.getType());
        pst.setString(++i, SqlHelper.clear(dto.getNewValue()));

        if (pst.executeUpdate() > 0) {
          try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
            if (generatedKeys.next()) {
              data.put("selected", generatedKeys.getLong(1));
            }
          }
        }
      }

      data.put("items", getList(con, LookupType.valueOf(dto.getType())).getData());
      return new ServiceResponse(data);

    } catch (Exception e) {
      log.error("Failed to insert lookup. " + dto.toString(), e);
    }
    return Responses.DataProblem.DB_PROBLEM;
  }

  public Lookup add(Connection con, LookupType type, String name) {
    final String query = "insert into lookup (company_id, type, name) values (?, ?, ?)";

    try (PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      Lookup found = findByTypeAndName(con, type.name(), name);
      if (found == null) {
        int i = 0;
        pst.setLong(++i, CurrentUser.getCompanyId());
        pst.setString(++i, type.name());
        pst.setString(++i, SqlHelper.clear(name));

        if (pst.executeUpdate() > 0) {
          try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
            if (generatedKeys.next()) {
              found = new Lookup();
              found.setId(generatedKeys.getLong(1));
              found.setCompanyId(CurrentUser.getCompanyId());
              found.setType(type);
              found.setName(name);
            }
          }
        }
      }
      return found;

    } catch (Exception e) {
      log.error("Failed to insert lookup. Type: " + type + ", Name: " + name, e);
    }
    return null;
  }

  private Lookup findByTypeAndName(Connection con, String type, String name) {
    return 
      db.findSingle(con, 
        String.format(
          "select * from lookup where company_id=%d and type='%s' and name='%s'", 
          CurrentUser.getCompanyId(), type, name
        ), 
        this::map);
  }

  public ServiceResponse getList(Connection con, LookupType type) {
    try {
      boolean isConNull = (con == null);
      Connection connection = (isConNull ? connection = db.getConnection() : con);

      List<Map<String, Object>> data = new ArrayList<>();
      List<Lookup> lookups = db.findMultiple(connection, "select * from lookup where type='"+type.name()+"' order by name", this::map);
      if (lookups != null && lookups.size() > 0) {
        for (Lookup lu: lookups) {
          Map<String, Object> map = new HashMap<>(2);
          map.put("value", lu.getId());
          map.put("text", lu.getName());
          data.add(map);
        }
      }

      if (isConNull) connection.close();
      return new ServiceResponse(data);

    } catch (Exception e) {
      log.error("Failed to get lookup. " + type, e);
    }

    return Responses.NotFound.DATA;
  }

  public Map<String, Lookup> getMap(LookupType type) {
    try (Connection con = db.getConnection()) {
      List<Lookup> lookups = db.findMultiple(con, "select * from lookup where type='"+type.name()+"'", this::map);
      if (lookups != null && lookups.size() > 0) {
        Map<String, Lookup> result = new HashMap<>(lookups.size());
        for (Lookup lu: lookups) {
          result.put(lu.getName(), lu);
        }
        return result;
      }
    } catch (Exception e) {
      log.error("Failed to get lookup map. " + type, e);
    }

    return new HashMap<>();
  }

  private Lookup map(ResultSet rs) {
    try {
      Lookup model = new Lookup();
      model.setId(rs.getLong("id"));
      model.setCompanyId(rs.getLong("company_id"));
      model.setType(LookupType.valueOf(rs.getString("type")));
      model.setName(rs.getString("name"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set lookup", e);
    }
    return null;
  }
  
}