-- -----------------------
-- @author mdpinar
-- @since 2021-07-09
-- -----------------------

set @planId = 10; -- Basic plan
set @admin_email = 'admin@account-i.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- account
insert into test.account (name, plan_id, status, admin_id) values ('Cancelled -Basic Plan- No link, No alarm', @planId, 'CANCELLED', @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');
insert into test.account_history (account_id, status) values (@account_id, 'CANCELLED');

-- membership
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');
