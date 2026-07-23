// MoMoGo API 공통 fetch 래퍼 및 CSRF/JWT 보안 헤더 처리 모듈

let accessTokenMemory: string | null = null;

export const setAccessToken = (token: string | null) => {
  accessTokenMemory = token;
};

export const getAccessToken = () => {
  return accessTokenMemory;
};

// 액세스 토큰이 만료되어 재발급도 실패했을 때(=재로그인 필요) 상위(App)에 알리기 위한 핸들러
let sessionExpiredHandler: (() => void) | null = null;

export const setSessionExpiredHandler = (fn: (() => void) | null) => {
  sessionExpiredHandler = fn;
};

// 백엔드 ErrorResponse(errorKey/code) 또는 로그인 실패 핸들러(error) 구조를 함께 다루기 위한 에러 타입
export class ApiError extends Error {
  status: number;
  code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

// 액세스 토큰 재발급(refresh 쿠키 기반). 백엔드가 refresh token을 1회용으로 회전(rotate)시키기 때문에
// 동시에 여러 번 호출되면(StrictMode 이중 마운트 등) 뒤에 도착한 요청은 이미 무효화된 토큰으로 실패한다.
// 진행 중인 재발급 요청을 공유(single-flight)해서 이 경쟁 상태를 방지한다.
let refreshInFlight: Promise<any> | null = null;

export function refreshAccessToken(): Promise<any> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        // request()를 재사용해야 CSRF(X-XSRF-TOKEN) 헤더가 함께 붙는다. 직접 fetch()를 호출하면
        // CSRF 검증에 걸려 매번 403으로 실패한다(백엔드가 /api/auth/refresh도 CSRF 대상으로 취급).
        const data = await request<any>('/api/auth/refresh', { method: 'POST' });
        if (data && data.accessToken) {
          accessTokenMemory = data.accessToken;
        }
        return data;
      } finally {
        refreshInFlight = null;
      }
    })();
  }
  return refreshInFlight;
}

// 브라우저 쿠키 조회 헬퍼 (JS에서 CSRF 토큰을 쿠키에서 읽어 헤더로 주입하기 위함)
export function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    const popped = parts.pop();
    if (popped) {
      return decodeURIComponent(popped.split(';').shift() || '');
    }
  }
  return null;
}

export interface RequestOptions extends RequestInit {
  params?: Record<string, string>;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { params, headers, ...restOptions } = options;

  let resolvedPath = path;

  // 모의 응답 인터셉터 (Mock Interceptor)
  const lowerPath = resolvedPath.toLowerCase();

  // C. 슈퍼 관리자용 모의 처리 (공간 및 문제 관리)
  if (lowerPath.startsWith('/api/super-admin/spaces')) {
    let superSpaces = JSON.parse(localStorage.getItem('momogo_super_spaces') || '[]');
    if (superSpaces.length === 0) {
      superSpaces = [
        { id: 'space-1', name: '마포고 수학 문제은행', description: '마포고 학생들을 위한 수학 퀴즈방', profileImageUrl: null, createdAt: new Date().toISOString() },
        { id: 'space-2', name: '수능 영어 1등급 정복', description: 'EBS 연계 교재 및 기출문제 모음', profileImageUrl: null, createdAt: new Date().toISOString() }
      ];
      localStorage.setItem('momogo_super_spaces', JSON.stringify(superSpaces));
    }

    const method = options.method?.toUpperCase() || 'GET';
    if (method === 'GET') {
      return superSpaces as unknown as T;
    }

    if (method === 'PUT' || method === 'PATCH') {
      const spaceId = resolvedPath.split('/').pop();
      const body = JSON.parse(options.body as string);
      superSpaces = superSpaces.map((s: any) => s.id === spaceId ? { ...s, ...body } : s);
      localStorage.setItem('momogo_super_spaces', JSON.stringify(superSpaces));
      return { id: spaceId, ...body } as unknown as T;
    }

    if (method === 'DELETE') {
      const spaceId = resolvedPath.split('/').pop();
      superSpaces = superSpaces.filter((s: any) => s.id !== spaceId);
      localStorage.setItem('momogo_super_spaces', JSON.stringify(superSpaces));
      return null as unknown as T;
    }
  }

