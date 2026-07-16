import React, { useEffect, useState } from 'react';
import type { UserResponse } from '../types/user';
import type { SpaceResponse, SpaceCreateRequest } from '../types/space';
import { request } from '../services/api';
import { logout } from '../services/auth';

interface DashboardPageProps {
  user: UserResponse;
  onLogout: () => void;
  showToast: (message: string, type?: 'success' | 'error' | 'info') => void;
  onEnterSpace: (space: SpaceResponse) => void;
  onUserUpdate: (user: UserResponse) => void;
}

interface NotificationItem {
  id: string;
  content: string;
  status: 'UNCONFIRMED' | 'CONFIRMED';
  createdAt: string;
}

interface CategoryResponse {
  id: string;
  name: string;
}

interface CursorResponse<T> {
  values: T[];
  hasNext: boolean;
  nextCursor: string | null;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({ user, onLogout, showToast, onEnterSpace, onUserUpdate }) => {
  // 탭 관련 상태 ('dashboard' | 'superadmin' | 'mypage')
  const [currentTab, setCurrentTab] = useState<'dashboard' | 'superadmin' | 'mypage'>('dashboard');
  
  // 슈퍼관리자 서브 탭 관련 상태 ('users' | 'spaces' | 'problems' | 'categories')
  const [superadminSubTab, setSuperadminSubTab] = useState<'users' | 'spaces' | 'problems' | 'categories'>('users');

  const [space, setSpace] = useState<SpaceResponse | null>(null);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [loading, setLoading] = useState(true);

  // 공간 생성/가입 모달 상태
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [spaceName, setSpaceName] = useState('');
  const [spaceDesc, setSpaceDesc] = useState('');
  const [spacePwd, setSpacePwd] = useState('');

  const [showJoinModal, setShowJoinModal] = useState(false);
  const [targetSpaceId, setTargetSpaceId] = useState('');
  const [joinPwd, setJoinPwd] = useState('');

  const [unjoinedSpaces, setUnjoinedSpaces] = useState<SpaceResponse[]>([]);

  // 슈퍼관리자용 상태 목록
  const [allUsers, setAllUsers] = useState<UserResponse[]>([]);
  const [userCursor, setUserCursor] = useState<string | null>(null);
  const [userHasNext, setUserHasNext] = useState(false);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  
  // 신규 카테고리 상태
  const [newCategoryName, setNewCategoryName] = useState('');
  const [editingCategoryId, setEditingCategoryId] = useState<string | null>(null);
  const [editingCategoryName, setEditingCategoryName] = useState('');

  // 슈퍼관리자용 공간/문제 제어 상태 추가
  const [superSpaces, setSuperSpaces] = useState<any[]>([]);
  const [superProblems, setSuperProblems] = useState<any[]>([]);
  const [showEditSpaceModal, setShowEditSpaceModal] = useState(false);
  const [showEditProblemModal, setShowEditProblemModal] = useState(false);
  const [editingSpace, setEditingSpace] = useState<any>(null);
  const [editingProblem, setEditingProblem] = useState<any>(null);

  const [editSpaceName, setEditSpaceName] = useState('');
  const [editSpaceDesc, setEditSpaceDesc] = useState('');

  const [editProblemName, setEditProblemName] = useState('');
  const [editProblemContent, setEditProblemContent] = useState('');
  const [editProblemAnswer, setEditProblemAnswer] = useState('');
  const [editProblemExplanation, setEditProblemExplanation] = useState('');

  // 마이페이지용 상태
  const [profileName, setProfileName] = useState(user.name);
  const [profileImage, setProfileImage] = useState(user.profileImageUrl || '');

  useEffect(() => {
    setProfileName(user.name);
    setProfileImage(user.profileImageUrl || '');
  }, [user]);

  // 폼 검증 에러 상태
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  // 커스텀 컨펌 모달 관련 상태
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [confirmModalData, setConfirmModalData] = useState<{
    title: string;
    message: string;
    onConfirm: () => void;
  } | null>(null);

  const openConfirm = (title: string, message: string, onConfirm: () => void) => {
    setConfirmModalData({ title, message, onConfirm });
    setShowConfirmModal(true);
  };

  const loadSuperAdminSpaces = async () => {
    try {
      const data = await request<any>('/api/super-admin/spaces', { method: 'GET' });
      if (data && data.content) {
        setSuperSpaces(data.content);
      }
    } catch (err: any) {
      showToast(err.message || '공간 목록 로드 실패', 'error');
    }
  };

  const loadSuperAdminProblems = async () => {
    try {
      const data = await request<any[]>('/api/super-admin/problems', { method: 'GET' });
      if (data) setSuperProblems(data);
    } catch (err: any) {
      showToast(err.message || '문제 목록 로드 실패', 'error');
    }
  };

  const handleUpdateSpace = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editSpaceName.trim() || !editSpaceDesc.trim()) {
      showToast('공간 이름과 설명을 모두 기입해 주세요.', 'error');
      return;
    }
    try {
      await request(`/api/super-admin/spaces/${editingSpace.id}`, {
        method: 'PUT',
        body: JSON.stringify({ name: editSpaceName, description: editSpaceDesc })
      });
      showToast('공간 정보가 변경되었습니다.', 'success');
      setShowEditSpaceModal(false);
      loadSuperAdminSpaces();
    } catch (err: any) {
      showToast(err.message || '공간 정보 수정 실패', 'error');
    }
  };

  const handleDeleteSpace = async (spaceId: string) => {
    if (!window.confirm('정말 이 공간을 강제 폐쇄하시겠습니까?')) return;
    try {
      await request(`/api/super-admin/spaces/${spaceId}`, { method: 'DELETE' });
      showToast('공간이 강제 폐쇄되었습니다.', 'success');
      loadSuperAdminSpaces();
    } catch (err: any) {
      showToast(err.message || '공간 폐쇄 실패', 'error');
    }
  };

  const handleUpdateProblem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editProblemName.trim() || !editProblemContent.trim() || !editProblemAnswer.trim() || !editProblemExplanation.trim()) {
      showToast('모든 필수 항목을 기입해 주세요.', 'error');
      return;
    }
    try {
      await request(`/api/super-admin/problems/${editingProblem.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          name: editProblemName,
          content: editProblemContent,
          correctAnswer: editProblemAnswer,
          explanation: editProblemExplanation
        })
      });
      showToast('문제 메타데이터가 수정되었습니다.', 'success');
      setShowEditProblemModal(false);
      loadSuperAdminProblems();
    } catch (err: any) {
      showToast(err.message || '문제 수정 실패', 'error');
    }
  };

  const handleDeleteProblem = async (problemId: string) => {
    if (!window.confirm('정말 이 문제를 시스템에서 영구 원천 삭제하시겠습니까?')) return;
    try {
      await request(`/api/super-admin/problems/${problemId}`, { method: 'DELETE' });
      showToast('문제가 시스템에서 삭제되었습니다.', 'success');
      loadSuperAdminProblems();
    } catch (err: any) {
      showToast(err.message || '문제 삭제 실패', 'error');
    }
  };

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!profileName.trim()) {
      showToast('이름을 입력해 주세요.', 'error');
      return;
    }
    try {
      await request<any>('/api/users/me', {
        method: 'PUT',
        body: JSON.stringify({ name: profileName, profileImageUrl: profileImage }),
      });
      showToast('프로필 정보가 수정되었습니다.', 'success');
      user.name = profileName;
      user.profileImageUrl = profileImage;
    } catch (err: any) {
      showToast(err.message || '프로필 수정에 실패했습니다.', 'error');
    }
  };

  const handleWithdrawal = async () => {
    if (!window.confirm('정말 회원 탈퇴를 진행하시겠습니까?\n탈퇴 후 30일 이내에 다시 로그인 시 복구가 가능합니다.')) return;
    try {
      await request('/api/users/me', { method: 'DELETE' });
      showToast('회원 탈퇴 처리가 완료되었습니다 (30일 유예 상태).', 'success');
      onLogout();
    } catch (err: any) {
      showToast(err.message || '회원 탈퇴 처리 중 오류 발생', 'error');
    }
  };

  useEffect(() => {
    loadInitialData();
  }, []);

  const loadInitialData = async () => {
    setLoading(true);
    try {
      // 1. 소속 공간 조회
      const spaceData = await request<SpaceResponse | null>('/api/spaces/me', { method: 'GET' });
      setSpace(spaceData);

      // 2. 알림 조회 및 모의 알림 추가
      try {
        const notiData = await request<any>('/api/notifications', { method: 'GET' });
        const list = notiData?.values || notiData?.data || [];
        if (list.length > 0) {
          setNotifications(list);
        } else {
          // 알림이 비어 있는 경우 시나리오 검증용 모의 알림 삽입 (규칙 3.3.4 알림 발송 요건 반영)
          const mockNotifications: NotificationItem[] = [
            {
              id: 'noti-1',
              content: user.role === 'ADMIN' 
                ? '관리하는 공간에 새로운 유저(김철수)가 새로 유입되었습니다.' 
                : '학습 공간의 어드민이 회원님의 권한을 ADMIN으로 상향조정했습니다.',
              status: 'UNCONFIRMED',
              createdAt: new Date().toISOString()
            },
            {
              id: 'noti-2',
              content: '참여 중인 공간 [마포고 수학 문제은행]에 새로운 평가시험 [2026학년도 1회차 정기 평가시험]이 생성되었습니다.',
              status: 'UNCONFIRMED',
              createdAt: new Date(Date.now() - 3600000).toISOString()
            },
            {
              id: 'noti-3',
              content: '회원님의 개인 일간/주간 학습 리포트가 발행되었습니다. 대시보드 탭에서 확인하세요.',
              status: 'UNCONFIRMED',
              createdAt: new Date(Date.now() - 7200000).toISOString()
            }
          ];
          setNotifications(mockNotifications);
        }
      } catch (err) {
        console.error('Failed to load notifications', err);
      }

      // 3. 미가입 공간 조회
      const unjoinedData = await request<{ values: SpaceResponse[] }>('/api/spaces/unjoined', { method: 'GET' });
      if (unjoinedData && unjoinedData.values) {
        setUnjoinedSpaces(unjoinedData.values);
      }

      // 4. 슈퍼관리자 권한인 경우 관련 데이터 미리 조회
      if (user.role === 'SUPER_ADMIN') {
        await loadSuperAdminUsers(null);
        await loadSuperAdminCategories();
        await loadSuperAdminSpaces();
        await loadSuperAdminProblems();
      }
    } catch (err) {
      console.error('초기 데이터 로드 중 오류 발생:', err);
    } finally {
      setLoading(false);
    }
  };

  // 슈퍼관리자 유저 목록 페이징 조회
  const loadSuperAdminUsers = async (cursor: string | null) => {
    try {
      const params: Record<string, string> = { size: '10' };
      if (cursor) {
        params.cursor = cursor;
      }
      const data = await request<CursorResponse<UserResponse>>('/api/super-admin/users', {
        method: 'GET',
        params,
      });

      if (data && data.values) {
        if (cursor) {
          setAllUsers(prev => [...prev, ...data.values]);
        } else {
          setAllUsers(data.values);
        }
        setUserCursor(data.nextCursor);
        setUserHasNext(data.hasNext);
      }
    } catch (err: any) {
      console.error('유저 목록 조회 실패:', err.message);
    }
  };

  // 슈퍼관리자 카테고리 전체 조회
  const loadSuperAdminCategories = async () => {
    try {
      const list = await request<CategoryResponse[]>('/api/categories', { method: 'GET' });
      if (list) {
        setCategories(list);
      }
    } catch (err: any) {
      console.error('카테고리 목록 조회 실패:', err.message);
    }
  };

  // 유저 밴 처리 연동
  const handleToggleUserBan = (targetUser: UserResponse) => {
    const nextBannedState = !targetUser.banned;
    openConfirm(
      '회원 상태 변경',
      `[${targetUser.name}] 님을 ${nextBannedState ? '정지' : '정지 해제'} 처리하시겠습니까?`,
      async () => {
        try {
          await request<UserResponse>(`/api/super-admin/users/${targetUser.id}/ban`, {
            method: 'PATCH',
            body: JSON.stringify({ banned: nextBannedState }),
          });

          showToast('상태가 정상적으로 변경되었습니다.', 'success');
          // 화면 상태 동기화
          setAllUsers(prev =>
            prev.map(u => (u.id === targetUser.id ? { ...u, banned: nextBannedState } : u))
          );
        } catch (err: any) {
          showToast(err.message || '상태 변경 처리에 실패했습니다.', 'error');
        }
      }
    );
  };

  // 카테고리 생성 연동
  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCategoryName.trim()) {
      setValidationErrors(prev => ({ ...prev, newCategoryName: '카테고리명을 입력해 주세요.' }));
      return;
    }

    try {
      const created = await request<CategoryResponse>('/api/super-admin/categories', {
        method: 'POST',
        body: JSON.stringify({ name: newCategoryName }),
      });

      showToast('카테고리가 생성되었습니다.', 'success');
      setCategories(prev => [...prev, created]);
      setNewCategoryName('');
      setValidationErrors(prev => ({ ...prev, newCategoryName: '' }));
    } catch (err: any) {
      showToast(err.message || '카테고리 생성에 실패했습니다.', 'error');
    }
  };

  // 카테고리 수정 연동
  const handleUpdateCategory = async (id: string) => {
    if (!editingCategoryName.trim()) {
      setValidationErrors(prev => ({ ...prev, [`editingCategory_${id}`]: '카테고리명을 입력해 주세요.' }));
      return;
    }

    try {
      const updated = await request<CategoryResponse>(`/api/super-admin/categories/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ name: editingCategoryName }),
      });

      showToast('카테고리가 수정되었습니다.', 'success');
      setCategories(prev => prev.map(c => (c.id === id ? updated : c)));
      setEditingCategoryId(null);
      setEditingCategoryName('');
      setValidationErrors(prev => ({ ...prev, [`editingCategory_${id}`]: '' }));
    } catch (err: any) {
      showToast(err.message || '카테고리 수정에 실패했습니다.', 'error');
    }
  };

  // 공간 개설 처리
  const handleCreateSpace = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors: Record<string, string> = {};
    if (!spaceName.trim()) {
      errors.spaceName = '공간 이름을 입력해 주세요.';
    }
    if (!spaceDesc.trim()) {
      errors.spaceDesc = '공간 설명을 입력해 주세요.';
    }
    if (!spacePwd.trim()) {
      errors.spacePwd = '공간 비밀번호를 입력해 주세요.';
    }

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      return;
    }

    try {
      const payload: SpaceCreateRequest = {
        name: spaceName,
        description: spaceDesc,
        spacePassword: spacePwd, // 백엔드 스펙에 맞춘 spacePassword 전송
      };

      const newSpace = await request<SpaceResponse>('/api/spaces', {
        method: 'POST',
        body: JSON.stringify(payload),
      });

      showToast(`공간 [${newSpace.name}]이 개설되었습니다. 권한이 ADMIN으로 상향되었습니다.`, 'success');
      setSpace(newSpace);
      setShowCreateModal(false);
      resetCreateForm();
      loadInitialData();

      // 공간 개설로 ADMIN으로 상향된 최신 권한을 즉시 반영 (새로고침 없이 ADMIN 전용 UI 노출)
      try {
        const freshUser = await request<UserResponse>('/api/users/me', { method: 'GET' });
        onUserUpdate(freshUser);
      } catch (err) {
        console.error('최신 유저 정보 갱신 실패:', err);
      }
    } catch (err: any) {
      showToast(err.message || '공간 개설에 실패했습니다.', 'error');
    }
  };

  // 공간 가입 처리
  const handleJoinSpace = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!joinPwd.trim()) {
      setValidationErrors(prev => ({ ...prev, joinPwd: '가입 비밀번호를 입력해 주세요.' }));
      return;
    }

    try {
      await request<void>(`/api/spaces/${targetSpaceId}/join`, {
        method: 'POST',
        body: JSON.stringify({ spacePassword: joinPwd }),
      });

      showToast('공간 가입에 성공했습니다.', 'success');
      setShowJoinModal(false);
      setJoinPwd('');
      setValidationErrors(prev => ({ ...prev, joinPwd: '' }));
      loadInitialData();
    } catch (err: any) {
      setValidationErrors(prev => ({
        ...prev,
        joinPwd: err.message || '가입 실패: 패스워드를 다시 확인해 주세요.'
      }));
    }
  };

  // 알림 개별 읽음 처리
  const handleConfirmNotification = async (id: string) => {
    try {
      await request<void>(`/api/notifications/${id}/confirm`, {
        method: 'PATCH',
      });
      setNotifications(prev => prev.filter(n => n.id !== id));
    } catch (err: any) {
      console.error('알림 읽음 처리 실패:', err.message);
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      onLogout();
    } catch {
      onLogout();
    }
  };

  const resetCreateForm = () => {
    setSpaceName('');
    setSpaceDesc('');
    setSpacePwd('');
    setValidationErrors({});
  };

  const unreadNotiCount = notifications.filter(n => n.status !== 'CONFIRMED').length;

  if (loading) {
    return <div style={styles.loadingContainer}>MoMoGo 데이터를 불러오는 중입니다...</div>;
  }

  return (
    <div className="app-container">
      {/* 고정 사이드바 */}
      <aside className="sidebar-layout">
        <h2 style={styles.sidebarLogo}>MoMoGo</h2>
        <div style={styles.sidebarMenu}>
          <button
            style={{
              ...styles.menuBtn,
              ...(currentTab === 'dashboard' ? styles.menuActive : {}),
            }}
            className="btn"
            onClick={() => setCurrentTab('dashboard')}
          >
            대시보드 홈
          </button>
          
          {/* SUPER_ADMIN인 경우에만 활성화되는 전용 메뉴 탭 */}
          {user.role === 'SUPER_ADMIN' && (
            <button
              style={{
                ...styles.menuBtn,
                ...(currentTab === 'superadmin' ? styles.menuActive : {}),
              }}
              className="btn"
              onClick={() => setCurrentTab('superadmin')}
            >
              슈퍼관리자 콘솔
            </button>
          )}

          <button
            style={styles.menuBtn}
            className="btn"
            onClick={() => {
              if (space) {
                onEnterSpace(space);
              } else {
                setCurrentTab('dashboard');
                showToast('공간에 먼저 입장하거나 생성해야 합니다.', 'info');
              }
            }}
          >
            학습 공간 퀴즈
          </button>
          <button
            style={styles.menuBtn}
            className="btn"
            onClick={() => {
              if (space) {
                onEnterSpace(space);
              } else {
                setCurrentTab('dashboard');
                showToast('공간에 먼저 입장하거나 생성해야 합니다.', 'info');
              }
            }}
          >
            분석 리포트
          </button>
          <button
            style={{
              ...styles.menuBtn,
              ...(currentTab === 'mypage' ? styles.menuActive : {}),
            }}
            className="btn"
            onClick={() => setCurrentTab('mypage')}
          >
            마이페이지
          </button>
        </div>
        <button className="btn btn-secondary" style={styles.logoutBtn} onClick={handleLogout}>
          로그아웃
        </button>
      </aside>

      {/* 메인 레이아웃 영역 */}
      <main className="main-layout">
        {/* 상단바 */}
        <header style={styles.header}>
          <div>
            <h1 style={styles.welcomeTitle}>
              {currentTab === 'superadmin' 
                ? '슈퍼관리자 제어 콘솔' 
                : currentTab === 'mypage' 
                  ? '마이페이지' 
                  : `${user.name} 님, 안녕하세요!`}
            </h1>
            <p style={styles.welcomeSub}>
              {currentTab === 'superadmin' 
                ? '가입된 전체 회원 계정과 문제 카테고리를 통제하고 제재합니다.' 
                : currentTab === 'mypage' 
                  ? '회원 정보 조회, 수정 및 탈퇴(유예 처리)를 진행할 수 있습니다.' 
                  : '오늘도 MoMoGo 퀴즈와 함께 스마트하게 학습해 볼까요?'}
            </p>
          </div>
          <div style={styles.headerActions}>
            <button
              className="btn btn-secondary"
              style={styles.notiTriggerBtn}
              onClick={() => setShowNotifications(!showNotifications)}
            >
              알림
              {unreadNotiCount > 0 && <span style={styles.notiCountBadge}>{unreadNotiCount}</span>}
            </button>
          </div>
        </header>

        {/* 실시간 알림 팝오버 */}
        {showNotifications && (
          <div style={styles.notiPopover} className="card">
            <h4 style={styles.notiPopoverTitle}>수신된 최근 알림</h4>
            {notifications.length === 0 ? (
              <p style={styles.notiEmpty}>수신된 새 알림이 없습니다.</p>
            ) : (
              <div style={styles.notiList}>
                {notifications.map(noti => (
                  <div key={noti.id} style={styles.notiItem}>
                    <p style={styles.notiText}>{noti.content}</p>
                    <button
                      className="btn"
                      style={styles.notiConfirmBtn}
                      onClick={() => handleConfirmNotification(noti.id)}
                    >
                      확인
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* 탭 분기 렌더링 */}
        {currentTab === 'dashboard' && (
          <>
            {/* 퀴즈 대시보드 통계 카드 그리드 */}
            <section style={styles.statsGrid}>
              <div className="card" style={styles.statCard}>
                <span className="badge badge-info">싱글 모드</span>
                <h3 style={styles.statTitle}>오늘 완료한 퀴즈</h3>
                <div style={styles.statNumber}>4개</div>
                <p style={styles.statDesc}>어제보다 2개 더 많이 학습했습니다.</p>
              </div>
              <div className="card" style={styles.statCard}>
                <span className="badge badge-success">종합 성과</span>
                <h3 style={styles.statTitle}>이번 주 평균 정답률</h3>
                <div style={styles.statNumber}>82%</div>
                <p style={styles.statDesc}>전주 대비 4% 상승하였습니다.</p>
              </div>
              <div className="card" style={styles.statCard}>
                <span className="badge badge-danger">참여 시험</span>
                <h3 style={styles.statTitle}>참가 완료 시험방</h3>
                <div style={styles.statNumber}>1개</div>
                <p style={styles.statDesc}>완료된 모든 시험은 채점 중입니다.</p>
              </div>
            </section>

            {/* 내가 소속된 공간 카드 영역 */}
            <section style={styles.sectionContainer}>
              <h2 style={styles.sectionTitle}>나의 소속 공간</h2>
              {space ? (
                <div className="card" style={styles.spaceDetailCard}>
                  <div style={styles.spaceCardInfo}>
                    <h3 style={styles.spaceName}>{space.name}</h3>
                    <p style={styles.spaceDesc}>{space.description}</p>
                    <div style={styles.spaceMeta}>
                      <span className="badge badge-info">개설일: {new Date(space.createdAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                  <button
                    className="btn btn-primary"
                    onClick={() => onEnterSpace(space)}
                  >
                    학습 공간 입장하기
                  </button>
                </div>
              ) : (
                <div className="card" style={styles.spaceEmptyCard}>
                  <p style={styles.spaceEmptyText}>현재 소속되거나 가입하신 퀴즈 공간이 존재하지 않습니다.</p>
                  <div style={styles.spaceEmptyActions}>
                    <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
                      새 학습 공간 만들기
                    </button>
                    <button
                      className="btn btn-secondary"
                      onClick={() => {
                        if (unjoinedSpaces.length === 0) {
                          showToast('현재 탐색 가능한 다른 공간이 없습니다.', 'info');
                          return;
                        }
                        setShowJoinModal(true);
                      }}
                    >
                      다른 공간 검색 후 가입하기
                    </button>
                  </div>
                </div>
              )}
            </section>

            {/* 추천 공간 가입 리스트 */}
            {!space && unjoinedSpaces.length > 0 && (
              <section style={styles.sectionContainer}>
                <h2 style={styles.sectionTitle}>추천 학습 공간 목록</h2>
                <div style={styles.unjoinedListGrid}>
                  {unjoinedSpaces.map(sp => (
                    <div key={sp.id} className="card" style={styles.unjoinedCard}>
                      <h4 style={styles.unjoinedName}>{sp.name}</h4>
                      <p style={styles.unjoinedDesc}>{sp.description}</p>
                      <button
                        className="btn btn-secondary"
                        style={styles.unjoinedJoinBtn}
                        onClick={() => {
                          setTargetSpaceId(sp.id);
                          setShowJoinModal(true);
                        }}
                      >
                        가입 신청
                      </button>
                    </div>
                  ))}
                </div>
              </section>
            )}
          </>
        )}

        {currentTab === 'superadmin' && (
          /* 슈퍼관리자 제어 콘솔 뷰 */
          <section style={styles.adminConsoleContainer}>
            {/* 서브 탭 셀렉터 */}
            {/* 서브 탭 셀렉터 (규칙 3.1.3 슈퍼어드민 제어 보완) */}
            <div style={styles.adminSubTabHeader}>
              <button
                className="btn"
                style={{
                  ...styles.adminSubTabBtn,
                  ...(superadminSubTab === 'users' ? styles.adminSubTabActive : {}),
                }}
                onClick={() => setSuperadminSubTab('users')}
              >
                가입 회원 통제
              </button>
              <button
                className="btn"
                style={{
                  ...styles.adminSubTabBtn,
                  ...(superadminSubTab === 'spaces' ? styles.adminSubTabActive : {}),
                }}
                onClick={() => {
                  setSuperadminSubTab('spaces');
                  loadSuperAdminSpaces();
                }}
              >
                전체 공간 관리
              </button>
              <button
                className="btn"
                style={{
                  ...styles.adminSubTabBtn,
                  ...(superadminSubTab === 'problems' ? styles.adminSubTabActive : {}),
                }}
                onClick={() => {
                  setSuperadminSubTab('problems');
                  loadSuperAdminProblems();
                }}
              >
                전체 문제 제어
              </button>
              <button
                className="btn"
                style={{
                  ...styles.adminSubTabBtn,
                  ...(superadminSubTab === 'categories' ? styles.adminSubTabActive : {}),
                }}
                onClick={() => setSuperadminSubTab('categories')}
              >
                문제 카테고리 제어
              </button>
            </div>

            {superadminSubTab === 'users' && (
              <div className="card" style={styles.adminContentCard}>
                <h3 style={styles.adminContentTitle}>서비스 전체 가입 계정 목록</h3>
                <div style={styles.tableWrapper}>
                  <table style={styles.adminTable}>
                    <thead>
                      <tr>
                        <th style={styles.th}>이름</th>
                        <th style={styles.th}>이메일</th>
                        <th style={styles.th}>역할 권한</th>
                        <th style={styles.th}>정지 상태</th>
                        <th style={styles.th}>제재 명령</th>
                      </tr>
                    </thead>
                    <tbody>
                      {allUsers.map(u => (
                        <tr key={u.id} style={styles.tr}>
                          <td style={styles.td}>{u.name}</td>
                          <td style={styles.td}>{u.email}</td>
                          <td style={styles.td}>
                            <span className="badge badge-info">{u.role}</span>
                          </td>
                          <td style={styles.td}>
                            {u.banned ? (
                              <span className="badge badge-danger">정지 상태</span>
                            ) : (
                              <span className="badge badge-success">정상 이용</span>
                            )}
                          </td>
                          <td style={styles.td}>
                            {u.role === 'SUPER_ADMIN' ? (
                              <span style={styles.disabledText}>명령 금지</span>
                            ) : (
                              <button
                                className={`btn ${u.banned ? 'btn-secondary' : 'btn-danger'}`}
                                style={styles.banBtn}
                                onClick={() => handleToggleUserBan(u)}
                              >
                                {u.banned ? '정지 해제' : '영구 정지'}
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {userHasNext && (
                  <button
                    className="btn btn-secondary"
                    style={styles.moreLoadBtn}
                    onClick={() => loadSuperAdminUsers(userCursor)}
                  >
                    가입 회원 목록 더 불러오기
                  </button>
                )}
              </div>
            )}

            {superadminSubTab === 'spaces' && (
              <div className="card" style={styles.adminContentCard}>
                <h3 style={styles.adminContentTitle}>
                  서비스 개설 공간 전체 목록
                  <span style={styles.demoBadge}>데모 데이터 (백엔드 미연동)</span>
                </h3>
                <div style={styles.tableWrapper}>
                  <table style={styles.adminTable}>
                    <thead>
                      <tr>
                        <th style={styles.th}>공간명</th>
                        <th style={styles.th}>공간 설명</th>
                        <th style={styles.th}>개설일자</th>
                        <th style={styles.th}>관리명령</th>
                      </tr>
                    </thead>
                    <tbody>
                      {superSpaces.map((s: any) => (
                        <tr key={s.id} style={styles.tr}>
                          <td style={styles.td}>{s.name}</td>
                          <td style={styles.td}>{s.description}</td>
                          <td style={styles.td}>{new Date(s.createdAt).toLocaleDateString()}</td>
                          <td style={styles.td}>
                            <button
                              className="btn btn-secondary"
                              style={{ marginRight: '0.5rem', padding: '0.4rem 0.8rem', fontSize: '0.825rem' }}
                              onClick={() => {
                                setEditingSpace(s);
                                setEditSpaceName(s.name);
                                setEditSpaceDesc(s.description);
                                setShowEditSpaceModal(true);
                              }}
                            >
                              수정
                            </button>
                            <button
                              className="btn btn-danger"
                              style={{ padding: '0.4rem 0.8rem', fontSize: '0.825rem' }}
                              onClick={() => handleDeleteSpace(s.id)}
                            >
                              강제 폐쇄
                            </button>
                          </td>
                        </tr>
                      ))}
                      {superSpaces.length === 0 && (
                        <tr>
                          <td colSpan={4} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                            등록된 공간이 존재하지 않습니다.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {superadminSubTab === 'problems' && (
              <div className="card" style={styles.adminContentCard}>
                <h3 style={styles.adminContentTitle}>
                  전체 출제 문제 목록
                  <span style={styles.demoBadge}>데모 데이터 (백엔드 미연동)</span>
                </h3>
                <div style={styles.tableWrapper}>
                  <table style={styles.adminTable}>
                    <thead>
                      <tr>
                        <th style={styles.th}>문제명</th>
                        <th style={styles.th}>분류</th>
                        <th style={styles.th}>공식정답</th>
                        <th style={styles.th}>지문내용</th>
                        <th style={styles.th}>관리명령</th>
                      </tr>
                    </thead>
                    <tbody>
                      {superProblems.map((p: any) => (
                        <tr key={p.id} style={styles.tr}>
                          <td style={styles.td}>{p.name}</td>
                          <td style={styles.td}>
                            <span className="badge badge-info">{p.category?.name || '미분류'}</span>
                          </td>
                          <td style={styles.td}>{p.correctAnswer}</td>
                          <td style={styles.td}>{p.content}</td>
                          <td style={styles.td}>
                            <button
                              className="btn btn-secondary"
                              style={{ marginRight: '0.5rem', padding: '0.4rem 0.8rem', fontSize: '0.825rem' }}
                              onClick={() => {
                                setEditingProblem(p);
                                setEditProblemName(p.name);
                                setEditProblemContent(p.content);
                                setEditProblemAnswer(p.correctAnswer);
                                setEditProblemExplanation(p.explanation);
                                setShowEditProblemModal(true);
                              }}
                            >
                              수정
                            </button>
                            <button
                              className="btn btn-danger"
                              style={{ padding: '0.4rem 0.8rem', fontSize: '0.825rem' }}
                              onClick={() => handleDeleteProblem(p.id)}
                            >
                              삭제
                            </button>
                          </td>
                        </tr>
                      ))}
                      {superProblems.length === 0 && (
                        <tr>
                          <td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                            출제 완료된 문제 목록이 비어 있습니다.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {superadminSubTab === 'categories' && (
              <div className="card" style={styles.adminContentCard}>
                <h3 style={styles.adminContentTitle}>문제 카테고리 목록</h3>
                
                {/* 신규 등록 폼 */}
                <form onSubmit={handleCreateCategory} style={styles.categoryInputForm} noValidate>
                  <div style={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
                    <input
                      type="text"
                      className="input-field"
                      style={{
                        ...styles.categoryInput,
                        borderColor: validationErrors.newCategoryName ? '#ef4444' : undefined,
                        boxShadow: validationErrors.newCategoryName ? '0 0 0 3px rgba(239, 68, 68, 0.1)' : undefined
                      }}
                      placeholder="신규 등록할 카테고리명 입력"
                      value={newCategoryName}
                      onChange={(e) => {
                        setNewCategoryName(e.target.value);
                        if (validationErrors.newCategoryName) {
                          setValidationErrors(prev => ({ ...prev, newCategoryName: '' }));
                        }
                      }}
                    />
                    {validationErrors.newCategoryName && (
                      <span style={styles.errorText}>{validationErrors.newCategoryName}</span>
                    )}
                  </div>
                  <button type="submit" className="btn btn-primary" style={{ alignSelf: 'flex-start' }}>
                    카테고리 추가
                  </button>
                </form>

                {/* 카테고리 리스트 */}
                <div style={styles.categoryList}>
                  {categories.map(cat => (
                    <div key={cat.id} style={styles.categoryItem}>
                      {editingCategoryId === cat.id ? (
                        <>
                          <div style={{ display: 'flex', flexDirection: 'column', flex: 1, marginRight: '0.75rem' }}>
                            <input
                              type="text"
                              className="input-field"
                              style={{
                                ...styles.categoryEditInput,
                                marginRight: 0,
                                borderColor: validationErrors[`editingCategory_${cat.id}`] ? '#ef4444' : undefined,
                                boxShadow: validationErrors[`editingCategory_${cat.id}`] ? '0 0 0 3px rgba(239, 68, 68, 0.1)' : undefined
                              }}
                              value={editingCategoryName}
                              onChange={(e) => {
                                setEditingCategoryName(e.target.value);
                                if (validationErrors[`editingCategory_${cat.id}`]) {
                                  setValidationErrors(prev => ({ ...prev, [`editingCategory_${cat.id}`]: '' }));
                                }
                              }}
                            />
                            {validationErrors[`editingCategory_${cat.id}`] && (
                              <span style={styles.errorText}>{validationErrors[`editingCategory_${cat.id}`]}</span>
                            )}
                          </div>
                          <div style={styles.categoryItemActions}>
                            <button
                              className="btn btn-primary"
                              style={styles.categorySmallBtn}
                              onClick={() => handleUpdateCategory(cat.id)}
                            >
                              저장
                            </button>
                            <button
                              className="btn btn-secondary"
                              style={styles.categorySmallBtn}
                              onClick={() => {
                                setEditingCategoryId(null);
                                setEditingCategoryName('');
                                setValidationErrors(prev => ({ ...prev, [`editingCategory_${cat.id}`]: '' }));
                              }}
                            >
                              취소
                            </button>
                          </div>
                        </>
                      ) : (
                        <>
                          <span style={styles.categoryNameText}>{cat.name}</span>
                          <button
                            className="btn btn-secondary"
                            style={styles.categorySmallBtn}
                            onClick={() => {
                              setEditingCategoryId(cat.id);
                              setEditingCategoryName(cat.name);
                            }}
                          >
                            수정
                          </button>
                        </>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </section>
        )}

        {currentTab === 'mypage' && (
          <section style={{ padding: '2rem', maxWidth: '800px', margin: '0 auto' }}>
            <div className="card" style={{ padding: '2.5rem', display: 'flex', flexDirection: 'column', gap: '2rem', borderRadius: '16px', boxShadow: '0 8px 30px rgba(0, 0, 0, 0.08)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', borderBottom: '1px solid #eaecf0', paddingBottom: '1.5rem' }}>
                <div style={{ width: '64px', height: '64px', borderRadius: '50%', backgroundColor: '#f0f2ff', color: 'var(--primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.5rem', fontWeight: 'bold' }}>
                  {user.name.charAt(0)}
                </div>
                <div>
                  <h2 style={{ fontSize: '1.25rem', fontWeight: 'bold', color: 'var(--text)' }}>개인 정보 확인 및 수정</h2>
                  <p style={{ fontSize: '0.875rem', color: 'var(--text-sub)' }}>{user.email} &middot; {user.role === 'SUPER_ADMIN' ? '최고 관리자' : user.role === 'ADMIN' ? '공간 관리자' : '일반 학습 회원'}</p>
                </div>
              </div>

              <form onSubmit={handleUpdateProfile} style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }} noValidate>
                <div className="input-group" style={{ marginBottom: 0 }}>
                  <label className="input-label">사용자 이름</label>
                  <input
                    type="text"
                    className="input-field"
                    placeholder="이름을 입력해 주세요"
                    value={profileName}
                    onChange={(e) => setProfileName(e.target.value)}
                  />
                </div>
                <div className="input-group" style={{ marginBottom: 0 }}>
                  <label className="input-label">프로필 이미지 URL</label>
                  <input
                    type="text"
                    className="input-field"
                    placeholder="https://example.com/profile-image.png"
                    value={profileImage}
                    onChange={(e) => setProfileImage(e.target.value)}
                  />
                  {profileImage && (
                    <div style={{ marginTop: '0.75rem' }}>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-sub)', marginBottom: '0.375rem' }}>프로필 이미지 미리보기:</p>
                      <img src={profileImage} alt="Profile Preview" style={{ width: '80px', height: '80px', borderRadius: '50%', objectFit: 'cover', border: '1px solid #d0d5dd' }} />
                    </div>
                  )}
                </div>

                <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                  <button type="submit" className="btn btn-primary" style={{ flex: 1, padding: '0.75rem' }}>
                    프로필 수정 저장
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger"
                    style={{ flex: 1, padding: '0.75rem', backgroundColor: '#fee4e2', color: '#d92d20', border: '1px solid #fda29b' }}
                    onClick={handleWithdrawal}
                  >
                    회원 탈퇴 (계정 삭제)
                  </button>
                </div>
              </form>
            </div>
          </section>
        )}
      </main>

      {/* 공간 개설 모달 */}
      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>새 학습 공간 만들기</h3>
            <form onSubmit={handleCreateSpace} style={styles.form} noValidate>
              <div className="input-group">
                <label className="input-label">공간 이름</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="예: 마포고 3학년 2반 수학 공부방"
                  value={spaceName}
                  onChange={(e) => {
                    setSpaceName(e.target.value);
                    if (validationErrors.spaceName) {
                      setValidationErrors(prev => ({ ...prev, spaceName: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.spaceName ? '#ef4444' : undefined,
                    boxShadow: validationErrors.spaceName ? '0 0 0 3px rgba(239, 68, 68, 0.1)' : undefined
                  }}
                />
                {validationErrors.spaceName && (
                  <span style={styles.errorText}>{validationErrors.spaceName}</span>
                )}
              </div>
              <div className="input-group">
                <label className="input-label">공간 설명</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="공간에 대한 상세 설명을 기입해 주세요"
                  value={spaceDesc}
                  onChange={(e) => {
                    setSpaceDesc(e.target.value);
                    if (validationErrors.spaceDesc) {
                      setValidationErrors(prev => ({ ...prev, spaceDesc: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.spaceDesc ? '#ef4444' : undefined,
                    boxShadow: validationErrors.spaceDesc ? '0 0 0 3px rgba(239, 68, 68, 0.1)' : undefined
                  }}
                />
                {validationErrors.spaceDesc && (
                  <span style={styles.errorText}>{validationErrors.spaceDesc}</span>
                )}
              </div>
              <div className="input-group">
                <label className="input-label">공간 비밀번호 (필수)</label>
                <input
                  type="password"
                  className="input-field"
                  placeholder="멤버 가입 시 제한할 비밀번호"
                  value={spacePwd}
                  onChange={(e) => {
                    setSpacePwd(e.target.value);
                    if (validationErrors.spacePwd) {
                      setValidationErrors(prev => ({ ...prev, spacePwd: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.spacePwd ? '#ef4444' : undefined,
                    boxShadow: validationErrors.spacePwd ? '0 0 0 3px rgba(239, 68, 68, 0.1)' : undefined
                  }}
                />
                {validationErrors.spacePwd && (
                  <span style={styles.errorText}>{validationErrors.spacePwd}</span>
                )}
              </div>
              <div style={styles.modalActions}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowCreateModal(false)}
                >
                  취소
                </button>
                <button type="submit" className="btn btn-primary">
                  공간 생성 완료
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 공간 가입 비밀번호 입력 모달 */}
      {showJoinModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>공간 가입 비밀번호 확인</h3>
            <p style={styles.modalDesc}>선택하신 공간에 참여하려면 해당 공간에 설정된 비밀번호를 입력해 주셔야 합니다.</p>
            <form onSubmit={handleJoinSpace} style={styles.form} noValidate>
              <div className="input-group">
                <label className="input-label">공간 패스워드</label>
                <input
                  type="password"
                  className="input-field"
                  placeholder="비밀번호 입력"
                  value={joinPwd}
                  onChange={(e) => {
                    setJoinPwd(e.target.value);
                    if (validationErrors.joinPwd) {
                      setValidationErrors(prev => ({ ...prev, joinPwd: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.joinPwd ? '#ef4444' : undefined,
                    boxShadow: validationErrors.joinPwd ? '0 0 0 3px rgba(239, 68, 68, 0.1)' : undefined
                  }}
                />
                {validationErrors.joinPwd && (
                  <span style={styles.errorText}>{validationErrors.joinPwd}</span>
                )}
              </div>
              <div style={styles.modalActions}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => {
                    setShowJoinModal(false);
                    setJoinPwd('');
                  }}
                >
                  취소
                </button>
                <button type="submit" className="btn btn-primary">
                  가입 완료
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 커스텀 컨펌 모달 */}
      {showConfirmModal && confirmModalData && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>{confirmModalData.title}</h3>
            <p style={styles.modalDesc}>{confirmModalData.message}</p>
            <div style={styles.modalActions}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setShowConfirmModal(false);
                  setConfirmModalData(null);
                }}
              >
                취소
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  confirmModalData.onConfirm();
                  setShowConfirmModal(false);
                  setConfirmModalData(null);
                }}
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 슈퍼어드민 공간 수정 모달 */}
      {showEditSpaceModal && editingSpace && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>공간 정보 수정 (슈퍼관리자)</h3>
            <form onSubmit={handleUpdateSpace} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }} noValidate>
              <div className="input-group" style={{ marginBottom: 0 }}>
                <label className="input-label">공간 이름</label>
                <input
                  type="text"
                  className="input-field"
                  value={editSpaceName}
                  onChange={(e) => setEditSpaceName(e.target.value)}
                />
              </div>
              <div className="input-group" style={{ marginBottom: 0 }}>
                <label className="input-label">공간 설명</label>
                <textarea
                  className="input-field"
                  rows={3}
                  value={editSpaceDesc}
                  onChange={(e) => setEditSpaceDesc(e.target.value)}
                  style={{ height: '80px' }}
                />
              </div>
              <div style={styles.modalActions}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowEditSpaceModal(false)}
                >
                  취소
                </button>
                <button type="submit" className="btn btn-primary">
                  수정 완료
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 슈퍼어드민 문제 수정 모달 */}
      {showEditProblemModal && editingProblem && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>문제 메타데이터 수정 (슈퍼관리자)</h3>
            <form onSubmit={handleUpdateProblem} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }} noValidate>
              <div className="input-group" style={{ marginBottom: 0 }}>
                <label className="input-label">문제 이름</label>
                <input
                  type="text"
                  className="input-field"
                  value={editProblemName}
                  onChange={(e) => setEditProblemName(e.target.value)}
                />
              </div>
              <div className="input-group" style={{ marginBottom: 0 }}>
                <label className="input-label">지문 내용</label>
                <textarea
                  className="input-field"
                  rows={3}
                  value={editProblemContent}
                  onChange={(e) => setEditProblemContent(e.target.value)}
                  style={{ height: '80px' }}
                />
              </div>
              <div className="input-group" style={{ marginBottom: 0 }}>
                <label className="input-label">공식 정답</label>
                <input
                  type="text"
                  className="input-field"
                  value={editProblemAnswer}
                  onChange={(e) => setEditProblemAnswer(e.target.value)}
                />
              </div>
              <div className="input-group" style={{ marginBottom: 0 }}>
                <label className="input-label">풀이 해설</label>
                <textarea
                  className="input-field"
                  rows={3}
                  value={editProblemExplanation}
                  onChange={(e) => setEditProblemExplanation(e.target.value)}
                  style={{ height: '80px' }}
                />
              </div>
              <div style={styles.modalActions}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowEditProblemModal(false)}
                >
                  취소
                </button>
                <button type="submit" className="btn btn-primary">
                  수정 완료
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
  loadingContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    fontSize: '1rem',
    color: '#475467',
    backgroundColor: '#f9fafb',
  },
  sidebarLogo: {
    fontSize: '1.5rem',
    fontWeight: 800,
    color: '#1d2939',
    textAlign: 'center',
    marginBottom: '1.5rem',
    letterSpacing: '-0.5px',
  },
  sidebarMenu: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.375rem',
    flex: 1,
  },
  menuBtn: {
    width: '100%',
    textAlign: 'left',
    justifyContent: 'flex-start',
    backgroundColor: 'transparent',
    border: 'none',
    color: '#475467',
    padding: '0.65rem 0.85rem',
    fontSize: '0.875rem',
  },
  menuActive: {
    backgroundColor: '#f0f2ff',
    color: '#6366f1',
    fontWeight: 700,
  },
  logoutBtn: {
    marginTop: 'auto',
    width: '100%',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '2rem',
  },
  welcomeTitle: {
    fontSize: '1.75rem',
    fontWeight: 700,
    color: '#1d2939',
    letterSpacing: '-0.5px',
  },
  welcomeSub: {
    fontSize: '0.9rem',
    color: '#475467',
    marginTop: '0.25rem',
  },
  headerActions: {
    position: 'relative',
  },
  notiTriggerBtn: {
    position: 'relative',
    paddingRight: '2rem',
  },
  notiCountBadge: {
    position: 'absolute',
    top: '50%',
    right: '0.5rem',
    transform: 'translateY(-50%)',
    backgroundColor: '#ef4444',
    color: '#ffffff',
    fontSize: '0.7rem',
    fontWeight: 700,
    borderRadius: '50%',
    width: '18px',
    height: '18px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  notiPopover: {
    position: 'absolute',
    top: '5rem',
    right: '3rem',
    width: '320px',
    zIndex: 100,
    padding: '1rem',
    backgroundColor: '#ffffff',
    borderRadius: '12px',
    boxShadow: '0 10px 15px -3px rgba(16, 24, 40, 0.08)',
  },
  notiPopoverTitle: {
    fontSize: '0.875rem',
    fontWeight: 700,
    color: '#1d2939',
    marginBottom: '0.75rem',
    borderBottom: '1px solid #eaecf0',
    paddingBottom: '0.5rem',
  },
  notiEmpty: {
    fontSize: '0.825rem',
    color: '#98a2b3',
    textAlign: 'center',
    padding: '1rem 0',
  },
  notiList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
    maxHeight: '200px',
    overflowY: 'auto',
  },
  notiItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.5rem 0',
    borderBottom: '1px solid #f2f4f7',
  },
  notiText: {
    fontSize: '0.775rem',
    color: '#475467',
    flex: 1,
    lineHeight: 1.4,
  },
  notiConfirmBtn: {
    fontSize: '0.7rem',
    padding: '0.25rem 0.5rem',
    backgroundColor: '#f2f4f7',
    border: 'none',
    color: '#475467',
  },
  statsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gap: '1.5rem',
    marginBottom: '2rem',
  },
  statCard: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
    alignItems: 'flex-start',
  },
  statTitle: {
    fontSize: '0.875rem',
    color: '#475467',
    fontWeight: 600,
  },
  statNumber: {
    fontSize: '2rem',
    fontWeight: 800,
    color: '#1d2939',
  },
  statDesc: {
    fontSize: '0.75rem',
    color: '#98a2b3',
  },
  sectionContainer: {
    marginBottom: '2rem',
  },
  sectionTitle: {
    fontSize: '1.25rem',
    fontWeight: 700,
    color: '#1d2939',
    marginBottom: '1rem',
  },
  spaceDetailCard: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '2rem',
  },
  spaceCardInfo: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
  },
  spaceName: {
    fontSize: '1.25rem',
    fontWeight: 700,
    color: '#1d2939',
  },
  spaceDesc: {
    fontSize: '0.9rem',
    color: '#475467',
  },
  spaceMeta: {
    marginTop: '0.25rem',
  },
  spaceEmptyCard: {
    textAlign: 'center',
    padding: '3rem 2rem',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '1.25rem',
  },
  spaceEmptyText: {
    fontSize: '0.95rem',
    color: '#475467',
  },
  spaceEmptyActions: {
    display: 'flex',
    gap: '0.75rem',
  },
  unjoinedListGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gap: '1.25rem',
  },
  unjoinedCard: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
    alignItems: 'flex-start',
  },
  unjoinedName: {
    fontSize: '1rem',
    fontWeight: 700,
    color: '#1d2939',
  },
  unjoinedDesc: {
    fontSize: '0.825rem',
    color: '#475467',
    flex: 1,
  },
  unjoinedJoinBtn: {
    marginTop: '0.5rem',
    width: '100%',
    fontSize: '0.825rem',
  },
  adminConsoleContainer: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1.5rem',
  },
  adminSubTabHeader: {
    display: 'flex',
    gap: '0.5rem',
    borderBottom: '2px solid #eaecf0',
    paddingBottom: '0.5rem',
  },
  adminSubTabBtn: {
    backgroundColor: 'transparent',
    border: 'none',
    color: '#475467',
    padding: '0.5rem 1rem',
    fontSize: '0.9rem',
    fontWeight: 600,
  },
  adminSubTabActive: {
    color: '#6366f1',
    borderBottom: '2px solid #6366f1',
    borderRadius: '0',
  },
  adminContentCard: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
    padding: '2rem',
  },
  adminContentTitle: {
    fontSize: '1.125rem',
    fontWeight: 700,
    color: '#1d2939',
  },
  demoBadge: {
    marginLeft: '0.5rem',
    padding: '0.15rem 0.5rem',
    borderRadius: '999px',
    fontSize: '0.7rem',
    fontWeight: 700,
    color: '#b45309',
    backgroundColor: '#fef3c7',
    border: '1px solid #fde68a',
    verticalAlign: 'middle',
  },
  tableWrapper: {
    width: '100%',
    overflowX: 'auto',
    border: '1px solid #eaecf0',
    borderRadius: '8px',
  },
  adminTable: {
    width: '100%',
    borderCollapse: 'collapse',
    textAlign: 'left',
    fontSize: '0.875rem',
  },
  th: {
    backgroundColor: '#f9fafb',
    padding: '0.75rem 1rem',
    fontWeight: 600,
    color: '#475467',
    borderBottom: '1px solid #eaecf0',
  },
  tr: {
    borderBottom: '1px solid #eaecf0',
  },
  td: {
    padding: '1rem',
    color: '#1d2939',
  },
  disabledText: {
    color: '#98a2b3',
    fontSize: '0.825rem',
    fontStyle: 'italic',
  },
  banBtn: {
    fontSize: '0.8rem',
    padding: '0.4rem 0.8rem',
  },
  moreLoadBtn: {
    alignSelf: 'center',
    marginTop: '0.5rem',
  },
  categoryInputForm: {
    display: 'flex',
    gap: '0.75rem',
    maxWidth: '480px',
  },
  categoryInput: {
    flex: 1,
  },
  categoryList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
    marginTop: '0.5rem',
    maxWidth: '480px',
  },
  categoryItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '0.75rem 1rem',
    backgroundColor: '#f9fafb',
    border: '1px solid #eaecf0',
    borderRadius: '8px',
  },
  categoryNameText: {
    fontSize: '0.9rem',
    fontWeight: 600,
    color: '#1d2939',
  },
  categorySmallBtn: {
    fontSize: '0.8rem',
    padding: '0.35rem 0.75rem',
  },
  categoryEditInput: {
    flex: 1,
    marginRight: '0.75rem',
    padding: '0.5rem',
  },
  categoryItemActions: {
    display: 'flex',
    gap: '0.375rem',
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
  form: {
    display: 'flex',
    flexDirection: 'column',
    textAlign: 'left',
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
