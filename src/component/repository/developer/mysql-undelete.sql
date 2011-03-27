-- for: synXrf4AAMnDfwAAAcjF
UPDATE iten_repo_node
SET uri = left(uri, char_length(uri) - 20), deleted = 0
WHERE uri LIKE '%LH3fbxABALHIQH9wSqI2'
AND deleted > 1;

