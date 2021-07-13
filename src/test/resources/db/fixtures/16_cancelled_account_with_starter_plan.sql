-- -----------------------
-- @author mdpinar
-- @since 2021-07-09
-- -----------------------

set @planId = 15; -- Starter plan
set @admin_email = 'admin@account-j.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- account
insert into test.account (name, plan_id, status, link_count, alarm_count, admin_id) values ('Cancelled -Starter Plan- 30 links, 6 alarms', @planId, 'CANCELLED', 30, 6, @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');
insert into test.account_history (account_id, status) values (@account_id, 'CANCELLED');

-- membership
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');
