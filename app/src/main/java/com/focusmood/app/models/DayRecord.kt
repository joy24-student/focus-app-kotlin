package com.focusmood.app.models

import org.json.JSONObject

data class DayRecord(
    val dateKey: String,
    var minutes: Int = 0,
    var sessions: Int = 0
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("dateKey", dateKey)
        obj.put("minutes", minutes)
        obj.put("sessions", sessions)
        return obj
    }

    companion object {
        fun fromJson(dateKey: String, obj: JSONObject): DayRecord {
            return DayRecord(
                dateKey = dateKey,
                minutes = obj.optInt("minutes", 0),
                sessions = obj.optInt("sessions", 0)
            )
        }
    }
}
