package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class ItemType : EntityBase() {
    var ItemTypeId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var Items: Array<Item>? = null
        get() {return field;}
        set(value) {field = value;}

    var ItemTypes: Array<ItemType>? = null
        get() {return field;}
        set(value) {field = value;}

    var Name: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Code: String = ""
        get() {return field;}
        set(value) {field = value;}

    var ParentItemTypeId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var ParentItemType: ItemType? = null
        get() {return field;}
        set(value) {field = value;}
}