-- Debezium은 SUPERUSER로 둬서 outbox 테이블이 Hibernate ddl-auto로 나중에 생기는
-- 순서 문제(권한 부여 시점)를 피한다. 데모용 의도적 단순화.
CREATE ROLE debezium WITH LOGIN PASSWORD 'debezium' SUPERUSER;

CREATE DATABASE rag;
CREATE DATABASE voice;

\c rag

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
	id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
	content text,
	metadata jsonb,
	embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING HNSW (embedding vector_cosine_ops);
