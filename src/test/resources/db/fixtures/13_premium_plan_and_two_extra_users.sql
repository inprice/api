-- -----------------------
-- @author mdpinar
-- @since 2021-07-23
-- -----------------------

set @admin_email = 'admin@workspace-m.com';
set @editor_email = 'editor@workspace-m.com';
set @viewer_email = 'viewer@workspace-m.com';

-- -----------------------

-- admin
insert into test.user (email, password, full_name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- editor
insert into test.user (email, password, full_name, timezone) values (@editor_email, @salted_pass, SUBSTRING_INDEX(@editor_email, '@', 1), @timezone);
set @editor_id = last_insert_id();

-- viewer
insert into test.user (email, password, full_name, timezone) values (@viewer_email, @salted_pass, SUBSTRING_INDEX(@viewer_email, '@', 1), @timezone);
set @viewer_id = last_insert_id();

-- workspace
insert into test.workspace (name, plan_id, status, subs_started_at, subs_renewal_at, user_count, admin_id) 
values ('Second - Premium Plan and Two Extra Users', @premium_plan_id, 'SUBSCRIBED', now(), @one_year_later, 1, @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'SUBSCRIBED');

-- membership
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');
insert into test.membership (email, user_id, workspace_id, role, status) values (@editor_email, @editor_id, @workspace_id, 'EDITOR', 'JOINED');
insert into test.membership (email, user_id, workspace_id, role, status) values (@viewer_email, @viewer_id, @workspace_id, 'VIEWER', 'JOINED');

-- brands
-- -----------------------
insert into test.brand (name, workspace_id) values ('NO BRAND', @workspace_id);
insert into test.brand (name, workspace_id) values ('LEVIS', @workspace_id);
insert into test.brand (name, workspace_id) values ('GREEN DOT', @workspace_id);
insert into test.brand (name, workspace_id) values ('GREYDER', @workspace_id);

-- Categories
-- -----------------------
insert into test.category (name, workspace_id) values ('COMPUTERS', @workspace_id);
insert into test.category (name, workspace_id) values ('GARDENING', @workspace_id);
insert into test.category (name, workspace_id) values ('ACCESSORIES', @workspace_id);
insert into test.category (name, workspace_id) values ('ACCUMULATORS', @workspace_id);

-- -----------------------
-- 2 smart prices
-- -----------------------
insert into smart_price (name, formula, lower_limit_formula, upper_limit_formula, workspace_id) 
values ('Special Formula', '(a*1.10)', 'min(i,a)', 'max(p,x)', @workspace_id);

insert into smart_price (name, formula, lower_limit_formula, upper_limit_formula, workspace_id) 
values ('Secret Formula', '(p*1.05)', 'min(i,a)/2', 'max(p,x)+0.30', @workspace_id);
