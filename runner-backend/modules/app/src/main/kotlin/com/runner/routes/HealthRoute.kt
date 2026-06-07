package com.runner.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.healthRoute() {
    get("/health") {
        call.respond(HttpStatusCode.OK)
    }
}
