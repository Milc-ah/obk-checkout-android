package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class ContainerType : EntityBase() {
    var ContainerTypeId: Int? = null
        get() {return field;}
        set(value) { field = value;}

    var Containers: Array<Container>? = null
        get() {return field; }
        set(value) { field = value; }

    var Name: String = ""
        get() {return field; }
        set(value) { field = value; }

    var Code: String = ""
        get() {return field; }
        set(value) { field = value; }

}