USE edu_course;

CREATE TABLE IF NOT EXISTS t_user_behavior (
    id BIGINT NOT NULL,
    user_id BIGINT NULL,
    session_id VARCHAR(64) NULL,
    course_id BIGINT NOT NULL,
    behavior_type VARCHAR(32) NOT NULL,
    behavior_value DECIMAL(10,2) NULL,
    context_json JSON NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_behavior_user_time (user_id, created_at),
    KEY idx_behavior_session_time (session_id, created_at),
    KEY idx_behavior_course_type (course_id, behavior_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_recommendation_log (
    id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    session_id VARCHAR(64) NULL,
    course_id BIGINT NOT NULL,
    rank_position INT NOT NULL,
    score DECIMAL(8,4) NOT NULL,
    reason VARCHAR(500) NULL,
    clicked TINYINT NOT NULL DEFAULT 0,
    purchased TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_rec_request (request_id),
    KEY idx_rec_user_time (user_id, created_at),
    KEY idx_rec_course_time (course_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
