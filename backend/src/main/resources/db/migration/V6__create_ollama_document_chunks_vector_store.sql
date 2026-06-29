create table document_chunks_ollama (
    id uuid default uuid_generate_v4() primary key,
    content text not null,
    metadata json not null,
    embedding vector(768) not null
);

create index idx_document_chunks_ollama_embedding
    on document_chunks_ollama using hnsw (embedding vector_cosine_ops);
