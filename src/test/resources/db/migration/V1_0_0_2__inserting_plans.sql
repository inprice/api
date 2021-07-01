-- @author mdpinar
-- 2021-05-10

insert into plan (id, name, user_limit, alarm_limit, link_limit, price) values
  (10, 'Basic Plan', 0, 5, 25, 20.00),
  (15, 'Starter Plan', 1, 10, 50, 40.00),
  (20, 'Standard Plan', 2, 20, 100, 60.00),
  (25, 'Pro Plan', 3, 50, 250, 80.00),
  (30, 'Premium Plan', 4, 100, 500, 150.00),
  (35, 'Enterprise Plan', 5, 250, 1000, 220.00)
  ;

insert into feature (id, description, allowed, order_no) values
  (10, 'Unlimited groups', true, 5),
  (20, 'Up to 25 links', true, 10),
  (25, 'Up to 5 alarms', true, 12),
  (27, 'No extra user!', false, 15),
  (30, 'Up to 50 links', true, 10),
  (35, 'Up to 10 alarms', true, 12),
  (37, 'Extra 1 user', true, 15),
  (40, 'Up to 100 links', true, 10),
  (45, 'Up to 20 alarms', true, 12),
  (47, 'Extra 2 users', true, 15),
  (50, 'Up to 250 links', true, 10),
  (55, 'Up to 50 alarms', true, 12),
  (57, 'Extra 3 users', true, 15),
  (60, 'Up to 500 links', true, 10),
  (65, 'Up to 100 alarms', true, 12),
  (67, 'Extra 4 users', true, 15),
  (70, 'Up to 1000 links', true, 10),
  (75, 'Up to 250 alarms', true, 12),
  (77, 'Extra 5 users', true, 15)
  ;

insert into plan_feature (plan_id, feature_id) values
  (10, 10),
  (10, 20),
  (10, 25),
  (10, 27),
  (15, 10),
  (15, 30),
  (15, 35),
  (15, 37),
  (20, 10),
  (20, 40),
  (20, 45),
  (20, 47),
  (25, 10),
  (25, 50),
  (25, 55),
  (25, 57),
  (30, 10),
  (30, 60),
  (30, 65),
  (30, 67),
  (35, 10),
  (35, 70),
  (35, 75),
  (35, 77)
  ;
