package com.runner

import com.runner.plugins.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val env = dotenv { ignoreIfMissing = true }
    val port = env["PORT", "8080"].toInt()

    embeddedServer(Netty, port = port) {
        configureDI()
        configureSerialization()
        configureDatabase()
        configureRouting()
    }.start(wait = true)
}
