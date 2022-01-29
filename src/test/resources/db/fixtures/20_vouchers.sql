-- -----------------------
-- @author mdpinar
-- @since 2021-07-08
-- -----------------------

insert into test.voucher (code, plan_id, days, description) values ('EA8XG2S7', @standard_plan_id, 30, 'Free to use for Standard plan');
insert into test.voucher (code, plan_id, days, description) values ('US8PN1X9', @professional_plan_id, 30, 'Free to use for Premium plan');
insert into test.voucher (code, plan_id, days, description) values ('FH5GV9AA', @professional_plan_id, 30, 'Free to use for Premium plan');
insert into test.voucher (code, plan_id, days, description) values ('YF4GZ5SG', @premium_plan_id, 30, 'Free to use for Professional plan');
insert into test.voucher (code, plan_id, days, description) values ('NE9FC64B', @premium_plan_id, 30, 'Free to use for Professional plan');

insert into test.voucher (code, plan_id, days, description, issued_at) values ('ZL7QN69L', @standard_plan_id, 30, 'Used voucher', DATE_SUB(now(), INTERVAL 1 MONTH));
