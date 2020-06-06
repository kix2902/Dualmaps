package com.redinput.dualmaps

import android.util.TypedValue
import android.widget.TextView
import androidx.core.widget.TextViewCompat

val Any.TAG: String
    get() = this::class.java.simpleName

fun TextView.setAdaptativeText(text: String, minSize: Int, maxSize: Int) {
    TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
    this.setTextSize(TypedValue.COMPLEX_UNIT_SP, maxSize.toFloat())
    this.text = text
    this.post {
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            this,
            minSize,
            maxSize,
            1,
            TypedValue.COMPLEX_UNIT_SP
        )
    }
}
