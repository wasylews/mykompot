package com.wasylews.mykompot.domain.repository

import com.wasylews.mykompot.domain.model.LoginResult

interface PortalRepository {
    fun login(username: String, password: String): LoginResult
}
