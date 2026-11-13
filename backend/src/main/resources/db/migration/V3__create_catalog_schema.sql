-- Catalog: disciplines and the technique engine.
-- Technique-specific behaviour lives in JSONB so a new discipline does not need a migration.

CREATE TABLE disciplines (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(2000),
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_disciplines_code UNIQUE (code),
    CONSTRAINT ck_disciplines_code CHECK (code ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE TABLE techniques (
    id UUID PRIMARY KEY,
    discipline_id UUID NOT NULL REFERENCES disciplines (id),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    difficulty VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    definition_schema_version INTEGER NOT NULL DEFAULT 1,
    movement_definition JSONB NOT NULL,
    required_landmarks JSONB NOT NULL,
    evaluation_criteria JSONB NOT NULL,
    scoring_rules JSONB,
    analyzer_config JSONB,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_techniques_discipline_code UNIQUE (discipline_id, code),
    CONSTRAINT ck_techniques_difficulty
        CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    CONSTRAINT ck_techniques_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_techniques_definition_version CHECK (definition_schema_version >= 1)
);

CREATE INDEX idx_techniques_discipline_status ON techniques (discipline_id, status);
CREATE INDEX idx_techniques_status_difficulty ON techniques (status, difficulty);
