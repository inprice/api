package io.inprice.scrapper.api.app.link;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.component.UserInfo;
import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.BulkDeleteStatements;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Props;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;

public class LinkRepository {

   private static final Logger log = LoggerFactory.getLogger(LinkRepository.class);

   private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);
   private static final BulkDeleteStatements bulkDeleteStatements = Beans.getSingleton(BulkDeleteStatements.class);

   public ServiceResponse findById(Long id) {
      Link model = dbUtils.findSingle(
         String.format(
            "select * from link " + 
            "where id = %d " + 
            "  and company_id = %d ", id, UserInfo.getCompanyId()), this::map);
      if (model != null) {
         return new ServiceResponse(model);
      }
      return Responses.NotFound.LINK;
   }

   public ServiceResponse getList(Long productId) {
      List<Link> links = dbUtils.findMultiple(
            String.format(
               "select * from link " + 
               "where product_id = %d " + 
               "  and company_id = %d " + 
               "order by name", productId, UserInfo.getCompanyId()), this::map);

      if (links != null && links.size() > 0) {
         return new ServiceResponse(links);
      }
      return Responses.NotFound.PRODUCT;
   }

   public ServiceResponse insert(LinkDTO linkDTO) {
      if (Props.isLinkUniqueness()) {
         boolean alreadyExists = doesExist(linkDTO.getUrl(), linkDTO.getProductId());
         if (alreadyExists) {
            return Responses.DataProblem.ALREADY_EXISTS;
         }
      }

      final String query = 
         "insert into link " + 
         "(url, product_id, company_id) " +
         "values  (?, ?, ?) ";

      try (Connection con = dbUtils.getConnection(); PreparedStatement pst = con.prepareStatement(query)) {

         int i = 0;
         pst.setString(++i, linkDTO.getUrl());
         pst.setLong(++i, linkDTO.getProductId());
         pst.setLong(++i, UserInfo.getCompanyId());

         if (pst.executeUpdate() > 0)
            return Responses.OK;
         else
            return Responses.DataProblem.DB_PROBLEM;

      } catch (SQLIntegrityConstraintViolationException ie) {
         log.error("Failed to insert link: " + ie.getMessage());
         return Responses.DataProblem.INTEGRITY_PROBLEM;
      } catch (Exception e) {
         log.error("Failed to insert link", e);
         return Responses.ServerProblem.EXCEPTION;
      }
   }

   public ServiceResponse deleteById(Long id) {
      boolean result = dbUtils.executeBatchQueries(
         bulkDeleteStatements.linksByLinkIdId(id),
         String.format("Failed to delete link. Id: %d", id), 1 // at least one execution must be successful
      );

      if (result)
         return Responses.OK;
      else
         return Responses.NotFound.LINK;
   }

   public ServiceResponse changeStatus(Long id, Long productId, LinkStatus status) {
      Connection con = null;
      try {
         con = dbUtils.getTransactionalConnection();

         final String q1 = 
            "update link " + 
            "set pre_status=status, status=?, last_update=now() " + 
            "where id=? " + 
            "  and status != ? " + 
            "  and product_id=? " + 
            "  and company_id=? ";

         boolean res1;
         boolean res2 = false;

         try (PreparedStatement pst = con.prepareStatement(q1)) {
            int i = 0;
            pst.setString(++i, status.name());
            pst.setLong(++i, id);
            pst.setString(++i, status.name());
            pst.setLong(++i, productId);
            pst.setLong(++i, UserInfo.getCompanyId());

            res1 = (pst.executeUpdate() > 0);
         }

         if (res1) {
            try (PreparedStatement pst = con
                  .prepareStatement("insert into link_history (link_id, status, product_id, company_id) "
                        + "values (?, ?, ?, ?)")) {
               int i = 0;
               pst.setLong(++i, id);
               pst.setString(++i, status.name());
               pst.setLong(++i, productId);
               pst.setLong(++i, UserInfo.getCompanyId());

               res2 = (pst.executeUpdate() > 0);
            }
         } else {
            log.warn("Link's status is already changed! Link Id: {}, New Status: {}", id, status);
         }

         if (res1) {
            if (res2) {
               dbUtils.commit(con);
               return Responses.OK;
            } else {
               dbUtils.rollback(con);
               return Responses.DataProblem.DB_PROBLEM;
            }
         } else {
            return Responses.NotFound.PRODUCT;
         }

      } catch (SQLException e) {
         if (con != null)
            dbUtils.rollback(con);
         log.error("Failed to change link's status. Link Id: " + id, e);
         return Responses.ServerProblem.EXCEPTION;
      } finally {
         if (con != null)
            dbUtils.close(con);
      }
   }

   private boolean doesExist(String url, Long productId) {
      Link model = dbUtils
         .findSingle(String.format(
            "select * from link " + 
            "where url = '%s' " + 
            "  and product_id = %d " + 
            "  and company_id = %d ", url, productId, UserInfo.getCompanyId()), this::map);
      return (model != null);
   }

   private Link map(ResultSet rs) {
      try {
         Link model = new Link();
         model.setId(rs.getLong("id"));
         model.setUrl(rs.getString("url"));
         model.setSku(rs.getString("sku"));
         model.setName(rs.getString("name"));
         model.setBrand(rs.getString("brand"));
         model.setSeller(rs.getString("seller"));
         model.setShipment(rs.getString("shipment"));
         model.setPrice(rs.getBigDecimal("price"));
         model.setPreStatus(LinkStatus.valueOf(rs.getString("pre_status")));
         model.setStatus(LinkStatus.valueOf(rs.getString("status")));
         model.setLastCheck(rs.getDate("last_check"));
         model.setLastUpdate(rs.getDate("last_update"));
         model.setRetry(rs.getInt("retry"));
         model.setHttpStatus(rs.getInt("http_status"));
         model.setWebsiteClassName(rs.getString("website_class_name"));
         model.setCompanyId(rs.getLong("company_id"));
         model.setProductId(rs.getLong("product_id"));
         model.setSiteId(rs.getLong("site_id"));

         // if not null then it is an imported product!
         model.setImportId(rs.getLong("import_id"));
         model.setImportRowId(rs.getLong("import_row_id"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set link's properties", e);
      }
      return null;
   }

}
