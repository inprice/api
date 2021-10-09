-- -----------------------
-- @author mdpinar
-- @since 2021-07-08
-- -----------------------

-- this user has used his only FREE USE right (see below)
set @admin_email = 'admin@workspace-as.com';

-- -----------------------

-- admin
insert into test.user (email, password, full_name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- workspace
insert into test.workspace (name, admin_id) values ('Second - Without A Plan and Extra User', @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');

-- membership
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');

-- voucher for Basic and Standard Plan
insert into test.voucher (code, plan_id, days, description, issuer_id) values ('MU3XF9NP', @basic_plan_id, 30, 'Another voucher for testing', @workspace_id);
insert into test.voucher (code, plan_id, days, description, issued_id) values ('KJ9QF6G7', @standard_plan_id, 30, 'Assigned to the second workspace', @workspace_id);

-- announces
insert into test.announce (type, level, title, body, starting_at, ending_at, user_id, workspace_id) 
values ('USER', 'WARNING', 'Here is another good news for someone else', 'This is another test announce for the admin user of Workspace-AS', now(), @one_month_later, @admin_id, @workspace_id);

-- adding free use right
insert into test.user_marks (email, mark) values (@admin_email, 'FREE_USE');
