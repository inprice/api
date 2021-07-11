-- -----------------------
-- @author mdpinar
-- @since 2021-07-11
-- -----------------------

set @shop1 = 'https://amazon.com/';
set @shop2 = 'https://ebay.com/';

set @shop1_platform_id = 2;
set @shop2_platform_id = 12;

-- -----------------------

call sp_create_group_and_links(1, 5, 4, 3, 2, 'https://amazon.com/', 2, 'Account-B', 3);