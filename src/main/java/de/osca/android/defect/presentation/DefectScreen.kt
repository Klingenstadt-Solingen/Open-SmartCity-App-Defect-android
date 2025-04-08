package de.osca.android.defect.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import de.osca.android.defect.R
import de.osca.android.defect.domain.entity.DefectFormBody
import de.osca.android.defect.presentation.args.DefectDesignArgs
import de.osca.android.essentials.domain.entity.Coordinates
import de.osca.android.essentials.domain.entity.Coordinates.Companion.TYPE_GEO_POINT
import de.osca.android.essentials.domain.entity.FileSystem
import de.osca.android.essentials.presentation.component.design.BaseCardContainer
import de.osca.android.essentials.presentation.component.design.BaseDataPrivacyElement
import de.osca.android.essentials.presentation.component.design.BaseDropDown
import de.osca.android.essentials.presentation.component.design.BaseTextField
import de.osca.android.essentials.presentation.component.design.MainButton
import de.osca.android.essentials.presentation.component.design.MasterDesignArgs
import de.osca.android.essentials.presentation.component.design.MultipleButtonField
import de.osca.android.essentials.presentation.component.design.RootContainer
import de.osca.android.essentials.presentation.component.design.SimpleSpacedList
import de.osca.android.essentials.presentation.component.screen_wrapper.ScreenWrapper
import de.osca.android.essentials.presentation.component.topbar.ScreenTopBar
import de.osca.android.essentials.presentation.nav_items.EssentialsNavItems
import de.osca.android.essentials.utils.extensions.SetSystemStatusBar
import de.osca.android.essentials.utils.extensions.getLastDeviceLocation
import de.osca.android.essentials.utils.extensions.shortToast
import de.osca.android.essentials.utils.extensions.toCoordinates
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * Main screen with a list of all cultures
 *
 * @param initialLocation with which location the Widget should egt initialized
 * @param provider provider for file saving
 * @param navController from the navigationGraph
 * @param defectViewModel screen creates the corresponding viewModel
 * @param masterDesignArgs main design arguments for the overall design
 * @param defectDesignArgs design arguments for this module
 */
