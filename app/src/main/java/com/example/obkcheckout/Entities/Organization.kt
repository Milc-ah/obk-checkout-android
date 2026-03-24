package com.example.obkcheckout.Entities

import kotlinx.serialization.Serializable

@Serializable

open class Organization : EntityBase() {
    var OrganizationId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var OrganizationTypeId: Int = 0
        get() {return field;}
        set(value) {field = value;}

    var OrganizationType: OrganizationType? = null
        get() {return field;}
        set(value) {field = value;}

    var Name: String = ""
        get() {return field;}
        set(value) {field = value;}

    var LegalEntityId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var LegalEntity: LegalEntity? = null
        get() {return field;}
        set(value) {field = value;}

    var Description: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Code: String = ""
        get() {return field;}
        set(value) {field = value;}

    var Acronym: String = ""
        get() {return field;}
        set(value) {field = value;}

    var LegacyId: Int? = null
        get() {return field;}
        set(value) {field = value;}

    var ExternalId: String = ""
        get() {return field;}
        set(value) {field = value;}
}