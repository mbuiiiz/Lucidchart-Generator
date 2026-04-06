function togglePasswordField(inputId, eyeId, eyeOffId) {
  const input = document.getElementById(inputId)
  const eyeIcon = document.getElementById(eyeId)
  const eyeOffIcon = document.getElementById(eyeOffId)

  if (!input || !eyeIcon || !eyeOffIcon) return

  if (input.type === 'password') {
    input.type = 'text'
    eyeIcon.classList.add('hidden')
    eyeOffIcon.classList.remove('hidden')
  } else {
    input.type = 'password'
    eyeIcon.classList.remove('hidden')
    eyeOffIcon.classList.add('hidden')
  }
}

function togglePassword(inputId, eyeIconId, eyeOffIconId) {
  const input = document.getElementById(inputId)
  const eyeIcon = document.getElementById(eyeIconId)
  const eyeOffIcon = document.getElementById(eyeOffIconId)

  if (input.type === 'password') {
    input.type = 'text'
    eyeIcon.classList.add('hidden')
    eyeOffIcon.classList.remove('hidden')
  } else {
    input.type = 'password'
    eyeIcon.classList.remove('hidden')
    eyeOffIcon.classList.add('hidden')
  }
}

function toggleEdit(button, isEditing) {
  const container = button.closest('.editable-card')
  if (!container) return

  const view = container.querySelector('.view-mode')
  const edit = container.querySelector('.edit-mode')

  if (!view || !edit) return

  view.classList.toggle('hidden', isEditing)
  edit.classList.toggle('hidden', !isEditing)

  if (isEditing) {
    const phoneSuccess = edit.querySelector('[data-phone-success]')
    const phoneError = edit.querySelector('[data-phone-error]')

    if (phoneSuccess) phoneSuccess.classList.add('hidden')
    if (phoneError) phoneError.classList.add('hidden')
  }
}

function toggleDeleteAccount(isEditing) {
  const deleteAccountView = document.getElementById('deleteAccountView')
  const deleteAccountConfirm = document.getElementById('deleteAccountConfirm')

  if (!deleteAccountView || !deleteAccountConfirm) return

  deleteAccountView.classList.toggle('hidden', isEditing)
  deleteAccountConfirm.classList.toggle('hidden', !isEditing)
}

function openPasswordModal() {
  const modal = document.getElementById('passwordModal')
  if (!modal) return

  modal.classList.remove('hidden')
  modal.classList.add('flex')
}

function closePasswordModal() {
  const modal = document.getElementById('passwordModal')
  if (!modal) return

  modal.classList.add('hidden')
  modal.classList.remove('flex')
}

function formatPhoneDigits(digits) {
  const clean = digits.replace(/\D/g, '').slice(0, 10)

  if (clean.length === 0) return ''
  if (clean.length <= 3) return `(${clean}`
  if (clean.length <= 6) return `(${clean.slice(0, 3)}) ${clean.slice(3)}`
  return `(${clean.slice(0, 3)}) ${clean.slice(3, 6)}-${clean.slice(6)}`
}

function initPhoneInput() {
  const phoneInput = document.getElementById('phoneInput')
  const hiddenInput = document.getElementById('fullPhoneNumber')
  const countryCode = document.getElementById('countryCode')
  const saveBtn = document.getElementById('savePhoneBtn')
  const validationMessage = document.getElementById('phoneValidationMessage')

  if (!phoneInput || !hiddenInput || !countryCode || !saveBtn || !validationMessage) return

  function getRawDigits() {
    return phoneInput.value.replace(/\D/g, '').slice(0, 10)
  }

  function syncHiddenValue() {
    const rawDigits = getRawDigits()
    hiddenInput.value = rawDigits ? `${countryCode.value}${rawDigits}` : ''
    return rawDigits
  }

  function resetValidationState() {
    saveBtn.disabled = true
    validationMessage.classList.add('hidden')
    validationMessage.textContent = ''
    validationMessage.classList.remove('text-red-500', 'text-green-600')
  }

  phoneInput.addEventListener('input', () => {
    const rawDigits = getRawDigits()
    phoneInput.value = formatPhoneDigits(rawDigits)
    syncHiddenValue()
    resetValidationState()
  })

  countryCode.addEventListener('change', () => {
    syncHiddenValue()
    resetValidationState()
  })

  window.validatePhoneInput = function () {
    const rawDigits = syncHiddenValue()

    validationMessage.classList.remove('hidden', 'text-red-500', 'text-green-600')

    if (rawDigits.length !== 10) {
      validationMessage.textContent = 'Phone number must contain exactly 10 digits'
      validationMessage.classList.add('text-red-500')
      saveBtn.disabled = true
      return
    }

    validationMessage.textContent = 'Valid phone number'
    validationMessage.classList.add('text-green-600')
    saveBtn.disabled = false
  }

  resetValidationState()
}

function initProfile(hasPasswordError, hasDeleteError, hasPhoneSuccess, hasNameSuccess, hasPasswordSuccess) {
  if (hasPasswordError) {
    openPasswordModal()
  }

  if (hasDeleteError) {
    toggleDeleteAccount(true)
  }

  if (hasPhoneSuccess) {
    const phoneSuccessMessage = document.getElementById('phoneSuccessMessage')
    if (phoneSuccessMessage) {
      setTimeout(() => {
        phoneSuccessMessage.classList.add('hidden')
      }, 3000)
    }
  }

  if (hasNameSuccess) {
    const nameSuccessMessage = document.getElementById('nameSuccessMessage')
    if (nameSuccessMessage) {
      setTimeout(() => {
        nameSuccessMessage.classList.add('hidden')
      }, 3000)
    }
  }

  if (hasPasswordSuccess) {
    const passwordSuccessMessage = document.getElementById('passwordSuccessMessage')
    if (passwordSuccessMessage) {
      setTimeout(() => {
        passwordSuccessMessage.classList.add('hidden')
      }, 3000)
    }
  }

  initPhoneInput()
}
