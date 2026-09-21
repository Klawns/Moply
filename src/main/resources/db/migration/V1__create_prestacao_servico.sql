CREATE TABLE tb_prestacao_servico (
    id UUID PRIMARY KEY,
    cliente VARCHAR(255) NOT NULL,
    horas_contratadas NUMERIC(10, 2) NOT NULL,
    valor_hora NUMERIC(10, 2) NOT NULL,
    numero_de_colaboradores INTEGER NOT NULL,
    data_do_servico DATE NOT NULL
);

