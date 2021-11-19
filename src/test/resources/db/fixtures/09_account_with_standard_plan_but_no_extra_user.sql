-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@workspace-d.com';

-- -----------------------

-- admin
insert into test.user (email, password, full_name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- workspace
insert into test.workspace (name, plan_id, status, subs_started_at, subs_renewal_at, alarm_count, admin_id) 
values ('With Standard Plan (Vouchered) but No Extra User', @standard_plan_id, 'VOUCHERED', now(), @one_year_later, 2, @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'VOUCHERED');

-- workspace transaction
insert into test.workspace_trans (workspace_id, event_id, event, reason)
values (@workspace_id, 'SY-12A', 'VOUCHERED', 'Voucher use: AS34FGD3');

-- membership
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');

-- -----------------------
-- products and links
-- product_name_addition, actives, tryings, waitings, problems, url, platform_id, workspace_name, workspace_id
-- -----------------------
call sp_create_product_and_links('CX-001', 'D', 1, 0, 0, 0, 'https://hepsiburada.com/', 84, 'Workspace-D', @workspace_id);

-- -----------------------
-- 2 alarms definitions for 2 entities (1 product and 1 link)
-- -----------------------
insert into alarm (name, topic, subject, subject_when, workspace_id) 
values ('Product minimum price is increased', 'PRODUCT', 'MINIMUM', 'INCREASED', @workspace_id);
update product set alarm_id=last_insert_id() where sku = 'CX-001';

insert into alarm (name, topic, subject, subject_when, workspace_id) 
values ('Link price is decreased', 'LINK', 'POSITION', 'EQUAL', @workspace_id);
update link set alarm_id=last_insert_id() where grup = 'ACTIVE' and workspace_id = @workspace_id;
