package elite.restapi

import elite.restapi.auth.AuthSession
import elite.restapi.auth.JwtService
import elite.restapi.auth.hash
import elite.restapi.db.DatabaseFactory
import elite.restapi.repository.RestRepository
import elite.restapi.routes.users
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.util.*
import org.slf4j.event.Level
import routes.todos

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    DatabaseFactory.init()
    val database = RestRepository()
    val jwtService = JwtService()
    val hashFun = { s: String -> hash(s) }

    install(Locations) {
    }

    install(Sessions) {
        cookie<AuthSession>("COOKIES")
    }

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(Authentication) {
        jwt {
            verifier(jwtService.verifier)
            realm = "MyServer"
            validate {
                val payload = it.payload
                val claim = payload.getClaim("id")
                val user = database.findUser(claim.asInt())
                user
            }
        }
    }

    install(ContentNegotiation) {
        gson {
        }
    }

    routing {
        users(database, jwtService, hashFun)
        todos(database)
    }
}

const val API_VERSION = "/v1"