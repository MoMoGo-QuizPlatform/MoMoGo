import React, { useEffect, useState } from 'react';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { SpacePage } from './pages/SpacePage'; // SpacePage 임포트 추가
import { refresh } from './services/auth';
import { request, setSessionExpiredHandler } from './services/api';
import type { UserResponse } from './types/user';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: string;
  message: string;
  type: ToastType;
}

const App: React.FC = () => {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [toasts, setToasts] = useState<Toast[]>([]);
  // 소셜 로그인 실패 시 백엔드가 리다이렉트로 넘긴 error/code 쿼리 파라미터를 담아 모달로 보여주기 위한 상태
  const [oauthErrorMessage, setOauthErrorMessage] = useState<string | null>(null);
  // 대시보드와 공간 상세 페이지 뷰 상태 정의
  const [view, setView] = useState<
    { type: 'dashboard'; initialTab?: 'dashboard' | 'superadmin' | 'mypage' } | { type: 'space'; space: any }
  >({ type: 'dashboard' });

  useEffect(() => {
    // 소셜 로그인 실패 리다이렉트(/oauth-callback?error=...&code=...)로 들어온 경우,
    // 주소창에 에러 메시지/코드가 그대로 노출되지 않도록 즉시 쿼리스트링을 제거하고 모달로 대체 안내한다.
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    const errorMsg = params.get('error');
    if (code || errorMsg) {
      window.history.replaceState({}, '', window.location.pathname);
      setOauthErrorMessage(
        code === 'USER-BANNED_USER'
          ? '해당 계정은 정지된 상태입니다.\n(관리자에게 문의하세요.)'
          : (errorMsg || '소셜 로그인 처리 중 오류가 발생했습니다.')
      );
    }

    checkSession();
  }, []);

  const checkSession = async () => {
    try {
      //  순수 쿠키 방식 (Cookie-Only Flow): URL 쿼리 파라미터에 토큰을 전혀 노출하지 않습니다.
      // 백엔드가 리다이렉트 시 보낸 HttpOnly Refresh Cookie로 /api/auth/refresh를 호출하여 Access Token을 안전하게 발급받습니다.
      const response = await refresh();
      if (response && response.accessToken) {
        const freshUser = await request<UserResponse>('/api/users/me', { method: 'GET' });
        setUser(freshUser);
      }
    } catch {
      setUser(null);
    } finally {
      setCheckingSession(false);
    }
  };

  const showToast = (message: string, type: ToastType = 'info') => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3000);
  };

  // 액세스 토큰 재발급까지 실패한 경우(=세션 만료) 강제 로그아웃 처리
  useEffect(() => {
    setSessionExpiredHandler(() => {
      setUser(null);
      setView({ type: 'dashboard' });
      showToast('세션이 만료되었습니다. 다시 로그인해 주세요.', 'error');
    });
    return () => setSessionExpiredHandler(null);
  }, []);

  if (checkingSession) {
    return (
      <div style={styles.loadingWrapper}>
        <img src="/MoMoGo_Logo.png" alt="MoMoGo Logo" style={{ width: '80px', height: 'auto', marginBottom: '8px' }} />
        <div style={styles.spinner}></div>
        <p style={styles.loadingText}>MoMoGo 세션을 확인하고 있습니다...</p>
      </div>
    );
  }

  return (
    <>
      {/* 글로벌 토스트 알림 컨테이너 */}
      <div style={styles.toastContainer}>
        {toasts.map((t) => (
          <div key={t.id} style={{ ...styles.toast, ...styles[t.type] }}>
            {t.message}
          </div>
        ))}
      </div>

      {/* 소셜 로그인 실패(정지 계정 등) 안내 모달 */}
      {oauthErrorMessage && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.oauthModalTitle}>로그인 실패</h3>
            <p style={styles.oauthModalDesc}>{oauthErrorMessage}</p>
            <div style={styles.oauthModalActions}>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => setOauthErrorMessage(null)}
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}

      {user === null ? (
        <LoginPage 
          onLoginSuccess={(loggedInUser) => {
            setUser(loggedInUser);
            setView({ type: 'dashboard' }); // 로그인 시 기본 대시보드 뷰 설정
          }} 
          showToast={showToast} 
        />
      ) : view.type === 'dashboard' ? (
        <DashboardPage
          user={user}
          initialTab={view.initialTab}
          onLogout={() => setUser(null)}
          showToast={showToast}
          onEnterSpace={(space) => setView({ type: 'space', space })}
          onUserUpdate={(updatedUser) => setUser(updatedUser)}
        />
      ) : (
        <SpacePage
          user={user}
          space={view.space}
          onBack={(tab = 'dashboard') => setView({ type: 'dashboard', initialTab: tab })}
          showToast={showToast}
        />
      )}
    </>
  );
};

const styles: Record<string, React.CSSProperties> = {
  loadingWrapper: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    backgroundColor: '#f9fafb',
    gap: '1rem',
  },
  spinner: {
    width: '40px',
    height: '40px',
    border: '3px solid #eaecf0',
    borderTopColor: '#6366f1',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  loadingText: {
    fontSize: '0.9rem',
    color: '#475467',
    fontWeight: 600,
  },
  toastContainer: {
    position: 'fixed',
    top: '20px',
    left: '50%',
    transform: 'translateX(-50%)',
    zIndex: 9999,
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
    width: '100%',
    maxWidth: '380px',
    pointerEvents: 'none',
  },
  toast: {
    padding: '12px 16px',
    borderRadius: '8px',
    fontSize: '0.875rem',
    fontWeight: 600,
    boxShadow: '0 4px 12px rgba(16, 24, 40, 0.1)',
    pointerEvents: 'auto',
    animation: 'slideDown 0.3s ease-out forwards',
    textAlign: 'center',
  },
  success: {
    backgroundColor: '#ecfdf5',
    color: '#047857',
    border: '1px solid #a7f3d0',
  },
  error: {
    backgroundColor: '#fef2f2',
    color: '#b91c1c',
    border: '1px solid #fca5a5',
  },
  info: {
    backgroundColor: '#f0f2ff',
    color: '#4338ca',
    border: '1px solid #c7d2fe',
  },
  oauthModalTitle: {
    fontSize: '1.25rem',
    fontWeight: 700,
    color: '#1d2939',
  },
  oauthModalDesc: {
    fontSize: '0.875rem',
    color: '#475467',
    lineHeight: 1.5,
    whiteSpace: 'pre-line',
  },
  oauthModalActions: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginTop: '0.5rem',
  },
};

if (typeof document !== 'undefined') {
  const style = document.createElement('style');
  style.innerHTML = `
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    @keyframes slideDown {
      from { transform: translateY(-20px); opacity: 0; }
      to { transform: translateY(0); opacity: 1; }
    }
  `;
  document.head.appendChild(style);
}

export default App;
