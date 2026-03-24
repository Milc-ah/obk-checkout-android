package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class ContactType : EntityBase() {
    var ContactTypeId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Description: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Type: String = ""
        get() {return field;}
        set(value) {field = value;}
}