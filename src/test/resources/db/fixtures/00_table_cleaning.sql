-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set foreign_key_checks=0;

truncate table test.access_log;
truncate table test.workspace;
truncate table test.workspace_history;
truncate table test.workspace_trans;
truncate table test.alarm;
truncate table test.smart_price;
truncate table test.announce;
truncate table test.announce_log;
truncate table test.checkout;
truncate table test.voucher;
truncate table test.link;
truncate table test.product;
truncate table test.brand;
truncate table test.category;
truncate table test.link_history;
truncate table test.link_price;
truncate table test.link_spec;
truncate table test.membership;
truncate table test.ticket;
truncate table test.ticket_comment;
truncate table test.ticket_history;
truncate table test.user;
truncate table test.user_session;
truncate table test.user_marks;

set foreign_key_checks=1;

-- global variables
set @salted_pass = 'eLgUOcQnH/Twai9hJF4Ing25yXoR2eGA0DseixPycjcTb//WqlbdEct3rykdJI7MAmoO2MBDBaAoVYGsV7LLuo'; -- 1234-AB

set @timezone = 'Europe/Istanbul';
set @one_month_later = DATE_ADD(now(), INTERVAL 1 MONTH);
set @one_year_later = DATE_ADD(now(), INTERVAL 1 YEAR);

-- plan ids
select id into @standard_plan_id from plan where name = 'Standard Plan';
select id into @professional_plan_id from plan where name = 'Professional Plan';
select id into @premium_plan_id from plan where name = 'Premium Plan';
select id into @enterprise_plan_id from plan where name = 'Enterprise Plan';
