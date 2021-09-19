-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

-- creating a super user
insert into test.user (email, password, name, timezone, privileged) values ('super@inprice.io', @salted_pass, 'Super User', @timezone, true);

-- creating a banned user
insert into test.user (email, password, name, timezone, banned, banned_at, ban_reason) values ('banned@inprice.io', @salted_pass, 'banned', @timezone, true, now(), 'No reason!');
insert into test.user_mark (email, type, description) values ('banned@inprice.io', 'BANNED', 'No reason!');

-- creating a system wide announce for everyone
insert into test.announce (type, level, title, body, starting_at, ending_at) 
values ('SYSTEM', 'WARNING', 'Your workspace\'s expiration date is near!', 'Please verify if your payment method is valid!', now(), @one_year_later);
