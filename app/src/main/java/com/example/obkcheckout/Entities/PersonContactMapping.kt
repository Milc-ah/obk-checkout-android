package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class PersonContactMapping : EntityBase() {
    var PersonContactMappingId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var PersonId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var ContactTypeId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var ContactType: ContactType? = null
        get() {return field;}
        set(value) {field = value;}

    var Name: String = ""
        get() {return field;}
        set(value) {field = value;}

    var PhoneNumber: String = ""
        get() {return field;}
        set(value) {field = value;}

    var CellNumber: String
        get() {return CellNumber;}
        set(value) {CellNumber = value;}
}