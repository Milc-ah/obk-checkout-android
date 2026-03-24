package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class MeasureUnit : EntityBase() {
    var MeasureUnitId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Name: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Code: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Factor: Double? = null
        get() {return field;}
        set(value) {field = value;}

    var ParentMeasureUnitId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var ParentMeasureUnit: MeasureUnit? = null
        get() {return field;}
        set(value) {field = value;}
}