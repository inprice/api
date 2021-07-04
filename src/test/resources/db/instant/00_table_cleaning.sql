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
truncate table test.member;
truncate table test.ticket;
truncate table test.ticket_comment;
truncate table test.ticket_history;
truncate table test.user;
truncate table test.user_session;
truncate table test.user_used;

set foreign_key_checks=1;