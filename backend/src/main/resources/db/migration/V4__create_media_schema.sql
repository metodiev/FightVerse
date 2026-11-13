-- Media metadata only. Binary content belongs in S3-compatible object storage, never in PostgreSQL.

CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    kind VARCHAR(32) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    byte_size BIGINT NOT NULL,
    checksum_sha256 CHAR(64),
    owner_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_media_assets_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_media_assets_kind CHECK (kind IN ('VIDEO', 'IMAGE', 'MODEL_3D', 'DOCUMENT')),
    CONSTRAINT ck_media_assets_size CHECK (byte_size > 0)
);

CREATE INDEX idx_media_assets_owner ON media_assets (owner_user_id);

CREATE TABLE technique_media (
    technique_id UUID NOT NULL REFERENCES techniques (id) ON DELETE CASCADE,
    media_asset_id UUID NOT NULL REFERENCES media_assets (id) ON DELETE CASCADE,
    media_role VARCHAR(32) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (technique_id, media_asset_id, media_role),
    CONSTRAINT ck_technique_media_role
        CHECK (media_role IN ('DEMONSTRATION', 'REFERENCE', 'ANNOTATED', 'AI_OVERLAY'))
);
