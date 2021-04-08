package ru.skillbranch.skillarticles.data.remote.interceptors

import dagger.Lazy
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.remote.RestService
import ru.skillbranch.skillarticles.data.remote.req.RefreshReq

// !! Lazy здесь - класс даггера, а не котлина
class TokenAuthenticator(
    private val prefs: PrefManager,
    private val lazyApy: Lazy<RestService>
    ) : Authenticator
{
    // если authenticate вернёт null, то будут вызваны последующие интерсепторы, иначе выполнится новый запрос
    override fun authenticate(route: Route?, response: Response): Request? {

        return if (response.code != 401) null
        else {
            // request new access token by refresh token (sync)
            val refreshRes = lazyApy.get().refreshAccessToken(
                RefreshReq(prefs.refreshToken)
            ).execute()

            if (!refreshRes.isSuccessful) null
            else {
                // save new refresh & access tokens
                val tokens = refreshRes.body()!!
                prefs.accessToken = "Bearer ${tokens.accessToken}"
                prefs.refreshToken = tokens.refreshToken

                // retry request with new access token
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.accessToken}")
                    .build()
            }

        }
    }
}