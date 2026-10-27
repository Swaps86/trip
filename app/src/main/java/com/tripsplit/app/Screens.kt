package com.tripsplit.app

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
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

// ---------- 3. Trip dashboard + report ----------
@Composable
fun DetailScreen(trip: Trip, vm: TripVM) {
    var tab by remember { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var addPerson by remember { mutableStateOf(false) }
    var view by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val left = trip.budget - trip.spent
    val groups = trip.expenses.groupBy { it.paidBy }.toList().sortedByDescending { g -> g.second.sumOf { it.amount } }
    val byCat = trip.expenses.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
        .toList().sortedByDescending { it.second }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text("Delete “${trip.name}”?") }, text = { Text("All its expenses will be removed.") },
        confirmButton = { TextButton(onClick = { vm.deleteTrip(trip.id) }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
    if (addPerson) {
        var nm by remember { mutableStateOf("") }
        val clean = nm.trim()
        AlertDialog(onDismissRequest = { addPerson = false }, title = { Text("Add person to trip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(nm, { nm = it }, label = { Text("Name") }, singleLine = true)
                    if (clean in trip.travelers) Text("This name is already in the trip", color = Owe, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = clean.isNotEmpty() && clean !in trip.travelers,
                    onClick = { vm.addTraveler(trip.id, clean); addPerson = false }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addPerson = false }) { Text("Cancel") } })
    }

    Scaffold(containerColor = Bg, floatingActionButton = {
        ExtendedFloatingActionButton(onClick = { vm.screen = Screen.Form(trip.id, null) },
            icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add expense") })
    }) { p ->
        LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Header(trip.name, "${fmtDate(trip.start)} – ${fmtDate(trip.end)}", { vm.screen = Screen.Home }) {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More options") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Delete trip") }, onClick = { menu = false; confirm = true })
                        }
                    }
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
                WhiteCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${trip.travelers.size} travelers", fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { addPerson = true }) { Text("+ Add person") }
                    }
                }
            }
            item {
                TabRow(selectedTabIndex = tab, containerColor = Bg) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Expenses") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Report") })
                }
            }
            if (tab == 0) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("View:", color = Muted, fontSize = 13.sp)
                        FilterChip(selected = view == 0, onClick = { view = 0 }, label = { Text("By date") })
                        FilterChip(selected = view == 1, onClick = { view = 1 }, label = { Text("By person") })
                    }
                }
                if (view == 0 && byCat.isNotEmpty()) item {
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
                else item { Text("Tap an expense to edit or delete it", color = Muted, fontSize = 13.sp) }
                items(if (view == 0) trip.expenses else emptyList(), key = { it.id }) { e ->
                    WhiteCard(Modifier.clickable { vm.screen = Screen.ExpenseView(trip.id, e.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.category, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text("${e.paidBy} paid · ${fmtDate(e.date)}", color = Muted, fontSize = 13.sp)
                                if (e.location.isNotBlank()) Text(e.location, color = Muted, fontSize = 13.sp)
                                if (e.note.isNotBlank()) Text(e.note, color = Muted, fontSize = 13.sp)
                            }
                            Text(inr(e.amount), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("  ›", color = Muted, fontSize = 20.sp)
                        }
                    }
                }
                if (view == 1) {
                    groups.forEach { (name, list) ->
                        val total = list.sumOf { it.amount }
                        item(key = "g_$name") {
                            WhiteCard {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        Text("${list.size} payment${if (list.size > 1) "s" else ""}", color = Muted, fontSize = 13.sp)
                                    }
                                    Text(inr(total), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                }
                                list.sortedBy { it.date }.forEach { e ->
                                    Row(Modifier.fillMaxWidth().clickable { vm.screen = Screen.ExpenseView(trip.id, e.id) }
                                        .padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${e.category} · ${fmtDate(e.date)}"); Text(inr(e.amount))
                                    }
                                }
                                if (list.size > 1) Text(list.joinToString(" + ") { inr(it.amount) } + " = " + inr(total),
                                    color = Muted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                item {
                    WhiteCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total spent", color = Muted, fontSize = 13.sp)
                                Text(inr(trip.spent), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Budget ${inr(trip.budget)}", color = Muted, fontSize = 13.sp)
                                val pct = if (trip.budget > 0) (trip.spent / trip.budget * 100).toInt() else 0
                                Text("$pct% used", fontWeight = FontWeight.Bold)
                            }
                        }
                        Progress(trip.spent, trip.budget)
                        Text(if (left >= 0) "Remaining: ${inr(left)}" else "Over budget by ${inr(-left)}",
                            color = Muted, fontSize = 13.sp)
                    }
                }
                if (byCat.isNotEmpty()) {
                    item { Text("BY CATEGORY", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    item {
                        WhiteCard {
                            byCat.forEach { (c, a) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(c); Text(inr(a), fontWeight = FontWeight.Bold)
                                }
                                Progress(a, trip.spent)
                            }
                        }
                    }
                    item { Text("PAID BY EACH PERSON", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    item {
                        WhiteCard {
                            groups.forEach { (name, list) ->
                                val tot = list.sumOf { it.amount }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(name, fontWeight = FontWeight.SemiBold)
                                        Text("${list.map { it.category }.distinct().joinToString(" + ")} · ${list.size} payment${if (list.size > 1) "s" else ""}",
                                            color = Muted, fontSize = 13.sp)
                                    }
                                    Text(inr(tot), fontWeight = FontWeight.Bold)
                                }
                                Progress(tot, trip.spent)
                            }
                            Text("A person's payments are added together", color = Muted, fontSize = 13.sp)
                        }
                    }
                    item { Text("EXPENSES (${trip.expenses.size})", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    item {
                        WhiteCard {
                            trip.expenses.sortedBy { it.date }.take(4).forEach { e ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(e.category, fontWeight = FontWeight.SemiBold)
                                        Text(fmtDate(e.date), color = Muted, fontSize = 13.sp)
                                    }
                                    Text(inr(e.amount), fontWeight = FontWeight.Bold)
                                }
                            }
                            if (trip.expenses.size > 4)
                                Text("+ ${trip.expenses.size - 4} more in the PDF", color = Muted, fontSize = 13.sp)
                        }
                    }
                }
                item {
                    Button(enabled = trip.expenses.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp),
                        onClick = { downloadPdf(ctx, trip) }) { Text("Download PDF") }
                }
                item {
                    OutlinedButton(enabled = trip.expenses.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp),
                        onClick = { shareText(ctx, summaryText(trip)) }) { Text("Share summary on WhatsApp") }
                }
                item {
                    OutlinedButton(enabled = trip.expenses.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp),
                        onClick = { sharePdf(ctx, trip) }) { Text("Share PDF on WhatsApp") }
                }
            }
        }
    }
}

