package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable
open class Address : EntityBase() {
    var AddressId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Address1: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Address2: String? = null
        get() {return field;}
        set(value) {field = value;}

    var CityId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var City: City? = null
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

    var PostalCode: String? = null
        get() {return field;}
        set(value) {field = value;}
}