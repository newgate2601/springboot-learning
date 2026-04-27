-- Large catalog fake data for PostgreSQL benchmark/testing.
--
-- Target volume:
-- - catalog_categories: 1,000,000 rows
-- - catalog_brands:       500,000 rows
-- - catalog_products:   3,000,000 rows
-- - catalog_product_skus: 10,000,000 rows
--
-- This script is intentionally set-based. Do not convert it to row-by-row inserts.
-- Recommended: run catalog_tables.sql first, then run this file manually with psql.

\timing on

begin;

set local synchronous_commit = off;
set local maintenance_work_mem = '1GB';
set local work_mem = '128MB';

truncate table catalog_product_skus, catalog_products, catalog_brands, catalog_categories
restart identity cascade;

-- Keep primary key, unique, and foreign key indexes/constraints.
-- Drop secondary indexes during bulk load, then recreate them after inserts.
drop index if exists idx_catalog_categories_featured_active_sort;
drop index if exists idx_catalog_categories_parent_id;
drop index if exists idx_catalog_brands_active_name;
drop index if exists idx_catalog_products_home_new;
drop index if exists idx_catalog_products_home_featured;
drop index if exists idx_catalog_products_category_id;
drop index if exists idx_catalog_products_brand_id;
drop index if exists idx_catalog_product_skus_sellable_product_price;
drop index if exists idx_catalog_product_skus_product_status_price;
drop index if exists idx_catalog_product_skus_barcode;

insert into catalog_categories (
    id,
    name,
    slug,
    parent_id,
    status,
    featured,
    sort_order,
    created_at,
    updated_at
)
select
    gs as id,
    'Category ' || gs as name,
    'category-' || gs as slug,
    case
        when gs = 1 then null
        else greatest(1, gs / 10)
    end as parent_id,
    case
        when gs % 20 = 0 then 'INACTIVE'
        else 'ACTIVE'
    end as status,
    gs % 1000 = 0 as featured,
    (gs % 10000)::integer as sort_order,
    now() - ((gs % 365) || ' days')::interval as created_at,
    now() - ((gs % 30) || ' days')::interval as updated_at
from generate_series(1, 1000000) as gs;

insert into catalog_brands (
    id,
    name,
    slug,
    logo_url,
    status,
    created_at,
    updated_at
)
select
    gs as id,
    'Brand ' || gs as name,
    'brand-' || gs as slug,
    'https://cdn.example.test/brands/' || gs || '.png' as logo_url,
    case
        when gs % 25 = 0 then 'INACTIVE'
        else 'ACTIVE'
    end as status,
    now() - ((gs % 365) || ' days')::interval as created_at,
    now() - ((gs % 30) || ' days')::interval as updated_at
from generate_series(1, 500000) as gs;

insert into catalog_products (
    id,
    name,
    slug,
    short_description,
    description,
    image_urls,
    brand_id,
    category_id,
    status,
    featured,
    published_at,
    created_at,
    updated_at
)
select
    gs as id,
    'Product ' || gs as name,
    'product-' || gs as slug,
    'Short description for product ' || gs as short_description,
    'Long benchmark description for product ' || gs as description,
    array[
        'https://cdn.example.test/products/' || gs || '/main.webp',
        'https://cdn.example.test/products/' || gs || '/alt.webp'
    ] as image_urls,
    ((gs - 1) % 500000) + 1 as brand_id,
    ((gs - 1) % 1000000) + 1 as category_id,
    case
        when gs % 100 = 0 then 'ARCHIVED'
        when gs % 20 = 0 then 'INACTIVE'
        when gs % 10 = 0 then 'DRAFT'
        else 'ACTIVE'
    end as status,
    gs % 53 = 0 as featured,
    case
        when gs % 10 = 0 then null
        else now() - ((gs % 730) || ' days')::interval
    end as published_at,
    now() - ((gs % 730) || ' days')::interval as created_at,
    now() - ((gs % 30) || ' days')::interval as updated_at
from generate_series(1, 3000000) as gs;

insert into catalog_product_skus (
    id,
    product_id,
    sku_code,
    barcode,
    variant_label,
    image_urls,
    sale_price,
    compare_price,
    status,
    created_at,
    updated_at
)
select
    gs as id,
    ((gs - 1) % 3000000) + 1 as product_id,
    'SKU-' || gs as sku_code,
    'BC-' || gs as barcode,
    'Variant ' || (((gs - 1) % 5) + 1) as variant_label,
    array[
        'https://cdn.example.test/skus/' || gs || '/main.webp'
    ] as image_urls,
    case
        when gs % 37 = 0 then 0::numeric(19, 2)
        else ((10000 + (gs % 900000))::numeric / 100)::numeric(19, 2)
    end as sale_price,
    case
        when gs % 3 = 0 then null
        else ((12000 + (gs % 900000))::numeric / 100)::numeric(19, 2)
    end as compare_price,
    case
        when gs % 50 = 0 then 'DISCONTINUED'
        when gs % 10 = 0 then 'INACTIVE'
        else 'ACTIVE'
    end as status,
    now() - ((gs % 730) || ' days')::interval as created_at,
    now() - ((gs % 30) || ' days')::interval as updated_at
from generate_series(1, 10000000) as gs;

select setval('catalog_categories_id_seq', 1000000, true);
select setval('catalog_brands_id_seq', 500000, true);
select setval('catalog_products_id_seq', 3000000, true);
select setval('catalog_product_skus_id_seq', 10000000, true);

create index idx_catalog_categories_featured_active_sort
    on catalog_categories (featured, sort_order, name, id)
    where status = 'ACTIVE';

create index idx_catalog_categories_parent_id
    on catalog_categories (parent_id)
    where parent_id is not null;

create index idx_catalog_brands_active_name
    on catalog_brands (name, id)
    where status = 'ACTIVE';

create index idx_catalog_products_home_new
    on catalog_products (status, published_at desc, id desc)
    include (category_id, brand_id, featured)
    where published_at is not null;

create index idx_catalog_products_home_featured
    on catalog_products (status, featured, published_at desc, id desc)
    include (category_id, brand_id)
    where published_at is not null;

create index idx_catalog_products_category_id
    on catalog_products (category_id);

create index idx_catalog_products_brand_id
    on catalog_products (brand_id)
    where brand_id is not null;

create index idx_catalog_product_skus_sellable_product_price
    on catalog_product_skus (product_id, sale_price)
    include (compare_price)
    where status = 'ACTIVE' and sale_price > 0;

create index idx_catalog_product_skus_product_status_price
    on catalog_product_skus (product_id, status, sale_price);

create index idx_catalog_product_skus_barcode
    on catalog_product_skus (barcode)
    where barcode is not null;

analyze catalog_categories;
analyze catalog_brands;
analyze catalog_products;
analyze catalog_product_skus;

commit;

-- Quick sanity checks. Each invalid_ref_count should be 0.
select 'products_missing_category' as check_name, count(*) as invalid_ref_count
from catalog_products p
left join catalog_categories c on c.id = p.category_id
where c.id is null
union all
select 'products_missing_brand' as check_name, count(*) as invalid_ref_count
from catalog_products p
left join catalog_brands b on b.id = p.brand_id
where b.id is null
union all
select 'skus_missing_product' as check_name, count(*) as invalid_ref_count
from catalog_product_skus s
left join catalog_products p on p.id = s.product_id
where p.id is null
union all
select 'categories_missing_parent' as check_name, count(*) as invalid_ref_count
from catalog_categories c
left join catalog_categories parent on parent.id = c.parent_id
where c.parent_id is not null and parent.id is null;

update catalog_products
set featured = (id % 53 = 0);
