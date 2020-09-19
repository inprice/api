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
import io.inprice.api.dto.Pair;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.RepositoryHelper;
import io.inprice.common.meta.LookupType;
import io.inprice.common.meta.Position;
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
      List<Lookup> lookups = 
        db.findMultiple(
          connection, 
          String.format(
            "select * from lookup " +
            "where company_id=%d " +
            "  and type='%s' " +
            "order by name",
            CurrentUser.getCompanyId(), type.name()
          ), 
        this::map
        );
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

  public ServiceResponse getAllList() {
    try (Connection connection = db.getConnection()) {
      Map<String, List<Pair<Long, String>>> data = new HashMap<>();

      //positions
      List<Map<String, Object>> positions = 
      db.findMultiple(
        connection, 
        String.format(
          "select pp.position, count(1) as counter from product as p " +
          "left join product_price as pp on pp.id=p.last_price_id " +
          "where p.company_id=%d " +
          "group by pp.position " +
          "order by pp.position ",
          CurrentUser.getCompanyId()
        ), 
        this::mapPositionCounts
      );

      if (positions != null && positions.size() > 0) {
        List<Pair<Long, String>> entryList = new ArrayList<>();
        for (Map<String, Object> pos: positions) {
          if (pos.get("position") != null) {
            int posNum = Integer.parseInt(pos.get("position").toString());
            String name = Position.getByOrdinal(posNum-1).name() + " (" + pos.get("counter").toString() + ")";
            entryList.add(new Pair<>((long)posNum+1, name));
          } else {
            String name = "NOT SET (" + pos.get("counter").toString() + ")";
            entryList.add(new Pair<>(1L, name));
          }
        }
        data.put("POSITIONS", entryList);
      }

      //brand and categories
      String[] types = { LookupType.BRAND.name(), LookupType.CATEGORY.name() };

      for (String type: types) {
        List<Map<String, Object>> lookups = 
          db.findMultiple(
            connection, 
            String.format(
              "select l.id, l.name, count(1) as counter from lookup as l " +
              "inner join product as p on p.%s_id = l.id " +
              "where p.company_id=%d " +
              "group by l.id, l.name " +
              "order by l.name ",
              type.toLowerCase(), CurrentUser.getCompanyId()
            ), 
            this::mapCountly
          );

        if (lookups != null && lookups.size() > 0) {
          List<Pair<Long, String>> entryList = new ArrayList<>();
          for (Map<String, Object> lu: lookups) {
            entryList.add(new Pair<>(Long.parseLong(lu.get("id").toString()), lu.get("name").toString() + " (" + lu.get("counter").toString() + ")"));
          }
          data.put(type, entryList);
        }

      }
      return new ServiceResponse(data);

    } catch (Exception e) {
      log.error("Failed to get all lookups for Company: " + CurrentUser.getCompanyId(), e);
    }

    return Responses.NotFound.DATA;
  }
/*
  public ServiceResponse getAllList() {
    try (Connection connection = db.getConnection()) {
      Map<String, List<Pair<Long, String>>> data = new HashMap<>();
      List<Lookup> lookups = 
        db.findMultiple(
          connection, 
          String.format(
            "select * from lookup " +
            "where company_id=%d " +
            "order by type, name",
            CurrentUser.getCompanyId()
          ), 
          this::map
        );

      if (lookups != null && lookups.size() > 0) {
        LookupType lastType = null;
        List<Pair<Long, String>> entryList = null;

        for (Lookup lu: lookups) {
          if (lastType == null || ! lastType.equals(lu.getType())) {
            if (entryList != null) {
              data.put(lastType.name(), entryList);
            }
            entryList = data.get(lu.getType().name());
            if (entryList == null) entryList = new ArrayList<>();
            lastType = lu.getType();
          }
          entryList.add(new Pair<>(lu.getId(), lu.getName()));
        }
        data.put(lastType.name(), entryList);
      }
      return new ServiceResponse(data);

    } catch (Exception e) {
      log.error("Failed to get all lookups for Company: " + CurrentUser.getCompanyId(), e);
    }

    return Responses.NotFound.DATA;
  }
*/
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

  private Map<String, Object> mapPositionCounts(ResultSet rs) {
    try {
      Map<String, Object> model = new HashMap<>(2);
      model.put("position", RepositoryHelper.nullIntegerHandler(rs, "position"));
      model.put("counter", rs.getInt("counter"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set position counts map", e);
    }
    return null;
  }

  private Map<String, Object> mapCountly(ResultSet rs) {
    try {
      Map<String, Object> model = new HashMap<>(3);
      model.put("id", rs.getLong("id"));
      model.put("name", rs.getString("name"));
      model.put("counter", rs.getInt("counter"));

      return model;
    } catch (SQLException e) {
      log.error("Failed to set count map", e);
    }
    return null;
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