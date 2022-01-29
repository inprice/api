-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@workspace-e.com';
set @editor_email = 'editor@workspace-e.com';

-- -----------------------

-- admin
insert into test.user (email, password, full_name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- editor
insert into test.user (email, password, full_name, timezone) values (@editor_email, @salted_pass, SUBSTRING_INDEX(@editor_email, '@', 1), @timezone);
set @editor_id = last_insert_id();

-- workspace
insert into test.workspace (name, plan_id, status, subs_started_at, subs_renewal_at, user_count, admin_id) 
values ('Second - Professional Plan and One Extra User', @professional_plan_id, 'SUBSCRIBED', now(), @one_year_later, 1, @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'SUBSCRIBED');

-- membership
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');
insert into test.membership (email, user_id, workspace_id, role, status) values (@editor_email, @editor_id, @workspace_id, 'EDITOR', 'JOINED');

-- -----------------------
-- 2 products and 11 links
-- product_name_addition, actives, tryings, waitings, problems, url, platform_id, workspace_name, workspace_id
-- -----------------------
call sp_create_product_and_links('DX-001', 'R', 5, 2, 1, 2, 'https://amazon.com/', 2, 'Workspace-E', @workspace_id);
call sp_create_product_and_links('DX-002', 'S', 0, 0, 1, 0, 'https://gittigidiyor.com', 83, 'Workspace-E', @workspace_id);

-- tickets
-- -----------------------

-- ticket
insert into test.ticket (priority, type, subject, body, comment_count, user_id, workspace_id) 
values ('LOW', 'PROBLEM', 'WORKSPACE', 'This ticket is opened by and editor and has one editable comment!', 1, @admin_id, @workspace_id);
set @ticket_id = last_insert_id();

-- history
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, workspace_id) 
values (@ticket_id, 'OPENED', 'NORMAL', 'SUPPORT', 'OTHER', @admin_id, @workspace_id);

-- comment
insert into test.ticket_comment (ticket_id, body, user_id, workspace_id) 
values (@ticket_id, 'I am adding this comment just because I am boring as hell.', @editor_id, @workspace_id);
