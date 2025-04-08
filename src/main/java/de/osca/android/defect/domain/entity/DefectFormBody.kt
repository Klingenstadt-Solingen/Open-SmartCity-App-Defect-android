package de.osca.android.defect.domain.entity

import androidx.compose.runtime.mutableStateListOf
import de.osca.android.essentials.domain.entity.Coordinates
import de.osca.android.essentials.utils.constants.EMAIL_PATTERN
import de.osca.android.essentials.utils.extensions.addIfNotContains
import java.util.regex.Pattern

/**
 *
 */
data class DefectFormBody(
    private var _defectTypeId: String = "",
    private var _name: String = "",
    private var _message: String = "",
    private var _address: String = "",
    private var _city: String = "",
    private var _postalCode: String = "",
    private var _phone: String = "",
    private var _email: String = "",
    private var _images: List<String> = emptyList(),
    private var _geoPoint: Coordinates = Coordinates.getDefaultCoordinates(0.0, 0.0),
    private val _invalidFields: MutableList<Int> = mutableStateListOf(),
    private var _dataPrivacyChecked: Boolean = false,
) {
    val name: String get() = _name
    val message: String get() = _message
    val address: String get() = _address
    val city: String get() = _city
    val postalCode: String get() = _postalCode
    val phone: String get() = _phone
    val email: String get() = _email
    val images: List<String> get() = _images
    val defectTypeId: String get() = _defectTypeId
    val geoPoint: Coordinates get() = _geoPoint
    val dataPrivacyChecked: Boolean get() = _dataPrivacyChecked
    val invalidFields: List<Int> get() = _invalidFields

    fun isValid(): Boolean = _invalidFields.isEmpty()

    fun hasGeoPoint(): Boolean = _geoPoint != Coordinates.getDefaultCoordinates(0.0, 0.0)

    fun verifyAll() {
        verifyMessage()
        verifyDataPrivacyChecked()
        verifyDefectTypeId()
        verifyMail()
        verifyName()
    }

    fun clearFields() {
        _message = ""
        _defectTypeId = ""
        _dataPrivacyChecked = false
        _name = ""
        _email = ""
        verifyAll()
    }

    fun withMessage(valueMessage: String) {
        _message = valueMessage
        verifyMessage()
    }

    fun withCity(valueCity: String) {
        _city = valueCity
    }

    fun withName(valueName: String) {
        _name = valueName
        verifyName()
    }

    fun withPhone(valuePhone: String) {
        _phone = valuePhone
    }

    fun withEmail(valueEmail: String) {
        _email = valueEmail
        verifyMail()
    }

    fun withAddress(valueAddress: String) {
        _address = valueAddress
    }

    fun withPostalCode(valuePostalCode: String) {
        _postalCode = valuePostalCode
    }

    fun withGeoPoint(valueGeoPoint: Coordinates) {
        _geoPoint = valueGeoPoint
        verifyGeoPoint()
    }

    fun withImages(valueImages: List<String>) {
        _images = valueImages
    }

    fun withDefectTypeId(valueDefectTypeId: String) {
        _defectTypeId = valueDefectTypeId
        verifyDefectTypeId()
    }

    fun withDataPrivacyChecked(dataPrivacyChecked: Boolean) {
        _dataPrivacyChecked = dataPrivacyChecked
        verifyDataPrivacyChecked()
    }

    private fun verifyMessage() {
        if (_message.length < 5) {
            _invalidFields.addIfNotContains(FIELD_MESSAGE)
        } else {
            _invalidFields.remove(FIELD_MESSAGE)
        }
    }

    private fun verifyName() {
        if (_name.isBlank()) {
            _invalidFields.addIfNotContains(FIELD_NAME)
        } else {
            _invalidFields.remove(FIELD_NAME)
        }
    }

    private fun verifyMail() {
        val emailPattern: Pattern =
            Pattern.compile(
                EMAIL_PATTERN,
                Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL,
            )
        if (_email.isEmpty() || emailPattern.matcher(_email).matches().not()) {
            _invalidFields.addIfNotContains(FIELD_MAIL)
        } else {
            _invalidFields.remove(FIELD_MAIL)
        }
    }

    private fun verifyGeoPoint() {
    }

    private fun verifyDataPrivacyChecked() {
        if (!_dataPrivacyChecked) {
            _invalidFields.addIfNotContains(FIELD_DATA_PRIVACY)
        } else {
            _invalidFields.remove(FIELD_DATA_PRIVACY)
        }
    }

    private fun verifyDefectTypeId() {
        if (_defectTypeId.isBlank()) {
            _invalidFields.addIfNotContains(FIELD_DEFECT_TYPE_DATA)
        } else {
            _invalidFields.remove(FIELD_DEFECT_TYPE_DATA)
        }
    }

    companion object {
        const val FIELD_MESSAGE = 0
        const val FIELD_DEFECT_TYPE_DATA = 1
        const val FIELD_DATA_PRIVACY = 2
        const val FIELD_NAME = 3
        const val FIELD_MAIL = 4
    }
}