  if (lowerPath.startsWith('/api/super-admin/problems')) {
    let superProblems = JSON.parse(localStorage.getItem('momogo_super_problems') || '[]');
    if (superProblems.length === 0) {
      superProblems = [
        { id: 'prob-1', name: '미적분 기초 계산', content: 'f(x) = x^2 일 때 f\'(3)의 값은?', correctAnswer: '6', explanation: '도함수는 2x이므로 3을 대입하면 6입니다.', category: { id: 'cat-1', name: '수학' } },
        { id: 'prob-2', name: '영어 빈칸 추론', content: '다음 빈칸에 가장 알맞은 단어는? "Actions speak louder than _______."', correctAnswer: 'words', explanation: '말보다 행동이 중요하다는 뜻의 속담입니다.', category: { id: 'cat-2', name: '영어' } }
      ];
      localStorage.setItem('momogo_super_problems', JSON.stringify(superProblems));
    }

    const method = options.method?.toUpperCase() || 'GET';
    if (method === 'GET') {
      return superProblems as unknown as T;
    }

    if (method === 'PUT' || method === 'PATCH') {
      const problemId = resolvedPath.split('/').pop();
      const body = JSON.parse(options.body as string);
      superProblems = superProblems.map((p: any) => p.id === problemId ? { ...p, ...body } : p);
      localStorage.setItem('momogo_super_problems', JSON.stringify(superProblems));
      return { id: problemId, ...body } as unknown as T;
    }

    if (method === 'DELETE') {
      const problemId = resolvedPath.split('/').pop();
      superProblems = superProblems.filter((p: any) => p.id !== problemId);
      localStorage.setItem('momogo_super_problems', JSON.stringify(superProblems));
      return null as unknown as T;
    }
  }

  // URL 파라미터 조립
  let url = resolvedPath;
  if (params) {
    const searchParams = new URLSearchParams(params);
    url += `?${searchParams.toString()}`;
  }

  const defaultHeaders: Record<string, string> = {
    'Accept': 'application/json',
  };

  // 요청 바디가 FormData인 경우 브라우저가 알아서 Boundary를 설정하도록 Content-Type 헤더 생략
  if (!(options.body instanceof FormData)) {
    defaultHeaders['Content-Type'] = 'application/json';
  }

  // JWT 토큰 주입
  if (accessTokenMemory) {
    defaultHeaders['Authorization'] = `Bearer ${accessTokenMemory}`;
  }

  // Spring Security CSRF 쿠키에서 XSRF-TOKEN을 읽어 변경 요청 시 헤더 주입
  const csrfMethods = ['POST', 'PUT', 'PATCH', 'DELETE'];
  const method = options.method?.toUpperCase() || 'GET';
  if (csrfMethods.includes(method)) {
    let xsrfToken = getCookie('XSRF-TOKEN');
    if (!xsrfToken && path !== '/api/auth/csrf-token') {
      try {
        // CSRF 토큰이 아직 없는 상태인 경우, 먼저 동기식으로 토큰 쿠키를 활성화합니다.
        await fetch('/api/auth/csrf-token', { credentials: 'include' });
        xsrfToken = getCookie('XSRF-TOKEN');
      } catch (e) {
        console.error('Failed to pre-fetch CSRF token:', e);
      }
    }
    if (xsrfToken) {
      defaultHeaders['X-XSRF-TOKEN'] = xsrfToken;
    }
  }

  const mergedHeaders = { ...defaultHeaders, ...headers };

  let response = await fetch(url, {
    ...restOptions,
    headers: mergedHeaders,
    credentials: 'include', // 세션 연장을 위한 Refresh Token 쿠키를 안전하게 포함시킵니다.
  });

