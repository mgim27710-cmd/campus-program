// ============================================================
// [3단계] 화면 동작
//   기능 1: 프로그램 목록 조회   GET    /api/programs
//   기능 2: 참가 신청           POST   /api/programs/{id}/registrations
//   기능 3: 신청 취소           DELETE /api/programs/{id}/registrations
//   기능 4: 신청 현황 확인       목록의 좌석 칸·잔여석·상태 + 10초마다 자동 갱신
//   (보조) 내 신청 내역          GET    /api/registrations?studentId=...
// ============================================================

// 화면 전체가 공유하는 상태
const state = {
  programs: [],            // 서버에서 받은 프로그램 목록
  myProgramIds: new Set(), // 내가 신청한 프로그램 번호 모음
  filter: "ALL",           // 현재 선택한 종류
  isBusy: false            // 신청·취소 요청 처리 중이면 true (버튼 연타 방지)
};

// 자주 쓰는 화면 요소
const listElement = document.getElementById("program-list");
const emptyMessage = document.getElementById("empty-message");
const studentIdInput = document.getElementById("student-id");
const studentNameInput = document.getElementById("student-name");
const meStatus = document.getElementById("me-status");
const lastUpdated = document.getElementById("last-updated");
const toast = document.getElementById("toast");

const AUTO_REFRESH_MS = 10000; // 현황 자동 갱신 간격 (10초)
const SEAT_GRID_LIMIT = 50;    // 정원이 이보다 크면 칸 대신 막대로 표시


// ------------------------------------------------------------
// 공통 도구
// ------------------------------------------------------------

// 서버 요청 공통 함수: 실패하면 서버가 보낸 안내 문구로 오류를 던진다
async function callApi(method, url, body) {
  const options = { method: method, headers: {} };
  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(url, options);
  } catch (networkError) {
    throw new Error("서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.");
  }

  // 응답 본문을 JSON으로 읽기 (비어 있을 수도 있으니 실패해도 넘어간다)
  let data = null;
  try {
    data = await response.json();
  } catch (parseError) {
    data = null;
  }

  if (!response.ok) {
    const message = data && data.message ? data.message : "요청을 처리하지 못했습니다. (" + response.status + ")";
    throw new Error(message);
  }
  return data;
}

