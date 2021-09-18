-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@account-g.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- account
insert into test.account (name, plan_id, status, subs_started_at, subs_renewal_at, admin_id) 
values ('With Pro Plan and No User', @pro_plan_id, 'SUBSCRIBED', now(), @one_year_later, @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');

-- memberships
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');

-- account transactions
insert into test.account_trans (account_id, event_id, event, reason)
values (@account_id, 'MA-002', 'SUBSCRIPTION', 'The first trial for renewal.');

insert into test.account_trans (account_id, event_id, event, successful, description)
values (@account_id, 'MM-004', 'SUBSCRIPTION', true, 'Subscription has been renewed successfully');

-- -----------------------
-- 2 products and 4 links
-- product_name_addition, actives, tryings, waitings, problems, url, platform_id, account_name, account_id
-- -----------------------
call sp_create_product_and_links('A', 1, 0, 0, 1, 'https://amazon.com/', 2, 'Account-G', @account_id);
call sp_create_product_and_links('B', 1, 0, 0, 1, 'https://ebay.com/', 12, 'Account-G', @account_id);
