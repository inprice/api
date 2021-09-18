-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@account-e.com';
set @editor_email = 'editor@account-e.com';

-- -----------------------

-- admin
insert into test.user (email, password, name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- editor
insert into test.user (email, password, name, timezone) values (@editor_email, @salted_pass, SUBSTRING_INDEX(@editor_email, '@', 1), @timezone);
set @editor_id = last_insert_id();

-- account
insert into test.account (name, plan_id, status, subs_started_at, subs_renewal_at, user_count, admin_id) 
values ('With Standard Plan with One Extra User', @standard_plan_id, 'SUBSCRIBED', now(), @one_year_later, 1, @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');

-- membership
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');
insert into test.membership (email, user_id, account_id, role, status) values (@editor_email, @editor_id, @account_id, 'EDITOR', 'JOINED');

-- -----------------------
-- 2 products and 11 links
-- product_name_addition, actives, tryings, waitings, problems, url, platform_id, account_name, account_id
-- -----------------------
call sp_create_product_and_links('R', 5, 2, 1, 2, 'https://amazon.com/', 2, 'Account-E', @account_id);
call sp_create_product_and_links('S', 0, 0, 1, 0, 'https://gittigidiyor.com', 83, 'Account-E', @account_id);

-- tickets
-- -----------------------

-- ticket
insert into test.ticket (priority, type, subject, body, comment_count, user_id, account_id) 
values ('LOW', 'PROBLEM', 'ACCOUNT', 'This ticket is opened by and editor and has one editable comment!', 1, @admin_id, @account_id);
set @ticket_id = last_insert_id();

-- history
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, account_id) 
values (@ticket_id, 'OPENED', 'NORMAL', 'SUPPORT', 'OTHER', @admin_id, @account_id);

-- comment
insert into test.ticket_comment (ticket_id, body, user_id, account_id) 
values (@ticket_id, 'I am adding this comment just because I am boring as hell.', @editor_id, @account_id);
