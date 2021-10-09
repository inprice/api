-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@workspace-c.com';
set @editor_email = 'editor@workspace-c.com'; -- will be a pending user in 12_workspace_with_pro_plan_and_two_pending_users.sql

-- -----------------------

-- admin
insert into test.user (email, password, full_name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- editor
insert into test.user (email, password, full_name, timezone) values (@editor_email, @salted_pass, SUBSTRING_INDEX(@editor_email, '@', 1), @timezone);
set @editor_id = last_insert_id();

-- workspace
insert into test.workspace (name, plan_id, status, subs_started_at, subs_renewal_at, user_count, alarm_count, admin_id) 
values ('With Starter Plan and One Extra User', @starter_plan_id, 'SUBSCRIBED', now(), @one_year_later, 1, 2, @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'SUBSCRIBED');

-- membership
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');
insert into test.membership (email, user_id, workspace_id, role, status) values (@editor_email, @editor_id, @workspace_id, 'EDITOR', 'JOINED');

-- -----------------------
-- products and links
-- product_name_addition, actives, tryings, waitings, problems, url, platform_id, workspace_name, workspace_id
-- -----------------------
call sp_create_product_and_links(null, 'X', 2, 1, 1, 0, 'https://amazon.com/', 2, 'Workspace-C', @workspace_id);
call sp_create_product_and_links(null, 'Y', 4, 1, 0, 3, 'https://ebay.com/', 12, 'Workspace-C', @workspace_id);

-- -----------------------
-- 2 alarms
-- -----------------------
insert into alarm (topic, product_id, subject, subject_when, amount_lower_limit, amount_upper_limit, workspace_id) 
select 'PRODUCT', id, 'MINIMUM', 'OUT_OF_LIMITS', 15.17, 50.23, @workspace_id from product where workspace_id = @workspace_id limit 1;

insert into alarm (topic, link_id, subject, subject_when, certain_position, workspace_id) 
select 'LINK', id, 'POSITION', 'EQUAL', 'AVERAGE', @workspace_id from link where workspace_id = @workspace_id limit 1;

-- tickets
insert into test.ticket (priority, type, subject, body, user_id, workspace_id) 
values ('NORMAL', 'SUPPORT', 'PRODUCT', 'Is there a limit for adding new products?', @admin_id, @workspace_id);

insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, workspace_id) 
values (last_insert_id(), 'OPENED', 'NORMAL', 'SUPPORT', 'PRODUCT', @admin_id, @workspace_id);

-- -----------------------

insert into test.ticket (priority, type, subject, body, user_id, workspace_id) 
values ('LOW', 'FEEDBACK', 'PAYMENT', 'You should allow us to use Amex cards.', @editor_id, @workspace_id);

insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, workspace_id) 
values (last_insert_id(), 'OPENED', 'LOW', 'FEEDBACK', 'PAYMENT', @editor_id, @workspace_id);

-- -----------------------

insert into test.ticket (status, priority, type, subject, body, user_id, workspace_id) 
values ('IN_PROGRESS', 'LOW', 'SUPPORT', 'LINK', 'Some links are not allowed to add, why?.', @editor_id, @workspace_id);
set @ticket_id = last_insert_id();

insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, workspace_id) 
values (@ticket_id, 'OPENED', 'LOW', 'SUPPORT', 'LINK', @editor_id, @workspace_id);

insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, workspace_id) 
values (@ticket_id, 'IN_PROGRESS', 'LOW', 'SUPPORT', 'LINK', @editor_id, @workspace_id);

