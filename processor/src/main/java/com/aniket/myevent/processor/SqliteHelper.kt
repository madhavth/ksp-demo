package com.aniket.myevent.processor

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class SQLiteWrapper(private val dbPath: String) {

    private var connection: Connection? = null

    init {
        connect()
    }

    private fun connect() {
            Class.forName("org.sqlite.JDBC");
            val url = "jdbc:sqlite:$dbPath"
            connection = DriverManager.getConnection(url)
            println("Connection to SQLite has been established.")
    }

    fun listTables(): List<String> {
        val tables = mutableListOf<String>()
        val query = "SELECT name FROM sqlite_master WHERE type='table';"
        connection!!.createStatement()!!.use { statement ->
                val resultSet = statement.executeQuery(query)
                while (resultSet.next()) {
                    tables.add(resultSet.getString("name"))
                }
            }

        return tables
    }

    fun getCustomerList(): List<Customer> {
        val list = mutableListOf<Customer>()

        val query = "SELECT * from customers;"

        connection!!.createStatement()!!.use {
            statement ->
            val resultSet = statement.executeQuery(query)
            while(resultSet.next()) {
                list.add(
                    Customer(
                        id = resultSet.getInt("customer_id"),
                        firstName = resultSet.getString("first_name"),
                        lastName = resultSet.getString("last_name"),
                        houseHoldIncome = resultSet.getInt("household_income"),
                        phoneNumber = resultSet.getInt("phone_number"),
                        email = resultSet.getString("email")
                    )
                )
            }
        }

        return list
    }



    fun close() {
        connection?.close()
    }
}

data class Customer(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val houseHoldIncome: Int,
    val phoneNumber: Int,
    val email: String
)
