package data

data class Product(
    val id: Int,
    val name: String,
    val image: String?,
    val price: Int,
    val description: String?,
    val available: Boolean,
    val idCategory: Int
)

data class User(val id: Int, val name: String, val command: ArrayList<Command>)
data class Command(val orders: ArrayList<Order>, val id: Long, var price: Int)
data class Order(val idProduct: Int, val quantity: Int, val id: Int, val idCommand: Int) {
    constructor() : this(0, 0, 0, 0)
}

data class OrdersRequest(val orders: ArrayList<Order>, val idUser: Int, val price: Int) {
    constructor() : this(idUser = 0, orders = ArrayList<Order>(), price = 0)
}

data class Category(val name: String, val image: String?, val products: ArrayList<Product>, val id: Int)
data class Catalogue(val categories: ArrayList<Category>)