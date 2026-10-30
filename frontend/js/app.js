const { createApp, ref, computed, onMounted } = Vue;

const isSubpath = window.location.pathname.startsWith('/BarberNotify');
const API_BASE = window.API_BASE_URL || (
  window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? (isSubpath ? '/BarberNotify/api' : 'http://localhost:8081/api')
    : (isSubpath ? '/BarberNotify/api' : '/api')
);

const ADMIN_API_BASE = window.ADMIN_API_BASE_URL || (
  window.location.hostname.includes('fillipython.ddns.net') || isSubpath
    ? `${window.location.protocol}//${window.location.host}/BarberAdmin/api/v1`
    : 'http://localhost:8080/api/v1'
);

function buildInitialsSvg(name) {
  const parts = String(name || 'BM').trim().split(/\s+/);
  const initials = parts.length > 1
    ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
    : parts[0].substring(0, 2).toUpperCase();
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120"><rect width="120" height="120" rx="60" fill="#18202f"/><circle cx="60" cy="60" r="56" fill="none" stroke="#d4af37" stroke-width="3"/><text x="50%" y="54%" dominant-baseline="middle" text-anchor="middle" fill="#fbbf24" font-family="Inter, sans-serif" font-weight="700" font-size="42">${initials}</text></svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

function resolveBarberAvatar(rawUrl, barberName, fallbackUrl) {
  if (rawUrl === 'REMOVED' || rawUrl === 'NONE') {
    return buildInitialsSvg(barberName);
  }
  if (!rawUrl) {
    return fallbackUrl || buildInitialsSvg(barberName);
  }
  if (rawUrl.startsWith('http://') || rawUrl.startsWith('https://') || rawUrl.startsWith('data:')) {
    return rawUrl;
  }
  const isProdHost = window.location.hostname.includes('fillipython.ddns.net') || isSubpath;
  const adminOrigin = isProdHost
    ? `${window.location.protocol}//${window.location.host}/BarberAdmin`
    : 'http://localhost:8080';
  if (rawUrl.startsWith('/BarberAdmin/')) {
    return `${window.location.protocol}//${window.location.host}${rawUrl}`;
  }
  if (rawUrl.startsWith('/api/')) {
    return `${adminOrigin}${rawUrl}`;
  }
  return `${adminOrigin}/api${rawUrl.startsWith('/') ? '' : '/'}${rawUrl}`;
}

const DEFAULT_BARBER_PHOTOS = {
  '8a350074-5524-453d-a625-2b25e7e8ca59': 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=250&auto=format&fit=crop&q=80',
  '70b1fee3-a338-4444-90f9-f4551afc15fe': 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=250&auto=format&fit=crop&q=80',
  '0670b734-45c1-4435-ad0e-b8d6b3099c7e': 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=250&auto=format&fit=crop&q=80'
};

const app = createApp({
  setup() {
    // Estado do Cliente
    const client = ref({
      id: null,
      name: '',
      phone: '',
      email: '',
      notificationsEnabled: false
    });

    const isIdentified = ref(false);
    const step = ref('ask_name'); // 'ask_name' | 'ask_notifications' | 'ask_phone' | 'ask_email' | 'flow'
    const currentStage = ref(1); // 1: Barbeiro, 2: Servicos, 3: Data/Hora, 4: Confirmacao

    // Inputs
    const inputName = ref('');
    const inputPhone = ref('');
    const inputEmail = ref('');
    const nameError = ref('');
    const phoneError = ref('');
    const emailError = ref('');

    // Notificacao Toast e Modal
    const toastMessage = ref('');
    const showAppointmentsModal = ref(false);
    const allAppointments = ref([]);
    const lastConfirmedAppointment = ref({});

    // Agendamentos filtrados exclusivamente para o cliente ativo
    const myAppointments = computed(() => {
      if (!client.value.phone && !client.value.name) return [];
      const currentPhoneDigits = (client.value.phone || '').replace(/\D/g, '');
      return allAppointments.value.filter(apt => {
        if (client.value.id && apt.clientId === client.value.id) return true;
        if (currentPhoneDigits && (apt.clientPhone || '').replace(/\D/g, '') === currentPhoneDigits) return true;
        if (client.value.name && apt.clientName && apt.clientName.toLowerCase() === client.value.name.toLowerCase()) return true;
        return false;
      });
    });

    // Barbeiros Cadastrados (Barber Man) sincronizados com o BarberAdmin
    const barbers = ref([
      {
        id: '8a350074-5524-453d-a625-2b25e7e8ca59',
        name: 'Carlos Silva',
        nickname: 'Mestre Carlos',
        email: 'contato@barberman.com.br',
        phone: '11988880001',
        role: 'Fundador & Barbeiro Master',
        bio: 'Fundador e Administrador Geral do Barber Man. Especialista em visagismo e cortes premium.',
        isAdmin: true,
        avatar: DEFAULT_BARBER_PHOTOS['8a350074-5524-453d-a625-2b25e7e8ca59']
      },
      {
        id: '70b1fee3-a338-4444-90f9-f4551afc15fe',
        name: 'Lucas Santana',
        nickname: 'Lucas Navalha',
        email: 'lucas@barberman.com.br',
        phone: '11988880002',
        role: 'Barbeiro Especialista',
        bio: 'Especialista em degrade navalhado e barba terapia.',
        isAdmin: false,
        avatar: DEFAULT_BARBER_PHOTOS['70b1fee3-a338-4444-90f9-f4551afc15fe']
      },
      {
        id: '0670b734-45c1-4435-ad0e-b8d6b3099c7e',
        name: 'Matheus Oliveira',
        nickname: 'Matheus Fade',
        email: 'matheus@barberman.com.br',
        phone: '11988880003',
        role: 'Barbeiro & Colorista',
        bio: 'Especialista em platinados, luzes e cortes modernos.',
        isAdmin: false,
        avatar: DEFAULT_BARBER_PHOTOS['0670b734-45c1-4435-ad0e-b8d6b3099c7e']
      }
    ]);

    const selectedBarber = ref(barbers.value[0]);

    // Lista Completa dos 16 Servicos Sincronizados com BarberAdmin
    const services = ref([
      {
        id: '16b14dc5-1df2-4041-a26b-4b292ca2db35',
        name: 'Botox/desondulacao',
        priceCents: 5000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'assets/services/botox_desondulacao.jpg'
      },
      {
        id: 'a7cb2ec3-f68f-42ff-859a-58666d3b727c',
        name: 'Selagem',
        priceCents: 7500,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'assets/services/selagem.jpg'
      },
      {
        id: 'b5eac08f-4c62-40be-b974-c00881aa99e0',
        name: 'Hidratacao',
        priceCents: 2000,
        durationMinutes: 15,
        durationLabel: '15min',
        image: 'assets/services/hidratacao.jpg'
      },
      {
        id: '650e3202-2f1f-435f-91b9-442e47912118',
        name: 'Alisamento',
        priceCents: 2000,
        durationMinutes: 15,
        durationLabel: '15min',
        image: 'assets/services/alisamento.jpg'
      },
      {
        id: '187ebf61-e48d-4665-a71e-8076768b3915',
        name: 'Pigmentacao',
        priceCents: 2000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'assets/services/pigmentacao.jpg'
      },
      {
        id: '5379777b-3c95-42b4-9baa-06b621a325b8',
        name: 'Corte',
        priceCents: 3000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'assets/services/corte.jpg'
      },
      {
        id: '1332638b-4d7f-467f-ba2e-c0f6614ee0c6',
        name: 'Corte kids',
        priceCents: 3000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'assets/services/corte_kids.jpg'
      },
      {
        id: 'bd3ac8a7-5955-4a97-a275-c621f635e635',
        name: 'Corte & Sobrancelha',
        priceCents: 3500,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'assets/services/corte_sobrancelha.jpg'
      },
      {
        id: 'b629d3af-6e6c-4c2f-930c-becd3c78bb58',
        name: 'Platinado & Corte',
        priceCents: 13000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'assets/services/platinado_corte.jpg'
      },
      {
        id: '497594fe-cdc2-4ae5-a928-523ffd3fd522',
        name: 'Luzes & Corte',
        priceCents: 10000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'assets/services/luzes_corte.jpg'
      },
      {
        id: '85f353f0-229b-4586-9850-dc2b65434a63',
        name: 'Corte & Barba',
        priceCents: 5000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'assets/services/corte_barba.jpg'
      },
      {
        id: 'e3ce8dc4-44f0-4d62-95d1-8651ea77d353',
        name: 'Barba',
        priceCents: 2500,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'assets/services/barba.jpg'
      },
      {
        id: '80af0829-4a7f-4745-a945-c7c8fa2dd00d',
        name: 'Corte, Barba & Sobrancelha',
        priceCents: 5500,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'assets/services/corte_barba_sobrancelha.jpg'
      },
      {
        id: '9095beb0-8eb7-447c-aa9f-e52e03e65c36',
        name: 'Corte & barba simples',
        priceCents: 4000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'assets/services/corte_barba_simples.jpg'
      },
      {
        id: '3dcf6649-b31e-4fad-b493-4d06c86b5ff1',
        name: 'Corte, sobrancelha & barba simples',
        priceCents: 4500,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'assets/services/corte_barba_simples.jpg'
      },
      {
        id: '3d0ecd31-320a-4f35-bd57-071a3bb66ec8',
        name: 'Pezinho & Barba',
        priceCents: 3000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'assets/services/barba.jpg'
      }
    ]);

    // Selecao Multipla de Servicos
    const selectedServices = ref([]);

    const isServiceSelected = (serviceId) => {
      return selectedServices.value.some(s => s.id === serviceId);
    };

    const toggleService = (service) => {
      const index = selectedServices.value.findIndex(s => s.id === service.id);
      if (index > -1) {
        selectedServices.value.splice(index, 1);
      } else {
        selectedServices.value.push(service);
      }
      if (selectedDay.value) {
        fetchSlots();
      }
    };

    const totalPriceCents = computed(() => {
      return selectedServices.value.reduce((acc, s) => acc + s.priceCents, 0);
    });

    const totalDurationMinutes = computed(() => {
      return selectedServices.value.reduce((acc, s) => acc + s.durationMinutes, 0);
    });

    const totalDurationLabel = computed(() => {
      const minutes = totalDurationMinutes.value;
      if (minutes <= 0) return '0min';
      const h = Math.floor(minutes / 60);
      const m = minutes % 60;
      if (h > 0 && m > 0) return `${h}hr ${m}min`;
      if (h > 0) return `${h}hr`;
      return `${m}min`;
    });

    // Datas e Horarios Disponiveis
    const selectedDay = ref('');
    const selectedTime = ref('');
    const apiSlots = ref([]);

    // BroadcastChannel para sincronizacao imediata entre abas do navegador (BarberNotify <-> BarberAdmin)
    const syncChannel = typeof window !== 'undefined' && window.BroadcastChannel ? new BroadcastChannel('barber_notify_sync') : null;
    if (syncChannel) {
      syncChannel.onmessage = (event) => {
        if (event.data === 'appointment_changed') {
          fetchSlots();
        } else if (event.data === 'staff_updated') {
          syncBarbersFromApis();
        }
      };
    }

    const syncBarbersFromApis = async () => {
      let notifyBarbers = null;
      let adminStaff = null;

      try {
        const res = await fetch(`${API_BASE}/barbers`);
        if (res.ok) notifyBarbers = await res.json();
      } catch (e) {}

      try {
        const resAdmin = await fetch(`${ADMIN_API_BASE}/staff`);
        if (resAdmin.ok) adminStaff = await resAdmin.json();
      } catch (e) {}

      // Se o BarberAdmin retornou a equipe atualizada (com as fotos alteradas ou removidas pelo barbeiro), usa como fonte principal
      const sourceList = (Array.isArray(adminStaff) && adminStaff.length > 0)
        ? adminStaff
        : (Array.isArray(notifyBarbers) && notifyBarbers.length > 0 ? notifyBarbers : null);

      if (sourceList) {
        const currentSelectedId = selectedBarber.value?.id;
        barbers.value = sourceList.map((b) => {
          const isMasterRole = b.role === 'MASTER' || b.isAdmin === true;
          const fallbackPhoto = DEFAULT_BARBER_PHOTOS[b.id] || 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=250&auto=format&fit=crop&q=80';
          // Verifica também se há override no notifyBarbers ou no adminStaff
          const adminMatch = Array.isArray(adminStaff) ? adminStaff.find(a => a.id === b.id || a.email === b.email) : null;
          const rawAvatar = (adminMatch && adminMatch.avatarUrl !== undefined && adminMatch.avatarUrl !== null)
            ? adminMatch.avatarUrl
            : b.avatarUrl;

          return {
            id: b.id,
            name: b.name,
            nickname: b.nickname || '',
            email: b.email || '',
            phone: b.phone || '',
            role: isMasterRole ? 'Fundador & Barbeiro Master' : 'Barbeiro Especialista',
            bio: b.bio || '',
            isAdmin: isMasterRole,
            avatar: resolveBarberAvatar(rawAvatar, b.name, fallbackPhoto)
          };
        });

        const found = barbers.value.find(b => b.id === currentSelectedId);
        selectedBarber.value = found || barbers.value[0];
      }
    };

    const sendWebhookToBarberAdmin = async (payload) => {
      try {
        await fetch(`${ADMIN_API_BASE}/barbernotify/webhook`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-BarberNotify-Secret': 'barbernotify_webhook_secret_2026'
          },
          body: JSON.stringify(payload)
        });
      } catch (e) {
        console.warn('Webhook direto para BarberAdmin indisponivel:', e);
      }
    };

    const fetchSlots = async () => {
      if (!selectedBarber.value?.id || !selectedDay.value) return;
      try {
        const duration = totalDurationMinutes.value > 0 ? totalDurationMinutes.value : 30;
        const res = await fetch(`${API_BASE}/appointments/slots?barberId=${selectedBarber.value.id}&date=${selectedDay.value}&duration=${duration}`);
        if (res.ok) {
          const data = await res.json();
          if (Array.isArray(data) && data.length > 0) {
            apiSlots.value = data;

            // Se o horario que o usuario tinha selecionado foi reservado por outro cliente em tempo real
            if (selectedTime.value) {
              const currentSlot = data.find(s => s.time === selectedTime.value);
              if (currentSlot && !currentSlot.available) {
                const lostTime = selectedTime.value;
                selectedTime.value = '';
                showToast(`Atenção: O horário ${lostTime} acabou de ser reservado por outro cliente!`);
              }
            }
          }
        }
      } catch (e) {
        console.warn('API slots offline, usando fallback local', e);
      }
    };

    // Polling em tempo real (atualiza a cada 2.5s se estiver na etapa de horarios)
    let slotsPollingInterval = null;
    const startSlotsPolling = () => {
      stopSlotsPolling();
      fetchSlots();
      slotsPollingInterval = setInterval(() => {
        if (currentStage.value === 3) {
          fetchSlots();
        }
      }, 2500);
    };

    const stopSlotsPolling = () => {
      if (slotsPollingInterval) {
        clearInterval(slotsPollingInterval);
        slotsPollingInterval = null;
      }
    };

    const availableDays = computed(() => {
      const days = [];
      const weekdays = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB'];
      const months = ['JAN', 'FEV', 'MAR', 'ABR', 'MAI', 'JUN', 'JUL', 'AGO', 'SET', 'OUT', 'NOV', 'DEZ'];

      const now = new Date();
      for (let i = 0; i < 10; i++) {
        const d = new Date(now);
        d.setDate(now.getDate() + i);
        const dayOfWeek = d.getDay();
        const isWorkday = dayOfWeek !== 0; // Domingo fechado
        const isoDate = d.toISOString().split('T')[0];

        days.push({
          isoDate,
          weekday: weekdays[dayOfWeek],
          dayNumber: d.getDate(),
          dayNumberFormatted: String(d.getDate()).padStart(2, '0'),
          monthUpper: months[d.getMonth()],
          isToday: i === 0,
          isWorkday
        });
      }
      return days;
    });

    const availableTimeSlots = computed(() => {
      if (apiSlots.value && apiSlots.value.length > 0) {
        return apiSlots.value;
      }

      const slots = [
        '09:00', '09:30', '10:00',
        '10:30', '11:00', '11:30',
        '12:00', '12:30', '13:00',
        '13:30', '15:30', '16:00',
        '16:30', '17:00', '17:30',
        '18:00'
      ];

      return slots.map((time) => ({
        time,
        available: true
      }));
    });

    const selectedDateFormattedFull = computed(() => {
      if (!selectedDay.value) return 'Selecione um dia e horario acima';
      const [year, month, day] = selectedDay.value.split('-');
      const d = new Date(parseInt(year), parseInt(month) - 1, parseInt(day));
      const weekdaysLong = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sab'];
      const monthsLong = [
        'janeiro', 'fevereiro', 'marco', 'abril', 'maio', 'junho',
        'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'
      ];
      const weekdayStr = weekdaysLong[d.getDay()];
      const monthStr = monthsLong[d.getMonth()];
      const timePart = selectedTime.value ? ` ${selectedTime.value}` : '';
      return `${weekdayStr}, ${parseInt(day)} de ${monthStr} de ${year} --${timePart}`;
    });

    const selectDay = (isoDate) => {
      selectedDay.value = isoDate;
      selectedTime.value = ''; // reseta horario ao mudar o dia
      fetchSlots();
    };

    const selectTime = (time) => {
      selectedTime.value = time;
    };

    const formatDisplayDate = (isoDate) => {
      if (!isoDate) return '';
      const [year, month, day] = isoDate.split('-');
      return `${day}/${month}/${year}`;
    };

    // Mascara de Telefone Dinamica: (xx) x xxxx-xxxx
    const formatPhone = (val) => {
      let digits = val.replace(/\D/g, '').substring(0, 11);
      if (digits.length <= 2) {
        return digits ? `(${digits}` : '';
      }
      if (digits.length <= 3) {
        return `(${digits.substring(0, 2)}) ${digits.substring(2)}`;
      }
      if (digits.length <= 7) {
        return `(${digits.substring(0, 2)}) ${digits.substring(2, 3)} ${digits.substring(3)}`;
      }
      return `(${digits.substring(0, 2)}) ${digits.substring(2, 3)} ${digits.substring(3, 7)}-${digits.substring(7, 11)}`;
    };

    const onPhoneInput = (e) => {
      inputPhone.value = formatPhone(e.target.value);
    };

    // Acoes do Chat de Cadastro
    const handleNameSubmit = () => {
      nameError.value = '';
      const trimmed = inputName.value.trim();
      if (!trimmed || trimmed.length < 3) {
        nameError.value = 'Por favor, informe seu nome completo (minimo 3 letras).';
        return;
      }
      client.value.name = trimmed;
      step.value = 'ask_notifications';
    };

    const handleNotificationsSubmit = (enabled) => {
      client.value.notificationsEnabled = enabled;
      step.value = 'ask_phone';
      if (enabled) {
        showToast('Notificacoes ativadas com sucesso!');
      }
    };

    const handlePhoneSubmit = async () => {
      phoneError.value = '';
      const raw = inputPhone.value.replace(/\D/g, '');
      if (raw.length < 10) {
        phoneError.value = 'Por favor, digite um telefone celular valido com DDD.';
        return;
      }

      client.value.phone = inputPhone.value.trim();

      // Procura se o cliente ja possui cadastro no backend PostgreSQL
      try {
        const checkRes = await fetch(`${API_BASE}/clients/search?phone=${raw}`);
        if (checkRes.ok) {
          const clientData = await checkRes.json();
          if (clientData && clientData.id) {
            client.value.id = clientData.id;
            client.value.name = clientData.name || client.value.name;
            client.value.email = clientData.email || '';
            saveClientLocally();
            isIdentified.value = true;
            step.value = 'flow';
            currentStage.value = 1;
            showToast(`Bem-vindo de volta, ${client.value.name}!`);
            return;
          }
        }
      } catch (e) {
        console.warn('Erro ao consultar cliente na API:', e);
      }

      step.value = 'ask_email';
    };

    const handleEmailSubmit = async () => {
      emailError.value = '';
      const trimmed = inputEmail.value.trim();
      if (!trimmed || !trimmed.includes('@') || !trimmed.includes('.')) {
        emailError.value = 'Por favor, informe um e-mail valido.';
        return;
      }

      client.value.email = trimmed;

      // Cadastra no backend PostgreSQL e obtem o UUID real
      try {
        const clientRes = await fetch(`${API_BASE}/clients`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: client.value.name,
            phone: client.value.phone,
            email: trimmed,
            notificationsEnabled: client.value.notificationsEnabled
          })
        });
        if (clientRes.ok) {
          const clientData = await clientRes.json();
          client.value.id = clientData.id;
        }
      } catch (e) {
        console.warn('Erro ao registrar cliente na API:', e);
      }

      saveClientLocally();
      isIdentified.value = true;
      step.value = 'flow';
      currentStage.value = 1;
      showToast('Cadastro concluido com sucesso!');
    };

    const selectBarber = (barber) => {
      selectedBarber.value = barber;
      if (selectedDay.value) {
        fetchSlots();
      }
      if (currentStage.value === 1) {
        currentStage.value = 2;
      }
    };

    const saveClientLocally = () => {
      localStorage.setItem('barber_man_client', JSON.stringify(client.value));

      const known = JSON.parse(localStorage.getItem('barber_man_known_users') || '[]');
      const existsIndex = known.findIndex(k => k.phone === client.value.phone);
      if (existsIndex > -1) {
        known[existsIndex] = client.value;
      } else {
        known.push(client.value);
      }
      localStorage.setItem('barber_man_known_users', JSON.stringify(known));
    };

    const resetClient = () => {
      localStorage.removeItem('barber_man_client');
      client.value = { id: null, name: '', phone: '', email: '', notificationsEnabled: false };
      isIdentified.value = false;
      step.value = 'ask_name';
      currentStage.value = 1;
      selectedServices.value = [];
      selectedDay.value = '';
      selectedTime.value = '';
      inputName.value = '';
      inputPhone.value = '';
      inputEmail.value = '';
      nameError.value = '';
      phoneError.value = '';
      emailError.value = '';
      showAppointmentsModal.value = false;
      showToast('Sessao encerrada. Digite seu nome para iniciar com nova conta.');
    };

    // Controle do Botao Principal Fixo Inferior
    const isNextDisabled = computed(() => {
      if (currentStage.value === 1) {
        return !selectedBarber.value;
      }
      if (currentStage.value === 2) {
        return selectedServices.value.length === 0;
      }
      if (currentStage.value === 3) {
        return !selectedDay.value || !selectedTime.value;
      }
      return false;
    });

    const mainActionLabel = computed(() => {
      if (currentStage.value === 1) return 'Confirmar Profissional';
      if (currentStage.value === 2) return `Avancar (${selectedServices.value.length} selecionado${selectedServices.value.length > 1 ? 's' : ''})`;
      if (currentStage.value === 3) return 'Revisar Agendamento';
      if (currentStage.value === 4) return 'Confirmar Agendamento';
      return 'Continuar';
    });

    const handleMainAction = () => {
      if (currentStage.value < 4) {
        currentStage.value++;
        if (currentStage.value === 3) {
          if (!selectedDay.value && availableDays.value.length > 0) {
            const firstWorkday = availableDays.value.find(d => d.isWorkday);
            selectedDay.value = firstWorkday ? firstWorkday.isoDate : availableDays.value[0].isoDate;
          }
          startSlotsPolling();
        } else {
          stopSlotsPolling();
        }
      } else if (currentStage.value === 4) {
        finalizeAppointment();
      }
    };

    const finalizeAppointment = async () => {
      if (!selectedTime.value || !selectedDay.value) {
        showToast('Por favor, selecione uma data e horário.');
        currentStage.value = 3;
        startSlotsPolling();
        return;
      }

      // Verifica se o slot selecionado ainda esta livre
      const chosenSlot = apiSlots.value.find(s => s.time === selectedTime.value);
      if (chosenSlot && !chosenSlot.available) {
        showToast('Este horário não está mais disponível. Por favor, selecione outro.');
        currentStage.value = 3;
        selectedTime.value = '';
        startSlotsPolling();
        return;
      }

      const scheduledIso = `${selectedDay.value}T${selectedTime.value}:00-03:00`;
      let clientDbId = client.value.id;

      // 1. Garante que o cliente esta cadastrado no backend para obter UUID real
      try {
        const clientRes = await fetch(`${API_BASE}/clients`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: client.value.name,
            phone: client.value.phone,
            email: client.value.email || null,
            notificationsEnabled: client.value.notificationsEnabled
          })
        });
        if (clientRes.ok) {
          const clientData = await clientRes.json();
          clientDbId = clientData.id;
          client.value.id = clientDbId;
          saveClientLocally();
        } else {
          showToast('Erro ao validar cadastro no servidor. Tente novamente.');
          return;
        }
      } catch (e) {
        showToast('Não foi possível conectar ao servidor. Verifique a conexão.');
        return;
      }

      // 2. Envio da reserva para a API (com controle atomico e lock pessimista no PostgreSQL)
      try {
        const aptRes = await fetch(`${API_BASE}/appointments`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            clientId: clientDbId,
            barberId: selectedBarber.value.id,
            serviceIds: selectedServices.value.map(s => s.id),
            scheduledAt: scheduledIso,
            clientNotes: 'Agendamento Barber Man Web'
          })
        });

        // 3. Garantia de atomicidade: se outro cliente confirmou primeiro, o servidor rejeita com 409
        if (aptRes.status === 409) {
          const errData = await aptRes.json().catch(() => ({}));
          showToast(errData.message || 'Ops! Este horário acabou de ser reservado por outro cliente. Por favor, escolha outro horário.');
          currentStage.value = 3; // Mantem na etapa de data/hora
          selectedTime.value = '';
          startSlotsPolling();
          return;
        }

        if (!aptRes.ok) {
          const errData = await aptRes.json().catch(() => ({}));
          showToast(errData.message || 'Ocorreu um erro ao confirmar o agendamento no servidor.');
          currentStage.value = 3;
          startSlotsPolling();
          return;
        }

        const aptData = await aptRes.json();

        // 4. Sincroniza também diretamente com o BarberAdmin via Webhook
        await sendWebhookToBarberAdmin({
          eventType: 'APPOINTMENT_CREATED',
          externalId: aptData.id,
          clientName: client.value.name,
          clientPhone: client.value.phone,
          clientEmail: client.value.email || '',
          barberId: selectedBarber.value.id,
          barberName: selectedBarber.value.name,
          serviceName: selectedServices.value.map(s => s.name).join(', '),
          scheduledAt: scheduledIso,
          durationMinutes: totalDurationMinutes.value || 30,
          customPriceCents: totalPriceCents.value,
          origin: 'PORTAL'
        });

        // 5. Agendamento validado e salvo com sucesso no banco de dados!
        const newAppointment = {
          id: aptData.id,
          clientId: clientDbId,
          clientName: client.value.name,
          clientPhone: client.value.phone,
          clientEmail: client.value.email,
          barberId: selectedBarber.value.id,
          barberName: selectedBarber.value.name,
          servicesNames: selectedServices.value.map(s => s.name),
          totalPriceCents: totalPriceCents.value,
          totalDurationLabel: totalDurationLabel.value,
          date: formatDisplayDate(selectedDay.value),
          time: selectedTime.value,
          scheduledAt: scheduledIso,
          status: 'CONFIRMADO',
          createdAt: new Date().toISOString()
        };

        allAppointments.value.unshift(newAppointment);
        localStorage.setItem('barber_man_appointments', JSON.stringify(allAppointments.value));
        lastConfirmedAppointment.value = newAppointment;

        // Notifica outras abas locais (incluindo BarberAdmin) para atualizarem a agenda
        syncChannel?.postMessage('appointment_changed');

        stopSlotsPolling();
        showToast(`Agendamento confirmado com sucesso!`);
        currentStage.value = 5;
      } catch (err) {
        showToast('Falha na comunicação com o servidor ao confirmar reserva.');
      }
    };

    const startNewBooking = () => {
      currentStage.value = 1;
      selectedServices.value = [];
      selectedTime.value = '';
    };

    const cancelAppointment = async (appointmentId) => {
      const targetApt = allAppointments.value.find(a => a.id === appointmentId);
      try {
        await fetch(`${API_BASE}/appointments/${appointmentId}/cancel`, {
          method: 'PATCH'
        });
      } catch (e) {
        console.warn('Erro ao cancelar agendamento na API:', e);
      }

      if (targetApt) {
        await sendWebhookToBarberAdmin({
          eventType: 'APPOINTMENT_CANCELLED',
          externalId: appointmentId,
          clientName: targetApt.clientName || client.value.name || 'Cliente',
          clientPhone: targetApt.clientPhone || client.value.phone || '11999999999',
          barberId: targetApt.barberId || selectedBarber.value?.id,
          barberName: targetApt.barberName || selectedBarber.value?.name,
          serviceName: (targetApt.servicesNames || ['Corte']).join(', '),
          scheduledAt: targetApt.scheduledAt || new Date().toISOString(),
          origin: 'PORTAL'
        });
      }

      const list = allAppointments.value.map(a => {
        if (a.id === appointmentId) {
          return { ...a, status: 'CANCELADO' };
        }
        return a;
      });
      allAppointments.value = list;
      localStorage.setItem('barber_man_appointments', JSON.stringify(list));
      syncChannel?.postMessage('appointment_changed');
      fetchSlots();
      showToast('Agendamento cancelado com sucesso.');
    };

    const showToast = (msg) => {
      toastMessage.value = msg;
      setTimeout(() => {
        toastMessage.value = '';
      }, 3500);
    };

    // Inicializacao
    onMounted(() => {
      const todayIso = new Date().toISOString().split('T')[0];
      const todayDisplay = formatDisplayDate(todayIso);

      // Mock interligado inicial entre BarberNotify e BarberAdmin
      const defaultMockAppointments = [
        {
          id: 'a1000000-0000-0000-0000-000000000001',
          clientId: 'c1000000-0000-0000-0000-000000000001',
          clientName: 'Gabriel Santos',
          clientPhone: '11977771001',
          clientEmail: 'gabriel.santos@exemplo.com',
          barberId: '8a350074-5524-453d-a625-2b25e7e8ca59',
          barberName: 'Carlos Silva',
          servicesNames: ['Platinado & Corte'],
          totalPriceCents: 13000,
          totalDurationLabel: '1hr',
          date: todayDisplay,
          time: '11:00',
          scheduledAt: `${todayIso}T11:00:00-03:00`,
          status: 'CONFIRMADO',
          createdAt: new Date().toISOString()
        },
        {
          id: 'a1000000-0000-0000-0000-000000000002',
          clientId: 'c1000000-0000-0000-0000-000000000002',
          clientName: 'Rodrigo Albuquerque',
          clientPhone: '11977771002',
          clientEmail: 'rodrigo.albuquerque@exemplo.com',
          barberId: '70b1fee3-a338-4444-90f9-f4551afc15fe',
          barberName: 'Lucas Santana',
          servicesNames: ['Corte & Barba'],
          totalPriceCents: 5000,
          totalDurationLabel: '1hr',
          date: todayDisplay,
          time: '14:00',
          scheduledAt: `${todayIso}T14:00:00-03:00`,
          status: 'CONFIRMADO',
          createdAt: new Date().toISOString()
        },
        {
          id: 'a1000000-0000-0000-0000-000000000003',
          clientId: 'c1000000-0000-0000-0000-000000000003',
          clientName: 'Rafael Moreira',
          clientPhone: '11977771003',
          clientEmail: 'rafael.moreira@exemplo.com',
          barberId: '0670b734-45c1-4435-ad0e-b8d6b3099c7e',
          barberName: 'Matheus Oliveira',
          servicesNames: ['Luzes & Corte'],
          totalPriceCents: 10000,
          totalDurationLabel: '1hr',
          date: todayDisplay,
          time: '16:00',
          scheduledAt: `${todayIso}T16:00:00-03:00`,
          status: 'CONFIRMADO',
          createdAt: new Date().toISOString()
        }
      ];

      // Carrega agendamentos salvos ou popula o mock interligado
      const stored = localStorage.getItem('barber_man_appointments');
      if (stored) {
        try {
          const parsed = JSON.parse(stored);
          allAppointments.value = Array.isArray(parsed) && parsed.length > 0 ? parsed : defaultMockAppointments;
        } catch (e) {
          allAppointments.value = defaultMockAppointments;
        }
      } else {
        allAppointments.value = defaultMockAppointments;
        localStorage.setItem('barber_man_appointments', JSON.stringify(defaultMockAppointments));
      }

      // Verifica se cliente ja esta salvo no navegador (2a iteracao em diante)
      const savedClient = localStorage.getItem('barber_man_client');
      if (savedClient) {
        try {
          const parsed = JSON.parse(savedClient);
          if (parsed && (parsed.name || parsed.phone)) {
            client.value = parsed;
            isIdentified.value = true;
            step.value = 'flow';
            currentStage.value = 1;
          }
        } catch (e) {
          console.error(e);
        }
      }

      // Sincroniza barbeiros e fotos com BarberNotify e BarberAdmin
      syncBarbersFromApis();

      // Sincroniza UUIDs dos servicos com o backend preservando a lista e fotos
      fetch(`${API_BASE}/services`)
        .then(r => r.json())
        .then(data => {
          if (Array.isArray(data) && data.length > 0) {
            services.value.forEach(localSrv => {
              const matched = data.find(apiSrv =>
                apiSrv.name.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, "") ===
                localSrv.name.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, "")
              );
              if (matched) {
                localSrv.id = matched.id;
              }
            });
          }
        })
        .catch(e => console.warn('Usando servicos locais:', e));
    });

    return {
      client,
      isIdentified,
      step,
      currentStage,
      inputPhone,
      inputName,
      inputEmail,
      nameError,
      phoneError,
      emailError,
      toastMessage,
      showAppointmentsModal,
      myAppointments,
      barbers,
      selectedBarber,
      services,
      selectedServices,
      totalPriceCents,
      totalDurationMinutes,
      totalDurationLabel,
      selectedDay,
      selectedTime,
      availableDays,
      availableTimeSlots,
      selectedDateFormattedFull,
      isServiceSelected,
      toggleService,
      selectBarber,
      selectDay,
      selectTime,
      formatDisplayDate,
      onPhoneInput,
      handleNameSubmit,
      handleNotificationsSubmit,
      handlePhoneSubmit,
      handleEmailSubmit,
      resetClient,
      isNextDisabled,
      mainActionLabel,
      handleMainAction,
      lastConfirmedAppointment,
      startNewBooking,
      cancelAppointment
    };
  }
});

app.mount('#app');
