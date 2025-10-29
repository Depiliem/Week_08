package com.example.week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.week_08.worker.FirstWorker
import com.example.week_08.worker.SecondWorker
// (ASSIGNMENT) Import worker dan service baru
import com.example.week_08.worker.ThirdWorker
import com.example.week_08.SecondNotificationService

class MainActivity : AppCompatActivity() {
    //Create an instance of a work manager
    //Work manager manages all your requests and workers
    //it also sets up the sequence for all your processes
    private val workManager = WorkManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v,
                                                                             insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,
                systemBars.bottom)
            insets
        }

        // Langkah 7: Tambahkan pengecekan izin notifikasi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        //Create a constraint of which your workers are bound to.
        //Here the workers cannot execute the given process if
        //there's no internet connection
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        //There are two types of work request:
        //OneTimeWorkRequest and PeriodicWorkRequest
        //OneTimeWorkRequest executes the request just once
        //PeriodicWorkRequest executed the request periodically

        //Create a one time work request that includes
        //all the constraints and inputs needed for the worker
        //This request is created for the FirstWorker class
        val firstRequest = OneTimeWorkRequest
            .Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker
                .INPUT_DATA_ID, id)
            ).build()

        //This request is created for the SecondWorker class
        val secondRequest = OneTimeWorkRequest
            .Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker
                .INPUT_DATA_ID, id)
            ).build()

        // (ASSIGNMENT) Buat request untuk worker ketiga
        val thirdRequest = OneTimeWorkRequest
            .Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker
                .INPUT_DATA_ID, id)
            ).build()

        //Sets up the process sequence from the work manager instance
        //Here it starts with FirstWorker, then SecondWorker
        // (Langkah 1 & 2)
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue() // thirdRequest di-enqueue secara terpisah nanti

        //All that's left to do is getting the output
        //...
        //isFinished is used to check if the state is either SUCCEEDED or FAILED
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                // Tambahkan pengecekan info != null
                if (info != null && info.state.isFinished) {
                    showResult("First process is done")
                }
            }
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                // Tambahkan pengecekan info != null
                if (info != null && info.state.isFinished) {
                    showResult("Second process is done")
                    // (Langkah 3) Panggil launchNotificationService
                    // (ASSIGNMENT) Kirim thirdRequest untuk dijalankan nanti
                    launchNotificationService(thirdRequest)
                }
            }

        // (ASSIGNMENT) Tambahkan observer untuk thirdRequest
        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("Third process is done")
                    // (Langkah 5) Panggil SecondNotificationService
                    launchSecondNotificationService()
                }
            }
    }

    //Build the data into the correct format before passing it to the worker as
    //input
    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    //Show the result as toast
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Langkah 8: Tambahkan fungsi launchNotificationService
    // (ASSIGNMENT) Modifikasi untuk menerima thirdRequest
    private fun launchNotificationService(thirdRequest: OneTimeWorkRequest) {
        //Observe if the service process is done or not
        //If it is, show a toast with the channel ID in it
        NotificationService.trackingCompletion.observe(
            this) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
            // (Langkah 4) Jalankan ThirdWorker SETELAH NotificationService selesai
            workManager.enqueue(thirdRequest)
        }

        //Create an Intent to start the NotificationService
        //An ID of "001" is also passed as the notification channel ID
        val serviceIntent = Intent(this,
            NotificationService::class.java).apply {
            // Gunakan EXTRA_ID dari NotificationService
            putExtra(NotificationService.EXTRA_ID, "001")
        }

        //Start the foreground service through the Service Intent
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // (ASSIGNMENT) Tambahkan fungsi baru untuk service kedua
    private fun launchSecondNotificationService() {
        //Observe if the service process is done or not
        SecondNotificationService.trackingCompletion.observe(
            this) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }

        //Create an Intent to start the SecondNotificationService
        val serviceIntent = Intent(this,
            SecondNotificationService::class.java).apply {
            // Gunakan EXTRA_ID dari SecondNotificationService
            putExtra(SecondNotificationService.EXTRA_ID, "002")
        }

        //Start the foreground service through the Service Intent
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // Companion object (tidak lagi dibutuhkan oleh service baru,
    // tapi kita biarkan untuk EXTRA_ID lama jika masih dipakai di tempat lain)
    companion object{
        const val EXTRA_ID = "Id" // Ini dipakai oleh service pertama
    }
}

