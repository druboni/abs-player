package com.druboni.absplayer.ui.navigation

import androidx.lifecycle.ViewModel
import com.druboni.absplayer.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(val prefs: UserPreferences) : ViewModel()
