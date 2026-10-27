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
    data class Add(val id: String) : Screen()
}

class TripVM(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("trips", 0)
    var screen by mutableStateOf<Screen>(Screen.Home)
    var trips by mutableStateOf(load()); private set

    private fun load(): List<Trip> = try {
        val a = JSONArray(prefs.getString("data", "[]"))
        List(a.length()) { tripFrom(a.getJSONObject(it)) }
    } catch (e: Exception) { emptyList() }

    private fun save(l: List<Trip>) {
        trips = l
        prefs.edit().putString("data", JSONArray(l.map { it.toJson() }).toString()).apply()
    }

    fun addTrip(t: Trip) = save(listOf(t) + trips)
    fun deleteTrip(id: String) { save(trips.filter { it.id != id }); screen = Screen.Home }
    fun addExpense(tid: String, e: Expense) =
        save(trips.map { if (it.id == tid) it.copy(expenses = listOf(e) + it.expenses) else it })
    fun deleteExpense(tid: String, eid: String) =
        save(trips.map { if (it.id == tid) it.copy(expenses = it.expenses.filter { x -> x.id != eid }) else it })
}
