package io.inprice.scrapper.api.helpers;

public class BulkDeleteStatements {
/*
  private String[] linksByProductId(Long productId) {
    return links(productId, null);
  }

  public String[] linksByLinkIdId(Long linkId) {
    return links(null, linkId);
  }
*/
  /**
   * Generates necessary delete statements for links
   *
   * @return delete statements
  private String[] links(Long productId, Long linkId) {
    String where = "where company_id=" + CurrentUser.getCompanyId();
    String where_1 = null;

    // delete by productId
    if (productId != null) {
      where += " and product_id=" + productId;
    }

    // delete by linkId
    if (linkId != null) {
      where_1 = where + " and id=" + linkId;
      where += " and link_id=" + linkId;
    } else {
      where_1 = where;
    }

    return new String[] {
      "delete from link_price " + where, 
      "delete from link_history " + where,
      "delete from link_spec " + where, 
      "delete from link " + where_1
    };
  }

  /**
   * Generates necessary delete statements for products
   *
   * @return delete statements
  public String[] products(Long productId) {
    String where = "where company_id=" + CurrentUser.getCompanyId();
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

  private String[] concatenate(String[] first, String[] second) {
    String[] both = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, both, first.length, second.length);
    return both;
  }
   */

}
