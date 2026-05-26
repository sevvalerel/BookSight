-- V8__create_nlp_results.sql

CREATE TABLE IF NOT EXISTS nlp_results (
                                           id           BIGSERIAL PRIMARY KEY,
                                           review_id    BIGINT NOT NULL UNIQUE REFERENCES reviews(review_id) ON DELETE CASCADE,
    detected_labels JSONB,
    label_scores    JSONB,
    processed_at VARCHAR(50)
    );

CREATE INDEX idx_nlp_results_review_id ON nlp_results(review_id);