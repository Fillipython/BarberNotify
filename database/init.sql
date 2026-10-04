-- Script de Inicializacao da Base de Dados: Caios Barber

-- Habilita extensão pgcrypto para geração de UUIDs se necessário
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

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

-- Índices para buscas rápidas ao digitar telefone ou email
CREATE INDEX IF NOT EXISTS idx_clients_phone ON clients (phone);
CREATE INDEX IF NOT EXISTS idx_clients_email ON clients (email);

-- Tabela de Logs de Notificações (Emails / SMS / WhatsApp de confirmação de cadastro ou lembretes)
CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID REFERENCES clients(id) ON DELETE SET NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP')),
    recipient VARCHAR(255) NOT NULL,
    title VARCHAR(150),
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de Serviços oferecidos pelo Caios Barber
CREATE TABLE IF NOT EXISTS services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price_cents INTEGER NOT NULL, -- valor em centavos (ex: 4500 = R$ 45,00)
    duration_minutes INTEGER NOT NULL DEFAULT 30,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de Agendamentos
CREATE TABLE IF NOT EXISTS appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    client_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Dados Iniciais de Demonstracao

-- Inserir Serviços padrão da barbearia
INSERT INTO services (name, description, price_cents, duration_minutes) VALUES
('Corte Degrade / Moderno', 'Corte completo com finalização e pomada modeladora', 4500, 40),
('Barba Terapia', 'Desenho de barba com toalha quente e óleos essenciais', 3500, 30),
('Combo Cabelo + Barba', 'Corte completo + Barboterapia completa', 7000, 60),
('Pézinho e Sobrancelha', 'Acabamento navalhado e alinhamento', 2000, 20)
ON CONFLICT DO NOTHING;

-- Inserir um cliente de teste
INSERT INTO clients (name, phone, email, notifications_enabled) VALUES
('Cliente Demonstração', '11999998888', 'cliente.teste@exemplo.com', TRUE)
ON CONFLICT (phone) DO NOTHING;

-- Registrar o log da notificação de boas-vindas do cliente teste
INSERT INTO notification_logs (client_id, channel, recipient, title, message, status)
SELECT 
    id, 
    'WHATSAPP', 
    '11999998888', 
    'Bem-vindo ao Caios Barber!', 
    'Olá Cliente Demonstração, seu cadastro no Caios Barber foi realizado com sucesso. Suas notificações estão ativadas!',
    'SENT'
FROM clients WHERE phone = '11999998888'
LIMIT 1;

INSERT INTO notification_logs (client_id, channel, recipient, title, message, status)
SELECT 
    id, 
    'EMAIL', 
    'cliente.teste@exemplo.com', 
    'Bem-vindo ao Caios Barber!', 
    'Olá Cliente Demonstração, seu cadastro no Caios Barber foi realizado com sucesso. Suas notificações estão ativadas!',
    'SENT'
FROM clients WHERE phone = '11999998888'
LIMIT 1;
