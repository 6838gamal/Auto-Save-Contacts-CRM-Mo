package gamalprojects.autosavecontactscrm.akramalahmadi.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import gamalprojects.autosavecontactscrm.akramalahmadi.database.Contact
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    /**
     * Generates a CSV file from a list of CRM contacts and initiates standard Android Sharing.
     * Prepends UTF-8 BOM (\uFEFF) to ensure Microsoft Excel parses Arabic characters perfectly.
     */
    fun exportContactsToCsv(context: Context, contacts: List<Contact>): Boolean {
        if (contacts.isEmpty()) return false

        try {
            val sDateFormater = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
            val csvHeader = "الاسم,رقم الهاتف,المصدر,تاريخ الحفظ,آخر نشاط,عدد التواصل\n"
            
            val csvContent = StringBuilder().apply {
                append("\uFEFF") // UTF-8 byte order mark (BOM) for Excel compatibility with Arabic
                append(csvHeader)
                for (c in contacts) {
                    val contactDate = sDateFormater.format(Date(c.timestamp))
                    val activityDate = sDateFormater.format(Date(c.lastActivity))
                    
                    // Escaping values just in case they have commas
                    val name = escapeCsv(c.name)
                    val phone = escapeCsv(c.originalPhone)
                    val source = escapeCsv(c.source)
                    
                    append("$name,$phone,$source,$contactDate,$activityDate,${c.interactionCount}\n")
                }
            }.toString()

            // Save cache file
            val cachePath = File(context.cacheDir, "exports")
            cachePath.mkdirs()
            val file = File(cachePath, "autosave_contacts_crm_${System.currentTimeMillis()}.csv")
            FileOutputStream(file).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            // Share the file
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "تصدير جهات اتصال الـ CRM")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "مشاركة التقرير عبر:"))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun escapeCsv(value: String): String {
        var clean = value.replace("\n", " ").trim()
        if (clean.contains(",") || clean.contains("\"")) {
            clean = clean.replace("\"", "\"\"")
            clean = "\"$clean\""
        }
        return clean
    }
}
