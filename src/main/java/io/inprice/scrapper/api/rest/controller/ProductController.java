package io.inprice.scrapper.api.rest.controller;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import java.util.HashMap;
import java.util.Map;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.service.ProductService;
import io.inprice.scrapper.api.utils.SqlHelper;
import io.inprice.scrapper.common.utils.NumberUtils;

public class ProductController {

	private static final ProductService service = Beans.getSingleton(ProductService.class);

	@Routing
	public void routes() {

		// insert
		post(Consts.Paths.Product.BASE, (req, res) -> {
			return Commons.createResponse(res, service.insert(Commons.toProductModel(req)));
		}, Global.gson::toJson);

		// update
		put(Consts.Paths.Product.BASE, (req, res) -> {
			return Commons.createResponse(res, service.update(Commons.toProductModel(req)));
		}, Global.gson::toJson);

		// delete
		delete(Consts.Paths.Product.BASE + "/:id", (req, res) -> {
			return Commons.createResponse(res, service.deleteById(NumberUtils.toLong(req.params(":id"))));
		}, Global.gson::toJson);

		// find
		get(Consts.Paths.Product.BASE + "/:id", (req, res) -> {
			return Commons.createResponse(res, service.findById(NumberUtils.toLong(req.params(":id"))));
		}, Global.gson::toJson);

		// list
		get(Consts.Paths.Product.BASE + "s", (req, res) -> {
			return Commons.createResponse(res, service.getList());
		}, Global.gson::toJson);

		// search
		get(Consts.Paths.Product.SEARCH, (req, res) -> {
			Map<String, String> searchMap = new HashMap<>(SqlHelper.STANDARD_SEARCH_MAP);
			if (req.queryMap().hasKeys()) {
				Map<String, String[]> queryMap = req.queryMap().toMap();
				for (Map.Entry<String, String[]> entry : queryMap.entrySet()) {
					if (searchMap.containsKey(entry.getKey())) {
						searchMap.put(entry.getKey(), entry.getValue()[0]);
					}
				}
			}
			return Commons.createResponse(res, service.search(searchMap));
		}, Global.gson::toJson);

		// toggle active status
		put(Consts.Paths.Product.TOGGLE_STATUS + "/:id", (req, res) -> {
			return Commons.createResponse(res, service.toggleStatus(NumberUtils.toLong(req.params(":id"))));
		}, Global.gson::toJson);

	}

}
