-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

-- creating a super user
insert into test.user (email, password, name, timezone, privileged) values ('super@inprice.io', @salted_pass, 'Super User', @timezone, true);

-- creating a banned user
insert into test.user (email, password, name, timezone, banned, banned_at, ban_reason) values ('banned@inprice.io', @salted_pass, 'banned',@timezone, true, now(), 'No reason!');
