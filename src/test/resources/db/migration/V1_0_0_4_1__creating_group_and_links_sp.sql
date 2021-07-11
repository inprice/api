-- @author mdpinar
-- @since 2021-07-11
DELIMITER $$

create procedure sp_create_group_and_links (
    in in_group_no int,
    in in_active_count int,
    in in_trying_count int,
    in in_waiting_count int,
    in in_problem_count int,
    in in_url varchar(100),
    in in_platform_id int,
    in in_account_name varchar(50),
    in in_account_id bigint unsigned
  )
begin
  start transaction;

  insert into test.link_group (name, actives, waitings, tryings, problems, account_id) 
  values (concat('Group ', in_group_no, ' of ', in_account_name), in_active_count, in_waiting_count, in_trying_count, in_problem_count, in_account_id);

  set @group_id = last_insert_id();

  -- for active links  
  if (in_active_count > 0) then
    set @counter := 0;
    while @counter < in_active_count do
      set @sku = LEFT(UUID(), 6);
      set @price = round(rand() * 50.49 + 0.01, 2);
  
      -- link of group
      insert into test.link (url, url_hash, sku, price, status, status_group, platform_id, group_id, account_id)
      values (concat(in_url, @sku), md5(concat(in_url, @sku)), @sku, @price, 'AVAILABLE', 'ACTIVE', in_platform_id, @group_id, in_account_id);
      set @link_id = last_insert_id();
  
      -- link price of link of group
      insert into test.link_price (link_id, price, group_id, account_id) values (@link_id, @price, @group_id, in_account_id);
  
      -- link history of link of group
      insert into test.link_history (link_id, status, group_id, account_id) values (@link_id, @status, @group_id, in_account_id);
  
      set @counter=@counter+1;
    end while;
  end if;

  -- for trying links  
  if (in_trying_count > 0) then
    set @counter := 0;
    while @counter < in_trying_count do
      set @sku = LEFT(UUID(), 6);
      set @price = round(rand() * 50.49 + 0.01, 2);
  
      -- link of group
      insert into test.link (url, url_hash, sku, price, status, status_group, platform_id, group_id, account_id)
      values (concat(in_url, @sku), md5(concat(in_url, @sku)), @sku, @price, 'NOT_AVAILABLE', 'TRYING', in_platform_id, @group_id, in_account_id);
      set @link_id = last_insert_id();
  
      -- link price of link of group
      insert into test.link_price (link_id, price, group_id, account_id) values (@link_id, @price, @group_id, in_account_id);
  
      -- link history of link of group
      insert into test.link_history (link_id, status, group_id, account_id) values (@link_id, @status, @group_id, in_account_id);
  
      set @counter=@counter+1;
    end while;
  end if;

  -- for waiting links  
  if (in_waiting_count > 0) then
    set @counter := 0;
    while @counter < in_waiting_count do
      set @sku = LEFT(UUID(), 6);
      set @price = round(rand() * 50.49 + 0.01, 2);
  
      -- link of group
      insert into test.link (url, url_hash, sku, price, status, status_group, platform_id, group_id, account_id)
      values (concat(in_url, @sku), md5(concat(in_url, @sku)), @sku, @price, 'TOBE_IMPLEMENTED', 'WAITING', in_platform_id, @group_id, in_account_id);
      set @link_id = last_insert_id();
  
      -- link price of link of group
      insert into test.link_price (link_id, price, group_id, account_id) values (@link_id, @price, @group_id, in_account_id);
  
      -- link history of link of group
      insert into test.link_history (link_id, status, group_id, account_id) values (@link_id, @status, @group_id, in_account_id);
  
      set @counter=@counter+1;
    end while;
  end if;

  -- for problem links  
  if (in_problem_count > 0) then
    set @counter := 0;
    while @counter < in_problem_count do
      set @sku = LEFT(UUID(), 6);
  
      -- link of group
      insert into test.link (url, url_hash, sku, status, status_group, platform_id, group_id, account_id)
      values (concat(in_url, @sku), md5(concat(in_url, @sku)), @sku, 'NOT_SUITABLE', 'PROBLEM', in_platform_id, @group_id, in_account_id);
      set @link_id = last_insert_id();
  
      -- link history of link of group
      insert into test.link_history (link_id, status, group_id, account_id) values (@link_id, @status, @group_id, in_account_id);
  
      set @counter=@counter+1;
    end while;
  end if;

  commit;

end$$

DELIMITER ;
