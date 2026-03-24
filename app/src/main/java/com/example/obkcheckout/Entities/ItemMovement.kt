package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class ItemMovement : EntityBase() {
    var ItemMovementId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var ItemMovements: Array<ItemMovement>? = null
        get() {return field;}
        set(value) {field = value;}

    var MovementId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var Movement: Movement? = null
        get() {return field;}
        set(value) {field = value;}

    var WarehouseId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var Warehouse: Warehouse? = null
        get() {return field;}
        set(value) {field = value;}

    var OrganizationId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var Organization: Organization? = null
        get() {return field;}
        set(value) {field = value;}

    var ContainerId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var Container: Container? = null
        get() {return field;}
        set(value) {field = value;}

    var ItemId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var Item: Item? = null
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

    var SourceItemMovementId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var SourceItemMovement: ItemMovement? = null
        get() {return field;}
        set(value) {field = value;}
}