package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class City : EntityBase() {
    var CityId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Name: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Acronym: String = ""
        get() {return field;}
        set(value) {field = value;}

    var StateId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var State: State? = null
        get() {return field;}
        set(value) {field = value;}

    var CountryId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var Country: Country? = null
        get() {return field;}
        set(value) {field = value;}
}