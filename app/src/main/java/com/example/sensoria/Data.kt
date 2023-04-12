package com.example.sensoria
import org.json.JSONObject

data class Data(val x: Float, val y: Float, val z: Float) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("x", x)
        json.put("y", y)
        json.put("z", z)
        return json
    }
}
