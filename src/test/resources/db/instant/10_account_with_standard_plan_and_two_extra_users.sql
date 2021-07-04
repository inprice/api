-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @planId = 20; -- Standard plan
set @admin_email = 'admin@account-f.com';
set @editor_email = 'editor@account-f.com';
set @viewer_email = 'viewer@account-f.com';
set @salted_pass = 'tgeCgsZWyabjtsslplwMrGJRHyTyI4zS4DlAWHnjrMQi2Nn9KwXBS9RROaPWK3BhIkEFtQcLK5TO3q8iihlhbg';

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), 'Europe/Istanbul');
set @admin_id = last_insert_id();

-- editor
insert into test.user (email, password, name, timezone) values (@editor_email, @salted_pass, SUBSTRING_INDEX(@editor_email, '@', 1), 'Europe/Istanbul');
set @editor_id = last_insert_id();

-- viewer
insert into test.user (email, password, name, timezone) values (@viewer_email, @salted_pass, SUBSTRING_INDEX(@viewer_email, '@', 1), 'Europe/Istanbul');
set @viewer_id = last_insert_id();

-- account
insert into test.account (name, plan_id, user_count, status, subs_started_at, subs_renewal_at, admin_id) values ('With Standard Plan and Two Extra Users', @planId, 2, 'SUBSCRIBED', now(), '2050-01-01 23:59:59', @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');

-- membership
insert into test.member (email, user_id, account_id, role, pre_status, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'PENDING', 'JOINED');
insert into test.member (email, user_id, account_id, role, pre_status, status) values (@editor_email, @editor_id, @account_id, 'EDITOR', 'PENDING', 'JOINED');
insert into test.member (email, user_id, account_id, role, pre_status, status) values (@viewer_email, @viewer_id, @account_id, 'VIEWER', 'PENDING', 'JOINED');
