BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "messages" (
	"text"	TEXT NOT NULL UNIQUE,
	"timestamp"	REAL NOT NULL,
	"user"	NUMERIC NOT NULL,
	"channel"	TEXT NOT NULL,
	"client_message_id"	TEXT,
	PRIMARY KEY("text")
);
COMMIT;
