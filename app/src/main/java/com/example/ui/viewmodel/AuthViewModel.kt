package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.User
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getInstance(application))

    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _requiresPasswordChange = MutableStateFlow(false)
    val requiresPasswordChange: StateFlow<Boolean> = _requiresPasswordChange.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun login(userId: String, passwordAttempt: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            val result = repository.authenticate(userId.trim().lowercase(), passwordAttempt)
            _isLoading.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()
                _currentUser.value = user
                if (user?.isInitialPassword == true) {
                    _requiresPasswordChange.value = true
                } else {
                    _requiresPasswordChange.value = false
                    onSuccess()
                }
            } else {
                _loginError.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
        }
    }

    fun completePasswordChange(newPassword: String, onDone: () -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.changePassword(user.userId, newPassword)
            _isLoading.value = false
            if (result.isSuccess) {
                _currentUser.value = user.copy(isInitialPassword = false)
                _requiresPasswordChange.value = false
                onDone()
            } else {
                _loginError.value = result.exceptionOrNull()?.message ?: "Password update failed"
            }
        }
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.logAudit(user.userId, "LOGOUT", "USER", user.userId, "User logged out")
            }
        }
        _currentUser.value = null
        _requiresPasswordChange.value = false
        _loginError.value = null
    }

    fun clearError() {
        _loginError.value = null
    }
}
