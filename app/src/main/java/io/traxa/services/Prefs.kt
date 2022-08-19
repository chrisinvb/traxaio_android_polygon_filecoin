package io.traxa.services

import android.content.Context
import android.util.Base64
import io.traxa.ui.containers.ContainerYardOrderDialog

class Prefs(context: Context) {

    private val preferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val edit = preferences.edit()

    companion object {
        private val UPLOAD_START_TIME = "upload_start_time"
        private val LATEST_RECORDING_ID = "latest_recording_id"
        private val IS_FIRST_TIME_OPENED = "is_first_time_opened"
        private val PLAYER_ID = "player_id"
        private val PLAYER_UUID = "player_uuid"
        private val CONTAINER_YARD_ORDER_TYPE = "container_yard_order_type"
        const val HAS_EDITED_IMAGE_ONCE = "has_edited_image_once"
    }



    fun getString(key: String) = preferences.getString(key, null)

    fun getBoolean(key: String) = preferences.getBoolean(key, false)
    fun setBoolean(key: String, value: Boolean) = edit.putBoolean(key, value).commit()

    fun setPlayerUUID(id: String) = edit.putString(PLAYER_UUID, id)
    fun getPlayerUUID() = preferences.getString(PLAYER_UUID, null)

    fun getPlayerId(): String? = preferences.getString(PLAYER_ID, null).let {
        if(it == null) return null
        else String(Base64.decode(it, Base64.URL_SAFE or Base64.NO_WRAP))
    }

    fun setPlayerId(id: String) = edit.putString(PLAYER_ID, id.let {
        Base64.encodeToString(it.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
    }).commit()

    fun getUploadStartTime() = preferences.getLong(UPLOAD_START_TIME, 0L)
    fun setUploadStartTime(value: Long) = edit.putLong(UPLOAD_START_TIME, value).commit()
    fun clearUploadStartTime() = edit.remove(UPLOAD_START_TIME).commit()

    fun isFirstTimeOpened() = preferences.getBoolean(IS_FIRST_TIME_OPENED, true)
    fun setFirstTimeOpened(value: Boolean) = edit.putBoolean(IS_FIRST_TIME_OPENED, value).commit()

    fun getLatestRecordingId() = preferences.getInt(LATEST_RECORDING_ID, -1)
    fun setLatestRecordingId(recordingId: Int) = edit.putInt(LATEST_RECORDING_ID, recordingId).commit()

    fun setWallet(name: String, address: String) = edit.putString("wallet_$name", address).commit()
    fun getWallet(name: String) = preferences.getString("wallet_$name", null)

    fun setContainerYardOrder(orderType: ContainerYardOrderDialog.OrderType) = edit.putString(CONTAINER_YARD_ORDER_TYPE, orderType.name).commit()
    fun getContainerYardOrder() = preferences.getString(
        CONTAINER_YARD_ORDER_TYPE, ContainerYardOrderDialog.OrderType.BY_TIME.name).let {
        if(it != null) ContainerYardOrderDialog.OrderType.valueOf(it)
        else ContainerYardOrderDialog.OrderType.BY_TIME
    }
}