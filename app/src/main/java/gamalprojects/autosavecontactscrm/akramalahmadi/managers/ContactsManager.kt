package gamalprojects.autosavecontactscrm.akramalahmadi.managers

import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

object ContactsManager {

    private const val TAG = "ContactsManager"

    /**
     * Checks if a phone number exists in the device's native contacts list.
     */
    fun isNumberInNativeContacts(context: Context, phoneNumber: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "READ_CONTACTS permission not granted, cannot check native contacts")
            return false
        }

        val cleaned = PhoneNumberUtils.cleanNumber(phoneNumber)
        if (cleaned.isEmpty()) return false

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val normalizedIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

                while (cursor.moveToNext()) {
                    val rawNum = if (numberIndex != -1) cursor.getString(numberIndex) else null
                    val normNum = if (normalizedIndex != -1) cursor.getString(normalizedIndex) else null

                    if (rawNum != null && PhoneNumberUtils.areNumbersEqual(rawNum, phoneNumber)) {
                        return true
                    }
                    if (normNum != null && PhoneNumberUtils.areNumbersEqual(normNum, phoneNumber)) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking native contacts", e)
        }

        return false
    }

    /**
     * Inserts a new contact into the device's native contacts database.
     * Returns true if successful.
     */
    fun saveToNativeContacts(context: Context, name: String, phoneNumber: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "WRITE_CONTACTS permission not granted, cannot save to native contacts")
            return false
        }

        try {
            val ops = ArrayList<ContentProviderOperation>()

            // Step 1: Add empty RawContact
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // Step 2: Add Contact Name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            // Step 3: Add Contact Phone
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Log.i(TAG, "Successfully saved $name ($phoneNumber) to native contacts")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save contact to native contacts", e)
            return false
        }
    }

    /**
     * Inserts a new contact specifically linked to a Google/Gmail account.
     * This triggers automatic synchronization to Google Contacts on Android.
     */
    fun saveToGoogleContacts(context: Context, name: String, phoneNumber: String, gmailAccount: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "WRITE_CONTACTS permission not granted, cannot save to Google Contacts")
            return false
        }

        try {
            val ops = ArrayList<ContentProviderOperation>()

            // Step 1: Add RawContact with Google account details
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "com.google")
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, gmailAccount.trim())
                    .build()
            )

            // Step 2: Add Contact Name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            // Step 3: Add Contact Phone
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Log.i(TAG, "Successfully saved $name ($phoneNumber) to Google account: $gmailAccount")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save contact to Google account", e)
            return false
        }
    }

    /**
     * Imports contacts associated with any Google/Gmail account or a specific one.
     * Returns a list of Pair(Name, Phone).
     */
    fun importFromGoogleContacts(context: Context, gmailAccount: String?): List<Pair<String, String>> {
        val importedList = ArrayList<Pair<String, String>>()
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "READ_CONTACTS permission not granted, cannot import from Google")
            return importedList
        }

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            "account_type",
            "account_name"
        )

        var selection = "account_type = ?"
        val selectionArgs = ArrayList<String>()
        selectionArgs.add("com.google")

        if (!gmailAccount.isNullOrBlank()) {
            selection += " AND account_name = ?"
            selectionArgs.add(gmailAccount.trim())
        }

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs.toTypedArray(),
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else ""
                    val rawNum = if (numberIndex != -1) cursor.getString(numberIndex) else ""
                    
                    if (name.isNotBlank() && rawNum.isNotBlank()) {
                        importedList.add(Pair(name, rawNum))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import from Google Contacts", e)
        }

        return importedList.distinctBy { it.second }
    }
}
