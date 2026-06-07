package com.runner.routes

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import com.runner.plugins.configureRouting
import com.runner.plugins.configureSerialization
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRouteTest {

    @Test
    fun `GET health returns 200`() = testApplication {
        application {
            configureSerialization()
            configureRouting()
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
