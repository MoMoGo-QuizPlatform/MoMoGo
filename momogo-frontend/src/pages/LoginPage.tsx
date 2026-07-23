import React, { useState } from 'react';
import { login, signUp, restoreUser, findPassword } from '../services/auth';
import type { UserResponse } from '../types/user';

interface LoginPageProps {
  onLoginSuccess: (user: UserResponse) => void;
  showToast: (message: string, type?: 'success' | 'error' | 'info') => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess, showToast }) => {
  const [isSignUp, setIsSignUp] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // 계정 복구(Restore) 관련 상태
  const [showRestoreModal, setShowRestoreModal] = useState(false);
  const [restorePassword, setRestorePassword] = useState('');

  // 비밀번호 찾기 관련 상태
  const [showFindPwdModal, setShowFindPwdModal] = useState(false);
  const [findPwdEmail, setFindPwdEmail] = useState('');
  const [findPwdSuccessMsg, setFindPwdSuccessMsg] = useState<string | null>(null);

  // 폼 검증 상태 (현대적인 인라인 에러 출력 용도)
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    const errors: Record<string, string> = {};

    if (isSignUp && !name.trim()) {
      errors.name = '이름을 입력해 주세요.';
    }
    if (!email.trim()) {
      errors.email = '이메일 주소를 입력해 주세요.';
    } else if (!/\S+@\S+\.\S+/.test(email)) {
      errors.email = '올바른 이메일 형식이 아닙니다.';
    }
    if (!password.trim()) {
      errors.password = '비밀번호를 입력해 주세요.';
    }

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      return;
    }

    setLoading(true);

    try {
      if (isSignUp) {
        // 회원가입
        await signUp({ name, email, password });
        showToast('회원가입이 완료되었습니다. 가입한 계정으로 로그인해 주세요.', 'success');
        setIsSignUp(false);
        setPassword('');
        setValidationErrors({});
      } else {
        // 로그인
        const response = await login(email, password);
        onLoginSuccess(response.user);
      }
    } catch (err: any) {
      // 탈퇴 유예 계정 처리: 백엔드가 내려주는 구조화된 에러 코드(errorKey/code)로 판별
      const code: string = err.code || '';
      if (code.includes('ALREADY_IN_PROGRESS_DELETE')) {
        setValidationErrors({});
        setShowRestoreModal(true);
      } else {
        setErrorMsg(err.message || '요청 처리에 실패했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  // 계정 복구 처리
  const handleRestore = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    const errors: Record<string, string> = {};

    if (!restorePassword.trim()) {
      errors.restorePassword = '비밀번호를 입력해 주세요.';
    }

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      return;
    }

    setLoading(true);

    try {
      await restoreUser(email, restorePassword);
      const loginRes = await login(email, restorePassword);
      onLoginSuccess(loginRes.user);
      showToast('계정이 복구되었으며 성공적으로 로그인되었습니다.', 'success');
      setShowRestoreModal(false);
      setRestorePassword('');
      setValidationErrors({});
    } catch (err: any) {
      showToast(err.message || '복구 처리에 실패했습니다. 비밀번호를 다시 확인해 주세요.', 'error');
    } finally {
      setLoading(false);
    }
  };

  // 임시 비밀번호 발급 처리
  const handleFindPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setFindPwdSuccessMsg(null);
    const errors: Record<string, string> = {};

    if (!findPwdEmail.trim()) {
      errors.findPwdEmail = '이메일 주소를 입력해 주세요.';
    } else if (!/\S+@\S+\.\S+/.test(findPwdEmail)) {
      errors.findPwdEmail = '올바른 이메일 형식이 아닙니다.';
    }

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      return;
    }

    setLoading(true);

    try {
      await findPassword(findPwdEmail);
      setFindPwdSuccessMsg('임시 비밀번호가 이메일로 발송되었습니다. (유효시간 3분)');
      setFindPwdEmail('');
      setValidationErrors({});
    } catch (err: any) {
      setErrorMsg(err.message || '비밀번호 찾기 처리에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 소셜 로그인 처리 (구글/카카오)
  const handleSocialLogin = (provider: 'google' | 'kakao') => {
    window.location.href = `/oauth2/authorization/${provider}`;
  };

  return (
    <div style={styles.container}>
      <div style={styles.loginCard} className="card">
        {/* 로고 영역 */}
        <div style={{ textAlign: 'center', marginBottom: '12px' }}>
          <img 
            src="/MoMoGo_Logo.png" 
            alt="MoMoGo Logo" 
            style={{ height: '56px', width: 'auto', objectFit: 'contain', cursor: 'pointer' }}
          />
        </div>

        <h1 style={styles.logoText}>MoMoGo</h1>
        <p style={styles.subText}>
          {isSignUp ? '간단한 정보를 입력해 계정을 만들어 보세요' : '학습 공간과 실시간 퀴즈 평가를 하나로'}
        </p>

        {errorMsg && <div style={styles.errorBanner}>{errorMsg}</div>}

        <form onSubmit={handleSubmit} style={styles.form} noValidate>
          {isSignUp && (
            <div className="input-group">
              <label className="input-label">이름</label>
              <input
                type="text"
                className="input-field"
                placeholder="이름을 입력해 주세요"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  if (validationErrors.name) {
                    setValidationErrors(prev => ({ ...prev, name: '' }));
                  }
                }}
                style={{
                  borderColor: validationErrors.name ? '#ef4444' : undefined,
                  boxShadow: validationErrors.name ? '0 0 0 3px rgba(239, 68, 68, 0.1)' : undefined
                }}
              />
              {validationErrors.name && (
                <span style={styles.errorText}>{validationErrors.name}</span>
              )}
            </div>
          )}

          <div className="input-group">
            <label className="input-label">이메일 주소</label>
            <input
              type="email"
              className="input-field"
              placeholder="example@domain.com"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (validationErrors.email) {
                  setValidationErrors(prev => ({ ...prev, email: '' }));
                }
              }}
              style={{
                borderColor: validationErrors.email ? '#ef4444' : undefined,
                boxShadow: validationErrors.email ? '0 0 0 3px rgba(239, 68, 68, 0.1)' : undefined
              }}
            />
            {validationErrors.email && (
              <span style={styles.errorText}>{validationErrors.email}</span>
            )}
          </div>

          <div className="input-group">
            <label className="input-label">비밀번호</label>
            <input
              type="password"
              className="input-field"
              placeholder="비밀번호를 입력해 주세요"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                if (validationErrors.password) {
                  setValidationErrors(prev => ({ ...prev, password: '' }));
                }
              }}
              style={{
                borderColor: validationErrors.password ? '#ef4444' : undefined,
                boxShadow: validationErrors.password ? '0 0 0 3px rgba(239, 68, 68, 0.1)' : undefined
              }}
            />
            {validationErrors.password && (
              <span style={styles.errorText}>{validationErrors.password}</span>
            )}
          </div>

          <button type="submit" className="btn btn-primary" style={styles.submitBtn} disabled={loading}>
            {loading ? '처리 중...' : isSignUp ? '회원가입 완료하기' : '로그인'}
          </button>
        </form>

        <div style={styles.footerLinks}>
          {!isSignUp && (
            <button
              type="button"
              style={styles.textLink}
              onClick={() => {
                setErrorMsg(null);
                setFindPwdSuccessMsg(null);
                setValidationErrors({});
                setShowFindPwdModal(true);
              }}
            >
              비밀번호 분실 찾기
            </button>
          )}
          <button
              type="button"
              style={styles.textLink}
              onClick={() => {
                setErrorMsg(null);
                setValidationErrors({});
                setIsSignUp(!isSignUp);
              }}
          >
            {isSignUp ? '이미 계정이 있으신가요? 로그인' : '처음이신가요? 이메일 회원가입'}
          </button>
        </div>

        {/* 소셜 로그인 버튼 세션 */}
        <div style={styles.socialDivider}>
          <span style={styles.socialDividerText}>또는 소셜 계정으로 시작하기</span>
        </div>

        <div style={styles.socialBtnGroup}>
          <button
            type="button"
            onClick={() => handleSocialLogin('google')}
            style={{ ...styles.socialBtn, ...styles.googleBtn }}
            disabled={loading}
          >
            <img src="https://img.icons8.com/color/16/000000/google-logo.png" alt="Google" style={{ marginRight: '8px' }} />
            Google로 시작하기
          </button>
          <button
            type="button"
            onClick={() => handleSocialLogin('kakao')}
            style={{ ...styles.socialBtn, ...styles.kakaoBtn }}
            disabled={loading}
          >
            <img src="https://img.icons8.com/ios-filled/16/000000/speech-bubble.png" alt="Kakao" style={{ marginRight: '8px', filter: 'brightness(0)' }} />
            Kakao로 시작하기
          </button>
        </div>
      </div>

      {/* 탈퇴 계정 복구 모달 */}
      {showRestoreModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>탈퇴 유예 계정 복구</h3>
            <p style={styles.modalDesc}>
              회원 탈퇴 신청 후 30일이 경과하지 않은 계정입니다. 계정을 복구하시려면 가입 당시의 비밀번호를 입력해 주세요.
            </p>
            <form onSubmit={handleRestore} style={styles.form} noValidate>
              <div className="input-group">
                <label className="input-label">비밀번호 확인</label>
                <input
                  type="password"
                  className="input-field"
                  placeholder="기존 계정 비밀번호"
                  value={restorePassword}
                  onChange={(e) => {
                    setRestorePassword(e.target.value);
                    if (validationErrors.restorePassword) {
                      setValidationErrors(prev => ({ ...prev, restorePassword: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.restorePassword ? '#ef4444' : undefined,
                    boxShadow: validationErrors.restorePassword ? '0 0 0 3px rgba(239, 68, 68, 0.1)' : undefined
                  }}
                />
                {validationErrors.restorePassword && (
                  <span style={styles.errorText}>{validationErrors.restorePassword}</span>
                )}
              </div>
              <div style={styles.modalActions}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => {
                    setShowRestoreModal(false);
                    setValidationErrors({});
                  }}
                  disabled={loading}
                >
                  취소
                </button>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  복구 및 로그인 진행
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 비밀번호 분실 찾기 모달 */}
      {showFindPwdModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>비밀번호 찾기</h3>
            <p style={styles.modalDesc}>
              가입하신 이메일 주소를 입력해 주시면 확인 코드 검증을 거쳐 3분 유효한 임시 비밀번호를 발송해 드립니다.
            </p>
            {findPwdSuccessMsg && <div style={styles.successBanner}>{findPwdSuccessMsg}</div>}
            <form onSubmit={handleFindPassword} style={styles.form} noValidate>
              <div className="input-group">
                <label className="input-label">가입 이메일 주소</label>
                <input
                  type="email"
                  className="input-field"
                  placeholder="example@domain.com"
                  value={findPwdEmail}
                  onChange={(e) => {
                    setFindPwdEmail(e.target.value);
                    if (validationErrors.findPwdEmail) {
                      setValidationErrors(prev => ({ ...prev, findPwdEmail: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.findPwdEmail ? '#ef4444' : undefined,
                    boxShadow: validationErrors.findPwdEmail ? '0 0 0 3px rgba(239, 68, 68, 0.1)' : undefined
                  }}
                />
                {validationErrors.findPwdEmail && (
                  <span style={styles.errorText}>{validationErrors.findPwdEmail}</span>
                )}
              </div>
              <div style={styles.modalActions}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => {
                    setShowFindPwdModal(false);
                    setFindPwdSuccessMsg(null);
                    setValidationErrors({});
                  }}
                  disabled={loading}
                >
                  닫기
                </button>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  임시 비밀번호 발급
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  socialDivider: {
    display: 'flex',
    alignItems: 'center',
    textAlign: 'center',
    margin: '1.25rem 0',
    color: '#667085',
    fontSize: '0.8rem',
  },
  socialDividerText: {
    padding: '0 10px',
    backgroundColor: '#fff',
    width: '100%',
    textAlign: 'center',
    position: 'relative',
  },
  socialBtnGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
    marginTop: '0.25rem',
  },
  socialBtn: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '0.75rem',
    borderRadius: '8px',
    fontSize: '0.875rem',
    fontWeight: 600,
    textDecoration: 'none',
    border: '1px solid #d0d5dd',
    transition: 'background-color 0.2s',
  },
  googleBtn: {
    backgroundColor: '#ffffff',
    color: '#344054',
  },
  kakaoBtn: {
    backgroundColor: '#fee500',
    color: '#191919',
    border: 'none',
  },
  container: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    backgroundColor: '#f9fafb',
    padding: '1.5rem',
  },
  loginCard: {
    width: '100%',
    maxWidth: '440px',
    padding: '2.5rem 2rem',
    textAlign: 'center',
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
    borderRadius: '16px',
    backgroundColor: '#ffffff',
  },
  mascotDecoration: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '0.5rem',
    marginBottom: '0.5rem',
  },
  mascotCircle: {
    width: '48px',
    height: '48px',
    borderRadius: '50%',
    backgroundColor: '#e0e7ff',
    border: '3px solid #6366f1',
    boxShadow: '0 0 10px rgba(99, 102, 241, 0.2)',
  },
  mascotSpeech: {
    fontSize: '0.75rem',
    fontWeight: 600,
    color: '#6366f1',
    backgroundColor: '#f0f2ff',
    padding: '0.35rem 0.75rem',
    borderRadius: '999px',
    border: '1px solid #c7d2fe',
  },
  logoText: {
    fontSize: '2.25rem',
    fontWeight: 800,
    color: '#1d2939',
    letterSpacing: '-1px',
    margin: '0',
  },
  subText: {
    fontSize: '0.875rem',
    color: '#475467',
    margin: '0 0 0.5rem 0',
  },
  form: {
    textAlign: 'left',
    display: 'flex',
    flexDirection: 'column',
  },
  submitBtn: {
    marginTop: '0.5rem',
    width: '100%',
    padding: '0.85rem',
  },
  footerLinks: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
    marginTop: '0.75rem',
    alignItems: 'center',
  },
  textLink: {
    border: 'none',
    background: 'none',
    color: '#6366f1',
    fontSize: '0.825rem',
    fontWeight: 600,
    cursor: 'pointer',
    textDecoration: 'underline',
  },
  errorBanner: {
    backgroundColor: '#fef2f2',
    color: '#ef4444',
    border: '1px solid rgba(239, 68, 68, 0.3)',
    borderRadius: '8px',
    padding: '0.75rem',
    fontSize: '0.825rem',
    textAlign: 'left',
  },
  successBanner: {
    backgroundColor: '#ecfdf5',
    color: '#10b981',
    border: '1px solid rgba(16, 185, 129, 0.3)',
    borderRadius: '8px',
    padding: '0.75rem',
    fontSize: '0.825rem',
    textAlign: 'left',
  },
  modalTitle: {
    fontSize: '1.25rem',
    fontWeight: 700,
    color: '#1d2939',
  },
  modalDesc: {
    fontSize: '0.875rem',
    color: '#475467',
    lineHeight: 1.5,
  },
  modalActions: {
    display: 'flex',
    gap: '0.75rem',
    justifyContent: 'flex-end',
    marginTop: '0.5rem',
  },
  errorText: {
    color: '#ef4444',
    fontSize: '0.75rem',
    marginTop: '0.25rem',
    textAlign: 'left',
    display: 'block',
  },
};
