package io.inprice.api.app.superuser.user.dto;

import java.util.Date;

import io.inprice.api.dto.BaseSearchDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccessLogSearchDTO extends BaseSearchDTO {

	private Long userId;
	private String method; //GET, POST, DELETE, PUT
	private Date startDate;
	private Date endDate;
	private Long accountId;

}
