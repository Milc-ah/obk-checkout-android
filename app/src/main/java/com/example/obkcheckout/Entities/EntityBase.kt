package com.example.obkcheckout.Entities

import com.example.obkcheckout.Utility.UUIDSerializer
import kotlin.uuid.Uuid
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi

@Serializable(with = UUIDSerializer::class)

open class EntityBase() {
    var RecordOwner: String = ""
        get() {return field}
        set(value) {field = value}

    var RecordEditor: String = ""
        get() {return field}
        set(value) {field = value}

    var RecordUpdate: Instant = Clock.System.now()
        get() {return field}
        set(value) {field = value}

    var RecordDelete: Boolean = false
        get() {return field}
        set(value) {field = value}

    @OptIn(ExperimentalUuidApi::class)
    var LocalKey: Uuid = Uuid.random()
        get() {return field}
        set(value) {field = value}

    var LastUpdatedBy: String = ""
        get() {return field}
        set(value) {field = value}

    var Timestamp: Instant = Clock.System.now()
        get() {return field}
        set(value) {field = value}

    var TimestampOffset: Float = 0f
        get() {return field}
        set(value) {field = value}

    var IsDeleted : Boolean
        get() {return RecordDelete}
        set(value) {RecordDelete = value}
}