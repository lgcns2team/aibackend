var stompClient = null;
var currentRoomId = null;

function createRoom() {
    var userId = document.getElementById('userId').value.trim();
    if (!userId) {
        alert("Please enter User ID (Teacher's ID) to create room");
        return;
    }

    fetch('http://localhost:8081/chat/room', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ teacherId: userId }),
    })
        .then(response => response.json())
        .then(data => {
            alert("Room Created! Room ID: " + data.roomId);
            document.getElementById('roomId').value = data.roomId;
            currentRoomId = data.roomId;
        })
        .catch((error) => {
            console.error('Error:', error);
            alert("Failed to create room");
        });
}

function connect() {
    var userId = document.getElementById('userId').value.trim();
    var username = document.getElementById('username').value.trim();
    var roomId = document.getElementById('roomId').value.trim();

    if (!userId || !username || !roomId) {
        alert("Please enter User ID, Username, and Room ID");
        return;
    }

    currentRoomId = roomId;

    var socket = new SockJS('http://localhost:8081/ws-stomp');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

        stompClient.subscribe('/topic/room/' + roomId, function (chatMessage) {
            showMessage(JSON.parse(chatMessage.body));
        });

        stompClient.send("/app/chat.addUser/" + roomId,
            {},
            JSON.stringify({ sender: username, type: 'JOIN', userId: userId, roomId: roomId })
        );
    }, function (error) {
        alert("Could not connect to WebSocket server. Please restart backend.");
    });
}

function sendMessage(event) {
    var messageContent = document.getElementById('message').value.trim();
    var username = document.getElementById('username').value.trim();
    var userId = document.getElementById('userId').value.trim();

    if (messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageContent,
            type: 'CHAT',
            userId: userId,
            roomId: currentRoomId
        };
        stompClient.send("/app/chat.sendMessage/" + currentRoomId, {}, JSON.stringify(chatMessage));
        document.getElementById('message').value = '';
    }
    event.preventDefault();
}

function showMessage(message) {
    var messageArea = document.getElementById('messageArea');
    var messageElement = document.createElement('li');

    if (message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' joined!';
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' left!';
    } else {
        messageElement.classList.add('chat-message');
        var usernameElement = document.createElement('strong');
        usernameElement.classList.add('nickname');
        var usernameText = document.createTextNode(message.sender); // Assuming sender is name
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);
    }

    var textElement = document.createElement('span');
    var messageText = document.createTextNode(message.content);
    textElement.appendChild(messageText);

    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

document.getElementById('messageForm').addEventListener('submit', sendMessage, true);
