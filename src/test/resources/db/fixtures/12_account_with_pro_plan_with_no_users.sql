-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@workspace-g.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- workspace
insert into test.workspace (name, plan_id, status, subs_started_at, subs_renewal_at, admin_id) 
values ('With Pro Plan and No User', @pro_plan_id, 'SUBSCRIBED', now(), @one_year_later, @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'SUBSCRIBED');

-- memberships
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');

-- workspace transactions
insert into test.workspace_trans (workspace_id, event_id, event, reason)
values (@workspace_id, 'MA-002', 'SUBSCRIPTION', 'The first trial for renewal.');

insert into test.workspace_trans (workspace_id, event_id, event, successful, description)
values (@workspace_id, 'MM-004', 'SUBSCRIPTION', true, 'Subscription has been renewed successfully');

-- -----------------------
-- 2 products and 4 links
-- product_name_addition, actives, tryings, waitings, problems, url, platform_id, workspace_name, workspace_id
-- -----------------------
call sp_create_product_and_links(null, 'A', 1, 0, 0, 1, 'https://amazon.com/', 2, 'Workspace-G', @workspace_id);
call sp_create_product_and_links(null, 'B', 1, 0, 0, 1, 'https://ebay.com/', 12, 'Workspace-G', @workspace_id);
