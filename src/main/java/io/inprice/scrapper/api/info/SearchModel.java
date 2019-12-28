package io.inprice.scrapper.api.info;

import java.util.Map;

import io.inprice.scrapper.api.utils.SqlHelper;
import io.inprice.scrapper.common.models.Model;

public class SearchModel {

	private String query;
	private int rowCount;
	private int pageNo;
	private String orderBy;
	private String orderDir;

	public SearchModel(Map<String, String> map, Class<?> clazz) {
		query = SqlHelper.clear(map.getOrDefault("query", "").trim());
		rowCount = Integer.parseInt(map.getOrDefault("rowCount", "10"));
		pageNo = Integer.parseInt(map.getOrDefault("pageNo", "1"));
		orderBy = SqlHelper.clear(map.getOrDefault("orderBy", "id").trim());
		orderDir = SqlHelper.clear(map.getOrDefault("orderDir", "asc").trim());
		
		//clearance
		if (query.length() > 500) query = query.substring(0, 500);
		if (rowCount < 10 || rowCount > 100) rowCount = 10;
		if (pageNo < 1) rowCount = 1;
		if (! orderDir.matches("(a|de)sc")) orderDir = "asc";
		
		try {
			clazz.getField(orderBy);
		} catch (NoSuchFieldException e) {
			orderBy = "id";
		}
	}
	
	public String getQuery() {
		return query;
	}

	public int getRowCount() {
		return rowCount;
	}

	public int getPageNo() {
		return pageNo;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public String getOrderDir() {
		return orderDir;
	}

}
