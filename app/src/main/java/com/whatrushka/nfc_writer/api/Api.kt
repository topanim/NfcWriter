package com.whatrushka.nfc_writer.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class Api(
    private val client: HttpClient
) {
    companion object {
        const val DOMAIN = "http://45.155.207.232:1290"
        const val ADD_CARD = "$DOMAIN/api/user/card"
    }

    suspend fun add_card(user_id: Int): String {
        val response = client
            .post(ADD_CARD) {
                contentType(ContentType.Application.Json)
                parameter("user_id", user_id)
            }
        return Json.decodeFromString(response.body())
    }
}