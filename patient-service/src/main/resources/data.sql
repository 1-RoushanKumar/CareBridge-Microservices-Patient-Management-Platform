-- Ensure the 'patient' table exists
DROP TABLE IF EXISTS patient CASCADE; -- Added CASCADE to handle dependencies cleanly

CREATE TABLE IF NOT EXISTS patient
(
    id                 UUID PRIMARY KEY,
    name               VARCHAR(255)        NOT NULL,
    email              VARCHAR(255) UNIQUE NOT NULL,
    address            VARCHAR(255)        NOT NULL,
    date_of_birth      DATE                NOT NULL,
    registered_date    DATE                NOT NULL,
    contact            VARCHAR(15) UNIQUE  NOT NULL,
    gender             VARCHAR(10)         NOT NULL,
    emergency_contact  VARCHAR(15)         NOT NULL,
    -- Add the new column here with a default value and NOT NULL constraint
    billing_account_status VARCHAR(255) NOT NULL DEFAULT 'PENDING_ACCOUNT_CREATION'
);

-- Your INSERT statements follow, and they will pick up the default value automatically for this column.
-- ... (rest of your INSERT statements) ...

-- Insert well-known UUIDs for specific patients
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '123e4567-e89b-12d3-a456-426614174000',
       'John Doe',
       'john.doe@example.com',
       '123 Main St, Springfield',
       '1985-06-15',
       '2024-01-10',
       '9876543210', -- Sample contact
       'Male',       -- Sample gender
       '0123456789'  -- Sample emergency contact
WHERE NOT EXISTS (SELECT 1
                  FROM patient
                  WHERE id = '123e4567-e89b-12d3-a456-426614174000');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '123e4567-e89b-12d3-a456-426614174001',
       'Jane Smith',
       'jane.smith@example.com',
       '456 Elm St, Shelbyville',
       '1990-09-23',
       '2023-12-01',
       '9876543211',
       'Female',
       '0123456790'
WHERE NOT EXISTS (SELECT 1
                  FROM patient
                  WHERE id = '123e4567-e89b-12d3-a456-426614174001');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '123e4567-e89b-12d3-a456-426614174002',
       'Alice Johnson',
       'alice.johnson@example.com',
       '789 Oak St, Capital City',
       '1978-03-12',
       '2022-06-20',
       '9876543212',
       'Female',
       '0123456791'
WHERE NOT EXISTS (SELECT 1
                  FROM patient
                  WHERE id = '123e4567-e89b-12d3-a456-426614174002');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '123e4567-e89b-12d3-a456-426614174003',
       'Bob Brown',
       'bob.brown@example.com',
       '321 Pine St, Springfield',
       '1982-11-30',
       '2023-05-14',
       '9876543213',
       'Male',
       '0123456792'
WHERE NOT EXISTS (SELECT 1
                  FROM patient
                  WHERE id = '123e4567-e89b-12d3-a456-426614174003');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '123e4567-e89b-12d3-a456-426614174004',
       'Emily Davis',
       'emily.davis@example.com',
       '654 Maple St, Shelbyville',
       '1995-02-05',
       '2024-03-01',
       '9876543214',
       'Female',
       '0123456793'
WHERE NOT EXISTS (SELECT 1
                  FROM patient
                  WHERE id = '123e4567-e89b-12d3-a456-426614174004');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '223e4567-e89b-12d3-a456-426614174005',
       'Michael Green',
       'michael.green@example.com',
       '987 Cedar St, Springfield',
       '1988-07-25',
       '2024-02-15',
       '9876543215',
       'Male',
       '0123456794'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174005');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '223e4567-e89b-12d3-a456-426614174006',
       'Sarah Taylor',
       'sarah.taylor@example.com',
       '123 Birch St, Shelbyville',
       '1992-04-18',
       '2023-08-25',
       '9876543216',
       'Female',
       '0123456795'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174006');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '223e4567-e89b-12d3-a456-426614174007',
       'David Wilson',
       'david.wilson@example.com',
       '456 Ash St, Capital City',
       '1975-01-11',
       '2022-10-10',
       '9876543217',
       'Male',
       '0123456796'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174007');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '223e4567-e89b-12d3-a456-426614174008',
       'Laura White',
       'laura.white@example.com',
       '789 Palm St, Springfield',
       '1989-09-02',
       '2024-04-20',
       '9876543218',
       'Female',
       '0123456797'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174008');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '223e4567-e89b-12d3-a456-426614174009',
       'James Harris',
       'james.harris@example.com',
       '321 Cherry St, Shelbyville',
       '1993-11-15',
       '2023-06-30',
       '9876543219',
       'Male',
       '0123456798'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174009');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '223e4567-e89b-12d3-a456-426614174010',
       'Emma Moore',
       'emma.moore@example.com',
       '654 Spruce St, Capital City',
       '1980-08-09',
       '2023-01-22',
       '9876543220',
       'Female',
       '0123456799'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174010');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '223e4567-e89b-12d3-a456-426614174011',
       'Ethan Martinez',
       'ethan.martinez@example.com',
       '987 Redwood St, Springfield',
       '1984-05-03',
       '2024-05-12',
       '9876543221',
       'Male',
       '0123456800'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174011');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '223e4567-e89b-12d3-a456-426614174012',
       'Sophia Clark',
       'sophia.clark@example.com',
       '123 Hickory St, Shelbyville',
       '1991-12-25',
       '2022-11-11',
       '9876543222',
       'Female',
       '0123456801'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174012');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '223e4567-e89b-12d3-a456-426614174013',
       'Daniel Lewis',
       'daniel.lewis@example.com',
       '456 Cypress St, Capital City',
       '1976-06-08',
       '2023-09-19',
       '9876543223',
       'Male',
       '0123456802'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174013');

INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, contact, gender, emergency_contact)
SELECT '223e4567-e89b-12d3-a456-426614174014',
       'Isabella Walker',
       'isabella.walker@example.com',
       '789 Willow St, Springfield',
       '1987-10-17',
       '2024-03-29',
       '9876543224',
       'Female',
       '0123456803'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174014');