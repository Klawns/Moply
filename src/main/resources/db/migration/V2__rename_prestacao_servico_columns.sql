ALTER TABLE tb_prestacao_servico
    RENAME TO tb_order_service;

ALTER TABLE tb_order_service
    RENAME COLUMN cliente TO customer;

ALTER TABLE tb_order_service
    RENAME COLUMN horas_contratadas TO contracted_hours;

ALTER TABLE tb_order_service
    RENAME COLUMN valor_hora TO hourly_rate;

ALTER TABLE tb_order_service
    RENAME COLUMN numero_de_colaboradores TO employee_count;

ALTER TABLE tb_order_service
    RENAME COLUMN data_do_servico TO service_date;