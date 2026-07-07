alter table cases
    add column agent_suggested_action varchar(40),
    add column agent_recommendation_notes text,
    add column recommended_by bigint references users (id),
    add column recommended_at timestamp with time zone;

alter table cases
    add constraint cases_agent_suggested_action_check check (
        agent_suggested_action is null
        or agent_suggested_action in ('REFUND', 'COMPENSATION', 'REPLACEMENT', 'REJECT_REQUEST', 'ESCALATE')
    );

create index idx_cases_recommended_by on cases (recommended_by);
create index idx_cases_recommended_at on cases (recommended_at desc);
