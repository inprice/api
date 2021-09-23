-- -----------------------
-- @author mdpinar
-- @since 2021-07-08
-- -----------------------

insert into test.credit (code, plan_id, days, description) values ('EA8XG2S7', @basic_plan_id, 30, 'Free to use for Basic plan');
insert into test.credit (code, plan_id, days, description) values ('US8PN1X9', @starter_plan_id, 30, 'Free to use for Starter plan');
insert into test.credit (code, plan_id, days, description) values ('FH5GV9AA', @starter_plan_id, 30, 'Free to use for Starter plan');
insert into test.credit (code, plan_id, days, description) values ('YF4GZ5SG', @standard_plan_id, 30, 'Free to use for Standard plan');
insert into test.credit (code, plan_id, days, description) values ('NE9FC64B', @standard_plan_id, 30, 'Free to use for Standard plan');

insert into test.credit (code, plan_id, days, description, issued_at) values ('ZL7QN69L', @standard_plan_id, 30, 'Used credit', DATE_SUB(now(), INTERVAL 1 MONTH));
