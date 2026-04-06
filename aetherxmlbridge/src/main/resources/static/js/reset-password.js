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
