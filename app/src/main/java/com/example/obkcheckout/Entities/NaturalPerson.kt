package com.example.obkcheckout.Entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.sql.Time
import kotlinx.serialization.Serializable

@Serializable

open class NaturalPerson : Person() {
    var NaturalPersonId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var LastName: String = ""
        get() {return field;}
        set(value) {field = value;}

    var FirstName: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Initials: String = ""
        get() {return field;}
        set(value) {field = value;}

    var AsianOrder: Boolean = false
        get() {return field;}
        set(value) {field = value;}

    var GenderId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Gender: Gender? = null
        get() {return field;}
        set(value) {field = value;}

    var TaxNumber: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Email: String = ""
        get() {return field;}
        set(value) {field = value;}

    var DateOfBirth: Instant = Clock.System.now()
        get() {return field;}
        set(value) {field = value;}

    var LegacyId: Int? = null
        get() {return field;}
        set(value) {field = value;}
}