// ============================================================================
// Focus Mood App - JavaScript Logic with To-Do List & Daywise Tracker
// ============================================================================

document.addEventListener('DOMContentLoaded', () => {
  // --------------------------------------------------------------------------
  // 1. Mood Configuration & Theme Colors
  // --------------------------------------------------------------------------
  const MOOD_QUOTES = {
    forest: '"Quiet your mind, like the still trees in an ancient forest."',
    rain: '"Like raindrops falling steadily, small efforts accumulate into greatness."',
    sunset: '"Honor the rhythm of work and rest. Each sunset brings peace."',
    midnight: '"The stars shine brightest in the stillness of deep night."',
    minimal: '"Eliminate the unnecessary so that the essential may speak."'
  };

  const MOOD_THEME_COLORS = {
    forest: '#0b1712',
    rain: '#0c121e',
    sunset: '#1c0f16',
    midnight: '#070714',
    minimal: '#111111'
  };

  // --------------------------------------------------------------------------
  // 2. Timer Configuration & State (With Custom Durations)
  // --------------------------------------------------------------------------
  const DEFAULT_MINUTES = {
    focus: 25,
    shortBreak: 5,
    longBreak: 15
  };

  let customMinutes = { ...DEFAULT_MINUTES };
  try {
    const savedCustom = localStorage.getItem('fm_custom_durations');
    if (savedCustom) {
      customMinutes = Object.assign(customMinutes, JSON.parse(savedCustom));
    }
  } catch (e) {
    customMinutes = { ...DEFAULT_MINUTES };
  }

  const MODES = {
    focus: { name: 'Deep Focus', duration: customMinutes.focus * 60 },
    shortBreak: { name: 'Short Break', duration: customMinutes.shortBreak * 60 },
    longBreak: { name: 'Long Break', duration: customMinutes.longBreak * 60 }
  };

  let currentMode = 'focus';
  let timeLeft = MODES[currentMode].duration;
  let totalDuration = MODES[currentMode].duration;
  let timerInterval = null;
  let isRunning = false;

  // Global All-Time stats
  let totalMinutesFocused = parseInt(localStorage.getItem('fm_focus_minutes') || '0', 10);

  // DOM Elements - Timer
  const timeDisplay = document.getElementById('timeDisplay');
  const modeStatus = document.getElementById('modeStatus');
  const startPauseBtn = document.getElementById('startPauseBtn');
  const startPauseText = document.getElementById('startPauseText');
  const resetBtn = document.getElementById('resetBtn');
  const skipBtn = document.getElementById('skipBtn');
  const tabButtons = document.querySelectorAll('.tab-btn');
  const tabFocus = document.getElementById('tabFocus');
  const tabShort = document.getElementById('tabShort');
  const tabLong = document.getElementById('tabLong');
  const progressRing = document.getElementById('progressRing');
  const todaySessionsEl = document.getElementById('todaySessionsDisplay');
  const todayMinutesEl = document.getElementById('todayMinutesDisplay');
  const allTimeMinutesEl = document.getElementById('allTimeMinutesDisplay');
  const themeColorMeta = document.getElementById('themeColorMeta');
  const clockDisplayWrap = document.getElementById('clockDisplayWrap');
  const timerActiveTaskPill = document.getElementById('timerActiveTaskPill');
  const timerActiveTaskText = document.getElementById('timerActiveTaskText');
  const unfocusTaskBtn = document.getElementById('unfocusTaskBtn');

  const RING_CIRCUMFERENCE = 2 * Math.PI * 130; // 816.814
  progressRing.style.strokeDasharray = `${RING_CIRCUMFERENCE}`;
  progressRing.style.strokeDashoffset = '0';

  function updateTabLabels() {
    if (tabFocus) tabFocus.textContent = `Focus (${customMinutes.focus}m)`;
    if (tabShort) tabShort.textContent = `Short (${customMinutes.shortBreak}m)`;
    if (tabLong) tabLong.textContent = `Long (${customMinutes.longBreak}m)`;
  }

  // --------------------------------------------------------------------------
  // 3. Mood Theme Selector
  // --------------------------------------------------------------------------
  const moodChips = document.querySelectorAll('.mood-chip');
  const moodQuoteEl = document.getElementById('moodQuote');

  function setMood(moodKey) {
    if (!MOOD_QUOTES[moodKey]) moodKey = 'forest';
    document.body.dataset.mood = moodKey;
    localStorage.setItem('fm_saved_mood', moodKey);

    if (themeColorMeta && MOOD_THEME_COLORS[moodKey]) {
      themeColorMeta.setAttribute('content', MOOD_THEME_COLORS[moodKey]);
    }

    moodChips.forEach(chip => {
      chip.classList.toggle('active', chip.dataset.setMood === moodKey);
    });

    if (moodQuoteEl) {
      moodQuoteEl.textContent = MOOD_QUOTES[moodKey];
    }
  }

  moodChips.forEach(chip => {
    chip.addEventListener('click', () => {
      setMood(chip.dataset.setMood);
    });
  });

  const savedMood = localStorage.getItem('fm_saved_mood') || 'forest';
  setMood(savedMood);

  // --------------------------------------------------------------------------
  // 4. Daywise Focus Tracker Logic (LocalStorage)
  // --------------------------------------------------------------------------
  function getLocalDateKey(date = new Date()) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  function getDaywiseHistory() {
    try {
      const raw = localStorage.getItem('fm_daywise_history');
      return raw ? JSON.parse(raw) : {};
    } catch (e) {
      return {};
    }
  }

  function saveDaywiseHistory(data) {
    localStorage.setItem('fm_daywise_history', JSON.stringify(data));
  }

  function recordDaywiseSession(minutes) {
    const history = getDaywiseHistory();
    const todayKey = getLocalDateKey();

    if (!history[todayKey]) {
      history[todayKey] = { minutes: 0, sessions: 0 };
    }

    history[todayKey].minutes += minutes;
    history[todayKey].sessions += 1;

    saveDaywiseHistory(history);
    renderDaywiseTracker();
  }

  function calculateStreak(history) {
    let streak = 0;
    const now = new Date();
    const todayKey = getLocalDateKey(now);

    const hasToday = history[todayKey] && history[todayKey].minutes > 0;
    let checkDate = new Date();

    if (!hasToday) {
      checkDate.setDate(checkDate.getDate() - 1);
    }

    while (true) {
      const key = getLocalDateKey(checkDate);
      if (history[key] && history[key].minutes > 0) {
        streak++;
        checkDate.setDate(checkDate.getDate() - 1);
      } else {
        break;
      }
    }

    return streak;
  }

  const streakBadge = document.getElementById('streakBadge');
  const weeklyTotalText = document.getElementById('weeklyTotalText');
  const activityBarsGrid = document.getElementById('activityBarsGrid');
  const dailyHistoryList = document.getElementById('dailyHistoryList');
  const historyToggleBtn = document.getElementById('historyToggleBtn');
  const historyContent = document.getElementById('historyContent');
  const clearHistoryBtn = document.getElementById('clearHistoryBtn');

  function renderDaywiseTracker() {
    const history = getDaywiseHistory();
    const todayKey = getLocalDateKey();
    const todayData = history[todayKey] || { minutes: 0, sessions: 0 };

    if (todaySessionsEl) todaySessionsEl.textContent = todayData.sessions;
    if (todayMinutesEl) todayMinutesEl.textContent = `${todayData.minutes}m`;
    if (allTimeMinutesEl) allTimeMinutesEl.textContent = `${totalMinutesFocused}m`;

    const histTodaySessions = document.getElementById('histTodaySessions');
    const histTodayMinutes = document.getElementById('histTodayMinutes');
    const histAllTimeMinutes = document.getElementById('histAllTimeMinutes');
    if (histTodaySessions) histTodaySessions.textContent = todayData.sessions;
    if (histTodayMinutes) histTodayMinutes.textContent = `${todayData.minutes}m`;
    if (histAllTimeMinutes) histAllTimeMinutes.textContent = `${totalMinutesFocused}m`;

    const streak = calculateStreak(history);
    if (streakBadge) {
      streakBadge.textContent = `🔥 ${streak} Day${streak === 1 ? '' : 's'} Streak`;
    }

    const past7Days = [];
    let weeklyMinutes = 0;
    let maxMinutesInWeek = 30;

    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const key = getLocalDateKey(d);
      const dayData = history[key] || { minutes: 0, sessions: 0 };
      const dayName = d.toLocaleDateString(undefined, { weekday: 'short' });
      const isToday = (key === todayKey);

      weeklyMinutes += dayData.minutes;
      if (dayData.minutes > maxMinutesInWeek) {
        maxMinutesInWeek = dayData.minutes;
      }

      past7Days.push({
        key,
        dayName,
        minutes: dayData.minutes,
        sessions: dayData.sessions,
        isToday
      });
    }

    if (weeklyTotalText) {
      weeklyTotalText.textContent = `Week: ${weeklyMinutes}m`;
    }

    if (activityBarsGrid) {
      activityBarsGrid.innerHTML = '';
      past7Days.forEach(day => {
        const col = document.createElement('div');
        col.className = `bar-column ${day.isToday ? 'is-today' : ''}`;

        const heightPercent = day.minutes > 0 ? Math.max(12, Math.round((day.minutes / maxMinutesInWeek) * 100)) : 4;
        const tooltip = `${day.dayName} (${day.key}): ${day.minutes}m (${day.sessions} sessions)`;

        col.innerHTML = `
          <div class="bar-pill-wrap" title="${tooltip}">
            <div class="bar-fill" style="height: ${heightPercent}%;"></div>
          </div>
          <span class="bar-label">${day.dayName}</span>
        `;
        activityBarsGrid.appendChild(col);
      });
    }

    if (dailyHistoryList) {
      dailyHistoryList.innerHTML = '';
      const dateKeys = Object.keys(history).sort().reverse();

      if (dateKeys.length === 0) {
        dailyHistoryList.innerHTML = `<div class="history-empty">No focus sessions recorded yet. Start your first session!</div>`;
      } else {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        const yesterdayKey = getLocalDateKey(yesterday);

        dateKeys.forEach(key => {
          const item = history[key];
          let displayDate = key;

          if (key === todayKey) {
            displayDate = `Today <span class="today-tag">Now</span>`;
          } else if (key === yesterdayKey) {
            displayDate = `Yesterday`;
          } else {
            const parts = key.split('-');
            if (parts.length === 3) {
              const dObj = new Date(parts[0], parts[1] - 1, parts[2]);
              displayDate = dObj.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
            }
          }

          const row = document.createElement('div');
          row.className = 'history-item';
          row.innerHTML = `
            <div class="history-date">${displayDate}</div>
            <div class="history-stats-right">
              <span class="history-minutes">${item.minutes}m</span>
              <span class="history-sessions">(${item.sessions} session${item.sessions === 1 ? '' : 's'})</span>
            </div>
          `;
          dailyHistoryList.appendChild(row);
        });
      }
    }
  }

  if (historyToggleBtn && historyContent) {
    historyToggleBtn.addEventListener('click', () => {
      const isHidden = historyContent.classList.toggle('hidden');
      historyToggleBtn.classList.toggle('open', !isHidden);
    });
  }

  if (clearHistoryBtn) {
    clearHistoryBtn.addEventListener('click', () => {
      if (confirm('Are you sure you want to clear your daywise focus history?')) {
        localStorage.removeItem('fm_daywise_history');
        renderDaywiseTracker();
      }
    });
  }

  // --------------------------------------------------------------------------
  // 5. Timer Mechanics
  // --------------------------------------------------------------------------
  function formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  function updateDisplay() {
    timeDisplay.textContent = formatTime(timeLeft);
    document.title = `${formatTime(timeLeft)} • Focus Mood`;

    const progress = totalDuration > 0 ? (totalDuration - timeLeft) / totalDuration : 0;
    const offset = RING_CIRCUMFERENCE * (1 - Math.min(Math.max(progress, 0), 1));
    progressRing.style.strokeDashoffset = offset;
  }

  function switchMode(modeKey) {
    if (!MODES[modeKey]) return;
    pauseTimer();
    currentMode = modeKey;
    totalDuration = MODES[modeKey].duration;
    timeLeft = totalDuration;

    tabButtons.forEach(btn => {
      btn.classList.toggle('active', btn.dataset.mode === modeKey);
    });

    modeStatus.textContent = `Ready to ${MODES[modeKey].name.toLowerCase()}`;
    updateDisplay();
  }

  function startTimer() {
    if (isRunning) return;
    isRunning = true;
    startPauseText.textContent = 'Pause';
    startPauseBtn.querySelector('.icon').textContent = '⏸';
    modeStatus.textContent = `Focusing...`;

    // Prompt for browser notification permission on first timer start if not decided yet
    if ('Notification' in window && Notification.permission === 'default') {
      requestNotificationPermission(false);
    }

    timerInterval = setInterval(() => {
      if (timeLeft > 0) {
        timeLeft--;
        updateDisplay();
      } else {
        handleTimerComplete();
      }
    }, 1000);
  }

  function pauseTimer() {
    isRunning = false;
    clearInterval(timerInterval);
    timerInterval = null;
    startPauseText.textContent = 'Start';
    startPauseBtn.querySelector('.icon').textContent = '▶';
    modeStatus.textContent = 'Paused';
  }

  function handleTimerComplete() {
    pauseTimer();

    // Default sounds removed! Trigger browser notification
    const isFocus = (currentMode === 'focus');
    const title = isFocus ? '🎉 Focus Session Complete!' : '⚡ Break Finished!';
    let bodyText = isFocus
      ? 'Great job! Time for a well-deserved break.'
      : 'Break finished! Ready to get back into focus?';

    const focusedTask = tasks.find(t => String(t.id) === String(activeFocusedTaskId));
    if (isFocus && focusedTask) {
      bodyText = `Completed focus session on: "${focusedTask.text}"! Take a break.`;
    }

    sendBrowserNotification(title, bodyText);
    showToast(`🔔 ${title} ${bodyText}`);

    if (currentMode === 'focus') {
      const minsCompleted = Math.round(MODES.focus.duration / 60);
      totalMinutesFocused += minsCompleted;
      localStorage.setItem('fm_focus_minutes', totalMinutesFocused);

      recordDaywiseSession(minsCompleted);

      switchMode('shortBreak');
      modeStatus.textContent = 'Session complete! Break time.';
    } else {
      switchMode('focus');
      modeStatus.textContent = 'Break finished! Back to focus.';
    }
  }

  function resetTimer() {
    pauseTimer();
    timeLeft = totalDuration;
    modeStatus.textContent = 'Ready to focus';
    updateDisplay();
  }

  function skipSession() {
    if (confirm('Skip to the next session?')) {
      if (currentMode === 'focus') {
        switchMode('shortBreak');
      } else {
        switchMode('focus');
      }
    }
  }

  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      switchMode(btn.dataset.mode);
    });
  });

  startPauseBtn.addEventListener('click', () => {
    if (isRunning) {
      pauseTimer();
    } else {
      startTimer();
    }
  });

  resetBtn.addEventListener('click', resetTimer);
  skipBtn.addEventListener('click', skipSession);

  // --------------------------------------------------------------------------
  // 6. Quick Minute Adjuster Buttons (-5m, -1m, +1m, +5m)
  // --------------------------------------------------------------------------
  const quickAdjButtons = document.querySelectorAll('.quick-adj-btn');
  quickAdjButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const deltaMins = parseInt(btn.dataset.delta, 10);
      if (isNaN(deltaMins)) return;

      const deltaSecs = deltaMins * 60;
      const newTime = timeLeft + deltaSecs;

      if (newTime >= 10 && newTime <= 180 * 60) {
        timeLeft = newTime;
        if (!isRunning) {
          totalDuration = Math.max(totalDuration, timeLeft);
        } else {
          totalDuration = Math.max(totalDuration, totalDuration + deltaSecs);
        }
        updateDisplay();
      }
    });
  });

  // --------------------------------------------------------------------------
  // 7. Custom Timer Settings Modal
  // --------------------------------------------------------------------------
  const customTimerBtn = document.getElementById('customTimerBtn');
  const customTimerModal = document.getElementById('customTimerModal');
  const closeModalBtn = document.getElementById('closeModalBtn');
  const customTimerForm = document.getElementById('customTimerForm');
  const customFocusInput = document.getElementById('customFocusInput');
  const customShortInput = document.getElementById('customShortInput');
  const customLongInput = document.getElementById('customLongInput');
  const valFocusLabel = document.getElementById('valFocusLabel');
  const valShortLabel = document.getElementById('valShortLabel');
  const valLongLabel = document.getElementById('valLongLabel');
  const resetDefaultsBtn = document.getElementById('resetDefaultsBtn');
  const presetChips = document.querySelectorAll('.preset-chip');

  function openCustomModal() {
    customFocusInput.value = customMinutes.focus;
    customShortInput.value = customMinutes.shortBreak;
    customLongInput.value = customMinutes.longBreak;
    valFocusLabel.textContent = customMinutes.focus;
    valShortLabel.textContent = customMinutes.shortBreak;
    valLongLabel.textContent = customMinutes.longBreak;
    customTimerModal.classList.remove('hidden');
  }

  function closeCustomModal() {
    customTimerModal.classList.add('hidden');
  }

  if (customTimerBtn) {
    customTimerBtn.addEventListener('click', openCustomModal);
  }

  if (clockDisplayWrap) {
    clockDisplayWrap.addEventListener('click', () => {
      if (!isRunning) {
        openCustomModal();
      }
    });
  }

  if (closeModalBtn) {
    closeModalBtn.addEventListener('click', closeCustomModal);
  }

  if (customTimerModal) {
    customTimerModal.addEventListener('click', (e) => {
      if (e.target === customTimerModal) {
        closeCustomModal();
      }
    });
  }

  customFocusInput.addEventListener('input', () => {
    valFocusLabel.textContent = customFocusInput.value || 0;
  });
  customShortInput.addEventListener('input', () => {
    valShortLabel.textContent = customShortInput.value || 0;
  });
  customLongInput.addEventListener('input', () => {
    valLongLabel.textContent = customLongInput.value || 0;
  });

  presetChips.forEach(chip => {
    chip.addEventListener('click', () => {
      const targetId = chip.dataset.target;
      const val = parseInt(chip.dataset.val, 10);
      const targetInput = document.getElementById(targetId);
      if (targetInput) {
        targetInput.value = val;
        targetInput.dispatchEvent(new Event('input'));
      }
    });
  });

  if (resetDefaultsBtn) {
    resetDefaultsBtn.addEventListener('click', () => {
      customFocusInput.value = DEFAULT_MINUTES.focus;
      customShortInput.value = DEFAULT_MINUTES.shortBreak;
      customLongInput.value = DEFAULT_MINUTES.longBreak;
      valFocusLabel.textContent = DEFAULT_MINUTES.focus;
      valShortLabel.textContent = DEFAULT_MINUTES.shortBreak;
      valLongLabel.textContent = DEFAULT_MINUTES.longBreak;
    });
  }

  if (customTimerForm) {
    customTimerForm.addEventListener('submit', (e) => {
      e.preventDefault();

      const newFocus = Math.max(1, Math.min(180, parseInt(customFocusInput.value, 10) || 25));
      const newShort = Math.max(1, Math.min(60, parseInt(customShortInput.value, 10) || 5));
      const newLong = Math.max(1, Math.min(60, parseInt(customLongInput.value, 10) || 15));

      customMinutes = {
        focus: newFocus,
        shortBreak: newShort,
        longBreak: newLong
      };

      localStorage.setItem('fm_custom_durations', JSON.stringify(customMinutes));

      MODES.focus.duration = customMinutes.focus * 60;
      MODES.shortBreak.duration = customMinutes.shortBreak * 60;
      MODES.longBreak.duration = customMinutes.longBreak * 60;

      updateTabLabels();

      pauseTimer();
      totalDuration = MODES[currentMode].duration;
      timeLeft = totalDuration;
      modeStatus.textContent = `Ready to ${MODES[currentMode].name.toLowerCase()}`;
      updateDisplay();

      closeCustomModal();
    });
  }

  // --------------------------------------------------------------------------
  // Helper: Toast Notifications
  // --------------------------------------------------------------------------
  let toastEl = document.querySelector('.app-toast');
  if (!toastEl) {
    toastEl = document.createElement('div');
    toastEl.className = 'app-toast';
    document.body.appendChild(toastEl);
  }
  let toastTimer = null;

  function showToast(message, duration = 3500) {
    if (!toastEl) return;
    toastEl.textContent = message;
    toastEl.classList.add('visible');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toastEl.classList.remove('visible');
    }, duration);
  }

  // --------------------------------------------------------------------------
  // Browser & Android Native Notifications Support
  // --------------------------------------------------------------------------
  const notifyBtn = document.getElementById('notifyBtn');
  const notifyBadgeDot = document.getElementById('notifyBadgeDot');

  function updateNotificationUI() {
    if (!notifyBtn) return;
    if (window.AndroidBridge) {
      notifyBtn.classList.remove('denied');
      notifyBtn.classList.add('granted');
      notifyBtn.title = 'System notifications active (Click to test)';
      if (notifyBadgeDot) notifyBadgeDot.classList.remove('hidden');
      return;
    }

    if (!('Notification' in window)) {
      notifyBtn.title = 'Browser notifications not supported';
      notifyBtn.classList.add('denied');
      if (notifyBadgeDot) notifyBadgeDot.classList.add('hidden');
      return;
    }

    if (Notification.permission === 'granted') {
      notifyBtn.classList.remove('denied');
      notifyBtn.classList.add('granted');
      notifyBtn.title = 'Browser notifications active (Click to test)';
      if (notifyBadgeDot) notifyBadgeDot.classList.remove('hidden');
    } else if (Notification.permission === 'denied') {
      notifyBtn.classList.remove('granted');
      notifyBtn.classList.add('denied');
      notifyBtn.title = 'Notifications blocked in browser settings';
      if (notifyBadgeDot) notifyBadgeDot.classList.add('hidden');
    } else {
      notifyBtn.classList.remove('granted', 'denied');
      notifyBtn.title = 'Click to enable browser notifications';
      if (notifyBadgeDot) notifyBadgeDot.classList.add('hidden');
    }
  }

  async function requestNotificationPermission(isManual = false) {
    if (window.AndroidBridge) {
      if (window.AndroidBridge.requestNotificationPermission) {
        window.AndroidBridge.requestNotificationPermission();
      }
      if (isManual) {
        showToast('Notifications active! Sending test alert...');
        sendBrowserNotification('🔔 Focus Mood Notifications', 'System notifications are working properly!');
      }
      updateNotificationUI();
      return true;
    }

    if (!('Notification' in window)) {
      if (isManual) showToast('Browser notifications are not supported in this browser.');
      return false;
    }

    if (Notification.permission === 'granted') {
      if (isManual) {
        showToast('Notifications active! Sending test alert...');
        sendBrowserNotification('🔔 Focus Mood Notifications', 'Browser notifications are working properly!');
      }
      updateNotificationUI();
      return true;
    }

    if (Notification.permission === 'denied') {
      if (isManual) {
        showToast('Notifications are blocked. Please enable them in your browser site permissions.');
      }
      updateNotificationUI();
      return false;
    }

    try {
      const permission = await Notification.requestPermission();
      updateNotificationUI();
      if (permission === 'granted') {
        showToast('🔔 Browser notifications enabled!');
        sendBrowserNotification('🎉 Notifications Enabled', 'You will be notified when your focus & break sessions complete.');
        return true;
      } else {
        if (isManual) showToast('Notifications were not granted.');
        return false;
      }
    } catch (e) {
      console.warn('Notification permission error:', e);
      return false;
    }
  }

  function sendBrowserNotification(title, body) {
    if (window.AndroidBridge && window.AndroidBridge.postNotification) {
      window.AndroidBridge.postNotification(title, body);
      return;
    }

    if (!('Notification' in window) || Notification.permission !== 'granted') return;
    try {
      const svgIcon = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="45" fill="%2310b981"/><path d="M35 50 L45 60 L65 40" stroke="white" stroke-width="8" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
      const n = new Notification(title, {
        body,
        icon: svgIcon,
        tag: 'focus-mood-alert',
        renotify: true
      });
      n.onclick = () => {
        window.focus();
        n.close();
      };
    } catch (e) {
      console.warn('Could not display notification:', e);
    }
  }

  if (notifyBtn) {
    notifyBtn.addEventListener('click', () => {
      requestNotificationPermission(true);
    });
  }

  // --------------------------------------------------------------------------
  // 8. Custom MP3 Music Player (LocalStorage Persistence & Easy Song Switching)
  // --------------------------------------------------------------------------
  const customAudioElement = document.getElementById('customAudioElement');
  const mp3FileInput = document.getElementById('mp3FileInput');
  const nowPlayingTitle = document.getElementById('nowPlayingTitle');
  const nowPlayingStatus = document.getElementById('nowPlayingStatus');
  const equalizerAnim = document.getElementById('equalizerAnim');
  const audioCurrentTime = document.getElementById('audioCurrentTime');
  const audioTotalDuration = document.getElementById('audioTotalDuration');
  const audioSeekBar = document.getElementById('audioSeekBar');
  const playPauseTrackBtn = document.getElementById('playPauseTrackBtn');
  const playPauseIcon = document.getElementById('playPauseIcon');
  const prevTrackBtn = document.getElementById('prevTrackBtn');
  const nextTrackBtn = document.getElementById('nextTrackBtn');
  const loopTrackBtn = document.getElementById('loopTrackBtn');
  const volumeSlider = document.getElementById('volumeSlider');
  const muteToggleBtn = document.getElementById('muteToggleBtn');
  const playlistList = document.getElementById('playlistList');
  const trackCountLabel = document.getElementById('trackCountLabel');
  const storageUsageBadge = document.getElementById('storageUsageBadge');
  const clearAllTracksBtn = document.getElementById('clearAllTracksBtn');

  let customTracks = [];
  try {
    const savedTracks = localStorage.getItem('fm_custom_tracks');
    customTracks = savedTracks ? JSON.parse(savedTracks) : [];
  } catch (e) {
    customTracks = [];
  }

  let currentTrackId = localStorage.getItem('fm_active_track_id') || null;
  let loopMode = localStorage.getItem('fm_loop_mode') || 'all'; // 'all', 'one', 'off'
  let savedVolume = parseFloat(localStorage.getItem('fm_audio_volume') || '0.7');
  if (isNaN(savedVolume) || savedVolume < 0 || savedVolume > 1) savedVolume = 0.7;
  let isSeeking = false;
  let previousVolume = savedVolume > 0 ? savedVolume : 0.7;

  if (customAudioElement) {
    customAudioElement.volume = savedVolume;
  }
  if (volumeSlider) {
    volumeSlider.value = savedVolume;
  }

  function formatAudioDuration(secs) {
    if (isNaN(secs) || secs < 0 || !isFinite(secs)) return '0:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${String(s).padStart(2, '0')}`;
  }

  function formatBytes(bytes) {
    if (bytes === 0) return '0 KB';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  function calculateStorageUsage() {
    let totalChars = 0;
    try {
      const saved = localStorage.getItem('fm_custom_tracks') || '';
      totalChars = saved.length;
    } catch (e) {}
    return formatBytes(totalChars);
  }

  function updateStorageBadge() {
    if (storageUsageBadge) {
      storageUsageBadge.textContent = calculateStorageUsage();
    }
  }

  function saveCustomTracks() {
    try {
      localStorage.setItem('fm_custom_tracks', JSON.stringify(customTracks));
      updateStorageBadge();
      return true;
    } catch (e) {
      console.error('LocalStorage save error:', e);
      if (e.name === 'QuotaExceededError' || e.code === 22) {
        showToast('⚠️ Storage limit reached! Browser localStorage allows ~5MB. Delete an existing song or upload a smaller audio file.');
      } else {
        showToast('⚠️ Could not save song to localStorage.');
      }
      return false;
    }
  }

  function getCurrentTrackIndex() {
    return customTracks.findIndex(t => String(t.id) === String(currentTrackId));
  }

  function updateLoopButtonUI() {
    if (!loopTrackBtn) return;
    loopTrackBtn.dataset.loop = loopMode;
    if (loopMode === 'all') {
      loopTrackBtn.textContent = '🔁';
      loopTrackBtn.title = 'Repeat Mode: All Songs';
      loopTrackBtn.classList.add('active');
    } else if (loopMode === 'one') {
      loopTrackBtn.textContent = '🔂';
      loopTrackBtn.title = 'Repeat Mode: Current Song';
      loopTrackBtn.classList.add('active');
    } else {
      loopTrackBtn.textContent = '➡️';
      loopTrackBtn.title = 'Repeat Mode: Off (Stop at end)';
      loopTrackBtn.classList.remove('active');
    }
  }

  function renderPlaylist() {
    if (!playlistList) return;
    playlistList.innerHTML = '';

    const count = customTracks.length;
    if (trackCountLabel) trackCountLabel.textContent = count;
    if (clearAllTracksBtn) clearAllTracksBtn.classList.toggle('hidden', count === 0);
    updateStorageBadge();
    updateLoopButtonUI();

    if (count === 0) {
      playlistList.innerHTML = `
        <div class="playlist-empty">
          No custom songs uploaded yet.<br>
          <label for="mp3FileInput" class="playlist-empty-upload">+ Upload MP3 audio</label> to focus with your music.
        </div>
      `;
      if (nowPlayingTitle) nowPlayingTitle.textContent = 'No song selected';
      if (nowPlayingStatus) nowPlayingStatus.textContent = 'Upload or select a track below';
      if (equalizerAnim) equalizerAnim.classList.remove('playing');
      if (playPauseIcon) playPauseIcon.textContent = '▶';
      if (playPauseTrackBtn) playPauseTrackBtn.title = 'Play';
      return;
    }

    // Verify currentTrackId exists in playlist
    let activeTrack = customTracks.find(t => String(t.id) === String(currentTrackId));
    if (!activeTrack && customTracks.length > 0) {
      activeTrack = customTracks[0];
      currentTrackId = activeTrack.id;
      localStorage.setItem('fm_active_track_id', currentTrackId);
    }

    if (activeTrack) {
      if (nowPlayingTitle) nowPlayingTitle.textContent = activeTrack.name;
      const isAudioPlaying = customAudioElement && !customAudioElement.paused && customAudioElement.src;
      if (nowPlayingStatus) {
        nowPlayingStatus.textContent = isAudioPlaying ? 'Playing' : (activeTrack.size ? `${activeTrack.size} • Ready` : 'Ready');
      }
    }

    customTracks.forEach((track) => {
      const isActive = String(track.id) === String(currentTrackId);
      const isPlaying = isActive && customAudioElement && !customAudioElement.paused;

      const item = document.createElement('div');
      item.className = `playlist-item ${isActive ? 'active' : ''}`;
      item.dataset.id = track.id;

      item.innerHTML = `
        <div class="playlist-item-left">
          <span class="playlist-track-icon">${isPlaying ? '🔊' : '🎵'}</span>
          <div class="playlist-track-info">
            <span class="playlist-track-name" title="${escapeHtml(track.name)}">${escapeHtml(track.name)}</span>
            <span class="playlist-track-size">${track.size || 'Audio file'}</span>
          </div>
        </div>
        <div class="playlist-item-actions">
          <span class="playlist-play-icon" title="${isPlaying ? 'Playing' : 'Play this song'}">${isPlaying ? '⏸' : '▶'}</span>
          <button type="button" class="playlist-del-btn" title="Delete song" aria-label="Delete song">✕</button>
        </div>
      `;

      // Click to select and switch song easily
      item.addEventListener('click', (e) => {
        if (e.target.closest('.playlist-del-btn')) return;

        if (String(track.id) === String(currentTrackId)) {
          if (customAudioElement.paused || !customAudioElement.src) {
            playTrack(track);
          } else {
            pauseTrack();
          }
        } else {
          currentTrackId = track.id;
          localStorage.setItem('fm_active_track_id', currentTrackId);
          playTrack(track);
        }
      });

      // Delete track
      const delBtn = item.querySelector('.playlist-del-btn');
      delBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        deleteTrack(track.id);
      });

      playlistList.appendChild(item);
    });
  }

  function playTrack(track) {
    if (!track || !track.data || !customAudioElement) return;

    currentTrackId = track.id;
    localStorage.setItem('fm_active_track_id', currentTrackId);

    if (customAudioElement.src !== track.data) {
      customAudioElement.src = track.data;
    }

    customAudioElement.play().then(() => {
      if (playPauseIcon) playPauseIcon.textContent = '⏸';
      if (playPauseTrackBtn) playPauseTrackBtn.title = 'Pause';
      if (equalizerAnim) equalizerAnim.classList.add('playing');
      if (nowPlayingTitle) nowPlayingTitle.textContent = track.name;
      if (nowPlayingStatus) nowPlayingStatus.textContent = 'Playing';
      renderPlaylist();
    }).catch(err => {
      console.warn('Audio play error:', err);
      showToast('⚠️ Tap play to start audio playback');
    });
  }

  function pauseTrack() {
    if (!customAudioElement) return;
    customAudioElement.pause();
    if (playPauseIcon) playPauseIcon.textContent = '▶';
    if (playPauseTrackBtn) playPauseTrackBtn.title = 'Play';
    if (equalizerAnim) equalizerAnim.classList.remove('playing');
    if (nowPlayingStatus) nowPlayingStatus.textContent = 'Paused';
    renderPlaylist();
  }

  function deleteTrack(trackId) {
    const isCurrent = String(trackId) === String(currentTrackId);
    if (isCurrent && customAudioElement) {
      customAudioElement.pause();
      customAudioElement.removeAttribute('src');
      if (equalizerAnim) equalizerAnim.classList.remove('playing');
      if (playPauseIcon) playPauseIcon.textContent = '▶';
    }

    customTracks = customTracks.filter(t => String(t.id) !== String(trackId));
    saveCustomTracks();

    if (isCurrent) {
      if (customTracks.length > 0) {
        currentTrackId = customTracks[0].id;
        localStorage.setItem('fm_active_track_id', currentTrackId);
      } else {
        currentTrackId = null;
        localStorage.removeItem('fm_active_track_id');
      }
    }

    renderPlaylist();
    showToast('🗑 Song removed');
  }

  function playNextTrack() {
    if (customTracks.length === 0) return;
    const currentIndex = getCurrentTrackIndex();
    let nextIndex = currentIndex + 1;
    if (nextIndex >= customTracks.length) {
      nextIndex = 0;
    }
    const nextTrack = customTracks[nextIndex];
    if (nextTrack) {
      playTrack(nextTrack);
    }
  }

  function playPrevTrack() {
    if (customTracks.length === 0) return;
    const currentIndex = getCurrentTrackIndex();
    let prevIndex = currentIndex - 1;
    if (prevIndex < 0) {
      prevIndex = customTracks.length - 1;
    }
    const prevTrack = customTracks[prevIndex];
    if (prevTrack) {
      playTrack(prevTrack);
    }
  }

  // File Upload Processing
  if (mp3FileInput) {
    mp3FileInput.addEventListener('change', (e) => {
      const files = Array.from(e.target.files);
      if (!files || files.length === 0) return;

      let processed = 0;
      let addedCount = 0;
      let hadQuotaError = false;

      files.forEach(file => {
        const reader = new FileReader();
        reader.onload = (event) => {
          processed++;
          const base64Data = event.target.result;
          const newTrack = {
            id: 'track_' + Date.now() + '_' + Math.floor(Math.random() * 100000),
            name: file.name,
            size: formatBytes(file.size),
            data: base64Data,
            addedAt: Date.now()
          };

          customTracks.push(newTrack);
          const saved = saveCustomTracks();
          if (saved) {
            addedCount++;
            if (!currentTrackId) {
              currentTrackId = newTrack.id;
              localStorage.setItem('fm_active_track_id', currentTrackId);
            }
          } else {
            customTracks.pop(); // Revert
            hadQuotaError = true;
          }

          if (processed === files.length) {
            renderPlaylist();
            mp3FileInput.value = '';
            if (addedCount > 0) {
              showToast(`🎵 Uploaded ${addedCount} song${addedCount > 1 ? 's' : ''}! Saved in localStorage.`);
            }
            if (hadQuotaError) {
              showToast('⚠️ Storage limit reached (~5MB in browser localStorage). Could not store all songs.');
            }
          }
        };

        reader.onerror = () => {
          processed++;
          showToast(`⚠️ Error reading ${file.name}`);
        };

        reader.readAsDataURL(file);
      });
    });
  }

  // Play/Pause Button
  if (playPauseTrackBtn) {
    playPauseTrackBtn.addEventListener('click', () => {
      if (customTracks.length === 0) {
        showToast('Please upload an MP3 song first!');
        if (mp3FileInput) mp3FileInput.click();
        return;
      }

      let activeTrack = customTracks.find(t => String(t.id) === String(currentTrackId));
      if (!activeTrack) {
        activeTrack = customTracks[0];
        currentTrackId = activeTrack.id;
        localStorage.setItem('fm_active_track_id', currentTrackId);
      }

      if (customAudioElement.paused || !customAudioElement.src) {
        playTrack(activeTrack);
      } else {
        pauseTrack();
      }
    });
  }

  if (nextTrackBtn) {
    nextTrackBtn.addEventListener('click', () => {
      playNextTrack();
    });
  }

  if (prevTrackBtn) {
    prevTrackBtn.addEventListener('click', () => {
      playPrevTrack();
    });
  }

  // Loop Mode Toggle
  if (loopTrackBtn) {
    loopTrackBtn.addEventListener('click', () => {
      if (loopMode === 'all') {
        loopMode = 'one';
      } else if (loopMode === 'one') {
        loopMode = 'off';
      } else {
        loopMode = 'all';
      }
      localStorage.setItem('fm_loop_mode', loopMode);
      updateLoopButtonUI();
      showToast(`Repeat mode: ${loopMode === 'all' ? 'All Songs' : loopMode === 'one' ? 'Current Song' : 'Off'}`);
    });
  }

  // Clear all tracks
  if (clearAllTracksBtn) {
    clearAllTracksBtn.addEventListener('click', () => {
      if (confirm('Are you sure you want to remove all uploaded songs from localStorage?')) {
        if (customAudioElement) {
          customAudioElement.pause();
          customAudioElement.removeAttribute('src');
        }
        customTracks = [];
        currentTrackId = null;
        localStorage.removeItem('fm_custom_tracks');
        localStorage.removeItem('fm_active_track_id');
        renderPlaylist();
        showToast('All songs removed from storage');
      }
    });
  }

  // Native Audio Events
  if (customAudioElement) {
    customAudioElement.addEventListener('timeupdate', () => {
      if (!isSeeking && customAudioElement.duration) {
        const pct = (customAudioElement.currentTime / customAudioElement.duration) * 100;
        if (audioSeekBar) audioSeekBar.value = pct;
        if (audioCurrentTime) audioCurrentTime.textContent = formatAudioDuration(customAudioElement.currentTime);
      }
    });

    customAudioElement.addEventListener('loadedmetadata', () => {
      if (audioTotalDuration) {
        audioTotalDuration.textContent = formatAudioDuration(customAudioElement.duration);
      }
    });

    customAudioElement.addEventListener('ended', () => {
      if (loopMode === 'one') {
        customAudioElement.currentTime = 0;
        customAudioElement.play();
      } else if (loopMode === 'all') {
        playNextTrack();
      } else {
        // Off
        const currentIndex = getCurrentTrackIndex();
        if (currentIndex < customTracks.length - 1) {
          playNextTrack();
        } else {
          pauseTrack();
          customAudioElement.currentTime = 0;
          if (audioSeekBar) audioSeekBar.value = 0;
          if (audioCurrentTime) audioCurrentTime.textContent = '0:00';
        }
      }
    });

    customAudioElement.addEventListener('error', (e) => {
      console.warn('Audio playback error:', e);
      if (equalizerAnim) equalizerAnim.classList.remove('playing');
      if (playPauseIcon) playPauseIcon.textContent = '▶';
    });
  }

  // Seek bar scrub
  if (audioSeekBar) {
    audioSeekBar.addEventListener('input', () => {
      isSeeking = true;
      if (customAudioElement && customAudioElement.duration) {
        const time = (audioSeekBar.value / 100) * customAudioElement.duration;
        if (audioCurrentTime) audioCurrentTime.textContent = formatAudioDuration(time);
      }
    });

    audioSeekBar.addEventListener('change', () => {
      if (customAudioElement && customAudioElement.duration) {
        customAudioElement.currentTime = (audioSeekBar.value / 100) * customAudioElement.duration;
      }
      isSeeking = false;
    });
  }

  // Volume Slider & Mute Toggle
  if (volumeSlider) {
    volumeSlider.addEventListener('input', (e) => {
      const val = parseFloat(e.target.value);
      if (customAudioElement) customAudioElement.volume = val;
      localStorage.setItem('fm_audio_volume', val);
      if (muteToggleBtn) {
        muteToggleBtn.textContent = val === 0 ? '🔇' : (val < 0.5 ? '🔉' : '🔊');
      }
    });
  }

  if (muteToggleBtn) {
    muteToggleBtn.addEventListener('click', () => {
      if (!customAudioElement) return;
      if (customAudioElement.volume > 0) {
        previousVolume = customAudioElement.volume;
        customAudioElement.volume = 0;
        if (volumeSlider) volumeSlider.value = 0;
        muteToggleBtn.textContent = '🔇';
      } else {
        const restoreVal = previousVolume > 0 ? previousVolume : 0.7;
        customAudioElement.volume = restoreVal;
        if (volumeSlider) volumeSlider.value = restoreVal;
        muteToggleBtn.textContent = restoreVal < 0.5 ? '🔉' : '🔊';
      }
      localStorage.setItem('fm_audio_volume', customAudioElement.volume);
    });
  }

  // --------------------------------------------------------------------------
  // 9. Full To-Do Task List Features (LocalStorage)
  // --------------------------------------------------------------------------
  let tasks = [];
  try {
    const savedTasks = localStorage.getItem('fm_todo_tasks');
    tasks = savedTasks ? JSON.parse(savedTasks) : [];
  } catch (e) {
    tasks = [];
  }

  let activeFocusedTaskId = localStorage.getItem('fm_focused_task_id') || null;
  let currentTaskFilter = 'all';

  const todoForm = document.getElementById('todoForm');
  const todoInput = document.getElementById('todoInput');
  const todoList = document.getElementById('todoList');
  const todoProgressBadge = document.getElementById('todoProgressBadge');
  const todoProgressFill = document.getElementById('todoProgressFill');
  const clearCompletedBtn = document.getElementById('clearCompletedBtn');
  const filterChips = document.querySelectorAll('.todo-filters .filter-chip');
  const filterAll = document.getElementById('filterAll');
  const filterActive = document.getElementById('filterActive');
  const filterCompleted = document.getElementById('filterCompleted');

  function saveTasks() {
    localStorage.setItem('fm_todo_tasks', JSON.stringify(tasks));
    if (activeFocusedTaskId) {
      localStorage.setItem('fm_focused_task_id', activeFocusedTaskId);
    } else {
      localStorage.removeItem('fm_focused_task_id');
    }
  }

  function renderTasks() {
    const totalCount = tasks.length;
    const completedCount = tasks.filter(t => t.completed).length;
    const activeCount = totalCount - completedCount;

    // 1. Update Progress Header
    if (todoProgressBadge) {
      todoProgressBadge.textContent = `${completedCount} / ${totalCount} done`;
    }
    if (todoProgressFill) {
      const pct = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;
      todoProgressFill.style.width = `${pct}%`;
    }

    // 2. Update Filter Chip Counts
    if (filterAll) filterAll.textContent = `All (${totalCount})`;
    if (filterActive) filterActive.textContent = `Active (${activeCount})`;
    if (filterCompleted) filterCompleted.textContent = `Done (${completedCount})`;

    // 3. Clear Completed button visibility
    if (clearCompletedBtn) {
      clearCompletedBtn.classList.toggle('hidden', completedCount === 0);
    }

    // 4. Update Timer Card Active Focus Pill
    const focusedTask = tasks.find(t => String(t.id) === String(activeFocusedTaskId));
    if (focusedTask && timerActiveTaskPill && timerActiveTaskText) {
      timerActiveTaskPill.classList.remove('hidden');
      timerActiveTaskText.textContent = focusedTask.text;
    } else {
      if (timerActiveTaskPill) timerActiveTaskPill.classList.add('hidden');
      activeFocusedTaskId = null;
    }

    // 5. Render Filtered Tasks List
    if (!todoList) return;
    todoList.innerHTML = '';

    const filtered = tasks.filter(t => {
      if (currentTaskFilter === 'active') return !t.completed;
      if (currentTaskFilter === 'completed') return t.completed;
      return true;
    });

    if (filtered.length === 0) {
      let emptyMsg = 'No tasks yet. Add one above to get started!';
      if (currentTaskFilter === 'active' && totalCount > 0) emptyMsg = 'All tasks completed! Great work 🎉';
      if (currentTaskFilter === 'completed' && totalCount > 0) emptyMsg = 'No completed tasks yet.';
      todoList.innerHTML = `<div class="todo-empty">${emptyMsg}</div>`;
      return;
    }

    filtered.forEach(task => {
      const isFocused = String(task.id) === String(activeFocusedTaskId);
      const itemEl = document.createElement('div');
      itemEl.className = `todo-item ${task.completed ? 'completed' : ''} ${isFocused ? 'is-focused' : ''}`;
      itemEl.dataset.id = task.id;

      itemEl.innerHTML = `
        <div class="todo-left" title="Click to toggle done">
          <span class="todo-checkbox" aria-label="Toggle completed"></span>
          <span class="todo-text">${escapeHtml(task.text)}</span>
        </div>
        <div class="todo-actions">
          <button type="button" class="todo-action-btn todo-focus-btn" title="${isFocused ? 'Unlink from Timer' : 'Focus on this task with Timer'}">
            ${isFocused ? '🎯' : '🎯'}
          </button>
          <button type="button" class="todo-action-btn todo-delete-btn" title="Delete Task">✕</button>
        </div>
      `;

      // Checkbox click
      itemEl.querySelector('.todo-left').addEventListener('click', () => {
        task.completed = !task.completed;
        saveTasks();
        renderTasks();
      });

      // Focus button click (Sets task as the current focus timer intent)
      itemEl.querySelector('.todo-focus-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        if (String(activeFocusedTaskId) === String(task.id)) {
          activeFocusedTaskId = null;
        } else {
          activeFocusedTaskId = task.id;
        }
        saveTasks();
        renderTasks();
      });

      // Delete button click
      itemEl.querySelector('.todo-delete-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        tasks = tasks.filter(t => t.id !== task.id);
        if (String(activeFocusedTaskId) === String(task.id)) {
          activeFocusedTaskId = null;
        }
        saveTasks();
        renderTasks();
      });

      todoList.appendChild(itemEl);
    });
  }

  function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  // Add Task
  if (todoForm && todoInput) {
    todoForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const text = todoInput.value.trim();
      if (!text) return;

      const newTask = {
        id: Date.now(),
        text,
        completed: false,
        createdAt: Date.now()
      };

      tasks.unshift(newTask);

      // If no task is currently focused, automatically focus this first task
      if (!activeFocusedTaskId) {
        activeFocusedTaskId = newTask.id;
      }

      saveTasks();
      renderTasks();
      todoInput.value = '';
    });
  }

  // Filter Buttons
  filterChips.forEach(chip => {
    chip.addEventListener('click', () => {
      filterChips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      currentTaskFilter = chip.dataset.filter;
      renderTasks();
    });
  });

  // Clear Completed
  if (clearCompletedBtn) {
    clearCompletedBtn.addEventListener('click', () => {
      tasks = tasks.filter(t => !t.completed);
      saveTasks();
      renderTasks();
    });
  }

  // Unfocus button from timer pill
  if (unfocusTaskBtn) {
    unfocusTaskBtn.addEventListener('click', () => {
      activeFocusedTaskId = null;
      saveTasks();
      renderTasks();
    });
  }

  // --------------------------------------------------------------------------
  // 10. Fullscreen Toggle
  // --------------------------------------------------------------------------
  const fullscreenBtn = document.getElementById('fullscreenBtn');
  if (fullscreenBtn) {
    fullscreenBtn.addEventListener('click', () => {
      if (window.AndroidBridge && window.AndroidBridge.toggleFullscreen) {
        window.AndroidBridge.toggleFullscreen();
        return;
      }
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen().catch(() => {});
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen().catch(() => {});
        }
      }
    });
  }

  // --------------------------------------------------------------------------
  // Two Bottom Tabs (Home & History)
  // --------------------------------------------------------------------------
  const navTabHome = document.getElementById('navTabHome');
  const navTabHistory = document.getElementById('navTabHistory');
  const tabHomeView = document.getElementById('tabHomeView');
  const tabHistoryView = document.getElementById('tabHistoryView');

  function switchAppTab(targetTab) {
    if (targetTab === 'home') {
      if (navTabHome) navTabHome.classList.add('active');
      if (navTabHistory) navTabHistory.classList.remove('active');
      if (tabHomeView) {
        tabHomeView.classList.remove('hidden');
        tabHomeView.classList.add('active');
      }
      if (tabHistoryView) {
        tabHistoryView.classList.add('hidden');
        tabHistoryView.classList.remove('active');
      }
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
      if (navTabHome) navTabHome.classList.remove('active');
      if (navTabHistory) navTabHistory.classList.add('active');
      if (tabHomeView) {
        tabHomeView.classList.add('hidden');
        tabHomeView.classList.remove('active');
      }
      if (tabHistoryView) {
        tabHistoryView.classList.remove('hidden');
        tabHistoryView.classList.add('active');
      }
      renderDaywiseTracker();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  if (navTabHome) {
    navTabHome.addEventListener('click', () => switchAppTab('home'));
  }
  if (navTabHistory) {
    navTabHistory.addEventListener('click', () => switchAppTab('history'));
  }

  // --------------------------------------------------------------------------
  // 11. Initial Application Load
  // --------------------------------------------------------------------------
  updateTabLabels();
  renderDaywiseTracker();
  renderTasks();
  updateDisplay();
  updateNotificationUI();
  renderPlaylist();
});
