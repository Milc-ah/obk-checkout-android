package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class Warehouse : EntityBase() {
    var WarehouseId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Inventories: Array<Inventory>? = null
        get() {return field;}
        set(value) {field = value;}

    var ItemMovements: Array<ItemMovement>? = null
        get() {return field;}
        set(value) {field = value;}

    var Name: String = ""
        get() {return field;}
        set(value) {field = value;}

    var WarehouseTypeId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var WarehouseType: WarehouseType? = null
        get() {return field;}
        set(value) {field = value;}
}