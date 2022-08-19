package io.traxa.services.network

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import io.traxa.BuildConfig
import io.traxa.models.Token
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import kotlin.math.ceil

class StardustService {

    private val BASE_URL = "https://core-api.stardust.gg/v1/"

    suspend fun setPlayerWallet(playerId: String, wallet: String) {

        val data = "{\"playerId\":\"%s\",\"props\":{\"polygon\":\"%s\"}}"
        val (_, response, result) = Fuel.put(BASE_URL + "player/mutate")
            .jsonBody(data.format(playerId, wallet))
            .header("x-api-key", BuildConfig.stardustApiKey)
            .awaitStringResponseResult()
    }

    suspend fun getPlayerInventory(playerId: String): List<Token> {

        val params = listOf("playerId" to playerId)
        val (_, response, result) = Fuel.get(BASE_URL + "player/get-inventory", params)
            .header("x-api-key", BuildConfig.stardustApiKey)
            .header("Content-Type", "application/json;charset=UTF-8")
            .awaitStringResponseResult()

        return if(response.statusCode == 200) {
            val data = result.get()
            val tokenIds = parseStardustInventoryResult(data)
            val tokens = arrayListOf<Token>()

            println("Token ids: $tokenIds")
            val numberOfRequests = ceil(tokenIds.size / 100.0).toInt()

            for(i in 0 until numberOfRequests) tokens.addAll(getTokens(tokenIds))

            return tokens
        }else emptyList()
    }

    suspend fun getTokens(ids: List<Long>): List<Token> {
        val params = listOf("tokenIds" to "$ids")
        val (_, response, result) = Fuel.get(BASE_URL + "token/get", params)
            .header("x-api-key", BuildConfig.stardustApiKey)
            .header("Content-Type", "application/json;charset=UTF-8")
            .awaitStringResponseResult()

        return if(response.statusCode == 200) {
            val data = result.get()
            return parseStardustTokens(data)
        }else emptyList()
    }

    suspend fun createPlayer(uuid: String): String? {
        val userData = "{\"uniqueId\": \"%s\",\"userData\": {}}"

        val (_, response, result) = Fuel.post(BASE_URL + "player/create")
            .header("x-api-key", BuildConfig.stardustApiKey)
            .header("Content-Type", "application/json;charset=UTF-8")
            .body(userData.format(uuid))
            .awaitStringResponseResult()

        return if(response.statusCode == 200) {
            val data = result.get()
            return JSONObject(data).getString("playerId")
        }else null
    }


    private fun parseStardustTokens(string: String): List<Token> {

        val jsonArray = JSONArray(string)
        val tokens = arrayListOf<Token>()

        for(i in (0 until jsonArray.length())) {
            val jsonObject = jsonArray.getJSONObject(i)
            val props = jsonObject.getJSONObject("props")
                .getJSONObject("mutable")

            val storageType = props.getString("Storage-Type")
            val storageLink = props.getString("Storage-Link")
            val captureKey = props.getString("Capture-Key")
            val containerIds = props.getString("Shipping-Container-IDs")
            val containerPositions = props.getString("Shipping-Container-Locations")
            val containerTypes = props.getString("Shipping-Container-Types")
            val timestamp = props.getLong("Timestamp")

            tokens.add(Token(captureKey, storageType, storageLink, containerIds, containerPositions, containerTypes, timestamp))
        }

        return tokens
    }

    private fun parseStardustInventoryResult(string: String): List<Long> {

        val jsonArray = JSONArray(string)
        val tokenIds = arrayListOf<Long>()

        for(i in (0 until jsonArray.length())) {
            val jsonObject = jsonArray.getJSONObject(i)
            tokenIds.add(jsonObject.getLong("tokenId"))
        }

        return tokenIds
    }

}