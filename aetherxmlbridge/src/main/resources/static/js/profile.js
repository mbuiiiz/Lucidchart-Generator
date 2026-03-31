function togglePasswordField(inputId, eyeId, eyeOffId) {
  const input = document.getElementById(inputId)
  const eyeIcon = document.getElementById(eyeId)
  const eyeOffIcon = document.getElementById(eyeOffId)

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

  const view = container.querySelector('.view-mode')
  const edit = container.querySelector('.edit-mode')

  view.classList.toggle('hidden', isEditing)
  edit.classList.toggle('hidden', !isEditing)
}

function toggleDeleteAccount(isEditing) {
  const deleteAccountView = document.getElementById('deleteAccountView')
  const deleteAccountConfirm = document.getElementById('deleteAccountConfirm')

  deleteAccountView.classList.toggle('hidden', isEditing)
  deleteAccountConfirm.classList.toggle('hidden', !isEditing)
}

function openPasswordModal() {
  const modal = document.getElementById('passwordModal')
  modal.classList.remove('hidden')
  modal.classList.add('flex')
}

function closePasswordModal() {
  const modal = document.getElementById('passwordModal')
  modal.classList.add('hidden')
  modal.classList.remove('flex')
}

function initProfile(hasPasswordError, hasDeleteError) {
  if (hasPasswordError) {
    openPasswordModal()
  }

  if (hasDeleteError) {
    toggleDeleteAccount(true)
  }
}
