import React, { useEffect, useState, useRef, useCallback } from 'react';
import type { UserResponse, UserCursorResponse, UserSortByType, SortDirectionType } from '../types/user';
import { PASSWORD_REGEX, PASSWORD_INVALID_MESSAGE } from '../types/user';
import type { SpaceResponse, SpaceCreateRequest } from '../types/space';
import { request, getAccessToken, connectNotificationSse } from '../services/api';
import { logout } from '../services/auth';

interface DashboardPageProps {
  user: UserResponse;
  initialTab?: 'dashboard' | 'superadmin' | 'mypage';
  onLogout: () => void;
  showToast: (message: string, type?: 'success' | 'error' | 'info') => void;
  onEnterSpace: (space: SpaceResponse) => void;
  onUserUpdate: (user: UserResponse) => void;
}

const DEFAULT_AVATAR = '/basic.png';

// 프로필 이미지 로드 실패(마이그레이션 이전 로컬 경로 등) 시 깨진 이미지 아이콘 대신 기본 아바타로 대체
const handleAvatarError = (e: React.SyntheticEvent<HTMLImageElement>) => {
  e.currentTarget.onerror = null;
  e.currentTarget.src = DEFAULT_AVATAR;
};

interface NotificationItem {
  id: string;
  title: string;
  content: string;
  type: string;
  isConfirmed: boolean;
  createdAt: string;
}

interface CategoryResponse {
  id: string;
  name: string;
}

interface DashboardSummaryResponse {
  todaySolvedCount: number;
  weeklyAccuracyRate: number;
  completedExamCount: number;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({ user, initialTab, onLogout, showToast, onEnterSpace, onUserUpdate }) => {
  // 탭 관련 상태 ('dashboard' | 'superadmin' | 'mypage')
  const [currentTab, setCurrentTab] = useState<'dashboard' | 'superadmin' | 'mypage'>(initialTab || 'dashboard');

  useEffect(() => {
    if (initialTab) {
      setCurrentTab(initialTab);
    }
  }, [initialTab]);

  // 슈퍼관리자 서브 탭 관련 상태 ('users' | 'spaces' | 'problems' | 'categories')
  const [superadminSubTab, setSuperadminSubTab] = useState<'users' | 'spaces' | 'problems' | 'categories'>('users');

  const [space, setSpace] = useState<SpaceResponse | null>(null);
  const [dashboardSummary, setDashboardSummary] = useState<DashboardSummaryResponse | null>(null);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
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
  const [userNextIdAfter, setUserNextIdAfter] = useState<string | null>(null);
  const [userHasNext, setUserHasNext] = useState(false);
  const [userTotalCount, setUserTotalCount] = useState<number | null>(null);
  const [overallUserTotalCount, setOverallUserTotalCount] = useState<number | null>(null);
  const [userLoadingMore, setUserLoadingMore] = useState(false);
  const userSentinelRef = useRef<HTMLDivElement | null>(null);

  const userCursorRef = useRef<string | null>(null);
  const userNextIdAfterRef = useRef<string | null>(null);
  const userHasNextRef = useRef<boolean>(false);
  const userLoadingMoreRef = useRef<boolean>(false);

  // 슈퍼관리자 유저 필터링 상태
  const [userSearchType, setUserSearchType] = useState<'name' | 'email' | 'role' | 'space'>('name');
  const [userSearchKeyword, setUserSearchKeyword] = useState('');
  const [userRoleFilter, setUserRoleFilter] = useState<'ALL' | 'USER' | 'ADMIN' | 'SUPER_ADMIN'>('ALL');
  const [userSortBy, setUserSortBy] = useState<UserSortByType>('createdAt');
  const [userSortDirection, setUserSortDirection] = useState<SortDirectionType>('DESCENDING');

  const userSortByRef = useRef<UserSortByType>('createdAt');
  const userSortDirectionRef = useRef<SortDirectionType>('DESCENDING');
  const userSearchTypeRef = useRef<'name' | 'email' | 'role' | 'space'>('name');
  const userSearchKeywordRef = useRef<string>('');
  const userRoleFilterRef = useRef<'ALL' | 'USER' | 'ADMIN' | 'SUPER_ADMIN'>('ALL');

  userCursorRef.current = userCursor;
  userNextIdAfterRef.current = userNextIdAfter;
  userHasNextRef.current = userHasNext;
  userLoadingMoreRef.current = userLoadingMore;
  userSortByRef.current = userSortBy;
  userSortDirectionRef.current = userSortDirection;
  userSearchTypeRef.current = userSearchType;
  userSearchKeywordRef.current = userSearchKeyword;
  userRoleFilterRef.current = userRoleFilter;

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
  const [profileFile, setProfileFile] = useState<File | null>(null);
  const [profilePreview, setProfilePreview] = useState('');
  // 실제로 저장 완료된 프로필 이미지의 프리뷰 상태 (제출 완료 전에는 상단에 변경 사항이 보이지 않게 함)
  const [savedProfilePreview, setSavedProfilePreview] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [removeProfileImage, setRemoveProfileImage] = useState<boolean>(false);

  // 프로필 이미지가 원격 URL일 경우, 인증 헤더를 추가해서 Blob으로 가져와 로컬 Object URL로 반환합니다.
  const loadAuthorizedImage = async (url: string) => {
    if (!url) {
      setProfilePreview('');
      setSavedProfilePreview('');
      return;
    }

    // 동일 출처(window.location.origin) 혹은 허용된 로컬 백엔드 출처(http://localhost:8080)이면서
    // /uploads/ 경로가 포함된 요청에만 인증 정보를 첨부하도록 검사합니다. (토큰 유출 방지)
    const isTargetSecure = (() => {
      try {
        if (url.startsWith('blob:')) return false;

        // 상대 경로인 경우 안전함
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
          return url.includes('/uploads/');
        }

        const parsedUrl = new URL(url);
        const isAllowedOrigin =
          parsedUrl.origin === window.location.origin ||
          parsedUrl.origin === 'http://localhost:8080';

        return isAllowedOrigin && parsedUrl.pathname.includes('/uploads/');
      } catch {
        return false;
      }
    })();

    if (!isTargetSecure) {
      setProfilePreview(url);
      setSavedProfilePreview(url);
      return;
    }

    try {
      const token = getAccessToken();
      const headers: Record<string, string> = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const response = await fetch(url, { headers, credentials: 'include' });
      if (response.ok) {
        const blob = await response.blob();
        const blobUrl = URL.createObjectURL(blob);
        setProfilePreview(blobUrl);
        setSavedProfilePreview(blobUrl);
      } else {
        setProfilePreview('');
        setSavedProfilePreview('');
      }
    } catch (e) {
      console.error('Failed to load authorized image:', e);
      setProfilePreview('');
      setSavedProfilePreview('');
    }
  };

