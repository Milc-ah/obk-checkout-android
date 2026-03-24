package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class State : EntityBase() {
    var StateId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Name: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Acronym: String = ""
        get() {return field;}
        set(value) {field = value;}

    var CountryId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var Country: Country? = null
        get() {return field;}
        set(value) {field = value;}
}