package com.focusmood.app.models

import org.json.JSONObject

data class AudioTrack(
    val id: String,
    val name: String,
    val size: String,
    val localPath: String,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("size", size)
        obj.put("localPath", localPath)
        obj.put("addedAt", addedAt)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): AudioTrack {
            return AudioTrack(
                id = obj.optString("id", ""),
                name = obj.optString("name", "Unknown Track"),
                size = obj.optString("size", "0 KB"),
                localPath = obj.optString("localPath", ""),
                addedAt = obj.optLong("addedAt", System.currentTimeMillis())
            )
        }
    }
}
