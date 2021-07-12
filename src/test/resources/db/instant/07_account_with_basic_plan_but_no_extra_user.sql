-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @planId = 10; -- Basic plan
set @admin_email = 'admin@account-b.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- account
insert into test.account (name, plan_id, status, subs_started_at, subs_renewal_at, admin_id) values ('With Basic Plan but No Extra User', @planId, 'SUBSCRIBED', now(), @one_year_later, @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');

-- membership
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');

-- -----------------------
-- groups and links
-- group_name_addition, actives, tryings, waitings, problems, url, platform_id, account_name, account_id
-- -----------------------
call sp_create_group_and_links('1', 5, 4, 3, 2, 'https://amazon.com/', 2, 'Account-B', @account_id);
call sp_create_group_and_links('2', 4, 3, 2, 1, 'https://ebay.com/', 12, 'Account-B', @account_id);
