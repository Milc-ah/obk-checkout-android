package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class PersonAddressMapping : EntityBase() {
    var PersonAddressMappingId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var PersonId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var AddressId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var Address: Address? = null
        get() {return field;}
        set(value) {field = value;}

    var AddressTypeId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var AddressType: AddressType? = null
        get() {return field;}
        set(value) {field = value;}
}