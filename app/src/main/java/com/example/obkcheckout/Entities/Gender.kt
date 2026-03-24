package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class Gender : EntityBase() {
    var GenderId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Description: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Acronym: String = ""
        get() {return field;}
        set(value) {field = value;}
}