  // 액세스 토큰 만료(401) 시 재발급 후 원 요청 1회 재시도 (인증 관련 요청 자체는 대상에서 제외)
  const isAuthEndpoint = resolvedPath.startsWith('/api/auth/');
  if (response.status === 401 && !isAuthEndpoint) {
    try {
      await refreshAccessToken();
      const retryHeaders = { ...mergedHeaders, Authorization: `Bearer ${accessTokenMemory}` };
      response = await fetch(url, {
        ...restOptions,
        headers: retryHeaders,
        credentials: 'include',
      });
    } catch {
      accessTokenMemory = null;
      if (sessionExpiredHandler) sessionExpiredHandler();
    }
  }

  // 204 No Content인 경우 JSON 파싱을 배제하고 즉시 종료
  if (response.status === 204) {
    return null as unknown as T;
  }

  if (!response.ok) {
    // 백엔드의 BusinessException 구조 파싱 시도
    let errorBody: any;
    try {
      errorBody = await response.json();
    } catch {
      errorBody = { message: '알 수 없는 네트워크 오류가 발생했습니다.' };
    }
    const code = errorBody.errorKey || errorBody.error || errorBody.code;
    throw new ApiError(response.status, errorBody.message || '요청 처리에 실패했습니다.', code);
  }

  // 응답 바디가 없는 200 OK(예: 공간 가입 API)도 있어 상태코드만으로 판단하지 않고 실제 바디 유무를 확인한다.
  const rawBody = await response.text();
  if (!rawBody) {
    return null as unknown as T;
  }
  return JSON.parse(rawBody);
}

export interface NotificationSseHandlers {
  onNotification: (data: any) => void;
  onError?: (err: unknown) => void;
}

// 알림 실시간 수신(SSE, GET /api/sse) 연결.
// 브라우저 기본 EventSource는 커스텀 헤더를 지원하지 않아 Authorization(JWT) 인증 방식과 맞지 않으므로,
// fetch + ReadableStream으로 SSE 프로토콜(event:/data: 라인, 빈 줄 구분)을 직접 파싱한다.
// 반환된 함수를 호출하면 연결을 종료한다.
export function connectNotificationSse(handlers: NotificationSseHandlers): () => void {
  const controller = new AbortController();
  let stopped = false;

  const buildHeaders = (): Record<string, string> => {
    const headers: Record<string, string> = { Accept: 'text/event-stream' };
    if (accessTokenMemory) {
      headers['Authorization'] = `Bearer ${accessTokenMemory}`;
    }
    return headers;
  };

  const openStream = async () => {
    let response = await fetch('/api/sse', {
      headers: buildHeaders(),
      credentials: 'include',
      signal: controller.signal,
    });

    // 액세스 토큰 만료 시 재발급 후 1회 재연결 시도
    if (response.status === 401) {
      await refreshAccessToken();
      response = await fetch('/api/sse', {
        headers: buildHeaders(),
        credentials: 'include',
        signal: controller.signal,
      });
    }

    if (!response.ok || !response.body) {
      throw new Error(`알림 SSE 연결 실패 (status: ${response.status})`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (!stopped) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      let sepIndex: number;
      while ((sepIndex = buffer.indexOf('\n\n')) !== -1) {
        const rawEvent = buffer.slice(0, sepIndex);
        buffer = buffer.slice(sepIndex + 2);

        const eventName = /^event:\s*(.+)$/m.exec(rawEvent)?.[1]?.trim() || 'message';
        const dataStr = rawEvent
            .split('\n')
            .filter(line => line.startsWith('data:'))
            .map(line => line.slice(5).trim())
            .join('\n');

        if (eventName === 'notifications' && dataStr) {
          try {
            handlers.onNotification(JSON.parse(dataStr));
          } catch (e) {
            console.error('알림 SSE 데이터 파싱 실패:', e);
          }
        }
      }
    }
  };

  const run = async () => {
    while (!stopped) {
      try {
        await openStream();
      } catch (err) {
        if (stopped) return;
        handlers.onError?.(err);
      }
      if (stopped) return;
      // 연결 종료/오류 시 5초 후 재연결
      await new Promise(resolve => setTimeout(resolve, 5000));
    }
  };

  run();

  return () => {
    stopped = true;
    controller.abort();
  };
}
