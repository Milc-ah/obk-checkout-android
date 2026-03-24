package com.example.obkcheckout.Utility

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
class Response {
    var data: JsonElement = JsonObject(emptyMap())
        get() {return field;}
        set(value) {field = value;}

    var rows: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var filteredRows: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var message:  String = ""
        get() {return field;}
        set(value) {field = value;}

    var totalTime: Instant = Clock.System.now()
        get() {return field;}
        set(value) {field = value;}

    var errors : Array<String>? = null
        get() {return field;}
        set(value) {field = value;}

    var success: Boolean = false
        get() {return field;}
        set(value) {field = value;}

}