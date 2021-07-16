-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@account-a.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- account
insert into test.account (name, admin_id) values ('Without A Plan and Extra User', @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');

-- membership
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');

-- coupon for Basic Plan (id=10)
insert into test.coupon (code, plan_id, days, description, issuer_id) values ('RB5QV6CF', 10, 30, 'Given for testing', @account_id);

-- announces
insert into test.announce (type, level, title, body, starting_at, ending_at, user_id, account_id) 
values ('USER', 'INFO', 'Here is a good news for you', 'This is a test announce for the admin user of Account-A', now(), @one_month_later, @admin_id, @account_id);

insert into test.announce (type, level, title, body, starting_at, ending_at, account_id) 
values ('ACCOUNT', 'INFO', 'A kind reminder for your usage', 'Please consider to pick a broader plan since your link count has reached its limit!', now(), @one_month_later,  @account_id);

-- tickets
insert into test.ticket (priority, type, subject, body, user_id, account_id) 
values ('NORMAL', 'FEEDBACK', 'COUPON', 'Are you planning to give free coupons out for a special time periods like Christmas.', @admin_id, @account_id);

insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, account_id) 
values (last_insert_id(), 'OPENED', 'NORMAL', 'FEEDBACK', 'COUPON', @admin_id, @account_id);

-- -----------------------

insert into test.ticket (priority, type, subject, body, user_id, account_id) 
values ('CRITICAL', 'PROBLEM', 'ACCOUNT', 'Login form doesnt allowe me to sign in. So I cannot track my links for two hours.', @admin_id, @account_id);

insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, account_id) 
values (last_insert_id(),'OPENED', 'CRITICAL', 'PROBLEM', 'ACCOUNT', @admin_id, @account_id);
