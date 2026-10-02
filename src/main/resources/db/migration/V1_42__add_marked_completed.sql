ALTER TABLE klanke
    ADD COLUMN marked_completed TIMESTAMP;

CREATE INDEX idx_klanke_status_sending ON klanke (id) WHERE status = 'SENDING';
