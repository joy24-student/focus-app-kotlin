package com.focusmood.app.storage

import android.content.Context
import android.content.SharedPreferences
import com.focusmood.app.models.AudioTrack
import com.focusmood.app.models.DayRecord
import com.focusmood.app.models.Mood
import com.focusmood.app.models.Task
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FocusStorage(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_mood_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SAVED_MOOD = "fm_saved_mood"
        private const val KEY_CUSTOM_FOCUS = "fm_custom_focus"
        private const val KEY_CUSTOM_SHORT = "fm_custom_short"
        private const val KEY_CUSTOM_LONG = "fm_custom_long"
        private const val KEY_ALL_TIME_MINUTES = "fm_focus_minutes"
        private const val KEY_ACTIVE_TASK_ID = "fm_focused_task_id"
        private const val KEY_TODO_TASKS = "fm_todo_tasks"
        private const val KEY_DAYWISE_HISTORY = "fm_daywise_history"
        private const val KEY_CUSTOM_TRACKS = "fm_custom_tracks"
        private const val KEY_ACTIVE_TRACK_ID = "fm_active_track_id"
        private const val KEY_LOOP_MODE = "fm_loop_mode"
        private const val KEY_AUDIO_VOLUME = "fm_audio_volume"

        fun getLocalDateKey(date: Date = Date()): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(date)
        }
    }

    // Mood
    fun getSavedMood(): Mood {
        val key = prefs.getString(KEY_SAVED_MOOD, Mood.FOREST.key)
        return Mood.fromKey(key)
    }

    fun saveMood(mood: Mood) {
        prefs.edit().putString(KEY_SAVED_MOOD, mood.key).apply()
    }

    // Custom Durations (in minutes)
    fun getCustomDurations(): Triple<Int, Int, Int> {
        val focus = prefs.getInt(KEY_CUSTOM_FOCUS, 25)
        val short = prefs.getInt(KEY_CUSTOM_SHORT, 5)
        val long = prefs.getInt(KEY_CUSTOM_LONG, 15)
        return Triple(focus, short, long)
    }

    fun saveCustomDurations(focus: Int, short: Int, long: Int) {
        prefs.edit()
            .putInt(KEY_CUSTOM_FOCUS, focus)
            .putInt(KEY_CUSTOM_SHORT, short)
            .putInt(KEY_CUSTOM_LONG, long)
            .apply()
    }

    // All-time minutes
    fun getAllTimeMinutes(): Int {
        return prefs.getInt(KEY_ALL_TIME_MINUTES, 0)
    }

    fun addAllTimeMinutes(mins: Int) {
        val updated = getAllTimeMinutes() + mins
        prefs.edit().putInt(KEY_ALL_TIME_MINUTES, updated).apply()
    }

    // Focused Task
    fun getActiveTaskId(): Long? {
        val id = prefs.getLong(KEY_ACTIVE_TASK_ID, -1L)
        return if (id != -1L) id else null
    }

    fun saveActiveTaskId(id: Long?) {
        if (id != null) {
            prefs.edit().putLong(KEY_ACTIVE_TASK_ID, id).apply()
        } else {
            prefs.edit().remove(KEY_ACTIVE_TASK_ID).apply()
        }
    }

    // Tasks List
    fun getTasks(): MutableList<Task> {
        val raw = prefs.getString(KEY_TODO_TASKS, null) ?: return mutableListOf()
        val list = mutableListOf<Task>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                list.add(Task.fromJson(array.getJSONObject(i)))
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveTasks(tasks: List<Task>) {
        val array = JSONArray()
        tasks.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_TODO_TASKS, array.toString()).apply()
    }

    // Daywise History
    fun getDaywiseHistory(): MutableMap<String, DayRecord> {
        val raw = prefs.getString(KEY_DAYWISE_HISTORY, null) ?: return mutableMapOf()
        val map = mutableMapOf<String, DayRecord>()
        try {
            val json = JSONObject(raw)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val obj = json.getJSONObject(k)
                map[k] = DayRecord.fromJson(k, obj)
            }
        } catch (_: Exception) {}
        return map
    }

    fun saveDaywiseHistory(map: Map<String, DayRecord>) {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v.toJson()) }
        prefs.edit().putString(KEY_DAYWISE_HISTORY, json.toString()).apply()
    }

    fun recordDaywiseSession(minutes: Int) {
        val history = getDaywiseHistory()
        val todayKey = getLocalDateKey()
        val record = history[todayKey] ?: DayRecord(todayKey, 0, 0)
        record.minutes += minutes
        record.sessions += 1
        history[todayKey] = record
        saveDaywiseHistory(history)
        addAllTimeMinutes(minutes)
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_DAYWISE_HISTORY).apply()
    }

    fun calculateStreak(): Int {
        val history = getDaywiseHistory()
        val cal = Calendar.getInstance()
        val todayKey = getLocalDateKey(cal.time)

        val hasToday = (history[todayKey]?.minutes ?: 0) > 0
        if (!hasToday) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        var streak = 0
        while (true) {
            val key = getLocalDateKey(cal.time)
            val dayData = history[key]
            if (dayData != null && dayData.minutes > 0) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    // Audio Tracks
    fun getTracks(): MutableList<AudioTrack> {
        val raw = prefs.getString(KEY_CUSTOM_TRACKS, null) ?: return mutableListOf()
        val list = mutableListOf<AudioTrack>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                list.add(AudioTrack.fromJson(array.getJSONObject(i)))
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveTracks(tracks: List<AudioTrack>) {
        val array = JSONArray()
        tracks.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_CUSTOM_TRACKS, array.toString()).apply()
    }

    fun getActiveTrackId(): String? {
        return prefs.getString(KEY_ACTIVE_TRACK_ID, null)
    }

    fun saveActiveTrackId(id: String?) {
        if (id != null) {
            prefs.edit().putString(KEY_ACTIVE_TRACK_ID, id).apply()
        } else {
            prefs.edit().remove(KEY_ACTIVE_TRACK_ID).apply()
        }
    }

    fun getLoopMode(): String {
        return prefs.getString(KEY_LOOP_MODE, "all") ?: "all"
    }

    fun saveLoopMode(mode: String) {
        prefs.edit().putString(KEY_LOOP_MODE, mode).apply()
    }

    fun getVolume(): Float {
        return prefs.getFloat(KEY_AUDIO_VOLUME, 0.7f)
    }

    fun saveVolume(volume: Float) {
        prefs.edit().putFloat(KEY_AUDIO_VOLUME, volume).apply()
    }
}
