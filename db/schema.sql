CREATE TABLE loan_offer_ingestions (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX loan_offer_ingestions_logged_at_idx ON loan_offer_ingestions(logged_at);

CREATE TABLE loan_offers (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  ingestion_id INTEGER NOT NULL,
  rate FLOAT NOT NULL,
  amount FLOAT NOT NULL,
  rangeMin INTEGER NOT NULL,
  rangeMax INTEGER NOT NULL,
  FOREIGN KEY(ingestion_id) REFERENCES loan_offer_ingestions(id)
);
