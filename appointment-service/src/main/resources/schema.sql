-- For a clean slate if you're frequently restarting for development
DROP TABLE IF EXISTS appointment;

CREATE TABLE IF NOT EXISTS appointment
(
    id                    UUID PRIMARY KEY,
    patient_id            UUID        NOT NULL, -- Foreign key to the patient service's patient table
    doctor_id             UUID        NOT NULL, -- Foreign key to a potential doctor service's doctor table
    appointment_date_time TIMESTAMP   NOT NULL,
    status                VARCHAR(50) NOT NULL  -- e.g., 'SCHEDULED', 'CANCELED', 'COMPLETED', 'RESCHEDULED', 'PENDING'
);

-- Optional: Add some initial sample data for appointments
-- Make sure the patient_id values here actually exist in your patient-service's patient table
-- (or match the UUIDs you plan to use for testing if patient-service is not yet running/connected)

INSERT INTO appointment (id, patient_id, doctor_id, appointment_date_time, status)
SELECT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', -- Sample appointment ID
       '123e4567-e89b-12d3-a456-426614174000', -- Patient ID (John Doe from your patient-service sample data)
       'd1e2f3g4-h5i6-j7k8-l9m0-n1o2p3q4r5s6', -- Sample Doctor ID (replace with real doctor ID if you have one)
       '2025-07-01 10:00:00',
       'SCHEDULED'
WHERE NOT EXISTS (SELECT 1 FROM appointment WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11');

INSERT INTO appointment (id, patient_id, doctor_id, appointment_date_time, status)
SELECT 'b1c2d3e4-f5a6-7b8c-9d0e-1f2a3b4c5d6e',
       '123e4567-e89b-12d3-a456-426614174001', -- Patient ID (Jane Smith)
       'd1e2f3g4-h5i6-j7k8-l9m0-n1o2p3q4r5s6',
       '2025-07-01 11:00:00',
       'SCHEDULED'
WHERE NOT EXISTS (SELECT 1 FROM appointment WHERE id = 'b1c2d3e4-f5a6-7b8c-9d0e-1f2a3b4c5d6e');

INSERT INTO appointment (id, patient_id, doctor_id, appointment_date_time, status)
SELECT 'c2d3e4f5-a6b7-8c9d-0e1f-2a3b4c5d6e7f',
       '123e4567-e89b-12d3-a456-426614174000', -- John Doe
       'd1e2f3g4-h5i6-j7k8-l9m0-n1o2p3q4r5s6',
       '2025-06-25 14:30:00',
       'COMPLETED'
WHERE NOT EXISTS (SELECT 1 FROM appointment WHERE id = 'c2d3e4f5-a6b7-8c9d-0e1f-2a3b4c5d6e7f');

INSERT INTO appointment (id, patient_id, doctor_id, appointment_date_time, status)
SELECT 'd3e4f5a6-b7c8-9d0e-1f2a-3b4c5d6e7f8a',
       '123e4567-e89b-12d3-a456-426614174002', -- Alice Johnson
       'd1e2f3g4-h5i6-j7k8-l9m0-n1o2p3q4r5s6',
       '2025-07-05 09:00:00',
       'CANCELED'
WHERE NOT EXISTS (SELECT 1 FROM appointment WHERE id = 'd3e4f5a6-b7c8-9d0e-1f2a-3b4c5d6e7f8a');