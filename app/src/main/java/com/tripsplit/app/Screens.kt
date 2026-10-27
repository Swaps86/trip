package com.tripsplit.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val Bg = Color(0xFFF6F7F4)
private val Green = Color(0xFF0F6B5C)
private val Muted = Color(0xFF55605A)
private val Owe = Color(0xFFA33A1A)
private val Get = Color(0xFF0B6B3A)

fun inr(d: Double): String {
    val f = NumberFormat.getNumberInstance(Locale("en", "IN"))
    f.maximumFractionDigits = 2; f.minimumFractionDigits = 0
    return (if (d < 0) "-₹" else "₹") + f.format(Math.abs(d))
}

fun fmtDate(ms: Long): String {
    val f = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH); f.timeZone = TimeZone.getTimeZone("UTC")
    return f.format(Date(ms))
}

fun todayUtc(): Long {
    val c = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

@Composable
private fun WhiteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }

@Composable
private fun Progress(spent: Double, budget: Double) {
    val p = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0).toFloat() else 0f
    LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(8.dp),
        color = if (budget > 0 && spent > budget) Owe else Green)
}

@Composable
private fun Header(title: String, sub: String? = null, onBack: () -> Unit, action: @Composable () -> Unit = {}) =
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (sub != null) Text(sub, color = Muted, fontSize = 13.sp)
        }
        action()
    }

