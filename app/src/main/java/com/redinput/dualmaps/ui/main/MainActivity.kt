package com.redinput.dualmaps.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.redinput.dualmaps.*
import com.redinput.dualmaps.R
import com.redinput.dualmaps.databinding.ActivityMainBinding
import com.redinput.dualmaps.ui.settings.SettingsActivity

class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnStreetViewPanoramaReadyCallback {

    companion object {
        private const val STREETVIEW_RADIUS = 100
        private const val MAP_ZOOM_DEFAULT = 13F
        private const val ANIMATION_DURATION_IN = 100L
        private const val ANIMATION_DURATION_OUT = 200L
        private const val MESSAGE_DURATION = 5000L
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private var marker: Marker? = null
    private var streetView: StreetViewPanorama? = null

    private var userMovedMap = false

    private lateinit var searchView: SearchView

    private val animRotate = RotateAnimation(
        0F,
        360F,
        Animation.RELATIVE_TO_SELF,
        0.5F,
        Animation.RELATIVE_TO_SELF,
        0.5F
    ).apply {
        duration = 400
        repeatCount = Animation.INFINITE
        repeatMode = Animation.RESTART
        interpolator = LinearInterpolator()
    }

    private val firebase = Firebase.analytics

    //region Activity init
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val streetViewFragment =
            supportFragmentManager.findFragmentById(R.id.street_view_fragment) as SupportStreetViewPanoramaFragment
        streetViewFragment.getStreetViewPanoramaAsync(this)

        binding.compass.setOnCompassDragListener {
            viewModel.updateBearing(it)
        }

        observeViewModel()
        checkPermissions()
    }

    private fun observeViewModel() {
        viewModel.liveLoading.observe(this, Observer { isLoading ->
            binding.appIcon.apply {
                when (isLoading) {
                    true -> {
                        clearAddress()
                        startAnimation(animRotate)
                    }
                    false -> clearAnimation()
                }
            }
        })

        viewModel.liveLocationStatus.observe(this, Observer { status ->
            if (status != null) {
                updateViews(status)
            }
        })

        viewModel.liveMessage.observe(this, Observer {
            processMessage(it)
        })

        viewModel.liveUIStatus.observe(this, Observer { status ->
            when (status.mapType) {
                0 -> googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                1 -> googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                2 -> googleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
                3 -> googleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
            }
            when (status.showCompass) {
                true -> binding.compass.visibility = View.VISIBLE
                false -> binding.compass.visibility = View.GONE
            }
            when (status.showAddress) {
                true -> binding.addressContainer.visibility = View.VISIBLE
                false -> binding.addressContainer.visibility = View.GONE
            }
        })
    }

