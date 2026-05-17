CREATE TABLE reading_status (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    book_id BIGINT NOT NULL REFERENCES books(book_id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('READING', 'WILL_READ', 'READ')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT unique_user_book_status UNIQUE (user_id, book_id)
);
