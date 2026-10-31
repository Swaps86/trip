package com.tripsplit.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray

sealed class Screen {
    object Home : Screen()
    object Create : Screen()
    data class Detail(val id: String) : Screen()
    data class ExpenseView(val id: String, val eid: String) : Screen()
    data class Form(val id: String, val eid: String?) : Screen()
    data class Toll(val id: String) : Screen()
}

class TripVM(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("trips", 0)
    var screen by mutableStateOf<Screen>(Screen.Home)
    var trips by mutableStateOf(load()); private set
    var customCats by mutableStateOf(loadCats()); private set

    val allCats: List<String> get() = CATEGORIES + customCats

    private fun load(): List<Trip> = try {
        val a = JSONArray(prefs.getString("data", "[]"))
        List(a.length()) { tripFrom(a.getJSONObject(it)) }
    } catch (e: Exception) { emptyList() }

    private fun loadCats(): List<String> = try {
        JSONArray(prefs.getString("cats", "[]")).strings()
    } catch (e: Exception) { emptyList() }

    private fun save(l: List<Trip>) {
        trips = l
        prefs.edit().putString("data", JSONArray(l.map { it.toJson() }).toString()).apply()
    }

    fun addTrip(t: Trip) = save(listOf(t) + trips)
    fun deleteTrip(id: String) { save(trips.filter { it.id != id }); screen = Screen.Home }

    fun addTraveler(tid: String, name: String) =
        save(trips.map { if (it.id == tid && name !in it.travelers) it.copy(travelers = it.travelers + name) else it })

    fun addCategory(name: String) {
        if (name.isBlank() || allCats.any { it.equals(name, true) }) return
        customCats = customCats + name
        prefs.edit().putString("cats", JSONArray(customCats).toString()).apply()
    }

    /** Adds a new expense or replaces the one with the same id. */
    fun saveExpense(tid: String, e: Expense) = save(trips.map { t ->
        if (t.id != tid) t else t.copy(
            expenses = if (t.expenses.any { it.id == e.id }) t.expenses.map { if (it.id == e.id) e else it }
            else listOf(e) + t.expenses
        )
    })

    fun deleteExpense(tid: String, eid: String) =
        save(trips.map { if (it.id == tid) it.copy(expenses = it.expenses.filter { x -> x.id != eid }) else it })
}
