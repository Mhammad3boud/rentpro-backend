-- USERS
CREATE TABLE users (
  user_id UUID PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER','TENANT')),
  status BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- TENANTS (created by owner)
CREATE TABLE tenants (
  tenant_id UUID PRIMARY KEY,
  user_id UUID UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  owner_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  full_name VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  emergency_contact VARCHAR(255),
  address TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- PROPERTIES
CREATE TABLE properties (
  property_id UUID PRIMARY KEY,
  owner_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  property_name VARCHAR(255) NOT NULL,
  property_type VARCHAR(20) NOT NULL CHECK (property_type IN ('STANDALONE','MULTI_UNIT')),
  usage_type VARCHAR(20) CHECK (usage_type IN ('RESIDENTIAL','COMMERCIAL','MIXED')),
  address TEXT,
  region VARCHAR(100),
  postcode VARCHAR(20),
  latitude DECIMAL(10,8),
  longitude DECIMAL(11,8),
  water_meter_no VARCHAR(100),
  electricity_meter_no VARCHAR(100),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- UNITS
CREATE TABLE units (
  unit_id UUID PRIMARY KEY,
  property_id UUID NOT NULL REFERENCES properties(property_id) ON DELETE CASCADE,
  unit_number VARCHAR(50) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_units_property_unit UNIQUE(property_id, unit_number)
);

-- LEASES
CREATE TABLE leases (
  lease_id UUID PRIMARY KEY,
  property_id UUID NOT NULL REFERENCES properties(property_id) ON DELETE CASCADE,
  unit_id UUID REFERENCES units(unit_id) ON DELETE SET NULL,
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE RESTRICT,
  lease_name VARCHAR(255),
  monthly_rent DECIMAL(10,2) NOT NULL,
  start_date DATE,
  end_date DATE,
  lease_status VARCHAR(20) NOT NULL CHECK (lease_status IN ('ACTIVE','EXPIRED','TERMINATED')),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ONE ACTIVE LEASE PER UNIT (partial unique index)
CREATE UNIQUE INDEX uq_active_lease_per_unit
ON leases(property_id, unit_id)
WHERE lease_status = 'ACTIVE';

-- RENT PAYMENTS
CREATE TABLE rent_payments (
  payment_id UUID PRIMARY KEY,
  lease_id UUID NOT NULL REFERENCES leases(lease_id) ON DELETE CASCADE,
  month_year VARCHAR(7) NOT NULL,
  amount_expected DECIMAL(10,2) NOT NULL,
  amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0,
  due_date DATE,
  paid_date DATE,
  payment_method VARCHAR(20) CHECK (payment_method IN ('CASH','BANK_TRANSFER','CHECK')),
  payment_status VARCHAR(20) NOT NULL CHECK (payment_status IN ('PAID','PENDING','OVERDUE','PARTIAL')),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_payment_lease_month UNIQUE(lease_id, month_year)
);

-- MAINTENANCE REQUESTS
CREATE TABLE maintenance_requests (
  request_id UUID PRIMARY KEY,
  lease_id UUID NOT NULL REFERENCES leases(lease_id) ON DELETE CASCADE,
  property_id UUID NOT NULL REFERENCES properties(property_id) ON DELETE CASCADE,
  unit_id UUID REFERENCES units(unit_id) ON DELETE SET NULL,
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  title VARCHAR(255),
  description TEXT,
  priority VARCHAR(10) NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH')),
  image_url VARCHAR(500),
  status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','IN_PROGRESS','RESOLVED')),
  assigned_technician VARCHAR(255),
  maintenance_cost DECIMAL(10,2),
  reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP
);

-- AI PREDICTIONS
CREATE TABLE ai_predictions (
  prediction_id UUID PRIMARY KEY,
  lease_id UUID NOT NULL REFERENCES leases(lease_id) ON DELETE CASCADE,
  prediction_type VARCHAR(20) NOT NULL CHECK (prediction_type IN ('LATE_PAYMENT','MAINTENANCE_RISK')),
  risk_score DECIMAL(5,2) NOT NULL,
  risk_level VARCHAR(10) NOT NULL CHECK (risk_level IN ('LOW','MEDIUM','HIGH')),
  predicted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- NOTIFICATIONS
CREATE TABLE notifications (
  notification_id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  type VARCHAR(30) NOT NULL CHECK (type IN ('RENT_DUE','RENT_OVERDUE','MAINTENANCE_UPDATE','LEASE_EXPIRY')),
  title VARCHAR(255),
  message TEXT,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
