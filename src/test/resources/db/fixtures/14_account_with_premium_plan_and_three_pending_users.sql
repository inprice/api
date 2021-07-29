-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@account-h.com';
set @editor_email = 'editor@account-h.com'; -- not exists as a user!
set @viewer_email = 'editor@account-c.com'; -- exists in 08_account_with_starter_plan_and_one_extra_user

-- this is an important line to consider!
-- the email is already defined in 10_account_with_standard_plan_with_one_extra_user.sql as EDITOR
-- here, we are binding him to this account as VIEWER
-- so that we can test one email with multiple accounts case!
set @other_viewer_email = 'editor@account-e.com';
-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- viewer
select id into @viewer_id from test.user where email=@viewer_email;

-- other viewer
select id into @other_viewer_id from test.user where email=@other_viewer_email;

-- account
insert into test.account (name, plan_id, user_count, status, subs_started_at, subs_renewal_at, admin_id) 
values ('With Premium Plan and Two Pending Users', @premium_plan_id, 3, 'SUBSCRIBED', now(), @one_year_later, @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');

-- memberships
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');
insert into test.membership (email, account_id) values (@editor_email,  @account_id); -- not exists!
insert into test.membership (email, user_id, account_id, role) values (@viewer_email, @viewer_id, @account_id, 'VIEWER');
insert into test.membership (email, user_id, account_id, role) values (@other_viewer_email, @other_viewer_id, @account_id, 'VIEWER');
