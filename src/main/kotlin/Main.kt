import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route
import java.util.concurrent.CompletionStage

@Throws(Exception::class)
fun main() {
    // boot up server using the route as defined below
    val system: ActorSystem<Void> = ActorSystem.create(Behaviors.empty(), "routes")
    val http: Http = Http.get(system)

    val routes: Routes = Routes()
    //In order to access all directives we need an instance where the routes are define.
    val binding: CompletionStage<ServerBinding> = http.newServerAt("localhost", 8080)
        .bind(routes.createRoute())
    println("Server online at http://localhost:8080/\nPress RETURN to stop...")
    System.`in`.read() // let it run until user presses return
    binding
        .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
        .thenAccept { _: Any? -> system.terminate() } // and shutdown when done
}

class Routes: AllDirectives() {

    fun createRoute(): Route? {
        return concat(
            path("hello") {
                get {
                    complete("<h1>Say hello to akka-http</h1>")
                }
            })
    }
}