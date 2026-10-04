const { createApp, ref, computed, onMounted } = Vue;

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

    // Base de Clientes Cadastrados (Mock local que simula a base de dados)
    const mockKnownClients = [
      {
        id: 'c1-demo',
        name: 'Cliente Exemplo',
        phone: '11999990000',
        email: 'cliente.exemplo@exemplo.com',
        notificationsEnabled: true
      },
      {
        id: 'c2-caio',
        name: 'Caio Teste',
        phone: '11988887777',
        email: 'caio.teste@exemplo.com',
        notificationsEnabled: false
      }
    ];

    // Barbeiros Cadastrados (Caio como Administrador Geral)
    const barbers = ref([
      {
        id: 'barber-caio',
        name: 'Caio Silva',
        role: 'Fundador & Barbeiro Master',
        bio: 'Administrador geral. 10 anos de experiencia em visagismo e cortes de alta precisao.',
        isAdmin: true,
        avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80'
      },
      {
        id: 'barber-lucas',
        name: 'Lucas Santana',
        role: 'Barbeiro Especialista',
        bio: 'Referencia em degrade navalhado, barboterapia e acabamentos detalhados.',
        isAdmin: false,
        avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80'
      },
      {
        id: 'barber-matheus',
        name: 'Matheus Oliveira',
        role: 'Barbeiro & Colorista',
        bio: 'Especialista em platinados, luzes, pigmentacao e cortes modernos.',
        isAdmin: false,
        avatar: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150&auto=format&fit=crop&q=80'
      }
    ]);

    const selectedBarber = ref(barbers.value[0]);

    // Lista Completa dos 16 Servicos do Caios Barber
    const services = ref([
      {
        id: 'srv-botox',
        name: 'Botox/desondulacao',
        priceCents: 5000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'https://images.unsplash.com/photo-1622286342621-4bd786c2447c?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-selagem',
        name: 'Selagem',
        priceCents: 7500,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'https://images.unsplash.com/photo-1517832606589-7629c3397143?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-hidratacao',
        name: 'Hidratacao',
        priceCents: 2000,
        durationMinutes: 15,
        durationLabel: '15min',
        image: 'https://images.unsplash.com/photo-1585747860715-2ba37e788b70?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-alisamento',
        name: 'Alisamento',
        priceCents: 2000,
        durationMinutes: 15,
        durationLabel: '15min',
        image: 'https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-pigmentacao',
        name: 'Pigmentacao',
        priceCents: 2000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-corte',
        name: 'Corte',
        priceCents: 3000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'https://images.unsplash.com/photo-1605497788044-5a32c7078486?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-corte-kids',
        name: 'Corte kids',
        priceCents: 3000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-corte-sobrancelha',
        name: 'Corte & Sobrancelha',
        priceCents: 3500,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-platinado-corte',
        name: 'Platinado & Corte',
        priceCents: 13000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'https://images.unsplash.com/photo-1534308983496-4fabb1a015ee?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-luzes-corte',
        name: 'Luzes & Corte',
        priceCents: 10000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'https://images.unsplash.com/photo-1512496015851-a90fb38ba796?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-corte-barba',
        name: 'Corte & Barba',
        priceCents: 5000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-barba',
        name: 'Barba',
        priceCents: 2500,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'https://images.unsplash.com/photo-1517832606589-7629c3397143?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-corte-barba-sobrancelha',
        name: 'Corte, Barba & Sobrancelha',
        priceCents: 5500,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-corte-barba-simples',
        name: 'Corte & barba simples',
        priceCents: 4000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'https://images.unsplash.com/photo-1622286342621-4bd786c2447c?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-corte-sobrancelha-barba-simples',
        name: 'Corte, sobrancelha & barba simples',
        priceCents: 4500,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'https://images.unsplash.com/photo-1605497788044-5a32c7078486?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'srv-pezinho-barba',
        name: 'Pezinho & Barba',
        priceCents: 3000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'https://images.unsplash.com/photo-1585747860715-2ba37e788b70?w=300&auto=format&fit=crop&q=80'
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
      // Horarios identicos a referencia
      const slots = [
        '09:00', '09:30', '10:00',
        '10:30', '11:00', '11:30',
        '12:00', '12:30', '13:00',
        '13:30', '15:30', '16:00',
        '16:30', '17:00', '17:30',
        '18:00'
      ];

      return slots.map((time) => {
        // Conflito simulado em alguns horarios especificos se for no primeiro dia
        const isConflict = false;
        return {
          time,
          available: !isConflict
        };
      });
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

    const handlePhoneSubmit = () => {
      phoneError.value = '';
      const raw = inputPhone.value.replace(/\D/g, '');
      if (raw.length < 10) {
        phoneError.value = 'Por favor, digite um telefone celular valido com DDD.';
        return;
      }

      // Procura se o telefone ja possui cadastro previo para auto-reconhecimento
      const foundInMock = mockKnownClients.find(c => c.phone.replace(/\D/g, '') === raw);
      const foundInStorage = JSON.parse(localStorage.getItem('caios_barber_known_users') || '[]')
        .find(c => c.phone.replace(/\D/g, '') === raw);

      const existing = foundInStorage || foundInMock;

      if (existing) {
        client.value.id = existing.id;
        client.value.phone = inputPhone.value.trim();
        client.value.email = existing.email || '';
        saveClientLocally();
        isIdentified.value = true;
        step.value = 'flow';
        currentStage.value = 1;
        showToast(`Bem-vindo de volta, ${client.value.name}!`);
      } else {
        client.value.phone = inputPhone.value.trim();
        step.value = 'ask_email';
      }
    };

    const handleEmailSubmit = () => {
      emailError.value = '';
      const trimmed = inputEmail.value.trim();
      if (!trimmed || !trimmed.includes('@') || !trimmed.includes('.')) {
        emailError.value = 'Por favor, informe um e-mail valido.';
        return;
      }

      client.value.id = 'c_' + Date.now();
      client.value.email = trimmed;
      saveClientLocally();
      isIdentified.value = true;
      step.value = 'flow';
      currentStage.value = 1;
      showToast('Cadastro concluido com sucesso!');
    };

    const selectBarber = (barber) => {
      selectedBarber.value = barber;
      if (currentStage.value === 1) {
        currentStage.value = 2;
      }
    };

    const saveClientLocally = () => {
      localStorage.setItem('caios_barber_client', JSON.stringify(client.value));
      
      const known = JSON.parse(localStorage.getItem('caios_barber_known_users') || '[]');
      const existsIndex = known.findIndex(k => k.phone === client.value.phone);
      if (existsIndex > -1) {
        known[existsIndex] = client.value;
      } else {
        known.push(client.value);
      }
      localStorage.setItem('caios_barber_known_users', JSON.stringify(known));
    };

    const resetClient = () => {
      localStorage.removeItem('caios_barber_client');
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
        if (currentStage.value === 3 && !selectedDay.value && availableDays.value.length > 0) {
          const firstWorkday = availableDays.value.find(d => d.isWorkday);
          selectedDay.value = firstWorkday ? firstWorkday.isoDate : availableDays.value[0].isoDate;
        }
      } else if (currentStage.value === 4) {
        finalizeAppointment();
      }
    };

    const finalizeAppointment = () => {
      const newAppointment = {
        id: 'apt_' + Date.now(),
        clientId: client.value.id,
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
        status: 'CONFIRMADO',
        createdAt: new Date().toISOString()
      };

      allAppointments.value.unshift(newAppointment);
      localStorage.setItem('caios_barber_appointments', JSON.stringify(allAppointments.value));
      lastConfirmedAppointment.value = newAppointment;

      showToast(`Agendamento confirmado! Enviamos comprovante para ${client.value.phone}.`);
      
      // Avanca diretamente para a etapa 5 de sucesso no chat, sem abrir modal
      currentStage.value = 5;
    };

    const startNewBooking = () => {
      currentStage.value = 1;
      selectedServices.value = [];
      selectedTime.value = '';
    };

    const cancelAppointment = (appointmentId) => {
      const list = allAppointments.value.map(a => {
        if (a.id === appointmentId) {
          return { ...a, status: 'CANCELADO' };
        }
        return a;
      });
      allAppointments.value = list;
      localStorage.setItem('caios_barber_appointments', JSON.stringify(list));
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
      // Carrega agendamentos salvos
      const stored = localStorage.getItem('caios_barber_appointments');
      if (stored) {
        try {
          allAppointments.value = JSON.parse(stored);
        } catch (e) {
          allAppointments.value = [];
        }
      }

      // Verifica se cliente ja esta salvo no navegador (2a iteracao em diante)
      const savedClient = localStorage.getItem('caios_barber_client');
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