@Composable
fun DefectScreen(
    navController: NavController,
    initialLocation: LatLng,
    boundingBottomLeft: LatLng,
    boundingTopRight: LatLng,
    maxSearchResults: Int = 10,
    provider: String,
    defectViewModel: DefectViewModel = hiltViewModel(),
    masterDesignArgs: MasterDesignArgs = defectViewModel.defaultDesignArgs,
    defectDesignArgs: DefectDesignArgs = defectViewModel.defectDesignArgs,
) {
    val context = LocalContext.current
    val location = remember { mutableStateOf(initialLocation) }
    val useAddressSearch = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(Unit) {
        defectViewModel.initializeContacts()
    }

    val defects = remember { defectViewModel.defectContacts }
    val invalidFields = remember { defectViewModel.formBody.value.invalidFields }
    val imageIndexClicked = remember { mutableIntStateOf(-1) }
    val bitmapList = remember { defectViewModel.bitmapList }
    var newBitmap: Bitmap?
    val locationText = remember { mutableStateOf(location.value.toString()) }
    val showPhotoDialogue = remember { mutableStateOf(false) }
    val sendAttempt = remember { defectViewModel.sendAttempt }
    val geocoder = Geocoder(context, Locale.GERMAN)
    var tempImageFile: File? = null
    val suggestions = remember { mutableStateListOf<Address>() }
    val initialIndex =
        defects.indexOfFirst { it.objectId == defectViewModel.formBody.value.defectTypeId }

    // reinject the selected location from viewModel into internal state, otherwise locate user
    LaunchedEffect(defectViewModel.formBody.value.geoPoint) {
        if (defectViewModel.formBody.value.hasGeoPoint()) {
            location.value =
                defectViewModel.formBody.value.geoPoint
                    .toLatLng()
            locationText.value = defectViewModel.formBody.value.address
        } else {
            context.getLastDeviceLocation { result ->
                result?.let { latLng ->
                    location.value =
                        LatLng(
                            latLng.latitude,
                            latLng.longitude,
                        )
                } ?: with(context) {
                    shortToast(text = getString(R.string.global_no_location))
                }
            }
        }
    }

    val openGalleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let {
                newBitmap =
                    if (Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, it)
                        ImageDecoder.decodeBitmap(source)
                    }

                bitmapList[imageIndexClicked.value] = newBitmap
            }
        }

    fun locationGeoCoder(location: LatLng?) {
        try {
            val addresses =
                geocoder.getFromLocation(
                    location?.latitude ?: 0.0,
                    location?.longitude ?: 0.0,
                    1,
                )
            if (addresses != null) {
                val address: String = addresses[0].getAddressLine(0)

                locationText.value = address

                defectViewModel.formBody.value.withAddress(address)
                defectViewModel.formBody.value.withCity(addresses[0].locality)
                defectViewModel.formBody.value.withPostalCode(addresses[0].postalCode)

                defectViewModel.formBody.value.withGeoPoint(
                    Coordinates(
                        TYPE_GEO_POINT,
                        location?.latitude ?: initialLocation.latitude,
                        location?.longitude ?: initialLocation.longitude,
                    ),
                )
            } else {
                throw IllegalStateException("Could not get location from LatLng.")
            }
        } catch (ex: Exception) {
            Log.e("DEFECT_SCREEN", ex.stackTraceToString())
        }
    }

    val openCameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                // why is this even needed :(
                if (imageIndexClicked.intValue < 0) {
                    imageIndexClicked.intValue = 0
                }
                tempImageFile?.let {
                    defectViewModel.addImage(imageIndexClicked.intValue, it)
                }
            }
        }

    SetSystemStatusBar(
        !(defectDesignArgs.mIsStatusBarWhite ?: masterDesignArgs.mIsStatusBarWhite),
        Color.Transparent,
    )

    ScreenWrapper(
        topBar = {
            ScreenTopBar(
                title = stringResource(id = defectDesignArgs.vModuleTitle),
                navController = navController,
                overrideBackgroundColor = defectDesignArgs.mTopBarBackColor,
                overrideTextColor = defectDesignArgs.mTopBarTextColor,
                masterDesignArgs = masterDesignArgs,
            )
        },
        viewModel = defectViewModel,
        retryAction = {
            defectViewModel.initializeContacts()
        },
        masterDesignArgs = masterDesignArgs,
        moduleDesignArgs = defectDesignArgs,
    ) {
        RootContainer(
            masterDesignArgs = masterDesignArgs,
            moduleDesignArgs = defectDesignArgs,
        ) {
            item {
                SimpleSpacedList(
                    masterDesignArgs = masterDesignArgs,
                ) {
                    BaseCardContainer(
                        text =
                            stringResource(id = R.string.defect_header_photos) + " " +
                                    "${bitmapList.filterNotNull().size}/${bitmapList.size}",
                        moduleDesignArgs = defectDesignArgs,
                        masterDesignArgs = masterDesignArgs,
                    ) {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                            ) {
                                val notNullList = bitmapList.filterNotNull()

                                bitmapList.forEachIndexed { index, bmp ->
                                    val visible = (bmp != null || notNullList.size >= index)

                                    Card(
                                        shape =
                                            RoundedCornerShape(
                                                defectDesignArgs.mShapeCard
                                                    ?: masterDesignArgs.mShapeCard,
                                            ),
                                        elevation = 0.dp,
                                        backgroundColor = masterDesignArgs.mCardBackColor,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier =
                                                Modifier
                                                    .size(60.dp)
                                                    .clickable {
                                                        imageIndexClicked.value = index
                                                        showPhotoDialogue.value = true
                                                    },
                                        ) {
                                            if (visible) {
                                                if (bmp != null) {
                                                    Image(
                                                        painter = rememberAsyncImagePainter(bmp),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.FillBounds,
                                                        modifier =
                                                            Modifier
                                                                .fillMaxSize(),
                                                    )
                                                } else {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_addphoto_svg),
                                                        contentDescription = null,
                                                        colorFilter =
                                                            ColorFilter.tint(
                                                                masterDesignArgs.mCardTextColor,
                                                            ),
                                                        modifier =
                                                            Modifier
                                                                .size(45.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(showPhotoDialogue.value) {
                                MultipleButtonField(
                                    buttons =
                                        listOf(
                                            stringResource(id = R.string.global_select_from_gallery) to {
                                                openGallery(
                                                    openGalleryLauncher,
                                                    showPhotoDialogue,
                                                )
                                            },
                                            stringResource(id = R.string.global_open_camera) to {
                                                tempImageFile =
                                                    File.createTempFile(
                                                        "defect-",
                                                        ".jpg",
                                                        context.cacheDir,
                                                    )
                                                tempImageFile?.let {
                                                    openCamera(
                                                        openCameraLauncher,
                                                        provider,
                                                        context,
                                                        it,
                                                        showPhotoDialogue,
                                                    )
                                                }
                                            },
                                            stringResource(id = R.string.global_clear) to {
                                                deletePicture(
                                                    bitmapList,
                                                    imageIndexClicked,
                                                    showPhotoDialogue,
                                                )
                                            },
                                        ),
                                    masterDesignArgs = masterDesignArgs,
                                    moduleDesignArgs = defectDesignArgs,
                                )
                            }
                        }
                    }

                    BaseCardContainer(
                        text = stringResource(id = R.string.defect_header_location),
                        moduleDesignArgs = defectDesignArgs,
                        masterDesignArgs = masterDesignArgs,
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = locationText.value,
                                style = masterDesignArgs.normalTextStyle,
                                color = masterDesignArgs.mCardTextColor,
                            )

                            Box(
                                modifier =
                                    Modifier
                                        .padding(top = 20.dp)
                                        .height(200.dp)
                                        .fillMaxWidth()
                                        .clip(
                                            RoundedCornerShape(
                                                defectDesignArgs.mShapeCard
                                                    ?: masterDesignArgs.mShapeCard,
                                            ),
                                        )
                                        .background(masterDesignArgs.mCardBackColor),
                            ) {
                                GoogleMap(
                                    modifier =
                                        Modifier
                                            .fillMaxSize(),
                                    cameraPositionState = cameraPositionState,
                                    properties =
                                        MapProperties(
                                            mapStyleOptions =
                                                if (defectDesignArgs.mapStyle != null) {
                                                    MapStyleOptions.loadRawResourceStyle(
                                                        context,
                                                        defectDesignArgs.mapStyle!!,
                                                    )
                                                } else {
                                                    null
                                                },
                                        ),
                                    uiSettings =
                                        MapUiSettings(
                                            compassEnabled = false,
                                            tiltGesturesEnabled = false,
                                            mapToolbarEnabled = false,
                                            indoorLevelPickerEnabled = false,
                                            myLocationButtonEnabled = false,
                                            zoomControlsEnabled = false,
                                        ),
                                    onMapClick = { latLng ->
                                        coroutineScope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(latLng, 15.0f),
                                            )
                                        }

                                        location.value = latLng
                                        locationGeoCoder(location.value)
                                    },
                                    onMapLoaded = {
                                        coroutineScope.launch {
                                            cameraPositionState.move(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    location.value,
                                                    15.0f,
                                                ),
                                            )
                                        }

                                        locationGeoCoder(location.value)
                                    },
                                ) {
                                    Marker(
                                        state = MarkerState(location.value),
                                    )
                                }
                            }

                            Column {
                                Button(
                                    shape = RoundedCornerShape(50),
                                    colors = masterDesignArgs.getButtonColors(),
                                    onClick = {
                                        useAddressSearch.value = !useAddressSearch.value
                                    },
                                    contentPadding = PaddingValues(2.dp),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(24.dp),
                                ) {
                                    Text(
                                        text = if (useAddressSearch.value) "Suche ausblenden" else "Adresse suchen",
                                        style = masterDesignArgs.bodyTextStyle,
                                        color =
                                            defectDesignArgs.mButtonContentColor
                                                ?: masterDesignArgs.mButtonContentColor,
                                    )
                                }
                            }

                            if (useAddressSearch.value) {
                                Column {
                                    BaseTextField(
                                        masterDesignArgs = masterDesignArgs,
                                        textFieldTitle = "Adresse suchen",
                                        onTextChange = { searchedText ->
                                            if (searchedText.length > 3) {
                                                val foundAddresses =
                                                    geocoder.getFromLocationName(
                                                        searchedText,
                                                        maxSearchResults,
                                                        boundingBottomLeft.latitude,
                                                        boundingBottomLeft.longitude,
                                                        boundingTopRight.latitude,
                                                        boundingTopRight.longitude,
                                                    )
                                                val mappedToLocations =
                                                    foundAddresses?.map {
                                                        LatLng(
                                                            it.latitude,
                                                            it.longitude,
                                                        )
                                                    }

                                                val unSortedList = mutableListOf<Address>()
                                                mappedToLocations?.forEach { loc ->
                                                    val address =
                                                        geocoder.getFromLocation(
                                                            loc.latitude,
                                                            loc.longitude,
                                                            5,
                                                        )

                                                    unSortedList.addAll(address.orEmpty())
                                                }

                                                // sort
                                                val sortedList =
                                                    unSortedList.sortedBy {
                                                        val loc = LatLng(it.latitude, it.longitude)
                                                        location.value
                                                            .toCoordinates()
                                                            .distanceTo(loc.toCoordinates())
                                                    }

                                                suggestions.clear()
                                                suggestions.addAll(sortedList)
                                            }
                                        },
                                        lineCount = 1,
                                        moduleDesignArgs = defectDesignArgs,
                                    )

                                    Text(
                                        text = "geben Sie bitte mindestens 4 Zeichen ein",
                                        style = masterDesignArgs.subtitleTextStyle,
                                        color =
                                            defectDesignArgs.mHintTextColor
                                                ?: masterDesignArgs.mHintTextColor,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(),
                                    )

                                    // displayedSuggestions
                                    if (suggestions.isNotEmpty()) {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 160.dp)
                                                    .padding(top = 16.dp),
                                        ) {
                                            suggestions.forEach { identifiedString ->
                                                item {
                                                    Text(
                                                        text =
                                                            "- " +
                                                                    identifiedString.getAddressLine(
                                                                        0,
                                                                    ),
                                                        style = masterDesignArgs.bodyTextStyle,
                                                        color =
                                                            defectDesignArgs.mMenuTextColor
                                                                ?: masterDesignArgs.mMenuTextColor,
                                                        modifier =
                                                            Modifier
                                                                .clickable {
                                                                    location.value =
                                                                        LatLng(
                                                                            identifiedString.latitude,
                                                                            identifiedString.longitude,
                                                                        )

                                                                    locationGeoCoder(location.value)

                                                                    coroutineScope.launch {
                                                                        cameraPositionState.animate(
                                                                            CameraUpdateFactory.newLatLngZoom(
                                                                                location.value,
                                                                                15.0f,
                                                                            ),
                                                                        )
                                                                    }
                                                                },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    BaseCardContainer(
                        text = stringResource(id = R.string.defect_personal_data),
                        moduleDesignArgs = defectDesignArgs,
                        masterDesignArgs = masterDesignArgs,
                    ) {
                        SimpleSpacedList(
                            masterDesignArgs = masterDesignArgs,
                            overrideSpace = 8.dp,
                        ) {
                            BaseTextField(
                                textFieldTitle = stringResource(id = R.string.global_contact_name),
                                textValue = remember { mutableStateOf(defectViewModel.formBody.value.name) },
                                isError = sendAttempt.value && invalidFields.contains(DefectFormBody.FIELD_NAME),
                                onTextChange = {
                                    defectViewModel.formBody.value.withName(it)
                                },
                                lineCount = 1,
                                masterDesignArgs = masterDesignArgs,
                                moduleDesignArgs = defectDesignArgs,
                            )
                            if (sendAttempt.value && invalidFields.contains(DefectFormBody.FIELD_NAME)) {
                                ErrorLabel(
                                    text = stringResource(id = R.string.global_name_missing_hint),
                                    masterDesignArgs = masterDesignArgs,
                                )
                            }

                            BaseTextField(
                                textFieldTitle = stringResource(id = R.string.global_contact_mail),
                                textValue = remember { mutableStateOf(defectViewModel.formBody.value.email) },
                                isError = sendAttempt.value && invalidFields.contains(DefectFormBody.FIELD_MAIL),
                                onTextChange = {
                                    defectViewModel.formBody.value.withEmail(it)
                                },
                                lineCount = 1,
                                masterDesignArgs = masterDesignArgs,
                                moduleDesignArgs = defectDesignArgs,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            )
                            if (sendAttempt.value && invalidFields.contains(DefectFormBody.FIELD_MAIL)) {
                                ErrorLabel(
                                    text = stringResource(id = R.string.global_email_missing_hint),
                                    masterDesignArgs = masterDesignArgs,
                                )
                            }
                        }
                    }

                    BaseCardContainer(
                        text = stringResource(id = R.string.defect_header_defect),
                        moduleDesignArgs = defectDesignArgs,
                        masterDesignArgs = masterDesignArgs,
                    ) {
                        SimpleSpacedList(
                            masterDesignArgs = masterDesignArgs,
                            overrideSpace = 8.dp,
                        ) {
                            BaseDropDown(
                                initialItem = if (initialIndex >= 0) initialIndex else 0,
                                displayTexts = defects.map { it.title },
                                onSelectedItemChanged = {
                                    defectViewModel.formBody.value.withDefectTypeId(defects[it].objectId)
                                },
                                masterDesignArgs = masterDesignArgs,
                                moduleDesignArgs = defectDesignArgs,
                            )

                            BaseTextField(
                                textFieldTitle = stringResource(id = R.string.global_contact_message),
                                textValue = remember { mutableStateOf(defectViewModel.formBody.value.message) },
                                isError = sendAttempt.value && invalidFields.contains(DefectFormBody.FIELD_MESSAGE),
                                fieldHeight = 150.dp,
                                onTextChange = {
                                    defectViewModel.formBody.value.withMessage(it)
                                },
                                masterDesignArgs = masterDesignArgs,
                                moduleDesignArgs = defectDesignArgs,
                            )
                            if (sendAttempt.value && invalidFields.contains(DefectFormBody.FIELD_MESSAGE)) {
                                ErrorLabel(
                                    text = stringResource(id = R.string.global_message_missing_hint),
                                    masterDesignArgs = masterDesignArgs,
                                )
                            }

                            BaseDataPrivacyElement(
                                initialState = defectViewModel.formBody.value.dataPrivacyChecked,
                                onSwitch = {
                                    defectViewModel.formBody.value.withDataPrivacyChecked(it)
                                },
                                onClickDataPrivacy = {
                                    navController.navigate(EssentialsNavItems.DataPrivacyNavItem.route)
                                },
                                masterDesignArgs = masterDesignArgs,
                                moduleDesignArgs = defectDesignArgs,
                            )
                            if (sendAttempt.value && invalidFields.contains(DefectFormBody.FIELD_DATA_PRIVACY)) {
                                ErrorLabel(
                                    text = stringResource(id = R.string.global_dataprivacy_missing_hint),
                                    masterDesignArgs = masterDesignArgs,
                                )
                            }

                            MainButton(
                                enabled = !sendAttempt.value,
                                buttonText = stringResource(id = R.string.global_send_button),
                                onClick = {
                                    defectViewModel.formBody.value.withPhone("0")
                                    sendAttempt.value = true
                                    defectViewModel.uploadImages(navController)
                                },
                                masterDesignArgs = masterDesignArgs,
                                moduleDesignArgs = defectDesignArgs,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun deletePicture(
    bitmapList: SnapshotStateList<Bitmap?>,
    imageIndexClicked: MutableState<Int>,
    showPhotoDialogue: MutableState<Boolean>,
) {
    bitmapList[imageIndexClicked.value] = null
    showPhotoDialogue.value = false
    sortBitmapList(bitmapList)
}

private fun openCamera(
    openCameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    provider: String,
    context: Context,
    tempImageFile: File,
    showPhotoDialogue: MutableState<Boolean>,
) {
    openCameraLauncher.launch(
        FileSystem.getImageUri(
            provider,
            context,
            tempImageFile,
        ),
    )
    showPhotoDialogue.value = false
}

private fun openGallery(
    openGalleryLauncher: ManagedActivityResultLauncher<String, Uri?>,
    showPhotoDialogue: MutableState<Boolean>,
) {
    openGalleryLauncher.launch("image/*")
    showPhotoDialogue.value = false
}

private fun sortBitmapList(list: MutableList<Bitmap?>) {
    for (x in 0 until list.size - 1) {
        if (list[x] == null) {
            for (y in x until list.size - 1) {
                if (list[y] != null) {
                    list[x] = list[y]
                    list[y] = null
                    break
                }
            }
        }
    }
}

@Composable
fun ErrorLabel(
    masterDesignArgs: MasterDesignArgs,
    text: String,
) {
    Text(
        text = text,
        color = masterDesignArgs.errorTextColor,
        style = masterDesignArgs.normalTextStyle,
        modifier =
            Modifier
                .padding(start = 8.dp),
    )
}
