-- -----------------------
-- @author mdpinar
-- @since 2021-07-04
-- -----------------------

-- 1234
set @salted_pass = 'tgeCgsZWyabjtsslplwMrGJRHyTyI4zS4DlAWHnjrMQi2Nn9KwXBS9RROaPWK3BhIkEFtQcLK5TO3q8iihlhbg';

-- creating a super user
insert into test.user (email, password, name, timezone, privileged) values ('super@inprice.io', @salted_pass, 'Super User', 'Europe/Istanbul', true);

-- creating a banned user
insert into test.user (email, password, name, timezone, banned, banned_at, ban_reason) values ('banned@inprice.io', @salted_pass, 'banned','Europe/Istanbul', true, now(), 'No reason!');
