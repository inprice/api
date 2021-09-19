-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@workspace-d.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- workspace
insert into test.workspace (name, plan_id, status, subs_started_at, subs_renewal_at, alarm_count, admin_id) 
values ('With Standard Plan (Couponed) but No Extra User', @standard_plan_id, 'COUPONED', now(), @one_year_later, 2, @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'COUPONED');

-- workspace transaction
insert into test.workspace_trans (workspace_id, event_id, event, reason)
values (@workspace_id, 'SY-12A', 'COUPONED', 'Coupon use: AS34FGD3');

-- membership
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');

-- -----------------------
-- products and links
-- product_name_addition, actives, tryings, waitings, problems, url, platform_id, workspace_name, workspace_id
-- -----------------------
call sp_create_product_and_links('D', 1, 0, 0, 0, 'https://hepsiburada.com/', 84, 'Workspace-D', @workspace_id);

-- -----------------------
-- 2 alarms
-- -----------------------
insert into alarm (topic, product_id, subject, subject_when, workspace_id) 
select 'PRODUCT', id, 'TOTAL', 'INCREASED', @workspace_id from product where workspace_id = @workspace_id limit 1;

insert into alarm (topic, link_id, subject, subject_when, workspace_id) 
select 'LINK', id, 'PRICE', 'DECREASED', @workspace_id from link where workspace_id = @workspace_id limit 1;
