package de.osca.android.defect.domain.entity

import com.google.gson.annotations.SerializedName
import com.parse.ParseFile
import de.osca.android.essentials.domain.entity.Coordinates
import de.osca.android.networkservice.TestParseFile

/**
 *
 */
data class DefectRequest(
    @SerializedName("name")
    var name: String = "",
    @SerializedName("address")
    var address: String = "",
    @SerializedName("postalCode")
    var postalCode: String = "",
    @SerializedName("city")
    var city: String = "",
    @SerializedName("phone")
    var phone: String = "",
    @SerializedName("email")
    var email: String = "",
    @SerializedName("message")
    var message: String = "",
    @SerializedName("contactId")
    var contactId: String = "",
    @SerializedName("geopoint")
    var geoPoint: Coordinates = Coordinates(),
    @SerializedName("images")
    var images: List<String> = emptyList()
) {
    companion object {
        fun fromDefectFormBody(form: DefectFormBody): DefectRequest {
            return DefectRequest(
                message = form.message,
                geoPoint = form.geoPoint,
                contactId = form.defectTypeId,
                name = form.name,
                email = form.email,
                address = form.address,
                postalCode = form.postalCode,
                city = form.city,
                phone = form.phone,
                images = form.images
            )
        }
    }
}
