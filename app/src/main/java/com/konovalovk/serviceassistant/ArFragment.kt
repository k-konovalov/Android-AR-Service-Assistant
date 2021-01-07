package com.konovalovk.serviceassistant

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.Observer
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.ux.ArFragment


class ArFragment: ArFragment() {
    private val viewModel = ArFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initRenderable(requireContext())
        initListeners()
    }

    private fun initListeners() {
        val onUpdateListener = Scene.OnUpdateListener { viewModel.findBarcode(this@ArFragment, planeDiscoveryController) }

        arSceneView?.scene?.addOnUpdateListener(onUpdateListener)

        setOnTapArPlaneListener { hitResult: HitResult, _: Plane?, motionEvent: MotionEvent? ->
            if (motionEvent?.action == MotionEvent.ACTION_UP)
                viewModel.onPlaneTap(hitResult, this)
        }

        //arSceneView?.scene?.removeOnUpdateListener()
        viewModel.rvAdapter.portClicked.observe(viewLifecycleOwner,
                Observer {
                    viewModel.initAndShowDetailARView(it ?: return@Observer, requireContext())
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