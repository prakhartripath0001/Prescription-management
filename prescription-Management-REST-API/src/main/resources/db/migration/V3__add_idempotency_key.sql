-- V3: Add idempotency_key column to prescription_records
-- ─────────────────────────────────────────────────────────────────────────────
-- Idempotency strategy: callers may supply an X-Idempotency-Key header (UUID)
-- on POST /api/v1/records. If the same key is sent again, the API returns the
-- original record instead of creating a duplicate. The DB unique constraint acts
-- as the last line of defence against concurrent duplicate submissions.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE prescription_records
    ADD COLUMN idempotency_key VARCHAR(64) NULL
        COMMENT 'Client-supplied deduplication key (UUID). NULL = no idempotency protection.',
    ADD CONSTRAINT uk_record_idempotency_key
        UNIQUE (idempotency_key);

