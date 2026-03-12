package com.example.hatchtracker.auth

import com.google.firebase.auth.FirebaseUser

/**
 * Represents the comprehensive state of a user session.
 * acts as the Single Source of Truth for authentication status.
 */
sealed class SessionState {
    /**
     * Session is initializing (startup check in progress).
     */
    object Initializing : SessionState()

    /**
     * Valid authenticated session.
     * @param user The FirebaseUser associated with the session.
     */
    data class Authenticated(val user: FirebaseUser) : SessionState()

    /**
     * No active session. Default state for fresh install or after data clear.
     */
    object Unauthenticated : SessionState()

    /**
     * User explicitly logged out.
     */
    object LoggedOut : SessionState()

    /**
     * Session has been revoked due to security event (e.g. password change).
     */
    object Revoked : SessionState()

    /**
     * Account has been disabled or deleted.
     */
    object Disabled : SessionState()

    /**
     * Token refresh failed, session expired.
     */
    object Expired : SessionState()
}
