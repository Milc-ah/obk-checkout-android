package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class WarehouseType : EntityBase() {
    var WarehouseTypeId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Name: String = ""
        get() {return field;}
        set(value) {field = value;}

    var StagingArea: Boolean = false
        get() {return field;}
        set(value) {field = value;}

    var DeliveryArea: Boolean = false
        get() {return field;}
        set(value) {field = value;}
}