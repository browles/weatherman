-- [1] Init
CREATE TABLE market_loan_offer_ingestions (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX market_loan_offer_ingestions_logged_at_idx ON market_loan_offer_ingestions(logged_at);

CREATE TABLE market_loan_offers (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  ingestion_id INTEGER NOT NULL,
  rate FLOAT NOT NULL,
  amount FLOAT NOT NULL,
  rangeMin INTEGER NOT NULL,
  rangeMax INTEGER NOT NULL,
  FOREIGN KEY(ingestion_id) REFERENCES market_loan_offer_ingestions(id)
);

CREATE TABLE loan_offers (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  order_id INTEGER NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  amount FLOAT NOT NULL,
  duration INTEGER NOT NULL,
  rate FLOAT NOT NULL
);

CREATE INDEX loan_offers_created_at_idx ON loan_offers(created_at);
