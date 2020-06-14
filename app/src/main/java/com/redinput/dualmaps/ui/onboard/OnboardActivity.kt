package com.redinput.dualmaps.ui.onboard

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.redinput.dualmaps.Onboard
import com.redinput.dualmaps.R
import com.redinput.dualmaps.databinding.ActivityOnboardBinding
import com.redinput.dualmaps.setAdaptativeText
import com.redinput.dualmaps.ui.main.MainActivity
import jonathanfinerty.once.Once

class OnboardActivity : AppCompatActivity() {

    companion object {
        private const val FIRST_LAUNCH = "first-launch"
        private const val CAROUSEL_IMAGE_DURATION = 1500
        private const val REQUEST_PERMISSION_CODE = 1687
    }

    private lateinit var binding: ActivityOnboardBinding
    private val viewModel: OnboardViewModel by viewModels()

    private lateinit var currentStep: Onboard.Step

    private val firebase = Firebase.analytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Once.beenDone(Once.THIS_APP_INSTALL, FIRST_LAUNCH)) {
            navigateToMainActivity()
            return

        } else {
            firebase.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, null)
        }

        binding = ActivityOnboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureViewModel()
        configureBindingListeners()
    }

    private fun configureBindingListeners() {
        binding.next.setOnClickListener {
            when (currentStep.type) {
                Onboard.Type.SIMPLE,
                Onboard.Type.PREFERENCE -> nextStep()
                Onboard.Type.PERMISSION -> checkPermission()
            }
        }

        binding.skip.setOnClickListener {
            navigateToMainActivity()
        }

        binding.checkboxCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.savePreferenceBoolean(currentStep.data.key!!, isChecked)
        }

        binding.checkboxText.setOnClickListener {
            binding.checkboxCheckbox.performClick()
        }
    }

    private fun configureViewModel() {
        viewModel.getObservableStatus().observe(this, Observer { status ->
            if (status.total != binding.indicator.childCount) {
                binding.indicator.initDots(status.total)
            }

            if (status.position >= status.total) {
                firebase.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, null)

                navigateToMainActivity()
                return@Observer
            }

            binding.indicator.setDotSelection(status.position)

            status.step?.let {
                currentStep = it

                binding.title.setAdaptativeText(currentStep.title, 20, 32)
                binding.next.text = currentStep.button

                binding.skip.visibility = when (currentStep.skippable) {
                    true -> View.VISIBLE
                    false -> View.GONE
                }

                when (currentStep.layout) {
                    Onboard.Layout.IMAGE -> showImageStep()
                    Onboard.Layout.CHECKBOX -> showCheckboxStep()
                    Onboard.Layout.TEXT -> showTextStep()
                }
            } ?: hideViews()
        })

        viewModel.loadOnboardFile()
    }

    private fun hideViews() {
        binding.imageContainer.visibility = View.GONE
        binding.checkboxContainer.visibility = View.GONE
    }

    private fun showTextStep() {
        // Nothing yet
    }

    private fun showImageStep() {
        binding.imageContainer.visibility = View.VISIBLE
        binding.checkboxContainer.visibility = View.GONE

        binding.imageDescription.setAdaptativeText(currentStep.data.description, 16, 22)

        val animation = AnimationDrawable()
        animation.isOneShot = false
        currentStep.data.images.forEach carousel@{ imageName ->
            val imageRes = getDrawableResourceByName(imageName)
            if (imageRes == 0) return@carousel

            animation.addFrame(
                ContextCompat.getDrawable(this, imageRes)!!,
                CAROUSEL_IMAGE_DURATION
            )
        }
        binding.imageImage.setImageDrawable(animation)
        animation.start()
    }

    private fun showCheckboxStep() {
        binding.checkboxContainer.visibility = View.VISIBLE
        binding.imageContainer.visibility = View.GONE

        binding.checkboxDescription.setAdaptativeText(currentStep.data.description, 18, 24)

        binding.checkboxText.setAdaptativeText(currentStep.data.checkbox!!, 14, 20)
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                currentStep.data.permission!!
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(currentStep.data.permission!!),
                REQUEST_PERMISSION_CODE
            )

        } else {
            nextStep()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                nextStep()

            } else {
                if (currentStep.data.mandatory!!) {
                    showDialogPermissionMandatory()
                } else {
                    showDialogPermissionWarning()
                }
            }
        }
    }

    private fun showDialogPermissionMandatory() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.mandatory_dialog_message)
            .setPositiveButton(R.string.mandatory_dialog_positive) { _, _ ->
                checkPermission()
            }
            .setNeutralButton(R.string.mandatory_dialog_negative) { _, _ ->
                finish()
            }
            .show()
    }

    private fun showDialogPermissionWarning() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.warning_dialog_message)
            .setPositiveButton(R.string.warning_dialog_positive) { _, _ ->
                nextStep()
            }
            .setOnCancelListener {
                nextStep()
            }
            .show()
    }

    private fun nextStep() {
        viewModel.nextStep()
    }

    private fun navigateToMainActivity() {
        Once.markDone(FIRST_LAUNCH)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun getDrawableResourceByName(name: String): Int {
        return resources.getIdentifier(name, "drawable", packageName)
    }
}