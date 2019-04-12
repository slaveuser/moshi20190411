package com.squareup.moshi.kotlin.codgen.LooksLikeAClass

import com.squareup.moshi.JsonClass

/**
 * https://github.com/square/moshi/issues/783
 */
@JsonClass(generateAdapter = true)
data class ClassInPackageThatLooksLikeAClass(val foo: String)