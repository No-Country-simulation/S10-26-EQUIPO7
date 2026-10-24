-- Esquema de Base de Datos - RepuSense (PostgreSQL)

CREATE TABLE IF NOT EXISTS business (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(50) NOT NULL,
    source_business_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    rating NUMERIC(3,2),
    review_count INT DEFAULT 0,
    phone VARCHAR(50),
    website VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_source_business UNIQUE (source, source_business_id)
);

CREATE TABLE IF NOT EXISTS lead (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT REFERENCES business(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    contact_email VARCHAR(255),
    ai_main_problem TEXT,
    ai_email_subject VARCHAR(255),
    ai_email_opening TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS search_run (
    id BIGSERIAL PRIMARY KEY,
    city VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    found_count INT DEFAULT 0,
    saved_count INT DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITH TIME ZONE
);