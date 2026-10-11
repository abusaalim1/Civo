package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ShortsLockManager {

    private const val PREFS_NAME = "focivo_shorts_lock_prefs"
    private const val KEY_SHORTS_LOCK_ENABLED = "key_shorts_lock_enabled"
    private const val KEY_REELS_LOCK_ENABLED = "key_reels_lock_enabled"
    private const val KEY_SHORTS_REELS_BLOCK_ENABLED = "key_shorts_reels_block_enabled"

    private val _isShortsLockState = MutableStateFlow(false)
    val isShortsLockState: StateFlow<Boolean> = _isShortsLockState.asStateFlow()

    private val _isReelsLockState = MutableStateFlow(false)
    val isReelsLockState: StateFlow<Boolean> = _isReelsLockState.asStateFlow()

    // Merged single option: one toggle drives both the YouTube Shorts lock
    // and the Instagram Reels lock.
    private val _isShortsReelsBlockState = MutableStateFlow(false)
    val isShortsReelsBlockState: StateFlow<Boolean> = _isShortsReelsBlockState.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        val prefs = getPrefs(context)
        val hasPerm = AiStudyGuardManager.isAccessibilityPermissionGranted(context)
        _isShortsLockState.value = hasPerm && prefs.getBoolean(KEY_SHORTS_LOCK_ENABLED, true)
        _isReelsLockState.value = hasPerm && prefs.getBoolean(KEY_REELS_LOCK_ENABLED, true)
        _isShortsReelsBlockState.value = hasPerm &&
                (prefs.getBoolean(KEY_SHORTS_LOCK_ENABLED, true) || prefs.getBoolean(KEY_REELS_LOCK_ENABLED, true))
    }

    fun isShortsLockEnabled(context: Context): Boolean {
        val hasPerm = AiStudyGuardManager.isAccessibilityPermissionGranted(context)
        if (!hasPerm) return false
        if (!AiStudyGuardManager.isStudyPeriodActive(context)) return false
        return getPrefs(context).getBoolean(KEY_SHORTS_LOCK_ENABLED, true)
    }

    fun setShortsLockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHORTS_LOCK_ENABLED, enabled).apply()
        val hasPerm = AiStudyGuardManager.isAccessibilityPermissionGranted(context)
        _isShortsLockState.value = hasPerm && enabled
    }

    fun isReelsLockEnabled(context: Context): Boolean {
        val hasPerm = AiStudyGuardManager.isAccessibilityPermissionGranted(context)
        if (!hasPerm) return false
        if (!AiStudyGuardManager.isStudyPeriodActive(context)) return false
        return getPrefs(context).getBoolean(KEY_REELS_LOCK_ENABLED, true)
    }

    fun setReelsLockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_REELS_LOCK_ENABLED, enabled).apply()
        val hasPerm = AiStudyGuardManager.isAccessibilityPermissionGranted(context)
        _isReelsLockState.value = hasPerm && enabled
        _isShortsReelsBlockState.value = hasPerm &&
                (getPrefs(context).getBoolean(KEY_SHORTS_LOCK_ENABLED, true) || enabled)
    }

    /**
     * Merged single option: one toggle controls BOTH the YouTube Shorts lock
     * and the Instagram Reels lock. Writes both underlying prefs so the
     * service-side checks (isShortsLockEnabled / isReelsLockEnabled) keep
     * working unchanged.
     */
    fun isShortsReelsBlockEnabled(context: Context): Boolean {
        val hasPerm = AiStudyGuardManager.isAccessibilityPermissionGranted(context)
        if (!hasPerm) return false
        if (!AiStudyGuardManager.isStudyPeriodActive(context)) return false
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_SHORTS_LOCK_ENABLED, true) ||
                prefs.getBoolean(KEY_REELS_LOCK_ENABLED, true)
    }

    fun setShortsReelsBlockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_SHORTS_LOCK_ENABLED, enabled)
            .putBoolean(KEY_REELS_LOCK_ENABLED, enabled)
            .putBoolean(KEY_SHORTS_REELS_BLOCK_ENABLED, enabled)
            .apply()
        val hasPerm = AiStudyGuardManager.isAccessibilityPermissionGranted(context)
        _isShortsLockState.value = hasPerm && enabled
        _isReelsLockState.value = hasPerm && enabled
        _isShortsReelsBlockState.value = hasPerm && enabled
    }
}
