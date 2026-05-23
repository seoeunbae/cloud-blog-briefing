-- Enable vector and UUID extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Standard Spring AI PgVectorStore table (using 768 dimensions for Google Vertex AI Embeddings)
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    metadata JSONB,
    embedding VECTOR(768)
);

-- Entity table representing GCP services, features, and concepts
CREATE TABLE IF NOT EXISTS gcp_entities (
    entity_id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    entity_type VARCHAR(50) NOT NULL, -- e.g., 'Service', 'Concept', 'Feature'
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Relationship table representing links between GCP services
CREATE TABLE IF NOT EXISTS gcp_relationships (
    relationship_id SERIAL PRIMARY KEY,
    source_entity_id VARCHAR(100) REFERENCES gcp_entities(entity_id) ON DELETE CASCADE,
    target_entity_id VARCHAR(100) REFERENCES gcp_entities(entity_id) ON DELETE CASCADE,
    relation_type VARCHAR(100) NOT NULL, -- e.g., 'RUNS', 'COMPETITOR_OF', 'IS_A', 'INTEGRATES_WITH'
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
