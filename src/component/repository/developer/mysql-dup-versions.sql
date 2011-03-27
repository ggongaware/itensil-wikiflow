SELECT v1.nodeId, nod.uri, v1.id, v1.vNumber, v2.id, v2.vNumber
FROM iten_repo_version v1 
LEFT JOIN iten_repo_node nod ON nod.id = v1.nodeId
LEFT JOIN iten_repo_version v2 ON (v1.nodeId = v2.nodeId AND v1.id <> v2.id)
WHERE v1.isDefault = 1 AND v2.isDefault = 1