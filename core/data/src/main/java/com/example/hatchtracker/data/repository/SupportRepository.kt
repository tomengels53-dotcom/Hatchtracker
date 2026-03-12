package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.ChatMessage
import com.example.hatchtracker.data.models.SupportFeature
import com.example.hatchtracker.data.models.SupportModule
import com.example.hatchtracker.data.models.SupportTicket
import com.example.hatchtracker.data.models.TicketStatus
import com.example.hatchtracker.data.remote.mappers.toDocument
import com.example.hatchtracker.data.remote.mappers.toModel
import com.example.hatchtracker.data.remote.models.ChatMessageDocument
import com.example.hatchtracker.data.firestore.FirestoreCollections
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {
    private val TICKETS_COLLECTION = "tickets"

    fun getSupportModules(): List<SupportModule> {
        return listOf(
            SupportModule("flock", "Flock Management"),
            SupportModule("incubation", "Incubation & Hatchery"),
            SupportModule("breeding", "Breeding & Matchmaking"),
            SupportModule("nursery", "Nursery & Brooder"),
            SupportModule("financial", "Account & Financials"),
            SupportModule("user_profile", "Profile"),
            SupportModule("localization", "Language & Localization"),
            SupportModule("other", "General / Other")
        )
    }

    fun getSupportFeatures(moduleId: String): List<SupportFeature> {
        return when (moduleId) {
            "flock" -> listOf(
                SupportFeature("add_bird", "flock", "Adding Birds"),
                SupportFeature("flock_edit", "flock", "Editing Flocks"),
                SupportFeature("inventory", "flock", "Bird Inventory")
            )
            "incubation" -> listOf(
                SupportFeature("start_hatch", "incubation", "Starting a Hatch"),
                SupportFeature("candling", "incubation", "Candling Results"),
                SupportFeature("hatch_outcome", "incubation", "Hatch Statistics")
            )
            "breeding" -> listOf(
                SupportFeature("pairing", "breeding", "Creating Pairs"),
                SupportFeature("recommendation", "breeding", "AI Recommendations"),
                SupportFeature("compatibility", "breeding", "Genetic Compatibility")
            )
            "nursery" -> listOf(
                SupportFeature("flocklet_stats", "nursery", "Updating Chick Stats"),
                SupportFeature("brooder_temp", "nursery", "Temperature Tracking"),
                SupportFeature("move_to_flock", "nursery", "Moving to Adult Flock")
            )
            "financial" -> listOf(
                SupportFeature("sales", "financial", "Sales Tracking"),
                SupportFeature("expenses", "financial", "Expense Logging"),
                SupportFeature("summary", "financial", "Financial Overview")
            )
            "user_profile" -> listOf(
                SupportFeature("change_country", "user_profile", "Country Change"),
                SupportFeature("data_correction", "user_profile", "Account Data Correction"),
                SupportFeature("identity_mismatch", "user_profile", "Email / Identity Mismatch")
            )
            "localization" -> listOf(
                SupportFeature("translation_error", "localization", "Translation Error"),
                SupportFeature("missing_translation", "localization", "Missing Translation"),
                SupportFeature("language_switch_issue", "localization", "Language Switch Issue")
            )
            else -> listOf(
                SupportFeature("general", moduleId, "General Issue"),
                SupportFeature("bug", moduleId, "Report a Bug"),
                SupportFeature("suggestion", moduleId, "Feature Suggestion")
            )
        }
    }

    suspend fun submitTicket(ticket: SupportTicket): Result<Unit> {
        return try {
            val docRef = firestore.collection(TICKETS_COLLECTION).document()
            val finalTicket = ticket.copy(ticketId = docRef.id)
            val payload = hashMapOf(
                "ticketId" to finalTicket.ticketId,
                "userId" to finalTicket.userId,
                "userEmail" to finalTicket.userEmail,
                "category" to finalTicket.category,
                "type" to finalTicket.type,
                "categoryDetail" to finalTicket.categoryDetail,
                "status" to finalTicket.status,
                "priority" to finalTicket.priority,
                "subscriptionTierAtCreation" to finalTicket.subscriptionTierAtCreation,
                "subject" to finalTicket.subject,
                "description" to finalTicket.description,
                "deviceInfo" to finalTicket.deviceInfo,
                "appVersion" to finalTicket.appVersion,
                "diagnostics" to finalTicket.diagnostics,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "sentimentScore" to finalTicket.sentimentScore,
                "tags" to finalTicket.tags,
                "hatchyTriage" to finalTicket.hatchyTriage,
                "changeRequest" to finalTicket.changeRequest
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTicketsForUser(userId: String): Flow<List<SupportTicket>> = callbackFlow {
        val listener = firestore.collection(TICKETS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error as Throwable)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    launch(Dispatchers.IO) {
                        val tickets = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(SupportTicket::class.java)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        trySend(tickets)
                    }
                }
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    // --- Admin Methods ---

    fun getAllTickets(): Flow<List<SupportTicket>> = callbackFlow {
        val listener = firestore.collection(TICKETS_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error as Throwable)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    launch(Dispatchers.IO) {
                        val tickets = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(SupportTicket::class.java)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        trySend(tickets)
                    }
                }
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    suspend fun updateTicketStatus(ticketId: String, status: TicketStatus, adminId: String? = null): Result<Unit> {
        return try {
            val updates = buildStatusUpdates(status, adminId)
            firestore.collection(TICKETS_COLLECTION).document(ticketId)
                .update(updates)
                .await()
                
            // Log manually or use batch if we want atomicity. 
            // Here we do it after success for simplicity as it is not a batch method currently,
            // OR we can't easily batch without refactoring 'update'.
            // Fire and forget log:
            com.example.hatchtracker.data.audit.AuditLogger.logAction(
                actionType = com.example.hatchtracker.model.AuditActionType.UPDATE,
                targetCollection = TICKETS_COLLECTION,
                targetDocumentId = ticketId,
                after = updates,
                reason = "Ticket status update"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildStatusUpdates(status: TicketStatus, adminId: String?): Map<String, Any> {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (status in setOf(TicketStatus.APPROVED, TicketStatus.RESOLVED, TicketStatus.REJECTED)) {
            updates["approvedAt"] = FieldValue.serverTimestamp()
            updates["approvedBy"] = adminId ?: ""
        } else {
            updates["approvedAt"] = FieldValue.delete()
            updates["approvedBy"] = FieldValue.delete()
        }
        return updates
    }

    suspend fun addInternalNote(ticketId: String, note: String, authorId: String): Result<Unit> {
        return try {
            val noteData = hashMapOf(
                "authorId" to authorId,
                "content" to note,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection(TICKETS_COLLECTION).document(ticketId)
                .collection("internal_notes")
                .add(noteData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getInternalNotes(ticketId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = firestore.collection(TICKETS_COLLECTION).document(ticketId)
            .collection("internal_notes")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error as Throwable)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notes = snapshot.documents.map { it.data ?: emptyMap() }
                    trySend(notes)
                }
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    // --- Chat Methods ---

    fun getMessages(ticketId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection(TICKETS_COLLECTION).document(ticketId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error as Throwable)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    launch(Dispatchers.IO) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ChatMessageDocument::class.java)?.toModel(idOverride = doc.id)
                        }
                        trySend(messages)
                    }
                }
            }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    suspend fun sendMessage(ticketId: String, message: ChatMessage): Result<Unit> {
        return try {
            val docRef = firestore.collection(TICKETS_COLLECTION).document(ticketId)
                .collection("messages")
                .document()
            
            val finalMessage = message.copy(id = docRef.id)
            docRef.set(finalMessage.toDocument()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // resolveCountryChange removed. 
    // Admin operations must be performed via Admin SDK / Cloud Functions, not client-side repositories.

    /**
     * Calls the Cloud Function to securely consume an approved Close Account/Country Change ticket.
     */
    suspend fun consumeCountryChangeTicket(ticketId: String, newCountry: String, newCurrency: String): Result<Unit> {
        return try {
            val data = hashMapOf(
                "ticketId" to ticketId,
                "newCountry" to newCountry,
                "newCurrency" to newCurrency
            )
            
            functions
                .getHttpsCallable("consumeCountryChangeTicket")
                .call(data)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

