package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class UserGreetingTest {

    @Test
    fun greetingName_withExplicitFirstAndLastName_returnsOnlyFirstName() {
        val user = User(
            id = "1",
            email = "ignacio@test.com",
            firstName = "Ignacio",
            lastName = "Maidana",
            displayName = "Ignacio Maidana"
        )
        assertEquals("Ignacio", user.greetingName)
        assertEquals("Ignacio Maidana", user.fullName)
    }

    @Test
    fun greetingName_legacyUserWithOnlyDisplayName_returnsFirstWordOfDisplayName() {
        val user = User(
            id = "2",
            email = "ignacio@test.com",
            firstName = "",
            lastName = "",
            displayName = "Ignacio Maidana"
        )
        assertEquals("Ignacio", user.greetingName)
        assertEquals("Ignacio Maidana", user.fullName)
    }

    @Test
    fun greetingName_legacyUserWithSingleWordDisplayName_returnsThatName() {
        val user = User(
            id = "3",
            email = "ignacio@test.com",
            firstName = "",
            lastName = "",
            displayName = "Ignacio"
        )
        assertEquals("Ignacio", user.greetingName)
    }

    @Test
    fun greetingName_userWithoutName_fallsBackToEmailPrefix() {
        val user = User(
            id = "4",
            email = "ignacio.maidana@test.com",
            firstName = "",
            lastName = "",
            displayName = ""
        )
        assertEquals("ignacio.maidana", user.greetingName)
    }

    @Test
    fun greetingName_completelyEmptyUser_fallsBackToUsuario() {
        val user = User()
        assertEquals("Usuario", user.greetingName)
    }

    @Test
    fun fullName_withFirstAndLastName_combinesBoth() {
        val user = User(
            firstName = "María",
            lastName = "González"
        )
        assertEquals("María González", user.fullName)
    }
}
