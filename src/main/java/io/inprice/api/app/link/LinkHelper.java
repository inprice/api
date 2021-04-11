package io.inprice.api.app.link;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.inprice.api.session.CurrentUser;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkHistory;
import io.inprice.common.models.LinkPrice;
import io.inprice.common.models.LinkSpec;

public class LinkHelper {

  public static List<Link> findDetailedLinkList(Long groupId, LinkDao linkDao) {
    List<Link> linkList = linkDao.findListByGroupId(groupId, CurrentUser.getAccountId());
    if (linkList != null && linkList.size() > 0) {
      int i = 0;
      Map<Long, Integer> refListToLinks = new LinkedHashMap<>(linkList.size());
      for (Link link: linkList) {
        link.setPriceList(new ArrayList<>());
        link.setSpecList(new ArrayList<>());
        link.setHistoryList(new ArrayList<>());
        refListToLinks.put(link.getId(), i++);
      }

      List<LinkSpec> specsList = linkDao.findSpecListByGroupId(groupId); // finding links' specs
      List<LinkPrice> pricesList = linkDao.findPriceListByGroupId(groupId); // finding links' prices
      List<LinkHistory> historiesList = linkDao.findHistoryListByGroupId(groupId); // finding links' histories
      for (LinkSpec linkSpec: specsList) linkList.get(refListToLinks.get(linkSpec.getLinkId())).getSpecList().add(linkSpec);
      for (LinkPrice linkPrice: pricesList) linkList.get(refListToLinks.get(linkPrice.getLinkId())).getPriceList().add(linkPrice);
      for (LinkHistory linkHistory: historiesList) linkList.get(refListToLinks.get(linkHistory.getLinkId())).getHistoryList().add(linkHistory);
    }
  	return linkList;
  }
	
}
