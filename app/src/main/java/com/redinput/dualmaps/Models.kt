package com.redinput.dualmaps

import androidx.annotation.StringRes
import com.squareup.moshi.JsonClass
import kotlin.random.Random

data class LocationStatus(
    var latitude: Double,
    var longitude: Double,
    var address: Address? = null,
    var bearing: Float = Random.nextDouble(0.0, 360.0).toFloat()
)

data class Address(
    val title: String,
    val subtitle: String
)

data class Message(
    val type: MessageType,
    @StringRes val text: Int
)

enum class MessageType {
    ERROR,
    WARNING
}

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

object Geocode {
    data class Location(
        val latitude: Double,
        val longitude: Double
    )

    data class Address(
        val title: String,
        val subtitle: String
    )

    data class Request(
        val query: String
    )
}