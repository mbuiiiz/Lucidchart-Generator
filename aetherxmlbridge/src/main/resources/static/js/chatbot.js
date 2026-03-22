const chatIcon = document.getElementById('chat-icon');
const chatPopup = document.getElementById('chat-popup');
const closeBtn = document.getElementById('close-btn');
const chatBox = document.getElementById('chat-box');
const chatInput = document.getElementById('chat-input');
const sendBtn = document.getElementById('send-btn');

chatIcon.addEventListener('click', () => {
    chatPopup.classList.toggle('open');
});

closeBtn.addEventListener('click', () => {
    chatPopup.classList.remove('open');
});

sendBtn.addEventListener('click', sendMessage);
chatInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        sendMessage();
    }
});

/**
 * Retreives the value user types in the chatbox
 * then performs a POST request on our ChatController.java endpoint
 * which "might" return a response from an LLM
 * TODO: Currently does not support conversation history and memory of past msg
 */
function sendMessage() {
    const userMessage = chatInput.value.trim();
    if (userMessage) {
        addMessageToChatBox('outgoing', userMessage);
        chatInput.value = '';
        chatBox.scrollTop = chatBox.scrollHeight;

        fetch('/api/ai/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain', // Do not change this otherwise will not work
            },
            body: userMessage,
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.text();
            })
            .then(data => {
                addMessageToChatBox('incoming', data);
                chatBox.scrollTop = chatBox.scrollHeight;
            })
            .catch(error => {
                console.error('Error:', error);
                addMessageToChatBox('incoming', 'Sorry, something went wrong. Please try again later.');
                chatBox.scrollTop = chatBox.scrollHeight;
            });
    }
}

// Creates your message
function addMessageToChatBox(type, message) {
    const chatMessage = document.createElement('div');
    chatMessage.classList.add('chat-message', 'mb-4', 'flex');

    const messageBubble = document.createElement('div');
    if (type === 'outgoing') {
        chatMessage.classList.add('justify-end');
        messageBubble.classList.add('bg-gray-900', 'text-white', 'rounded-lg', 'p-3', 'max-w-xs');
    } else {
        messageBubble.classList.add('bg-gray-100', 'text-gray-800', 'rounded-lg', 'p-3', 'max-w-xs');
    }

    const p = document.createElement('p');
    p.textContent = message;

    messageBubble.appendChild(p);
    chatMessage.appendChild(messageBubble);
    chatBox.appendChild(chatMessage);

    // Smooth scroll to the bottom
    chatBox.scrollTo({
        top: chatBox.scrollHeight,
        behavior: 'smooth'
    });
}
