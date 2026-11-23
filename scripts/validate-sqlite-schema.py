from pathlib import Path
import sqlite3

root = Path(__file__).resolve().parents[1]
schema = (root / "src/main/resources/schema.sql").read_text(encoding="utf-8")
connection = sqlite3.connect(":memory:")
connection.execute("PRAGMA foreign_keys = ON")
connection.executescript(schema)

expected = {"customers", "user_credentials", "branches", "accounts", "transactions", "transaction_requests"}
actual = {row[0] for row in connection.execute("SELECT name FROM sqlite_master WHERE type='table'")}
assert expected.issubset(actual), f"Missing tables: {expected - actual}"
assert connection.execute("SELECT branch_code FROM branches ORDER BY branch_code").fetchall() == [("FRT",), ("GAB",), ("PAL",)]
print("PASS: schema creates the bank tables and all three branch records")

connection.execute(
    "INSERT INTO customers VALUES(?,?,?,?,?,?,?,?,?,?)",
    ("C1", "Test", "Person", "Address", "71234567", "test@example.bw", 1, "GRS", "Francistown", "2026-11-10T10:00:00"),
)
connection.execute(
    "INSERT INTO user_credentials VALUES(?,?,?,?,?,?,?,?)",
    ("U1", "testuser", "hash", "CUSTOMER", "C1", 1, 0, "2026-11-10T10:00:00"),
)
connection.execute(
    "INSERT INTO user_credentials VALUES(?,?,?,?,?,?,?,?)",
    ("U2", "manager1", "hash", "MANAGER", None, 1, 1, "2026-11-10T10:01:00"),
)
connection.execute(
    "INSERT INTO user_credentials VALUES(?,?,?,?,?,?,?,?)",
    ("U3", "teller1", "hash", "TELLER", None, 1, 0, "2026-11-10T10:02:00"),
)
connection.execute(
    "INSERT INTO accounts VALUES(?,?,?,?,?,?,?,?)",
    ("FRT-A1", "C1", "INVESTMENT", 500.00, "Francistown", "FRT", "ACTIVE", "2026-11-10"),
)
assert connection.execute("SELECT must_change_password FROM user_credentials WHERE user_id='U2'").fetchone()[0] == 1
print("PASS: manager role, teller role and branch-coded accounts are supported")

connection.execute(
    "INSERT INTO transaction_requests(request_id,account_number,destination_account_number,transaction_type,amount,submitted_at,reference,status,submitted_by_user_id) VALUES(?,?,?,?,?,?,?,?,?)",
    ("R1", "FRT-A1", None, "DEPOSIT", 100.00, "2026-11-10T10:05:00", "REQ-1", "PENDING", "U1"),
)
assert connection.execute("SELECT balance FROM accounts WHERE account_number='FRT-A1'").fetchone()[0] == 500
print("PASS: pending transaction requests can be created without changing account balances")
connection.commit()

connection.execute("BEGIN")
connection.execute("UPDATE accounts SET balance=? WHERE account_number=?", (600.00, "FRT-A1"))
connection.execute(
    "INSERT INTO transactions VALUES(?,?,?,?,?,?,?)",
    ("T1", "FRT-A1", "DEPOSIT", 100.00, "2026-11-10T10:06:00", "DEP-1", 600.00),
)
connection.execute("UPDATE transaction_requests SET status='APPROVED',reviewed_by_user_id='U3',reviewed_at=? WHERE request_id='R1'", ("2026-11-10T10:06:00",))
connection.commit()
assert connection.execute("SELECT balance FROM accounts WHERE account_number='FRT-A1'").fetchone()[0] == 600
assert connection.execute("SELECT status FROM transaction_requests WHERE request_id='R1'").fetchone()[0] == "APPROVED"
print("PASS: an approved request can be recorded with the resulting balance and ledger entry")

try:
    connection.execute(
        "INSERT INTO transactions VALUES(?,?,?,?,?,?,?)",
        ("T2", "FRT-A1", "DEPOSIT", 10.00, "2026-11-10T10:07:00", "DEP-1", 610.00),
    )
    connection.commit()
    raise AssertionError("Duplicate transaction reference was accepted")
except sqlite3.IntegrityError:
    connection.rollback()
print("PASS: duplicate transaction references are rejected")

try:
    connection.execute(
        "INSERT INTO accounts VALUES(?,?,?,?,?,?,?,?)",
        ("FRT-A2", "C1", "SAVINGS", -1, "Francistown", "FRT", "ACTIVE", "2026-11-10"),
    )
    connection.commit()
    raise AssertionError("Negative account balance was accepted")
except sqlite3.IntegrityError:
    connection.rollback()
print("PASS: negative account balances are rejected")

try:
    connection.execute(
        "INSERT INTO transaction_requests(request_id,account_number,destination_account_number,transaction_type,amount,submitted_at,reference,status,submitted_by_user_id) VALUES(?,?,?,?,?,?,?,?,?)",
        ("R2", "FRT-A1", None, "TRANSFER", 10.00, "2026-11-10T10:08:00", "REQ-2", "PENDING", "U1"),
    )
    connection.commit()
    raise AssertionError("A transfer without a destination account was accepted")
except sqlite3.IntegrityError:
    connection.rollback()
print("PASS: transfers require a destination account")
print("RESULT: 7 schema checks passed using SQLite " + sqlite3.sqlite_version)
