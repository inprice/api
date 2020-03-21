package io.inprice.scrapper.api.helpers;

import java.sql.ResultSet;

public interface ModelMapper<M> {

   M map(ResultSet rs);

}
