const pwInput = document.getElementById('passwordInput')
const cpwInput = document.getElementById('confirmPasswordInput')

const eyeIcon1 = document.getElementById('eyeIcon')
const eyeOffIcon1 = document.getElementById('eyeOffIcon')
const eyeIcon2 = document.getElementById('eyeIcon2')
const eyeOffIcon2 = document.getElementById('eyeOffIcon2')

const toggleBtn = document.getElementById('toggle-pw')
const toggleBtn2 = document.getElementById('toggle-pw-confirm')

toggleBtn.addEventListener('click', () => {
  if (pwInput.type === 'password') {
    pwInput.type = 'text'
    eyeIcon1.classList.add('hidden')
    eyeOffIcon1.classList.remove('hidden')
  } else {
    pwInput.type = 'password'
    eyeIcon1.classList.remove('hidden')
    eyeOffIcon1.classList.add('hidden')
  }
})

toggleBtn2.addEventListener('click', () => {
  if (cpwInput.type === 'password') {
    cpwInput.type = 'text'
    eyeIcon2.classList.add('hidden')
    eyeOffIcon2.classList.remove('hidden')
  } else {
    cpwInput.type = 'password'
    eyeIcon2.classList.remove('hidden')
    eyeOffIcon2.classList.add('hidden')
  }
})
