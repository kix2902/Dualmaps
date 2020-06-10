package com.redinput.dualmaps.data

import com.redinput.dualmaps.GeoNames

interface NetworkSource {
    fun getRandomLocation(): GeoNames.Response
}