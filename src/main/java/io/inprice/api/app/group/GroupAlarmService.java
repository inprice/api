package io.inprice.api.app.group;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.jdbi.v3.core.Handle;

import io.inprice.api.app.alarm.AlarmDao;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.converters.GroupRefreshResultConverter;
import io.inprice.common.info.GroupRefreshResult;
import io.inprice.common.models.Alarm;
import io.inprice.common.repository.CommonDao;

public class GroupAlarmService {
	
	public static GroupRefreshResult updateAlarm(Long groupId, Handle handle) {
		Set<Long> idSet = new HashSet<>(1);
		idSet.add(groupId);
		return updateAlarm(idSet, handle);
	}

	public static GroupRefreshResult updateAlarm(Set<Long> groupIdSet, Handle handle) {
		AlarmDao alarmDao = handle.attach(AlarmDao.class);
		CommonDao commonDao = handle.attach(CommonDao.class);
		
		GroupRefreshResult lastGRR = null;
		
		for (Long gid: groupIdSet) {
			lastGRR = GroupRefreshResultConverter.convert(commonDao.refreshGroup(gid));

			if (lastGRR.getAlarmId() != null) {
				Alarm alarm = alarmDao.findById(gid, CurrentUser.getAccountId());
				if (alarm != null) {
					
			  	BigDecimal newAmount = BigDecimal.ZERO;
		  		switch (alarm.getSubject()) {
  	  			case MINIMUM: {
  	  				newAmount = lastGRR.getMinPrice();
  	  				break;
  	  			}
  	  			case AVERAGE: {
  	  				newAmount = lastGRR.getAvgPrice();
  	  				break;
  	  			}
  	  			case MAXIMUM: {
  	  				newAmount = lastGRR.getMaxPrice();
  	  				break;
  	  			}
  	  			case TOTAL: {
  	  				newAmount = lastGRR.getTotal();
  	  				break;
  	  			}
  					default: break;
  				}
		  		
		  		handle.execute(
	  				"update alarm set last_status=?, last_amount=?, updated_at=now() where id=? ",
	          lastGRR.getLevel(),
	          newAmount,
	          lastGRR.getAlarmId()
					);
				}
			}
		}
		
		return lastGRR;
	}
	
}
