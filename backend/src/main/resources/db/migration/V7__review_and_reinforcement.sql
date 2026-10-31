ALTER TABLE practice ADD practice_kind VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
 ADD seed_item_ids JSON NULL, ADD recipient_grade_at_preview INT NULL;
UPDATE practice SET recipient_grade_at_preview=original_grade WHERE source='PARENT';
CREATE TABLE practice_point (
 practice_id BIGINT NOT NULL, point_id BIGINT NOT NULL, point_revision BIGINT NOT NULL,
 original_grade INT NOT NULL, point_name VARCHAR(100) NOT NULL, total INT NOT NULL,
 PRIMARY KEY(practice_id,point_id,point_revision),
 FOREIGN KEY(practice_id) REFERENCES practice(id), FOREIGN KEY(point_id) REFERENCES knowledge_point(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO practice_point(practice_id,point_id,point_revision,original_grade,point_name,total)
 SELECT id,point_id,point_revision,original_grade,point_name,total FROM practice;
ALTER TABLE practice_item ADD point_id BIGINT NULL, ADD point_revision BIGINT NULL,
 ADD origin_item_id BIGINT NULL, ADD FOREIGN KEY(origin_item_id) REFERENCES practice_item(id);
UPDATE practice_item i JOIN practice p ON p.id=i.practice_id SET i.point_id=p.point_id,i.point_revision=p.point_revision;
