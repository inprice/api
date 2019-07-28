package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.common.models.Country;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CountryRepository {

    private static final Logger log = LoggerFactory.getLogger(CompanyRepository.class);
    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    private final List<Country> countries;
    private final Map<Long, Country> countryIdMap;

    public CountryRepository() {
        countryIdMap = new HashMap<>();

        countries = dbUtils.findMultiple("select * from country", CountryRepository::map);
        for (Country country: countries) {
            countryIdMap.put(country.getId(), country);
        }
        Collections.sort(countries);
    }

    public Country findById(Long id) {
        return countryIdMap.get(id);
    }

    public List<Country> getAll() {
        return countries;
    }

    private static Country map(ResultSet rs) {
        try {
            Country model = new Country();
            model.setId(rs.getLong("id"));
            model.setName(rs.getString("name"));
            model.setCode(rs.getString("code"));
            model.setFlag(rs.getString("flag"));
            model.setLang(rs.getString("lang"));
            model.setLocale(rs.getString("locale"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set country's properties", e);
        }
        return null;
    }

}
