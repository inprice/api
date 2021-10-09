-- -----------------------
-- @author mdpinar
-- @since 2021-07-09
-- -----------------------

set @admin_email = 'admin@workspace-i.com';

-- -----------------------

-- admin
insert into test.user (email, password, full_name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- workspace
insert into test.workspace (name, plan_id, status, admin_id) values ('Cancelled -Basic Plan- No link, No alarm', @basic_plan_id, 'CANCELLED', @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'SUBSCRIBED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CANCELLED');

-- membership
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');
