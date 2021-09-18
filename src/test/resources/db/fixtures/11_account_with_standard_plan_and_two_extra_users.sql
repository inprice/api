-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

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
insert into test.account (name, plan_id, status, subs_started_at, subs_renewal_at, user_count, admin_id) 
values ('With Standard Plan and Two Extra Users', @standard_plan_id, 'SUBSCRIBED', now(), @one_year_later, 2, @admin_id);
set @account_id = last_insert_id();

-- account history
insert into test.account_history (account_id, status) values (@account_id, 'CREATED');
insert into test.account_history (account_id, status) values (@account_id, 'SUBSCRIBED');

-- account transactions
insert into test.account_trans (account_id, event_id, event, reason)
values (@account_id, 'AX-123', 'PAYMENT', 'Insufficient credit.');

insert into test.account_trans (account_id, event_id, event, reason)
values (@account_id, 'AX-124', 'PAYMENT', 'Not authorized.');

insert into test.account_trans (account_id, event_id, event, successful, description, file_url)
values (@account_id, 'AA-011', 'PAYMENT', true, 'Payment has been made successfully', 'https://invoices.inprice.io/145/AA_011.pdf');

-- membership
insert into test.membership (email, user_id, account_id, role, status) values (@admin_email, @admin_id, @account_id, 'ADMIN', 'JOINED');
insert into test.membership (email, user_id, account_id, role, status) values (@editor_email, @editor_id, @account_id, 'EDITOR', 'JOINED');
insert into test.membership (email, user_id, account_id, role, status) values (@viewer_email, @viewer_id, @account_id, 'VIEWER', 'JOINED');

-- -----------------------
-- 2 products and 12 links
-- product_name_addition, actives, tryings, waitings, problems, url, platform_id, account_name, account_id
-- -----------------------
call sp_create_product_and_links('K', 3, 0, 1, 1, 'https://amazon.com/', 2, 'Account-F', @account_id);
call sp_create_product_and_links('G', 2, 1, 0, 1, 'https://ebay.com/', 12, 'Account-F', @account_id);
call sp_create_product_and_links('I', 1, 1, 1, 0, 'https://mediamarkt.es', 40, 'Account-F', @account_id);

-- tickets
-- -----------------------

-- ticket 1
insert into test.ticket (priority, type, subject, body, comment_count, user_id, account_id) 
values ('NORMAL', 'SUPPORT', 'OTHER', 'I am unable to find where should I enter my invoice info, pls help!', 2, @editor_id, @account_id);
set @ticket_id = last_insert_id();

-- history
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, account_id) 
values (@ticket_id, 'OPENED', 'NORMAL', 'SUPPORT', 'OTHER', @editor_id, @account_id);

-- comment 1
insert into test.ticket_comment (ticket_id, body, editable, user_id, account_id) 
values (@ticket_id, 'I have no idea what should I write in this comment.', false, @viewer_id, @account_id);

-- comment 2
insert into test.ticket_comment (ticket_id, body, user_id, account_id) 
values (@ticket_id, 'This is the first comment that can be modified', @viewer_id, @account_id);

-- -----------------------

-- ticket 2
insert into test.ticket (priority, type, subject, body, comment_count, user_id, account_id) 
values ('HIGH', 'PROBLEM', 'PAYMENT', 'My last payment seems to failed but my bank account confirms the transaction.', 2, @viewer_id, @account_id);
set @ticket_id = last_insert_id();

-- history
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, account_id) 
values (@ticket_id, 'OPENED', 'HIGH', 'PROBLEM', 'PAYMENT', @viewer_id, @account_id);

-- comment 1
insert into test.ticket_comment (ticket_id, body, editable, user_id, account_id) 
values (@ticket_id, 'This comment is deliberately closed to modifications', false, @viewer_id, @account_id);

-- comment 2
insert into test.ticket_comment (ticket_id, body, user_id, account_id) 
values (@ticket_id, 'This comment can be modified', @editor_id, @account_id);

-- -----------------------

-- ticket 3
insert into test.ticket (status, priority, type, subject, body, user_id, account_id) 
values ('CLOSED', 'LOW', 'SUPPORT', 'PAYMENT', 'I even do not know what my problem is. Please close this ticket.', @admin_id, @account_id);
set @ticket_id = last_insert_id();

-- comment
insert into test.ticket_comment (ticket_id, body, user_id, account_id) 
values (@ticket_id, 'Even though this comment is editable, it cannot be deleted since its ticket closed', @viewer_id, @account_id);

-- history 1
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, account_id) 
values (@ticket_id, 'OPENED', 'HIGH', 'PROBLEM', 'PAYMENT', @admin_id, @account_id);

-- history 2
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, account_id) 
values (@ticket_id, 'CLOSED', 'LOW', 'SUPPORT', 'PAYMENT', @admin_id, @account_id);

