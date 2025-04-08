package de.osca.android.defect.presentation

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import de.osca.android.defect.R
import de.osca.android.defect.data.DefectApiService
import de.osca.android.defect.domain.entity.DefectContact
import de.osca.android.defect.domain.entity.DefectFormBody
import de.osca.android.defect.domain.entity.DefectRequest
import de.osca.android.defect.presentation.args.DefectDesignArgs
import de.osca.android.essentials.domain.entity.FileSystem
import de.osca.android.essentials.presentation.base.BaseViewModel
import de.osca.android.essentials.utils.extensions.*
import de.osca.android.essentials.utils.strings.EssentialsStrings
import de.osca.android.networkservice.utils.RequestHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Defect
 * @param defectDesignArgs the design arguments of the module
 * @param defectApiService the api-endpoints for Parse
 * @param requestHandler handling the request and response for parse
 * @param strings handling the strings from essentials
 *
 * @property googleMap
 * @property defectContacts
 * @property formBody
 * @property marker
 * @property sendAttempt
 * @property bitmapList
 */
@HiltViewModel
class DefectViewModel @Inject constructor(
    val defectDesignArgs: DefectDesignArgs,
    private val defectApiService: DefectApiService,
    private val requestHandler: RequestHandler,
    private val strings: EssentialsStrings
) : BaseViewModel() {
    val defectContacts = mutableStateListOf<DefectContact>()
    var formBody = mutableStateOf(DefectFormBody())
    val sendAttempt = mutableStateOf(false)
    val bitmapList = mutableStateListOf<Bitmap?>(null, null, null, null)

    /**
     * call this function to initialize all defect contacts.
     * it sets the screen to loading, fetches the data from parse and when
     * it finished successful then displays the content and when an error
     * occurred it displays an message screen
     */
    fun initializeContacts() {
        formBody.value.verifyAll()

        viewModelScope.launch {
            wrapperState.loading()
            async {
                fetchDefectContacts()
            }
        }
    }

    /**
     * fetches all defect contacts from parse and when successfully loaded then
     * displays the content
     */
    fun fetchDefectContacts(): Job = launchDataLoad {
        val result = requestHandler.makeRequest(defectApiService::getDefectContacts) ?: emptyList()
        defectContacts.resetWith(result)
        defectContacts.sortBy { it.position }

        wrapperState.displayContent()
    }

    /**
     * performs the uploading of the defect images.
     */
    fun uploadImages(navController: NavController): Job = launchDataLoad {
        if (formBody.value.isValid()) {
            wrapperState.loading()
            // converts images into base64 encoded strings

            withContext(Dispatchers.IO) {
                val streamList = mutableStateListOf<String>()
                for (bitmap in bitmapList) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val base64String =
                        Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                    streamList.add(base64String)
                }
                formBody.value.withImages(streamList)
            }


            sendFormData(navController)
        }
        sendAttempt.value = false
    }

    /**
     * performs the sending of the form data.
     * it shows a toast for success and for error
     */
    private suspend fun sendFormData(navController: NavController) {
        val defectRequestData = DefectRequest.fromDefectFormBody(formBody.value)
        val result = requestHandler.makeRequest {
            defectApiService.postDefectForm(defectRequestData)
        }
        if (result?.isSuccessful == true) {
            formBody.value.clearFields()

            shortToast(strings.getString(R.string.defect_sent_successfully))

            navController.navigateUp()
        } else {
            wrapperState.displayContent()
            shortToast(strings.getString(R.string.defect_failure))
        }

        sendAttempt.value = false
    }

    fun addImage(position: Int, file: File): Job = launchDataLoad {
        bitmapList[position] = FileSystem.loadImageFromFile(file)
        FileSystem.removeFile(file)
    }
}
