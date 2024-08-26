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

    fun close() {
        connection?.close()
    }
}
