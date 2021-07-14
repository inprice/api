-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @planId = 15; -- Starter plan
set @admin_email = 'admin@account-c.com';
set @editor_email = 'editor@account-c.com'; -- will be a pending user in 12_account_with_pro_plan_and_two_pending_users.sql

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- editor
insert into test.user (email, password, name, timezone) values (@editor_email, @salted_pass, SUBSTRING_INDEX(@editor_email, '@', 1), @timezone);
set @editor_id = last_insert_id();

-- account
insert into test.account (name, plan_id, user_count, status, subs_started_at, subs_renewal_at, admin_id) values ('With Starter Plan and One Extra User', @planId, 1, 'SUBSCRIBED', now(), @one_year_later, @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');

-- membership
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');
insert into test.membership (email, user_id, account_id, role, status) values (@editor_email, @editor_id, @account_id, 'EDITOR', 'JOINED');

-- -----------------------
-- groups and links
-- group_name_addition, actives, tryings, waitings, problems, url, platform_id, account_name, account_id
-- -----------------------
call sp_create_group_and_links('X', 2, 1, 1, 0, 'https://amazon.com/', 2, 'Account-C', @account_id);
call sp_create_group_and_links('Y', 4, 1, 0, 3, 'https://ebay.com/', 12, 'Account-C', @account_id);

-- -----------------------
-- 2 alarms
-- -----------------------
insert into alarm (topic, group_id, subject, subject_when, amount_lower_limit, amount_upper_limit, account_id) 
select 'GROUP', id, 'MINIMUM', 'OUT_OF_LIMITS', 15.17, 50.23, @account_id from link_group where account_id = @account_id limit 1;

insert into alarm (topic, link_id, subject, subject_when, certain_status, account_id) 
select 'LINK', id, 'STATUS', 'EQUAL', 'AVERAGE', @account_id from link where account_id = @account_id limit 1;
