var stompClient = null;
var currentRoomId = null;
var hasSelectedStatus = false;

// room 만들기는 네 REST 주소가 /api/debate/room인지 /chat/room인지에 따라 맞춰야 함.
// 여기서는 기존 코드 유지(필요하면 변경)
function createRoom() {
  var userId = document.getElementById('userId').value.trim(); // (REST createRoom에 쓰는 값이면 유지)
  if (!userId) {
    alert("Please enter User ID (Teacher's ID) to create room");
    return;
  }

  fetch('http://localhost:8081/chat/room', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ teacherId: userId }),
  })
    .then(res => res.json())
    .then(data => {
      alert("Room Created! Room ID: " + data.roomId);
      document.getElementById('roomId').value = data.roomId;
      currentRoomId = data.roomId;
    })
    .catch(err => {
      console.error(err);
      alert("Failed to create room");
    });
}

function connect() {
  var roomId = document.getElementById('roomId').value.trim();
  var token = document.getElementById('token') ? document.getElementById('token').value.trim() : "";

  if (!roomId) {
    alert("Please enter Room ID");
    return;
  }
  if (!token) {
    alert("JWT token is required (Principal 기반 join/status/chat)");
    return;
  }

  currentRoomId = roomId;
  hasSelectedStatus = false;
  setChatEnabled(false);

  // 서버 설정에 맞게 endpoint 수정 필요:
  // 예: SockJS("http://localhost:8080/ws") 라면 거기에 맞추기
  var socket = new SockJS('http://localhost:8081/ws-stomp');
  stompClient = Stomp.over(socket);

  // (선택) stompClient.debug = null; // 콘솔 로그 줄이기

  // CONNECT 시 JWT 전달 (Principal 세팅 목적)
  stompClient.connect(
    { Authorization: token.startsWith("Bearer ") ? token : ("Bearer " + token) },
    function (frame) {
      console.log('Connected: ' + frame);

      // 방 토픽 구독
      stompClient.subscribe('/topic/room/' + roomId, function (msg) {
        var payload = JSON.parse(msg.body);
        showMessage(payload);

        // 내가 STATUS 선택 완료했을 때 채팅 활성화 UX를 만들려면:
        // 서버가 userId를 내려주지 않는 구조면(지금 out에 userId set 안 함) "내 status 선택"을
        // 프론트가 자체적으로 성공했다고 가정하고 enable 처리하는 편이 단순함.
      });

      // JOIN 전송 (payload 없음)
      stompClient.send("/app/room/" + roomId + "/join", {}, "");
    },
    function (error) {
      console.error(error);
      alert("Could not connect to WebSocket server. Please restart backend.");
    }
  );
}

// 찬성/반대 선택 버튼에서 호출하도록 만들기
function selectStatus(status) {
  if (!stompClient || !currentRoomId) {
    alert("Connect first");
    return;
  }
  if (status !== "PRO" && status !== "CON") {
    alert("status must be PRO or CON");
    return;
  }

  // status 전송: { status: "PRO" | "CON" }
  stompClient.send(
    "/app/room/" + currentRoomId + "/status",
    {},
    JSON.stringify({ status: status })
  );

  // UX: 클릭 즉시 채팅창 활성화(서버에서 막히면 채팅 전송 시 에러가 나고 콘솔/서버로그로 확인 가능)
  hasSelectedStatus = true;
  setChatEnabled(true);
}

function sendMessage(event) {
  event.preventDefault();

  if (!stompClient || !currentRoomId) return;

  if (!hasSelectedStatus) {
    alert("Please select PRO/CON first");
    return;
  }

  var messageContent = document.getElementById('message').value.trim();
  if (!messageContent) return;

  // 서버가 userId/sender/status는 Principal/Redis에서 결정하므로 content만 보내면 됨
  stompClient.send(
    "/app/room/" + currentRoomId + "/chat",
    {},
    JSON.stringify({ content: messageContent })
  );

  document.getElementById('message').value = '';
}

// 서버 응답 표시
function showMessage(message) {
  var messageArea = document.getElementById('messageArea');
  var messageElement = document.createElement('li');

  // type은 enum이라 JSON에 "JOIN"/"STATUS"/"CHAT"/"LEAVE" 형태로 옴
  if (message.type === 'JOIN') {
    messageElement.classList.add('event-message');
    messageElement.appendChild(document.createTextNode((message.sender || "Someone") + " joined!"));
  } else if (message.type === 'LEAVE') {
    messageElement.classList.add('event-message');
    messageElement.appendChild(document.createTextNode((message.sender || "Someone") + " left!"));
  } else if (message.type === 'STATUS') {
    messageElement.classList.add('event-message');
    messageElement.appendChild(document.createTextNode((message.sender || "Someone") + " selected: " + message.status));
  } else { // CHAT
    messageElement.classList.add('chat-message');

    var usernameElement = document.createElement('strong');
    usernameElement.classList.add('nickname');
    usernameElement.appendChild(document.createTextNode(message.sender || "Unknown"));
    messageElement.appendChild(usernameElement);

    var textElement = document.createElement('span');
    textElement.appendChild(document.createTextNode(" " + (message.content || "")));
    messageElement.appendChild(textElement);
  }

  messageArea.appendChild(messageElement);
  messageArea.scrollTop = messageArea.scrollHeight;
}

// 채팅 입력 UI on/off
function setChatEnabled(enabled) {
  var input = document.getElementById('message');
  var btn = document.getElementById('sendBtn'); // 있으면
  if (input) input.disabled = !enabled;
  if (btn) btn.disabled = !enabled;

  // form submit 리스너가 이미 달려있다면 그대로 두고 disabled로만 막으면 됨
}

document.getElementById('messageForm').addEventListener('submit', sendMessage, true);
