package com.example.ecozaschitnik

import android.content.Context

object UserRoleManager {

    private const val PREF_NAME = "user_role_prefs"
    private const val KEY_ROLE = "user_role"

    // Получить текущую роль (по умолчанию USER)
    fun getRole(context: Context): UserRole {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_ROLE, UserRole.USER.name)
        return try {
            UserRole.valueOf(name ?: UserRole.USER.name)
        } catch (e: IllegalArgumentException) {
            UserRole.USER
        }
    }

    // Установить роль
    fun setRole(context: Context, role: UserRole) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ROLE, role.name)
            .apply()
    }

    // Удобный шорткат: это админ?
    fun isAdmin(context: Context): Boolean {
        return getRole(context) == UserRole.ADMIN
    }
}
