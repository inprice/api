package io.inprice.api.app.lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.lookup.dto.LookupDTO;
import io.inprice.api.app.lookup.mapper.LookupWithInfo;
import io.inprice.api.app.product.ProductDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.Pair;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.LookupType;
import io.inprice.common.meta.Position;
import io.inprice.common.models.Lookup;

class LookupService {

  private static final Logger log = LoggerFactory.getLogger(LookupService.class);

  Response add(LookupDTO dto) {
    Response res = validate(dto);

    if (res.isOK()) {
      try (Handle handle = Database.getHandle()) {
        LookupDao lookupDao = handle.attach(LookupDao.class);

        Map<String, Object> data = new HashMap<>(3);
        data.put("type", dto.getType());
        data.put("items", getConvertedList(lookupDao, dto.getType()));

        Lookup lookup = lookupDao.findByTypeAndName(dto.getType(), dto.getNewValue(), CurrentUser.getCompanyId());
        if (lookup != null) {
          data.put("selected", lookup.getId());
        } else {
          long addedId = lookupDao.insert(CurrentUser.getCompanyId(), dto.getType(), SqlHelper.clear(dto.getNewValue()));
          if (addedId > 0) {
            data.put("selected", addedId);
          }
        }

        return new Response(data);
      } catch (Exception e) {
        log.error("Failed to insert lookup. " + dto.toString(), e);
      }
    }
    return Responses.DataProblem.DB_PROBLEM;
  }

  Response getList(LookupType type) {
    try (Handle handle = Database.getHandle()) {
      LookupDao lookupDao = handle.attach(LookupDao.class);
      return new Response(getConvertedList(lookupDao, type.name()));
    }
  }

  Response getAllList() {
    Map<String, List<Pair<Long, String>>> data = new HashMap<>();

    try (Handle handle = Database.getHandle()) {
      LookupDao lookupDao = handle.attach(LookupDao.class);
      ProductDao productDao = handle.attach(ProductDao.class);

      Map<Integer, Integer> positions = productDao.findPositionDists(CurrentUser.getCompanyId());
      if (positions != null && positions.size() > 0) {
        List<Pair<Long, String>> entryList = new ArrayList<>();
        for (Entry<Integer, Integer> entry: positions.entrySet()) {

          if (entry.getKey() != null) {
            String name = Position.getByOrdinal(entry.getKey()-1).name() + " (" + entry.getValue() + ")";
            entryList.add(new Pair<>((long)entry.getKey(), name));
          } else {
            String name = "NOT SET (" + entry.getValue() + ")";
            entryList.add(new Pair<>(1L, name));
          }
        }
        data.put("POSITIONS", entryList);
      }

      //brand and categories
      String[] types = { LookupType.BRAND.name(), LookupType.CATEGORY.name() };

      for (String type: types) {
        List<LookupWithInfo> lookups = lookupDao.findLookupsWithInfo(type.toLowerCase(), CurrentUser.getCompanyId());
        if (lookups != null && lookups.size() > 0) {
          List<Pair<Long, String>> entryList = new ArrayList<>();
          for (LookupWithInfo lwi: lookups) {
            entryList.add(new Pair<>(lwi.getId(), lwi.getName() + " (" + lwi.getCount() + ")"));
          }
          data.put(type, entryList);
        }

      }
      return new Response(data);

    } catch (Exception e) {
      log.error("Failed to get all lookups for Company: " + CurrentUser.getCompanyId(), e);
    }

    return Responses.NotFound.DATA;

  }

  private List<Map<String, Object>> getConvertedList(LookupDao lookupDao, String type) {
    List<Map<String, Object>> data = new ArrayList<>();

    List<Lookup> lookups = lookupDao.findListByCompanyIdAndType(CurrentUser.getCompanyId(), type);
    if (lookups != null && lookups.size() > 0) {
      for (Lookup lu: lookups) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("value", lu.getId());
        map.put("text", lu.getName());
        data.add(map);
      }
    }
    return data;
  }

  private Response validate(LookupDTO dto) {
    String problem = null;

    if (dto == null) {
      problem = "Invalid company info!";
    }

    if (problem == null) {
      dto.setType(SqlHelper.clear(dto.getType()));
      dto.setNewValue(SqlHelper.clear(dto.getNewValue()));
      try {
        LookupType.valueOf(dto.getType()); //just checking
      } catch (Exception e) {
        problem = "Invalid type!";
      }
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getNewValue())) {
        problem = "Value cannot be empty!";
      } else {
        if (dto.getNewValue().length() < 1 || dto.getNewValue().length() > 50) {
          problem = "Value must be 1-50 chars!";
        }
      }
    }

    if (problem == null) {
      return Responses.OK;
    } else {
      return new Response(problem);
    }
  }

}
