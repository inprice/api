package io.inprice.scrapper.api.info;

import java.util.List;
import java.util.Map;

import io.inprice.scrapper.api.helpers.SqlHelper;

public class SearchModel {

  private String table;
  private String term;
  private boolean isExactSearch;
  private int lastRowNo;
  private String orderBy;
  private List<String> fields;

  public final int ROW_LIMIT = 25;

  public SearchModel(Map<String, String> map, String orderBy, Class<?> clazz) {
    this.term = SqlHelper.clear(map.getOrDefault("term", "").trim());
    this.lastRowNo = Integer.parseInt(map.getOrDefault("lastRowNo", "0"));
    this.orderBy = orderBy;

    // clearance
    if (term.length() > 255)
      term = term.substring(0, 255);
    if (lastRowNo < 0 || lastRowNo > 1000)
      lastRowNo = 0;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public String getTerm() {
    return term;
  }

  public boolean isExactSearch() {
    return isExactSearch;
  }

  public void setExactSearch(boolean isExactSearch) {
    this.isExactSearch = isExactSearch;
  }

  public int getLastRowNo() {
    return lastRowNo;
  }

  public String getOrderBy() {
    return orderBy;
  }

  public List<String> getFields() {
    return fields;
  }

  public void setFields(List<String> fields) {
    this.fields = fields;
  }

  @Override
  public String toString() {
    return "[isExactSearch=" + isExactSearch + ", lastRowNo=" + lastRowNo
        + ", orderBy=" + orderBy + ", table=" + table + ", term=" + term + "]";
  }

}
