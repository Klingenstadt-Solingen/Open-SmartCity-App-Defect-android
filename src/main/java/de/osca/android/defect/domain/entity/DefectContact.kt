package de.osca.android.defect.domain.entity

import com.google.gson.annotations.SerializedName

/**
 * The Json-Response-Data-Structure
 */
data class DefectContact(
    @SerializedName("objectId")
    val objectId: String = "",
    @SerializedName("title")
    val title: String = "",
    @SerializedName("emailSubject")
    val emailSubject: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("position")
    val position: Int = 0
)