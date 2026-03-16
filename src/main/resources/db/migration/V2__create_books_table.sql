CREATE TABLE books (
    book_id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    publication_year INT,
    avg_rating DOUBLE PRECISION DEFAULT 0.0,
    review_count INT DEFAULT 0,
    description TEXT
);
