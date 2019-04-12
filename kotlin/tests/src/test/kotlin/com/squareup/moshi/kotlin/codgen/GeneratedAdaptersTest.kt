/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.moshi.kotlin.codgen

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import java.util.Locale
import kotlin.reflect.full.memberProperties

@Suppress("UNUSED", "UNUSED_PARAMETER")
class GeneratedAdaptersTest {

  private val moshi = Moshi.Builder().build()

  @Test
  fun jsonAnnotation() {
    val adapter = moshi.adapter(JsonAnnotation::class.java)

    // Read
    @Language("JSON")
    val json = """{"foo": "bar"}"""

    val instance = adapter.fromJson(json)!!
    assertThat(instance.bar).isEqualTo("bar")

    // Write
    @Language("JSON")
    val expectedJson = """{"foo":"baz"}"""

    assertThat(adapter.toJson(JsonAnnotation("baz"))).isEqualTo(expectedJson)
  }

  @JsonClass(generateAdapter = true)
  data class JsonAnnotation(@Json(name = "foo") val bar: String)

  @Test
  fun jsonAnnotationWithDollarSign() {
    val adapter = moshi.adapter(JsonAnnotationWithDollarSign::class.java)

    // Read
    val json = "{\"\$foo\": \"bar\"}"

    val instance = adapter.fromJson(json)!!
    assertThat(instance.bar).isEqualTo("bar")

    // Write
    val expectedJson = "{\"\$foo\":\"baz\"}"

    assertThat(adapter.toJson(JsonAnnotationWithDollarSign("baz"))).isEqualTo(expectedJson)
  }

  @JsonClass(generateAdapter = true)
  data class JsonAnnotationWithDollarSign(@Json(name = "\$foo") val bar: String)

  @Test
  fun defaultValues() {
    val adapter = moshi.adapter(DefaultValues::class.java)

    // Read/write with default values
    @Language("JSON")
    val json = """{"foo":"fooString"}"""

    val instance = adapter.fromJson(json)!!
    assertThat(instance.foo).isEqualTo("fooString")
    assertThat(instance.bar).isEqualTo("")
    assertThat(instance.nullableBar).isNull()
    assertThat(instance.bazList).apply {
      isNotNull()
      isEmpty()
    }

    @Language("JSON") val expected = """{"foo":"fooString","bar":"","bazList":[]}"""
    assertThat(adapter.toJson(DefaultValues("fooString"))).isEqualTo(expected)

    // Read/write with real values
    @Language("JSON")
    val json2 = """
      {"foo":"fooString","bar":"barString","nullableBar":"bar","bazList":["baz"]}
      """.trimIndent()

    val instance2 = adapter.fromJson(json2)!!
    assertThat(instance2.foo).isEqualTo("fooString")
    assertThat(instance2.bar).isEqualTo("barString")
    assertThat(instance2.nullableBar).isEqualTo("bar")
    assertThat(instance2.bazList).containsExactly("baz")
    assertThat(adapter.toJson(instance2)).isEqualTo(json2)
  }

  @JsonClass(generateAdapter = true)
  data class DefaultValues(
    val foo: String,
    val bar: String = "",
    val nullableBar: String? = null,
    val bazList: List<String> = emptyList())

  @Test
  fun nullableArray() {
    val adapter = moshi.adapter(NullableArray::class.java)

    @Language("JSON")
    val json = """{"data":[null,"why"]}"""

    val instance = adapter.fromJson(json)!!
    assertThat(instance.data).containsExactly(null, "why")
    assertThat(adapter.toJson(instance)).isEqualTo(json)
  }

  @JsonClass(generateAdapter = true)
  data class NullableArray(val data: Array<String?>)

  @Test
  fun primitiveArray() {
    val adapter = moshi.adapter(PrimitiveArray::class.java)

    @Language("JSON")
    val json = """{"ints":[0,1]}"""

    val instance = adapter.fromJson(json)!!
    assertThat(instance.ints).containsExactly(0, 1)
    assertThat(adapter.toJson(instance)).isEqualTo(json)
  }

  @JsonClass(generateAdapter = true)
  data class PrimitiveArray(val ints: IntArray)

  @Test
  fun nullableTypes() {
    val adapter = moshi.adapter(NullabeTypes::class.java)

    @Language("JSON")
    val json = """{"foo":"foo","nullableString":null}"""
    @Language("JSON")
    val invalidJson = """{"foo":null,"nullableString":null}"""

    val instance = adapter.fromJson(json)!!
    assertThat(instance.foo).isEqualTo("foo")
    assertThat(instance.nullableString).isNull()

    try {
      adapter.fromJson(invalidJson)
      fail("The invalid json should have failed!")
    } catch (e: JsonDataException) {
      assertThat(e).hasMessageContaining("foo")
    }
  }

