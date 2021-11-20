package io.inprice.api.app.alarm.mapper;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlarmEntity implements Serializable {

	private static final long serialVersionUID = 38599657433534809L;

	private Long id;
	private String sku;
	private String name;

}
