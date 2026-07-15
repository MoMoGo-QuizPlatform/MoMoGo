import React, { useEffect, useState } from 'react';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { SpacePage } from './pages/SpacePage'; // SpacePage 임포트 추가
import { refresh } from './services/auth';
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
  // 대시보드와 공간 상세 페이지 뷰 상태 정의
  const [view, setView] = useState<{ type: 'dashboard' } | { type: 'space'; space: any }>({ type: 'dashboard' });

  useEffect(() => {
    checkSession();
  }, []);

  const checkSession = async () => {
    try {
      const response = await refresh();
      if (response && response.user) {
        setUser(response.user);
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

  if (checkingSession) {
    return (
      <div style={styles.loadingWrapper}>
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
          onLogout={() => setUser(null)} 
          showToast={showToast} 
          onEnterSpace={(space) => setView({ type: 'space', space })}
        />
      ) : (
        <SpacePage
          user={user}
          space={view.space}
          onBack={() => setView({ type: 'dashboard' })}
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
