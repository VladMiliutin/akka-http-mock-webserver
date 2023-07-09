package xyz.vladm.akka_mock_server

import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.util.concurrent.CompletionStage

fun main() {
    val port = (System.getenv()["HTTP_PORT"] ?: "8080").toInt()
    val folder = System.getenv()["MOCK_FOLDER"] ?: "./examples"

    val mapper = ObjectMapper().registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, true)
            .configure(KotlinFeature.NullToEmptyMap, true)
            .configure(KotlinFeature.NullIsSameAsDefault, true)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val mocksExplorer = MocksExplorer(mapper)
    val mocks = mocksExplorer.findMocks(folder)

    val route = RouteBuilder(mapper).build(mocks)

    // boot up server using the route as defined below
    val system: ActorSystem<Void> = ActorSystem.create(Behaviors.empty(), "routes")
    val http: Http = Http.get(system)

    //In order to access all directives we need an instance where the routes are define.
    val binding: CompletionStage<ServerBinding> = http.newServerAt("localhost", port)
        .bind(route)
    println("Server online at http://localhost:$port/")
}
