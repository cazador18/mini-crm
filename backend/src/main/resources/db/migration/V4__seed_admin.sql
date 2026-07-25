-- Bootstrap ADMIN account: username=admin, password=admin123.
-- Change this password immediately after first login in any real deployment.
INSERT INTO users (username, email, password_hash, role, enabled)
VALUES ('admin', 'admin@minicrm.local', '$2a$10$8qyUzN6AQ8S59qUO5yuIZO8/vNgNSc3oPNGUJ0tIrhtcJIC0PBD9q', 'ADMIN', TRUE);
