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
) engine=innodb;
create unique index ix1 on site (name);

create table plan (
  id                        bigint auto_increment not null,
  active                    boolean default true,
  name                      varchar(30) not null,
  description               varchar(70),
  css                       varchar(50),
  price                     double default 0,
  row_limit                 smallint,
  order_no                  smallint,
  primary key (id)
) engine=innodb;
create unique index ix1 on plan (name);

create table plan_rows (
  id                        bigint auto_increment not null,
  description               varchar(120) not null,
  order_no                  smallint,
  plan_id                   bigint not null,
  primary key (id)
) engine=innodb;

create table user (
  id                        bigint auto_increment not null,
  email                     varchar(100) not null,
  name                      varchar(70) not null,
  password_hash             varchar(255) not null,
  password_salt             varchar(255) not null,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb;
create unique index ix1 on user (email);

create table company (
  id                        bigint auto_increment not null,
  name                      varchar(70) not null,
  sector                    varchar(50),
  website                   varchar(100),
  country                   varchar(50) not null,
  admin_id                  bigint not null,
  plan_id                   bigint,
  plan_status               enum('NOT_SET', 'ACTIVE', 'PAUSED', 'CANCELLED') not null default 'NOT_SET',
  due_date                  datetime,
  retry                     smallint default 0,
  last_collecting_time      datetime,
  last_collecting_status    boolean default false,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb;
create index ix1 on company (name);
alter table company add foreign key (admin_id) references user (id);
alter table company add foreign key (plan_id) references plan (id);

create table user_company (
  id                        bigint auto_increment not null,
  email                     varchar(100) not null,
  user_id                   bigint,
  company_id                bigint not null,
  role                      enum('ADMIN', 'EDITOR', 'VIEWER') not null default 'EDITOR',
  pre_status                enum('PENDING', 'JOINED', 'PAUSED', 'LEFT') not null default 'PENDING',
  status                    enum('PENDING', 'JOINED', 'PAUSED', 'LEFT') not null default 'PENDING',
  retry                     smallint default 1,
  updated_at                timestamp,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb;
create index ix1 on user_company (email);
alter table user_company add foreign key (user_id) references user (id);
alter table user_company add foreign key (company_id) references company (id);

create table user_session (
  _hash                     varchar(32) not null,
  user_id                   bigint not null,
  company_id                bigint not null,
  ip                        varchar(255),
  os                        varchar(30),
  browser                   varchar(100),
  user_agent                varchar(500),
  accessed_at               timestamp not null default current_timestamp,
  primary key (_hash)
) engine=innodb;
alter table user_session add foreign key (user_id) references user (id);
alter table user_session add foreign key (company_id) references company (id);
create index ix1 on user_session (accessed_at);

create table product (
  id                        bigint auto_increment not null,
  active                    boolean default true,
  code                      varchar(120) not null,
  name                      varchar(500) not null,
  brand                     varchar(100),
  category                  varchar(100),
  position                  int default 3,
  price                     double default 0,
  avg_price                 double default 0,
  min_platform              varchar(50),
  min_seller                varchar(50),
  min_price                 double default 0,
  max_platform              varchar(50),
  max_seller                varchar(50),
  max_price                 double default 0,
  company_id                bigint not null,
  import_id                 bigint,
  updated_at                datetime,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb;
create unique index ix1 on product (company_id, code);
create index ix2 on product (company_id, name);
alter table product add foreign key (company_id) references company (id);

create table product_price (
  id                        bigint auto_increment not null,
  product_id                bigint not null,
  position                  int default 3,
  price                     double default 0,
  avg_price                 double default 0,
  min_platform              varchar(50),
  min_seller                varchar(50),
  min_price                 double default 0,
  max_platform              varchar(50),
  max_seller                varchar(50),
  max_price                 double default 0,
  company_id                bigint not null,
  created_at                timestamp not null default current_timestamp,
  primary key (id)
) engine=innodb;
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
  pre_status                varchar(25) not null default 'NEW',
  status                    varchar(25) not null default 'NEW',
  retry                     smallint default 0,
  http_status               smallint default 0,
  website_class_name        varchar(100),
  product_id                bigint,
  site_id                   bigint,
  company_id                bigint,
  import_id                 bigint,
  import_row_id             bigint,
  primary key (id)
) engine=innodb;
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
) engine=innodb;
create index ix1 on link_price (created_at);
alter table link_price add foreign key (link_id) references link (id);

create table link_spec (
  id                        bigint auto_increment not null,
  link_id                   bigint not null,
  _key                      varchar(100),
  _value                    varchar(500),
  product_id                bigint not null,
  company_id                bigint not null,
  primary key (id)
) engine=innodb;
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
) engine=innodb;
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
) engine=innodb;
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
) engine=innodb;
alter table link add foreign key (import_row_id) references import_product_row (id);
