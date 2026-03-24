package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class Country : EntityBase() {
    var CountryId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Name: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Acronym: String = ""
        get() {return field;}
        set(value) {field = value;}
}