package com.konovalovk.serviceassistant

import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment


class ArFragment: ArFragment() {
    private val viewModel = ArFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initRenderable(requireContext())
        initListeners()
    }

    private fun initListeners() {
        val onUpdateListener = object : Scene.OnUpdateListener {
            override fun onUpdate(frameTime: FrameTime?) {
                val image: Image = try {
                    arSceneView?.arFrame?.acquireCameraImage() ?: return
                } catch (e: java.lang.Exception) { return }
                viewModel.findBarcode(image, this@ArFragment, planeDiscoveryController)
            }
        }

        arSceneView?.scene?.addOnUpdateListener(onUpdateListener)

        setOnTapArPlaneListener { hitResult: HitResult, _: Plane?, motionEvent: MotionEvent? ->
            if (motionEvent?.action == MotionEvent.ACTION_UP)
                viewModel.onTap(hitResult, this)
        }

        //arSceneView?.scene?.removeOnUpdateListener()
        viewModel.rvAdapter.portClicked.observe(viewLifecycleOwner,
                Observer {
                    viewModel.initDetailView(it ?: return@Observer, requireContext())
                }
        )
    }

    override fun getSessionConfiguration(session: Session?): Config {
        // Set the Instant Placement mode.
        val config = Config(session)
        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        config.focusMode = Config.FocusMode.AUTO

        return config
    }
}