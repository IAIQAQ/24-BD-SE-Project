CREATE TABLE IF NOT EXISTS parent_profile(account_id BIGINT PRIMARY KEY,nickname VARCHAR(64) NOT NULL,FOREIGN KEY(account_id) REFERENCES sys_account(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS family_link(id BIGINT PRIMARY KEY AUTO_INCREMENT,parent_id BIGINT NOT NULL,student_id BIGINT NOT NULL,state VARCHAR(16) NOT NULL DEFAULT 'PENDING',UNIQUE(parent_id,student_id),FOREIGN KEY(parent_id) REFERENCES parent_profile(account_id),FOREIGN KEY(student_id) REFERENCES student_profile(account_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 支持从专用测试库一次失败的DDL继续；未修改任何已成功发布迁移。
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='practice' AND column_name='source')=0,
'ALTER TABLE practice ADD source VARCHAR(16) NOT NULL DEFAULT ''SELF'',ADD sender_id BIGINT NULL,ADD original_grade INT NOT NULL DEFAULT 1,ADD point_name VARCHAR(100) NOT NULL DEFAULT ''20以内加法（综合）'',ADD content_error BOOLEAN NOT NULL DEFAULT FALSE,ADD error_note VARCHAR(500) NOT NULL DEFAULT '''',MODIFY create_key VARCHAR(128) NOT NULL,ADD FOREIGN KEY(sender_id) REFERENCES parent_profile(account_id)','SELECT 1');
PREPARE migration_ddl FROM @ddl; EXECUTE migration_ddl; DEALLOCATE PREPARE migration_ddl;
UPDATE practice p JOIN knowledge_point k ON k.id=p.point_id SET p.original_grade=k.grade,p.point_name=k.name;
ALTER TABLE question ADD revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE knowledge_point ADD textbook VARCHAR(256) NOT NULL DEFAULT '内部综合范围（待教材审核）',ADD evidence VARCHAR(1000) NOT NULL DEFAULT 'https://www.moe.gov.cn/srcsite/A26/s8001/202204/W020220420582346895190.pdf';
ALTER TABLE practice_item ADD source_revision BIGINT NOT NULL DEFAULT 1;
CREATE TABLE question_revision(question_id BIGINT NOT NULL,revision BIGINT NOT NULL,point_id BIGINT NOT NULL,stem VARCHAR(200) NOT NULL,standard_answer VARCHAR(128) NOT NULL,explanation VARCHAR(500) NOT NULL,fingerprint CHAR(64) NOT NULL,is_error BOOLEAN NOT NULL DEFAULT FALSE,error_note VARCHAR(500) NOT NULL DEFAULT '',PRIMARY KEY(question_id,revision),FOREIGN KEY(question_id) REFERENCES question(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO question_revision(question_id,revision,point_id,stem,standard_answer,explanation,fingerprint) SELECT id,revision,point_id,stem,standard_answer,explanation,fingerprint FROM question;
