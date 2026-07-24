ALTER TABLE clients ADD COLUMN owner_id BIGINT NOT NULL REFERENCES users(id);
CREATE INDEX idx_clients_owner_id ON clients (owner_id);
