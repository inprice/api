package io.inprice.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimpleDef implements Serializable {

	private static final long serialVersionUID = 98730118192504028L;

	private Long id;
	private String name;
	
}
