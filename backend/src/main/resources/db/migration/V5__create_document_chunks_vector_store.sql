create extension if not exists vector;
create extension if not exists hstore;
create extension if not exists "uuid-ossp";

create table document_chunks (
    id uuid default uuid_generate_v4() primary key,
    content text not null,
    metadata json not null,
    embedding vector(1536) not null
);

create index idx_document_chunks_embedding
    on document_chunks using hnsw (embedding vector_cosine_ops);
