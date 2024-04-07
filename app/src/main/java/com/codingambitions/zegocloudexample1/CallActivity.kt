package com.codingambitions.zegocloudexample1

import android.Manifest
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.permissionx.guolindev.PermissionX
import com.tencent.mmkv.MMKV
import com.zegocloud.uikit.plugin.invitation.ZegoInvitationType
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.event.CallEndListener
import com.zegocloud.uikit.prebuilt.call.event.ErrorEventsListener
import com.zegocloud.uikit.prebuilt.call.event.SignalPluginConnectListener
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoCallInvitationData
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoUIKitPrebuiltCallConfigProvider
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import com.zegocloud.uikit.service.express.IExpressEngineEventHandler
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason
import org.json.JSONObject
import timber.log.Timber
import kotlin.random.Random

class CallActivity : AppCompatActivity() {

    private lateinit var userID: String
    private lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        val yourUserID = findViewById<TextView>(R.id.your_user_id)
        val yourUserName = findViewById<TextView>(R.id.your_user_name)

        userID = Random.nextInt(100000, 999999).toString()
        userName = "User_${userID}"

        yourUserID.text = getString(R.string.your_user_id, userID)
        yourUserName.text = getString(R.string.your_user_name, userName)

        val appID: Long = BuildConfig.appID.toLong()
        val appSign: String = BuildConfig.appSign

        initCallInviteService(appID, appSign, userID, userName)

        initVoiceButton()

        initVideoButton()

        //setLogoutButtonListener()

    }

    private fun initVideoButton() {
        val newVideoCall = findViewById<ZegoSendCallInvitationButton>(R.id.new_video_call)
        newVideoCall.setIsVideoCall(true)

        //for notification sound
        newVideoCall.resourceID = "zego_data"


        newVideoCall.setOnClickListener { view ->
            val inputLayout = findViewById<TextInputLayout>(R.id.target_user_id)
            val targetUserID = inputLayout.editText?.text.toString()
            val split = targetUserID.split(",")
            val users = ArrayList<ZegoUIKitUser>()
            for (userID in split) {
                println("userID=$userID")
                val userName = "User_${userID}"
                users.add(ZegoUIKitUser(userID, userName))
            }
            newVideoCall.setInvitees(users)
        }

    }

    private fun initVoiceButton() {
        val newVoiceCall = findViewById<ZegoSendCallInvitationButton>(R.id.new_voice_call)
        newVoiceCall.setIsVideoCall(false)

        newVoiceCall.resourceID = "zego_data"

        val inputLayout = findViewById<TextInputLayout>(R.id.target_user_id)
        val targetUserID = inputLayout.editText?.text.toString()
        val split = targetUserID.split(",")
        val users = ArrayList<ZegoUIKitUser>()
        for (userID in split) {
            println("userID=$userID")
            val userName = "User_${userID}"
            users.add(ZegoUIKitUser(userID, userName))
        }
        newVoiceCall.setInvitees(users)
    }

    private fun initCallInviteService(
        appID: Long,
        appSign: String,
        userID: String,
        userName: String
    ) {
        val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig().apply {
            provider =
                ZegoUIKitPrebuiltCallConfigProvider { invitationData -> getConfig(invitationData) }
        }

        ZegoUIKitPrebuiltCallService.events.errorEventsListener =
            ErrorEventsListener { errorCode, message -> Timber.d("onError() called with: errorCode = [$errorCode], message = [$message]") }

        ZegoUIKitPrebuiltCallService.events.invitationEvents.pluginConnectListener =
            SignalPluginConnectListener { state, event, extendedData -> Timber.d("onSignalPluginConnectionStateChanged() called with: state = [$state], event = [$event], extendedData = [$extendedData]") }

        ZegoUIKitPrebuiltCallService.init(
            application,
            appID,
            appSign,
            userID,
            userName,
            callInvitationConfig
        )

        ZegoUIKitPrebuiltCallService.events.callEvents.callEndListener =
            CallEndListener { callEndReason, jsonObject -> Timber.d("onCallEnd() called with: callEndReason = [$callEndReason], jsonObject = [$jsonObject]") }

        ZegoUIKitPrebuiltCallService.events.callEvents.setExpressEngineEventHandler(object :
            IExpressEngineEventHandler() {
            override fun onRoomStateChanged(
                roomID: String,
                reason: ZegoRoomStateChangedReason,
                errorCode: Int,
                extendedData: JSONObject
            ) {
                Timber.d("onRoomStateChanged() called with: roomID = [$roomID], reason = [$reason], errorCode = [$errorCode], extendedData = [$extendedData]")
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val builder = AlertDialog.Builder(this@CallActivity)
        builder.setTitle("Sign Out")
        builder.setMessage("Are you sure to Sign Out?After Sign out you can't receive offline calls")
        builder.setNegativeButton(R.string.call_cancel) { dialog, _ -> dialog.dismiss() }
        builder.setPositiveButton(R.string.call_ok) { dialog, _ ->
            dialog.dismiss()
            signOut()
            finish()
        }
        builder.create().show()
    }

    private fun getConfig(invitationData: ZegoCallInvitationData): ZegoUIKitPrebuiltCallConfig {
        val isVideoCall = invitationData.type == ZegoInvitationType.VIDEO_CALL.value
        val isGroupCall = invitationData.invitees.size > 1
        return when {
            isVideoCall && isGroupCall -> ZegoUIKitPrebuiltCallConfig.groupVideoCall()
            !isVideoCall && isGroupCall -> ZegoUIKitPrebuiltCallConfig.groupVoiceCall()
            !isVideoCall -> ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall()
            else -> ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ZegoUIKitPrebuiltCallService.endCall()
    }

    private fun setLogoutButtonListener() {
        findViewById<TextView>(R.id.user_logout).setOnClickListener {
            val builder = AlertDialog.Builder(this@CallActivity)
            builder.setTitle("Sign Out")
            builder.setMessage("Are you sure to Sign Out?After Sign out you can't receive offline calls")
            builder.setNegativeButton(R.string.call_cancel) { dialog, _ -> dialog.dismiss() }
            builder.setPositiveButton(R.string.call_ok) { dialog, _ ->
                dialog.dismiss()
                signOut()
                finish()
            }
            builder.create().show()
        }
    }

    private fun signOut() {
        MMKV.defaultMMKV().remove("user_id")
        MMKV.defaultMMKV().remove("user_name")
        ZegoUIKitPrebuiltCallService.unInit()
    }

    fun offlineUsePermission() {
        PermissionX.init(this).permissions(Manifest.permission.SYSTEM_ALERT_WINDOW)
            .onExplainRequestReason { scope, deniedList ->
                val message =
                    "We need your consent for the following permissions in order to use the offline call function properly"
                scope.showRequestReasonDialog(deniedList, message, "Allow", "Deny")
            }.request { _, _, _ -> }
    }

}