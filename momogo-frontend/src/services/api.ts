// MoMoGo API 공통 fetch 래퍼 및 CSRF/JWT 보안 헤더 처리 모듈

let accessTokenMemory: string | null = null;

export const setAccessToken = (token: string | null) => {
  accessTokenMemory = token;
};

export const getAccessToken = () => {
  return accessTokenMemory;
};

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
  
  // 1. URL 재매핑 (Super Admin User API)
  let resolvedPath = path;
  if (path.startsWith('/api/super-admin/users')) {
    resolvedPath = path.replace('/api/super-admin/users', '/api/users');
  }

  // 2. 모의 응답 인터셉터 (Mock Interceptor)
  const lowerPath = resolvedPath.toLowerCase();

  // A. 싱글모드 문제 제출 모의 처리
  if (lowerPath.includes('/solve')) {
    return { success: true } as unknown as T;
  }

  // B. 대시보드 API 모의 처리
  if (lowerPath.startsWith('/api/dashboards')) {
    const singleHistory = JSON.parse(localStorage.getItem('momogo_single_history') || '[]');
    const examsHistory = JSON.parse(localStorage.getItem('momogo_exams_history') || '[]');
    const examDetails = JSON.parse(localStorage.getItem('momogo_exam_details') || '{}');

    if (lowerPath === '/api/dashboards/single/summary') {
      const now = new Date();
      const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
      const startOfWeek = now.getTime() - 7 * 24 * 60 * 60 * 1000;

      const daily = singleHistory.filter((h: any) => new Date(h.solvedAt).getTime() >= startOfToday).length;
      const weekly = singleHistory.filter((h: any) => new Date(h.solvedAt).getTime() >= startOfWeek).length;

      let correctRate = 0;
      if (singleHistory.length > 0) {
        const correct = singleHistory.filter((h: any) => h.isSolved).length;
        correctRate = Math.round((correct / singleHistory.length) * 100);
      }
      return {
        dailySolvedCount: daily,
        weeklySolvedCount: weekly,
        averageCorrectRate: correctRate
      } as unknown as T;
    }

    if (lowerPath === '/api/dashboards/single/history') {
      return singleHistory as unknown as T;
    }

    if (lowerPath === '/api/dashboards/exams') {
      return examsHistory as unknown as T;
    }

    if (lowerPath.startsWith('/api/dashboards/exams/')) {
      const roomId = resolvedPath.split('/').pop() || '';
      return (examDetails[roomId] || {
        roomId,
        roomName: '평가 시험',
        description: '상세 내역이 존재하지 않습니다.',
        score: 0,
        problems: []
      }) as unknown as T;
    }
  }

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

  const response = await fetch(url, {
    ...restOptions,
    headers: mergedHeaders,
    credentials: 'include', // 세션 연장을 위한 Refresh Token 쿠키를 안전하게 포함시킵니다.
  });

  // 204 No Content인 경우 JSON 파싱을 배제하고 즉시 종료
  if (response.status === 204) {
    return null as unknown as T;
  }

  if (!response.ok) {
    // 백엔드의 BusinessException 구조 파싱 시도
    let errorBody;
    try {
      errorBody = await response.json();
    } catch {
      errorBody = { message: '알 수 없는 네트워크 오류가 발생했습니다.' };
    }
    throw new Error(errorBody.message || '요청 처리에 실패했습니다.');
  }

  return response.json();
}
