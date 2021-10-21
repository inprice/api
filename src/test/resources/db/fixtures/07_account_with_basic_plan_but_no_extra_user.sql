-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@workspace-b.com';

-- -----------------------

-- admin
insert into test.user (email, password, full_name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- workspace
insert into test.workspace (name, plan_id, status, subs_started_at, subs_renewal_at, alarm_count, admin_id) 
values ('With Basic Plan (Free Use) but No Extra User', @basic_plan_id, 'FREE', now(), @one_year_later, 5, @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'FREE');

-- workspace transaction
insert into test.workspace_trans (workspace_id, event_id, event, reason)
values (@workspace_id, 'SY-12A', 'FREE USE', 'Free use started.');

-- membership
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');

-- -----------------------
-- 2 products and 24 links
-- product_name_addition, actives, tryings, waitings, problems, url, platform_id, workspace_name, workspace_id
-- -----------------------
call sp_create_product_and_links(null, '1', 5, 4, 3, 2, 'https://amazon.com/', 2, 'Workspace-B', @workspace_id);
call sp_create_product_and_links(null, '2', 4, 3, 2, 1, 'https://ebay.com/', 12, 'Workspace-B', @workspace_id);

-- -----------------------
-- 5 alarms
-- -----------------------
insert into alarm (topic, product_id, subject, subject_when, workspace_id) 
select 'PRODUCT', id, 'POSITION', 'CHANGED', @workspace_id from product where workspace_id = @workspace_id limit 2;

insert into alarm (topic, link_id, subject, subject_when, workspace_id) 
select 'LINK', id, 'POSITION', 'CHANGED', @workspace_id from link where workspace_id = @workspace_id limit 3;

-- -----------------------
-- 1 smart price
-- -----------------------
insert into smart_price (name, formula, lower_limit_formula, upper_limit_formula, workspace_id) 
values ('Base Formula', 'min((p*1.10)+0.75,a)', '(i-(i*10/100))', 'a+1.50', @workspace_id);
