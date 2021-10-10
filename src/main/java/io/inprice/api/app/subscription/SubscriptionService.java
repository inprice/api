package io.inprice.api.app.subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.app.system.SystemDao;
import io.inprice.api.config.Props;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CustomerDTO;
import io.inprice.api.helpers.Commons;
import io.inprice.api.info.Response;
import io.inprice.api.publisher.EmailPublisher;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.WorkspaceStatus;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.meta.Marks;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Workspace;
import io.inprice.common.models.WorkspaceTrans;
import io.inprice.common.models.Plan;
import io.inprice.common.models.UserMarks;

class SubscriptionService {

  Response createCheckout(int planId) {
  	return Responses.METHOD_NOT_ALLOWED;
  }

  Response cancel() {
    Response res = Responses.DataProblem.SUBSCRIPTION_PROBLEM;

    try (Handle handle = Database.getHandle()) {
      WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);

      Workspace workspace = workspaceDao.findById(CurrentUser.getWorkspaceId());
      if (workspace.getStatus().isOKForCancel()) {

      	handle.begin();
      	
        SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);
        boolean isOK = subscriptionDao.terminate(workspace.getId(), WorkspaceStatus.CANCELLED);
        if (isOK) {

          WorkspaceTrans trans = new WorkspaceTrans();
          trans.setWorkspaceId(workspace.getId());
          trans.setSuccessful(Boolean.TRUE);
          trans.setDescription(("Manual cancelation."));
          
          switch (workspace.getStatus()) {
  					case VOUCHERED: {
  						trans.setEvent(SubsEvent.VOUCHER_USE_CANCELLED);
  						break;
  					}
  					case FREE: {
  						trans.setEvent(SubsEvent.FREE_USE_CANCELLED);
  						break;
  					}
  					default:
  						trans.setEvent(SubsEvent.SUBSCRIPTION_CANCELLED);
  						break;
					}

          isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());

          if (isOK) {
            isOK = 
              workspaceDao.insertStatusHistory(
                workspace.getId(),
                WorkspaceStatus.CANCELLED.name(),
                workspace.getPlanId()
              );
            if (isOK) {
              Map<String, Object> mailMap = Map.of(
              	"fullName", CurrentUser.getFullName(),
              	"workspaceName", StringUtils.isNotBlank(workspace.getTitle()) ? workspace.getTitle() : workspace.getName()
        			);
              
              EmailPublisher.publish(
          			EmailData.builder()
            			.template(EmailTemplate.FREE_WORKSPACE_CANCELLED)
            			.to(CurrentUser.getEmail())
            			.subject("Notification about your cancelled plan in inprice.")
            			.data(mailMap)
            		.build()	
          		);

              res = Responses.OK;
            }
          }
        }

