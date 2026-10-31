-- Only run on a freshly initialized mathdrill_demo; never on dev/test databases.
UPDATE mathdrill_demo.knowledge_point SET name='10以内加法',semester='口算练习',textbook='自编加法练习（非完整教材）' WHERE id=1;
INSERT INTO mathdrill_demo.knowledge_point(id,name,grade,semester,textbook) VALUES(2,'20以内加法（得数11至20）',1,'口算练习','自编加法练习（非完整教材）');
UPDATE mathdrill_demo.question SET point_id=2 WHERE id IN (12,13,14,15,16,17,18,19,20,30,31,32,33,34,35,36,37,38,39,47,48,49,50,51,52,53,54,55,56,62,63,64,65,66,67,68,69,70,71,75,76,77,78,79,80,81,82,83,84,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120);
UPDATE mathdrill_demo.question_revision r JOIN mathdrill_demo.question q ON q.id=r.question_id SET r.point_id=q.point_id;
