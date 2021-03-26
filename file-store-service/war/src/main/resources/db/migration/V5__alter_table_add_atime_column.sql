ALTER TABLE file_attributes ADD COLUMN atime TIMESTAMP WITH TIME ZONE DEFAULT NULL;
UPDATE file_attributes SET atime=now();