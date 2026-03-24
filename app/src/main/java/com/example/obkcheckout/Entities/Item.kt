package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class Item : EntityBase() {
    var ItemId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Inventories: Array<Inventory>? = null
        get() {return field;}
        set(value) {field = value;}

    var ItemMoviments: Array<ItemMovement>? = null
        get() {return field;}
        set(value) {field = value;}

    var Name: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Description: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Code: String = ""
        get() {return field;}
        set(value) {field = value;}

    var ItemTypeId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var ItemType: ItemType? = null
        get() {return field;}
        set(value) {field = value;}

    var MeasureUnitId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var MeasureUnit: MeasureUnit? = null
        get() {return field;}
        set(value) {field = value;}

    var Units: Double = 0.0
        get() {return field;}
        set(value) {field = value;}

    var UnitWeightMeasureUnitId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var UnitWeightMeasureUnit: MeasureUnit? = null
        get() {return field;}
        set(value) {field = value;}

    var UnitWeight: Double? = null
        get() {return field;}
        set(value) {field = value;}
}