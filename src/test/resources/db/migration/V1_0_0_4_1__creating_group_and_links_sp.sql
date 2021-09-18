-- @author mdpinar
-- @since 2021-07-11
DELIMITER $$

create procedure sp_create_product_and_links (
    in in_product_name_addition char(1),
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
  
  insert into product (name, actives, waitings, tryings, problems, account_id) 
  values (concat('Product ', in_product_name_addition, ' of ', in_account_name), in_active_count, in_waiting_count, in_trying_count, in_problem_count, in_account_id);

  set @product_id = last_insert_id();

  -- for active links  
  if (in_active_count > 0) then

    set @counter := 0;
    while @counter < in_active_count do

      set @status = 'AVAILABLE';
      set @sku = LEFT(UUID(), 6);
      set @price = round(rand() * 50.49 + 0.01, 2);
  
      -- link of product
      insert into link (url, url_hash, sku, price, status, grup, platform_id, product_id, account_id)
      values (concat(in_url, @sku), md5(concat(in_url, @sku)), @sku, @price, @status, 'ACTIVE', in_platform_id, @product_id, in_account_id);
      set @link_id = last_insert_id();
  
      -- link price of link of product
      insert into link_price (link_id, new_price, product_id, account_id) values (@link_id, @price, @product_id, in_account_id);
  
      -- link history of link of product
      insert into link_history (link_id, status, product_id, account_id) values (@link_id, @status, @product_id, in_account_id);
  
      set @counter=@counter+1;
    end while;
  end if;

  -- for trying links  
  if (in_trying_count > 0) then

    set @counter := 0;
    while @counter < in_trying_count do

      set @status = 'NOT_AVAILABLE';
      set @sku = LEFT(UUID(), 6);
      set @price = round(rand() * 50.49 + 0.01, 2);
  
      -- link of product
      insert into link (url, url_hash, sku, price, status, grup, platform_id, product_id, account_id)
      values (concat(in_url, @sku), md5(concat(in_url, @sku)), @sku, @price, @status, 'TRYING', in_platform_id, @product_id, in_account_id);
      set @link_id = last_insert_id();
  
      -- link price of link of product
      insert into link_price (link_id, new_price, product_id, account_id) values (@link_id, @price, @product_id, in_account_id);
  
      -- link history of link of product
      insert into link_history (link_id, status, product_id, account_id) values (@link_id, @status, @product_id, in_account_id);
  
      set @counter=@counter+1;
    end while;
  end if;

  -- for waiting links  
  if (in_waiting_count > 0) then

    set @counter := 0;
    while @counter < in_waiting_count do

      set @status = 'TOBE_IMPLEMENTED';
      set @sku = LEFT(UUID(), 6);
      set @price = round(rand() * 50.49 + 0.01, 2);
  
      -- link of product
      insert into link (url, url_hash, sku, price, status, grup, platform_id, product_id, account_id)
      values (concat(in_url, @sku), md5(concat(in_url, @sku)), @sku, @price, @status, 'WAITING', in_platform_id, @product_id, in_account_id);
      set @link_id = last_insert_id();
  
      -- link price of link of product
      insert into link_price (link_id, new_price, product_id, account_id) values (@link_id, @price, @product_id, in_account_id);
  
      -- link history of link of product
      insert into link_history (link_id, status, product_id, account_id) values (@link_id, @status, @product_id, in_account_id);
  
      set @counter=@counter+1;
    end while;
  end if;

  -- for problem links  
  if (in_problem_count > 0) then

    set @counter := 0;
    while @counter < in_problem_count do

      set @status = 'NOT_SUITABLE';
      set @sku = LEFT(UUID(), 6);
  
      -- link of product
      insert into link (url, url_hash, sku, status, grup, platform_id, product_id, account_id)
      values (concat(in_url, @sku), md5(concat(in_url, @sku)), @sku, @status, 'PROBLEM', in_platform_id, @product_id, in_account_id);
      set @link_id = last_insert_id();
  
      -- link history of link of product
      insert into link_history (link_id, status, product_id, account_id) values (@link_id, @status, @product_id, in_account_id);
  
      set @counter=@counter+1;
    end while;
  end if;

  update account set link_count = link_count + in_active_count + in_trying_count + in_waiting_count + in_problem_count where id = in_account_id;

  commit;

end$$

DELIMITER ;
