-- Catalog module schema for PostgreSQL.
-- Tables used by CategoryEntity, BrandEntity, ProductEntity, ProductSkuEntity.
-- The Java code stores foreign keys as *_id fields and joins manually in JPQL.

create table if not exists catalog_categories (
    id bigserial primary key,
    name varchar(255) not null,
    slug varchar(255) not null,
    parent_id bigint,
    status varchar(32) not null,
    featured boolean not null default false,
    sort_order integer not null default 0,
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,

    constraint uk_catalog_categories_slug unique (slug),
    constraint ck_catalog_categories_status check (status in ('ACTIVE', 'INACTIVE')),
    constraint fk_catalog_categories_parent
        foreign key (parent_id) references catalog_categories (id)
);

create table if not exists catalog_brands (
    id bigserial primary key,
    name varchar(255) not null,
    slug varchar(255) not null,
    logo_url varchar(1000),
    status varchar(32) not null,
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,

    constraint uk_catalog_brands_slug unique (slug),
    constraint ck_catalog_brands_status check (status in ('ACTIVE', 'INACTIVE'))
);

create table if not exists catalog_products (
    id bigserial primary key,
    name varchar(255) not null,
    slug varchar(255) not null,
    short_description varchar(1000),
    description text,
    image_urls text[],
    brand_id bigint,
    category_id bigint not null,
    status varchar(32) not null,
    featured boolean not null default false,
    published_at timestamp without time zone,
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,

    constraint uk_catalog_products_slug unique (slug),
    constraint ck_catalog_products_status check (status in ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED')),
    constraint fk_catalog_products_brand
        foreign key (brand_id) references catalog_brands (id),
    constraint fk_catalog_products_category
        foreign key (category_id) references catalog_categories (id)
);

create table if not exists catalog_product_skus (
    id bigserial primary key,
    product_id bigint not null,
    sku_code varchar(100) not null,
    barcode varchar(100),
    variant_label varchar(255),
    image_urls text[],
    sale_price numeric(19, 2) not null,
    compare_price numeric(19, 2),
    status varchar(32) not null,
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,

    constraint uk_catalog_product_skus_sku_code unique (sku_code),
    constraint ck_catalog_product_skus_status check (status in ('ACTIVE', 'INACTIVE', 'DISCONTINUED')),
    constraint ck_catalog_product_skus_sale_price check (sale_price >= 0),
    constraint ck_catalog_product_skus_compare_price check (compare_price is null or compare_price >= 0),
    constraint fk_catalog_product_skus_product
        foreign key (product_id) references catalog_products (id)
);

-- Category list/home queries.
create index if not exists idx_catalog_categories_featured_active_sort
    on catalog_categories (featured, sort_order, name, id)
    where status = 'ACTIVE';

create index if not exists idx_catalog_categories_parent_id
    on catalog_categories (parent_id)
    where parent_id is not null;

-- Brand lookup/filter queries.
create index if not exists idx_catalog_brands_active_name
    on catalog_brands (name, id)
    where status = 'ACTIVE';

-- Product home "newest" query:
-- where status = ACTIVE and published_at is not null
-- order by published_at desc, id desc.
create index if not exists idx_catalog_products_home_new
    on catalog_products (status, published_at desc, id desc)
    include (category_id, brand_id, featured)
    where published_at is not null;

-- Product home "featured" query:
-- where status = ACTIVE and featured = true and published_at is not null
-- order by published_at desc, id desc.
create index if not exists idx_catalog_products_home_featured
    on catalog_products (status, featured, published_at desc, id desc)
    include (category_id, brand_id)
    where published_at is not null;

create index if not exists idx_catalog_products_category_id
    on catalog_products (category_id);

create index if not exists idx_catalog_products_brand_id
    on catalog_products (brand_id)
    where brand_id is not null;

-- Product detail lookup by slug is already covered by uk_catalog_products_slug.

-- SKU existence and min price queries used by home product projections.
-- This is the most important SKU index for:
-- exists active sellable SKU, min(sale_price), and reading compare_price.
create index if not exists idx_catalog_product_skus_sellable_product_price
    on catalog_product_skus (product_id, sale_price)
    include (compare_price)
    where status = 'ACTIVE' and sale_price > 0;

-- General SKU lookup by product when inactive/discontinued rows are also needed.
create index if not exists idx_catalog_product_skus_product_status_price
    on catalog_product_skus (product_id, status, sale_price);

create index if not exists idx_catalog_product_skus_barcode
    on catalog_product_skus (barcode)
    where barcode is not null;
