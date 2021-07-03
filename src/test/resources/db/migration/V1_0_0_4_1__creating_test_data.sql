-- passwords are 1234
set @salted_pass = 'tgeCgsZWyabjtsslplwMrGJRHyTyI4zS4DlAWHnjrMQi2Nn9KwXBS9RROaPWK3BhIkEFtQcLK5TO3q8iihlhbg';
set @timezone = 'Europe/Istanbul';

-- creating a super user
insert into user (email, password, name, timezone, privileged) values ('super@inprice.io', @salted_pass, 'Super User', @timezone, true);

-- creating a banned user without an account
insert into user (email, password, name, timezone, banned, banned_at, ban_reason) values ('banned@inprice.io', @salted_pass, 'banned', @timezone, true, now(), 'No reason!');

-- ---------------------------------------------------------
-- Acme X account and its USERS
-- ---------------------------------------------------------
set @email = 'admin@acme-x.com';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into account (name, admin_id) values ('Acme X Inc.', @user_id);
set @account_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'ADMIN', 'PENDING', 'JOINED');

-- creating an editor
set @email = 'editor@acme-x.com';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'EDITOR', 'PENDING', 'JOINED');

-- creating a viewer
set @email = 'viewer@acme-x.com';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'VIEWER', 'PENDING', 'JOINED');

-- ---------------------------------------------------------
-- Acme Y account and its USERS
-- ---------------------------------------------------------
set @email = 'admin@acme-y.com';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into account (name, admin_id) values ('Acme Y Inc.', @user_id);
set @account_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'ADMIN', 'PENDING', 'JOINED');

-- creating an editor
set @email = 'editor@acme-y.com';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'EDITOR', 'PENDING', 'JOINED');

-- creating a viewer
set @email = 'viewer@acme-y.com';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'VIEWER', 'PENDING', 'JOINED');

-- ---------------------------------------------------------
-- Acme Z account (with Basic plan) and its USERS
-- ---------------------------------------------------------
set @email = 'admin@acme-z.com';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into account (name, admin_id, plan_id) values ('Acme Z Inc.', @user_id, 10);
set @account_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'ADMIN', 'PENDING', 'JOINED');

-- creating an editor
set @email = 'editor@acme-z.com';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'EDITOR', 'PENDING', 'JOINED');

-- creating a viewer
set @email = 'viewer@acme-z.com';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'VIEWER', 'PENDING', 'JOINED');

-- ---------------------------------------------------------
-- Acme S account (with Starter plan) and its Admin user
-- ---------------------------------------------------------
set @email = 'admin@acme-s.com';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into account (name, admin_id, user_count, plan_id) values ('Acme S Inc.', @user_id, 1, 20);
set @account_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'ADMIN', 'PENDING', 'JOINED');

-- creating an editor
set @email = 'editor@acme-s.com';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'EDITOR', 'PENDING', 'JOINED');

-- ---------------------------------------------------------

