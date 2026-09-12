USE edu_course;

CREATE TABLE IF NOT EXISTS t_course_favorite (
    id         BIGINT   NOT NULL,
    user_id    BIGINT   NOT NULL,
    course_id  BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_user_course (user_id, course_id),
    KEY idx_favorite_course (course_id),
    KEY idx_favorite_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程收藏';

CREATE TABLE IF NOT EXISTS t_course_review (
    id         BIGINT        NOT NULL,
    user_id    BIGINT        NOT NULL,
    username   VARCHAR(50)   NOT NULL,
    course_id  BIGINT        NOT NULL,
    rating     TINYINT       NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    created_at DATETIME      NOT NULL,
    updated_at DATETIME      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_user_course (user_id, course_id),
    KEY idx_review_course_time (course_id, updated_at),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程评价';
