package com.redinput.dualmaps.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.redinput.dualmaps.LocationStatus
import com.redinput.dualmaps.R
import com.redinput.dualmaps.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnStreetViewPanoramaReadyCallback {

    companion object {
        private const val STREETVIEW_RADIUS = 100
        private const val MAP_ZOOM_DEFAULT = 13F
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
        viewModel.getObservableLoading().observe(this, Observer { isLoading ->
            binding.appIcon.apply {
                when (isLoading) {
                    true -> startAnimation(animRotate)
                    false -> clearAnimation()
                }
            }
        })

        viewModel.getObservableStatus().observe(this, Observer { status ->
            if (status != null) {
                updateViews(status)
            }
        })

        viewModel.getObservableMessage().observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        })
    }

    private fun checkPermissions() {
        if (hasLocationPermission()) {
            viewModel.getCurrentLocation()
        } else {
            viewModel.getRandomLocation()
        }
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

        viewModel.getObservableStatus().value?.also { status ->
            val position = LatLng(status.latitude, status.longitude)
            marker = googleMap?.addMarker(MarkerOptions().position(position).draggable(true))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLng(position))

            updateBearingMap(status.bearing)
        }

        googleMap?.setOnCameraMoveStartedListener(mapStartedListener)
        googleMap?.setOnCameraIdleListener(mapStoppedListener)
        googleMap?.setOnCameraMoveCanceledListener(mapCancelledListener)

        googleMap?.setOnMapLongClickListener { position ->
            viewModel.updateLocation(position)
        }
        googleMap?.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragEnd(marker: Marker?) {
                marker?.position?.let { viewModel.updateLocation(it) }
            }

            override fun onMarkerDragStart(m: Marker?) {
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

        viewModel.getObservableStatus().value?.also { status ->
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
    //endregion

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
        val status = viewModel.getObservableStatus().value
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
            Toast.makeText(this, R.string.share_text_no_location, Toast.LENGTH_LONG).show()
        }
    }
}