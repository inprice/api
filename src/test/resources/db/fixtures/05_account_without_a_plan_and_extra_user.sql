-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@workspace-a.com';

-- -----------------------

-- admin
insert into test.user (email, password, full_name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- workspace
insert into test.workspace (name, admin_id) values ('Without A Plan and Extra User', @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');

-- membership
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');

-- voucher for Basic Plan
insert into test.voucher (code, plan_id, days, description, issuer_id) values ('RB5QV6CF', @basic_plan_id, 30, 'Given for testing', @workspace_id);

-- announces
insert into test.announce (type, level, title, body, starting_at, ending_at, user_id, workspace_id) 
values ('USER', 'INFO', 'Here is a good news for you', 'This is a test announce for the admin user of Workspace-A', now(), @one_month_later, @admin_id, @workspace_id);

insert into test.announce (type, level, title, body, starting_at, ending_at, workspace_id) 
values ('WORKSPACE', 'INFO', 'A kind reminder for your usage', 'Please consider to pick a broader plan since your link count has reached its limit!', now(), @one_month_later,  @workspace_id);

-- tickets
-- -----------------------

-- ticket 1
insert into test.ticket (priority, type, subject, body, comment_count, user_id, workspace_id) 
values ('NORMAL', 'FEEDBACK', 'VOUCHER', 'Are you planning to give free vouchers out for a special time periods like Christmas.', 2, @admin_id, @workspace_id);
set @ticket_id = last_insert_id();

-- history
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, workspace_id) 
values (@ticket_id, 'OPENED', 'NORMAL', 'FEEDBACK', 'VOUCHER', @admin_id, @workspace_id);

-- comment 1
insert into test.ticket_comment (ticket_id, body, editable, user_id, workspace_id) 
values (@ticket_id, 'I love this comment so much that you cannot delete it!', false, @admin_id, @workspace_id);

-- comment 2
insert into test.ticket_comment (ticket_id, body, user_id, workspace_id) 
values (@ticket_id, 'However, this is not my favourite, can be deleted.', @admin_id, @workspace_id);

-- -----------------------

-- ticket 2
insert into test.ticket (priority, type, subject, body, comment_count, user_id, workspace_id) 
values ('CRITICAL', 'PROBLEM', 'WORKSPACE', 'Login form doesnt allowe me to sign in. So I cannot track my links for two hours.', 1, @admin_id, @workspace_id);
set @ticket_id = last_insert_id();

-- history
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, workspace_id) 
values (@ticket_id,'OPENED', 'CRITICAL', 'PROBLEM', 'WORKSPACE', @admin_id, @workspace_id);

-- comment
insert into test.ticket_comment (ticket_id, body, user_id, workspace_id) 
values (@ticket_id, 'I am still waiting a response for this problem!', @admin_id, @workspace_id);

-- -----------------------
-- 2 smart prices
-- -----------------------
insert into smart_price (name, formula, lower_limit_formula, upper_limit_formula, workspace_id) 
values ('Smart Formula', '(p*1.10)', 'a', 'x', @workspace_id);

insert into smart_price (name, formula, lower_limit_formula, upper_limit_formula, workspace_id) 
values ('Other Formula', '(p/1.10)', 'a+i', 'x*2', @workspace_id);