        if (res.isOK())
        	handle.commit();
        else
        	handle.rollback();

      } else {
        res = Responses.Illegal.NOT_SUITABLE_FOR_CANCELLATION;
      }
    }

    if (res.isOK()) {
      res = Commons.refreshSession(CurrentUser.getWorkspaceId());
    }

    return res;
  }

  Response startFreeUse() {
    Response response = Responses.DataProblem.SUBSCRIPTION_PROBLEM;

    try (Handle handle = Database.getHandle()) {

      WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
      UserMarks um_FREE_USE = workspaceDao.findUserMarkByEmail(CurrentUser.getEmail(), Marks.FREE_USE.name());
      if (um_FREE_USE == null || um_FREE_USE.getBooleanVal().equals(Boolean.FALSE)) {

        Workspace workspace = workspaceDao.findById(CurrentUser.getWorkspaceId());
        if (workspace != null) {

          if (workspace.getStatus().isOKForFreeUse()) {
          	SystemDao planDao = handle.attach(SystemDao.class);
            SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

            Plan basicPlan = planDao.findByName("Basic Plan");

          	handle.begin();
            
            boolean isOK = 
              subscriptionDao.startFreeUseOrApplyVoucher(
                CurrentUser.getWorkspaceId(),
                WorkspaceStatus.FREE.name(),
                basicPlan.getId(),
                Props.getConfig().APP.FREE_USE_DAYS
              );

            if (isOK) {
              WorkspaceTrans trans = new WorkspaceTrans();
              trans.setWorkspaceId(CurrentUser.getWorkspaceId());
              trans.setEvent(SubsEvent.FREE_USE_STARTED);
              trans.setSuccessful(Boolean.TRUE);
              trans.setDescription(("Free subscription has been started."));
              
              isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
              if (isOK) {
                isOK = 
                  workspaceDao.insertStatusHistory(
                    workspace.getId(),
                    WorkspaceStatus.FREE.name(),
                    basicPlan.getId()
                  );
                if (um_FREE_USE == null) workspaceDao.addUserMark(CurrentUser.getEmail(), Marks.FREE_USE.name(), true);
              }
            }

            if (isOK) {
              response = Commons.refreshSession(workspaceDao, workspace.getId());
              handle.commit();
            } else {
            	handle.rollback();
            }

          } else {
            response = Responses.Illegal.NO_FREE_USE_RIGHT;
          }
        } else {
        	response = Responses.NotFound.WORKSPACE;
        }
      } else {
        response = Responses.Already.FREE_USE_USED;
      }
    }

    return response;
  }

  Response getInfo() {
    Map<String, Object> data = new HashMap<>(3);

    try (Handle handle = Database.getHandle()) {
      WorkspaceDao dao = handle.attach(WorkspaceDao.class);
      Workspace workspace = dao.findById(CurrentUser.getWorkspaceId());
      if (workspace != null) {
      	Map<String, Object> info = new HashMap<>(10);
      	info.put("title", workspace.getTitle());
      	info.put("contactName", workspace.getContactName());
      	info.put("taxId", workspace.getTaxId());
      	info.put("taxOffice", workspace.getTaxOffice());
      	info.put("address1", workspace.getAddress1());
      	info.put("address2", workspace.getAddress2());
      	info.put("postcode", workspace.getPostcode());
      	info.put("city", workspace.getCity());
      	info.put("state", workspace.getState());
      	info.put("country", workspace.getCountry());

      	data.put("info", info);
      	
        SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);
        
        List<WorkspaceTrans> allTrans = subscriptionDao.findListByWorkspaceId(CurrentUser.getWorkspaceId());
        data.put("transactions", allTrans);

        if (CollectionUtils.isNotEmpty(allTrans)) {
          List<WorkspaceTrans> invoiceTrans = new ArrayList<>();
          for (WorkspaceTrans st : allTrans) {
            if (st.getFileUrl() != null) {
              invoiceTrans.add(st);
            }
          }
          data.put("invoices", invoiceTrans);
        }
      } else {
      	return Responses.NotFound.WORKSPACE;
      }
    }

    return new Response(data);
  }

  Response saveInfo(CustomerDTO dto) {
  	Response res = Responses.NotFound.WORKSPACE;

    String problem = validateInvoiceInfo(dto);

    if (problem == null) {
      Workspace workspace = null;
      try (Handle handle = Database.getHandle()) {
        WorkspaceDao dao = handle.attach(WorkspaceDao.class);
        workspace = dao.findById(CurrentUser.getWorkspaceId());

        if (workspace != null) {
          boolean isOK = dao.update(dto, CurrentUser.getWorkspaceId());
          if (isOK) {
            res = Responses.OK;
          } else {
          	res = Responses.DataProblem.DB_PROBLEM;
          }
        }
      }
    } else {
      res = new Response(problem);
    }

    return res;
  }

  private String validateInvoiceInfo(CustomerDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getTitle())) {
      problem = "Company Name cannot be empty!";
    } else if (dto.getTitle().length() < 3 || dto.getTitle().length() > 255) {
      problem = "Company Name must be between 3 - 255 chars!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getAddress1())) {
        problem = "Address line 1 cannot be empty!";
      } else if (dto.getAddress1().length() > 255) {
      	problem = "Address line 1 can be up to 255 chars!";
      }
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getCity())) {
        problem = "City cannot be empty!";
      } else if (dto.getCity().length() < 2 || dto.getCity().length() > 50) {
        problem = "City must be between 2 - 50 chars!";
      }
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getCountry())) {
        problem = "Country cannot be empty!";
      } else if (dto.getCountry().length() < 3 || dto.getCountry().length() > 50) {
        problem = "Country must be between 3 - 50 chars!";
      }
    }
    
    if (problem == null) {
    	if (StringUtils.isNotBlank(dto.getContactName()) && dto.getContactName().length() > 50) {
    		problem = "Contact Name can be up to 50 chars!";
    	}
    }

    if (problem == null) {
    	if (StringUtils.isNotBlank(dto.getTaxId()) && dto.getTaxId().length() > 16) {
    		problem = "Tax Id can be up to 16 chars!";
    	}
    }

    if (problem == null) {
    	if (StringUtils.isNotBlank(dto.getTaxOffice()) && dto.getTaxOffice().length() > 25) {
    		problem = "Tax Office can be up to 25 chars!";
    	}
    }

    if (problem == null) {
      if (StringUtils.isNotBlank(dto.getAddress2()) && dto.getAddress2().length() > 255) {
        problem = "Address line 2 can be up to 255 chars!";
      }
    }

    if (problem == null) {
      if (StringUtils.isNotBlank(dto.getPostcode()) && dto.getPostcode().length() > 8) {
        problem = "Postcode can be up to 8 chars!";
      }
    }

    if (problem == null) {
      if (StringUtils.isNotBlank(dto.getState()) && dto.getState().length() > 50) {
        problem = "State can be up to 50 chars!";
      }
    }

    return problem;
  }

}
