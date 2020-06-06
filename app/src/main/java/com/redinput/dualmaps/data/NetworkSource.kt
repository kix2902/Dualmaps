package com.redinput.dualmaps.data

import com.redinput.dualmaps.GeoNames
import java.io.IOException

interface NetworkSource {
    fun getRandomLocation(): GeoNames.Response
}