// 사용자가 입력한 글자를 HTML로 그대로 넣지 않도록 바꿔 준다 (보안)
function escapeHtml(text) {
  return String(text)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

// "2026-11-20T14:00:00" → "11월 20일(금) 14:00"
function formatDateTime(isoText) {
  const date = new Date(isoText);
  const dayNames = ["일", "월", "화", "수", "목", "금", "토"];
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const dayName = dayNames[date.getDay()];
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");
  return month + "월 " + day + "일(" + dayName + ") " + hour + ":" + minute;
}

// 화면 아래에 안내 메시지를 4초 동안 보여준다
let toastTimer = null;
function showToast(message, isError) {
  toast.textContent = message;
  toast.classList.toggle("is-error", Boolean(isError));
  toast.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(function () {
    toast.hidden = true;
  }, 4000);
}

// 입력한 학번·이름 읽기. 비어 있으면 안내하고 null
// (학번 형식 검사는 서버가 담당 → 실패 시험 때 서버 응답을 그대로 확인할 수 있다)
function readStudent() {
  const studentId = studentIdInput.value.trim();
  const studentName = studentNameInput.value.trim();

  if (studentId === "" || studentName === "") {
    showToast("먼저 위에 학번과 이름을 입력해 주세요.", true);
    if (studentId === "") {
      studentIdInput.focus();
    } else {
      studentNameInput.focus();
    }
    return null;
  }
  return { studentId: studentId, studentName: studentName };
}


// ------------------------------------------------------------
// 기능 1·4: 목록과 현황 불러오기
// ------------------------------------------------------------
async function loadPrograms() {
  try {
    state.programs = await callApi("GET", "/api/programs");
    lastUpdated.textContent = "현황 갱신 " + new Date().toLocaleTimeString("ko-KR");
    render();
  } catch (error) {
    lastUpdated.textContent = "목록을 불러오지 못했습니다";
    showToast(error.message, true);
  }
}

// 내 신청 내역 불러오기 (showResult가 true면 결과를 문장으로 안내)
async function loadMyRegistrations(showResult) {
  const studentId = studentIdInput.value.trim();
  if (studentId === "") {
    state.myProgramIds = new Set();
    render();
    return;
  }

  try {
    const myList = await callApi("GET", "/api/registrations?studentId=" + encodeURIComponent(studentId));
    state.myProgramIds = new Set(myList.map(function (item) { return item.programId; }));
    if (showResult) {
      meStatus.textContent = myList.length === 0
        ? "아직 신청한 프로그램이 없습니다."
        : "신청한 프로그램 " + myList.length + "개를 표시했습니다.";
    }
    render();
  } catch (error) {
    meStatus.textContent = error.message;
    showToast(error.message, true);
  }
}


// ------------------------------------------------------------
// 기능 2: 참가 신청
// ------------------------------------------------------------
async function applyProgram(programId) {
  const student = readStudent();
  if (student === null || state.isBusy) {
    return;
  }

  state.isBusy = true;
  render();
  try {
    const result = await callApi("POST", "/api/programs/" + programId + "/registrations", student);
    state.myProgramIds.add(programId);
    showToast("신청했습니다. " + result.programTitle + " (남은 자리 " + result.remainingSeats + "석)");
  } catch (error) {
    // 정원 마감, 중복 신청, 학번 형식 오류 등 서버 안내 그대로 표시
    showToast(error.message, true);
  } finally {
    state.isBusy = false;
    await loadPrograms();
  }
}


// ------------------------------------------------------------
// 기능 3: 신청 취소
// ------------------------------------------------------------
async function cancelProgram(programId) {
  const student = readStudent();
  if (student === null || state.isBusy) {
    return;
  }

  const program = state.programs.find(function (item) { return item.id === programId; });
  const title = program ? program.title : "이 프로그램";
  if (!confirm("‘" + title + "’ 신청을 취소할까요?")) {
    return;
  }

  state.isBusy = true;
  render();
  try {
    await callApi("DELETE", "/api/programs/" + programId + "/registrations", student);
    state.myProgramIds.delete(programId);
    showToast("신청을 취소했습니다.");
  } catch (error) {
    // 이름 불일치, 신청 내역 없음 등
    showToast(error.message, true);
  } finally {
    state.isBusy = false;
    await loadPrograms();
  }
}


// ------------------------------------------------------------
// 화면 그리기
// ------------------------------------------------------------

// 좌석 칸 (정원이 크면 막대)
function renderSeats(program) {
  const countHtml =
    '<div class="seats-count"><strong>' + program.appliedCount + "</strong> / " + program.capacity +
    "명, 남은 자리 " + program.remainingSeats + "석</div>";

  if (program.capacity > SEAT_GRID_LIMIT) {
    const percent = Math.round((program.appliedCount / program.capacity) * 100);
    return countHtml + '<div class="seat-bar" aria-hidden="true"><span style="width:' + percent + '%"></span></div>';
  }

  let seatsHtml = "";
  for (let index = 0; index < program.capacity; index++) {
    const isTaken = index < program.appliedCount;
    seatsHtml += '<span class="seat' + (isTaken ? " is-taken" : "") + '"></span>';
  }
  return countHtml + '<div class="seat-grid" aria-hidden="true">' + seatsHtml + "</div>";
}

// 상태 문구와 버튼
function renderAction(program, isMine) {
  const disabled = state.isBusy ? " disabled" : "";

  // 내가 신청한 프로그램 → 취소 버튼
  if (isMine) {
    return (
      '<span class="status status-mine">신청 완료</span>' +
      '<button type="button" class="btn btn-cancel" data-action="cancel" data-id="' + program.id + '"' + disabled + ">신청 취소</button>"
    );
  }

  // 신청 가능 → 신청 버튼
  if (program.status === "OPEN") {
    return (
      '<span class="status status-OPEN">' + escapeHtml(program.statusLabel) + "</span>" +
      '<button type="button" class="btn btn-primary" data-action="apply" data-id="' + program.id + '"' + disabled + ">신청하기</button>"
    );
  }

  // 신청 예정 → 시작 시각 안내
  if (program.status === "UPCOMING") {
    return (
      '<span class="status status-UPCOMING">' + escapeHtml(program.statusLabel) + "</span>" +
      '<span class="status-note">' + formatDateTime(program.applyStartAt) + " 시작</span>"
    );
  }

  // 정원 마감, 기간 종료
  return (
    '<span class="status status-' + program.status + '">' + escapeHtml(program.statusLabel) + "</span>" +
    '<button type="button" class="btn btn-quiet" disabled>신청 불가</button>'
  );
}

// 프로그램 한 줄
function renderProgram(program) {
  const isMine = state.myProgramIds.has(program.id);
  const isDimmed = !isMine && (program.status === "CLOSED" || program.status === "FULL");

  return (
    '<li class="program' + (isDimmed ? " is-dimmed" : "") + '">' +
      "<div>" +
        '<div class="program-kind">' + escapeHtml(program.categoryLabel) + "</div>" +
        '<h3 class="program-title">' + escapeHtml(program.title) + "</h3>" +
        '<dl class="program-meta">' +
          "<dt>일시</dt><dd>" + formatDateTime(program.eventAt) + "</dd>" +
          "<dt>장소</dt><dd>" + escapeHtml(program.location || "추후 안내") + "</dd>" +
          "<dt>마감</dt><dd>" + formatDateTime(program.applyEndAt) + "</dd>" +
        "</dl>" +
      "</div>" +
      '<div class="seats">' + renderSeats(program) + "</div>" +
      '<div class="program-action">' + renderAction(program, isMine) + "</div>" +
    "</li>"
  );
}

// 목록 전체 다시 그리기
function render() {
  const visiblePrograms = state.programs.filter(function (program) {
    return state.filter === "ALL" || program.category === state.filter;
  });

  listElement.innerHTML = visiblePrograms.map(renderProgram).join("");
  emptyMessage.hidden = visiblePrograms.length > 0;
}


// ------------------------------------------------------------
// 이벤트 연결
// ------------------------------------------------------------

// 목록 안의 버튼 클릭 (버튼마다 따로 연결하지 않고 목록 하나에서 처리)
listElement.addEventListener("click", function (event) {
  const button = event.target.closest("button[data-action]");
  if (button === null) {
    return;
  }
  const programId = Number(button.dataset.id);
  if (button.dataset.action === "apply") {
    applyProgram(programId);
  } else if (button.dataset.action === "cancel") {
    cancelProgram(programId);
  }
});

// 종류 버튼
document.querySelectorAll(".chip").forEach(function (chip) {
  chip.addEventListener("click", function () {
    state.filter = chip.dataset.filter;
    document.querySelectorAll(".chip").forEach(function (other) {
      other.setAttribute("aria-pressed", String(other === chip));
    });
    render();
  });
});

// 내 신청 내역 불러오기
document.getElementById("me-form").addEventListener("submit", function (event) {
  event.preventDefault();
  if (readStudent() !== null) {
    loadMyRegistrations(true);
  }
});

// 학번을 바꾸면 이전 학번의 "신청 완료" 표시를 지운다
studentIdInput.addEventListener("input", function () {
  state.myProgramIds = new Set();
  meStatus.textContent = "학번과 이름을 입력하면 신청한 프로그램이 표시됩니다.";
  render();
});

// 새로고침 버튼
document.getElementById("refresh-button").addEventListener("click", function () {
  loadPrograms();
});

// 10초마다 현황 자동 갱신 (다른 탭을 보고 있을 때는 쉬어서 서버 부하를 줄인다)
setInterval(function () {
  if (!document.hidden && !state.isBusy) {
    loadPrograms();
  }
}, AUTO_REFRESH_MS);

// 첫 화면 불러오기
loadPrograms();
