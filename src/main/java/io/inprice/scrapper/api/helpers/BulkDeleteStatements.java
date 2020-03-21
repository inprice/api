package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.rest.component.UserInfo;

import java.util.Arrays;

class BulkDeleteStatements {

   private String[] linksByProductId(Long productId) {
      return links(productId, null);
   }

   String[] linksByLinkIdId(Long linkId) {
      return links(null, linkId);
   }

   /**
    * Generates necessary delete statements for links
    *
    * @return delete statements
    */
   private String[] links(Long productId, Long linkId) {
      String where = "where company_id=" + UserInfo.getCompanyId();
      String where_1 = where;

      // delete by productId
      if (productId != null) {
         where += " and product_id=" + productId;
      }

      // delete by linkId
      if (linkId != null) {
         where += " and link_id=" + linkId;
         where_1 += " and id=" + linkId;
      }

      return new String[] { "delete from link_price " + where, "delete from link_history " + where,
            "delete from link_spec " + where, "delete from link " + where_1 };
   }

   /**
    * Generates necessary delete statements for products
    *
    * @return delete statements
    */
   String[] products(Long productId) {
      String where = "where company_id=" + UserInfo.getCompanyId();
      String where_1 = where;

      String[] linkDeletions = null;

      // delete by productId
      if (productId != null) {
         where += " and product_id=" + productId;
         where_1 += " and id=" + productId;
         linkDeletions = linksByProductId(productId);
      }

      String[] productDeletions = { "delete from product_price " + where, "delete from product " + where_1 };

      return concatenate(linkDeletions, productDeletions);
   }

   /**
    * Generates necessary delete statements for a company
    *
    * @return delete statements String[] companies(Long companyId) { if (companyId
    *         == null) { return null; }
    * 
    *         String[] productDeletions = productsByCompanyId(companyId);
    * 
    *         String[] companyDeletions = { String.format("delete from
    *         import_product_row where workspace_id=%d and company_id=%d",
    *         workspaceId, UserInfo.getCompanyId()), String.format("delete from
    *         import_product where workspace_id=%d and company_id=%d", workspaceId,
    *         UserInfo.getCompanyId()), String.format("delete from
    *         workspace_history where workspace_id=%d and company_id=%d",
    *         workspaceId, UserInfo.getCompanyId()), String.format("delete from
    *         workspace where id=%d and company_id=%d", workspaceId,
    *         UserInfo.getCompanyId()) };
    * 
    *         return concatenate(productDeletions, companyDeletions); }
    */

   private String[] concatenate(String[] first, String[] second) {
      String[] both = Arrays.copyOf(first, first.length + second.length);
      System.arraycopy(second, 0, both, first.length, second.length);
      return both;
   }

}
