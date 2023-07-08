import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.PathMatchers.longSegment;

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

data class Item(val name: String, val id: Long)
data class Order(val items: List<Item>)
// (fake) async database query api
private fun fetchItem(itemId: Long): CompletionStage<Item?>{
    return CompletableFuture.completedFuture(Item("foo", itemId))
}

// (fake) async database query api
private fun saveOrder(order: Order): CompletionStage<Done> {
    return CompletableFuture.completedFuture(Done.getInstance())
}
class Routes: AllDirectives() {

    fun createRoute(): Route? {
        return concat(
            get {
                pathPrefix("item") {
                    path(longSegment()) { id ->
                        return@path onSuccess(fetchItem(id)) { maybeItem ->
                            return@onSuccess maybeItem?.let { item ->
                                completeOK(item, Jackson.marshaller<Item>())
                            } ?: complete(StatusCodes.NOT_FOUND, "Not found")
                        }
                    }
                }
            },
            post {
                path("create-order") {
                    entity(Jackson.unmarshaller(Order::class.java)) { order ->
                        return@entity onSuccess(saveOrder(order)) { _ ->
                            complete("Order created")
                        }
                    }
                }
            }
        )
    }
}