  // 프로필 이미지 삭제 처리 함수 (명시적 삭제 플래그 사용)
  const handleDeleteProfileImage = () => {
    setProfileFile(null);
    setRemoveProfileImage(true);
    setProfilePreview(''); // 편집 미리보기 비우기 (기본 아바타 노출)

    // 파일 input 엘리먼트 값도 리셋
    const fileInput = document.getElementById('profile-file-input') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  };

  useEffect(() => {
    setProfileName(user.name);
    setProfileFile(null);
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setRemoveProfileImage(false);
    if (user.profileImageUrl) {
      loadAuthorizedImage(user.profileImageUrl);
    } else {
      setProfilePreview('');
      setSavedProfilePreview('');
    }
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

  // 백엔드가 Page 응답(기본 size=10)을 주기 때문에, 관리자 콘솔의 "전체 목록" 요건을 맞추려면
  // last=true가 될 때까지 모든 페이지를 순회하여 합쳐야 한다.
  const loadAllPages = async (path: string): Promise<any[]> => {
    let page = 0;
    let all: any[] = [];
    while (true) {
      const data = await request<{ content: any[]; last: boolean }>(path, {
        method: 'GET',
        params: { page: String(page), size: '100' }
      });
      if (!data) break;
      all = all.concat(data.content);
      if (data.last) break;
      page += 1;
    }
    return all;
  };

  const loadSuperAdminSpaces = async () => {
    try {
      setSuperSpaces(await loadAllPages('/api/super-admin/spaces'));
    } catch (err: any) {
      showToast(err.message || '공간 목록 로드 실패', 'error');
    }
  };

  const loadSuperAdminProblems = async () => {
    try {
      setSuperProblems(await loadAllPages('/api/super-admin/problems'));
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

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setProfileFile(file);
      setProfilePreview(URL.createObjectURL(file));
      setRemoveProfileImage(false);
    }
  };

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!profileName.trim()) {
      showToast('이름을 입력해 주세요.', 'error');
      return;
    }

    if (newPassword) {
      if (!currentPassword) {
        showToast('비밀번호를 변경하려면 현재 비밀번호를 입력해 주세요.', 'error');
        return;
      }
      if (newPassword !== confirmPassword) {
        showToast('새 비밀번호와 비밀번호 확인이 일치하지 않습니다.', 'error');
        return;
      }
      if (!PASSWORD_REGEX.test(newPassword)) {
        showToast(PASSWORD_INVALID_MESSAGE, 'error');
        return;
      }
    }

    try {
      const formData = new FormData();

      const updateData = {
        name: profileName,
        currentPassword: currentPassword || null,
        newPassword: newPassword || null,
        removeProfileImage: removeProfileImage
      };

      const jsonBlob = new Blob([JSON.stringify(updateData)], { type: 'application/json' });
      formData.append('data', jsonBlob);

      if (profileFile) {
        formData.append('file', profileFile);
      }

      const updatedUser = await request<any>('/api/users/me', {
        method: 'PATCH',
        body: formData,
      });

      showToast('개인 정보가 수정되었습니다.', 'success');
      setRemoveProfileImage(false);

      // 부모 상태 동기화 및 폼 초기화
      if (onUserUpdate) {
        onUserUpdate(updatedUser);
      }

      if (newPassword) {
        showToast('비밀번호가 변경되어 다시 로그인해 주세요.', 'info');
        setTimeout(() => {
          onLogout();
        }, 1500);
      }
    } catch (err: any) {
      showToast(err.message || '개인 정보 수정에 실패했습니다.', 'error');
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

  const handleCancel = () => {
    // 폼 입력 및 파일 프리뷰 상태 초기화
    setProfileName(user.name);
    setProfileFile(null);
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setRemoveProfileImage(false);
    if (user.profileImageUrl) {
      loadAuthorizedImage(user.profileImageUrl);
    } else {
      setProfilePreview('');
      setSavedProfilePreview('');
    }
    // 대시보드 홈 탭으로 복귀
    setCurrentTab('dashboard');
  };

  useEffect(() => {
    loadInitialData();
  }, []);

  // 알림 실시간 수신(SSE) 연결
  // SSE 연결이 확립된 뒤에 초기 목록(GET)을 조회해야, "목록 조회~SSE 연결" 사이에 발생한
  // 알림을 놓치지 않는다. 재연결 시에도 다시 확립 시점 기준으로 목록을 새로 맞춘다.
  useEffect(() => {
    const loadNotifications = async () => {
      try {
        const notiData = await request<any>('/api/notifications', { method: 'GET' });
        // GET은 확인 완료된 알림까지 전부 내려주므로, 미확인 알림만 목록에 남긴다.
        setNotifications((notiData?.data || []).filter((n: NotificationItem) => !n.isConfirmed));
      } catch (err) {
        console.error('Failed to load notifications', err);
      }
    };

    const disconnect = connectNotificationSse({
      onConnect: () => {
        loadNotifications();
      },
      onNotification: (noti: NotificationItem) => {
        setNotifications(prev => (prev.some(n => n.id === noti.id) ? prev : [noti, ...prev]));
      },
      onError: (err) => {
        console.error('알림 SSE 연결 오류:', err);
      },
    });
    return () => disconnect();
  }, []);

  const loadInitialData = async () => {
    setLoading(true);
    try {
      // 1. 소속 공간 조회
      const spaceData = await request<SpaceResponse | null>('/api/spaces/me', { method: 'GET' });
      setSpace(spaceData);

      // 1-1. 메인 대시보드 요약 카드(싱글모드/종합성과/참여시험) 조회
      try {
        const summaryData = await request<DashboardSummaryResponse>('/api/dashboards/summary', { method: 'GET' });
        setDashboardSummary(summaryData);
      } catch (err) {
        console.error('대시보드 요약 조회 실패', err);
      }

      // 2. 미가입 공간 조회
      const unjoinedData = await request<{ values: SpaceResponse[] }>('/api/spaces/unjoined', { method: 'GET' });
      if (unjoinedData && unjoinedData.values) {
        setUnjoinedSpaces(unjoinedData.values);
      }

      // 3. 슈퍼관리자 권한인 경우 관련 데이터 미리 조회
      if (user.role === 'SUPER_ADMIN') {
        await loadSuperAdminUsers(null, null);
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

  // 슈퍼관리자 유저 목록 페이징 및 필터 조회
  const loadSuperAdminUsers = async (
    cursorVal: string | null = null,
    idAfterVal: string | null = null,
    overrideFilters?: {
      searchType?: 'name' | 'email' | 'role' | 'space';
      searchKeyword?: string;
      role?: 'ALL' | 'USER' | 'ADMIN' | 'SUPER_ADMIN';
      sortBy?: UserSortByType;
      sortDirection?: SortDirectionType;
    }
  ) => {
    if (cursorVal && userLoadingMore) return;
    try {
      if (!cursorVal) {
        userCursorRef.current = null;
        userNextIdAfterRef.current = null;
        userHasNextRef.current = false;
        setUserCursor(null);
        setUserNextIdAfter(null);
        setUserHasNext(false);
        setAllUsers([]);
      } else {
        setUserLoadingMore(true);
      }

      const filters = {
        searchType: overrideFilters?.searchType || userSearchTypeRef.current,
        searchKeyword: overrideFilters?.searchKeyword !== undefined ? overrideFilters.searchKeyword : userSearchKeywordRef.current,
        role: overrideFilters?.role || userRoleFilterRef.current,
        sortBy: overrideFilters?.sortBy || userSortByRef.current,
        sortDirection: overrideFilters?.sortDirection || userSortDirectionRef.current,
      };

      const params: Record<string, string> = {
        limit: '20',
        sortBy: filters.sortBy,
        sortDirection: filters.sortDirection,
      };

      if (filters.searchType === 'role') {
        if (filters.role && filters.role !== 'ALL') {
          params.role = filters.role;
        }
      } else {
        const trimmedKeyword = filters.searchKeyword.trim();
        if (trimmedKeyword) {
          if (filters.searchType === 'name') {
            params.nameLike = trimmedKeyword;
          } else if (filters.searchType === 'email') {
            params.emailLike = trimmedKeyword;
          } else if (filters.searchType === 'space') {
            params.spaceNameLike = trimmedKeyword;
          }
        }
      }

      if (cursorVal && idAfterVal) {
        params.cursor = cursorVal;
        params.idAfter = idAfterVal;
      }

      const data = await request<UserCursorResponse<UserResponse>>('/api/super-admin/users', {
        method: 'GET',
        params,
      });

      const list: UserResponse[] = data?.data || (Array.isArray(data) ? data : []);

      if (cursorVal) {
        setAllUsers(prev => {
          const existingIds = new Set(prev.map(u => u.id));
          const newItems = list.filter(u => !existingIds.has(u.id));
          return [...prev, ...newItems];
        });
      } else {
        setAllUsers(list);
        if (typeof data?.totalCount === 'number' && data.totalCount >= 0) {
          setUserTotalCount(data.totalCount);
          const isUnfiltered = !filters.searchKeyword.trim() && filters.role === 'ALL';
          if (isUnfiltered || overallUserTotalCount === null) {
            setOverallUserTotalCount(data.totalCount);
          }
        }
      }

      setUserCursor(data?.nextCursor || null);
      setUserNextIdAfter(data?.nextIdAfter || null);
      setUserHasNext(!!data?.hasNext);
    } catch (err: any) {
      console.error('유저 목록 조회 실패:', err.message);
    } finally {
      setUserLoadingMore(false);
    }
  };

  // 슈퍼관리자 다음 유저 목록 로드 함수
  const loadNextUserPage = useCallback(() => {
    if (
      userHasNextRef.current &&
      !userLoadingMoreRef.current &&
      userCursorRef.current &&
      userNextIdAfterRef.current
    ) {
      loadSuperAdminUsers(userCursorRef.current, userNextIdAfterRef.current);
    }
  }, []);

  // 슈퍼관리자 유저 자동 무한 스크롤 (IntersectionObserver + Capture Scroll + Viewport Check)
  useEffect(() => {
    const sentinel = userSentinelRef.current;

    const checkSentinelInView = () => {
      if (!userSentinelRef.current || !userHasNextRef.current || userLoadingMoreRef.current) return;
      const rect = userSentinelRef.current.getBoundingClientRect();
      if (rect.top <= window.innerHeight + 400 && rect.bottom >= 0) {
        loadNextUserPage();
      }
    };

    let observer: IntersectionObserver | null = null;
    if (sentinel) {
      observer = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting) {
            checkSentinelInView();
          }
        },
        { rootMargin: '400px' }
      );
      observer.observe(sentinel);
    }

    window.addEventListener('scroll', checkSentinelInView, true);
    const timer = setTimeout(checkSentinelInView, 150);

    return () => {
      if (observer && sentinel) {
        observer.unobserve(sentinel);
      }
      window.removeEventListener('scroll', checkSentinelInView, true);
      clearTimeout(timer);
    };
  }, [loadNextUserPage, allUsers.length]);

  // 유저 검색 실행 핸들러
  const handleUserSearch = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    userSearchTypeRef.current = userSearchType;
    userSearchKeywordRef.current = userSearchKeyword;
    userRoleFilterRef.current = userRoleFilter;
    loadSuperAdminUsers(null, null, {
      searchType: userSearchType,
      searchKeyword: userSearchKeyword,
      role: userRoleFilter,
    });
  };

  // 유저 필터링 조건 초기화 핸들러
  const handleUserFilterReset = () => {
    setUserSearchType('name');
    setUserSearchKeyword('');
    setUserRoleFilter('ALL');
    setUserSortBy('createdAt');
    setUserSortDirection('DESCENDING');
    userSearchTypeRef.current = 'name';
    userSearchKeywordRef.current = '';
    userRoleFilterRef.current = 'ALL';
    userSortByRef.current = 'createdAt';
    userSortDirectionRef.current = 'DESCENDING';
    setUserTotalCount(null);
    loadSuperAdminUsers(null, null, {
      searchType: 'name',
      searchKeyword: '',
      role: 'ALL',
      sortBy: 'createdAt',
      sortDirection: 'DESCENDING',
    });
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

  // 유저 밴 처리 연동 (요구사항 7)
  const handleToggleUserBan = (targetUser: UserResponse) => {
    const nextBannedState = !targetUser.isBanned;
    openConfirm(
        '회원 상태 변경',
        `[${targetUser.name}] 님을 ${nextBannedState ? '영구 정지(Ban)' : '정지 해제'} 처리하시겠습니까?`,
        async () => {
          try {
            await request<UserResponse>(`/api/super-admin/users/${targetUser.id}/ban`, {
              method: 'PATCH',
              body: JSON.stringify({ banned: nextBannedState }),
            });

            showToast('회원 정지 상태가 변경되었습니다.', 'success');
            setAllUsers(prev =>
                prev.map(u => (u.id === targetUser.id ? { ...u, isBanned: nextBannedState } : u))
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
      showToast(err.message || '알림 확인 처리에 실패했습니다.', 'error');
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

  const unreadNotiCount = notifications.filter(n => !n.isConfirmed).length;

  if (loading) {
    return <div style={styles.loadingContainer}>MoMoGo 데이터를 불러오는 중입니다...</div>;
  }

  return (
      <div className="app-container">
        {/* 고정 사이드바 */}
        <aside className="sidebar-layout">
          <div 
            style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '0.5rem 0', marginBottom: '1rem', cursor: 'pointer' }}
            onClick={() => setCurrentTab('dashboard')}
          >
            <img src="/MoMoGo_Logo.png" alt="MoMoGo Logo" style={{ height: '36px', width: 'auto', objectFit: 'contain' }} />
            <h2 style={{ ...styles.sidebarLogo, marginBottom: 0 }}>MoMoGo</h2>
          </div>

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
                  onClick={() => { setShowNotifications(!showNotifications); setShowUserMenu(false); }}
              >
                🔔 알림
                {unreadNotiCount > 0 && <span style={styles.notiCountBadge}>{unreadNotiCount}</span>}
              </button>

              {/* 상단 유저 정보 위젯 (요구사항 2, 2-1, 14) */}
              <div style={{ position: 'relative' }}>
                <div 
                  style={styles.userHeaderWidget}
                  onClick={() => { setShowUserMenu(!showUserMenu); setShowNotifications(false); }}
                >
                  <img
                    src={profilePreview || (removeProfileImage ? DEFAULT_AVATAR : savedProfilePreview || user.profileImageUrl || DEFAULT_AVATAR)}
                    alt={user.name}
                    style={styles.headerAvatar}
                    onError={handleAvatarError}
                  />
                  <span style={styles.headerUserName}>{user.name}</span>
                  {user.role && (
                    <span className={`badge ${user.role === 'SUPER_ADMIN' ? 'badge-danger' : user.role === 'ADMIN' ? 'badge-success' : 'badge-info'}`}>
                      {user.role}
                    </span>
                  )}
                </div>

                {showUserMenu && (
                  <div style={styles.userMenuPopover} className="card">
                    <div style={styles.userMenuHeader}>
                      <img
                        src={profilePreview || (removeProfileImage ? DEFAULT_AVATAR : savedProfilePreview || user.profileImageUrl || DEFAULT_AVATAR)}
                        alt={user.name}
                        style={styles.menuAvatar}
                        onError={handleAvatarError}
                      />
                      <div>
                        <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{user.name}</div>
                        <div style={{ fontSize: '0.8rem', color: '#6b7280' }}>{user.email}</div>
                        <span className={`badge ${user.role === 'SUPER_ADMIN' ? 'badge-danger' : user.role === 'ADMIN' ? 'badge-success' : 'badge-info'}`}>
                          {user.role}
                        </span>
                      </div>
                    </div>
                    <hr style={{ margin: '8px 0', border: 'none', borderTop: '1px solid #eaecf0' }} />
                    <button 
                      type="button" 
                      style={styles.popoverItemBtn} 
                      onClick={() => { setCurrentTab('mypage'); setShowUserMenu(false); }}
                    >
                      👤 마이페이지 이동
                    </button>
                    <button 
                      type="button" 
                      style={{ ...styles.popoverItemBtn, color: '#ef4444' }} 
                      onClick={() => { setShowUserMenu(false); handleLogout(); }}
                    >
                      🚪 로그아웃
                    </button>
                  </div>
                )}
              </div>
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
                    <h3 style={styles.statTitle}>오늘 맞춘 퀴즈</h3>
                    <div style={styles.statNumber}>{dashboardSummary?.todaySolvedCount ?? 0}개</div>
                    <p style={styles.statDesc}>오늘 정답 처리된 싱글모드 문제 수입니다.</p>
                  </div>
                  <div className="card" style={styles.statCard}>
                    <span className="badge badge-success">종합 성과</span>
                    <h3 style={styles.statTitle}>이번 주 평균 정답률</h3>
                    <div style={styles.statNumber}>{(dashboardSummary?.weeklyAccuracyRate ?? 0).toFixed(1)}%</div>
                    <p style={styles.statDesc}>이번 주 월요일부터 지금까지의 정답률입니다.</p>
                  </div>
                  <div className="card" style={styles.statCard}>
                    <span className="badge badge-danger">참여 시험</span>
                    <h3 style={styles.statTitle}>참가 완료 시험방</h3>
                    <div style={styles.statNumber}>{dashboardSummary?.completedExamCount ?? 0}개</div>
                    <p style={styles.statDesc}>답안을 제출해 응시를 완료한 시험방 수입니다.</p>
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
                          {user.role !== 'SUPER_ADMIN' && (
                            <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
                              새 학습 공간 만들기
                            </button>
                          )}
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

                {/* 참여 가능한 공간 목록 (나의 소속 공간 하단 노출) (요구사항 6) */}
                {unjoinedSpaces.length > 0 && (
                    <section style={styles.sectionContainer}>
                      <h2 style={styles.sectionTitle}>참여 가능한 공간 목록</h2>
                      <div style={styles.unjoinedListGrid}>
                        {unjoinedSpaces.map(sp => (
                            <div key={sp.id} className="card" style={styles.unjoinedCard}>
                              <h4 style={styles.unjoinedName}>{sp.name}</h4>
                              <p style={styles.unjoinedDesc}>{sp.description}</p>
                              <button
                                  className="btn btn-primary"
                                  style={{ marginTop: '0.75rem', width: '100%', whiteSpace: 'pre-line', lineHeight: 1.3 }}
                                  onClick={() => {
                                    setTargetSpaceId(sp.id);
                                    setShowJoinModal(true);
                                  }}
                              >
                                {"비밀번호 입력 후\n가입하기"}
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
                {/* 슈퍼관리자 전용 대시보드 통계 카드 (요구사항 15) */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
                  <div className="card" style={{ padding: '1.25rem', borderLeft: '4px solid #3b82f6' }}>
                    <div style={{ fontSize: '0.85rem', color: '#6b7280', fontWeight: 600 }}>전체 가입 회원</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#1d2939', marginTop: '0.25rem' }}>
                      {overallUserTotalCount !== null ? `${overallUserTotalCount}명` : userTotalCount !== null ? `${userTotalCount}명` : `${allUsers.length}명`}
                    </div>
                  </div>
                  <div className="card" style={{ padding: '1.25rem', borderLeft: '4px solid #6366f1' }}>
                    <div style={{ fontSize: '0.85rem', color: '#6b7280', fontWeight: 600 }}>전체 생성 공간</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#1d2939', marginTop: '0.25rem' }}>{superSpaces.length}개</div>
                  </div>
                  <div className="card" style={{ padding: '1.25rem', borderLeft: '4px solid #10b981' }}>
                    <div style={{ fontSize: '0.85rem', color: '#6b7280', fontWeight: 600 }}>전체 출제 문제</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#1d2939', marginTop: '0.25rem' }}>{superProblems.length}개</div>
                  </div>
                  <div className="card" style={{ padding: '1.25rem', borderLeft: '4px solid #ef4444' }}>
                    <div style={{ fontSize: '0.85rem', color: '#6b7280', fontWeight: 600 }}>영구 정지 유저</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#ef4444', marginTop: '0.25rem' }}>{allUsers.filter(u => u.isBanned).length}명</div>
                  </div>
                </div>

                {/* 서브 탭 셀렉터 */}
                <div style={styles.adminSubTabHeader}>
                  <button
                      className="btn"
                      style={{
                        ...styles.adminSubTabBtn,
                        ...(superadminSubTab === 'users' ? styles.adminSubTabActive : {}),
                      }}
                      onClick={() => setSuperadminSubTab('users')}
                  >
                    전체 사용자 관리
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
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1rem' }}>
                        <div>
                          <h3 style={{ ...styles.adminContentTitle, margin: 0 }}>전체 사용자 목록</h3>
                        </div>
                        <span className="badge badge-info" style={{ fontSize: '0.85rem', padding: '0.4rem 0.75rem' }}>
                          총 {userTotalCount !== null ? `${userTotalCount}명 검색됨` : `${allUsers.length}명 표시중`}
                        </span>
                      </div>

                      {/* 프리미엄 유저 검색 및 QueryDSL 필터 바 */}
                      <form
                        onSubmit={handleUserSearch}
                        style={{
                          display: 'flex',
                          flexWrap: 'wrap',
                          gap: '0.75rem',
                          alignItems: 'flex-end',
                          backgroundColor: '#ffffff',
                          padding: '1rem 1.25rem',
                          borderRadius: '12px',
                          marginBottom: '1.25rem',
                          border: '1px solid #e2e8f0',
                          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.03)',
                        }}
                      >
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                          <label style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b' }}>검색 조건</label>
                          <select
                            className="input"
                            value={userSearchType}
                            onChange={(e) => {
                              const newType = e.target.value as 'name' | 'email' | 'role';
                              setUserSearchType(newType);
                              if (newType === 'role') {
                                loadSuperAdminUsers(null, null, { searchType: 'role', searchKeyword: '' });
                              }
                            }}
                            style={{
                              width: '115px',
                              height: '38px',
                              padding: '0 1.85rem 0 0.75rem',
                              fontSize: '0.85rem',
                              fontWeight: 500,
                              borderRadius: '8px',
                              border: '1px solid #cbd5e1',
                              backgroundColor: '#ffffff',
                              color: '#1e293b',
                              appearance: 'none',
                              WebkitAppearance: 'none',
                              backgroundImage: `url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e")`,
                              backgroundRepeat: 'no-repeat',
                              backgroundPosition: 'right 0.55rem center',
                              backgroundSize: '1rem',
                              cursor: 'pointer',
                              outline: 'none',
                              boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)',
                            }}
                          >
                            <option value="name">이름</option>
                            <option value="email">이메일</option>
                            <option value="space">소속 공간</option>
                            <option value="role">역할</option>
                          </select>
                        </div>

                        {userSearchType === 'role' ? (
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', flex: '1 1 200px', minWidth: '180px', maxWidth: '280px' }}>
                            <label style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b' }}>역할 선택</label>
                            <select
                              className="input"
                              value={userRoleFilter}
                              onChange={(e) => {
                                const val = e.target.value as 'ALL' | 'USER' | 'ADMIN' | 'SUPER_ADMIN';
                                setUserRoleFilter(val);
                                loadSuperAdminUsers(null, null, { searchType: 'role', role: val });
                              }}
                              style={{
                                width: '100%',
                                height: '38px',
                                padding: '0 1.85rem 0 0.75rem',
                                fontSize: '0.85rem',
                                fontWeight: 500,
                                borderRadius: '8px',
                                border: '1px solid #cbd5e1',
                                backgroundColor: '#ffffff',
                                color: '#1e293b',
                                appearance: 'none',
                                WebkitAppearance: 'none',
                                backgroundImage: `url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e")`,
                                backgroundRepeat: 'no-repeat',
                                backgroundPosition: 'right 0.55rem center',
                                backgroundSize: '1rem',
                                cursor: 'pointer',
                                outline: 'none',
                                boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)',
                              }}
                            >
                              <option value="ALL">전체</option>
                              <option value="USER">일반 유저</option>
                              <option value="ADMIN">공간 관리자</option>
                              <option value="SUPER_ADMIN">최고 관리자</option>
                            </select>
                          </div>
                        ) : (
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', flex: '1 1 260px', minWidth: '220px', maxWidth: '360px' }}>
                            <label style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b' }}>검색어</label>
                            <input
                              type="text"
                              className="input"
                              placeholder={
                                userSearchType === 'name'
                                  ? '검색할 이름을 입력하세요...'
                                  : userSearchType === 'email'
                                  ? '검색할 이메일을 입력하세요...'
                                  : '검색할 소속 공간 이름을 입력하세요...'
                              }
                              value={userSearchKeyword}
                              onChange={(e) => setUserSearchKeyword(e.target.value)}
                              style={{
                                width: '100%',
                                height: '38px',
                                padding: '0 0.85rem',
                                fontSize: '0.85rem',
                                fontWeight: 500,
                                borderRadius: '8px',
                                border: '1px solid #cbd5e1',
                                backgroundColor: '#ffffff',
                                color: '#1e293b',
                                outline: 'none',
                                boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)',
                              }}
                            />
                          </div>
                        )}

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                          <label style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b' }}>정렬 기준</label>
                          <select
                            className="input"
                            value={userSortBy}
                            onChange={(e) => {
                              const val = e.target.value as UserSortByType;
                              setUserSortBy(val);
                              userSortByRef.current = val;
                              loadSuperAdminUsers(null, null, { sortBy: val });
                            }}
                            style={{
                              width: '115px',
                              height: '38px',
                              padding: '0 1.85rem 0 0.75rem',
                              fontSize: '0.85rem',
                              fontWeight: 500,
                              borderRadius: '8px',
                              border: '1px solid #cbd5e1',
                              backgroundColor: '#ffffff',
                              color: '#1e293b',
                              appearance: 'none',
                              WebkitAppearance: 'none',
                              backgroundImage: `url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e")`,
                              backgroundRepeat: 'no-repeat',
                              backgroundPosition: 'right 0.55rem center',
                              backgroundSize: '1rem',
                              cursor: 'pointer',
                              outline: 'none',
                              boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)',
                            }}
                          >
                            <option value="createdAt">가입 시간</option>
                            <option value="updatedAt">수정 시간</option>
                            <option value="deletedAt">탈퇴 시간</option>
                          </select>
                        </div>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                          <label style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b' }}>정렬 순서</label>
                          <select
                            className="input"
                            value={userSortDirection}
                            onChange={(e) => {
                              const val = e.target.value as SortDirectionType;
                              setUserSortDirection(val);
                              userSortDirectionRef.current = val;
                              loadSuperAdminUsers(null, null, { sortDirection: val });
                            }}
                            style={{
                              width: '175px',
                              height: '38px',
                              padding: '0 1.85rem 0 0.75rem',
                              fontSize: '0.85rem',
                              fontWeight: 500,
                              borderRadius: '8px',
                              border: '1px solid #cbd5e1',
                              backgroundColor: '#ffffff',
                              color: '#1e293b',
                              appearance: 'none',
                              WebkitAppearance: 'none',
                              backgroundImage: `url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e")`,
                              backgroundRepeat: 'no-repeat',
                              backgroundPosition: 'right 0.55rem center',
                              backgroundSize: '1rem',
                              cursor: 'pointer',
                              outline: 'none',
                              boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)',
                            }}
                          >
                            <option value="DESCENDING">내림차순 (최신 순)</option>
                            <option value="ASCENDING">오름차순 (오래된 순)</option>
                          </select>
                        </div>

                        <div style={{ display: 'flex', gap: '0.4rem', marginLeft: 'auto' }}>
                          <button
                            type="submit"
                            style={{
                              height: '38px',
                              padding: '0 1rem',
                              fontSize: '0.85rem',
                              fontWeight: 600,
                              borderRadius: '8px',
                              backgroundColor: '#4f46e5',
                              color: '#ffffff',
                              border: 'none',
                              cursor: 'pointer',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '0.35rem',
                              boxShadow: '0 2px 6px rgba(79, 70, 229, 0.25)',
                            }}
                          >
                            <span>🔍</span> 검색
                          </button>
                          <button
                            type="button"
                            onClick={handleUserFilterReset}
                            style={{
                              height: '38px',
                              padding: '0 0.85rem',
                              fontSize: '0.85rem',
                              fontWeight: 600,
                              borderRadius: '8px',
                              backgroundColor: '#f1f5f9',
                              color: '#475569',
                              border: '1px solid #cbd5e1',
                              cursor: 'pointer',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '0.35rem',
                            }}
                          >
                            <span>🔄</span> 초기화
                          </button>
                        </div>
                      </form>

                      <div style={styles.tableWrapper}>
                        <table style={styles.adminTable}>
                          <thead>
                          <tr>
                            <th style={{ ...styles.th, width: '13%' }}>이름</th>
                            <th style={{ ...styles.th, width: '25%' }}>이메일</th>
                            <th style={{ ...styles.th, width: '18%' }}>소속 공간</th>
                            <th style={{ ...styles.th, width: '18%' }}>
                              {userSortBy === 'deletedAt' ? '탈퇴일시' : userSortBy === 'updatedAt' ? '수정일시' : '가입일시'}
                            </th>
                            <th style={{ ...styles.th, width: '11%' }}>역할 권한</th>
                            <th style={{ ...styles.th, width: '75px' }}>정지 상태</th>
                            <th style={{ ...styles.th, width: '75px' }}>유저 정지</th>
                          </tr>
                          </thead>
                          <tbody>
                          {allUsers.length === 0 ? (
                            <tr>
                              <td colSpan={7} style={{ ...styles.td, textAlign: 'center', color: '#6b7280', padding: '2rem' }}>
                                검색 조건에 해당하는 회원 데이터가 없습니다.
                              </td>
                            </tr>
                          ) : (
                            allUsers.map(u => {
                              const targetDateStr = userSortBy === 'deletedAt' ? u.deletedAt : userSortBy === 'updatedAt' ? u.updatedAt : u.createdAt;
                              let formattedDate = '-';
                              if (targetDateStr) {
                                try {
                                  formattedDate = new Date(targetDateStr).toLocaleString('ko-KR', {
                                    year: 'numeric',
                                    month: '2-digit',
                                    day: '2-digit',
                                    hour: '2-digit',
                                    minute: '2-digit',
                                  });
                                } catch {
                                  formattedDate = targetDateStr;
                                }
                              }

                              return (
                                <tr key={u.id} style={styles.tr}>
                                  <td style={styles.td}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                      <img
                                        src={u.profileImageUrl || '/basic.png'}
                                        alt={u.name}
                                        style={{ width: '28px', height: '28px', borderRadius: '50%', objectFit: 'cover', border: '1px solid #eaecf0', flexShrink: 0 }}
                                        onError={handleAvatarError}
                                      />
                                      <span>{u.name}</span>
                                    </div>
                                  </td>
                                  <td style={{ ...styles.td, wordBreak: 'break-all' }}>{u.email}</td>
                                  <td style={styles.td}>
                                    {u.spaceName ? (
                                      <span style={{ fontWeight: 600, color: '#4f46e5', backgroundColor: '#eef2ff', padding: '0.2rem 0.55rem', borderRadius: '6px', fontSize: '0.8rem', display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                                        <span>🏠</span> {u.spaceName}
                                      </span>
                                    ) : (
                                      <span style={{ color: '#94a3b8', fontSize: '0.8rem', fontStyle: 'italic' }}>
                                        미소속
                                      </span>
                                    )}
                                  </td>
                                  <td style={{ ...styles.td, fontSize: '0.825rem', color: '#475467' }}>{formattedDate}</td>
                                  <td style={styles.td}>
                                    <span className="badge badge-info">
                                      {u.role === 'SUPER_ADMIN' ? '최고 관리자' : u.role === 'ADMIN' ? '공간 관리자' : '일반 유저'}
                                    </span>
                                  </td>
                                  <td style={styles.td}>
                                    {u.deletedAt ? (
                                        <span className="badge" style={{ backgroundColor: '#f1f5f9', color: '#64748b', border: '1px solid #cbd5e1' }}>탈퇴 완료</span>
                                    ) : u.isBanned ? (
                                        <span className="badge badge-danger">정지 상태</span>
                                    ) : (
                                        <span className="badge badge-success">정상 이용</span>
                                    )}
                                  </td>
                                  <td style={styles.td}>
                                    {u.role === 'SUPER_ADMIN' || u.deletedAt ? (
                                        <span style={styles.disabledText}>변경 불가</span>
                                    ) : (
                                        <div
                                            role="switch"
                                            aria-checked={u.isBanned}
                                            style={styles.banSwitch}
                                            onClick={() => handleToggleUserBan(u)}
                                        >
                                          <div
                                              style={{
                                                ...styles.banSwitchTrack,
                                                backgroundColor: u.isBanned ? '#ef4444' : '#d0d5dd',
                                              }}
                                          />
                                          <div
                                              style={{
                                                ...styles.banSwitchKnob,
                                                transform: u.isBanned ? 'translateX(20px)' : 'translateX(0)',
                                              }}
                                          />
                                        </div>
                                    )}
                                  </td>
                                </tr>
                              );
                            })
                          )}
                          </tbody>
                        </table>
                      </div>

                      {userHasNext && (
                          <div
                              ref={userSentinelRef}
                              style={{
                                  padding: userLoadingMore ? '1rem' : '0.5rem',
                                  textAlign: 'center',
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                  gap: '8px',
                                  minHeight: '24px',
                              }}
                          >
                            {userLoadingMore && (
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#6366f1', fontSize: '0.85rem', fontWeight: 500 }}>
                                  <span
                                      style={{
                                        width: '14px',
                                        height: '14px',
                                        border: '2px solid #6366f1',
                                        borderRightColor: 'transparent',
                                        borderRadius: '50%',
                                        display: 'inline-block',
                                        animation: 'spin 0.75s linear infinite'
                                      }}
                                  />
                                  <span>사용자 목록을 불러오는 중...</span>
                                </div>
                            )}
                          </div>
                      )}
                    </div>
                )}

                {superadminSubTab === 'spaces' && (
                    <div className="card" style={styles.adminContentCard}>
                      <h3 style={styles.adminContentTitle}>
                        서비스 개설 공간 전체 목록
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
                                  <span className="badge badge-info">{p.categoryName || '미분류'}</span>
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
                  {/* 카드 내부 최상단에 배치된 타이틀 영역 */}
                  <div style={{ borderLeft: '4px solid var(--primary)', paddingLeft: '0.875rem', marginBottom: '0.5rem' }}>
                    <h2 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: 'var(--text)', margin: '0 0 0.375rem 0', display: 'flex', alignItems: 'center', gap: '0.5rem', lineHeight: 1.2 }}>
                      👤 개인 정보 수정
                    </h2>
                    <p style={{ fontSize: '0.875rem', color: 'var(--text-sub)', margin: 0 }}>
                      회원님의 프로필 이미지, 이메일 정보, 비밀번호 등의 계정 설정을 관리합니다.
                    </p>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid #eaecf0', paddingBottom: '1.5rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                      <img
                        src={profilePreview || (removeProfileImage ? DEFAULT_AVATAR : savedProfilePreview || user.profileImageUrl || DEFAULT_AVATAR)}
                        alt="Profile"
                        style={{ width: '64px', height: '64px', borderRadius: '50%', objectFit: 'cover', border: '1px solid #eaecf0' }}
                        onError={handleAvatarError}
                      />
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem' }}>
                        <span style={{ fontSize: '1.125rem', fontWeight: '600', color: 'var(--text)', display: 'flex', alignItems: 'center' }}>
                          <span style={{ display: 'inline-block', width: '1.5rem', textAlign: 'left' }}>📧</span>
                          <span>{user.email}</span>
                        </span>
                        <span style={{ fontSize: '0.875rem', color: 'var(--text-sub)', display: 'flex', alignItems: 'center' }}>
                          <span style={{ display: 'inline-block', width: '1.5rem', textAlign: 'left' }}>
                            {user.role === 'SUPER_ADMIN' ? '👑' : user.role === 'ADMIN' ? '🔑' : '🎓'}
                          </span>
                          <span>
                            {user.role === 'SUPER_ADMIN' ? '최고 관리자' : user.role === 'ADMIN' ? '공간 관리자' : '일반 학습 회원'}
                          </span>
                        </span>
                      </div>
                    </div>
                    <button
                        type="button"
                        className="btn btn-danger"
                        style={{ padding: '0.5rem 1rem', fontSize: '0.875rem', backgroundColor: '#fee4e2', color: '#d92d20', border: '1px solid #fda29b', cursor: 'pointer' }}
                        onClick={handleWithdrawal}
                    >
                      회원 탈퇴
                    </button>
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
                      <label className="input-label">프로필 이미지 업로드</label>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', width: '100%' }}>
                        <input
                            type="file"
                            accept="image/*"
                            id="profile-file-input"
                            style={{ display: 'none' }}
                            onChange={handleFileChange}
                        />
                        <label
                            htmlFor="profile-file-input"
                            className="btn btn-secondary"
                            style={{ cursor: 'pointer', margin: 0, padding: '0.5rem 1rem', fontSize: '0.875rem' }}
                        >
                          이미지 선택
                        </label>
                        <span style={{ fontSize: '0.875rem', color: 'var(--text-sub)' }}>
                          {profileFile && profileFile.name !== 'clear_profile.png' ? profileFile.name : '선택된 파일 없음'}
                        </span>
                        {profilePreview && (
                          <button
                              type="button"
                              className="btn btn-danger"
                              style={{ cursor: 'pointer', margin: '0 0 0 auto', padding: '0.5rem 1rem', fontSize: '0.875rem' }}
                              onClick={handleDeleteProfileImage}
                          >
                            이미지 삭제
                          </button>
                        )}
                      </div>
                    </div>

                    {/* 비밀번호 변경 영역 (소셜 가입 유저는 비밀번호 변경 불가) */}
                    {user.social === 'NONE' && (
                        <div style={{ borderTop: '1px solid #eaecf0', paddingTop: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                          <h3 style={{ fontSize: '1rem', fontWeight: 'bold', color: 'var(--text)', margin: '0 0 0.5rem 0' }}>비밀번호 변경</h3>
                          <div className="input-group" style={{ marginBottom: 0 }}>
                            <label className="input-label">현재 비밀번호</label>
                            <input
                                type="password"
                                className="input-field"
                                placeholder="현재 비밀번호를 입력해 주세요"
                                value={currentPassword}
                                onChange={(e) => setCurrentPassword(e.target.value)}
                            />
                          </div>
                          <div className="input-group" style={{ marginBottom: 0 }}>
                            <label className="input-label">새 비밀번호</label>
                            <input
                                type="password"
                                className="input-field"
                                placeholder="새 비밀번호 (8~20자, 영문/숫자/특수문자 @$!%*#?& 포함)"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                            />
                          </div>
                          <div className="input-group" style={{ marginBottom: 0 }}>
                            <label className="input-label">새 비밀번호 확인</label>
                            <input
                                type="password"
                                className="input-field"
                                placeholder="새 비밀번호를 한번 더 입력해 주세요"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                            />
                          </div>
                        </div>
                    )}

                    <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                      <button type="submit" className="btn btn-primary" style={{ flex: 1, padding: '0.75rem' }}>
                        프로필 수정 저장
                      </button>
                      <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ flex: 1, padding: '0.75rem' }}
                          onClick={handleCancel}
                      >
                        취소
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
    flexWrap: 'wrap',
    gap: '1rem',
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
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
    position: 'relative',
    flexWrap: 'wrap',
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
  userHeaderWidget: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.625rem',
    padding: '0.4rem 0.75rem',
    borderRadius: '24px',
    backgroundColor: '#ffffff',
    border: '1px solid #eaecf0',
    cursor: 'pointer',
    boxShadow: '0 1px 3px rgba(16, 24, 40, 0.05)',
  },
  headerAvatar: {
    width: '32px',
    height: '32px',
    borderRadius: '50%',
    objectFit: 'cover',
    border: '1px solid #eaecf0',
  },
  headerUserName: {
    fontWeight: 600,
    fontSize: '0.875rem',
    color: '#1d2939',
  },
  userMenuPopover: {
    position: 'absolute',
    top: 'calc(100% + 8px)',
    right: 0,
    width: '240px',
    padding: '12px',
    borderRadius: '12px',
    backgroundColor: '#ffffff',
    boxShadow: '0 10px 25px rgba(0, 0, 0, 0.1)',
    zIndex: 1000,
    border: '1px solid #eaecf0',
  },
  userMenuHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    paddingBottom: '8px',
  },
  menuAvatar: {
    width: '40px',
    height: '40px',
    borderRadius: '50%',
    objectFit: 'cover',
    border: '1px solid #eaecf0',
  },
  popoverItemBtn: {
    width: '100%',
    padding: '8px 10px',
    borderRadius: '6px',
    border: 'none',
    backgroundColor: 'transparent',
    textAlign: 'left',
    fontSize: '0.85rem',
    fontWeight: 600,
    color: '#344054',
    cursor: 'pointer',
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
    padding: '0.55rem 1.1rem',
    fontSize: '0.925rem',
    fontWeight: 600,
    fontFamily: "'Pretendard', -apple-system, BlinkMacSystemFont, system-ui, Roboto, sans-serif",
    letterSpacing: '-0.015em',
    cursor: 'pointer',
    transition: 'all 0.15s ease',
  },
  adminSubTabActive: {
    color: '#4f46e5',
    borderBottom: '2.5px solid #4f46e5',
    borderRadius: '0',
    fontWeight: 700,
  },
  adminContentCard: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
    padding: '2rem',
  },
  adminContentTitle: {
    fontSize: '1.2rem',
    fontWeight: 700,
    color: '#0f172a',
    fontFamily: "'Pretendard', -apple-system, BlinkMacSystemFont, system-ui, Roboto, sans-serif",
    letterSpacing: '-0.025em',
  },
  tableWrapper: {
    width: '100%',
    overflowX: 'auto',
    border: '1px solid #e2e8f0',
    borderRadius: '10px',
    boxShadow: '0 1px 3px rgba(0, 0, 0, 0.02)',
  },
  adminTable: {
    width: '100%',
    tableLayout: 'fixed',
    borderCollapse: 'collapse',
    textAlign: 'left',
    fontSize: '0.875rem',
  },
  th: {
    backgroundColor: '#f8fafc',
    padding: '0.85rem 1.1rem',
    fontWeight: 600,
    color: '#475569',
    borderBottom: '1px solid #e2e8f0',
    fontFamily: "'Pretendard', -apple-system, BlinkMacSystemFont, system-ui, Roboto, sans-serif",
    letterSpacing: '-0.015em',
    fontSize: '0.85rem',
  },
  tr: {
    borderBottom: '1px solid #f1f5f9',
    transition: 'background-color 0.15s ease',
  },
  td: {
    padding: '0.95rem 1.1rem',
    color: '#1e293b',
    fontFamily: "'Pretendard', -apple-system, BlinkMacSystemFont, system-ui, Roboto, sans-serif",
    letterSpacing: '-0.015em',
    fontSize: '0.875rem',
  },
  disabledText: {
    color: '#98a2b3',
    fontSize: '0.825rem',
    fontStyle: 'italic',
  },
  banSwitch: {
    position: 'relative',
    display: 'inline-block',
    width: '44px',
    height: '24px',
    cursor: 'pointer',
  },
  banSwitchTrack: {
    position: 'absolute',
    inset: 0,
    borderRadius: '999px',
    transition: 'background-color 0.2s',
  },
  banSwitchKnob: {
    position: 'absolute',
    top: '2px',
    left: '2px',
    width: '20px',
    height: '20px',
    borderRadius: '50%',
    backgroundColor: '#fff',
    transition: 'transform 0.2s',
    boxShadow: '0 1px 2px rgba(0,0,0,0.3)',
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
