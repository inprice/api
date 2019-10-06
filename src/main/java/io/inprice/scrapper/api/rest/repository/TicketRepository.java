package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.dto.TicketDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.common.meta.TicketSource;
import io.inprice.scrapper.common.meta.TicketStatus;
import io.inprice.scrapper.common.meta.TicketType;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TicketRepository {

    private static final Logger log = LoggerFactory.getLogger(TicketRepository.class);
    private final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public ServiceResponse<Ticket> getList(TicketSource source, Long id) {
        List<Ticket> tickets = dbUtils.findMultiple(
            String.format(
                "select * from ticket " +
                "where %s = %d " +
                "order by created_at", findField(source), id
            ), this::map);

        if (tickets != null && tickets.size() > 0) {
            return new ServiceResponse<>(tickets);
        }
        return Responses.NotFound.TICKET;
    }

    public ServiceResponse insert(TicketDTO ticketDTO) {
        Connection con = null;
        try {
            con = dbUtils.getTransactionalConnection();

            final String field = findField(ticketDTO.getSource());
            final boolean isSystemUser = UserType.SYSTEM.equals(Context.getAuthUser().getType());

            final String query =
                    "insert into ticket " +
                    "(source, ticket_type, status, description, workspace_id, company_id, " + field + ") " +
                    " values " +
                    "(?, ?, ?, ?, ?, ?) ";

            try (PreparedStatement pst = con.prepareStatement(query)) {
                int i = 0;
                pst.setString(++i, ticketDTO.getSource().name());
                pst.setString(++i, ticketDTO.getType().name());

                if (isSystemUser)
                    pst.setString(++i, TicketStatus.ANSWERED.name());
                else
                    pst.setString(++i, TicketStatus.NEW.name());

                pst.setString(++i, ticketDTO.getDescription());
                pst.setLong(++i, Context.getWorkspaceId());
                pst.setLong(++i, Context.getCompanyId());
                pst.setLong(++i, ticketDTO.getSourceId());

                final boolean res = (pst.executeUpdate() > 0);

                //if it is an answer to a ticket
                if (res && isSystemUser) {
                    try (PreparedStatement
                         pst1 =
                             con.prepareStatement(
                             "update ticket set status=? " +
                                    "where status=? " +
                                     "  and source=? " +
                                     "  and " + field + "=? " +
                                     "  and workspace_id=? " +
                                     "  and company_id=? ")) {
                        int j = 0;
                        pst.setString(++j, TicketStatus.ANSWERED.name());
                        pst.setString(++j, TicketStatus.NEW.name());
                        pst.setString(++j, ticketDTO.getSource().name());
                        pst.setLong(++j, ticketDTO.getSourceId());
                        pst.setLong(++j, Context.getWorkspaceId());
                        pst.setLong(++j, Context.getCompanyId());

                        if (pst1.executeUpdate() < 1) {
                            log.warn("Previous ticket's of a ticket couldn't be answered by system user! " + ticketDTO.getSourceId());
                        }
                    }
                }

                if (res) {
                    dbUtils.commit(con);
                    return Responses.OK;
                } else {
                    dbUtils.rollback(con);
                    return Responses.DataProblem.DB_PROBLEM;
                }
            }
        } catch (SQLException e) {
            if (con != null) dbUtils.rollback(con);
            log.error("Failed to insert a new ticket" + ticketDTO.toString(), e);
            return Responses.ServerProblem.EXCEPTION;
        } finally {
            if (con != null) dbUtils.close(con);
        }
    }

    public ServiceResponse update(TicketDTO ticketDTO) {
        try (Connection con = dbUtils.getConnection();
             PreparedStatement pst = con.prepareStatement("update ticket set description=? where id=? and company_id=?")) {

            int i = 0;
            pst.setString(++i, ticketDTO.getDescription());
            pst.setLong(++i, ticketDTO.getId());
            pst.setLong(++i, Context.getCompanyId());

            if (pst.executeUpdate() > 0)
                return Responses.OK;
            else
                return Responses.NotFound.TICKET;

        } catch (SQLException sqle) {
            log.error("Failed to update ticket", sqle);
            return Responses.ServerProblem.EXCEPTION;
        }
    }

    public ServiceResponse deleteById(Long id) {
        boolean result = dbUtils.executeQuery(
            String.format(
                "delete from ticket where id=%d and status='%s' and company_id=%d",
                id, TicketStatus.NEW, Context.getCompanyId()
            ),"Failed to delete ticket. Id: " + id
        );

        if (result) {
            return Responses.OK;
        }
        return Responses.NotFound.TICKET;
    }

    private String findField(TicketSource source) {
        return source.name().toLowerCase() + "_id";
    }

    private Ticket map(ResultSet rs) {
        try {
            Ticket model = new Ticket();
            model.setId(rs.getLong("id"));
            model.setStatus(TicketStatus.valueOf(rs.getString("status")));
            model.setSource(TicketSource.valueOf(rs.getString("source")));
            model.setType(TicketType.valueOf(rs.getString("ticket_type")));
            model.setDescription(rs.getString("description"));
            model.setLinkId(rs.getLong("link_id"));
            model.setProductId(rs.getLong("product_id"));
            model.setWorkspaceId(rs.getLong("workspace_id"));
            model.setCompanyId(rs.getLong("company_id"));
            model.setUserId(rs.getLong("user_id"));
            model.setCreatedAt(rs.getDate("created_at"));

            return model;
        } catch (SQLException e) {
            log.error("Failed to set ticket's properties", e);
        }
        return null;
    }

}
