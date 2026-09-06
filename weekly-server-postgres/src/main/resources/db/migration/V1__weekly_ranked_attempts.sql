CREATE TABLE IF NOT EXISTS weekly_ranked_attempts (
    challenge_id VARCHAR(128) NOT NULL,
    subject_key VARCHAR(256) NOT NULL,
    score INTEGER NOT NULL CHECK (score >= 0),
    max_tile_level INTEGER NOT NULL CHECK (max_tile_level >= 0),
    protocol_version INTEGER NOT NULL CHECK (protocol_version > 0),
    accepted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (challenge_id, subject_key)
);

CREATE INDEX IF NOT EXISTS weekly_ranked_attempts_challenge_score_idx
    ON weekly_ranked_attempts (challenge_id, score DESC);
