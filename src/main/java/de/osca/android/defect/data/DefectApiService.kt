package de.osca.android.defect.data

import de.osca.android.defect.domain.entity.DefectContact
import de.osca.android.defect.domain.entity.DefectRequest
import de.osca.android.essentials.domain.entity.ObjectCreationResponse
import de.osca.android.essentials.utils.annotations.UnwrappedResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DefectApiService {

    /**
     * Getting all DefectContacts
     * Endpoint: "classes/DefectFormContact"
     */
    @GET("classes/DefectFormContact")
    suspend fun getDefectContacts(): Response<List<DefectContact>>

    /**
     * Sending the DefectFormData
     * Endpoint: "classes/DefectFormData"
     */
    @UnwrappedResponse
    @POST("classes/DefectFormData")
    suspend fun postDefectForm(@Body defectRequest: DefectRequest) : Response<ObjectCreationResponse>
}