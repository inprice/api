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
