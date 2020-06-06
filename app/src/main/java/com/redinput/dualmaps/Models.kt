package com.redinput.dualmaps

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlin.random.Random

data class LocationStatus(
    var latitude: Double,
    var longitude: Double,
    var bearing: Float = Random.nextDouble(0.0, 360.0).toFloat()
)

data class OnboardState(
    var position: Int = 0,
    var total: Int = 1,
    var step: Onboard.Step? = null
)

object Onboard {
    @JsonClass(generateAdapter = true)
    data class Step(
        val title: String,
        val type: Type,
        val layout: Layout,
        val button: String,
        val skippable: Boolean,
        val data: Data
    )

    @JsonClass(generateAdapter = true)
    data class Data(
        val description: String,
        val images: List<String> = listOf(),
        val permission: String? = null,
        val mandatory: Boolean? = null,
        val key: String? = null,
        val checkbox: String? = null
    )

    enum class Type {
        SIMPLE,
        PERMISSION,
        PREFERENCE
    }

    enum class Layout {
        TEXT,
        IMAGE,
        CHECKBOX
    }
}

object GeoNames {
    @JsonClass(generateAdapter = true)
    data class Response(
        val nearest: Location
    )

    @JsonClass(generateAdapter = true)
    data class Location(
        val latt: Double,
        val longt: Double
    )
}