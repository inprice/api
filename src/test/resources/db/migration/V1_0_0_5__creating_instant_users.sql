-- passwords are 1234
set @salted_pass = 'tgeCgsZWyabjtsslplwMrGJRHyTyI4zS4DlAWHnjrMQi2Nn9KwXBS9RROaPWK3BhIkEFtQcLK5TO3q8iihlhbg';
set @timezone = 'Europe/Istanbul';

-- creating a super user
insert into user (email, password, name, timezone, privileged) values ('super@inprice.io', @salted_pass, 'Super User', @timezone, true);


-- creating an admin user with account
set @email = 'admin@inprice.io';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into account (name, admin_id) values ('Acme in Detroit Inc.', @user_id);
set @account_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'ADMIN', 'PENDING', 'JOINED');

-- creating an editor
set @email = 'editor@inprice.io';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'EDITOR', 'PENDING', 'JOINED');

-- creating an viewer
set @email = 'viewer@inprice.io';

insert into user (email, password, name, timezone) values (@email, @salted_pass, SUBSTRING_INDEX(@email, '@', 1), @timezone);
set @user_id = last_insert_id();

insert into test.member (email, user_id, account_id, role, pre_status, status) values (@email, @user_id, @account_id, 'VIEWER', 'PENDING', 'JOINED');


-- creating a banned user without an account
insert into user (email, password, name, timezone, banned, banned_at, ban_reason) values ('banned@inprice.io', @salted_pass, 'banned', @timezone, true, now(), 'No reason!');

