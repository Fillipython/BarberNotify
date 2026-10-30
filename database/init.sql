-- Script de Inicializacao da Base de Dados: Barber Man (Sincronizado com BarberAdmin)

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

-- Dados Iniciais: Barbeiros (UUIDs e Fotos sincronizados com BarberAdmin)
INSERT INTO barbers (id, name, nickname, phone, email, avatar_url, bio, is_admin, is_active) VALUES
(
    '8a350074-5524-453d-a625-2b25e7e8ca59',
    'Carlos Silva',
    'Mestre Carlos',
    '11988880001',
    'contato@barberman.com.br',
    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=250&auto=format&fit=crop&q=80',
    'Fundador e Administrador Geral do Barber Man. Especialista em visagismo e cortes premium.',
    TRUE,
    TRUE
),
(
    '70b1fee3-a338-4444-90f9-f4551afc15fe',
    'Lucas Santana',
    'Lucas Navalha',
    '11988880002',
    'lucas@barberman.com.br',
    'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=250&auto=format&fit=crop&q=80',
    'Especialista em degrade navalhado e barba terapia.',
    FALSE,
    TRUE
),
(
    '0670b734-45c1-4435-ad0e-b8d6b3099c7e',
    'Matheus Oliveira',
    'Matheus Fade',
    '11988880003',
    'matheus@barberman.com.br',
    'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=250&auto=format&fit=crop&q=80',
    'Especialista em platinados, luzes e cortes modernos.',
    FALSE,
    TRUE
)
ON CONFLICT (phone) DO UPDATE SET
    name = EXCLUDED.name,
    nickname = EXCLUDED.nickname,
    email = EXCLUDED.email,
    avatar_url = COALESCE(barbers.avatar_url, EXCLUDED.avatar_url),
    bio = EXCLUDED.bio;

-- Dados Iniciais: Todos os 16 Servicos do Barber Man (UUIDs sincronizados com BarberAdmin)
INSERT INTO services (id, name, price_cents, duration_minutes) VALUES
('16b14dc5-1df2-4041-a26b-4b292ca2db35', 'Botox/desondulacao', 5000, 60),
('a7cb2ec3-f68f-42ff-859a-58666d3b727c', 'Selagem', 7500, 60),
('b5eac08f-4c62-40be-b974-c00881aa99e0', 'Hidratacao', 2000, 15),
('650e3202-2f1f-435f-91b9-442e47912118', 'Alisamento', 2000, 15),
('187ebf61-e48d-4665-a71e-8076768b3915', 'Pigmentacao', 2000, 30),
('5379777b-3c95-42b4-9baa-06b621a325b8', 'Corte', 3000, 30),
('1332638b-4d7f-467f-ba2e-c0f6614ee0c6', 'Corte kids', 3000, 30),
('bd3ac8a7-5955-4a97-a275-c621f635e635', 'Corte & Sobrancelha', 3500, 30),
('b629d3af-6e6c-4c2f-930c-becd3c78bb58', 'Platinado & Corte', 13000, 60),
('497594fe-cdc2-4ae5-a928-523ffd3fd522', 'Luzes & Corte', 10000, 60),
('85f353f0-229b-4586-9850-dc2b65434a63', 'Corte & Barba', 5000, 60),
('e3ce8dc4-44f0-4d62-95d1-8651ea77d353', 'Barba', 2500, 30),
('80af0829-4a7f-4745-a945-c7c8fa2dd00d', 'Corte, Barba & Sobrancelha', 5500, 60),
('9095beb0-8eb7-447c-aa9f-e52e03e65c36', 'Corte & barba simples', 4000, 30),
('3dcf6649-b31e-4fad-b493-4d06c86b5ff1', 'Corte, sobrancelha & barba simples', 4500, 30),
('3d0ecd31-320a-4f35-bd57-071a3bb66ec8', 'Pezinho & Barba', 3000, 30)
ON CONFLICT (id) DO NOTHING;

-- Clientes Mock Interligados com BarberAdmin
INSERT INTO clients (id, name, phone, email, notifications_enabled) VALUES
('c1000000-0000-0000-0000-000000000001', 'Gabriel Santos', '11977771001', 'gabriel.santos@exemplo.com', TRUE),
('c1000000-0000-0000-0000-000000000002', 'Rodrigo Albuquerque', '11977771002', 'rodrigo.albuquerque@exemplo.com', TRUE),
('c1000000-0000-0000-0000-000000000003', 'Rafael Moreira', '11977771003', 'rafael.moreira@exemplo.com', TRUE),
('c1000000-0000-0000-0000-000000000004', 'Bruno Mendes', '11977771004', 'bruno.mendes@exemplo.com', FALSE)
ON CONFLICT (phone) DO NOTHING;
