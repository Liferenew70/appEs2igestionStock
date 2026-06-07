package com.example.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class StockRepository(private val stockDao: StockDao) {

    val allCompanies: Flow<List<Company>> = stockDao.getAllCompanies()

    fun getProductsForCompany(companyId: String): Flow<List<Product>> =
        stockDao.getProductsByCompany(companyId)

    fun getMovementsForCompany(companyId: String): Flow<List<Movement>> =
        stockDao.getMovementsByCompany(companyId)

    suspend fun getCompanyById(id: String): Company? =
        stockDao.getCompanyById(id)

    suspend fun insertCompany(company: Company) {
        stockDao.insertCompany(company)
    }

    suspend fun deleteCompany(id: String) {
        stockDao.deleteCompany(id)
    }

    suspend fun clearCompanies() {
        stockDao.clearCompanies()
    }

    suspend fun insertProduct(product: Product) {
        stockDao.insertProduct(product)
    }

    suspend fun insertProducts(products: List<Product>) {
        stockDao.insertProducts(products)
    }

    suspend fun deleteProduct(companyId: String, code: String) {
        stockDao.deleteProduct(companyId, code)
    }

    suspend fun clearAllProducts(companyId: String) {
        stockDao.clearProductsByCompany(companyId)
    }

    suspend fun insertMovement(movement: Movement) {
        stockDao.insertMovement(movement)
    }

    suspend fun insertMovements(movements: List<Movement>) {
        stockDao.insertMovements(movements)
    }

    suspend fun deleteMovement(id: Int) {
        stockDao.deleteMovement(id)
    }

    suspend fun clearAllMovements(companyId: String) {
        stockDao.clearMovementsByCompany(companyId)
    }

    suspend fun multiplyPrices(companyId: String, multiplier: Double) {
        stockDao.multiplyPrices(companyId, multiplier)
    }

    /**
     * Export all data to JSON String for a given company.
     */
    fun exportToJson(company: Company, products: List<Product>, movements: List<Movement>): String {
        val rootObj = JSONObject()
        
        val compObj = JSONObject()
        compObj.put("id", company.id)
        compObj.put("name", company.name)
        compObj.put("logoUri", company.logoUri)
        compObj.put("currencySymbol", company.currencySymbol)
        compObj.put("customHex", company.customHex)
        compObj.put("conversionRateFromEur", company.conversionRateFromEur)
        rootObj.put("company", compObj)

        val productsArray = JSONArray()
        for (prod in products) {
            val prodObj = JSONObject()
            prodObj.put("code", prod.code)
            prodObj.put("designation", prod.designation)
            prodObj.put("category", prod.category)
            prodObj.put("unitPrice", prod.unitPrice)
            prodObj.put("initialStock", prod.initialStock)
            prodObj.put("purchasePrice", prod.purchasePrice)
            productsArray.put(prodObj)
        }
        rootObj.put("products", productsArray)

        val movementsArray = JSONArray()
        for (mov in movements) {
            val movObj = JSONObject()
            movObj.put("id", mov.id)
            movObj.put("productCode", mov.productCode)
            movObj.put("type", mov.type)
            movObj.put("quantity", mov.quantity)
            movObj.put("timestamp", mov.timestamp)
            movObj.put("notes", mov.notes)
            movObj.put("subType", mov.subType)
            movObj.put("creditQuantity", mov.creditQuantity)
            movObj.put("amountPaid", mov.amountPaid)
            movObj.put("amountRemaining", mov.amountRemaining)
            movObj.put("supplierName", mov.supplierName)
            movementsArray.put(movObj)
        }
        rootObj.put("movements", movementsArray)

        return rootObj.toString(2)
    }

    /**
     * Import data from JSON string.
     * @param targetCompanyId The active company ID to import into.
     * @param overwrite If true, clears current company-specific database tables before inserting.
     *                  If false, appends the products and movements.
     */
    suspend fun importFromJson(companyId: String, jsonString: String, overwrite: Boolean): Boolean {
        try {
            val rootObj = JSONObject(jsonString)
            
            // If the JSON contains its own company object, let's parse it and register him!
            var actualCompanyId = companyId
            if (rootObj.has("company")) {
                val compObj = rootObj.getJSONObject("company")
                val parsedCompId = compObj.optString("id", "")
                val parsedName = compObj.optString("name", "")
                if (parsedCompId.isNotEmpty() && parsedName.isNotEmpty()) {
                    if (overwrite) {
                        actualCompanyId = parsedCompId
                    }
                    val company = Company(
                        id = parsedCompId,
                        name = parsedName,
                        logoUri = compObj.optString("logoUri", ""),
                        currencySymbol = compObj.optString("currencySymbol", "FCFA"),
                        customHex = compObj.optString("customHex", ""),
                        conversionRateFromEur = compObj.optDouble("conversionRateFromEur", 655.957)
                    )
                    stockDao.insertCompany(company)
                }
            }

            val parsedProducts = mutableListOf<Product>()
            val productsArray = rootObj.optJSONArray("products")
            if (productsArray != null) {
                for (i in 0 until productsArray.length()) {
                    val prodObj = productsArray.getJSONObject(i)
                    parsedProducts.add(
                        Product(
                            companyId = actualCompanyId,
                            code = prodObj.getString("code"),
                            designation = prodObj.getString("designation"),
                            category = prodObj.optString("category", "Divers"),
                            unitPrice = prodObj.optDouble("unitPrice", 0.0),
                            initialStock = prodObj.optDouble("initialStock", 0.0),
                            purchasePrice = prodObj.optDouble("purchasePrice", 0.0)
                        )
                    )
                }
            }

            val parsedMovements = mutableListOf<Movement>()
            val movementsArray = rootObj.optJSONArray("movements")
            if (movementsArray != null) {
                for (i in 0 until movementsArray.length()) {
                    val movObj = movementsArray.getJSONObject(i)
                    parsedMovements.add(
                        Movement(
                            id = if (overwrite) movObj.optInt("id", 0) else 0,
                            companyId = actualCompanyId,
                            productCode = movObj.getString("productCode"),
                            type = movObj.getString("type"),
                            quantity = movObj.optDouble("quantity", 0.0),
                            timestamp = movObj.optLong("timestamp", System.currentTimeMillis()),
                            notes = movObj.optString("notes", ""),
                            subType = movObj.optString("subType", ""),
                            creditQuantity = movObj.optDouble("creditQuantity", 0.0),
                            amountPaid = movObj.optDouble("amountPaid", 0.0),
                            amountRemaining = movObj.optDouble("amountRemaining", 0.0),
                            supplierName = movObj.optString("supplierName", "")
                        )
                    )
                }
            }

            if (overwrite) {
                clearAllProducts(actualCompanyId)
                clearAllMovements(actualCompanyId)
            }

            if (parsedProducts.isNotEmpty()) {
                stockDao.insertProducts(parsedProducts)
            }
            if (parsedMovements.isNotEmpty()) {
                stockDao.insertMovements(parsedMovements)
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
