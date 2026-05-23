package gamalprojects.autosavecontactscrm.akramalahmadi.managers

object PhoneNumberUtils {

    /**
     * Cleans the phone number of any non-digit characters.
     */
    fun cleanNumber(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "")
    }

    /**
     * Standardizes a phone number.
     * Removes leading +, 00, or local 0 so we can compare the core number.
     * In Yemen, a mobile number like 777777777 can appear as:
     * +967777777777, 967777777777, 0777777777, 777777777.
     */
    fun normalize(phone: String): String {
        val cleaned = cleanNumber(phone)
        if (cleaned.isEmpty()) return ""

        // Handing Yemen/General country codes
        // If it starts with 967, take the last 9 digits.
        if (cleaned.startsWith("967") && cleaned.length >= 12) {
            return cleaned.substring(cleaned.length - 9)
        }
        
        // If it starts with 0 and occupies 10 digits (e.g. 0777777777)
        if (cleaned.startsWith("0") && cleaned.length == 10) {
            return cleaned.substring(1)
        }

        // Default: if length is longer than 9, return latest 9 digits for matching
        if (cleaned.length >= 9) {
            return cleaned.substring(cleaned.length - 9)
        }

        return cleaned
    }

    /**
     * Compares two phone numbers for equality after normalization.
     */
    fun areNumbersEqual(p1: String, p2: String): Boolean {
        val n1 = normalize(p1)
        val n2 = normalize(p2)
        if (n1.isEmpty() || n2.isEmpty()) return false
        return n1 == n2
    }
}
