alter table cases
    add column active boolean not null default true,
    add column closed_at timestamp with time zone,
    add column archived_at timestamp with time zone;

create index idx_cases_active_created_at on cases (active, created_at desc);
create index idx_cases_closed_archival on cases (status, active, closed_at);
