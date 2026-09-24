package com.maidana.finanzasapp.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val displayName: String = ""
) {
    /**
     * Nombre para el saludo en el Dashboard ("¡Hola, {greetingName}!").
     * Si [firstName] está presente se utiliza directamente.
     * Si es un usuario legacy sin firstName, se extrae la primera palabra de [displayName].
     * Si no hay nombre disponible, recurre al prefijo del correo o "Usuario".
     */
    val greetingName: String
        get() {
            if (firstName.isNotBlank()) return firstName.trim()
            if (displayName.isNotBlank()) {
                val clean = displayName.trim()
                return clean.substringBefore(" ")
            }
            if (email.isNotBlank()) {
                return email.substringBefore("@")
            }
            return "Usuario"
        }

    val fullName: String
        get() {
            val combined = "$firstName $lastName".trim()
            if (combined.isNotBlank()) return combined
            if (displayName.isNotBlank()) return displayName.trim()
            return email.substringBefore("@").ifBlank { "Usuario" }
        }
}
