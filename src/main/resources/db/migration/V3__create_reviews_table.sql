CREATE TABLE reviews (
    review_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    book_id BIGINT NOT NULL REFERENCES books(book_id),
    review_text TEXT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    is_analysis_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT unique_user_book_review UNIQUE (user_id, book_id)
);