  @JsonClass(generateAdapter = true)
  data class NullabeTypes(
      val foo: String,
      val nullableString: String?
  )

  @Test
  fun collections() {
    val adapter = moshi.adapter(SpecialCollections::class.java)

    val specialCollections = SpecialCollections(
        mutableListOf(),
        mutableSetOf(),
        mutableMapOf(),
        emptyList(),
        emptySet(),
        emptyMap()
    )

    val json = adapter.toJson(specialCollections)
    val newCollections = adapter.fromJson(json)
    assertThat(newCollections).isEqualTo(specialCollections)
  }

  @JsonClass(generateAdapter = true)
  data class SpecialCollections(
      val mutableList: MutableList<String>,
      val mutableSet: MutableSet<String>,
      val mutableMap: MutableMap<String, String>,
      val immutableList: List<String>,
      val immutableSet: Set<String>,
      val immutableMap: Map<String, String>
  )

  @Test
  fun mutableProperties() {
    val adapter = moshi.adapter(MutableProperties::class.java)

    val mutableProperties = MutableProperties(
        "immutableProperty",
        "mutableProperty",
        mutableListOf("immutableMutableList"),
        mutableListOf("immutableImmutableList"),
        mutableListOf("mutableMutableList"),
        mutableListOf("mutableImmutableList"),
        "immutableProperty",
        "mutableProperty",
        mutableListOf("immutableMutableList"),
        mutableListOf("immutableImmutableList"),
        mutableListOf("mutableMutableList"),
        mutableListOf("mutableImmutableList")
    )

    val json = adapter.toJson(mutableProperties)
    val newMutableProperties = adapter.fromJson(json)
    assertThat(newMutableProperties).isEqualTo(mutableProperties)
  }

  @JsonClass(generateAdapter = true)
  data class MutableProperties(
      val immutableProperty: String,
      var mutableProperty: String,
      val immutableMutableList: MutableList<String>,
      val immutableImmutableList: List<String>,
      var mutableMutableList: MutableList<String>,
      var mutableImmutableList: List<String>,
      val nullableImmutableProperty: String?,
      var nullableMutableProperty: String?,
      val nullableImmutableMutableList: MutableList<String>?,
      val nullableImmutableImmutableList: List<String>?,
      var nullableMutableMutableList: MutableList<String>?,
      var nullableMutableImmutableList: List<String>
  )

  @Test
  fun nullableTypeParams() {
    val adapter = moshi.adapter<NullableTypeParams<Int>>(
        Types.newParameterizedTypeWithOwner(GeneratedAdaptersTest::class.java,
            NullableTypeParams::class.java, Int::class.javaObjectType))
    val nullSerializing = adapter.serializeNulls()

    val nullableTypeParams = NullableTypeParams(
        listOf("foo", null, "bar"),
        setOf("foo", null, "bar"),
        mapOf("foo" to "bar", "baz" to null),
        null,
        1
    )

    val noNullsTypeParams = NullableTypeParams(
        nullableTypeParams.nullableList,
        nullableTypeParams.nullableSet,
        nullableTypeParams.nullableMap.filterValues { it != null },
        null,
        1
    )

    val json = adapter.toJson(nullableTypeParams)
    val newNullableTypeParams = adapter.fromJson(json)
    assertThat(newNullableTypeParams).isEqualTo(noNullsTypeParams)

    val nullSerializedJson = nullSerializing.toJson(nullableTypeParams)
    val nullSerializedNullableTypeParams = adapter.fromJson(nullSerializedJson)
    assertThat(nullSerializedNullableTypeParams).isEqualTo(nullableTypeParams)
  }

  @JsonClass(generateAdapter = true)
  data class NullableTypeParams<T>(
    val nullableList: List<String?>,
    val nullableSet: Set<String?>,
    val nullableMap: Map<String, String?>,
    val nullableT: T?,
    val nonNullT: T
  )

  @Test fun doNotGenerateAdapter() {
    try {
      Class.forName("${GeneratedAdaptersTest::class.java.name}_DoNotGenerateAdapterJsonAdapter")
      fail("found a generated adapter for a type that shouldn't have one")
    } catch (expected: ClassNotFoundException) {
    }
  }

  @JsonClass(generateAdapter = false)
  data class DoNotGenerateAdapter(val foo: String)

