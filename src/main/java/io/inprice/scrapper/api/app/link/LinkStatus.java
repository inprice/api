package io.inprice.scrapper.api.app.link;

import java.util.HashSet;
import java.util.Set;

/**
 * This class consists of several statuses that are used to manage links's
 * positions.
 *
 * The statuses below are divided into three groups: a) neutral The links in
 * this group are needed to be handled differently. NEW status, for example, is
 * the initial position. This group will be excluded from analysis and reports.
 *
 * b) positive This group is used for AVAILABLE state and internal problems like
 * SOCKET_ERROR and ClASS_PROBLEM. Other than AVAILABLE state, the links in this
 * group actually have no problems. So, they are needed to be handled by a
 * developer. This group will be included in analysis and reports.
 *
 * c) negative This group is used for troubled states like UNAVAILABLE and
 * NETWORK_ERROR. The links in this group have problems in network or e-commerce
 * website. This group will be excluded from analysis and reports.
 *
 * d) user User driven links take place in this group. This group will be
 * excluded from analysis and reports.
 *
 */
public enum LinkStatus {

   /**
    * The initial state. Used for first recorded links.
    */
   NEW("neutral"),

   /**
    * Used for links which have no problem about collecting data
    */
   AVAILABLE("positive"),

   /**
    * Used for links which need to be renewed
    */
   RENEWED("neutral"),

   /**
    * Used for links whose websites have not been implemented yet. Data won't be
    * collected till implemented
    */
   BE_IMPLEMENTED("neutral"),

   /**
    * Used for newly implemented links
    */
   IMPLEMENTED("neutral"),

   /**
    * Used for duplicate links/products
    */
   DUPLICATE("negative"),

   /**
    * Used for links that will never be implemented
    */
   WONT_BE_IMPLEMENTED("negative"),

   /**
    * Used for indicating links whose URLs are wrong Data never be collected
    */
   IMPROPER("negative"),

   /**
    * Used for indicating links whose URLs not specific The url may point a search
    * page or a product listing page on the website A url must refer only one
    * product's page on which price, name... exist Data never be collected
    */
   NOT_A_PRODUCT_PAGE("negative"),

   /**
    * Used for indicating links whose data is missing
    */
   NO_DATA("negative"),

   /**
    * The links have no sufficient stock are put in this state Data won't be
    * collected till it is available again. Availability checks are done a certain
    * times (look at system parameters)
    */
   NOT_AVAILABLE("negative"),

   /**
    * Used for indicating links whose data has problem
    */
   READ_ERROR("negative"),

   /**
    * Used for specifying links whose website block us for data collecting at first
    * step Availability checks are done a certain times (look at system parameters)
    */
   SOCKET_ERROR("neutral"),

   /**
    * Used for links returning http error codes greater than 399 Data won't be
    * collected till it is available again. Availability checks are done a certain
    * times (look at system parameters)
    */
   NETWORK_ERROR("negative"),

   /**
    * This state is used for indicating links which we face problem during
    * resolving the class name Availability checks are done a certain times (look
    * at system parameters)
    */
   CLASS_PROBLEM("negative"),

   /**
    * Used for internal error like 500 Data won't be collected till it is available
    * again. Availability checks are done a certain times (look at system
    * parameters)
    */
   INTERNAL_ERROR("negative"),

   /**
    * Used for links paused by users. Data won't be collected till it is activated
    * again.
    */
   PAUSED("user"),

   /**
    * Used for paused links to resume.
    */
   RESUMED("user");

   private String group;

   LinkStatus(String group) {
      this.group = group;
   }

   /**
    * Set of positive statuses
    */
   private static final Set<LinkStatus> positiveSet = new HashSet<>();

   /**
    * Set of negative statuses
    */
   private static final Set<LinkStatus> negativeSet = new HashSet<>();

   /**
    * Set of user driven statuses
    */
   private static final Set<LinkStatus> userDrivenSet = new HashSet<>();

   /**
    * Set of neutral statuses
    */
   private static final Set<LinkStatus> neutralSet = new HashSet<>();

   /**
    * Joined statues of positives separated by comma
    */
   private static StringBuilder joinedPositives = new StringBuilder();

   /**
    * Joined statues of negatives separated by comma
    */
   private static StringBuilder joinedNegatives = new StringBuilder();

   /**
    * Joined statues of user drivens separated by comma
    */
   private static StringBuilder joinedUserDrivens = new StringBuilder();

   /**
    * Joined statues of neutrals separated by comma
    */
   private static StringBuilder joinedNeutrals = new StringBuilder();

   /*
    * Separating all the statuses by groups
    */
   static {
      for (LinkStatus ls : values()) {
         switch (ls.group) {
            case "positive": {
               positiveSet.add(ls);
               if (joinedPositives.length() > 0)
                  joinedPositives.append(",");
               joinedPositives.append("'");
               joinedPositives.append(ls.name());
               joinedPositives.append("'");
               break;
            }
            case "negative": {
               negativeSet.add(ls);
               if (joinedNegatives.length() > 0)
                  joinedNegatives.append(",");
               joinedNegatives.append("'");
               joinedNegatives.append(ls.name());
               joinedNegatives.append("'");
               break;
            }
            case "user": {
               userDrivenSet.add(ls);
               if (joinedUserDrivens.length() > 0)
                  joinedUserDrivens.append(",");
               joinedUserDrivens.append("'");
               joinedUserDrivens.append(ls.name());
               joinedUserDrivens.append("'");
               break;
            }
            case "neutral": {
               neutralSet.add(ls);
               if (joinedNeutrals.length() > 0)
                  joinedNeutrals.append(",");
               joinedNeutrals.append("'");
               joinedNeutrals.append(ls.name());
               joinedNeutrals.append("'");
               break;
            }
         }
      }
   }

   /**
    * Finds LinkStatus by given name
    *
    * @param name
    * @return LinkStatus
    */
   public LinkStatus findByName(String name) {
      return LinkStatus.valueOf(name);
   }

   public String getGroup() {
      return group;
   }

   public boolean isPositive() {
      return "positive".equals(group);
   }

   public boolean isNegative() {
      return "negative".equals(group);
   }

   public boolean isUserDriven() {
      return "user".equals(group);
   }

   public boolean isNeutral() {
      return "neutral".equals(group);
   }

   public static StringBuilder getJoinedPositives() {
      return joinedPositives;
   }

   public static StringBuilder getJoinedNegatives() {
      return joinedNegatives;
   }

   public static StringBuilder getJoinedUserDrivens() {
      return joinedUserDrivens;
   }

   public static StringBuilder getJoinedNeutrals() {
      return joinedNeutrals;
   }

   public Set<LinkStatus> getPositiveSet() {
      return positiveSet;
   }

   public Set<LinkStatus> getNegativeSet() {
      return negativeSet;
   }

   public Set<LinkStatus> getUserDrivenSet() {
      return userDrivenSet;
   }

   public Set<LinkStatus> getNeutralSet() {
      return neutralSet;
   }

}
