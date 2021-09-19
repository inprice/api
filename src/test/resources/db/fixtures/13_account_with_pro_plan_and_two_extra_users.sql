-- -----------------------
-- @author mdpinar
-- @since 2021-07-23
-- -----------------------

set @admin_email = 'admin@workspace-m.com';
set @editor_email = 'editor@workspace-m.com';
set @viewer_email = 'viewer@workspace-m.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- editor
insert into test.user (email, password, name, timezone) values (@editor_email, @salted_pass, SUBSTRING_INDEX(@editor_email, '@', 1), @timezone);
set @editor_id = last_insert_id();

-- viewer
insert into test.user (email, password, name, timezone) values (@viewer_email, @salted_pass, SUBSTRING_INDEX(@viewer_email, '@', 1), @timezone);
set @viewer_id = last_insert_id();

-- workspace
insert into test.workspace (name, plan_id, status, subs_started_at, subs_renewal_at, user_count, admin_id) 
values ('With Pro Plan and Two Extra Users', @pro_plan_id, 'SUBSCRIBED', now(), @one_year_later, 1, @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'SUBSCRIBED');

-- membership
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');
insert into test.membership (email, user_id, workspace_id, role, status) values (@editor_email, @editor_id, @workspace_id, 'EDITOR', 'JOINED');
insert into test.membership (email, user_id, workspace_id, role, status) values (@viewer_email, @viewer_id, @workspace_id, 'VIEWER', 'JOINED');
