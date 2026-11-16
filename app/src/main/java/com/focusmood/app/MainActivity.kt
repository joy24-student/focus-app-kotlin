package com.focusmood.app

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.focusmood.app.dialogs.CustomTimerDialog
import com.focusmood.app.models.AudioTrack
import com.focusmood.app.models.DayRecord
import com.focusmood.app.models.Mood
import com.focusmood.app.models.Task
import com.focusmood.app.storage.FocusStorage
import com.focusmood.app.views.EqualizerBarView
import com.focusmood.app.views.FocusRingView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    enum class TimerMode(val title: String) {
        FOCUS("Deep Focus"),
        SHORT_BREAK("Short Break"),
        LONG_BREAK("Long Break")
    }

    private lateinit var storage: FocusStorage
    private var currentMood: Mood = Mood.FOREST
    private var isFullscreen = false

    // Navigation Tab (0: Home, 1: History)
    private var currentNavTab = 0

    // Timer State
    private var currentTimerMode = TimerMode.FOCUS
    private var customFocusMinutes = 25
    private var customShortMinutes = 5
    private var customLongMinutes = 15
    private var timeLeftSeconds = 25 * 60
    private var totalDurationSeconds = 25 * 60
    private var isTimerRunning = false
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    // Audio Player State
    private var mediaPlayer: MediaPlayer? = null
    private var playlist = mutableListOf<AudioTrack>()
    private var currentTrackId: String? = null
    private var isAudioSeeking = false
    private var loopMode = "all" // "all", "one", "off"
    private var currentVolume = 0.7f
    private var previousVolume = 0.7f
    private val audioProgressHandler = Handler(Looper.getMainLooper())
    private var audioProgressRunnable: Runnable? = null

    // Tasks & Filter
    private var tasks = mutableListOf<Task>()
    private var activeTaskId: Long? = null
    private var currentTaskFilter = "all" // "all", "active", "completed"

    // Views - Navigation
    private lateinit var appRoot: RelativeLayout
    private lateinit var viewHome: ScrollView
    private lateinit var viewHistory: ScrollView
    private lateinit var tabNavHome: LinearLayout
    private lateinit var tabNavHistory: LinearLayout
    private lateinit var iconNavHome: TextView
    private lateinit var iconNavHistory: TextView
    private lateinit var textNavHome: TextView
    private lateinit var textNavHistory: TextView

    // Views - Header
    private lateinit var logoDot: View
    private lateinit var logoText: TextView
    private lateinit var btnNotify: Button
    private lateinit var notifyBadgeDot: View
    private lateinit var btnFullscreen: Button

    // Views - Mood Chips
    private lateinit var chipForest: TextView
    private lateinit var chipRain: TextView
    private lateinit var chipSunset: TextView
    private lateinit var chipMidnight: TextView
    private lateinit var chipMinimal: TextView

    // Views - Timer
    private lateinit var tabFocus: TextView
    private lateinit var tabShort: TextView
    private lateinit var tabLong: TextView
    private lateinit var btnCustomTimer: Button
    private lateinit var focusRingView: FocusRingView
    private lateinit var pillActiveTask: LinearLayout
    private lateinit var tvActiveTaskName: TextView
    private lateinit var btnUnlinkTask: TextView
    private lateinit var btnMinus5: Button
    private lateinit var btnMinus1: Button
    private lateinit var btnPlus1: Button
    private lateinit var btnPlus5: Button
    private lateinit var btnStartPause: Button
    private lateinit var btnReset: Button
    private lateinit var btnSkip: Button

    // Views - Tasks
    private lateinit var tvTodoProgressBadge: TextView
    private lateinit var pbTodoProgress: ProgressBar
    private lateinit var etTaskInput: EditText
    private lateinit var btnAddTask: Button
    private lateinit var filterAll: TextView
    private lateinit var filterActive: TextView
    private lateinit var filterDone: TextView
    private lateinit var todoListContainer: LinearLayout
    private lateinit var btnClearCompleted: Button

    // Views - MP3 Player
    private lateinit var tvStorageUsage: TextView
    private lateinit var btnUploadMp3: Button
    private lateinit var equalizerView: EqualizerBarView
    private lateinit var tvNowPlayingTitle: TextView
    private lateinit var tvNowPlayingStatus: TextView
    private lateinit var tvAudioCurrentTime: TextView
    private lateinit var sbAudioProgress: SeekBar
    private lateinit var tvAudioTotalDuration: TextView
    private lateinit var btnPrevTrack: Button
    private lateinit var btnPlayPauseTrack: Button
    private lateinit var btnNextTrack: Button
    private lateinit var btnLoopTrack: Button
    private lateinit var btnMuteToggle: TextView
    private lateinit var sbVolume: SeekBar
    private lateinit var tvTrackCount: TextView
    private lateinit var btnClearAllTracks: Button
    private lateinit var playlistContainer: LinearLayout

    // Views - History Tab
    private lateinit var tvStreakBadge: TextView
    private lateinit var tvHistTodaySessions: TextView
    private lateinit var tvHistTodayMinutes: TextView
    private lateinit var tvHistAllTimeMinutes: TextView
    private lateinit var tvWeeklyTotal: TextView
    private lateinit var chartBarsContainer: LinearLayout
    private lateinit var historyListContainer: LinearLayout
    private lateinit var btnClearHistory: Button

    // Views - Quote
    private lateinit var tvMoodQuote: TextView

    companion object {
        private const val REQUEST_PICK_AUDIO = 1001
        private const val CHANNEL_ID = "focus_mood_alerts"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storage = FocusStorage(this)
        currentMood = storage.getSavedMood()
        val customDurations = storage.getCustomDurations()
        customFocusMinutes = customDurations.first
        customShortMinutes = customDurations.second
        customLongMinutes = customDurations.third
        timeLeftSeconds = customFocusMinutes * 60
        totalDurationSeconds = timeLeftSeconds
        loopMode = storage.getLoopMode()
        currentVolume = storage.getVolume()
        tasks = storage.getTasks()
        activeTaskId = storage.getActiveTaskId()
        playlist = storage.getTracks()
        currentTrackId = storage.getActiveTrackId()

        bindViews()
        createNotificationChannel()
        setupListeners()
        applyMoodTheme(currentMood)
        updateTimerTabsLabels()
        updateTimerDisplay()
        renderTasks()
        renderPlaylist()
        renderHistoryTab()
        updateAudioControlsUI()
    }

    private fun bindViews() {
        appRoot = findViewById(R.id.appRoot)
        viewHome = findViewById(R.id.viewHome)
        viewHistory = findViewById(R.id.viewHistory)
        tabNavHome = findViewById(R.id.tabNavHome)
        tabNavHistory = findViewById(R.id.tabNavHistory)
        iconNavHome = findViewById(R.id.iconNavHome)
        iconNavHistory = findViewById(R.id.iconNavHistory)
        textNavHome = findViewById(R.id.textNavHome)
        textNavHistory = findViewById(R.id.textNavHistory)

        logoDot = findViewById(R.id.logoDot)
        logoText = findViewById(R.id.logoText)
        btnNotify = findViewById(R.id.btnNotify)
        notifyBadgeDot = findViewById(R.id.notifyBadgeDot)
        btnFullscreen = findViewById(R.id.btnFullscreen)

        chipForest = findViewById(R.id.chipForest)
        chipRain = findViewById(R.id.chipRain)
        chipSunset = findViewById(R.id.chipSunset)
        chipMidnight = findViewById(R.id.chipMidnight)
        chipMinimal = findViewById(R.id.chipMinimal)

        tabFocus = findViewById(R.id.tabFocus)
        tabShort = findViewById(R.id.tabShort)
        tabLong = findViewById(R.id.tabLong)
        btnCustomTimer = findViewById(R.id.btnCustomTimer)
        focusRingView = findViewById(R.id.focusRingView)
        pillActiveTask = findViewById(R.id.pillActiveTask)
        tvActiveTaskName = findViewById(R.id.tvActiveTaskName)
        btnUnlinkTask = findViewById(R.id.btnUnlinkTask)
        btnMinus5 = findViewById(R.id.btnMinus5)
        btnMinus1 = findViewById(R.id.btnMinus1)
        btnPlus1 = findViewById(R.id.btnPlus1)
        btnPlus5 = findViewById(R.id.btnPlus5)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnReset = findViewById(R.id.btnReset)
        btnSkip = findViewById(R.id.btnSkip)

        tvTodoProgressBadge = findViewById(R.id.tvTodoProgressBadge)
        pbTodoProgress = findViewById(R.id.pbTodoProgress)
        etTaskInput = findViewById(R.id.etTaskInput)
        btnAddTask = findViewById(R.id.btnAddTask)
        filterAll = findViewById(R.id.filterAll)
        filterActive = findViewById(R.id.filterActive)
        filterDone = findViewById(R.id.filterDone)
        todoListContainer = findViewById(R.id.todoListContainer)
        btnClearCompleted = findViewById(R.id.btnClearCompleted)

        tvStorageUsage = findViewById(R.id.tvStorageUsage)
        btnUploadMp3 = findViewById(R.id.btnUploadMp3)
        equalizerView = findViewById(R.id.equalizerView)
        tvNowPlayingTitle = findViewById(R.id.tvNowPlayingTitle)
        tvNowPlayingStatus = findViewById(R.id.tvNowPlayingStatus)
        tvAudioCurrentTime = findViewById(R.id.tvAudioCurrentTime)
        sbAudioProgress = findViewById(R.id.sbAudioProgress)
        tvAudioTotalDuration = findViewById(R.id.tvAudioTotalDuration)
        btnPrevTrack = findViewById(R.id.btnPrevTrack)
        btnPlayPauseTrack = findViewById(R.id.btnPlayPauseTrack)
        btnNextTrack = findViewById(R.id.btnNextTrack)
        btnLoopTrack = findViewById(R.id.btnLoopTrack)
        btnMuteToggle = findViewById(R.id.btnMuteToggle)
        sbVolume = findViewById(R.id.sbVolume)
        tvTrackCount = findViewById(R.id.tvTrackCount)
        btnClearAllTracks = findViewById(R.id.btnClearAllTracks)
        playlistContainer = findViewById(R.id.playlistContainer)

        tvStreakBadge = findViewById(R.id.tvStreakBadge)
        tvHistTodaySessions = findViewById(R.id.tvHistTodaySessions)
        tvHistTodayMinutes = findViewById(R.id.tvHistTodayMinutes)
        tvHistAllTimeMinutes = findViewById(R.id.tvHistAllTimeMinutes)
        tvWeeklyTotal = findViewById(R.id.tvWeeklyTotal)
        chartBarsContainer = findViewById(R.id.chartBarsContainer)
        historyListContainer = findViewById(R.id.historyListContainer)
        btnClearHistory = findViewById(R.id.btnClearHistory)

        tvMoodQuote = findViewById(R.id.tvMoodQuote)
    }

    private fun setupListeners() {
        // Bottom Navigation Tabs
        tabNavHome.setOnClickListener { switchNavTab(0) }
        tabNavHistory.setOnClickListener { switchNavTab(1) }

        // Mood Chips
        chipForest.setOnClickListener { applyMoodTheme(Mood.FOREST) }
        chipRain.setOnClickListener { applyMoodTheme(Mood.RAIN) }
        chipSunset.setOnClickListener { applyMoodTheme(Mood.SUNSET) }
        chipMidnight.setOnClickListener { applyMoodTheme(Mood.MIDNIGHT) }
        chipMinimal.setOnClickListener { applyMoodTheme(Mood.MINIMAL) }

        // Fullscreen & Notification
        btnFullscreen.setOnClickListener { toggleFullscreen() }
        btnNotify.setOnClickListener { triggerTestNotification() }

        // Timer Modes
        tabFocus.setOnClickListener { switchTimerMode(TimerMode.FOCUS) }
        tabShort.setOnClickListener { switchTimerMode(TimerMode.SHORT_BREAK) }
        tabLong.setOnClickListener { switchTimerMode(TimerMode.LONG_BREAK) }

        // Custom Timer Settings Dialog
        val openDialog = {
            CustomTimerDialog(this, customFocusMinutes, customShortMinutes, customLongMinutes) { f, s, l ->
                customFocusMinutes = f
                customShortMinutes = s
                customLongMinutes = l
                storage.saveCustomDurations(f, s, l)
                updateTimerTabsLabels()
                switchTimerMode(currentTimerMode)
            }.show()
        }
        btnCustomTimer.setOnClickListener { openDialog() }
        focusRingView.setOnClickListener {
            if (!isTimerRunning) openDialog()
        }

        // Quick Minute Adjusters
        btnMinus5.setOnClickListener { adjustTimer(-5) }
        btnMinus1.setOnClickListener { adjustTimer(-1) }
        btnPlus1.setOnClickListener { adjustTimer(1) }
        btnPlus5.setOnClickListener { adjustTimer(5) }

        // Primary Timer Controls
        btnStartPause.setOnClickListener {
            if (isTimerRunning) pauseTimer() else startTimer()
        }
        btnReset.setOnClickListener { resetTimer() }
        btnSkip.setOnClickListener { skipTimer() }

        // Unlink Active Task
        btnUnlinkTask.setOnClickListener {
            activeTaskId = null
            storage.saveActiveTaskId(null)
            renderTasks()
        }

        // Add Task
        btnAddTask.setOnClickListener { addTaskFromInput() }
        etTaskInput.setOnEditorActionListener { _, _, _ ->
            addTaskFromInput()
            true
        }

        // Filter Chips
        filterAll.setOnClickListener { setTaskFilter("all") }
        filterActive.setOnClickListener { setTaskFilter("active") }
        filterDone.setOnClickListener { setTaskFilter("completed") }
        btnClearCompleted.setOnClickListener {
            tasks.removeAll { it.completed }
            storage.saveTasks(tasks)
            renderTasks()
        }

        // Audio Player Controls
        btnUploadMp3.setOnClickListener { openAudioPicker() }
        btnPlayPauseTrack.setOnClickListener { toggleAudioPlayback() }
        btnPrevTrack.setOnClickListener { playPreviousTrack() }
        btnNextTrack.setOnClickListener { playNextTrack() }
        btnLoopTrack.setOnClickListener { toggleLoopMode() }
        btnClearAllTracks.setOnClickListener { clearAllTracks() }

        btnMuteToggle.setOnClickListener {
            if (currentVolume > 0f) {
                previousVolume = currentVolume
                setAudioVolume(0f)
            } else {
                setAudioVolume(if (previousVolume > 0f) previousVolume else 0.7f)
            }
        }

        sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) setAudioVolume(progress / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbAudioProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) {
                    val duration = mediaPlayer!!.duration
                    val seekTo = (progress.toFloat() / 1000f * duration).toInt()
                    tvAudioCurrentTime.text = formatTime(seekTo / 1000)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { isAudioSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (mediaPlayer != null) {
                    val duration = mediaPlayer!!.duration
                    val seekTo = (sbAudioProgress.progress.toFloat() / 1000f * duration).toInt()
                    mediaPlayer?.seekTo(seekTo)
                }
                isAudioSeeking = false
            }
        })

        // History Tab Clear
        btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear your daywise focus history?")
                .setPositiveButton("Clear") { _, _ ->
                    storage.clearHistory()
                    renderHistoryTab()
                    Toast.makeText(this, "Daywise history cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // =========================================================================
    // 1. Navigation Tabs (Home & History)
    // =========================================================================
    private fun switchNavTab(tabIndex: Int) {
        currentNavTab = tabIndex
        if (tabIndex == 0) {
            viewHome.visibility = View.VISIBLE
            viewHistory.visibility = View.GONE
            textNavHome.setTextColor(currentMood.primaryColor)
            textNavHistory.setTextColor(Color.parseColor("#888888"))
        } else {
            viewHome.visibility = View.GONE
            viewHistory.visibility = View.VISIBLE
            textNavHome.setTextColor(Color.parseColor("#888888"))
            textNavHistory.setTextColor(currentMood.primaryColor)
            renderHistoryTab()
        }
    }

    // =========================================================================
    // 2. Mood Themes
    // =========================================================================
    private fun applyMoodTheme(mood: Mood) {
        currentMood = mood
        storage.saveMood(mood)

        appRoot.setBackgroundColor(mood.bgColor)
        window.statusBarColor = mood.bgColor
        window.navigationBarColor = mood.bgColor

        focusRingView.updateThemeColors(mood.primaryColor, mood.textColor, mood.mutedColor)
        equalizerView.setBarColor(mood.primaryColor)
        tvMoodQuote.text = mood.quote
        tvMoodQuote.setTextColor(mood.subColor)

        // Mood chips visual active state
        val chips = listOf(chipForest, chipRain, chipSunset, chipMidnight, chipMinimal)
        chips.forEach { chip ->
            val isActive = chip.text.toString().contains(mood.displayName.split(" ").last(), ignoreCase = true)
            chip.setBackgroundResource(if (isActive) R.drawable.chip_active else R.drawable.chip_background)
            chip.setTextColor(if (isActive) mood.textColor else Color.parseColor("#aaaaaa"))
        }

        if (currentNavTab == 0) {
            textNavHome.setTextColor(mood.primaryColor)
        } else {
            textNavHistory.setTextColor(mood.primaryColor)
        }

        updateTimerDisplay()
    }

    // =========================================================================
    // 3. Pomodoro Timer Mechanics
    // =========================================================================
    private fun updateTimerTabsLabels() {
        tabFocus.text = "Focus (${customFocusMinutes}m)"
        tabShort.text = "Short (${customShortMinutes}m)"
        tabLong.text = "Long (${customLongMinutes}m)"
    }

    private fun switchTimerMode(mode: TimerMode) {
        pauseTimer()
        currentTimerMode = mode
        totalDurationSeconds = when (mode) {
            TimerMode.FOCUS -> customFocusMinutes * 60
            TimerMode.SHORT_BREAK -> customShortMinutes * 60
            TimerMode.LONG_BREAK -> customLongMinutes * 60
        }
        timeLeftSeconds = totalDurationSeconds

        tabFocus.setBackgroundResource(if (mode == TimerMode.FOCUS) R.drawable.chip_active else R.drawable.item_background)
        tabShort.setBackgroundResource(if (mode == TimerMode.SHORT_BREAK) R.drawable.chip_active else R.drawable.item_background)
        tabLong.setBackgroundResource(if (mode == TimerMode.LONG_BREAK) R.drawable.chip_active else R.drawable.item_background)

        updateTimerDisplay()
    }

    private fun startTimer() {
        if (isTimerRunning) return
        isTimerRunning = true
        btnStartPause.text = "⏸ Pause"

        timerRunnable = object : Runnable {
            override fun run() {
                if (timeLeftSeconds > 0) {
                    timeLeftSeconds--
                    updateTimerDisplay()
                    timerHandler.postDelayed(this, 1000)
                } else {
                    handleTimerComplete()
                }
            }
        }
        timerHandler.postDelayed(timerRunnable!!, 1000)
        updateTimerDisplay()
    }

    private fun pauseTimer() {
        isTimerRunning = false
        btnStartPause.text = "▶ Start"
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
        updateTimerDisplay()
    }

    private fun resetTimer() {
        pauseTimer()
        timeLeftSeconds = totalDurationSeconds
        updateTimerDisplay()
    }

    private fun skipTimer() {
        AlertDialog.Builder(this)
            .setTitle("Skip Session")
            .setMessage("Skip to the next session?")
            .setPositiveButton("Skip") { _, _ ->
                if (currentTimerMode == TimerMode.FOCUS) {
                    switchTimerMode(TimerMode.SHORT_BREAK)
                } else {
                    switchTimerMode(TimerMode.FOCUS)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun adjustTimer(deltaMinutes: Int) {
        val deltaSecs = deltaMinutes * 60
        val newTime = timeLeftSeconds + deltaSecs
        if (newTime in 10..(180 * 60)) {
            timeLeftSeconds = newTime
            if (!isTimerRunning) {
                totalDurationSeconds = Math.max(totalDurationSeconds, timeLeftSeconds)
            } else {
                totalDurationSeconds = Math.max(totalDurationSeconds, totalDurationSeconds + deltaSecs)
            }
            updateTimerDisplay()
        }
    }

    private fun handleTimerComplete() {
        pauseTimer()
        val isFocus = (currentTimerMode == TimerMode.FOCUS)
        val title = if (isFocus) "🎉 Focus Session Complete!" else "⚡ Break Finished!"

        val focusedTask = tasks.firstOrNull { it.id == activeTaskId }
        val body = if (isFocus && focusedTask != null) {
            "Completed focus session on: \"${focusedTask.text}\"! Take a break."
        } else if (isFocus) {
            "Great job! Time for a well-deserved break."
        } else {
            "Break finished! Ready to get back into focus?"
        }

        sendNotification(title, body)
        Toast.makeText(this, "$title $body", Toast.LENGTH_LONG).show()

        if (isFocus) {
            val mins = Math.round(totalDurationSeconds / 60f)
            storage.recordDaywiseSession(mins)
            switchTimerMode(TimerMode.SHORT_BREAK)
        } else {
            switchTimerMode(TimerMode.FOCUS)
        }
    }

    private fun updateTimerDisplay() {
        val formattedTime = formatTime(timeLeftSeconds)
        val status = if (isTimerRunning) {
            "Focusing..."
        } else if (timeLeftSeconds < totalDurationSeconds) {
            "Paused"
        } else {
            "Ready to ${currentTimerMode.title.lowercase()}"
        }

        val progress = if (totalDurationSeconds > 0) {
            (totalDurationSeconds - timeLeftSeconds).toFloat() / totalDurationSeconds.toFloat()
        } else 0f

        focusRingView.setProgress(progress, formattedTime, status)

        // Active task pill
        val activeTask = tasks.firstOrNull { it.id == activeTaskId }
        if (activeTask != null) {
            pillActiveTask.visibility = View.VISIBLE
            tvActiveTaskName.text = activeTask.text
        } else {
            pillActiveTask.visibility = View.GONE
        }
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    // =========================================================================
    // 4. To-Do Tasks
    // =========================================================================
    private fun addTaskFromInput() {
        val text = etTaskInput.text.toString().trim()
        if (text.isEmpty()) return

        val task = Task(id = System.currentTimeMillis(), text = text, completed = false)
        tasks.add(0, task)
        if (activeTaskId == null) {
            activeTaskId = task.id
            storage.saveActiveTaskId(activeTaskId)
        }
        storage.saveTasks(tasks)
        etTaskInput.text.clear()
        renderTasks()
    }

    private fun setTaskFilter(filter: String) {
        currentTaskFilter = filter
        filterAll.setBackgroundResource(if (filter == "all") R.drawable.chip_active else R.drawable.chip_background)
        filterActive.setBackgroundResource(if (filter == "active") R.drawable.chip_active else R.drawable.chip_background)
        filterDone.setBackgroundResource(if (filter == "completed") R.drawable.chip_active else R.drawable.chip_background)
        renderTasks()
    }

    private fun renderTasks() {
        val total = tasks.size
        val doneCount = tasks.count { it.completed }
        val activeCount = total - doneCount

        tvTodoProgressBadge.text = "$doneCount / $total done"
        val pct = if (total > 0) ((doneCount.toFloat() / total.toFloat()) * 100).toInt() else 0
        pbTodoProgress.progress = pct

        filterAll.text = "All ($total)"
        filterActive.text = "Active ($activeCount)"
        filterDone.text = "Done ($doneCount)"
        btnClearCompleted.visibility = if (doneCount > 0) View.VISIBLE else View.GONE

        todoListContainer.removeAllViews()

        val filtered = tasks.filter {
            when (currentTaskFilter) {
                "active" -> !it.completed
                "completed" -> it.completed
                else -> true
            }
        }

        if (filtered.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = if (total == 0) "No tasks yet. Add one above to get started!" else "No tasks in this filter."
                setTextColor(Color.parseColor("#888888"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 24, 0, 24)
            }
            todoListContainer.addView(emptyTv)
        } else {
            val inflater = LayoutInflater.from(this)
            filtered.forEach { task ->
                val itemView = inflater.inflate(R.layout.item_task, todoListContainer, false)
                val tvCheck = itemView.findViewById<TextView>(R.id.tvTaskCheck)
                val tvText = itemView.findViewById<TextView>(R.id.tvTaskText)
                val btnFocus = itemView.findViewById<TextView>(R.id.btnTaskFocus)
                val btnDelete = itemView.findViewById<TextView>(R.id.btnTaskDelete)

                tvText.text = task.text
                if (task.completed) {
                    tvCheck.text = "✓"
                    tvCheck.setBackgroundResource(R.drawable.chip_active)
                    tvText.paintFlags = tvText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    tvText.setTextColor(Color.parseColor("#777777"))
                } else {
                    tvCheck.text = ""
                    tvCheck.setBackgroundResource(R.drawable.chip_background)
                    tvText.paintFlags = tvText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    tvText.setTextColor(currentMood.textColor)
                }

                val isFocused = (task.id == activeTaskId)
                btnFocus.setBackgroundResource(if (isFocused) R.drawable.badge_background else 0)

                tvCheck.setOnClickListener {
                    task.completed = !task.completed
                    storage.saveTasks(tasks)
                    renderTasks()
                }

                btnFocus.setOnClickListener {
                    activeTaskId = if (activeTaskId == task.id) null else task.id
                    storage.saveActiveTaskId(activeTaskId)
                    renderTasks()
                    updateTimerDisplay()
                }

                btnDelete.setOnClickListener {
                    tasks.remove(task)
                    if (activeTaskId == task.id) {
                        activeTaskId = null
                        storage.saveActiveTaskId(null)
                    }
                    storage.saveTasks(tasks)
                    renderTasks()
                    updateTimerDisplay()
                }

                todoListContainer.addView(itemView)
            }
        }
        updateTimerDisplay()
    }

    // =========================================================================
    // 5. Custom MP3 Audio Player
    // =========================================================================
    private fun openAudioPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(Intent.createChooser(intent, "Select Audio Files"), REQUEST_PICK_AUDIO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_AUDIO && resultCode == RESULT_OK) {
            val audioDir = File(filesDir, "audio").apply { mkdirs() }

            fun processUri(uri: Uri) {
                var fileName = "track_${System.currentTimeMillis()}.mp3"
                var fileSizeStr = "Unknown size"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) fileName = cursor.getString(nameIdx)
                        if (sizeIdx != -1) {
                            val bytes = cursor.getLong(sizeIdx)
                            fileSizeStr = formatBytes(bytes)
                        }
                    }
                }

                val destFile = File(audioDir, "${System.currentTimeMillis()}_$fileName")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val track = AudioTrack(
                    id = "track_${System.currentTimeMillis()}_${(100..999).random()}",
                    name = fileName,
                    size = fileSizeStr,
                    localPath = destFile.absolutePath
                )
                playlist.add(track)
                if (currentTrackId == null) {
                    currentTrackId = track.id
                    storage.saveActiveTrackId(currentTrackId)
                }
            }

            data?.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    processUri(clip.getItemAt(i).uri)
                }
            } ?: data?.data?.let { uri ->
                processUri(uri)
            }

            storage.saveTracks(playlist)
            renderPlaylist()
            Toast.makeText(this, "Uploaded audio saved for offline playback!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderPlaylist() {
        tvTrackCount.text = "Your Songs (${playlist.size})"
        btnClearAllTracks.visibility = if (playlist.isNotEmpty()) View.VISIBLE else View.GONE

        var totalBytes = 0L
        playlist.forEach { track ->
            val f = File(track.localPath)
            if (f.exists()) totalBytes += f.length()
        }
        tvStorageUsage.text = formatBytes(totalBytes)

        val activeTrack = playlist.firstOrNull { it.id == currentTrackId } ?: playlist.firstOrNull()
        if (activeTrack != null) {
            tvNowPlayingTitle.text = activeTrack.name
            val isPlaying = mediaPlayer?.isPlaying == true
            tvNowPlayingStatus.text = if (isPlaying) "Playing" else "${activeTrack.size} • Ready"
        } else {
            tvNowPlayingTitle.text = "No song selected"
            tvNowPlayingStatus.text = "Upload or select a track below"
        }

        playlistContainer.removeAllViews()
        if (playlist.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No custom songs uploaded yet.\nTap '+ Upload MP3' above to focus with your music."
                setTextColor(Color.parseColor("#888888"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 24, 0, 24)
            }
            playlistContainer.addView(emptyTv)
            return
        }

        val inflater = LayoutInflater.from(this)
        playlist.forEach { track ->
            val itemView = inflater.inflate(R.layout.item_playlist_track, playlistContainer, false)
            val tvIcon = itemView.findViewById<TextView>(R.id.tvTrackIcon)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvTrackTitle)
            val tvSize = itemView.findViewById<TextView>(R.id.tvTrackSize)
            val btnPlay = itemView.findViewById<TextView>(R.id.btnTrackPlay)
            val btnDel = itemView.findViewById<TextView>(R.id.btnTrackDelete)

            val isActive = (track.id == currentTrackId)
            val isPlaying = isActive && (mediaPlayer?.isPlaying == true)

            tvTitle.text = track.name
            tvSize.text = track.size
            tvIcon.text = if (isPlaying) "🔊" else "🎵"
            btnPlay.text = if (isPlaying) "⏸" else "▶"
            itemView.setBackgroundResource(if (isActive) R.drawable.chip_active else R.drawable.item_background)

            itemView.setOnClickListener {
                if (isActive) {
                    toggleAudioPlayback()
                } else {
                    playTrack(track)
                }
            }

            btnDel.setOnClickListener {
                if (currentTrackId == track.id) {
                    stopAudio()
                }
                File(track.localPath).delete()
                playlist.remove(track)
                if (currentTrackId == track.id) {
                    currentTrackId = playlist.firstOrNull()?.id
                    storage.saveActiveTrackId(currentTrackId)
                }
                storage.saveTracks(playlist)
                renderPlaylist()
            }

            playlistContainer.addView(itemView)
        }
    }

    private fun playTrack(track: AudioTrack) {
        currentTrackId = track.id
        storage.saveActiveTrackId(currentTrackId)

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(track.localPath)
                prepare()
                setVolume(currentVolume, currentVolume)
                setOnCompletionListener { onAudioTrackCompleted() }
                start()
            }
            startAudioProgressTracking()
            updateAudioControlsUI()
            renderPlaylist()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not play audio track: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleAudioPlayback() {
        if (playlist.isEmpty()) {
            openAudioPicker()
            return
        }

        val activeTrack = playlist.firstOrNull { it.id == currentTrackId } ?: playlist.first()
        if (mediaPlayer == null) {
            playTrack(activeTrack)
        } else if (mediaPlayer!!.isPlaying) {
            mediaPlayer?.pause()
            updateAudioControlsUI()
            renderPlaylist()
        } else {
            mediaPlayer?.start()
            startAudioProgressTracking()
            updateAudioControlsUI()
            renderPlaylist()
        }
    }

    private fun playPreviousTrack() {
        if (playlist.isEmpty()) return
        val idx = playlist.indexOfFirst { it.id == currentTrackId }
        val prevIdx = if (idx > 0) idx - 1 else playlist.size - 1
        playTrack(playlist[prevIdx])
    }

    private fun playNextTrack() {
        if (playlist.isEmpty()) return
        val idx = playlist.indexOfFirst { it.id == currentTrackId }
        val nextIdx = if (idx < playlist.size - 1) idx + 1 else 0
        playTrack(playlist[nextIdx])
    }

    private fun onAudioTrackCompleted() {
        when (loopMode) {
            "one" -> {
                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()
            }
            "all" -> playNextTrack()
            else -> {
                val idx = playlist.indexOfFirst { it.id == currentTrackId }
                if (idx < playlist.size - 1) playNextTrack() else stopAudio()
            }
        }
    }

    private fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        sbAudioProgress.progress = 0
        tvAudioCurrentTime.text = "0:00"
        updateAudioControlsUI()
        renderPlaylist()
    }

    private fun toggleLoopMode() {
        loopMode = when (loopMode) {
            "all" -> "one"
            "one" -> "off"
            else -> "all"
        }
        storage.saveLoopMode(loopMode)
        updateAudioControlsUI()
        val desc = when (loopMode) {
            "all" -> "Repeat: All Songs"
            "one" -> "Repeat: Current Song"
            else -> "Repeat: Off"
        }
        Toast.makeText(this, desc, Toast.LENGTH_SHORT).show()
    }

    private fun setAudioVolume(vol: Float) {
        currentVolume = vol.coerceIn(0f, 1f)
        storage.saveVolume(currentVolume)
        mediaPlayer?.setVolume(currentVolume, currentVolume)
        sbVolume.progress = (currentVolume * 100).toInt()
        btnMuteToggle.text = if (currentVolume == 0f) "🔇" else if (currentVolume < 0.5f) "🔉" else "🔊"
    }

    private fun startAudioProgressTracking() {
        audioProgressRunnable?.let { audioProgressHandler.removeCallbacks(it) }
        audioProgressRunnable = object : Runnable {
            override fun run() {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying && !isAudioSeeking) {
                    val current = mp.currentPosition
                    val duration = mp.duration
                    if (duration > 0) {
                        val pct = ((current.toFloat() / duration.toFloat()) * 1000).toInt()
                        sbAudioProgress.progress = pct
                        tvAudioCurrentTime.text = formatTime(current / 1000)
                        tvAudioTotalDuration.text = formatTime(duration / 1000)
                    }
                }
                audioProgressHandler.postDelayed(this, 500)
            }
        }
        audioProgressHandler.post(audioProgressRunnable!!)
    }

    private fun updateAudioControlsUI() {
        val isPlaying = mediaPlayer?.isPlaying == true
        btnPlayPauseTrack.text = if (isPlaying) "⏸" else "▶"
        equalizerView.setPlaying(isPlaying)

        btnLoopTrack.text = when (loopMode) {
            "one" -> "🔂"
            "off" -> "➡️"
            else -> "🔁"
        }
    }

    private fun clearAllTracks() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Songs")
            .setMessage("Are you sure you want to remove all uploaded songs?")
            .setPositiveButton("Remove") { _, _ ->
                stopAudio()
                playlist.forEach { File(it.localPath).delete() }
                playlist.clear()
                currentTrackId = null
                storage.saveTracks(playlist)
                storage.saveActiveTrackId(null)
                renderPlaylist()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb) else String.format(Locale.US, "%.0f KB", kb)
    }

    // =========================================================================
    // 6. History Tab & Daywise Tracker
    // =========================================================================
    private fun renderHistoryTab() {
        val history = storage.getDaywiseHistory()
        val todayKey = FocusStorage.getLocalDateKey()
        val todayRecord = history[todayKey] ?: DayRecord(todayKey, 0, 0)
        val allTimeMins = storage.getAllTimeMinutes()
        val streak = storage.calculateStreak()

        tvStreakBadge.text = "🔥 $streak Day${if (streak == 1) "" else "s"} Streak"
        tvHistTodaySessions.text = todayRecord.sessions.toString()
        tvHistTodayMinutes.text = "${todayRecord.minutes}m"
        tvHistAllTimeMinutes.text = "${allTimeMins}m"

        // 7-day mini bar chart
        chartBarsContainer.removeAllViews()
        var weeklyTotal = 0
        var maxMins = 30
        val cal = Calendar.getInstance()
        val past7Days = mutableListOf<DayRecord>()

        for (i in 6 downTo 0) {
            val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val key = FocusStorage.getLocalDateKey(d.time)
            val rec = history[key] ?: DayRecord(key, 0, 0)
            past7Days.add(rec)
            weeklyTotal += rec.minutes
            if (rec.minutes > maxMins) maxMins = rec.minutes
        }
        tvWeeklyTotal.text = "Week: ${weeklyTotal}m"

        val sdfDayName = SimpleDateFormat("EEE", Locale.US)
        past7Days.forEach { rec ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }

            val barWrap = LinearLayout(this).apply {
                gravity = Gravity.BOTTOM
                layoutParams = LinearLayout.LayoutParams(24, 0, 1f)
            }

            val heightFraction = if (rec.minutes > 0) Math.max(0.12f, rec.minutes.toFloat() / maxMins.toFloat()) else 0.05f
            val isToday = (rec.dateKey == todayKey)

            val fillView = View(this).apply {
                setBackgroundColor(if (isToday) currentMood.primaryColor else Color.parseColor("#44ffffff"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (heightFraction * 180).toInt()
                )
            }
            barWrap.addView(fillView)
            col.addView(barWrap)

            val label = TextView(this).apply {
                val dObj = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(rec.dateKey)
                text = if (dObj != null) sdfDayName.format(dObj) else rec.dateKey.takeLast(2)
                textSize = 10f
                setTextColor(if (isToday) currentMood.textColor else Color.parseColor("#888888"))
                setPadding(0, 6, 0, 0)
            }
            col.addView(label)
            chartBarsContainer.addView(col)
        }

        // Daywise History Log List
        historyListContainer.removeAllViews()
        val sortedKeys = history.keys.sortedDescending()
        if (sortedKeys.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No focus sessions recorded yet. Start your first session!"
                setTextColor(Color.parseColor("#888888"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 20, 0, 20)
            }
            historyListContainer.addView(emptyTv)
        } else {
            val inflater = LayoutInflater.from(this)
            sortedKeys.forEach { key ->
                val rec = history[key] ?: return@forEach
                val row = inflater.inflate(R.layout.item_history_day, historyListContainer, false)
                val tvDate = row.findViewById<TextView>(R.id.tvHistoryDate)
                val tvStats = row.findViewById<TextView>(R.id.tvHistoryStats)

                tvDate.text = if (key == todayKey) "Today (Now)" else key
                tvStats.text = "${rec.minutes}m (${rec.sessions} session${if (rec.sessions == 1) "" else "s"})"
                historyListContainer.addView(row)
            }
        }
    }

    // =========================================================================
    // 7. System Notifications
    // =========================================================================
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Mood Timer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when your focus and break sessions finish"
                enableVibration(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        notifyBadgeDot.visibility = View.VISIBLE
    }

    private fun sendNotification(title: String, body: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(this)
        }

        builder.setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher)
            .setAutoCancel(true)

        nm.notify(101, builder.build())
    }

    private fun triggerTestNotification() {
        sendNotification("🔔 Focus Mood Notifications", "Browser and system notifications are working properly!")
        Toast.makeText(this, "Notifications active! Sending test alert...", Toast.LENGTH_SHORT).show()
    }

    // =========================================================================
    // 8. Fullscreen Toggle
    // =========================================================================
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
            Toast.makeText(this, "Fullscreen Mode", Toast.LENGTH_SHORT).show()
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pauseTimer()
        mediaPlayer?.release()
        mediaPlayer = null
        audioProgressRunnable?.let { audioProgressHandler.removeCallbacks(it) }
    }
}
