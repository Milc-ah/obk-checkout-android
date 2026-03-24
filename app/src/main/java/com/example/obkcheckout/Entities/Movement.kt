package com.example.obkcheckout.Entities

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable

open class Movement : EntityBase() {
    var MovementId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var ItemMovements: Array<ItemMovement>? = null
        get() {return field;}
        set(value) {field = value;}

    var OrganizationId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var Organization: Organization? = null
        get() {return field;}
        set(value) {field = value;}

    var MovementDateTime: Instant = Clock.System.now()
        get() {return field;}
        set(value) {field = value;}
}