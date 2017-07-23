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

-- [2] Add ticker
CREATE TABLE ticker_ingestions (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  api_start TIMESTAMP NOT NULL,
  api_end TIMESTAMP NOT NULL
);

CREATE INDEX ticker_ingestions_api_end_idx ON ticker_ingestions(api_end);

CREATE TABLE ticker (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  ingestion_id INTEGER NOT NULL,
  currency_pair TEXT NOT NULL,
  last REAL NOT NULL,
  highest_bid REAL NOT NULL,
  lowest_ask REAL NOT NULL,
  FOREIGN KEY(ingestion_id) REFERENCES ticker_ingestions(id)
);

CREATE INDEX ticker_currency_pair_idx ON ticker(currency_pair);

-- [3] Add ingestion_id index
CREATE INDEX ticker_ingestion_id_idx ON ticker(ingestion_id);
