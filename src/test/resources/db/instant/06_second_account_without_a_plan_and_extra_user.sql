-- -----------------------
-- @author mdpinar
-- @since 2021-07-08
-- -----------------------

set @admin_email = 'admin@account-as.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- account
insert into test.account (name, admin_id) values ('Second - Without A Plan and Extra User', @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');

-- membership
insert into test.membership (email, user_id, account_id, role, pre_status, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'PENDING', 'JOINED');

-- coupon for Basic Plan (id=10)
insert into test.coupon (code, plan_id, days, description, issuer_id) values ('MU3XF9NP', 10, 30, 'Another coupon for testing', @account_id);
