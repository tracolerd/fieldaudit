package com.tracolerd.fieldaudit.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tracolerd.fieldaudit.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SyncPayload(
    val id: String,
    val title: String,
    val status: String,
    val local_version: Int,
)

@Serializable
data class SyncResponse(
    val id: String,
    val title: String,
    val status: String,
    val server_version: Int,
    val message: String,
)

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.checklistDao()
        val apiBaseUrl = (inputData.getString(KEY_API_BASE_URL) ?: BuildConfig.API_BASE_URL)
            .trim()
            .trimEnd('/')

        if (apiBaseUrl.isBlank() || apiBaseUrl.contains("example.invalid")) {
            // A release build must be configured with a real HTTPS API origin.
            return Result.failure()
        }

        val client = HttpClient(CIO) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        return try {
            var retryNeeded = false
            val pending = dao.getPendingSyncInspections()

            for (inspection in pending) {
                try {
                    val response = client.post("$apiBaseUrl/api/v1/inspections/sync") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            SyncPayload(
                                id = inspection.id,
                                title = inspection.title,
                                // The current prototype supports draft creation only;
                                // sync state itself is not a business workflow state.
                                status = "DRAFT",
                                local_version = inspection.localVersion,
                            ),
                        )
                    }

                    when (classifySyncStatus(response.status.value)) {
                        SyncResponseDisposition.ACCEPTED -> {
                            val body: SyncResponse = response.body()
                            if (body.id != inspection.id || body.status != "SYNCED") {
                                retryNeeded = true
                            } else {
                                dao.markSynced(inspection.id, body.server_version)
                            }
                        }
                        SyncResponseDisposition.CONFLICT -> {
                            dao.updateSyncStatus(inspection.id, "CONFLICT")
                        }
                        SyncResponseDisposition.PERMANENT_FAILURE -> {
                            dao.updateSyncStatus(inspection.id, "SYNC_FAILED")
                        }
                        SyncResponseDisposition.RETRY -> {
                            retryNeeded = true
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Preserve PENDING_SYNC and let WorkManager apply backoff.
                    retryNeeded = true
                }
            }

            if (retryNeeded) Result.retry() else Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        } finally {
            client.close()
        }
    }

    companion object {
        const val KEY_API_BASE_URL = "api_base_url"
    }
}
