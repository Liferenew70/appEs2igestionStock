package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StockApp(viewModel: StockViewModel) {
    val company by viewModel.companyState.collectAsStateWithLifecycle()
    val allCompanies by viewModel.allCompanies.collectAsStateWithLifecycle()

    if (company == null) {
        OnboardingWorkspaceScreen(
            allCompanies = allCompanies,
            onSelect = { viewModel.selectCompany(it) },
            onCreate = { name, icon, currency -> viewModel.createAndSelectCompany(name, icon, currency) },
            onDelete = { viewModel.deleteCompanyProfile(it) },
            onLoadDemo = { viewModel.loadDemoData() }
        )
    } else {
        MainWorkspaceDashboard(viewModel = viewModel, company = company!!)
    }
}

// ----------------------------------------------------
// MULTI-COMPANY ONBOARDING / LANDING / WORKSPACE SCREEN
// ----------------------------------------------------
@Composable
fun OnboardingWorkspaceScreen(
    allCompanies: List<Company>,
    onSelect: (String) -> Unit,
    onCreate: (String, String, String) -> Unit,
    onDelete: (String) -> Unit,
    onLoadDemo: () -> Unit
) {
    var inputName by remember { mutableStateOf("") }
    var selectedSymbol by remember { mutableStateOf("FCFA") }
    var selectedLogo by remember { mutableStateOf("STORE") }
    var showError by remember { mutableStateOf(false) }

    val currencyOptions = listOf("FCFA", "EUR", "USD", "INR", "CNY", "CAD")
    val logoOptions = listOf("STORE", "WINE", "BEER", "WAREHOUSE", "BOX")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warehouse,
                contentDescription = "Logo",
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Gestion de Stocks v2.0",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Comptabilité locale d'approvisionnement et d'inventaire.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            if (allCompanies.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Vos Espaces de Stocks :",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        allCompanies.forEach { comp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .clickable { onSelect(comp.id) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when(comp.logoUri) {
                                                "WINE" -> Icons.Default.WineBar
                                                "BEER" -> Icons.Default.LocalBar
                                                "BOX" -> Icons.Default.Inventory
                                                "WAREHOUSE" -> Icons.Default.Warehouse
                                                else -> Icons.Default.Storefront
                                            },
                                            contentDescription = "Logo",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Column {
                                        Text(comp.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Profil: ${comp.id} • Devise: ${comp.currencySymbol}", 
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                IconButton(onClick = { onDelete(comp.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Créer un Nouvel Espace",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = {
                            inputName = it
                            showError = false
                        },
                        label = { Text("Nom de l'Entreprise ou Cave") },
                        isError = showError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showError) {
                        Text(
                            text = "Saisissez un nom d'entreprise valide",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text("Icône :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        logoOptions.forEach { opt ->
                            val isSelected = selectedLogo == opt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedLogo = opt }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(opt) {
                                        "WINE" -> Icons.Default.WineBar
                                        "BEER" -> Icons.Default.LocalBar
                                        "BOX" -> Icons.Default.Inventory
                                        "WAREHOUSE" -> Icons.Default.Warehouse
                                        else -> Icons.Default.Storefront
                                    },
                                    contentDescription = opt,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Text("Devise :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currencyOptions.forEach { opt ->
                            val isSelected = selectedSymbol == opt
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSymbol = opt },
                                label = { Text(opt, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (inputName.trim().isNotBlank()) {
                                onCreate(inputName.trim(), selectedLogo, selectedSymbol)
                            } else {
                                showError = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Créer l'Espace", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = " OU ",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            OutlinedButton(
                onClick = onLoadDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Casino, "Demo", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Charger Données d'Exemple", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ----------------------------------------------------
// MAIN WORKSPACE DASHBOARD
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWorkspaceDashboard(viewModel: StockViewModel, company: Company) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val tabs = listOf(
        TabItem("Inventaire", Icons.Default.Inventory),
        TabItem("Mouvements", Icons.Default.SwapHoriz),
        TabItem("Analyses", Icons.Default.BarChart),
        TabItem("Données", Icons.Default.Backup),
        TabItem("Tutoriel", Icons.Default.MenuBook)
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val logo = company.logoUri
                                if (logo.startsWith("content://") || logo.startsWith("file://") || logo.contains("/")) {
                                    androidx.compose.foundation.Image(
                                        painter = coil.compose.rememberAsyncImagePainter(logo),
                                        contentDescription = "Photo Profil",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = when(logo) {
                                            "WINE" -> Icons.Default.WineBar
                                            "BEER" -> Icons.Default.LocalBar
                                            "BOX" -> Icons.Default.Inventory
                                            "WAREHOUSE" -> Icons.Default.Warehouse
                                            else -> Icons.Default.Storefront
                                        },
                                        contentDescription = "Logo",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = company.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = company.currencySymbol,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Text(
                                    text = "Session: Admin • Stock Global",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Changer Theme"
                            )
                        }
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Déconnecter",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(tab.title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)) },
                            icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> TabInventory(viewModel)
                1 -> TabMovements(viewModel)
                2 -> TabCharts(viewModel)
                3 -> TabBackup(viewModel)
                4 -> TabTutorial()
            }
        }
    }
}

// ==========================================
// VIEW TAB 1: INVENTAIRE (Inventory List)
// ==========================================
@Composable
fun TabInventory(viewModel: StockViewModel) {
    val items by viewModel.productUiStates.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()
    val activeCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val movements by viewModel.movementsState.collectAsStateWithLifecycle()
    val products by viewModel.productsState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var viewModeSpreadsheet by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductUiState?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        val totalProductsCount = items.size
        val totalStockUnits = items.sumOf { it.finalStock }.toInt()
        val currentTotalVal = items.sumOf { it.totalValue }
        
        val pricesMap = products.associate { it.code to it.unitPrice }
        val totalSalesValue = movements.filter { m -> m.type == "SORTIE" && m.subType != "REGLEMENT_FOURNISSEUR" }.sumOf { it.quantity * (pricesMap[it.productCode] ?: 0.0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Total Produits",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalProductsCount",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Huile, Riz, Blé...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Volume Stock",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "%,d", totalStockUnits),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Val: " + formatMoney(currentTotalVal, currencySymbol),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Ventes (Mois)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMoney(totalSalesValue, currencySymbol),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = Color(38, 166, 91)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Chiffre d'Affaires",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                            color = Color(38, 166, 91).copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Recherche article...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, "Rechercher", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, "Effacer", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedIconButton(
                    onClick = { viewModeSpreadsheet = !viewModeSpreadsheet },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = if (viewModeSpreadsheet) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (viewModeSpreadsheet) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = if (viewModeSpreadsheet) Icons.Default.GridOn else Icons.Default.FormatListBulleted,
                        contentDescription = "Toggle View",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat == activeCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedCategory.value = cat },
                        label = { Text(cat, style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun article trouvé.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                if (viewModeSpreadsheet) {
                    SpreadsheetView(
                        items = items,
                        currencySymbol = currencySymbol,
                        onEdit = { editingProduct = it },
                        onDelete = { viewModel.deleteProduct(it) }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(items, key = { it.product.code }) { item ->
                            ProductCardElement(
                                item = item,
                                currencySymbol = currencySymbol,
                                onEdit = { editingProduct = item },
                                onDelete = { viewModel.deleteProduct(item.product.code) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Créer Produit")
        }

        if (showAddDialog) {
            val nextCodeInt = (items.map { it.product.code.trim().toIntOrNull() ?: 0 }.maxOrNull() ?: 0) + 1
            val calculatedAutoCode = String.format(Locale.US, "%05d", nextCodeInt)
            
            AddProductDialog(
                autoCode = calculatedAutoCode,
                currency = currencySymbol,
                onDismiss = { showAddDialog = false },
                onAdd = { code, name, cat, price, stock, purchasePrice ->
                    viewModel.addProduct(code, name, cat, price, stock, purchasePrice)
                    showAddDialog = false
                }
            )
        }

        if (editingProduct != null) {
            EditProductDialog(
                item = editingProduct!!,
                currency = currencySymbol,
                onDismiss = { editingProduct = null },
                onEdit = { code, designation, category, unitPrice, initialStock, purchasePrice ->
                    viewModel.updateProduct(code, designation, category, unitPrice, initialStock, purchasePrice)
                    editingProduct = null
                }
            )
        }
    }
}

// ----------------------------------------------------
// VIEW TAB CARD ELEMENT (Inventory Comfort item format)
// ----------------------------------------------------
@Composable
fun ProductCardElement(
    item: ProductUiState,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteAlert by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F6F9)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4E0ED))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Title & Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.product.designation,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1C1B1F),
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Éditer",
                            tint = Color(0xFF7E56C2),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { showDeleteAlert = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color(0xFFC64E4D),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Row 2: Code Badge and Category Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEBE5F5), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Code: ${item.product.code}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF7E56C2)
                    )
                }

                Text(
                    text = item.product.category,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF7E56C2)
                )
            }

            HorizontalDivider(color = Color(0xFFE4E0ED).copy(alpha = 0.6f))

            // Row 3: Grid P.A., P.U., Entrées, Sorties
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // P.A. (Achat)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "P.A. (Achat)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7A757F)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatMoney(item.product.purchasePrice, currencySymbol),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF7E56C2)
                    )
                }

                // P.U. (Vente)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "P.U. (Vente)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7A757F)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatMoney(item.product.unitPrice, currencySymbol),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1C1B1F)
                    )
                }

                // Entrées
                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Entrées",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7A757F)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "+${item.totalEntries}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF508D69)
                    )
                }

                // Sorties
                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Sorties",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7A757F)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (item.totalSorties > 0) "-${item.totalSorties}" else "${-item.totalSorties}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFC64E4D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Row 4: Benefit & Supplier Debt next to each other
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Benefice
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Bénéfice",
                        tint = Color(0xFF508D69),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Bénéfice: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A757F)
                    )
                    Text(
                        text = formatMoney(item.netProfit, currencySymbol),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF508D69)
                    )
                }

                // Dette Fourn
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Dette",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Dette Fourn.: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A757F)
                    )
                    Text(
                        text = formatMoney(item.debtToSupplier, currencySymbol),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1C1B1F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Row 5: Bottom gray background row for Stock Final and Valorisation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEDEBF0), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HomeWork,
                            contentDescription = "Stock Final",
                            tint = Color(0xFF7E56C2),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Stock Final: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7A757F)
                        )
                        Text(
                            text = "${item.finalStock}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF7E56C2)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Valorisation: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7A757F)
                        )
                        Text(
                            text = formatMoney(item.totalValue, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF7E56C2)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showDeleteAlert = false },
            title = { Text("Supprimer ?") },
            text = { Text("Voulez-vous supprimer '${item.product.designation}' ?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteAlert = false
                }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAlert = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

// ==========================================
// SPREADSHEET TABLE GRID VIEW
// ==========================================
@Composable
fun SpreadsheetView(
    items: List<ProductUiState>,
    currencySymbol: String,
    onEdit: (ProductUiState) -> Unit,
    onDelete: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(vertical = 6.dp)
                ) {
                    TableCell("Code", header = true, width = 70.dp)
                    TableCell("Désignation", header = true, width = 160.dp)
                    TableCell("Catégorie", header = true, width = 100.dp)
                    TableCell("P.A. Achat", header = true, width = 80.dp)
                    TableCell("P.U. Vente", header = true, width = 80.dp)
                    TableCell("Stock", header = true, width = 70.dp)
                    TableCell("Valeur", header = true, width = 90.dp)
                    TableCell("Bénéfices", header = true, width = 90.dp)
                    TableCell("Actions", header = true, width = 80.dp)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items, key = { it.product.code }) { item ->
                        var showConfirm by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .background(
                                    if (items.indexOf(item) % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCell(item.product.code, width = 70.dp, bold = true)
                            TableCell(item.product.designation, width = 160.dp)
                            TableCell(item.product.category, width = 100.dp)
                            TableCell(formatMoney(item.product.purchasePrice, currencySymbol), width = 80.dp)
                            TableCell(formatMoney(item.product.unitPrice, currencySymbol), width = 80.dp)
                            TableCell("${item.finalStock}", width = 70.dp, bold = true, color = if (item.finalStock <=0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            TableCell(formatMoney(item.totalValue, currencySymbol), width = 90.dp, bold = true, color = MaterialTheme.colorScheme.secondary)
                            TableCell(formatMoney(item.netProfit, currencySymbol), width = 90.dp, bold = true, color = Color(38, 166, 91))
                            
                            Row(
                                modifier = Modifier.width(80.dp).padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, "modifier", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).clickable { onEdit(item) })
                                Icon(Icons.Default.Delete, "supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp).clickable { showConfirm = true })
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        if (showConfirm) {
                            AlertDialog(
                                onDismissRequest = { showConfirm = false },
                                title = { Text("Supprimer ?") },
                                text = { Text("Supprimer '${item.product.designation}' ?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        onDelete(item.product.code)
                                        showConfirm = false
                                    }) {
                                        Text("Oui", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showConfirm = false }) { Text("Non") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(
    text: String,
    header: Boolean = false,
    width: androidx.compose.ui.unit.Dp,
    bold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        style = if (header) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
               else if (bold) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
               else MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = if (header) MaterialTheme.colorScheme.onSecondaryContainer else color
    )
}

// ----------------------------------------------------
// ADD / EDIT PRODUCT DIALOGS PART
// ----------------------------------------------------
@Composable
fun AddProductDialog(
    autoCode: String,
    currency: String,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, Double, Double, Double) -> Unit
) {
    var code by remember { mutableStateOf(autoCode) }
    var designation by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var initialStock by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Ajouter un Nouvel Article", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code Article (Unique)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("Désignation / Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = purchasePrice, onValueChange = { purchasePrice = it }, label = { Text("Prix d'Achat ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = unitPrice, onValueChange = { unitPrice = it }, label = { Text("Prix Vente Unitaire ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = initialStock, onValueChange = { initialStock = it }, label = { Text("Stock Initial") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())

                if (hasError) {
                    Text("Veuillez saisir des informations numériques valides.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val pa = purchasePrice.toDoubleOrNull() ?: 0.0
                            val pu = unitPrice.toDoubleOrNull()
                            val si = initialStock.toDoubleOrNull()
                            if (code.isNotBlank() && designation.isNotBlank() && category.isNotBlank() && pu != null && si != null) {
                                onAdd(code, designation, category, pu, si, pa)
                            } else {
                                hasError = true
                            }
                        }
                    ) {
                        Text("Ajouter")
                    }
                }
            }
        }
    }
}

@Composable
fun EditProductDialog(
    item: ProductUiState,
    currency: String,
    onDismiss: () -> Unit,
    onEdit: (String, String, String, Double, Double, Double) -> Unit
) {
    var designation by remember { mutableStateOf(item.product.designation) }
    var category by remember { mutableStateOf(item.product.category) }
    var purchasePrice by remember { mutableStateOf(item.product.purchasePrice.toString()) }
    var unitPrice by remember { mutableStateOf(item.product.unitPrice.toString()) }
    var initialStock by remember { mutableStateOf(item.product.initialStock.toString()) }
    var hasError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Modifier l'Article", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Code : ${item.product.code}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("Désignation") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = purchasePrice, onValueChange = { purchasePrice = it }, label = { Text("Prix Achat ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = unitPrice, onValueChange = { unitPrice = it }, label = { Text("Prix Vente ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = initialStock, onValueChange = { initialStock = it }, label = { Text("Stock Initial") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())

                if (hasError) {
                    Text("Saisissez des valeurs numériques.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val pa = purchasePrice.toDoubleOrNull() ?: 0.0
                            val pu = unitPrice.toDoubleOrNull()
                            val si = initialStock.toDoubleOrNull()
                            if (designation.isNotBlank() && category.isNotBlank() && pu != null && si != null) {
                                onEdit(item.product.code, designation, category, pu, si, pa)
                            } else {
                                hasError = true
                            }
                        }
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

// ==========================================
// CHANNELS VECTOR BAR CHART (Inventory graphics)
// ==========================================
@Composable
fun StockBarChart(products: List<ProductUiState>) {
    val chartData = products

    if (chartData.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Aucun stock à tracer pour l'instant.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    val maxStock = chartData.maxOfOrNull { m -> if (m.finalStock > 0) m.finalStock else 0.0 } ?: 1.0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chartData.forEach { item ->
            val ratio = (item.finalStock / maxStock).toFloat()
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.product.designation,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${item.finalStock.toInt()} u.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = ratio.coerceAtLeast(0.04f))
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

// ==========================================
// VIEW TAB 2: MOUVEMENTS (Entrées / Sorties List)
// ==========================================
@Composable
fun TabMovements(viewModel: StockViewModel) {
    val movements by viewModel.movementsState.collectAsStateWithLifecycle()
    val products by viewModel.productsState.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()

    var showAddMovementDialog by remember { mutableStateOf(false) }
    var showSupplierDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historique des Opérations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${movements.size} entrées/sorties",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (movements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun mouvement enregistré.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(movements, key = { it.id }) { mov ->
                        var showConfirmDelete by remember { mutableStateOf(false) }
                        val matchedProduct = products.find { it.code == mov.productCode }
                        val desc = matchedProduct?.designation ?: "Code article: ${mov.productCode}"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isRepayment = mov.subType == "REGLEMENT_FOURNISSEUR"
                                    val isEntree = mov.type == "ENTREE"

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(
                                                color = if (isRepayment) MaterialTheme.colorScheme.primaryContainer
                                                       else if (isEntree) Color(230, 246, 235) else Color(253, 235, 235),
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isRepayment) Icons.Default.LocalShipping
                                                           else if (isEntree) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = mov.type,
                                            tint = if (isRepayment) MaterialTheme.colorScheme.primary
                                                   else if (isEntree) Color(38, 166, 91) else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isRepayment) "Règlement : ${mov.supplierName}" else desc,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isRepayment) "REPAYMENT" else if (isEntree) "ENTRÉE" else "SORTIE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isRepayment) MaterialTheme.colorScheme.primary
                                                       else if (isEntree) Color(38, 166, 91) else MaterialTheme.colorScheme.error
                                            )
                                            if (mov.subType.isNotEmpty() && !isRepayment) {
                                                Text(
                                                    text = "(${mov.subType})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                        }

                                        if (mov.notes.isNotBlank()) {
                                            Text(
                                                text = "Note: ${mov.notes}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                        Text(
                                            text = dateFormat.format(Date(mov.timestamp)),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isRepayment = mov.subType == "REGLEMENT_FOURNISSEUR"
                                    if (isRepayment) {
                                        Text(
                                            text = "-${mov.amountPaid.toInt()} XOF",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Text(
                                            text = "${if (mov.type == "ENTREE") "+" else "-"}${mov.quantity}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (mov.type == "ENTREE") Color(38, 166, 91) else MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(onClick = { showConfirmDelete = true }, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Supprimer",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (showConfirmDelete) {
                            AlertDialog(
                                onDismissRequest = { showConfirmDelete = false },
                                title = { Text("Supprimer l'opération ?") },
                                text = { Text("Voulez-vous annuler et supprimer ce mouvement de stock définitivement ?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deleteMovement(mov.id)
                                        showConfirmDelete = false
                                    }) {
                                        Text("Oui, supprimer", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showConfirmDelete = false }) {
                                        Text("Annuler")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FloatingActionButton(
                onClick = { showSupplierDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = "Fournisseurs", modifier = Modifier.size(20.dp))
            }

            FloatingActionButton(
                onClick = { showAddMovementDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Opération")
            }
        }

        if (showAddMovementDialog) {
            AddMovementDialog(
                products = products,
                viewModel = viewModel,
                onDismiss = { showAddMovementDialog = false },
                onAdd = { code, type, subType, qty, notes, credQty, paid, remaining, supplier ->
                    viewModel.addMovement(
                        productCode = code,
                        type = type,
                        quantity = qty,
                        notes = notes,
                        subType = subType,
                        creditQuantity = credQty,
                        amountPaid = paid,
                        amountRemaining = remaining,
                        supplierName = supplier
                    )
                    showAddMovementDialog = false
                }
            )
        }

        if (showSupplierDialog) {
            SuppliersManagerDialog(
                suppliers = suppliers,
                onDismiss = { showSupplierDialog = false },
                onRepay = { name, amount ->
                    viewModel.makeSupplierRepayment(name, amount)
                }
            )
        }
    }
}

// ==========================================
// VIEW TAB 3: STATISTIQUES & ANALYSES
// ==========================================
@Composable
fun TabCharts(viewModel: StockViewModel) {
    val items by viewModel.productUiStates.collectAsStateWithLifecycle()
    val movements by viewModel.movementsState.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    var expandedDropdownMonth by remember { mutableStateOf(false) }

    val monthsFrench = listOf(
        "Janvier" to 0, "Février" to 1, "Mars" to 2, "Avril" to 3, "Mai" to 4, "Juin" to 5,
        "Juillet" to 6, "Août" to 7, "Septembre" to 8, "Octobre" to 9, "Novembre" to 10, "Décembre" to 11
    )

    val filteredMovements = remember(movements, selectedPeriod) {
        val now = System.currentTimeMillis()
        val c = Calendar.getInstance()
        when {
            selectedPeriod == "WEEK" -> movements.filter { it.timestamp >= now - (7L * 24 * 60 * 60 * 1000) }
            selectedPeriod == "MONTH" -> movements.filter { it.timestamp >= now - (30L * 24 * 60 * 60 * 1000) }
            selectedPeriod.startsWith("CAL_") -> {
                val targetMonthIdx = selectedPeriod.removePrefix("CAL_").toIntOrNull() ?: 0
                movements.filter { m ->
                    c.timeInMillis = m.timestamp
                    c.get(Calendar.MONTH) == targetMonthIdx
                }
            }
            else -> movements
        }
    }

    val productMap = remember(items) { items.associateBy { it.product.code } }

    val periodSalesQty = remember(filteredMovements) {
        filteredMovements.filter { m -> m.type == "SORTIE" && m.subType != "REGLEMENT_FOURNISSEUR" && m.subType != "PERTE" }.sumOf { it.quantity }
    }

    val periodSalesRevenue = remember(filteredMovements, productMap) {
        filteredMovements.filter { m -> m.type == "SORTIE" && m.subType != "REGLEMENT_FOURNISSEUR" && m.subType != "PERTE" }.sumOf { m ->
            val sellPrice = productMap[m.productCode]?.product?.unitPrice ?: 0.0
            m.quantity * sellPrice
        }
    }

    val periodLossQty = remember(filteredMovements) {
        filteredMovements.filter { m -> m.type == "SORTIE" && m.subType == "PERTE" }.sumOf { it.quantity }
    }

    val periodLossValue = remember(filteredMovements, productMap) {
        filteredMovements.filter { m -> m.type == "SORTIE" && m.subType == "PERTE" }.sumOf { m ->
            val purchasePrice = productMap[m.productCode]?.product?.purchasePrice ?: 0.0
            m.quantity * purchasePrice
        }
    }

    val periodGrossProfit = remember(filteredMovements, productMap) {
        filteredMovements.filter { m -> m.type == "SORTIE" && m.subType != "REGLEMENT_FOURNISSEUR" && m.subType != "PERTE" }.sumOf { m ->
            val prod = productMap[m.productCode]?.product
            val sellPrice = prod?.unitPrice ?: 0.0
            val buyPrice = prod?.purchasePrice ?: 0.0
            val profitMargin = sellPrice - buyPrice
            m.quantity * profitMargin
        }
    }

    val periodNetProfit = remember(periodGrossProfit, periodLossValue) {
        maxOf(0.0, periodGrossProfit - periodLossValue)
    }

    val periodSupplierDebt = remember(filteredMovements) {
        filteredMovements.filter { m -> m.type == "ENTREE" && m.subType == "CREDIT" }.sumOf { m -> m.amountRemaining }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Analyses de Performance & Comptabilité",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Filtrer la période d'analyse :",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedPeriod == "WEEK",
                        onClick = { viewModel.selectedPeriod.value = "WEEK" },
                        label = { Text("7 Jours") }
                    )
                    FilterChip(
                        selected = selectedPeriod == "MONTH",
                        onClick = { viewModel.selectedPeriod.value = "MONTH" },
                        label = { Text("30 Jours") }
                    )
                    FilterChip(
                        selected = selectedPeriod == "ALL",
                        onClick = { viewModel.selectedPeriod.value = "ALL" },
                        label = { Text("Historique Complet") }
                    )

                    Box {
                        val isMonthActive = selectedPeriod.startsWith("CAL_")
                        val activeMonthLabel = if (isMonthActive) {
                            val idx = selectedPeriod.removePrefix("CAL_").toIntOrNull() ?: 0
                            monthsFrench.find { it.second == idx }?.first ?: "Calendrier"
                        } else "Choisir un Mois"

                        FilterChip(
                            selected = isMonthActive,
                            onClick = { expandedDropdownMonth = true },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(activeMonthLabel)
                                    Icon(Icons.Default.ArrowDropDown, "dropdown", modifier = Modifier.size(16.dp))
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = expandedDropdownMonth,
                            onDismissRequest = { expandedDropdownMonth = false }
                        ) {
                            monthsFrench.forEach { (name, idx) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        viewModel.selectedPeriod.value = "CAL_$idx"
                                        expandedDropdownMonth = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        val labelStr = when {
            selectedPeriod == "WEEK" -> "Cette Semaine"
            selectedPeriod == "MONTH" -> "Ce Mois-ci"
            selectedPeriod.startsWith("CAL_") -> {
                val idx = selectedPeriod.removePrefix("CAL_").toIntOrNull() ?: 0
                monthsFrench.find { it.second == idx }?.first ?: "Mensuel"
            }
            else -> "Tout l'Historique"
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Chiffres Clés Comptables ($labelStr)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("CHIFFRE VENTES", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(formatMoney(periodSalesRevenue, currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("${periodSalesQty.toInt()} unités", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32).copy(alpha = 0.8f))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4527A0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("BÉNÉFICE NET", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White.copy(alpha = 0.9f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(formatMoney(periodNetProfit, currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Reste net", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("PERTES STOCKS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFC62828))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(formatMoney(periodLossValue, currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            Text("${periodLossQty.toInt()} unités perdues", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828).copy(alpha = 0.8f))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("DETTE FOURN.", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(formatMoney(periodSupplierDebt, currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Crédit restant", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Classement & Analyse des Rotations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val sortedBySales = items.filter { it.totalSalesQty > 0 }.sortedByDescending { it.totalSalesQty }
                val sortedByProfit = items.filter { it.netProfit > 0 }.sortedByDescending { it.netProfit }

                Text("🥇 Article le Plus Vendu :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                if (sortedBySales.isNotEmpty()) {
                    val top = sortedBySales.first()
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(top.product.designation, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("${top.totalSalesQty.toInt()} u.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text("Aucune vente enregistrée.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text("💰 Article le Plus Rentable :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                if (sortedByProfit.isNotEmpty()) {
                    val top = sortedByProfit.first()
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(top.product.designation, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(formatMoney(top.netProfit, currencySymbol), color = Color(38, 166, 91), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text("Aucun bénéfice enregistré.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Suivi Trimestriel de l'Évolution",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Entrées de stock vs Sorties et Ventes", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 10.dp))
                
                StockMovementCurveChart(movements = filteredMovements)
            }
        }

        var chartFilter by remember { mutableStateOf("ALL") }
        var expandedChartFilter by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (chartFilter) {
                            "ALL" -> "Quantité en Stock (Tous)"
                            "TOP_5" -> "Quantité en Stock (Top 5)"
                            "TOP_10" -> "Quantité en Stock (Top 10)"
                            else -> "Quantité en Stock"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        OutlinedButton(
                            onClick = { expandedChartFilter = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (chartFilter) {
                                        "ALL" -> "Tous les Produits"
                                        "TOP_5" -> "Top 5"
                                        "TOP_10" -> "Top 10"
                                        else -> "Filtre"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "dropdown", modifier = Modifier.size(14.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = expandedChartFilter,
                            onDismissRequest = { expandedChartFilter = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tous les Produits", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    chartFilter = "ALL"
                                    expandedChartFilter = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Top 5 Stocks", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    chartFilter = "TOP_5"
                                    expandedChartFilter = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Top 10 Stocks", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    chartFilter = "TOP_10"
                                    expandedChartFilter = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                val filteredChartProducts = remember(items, chartFilter) {
                    val list = items.filter { it.finalStock >= 0 }
                    when (chartFilter) {
                        "TOP_5" -> list.sortedByDescending { it.finalStock }.take(5)
                        "TOP_10" -> list.sortedByDescending { it.finalStock }.take(10)
                        else -> list.sortedByDescending { it.finalStock } // ALL!
                    }
                }

                StockBarChart(products = filteredChartProducts)
            }
        }
    }
}

// ==========================================
// VIEW TAB 4: CONFIGURATION, IMPORTS/EXPORTS
// ==========================================
@Composable
fun TabBackup(viewModel: StockViewModel) {
    val context = LocalContext.current
    val compState by viewModel.companyState.collectAsStateWithLifecycle()
    var inputEnterpriseName by remember { mutableStateOf("") }
    var showClearWarning by remember { mutableStateOf(false) }

    var importOverwriteMode by remember { mutableStateOf(false) }
    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importJsonBackup(context, uri, importOverwriteMode)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = java.io.File(context.filesDir, "profile_company_${compState?.id ?: "temp"}.png")
                    file.outputStream().use { outputStream ->
                        inputStream.use { it.copyTo(outputStream) }
                    }
                    viewModel.updateCompanyLogo(file.absolutePath)
                    Toast.makeText(context, "Photo de profil mise à jour avec succès !", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur de sélection : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(compState) {
        compState?.let {
            inputEnterpriseName = it.name
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Données & Personnalisation de l'Espace",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (compState != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Éditer le profil de l'Espace",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Centered circular profile preview with click picker
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            val logo = compState?.logoUri ?: ""
                            if (logo.startsWith("content://") || logo.startsWith("file://") || logo.contains("/")) {
                                androidx.compose.foundation.Image(
                                    painter = coil.compose.rememberAsyncImagePainter(logo),
                                    contentDescription = "Avatar de l'Espace",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = when(logo) {
                                        "WINE" -> Icons.Default.WineBar
                                        "BEER" -> Icons.Default.LocalBar
                                        "BOX" -> Icons.Default.Inventory
                                        "WAREHOUSE" -> Icons.Default.Warehouse
                                        else -> Icons.Default.Storefront
                                    },
                                    contentDescription = "Logo",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Caméra", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sélectionner la photo (Galerie)", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    OutlinedTextField(
                        value = inputEnterpriseName,
                        onValueChange = { inputEnterpriseName = it },
                        label = { Text("Nom de l'Espace / Entreprise") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Symbole corporatif :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("STORE", "WINE", "BEER", "WAREHOUSE", "BOX").forEach { opt ->
                            val isSelected = compState?.logoUri == opt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.updateCompanyLogo(opt) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(opt) {
                                        "WINE" -> Icons.Default.WineBar
                                        "BEER" -> Icons.Default.LocalBar
                                        "BOX" -> Icons.Default.Inventory
                                        "WAREHOUSE" -> Icons.Default.Warehouse
                                        else -> Icons.Default.Storefront
                                    },
                                    contentDescription = opt,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (inputEnterpriseName.trim().isNotEmpty()) {
                                viewModel.createAndSelectCompany(inputEnterpriseName.trim(), compState?.logoUri ?: "STORE", compState?.currencySymbol ?: "FCFA")
                                Toast.makeText(context, "Modifications enregistrées !", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Save, "Sauvegarder", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mettre à jour")
                    }
                }
            }
        }

        val activeHex by viewModel.customAccentHex.collectAsStateWithLifecycle()
        var hexInputCode by remember { mutableStateOf(activeHex) }
        val predefinedColorsList = listOf(
            "#800020" to "Bordeaux",
            "#1E5E3A" to "Vert Cave",
            "#1F4E79" to "Saphir",
            "#A0522D" to "Bois Oak",
            "#7E57C2" to "Pourpre",
            "#E65100" to "Orange"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Nuancier Couleur & Conception visuelle",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Définissez un habillage personnalisé pour colorer l'application en temps réel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    predefinedColorsList.forEach { (hex, name) ->
                        val isSel = activeHex.replace("#", "").uppercase() == hex.replace("#", "").uppercase()
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                .clickable {
                                    viewModel.updateCustomAccentHex(hex)
                                    hexInputCode = hex
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) {
                                Icon(Icons.Default.Check, "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hexInputCode,
                        onValueChange = { hexInputCode = it },
                        label = { Text("Code Hexadécimal (#ffffff)") },
                        placeholder = { Text("#800020") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            var code = hexInputCode.trim()
                            if (code.isNotEmpty()) {
                                if (!code.startsWith("#")) {
                                    code = "#$code"
                                }
                                try {
                                    android.graphics.Color.parseColor(code)
                                    viewModel.updateCustomAccentHex(code)
                                    Toast.makeText(context, "Teinte personnalisée appliquée !", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Format hexadécimal erroné !", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Appliquer")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Exporter & Sauvegarder",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.sharePdf(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(220, 53, 69)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, "PDF", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", maxLines = 1)
                    }

                    Button(
                        onClick = { viewModel.shareCsv(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(38, 166, 91)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.TextSnippet, "Excel", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", maxLines = 1)
                    }

                    Button(
                        onClick = { viewModel.shareJson(context) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Code, "JSON", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("JSON", maxLines = 1)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Reconstituer / Importer (JSON)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = {
                        importOverwriteMode = true
                        jsonImportLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Warning, "Overwrite", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Écraser les données")
                }

                OutlinedButton(
                    onClick = {
                        importOverwriteMode = false
                        jsonImportLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Publish, "Append", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Importer pour s'ajouter (Conserver l'existant)")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Zone Dangereuse", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showClearWarning = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Supprimer toutes les données de cet Espace")
                }
            }
        }

        if (showClearWarning) {
            AlertDialog(
                onDismissRequest = { showClearWarning = false },
                title = { Text("Réinitialisation de l'espace ?") },
                text = { Text("Êtes-vous sûr de vouloir vider le stock et l'historique complet ? Cette action est irréversible.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAllData()
                        showClearWarning = false
                    }) {
                        Text("Confirmer l'effacement", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearWarning = false }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

// ==========================================
// VIEW TAB 5: GUIDE / INTERACTIVE TUTORIAL
// ==========================================
@Composable
fun TabTutorial() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Manuel d'utilisation & Onboarding",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📊 Guide Rapide d'Installation", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "1. Saisie des Articles : Allez sur l'onglet 'Inventaire' et créez vos fiches produits en spécifiant de préférence le Code Article, la Désignation, Prix d'Achat, Prix de Vente et Stock Initial.\n\n" +
                           "2. Enregistrement des flux : Utilisez l'onglet 'Mouvements' pour enregistrer vos opérations d'Approvisionnements (Entrées) ou de Ventes (Sorties). Les volumes et profits sont calculés en temps réel.\n\n" +
                           "3. Surveillance de la Caisse & Dettes : Tout achat à crédit engendre une dette fournisseur automatique. Utilisez l'icône de Voiture/Camion pour visualiser vos encours et y enregistrer des règlements d'acompte.\n\n" +
                           "4. Protection antigaspi (Rupture de Stock) : L'outil bloque automatiquement les sorties de marchandises si vous tentez de vendre une quantité supérieure au stock restant physiquement en rayon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("💾 Sauvegardes, Excel & Comptabilité", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Text(
                    text = "- Export Excel : Génère des feuilles directes contenant des calculs de Chiffres d'Affaires et des marges de bénéfice net par référence.\n\n" +
                           "- Export PDF : Génère des rapports administratifs d'inventaire officiels.\n\n" +
                           "- Restauration JSON : Permet de transiter des listes d'articles entières de manière sécurisée hors-ligne.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ----------------------------------------------------
// POPUPS / DIALOGS ELEMENTS & CRITICAL DIALOG CONTROLLERS
// ----------------------------------------------------

@Composable
fun SuppliersManagerDialog(
    suppliers: List<SupplierUiState>,
    onDismiss: () -> Unit,
    onRepay: (String, Double) -> Unit
) {
    var selectedSupplierForPay by remember { mutableStateOf<SupplierUiState?>(null) }
    var repayAmount by remember { mutableStateOf("") }
    var isErr by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Suivi des Dettes Fournisseurs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (suppliers.isEmpty()) {
                    Text(
                        "Aucun fournisseur enregistré. Créez un mouvement d'Entrée à Crédit en renseignant un fournisseur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Text("D'accord")
                    }
                    return@Column
                }

                if (selectedSupplierForPay == null) {
                    suppliers.forEach { sup ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .clickable { selectedSupplierForPay = sup }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(sup.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Balance: ${formatMoney(sup.outstandingDebt, "XOF")}", 
                                    color = if (sup.outstandingDebt > 0) MaterialTheme.colorScheme.error else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                "Total Crédits : ${formatMoney(sup.totalDues, "XOF")} | Total Réglé : ${formatMoney(sup.totalRepaid, "XOF")}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Fermer")
                    }
                } else {
                    val sup = selectedSupplierForPay!!
                    Text(
                        text = "Règlement à : ${sup.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Encours : ${formatMoney(sup.outstandingDebt, "XOF")}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = repayAmount,
                        onValueChange = { repayAmount = it; isErr = false },
                        label = { Text("Montant du paiement") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isErr) {
                        Text("Saisissez un montant valide.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { selectedSupplierForPay = null; repayAmount = "" }) {
                            Text("Retour")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val value = repayAmount.toDoubleOrNull()
                                if (value != null && value > 0) {
                                    onRepay(sup.name, value)
                                    selectedSupplierForPay = null
                                    repayAmount = ""
                                    onDismiss()
                                } else {
                                    isErr = true
                                }
                            }
                        ) {
                            Text("Confirmer règlement")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMovementDialog(
    products: List<Product>,
    viewModel: StockViewModel,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, Double, String, Double, Double, Double, String) -> Unit
) {
    if (products.isEmpty()) {
        Dialog(onDismissRequest = onDismiss) {
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Veuillez d'abord créer un produit !", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onDismiss) { Text("D'accord") }
                }
            }
        }
        return
    }

    var selectedProductCode by remember { mutableStateOf(products.first().code) }
    var type by remember { mutableStateOf("ENTREE") } // "ENTREE", "SORTIE"
    var subType by remember { mutableStateOf("CASH") } 
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var supplierName by remember { mutableStateOf("") }
    var amountPaid by remember { mutableStateOf("") }
    var amountRemaining by remember { mutableStateOf("") }

    var dropdownOpen by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val activeSelectedProd = products.find { it.code == selectedProductCode }
    val currentAvailableStock = remember(selectedProductCode, viewModel) {
        viewModel.getAvailableStock(selectedProductCode)
    }

    val isStockInsufficient = remember(type, subType, quantity, currentAvailableStock) {
        if (type == "SORTIE") {
            val q = quantity.toDoubleOrNull() ?: 0.0
            q > currentAvailableStock
        } else false
    }

    LaunchedEffect(type) {
        subType = if (type == "ENTREE") "CASH" else "VENTE"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Nouvelle Opération de Stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { dropdownOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(activeSelectedProd?.let { "${it.designation} (${it.code})" } ?: "Sélectez un article", color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ArrowDropDown, "dropdown")
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownOpen,
                        onDismissRequest = { dropdownOpen = false },
                        modifier = Modifier.fillMaxWidth(0.8f).heightIn(max = 240.dp)
                    ) {
                        products.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text("${prod.designation} (${prod.code})") },
                                onClick = {
                                    selectedProductCode = prod.code
                                    dropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Stock disponible en rayon : $currentAvailableStock unités",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (currentAvailableStock <= 0) MaterialTheme.colorScheme.error else Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedCard(
                        onClick = { type = "ENTREE" },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (type == "ENTREE") Color(230, 246, 235) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                            Text("ENTRÉE", fontWeight = FontWeight.Bold, color = if (type == "ENTREE") Color(38, 166, 91) else Color.Gray)
                        }
                    }

                    ElevatedCard(
                        onClick = { type = "SORTIE" },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (type == "SORTIE") Color(253, 235, 235) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                            Text("SORTIE", fontWeight = FontWeight.Bold, color = if (type == "SORTIE") MaterialTheme.colorScheme.error else Color.Gray)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (type == "ENTREE") {
                        FilterChip(
                            selected = subType == "CASH",
                            onClick = { subType = "CASH" },
                            label = { Text("Comptant") }
                        )
                        FilterChip(
                            selected = subType == "CREDIT",
                            onClick = { subType = "CREDIT" },
                            label = { Text("Achat à Crédit") }
                        )
                    } else {
                        FilterChip(
                            selected = subType == "VENTE",
                            onClick = { subType = "VENTE" },
                            label = { Text("Vente") }
                        )
                        FilterChip(
                            selected = subType == "PERTE",
                            onClick = { subType = "PERTE" },
                            label = { Text("Perte") }
                        )
                    }
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it; isError = false },
                    label = { Text("Quantité") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isStockInsufficient) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Rupture de Stock ! SEUL $currentAvailableStock en rayon.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (type == "ENTREE" && subType == "CREDIT") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Crédit détails", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            
                            OutlinedTextField(
                                value = supplierName,
                                onValueChange = { supplierName = it },
                                label = { Text("Nom du Fournisseur") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = amountPaid,
                                onValueChange = { amountPaid = it },
                                label = { Text("Acompte donné (FCFA)") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = amountRemaining,
                                onValueChange = { amountRemaining = it },
                                label = { Text("Reste à payer (FCFA)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes optionnelles") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val q = quantity.toDoubleOrNull()
                            if (q == null || q <= 0) {
                                isError = true
                                errorMsg = "Veuillez entrer une quantité valide."
                                return@Button
                            }

                            if (type == "SORTIE" && q > currentAvailableStock) {
                                isError = true
                                errorMsg = "Impossible d'opérer : Rupture de stock."
                                return@Button
                            }

                            val p = amountPaid.toDoubleOrNull() ?: 0.0
                            val r = amountRemaining.toDoubleOrNull() ?: 0.0

                            onAdd(selectedProductCode, type, subType, q, notes, q, p, r, supplierName)
                        },
                        enabled = !isStockInsufficient
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

// ==========================================
// CHANNELS VECTOR BAR CHART (Line Graph Curve Plotter)
// ==========================================
@Composable
fun StockMovementCurveChart(movements: List<Movement>) {
    val months = listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc")
    val periodLabels = remember(movements) {
        val labels = mutableListOf<String>()
        val c = Calendar.getInstance()
        for (i in 5 downTo 0) {
            c.timeInMillis = System.currentTimeMillis()
            c.add(Calendar.MONTH, -i)
            labels.add("${months[c.get(Calendar.MONTH)]} ${c.get(Calendar.YEAR) % 100}")
        }
        labels
    }

    val entries = remember(movements, periodLabels) {
        val list = MutableList(6) { 0.0 }
        movements.forEach { m ->
            val c = Calendar.getInstance()
            c.timeInMillis = m.timestamp
            val key = "${months[c.get(Calendar.MONTH)]} ${c.get(Calendar.YEAR) % 100}"
            val idx = periodLabels.indexOf(key)
            if (idx != -1 && m.type == "ENTREE") list[idx] += m.quantity
        }
        list
    }

    val sorties = remember(movements, periodLabels) {
        val list = MutableList(6) { 0.0 }
        movements.forEach { m ->
            val c = Calendar.getInstance()
            c.timeInMillis = m.timestamp
            val key = "${months[c.get(Calendar.MONTH)]} ${c.get(Calendar.YEAR) % 100}"
            val idx = periodLabels.indexOf(key)
            if (idx != -1 && m.type == "SORTIE") list[idx] += m.quantity
        }
        list
    }

    val maxVal = maxOf(entries.maxOrNull() ?: 1.0, sorties.maxOrNull() ?: 1.0, 10.0)

    val gridLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
    val textPaintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val errorColor = MaterialTheme.colorScheme.error

    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padLeft = 40f
            val padBottom = 30f
            val padTop = 10f
            val padRight = 10f

            val chartWidth = width - padLeft - padRight
            val chartHeight = height - padBottom - padTop

            // Grid lines
            for (i in 0..3) {
                val y = padTop + chartHeight * (1f - i / 3f)
                drawLine(gridLineColor, androidx.compose.ui.geometry.Offset(padLeft, y), androidx.compose.ui.geometry.Offset(width - padRight, y))
            }

            fun getPoint(index: Int, valD: Double): androidx.compose.ui.geometry.Offset {
                val x = padLeft + (index / 5f) * chartWidth
                val y = padTop + chartHeight * (1f - (valD / maxVal).toFloat())
                return androidx.compose.ui.geometry.Offset(x, y)
            }

            for (i in 0..5) {
                val pt = getPoint(i, 0.0)
                drawContext.canvas.nativeCanvas.drawText(
                    periodLabels[i],
                    pt.x - 20f,
                    height - 6f,
                    android.graphics.Paint().apply {
                        color = textPaintColor
                        textSize = 20f
                    }
                )
            }

            val pathEntries = androidx.compose.ui.graphics.Path()
            val pathSorties = androidx.compose.ui.graphics.Path()

            for (i in 0..5) {
                val ptE = getPoint(i, entries[i])
                val ptS = getPoint(i, sorties[i])
                if (i == 0) {
                    pathEntries.moveTo(ptE.x, ptE.y)
                    pathSorties.moveTo(ptS.x, ptS.y)
                } else {
                    pathEntries.lineTo(ptE.x, ptE.y)
                    pathSorties.lineTo(ptS.x, ptS.y)
                }
                drawCircle(Color(38, 166, 91), 4f, ptE)
                drawCircle(errorColor, 4f, ptS)
            }

            drawPath(pathEntries, Color(38, 166, 91), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
            drawPath(pathSorties, errorColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        }
    }
}

// ----------------------------------------------------
// UTILITIES HELPERS
// ----------------------------------------------------
data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

fun formatMoney(amount: Double, symbol: String): String {
    val isCfa = symbol.contains("CFA", ignoreCase = true) || symbol.contains("FCFA", ignoreCase = true) || symbol.contains("XOF", ignoreCase = true)
    return if (isCfa) {
        String.format(Locale.getDefault(), "%,.0f XOF", amount)
    } else {
        String.format(Locale.getDefault(), "%,.2f %s", amount, symbol)
    }
}

fun List<Double>.indexOfMax(): Int? {
    if (isEmpty()) return null
    var maxIdx = 0
    var maxVal = this[0]
    for (i in 1 until size) {
        if (this[i] > maxVal) {
            maxVal = this[i]
            maxIdx = i
        }
    }
    return maxIdx
}
