CREATE TABLE knowledge_point (
 id BIGINT PRIMARY KEY, name VARCHAR(100) NOT NULL, grade INT NOT NULL, semester VARCHAR(40) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE question (
 id BIGINT PRIMARY KEY, point_id BIGINT NOT NULL, stem VARCHAR(200) NOT NULL,
 standard_answer VARCHAR(128) NOT NULL, explanation VARCHAR(500) NOT NULL, fingerprint CHAR(64) NOT NULL UNIQUE,
 reviewed BOOLEAN NOT NULL, enabled BOOLEAN NOT NULL,
 FOREIGN KEY(point_id) REFERENCES knowledge_point(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE practice (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, student_id BIGINT NOT NULL, point_id BIGINT NOT NULL,
 total INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', version BIGINT NOT NULL DEFAULT 0,
 editor_session_id VARCHAR(36), current_seq INT NOT NULL DEFAULT 1,
 create_key VARCHAR(64) NOT NULL, create_hash CHAR(64) NOT NULL, submit_key VARCHAR(64), submit_hash CHAR(64),
 correct_count INT, elapsed_ms BIGINT NOT NULL DEFAULT 0, timing_complete BOOLEAN NOT NULL DEFAULT TRUE,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), submitted_at DATETIME(3),
 UNIQUE(student_id,create_key), FOREIGN KEY(student_id) REFERENCES student_profile(account_id), FOREIGN KEY(point_id) REFERENCES knowledge_point(id),
 CHECK(total BETWEEN 1 AND 100), INDEX ix_practice_owner(student_id,status,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE practice_item (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, practice_id BIGINT NOT NULL, seq INT NOT NULL, question_id BIGINT NOT NULL,
 stem VARCHAR(200) NOT NULL, standard_answer VARCHAR(128) NOT NULL, explanation VARCHAR(500) NOT NULL,
 fingerprint CHAR(64) NOT NULL, raw VARCHAR(128) NOT NULL DEFAULT '', outcome VARCHAR(20),
 UNIQUE(practice_id,seq), UNIQUE(practice_id,fingerprint), FOREIGN KEY(practice_id) REFERENCES practice(id), FOREIGN KEY(question_id) REFERENCES question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE practice_receipt (
 practice_id BIGINT NOT NULL, kind VARCHAR(16) NOT NULL, operation_key VARCHAR(64) NOT NULL,
 payload_hash CHAR(64) NOT NULL, editor_session_id VARCHAR(36), response_json JSON NOT NULL,
 PRIMARY KEY(practice_id,kind,operation_key), FOREIGN KEY(practice_id) REFERENCES practice(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE practice_segment (
 practice_id BIGINT NOT NULL, editor_session_id VARCHAR(36) NOT NULL, segment_id VARCHAR(36) NOT NULL,
 cumulative_ms BIGINT NOT NULL DEFAULT 0, ended BOOLEAN NOT NULL DEFAULT FALSE,
 started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(practice_id,segment_id), FOREIGN KEY(practice_id) REFERENCES practice(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE mistake_record (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, student_id BIGINT NOT NULL, practice_id BIGINT NOT NULL, item_id BIGINT NOT NULL UNIQUE,
 fingerprint CHAR(64) NOT NULL, FOREIGN KEY(student_id) REFERENCES student_profile(account_id), FOREIGN KEY(practice_id) REFERENCES practice(id), FOREIGN KEY(item_id) REFERENCES practice_item(id), INDEX ix_mistake_owner(student_id,fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
