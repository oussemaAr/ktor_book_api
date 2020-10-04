package elite.restapi.routes

import elite.restapi.API_VERSION
import elite.restapi.auth.AuthSession
import elite.restapi.auth.JwtService
import elite.restapi.repository.Repository
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*

const val USERS = "$API_VERSION/users"
const val USER_LOGIN = "$USERS/login"
const val USER_CREATE = "$USERS/create"

@KtorExperimentalLocationsAPI
@Location(USER_LOGIN)
class UserLoginRoute

@KtorExperimentalLocationsAPI
@Location(USER_CREATE)
class UserCreateRoute

@KtorExperimentalLocationsAPI
fun Route.users(
    repository: Repository,
    jwtService: JwtService,
    hashFunction: (String) -> String
) {
    post<UserLoginRoute> {
        val params = call.receive<Parameters>()
        val password = params["password"] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")
        val email = params["email"] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")
        val hash = hashFunction(password)

        try {
            val currentUser = repository.findUserByEmail(email)

            currentUser?.userId?.let {
                if (currentUser.passwordHash == hash) {
                    call.sessions.set(AuthSession(it))
                    call.respondText(jwtService.generateToken(currentUser))
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest, "Problems retrieving User"
                    )
                }
            }
        } catch (e: Throwable) {
            application.log.error("Failed to register user", e)
            call.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
        }
    }

    post<UserCreateRoute> {
        val params = call.receive<Parameters>()
        val password = params["password"] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Field")
        val displayName =
            params["displayName"] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")
        val email = params["email"] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")
        val hash = hashFunction(password)
        println("$email : $password : $displayName : $hash")

        try {
            val newUser = repository.addUser(email, displayName, hash) // 6
            newUser?.userId?.let {
                call.sessions.set(AuthSession(it))
                call.respondText(
                    jwtService.generateToken(newUser),
                    status = HttpStatusCode.Created
                )
            }
        } catch (e: Throwable) {
            application.log.error("Failed to register user", e)
            call.respond(HttpStatusCode.BadRequest, "Problems creating User")
        }
    }


}