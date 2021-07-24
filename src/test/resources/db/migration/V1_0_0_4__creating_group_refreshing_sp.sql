-- @author mdpinar
-- @since 2021-03-13
DELIMITER $$

create procedure sp_refresh_group (
    in in_group_id bigint,
    out minPrice decimal(9,2),
    out avgPrice decimal(9,2),
    out maxPrice decimal(9,2),
    out total decimal(9,2),
    out level varchar(10),
    out alarmId bigint unsigned
  )
begin
  start transaction;

  select price, alarm_id into @groupPrice, @alarmId from link_group where id = in_group_id for update;

  select
    @total    := sum(case when status_group = 'ACTIVE' then price else 0 end),
    @minPrice := min(case when status_group = 'ACTIVE' then price end),
    @avgPrice := avg(case when status_group = 'ACTIVE' then price end),
    @maxPrice := max(case when status_group = 'ACTIVE' then price end),
    @actives  := sum(case when status_group = 'ACTIVE' then 1 else 0 end),
    @tryings  := sum(case when status_group = 'TRYING' then 1 else 0 end),
    @waitings := sum(case when status_group = 'WAITING' then 1 else 0 end),
    @problems := sum(case when status_group = 'PROBLEM' then 1 else 0 end)
  from link where group_id = in_group_id;

  if @total    is null then set @total    := 0; end if;
  if @minPrice is null then set @minPrice := 0; end if;
  if @avgPrice is null then set @avgPrice := 0; end if;
  if @maxPrice is null then set @maxPrice := 0; end if;
  if @actives  is null then set @actives  := 0; end if;
  if @tryings  is null then set @tryings  := 0; end if;
  if @waitings is null then set @waitings := 0; end if;
  if @problems is null then set @problems := 0; end if;

  set @level       := 'NA';
  set @minSeller   := 'NA';
  set @minPlatform := 'NA';
  set @maxSeller   := 'NA';
  set @maxPlatform := 'NA';

  set @minDiff := 0;
  set @avgDiff := 0;
  set @maxDiff := 0;

  if @actives > 0 or @tryings > 0 or @waitings > 0 or @problems > 0 then

    update link set level='NA' where group_id=in_group_id;

    if @actives > 1 then

      if @minPrice != @maxPrice then
        select @minSeller := seller, @minPlatform:= p.name from link as l inner join platform as p on p.id = l.platform_id where price = @minPrice;
        select @maxSeller := seller, @maxPlatform:= p.name from link as l inner join platform as p on p.id = l.platform_id where price = @maxPrice;
      else
        set @level := 'EQUAL';
        set @avgPrice := @minPrice;
      end if;

      if @groupPrice > 0 and @minPrice != @maxPrice then
        set @minDiff := ROUND(((@minPrice / @groupPrice)-1)*100, 2);
        set @avgDiff := ROUND(((@avgPrice / @groupPrice)-1)*100, 2);
        set @maxDiff := ROUND(((@maxPrice / @groupPrice)-1)*100, 2);

        if @groupPrice <= @minPrice then 
          set @level := 'LOWEST';
          set @minSeller := 'You';
          set @minPlatform := 'Yours';
          set @minPrice := @groupPrice;
        elseif @groupPrice < @avgPrice then 
          set @level := 'LOWER';
        elseif @groupPrice = @avgPrice then 
          set @level := 'AVERAGE';
        elseif @groupPrice < @maxPrice then 
          set @level := 'HIGHER';
        elseif @groupPrice >= @maxPrice then 
          set @level := 'HIGHEST';
          set @maxSeller := 'You';
          set @maxPlatform := 'Yours';
          set @maxPrice := @groupPrice;
        end if;
      end if;

      if @minPrice != @maxPrice then
        update link set level = 
        (case
            when price <= @minPrice then 'LOWEST'
            when price > @minPrice and price < @avgPrice then 'LOWER'
            when price = @avgPrice then 'AVERAGE'
            when price > @avgPrice and price < @maxPrice then 'HIGHER'
            when price >= @maxPrice then 'HIGHEST'
          end)
        where group_id=in_group_id and status_group='ACTIVE';
      else
        update link set level = 'EQUAL' where group_id=in_group_id and status_group='ACTIVE';
      end if;

    end if;

  end if;

  update link_group set
    min_seller = @minSeller, min_platform = @minPlatform, min_price = @minPrice, min_diff = @minDiff,
    avg_price = @avgPrice, avg_diff = @avgDiff,
    max_seller = @maxSeller, max_platform = @maxPlatform, max_price = @maxPrice, max_diff = @maxDiff,
    actives = @actives, tryings = @tryings, waitings = @waitings, problems = @problems, 
    level = @level, total = @total, updated_at = now()
  where id = in_group_id;

  commit;

  select @minPrice, @avgPrice, @maxPrice, @total, @level, @alarmId into minPrice, avgPrice, maxPrice, total, level, alarmId;

end$$

DELIMITER ;
