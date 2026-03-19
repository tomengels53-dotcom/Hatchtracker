import { signInWithEmail, signInWithGoogle, requestPasswordReset, watchAuth } from "/assets/firebase-client.js";

const loginForm = document.getElementById("login-form");
const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");
const emailLoginButton = document.getElementById("email-login-button");
const googleLoginButton = document.getElementById("google-login-button");
const resetPasswordButton = document.getElementById("reset-password-button");
const authMessage = document.getElementById("auth-message");
const authError = document.getElementById("auth-error");

function showMessage(text) {
  authMessage.hidden = false;
  authError.hidden = true;
  authMessage.textContent = text;
}

function showError(text) {
  authError.hidden = false;
  authMessage.hidden = true;
  authError.textContent = text;
}

function clearStatus() {
  authMessage.hidden = true;
  authError.hidden = true;
  authMessage.textContent = "";
  authError.textContent = "";
}

function setBusy(isBusy) {
  emailLoginButton.disabled = isBusy;
  googleLoginButton.disabled = isBusy;
  resetPasswordButton.disabled = isBusy;
}

watchAuth((user) => {
  if (user) {
    window.location.replace("/portal/");
  }
});

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  clearStatus();
  setBusy(true);

  try {
    await signInWithEmail(emailInput.value.trim(), passwordInput.value);
    window.location.replace("/portal/");
  } catch (error) {
    showError(error.message || "Unable to sign in.");
  } finally {
    setBusy(false);
  }
});

googleLoginButton.addEventListener("click", async () => {
  clearStatus();
  setBusy(true);

  try {
    await signInWithGoogle();
    window.location.replace("/portal/");
  } catch (error) {
    showError(error.message || "Unable to continue with Google.");
  } finally {
    setBusy(false);
  }
});

resetPasswordButton.addEventListener("click", async () => {
  clearStatus();

  const email = emailInput.value.trim();
  if (!email) {
    showError("Enter your email address first.");
    return;
  }

  setBusy(true);

  try {
    await requestPasswordReset(email);
    showMessage(`Password reset email sent to ${email}.`);
  } catch (error) {
    showError(error.message || "Unable to send password reset email.");
  } finally {
    setBusy(false);
  }
});
