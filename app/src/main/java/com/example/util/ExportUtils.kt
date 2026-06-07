package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.Movement
import com.example.data.Product
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    /**
     * Generates a premium Excel CSV workbook string (UTF-8 with BOM & Semicolon delimited)
     * displaying totals, sales, and net profits per product and overall.
     */
    fun generateCsv(products: List<Product>, movements: List<Movement>, currency: String): String {
        val sb = StringBuilder()
        
        // Add UTF-8 Byte Order Mark (BOM) so Excel opens it with perfect automatic column sorting and encodings
        sb.append('\uFEFF')
        
        // Document Header Titles
        sb.append("RAPPORT D'INVENTAIRE ET DE COMPTABILITÉ COMMERCIALE\n")
        sb.append("Date du Rapport;${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("Devise active;$currency\n\n")

        // Headers
        sb.append("Code Article;Designation;Categorie;Prix Achat (P.A.);Prix Vente (P.U.);Stock Initial;Entrees (Qte);Sorties Ventes (Qte);Pertes (Qte);Stock Final;Valeur de Stock (P.U. * Stock Final);Ventes Totales Realisees;Benefice des Ventes;Valeur des Pertes;Benefice Net\n")

        val entriesMap = movements.filter { it.type == "ENTREE" && it.subType != "REGLEMENT_FOURNISSEUR" }.groupBy { it.productCode }

        var grandTotalStock = 0.0
        var grandTotalValuation = 0.0
        var grandTotalSales = 0.0
        var grandTotalSalesProfit = 0.0
        var grandTotalLosses = 0.0
        var grandTotalProfit = 0.0

        for (p in products) {
            val pMovements = movements.filter { it.productCode == p.code }
            
            val totalEntries = entriesMap[p.code]?.sumOf { it.quantity } ?: 0.0
            
            val salesQty = pMovements.filter { m -> m.type == "SORTIE" && (m.subType == "VENTE" || m.subType.isEmpty()) }.sumOf { it.quantity }
            val lossQty = pMovements.filter { m -> m.type == "SORTIE" && m.subType == "PERTE" }.sumOf { it.quantity }
            val totalSorties = salesQty + lossQty

            val finalStock = p.initialStock + totalEntries - totalSorties
            val stockValue = finalStock * p.unitPrice
            
            val totalSalesValue = salesQty * p.unitPrice
            val salesProfitValue = salesQty * (p.unitPrice - p.purchasePrice)
            val lossValue = lossQty * p.purchasePrice
            val netProfitValue = salesProfitValue - lossValue

            grandTotalStock += finalStock
            grandTotalValuation += stockValue
            grandTotalSales += totalSalesValue
            grandTotalSalesProfit += salesProfitValue
            grandTotalLosses += lossValue
            grandTotalProfit += netProfitValue

            sb.append("${p.code};")
            sb.append("${p.designation.replace(";", ",")};")
            sb.append("${p.category.replace(";", ",")};")
            sb.append(String.format(Locale.US, "%.2f", p.purchasePrice) + ";")
            sb.append(String.format(Locale.US, "%.2f", p.unitPrice) + ";")
            sb.append(String.format(Locale.US, "%.1f", p.initialStock) + ";")
            sb.append(String.format(Locale.US, "%.1f", totalEntries) + ";")
            sb.append(String.format(Locale.US, "%.1f", salesQty) + ";")
            sb.append(String.format(Locale.US, "%.1f", lossQty) + ";")
            sb.append(String.format(Locale.US, "%.1f", finalStock) + ";")
            sb.append(String.format(Locale.US, "%.2f", stockValue) + ";")
            sb.append(String.format(Locale.US, "%.2f", totalSalesValue) + ";")
            sb.append(String.format(Locale.US, "%.2f", salesProfitValue) + ";")
            sb.append(String.format(Locale.US, "%.2f", lossValue) + ";")
            sb.append(String.format(Locale.US, "%.2f", netProfitValue) + "\n")
        }

        // Add blank separator lines
        sb.append("\n")
        
        // Sum Overall Grand Totals
        sb.append("TOTAL RECAPITULATIF DES PRODUITS;;;;;;\n")
        sb.append("Volume Total en Stock;** ${String.format(Locale.US, "%.1f", grandTotalStock)} **\n")
        sb.append("Valorisation Totale du Stock;** ${String.format(Locale.US, "%.2f", grandTotalValuation)} $currency **\n")
        sb.append("Accumulation Totale des Ventes;** ${String.format(Locale.US, "%.2f", grandTotalSales)} $currency **\n")
        sb.append("Total Benefices des Ventes;** ${String.format(Locale.US, "%.2f", grandTotalSalesProfit)} $currency **\n")
        sb.append("Total Valeur des Pertes de Stocks;** ${String.format(Locale.US, "%.2f", grandTotalLosses)} $currency **\n")
        sb.append("Total Benefices Net Realises;** ${String.format(Locale.US, "%.2f", grandTotalProfit)} $currency **\n")

        return sb.toString()
    }

    /**
     * Generates an executive-level responsive A4 Landscape PDF Document containing inventory tables and financial statistics.
     * Supports multiple pages automatically.
     */
    fun generatePdf(
        context: Context,
        file: File,
        companyName: String,
        products: List<Product>,
        movements: List<Movement>
    ): Boolean {
        try {
            val pdfDoc = PdfDocument()

            // Page dimensions (A4 Landscape = 842 x 595 points)
            val pageWidth = 842
            val pageHeight = 595
            val margin = 36f
            val printableWidth = pageWidth - (margin * 2)

            val entriesMap = movements.filter { it.type == "ENTREE" && it.subType != "REGLEMENT_FOURNISSEUR" }.groupBy { it.productCode }

            data class RowData(
                val code: String,
                val designation: String,
                val category: String,
                val purchasePriceStr: String,
                val sellingPriceStr: String,
                val finalStockStr: String,
                val totalSalesStr: String,
                val salesProfitStr: String,
                val lossValueStr: String,
                val netProfitStr: String,
                val rawValue: Double,
                val rawSales: Double,
                val rawSalesProfit: Double,
                val rawLoss: Double,
                val rawProfit: Double
            )

            var totalValorization = 0.0
            var cumulativeSales = 0.0
            var cumulativeSalesProfits = 0.0
            var cumulativeLosses = 0.0
            var cumulativeProfits = 0.0

            val rows = products.map { p ->
                val pMovements = movements.filter { it.productCode == p.code }
                
                val totalEntries = entriesMap[p.code]?.sumOf { it.quantity } ?: 0.0
                val salesQty = pMovements.filter { m -> m.type == "SORTIE" && (m.subType == "VENTE" || m.subType.isEmpty()) }.sumOf { it.quantity }
                val lossQty = pMovements.filter { m -> m.type == "SORTIE" && m.subType == "PERTE" }.sumOf { it.quantity }
                val totalSorties = salesQty + lossQty

                val finalStock = p.initialStock + totalEntries - totalSorties
                val stockValue = finalStock * p.unitPrice
                val totalSales = salesQty * p.unitPrice
                val salesProfit = salesQty * (p.unitPrice - p.purchasePrice)
                val lossValue = lossQty * p.purchasePrice
                val netProfit = salesProfit - lossValue

                totalValorization += stockValue
                cumulativeSales += totalSales
                cumulativeSalesProfits += salesProfit
                cumulativeLosses += lossValue
                cumulativeProfits += netProfit

                RowData(
                    code = p.code,
                    designation = p.designation,
                    category = p.category,
                    purchasePriceStr = String.format(Locale.getDefault(), "%,.0f", p.purchasePrice),
                    sellingPriceStr = String.format(Locale.getDefault(), "%,.0f", p.unitPrice),
                    finalStockStr = String.format(Locale.getDefault(), "%,.1f", finalStock),
                    totalSalesStr = String.format(Locale.getDefault(), "%,.0f", totalSales),
                    salesProfitStr = String.format(Locale.getDefault(), "%,.0f", salesProfit),
                    lossValueStr = String.format(Locale.getDefault(), "%,.0f", lossValue),
                    netProfitStr = String.format(Locale.getDefault(), "%,.0f", netProfit),
                    rawValue = stockValue,
                    rawSales = totalSales,
                    rawSalesProfit = salesProfit,
                    rawLoss = lossValue,
                    rawProfit = netProfit
                )
            }

            // Paints configuration
            val titlePaint = Paint().apply {
                color = Color.rgb(128, 0, 32) // Bordeaux Accent
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val metaPaint = Paint().apply {
                color = Color.rgb(60, 60, 65)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(128, 0, 32) // Bordeaux Header
                style = Paint.Style.FILL
            }

            val zebraPaint = Paint().apply {
                color = Color.rgb(253, 246, 246) // Soft Bordeaux zebra tint
                style = Paint.Style.FILL
            }

            val gridPaint = Paint().apply {
                color = Color.rgb(235, 220, 220)
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val cellTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val cellTextBoldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val profitTextPaint = Paint().apply {
                color = Color.rgb(38, 166, 91)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val totalBoxPaint = Paint().apply {
                color = Color.rgb(253, 245, 245)
                style = Paint.Style.FILL
            }

            val tableHeaders = arrayOf("Code", "Designation", "P.A.", "P.U. Vente", "Stock Fin", "Ventes (Val)", "Ben. Ventes", "Pertes Stock", "Ben. Net")
            
            // Calculate relative responsive columns out of printableWidth (770 pt available)
            val widthsScale = floatArrayOf(0.10f, 0.22f, 0.08f, 0.08f, 0.08f, 0.11f, 0.11f, 0.10f, 0.12f)
            val colWidths = widthsScale.map { it * printableWidth }

            val rowHeight = 22f
            val headerHeight = 26f
            val contentStartY = 130f
            val bottomMargin = 50f
            val maxDrawHeight = pageHeight - bottomMargin

            var currentItemIndex = 0
            var pageNumber = 1
            val totalItems = rows.size
            
            while (currentItemIndex < totalItems || totalItems == 0) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                // Draw Top Header Decor Line
                val bannerPaint = Paint().apply {
                    color = Color.rgb(128, 0, 32)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, bannerPaint)

                // Title and Metadata
                canvas.drawText("RAPPORT GÉNÉRAL D'INVENTAIRE ET DE COMPTABILITÉ FINANCIÈRE", margin, 42f, titlePaint)
                canvas.drawText("Entreprise: ${companyName.uppercase()}", margin, 62f, metaPaint)
                
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                canvas.drawText("Date d'édition: $dateStr", margin, 78f, metaPaint)
                canvas.drawText("Page: $pageNumber", pageWidth - margin - 50f, 78f, metaPaint)

                // Draw Table Headers background
                canvas.drawRect(margin, contentStartY - headerHeight, pageWidth - margin, contentStartY, headerBgPaint)

                // Draw Table Header labels
                var currentX = margin
                for (i in tableHeaders.indices) {
                    val colWidth = colWidths[i]
                    canvas.drawText(
                        tableHeaders[i],
                        currentX + 5f,
                        contentStartY - headerHeight / 2f + 4f,
                        headerTextPaint
                    )
                    currentX += colWidth
                }

                var currentY = contentStartY
                
                if (totalItems == 0) {
                    canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, zebraPaint)
                    canvas.drawText("Aucun produit enregistré pour le moment.", margin + 12f, currentY + 14f, cellTextPaint)
                    canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, gridPaint)
                    currentY += rowHeight
                } else {
                    while (currentItemIndex < totalItems && (currentY + rowHeight) <= maxDrawHeight) {
                        val row = rows[currentItemIndex]

                        // Alternating colors
                        if (currentItemIndex % 2 == 1) {
                            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, zebraPaint)
                        }

                        // Cell lines
                        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, gridPaint)

                        // Write item cell values
                        var cellX = margin
                        
                        // 1. Code
                        canvas.drawText(row.code, cellX + 4f, currentY + 14f, cellTextBoldPaint)
                        cellX += colWidths[0]

                        // 2. Designation
                        var desc = row.designation
                        if (cellTextPaint.measureText(desc) > colWidths[1] - 8f) {
                            while (desc.isNotEmpty() && cellTextPaint.measureText("$desc...") > colWidths[1] - 8f) {
                                desc = desc.dropLast(1)
                            }
                            desc = "$desc..."
                        }
                        canvas.drawText(desc, cellX + 4f, currentY + 14f, cellTextPaint)
                        cellX += colWidths[1]

                        // 3. P.A.
                        canvas.drawText(row.purchasePriceStr, cellX + 4f, currentY + 14f, cellTextPaint)
                        cellX += colWidths[2]

                        // 4. P.U. Vente
                        canvas.drawText(row.sellingPriceStr, cellX + 4f, currentY + 14f, cellTextPaint)
                        cellX += colWidths[3]

                        // 5. Stock Final
                        canvas.drawText(row.finalStockStr, cellX + 4f, currentY + 14f, cellTextPaint)
                        cellX += colWidths[4]

                        // 6. Ventes
                        canvas.drawText(row.totalSalesStr, cellX + 4f, currentY + 14f, cellTextBoldPaint)
                        cellX += colWidths[5]

                        // 7. Ben Ventes
                        canvas.drawText(row.salesProfitStr, cellX + 4f, currentY + 14f, cellTextPaint)
                        cellX += colWidths[6]

                        // 8. Pertes
                        canvas.drawText(row.lossValueStr, cellX + 4f, currentY + 14f, cellTextPaint)
                        cellX += colWidths[7]

                        // 9. Net Profit
                        val profitPaintToUse = if (row.rawProfit >= 0) profitTextPaint else Paint().apply {
                            color = Color.rgb(220, 53, 69)
                            textSize = 9.5f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            isAntiAlias = true
                        }
                        canvas.drawText(row.netProfitStr, cellX + 4f, currentY + 14f, profitPaintToUse)

                        currentY += rowHeight
                        currentItemIndex++
                    }
                }

                // If this is the last page, draw the Totals and Footer Summary
                if (currentItemIndex >= totalItems) {
                    val finalBoxHeight = 55f
                    if (currentY + finalBoxHeight + 10f <= pageHeight - 30f) {
                        canvas.drawRect(margin, currentY + 10f, pageWidth - margin, currentY + 10f + finalBoxHeight, totalBoxPaint)
                        canvas.drawRect(margin, currentY + 10f, pageWidth - margin, currentY + 10f + finalBoxHeight, gridPaint)

                        val prefs = context.getSharedPreferences("stock_prefs_v2", Context.MODE_PRIVATE)
                        val currSymbol = prefs.getString("currency_symbol", "FCFA") ?: "FCFA"
                        
                        val totalValStr = "Valorisation Stock: ${String.format(Locale.getDefault(), "%,.0f", totalValorization)} $currSymbol"
                        val totalSalesStr = "Ventes: ${String.format(Locale.getDefault(), "%,.0f", cumulativeSales)} $currSymbol"
                        val totalBenStr = "Bénéfice Ventes: ${String.format(Locale.getDefault(), "%,.0f", cumulativeSalesProfits)} $currSymbol"
                        val totalLossStr = "Pertes Stock: ${String.format(Locale.getDefault(), "%,.0f", cumulativeLosses)} $currSymbol"
                        val totalProfitStr = "Bénéfice Net: ${String.format(Locale.getDefault(), "%,.0f", cumulativeProfits)} $currSymbol"

                        val redPaint = Paint().apply {
                            color = Color.rgb(220, 53, 69)
                            textSize = 9.5f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            isAntiAlias = true
                        }

                        canvas.drawText("TOTAL GENERAL ($totalItems Articles) :", margin + 12f, currentY + 26f, cellTextBoldPaint)
                        canvas.drawText(totalValStr, margin + 12f, currentY + 44f, cellTextBoldPaint)
                        canvas.drawText(totalSalesStr, margin + 260f, currentY + 26f, cellTextBoldPaint)
                        canvas.drawText(totalBenStr, margin + 260f, currentY + 44f, cellTextBoldPaint)
                        canvas.drawText(totalLossStr, margin + 510f, currentY + 26f, redPaint)
                        canvas.drawText(totalProfitStr, margin + 510f, currentY + 44f, profitTextPaint)
                    }
                }

                val footerPaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 9f
                    isAntiAlias = true
                }
                canvas.drawText("Comptabilité Stocks - Document d'Inventaire Corporate.", margin, pageHeight - 20f, footerPaint)

                pdfDoc.finishPage(page)
                if (totalItems == 0) break
                pageNumber++
            }

            val fileOutputStream = FileOutputStream(file)
            pdfDoc.writeTo(fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            pdfDoc.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun generateInvoicePdf(
        context: Context,
        file: File,
        companyName: String,
        clientName: String,
        clientPhone: String,
        timestamp: Long,
        totalAmount: Double,
        subType: String,
        productsJson: String,
        currencySymbol: String
    ): Boolean {
        try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 36f
            
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas
            
            val titlePaint = Paint().apply {
                color = Color.rgb(128, 0, 32)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val companyPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val standardPaint = Paint().apply {
                color = Color.rgb(60, 60, 65)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            
            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val tableHeaderPaint = Paint().apply {
                color = Color.WHITE
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(40, 44, 52)
            }
            
            canvas.drawText("FACTURE CLIENT", margin, 60f, titlePaint)
            canvas.drawText("Entreprise: $companyName", margin, 90f, companyPaint)
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
            canvas.drawText("Date: $dateStr", margin, 110f, standardPaint)
            
            canvas.drawText("Client: $clientName", margin, 140f, boldPaint)
            if (clientPhone.isNotEmpty()) {
                canvas.drawText("Téléphone: $clientPhone", margin, 160f, standardPaint)
            }
            canvas.drawText("Type: ${if (subType == "PERTE") "Perte de Stock" else "Vente"}", margin, 180f, standardPaint)
            
            val topY = 210f
            val tableHeight = 24f
            canvas.drawRect(margin, topY, pageWidth - margin, topY + tableHeight, headerBgPaint)
            
            canvas.drawText("Désignation", margin + 10f, topY + 16f, tableHeaderPaint)
            canvas.drawText("Quantité", margin + 250f, topY + 16f, tableHeaderPaint)
            canvas.drawText("P.U.", margin + 350f, topY + 16f, tableHeaderPaint)
            canvas.drawText("Total", margin + 440f, topY + 16f, tableHeaderPaint)
            
            var currentY = topY + tableHeight
            val array = org.json.JSONArray(productsJson)
            
            val lineStrokePaint = Paint().apply {
                color = Color.rgb(220, 220, 225)
                strokeWidth = 1f
            }
            
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val des = obj.optString("designation", "Inconnu")
                val qty = obj.optDouble("qty", 0.0)
                val price = obj.optDouble("price", 0.0)
                val sum = qty * price
                
                canvas.drawText(des, margin + 10f, currentY + 16f, standardPaint)
                canvas.drawText(String.format(Locale.getDefault(), "%,.1f", qty), margin + 250f, currentY + 16f, standardPaint)
                canvas.drawText(String.format(Locale.getDefault(), "%,.0f $currencySymbol", price), margin + 350f, currentY + 16f, standardPaint)
                canvas.drawText(String.format(Locale.getDefault(), "%,.0f $currencySymbol", sum), margin + 440f, currentY + 16f, boldPaint)
                
                currentY += tableHeight
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, lineStrokePaint)
            }
            
            currentY += 20f
            val totalStr = if (currencySymbol.contains("CFA") || currencySymbol.contains("XOF")) {
                String.format(Locale.getDefault(), "%,.0f XOF", totalAmount)
            } else {
                String.format(Locale.getDefault(), "%,.2f %s", totalAmount, currencySymbol)
            }
            canvas.drawText("MONTANT TOTAL A PAYER :", margin + 230f, currentY + 16f, boldPaint)
            val bordeauxPaint = Paint().apply {
                color = Color.rgb(128, 0, 32)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(totalStr, margin + 410f, currentY + 18f, bordeauxPaint)
            
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 9f
                isAntiAlias = true
            }
            canvas.drawText("Merci pour votre confiance !", margin, pageHeight - 40f, footerPaint)
            canvas.drawText("Généré automatiquement par Stock Management.", margin, pageHeight - 25f, footerPaint)
            
            pdfDoc.finishPage(page)
            
            val fileOutputStream = FileOutputStream(file)
            pdfDoc.writeTo(fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            pdfDoc.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
