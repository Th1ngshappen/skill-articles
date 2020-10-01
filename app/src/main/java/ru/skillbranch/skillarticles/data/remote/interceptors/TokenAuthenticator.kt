package ru.skillbranch.skillarticles.data.remote.interceptors

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.remote.NetworkManager
import ru.skillbranch.skillarticles.data.remote.req.RefreshReq

class TokenAuthenticator : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code == 401) {

            val refreshRes = NetworkManager.api.refreshAccessToken(
                RefreshReq(PrefManager.refreshToken)
            ).execute()

            return if (refreshRes.isSuccessful) {

                val tokens = refreshRes.body()!!
                PrefManager.accessToken = "Bearer ${tokens.accessToken}"
                PrefManager.refreshToken = tokens.refreshToken

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.accessToken}")
                    .build()

            } else null

        } else return null
    }
}