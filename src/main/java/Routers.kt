import DataSource.doOrederRequest
import DataSource.getAllProduct
import DataSource.getCategories
import DataSource.getOrdersByCommades
import DataSource.getUser
import DataSource.prepareDatabase
import data.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Launcher
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler


fun main() {
    Launcher.executeCommand("run", BurgerStoreVecticle::class.java.getName())

}

class BurgerStoreVecticle : AbstractVerticle() {
    companion object {
    }

    var catalogue = Catalogue(ArrayList())
    val users = ArrayList<User>()
    val malek = User(1, "malek", command = ArrayList())

    init {
        users.add(malek)

    }


    private fun startHttpServer(): Future<Void> {
        val future = Future.future<Void>();

        val router = Router.router(vertx)
        router.get("/").handler {
            println(catalogue.toString())

            it.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(catalogue));
        }
        router.get("/category/:id").handler {
            val id = it.request().getParam("id")
            it.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(catalogue.categories.filter { it.id == id.toInt() }))
        }
        router.get("/user/:id").handler {
            val id = it.request().getParam("id").toInt()
            getUser(id).setHandler { asyncResult ->
                if (asyncResult.failed()) {
                    it.response().setStatusCode(400).end()
                } else {
                    it.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(asyncResult.result()))
                }
            }

        }

        router.route().handler(BodyHandler.create())
        router.route(HttpMethod.POST, "/neworder").handler { routingContext ->
            val newOrder = Json.decodeValue(routingContext.bodyAsString, OrdersRequest::class.java)
            if (newOrder == null) {
                routingContext.response().setStatusCode(400).end()
            } else {
                doOrederRequest(newOrder).setHandler {
                    if (it.failed()) {
                        routingContext.response().setStatusCode(400).end()
                    } else {
                        routingContext.response().setStatusCode(200).end()
                    }
                }
            }
        }


        router.get("/product/:id").handler(this::getProductById)
        router.get("/orders/:id").handler { routingContext ->
            val idCommand = routingContext.request().getParam("id")
            if (idCommand == null) {
                routingContext.response()
                    .setStatusCode(400).end()
            } else {
                try {
                    getOrdersByCommades(idCommand.toInt()).setHandler {
                        if (it.failed()) {
                            println(it.toString())
                            routingContext.response()
                                .setStatusCode(400).end()
                        } else {
                            routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encodePrettily(it.result()))
                        }
                    }
                } catch (e: Throwable) {
                    routingContext.response().setStatusCode(400).end();

                }


            }
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            {

                if (it.succeeded()) {
                    println("HTTP server running on port 8080");
                } else {
                    println("HTTP server running on port 8080")
                }
            }
        return future
    }

    override fun start(startFuture: Future<Void>?) {
        super.start(startFuture)
        prepareDatabase(vertx).compose {
            catalogue=it
            startHttpServer()
        }.setHandler(startFuture?.completer())
    }

    fun getProductById(routingContext: RoutingContext) {
        val id = routingContext.request().getParam("id")
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            try {
                return routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(catalogue.categories.flatMap { it.products }.first { it.id == id.toInt() }))
            } catch (e: NoSuchElementException) {
                return routingContext.response()
                    .setStatusCode(400).end()
            }

        }
    }


}


class Lunch {
    companion object {
        @JvmStatic
        fun lunch(args: Array<String>) {

        }
    }
}