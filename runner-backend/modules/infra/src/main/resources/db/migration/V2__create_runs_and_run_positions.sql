CREATE TABLE runs (
    id               UUID         PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users(id),
    started_at       TIMESTAMPTZ  NOT NULL,
    duration_seconds INTEGER      NOT NULL,
    distance_km      NUMERIC(6,3) NOT NULL,
    pace_sec_per_km  INTEGER      NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE run_positions (
    id      BIGSERIAL        PRIMARY KEY,
    run_id  UUID             NOT NULL REFERENCES runs(id),
    seq     INTEGER          NOT NULL,
    lat     DOUBLE PRECISION NOT NULL,
    lon     DOUBLE PRECISION NOT NULL
);
