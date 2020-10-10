package io.inprice.api.app.product;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.link.LinkDao;
import io.inprice.api.app.product.dto.ProductDTO;
import io.inprice.api.app.product.dto.ProductSearchDTO;
import io.inprice.api.app.product.mapper.ProductReducer;
import io.inprice.api.app.product.mapper.SimpleSearch;
import io.inprice.api.app.tag.TagDao;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.validator.ProductValidator;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.Link;
import io.inprice.common.models.Product;
import io.inprice.common.models.ProductTag;

public class ProductService {

  private static final Logger log = LoggerFactory.getLogger(ProductService.class);

  public Response findById(Long id) {
    try (Handle handle = Database.getHandle()) {
      ProductDao productDao = handle.attach(ProductDao.class);

      Product product = productDao.findById(id, CurrentUser.getCompanyId());
      if (product != null) {
        return new Response(product);
      }
      return Responses.NotFound.PRODUCT;
    }
  }

  public Response findEverythingById(Long id) {
    Map<String, Object> dataMap = new HashMap<>(2);

    try (Handle handle = Database.getHandle()) {
      ProductDao productDao = handle.attach(ProductDao.class);

      Product product = productDao.findById(id, CurrentUser.getCompanyId());
      if (product != null) {
        dataMap.put("product", product);

        LinkDao linkDao = handle.attach(LinkDao.class);
        List<Link> linkList = linkDao.findListByProductIdAndCompanyId(id, CurrentUser.getCompanyId());
        if (linkList != null) {
          dataMap.put("links", linkList);
        }

        return new Response(dataMap);
      }
    }
    return Responses.NotFound.PRODUCT;
  }

  public Response simpleSearch(String term) {
    try (Handle handle = Database.getHandle()) {
      ProductDao productDao = handle.attach(ProductDao.class);

      List<SimpleSearch> productList = 
        productDao.searchSimpleByTermAndCompanyId(
          SqlHelper.clear(term), 
          CurrentUser.getCompanyId(), 
          Consts.ROW_LIMIT_FOR_LISTS
        );

      return new Response(Collections.singletonMap("rows", productList));
    }
  }

