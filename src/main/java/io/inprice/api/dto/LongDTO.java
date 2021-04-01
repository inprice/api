package io.inprice.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LongDTO implements Serializable {

	private static final long serialVersionUID = -7409381497867977979L;

	private Long value;

}
