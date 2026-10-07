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
        id: 'c2-carlos',
        name: 'Carlos Oliveira',
        phone: '11988887777',
        email: 'carlos.oliveira@exemplo.com',
        notificationsEnabled: false
      }
    ];

    // Barbeiros Cadastrados (Barber Man) com UUIDs reais do PostgreSQL
    const barbers = ref([
      {
        id: '256ae6f2-f471-4d6f-8874-511bb968076d',
        name: 'Carlos Silva',
        role: 'Fundador & Barbeiro Master',
        bio: 'Administrador geral do Barber Man. 10 anos de experiencia em visagismo e cortes de alta precisao.',
        isAdmin: true,
        avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80'
      },
      {
        id: '3542d755-e20f-431f-b783-f1beb18b8a0f',
        name: 'Lucas Santana',
        role: 'Barbeiro Especialista',
        bio: 'Referencia em degrade navalhado, barboterapia e acabamentos detalhados.',
        isAdmin: false,
        avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80'
      },
      {
        id: 'b202cd1e-fd65-4008-be9d-b7e301dba107',
        name: 'Matheus Oliveira',
        role: 'Barbeiro & Colorista',
        bio: 'Especialista em platinados, luzes, pigmentacao e cortes modernos.',
        isAdmin: false,
        avatar: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150&auto=format&fit=crop&q=80'
      }
    ]);

    const selectedBarber = ref(barbers.value[0]);

    // Lista Exata dos 14 Servicos Solicitados com Precos e Duracoes
    const services = ref([
      {
        id: '71255167-685c-4401-bafd-ba7b3d28a5da',
        name: 'Botox/desondulaçao',
        priceCents: 5000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'https://images.unsplash.com/photo-1622286342621-4bd786c2447c?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'ad0075d8-7a6a-4cb6-a706-3058da6f47f6',
        name: 'Selagem',
        priceCents: 7500,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: '6ebfeaed-607f-490a-8875-a4add597cc8a',
        name: 'Hidratação',
        priceCents: 2000,
        durationMinutes: 15,
        durationLabel: '15min',
        image: 'https://images.unsplash.com/photo-1585747860715-2ba37e788b70?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: '5caac1e8-fbca-4b97-a107-13782cad4b40',
        name: 'Alisamento',
        priceCents: 2000,
        durationMinutes: 15,
        durationLabel: '15min',
        image: 'https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'a888baaf-c1b6-4161-aa50-d586b81ce2c8',
        name: 'Pigmentação',
        priceCents: 2000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'af31c8c8-b37a-4164-81c7-85aeeaad0f72',
        name: 'Corte',
        priceCents: 3000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'https://images.unsplash.com/photo-1605497788044-5a32c7078486?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'b0243eb9-2acc-4111-bf3c-3bd0df6aa9bb',
        name: 'Corte kids',
        priceCents: 3000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'assets/services/corte_kids.jpg'
      },
      {
        id: 'b966c13f-7a2a-4023-b3e4-67119c3bd6cf',
        name: 'Corte & Sobrancelha',
        priceCents: 3500,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'adfdc695-da63-4b35-98d0-d704ba3d5c76',
        name: 'Platinado & Corte',
        priceCents: 13000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'https://images.unsplash.com/photo-1534308983496-4fabb1a015ee?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: 'c94cfaa6-b11d-4665-a2d0-057e7da7d0bc',
        name: 'Luzes & Corte',
        priceCents: 10000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'assets/services/luzes_corte.jpg'
      },
      {
        id: '7bac199d-6214-4f23-8d3d-900080bff0bd',
        name: 'Corte & Barba',
        priceCents: 5000,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=300&auto=format&fit=crop&q=80'
      },
      {
        id: '68c71d3d-7a24-4a3d-8576-25c7d56ccff4',
        name: 'Barba',
        priceCents: 2500,
        durationMinutes: 30,
        durationLabel: '30min',
        image: './assets/services/barba.jpg'
      },
      {
        id: 'e982fc13-3932-447e-b777-e5f66ea065df',
        name: 'Corte, Barba & Sobrancelha',
        priceCents: 5500,
        durationMinutes: 60,
        durationLabel: '1hr',
        image: 'assets/services/corte_barba_sobrancelha.jpg'
      },
      {
        id: '59448ee1-f364-4862-baa6-8bbf46da6b34',
        name: 'Corte & barba simples',
        priceCents: 4000,
        durationMinutes: 30,
        durationLabel: '30min',
        image: 'assets/services/corte_barba_simples.jpg'
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

    // BroadcastChannel para sincronizacao imediata entre abas do navegador
    const syncChannel = typeof window !== 'undefined' && window.BroadcastChannel ? new BroadcastChannel('barber_notify_sync') : null;
    if (syncChannel) {
      syncChannel.onmessage = (event) => {
        if (event.data === 'appointment_changed') {
          fetchSlots();
        }
      };
    }

    const fetchSlots = async () => {
      if (!selectedBarber.value?.id || !selectedDay.value) return;
      try {
        const duration = totalDurationMinutes.value > 0 ? totalDurationMinutes.value : 30;
        const res = await fetch(`http://localhost:8081/api/appointments/slots?barberId=${selectedBarber.value.id}&date=${selectedDay.value}&duration=${duration}`);
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
        const checkRes = await fetch(`http://localhost:8081/api/clients?phone=${raw}`);
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
        const clientRes = await fetch('http://localhost:8081/api/clients', {
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
        const clientRes = await fetch('http://localhost:8081/api/clients', {
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
        const aptRes = await fetch('http://localhost:8081/api/appointments', {
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

        // 4. Agendamento validado e salvo com sucesso no banco de dados!
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
          status: 'CONFIRMADO',
          createdAt: new Date().toISOString()
        };

        allAppointments.value.unshift(newAppointment);
        localStorage.setItem('barber_man_appointments', JSON.stringify(allAppointments.value));
        lastConfirmedAppointment.value = newAppointment;

        // Notifica outras abas locais para atualizarem os slots
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

    const cancelAppointment = (appointmentId) => {
      const list = allAppointments.value.map(a => {
        if (a.id === appointmentId) {
          return { ...a, status: 'CANCELADO' };
        }
        return a;
      });
      allAppointments.value = list;
      localStorage.setItem('barber_man_appointments', JSON.stringify(list));
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
      const stored = localStorage.getItem('barber_man_appointments');
      if (stored) {
        try {
          allAppointments.value = JSON.parse(stored);
        } catch (e) {
          allAppointments.value = [];
        }
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

      // Sincroniza lista de barbeiros com a API do backend
      fetch('http://localhost:8081/api/barbers')
        .then(r => r.json())
        .then(data => {
          if (Array.isArray(data) && data.length > 0) {
            barbers.value = data.map((b, idx) => ({
              id: b.id,
              name: b.name,
              role: b.isAdmin ? 'Fundador & Barbeiro Master' : 'Barbeiro Especialista',
              bio: b.bio || '',
              isAdmin: b.isAdmin,
              avatar: b.avatarUrl || barbers.value[idx]?.avatar || 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80'
            }));
            selectedBarber.value = barbers.value[0];
          }
        })
        .catch(e => console.warn('Usando barbeiros locais:', e));

      // Sincroniza UUIDs dos servicos com o backend preservando a lista e fotos
      fetch('http://localhost:8081/api/services')
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
