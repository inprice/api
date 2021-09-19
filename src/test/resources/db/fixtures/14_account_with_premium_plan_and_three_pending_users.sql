-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@workspace-h.com';
set @editor_email = 'editor@workspace-h.com'; -- not exists as a user!
set @viewer_email = 'editor@workspace-c.com'; -- exists in 08_workspace_with_starter_plan_and_one_extra_user

-- this is an important line to consider!
-- the email is already defined in 10_workspace_with_standard_plan_with_one_extra_user.sql as EDITOR
-- here, we are binding him to this workspace as VIEWER
-- so that we can test one email with multiple workspaces case!
set @other_viewer_email = 'editor@workspace-e.com';
-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- viewer
select id into @viewer_id from test.user where email=@viewer_email;

-- other viewer
select id into @other_viewer_id from test.user where email=@other_viewer_email;

-- workspace
insert into test.workspace (name, plan_id, user_count, status, subs_started_at, subs_renewal_at, admin_id) 
values ('With Premium Plan and Two Pending Users', @premium_plan_id, 3, 'SUBSCRIBED', now(), @one_year_later, @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'SUBSCRIBED');

-- memberships
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');
insert into test.membership (email, workspace_id) values (@editor_email,  @workspace_id); -- not exists!
insert into test.membership (email, user_id, workspace_id, role) values (@viewer_email, @viewer_id, @workspace_id, 'VIEWER');
insert into test.membership (email, user_id, workspace_id, role) values (@other_viewer_email, @other_viewer_id, @workspace_id, 'VIEWER');
