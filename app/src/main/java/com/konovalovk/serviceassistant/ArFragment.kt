package com.konovalovk.serviceassistant

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.ux.BaseArFragment


class ArFragment: BaseArFragment() {
    private val viewModel = ArFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initRenderable(requireContext())
        initListeners()
    }

    private fun initListeners() {
        this.setOnTapArPlaneListener { hitResult: HitResult, _: Plane?, _: MotionEvent? ->
            viewModel.onTap(hitResult, this)
        }

        arSceneView?.scene?.addOnUpdateListener {
            val image: Image = try {
                arSceneView?.arFrame?.acquireCameraImage() ?: return@addOnUpdateListener
            } catch (e: java.lang.Exception){
                return@addOnUpdateListener
            }
            viewModel.findBarcode(image, this, requireContext())

        }
    }


    override fun isArRequired(): Boolean {
        return true
    }

    override fun getAdditionalPermissions(): Array<String> {
        return arrayOf()
    }

    override fun handleSessionException(sessionException: UnavailableException?) {
        val message = when (sessionException) {
            is UnavailableArcoreNotInstalledException -> "Please install ARCore"
            is UnavailableApkTooOldException -> "Please update ARCore"
            is UnavailableSdkTooOldException -> "Please update this app"
            is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
            else -> "Failed to create AR session"
        }
        Log.e("ArFragment", "Error: $message", sessionException)
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }

    override fun getSessionConfiguration(session: Session?): Config {
        // Set the Instant Placement mode.
        val config = Config(session)
        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        config.focusMode = Config.FocusMode.AUTO

        return config
    }

    override fun getSessionFeatures(): MutableSet<Session.Feature> {
        return mutableSetOf()
    }
}