  public Response fullSearch(ProductSearchDTO dto) {
    clearSearchDto(dto);

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder criteria = new StringBuilder();

    if (StringUtils.isNotBlank(dto.getTerm())) {
      criteria.append("where ");
      criteria.append("p.code like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or ");
      criteria.append("p.name like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' ");
    } else {
      criteria.append("where 1=1 ");
    }

    //company
    criteria.append(" and p.company_id = ");
    criteria.append(CurrentUser.getCompanyId());

    if (dto.getPosition() != null && dto.getPosition() > 0) {
      criteria.append(" and p.position = ");
      criteria.append(dto.getPosition());
    }

    if (dto.getSelectedTags() != null && dto.getSelectedTags().length > 0) {
      criteria.append(
        String.format(" and p.id in (select product_id from product_tag t where t.name in ('%s')) ", String.join("', '", dto.getSelectedTags()))
      );
    }

    //limiting
    String limit = " limit " + Consts.ROW_LIMIT_FOR_LISTS;
    if (dto.getLoadMore() && dto.getRowCount() >= Consts.ROW_LIMIT_FOR_LISTS) {
      limit = " limit " + dto.getRowCount() + ", " + Consts.ROW_LIMIT_FOR_LISTS;
    }

    //---------------------------------------------------
    //fetching the data
    //---------------------------------------------------
    try (Handle handle = Database.getHandle()) {
      List<Product> searchResult =
        handle.createQuery(
          "select "+ProductDao.PRODUCT_FIELDS+", "+ProductDao.TAG_FIELDS+" from product as p " + 
          "left join product_tag as pt on pt.product_id = p.id " +
          criteria +
          " order by p.name " +
          limit
        )
        .registerRowMapper(BeanMapper.factory(Product.class, "p"))
        .registerRowMapper(BeanMapper.factory(ProductTag.class, "pt"))
        .reduceRows(new ProductReducer())
      .collect(Collectors.toList());

      return new Response(Collections.singletonMap("rows", searchResult));
    } catch (Exception e) {
      log.error("Failed in full search for products. ", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  public Response insert(ProductDTO dto) {
    Response[] res = { Responses.DataProblem.DB_PROBLEM };
    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(transactional -> {
        res[0] = ProductCreator.create(transactional, dto);
        return res[0].isOK();
      });
    }
    return res[0];
  }

  public Response update(ProductDTO dto) {
    if (dto != null && dto.getId() != null && dto.getId() > 0) {

      String problem = ProductValidator.validate(dto);
      if (problem == null) {

        final Response[] res = { Responses.DataProblem.DB_PROBLEM };

        try (Handle handle = Database.getHandle()) {
          handle.inTransaction(transactional -> {
            ProductDao productDao = transactional.attach(ProductDao.class);

            Product product = productDao.findByCode(dto.getCode(), CurrentUser.getCompanyId());
            if (product != null) {
              if (product.getId().equals(dto.getId())) {
                boolean isUpdated = 
                  productDao.update(
                    dto.getId(),
                    dto.getCompanyId(),
                    dto.getCode(),
                    dto.getName(),
                    dto.getPrice()
                  );
                  //TODO: burada yapılacak denetim
                  // if product.price().equals(dto.price) then commonDao daki fiyatları düzenleme kısmını çalıştır!!!
                if (isUpdated) {
                  if (dto.getTagsChanged()) {
                    TagDao tagDao = transactional.attach(TagDao.class);
                    tagDao.deleteTags(dto.getId(), dto.getCompanyId());
                    if (dto.getTags() != null && dto.getTags().size() > 0) {
                      tagDao.insertTags(dto.getId(), dto.getCompanyId(), dto.getTags());
                    }
                  }
                  res[0] = Responses.OK;
                } else {
                  res[0] = Responses.PermissionProblem.PRODUCT_LIMIT_PROBLEM;
                }
              } else {
                res[0] = Responses.DataProblem.ALREADY_EXISTS;
              }
            } else {
              res[0] = Responses.NotFound.PRODUCT;
            }

            return res[0].isOK();
          });
        }
        return res[0];

      } else {
        return new Response(problem);
      }
    }
    return Responses.Invalid.PRODUCT;
  }

  Response deleteById(Long id) {
    if (id != null && id > 0) {
      final boolean[] isOK = { false };
      final String where = String.format("where product_id=%d and company_id=%d", id, CurrentUser.getCompanyId());

      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(transactional -> {
          Batch batch = transactional.createBatch();
          batch.add("delete from link_price " + where);
          batch.add("delete from link_history " + where);
          batch.add("delete from link_spec " + where);
          batch.add("delete from link " + where);
          batch.add("delete from product_price " + where);
          batch.add("delete from product " + where.replace("product_", "")); //this clause is important since determines the success!
          batch.add("update company set product_count=product_count-1 where product_count>0 and id=" + CurrentUser.getCompanyId());
          int[] result = batch.execute();
          isOK[0] = result[5] > 0;
          return isOK[0];
        });
      }

      if (isOK[0]) {
        return Responses.OK;
      }
    }
    return Responses.Invalid.PRODUCT;
  }

  public Response toggleStatus(Long id) {
    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
        ProductDao productDao = handle.attach(ProductDao.class);

        boolean isOK = productDao.toggleStatus(id, CurrentUser.getCompanyId());
        if (isOK) {
          return Responses.OK;
        }
      }
    }
    return Responses.NotFound.PRODUCT;
  }

  private void clearSearchDto(ProductSearchDTO dto) {
    dto.setTerm(SqlHelper.clear(dto.getTerm()));
    if (dto.getSelectedTags() != null && dto.getSelectedTags().length > 0) {
      for (int i = 0; i < dto.getSelectedTags().length; i++) {
        String tag = dto.getSelectedTags()[i];
        if (StringUtils.isNotBlank(tag)) {
          dto.getSelectedTags()[i] = SqlHelper.clear(tag);
        }
      }
    }
  }

}
