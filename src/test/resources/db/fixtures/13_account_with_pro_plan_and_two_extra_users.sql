-- -----------------------
-- @author mdpinar
-- @since 2021-07-23
-- -----------------------

set @planId = 25; -- Pro plan
set @admin_email = 'admin@account-m.com';
set @editor_email = 'editor@account-m.com';
set @viewer_email = 'viewer@account-m.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- editor
insert into test.user (email, password, name, timezone) values (@editor_email, @salted_pass, SUBSTRING_INDEX(@editor_email, '@', 1), @timezone);
set @editor_id = last_insert_id();

-- viewer
insert into test.user (email, password, name, timezone) values (@viewer_email, @salted_pass, SUBSTRING_INDEX(@viewer_email, '@', 1), @timezone);
set @viewer_id = last_insert_id();

-- account
insert into test.account (name, plan_id, status, subs_started_at, subs_renewal_at, user_count, admin_id) 
values ('With Pro Plan and Two Extra Users', @planId, 'SUBSCRIBED', now(), @one_year_later, 1, @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');

-- membership
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');
insert into test.membership (email, user_id, account_id, role, status) values (@editor_email, @editor_id, @account_id, 'EDITOR', 'JOINED');
insert into test.membership (email, user_id, account_id, role, status) values (@viewer_email, @viewer_id, @account_id, 'VIEWER', 'JOINED');
