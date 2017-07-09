CREATE TABLE loan_order_ingestions (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX loan_order_ingestions_logged_at_idx ON loan_order_ingestions(logged_at);

CREATE TABLE loan_orders (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  ingestion_id INTEGER NOT NULL,
  rate FLOAT NOT NULL,
  amount FLOAT NOT NULL,
  rangeMin INTEGER NOT NULL,
  rangeMax INTEGER NOT NULL,
  FOREIGN KEY(ingestion_id) REFERENCES loan_order_ingestions(id)
);
