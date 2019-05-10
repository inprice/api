-- @author mdpinar

-- info tables

create table country (
  id                        bigint auto_increment not null,
  code                      varchar(2) not null,
  name                      varchar(50) not null,
  locale                    varchar(5) not null,
  lang                      varchar(30),
  flag                      varchar(10),
  primary key (id)
) engine=innodb default charset=utf8;
create unique index country_ix1 on country (code);
create unique index country_ix2 on country (name);

create table site (
  id                        bigint auto_increment not null,
  active                    tinyint(1) default 1,
  name                      varchar(50) not null,
  url                       varchar(250) not null,
  logo                      varchar(250),
  logo_mini                 varchar(250),
  currency_code             varchar(5) not null,
  currency_symbol           varchar(5),
  thousand_separator        varchar(1) default ',',
  decimal_separator         varchar(1) default '.',
  country_id                bigint   not null,
  class_name                varchar(30) not null,
  insert_at                 timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create unique index site_ix1 on site (name);
create unique index site_ix2 on site (url);
alter table site add foreign key (country_id) references country (id);

create table plan (
  id                        bigint auto_increment not null,
  active                    tinyint(1) default 1,
  name                      varchar(30) not null,
  desc_1                    varchar(100),
  desc_2                    varchar(100),
  desc_3                    varchar(100),
  row_limit                 int,
  price                     double,
  price_1                   double,
  order_no                  smallint,
  css_class                 varchar(150),
  free                      tinyint(1) default 0,
  primary key (id)
) engine=innodb default charset=utf8;
create unique index plan_ix1 on plan (name);

create table sector (
  id                        bigint auto_increment not null,
  name                      varchar(50) not null,
  primary key (id)
) engine=innodb default charset=utf8;
create unique index sector_ix1 on sector (name);

-- customer tables

create table customer (
  id                        bigint auto_increment not null,
  user_type                 varchar(25) not null default 'USER',
  email                     varchar(150) not null,
  password_hash             varchar(250) not null,
  password_salt             varchar(200) not null,
  company_name              varchar(250) not null,
  contact_name              varchar(150) not null,
  website                   varchar(150),
  sector_id                 bigint,
  country_id                bigint,
  insert_at                 timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create unique index customer_ix1 on customer (email);
create index customer_ix2 on customer (title);
alter table customer add foreign key (sector_id) references sector (id);
alter table customer add foreign key (country_id) references country (id);

create table customer_brand (
  id                        bigint auto_increment not null,
  name                      varchar(150) not null,
  customer_id               bigint,
  insert_at                 timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create unique index customer_brand_ix1 on customer_brand (customer_id, name);
alter table customer_brand add foreign key (customer_id) references customer (id);

create table customer_plan (
  id                        bigint auto_increment not null,
  active                    tinyint(1) default 1,
  name                      varchar(50) not null,
  monthly                   tinyint(1) default 1,
  customer_id               bigint,
  brand_id                  bigint,
  plan_id                   bigint,
  due_date                  datetime,
  last_collecting_time      datetime,
  last_collecting_status    tinyint(1),
  collecting_retries        int,
  insert_at                 timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create index customer_plan_ix1 on customer_plan (last_collecting_time);
create index customer_plan_ix2 on customer_plan (due_date);
alter table customer_plan add foreign key (customer_id) references customer (id);
alter table customer_plan add foreign key (brand_id) references customer_brand (id);
alter table customer_plan add foreign key (plan_id) references plan (id);

create table customer_plan_history (
  id                        bigint auto_increment not null,
  customer_plan_id          bigint,
  collecting_status         tinyint(1),
  insert_at                 timestamp not null default current_timestamp,
primary key (id)
) engine=innodb default charset=utf8;
create index customer_plan_history_ix1 on customer_plan (insert_at);
alter table customer_plan_history add foreign key (customer_plan_id) references customer_plan (id);

create table product (
  id                        bigint auto_increment not null,
  active                    tinyint(1) default 1,
  code                      varchar(120),
  name                      varchar(500),
  brand                     varchar(100),
  category                  varchar(150),
  price                     double,
  position                  int default 3,
  last_update               datetime,
  min_seller                varchar(150),
  max_seller                varchar(150),
  min_price                 double,
  avg_price                 double,
  max_price                 double,
  customer_plan_id          bigint,
  primary key (id)
) engine=innodb default charset=utf8;
create unique index product_ix1 on product (customer_plan_id, code);
create unique index product_ix2 on product (customer_plan_id, name);
alter table product add foreign key (customer_plan_id) references customer_plan (id);

create table link (
  id                        bigint auto_increment not null,
  title                     varchar(500),
  code                      varchar(120),
  url                       varchar(2000) not null,
  alt_url                   varchar(2000),
  brand                     varchar(150),
  seller                    varchar(150),
  shipment                  varchar(150),
  price                     double,
  cycle                     int default 0,
  status                    varchar(15) not null default 'NEW',
  note                      varchar(300),
  customer_plan_id          bigint,
  product_id                bigint,
  site_id                   bigint,
  primary key (id)
) engine=innodb default charset=utf8;
create index link_ix1 on link (cycle, status);
create index link_ix2 on link (title);
create index link_ix3 on link (code);
alter table link add foreign key (product_id) references product (id);
alter table link add foreign key (site_id) references site (id);
alter table link add foreign key (customer_plan_id) references customer_plan (id);

create table link_price (
  id                        bigint auto_increment not null,
  link_id                   bigint not null,
  price                     double,
  stock_status              varchar(100),
  insert_at                 timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create index link_price_ix1 on link_price (insert_at);
alter table link_price add foreign key (link_id) references link (id);

create table link_spec (
  id                        bigint auto_increment not null,
  link_id                   bigint not null,
  _key                      varchar(100),
  _value                    varchar(500),
  primary key (id)
) engine=innodb default charset=utf8;
alter table link_spec add foreign key (link_id) references link (id);

create table link_history (
  id                        bigint auto_increment not null,
  link_id                   bigint not null,
  status                    varchar(15) not null,
  insert_at                 timestamp not null default current_timestamp,
 primary key (id)
) engine=innodb default charset=utf8;
create index link_history_ix1 on link_history (insert_at);
alter table link_history add foreign key (link_id) references link (id);

