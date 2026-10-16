
-- Test users (prescribers)
INSERT INTO users (name, registration_number, phone_number) VALUES
    ('Dr. Alice Johnson',  'REG-2024-001', '+1-555-100-0001'),
    ('Dr. Bob Williams',   'REG-2024-002', '+1-555-100-0002'),
    ('Dr. Carol Martinez', 'REG-2024-003', '+1-555-100-0003');

-- Test clients (patients)
INSERT INTO clients (name, date_of_birth, phone_number) VALUES
    ('James Wilson',   '1985-03-12', '+1-555-200-0001'),
    ('Sarah Connor',   '1992-07-28', '+1-555-200-0002'),
    ('Michael Brown',  '1978-11-05', '+1-555-200-0003'),
    ('Emily Davis',    '2000-01-19', '+1-555-200-0004');

-- Sample prescription records
INSERT INTO prescription_records (user_id, client_id, record_date, diagnosis, status) VALUES
    (1, 1, '2026-10-01', 'Seasonal allergic rhinitis',        'ACTIVE'),
    (1, 2, '2026-10-05', 'Mild hypertension — stage 1',       'ACTIVE'),
    (2, 3, '2026-10-10', 'Type 2 diabetes — initial consult', 'DRAFT'),
    (3, 4, '2026-10-14', 'Acute upper respiratory infection',  'COMPLETED');

-- Sample record entries (medications / items per record)
INSERT INTO record_entries (record_id, entry_name, quantity, frequency, duration_days, instructions) VALUES
    -- Record 1 entries
    (1, 'Loratadine 10mg',       1.00, 'Once daily',              30,  'Take in the morning'),
    (1, 'Fluticasone nasal spray', 2.00, 'Twice daily',           30,  '2 sprays each nostril'),

    -- Record 2 entries
    (2, 'Amlodipine 5mg',        1.00, 'Once daily',              90,  'Take with water'),
    (2, 'Hydrochlorothiazide 25mg', 1.00, 'Once daily (morning)', 90,  'Monitor blood pressure weekly'),

    -- Record 3 entries
    (3, 'Metformin 500mg',       2.00, 'Twice daily with meals',  60,  'Check blood glucose daily'),

    -- Record 4 entries
    (4, 'Amoxicillin 500mg',     1.00, 'Three times daily',        7,  'Complete full course'),
    (4, 'Paracetamol 500mg',     2.00, 'Every 6 hours if needed',  5,  'Max 8 tablets per day'),
    (4, 'Saline nasal spray',    3.00, 'Three times daily',         7,  'Tilt head slightly forward');

