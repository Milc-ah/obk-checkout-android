package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class Person : EntityBase() {
    var PersonId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var PersonAddressMappings: Array<PersonAddressMapping>? = null
        get() {return field;}
        set(value) {field = value;}

    var PersonContactMappings: Array<PersonContactMapping>? = null
        get() {return field;}
        set(value) {field = value;}

    var PersonTypeId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var PersonType: PersonType? = null
        get() {return field;}
        set(value) {field = value;}

    var FullName: String = ""
        get() {return field;}
        set(value) {field = value;}

    var NickName: String = ""
        get() {return field;}
        set(value) {field = value;}

    var ProfilePicture: String? = null
        get() {return field;}
        set(value) {field = value;}
}