-- @author mdpinar
-- 2021-05-10

-- ------------
-- plans
-- ------------
insert into plan (name, user_limit, alarm_limit, link_limit, price) values ('Basic Plan', 0, 5, 25, 20.00);
set @p_basic_id = last_insert_id();

insert into plan (name, user_limit, alarm_limit, link_limit, price) values ('Starter Plan', 1, 10, 50, 40.00);
set @p_starter_id = last_insert_id();

insert into plan (name, user_limit, alarm_limit, link_limit, price) values ('Standard Plan', 2, 20, 100, 60.00);
set @p_standard_id = last_insert_id();

insert into plan (name, user_limit, alarm_limit, link_limit, price) values ('Pro Plan', 3, 50, 250, 80.00);
set @p_pro_id = last_insert_id();

insert into plan (name, user_limit, alarm_limit, link_limit, price) values ('Premium Plan', 4, 100, 500, 150.00);
set @p_premium_id = last_insert_id();

insert into plan (name, user_limit, alarm_limit, link_limit, price) values ('Enterprise Plan', 5, 200, 1000, 220.00);
set @p_enterprise_id = last_insert_id();


-- ------------
-- features
-- ------------
insert into plan_feature (description, allowed, order_no) values ('Unlimited groups', true, 5);
set @f_unlimited_groups_id = last_insert_id();

-- ------------

insert into plan_feature (description, allowed, order_no) values ('Up to 25 links', true, 10);
set @f_upto_25_links_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('Up to 5 alarms', true, 12);
set @f_upto_5_alarms_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('No extra user!', false, 15);
set @f_no_user_id = last_insert_id();

-- ------------

insert into plan_feature (description, allowed, order_no) values ('Up to 50 links', true, 10);
set @f_upto_50_links_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('Up to 10 alarms', true, 12);
set @f_upto_10_alarms_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('Extra 1 user', true, 15);
set @f_extra_1_user_id = last_insert_id();

-- ------------

insert into plan_feature (description, allowed, order_no) values ('Up to 100 links', true, 10);
set @f_upto_100_links_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('Up to 20 alarms', true, 12);
set @f_upto_20_alarms_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('Extra 2 users', true, 15);
set @f_extra_2_user_id = last_insert_id();

-- ------------

insert into plan_feature (description, allowed, order_no) values ('Up to 250 links', true, 10);
set @f_upto_250_links_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('Up to 50 alarms', true, 12);
set @f_upto_50_alarms_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('Extra 3 users', true, 15);
set @f_extra_3_user_id = last_insert_id();

-- ------------

insert into plan_feature (description, allowed, order_no) values ('Up to 500 links', true, 10);
set @f_upto_500_links_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('Up to 100 alarms', true, 12);
set @f_upto_100_alarms_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('Extra 4 users', true, 15);
set @f_extra_4_user_id = last_insert_id();

-- ------------

insert into plan_feature (description, allowed, order_no) values ('Up to 1000 links', true, 10);
set @f_upto_1000_links_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('Up to 200 alarms', true, 12);
set @f_upto_200_alarms_id = last_insert_id();

insert into plan_feature (description, allowed, order_no) values ('Extra 5 users', true, 15);
set @f_extra_5_user_id = last_insert_id();


-- ------------
-- many-to-many relationships between plans and plan_features
-- ------------

insert into plans_and_features (plan_id, feature_id) values
  (@p_basic_id, @f_unlimited_groups_id),
  (@p_basic_id, @f_upto_25_links_id),
  (@p_basic_id, @f_upto_5_alarms_id),
  (@p_basic_id, @f_no_user_id),

  (@p_starter_id, @f_unlimited_groups_id),
  (@p_starter_id, @f_upto_50_links_id),
  (@p_starter_id, @f_upto_10_alarms_id),
  (@p_starter_id, @f_extra_1_user_id),

  (@p_standard_id, @f_unlimited_groups_id),
  (@p_standard_id, @f_upto_100_links_id),
  (@p_standard_id, @f_upto_20_alarms_id),
  (@p_standard_id, @f_extra_2_user_id),

  (@p_pro_id, @f_unlimited_groups_id),
  (@p_pro_id, @f_upto_250_links_id),
  (@p_pro_id, @f_upto_50_alarms_id),
  (@p_pro_id, @f_extra_3_user_id),

  (@p_premium_id, @f_unlimited_groups_id),
  (@p_premium_id, @f_upto_500_links_id),
  (@p_premium_id, @f_upto_100_alarms_id),
  (@p_premium_id, @f_extra_4_user_id),

  (@p_enterprise_id, @f_unlimited_groups_id),
  (@p_enterprise_id, @f_upto_1000_links_id),
  (@p_enterprise_id, @f_upto_200_alarms_id),
  (@p_enterprise_id, @f_extra_5_user_id)
  ;
