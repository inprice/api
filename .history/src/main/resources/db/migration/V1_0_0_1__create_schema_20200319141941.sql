-- @author mdpinar

create table site (
  id                        bigint auto_increment not null,
  active                    boolean default true,
  name                      varchar(50) not null,
  domain                    varchar(100) not null,
  country                   varchar(50) not null,
  class_name                varchar(100) not null,
  logo_url                  varchar(250),
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create unique index ix1 on site (name);

create table plan (
  id                        bigint auto_increment not null,
  active                    boolean default true,
  name                      varchar(30) not null,
  price                     double default 0,
  row_limit                 smallint,
  order_no                  smallint,
  primary key (id)
) engine=innodb default charset=utf8;
create unique index ix1 on plan (name);

create table plan_rows (
  id                        bigint auto_increment not null,
  description               varchar(120) not null,
  order_no                  smallint,
  plan_id                   bigint not null,
  primary key (id)
) engine=innodb default charset=utf8;

create table company (
  id                        bigint auto_increment not null,
  name                      varchar(70) not null,
  country                   varchar(50) not null,
  sector                    varchar(50),
  website                   varchar(150),
  owner_id                  bigint not null,
  plan_id                   bigint,
  plan_status               enum('NOT_SET', 'ACTIVE', 'PAUSED', 'CANCELLED') not null default 'NOT_SET',
  due_date                  datetime,
  retry                     smallint default 0,
  last_collecting_time      datetime,
  last_collecting_status    boolean default false,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create index ix1 on company (name);
alter table company add foreign key (plan_id) references plan (id);

create table member (
  id                        bigint auto_increment not null,
  email                     varchar(150) not null,
  company_id                bigint not null,
  role                      enum('ADMIN', 'EDITOR', 'READER') not null default 'EDITOR',
  status                    enum('PENDING', 'JOINED', 'LEFT', 'REJECTED', 'CANCELLED', 'PENDING') not null default 'PENDING',
  retry                     smallint default 1,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create index ix1 on member (email);
alter table member add foreign key (company_id) references company (id);

create table member_history (
  id                        bigint auto_increment not null,
  member_id                 bigint not null,
  status                    enum('PENDING', 'JOINED', 'LEFT', 'REJECTED', 'CANCELLED', 'PENDING') not null default 'PENDING',
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
alter table member_history add foreign key (member_id) references member (id);

create table user (
  id                        bigint auto_increment not null,
  email                     varchar(150) not null,
  name                      varchar(50) not null,
  password_hash             varchar(255) not null,
  password_salt             varchar(255) not null,
  company_id                bigint not null,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create unique index ix1 on user (email);
alter table user add foreign key (company_id) references company (id);

create table product (
  id                        bigint auto_increment not null,
  active                    boolean default true,
  code                      varchar(120) not null,
  name                      varchar(500) not null,
  brand                     varchar(100),
  category                  varchar(100),
  price                     double default 0,
  position                  int default 3,
  min_seller                varchar(150),
  max_seller                varchar(150),
  min_price                 double default 0,
  avg_price                 double default 0,
  max_price                 double default 0,
  company_id                bigint not null,
  import_id                 bigint,
  updated_at                datetime,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create unique index ix1 on product (company_id, code);
create index ix2 on product (company_id, name);
alter table product add foreign key (company_id) references company (id);

create table product_price (
  id                        bigint auto_increment not null,
  product_id                bigint not null,
  min_seller                varchar(150),
  max_seller                varchar(150),
  price                     double default 0,
  position                  int default 4,
  min_price                 double default 0,
  avg_price                 double default 0,
  max_price                 double default 0,
  company_id                bigint not null,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create index ix1 on product_price (created_at);
alter table product_price add foreign key (product_id) references product (id);

create table link (
  id                        bigint auto_increment not null,
  url                       varchar(2000) not null,
  sku                       varchar(70),
  name                      varchar(500),
  brand                     varchar(150),
  seller                    varchar(150),
  shipment                  varchar(150),
  price                     double default 0,
  last_check                datetime default now(),
  last_update               datetime,
  status                    varchar(25) not null default 'NEW',
  previous_status           varchar(25) not null default 'NEW',
  retry                     smallint default 0,
  http_status               smallint default 0,
  website_class_name        varchar(100),
  product_id                bigint,
  site_id                   bigint,
  company_id                bigint,
  import_id                 bigint,
  import_row_id             bigint,
  primary key (id)
) engine=innodb default charset=utf8;
create index ix1 on link (status);
create index ix2 on link (name);
create index ix3 on link (last_update);
create index ix4 on link (last_check);
alter table link add foreign key (product_id) references product (id);
alter table link add foreign key (site_id) references site (id);
alter table link add foreign key (company_id) references company (id);

create table link_price (
  id                        bigint auto_increment not null,
  link_id                   bigint not null,
  price                     double default 0,
  product_id                bigint not null,
  company_id                bigint not null,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create index ix1 on link_price (created_at);
alter table link_price add foreign key (link_id) references link (id);

create table link_spec (
  id                        bigint auto_increment not null,
  link_id                   bigint not null,
  _key                      varchar(100),
  _value                    varchar(500),
  workspace_id              bigint not null,
  company_id                bigint not null,
  primary key (id)
) engine=innodb default charset=utf8;
alter table link_spec add foreign key (link_id) references link (id);

create table link_history (
  id                        bigint auto_increment not null,
  link_id                   bigint not null,
  status                    varchar(25) not null,
  http_status               smallint default 0,
  product_id                bigint not null,
  company_id                bigint not null,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
create index ix1 on link_history (created_at);
alter table link_history add foreign key (link_id) references link (id);

create table import_product (
  id                        bigint auto_increment not null,
  import_type               enum('CSV', 'URL', 'EBAY_SKU', 'AMAZON_ASIN') not null default 'CSV',
  status                    varchar(25) not null,
  result                    varchar(255),
  total_count               smallint default 0,
  insert_count              smallint default 0,
  duplicate_count           smallint default 0,
  problem_count             smallint default 0,
  company_id                bigint not null,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb default charset=utf8;
alter table import_product add foreign key (company_id) references company (id);
alter table link add foreign key (import_id) references import_product (id);
alter table product add foreign key (import_id) references import_product (id);

create table import_product_row (
  id                        bigint auto_increment not null,
  import_id                 bigint not null,
  import_type               enum('CSV', 'URL', 'EBAY_SKU', 'AMAZON_ASIN') not null default 'CSV',
  data                      varchar(1024) not null,
  status                    varchar(25) not null default 'NEW',
  last_update               datetime,
  description               varchar(255),
  link_id                   bigint,
  company_id                bigint not null,
  primary key (id)
) engine=innodb default charset=utf8;
alter table link add foreign key (import_row_id) references import_product_row (id);
