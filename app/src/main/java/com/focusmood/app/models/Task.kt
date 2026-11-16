package com.focusmood.app.models

import org.json.JSONObject

data class Task(
    val id: Long,
    var text: String,
    var completed: Boolean,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("text", text)
        obj.put("completed", completed)
        obj.put("createdAt", createdAt)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): Task {
            return Task(
                id = obj.optLong("id", System.currentTimeMillis()),
                text = obj.optString("text", ""),
                completed = obj.optBoolean("completed", false),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}
