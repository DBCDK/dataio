create table sink_record_delivery_watermark (
    sink_id       int          not null,
    record_key    varchar      not null,
    job_id        int          not null,
    chunk_id      int          not null,
    item_id       smallint     not null,
    last_modified timestamptz  not null default now(),
    primary key (sink_id, record_key)
);

create index sink_record_delivery_watermark_last_modified_index
    on sink_record_delivery_watermark (last_modified);
