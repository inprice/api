package io.inprice.api.app.link;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.inprice.common.models.Link;
import io.inprice.common.models.LinkHistory;
import io.inprice.common.models.LinkPrice;
import io.inprice.common.models.LinkSpec;

public class LinkRepository {

  public static List<Link> findDetailedLinkList(Long groupId, long accountId, LinkDao linkDao) {
    List<Link> linkList = linkDao.findListByGroupId(groupId, accountId);
    if (linkList != null && linkList.size() > 0) {
      int i = 0;
      Map<Long, Integer> refMapToLinks = new LinkedHashMap<>(linkList.size());
      for (Link link: linkList) {
        link.setPriceList(new ArrayList<>());
        link.setHistoryList(new ArrayList<>());

        List<LinkSpec> specList = new ArrayList<>();
        if (StringUtils.isNotBlank(link.getBrand())) specList.add(new LinkSpec("Brand", link.getBrand()));
        if (StringUtils.isNotBlank(link.getSku())) specList.add(new LinkSpec("Code", link.getSku()));
        link.setSpecList(specList);

        refMapToLinks.put(link.getId(), i++);
      }

      List<LinkSpec> specsList = linkDao.findSpecListByGroupId(groupId); // finding links' specs
      for (LinkSpec linkSpec: specsList) {
      	linkList.get(refMapToLinks.get(linkSpec.getLinkId())).getSpecList().add(linkSpec);
      }

      List<LinkPrice> pricesList = linkDao.findPriceListByGroupId(groupId); // finding links' prices
      for (LinkPrice linkPrice: pricesList) {
      	linkList.get(refMapToLinks.get(linkPrice.getLinkId())).getPriceList().add(linkPrice);
      }

      List<LinkHistory> historiesList = linkDao.findHistoryListByGroupId(groupId); // finding links' histories
      for (LinkHistory linkHistory: historiesList) {
      	linkList.get(refMapToLinks.get(linkHistory.getLinkId())).getHistoryList().add(linkHistory);
      }
    }
  	return linkList;
  }

}
