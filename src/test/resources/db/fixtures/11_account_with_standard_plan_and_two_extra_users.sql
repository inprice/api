-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @planId = 20; -- Standard plan
set @admin_email = 'admin@account-f.com';
set @editor_email = 'editor@account-f.com';

-- this is an important line to consider!
-- the email is already defined in 10_account_with_standard_plan_with_one_extra_user.sql as EDITOR
-- here, we are binding him to this account as VIEWER
-- so that we can test one email with multiple accounts case!
set @viewer_email = 'editor@account-e.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- editor
insert into test.user (email, password, name, timezone) values (@editor_email, @salted_pass, SUBSTRING_INDEX(@editor_email, '@', 1), @timezone);
set @editor_id = last_insert_id();

-- viewer
select id into @viewer_id from test.user where email=@viewer_email;

-- account
insert into test.account (name, plan_id, user_count, status, subs_started_at, subs_renewal_at, admin_id) values ('With Standard Plan and Two Extra Users', @planId, 2, 'SUBSCRIBED', now(), @one_year_later, @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');

-- membership
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');
insert into test.membership (email, user_id, account_id, role, status) values (@editor_email, @editor_id, @account_id, 'EDITOR', 'JOINED');
insert into test.membership (email, user_id, account_id, role, status) values (@viewer_email, @viewer_id, @account_id, 'VIEWER', 'JOINED');

-- tickets
insert into test.ticket (priority, type, subject, body, user_id, account_id) 
values ('NORMAL', 'SUPPORT', 'OTHER', 'I am unable to find where should I enter my invoice info, pls help!', @viewer_id, @account_id);

insert into test.ticket (priority, type, subject, body, user_id, account_id) 
values ('HIGH', 'PROBLEM', 'PAYMENT', 'My last payment seems to failed but my bank account confirms the transaction.', @viewer_id, @account_id);
