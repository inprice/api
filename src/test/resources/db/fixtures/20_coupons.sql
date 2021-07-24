-- -----------------------
-- @author mdpinar
-- @since 2021-07-08
-- -----------------------

insert into test.coupon (code, plan_id, days, description) values ('EA8XG2S7', 10, 30, 'Free to use for Basic plan');
insert into test.coupon (code, plan_id, days, description) values ('US8PN1X9', 15, 30, 'Free to use for Starter plan');
insert into test.coupon (code, plan_id, days, description) values ('FH5GV9AA', 15, 30, 'Free to use for Starter plan');
insert into test.coupon (code, plan_id, days, description) values ('YF4GZ5SG', 20, 30, 'Free to use for Standard plan');
insert into test.coupon (code, plan_id, days, description) values ('NE9FC64B', 20, 30, 'Free to use for Standard plan');

insert into test.coupon (code, plan_id, days, description, issued_at) values ('ZL7QN69L', 20, 30, 'Used coupon', DATE_SUB(now(), INTERVAL 1 MONTH));
