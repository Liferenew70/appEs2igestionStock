package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.ExportUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Calendar
import java.util.Locale

data class ProductUiState(
    val product: Product,
    val totalEntries: Double,
    val totalSorties: Double,
    val finalStock: Double,
    val totalValue: Double,
    val totalSalesQty: Double = 0.0,
    val totalLossQty: Double = 0.0,
    val netProfit: Double = 0.0,
    val debtToSupplier: Double = 0.0,
    val totalNegotiatedGain: Double = 0.0,
    val totalPurchaseLoss: Double = 0.0
)

data class SupplierUiState(
    val name: String,
    val totalDues: Double,          // Sum of amountRemaining of CREDIT entries
    val totalRepaid: Double,         // Sum of quantities marked as repayments
    val outstandingDebt: Double      // dues - repaid
)

@OptIn(ExperimentalCoroutinesApi::class)
class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = StockRepository(database.stockDao())
    private val prefs = application.getSharedPreferences("stock_prefs_v2", Context.MODE_PRIVATE)

    // Flow of listed companies
    val allCompanies: StateFlow<List<Company>> = repository.allCompanies
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Tracks the active company ID
    private val _activeCompanyId = MutableStateFlow(prefs.getString("active_company_id", "") ?: "")
    val activeCompanyId: StateFlow<String> = _activeCompanyId.asStateFlow()

    // Active Company object
    val companyState: StateFlow<Company?> = combine(_activeCompanyId, allCompanies) { id, list ->
        if (id.isEmpty()) null
        else list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Products Segmented for this company
    val productsState: StateFlow<List<Product>> = _activeCompanyId
        .flatMapLatest { id ->
            if (id.isEmpty()) flowOf(emptyList())
            else repository.getProductsForCompany(id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Movements Segmented for this company
    val movementsState: StateFlow<List<Movement>> = _activeCompanyId
        .flatMapLatest { id ->
            if (id.isEmpty()) flowOf(emptyList())
            else repository.getMovementsForCompany(id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Deliveries Flow for active company
    val deliveriesState: StateFlow<List<ProductDelivery>> = _activeCompanyId
        .flatMapLatest { id ->
            if (id.isEmpty()) flowOf(emptyList())
            else repository.getProductDeliveries(id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Invoices Flow for active company
    val invoicesState: StateFlow<List<InvoiceRecord>> = _activeCompanyId
        .flatMapLatest { id ->
            if (id.isEmpty()) flowOf(emptyList())
            else repository.getInvoiceRecords(id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Filters and Preferences Settings
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Tout")

    // Theme state
    val isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val customAccentHex = MutableStateFlow(prefs.getString("custom_accent_hex", "") ?: "")
    val currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "FCFA") ?: "FCFA")

    // Advanced Timeline Filters State for Analytics
    val selectedPeriod = MutableStateFlow("ALL") // "WEEK", "MONTH", "ALL", "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC", "CUSTOM"
    val customRangeStart = MutableStateFlow(0L)
    val customRangeEnd = MutableStateFlow(0L)

    // Live list of unique Suppliers gathered automatically from credit inputs or repayments
    val suppliersList: StateFlow<List<SupplierUiState>> = movementsState.map { movements ->
        val mapDues = mutableMapOf<String, Double>()
        val mapRepaid = mutableMapOf<String, Double>()

        for (m in movements) {
            val sName = m.supplierName.trim()
            if (sName.isEmpty()) continue

            if (m.type == "ENTREE" && m.subType == "CREDIT") {
                mapDues[sName] = (mapDues[sName] ?: 0.0) + m.amountRemaining
            } else if (m.type == "SORTIE" && m.subType == "REGLEMENT_FOURNISSEUR") {
                mapRepaid[sName] = (mapRepaid[sName] ?: 0.0) + m.amountPaid
            }
        }

        val allNames = (mapDues.keys + mapRepaid.keys).distinct().sorted()
        allNames.map { name ->
            val dues = mapDues[name] ?: 0.0
            val repaid = mapRepaid[name] ?: 0.0
            SupplierUiState(
                name = name,
                totalDues = dues,
                totalRepaid = repaid,
                outstandingDebt = maxOf(0.0, dues - repaid)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Combines products and movements to create a calculated UI State representing actual levels
    val productUiStates: StateFlow<List<ProductUiState>> = combine(
        productsState,
        movementsState,
        searchQuery,
        selectedCategory
    ) { products, movements, search, category ->
        val movementsByProduct = movements.groupBy { it.productCode }

        products.map { p ->
            val pMovements = movementsByProduct[p.code] ?: emptyList()
            
            val entries = pMovements.filter { it.type == "ENTREE" && it.subType != "REGLEMENT_FOURNISSEUR" }.sumOf { it.quantity }
            val sorties = pMovements.filter { it.type == "SORTIE" && it.subType != "REGLEMENT_FOURNISSEUR" }.sumOf { it.quantity }
            
            val salesQty = pMovements.filter { it.type == "SORTIE" && (it.subType == "VENTE" || it.subType == "") }.sumOf { it.quantity }
            val lossQty = pMovements.filter { it.type == "SORTIE" && it.subType == "PERTE" }.sumOf { it.quantity }
            
            val initialDues = pMovements.filter { it.type == "ENTREE" && it.subType == "CREDIT" }.sumOf { it.amountRemaining }
            val directRepays = pMovements.filter { it.type == "SORTIE" && it.subType == "REGLEMENT_FOURNISSEUR" }.sumOf { it.amountPaid }
            val debtToSupplier = maxOf(0.0, initialDues - directRepays)

            val finalStock = p.initialStock + entries - sorties
            val totalValue = finalStock * p.unitPrice
            
            // Calculate negotiated gains and purchase losses from entries
            var negotiatedGains = 0.0
            var purchaseLosses = 0.0
            val purchaseEntrees = pMovements.filter { it.type == "ENTREE" && it.subType != "REGLEMENT_FOURNISSEUR" }
            for (m in purchaseEntrees) {
                val expectedCost = m.quantity * p.purchasePrice
                val actualCost = m.amountPaid + m.amountRemaining
                if (m.quantity > 0 && actualCost > 0) {
                    if (actualCost < expectedCost) {
                        negotiatedGains += (expectedCost - actualCost)
                    } else if (actualCost > expectedCost) {
                        purchaseLosses += (actualCost - expectedCost)
                    }
                }
            }
            
            // profit = (sales volume * profit margin) - loss value + negotiated gains - losses from purchase price increases
            val netProfit = (salesQty * (p.unitPrice - p.purchasePrice)) - (lossQty * p.purchasePrice) + negotiatedGains - purchaseLosses

            ProductUiState(
                product = p,
                totalEntries = entries,
                totalSorties = sorties,
                finalStock = finalStock,
                totalValue = totalValue,
                totalSalesQty = salesQty,
                totalLossQty = lossQty,
                netProfit = netProfit,
                debtToSupplier = debtToSupplier,
                totalNegotiatedGain = negotiatedGains,
                totalPurchaseLoss = purchaseLosses
            )
        }.filter { uiState ->
            val matchesCategory = category == "Tout" || uiState.product.category.equals(category, ignoreCase = true)
            
            val matchesSearch = search.isEmpty() || 
                    uiState.product.code.contains(search, ignoreCase = true) ||
                    uiState.product.designation.contains(search, ignoreCase = true) ||
                    uiState.product.category.contains(search, ignoreCase = true)

            matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All distinct categories dynamically collected from active products list
    val categoriesState: StateFlow<List<String>> = productsState
        .map { list ->
            val cats = list.map { it.category }.distinct().sorted()
            listOf("Tout") + cats
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Tout"))

    fun toggleDarkMode() {
        val newValue = !isDarkMode.value
        isDarkMode.value = newValue
        prefs.edit().putBoolean("dark_mode", newValue).apply()
    }

    // Set Custom Theme Accent color HEX style
    fun updateCustomAccentHex(hex: String) {
        customAccentHex.value = hex
        prefs.edit().putString("custom_accent_hex", hex).apply()
        
        // Update active database object to preserve config
        val currentCompany = companyState.value
        if (currentCompany != null) {
            viewModelScope.launch {
                repository.insertCompany(currentCompany.copy(customHex = hex))
            }
        }
    }

    // Change company active Currency symbols
    fun updateCurrencySettings(newSymbol: String, rateFromEur: Double) {
        currencySymbol.value = newSymbol
        prefs.edit().putString("currency_symbol", newSymbol).apply()
        
        val currentCompany = companyState.value
        if (currentCompany != null) {
            viewModelScope.launch {
                repository.insertCompany(currentCompany.copy(
                    currencySymbol = newSymbol,
                    conversionRateFromEur = rateFromEur
                ))
            }
        }
    }

    // Login or select active company profile
    fun selectCompany(companyId: String) {
        val trimmed = companyId.trim()
        val match = allCompanies.value.find {
            it.id.trim().lowercase(Locale.ROOT) == trimmed.lowercase(Locale.ROOT) ||
            it.id.trim().replace("\\s+".toRegex(), "_").lowercase(Locale.ROOT) == trimmed.replace("\\s+".toRegex(), "_").lowercase(Locale.ROOT)
        }
        val targetId = match?.id ?: trimmed
        
        if (targetId.isNotEmpty()) {
            _activeCompanyId.value = targetId
            prefs.edit().putString("active_company_id", targetId).apply()
            
            // Sync preferences
            viewModelScope.launch {
                val comp = repository.getCompanyById(targetId)
                if (comp != null) {
                    currencySymbol.value = comp.currencySymbol
                    customAccentHex.value = comp.customHex
                    prefs.edit()
                        .putString("currency_symbol", comp.currencySymbol)
                        .putString("custom_accent_hex", comp.customHex)
                        .apply()
                }
            }
        }
    }

    // Create a new company
    fun createAndSelectCompany(name: String, logo: String = "STORE", currency: String = "FCFA") {
        val id = name.trim().replace("\\s+".toRegex(), "_").lowercase(Locale.ROOT)
        val comp = Company(
            id = id,
            name = name.trim(),
            logoUri = logo,
            currencySymbol = currency,
            customHex = ""
        )
        viewModelScope.launch {
            repository.insertCompany(comp)
            selectCompany(id)
        }
    }

    // Logouts the user, resetting active state, which invokes landing onboarding selector
    fun disconnect() {
        _activeCompanyId.value = ""
        prefs.edit().putString("active_company_id", "").apply()
    }

    // Deletes an entire company profile
    fun deleteCompanyProfile(compId: String) {
        viewModelScope.launch {
            repository.clearAllProducts(compId)
            repository.clearAllMovements(compId)
            repository.deleteCompany(compId)
            if (_activeCompanyId.value == compId) {
                disconnect()
            }
        }
    }

    // Change Company Logo symbol
    fun updateCompanyLogo(logo: String) {
        val currentCompany = companyState.value
        if (currentCompany != null) {
            viewModelScope.launch {
                repository.insertCompany(currentCompany.copy(logoUri = logo))
            }
        }
    }

    // Add Product Actions
    fun addProduct(
        code: String,
        designation: String,
        category: String,
        unitPrice: Double,
        initialStock: Double,
        purchasePrice: Double = 0.0
    ) {
        val cId = _activeCompanyId.value
        if (cId.isEmpty()) return
        viewModelScope.launch {
            val prod = Product(
                companyId = cId,
                code = code,
                designation = designation,
                category = category,
                unitPrice = unitPrice,
                initialStock = initialStock,
                purchasePrice = purchasePrice
            )
            repository.insertProduct(prod)
        }
    }

    fun updateProduct(
        code: String,
        designation: String,
        category: String,
        unitPrice: Double,
        initialStock: Double,
        purchasePrice: Double
    ) {
        val cId = _activeCompanyId.value
        if (cId.isEmpty()) return
        viewModelScope.launch {
            val prod = Product(
                companyId = cId,
                code = code,
                designation = designation,
                category = category,
                unitPrice = unitPrice,
                initialStock = initialStock,
                purchasePrice = purchasePrice
            )
            repository.insertProduct(prod)
        }
    }

    fun deleteProduct(code: String) {
        val cId = _activeCompanyId.value
        if (cId.isEmpty()) return
        viewModelScope.launch {
            repository.deleteProduct(cId, code)
        }
    }

    // Check available stock helper before saving operations
    fun getAvailableStock(productCode: String): Double {
        val pState = productUiStates.value.find { it.product.code == productCode }
        return pState?.finalStock ?: 0.0
    }

    // Saisie des mouvements (Entrées / Sorties / Repayments)
    fun addMovement(
        productCode: String,
        type: String,
        quantity: Double,
        notes: String,
        subType: String = "",
        creditQuantity: Double = 0.0,
        amountPaid: Double = 0.0,
        amountRemaining: Double = 0.0,
        supplierName: String = ""
    ) {
        val cId = _activeCompanyId.value
        if (cId.isEmpty()) return
        viewModelScope.launch {
            val mov = Movement(
                companyId = cId,
                productCode = productCode,
                type = type.uppercase(),
                quantity = quantity,
                timestamp = System.currentTimeMillis(),
                notes = notes,
                subType = subType,
                creditQuantity = creditQuantity,
                amountPaid = amountPaid,
                amountRemaining = amountRemaining,
                supplierName = supplierName
            )
            repository.insertMovement(mov)
        }
    }

    fun updateMovement(
        id: Int,
        productCode: String,
        type: String,
        quantity: Double,
        timestamp: Long,
        notes: String,
        subType: String = "",
        creditQuantity: Double = 0.0,
        amountPaid: Double = 0.0,
        amountRemaining: Double = 0.0,
        supplierName: String = ""
    ) {
        val cId = _activeCompanyId.value
        if (cId.isEmpty()) return
        viewModelScope.launch {
            val mov = Movement(
                id = id,
                companyId = cId,
                productCode = productCode,
                type = type.uppercase(),
                quantity = quantity,
                timestamp = timestamp,
                notes = notes,
                subType = subType,
                creditQuantity = creditQuantity,
                amountPaid = amountPaid,
                amountRemaining = amountRemaining,
                supplierName = supplierName
            )
            repository.insertMovement(mov)
        }
    }

    fun deleteMovement(id: Int) {
        viewModelScope.launch {
            repository.deleteMovement(id)
        }
    }

    // Delivery settings & tracking actions
    fun addOrUpdateDelivery(productCode: String, daysOfDelay: Int, orderedQty: Double = 0.0, isOrdered: Boolean = false, expectedSecs: Long = 0L) {
        val cId = _activeCompanyId.value
        if (cId.isEmpty()) return
        viewModelScope.launch {
            repository.insertProductDelivery(
                ProductDelivery(
                    companyId = cId,
                    productCode = productCode,
                    deliveryDays = daysOfDelay,
                    orderedQuantity = orderedQty,
                    isOrdered = isOrdered,
                    expectedDeliveryDate = expectedSecs
                )
            )
        }
    }

    fun deleteDelivery(productCode: String) {
        val cId = _activeCompanyId.value
        if (cId.isEmpty()) return
        viewModelScope.launch {
            repository.deleteProductDelivery(cId, productCode)
        }
    }

    // Invoice tracking action
    fun addInvoice(clientName: String, clientPhone: String, subType: String, productsJson: String, totalAmount: Double) {
        val cId = _activeCompanyId.value
        if (cId.isEmpty()) return
        viewModelScope.launch {
            repository.insertInvoiceRecord(
                InvoiceRecord(
                    companyId = cId,
                    clientName = clientName.trim(),
                    clientPhone = clientPhone.trim(),
                    timestamp = System.currentTimeMillis(),
                    subType = subType,
                    productsJson = productsJson,
                    totalAmount = totalAmount
                )
            )
        }
    }

    fun getCurrentMonthAvailableBalance(): Double {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)

        val prods = productsState.value
        val movs = movementsState.value

        val cal = Calendar.getInstance()

        // 1. Calculate current month's sales
        var salesSum = 0.0
        val salesMovs = movs.filter { 
            it.type == "SORTIE" && 
            (it.subType == "VENTE" || it.subType.isEmpty()) 
        }
        for (m in salesMovs) {
            cal.timeInMillis = m.timestamp
            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth) {
                val p = prods.find { it.code == m.productCode }
                if (p != null) {
                    salesSum += m.quantity * p.unitPrice
                }
            }
        }

        // 2. Subtract current month's supplier repayments
        var repaymentSum = 0.0
        val repaymentMovs = movs.filter { 
            it.type == "SORTIE" && 
            it.subType == "REGLEMENT_FOURNISSEUR" 
        }
        for (m in repaymentMovs) {
            cal.timeInMillis = m.timestamp
            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth) {
                repaymentSum += m.amountPaid
            }
        }

        return maxOf(0.0, salesSum - repaymentSum)
    }

    // Direct Supplier Payment Operation (Deducted from general cash index or reduces supplier credit)
    fun makeSupplierRepayment(supplierName: String, amountPaid: Double, sourceProductCode: String = ""): Boolean {
        val cId = _activeCompanyId.value
        if (cId.isEmpty()) return false
        
        val available = getCurrentMonthAvailableBalance()
        if (amountPaid > available) {
            return false // Balance insufficient for this cycle!
        }

        viewModelScope.launch {
            // If the user chooses a specific product, we link the repayment to that product's code
            val code = sourceProductCode.ifEmpty {
                productsState.value.firstOrNull()?.code ?: "00000"
            }
            
            val mov = Movement(
                companyId = cId,
                productCode = code,
                type = "SORTIE",
                quantity = 0.0, // Represents monetary payout
                timestamp = System.currentTimeMillis(),
                notes = "Règlement Fournisseur: $supplierName",
                subType = "REGLEMENT_FOURNISSEUR",
                amountPaid = amountPaid,
                amountRemaining = 0.0,
                supplierName = supplierName
            )
            repository.insertMovement(mov)
        }
        return true
    }

    // Clear All Company Specific Data
    fun clearAllData() {
        val cId = _activeCompanyId.value
        if (cId.isEmpty()) return
        viewModelScope.launch {
            repository.clearAllProducts(cId)
            repository.clearAllMovements(cId)
        }
    }

    // Shared Exports utilities (shares files locally through FileProvider)
    fun shareJson(context: Context) {
        viewModelScope.launch {
            val comp = companyState.value ?: return@launch
            val jsonStr = repository.exportToJson(comp, productsState.value, movementsState.value)
            try {
                val file = File(context.cacheDir, "backup_${comp.id}.json")
                file.writeText(jsonStr)
                shareFile(context, file, "application/json")
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur export JSON: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareCsv(context: Context) {
        val comp = companyState.value ?: return
        val csvStr = ExportUtils.generateCsv(productsState.value, movementsState.value, currencySymbol.value)
        try {
            // Saved directly with .xlsx prefix so modern spreadsheets open with complete alignment and encoding
            val file = File(context.cacheDir, "inventaire_${comp.id}.xlsx")
            file.writeText(csvStr, Charsets.UTF_8)
            shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur export spreadsheet: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf(context: Context) {
        val comp = companyState.value ?: return
        val file = File(context.cacheDir, "rapport_${comp.id}.pdf")
        
        val success = ExportUtils.generatePdf(
            context = context,
            file = file,
            companyName = comp.name,
            products = productsState.value,
            movements = movementsState.value
        )
        
        if (success) {
            shareFile(context, file, "application/pdf")
        } else {
            Toast.makeText(context, "Erreur lors de la génération du PDF", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareInvoicePdf(context: Context, invoice: InvoiceRecord) {
        val comp = companyState.value ?: return
        val file = File(context.cacheDir, "facture_${invoice.id}.pdf")
        
        val success = ExportUtils.generateInvoicePdf(
            context = context,
            file = file,
            companyName = comp.name,
            clientName = invoice.clientName,
            clientPhone = invoice.clientPhone,
            timestamp = invoice.timestamp,
            totalAmount = invoice.totalAmount,
            subType = invoice.subType,
            productsJson = invoice.productsJson,
            currencySymbol = currencySymbol.value
        )
        
        if (success) {
            shareFile(context, file, "application/pdf")
        } else {
            Toast.makeText(context, "Erreur lors de la génération du PDF de la facture", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.gestionstocks.hqytxk.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Partage d'Export Stocks & Inventaires")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "Partager avec l'application")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erreur de partage: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun importJsonBackup(context: Context, uri: Uri, overwrite: Boolean) {
        viewModelScope.launch {
            val cId = _activeCompanyId.value
            if (cId.isEmpty()) return@launch
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val sb = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    sb.append(line)
                    line = reader.readLine()
                }
                inputStream?.close()

                val success = repository.importFromJson(cId, sb.toString(), overwrite)
                if (success) {
                    Toast.makeText(context, "Importation réussie !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Format JSON d'importation invalide", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur d'importation: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun loadDemoData() {
        val cId = "cave_premium"
        _activeCompanyId.value = cId
        prefs.edit().putString("active_company_id", cId).apply()

        viewModelScope.launch {
            repository.insertCompany(Company(
                id = cId,
                name = "Cave Premium de l'Ouest",
                logoUri = "WINE",
                currencySymbol = "FCFA",
                customHex = "#800020"
            ))

            val demoProducts = listOf(
                Product(cId, "00001", "Château Margaux 2018", "Vins", 125000.0, 30.0, purchasePrice = 85000.0),
                Product(cId, "00002", "Pack Heineken Premium", "Bières", 15000.0, 25.0, purchasePrice = 11000.0),
                Product(cId, "00003", "Dom Pérignon Prestige", "Champagnes", 185000.0, 10.0, purchasePrice = 135000.0),
                Product(cId, "00004", "Coca-Cola Pack x24", "Sans Alcool", 4500.0, 150.0, purchasePrice = 3000.0),
                Product(cId, "00005", "Saint-Émilion Grand Cru", "Vins", 35000.0, 50.0, purchasePrice = 22000.0),
                Product(cId, "00006", "Jus de Mangue local 1L", "Jus", 1500.0, 200.0, purchasePrice = 800.0)
            )
            repository.insertProducts(demoProducts)

            val ms = System.currentTimeMillis()
            val hourMs = 60 * 60 * 1000L
            val dayMs = 24 * hourMs

            val demoMovements = listOf(
                Movement(companyId = cId, productCode = "00001", type = "ENTREE", quantity = 15.0, timestamp = ms - 5 * dayMs, notes = "Livraison Directe", subType = "CASH", amountPaid = 1275000.0, supplierName = "Vignobles de Bordeaux"),
                Movement(companyId = cId, productCode = "00001", type = "SORTIE", quantity = 8.0, timestamp = ms - 4 * dayMs, notes = "Vente Restaurant", subType = "VENTE"),
                Movement(companyId = cId, productCode = "00002", type = "SORTIE", quantity = 10.0, timestamp = ms - 3 * dayMs, notes = "Soirée Gala", subType = "VENTE"),
                Movement(companyId = cId, productCode = "00003", type = "ENTREE", quantity = 5.0, timestamp = ms - 2 * dayMs, notes = "Saisie acompte credit", subType = "CREDIT", creditQuantity = 5.0, amountPaid = 300000.0, amountRemaining = 375000.0, supplierName = "Grossiste Reims"),
                Movement(companyId = cId, productCode = "00004", type = "SORTIE", quantity = 40.0, timestamp = ms - 1 * dayMs, notes = "Livraison Cocktail", subType = "VENTE"),
                Movement(companyId = cId, productCode = "00005", type = "ENTREE", quantity = 20.0, timestamp = ms - 12 * hourMs, notes = "Réassort Cave", subType = "CASH", amountPaid = 440000.0, supplierName = "Vignobles de Bordeaux"),
                Movement(companyId = cId, productCode = "00006", type = "SORTIE", quantity = 15.0, timestamp = ms - 3 * hourMs, notes = "Bouteilles cassées transport", subType = "PERTE")
            )
            repository.insertMovements(demoMovements)
        }
    }
}
