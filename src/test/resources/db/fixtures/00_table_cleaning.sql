-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

set foreign_key_checks=0;

truncate table test.access_log;
truncate table test.account;
truncate table test.account_history;
truncate table test.account_trans;
truncate table test.alarm;
truncate table test.announce;
truncate table test.announce_log;
truncate table test.checkout;
truncate table test.coupon;
truncate table test.link;
truncate table test.link_group;
truncate table test.link_history;
truncate table test.link_price;
truncate table test.link_spec;
truncate table test.membership;
truncate table test.ticket;
truncate table test.ticket_comment;
truncate table test.ticket_history;
truncate table test.user;
truncate table test.user_session;
truncate table test.user_mark;

set foreign_key_checks=1;

-- global variables
set @salted_pass = 'tgeCgsZWyabjtsslplwMrGJRHyTyI4zS4DlAWHnjrMQi2Nn9KwXBS9RROaPWK3BhIkEFtQcLK5TO3q8iihlhbg'; -- 1234

set @timezone = 'Europe/Istanbul';
set @one_month_later = DATE_ADD(now(), INTERVAL 1 MONTH);
set @one_year_later = DATE_ADD(now(), INTERVAL 1 YEAR);

-- plan ids
select id into @basic_plan_id from plan where name = 'Basic Plan';
select id into @starter_plan_id from plan where name = 'Starter Plan';
select id into @standard_plan_id from plan where name = 'Standard Plan';
select id into @pro_plan_id from plan where name = 'Pro Plan';
select id into @premium_plan_id from plan where name = 'Premium Plan';
select id into @enterprise_plan_id from plan where name = 'Enterprise Plan';
