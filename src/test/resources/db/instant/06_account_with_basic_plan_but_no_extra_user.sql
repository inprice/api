-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @planId = 10; -- Basic plan
set @admin_email = 'admin@account-b.com';
set @salted_pass = 'tgeCgsZWyabjtsslplwMrGJRHyTyI4zS4DlAWHnjrMQi2Nn9KwXBS9RROaPWK3BhIkEFtQcLK5TO3q8iihlhbg';

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), 'Europe/Istanbul');
set @admin_id = last_insert_id();

-- account
insert into test.account (name, plan_id, status, subs_started_at, subs_renewal_at, admin_id) values ('With Basic Plan but No Extra User', @planId, 'SUBSCRIBED', now(), '2050-01-01 23:59:59', @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');

-- membership
insert into test.member (email, user_id, account_id, role, pre_status, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'PENDING', 'JOINED');
