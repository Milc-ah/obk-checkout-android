package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class LegalEntity : Person() {
    var LegalEntityId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var LegalName: String = ""
        get() {return field;}
        set(value) {field = value;}

    var CommercialName: String = ""
        get() {return field;}
        set(value) {field = value;}

    var ABN: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Email: String= ""
        get() {return field;}
        set(value) {field = value;}
}