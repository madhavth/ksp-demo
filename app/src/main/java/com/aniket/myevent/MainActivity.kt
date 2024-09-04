package com.aniket.myevent

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bookTicketButton: Button = findViewById(R.id.bookTicketButton)

        bookTicketButton.setOnClickListener {
            EventUtils.postEvent(
                TicketBookToClickedParamsEvent(
                    TicketBookToClickedParams(
                        eventName = "TicketBookClicked",
                        screenName = "MainActivity",
                        ticketNumber = 1223,
                        ticketAmount = "1220"
                    )
                )
            )
        }
    }
}


object ConcurrentModificationExceptionExample {
    @JvmStatic
    fun main(args: Array<String>) {
        val map: MutableMap<Int, String> = HashMap()

        // Populate the HashMap with some initial data
        map[1] = "one"
        map[2] = "two"
        map[3] = "three"

        // Thread 1: Iterates over the HashMap and prints the entries
        val thread1 = Thread {
            val iterator: Iterator<Map.Entry<Int, String>> =
                map.entries.iterator()
            while (iterator.hasNext()) {
                val entry =
                    iterator.next()
                println("Thread 1: Key: " + entry.key + ", Value: " + entry.value)


                // Simulate some delay to make concurrent modification likely
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

        // Thread 2: Modifies the HashMap while Thread 1 is iterating over it
        val thread2 = Thread {
            try {
                // Simulate delay to ensure Thread 1 starts iterating first
                Thread.sleep(50)
                println("Thread 2: Modifying the map.")
                map[4] =
                    "four" // This operation will cause ConcurrentModificationException in Thread 1
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        // Start both threads
        thread1.start()
        thread2.start()

        try {
            thread1.join()
            thread2.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        println("Final HashMap: $map")
    }
}


object HashMapModificationExceptionExample {
    @JvmStatic
    fun main(args: Array<String>) {
        val map: MutableMap<Int, String> = HashMap()

        // Populate the HashMap with some initial data
        map[1] = "one"
        map[2] = "two"

        // Thread 1: Iterates over the HashMap and prints the entries
        val thread1 = Thread {
            val iterator: Iterator<Map.Entry<Int, String>> =
                map.entries.iterator()
            while (iterator.hasNext()) {
                val entry =
                    iterator.next()
                println("Thread 1: Key: " + entry.key + ", Value: " + entry.value)

                // Simulate some delay to make concurrent modification likely
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

        // Thread 2: Modifies the HashMap while Thread 1 is iterating over it
        val thread2 = Thread {
            try {
                // Simulate delay to ensure Thread 1 starts iterating first
                Thread.sleep(10)
                println("Thread 2: Modifying the map.")
                map[3] =
                    "three" // This operation will likely cause ConcurrentModificationException in Thread 1
                map.remove(1) // Additional modification to increase the chance of exception
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        // Start both threads
        thread1.start()
        thread2.start()

        try {
            thread1.join()
            thread2.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        println("Final HashMap: $map")
    }
}


fun main() = runBlocking {
    // Create a HashMap
    val map = HashMap<Int, String>()

    // Populate the map with some initial data
    map[1] = "one"
    map[2] = "two"

    // Coroutine 1: Continuously adds entries to the map
    val job1 = launch(Dispatchers.Default) {
        while (true) {
            map[3] = "three"
            map.remove(1) // Removing an entry
            println("Coroutine 1 added and removed key 1")
            delay(100) // Simulate some delay
        }
    }

    // Coroutine 2: Continuously reads from the map
    val job2 = launch(Dispatchers.Default) {
        while (true) {
            val value = map[2]
            println("Coroutine 2 read key 2: $value")
            delay(100) // Simulate some delay
        }
    }

    // Run the coroutines for a short while
    delay(5000) // Let the coroutines run for 5 seconds

    // Cancel the coroutines to stop the infinite loops
    job1.cancelAndJoin()
    job2.cancelAndJoin()

    // Print the final state of the map
    println("Final map: $map")
}