// ---------- 1. Trip list ----------
@Composable
fun HomeScreen(vm: TripVM) {
    Scaffold(containerColor = Bg, floatingActionButton = {
        ExtendedFloatingActionButton(onClick = { vm.screen = Screen.Create },
            icon = { Icon(Icons.Default.Add, null) }, text = { Text("New trip") })
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            Text("My trips", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            if (vm.trips.isEmpty()) Text("No trips yet. Tap “New trip” to start.", color = Muted)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(vm.trips, key = { it.id }) { t ->
                    WhiteCard(Modifier.clickable { vm.screen = Screen.Detail(t.id) }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(fmtDate(t.start), color = Muted, fontSize = 13.sp)
                        }
                        Text("${t.destinations.joinToString().ifBlank { "No destinations" }} · ${t.travelers.size} travelers",
                            color = Muted, fontSize = 13.sp)
                        Progress(t.spent, t.budget)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Spent ${inr(t.spent)}", color = Muted, fontSize = 13.sp)
                            Text("Budget ${inr(t.budget)}", color = Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------- 2. Create trip ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) { Text("$label: ${fmtDate(value)}") }
    if (open) {
        val st = rememberDatePickerState(initialSelectedDateMillis = value)
        DatePickerDialog(onDismissRequest = { open = false }, confirmButton = {
            TextButton(onClick = { st.selectedDateMillis?.let(onChange); open = false }) { Text("OK") }
        }) { DatePicker(st) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipEditor(label: String, items: List<String>, onChange: (List<String>) -> Unit) {
    var text by remember { mutableStateOf("") }
    fun add() { val t = text.trim(); if (t.isNotEmpty() && t !in items) onChange(items + t); text = "" }
    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(label) }, singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = { IconButton(onClick = { add() }) { Icon(Icons.Default.Add, "Add") } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { add() }))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { name ->
            AssistChip(onClick = { onChange(items - name) }, label = { Text(name) },
                trailingIcon = { Icon(Icons.Default.Close, "Remove $name", Modifier.size(16.dp)) })
        }
    }
}

@Composable
fun CreateScreen(vm: TripVM) {
    var name by remember { mutableStateOf("") }
    var start by remember { mutableLongStateOf(todayUtc()) }
    var end by remember { mutableLongStateOf(todayUtc()) }
    var dests by remember { mutableStateOf(listOf<String>()) }
    var trav by remember { mutableStateOf(listOf<String>()) }
    var budget by remember { mutableStateOf("") }
    val ok = name.isNotBlank() && trav.isNotEmpty() && end >= start
    Scaffold(containerColor = Bg) { p ->
        Column(Modifier.padding(p).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Header("New trip", onBack = { vm.screen = Screen.Home })
            OutlinedTextField(name, { name = it }, label = { Text("Trip name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            DateField("Start", start, { start = it; if (end < it) end = it }, Modifier.fillMaxWidth())
            DateField("End", end, { end = it }, Modifier.fillMaxWidth())
            if (end < start) Text("End date must be after start date", color = Owe, fontSize = 13.sp)
            ChipEditor("Add destination", dests) { dests = it }
            ChipEditor("Add traveler (name)", trav) { trav = it }
            OutlinedTextField(budget, { budget = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Total budget (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Button(enabled = ok, modifier = Modifier.fillMaxWidth().height(52.dp), onClick = {
                vm.addTrip(Trip(name = name.trim(), start = start, end = end, destinations = dests,
                    travelers = trav, budget = budget.toDoubleOrNull() ?: 0.0))
                vm.screen = Screen.Home
            }) { Text("Create trip") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------- 3. Trip dashboard + split ----------
@Composable
fun DetailScreen(trip: Trip, vm: TripVM) {
    var tab by remember { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val bal = balances(trip)
    val pays = settlements(bal)
    val left = trip.budget - trip.spent
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text("Delete “${trip.name}”?") }, text = { Text("All its expenses will be removed.") },
        confirmButton = { TextButton(onClick = { vm.deleteTrip(trip.id) }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })

    Scaffold(containerColor = Bg, floatingActionButton = {
        ExtendedFloatingActionButton(onClick = { vm.screen = Screen.Add(trip.id) },
            icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add expense") })
    }) { p ->
        LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Header(trip.name, "${fmtDate(trip.start)} – ${fmtDate(trip.end)}", { vm.screen = Screen.Home }) {
                    IconButton(onClick = { confirm = true }) { Icon(Icons.Default.Delete, "Delete trip") }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Green)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Spent so far", color = Color.White, fontSize = 13.sp)
                        Text(inr(trip.spent), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        val p2 = if (trip.budget > 0) (trip.spent / trip.budget).coerceIn(0.0, 1.0).toFloat() else 0f
                        LinearProgressIndicator(progress = { p2 }, Modifier.fillMaxWidth().height(8.dp),
                            color = Color.White, trackColor = Color(0x664F9A8C))
                        Text(if (left >= 0) "${inr(left)} left of ${inr(trip.budget)}"
                             else "Over budget by ${inr(-left)} (budget ${inr(trip.budget)})",
                            color = Color.White, fontSize = 13.sp)
                    }
                }
            }
            item {
                TabRow(selectedTabIndex = tab, containerColor = Bg) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Expenses") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Split") })
                }
            }
            if (tab == 0) {
                val byCat = trip.expenses.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
                    .toList().sortedByDescending { it.second }
                if (byCat.isNotEmpty()) item {
                    WhiteCard {
                        byCat.forEach { (c, a) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(c, fontWeight = FontWeight.SemiBold)
                                Text("${inr(a)} · ${(a / trip.spent * 100).toInt()}%", color = Muted)
                            }
                        }
                    }
                }
                if (trip.expenses.isEmpty()) item { Text("No expenses yet. Tap “Add expense”.", color = Muted) }
                items(trip.expenses, key = { it.id }) { e ->
                    WhiteCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.category, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text("${e.paidBy} paid · ${fmtDate(e.date)}", color = Muted, fontSize = 13.sp)
                                if (e.note.isNotBlank()) Text(e.note, color = Muted, fontSize = 13.sp)
                            }
                            Text(inr(e.amount), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            IconButton(onClick = { vm.deleteExpense(trip.id, e.id) }) {
                                Icon(Icons.Default.Delete, "Delete expense")
                            }
                        }
                    }
                }
            } else {
                item {
                    val share = if (trip.travelers.isNotEmpty()) trip.spent / trip.travelers.size else 0.0
                    Text("Total ${inr(trip.spent)} · equal share ${inr(share)} per person", color = Muted, fontSize = 13.sp)
                }
                item {
                    WhiteCard {
                        trip.travelers.forEach { n ->
                            val paid = trip.expenses.filter { it.paidBy == n }.sumOf { it.amount }
                            val net = bal[n] ?: 0.0
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(n, fontWeight = FontWeight.SemiBold)
                                    Text("Paid ${inr(paid)}", color = Muted, fontSize = 13.sp)
                                }
                                Text(if (net > 0.005) "+${inr(net)}" else inr(net), fontWeight = FontWeight.Bold,
                                    color = if (net > 0.005) Get else if (net < -0.005) Owe else Muted)
                            }
                        }
                    }
                }
                item { Text("WHO PAYS WHOM", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                if (pays.isEmpty()) item { Text("All settled. Nobody owes anything.", color = Muted) }
                items(pays) { pm ->
                    WhiteCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${pm.from} pays ${pm.to}")
                            Text(inr(pm.amount), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (pays.isNotEmpty()) item {
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        val text = "${trip.name} – settle up\n" + pays.joinToString("\n") { "${it.from} pays ${it.to} ${inr(it.amount)}" }
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, text), "Share"))
                    }) { Text("Share summary (WhatsApp etc.)") }
                }
            }
        }
    }
}

// ---------- 4. Add expense ----------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddScreen(trip: Trip, vm: TripVM) {
    var amount by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(CATEGORIES[0]) }
    var date by remember { mutableLongStateOf(todayUtc()) }
    var paid by remember { mutableStateOf(trip.travelers.first()) }
    var split by remember { mutableStateOf(trip.travelers.toSet()) }
    var note by remember { mutableStateOf("") }
    val amt = amount.toDoubleOrNull() ?: 0.0
    Scaffold(containerColor = Bg) { p ->
        Column(Modifier.padding(p).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Header("Add expense", trip.name, onBack = { vm.screen = Screen.Detail(trip.id) })
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("Category", color = Muted, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORIES.forEach { c -> FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) }) }
            }
            DateField("Date", date, { date = it }, Modifier.fillMaxWidth())
            Text("Paid by", color = Muted, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                trip.travelers.forEach { n -> FilterChip(selected = paid == n, onClick = { paid = n }, label = { Text(n) }) }
            }
            Text("Split among", color = Muted, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                trip.travelers.forEach { n ->
                    FilterChip(selected = n in split, onClick = { split = if (n in split) split - n else split + n },
                        label = { Text(n) })
                }
            }
            if (split.isNotEmpty() && amt > 0)
                Text("Equal split · ${inr(amt / split.size)} each", color = Muted, fontSize = 13.sp)
            OutlinedTextField(note, { note = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = amt > 0 && split.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp), onClick = {
                vm.addExpense(trip.id, Expense(category = cat, amount = amt, date = date, note = note.trim(),
                    paidBy = paid, splitAmong = trip.travelers.filter { it in split }))
                vm.screen = Screen.Detail(trip.id)
            }) { Text("Save expense") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
