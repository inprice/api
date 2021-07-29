-- -----------------------
-- @author mdpinar
-- @since 2021-07-08
-- -----------------------

-- this user has used his only FREE USE right (see below)
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
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');

-- coupon for Basic and Standard Plan
insert into test.coupon (code, plan_id, days, description, issuer_id) values ('MU3XF9NP', @basic_plan_id, 30, 'Another coupon for testing', @account_id);
insert into test.coupon (code, plan_id, days, description, issued_id) values ('KJ9QF6G7', @standard_plan_id, 30, 'Assigned to the second account', @account_id);

-- announces
insert into test.announce (type, level, title, body, starting_at, ending_at, user_id, account_id) 
values ('USER', 'WARNING', 'Here is another good news for someone else', 'This is another test announce for the admin user of Account-AS', now(), @one_month_later, @admin_id, @account_id);

-- adding free use right
insert into test.user_mark (email, type) values (@admin_email, 'FREE_USE');
