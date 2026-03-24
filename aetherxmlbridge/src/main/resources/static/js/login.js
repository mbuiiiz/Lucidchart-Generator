const input = document.getElementById('passwordInput')
const eyeIcon = document.getElementById('eyeIcon')
const eyeOffIcon = document.getElementById('eyeOffIcon')
const toggleBtn = document.getElementById('toggle-password')

toggleBtn.addEventListener('click', ()=> {
  if (input.type === 'password') {
    input.type = 'text'
    eyeIcon.classList.add('hidden')
    eyeOffIcon.classList.remove('hidden')
  } else {
    input.type = 'password'
    eyeIcon.classList.remove('hidden')
    eyeOffIcon.classList.add('hidden')
  }
})