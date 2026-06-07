package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "companies")
data class Company(
    @PrimaryKey val id: String, // Unique identifier (e.g., lowercase trimmed name)
    val name: String,
    val logoUri: String = "", // Local image URI or predefined vector symbol name ("WINE", "BEER", "STORE", "WAREHOUSE", "BOX")
    val currencySymbol: String = "FCFA",
    val customHex: String = "",
    val conversionRateFromEur: Double = 655.957 // Default conversion helper (e.g. 1 EUR = 655.957 CFA)
)

@Entity(tableName = "products", primaryKeys = ["companyId", "code"])
data class Product(
    val companyId: String,
    val code: String,
    val designation: String,
    val category: String,
    val unitPrice: Double,
    val initialStock: Double,
    val purchasePrice: Double = 0.0
)

@Entity(tableName = "movements")
data class Movement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyId: String,
    val productCode: String,
    val type: String, // "ENTREE" or "SORTIE"
    val quantity: Double,
    val timestamp: Long,
    val notes: String = "",
    val subType: String = "", // For SORTIE: "VENTE" or "PERTE". For ENTREE: "CASH" or "CREDIT". Also "REGLEMENT_FOURNISSEUR"
    val creditQuantity: Double = 0.0,
    val amountPaid: Double = 0.0,
    val amountRemaining: Double = 0.0,
    val supplierName: String = "" // Tracks which supplier this movement is associated with
)

@Dao
interface StockDao {
    // Multi-company Profiles
    @Query("SELECT * FROM companies ORDER BY name ASC")
    fun getAllCompanies(): Flow<List<Company>>

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    suspend fun getCompanyById(id: String): Company?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: Company)

    @Query("DELETE FROM companies WHERE id = :id")
    suspend fun deleteCompany(id: String)

    @Query("DELETE FROM companies")
    suspend fun clearCompanies()

    // Products (segmented by company)
    @Query("SELECT * FROM products WHERE companyId = :companyId ORDER BY designation ASC")
    fun getProductsByCompany(companyId: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Query("DELETE FROM products WHERE companyId = :companyId AND code = :code")
    suspend fun deleteProduct(companyId: String, code: String)

    @Query("DELETE FROM products WHERE companyId = :companyId")
    suspend fun clearProductsByCompany(companyId: String)

    // Movements (segmented by company)
    @Query("SELECT * FROM movements WHERE companyId = :companyId ORDER BY timestamp DESC")
    fun getMovementsByCompany(companyId: String): Flow<List<Movement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: Movement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<Movement>)

    @Query("DELETE FROM movements WHERE id = :id")
    suspend fun deleteMovement(id: Int)

    @Query("DELETE FROM movements WHERE companyId = :companyId")
    suspend fun clearMovementsByCompany(companyId: String)

    @Query("UPDATE products SET unitPrice = unitPrice * :multiplier WHERE companyId = :companyId")
    suspend fun multiplyPrices(companyId: String, multiplier: Double)
}

@Database(entities = [Company::class, Product::class, Movement::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stock_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
