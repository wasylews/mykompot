package com.wasylews.mykompot.data.repository

import com.wasylews.mykompot.domain.model.LoginResult
import com.wasylews.mykompot.domain.repository.PortalRepository
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Singleton
class StubPortalRepository : PortalRepository {
    override fun login(username: String, password: String): LoginResult = LoginResult.Success
}
