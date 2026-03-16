CREATE TABLE recommendations (
    recommendation_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    book_id BIGINT NOT NULL REFERENCES books(book_id),
    reason TEXT,
    confidence_score DOUBLE PRECISION,
    is_ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);
