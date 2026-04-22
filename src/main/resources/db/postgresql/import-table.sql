create table if not exists excel_import_records (
    id bigserial primary key,
    employee_code varchar(32) not null,
    full_name varchar(128) not null,
    email varchar(180) not null,
    age integer not null,
    department varchar(64) not null,
    salary numeric(18, 2) not null,
    join_date date not null,
    active boolean not null
);

create index if not exists idx_excel_import_records_employee_code
    on excel_import_records (employee_code);

create index if not exists idx_excel_import_records_email
    on excel_import_records (email);

create table if not exists async_excel_import_jobs (
    id uuid primary key,
    status varchar(32) not null,
    reader varchar(64) not null,
    object_key varchar(512) not null,
    file_name varchar(255) not null,
    content_type varchar(128),
    file_size bigint,
    reset_table boolean not null,
    processed_rows integer not null default 0,
    imported_rows integer not null default 0,
    failed_rows integer not null default 0,
    message varchar(1000),
    version bigint not null default 0,
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone,
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    updated_at timestamp with time zone not null
);

create index if not exists idx_async_excel_import_jobs_status
    on async_excel_import_jobs (status);

create index if not exists idx_async_excel_import_jobs_created_at
    on async_excel_import_jobs (created_at);

    create table if not exists async_excel_import_jobs (
        id uuid primary key,
        status varchar(32) not null,
        reader varchar(64) not null,
        object_key varchar(512) not null,
        file_name varchar(255) not null,
        content_type varchar(128),
        file_size bigint,
        reset_table boolean not null,
        processed_rows integer not null default 0,
        imported_rows integer not null default 0,
        failed_rows integer not null default 0,
        message varchar(1000),
        version bigint not null default 0,
        created_at timestamp with time zone not null,
        expires_at timestamp with time zone,
        started_at timestamp with time zone,
        finished_at timestamp with time zone,
        updated_at timestamp with time zone not null
    );

    create index if not exists idx_async_excel_import_jobs_status
        on async_excel_import_jobs (status);

    create index if not exists idx_async_excel_import_jobs_created_at
        on async_excel_import_jobs (created_at);
