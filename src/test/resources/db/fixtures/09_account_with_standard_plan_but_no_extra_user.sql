-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @planId = 20; -- Standard plan
set @admin_email = 'admin@account-d.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- account
insert into test.account (name, plan_id, status, subs_started_at, subs_renewal_at, alarm_count, admin_id) 
values ('With Standard Plan (Couponed) but No Extra User', @planId, 'COUPONED', now(), @one_year_later, 2, @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'COUPONED');

-- account transaction
insert into test.account_trans (account_id, event_id, event, reason)
values (@account_id, 'SY-12A', 'COUPONED', 'Coupon use: AS34FGD3');

-- membership
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');

-- -----------------------
-- groups and links
-- group_name_addition, actives, tryings, waitings, problems, url, platform_id, account_name, account_id
-- -----------------------
call sp_create_group_and_links('D', 1, 0, 0, 0, 'https://hepsiburada.com/', 84, 'Account-D', @account_id);

-- -----------------------
-- 2 alarms
-- -----------------------
insert into alarm (topic, group_id, subject, subject_when, account_id) 
select 'GROUP', id, 'TOTAL', 'INCREASED', @account_id from link_group where account_id = @account_id limit 1;

insert into alarm (topic, link_id, subject, subject_when, account_id) 
select 'LINK', id, 'PRICE', 'DECREASED', @account_id from link where account_id = @account_id limit 1;
