import { loadSessionProfile, logout, watchAuth } from "/assets/firebase-client.js";

const nameField = document.getElementById("portal-name");
const emailField = document.getElementById("portal-email");
const uidField = document.getElementById("portal-uid");
const tierField = document.getElementById("portal-tier");
const languageField = document.getElementById("portal-language");
const errorField = document.getElementById("portal-error");
const signOutButton = document.getElementById("sign-out-button");

function setText(element, value) {
  element.textContent = value || "Not set";
}

function showError(text) {
  errorField.hidden = false;
  errorField.textContent = text;
}

watchAuth(async (user) => {
  if (!user) {
    window.location.replace("/login/");
    return;
  }

  try {
    const profile = await loadSessionProfile(user);

    setText(nameField, profile.displayName || user.displayName || "Unnamed user");
    setText(emailField, user.email || "No email");
    setText(uidField, user.uid);
    setText(tierField, profile.subscriptionTier || "FREE");
    setText(languageField, profile.preferredLanguage || navigator.language || "Unknown");
  } catch (error) {
    showError(error.message || "Unable to load your profile.");
  }
});

signOutButton.addEventListener("click", async () => {
  try {
    await logout();
    window.location.replace("/");
  } catch (error) {
    showError(error.message || "Unable to sign out.");
  }
});
