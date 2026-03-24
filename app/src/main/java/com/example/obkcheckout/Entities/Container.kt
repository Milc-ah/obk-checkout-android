package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

class Container : EntityBase() {
    var ContainerId: Int? = null
        get() { return field; }
        set(value) { field  = value; }

    var Inventories: Array<Inventory>? = null
        get() { return field; }
        set(value) { field = value; }

    var ItemMovements: Array<ItemMovement>? = null
        get() { return field; }
        set(value) { field = value; }

    var ContainerTypeId: Int = 0
        get() { return field; }
        set(value) { field = value; }

    var ContainerType: ContainerType? = null
        get() { return field; }
        set(value) { field = value; }

    var Description: String? = null
        get() { return field; }
        set(value) { field = value; }

    var Code: String? = null
        get() { return field; }
        set(value) { field = value; }

    var Capacity: Double = 0.0
        get() { return field; }
        set(value) { field = value; }

    var MeasureUnitId: Int = 0
        get() { return field; }
        set(value) { field = value; }

    var MeasureUnit: MeasureUnit? = null
        get() { return field; }
        set(value) { field = value; }

    var Length: Double = 0.0
        get() { return field; }
        set(value) { field = value; }

    var Width: Double = 0.0
        get() { return field; }
        set(value) { field = value; }

    var Height: Double = 0.0
        get() { return field; }
        set(value) { field = value; }
}