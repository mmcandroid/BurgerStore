import data.*
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient

object DataSource {
    lateinit var dbClient: JDBCClient

    fun prepareDatabase(vertx: Vertx): Future<Catalogue> {
        return getdbClient(vertx).compose {
            getAllProduct().compose {
                println(it.toString())
                getCategories(it)
            }.compose {
                println(it.toString())
                val future = Future.future<Catalogue>()
                val catalogue = Catalogue(it)
                println(catalogue)
                future.complete(catalogue)
                future
            }
        }

    }

    fun getdbClient(vertx: Vertx): Future<JDBCClient> {
        val future = Future.future<JDBCClient>();
        if (!::dbClient.isInitialized) {
            dbClient = JDBCClient.createShared(
                vertx, JsonObject()
                    .put("url", "jdbc:sqlite:C:/Users/malek/Burger.db")
                    .put("driver_class", "org.sqlite.JDBC")
                    .put("max_pool_size", 30)

            )
        }
        future.complete(dbClient)
        return future
    }

    fun getAllProduct(): Future<ArrayList<Product>> {
        val future = Future.future<ArrayList<Product>>()
        val products = ArrayList<Product>()

        dbClient.getConnection {
            if (it.failed()) {
                future.fail(it.cause())
            } else {
                it?.result()?.query("select * from product ") {
                    if (it.failed()) {
                        future.fail(it.cause())
                    } else {
                        it?.result()?.results?.let { productRaws ->
                            for (productraw in productRaws) {
                                products.add(
                                    Product(
                                        id = productraw.getInteger(0),
                                        name = productraw.getString(1),
                                        idCategory = productraw.getInteger(2),
                                        price = productraw.getInteger(3),
                                        image = productraw.getString(4),
                                        description = productraw.getString(5),
                                        available = productraw.getInteger(6) == 1
                                    )
                                )
                            }
                            println(products.toString())
                            future.complete(products)
                        }
                    }
                }
            }
        }

        return future
    }

    fun getOrdersByCommades(idCommand: Int): Future<ArrayList<Order>> {
        val future = Future.future<ArrayList<Order>>()
        dbClient.getConnection {
            if (it.failed()) {
                println(it.cause())
                future.fail(it.cause())
            } else {
                it?.result()?.query("select * from orders where `idCommande`=$idCommand") { result ->
                    if (result.failed()) {
                        println(it.cause())
                        future.fail(it.cause())
                    } else {
                        val jsonArray = result?.result()?.results
                        if (jsonArray != null) {
                            val orders = ArrayList<Order>()
                            var iteratration = jsonArray.size
                            if (jsonArray.isEmpty()) {
                                future.complete(orders)
                            } else {
                                for (row in jsonArray) {
                                    val order = Order(
                                        idProduct = row.getInteger(1),
                                        quantity = row.getInteger(2),
                                        idCommand = idCommand,
                                        id = row.getInteger(0)
                                    )
                                    orders.add(order)
                                    if (iteratration == 1) {
                                        future.complete(orders)
                                    }
                                    iteratration -= 1

                                }
                            }
                        } else {
                            future.fail("null")
                        }
                    }
                }
            }
        }
        return future
    }

    fun getUserCommades(idUser: Int): Future<ArrayList<Command>> {
        val future = Future.future<ArrayList<Command>>()
        val commandes = ArrayList<Command>()
        dbClient.getConnection {
            if (it.failed()) {
                println(it.cause())

                future.fail(it.cause())
            } else {
                it?.result()?.query("select * from commande where `idUser`==$idUser") { res ->
                    if (res.failed()) {
                        println(it.cause())
                        future.fail(res.cause())
                    } else {
                        val comandJsonResult = res.result()?.results
                        if (comandJsonResult == null) {
                            future.fail("null")
                        } else {
                            var iter = comandJsonResult.size
                            for (row in comandJsonResult) {
                                val command = Command(
                                    id = row.getInteger(0).toLong(),
                                    price = row.getInteger(2),
                                    orders = ArrayList()
                                )
                                getOrdersByCommades(idCommand = row.getInteger(1)).setHandler {
                                    if (it.failed()) {
                                        future.fail(it.cause())
                                    } else {
                                        command.orders.addAll(it.result())
                                        iter = iter - 1
                                        if (iter == 0) {
                                            future.complete(commandes)
                                        }

                                    }
                                }
                                commandes.add(command)
                            }
                        }
                    }
                }
            }
        }
        return future
    }

