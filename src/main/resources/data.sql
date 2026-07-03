-- ============================================================
-- Sample seed data for the H2 in-memory database.
-- Transactions span several recent months so any date-range
-- query from 1 to 6 months back will return meaningful data.
-- ============================================================

-- Customers
INSERT INTO customers (customer_id, first_name, last_name, email)
VALUES
  ('C001', 'Alice',  'Johnson', 'alice.johnson@example.com'),
  ('C002', 'Bob',    'Martinez','bob.martinez@example.com'),
  ('C003', 'Carol',  'Smith',   'carol.smith@example.com'),
  ('C004', 'David',  'Lee',     'david.lee@example.com');

-- Transactions  (dates are relative offsets from today, expressed as fixed dates
--  that fall within the last 6 months at the time of initial project creation;
--  replace with CURRENT_DATE arithmetic if H2 dialect supports it)

-- C001 – Alice Johnson
INSERT INTO transactions (transaction_id, customer_id, amount, transaction_date) VALUES
  ('T001','C001', 120.00, DATEADD('MONTH', -1, CURRENT_DATE)),
  ('T002','C001',  75.50, DATEADD('MONTH', -1, CURRENT_DATE) - 10),
  ('T003','C001', 200.00, DATEADD('MONTH', -2, CURRENT_DATE)),
  ('T004','C001',  45.00, DATEADD('MONTH', -2, CURRENT_DATE) - 5),
  ('T005','C001', 130.75, DATEADD('MONTH', -3, CURRENT_DATE)),
  ('T006','C001',  88.00, DATEADD('MONTH', -3, CURRENT_DATE) - 10),
  ('T007','C001', 310.00, DATEADD('MONTH', -4, CURRENT_DATE)),
  ('T008','C001',  55.00, DATEADD('MONTH', -5, CURRENT_DATE));

-- C002 – Bob Martinez
INSERT INTO transactions (transaction_id, customer_id, amount, transaction_date) VALUES
  ('T009', 'C002',  95.00, DATEADD('MONTH', -1, CURRENT_DATE)),
  ('T010', 'C002', 150.00, DATEADD('MONTH', -1, CURRENT_DATE) - 14),
  ('T011', 'C002',  60.00, DATEADD('MONTH', -2, CURRENT_DATE)),
  ('T012', 'C002', 240.00, DATEADD('MONTH', -2, CURRENT_DATE) - 8),
  ('T013', 'C002',  30.00, DATEADD('MONTH', -3, CURRENT_DATE)),
  ('T014', 'C002', 175.50, DATEADD('MONTH', -3, CURRENT_DATE) - 14),
  ('T015', 'C002',  82.00, DATEADD('MONTH', -4, CURRENT_DATE)),
  ('T016', 'C002', 115.00, DATEADD('MONTH', -5, CURRENT_DATE));

-- C003 – Carol Smith
INSERT INTO transactions (transaction_id, customer_id, amount, transaction_date) VALUES
  ('T017','C003',  50.00, DATEADD('MONTH', -1, CURRENT_DATE)),
  ('T018','C003', 100.00, DATEADD('MONTH', -1, CURRENT_DATE) - 17),
  ('T019','C003', 400.00, DATEADD('MONTH', -2, CURRENT_DATE)),
  ('T020','C003',  70.00, DATEADD('MONTH', -2, CURRENT_DATE) - 14),
  ('T021','C003',  25.00, DATEADD('MONTH', -3, CURRENT_DATE)),
  ('T022','C003', 135.00, DATEADD('MONTH', -3, CURRENT_DATE) - 12),
  ('T023','C003',  90.00, DATEADD('MONTH', -4, CURRENT_DATE)),
  ('T024','C003', 210.00, DATEADD('MONTH', -5, CURRENT_DATE));

-- C004 – David Lee
INSERT INTO transactions (transaction_id, customer_id, amount, transaction_date) VALUES
  ('T025','C004', 500.00, DATEADD('MONTH', -1, CURRENT_DATE)),
  ('T026','C004',  65.00, DATEADD('MONTH', -1, CURRENT_DATE) - 23),
  ('T027','C004', 110.00, DATEADD('MONTH', -2, CURRENT_DATE)),
  ('T028','C004',  48.00, DATEADD('MONTH', -3, CURRENT_DATE)),
  ('T029','C004', 155.00, DATEADD('MONTH', -3, CURRENT_DATE) - 24),
  ('T030','C004',  77.50, DATEADD('MONTH', -4, CURRENT_DATE));