  @Test fun constructorParameters() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ConstructorParameters::class.java)

    val encoded = ConstructorParameters(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ConstructorParameters(var a: Int, var b: Int)

  @Test fun properties() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(Properties::class.java)

    val encoded = Properties()
    encoded.a = 3
    encoded.b = 5
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":3,"b":5}""")!!
    assertThat(decoded.a).isEqualTo(3)
    assertThat(decoded.b).isEqualTo(5)
  }

  @JsonClass(generateAdapter = true)
  class Properties {
    var a: Int = -1
    var b: Int = -1
  }

  @Test fun constructorParametersAndProperties() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ConstructorParametersAndProperties::class.java)

    val encoded = ConstructorParametersAndProperties(3)
    encoded.b = 5
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ConstructorParametersAndProperties(var a: Int) {
    var b: Int = -1
  }

  @Test fun immutableConstructorParameters() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ImmutableConstructorParameters::class.java)

    val encoded = ImmutableConstructorParameters(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ImmutableConstructorParameters(val a: Int, val b: Int)

  @Test fun immutableProperties() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ImmutableProperties::class.java)

    val encoded = ImmutableProperties(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":3,"b":5}""")!!
    assertThat(decoded.a).isEqualTo(3)
    assertThat(decoded.b).isEqualTo(5)
  }

  @JsonClass(generateAdapter = true)
  class ImmutableProperties(a: Int, b: Int) {
    val a = a
    val b = b
  }

  @Test fun constructorDefaults() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ConstructorDefaultValues::class.java)

    val encoded = ConstructorDefaultValues(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"b":6}""")!!
    assertThat(decoded.a).isEqualTo(-1)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ConstructorDefaultValues(var a: Int = -1, var b: Int = -2)

  @Test fun requiredValueAbsent() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(RequiredValueAbsent::class.java)

    try {
      jsonAdapter.fromJson("""{"a":4}""")
      fail()
    } catch(expected: JsonDataException) {
      assertThat(expected).hasMessage("Required property 'b' missing at \$")
    }
  }

  @JsonClass(generateAdapter = true)
  class RequiredValueAbsent(var a: Int = 3, var b: Int)

  @Test fun nonNullConstructorParameterCalledWithNullFailsWithJsonDataException() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(HasNonNullConstructorParameter::class.java)

    try {
      jsonAdapter.fromJson("{\"a\":null}")
      fail()
    } catch (expected: JsonDataException) {
      assertThat(expected).hasMessage("Non-null value 'a' was null at \$.a")
    }
  }

  @Test fun nonNullConstructorParameterCalledWithNullFromAdapterFailsWithJsonDataException() {
    val moshi = Moshi.Builder().add(object {
      @FromJson fun fromJson(string: String): String? = null
    }).build()
    val jsonAdapter = moshi.adapter(HasNonNullConstructorParameter::class.java)

    try {
      jsonAdapter.fromJson("{\"a\":\"hello\"}")
      fail()
    } catch (expected: JsonDataException) {
      assertThat(expected).hasMessage("Non-null value 'a' was null at \$.a")
    }
  }

  @JsonClass(generateAdapter = true)
  data class HasNonNullConstructorParameter(val a: String)

  @JsonClass(generateAdapter = true)
  data class HasNullableConstructorParameter(val a: String?)

  @Test fun explicitNull() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ExplicitNull::class.java)

    val encoded = ExplicitNull(null, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"b":5}""")
    assertThat(jsonAdapter.serializeNulls().toJson(encoded)).isEqualTo("""{"a":null,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":null,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(null)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ExplicitNull(var a: Int?, var b: Int?)

  @Test fun absentNull() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(AbsentNull::class.java)

    val encoded = AbsentNull(null, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"b":5}""")
    assertThat(jsonAdapter.serializeNulls().toJson(encoded)).isEqualTo("""{"a":null,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"b":6}""")!!
    assertThat(decoded.a).isNull()
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class AbsentNull(var a: Int?, var b: Int?)

  @Test fun constructorParameterWithQualifier() {
    val moshi = Moshi.Builder()
        .add(UppercaseJsonAdapter())
        .build()
    val jsonAdapter = moshi.adapter(ConstructorParameterWithQualifier::class.java)

    val encoded = ConstructorParameterWithQualifier("Android", "Banana")
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":"ANDROID","b":"Banana"}""")

    val decoded = jsonAdapter.fromJson("""{"a":"Android","b":"Banana"}""")!!
    assertThat(decoded.a).isEqualTo("android")
    assertThat(decoded.b).isEqualTo("Banana")
  }

  @JsonClass(generateAdapter = true)
  class ConstructorParameterWithQualifier(@Uppercase(inFrench = true) var a: String, var b: String)

  @Test fun propertyWithQualifier() {
    val moshi = Moshi.Builder()
        .add(UppercaseJsonAdapter())
        .build()
    val jsonAdapter = moshi.adapter(PropertyWithQualifier::class.java)

    val encoded = PropertyWithQualifier()
    encoded.a = "Android"
    encoded.b = "Banana"
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":"ANDROID","b":"Banana"}""")

    val decoded = jsonAdapter.fromJson("""{"a":"Android","b":"Banana"}""")!!
    assertThat(decoded.a).isEqualTo("android")
    assertThat(decoded.b).isEqualTo("Banana")
  }

  @JsonClass(generateAdapter = true)
  class PropertyWithQualifier {
    @Uppercase(inFrench = true) var a: String = ""
    var b: String = ""
  }

  @Test fun constructorParameterWithJsonName() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ConstructorParameterWithJsonName::class.java)

    val encoded = ConstructorParameterWithJsonName(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"key a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"key a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ConstructorParameterWithJsonName(@Json(name = "key a") var a: Int, var b: Int)

  @Test fun propertyWithJsonName() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(PropertyWithJsonName::class.java)

    val encoded = PropertyWithJsonName()
    encoded.a = 3
    encoded.b = 5
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"key a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"key a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class PropertyWithJsonName {
    @Json(name = "key a") var a: Int = -1
    var b: Int = -1
  }

  @Test fun transientConstructorParameter() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(TransientConstructorParameter::class.java)

    val encoded = TransientConstructorParameter(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(-1)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class TransientConstructorParameter(@Transient var a: Int = -1, var b: Int = -1)

  @Test fun transientProperty() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(TransientProperty::class.java)

    val encoded = TransientProperty()
    encoded.a = 3
    encoded.setB(4)
    encoded.c = 5
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"c":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":5,"c":6}""")!!
    assertThat(decoded.a).isEqualTo(-1)
    assertThat(decoded.getB()).isEqualTo(-1)
    assertThat(decoded.c).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class TransientProperty {
    @Transient var a: Int = -1
    @Transient private var b: Int = -1
    var c: Int = -1

    fun getB() = b

    fun setB(b: Int) {
      this.b = b
    }
  }

  @Test fun nonNullPropertySetToNullFailsWithJsonDataException() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(HasNonNullProperty::class.java)

    try {
      jsonAdapter.fromJson("{\"a\":null}")
      fail()
    } catch (expected: JsonDataException) {
      assertThat(expected).hasMessage("Non-null value 'a' was null at \$.a")
    }
  }

  @Test fun nonNullPropertySetToNullFromAdapterFailsWithJsonDataException() {
    val moshi = Moshi.Builder().add(object {
      @FromJson fun fromJson(string: String): String? = null
    }).build()
    val jsonAdapter = moshi.adapter(HasNonNullProperty::class.java)

    try {
      jsonAdapter.fromJson("{\"a\":\"hello\"}")
      fail()
    } catch (expected: JsonDataException) {
      assertThat(expected).hasMessage("Non-null value 'a' was null at \$.a")
    }
  }

  @JsonClass(generateAdapter = true)
  class HasNonNullProperty {
    var a: String = ""
  }

  @Test fun manyProperties32() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ManyProperties32::class.java)

    val encoded = ManyProperties32(
        101, 102, 103, 104, 105,
        106, 107, 108, 109, 110,
        111, 112, 113, 114, 115,
        116, 117, 118, 119, 120,
        121, 122, 123, 124, 125,
        126, 127, 128, 129, 130,
        131, 132)
    val json = ("""
        |{
        |"v01":101,"v02":102,"v03":103,"v04":104,"v05":105,
        |"v06":106,"v07":107,"v08":108,"v09":109,"v10":110,
        |"v11":111,"v12":112,"v13":113,"v14":114,"v15":115,
        |"v16":116,"v17":117,"v18":118,"v19":119,"v20":120,
        |"v21":121,"v22":122,"v23":123,"v24":124,"v25":125,
        |"v26":126,"v27":127,"v28":128,"v29":129,"v30":130,
        |"v31":131,"v32":132
        |}
        |""").trimMargin().replace("\n", "")

    assertThat(jsonAdapter.toJson(encoded)).isEqualTo(json)

    val decoded = jsonAdapter.fromJson(json)!!
    assertThat(decoded.v01).isEqualTo(101)
    assertThat(decoded.v32).isEqualTo(132)
  }

  @JsonClass(generateAdapter = true)
  class ManyProperties32(
    var v01: Int, var v02: Int, var v03: Int, var v04: Int, var v05: Int,
    var v06: Int, var v07: Int, var v08: Int, var v09: Int, var v10: Int,
    var v11: Int, var v12: Int, var v13: Int, var v14: Int, var v15: Int,
    var v16: Int, var v17: Int, var v18: Int, var v19: Int, var v20: Int,
    var v21: Int, var v22: Int, var v23: Int, var v24: Int, var v25: Int,
    var v26: Int, var v27: Int, var v28: Int, var v29: Int, var v30: Int,
    var v31: Int, var v32: Int)

  @Test fun manyProperties33() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ManyProperties33::class.java)

    val encoded = ManyProperties33(
        101, 102, 103, 104, 105,
        106, 107, 108, 109, 110,
        111, 112, 113, 114, 115,
        116, 117, 118, 119, 120,
        121, 122, 123, 124, 125,
        126, 127, 128, 129, 130,
        131, 132, 133)
    val json = ("""
        |{
        |"v01":101,"v02":102,"v03":103,"v04":104,"v05":105,
        |"v06":106,"v07":107,"v08":108,"v09":109,"v10":110,
        |"v11":111,"v12":112,"v13":113,"v14":114,"v15":115,
        |"v16":116,"v17":117,"v18":118,"v19":119,"v20":120,
        |"v21":121,"v22":122,"v23":123,"v24":124,"v25":125,
        |"v26":126,"v27":127,"v28":128,"v29":129,"v30":130,
        |"v31":131,"v32":132,"v33":133
        |}
        |""").trimMargin().replace("\n", "")

    assertThat(jsonAdapter.toJson(encoded)).isEqualTo(json)

    val decoded = jsonAdapter.fromJson(json)!!
    assertThat(decoded.v01).isEqualTo(101)
    assertThat(decoded.v32).isEqualTo(132)
    assertThat(decoded.v33).isEqualTo(133)
  }

  @JsonClass(generateAdapter = true)
  class ManyProperties33(
    var v01: Int, var v02: Int, var v03: Int, var v04: Int, var v05: Int,
    var v06: Int, var v07: Int, var v08: Int, var v09: Int, var v10: Int,
    var v11: Int, var v12: Int, var v13: Int, var v14: Int, var v15: Int,
    var v16: Int, var v17: Int, var v18: Int, var v19: Int, var v20: Int,
    var v21: Int, var v22: Int, var v23: Int, var v24: Int, var v25: Int,
    var v26: Int, var v27: Int, var v28: Int, var v29: Int, var v30: Int,
    var v31: Int, var v32: Int, var v33: Int)

  @Test fun unsettablePropertyIgnored() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(UnsettableProperty::class.java)

    val encoded = UnsettableProperty()
    encoded.b = 5
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(-1)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class UnsettableProperty {
    val a: Int = -1
    var b: Int = -1
  }

  @Test fun getterOnlyNoBackingField() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(GetterOnly::class.java)

    val encoded = GetterOnly(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
    assertThat(decoded.total).isEqualTo(10)
  }

  @JsonClass(generateAdapter = true)
  class GetterOnly(var a: Int, var b: Int) {
    val total : Int
      get() = a + b
  }

  @Test fun getterAndSetterNoBackingField() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(GetterAndSetter::class.java)

    val encoded = GetterAndSetter(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5,"total":8}""")

    // Whether b is 6 or 7 is an implementation detail. Currently we call constructors then setters.
    val decoded1 = jsonAdapter.fromJson("""{"a":4,"b":6,"total":11}""")!!
    assertThat(decoded1.a).isEqualTo(4)
    assertThat(decoded1.b).isEqualTo(7)
    assertThat(decoded1.total).isEqualTo(11)

    // Whether b is 6 or 7 is an implementation detail. Currently we call constructors then setters.
    val decoded2 = jsonAdapter.fromJson("""{"a":4,"total":11,"b":6}""")!!
    assertThat(decoded2.a).isEqualTo(4)
    assertThat(decoded2.b).isEqualTo(7)
    assertThat(decoded2.total).isEqualTo(11)
  }

  @JsonClass(generateAdapter = true)
  class GetterAndSetter(var a: Int, var b: Int) {
    var total : Int
      get() = a + b
      set(value) {
        b = value - a
      }
  }

  @Test fun supertypeConstructorParameters() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(SubtypeConstructorParameters::class.java)

    val encoded = SubtypeConstructorParameters(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  open class SupertypeConstructorParameters(var a: Int)

  @JsonClass(generateAdapter = true)
  class SubtypeConstructorParameters(a: Int, var b: Int) : SupertypeConstructorParameters(a)

  @Test fun supertypeProperties() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(SubtypeProperties::class.java)

    val encoded = SubtypeProperties()
    encoded.a = 3
    encoded.b = 5
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"b":5,"a":3}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  open class SupertypeProperties {
    var a: Int = -1
  }

  @JsonClass(generateAdapter = true)
  class SubtypeProperties : SupertypeProperties() {
    var b: Int = -1
  }

  /** Generated adapters don't track enough state to detect duplicated values. */
  @Ignore @Test fun duplicatedValueParameter() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(DuplicateValueParameter::class.java)

    try {
      jsonAdapter.fromJson("""{"a":4,"a":4}""")
      fail()
    } catch(expected: JsonDataException) {
      assertThat(expected).hasMessage("Multiple values for 'a' at $.a")
    }
  }

  class DuplicateValueParameter(var a: Int = -1, var b: Int = -2)

  /** Generated adapters don't track enough state to detect duplicated values. */
  @Ignore @Test fun duplicatedValueProperty() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(DuplicateValueProperty::class.java)

    try {
      jsonAdapter.fromJson("""{"a":4,"a":4}""")
      fail()
    } catch(expected: JsonDataException) {
      assertThat(expected).hasMessage("Multiple values for 'a' at $.a")
    }
  }

  class DuplicateValueProperty {
    var a: Int = -1
    var b: Int = -2
  }

  @Test fun extensionProperty() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ExtensionProperty::class.java)

    val encoded = ExtensionProperty(3)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
  }

  @JsonClass(generateAdapter = true)
  class ExtensionProperty(var a: Int)

  var ExtensionProperty.b: Int
    get() {
      throw AssertionError()
    }
    set(value) {
      throw AssertionError()
    }

  /** https://github.com/square/moshi/issues/563 */
  @Test fun qualifiedAdaptersAreShared() {
    val moshi = Moshi.Builder()
        .add(UppercaseJsonAdapter())
        .build()
    val jsonAdapter = moshi.adapter(MultiplePropertiesShareAdapter::class.java)

    val encoded = MultiplePropertiesShareAdapter("Android", "Banana")
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":"ANDROID","b":"BANANA"}""")

    val delegateAdapters = GeneratedAdaptersTest_MultiplePropertiesShareAdapterJsonAdapter::class
        .memberProperties.filter {
      it.returnType.classifier == JsonAdapter::class
    }
    assertThat(delegateAdapters).hasSize(1)
  }

  @JsonClass(generateAdapter = true)
  class MultiplePropertiesShareAdapter(
    @Uppercase(true) var a: String,
    @Uppercase(true) var b: String
  )

  @Test fun toJsonOnly() {
    val moshi = Moshi.Builder()
        .add(CustomToJsonOnlyAdapter())
        .build()
    val jsonAdapter = moshi.adapter(CustomToJsonOnly::class.java)

    assertThat(jsonAdapter.toJson(CustomToJsonOnly(1, 2))).isEqualTo("""[1,2]""")

    val fromJson = jsonAdapter.fromJson("""{"a":3,"b":4}""")!!
    assertThat(fromJson.a).isEqualTo(3)
    assertThat(fromJson.b).isEqualTo(4)
  }

  @JsonClass(generateAdapter = true)
  class CustomToJsonOnly(var a: Int, var b: Int)

  class CustomToJsonOnlyAdapter {
    @ToJson fun toJson(v: CustomToJsonOnly) : List<Int> {
      return listOf(v.a, v.b)
    }
  }

  @Test fun fromJsonOnly() {
    val moshi = Moshi.Builder()
        .add(CustomFromJsonOnlyAdapter())
        .build()
    val jsonAdapter = moshi.adapter(CustomFromJsonOnly::class.java)

    assertThat(jsonAdapter.toJson(CustomFromJsonOnly(1, 2))).isEqualTo("""{"a":1,"b":2}""")

    val fromJson = jsonAdapter.fromJson("""[3,4]""")!!
    assertThat(fromJson.a).isEqualTo(3)
    assertThat(fromJson.b).isEqualTo(4)
  }

  @JsonClass(generateAdapter = true)
  class CustomFromJsonOnly(var a: Int, var b: Int)

  class CustomFromJsonOnlyAdapter {
    @FromJson fun fromJson(v: List<Int>) : CustomFromJsonOnly {
      return CustomFromJsonOnly(v[0], v[1])
    }
  }

  @Test fun privateTransientIsIgnored() {
    val jsonAdapter = moshi.adapter(PrivateTransient::class.java)

    val privateTransient = PrivateTransient()
    privateTransient.writeA(1)
    privateTransient.b = 2
    assertThat(jsonAdapter.toJson(privateTransient)).isEqualTo("""{"b":2}""")

    val fromJson = jsonAdapter.fromJson("""{"a":3,"b":4}""")!!
    assertThat(fromJson.readA()).isEqualTo(-1)
    assertThat(fromJson.b).isEqualTo(4)
  }

  @JsonClass(generateAdapter = true)
  class PrivateTransient {
    @Transient private var a: Int = -1
    var b: Int = -1

    fun readA(): Int {
      return a
    }

    fun writeA(a: Int) {
      this.a = a
    }
  }

  @Test fun propertyIsNothing() {
    val moshi = Moshi.Builder()
        .add(NothingAdapter())
        .build()
    val jsonAdapter = moshi.adapter(HasNothingProperty::class.java).serializeNulls()

    val toJson = HasNothingProperty()
    toJson.a = "1"
    assertThat(jsonAdapter.toJson(toJson)).isEqualTo("""{"a":"1","b":null}""")

    val fromJson = jsonAdapter.fromJson("""{"a":"3","b":null}""")!!
    assertThat(fromJson.a).isEqualTo("3")
    assertNull(fromJson.b)
  }

  class NothingAdapter {
    @ToJson fun toJson(jsonWriter: JsonWriter, unused: Nothing?) {
      jsonWriter.nullValue()
    }

    @FromJson fun fromJson(jsonReader: JsonReader) : Nothing? {
      jsonReader.skipValue()
      return null
    }
  }

  @JsonClass(generateAdapter = true)
  class HasNothingProperty {
    var a: String? = null
    var b: Nothing? = null
  }

  @Test fun enclosedParameterizedType() {
    val jsonAdapter = moshi.adapter(HasParameterizedProperty::class.java)

    assertThat(jsonAdapter.toJson(HasParameterizedProperty(Twins("1", "2"))))
        .isEqualTo("""{"twins":{"a":"1","b":"2"}}""")

    val hasParameterizedProperty = jsonAdapter.fromJson("""{"twins":{"a":"3","b":"4"}}""")!!
    assertThat(hasParameterizedProperty.twins.a).isEqualTo("3")
    assertThat(hasParameterizedProperty.twins.b).isEqualTo("4")
  }

  @JsonClass(generateAdapter = true)
  class Twins<T>(var a: T, var b: T)

  @JsonClass(generateAdapter = true)
  class HasParameterizedProperty(val twins: Twins<String>)

  @Test fun uppercasePropertyName() {
    val adapter = moshi.adapter(UppercasePropertyName::class.java)

    val instance = adapter.fromJson("""{"AAA":1,"BBB":2}""")!!
    assertThat(instance.AAA).isEqualTo(1)
    assertThat(instance.BBB).isEqualTo(2)

    assertThat(adapter.toJson(UppercasePropertyName(3, 4))).isEqualTo("""{"AAA":3,"BBB":4}""")
  }

  @JsonClass(generateAdapter = true)
  class UppercasePropertyName(val AAA: Int, val BBB: Int)

  /** https://github.com/square/moshi/issues/574 */
  @Test fun mutableUppercasePropertyName() {
    val adapter = moshi.adapter(MutableUppercasePropertyName::class.java)

    val instance = adapter.fromJson("""{"AAA":1,"BBB":2}""")!!
    assertThat(instance.AAA).isEqualTo(1)
    assertThat(instance.BBB).isEqualTo(2)

    val value = MutableUppercasePropertyName()
    value.AAA = 3
    value.BBB = 4
    assertThat(adapter.toJson(value)).isEqualTo("""{"AAA":3,"BBB":4}""")
  }

  @JsonClass(generateAdapter = true)
  class MutableUppercasePropertyName {
    var AAA: Int = -1
    var BBB: Int = -1
  }

  @JsonQualifier
  annotation class Uppercase(val inFrench: Boolean, val onSundays: Boolean = false)

  class UppercaseJsonAdapter {
    @ToJson fun toJson(@Uppercase(inFrench = true) s: String) : String {
      return s.toUpperCase(Locale.US)
    }
    @FromJson @Uppercase(inFrench = true) fun fromJson(s: String) : String {
      return s.toLowerCase(Locale.US)
    }
  }

  @JsonClass(generateAdapter = true)
  data class HasNullableBoolean(val boolean: Boolean?)

  @Test fun nullablePrimitivesUseBoxedPrimitiveAdapters() {
    val moshi = Moshi.Builder()
        .add(JsonAdapter.Factory { type, _, _ ->
          if (Boolean::class.javaObjectType == type) {
            return@Factory object:JsonAdapter<Boolean?>() {
              override fun fromJson(reader: JsonReader): Boolean? {
                if (reader.peek() != JsonReader.Token.BOOLEAN) {
                  reader.skipValue()
                  return null
                }
                return reader.nextBoolean()
              }

              override fun toJson(writer: JsonWriter, value: Boolean?) {
                writer.value(value)
              }
            }
          }
          null
        })
        .build()
    val adapter = moshi.adapter(HasNullableBoolean::class.java).serializeNulls()
    assertThat(adapter.fromJson("""{"boolean":"not a boolean"}"""))
        .isEqualTo(HasNullableBoolean(null))
    assertThat(adapter.toJson(HasNullableBoolean(null))).isEqualTo("""{"boolean":null}""")
  }

  @Test fun adaptersAreNullSafe() {
    val moshi = Moshi.Builder().build()
    val adapter = moshi.adapter(HasNonNullConstructorParameter::class.java)
    assertThat(adapter.fromJson("null")).isNull()
    assertThat(adapter.toJson(null)).isEqualTo("null")
  }

  @Retention(AnnotationRetention.RUNTIME)
  annotation class Nullable

  @Test fun delegatesToInstalledAdaptersBeforeNullChecking() {
    val moshi = Moshi.Builder()
        .add(object {
          @FromJson fun fromJson(@Nullable string: String?): String {
            return string ?: "fallback"
          }

          @ToJson fun toJson(@Nullable value: String?): String {
            return value ?: "fallback"
          }
        })
        .build()

    val hasNonNullConstructorParameterAdapter =
        moshi.adapter(HasNonNullConstructorParameter::class.java)
    assertThat(hasNonNullConstructorParameterAdapter
        .fromJson("{\"a\":null}")).isEqualTo(HasNonNullConstructorParameter("fallback"))

    val hasNullableConstructorParameterAdapter =
        moshi.adapter(HasNullableConstructorParameter::class.java)
    assertThat(hasNullableConstructorParameterAdapter
        .fromJson("{\"a\":null}")).isEqualTo(HasNullableConstructorParameter("fallback"))
    assertThat(hasNullableConstructorParameterAdapter
        .toJson(HasNullableConstructorParameter(null))).isEqualTo("{\"a\":\"fallback\"}")
  }

  @JsonClass(generateAdapter = true)
  data class HasNullableTypeWithGeneratedAdapter(val a: HasNonNullConstructorParameter?)

  @Test fun delegatesToInstalledAdaptersBeforeNullCheckingWithGeneratedAdapter() {
    val moshi = Moshi.Builder().build()
    val adapter = moshi.adapter(HasNullableTypeWithGeneratedAdapter::class.java)

    val encoded = HasNullableTypeWithGeneratedAdapter(null)
    assertThat(adapter.toJson(encoded)).isEqualTo("""{}""")
    assertThat(adapter.serializeNulls().toJson(encoded)).isEqualTo("""{"a":null}""")

    val decoded = adapter.fromJson("""{"a":null}""")!!
    assertThat(decoded.a).isEqualTo(null)
  }

  @JsonClass(generateAdapter = true)
  data class HasCollectionOfPrimitives(val listOfInts: List<Int>)

  @Test fun hasCollectionOfPrimitives() {
    val moshi = Moshi.Builder().build()
    val adapter = moshi.adapter(HasCollectionOfPrimitives::class.java)

    val encoded = HasCollectionOfPrimitives(listOf(1, 2, -3))
    assertThat(adapter.toJson(encoded)).isEqualTo("""{"listOfInts":[1,2,-3]}""")

    val decoded = adapter.fromJson("""{"listOfInts":[4,-5,6]}""")!!
    assertThat(decoded).isEqualTo(HasCollectionOfPrimitives(listOf(4, -5, 6)))
  }

  /**
   * This is here mostly just to ensure it still compiles. Covers variance, @Json, default values,
   * nullability, primitive arrays, and some wacky generics.
   */
  @JsonClass(generateAdapter = true)
  data class SmokeTestType(
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String,
    val age: Int,
    val nationalities: List<String> = emptyList(),
    val weight: Float,
    val tattoos: Boolean = false,
    val race: String?,
    val hasChildren: Boolean = false,
    val favoriteFood: String? = null,
    val favoriteDrink: String? = "Water",
    val wildcardOut: MutableList<out String> = mutableListOf(),
    val nullableWildcardOut: MutableList<out String?> = mutableListOf(),
    val wildcardIn: Array<in String>,
    val any: List<*>,
    val anyTwo: List<Any>,
    val anyOut: MutableList<out Any>,
    val nullableAnyOut: MutableList<out Any?>,
    val favoriteThreeNumbers: IntArray,
    val favoriteArrayValues: Array<String>,
    val favoriteNullableArrayValues: Array<String?>,
    val nullableSetListMapArrayNullableIntWithDefault: Set<List<Map<String, Array<IntArray?>>>>? = null,
    val aliasedName: TypeAliasName = "Woah",
    val genericAlias: GenericTypeAlias = listOf("Woah")
  )
}

typealias TypeAliasName = String
typealias GenericTypeAlias = List<String>
