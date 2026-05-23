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
}
