-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set @admin_email = 'admin@workspace-f.com';
set @editor_email = 'editor@workspace-f.com';

-- this is an important line to consider!
-- the email is already defined in 10_workspace_with_standard_plan_with_one_extra_user.sql as EDITOR
-- here, we are binding him to this workspace as VIEWER
-- so that we can test one email with multiple workspaces case!
set @viewer_email = 'editor@workspace-e.com';

-- -----------------------

-- admin
insert into test.user (email, password, full_name, timezone) values (@admin_email, @salted_pass, SUBSTRING_INDEX(@admin_email, '@', 1), @timezone);
set @admin_id = last_insert_id();

-- editor
insert into test.user (email, password, full_name, timezone) values (@editor_email, @salted_pass, SUBSTRING_INDEX(@editor_email, '@', 1), @timezone);
set @editor_id = last_insert_id();

-- viewer
select id into @viewer_id from test.user where email=@viewer_email;

-- workspace
insert into test.workspace (name, plan_id, status, subs_started_at, subs_renewal_at, user_count, admin_id) 
values ('Premium Plan and Two Extra Users', @premium_plan_id, 'SUBSCRIBED', now(), @one_year_later, 2, @admin_id);
set @workspace_id = last_insert_id();

-- workspace history
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'CREATED');
insert into test.workspace_history (workspace_id, status) values (@workspace_id, 'SUBSCRIBED');

-- workspace transactions
insert into test.workspace_trans (workspace_id, event_id, event, reason)
values (@workspace_id, 'AX-123', 'PAYMENT', 'Insufficient voucher.');

insert into test.workspace_trans (workspace_id, event_id, event, reason)
values (@workspace_id, 'AX-124', 'PAYMENT', 'Not authorized.');

insert into test.workspace_trans (workspace_id, event_id, event, successful, description, file_url)
values (@workspace_id, 'AA-011', 'PAYMENT', true, 'Payment has been made successfully', 'https://invoices.inprice.io/145/AA_011.pdf');

-- membership
insert into test.membership (email, user_id, workspace_id, role, status) values (@admin_email, @admin_id, @workspace_id, 'ADMIN', 'JOINED');
insert into test.membership (email, user_id, workspace_id, role, status) values (@editor_email, @editor_id, @workspace_id, 'EDITOR', 'JOINED');
insert into test.membership (email, user_id, workspace_id, role, status) values (@viewer_email, @viewer_id, @workspace_id, 'VIEWER', 'JOINED');

-- -----------------------
-- brands
-- -----------------------
insert into test.brand (name, workspace_id) values
  ('APPLE', @workspace_id),
  ('LENOVO', @workspace_id),
  ('SAMSUNG', @workspace_id);

-- -----------------------
-- categories
-- -----------------------
insert into test.category (name, workspace_id) values
  ('MEN', @workspace_id),
  ('KIDS', @workspace_id),
  ('WOMEN', @workspace_id);

-- -----------------------
-- 3 products and 12 links
-- product_name_addition, actives, tryings, waitings, problems, url, platform_id, workspace_name, workspace_id
-- -----------------------
call sp_create_product_and_links('F-1', 'K', 3, 0, 1, 1, 'https://amazon.com/', 2, 'Workspace-F', @workspace_id);
call sp_create_product_and_links('F-2', 'G', 2, 1, 0, 1, 'https://ebay.com/', 12, 'Workspace-F', @workspace_id);
call sp_create_product_and_links('F-3', 'I', 1, 1, 1, 0, 'https://mediamarkt.es', 40, 'Workspace-F', @workspace_id);

-- -----------------------
-- 2 alarms definitions for 5 entities (1 product and 2 links)
-- -----------------------
insert into alarm (name, topic, subject, subject_when, workspace_id) 
values ('Product position is changed', 'PRODUCT', 'POSITION', 'CHANGED', @workspace_id);
update product set alarm_id=last_insert_id() where sku = 'F-3';

insert into alarm (name, topic, subject, subject_when, workspace_id) 
values ('Link status is changed', 'LINK', 'POSITION', 'CHANGED', @workspace_id);
update link set alarm_id=last_insert_id() where grup = 'WAITING' and workspace_id = @workspace_id;

-- tickets
-- -----------------------

-- ticket 1
insert into test.ticket (priority, type, subject, body, comment_count, user_id, workspace_id) 
values ('NORMAL', 'SUPPORT', 'OTHER', 'I am unable to find where should I enter my invoice info, pls help!', 2, @editor_id, @workspace_id);
set @ticket_id = last_insert_id();

-- history
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, workspace_id) 
values (@ticket_id, 'OPENED', 'NORMAL', 'SUPPORT', 'OTHER', @editor_id, @workspace_id);

-- comment 1
insert into test.ticket_comment (ticket_id, body, editable, user_id, workspace_id) 
values (@ticket_id, 'I have no idea what should I write in this comment.', false, @viewer_id, @workspace_id);

-- comment 2
insert into test.ticket_comment (ticket_id, body, user_id, workspace_id) 
values (@ticket_id, 'This is the first comment that can be modified', @viewer_id, @workspace_id);

-- -----------------------

-- ticket 2
insert into test.ticket (priority, type, subject, body, comment_count, user_id, workspace_id) 
values ('HIGH', 'PROBLEM', 'PAYMENT', 'My last payment seems to failed but my bank workspace confirms the transaction.', 2, @viewer_id, @workspace_id);
set @ticket_id = last_insert_id();

-- history
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, workspace_id) 
values (@ticket_id, 'OPENED', 'HIGH', 'PROBLEM', 'PAYMENT', @viewer_id, @workspace_id);

-- comment 1
insert into test.ticket_comment (ticket_id, body, editable, user_id, workspace_id) 
values (@ticket_id, 'This comment is deliberately closed to modifications', false, @viewer_id, @workspace_id);

-- comment 2
insert into test.ticket_comment (ticket_id, body, user_id, workspace_id) 
values (@ticket_id, 'This comment can be modified', @editor_id, @workspace_id);

-- -----------------------

-- ticket 3
insert into test.ticket (status, priority, type, subject, body, user_id, workspace_id) 
values ('CLOSED', 'LOW', 'SUPPORT', 'PAYMENT', 'I even do not know what my problem is. Please close this ticket.', @admin_id, @workspace_id);
set @ticket_id = last_insert_id();

-- comment
insert into test.ticket_comment (ticket_id, body, user_id, workspace_id) 
values (@ticket_id, 'Even though this comment is editable, it cannot be deleted since its ticket closed', @viewer_id, @workspace_id);

-- history 1
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, workspace_id) 
values (@ticket_id, 'OPENED', 'HIGH', 'PROBLEM', 'PAYMENT', @admin_id, @workspace_id);

-- history 2
insert into test.ticket_history (ticket_id, status, priority, type, subject, user_id, workspace_id) 
values (@ticket_id, 'CLOSED', 'LOW', 'SUPPORT', 'PAYMENT', @admin_id, @workspace_id);