    fun doOrederRequest(ordersRequest: OrdersRequest): Future<Void> {
        val future = Future.future<Void>()
        addCommande(idUser = ordersRequest.idUser, price = ordersRequest.price).setHandler {
            addOrders(orders = ordersRequest.orders, idCommand = it.result()).setHandler {
                if (it.succeeded()) {
                    future.complete()
                } else {
                    future.fail(it.cause())
                }
            }
        }




        return future
    }

    fun addCommande(idUser: Int, price: Int): Future<Int> {
        val future = Future.future<Int>()
        dbClient.getConnection { sqlConnection ->
            if (sqlConnection.failed()) {
                println(sqlConnection.cause())
                future.fail(sqlConnection.cause())
            } else {
                sqlConnection.result().update("insert into commande values (null,$idUser,$price)") {
                    if (it.failed()) {
                        println(sqlConnection.cause())
                        future.fail(sqlConnection.cause())
                    } else {
                        future.complete(it.result().keys.getInteger(0))
                    }
                }


            }
        }
        return future
    }

    fun getCategories(products: ArrayList<Product>): Future<ArrayList<Category>> {
        val future = Future.future<ArrayList<Category>>()
        val categorys = ArrayList<Category>()

        dbClient.getConnection {
            it.result().query("select * from categorie") { res ->
                if (res.failed()) {
                    future.fail(res.cause())
                } else {
                    res?.result()?.results?.let { rows ->
                        for (row in rows) {
                            val catProd = ArrayList<Product>()
                            catProd.addAll(products.filter { it.idCategory == row.getInteger(0) })
                            categorys.add(
                                Category(
                                    name = row.getString(1),
                                    id = row.getInteger(0),
                                    image = row.getString(3),
                                    products = catProd
                                )
                            )
                        }
                        future.complete(categorys)
                    }
                }
            }

        }

        return future
    }

    fun addOrders(orders: ArrayList<Order>, idCommand: Int): Future<Void> {
        val future = Future.future<Void>()

        dbClient.getConnection { sqlConnection ->

            if (sqlConnection.failed()) {
                println(sqlConnection.cause())
                future.fail(sqlConnection.cause())
            } else {
                var iteration = orders.size
                for (order in orders) {
                    sqlConnection.result()
                        .update("insert into orders values (null,${order.idProduct},${order.quantity},$idCommand)") {
                            if (it.failed()) {
                                println(sqlConnection.cause())
                                future.fail(sqlConnection.cause())
                            } else {
                                iteration -= 1
                                if (iteration == 0) {
                                    future.complete()
                                }
                            }
                        }
                }
            }
        }
        return future
    }

    fun getUser(id: Int): Future<User> {
        val future = Future.future<User>()

        dbClient.getConnection {
            if (it.failed()) {
                future.fail(it.cause())
            } else {
                it.result().query("select * from user where `id`==$id") {
                    if (it.failed()) {
                        println(it.cause())
                        future.fail(it.cause())
                    } else {
                        it?.result()?.results?.let {
                            if (it.isEmpty()) {
                                future.fail("is empty")
                            } else {
                                val user = User(
                                    id = it[0].getInteger(0),
                                    name = it[0].getString(1),
                                    command = ArrayList()
                                )

                                getUserCommades(it[0].getInteger(0)).setHandler {
                                    if (it.failed()) {
                                        future.fail(it.cause())
                                    } else {
                                        user.command.addAll(it.result())
                                        future.complete(
                                            user
                                        )
                                    }
                                }

                            }

                        }
                    }
                }
            }
        }



        return future
    }


}