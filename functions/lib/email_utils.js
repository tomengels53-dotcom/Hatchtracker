"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.generateEmailHtml = exports.triggerTransactionalEmail = void 0;
const admin = require("firebase-admin");
const firestore = admin.firestore();
/**
 * Triggers a transactional email by creating an audit event and a mail queue document.
 *
 * @param uid User ID for the recipient
 * @param email Recipient email address
 * @param type Type of email being sent
 * @param subject Email subject line
 * @param html HTML content of the email
 * @param metadata Optional contextual data for the audit trail
 */
async function triggerTransactionalEmail(uid, email, type, subject, html, metadata = {}) {
    // 1. Create Immutable Email Event (Audit Log)
    const emailEventRef = firestore.collection("emailEvents").doc();
    const emailEventId = emailEventRef.id;
    await emailEventRef.set({
        uid,
        email,
        type,
        status: "pending",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        metadata: metadata
    });
    // 2. Create Mail document for "Trigger Email" Extension
    const mailRef = firestore.collection("mail").doc();
    await mailRef.set({
        to: email,
        message: {
            subject: subject,
            html: html
        },
        metadata: {
            emailEventId: emailEventId
        }
    });
    console.log(`Email '${type}' triggered for user ${uid} (Event: ${emailEventId})`);
}
exports.triggerTransactionalEmail = triggerTransactionalEmail;
/**
 * Generates a basic HTML template for emails.
 * In a production environment, this would use a more robust templating engine (Handlebars, etc.)
 */
function generateEmailHtml(title, body) {
    return `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
            <h2 style="color: #2e7d32;">HatchBase</h2>
            <hr style="border: 0; border-top: 1px solid #eeeeee; margin: 20px 0;">
            <h3>${title}</h3>
            <p>${body}</p>
            <footer style="margin-top: 40px; font-size: 12px; color: #757575;">
                <p>&copy; 2025 HatchBase. All rights reserved.</p>
                <p>You are receiving this email because it is related to your subscription activity.</p>
            </footer>
        </div>
    `;
}
exports.generateEmailHtml = generateEmailHtml;
//# sourceMappingURL=email_utils.js.map