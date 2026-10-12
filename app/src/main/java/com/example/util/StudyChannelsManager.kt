package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * v1.3.0 — user-customizable YouTube study-channel allowlist.
 *
 * The built-in education channels come from [StudyGuardKnowledge]; the user can
 * ADD custom channel names and REMOVE any channel (built-in or custom).
 * Persisted in prefs; pushed into StudyGuardKnowledge on init and every edit
 * so the YouTube guard treats allowlisted channels as STUDY.
 */
object StudyChannelsManager {

    private const val PREFS_NAME = "focivo_study_channels_prefs"
    private const val KEY_CUSTOM = "custom_channels"
    private const val KEY_REMOVED_BUILTIN = "removed_builtin_channels"

    data class ChannelEntry(val name: String, val isCustom: Boolean)

    private val _channelsState = MutableStateFlow<List<ChannelEntry>>(emptyList())
    val channelsState: StateFlow<List<ChannelEntry>> = _channelsState.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun init(context: Context) {
        refresh(context)
    }

    /** Effective display list: (built-in − removed) + custom, sorted. */
    fun getChannels(context: Context): List<ChannelEntry> {
        val prefs = getPrefs(context)
        val custom = prefs.getStringSet(KEY_CUSTOM, emptySet()).orEmpty()
        val removed = prefs.getStringSet(KEY_REMOVED_BUILTIN, emptySet()).orEmpty()
        val builtin = StudyGuardKnowledge.getBuiltInChannels()
            .filter { it !in removed }
            .map { ChannelEntry(it, false) }
        val customs = custom.map { ChannelEntry(it, true) }
        return (builtin + customs).sortedBy { it.name.lowercase() }
    }

    fun addCustomChannel(context: Context, name: String): Boolean {
        val clean = name.trim()
        if (clean.length < 2) return false
        val prefs = getPrefs(context)
        val custom = prefs.getStringSet(KEY_CUSTOM, emptySet()).orEmpty().toMutableSet()
        // Avoid duplicates (case-insensitive) against built-in and custom lists
        val lower = clean.lowercase()
        if (custom.any { it.lowercase() == lower }) return false
        if (StudyGuardKnowledge.getBuiltInChannels().any { it.lowercase() == lower }) return false
        custom.add(clean)
        prefs.edit().putStringSet(KEY_CUSTOM, custom).apply()
        pushToKnowledge(context)
        refresh(context)
        return true
    }

    fun removeChannel(context: Context, entry: ChannelEntry) {
        val prefs = getPrefs(context)
        if (entry.isCustom) {
            val custom = prefs.getStringSet(KEY_CUSTOM, emptySet()).orEmpty().toMutableSet()
            custom.removeAll { it == entry.name }
            prefs.edit().putStringSet(KEY_CUSTOM, custom).apply()
        } else {
            val removed = prefs.getStringSet(KEY_REMOVED_BUILTIN, emptySet()).orEmpty().toMutableSet()
            removed.add(entry.name)
            prefs.edit().putStringSet(KEY_REMOVED_BUILTIN, removed).apply()
        }
        pushToKnowledge(context)
        refresh(context)
    }

    fun resetToDefaults(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_CUSTOM)
            .remove(KEY_REMOVED_BUILTIN)
            .apply()
        pushToKnowledge(context)
        refresh(context)
    }

    private fun pushToKnowledge(context: Context) {
        val prefs = getPrefs(context)
        StudyGuardKnowledge.setCustomChannels(prefs.getStringSet(KEY_CUSTOM, emptySet()).orEmpty())
        StudyGuardKnowledge.setDisabledChannels(prefs.getStringSet(KEY_REMOVED_BUILTIN, emptySet()).orEmpty())
    }

    private fun refresh(context: Context) {
        pushToKnowledge(context)
        _channelsState.value = getChannels(context)
    }
}
