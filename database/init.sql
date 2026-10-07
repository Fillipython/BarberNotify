-- Script de Inicializacao da Base de Dados: Barber Man

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Tabela de Barbeiros / Profissionais (Barbeiro Master como fundador e administrador geral)
CREATE TABLE IF NOT EXISTS barbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    nickname VARCHAR(50),
    phone VARCHAR(25) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    avatar_url TEXT,
    bio TEXT,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de Clientes
CREATE TABLE IF NOT EXISTS clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(25) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_clients_phone ON clients (phone);
CREATE INDEX IF NOT EXISTS idx_clients_email ON clients (email);

-- Tabela de Servicos
CREATE TABLE IF NOT EXISTS services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    description TEXT,
    price_cents INTEGER NOT NULL,
    duration_minutes INTEGER NOT NULL,
    image_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de Agendamentos
CREATE TABLE IF NOT EXISTS appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    barber_id UUID NOT NULL REFERENCES barbers(id) ON DELETE RESTRICT,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    total_price_cents INTEGER NOT NULL DEFAULT 0,
    total_duration_minutes INTEGER NOT NULL DEFAULT 30,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    client_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_appointments_barber_date ON appointments (barber_id, scheduled_at);
-- Restricao de unicidade no banco: impede fisicamente que duas transacoes ativas reservem o mesmo barbeiro no mesmo horario exato
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_barber_active_slot ON appointments (barber_id, scheduled_at) WHERE (status <> 'CANCELLED');

-- Tabela N:N de Servicos selecionados no Agendamento (permite mais de um servico)
CREATE TABLE IF NOT EXISTS appointment_services (
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    price_cents_at_booking INTEGER NOT NULL,
    duration_minutes_at_booking INTEGER NOT NULL,
    PRIMARY KEY (appointment_id, service_id)
);

-- Tabela de Logs de Notificacoes
CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID REFERENCES clients(id) ON DELETE SET NULL,
    appointment_id UUID REFERENCES appointments(id) ON DELETE SET NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP')),
    recipient VARCHAR(255) NOT NULL,
    title VARCHAR(150),
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Dados Iniciais: Barbeiros
INSERT INTO barbers (name, nickname, phone, email, bio, is_admin, is_active) VALUES
('Carlos Silva', 'Mestre Carlos', '11988880001', 'contato@barberman.com.br', 'Fundador e Administrador Geral do Barber Man. Especialista em visagismo e cortes premium.', TRUE, TRUE),
('Lucas Santana', 'Lucas Navalha', '11988880002', 'lucas@barberman.com.br', 'Especialista em degrade navalhado e barba terapia.', FALSE, TRUE),
('Matheus Oliveira', 'Matheus Fade', '11988880003', 'matheus@barberman.com.br', 'Especialista em platinados, luzes e cortes modernos.', FALSE, TRUE)
ON CONFLICT (phone) DO NOTHING;

-- Dados Iniciais: Todos os 16 Servicos do Barber Man
INSERT INTO services (name, price_cents, duration_minutes) VALUES
('Botox/desondulacao', 5000, 60),
('Selagem', 7500, 60),
('Hidratacao', 2000, 15),
('Alisamento', 2000, 15),
('Pigmentacao', 2000, 30),
('Corte', 3000, 30),
('Corte kids', 3000, 30),
('Corte & Sobrancelha', 3500, 30),
('Platinado & Corte', 13000, 60),
('Luzes & Corte', 10000, 60),
('Corte & Barba', 5000, 60),
('Barba', 2500, 30),
('Corte, Barba & Sobrancelha', 5500, 60),
('Corte & barba simples', 4000, 30),
('Corte, sobrancelha & barba simples', 4500, 30),
('Pezinho & Barba', 3000, 30)
ON CONFLICT DO NOTHING;
