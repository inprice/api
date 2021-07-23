package io.inprice.api.app.superuser.link.dto;

import java.io.Serializable;
import java.util.Set;

import io.inprice.common.meta.LinkStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BulkChangetDTO implements Serializable {

	private static final long serialVersionUID = 5206568143952756331L;

	//acceptable statuses: PAUSED, RESOLVED and NOT_SUITABLE
	private LinkStatus status;
	private Set<Long> idSet;

}