    private fun checkPermissions() {
        if (viewModel.liveLocationStatus.value == null) {
            if (hasLocationPermission()) {
                viewModel.getCurrentLocation()
            } else {
                viewModel.getRandomLocation()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        viewModel.updateUI()
    }
    //endregion


    //region Menu related methods
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)

        val searchItem = menu.findItem(R.id.menu_search)
        searchView = searchItem.actionView as SearchView

        searchView.queryHint = "Search"
        searchView.isSubmitButtonEnabled = true
        searchView.setIconifiedByDefault(true)
        searchView.imeOptions = EditorInfo.IME_ACTION_SEARCH

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    firebase.logEvent(FirebaseAnalytics.Event.SEARCH) {
                        param(FirebaseAnalytics.Param.SEARCH_TERM, it)
                    }

                    viewModel.searchLocation(it)
                    searchItem.collapseActionView()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_share -> {
                shareLocation()
                return true
            }
            R.id.menu_random -> {
                viewModel.getRandomLocation()
                return true
            }
            R.id.menu_location -> {
                if (hasLocationPermission()) {
                    viewModel.getCurrentLocation()
                } else {
                    showDialogPermission()
                }
                return true
            }
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
    //endregion


    //region Map methods
    override fun onMapReady(map: GoogleMap?) {
        googleMap = map

        googleMap?.isBuildingsEnabled = false
        googleMap?.isIndoorEnabled = false
        googleMap?.isTrafficEnabled = false
        googleMap?.uiSettings?.apply {
            isCompassEnabled = false
            isRotateGesturesEnabled = true
            isScrollGesturesEnabled = true
            isTiltGesturesEnabled = false
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = true
            isMapToolbarEnabled = false
        }
        googleMap?.moveCamera(CameraUpdateFactory.zoomTo(MAP_ZOOM_DEFAULT))

        viewModel.liveLocationStatus.value?.also { status ->
            val position = LatLng(status.latitude, status.longitude)
            marker = googleMap?.addMarker(MarkerOptions().position(position).draggable(true))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLng(position))

            updateBearingMap(status.bearing)
        }
        viewModel.liveUIStatus.value?.also { status ->
            when (status.mapType) {
                0 -> googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                1 -> googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                2 -> googleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
                3 -> googleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
            }
        }

        googleMap?.setOnCameraMoveStartedListener(mapStartedListener)
        googleMap?.setOnCameraIdleListener(mapStoppedListener)
        googleMap?.setOnCameraMoveCanceledListener(mapCancelledListener)

        googleMap?.setOnMapLongClickListener { position ->
            window.decorView.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            viewModel.updateLocation(position)
        }
        googleMap?.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragEnd(marker: Marker?) {
                marker?.position?.let { viewModel.updateLocation(it) }
            }

            override fun onMarkerDragStart(m: Marker?) {
                window.decorView.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }

            override fun onMarkerDrag(m: Marker?) {
            }
        })
    }

    private val mapStartedListener = GoogleMap.OnCameraMoveStartedListener { reason ->
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            userMovedMap = true
            googleMap?.setOnCameraMoveListener(mapMovedListener)
        }
    }

    private val mapMovedListener = GoogleMap.OnCameraMoveListener {
        googleMap?.cameraPosition?.bearing?.let { viewModel.updateBearing(it) }
    }

    private val mapStoppedListener = GoogleMap.OnCameraIdleListener {
        userMovedMap = false
        googleMap?.setOnCameraMoveListener(null)
    }

    private val mapCancelledListener = GoogleMap.OnCameraMoveCanceledListener {
        userMovedMap = false
        googleMap?.setOnCameraMoveListener(null)
    }
    //endregion


    //region StreetView methods
    override fun onStreetViewPanoramaReady(panorama: StreetViewPanorama?) {
        streetView = panorama

        viewModel.liveLocationStatus.value?.also { status ->
            val position = LatLng(status.latitude, status.longitude)
            updatePositionStreetView(position)
            updateBearingStreetView(status.bearing)
        }
    }

    private val streetViewPositionListener = StreetViewPanorama.OnStreetViewPanoramaChangeListener {
        if (it != null) {
            showStreetView()
            viewModel.updateLocation(it.position)
        } else {
            hideStreetView()
        }
    }

    private val streetViewBearingListener =
        StreetViewPanorama.OnStreetViewPanoramaCameraChangeListener {
            viewModel.updateBearing(it.bearing)
        }

    private fun hideStreetView() {
        binding.streetViewFragment.visibility = View.GONE
        binding.streetViewEmpty.visibility = View.VISIBLE
    }

    private fun showStreetView() {
        binding.streetViewFragment.visibility = View.VISIBLE
        binding.streetViewEmpty.visibility = View.GONE
    }
    //endregion


    //region Update views
    private fun updateViews(status: LocationStatus) {
        val position = LatLng(status.latitude, status.longitude)
        if (position != marker?.position) updatePositionMap(position)
        if (position != streetView?.location?.position) updatePositionStreetView(position)

        if (status.bearing != marker?.rotation) updateBearingMap(status.bearing)
        if (status.bearing != streetView?.panoramaCamera?.bearing) updateBearingStreetView(status.bearing)
        binding.compass.setDegrees(status.bearing)

        status.address?.let { showAddress(it) } ?: emptyAddress(position)
    }

    private fun updatePositionMap(position: LatLng) {
        googleMap?.moveCamera(CameraUpdateFactory.newLatLng(position))
        if (marker == null) {
            marker = googleMap?.addMarker(MarkerOptions().position(position).draggable(true))
        } else {
            marker?.position = position
        }
    }

    private fun updatePositionStreetView(position: LatLng) {
        streetView?.setOnStreetViewPanoramaChangeListener(null)
        streetView?.setPosition(position, STREETVIEW_RADIUS, StreetViewSource.OUTDOOR)
        streetView?.setOnStreetViewPanoramaChangeListener(streetViewPositionListener)
    }

    private fun updateBearingMap(bearing: Float) {
        if (!userMovedMap) {
            val camera = googleMap?.cameraPosition?.let {
                CameraPosition.Builder(it)
                    .bearing(bearing)
                    .build()
            } ?: marker?.let {
                CameraPosition.Builder()
                    .target(it.position)
                    .bearing(bearing)
                    .build()
            }

            if (camera != null) {
                googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
            }
        }
    }

    private fun updateBearingStreetView(bearing: Float) {
        streetView?.setOnStreetViewPanoramaCameraChangeListener(null)

        val camera = streetView?.panoramaCamera?.let {
            StreetViewPanoramaCamera.Builder(it)
                .bearing(bearing)
                .build()
        } ?: run {
            StreetViewPanoramaCamera.Builder()
                .bearing(bearing)
                .build()
        }
        streetView?.animateTo(camera, 0L)

        Handler().postDelayed({
            streetView?.setOnStreetViewPanoramaCameraChangeListener(streetViewBearingListener)
        }, 100)
    }

    private fun clearAddress() {
        updateAddress("--", "--")
    }

    private fun showAddress(address: Address) {
        updateAddress(address.title, address.subtitle)
    }

    private fun emptyAddress(position: LatLng) {
        val title = getString(R.string.address_empty_title)
        val subtitle =
            getString(R.string.address_empty_subtitle, position.latitude, position.longitude)
        updateAddress(title, subtitle)
    }

    private fun updateAddress(title: String, subtitle: String) {
        if (binding.locationTitle.text != title) {
            binding.locationTitle.setAdaptativeText(title, 12, 24)
        }
        if (binding.locationSubtitle.text != subtitle) {
            binding.locationSubtitle.setAdaptativeText(subtitle, 8, 16)
        }
    }
    //endregion

    private fun processMessage(message: Message) {
        when (message.type) {
            MessageType.ERROR -> binding.bannerTop.setBackgroundResource(R.color.red)
            MessageType.WARNING -> binding.bannerTop.setBackgroundResource(R.color.yellow)
        }
        binding.bannerTop.text = getString(message.text)
        showBannerTop()

        Handler().postDelayed({ hideBannerTop() }, MESSAGE_DURATION)
    }

    private fun showBannerTop() {
        val transition = Fade(Fade.MODE_IN).apply {
            duration = ANIMATION_DURATION_IN
            addTarget(binding.bannerTop)
        }

        TransitionManager.beginDelayedTransition(binding.root, transition)
        binding.bannerTop.visibility = View.VISIBLE
    }

    private fun hideBannerTop() {
        val transition = Fade(Fade.MODE_OUT).apply {
            duration = ANIMATION_DURATION_OUT
            addTarget(binding.bannerTop)
        }

        TransitionManager.beginDelayedTransition(binding.root, transition)
        binding.bannerTop.visibility = View.GONE
    }

    private fun showDialogPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_permission_title)
            .setMessage(R.string.dialog_permission_message)
            .setPositiveButton(R.string.dialog_permission_positive) { _, _ ->
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton(R.string.dialog_permission_negative, null)
            .show()
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun shareLocation() {
        firebase.logEvent(FirebaseAnalytics.Event.SHARE) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "text")
            param(FirebaseAnalytics.Param.METHOD, "menu")
        }

        val status = viewModel.liveLocationStatus.value
        if (status != null) {
            val text = getString(R.string.share_text_placeholder, status.latitude, status.longitude)

            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, getString(R.string.share_title))
            startActivity(shareIntent)

        } else {
            processMessage(Message(MessageType.WARNING, R.string.share_text_no_location))
        }
    }
}
