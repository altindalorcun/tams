CREATE TABLE category_prefix_limits (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id         UUID        NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    course_code_prefix  VARCHAR(10) NOT NULL,
    max_count           INTEGER     NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_category_prefix_limits_category_id ON category_prefix_limits (category_id);
