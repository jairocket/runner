package com.runner.plugins

import com.runner.routes.healthRoute
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        healthRoute()
    }
}
