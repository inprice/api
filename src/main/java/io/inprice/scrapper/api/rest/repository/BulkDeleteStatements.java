package io.inprice.scrapper.api.rest.repository;

import io.inprice.scrapper.api.rest.component.Context;

import java.util.Arrays;

class BulkDeleteStatements {

    String[] linksByLinkIdId(Long linkId) {
        return links(null, null, linkId);
    }

    private String[] linksByWorkspaceId(Long workspaceId) {
        return links(workspaceId, null, null);
    }

    private String[] linksByProductId(Long productId) {
        return links(null, productId, null);
    }

    /**
     * Generates necessary delete statements for links
     *
     * @return generated just delete statements
     */
    private String[] links(Long workspaceId, Long productId, Long linkId) {
        final String companyPart = " and company_id=" + Context.getCompanyId();
        String workspacePart = " and workspace_id=" + Context.getWorkspaceId();

        String where = "";
        String where_1 = "where 1=1";

        //delete by workspaceId
        if (workspaceId != null && productId == null && linkId == null) {
            where = "where workspace_id=" + workspaceId;
            workspacePart = "";
        }

        //delete by productId
        if (productId != null && linkId == null) {
            where = "where product_id=" + productId;
        }

        //delete by linkId
        if (linkId != null) {
            where = "where link_id=" + linkId;
            where_1 = "where id=" + linkId;
        }

        return new String[] {
            "delete from link_price " + where + workspacePart + companyPart,
            "delete from link_history " + where + workspacePart + companyPart,
            "delete from link_spec " + where + workspacePart + companyPart,
            "delete from link " + where_1 + workspacePart + companyPart
        };
    }

    String[] productsByProductId(Long productId) {
        return products(null, productId);
    }

    private String[] productsByWorkspaceId(Long workspaceId) {
        return products(workspaceId, null);
    }

    /**
     * Generates necessary delete statements for products
     *
     * @return generated just delete statements
     */
    private String[] products(Long workspaceId, Long productId) {
        final String companyPart = " and company_id=" + Context.getCompanyId();
        String workspacePart = " and workspace_id=" + Context.getWorkspaceId();

        String where = "";
        String where_1 = "where 1=1";

        String[] linkDeletions = null;

        //delete by workspaceId
        if (workspaceId != null && productId == null) {
            where = "where workspace_id=" + workspaceId;
            workspacePart = "";
            linkDeletions = linksByWorkspaceId(workspaceId);
        }

        //delete by productId
        if (productId != null) {
            where = "where product_id=" + productId;
            where_1 = "where id=" + productId;
            linkDeletions = linksByProductId(productId);
        }

        String[] productDeletions = {
            "delete from product_price " + where + workspacePart + companyPart,
            "delete from product " + where_1 + workspacePart + companyPart
        };

        return concatenate(linkDeletions, productDeletions);
    }

    /**
     * Generates necessary delete statements for workspaces
     *
     * @return generated just delete statements
     */
    String[] workspaces(Long workspaceId) {
        if (workspaceId == null) return null;

        String[] productDeletions = productsByWorkspaceId(workspaceId);

        String[] workspaceDeletions = {
            String.format(
            "delete from import_product_row where workspace_id=%d and company_id=%d",
                workspaceId, Context.getCompanyId()
            ),
            String.format(
            "delete from import_product where workspace_id=%d and company_id=%d",
                workspaceId, Context.getCompanyId()
            ),
            String.format(
            "delete from workspace_history where workspace_id=%d and company_id=%d",
                workspaceId, Context.getCompanyId()
            ),
            String.format(
            "delete from workspace where id=%d and company_id=%d",
                workspaceId, Context.getCompanyId()
            )
        };

        return concatenate(productDeletions, workspaceDeletions);
    }

    private String[] concatenate(String[] first, String[] second) {
        String[] both = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, both, first.length, second.length);
        return both;
    }

}
