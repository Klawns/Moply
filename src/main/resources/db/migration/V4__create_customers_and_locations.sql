CREATE TABLE tb_customer (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    notes TEXT,
    CONSTRAINT chk_customer_name_not_blank CHECK (length(trim(name)) > 0)
);

CREATE TABLE tb_customer_location (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    name TEXT NOT NULL,
    address TEXT,
    notes TEXT,
    CONSTRAINT fk_customer_location_customer FOREIGN KEY (customer_id) REFERENCES tb_customer (id),
    CONSTRAINT chk_customer_location_name_not_blank CHECK (length(trim(name)) > 0)
);

CREATE INDEX idx_customer_location_customer_id ON tb_customer_location (customer_id);
