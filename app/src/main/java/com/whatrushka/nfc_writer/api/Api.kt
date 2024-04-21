package com.whatrushka.nfc_writer.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CreateCardResponse(
    @SerialName("secret_key") val secretKey: String
)

class Api(
    private val client: HttpClient
) {
    companion object {
        private const val DOMAIN = "http://45.155.207.232:1290"
        const val ADD_CARD = "$DOMAIN/api/user/card"
    }

    suspend fun addCard(userId: Int): String {
        val response = client
            .post(ADD_CARD) {
                contentType(ContentType.Application.Json)
                parameter("user_id", userId)
            }
        return response.body<CreateCardResponse>().secretKey
    }
}