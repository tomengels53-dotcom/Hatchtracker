package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.data.models.DisclaimerAcknowledgement
import com.example.hatchtracker.data.models.DisclaimerType
import com.example.hatchtracker.data.models.LegalDisclaimer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

object DisclaimerManager {
    private val db by lazy { FirebaseFirestore.getInstance() }
    
    // Cache current disclaimers to avoid repetitive fetches
    private val _activeGlobalDisclaimer = MutableStateFlow<LegalDisclaimer?>(null)

    /**
     * Checks if the user needs to acknowledge the global disclaimer.
     * Returns the disclaimer if acknowledgment is needed, or null if all good.
     */
    fun getPendingGlobalDisclaimer(): Flow<LegalDisclaimer?> = flow {
        val user = UserAuthManager.currentUser.value
        if (user == null) {
            emit(null)
            return@flow
        }

        // 1. Fetch latest Global Disclaimer
        val disclaimerSnapshot = db.collection("legalDisclaimers")
            .whereEqualTo("type", DisclaimerType.GLOBAL_GENETICS.name)
            .orderBy("version", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            
        val latestDisclaimer = disclaimerSnapshot.toObjects(LegalDisclaimer::class.java).firstOrNull()
        
        if (latestDisclaimer == null) {
            emit(null)
            return@flow
        }

        // 2. Check if user acknowledged this specific version
        val ackSnapshot = db.collection("users")
            .document(user.uid)
            .collection("disclaimerAcks")
            .document(latestDisclaimer.id)
            .get()
            .await()
            
        val ack = ackSnapshot.toObject(DisclaimerAcknowledgement::class.java)
        
        // Logic: Show if no ack exists OR (it requires re-accept AND version is newer than ack)
        // Actually, if ack exists for this specific ID and matches version, we are good.
        // We track by disclaimer ID. If a new version comes out, typically we creating a NEW doc ID or updating version?
        // Design said: "id (string, e.g., 'global_v1')" and "version (int)". 
        // If we update the SAME doc ID but bump version, we need to check the ack's stored version.
        
        val needsAck = if (ack == null) {
            true
        } else {
             // If I acknowledged version 1, and now it is version 2:
             if (latestDisclaimer.version > ack.disclaimerVersion) {
                 // Only force if requiresReaccept is true
                 latestDisclaimer.requiresReaccept
             } else {
                 false
             }
        }
        
        if (needsAck) {
             emit(latestDisclaimer)
        } else {
            emit(null)
        }
    }

    /**
     * Fetches a disclaimer text for inline display (non-blocking).
     */
    suspend fun getInlineDisclaimer(type: DisclaimerType): LegalDisclaimer? {
        // Simple cache or fetch
        // For simplicity, fetch fresh (or use a real cache in production)
         val disclaimerSnapshot = db.collection("legalDisclaimers")
            .whereEqualTo("type", type.name)
            .orderBy("version", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            
        return disclaimerSnapshot.toObjects(LegalDisclaimer::class.java).firstOrNull()
    }

    suspend fun acknowledgeDisclaimer(disclaimer: LegalDisclaimer) {
        val user = UserAuthManager.currentUser.value ?: return
        
        val ack = DisclaimerAcknowledgement(
            disclaimerId = disclaimer.id,
            disclaimerVersion = disclaimer.version,
            acknowledgedAt = System.currentTimeMillis()
        )
        
        db.collection("users")
            .document(user.uid)
            .collection("disclaimerAcks")
            .document(disclaimer.id)
            .set(ack)
            .await()
    }
}
