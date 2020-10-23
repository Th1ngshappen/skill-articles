package ru.skillbranch.skillarticles.data.remote.interceptors

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.remote.NetworkManager
import ru.skillbranch.skillarticles.data.remote.req.RefreshReq

class TokenAuthenticator : Authenticator {

    private val pref = PrefManager
    private val api by lazy { NetworkManager.api }

    // если authenticate вернёт null, то будут вызваны последующие интерсепторы, иначе выполнится новый запрос
    override fun authenticate(route: Route?, response: Response): Request? {

        return if (response.code != 401) null
        else {
            // request new access token by refresh token (sync)
            val refreshRes = api.refreshAccessToken(
                RefreshReq(pref.refreshToken)
            ).execute()

            if (!refreshRes.isSuccessful) null
            else {
                // save new refresh & access tokens
                val tokens = refreshRes.body()!!
                pref.accessToken = "Bearer ${tokens.accessToken}"
                pref.refreshToken = tokens.refreshToken

                // retry request with new access token
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.accessToken}")
                    .build()
            }

        }
    }
}