// ---------- 4. Expense details (Edit / Delete) ----------
@Composable
fun ExpenseScreen(trip: Trip, e: Expense, vm: TripVM) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text("Delete this expense?") }, text = { Text("${e.category} · ${inr(e.amount)}") },
        confirmButton = {
            TextButton(onClick = { vm.deleteExpense(trip.id, e.id); vm.screen = Screen.Detail(trip.id) }) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
    val n = e.splitAmong.size.coerceAtLeast(1)
    Scaffold(containerColor = Bg) { p ->
        Column(Modifier.padding(p).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Header("Expense", trip.name, onBack = { vm.screen = Screen.Detail(trip.id) })
            WhiteCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(e.category, color = Green, fontWeight = FontWeight.SemiBold)
                    Text(inr(e.amount), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text(fmtDate(e.date), color = Muted)
                }
            }
            WhiteCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Paid by", color = Muted); Text(e.paidBy, fontWeight = FontWeight.SemiBold)
                }
            }
            if (e.location.isNotBlank()) WhiteCard {
                Text("LOCATION", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(e.location)
            }
            if (e.note.isNotBlank()) WhiteCard {
                Text("NOTES", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(e.note)
            }
            Spacer(Modifier.height(8.dp))
            Button(modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = { vm.screen = Screen.Form(trip.id, e.id) }) { Text("Edit") }
            OutlinedButton(modifier = Modifier.fillMaxWidth().height(52.dp), onClick = { confirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Owe)) { Text("Delete expense") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------- 5. Add / Edit expense ----------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseForm(trip: Trip, existing: Expense?, vm: TripVM) {
    val ctx = LocalContext.current
    var amount by remember {
        mutableStateOf(existing?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    var cat by remember { mutableStateOf(existing?.category ?: vm.allCats.first()) }
    var date by remember { mutableLongStateOf(existing?.date ?: todayUtc()) }
    var paid by remember { mutableStateOf(existing?.paidBy ?: trip.travelers.first()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var detecting by remember { mutableStateOf(false) }
    var newCat by remember { mutableStateOf(false) }
    val amt = amount.toDoubleOrNull() ?: 0.0
    val cats = (vm.allCats + cat).distinct()

    fun detect(force: Boolean) {
        detecting = true
        detectPlace(ctx) { r ->
            detecting = false
            if (r != null && (force || location.isBlank())) location = r
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.any { it }) detect(false)
    }
    fun ensureAndDetect(force: Boolean) {
        if (hasLocPermission(ctx)) detect(force)
        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(Unit) { if (existing == null) ensureAndDetect(false) }

    if (newCat) {
        var nm by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { newCat = false }, title = { Text("New category") },
            text = { OutlinedTextField(nm, { nm = it }, label = { Text("Category name") }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = nm.isNotBlank(), onClick = {
                    val c = nm.trim(); vm.addCategory(c)
                    cat = vm.allCats.firstOrNull { it.equals(c, true) } ?: c
                    newCat = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { newCat = false }) { Text("Cancel") } })
    }
    val back = { vm.screen = if (existing != null) Screen.ExpenseView(trip.id, existing.id) else Screen.Detail(trip.id) }
    Scaffold(containerColor = Bg) { p ->
        Column(Modifier.padding(p).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Header(if (existing != null) "Edit expense" else "Add expense", trip.name, onBack = { back() })
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("Category", color = Muted, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cats.forEach { c -> FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) }) }
                AssistChip(onClick = { newCat = true }, label = { Text("+ Add") })
            }
            DateField("Date", date, { date = it }, Modifier.fillMaxWidth())
            Text("Paid by", color = Muted, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                trip.travelers.forEach { n -> FilterChip(selected = paid == n, onClick = { paid = n }, label = { Text(n) }) }
            }
            OutlinedTextField(location, { location = it }, label = { Text("Location") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(if (detecting) "Detecting your location…" else "Auto-detected · you can edit it") },
                trailingIcon = {
                    IconButton(onClick = { ensureAndDetect(true) }) { Icon(Icons.Default.Refresh, "Detect location again") }
                })
            OutlinedTextField(note, { note = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = amt > 0, modifier = Modifier.fillMaxWidth().height(52.dp), onClick = {
                val e = (existing ?: Expense(category = cat, amount = amt, date = date, note = "", paidBy = paid,
                    splitAmong = trip.travelers)).copy(category = cat, amount = amt, date = date,
                    note = note.trim(), paidBy = paid, location = location.trim())
                vm.saveExpense(trip.id, e)
                vm.screen = if (existing != null) Screen.ExpenseView(trip.id, e.id) else Screen.Detail(trip.id)
            }) { Text(if (existing != null) "Save changes" else "Save expense") }
            if (existing != null) OutlinedButton(modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = { back() }) { Text("Cancel") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
