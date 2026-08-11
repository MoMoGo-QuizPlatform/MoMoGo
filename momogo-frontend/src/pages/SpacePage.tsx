import React, { useEffect, useState, useRef } from 'react';
import type { UserResponse } from '../types/user';
import type { SpaceResponse } from '../types/space';
import { request, getAccessToken, connectNotificationSse } from '../services/api';

const generateUUID = (): string => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

interface SpacePageProps {
  user: UserResponse;
  space: SpaceResponse;
  onBack: (tab?: 'dashboard' | 'mypage') => void;
  showToast: (message: string, type?: 'success' | 'error' | 'info') => void;
  initialTab?: 'problems' | 'exams' | 'dashboard';
  onTabChange?: (tab: 'problems' | 'exams' | 'dashboard') => void;
}

interface CategoryResponse {
  id: string;
  name: string;
}

interface NotificationItem {
  id: string;
  title: string;
  content: string;
  type: string;
  isConfirmed: boolean;
  createdAt: string;
}

interface ProblemResponse {
  id: string;
  name: string;
  content: string;
  correctAnswer: string;
  explanation: string;
  categoryId: string | null;
  categoryName: string | null;
  isSolved: boolean;
}

// 문제 편집용 단건 조회 응답 (정답 포함, ADMIN 전용 API)
interface ProblemDetailResponse {
  id: string;
  spaceId: string;
  categoryId: string | null;
  categoryName: string | null;
  name: string;
  content: string;
  correctAnswer: string;
  explanation: string;
}

// 문제 제출/채점 응답
interface ProblemSolveResponse {
  isSolved: boolean;
  correctAnswer: string;
  explanation: string;
  tryCount: number;
  solvedCount: number;
}

interface RoomResponse {
  id: string;
  name: string;
  description: string;
  testStartAt: string;
  testEndAt: string;
  isEnded: boolean;
  isAiGradingInProgress: boolean;
}

interface RoomReportResponse {
  roomId: string;
  roomName: string;
  totalApplicants: number;
  attendedCount: number;
  averageScore: number;
  maxScore: number;
  takerGrades: {
    userId: string;
    name: string;
    email: string;
    profileImageUrl: string | null;
    isAttended: boolean;
    score: number;
  }[];
}

interface SingleSummaryResponse {
  dailySolvedCount: number;
  weeklySolvedCount: number;
  averageCorrectRate: number;
}

interface SingleHistoryResponse {
  problemId: string;
  problemName: string;
  categoryName: string;
  isSolved: boolean;
  userAnswer: string;
  correctAnswer: string;
  solvedAt: string;
}

interface ExamListItemResponse {
  roomId: string;
  roomName: string;
  description: string;
  score: number;
  totalProblems: number;
  testStartAt: string;
  testEndAt: string;
  isEnded: boolean;
}

interface ExamDetailResponse {
  roomId: string;
  roomName: string;
  description: string;
  score: number;
  problems: {
    problemId: string;
    problemOrder: number;
    name: string;
    content: string;
    userAnswer: string;
    correctAnswer: string;
    explanation: string;
    isCorrect: boolean;
  }[];
}

interface AnswerGradingItem {
  answerId: string;
  userId: string;
  userName: string;
  userProfileImageUrl: string | null;
  problemId: string;
  problemOrder: number;
  problemName: string;
  userAnswer: string;
  correctAnswer: string;
  isCorrect: boolean | null;
}

interface RoomGradingResponse {
  roomId: string;
  roomName: string;
  isAiGradingInProgress: boolean;
  answers: AnswerGradingItem[];
}

interface RoomProblemResponse {
  id: string;
  roomId: string;
  problemOrder: number;
  name: string;
  content: string;
}

interface SpaceRankingResponse {
  userId: string;
  userName: string;
  profileImageUrl: string | null;
  solvedCount: number;
}

// 프로필 이미지 로드 실패(마이그레이션 이전 로컬 경로 등) 시 깨진 이미지 아이콘 대신 기본 아바타로 대체
const handleAvatarError = (e: React.SyntheticEvent<HTMLImageElement>) => {
  e.currentTarget.onerror = null;
  e.currentTarget.src = '/basic.png';
};

export const SpacePage: React.FC<SpacePageProps> = ({ user, space, onBack, showToast, initialTab, onTabChange }) => {
  const [activeTab, setActiveTabState] = useState<'problems' | 'exams' | 'dashboard'>(initialTab ?? 'problems');
  const setActiveTab = (tab: 'problems' | 'exams' | 'dashboard') => {
    setActiveTabState(tab);
    onTabChange?.(tab);
  };

  // 데이터 로딩 상태
  const [loading, setLoading] = useState(false);

  // 1. 문제 은행 관련 상태
  const [problems, setProblems] = useState<ProblemResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('');

  // 1-1. 문제 생성/편집 모달
  const [showCreateProblemModal, setShowCreateProblemModal] = useState(false);
  const [showCreateAIProblemModal, setShowCreateAIProblemModal] = useState(false);
  const [editingProblem, setEditingProblem] = useState<ProblemResponse | null>(null);

  // 문제 폼 상태
  const [probName, setProbName] = useState('');
  const [probContent, setProbContent] = useState('');
  const [probAnswer, setProbAnswer] = useState('');
  const [probExplanation, setProbExplanation] = useState('');
  const [probCategory, setProbCategory] = useState('');

  // AI 문제 생성 폼 상태
  const [aiRefData, setAiRefData] = useState('');
  const [aiCount, setAiCount] = useState(3);
  const [aiCategory, setAiCategory] = useState('');

  // 문제 풀이 모달
  const [showSolveModal, setShowSolveModal] = useState(false);
  const [solvingProblem, setSolvingProblem] = useState<ProblemResponse | null>(null);
  const [solveUserAnswer, setSolveUserAnswer] = useState('');
  const [solveResult, setSolveResult] = useState<{ correct: boolean; answer: string; exp: string } | null>(null);

  // 2. 평가 시험방 관련 상태
  const [rooms, setRooms] = useState<RoomResponse[]>([]);
  const [showCreateRoomModal, setShowCreateRoomModal] = useState(false);
  const [roomWizardStep, setRoomWizardStep] = useState<1 | 2>(1);

  // 시험방 생성 폼 상태 (1단계: 기본 정보)
  const [roomName, setRoomName] = useState('');
  const [roomDesc, setRoomDesc] = useState('');
  const [roomStart, setRoomStart] = useState('');
  const [roomEnd, setRoomEnd] = useState('');
  const [selectedMembers, setSelectedMembers] = useState<string[]>([]);
  const [memberSearchQuery, setMemberSearchQuery] = useState('');

  // 시험방 생성 폼 상태 (2단계: 문제 출제 - 수동/AI 혼합)
  const [examProblemsList, setExamProblemsList] = useState<{
    categoryId: string;
    problemOrder: number;
    name: string;
    content: string;
    explanation: string;
    correctAnswer: string;
  }[]>([{ categoryId: '', problemOrder: 1, name: '', content: '', explanation: '', correctAnswer: '' }]);

  // 2단계 내 AI 자동 출제 (싱글모드 AI 출제기와 동일한 방식, 결과는 저장 전 미리보기로 목록에 추가됨)
  const [showRoomAiModal, setShowRoomAiModal] = useState(false);
  const [roomAiCategory, setRoomAiCategory] = useState('');
  const [roomAiRefData, setRoomAiRefData] = useState('');
  const [roomAiCount, setRoomAiCount] = useState(3);
  const [roomAiLoading, setRoomAiLoading] = useState(false);

  const [allUsersList, setAllUsersList] = useState<UserResponse[]>([]);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);

  // 2-1. 실시간 대기실 / 시험 모드 관련 상태
  const [currentExamRoom, setCurrentExamRoom] = useState<RoomResponse | null>(null);
  const [examMode, setExamMode] = useState<'none' | 'waiting' | 'testing'>('none');
  const [examProblems, setExamProblems] = useState<RoomProblemResponse[]>([]);
  const [currentProblemIndex, setCurrentProblemIndex] = useState(0);
  const [examAnswers, setExamAnswers] = useState<Record<string, string>>({});
  const [timeLeft, setTimeLeft] = useState(0);

  // 2-2. 성적표 / 통계 모달
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportData, setReportData] = useState<RoomReportResponse | null>(null);

  // 2-3. 채점 검토 모달 (AI 채점 + 수동 채점 확인 후 확정)
  const [showGradingModal, setShowGradingModal] = useState(false);
  const [gradingRoomId, setGradingRoomId] = useState<string | null>(null);
  const [gradingData, setGradingData] = useState<RoomGradingResponse | null>(null);
  const [gradingLoading, setGradingLoading] = useState(false);

  // 3. 마이페이지 대시보드 관련 상태
  const [summary, setSummary] = useState<SingleSummaryResponse | null>(null);
  const [history, setHistory] = useState<SingleHistoryResponse[]>([]);
  const [examsHistory, setExamsHistory] = useState<ExamListItemResponse[]>([]);
  const [viewingExamDetail, setViewingExamDetail] = useState<ExamDetailResponse | null>(null);
  const [ranking, setRanking] = useState<SpaceRankingResponse[]>([]);

  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const timerRef = useRef<any>(null);

  useEffect(() => {
    loadCategories();
    loadProblems();
    loadRooms();
    loadDashboardData();
    loadMembersForInvitation();
  }, [selectedCategory, activeTab]);

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

  // 대기실 또는 시험 중일 때의 타이머 처리
  useEffect(() => {
    if (examMode === 'testing' && timeLeft > 0) {
      timerRef.current = setTimeout(() => {
        setTimeLeft(prev => prev - 1);
      }, 1000);
      return () => {
        if (timerRef.current) clearTimeout(timerRef.current);
      };
    } else if (examMode === 'testing' && timeLeft === 0) {
      handleForceSubmitExam();
    }
  }, [timeLeft, examMode]);

  // 카테고리 로딩
  const loadCategories = async () => {
    try {
      const data = await request<CategoryResponse[]>('/api/categories', { method: 'GET' });
      if (data) setCategories(data);
    } catch (err: any) {
      console.error(err);
    }
  };

  // 문제 목록 로딩
  const loadProblems = async () => {
    try {
      setLoading(true);
      const params: Record<string, string> = { size: '100' };
      if (selectedCategory) {
        params.categoryId = selectedCategory;
      }
      const data = await request<any>(`/api/spaces/${space.id}/problems`, {
        method: 'GET',
        params,
      });
      if (data && data.content) {
        setProblems(data.content);
      }
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // 시험방 로딩
  const loadRooms = async () => {
    try {
      const data = await request<RoomResponse[]>(`/api/spaces/${space.id}/rooms`, {
        method: 'GET',
      });
      if (data) setRooms(data);
    } catch {
      setRooms([]);
    }
  };

  // 대시보드 로딩
  const loadDashboardData = async () => {
    try {
      const summaryData = await request<SingleSummaryResponse>('/api/dashboards/single/summary', { method: 'GET' });
      const historyData = await request<SingleHistoryResponse[]>('/api/dashboards/single/history', { method: 'GET' });
      const examHistoryData = await request<ExamListItemResponse[]>('/api/dashboards/exams', { method: 'GET' });
      const rankingData = await request<SpaceRankingResponse[]>(`/api/dashboards/spaces/${space.id}/ranking`, { method: 'GET' });

      if (summaryData) setSummary(summaryData);
      if (historyData) setHistory(historyData);
      if (examHistoryData) setExamsHistory(examHistoryData);
      if (rankingData) setRanking(rankingData);
    } catch (err: any) {
      console.error(err);
    }
  };

  // 멤버 가입 리스트 조회 (초대용/응시 대상자 지정용) - 내가 소속된 공간의 전체 유저 목록
  const loadMembersForInvitation = async () => {
    try {
      const usersData = await request<any[]>('/api/users', { method: 'GET' });
      setAllUsersList(usersData || []);
    } catch (err) {
      console.error('공간 멤버 목록 조회 실패:', err);
      setAllUsersList([]);
    }
  };

  // 문제 수동 생성/수정 완료
  const handleSaveProblem = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors: Record<string, string> = {};
    if (!probName.trim()) errors.probName = '문제 이름을 입력해 주세요.';
    if (!probContent.trim()) errors.probContent = '문제 내용을 입력해 주세요.';
    if (!probAnswer.trim()) errors.probAnswer = '정답을 입력해 주세요.';
    if (!probExplanation.trim()) errors.probExplanation = '해설을 입력해 주세요.';
    if (!probCategory) errors.probCategory = '카테고리를 선택해 주세요.';

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      return;
    }

    try {
      const payload = {
        categoryId: probCategory,
        name: probName,
        content: probContent,
        explanation: probExplanation,
        correctAnswer: probAnswer,
      };

      if (editingProblem) {
        // 수정
        await request(`/api/spaces/${space.id}/problems/${editingProblem.id}`, {
          method: 'PATCH',
          body: JSON.stringify(payload),
        });
        showToast('문제가 수정되었습니다.', 'success');
      } else {
        // 생성
        await request(`/api/spaces/${space.id}/problems`, {
          method: 'POST',
          body: JSON.stringify(payload),
        });
        showToast('새로운 문제가 등록되었습니다.', 'success');
      }
      setShowCreateProblemModal(false);
      setEditingProblem(null);
      resetProblemForm();
      loadProblems();
    } catch (err: any) {
      showToast(err.message || '문제 저장에 실패했습니다.', 'error');
    }
  };

  // AI 문제 생성 완료
  const handleGenerateAIProblems = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors: Record<string, string> = {};
    if (!aiCategory) errors.aiCategory = '카테고리를 선택해 주세요.';
    if (!aiRefData.trim()) errors.aiRefData = '참고 학습 자료를 기입해 주세요.';

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      return;
    }

    try {
      setLoading(true);
      await request(`/api/spaces/${space.id}/problems/ai`, {
        method: 'POST',
        headers: { 'Idempotency-Key': generateUUID() },
        body: JSON.stringify({
          categoryId: aiCategory,
          referenceText: aiRefData,
          questionCount: aiCount,
        }),
      });

      showToast(`AI 기반 문제 ${aiCount}개가 자동 생성되어 등록되었습니다.`, 'success');
      setShowCreateAIProblemModal(false);
      setAiRefData('');
      setAiCategory('');
      setValidationErrors({});
      loadProblems();
    } catch (err: any) {
      showToast(err.message || 'AI 생성 도중 서버 내부 오류가 발생했습니다.', 'error');
    } finally {
      setLoading(false);
    }
  };

  // 문제 편집 모달 열기 (정답은 목록 응답에 없어서 단건 조회로 별도 가져옴)
  const handleOpenEditProblem = async (prob: ProblemResponse) => {
    try {
      const detail = await request<ProblemDetailResponse>(
        `/api/spaces/${space.id}/problems/${prob.id}`,
        { method: 'GET' }
      );
      setEditingProblem(prob);
      setProbName(detail.name);
      setProbContent(detail.content);
      setProbAnswer(detail.correctAnswer);
      setProbExplanation(detail.explanation);
      setProbCategory(detail.categoryId || '');
      setShowCreateProblemModal(true);
    } catch (err: any) {
      showToast(err.message || '문제 정보를 불러오지 못했습니다.', 'error');
    }
  };

  // 문제 삭제
  const handleDeleteProblem = async (problemId: string) => {
    if (!window.confirm('정말 이 문제를 삭제하시겠습니까?')) return;
    try {
      await request(`/api/spaces/${space.id}/problems/${problemId}`, { method: 'DELETE' });
      showToast('문제가 삭제되었습니다.', 'success');
      loadProblems();
    } catch (err: any) {
      showToast(err.message || '문제 삭제에 실패했습니다.', 'error');
    }
  };

  // 싱글모드 문제 풀이 제출
  const handleSolveProblem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!solveUserAnswer.trim()) {
      setValidationErrors({ solveUserAnswer: '답안을 입력해 주세요.' });
      return;
    }

    try {
      const result = await request<ProblemSolveResponse>(
        `/api/spaces/${space.id}/problems/${solvingProblem?.id}/solve`,
        {
          method: 'POST',
          body: JSON.stringify({ userAnswer: solveUserAnswer }),
        }
      );

      setSolveResult({
        correct: result.isSolved,
        answer: result.correctAnswer,
        exp: result.explanation,
      });
      loadDashboardData();
    } catch (err: any) {
      showToast(err.message || '답안 제출에 실패했습니다.', 'error');
    }
  };

  // 시험방 생성 마법사 1단계(기본 정보) 검증 - 통과 시 2단계(문제 출제)로 전환
  const validateRoomStep1 = () => {
    const errors: Record<string, string> = {};
    if (!roomName.trim()) errors.roomName = '시험 이름을 입력해 주세요.';
    if (!roomStart) errors.roomStart = '시작 일시를 입력해 주세요.';
    if (!roomEnd) errors.roomEnd = '종료 일시를 입력해 주세요.';
    if (selectedMembers.length === 0) errors.selectedMembers = '최소 한 명의 응시자를 선택해 주세요.';

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      return false;
    }
    setValidationErrors({});
    return true;
  };

  // 시험방 생성 마법사 2단계(문제 출제) 검증 후 최종 개설 요청
  const submitCreateRoom = async () => {
    const errors: Record<string, string> = {};
    examProblemsList.forEach((p, i) => {
      if (!p.categoryId) errors[`problem_${i}_category`] = '카테고리를 선택해 주세요.';
      if (!p.name.trim()) errors[`problem_${i}_name`] = '문제명을 입력해 주세요.';
      if (!p.content.trim()) errors[`problem_${i}_content`] = '문제 내용을 입력해 주세요.';
      if (!p.correctAnswer.trim()) errors[`problem_${i}_answer`] = '정답을 입력해 주세요.';
    });

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      showToast('출제 문제 정보를 모두 입력해 주세요. (카테고리 포함)', 'error');
      return;
    }

    try {
      const payload = {
        name: roomName,
        description: roomDesc,
        testStartAt: new Date(roomStart).toISOString(),
        testEndAt: new Date(roomEnd).toISOString(),
        userIds: selectedMembers,
        problems: examProblemsList.map((p, i) => ({
          ...p,
          problemOrder: i + 1,
        })),
      };

      await request<any>(`/api/spaces/${space.id}/rooms`, {
        method: 'POST',
        body: JSON.stringify(payload),
      });

      showToast('정기 평가시험이 개설되었습니다.', 'success');
      setShowCreateRoomModal(false);
      resetRoomForm();
      loadRooms();
    } catch (err: any) {
      showToast(err.message || '시험방 개설에 실패했습니다.', 'error');
    }
  };

  // 마법사 폼 제출 - 1단계면 다음 단계로, 2단계면 실제 개설 요청
  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (roomWizardStep === 1) {
      if (validateRoomStep1()) {
        setRoomWizardStep(2);
      }
      return;
    }
    await submitCreateRoom();
  };

  // 2단계: AI 자동 출제 - 생성 결과를 저장하지 않고 문제 목록에 미리보기로 추가
  const handleGenerateRoomAiProblems = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors: Record<string, string> = {};
    if (!roomAiCategory) errors.roomAiCategory = '카테고리를 선택해 주세요.';
    if (!roomAiRefData.trim()) errors.roomAiRefData = '참고 학습 자료를 기입해 주세요.';

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      return;
    }

    try {
      setRoomAiLoading(true);
      const generated = await request<{ name: string; content: string; explanation: string; correctAnswer: string }[]>(
          `/api/spaces/${space.id}/rooms/ai-draft-problems`,
          {
            method: 'POST',
            body: JSON.stringify({
              referenceText: roomAiRefData,
              questionCount: roomAiCount,
            }),
          }
      );

      const newItems = (generated || []).map(g => ({
        categoryId: roomAiCategory,
        problemOrder: 0,
        name: g.name,
        content: g.content,
        explanation: g.explanation,
        correctAnswer: g.correctAnswer,
      }));

      setExamProblemsList(prev => {
        const withoutEmptyFirst = prev.length === 1 && !prev[0].name.trim() && !prev[0].content.trim()
            ? []
            : prev;
        return [...withoutEmptyFirst, ...newItems];
      });

      showToast(`AI 문제 ${newItems.length}개가 생성되어 목록에 추가되었습니다. 내용을 검토한 뒤 개설해 주세요.`, 'success');
      setShowRoomAiModal(false);
      setRoomAiRefData('');
      setRoomAiCategory('');
      setValidationErrors({});
    } catch (err: any) {
      showToast(err.message || 'AI 생성 도중 서버 내부 오류가 발생했습니다.', 'error');
    } finally {
      setRoomAiLoading(false);
    }
  };

  // 실시간 대기실 입장 처리
  const handleEnterWaitRoom = async (room: RoomResponse) => {
    setCurrentExamRoom(room);
    setExamMode('waiting');
  };

  // 시험 시작하기
  const handleStartExam = async () => {
    if (!currentExamRoom) return;
    try {
      const data = await request<RoomProblemResponse[]>(`/api/rooms/${currentExamRoom.id}/problems`, {
        method: 'GET',
      });
      if (data && data.length > 0) {
        setExamProblems(data);
        setExamMode('testing');
        setCurrentProblemIndex(0);
        setExamAnswers({});

        // 시험 시간 계산 (초 단위)
        const endTime = new Date(currentExamRoom.testEndAt).getTime();
        const now = new Date().getTime();
        const diff = Math.max(0, Math.floor((endTime - now) / 1000));
        setTimeLeft(diff > 0 ? diff : 600); // 테스트용 기본 10분 설정 fallback
      } else {
        showToast('배치된 시험 문제가 존재하지 않습니다.', 'error');
      }
    } catch (err: any) {
      showToast(err.message || '시험 시작에 실패했습니다.', 'error');
    }
  };

  // 시험 답안 작성
  const handleMarkAnswer = (probId: string, value: string) => {
    setExamAnswers(prev => ({
      ...prev,
      [probId]: value,
    }));
  };

  // 시험 제출 처리
  const handleSubmitExam = async () => {
    if (!currentExamRoom) return;
    try {
      const answersPayload = Object.keys(examAnswers).map(k => ({
        roomProblemId: k,
        userAnswer: examAnswers[k],
      }));

      await request(`/api/rooms/${currentExamRoom.id}/submit`, {
        method: 'POST',
        body: JSON.stringify({ answers: answersPayload }),
      });

      showToast('시험 답안이 성공적으로 제출되었습니다.', 'success');
      exitExamMode();
    } catch (err: any) {
      showToast(err.message || '답안 제출에 실패했습니다.', 'error');
    }
  };

  // 강제 제출 (시간 초과 시)
  const handleForceSubmitExam = () => {
    showToast('시험 시간이 만료되어 자동 제출을 시행합니다.', 'info');
    handleSubmitExam();
  };

  // AI 채점 가동
  const handleAiGrade = async (roomId: string) => {
    try {
      await request(`/api/rooms/${roomId}/ai-grade`, { method: 'POST' });
      showToast('AI 선제 채점 백그라운드 태스크가 가동되었습니다.', 'success');
      loadRooms();
    } catch (err: any) {
      showToast(err.message || 'AI 채점 실행 실패', 'error');
    }
  };

  // 채점 검토 모달 데이터 로딩 (AI 채점 진행 상태 및 답안별 정오 판정 포함)
  const loadGradingData = async (roomId: string) => {
    try {
      setGradingLoading(true);
      const data = await request<RoomGradingResponse>(`/api/rooms/${roomId}/grading`, { method: 'GET' });
      setGradingData(data);
    } catch (err: any) {
      showToast(err.message || '채점 데이터 로드 실패', 'error');
    } finally {
      setGradingLoading(false);
    }
  };

  // 채점 검토 모달 열기
  const handleOpenGrading = (roomId: string) => {
    setGradingRoomId(roomId);
    setGradingData(null);
    setShowGradingModal(true);
    loadGradingData(roomId);
  };

  // 채점 검토 모달 내 AI 채점 실행 (완료 후 "새로고침"으로 결과 재조회)
  const handleAiGradeInModal = async () => {
    if (!gradingRoomId) return;
    await handleAiGrade(gradingRoomId);
    await loadGradingData(gradingRoomId);
  };

  // 답안 한 건 수동 채점 (클릭 즉시 저장)
  const handleManualGrade = async (answerId: string, isCorrect: boolean) => {
    if (!gradingRoomId) return;
    try {
      await request(`/api/rooms/${gradingRoomId}/grading/${answerId}`, {
        method: 'PATCH',
        body: JSON.stringify({ isCorrect }),
      });
      setGradingData(prev => prev ? {
        ...prev,
        answers: prev.answers.map(a => a.answerId === answerId ? { ...a, isCorrect } : a),
      } : prev);
    } catch (err: any) {
      showToast(err.message || '수동 채점 반영 실패', 'error');
    }
  };

  // 채점 검토 모달에서 최종 확정
  const handleFinalizeFromGrading = async () => {
    if (!gradingRoomId) return;
    try {
      await request(`/api/rooms/${gradingRoomId}/finalize-grade`, { method: 'POST' });
      showToast('최종 채점 마감이 확정되었습니다. 이제 리포트를 열람할 수 있습니다.', 'success');
      setShowGradingModal(false);
      setGradingData(null);
      setGradingRoomId(null);
      loadRooms();
      loadDashboardData();
    } catch (err: any) {
      showToast(err.message || '채점 확정 실패', 'error');
    }
  };

  // 리포트 팝업 열기
  const handleOpenReport = async (roomId: string) => {
    try {
      const data = await request<RoomReportResponse>(`/api/rooms/${roomId}/report`, { method: 'GET' });
      if (data) {
        setReportData(data);
        setShowReportModal(true);
      }
    } catch (err: any) {
      showToast(err.message || '리포트 로드 실패', 'error');
    }
  };

  // 리포트 PDF 다운로드 (Authorization 헤더 인증이 필요해 window.open 대신 인증된 fetch로 blob을 받아 저장)
  const handleDownloadPdf = async (roomId: string) => {
    try {
      const headers: Record<string, string> = {};
      const token = getAccessToken();
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const response = await fetch(`/api/rooms/${roomId}/report/download`, {
        headers,
        credentials: 'include',
      });
      if (!response.ok) throw new Error('PDF 다운로드에 실패했습니다.');

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `room_${roomId}_report.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      showToast(err.message || 'PDF 다운로드에 실패했습니다.', 'error');
    }
  };

  // 마이페이지 시험 상세 보기
  const handleViewExamDetail = async (roomId: string) => {
    try {
      const data = await request<ExamDetailResponse>(`/api/dashboards/exams/${roomId}`, { method: 'GET' });
      if (data) {
        setViewingExamDetail(data);
      }
    } catch (err: any) {
      showToast(err.message || '시험 리뷰 정보를 가져올 수 없습니다.', 'error');
    }
  };

  const exitExamMode = () => {
    setExamMode('none');
    setCurrentExamRoom(null);
    setExamProblems([]);
    setExamAnswers({});
    loadRooms();
    loadDashboardData();
  };

  const resetProblemForm = () => {
    setProbName('');
    setProbContent('');
    setProbAnswer('');
    setProbExplanation('');
    setProbCategory('');
    setValidationErrors({});
  };

  const resetRoomForm = () => {
    setRoomWizardStep(1);
    setRoomName('');
    setRoomDesc('');
    setRoomStart('');
    setRoomEnd('');
    setSelectedMembers([]);
    setMemberSearchQuery('');
    setExamProblemsList([{ categoryId: '', problemOrder: 1, name: '', content: '', explanation: '', correctAnswer: '' }]);
    setShowRoomAiModal(false);
    setRoomAiCategory('');
    setRoomAiRefData('');
    setRoomAiCount(3);
    setValidationErrors({});
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // 실시간 대기 및 진행 모드 뷰 렌더러
  if (examMode === 'waiting' && currentExamRoom) {
    return (
      <div style={styles.examAppContainer}>
        <div style={styles.examWaitCard} className="card">
          <h2 style={styles.examWaitTitle}>실시간 대기실</h2>
          <div style={styles.examWaitInfoBox}>
            <p><strong>시험방 이름:</strong> {currentExamRoom.name}</p>
            <p><strong>설명:</strong> {currentExamRoom.description}</p>
            <p><strong>시험 시간:</strong> {new Date(currentExamRoom.testStartAt).toLocaleTimeString()} ~ {new Date(currentExamRoom.testEndAt).toLocaleTimeString()}</p>
          </div>

          <div style={styles.examWaitActions}>
            <button className="btn btn-secondary" onClick={exitExamMode}>대기실 나가기</button>
            <button className="btn btn-primary" onClick={handleStartExam}>시험 응시 및 입장하기</button>
          </div>
        </div>
      </div>
    );
  }

  if (examMode === 'testing' && currentExamRoom && examProblems.length > 0) {
    const curProb = examProblems[currentProblemIndex];
    return (
      <div style={styles.examSolveContainer}>
        <header style={styles.examHeader}>
          <div style={styles.examHeaderTitle}>
            <span>{currentExamRoom.name}</span>
            <span style={styles.badgeTimer}>{formatTime(timeLeft)} 남음</span>
          </div>
          <div style={styles.progressBarBg}>
            <div
              style={{
                ...styles.progressBarFill,
                width: `${((currentProblemIndex + 1) / examProblems.length) * 100}%`,
              }}
            ></div>
          </div>
        </header>

        <div style={styles.examSolveBody}>
          {/* 문제 표시판 */}
          <div className="card" style={styles.examProblemCard}>
            <span style={styles.badgeNum}>문제 {currentProblemIndex + 1} / {examProblems.length}</span>
            <h3 style={styles.examProblemName}>{curProb.name}</h3>
            <p style={styles.examProblemText}>{curProb.content}</p>

            <div className="input-group" style={{ marginTop: '2rem' }}>
              <label className="input-label">내 주관식 답안 입력</label>
              <input
                type="text"
                className="input-field"
                placeholder="답안을 입력하세요"
                value={examAnswers[curProb.id] || ''}
                onChange={(e) => handleMarkAnswer(curProb.id, e.target.value)}
              />
            </div>
          </div>

          {/* OMR 마킹판 리스트 */}
          <aside className="card" style={styles.omrSidebar}>
            <h4 style={styles.omrTitle}>OMR 마킹 현황</h4>
            <div style={styles.omrGrid}>
              {examProblems.map((p, idx) => (
                <button
                  key={p.id}
                  style={{
                    ...styles.omrBtn,
                    ...(currentProblemIndex === idx ? styles.omrBtnActive : {}),
                    ...(examAnswers[p.id] ? styles.omrBtnFilled : {}),
                  }}
                  onClick={() => setCurrentProblemIndex(idx)}
                >
                  {idx + 1}
                </button>
              ))}
            </div>
          </aside>
        </div>

        <footer style={styles.examFooter}>
          <button
            className="btn btn-secondary"
            disabled={currentProblemIndex === 0}
            onClick={() => setCurrentProblemIndex(prev => prev - 1)}
          >
            이전 문제
          </button>
          {currentProblemIndex < examProblems.length - 1 ? (
            <button
              className="btn btn-primary"
              onClick={() => setCurrentProblemIndex(prev => prev + 1)}
            >
              다음 문제
            </button>
          ) : (
            <button
              className="btn btn-danger"
              style={{ backgroundColor: '#10b981' }}
              onClick={handleSubmitExam}
            >
              답안 일괄 제출하기
            </button>
          )}
        </footer>
      </div>
    );
  }

  // 응시 대상자 검색/선택 (관리자 본인은 응시 대상에서 제외)
  const selectableMembers = allUsersList.filter(u => u.id !== user.id);
  const filteredMembers = selectableMembers.filter(u => {
    const q = memberSearchQuery.trim().toLowerCase();
    if (!q) return true;
    return (u.name || '').toLowerCase().includes(q) || (u.email || '').toLowerCase().includes(q);
  });
  const allFilteredSelected = filteredMembers.length > 0 && filteredMembers.every(u => selectedMembers.includes(u.id || ''));

  // 시험 시작~종료 소요 시간 표시용 (30분 단위 반올림 없이 그대로 표기)
  const examDurationLabel = (() => {
    if (!roomStart || !roomEnd) return null;
    const diffMs = new Date(roomEnd).getTime() - new Date(roomStart).getTime();
    if (isNaN(diffMs) || diffMs <= 0) return null;
    const totalMinutes = Math.round(diffMs / 60000);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    if (hours === 0) return `${minutes}분`;
    if (minutes === 0) return `${hours}시간`;
    return `${hours}시간 ${minutes}분`;
  })();

  const unreadNotiCount = notifications.filter(n => !n.isConfirmed).length;

  return (
    <div className="app-container">
      {/* 사이드바 메뉴 */}
      <aside className="sidebar-layout">
        <div 
          style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '0.5rem 0', marginBottom: '0.5rem', cursor: 'pointer' }}
          onClick={() => onBack('dashboard')}
        >
          <img src="/MoMoGo_Logo.png" alt="MoMoGo Logo" style={{ height: '32px', width: 'auto', objectFit: 'contain' }} />
          <h2 style={{ ...styles.sidebarTitle, marginBottom: 0 }}>MoMoGo</h2>
        </div>
        <div style={styles.spaceBadge}>{space.name}</div>
        <div style={styles.sidebarMenu}>
          <button
            style={{
              ...styles.menuBtn,
              ...(activeTab === 'problems' ? styles.menuActive : {}),
            }}
            onClick={() => setActiveTab('problems')}
          >
            싱글 문제은행
          </button>
          <button
            style={{
              ...styles.menuBtn,
              ...(activeTab === 'exams' ? styles.menuActive : {}),
            }}
            onClick={() => setActiveTab('exams')}
          >
            실시간 평가시험
          </button>
          <button
            style={{
              ...styles.menuBtn,
              ...(activeTab === 'dashboard' ? styles.menuActive : {}),
            }}
            onClick={() => setActiveTab('dashboard')}
          >
            성적 대시보드
          </button>
        </div>
        <button className="btn btn-secondary" style={styles.backBtn} onClick={() => onBack('dashboard')}>
          메인 페이지 이동
        </button>
      </aside>

      {/* 본문 레이아웃 */}
      <main className="main-layout">
        {/* 공간 페이지 상단바 (요구사항 2, 2-1, 14) */}
        <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem', paddingBottom: '1rem', borderBottom: '1px solid #eaecf0', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#1d2939', margin: 0 }}>{space.name}</h1>
            <p style={{ fontSize: '0.875rem', color: '#667085', margin: '4px 0 0 0' }}>{space.description}</p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <button
                className="btn btn-secondary"
                style={styles.notiTriggerBtn}
                onClick={() => { setShowNotifications(!showNotifications); setShowUserMenu(false); }}
            >
              🔔 알림
              {unreadNotiCount > 0 && <span style={styles.notiCountBadge}>{unreadNotiCount}</span>}
            </button>

            <div style={{ position: 'relative' }}>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.625rem',
                  padding: '0.4rem 0.75rem',
                  borderRadius: '24px',
                  backgroundColor: '#ffffff',
                  border: '1px solid #eaecf0',
                  cursor: 'pointer',
                  boxShadow: '0 1px 3px rgba(16, 24, 40, 0.05)',
                }}
                onClick={() => { setShowUserMenu(!showUserMenu); setShowNotifications(false); }}
              >
                <img 
                  src={user.profileImageUrl || '/basic.png'}
                  alt={user.name}
                  style={{ width: '32px', height: '32px', borderRadius: '50%', objectFit: 'cover', border: '1px solid #eaecf0' }}
                  onError={handleAvatarError}
                />
                <span style={{ fontWeight: 600, fontSize: '0.875rem', color: '#1d2939' }}>{user.name}</span>
                {user.role && (
                  <span className={`badge ${user.role === 'SUPER_ADMIN' ? 'badge-danger' : user.role === 'ADMIN' ? 'badge-success' : 'badge-info'}`}>
                    {user.role}
                  </span>
                )}
              </div>

              {showUserMenu && (
                <div style={{
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
                }} className="card">
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', paddingBottom: '8px' }}>
                    <img 
                      src={user.profileImageUrl || '/basic.png'}
                      alt={user.name}
                      style={{ width: '40px', height: '40px', borderRadius: '50%', objectFit: 'cover', border: '1px solid #eaecf0' }}
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
                    style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: 'none', backgroundColor: 'transparent', textAlign: 'left', fontSize: '0.85rem', fontWeight: 600, color: '#344054', cursor: 'pointer' }}
                    onClick={() => { onBack('mypage'); setShowUserMenu(false); }}
                  >
                    👤 메인 마이페이지 이동
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

        {/* 싱글 문제은행 탭 */}
        {activeTab === 'problems' && (
          <div>
            <div style={styles.headerRow}>
              <h2>문제 은행 목록</h2>
              {user.role === 'ADMIN' && (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button className="btn btn-secondary" onClick={() => setShowCreateAIProblemModal(true)}>
                    AI 자동 출제
                  </button>
                  <button className="btn btn-primary" onClick={() => setShowCreateProblemModal(true)}>
                    문제 직접 등록
                  </button>
                </div>
              )}
            </div>

            {/* 필터 영역 */}
            <div className="card" style={styles.filterCard}>
              <div className="input-group" style={{ margin: 0, flex: 1 }}>
                <label className="input-label">카테고리 필터</label>
                <select
                  className="input-field"
                  value={selectedCategory}
                  onChange={(e) => setSelectedCategory(e.target.value)}
                >
                  <option value="">전체 카테고리</option>
                  {categories.map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* 문제 리스트 */}
            {loading ? (
              <p>문제를 불러오고 있습니다...</p>
            ) : (
              <div style={styles.problemGrid}>
                {problems.map(prob => (
                  <div
                    key={prob.id}
                    className="card"
                    style={{ ...styles.problemCard, opacity: prob.isSolved ? 0.75 : 1 }}
                  >
                    <div>
                      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem', flexWrap: 'wrap' }}>
                        <span className="badge badge-info">
                          {prob.categoryName || '미분류'}
                        </span>
                        <span className={prob.isSolved ? 'badge badge-success' : 'badge'} style={prob.isSolved ? undefined : styles.badgeUnsolved}>
                          {prob.isSolved ? '풀이완료' : '미풀이'}
                        </span>
                      </div>
                      <h3 style={styles.problemTitleText}>{prob.name}</h3>
                      <p style={styles.problemContentText}>{prob.content}</p>
                    </div>
                    <div style={styles.problemCardActions}>
                      <button
                        className="btn btn-primary"
                        style={styles.cardBtn}
                        onClick={() => {
                          setSolvingProblem(prob);
                          setSolveUserAnswer('');
                          setSolveResult(null);
                          setValidationErrors({});
                          setShowSolveModal(true);
                        }}
                      >
                        문제 풀기
                      </button>
                      {user.role === 'ADMIN' && (
                        <div style={{ display: 'flex', gap: '0.25rem' }}>
                          <button
                            className="btn btn-secondary"
                            style={styles.smallIconBtn}
                            onClick={() => handleOpenEditProblem(prob)}
                          >
                            편집
                          </button>
                          <button
                            className="btn btn-danger"
                            style={styles.smallIconBtn}
                            onClick={() => handleDeleteProblem(prob.id)}
                          >
                            삭제
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
                {problems.length === 0 && (
                  <p style={{ gridColumn: '1/-1', textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                    등록된 연습문제가 존재하지 않습니다.
                  </p>
                )}
              </div>
            )}
          </div>
        )}

        {/* 실시간 평가시험 탭 */}
        {activeTab === 'exams' && (
          <div>
            <div style={styles.headerRow}>
              <h2>평가 시험 관리</h2>
              {user.role === 'ADMIN' && (
                <button className="btn btn-primary" onClick={() => setShowCreateRoomModal(true)}>
                  평가 시험방 개설
                </button>
              )}
            </div>

            <div style={styles.examGrid}>
              {rooms.map(room => {
                // 이미 답안을 제출한 시험인지 여부 (재입장/재응시 차단용)
                const alreadySubmitted = examsHistory.some(ex => ex.roomId === room.id);
                return (
                  <div key={room.id} className="card" style={styles.examCard}>
                    <div>
                      <h3 style={styles.examTitleText}>{room.name}</h3>
                      <p style={styles.examDescText}>{room.description}</p>
                      <div style={styles.examMetaRow}>
                        <span className="badge badge-info">
                          시작: {new Date(room.testStartAt).toLocaleString()}
                        </span>
                        <span className="badge badge-info">
                          종료: {new Date(room.testEndAt).toLocaleString()}
                        </span>
                      </div>
                    </div>
                    <div style={styles.examCardActions}>
                      {user.role === 'ADMIN' ? (
                        <div style={{ display: 'flex', gap: '0.5rem', width: '100%' }}>
                          {!room.isEnded ? (
                            <button
                              className="btn btn-primary"
                              style={{ flex: 1 }}
                              onClick={() => handleOpenGrading(room.id)}
                            >
                              채점하기
                            </button>
                          ) : (
                            <>
                              <button
                                className="btn btn-secondary"
                                style={{ flex: 1 }}
                                onClick={() => handleOpenReport(room.id)}
                              >
                                결과 리포트
                              </button>
                              <button
                                className="btn btn-danger"
                                style={{ flex: 1, backgroundColor: '#10b981' }}
                                onClick={() => handleDownloadPdf(room.id)}
                              >
                                PDF 다운
                              </button>
                            </>
                          )}
                        </div>
                      ) : alreadySubmitted && room.isEnded ? (
                        // 채점 확정(방 마감) 전에는 정답/해설을 보여주지 않는다.
                        // 같은 방에서 다른 응시자가 아직 풀고 있을 수 있어, 먼저 제출한 사람이
                        // 결과를 미리 보면 옆에서 컨닝하는 데 악용될 수 있기 때문.
                        <button
                          className="btn btn-secondary"
                          style={{ width: '100%' }}
                          onClick={() => handleViewExamDetail(room.id)}
                        >
                          응시 완료 (결과 보기)
                        </button>
                      ) : alreadySubmitted ? (
                        <button className="btn btn-secondary" style={{ width: '100%' }} disabled>
                          제출 완료 (채점 대기 중)
                        </button>
                      ) : room.isEnded ? (
                        <button className="btn btn-secondary" style={{ width: '100%' }} disabled>
                          종료된 시험입니다
                        </button>
                      ) : (
                        <button
                          className="btn btn-primary"
                          style={{ width: '100%' }}
                          onClick={() => handleEnterWaitRoom(room)}
                        >
                          대기실 입장하기
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
              {rooms.length === 0 && (
                <p style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)', gridColumn: '1/-1' }}>
                  등록된 평가 시험 내역이 존재하지 않습니다.
                </p>
              )}
            </div>
          </div>
        )}

        {/* 성적 대시보드 탭 */}
        {activeTab === 'dashboard' && (
          <div>
            <h2>
              내 학습 대시보드
            </h2>

            {/* 카드 요약 정보 (규칙 3.6 개인 대시보드 반영) */}
            {summary && (
              <div style={{ ...styles.summaryGrid, gridTemplateColumns: 'repeat(4, 1fr)' }}>
                <div className="card" style={styles.summaryCard}>
                  <span style={styles.summaryLabel}>시도한 문제 수</span>
                  <h3 style={styles.summaryVal}>{history.length}개</h3>
                </div>
                <div className="card" style={styles.summaryCard}>
                  <span style={styles.summaryLabel}>정답 맞춘 문제 수</span>
                  <h3 style={styles.summaryVal}>{history.filter(h => h.isSolved).length}개</h3>
                </div>
                <div className="card" style={styles.summaryCard}>
                  <span style={styles.summaryLabel}>평균 정답률</span>
                  <h3 style={styles.summaryVal}>{summary.averageCorrectRate.toFixed(1)}%</h3>
                </div>
                <div className="card" style={styles.summaryCard}>
                  <span style={styles.summaryLabel}>평균 오답률</span>
                  <h3 style={styles.summaryVal}>{(100 - summary.averageCorrectRate).toFixed(1)}%</h3>
                </div>
              </div>
            )}

            {/* 공간 대시보드: 랭킹 표시판 (규칙 3.6 공간 대시보드 반영) */}
            <div className="card" style={{ marginTop: '0.5rem', marginBottom: '2rem', padding: '1.5rem' }}>
              <h3 style={{ ...styles.dashboardSectionTitle, borderBottom: '2px solid var(--primary-border)', paddingBottom: '0.5rem', marginBottom: '1rem' }}>
                공간 랭킹 대시보드
              </h3>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-sub)', marginBottom: '1.25rem' }}>
                공간 내부에 등록되어 있는 문제를 푼 유저들의 이번 주(월요일~현재) 실시간 랭킹입니다.
              </p>
              <div style={styles.tableScrollFixed}>
                <table style={styles.table}>
                  <thead style={styles.stickyThead}>
                    <tr>
                      <th style={{ width: '80px' }}>순위</th>
                      <th>이름</th>
                      <th>해결한 문제 수</th>
                    </tr>
                  </thead>
                  <tbody>
                    {ranking.map((r, i) => {
                      const isMe = r.userId === user.id;
                      const medal = i === 0 ? ' 🥇' : i === 1 ? ' 🥈' : i === 2 ? ' 🥉' : '';
                      return (
                        <tr
                          key={r.userId}
                          style={isMe ? { backgroundColor: '#f0f2ff', borderLeft: '4px solid var(--primary)' } : undefined}
                        >
                          <td><strong>{i + 1}등{medal}</strong></td>
                          <td>
                            <div style={styles.nameWithAvatar}>
                              <img src={r.profileImageUrl || '/basic.png'} alt={r.userName} style={styles.avatarImg} onError={handleAvatarError} />
                              <span>{r.userName}{isMe ? ' (나)' : ''}</span>
                            </div>
                          </td>
                          <td>{r.solvedCount}개</td>
                        </tr>
                      );
                    })}
                    {ranking.length === 0 && (
                      <tr>
                        <td colSpan={3} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                          이번 주 랭킹 데이터가 존재하지 않습니다.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div style={styles.dashboardSplit}>
              {/* 싱글모드 풀이 이력 */}
              <div className="card" style={styles.dashboardSectionCard}>
                <h3 style={styles.dashboardSectionTitle}>싱글모드 오답 학습 이력</h3>
                <div style={styles.tableScrollFixed}>
                  <table style={styles.table}>
                    <thead style={styles.stickyThead}>
                      <tr>
                        <th>문제명</th>
                        <th>카테고리</th>
                        <th>결과</th>
                        <th>제출 답안</th>
                      </tr>
                    </thead>
                    <tbody>
                      {history.map((h, i) => (
                        <tr key={i}>
                          <td>{h.problemName}</td>
                          <td>{h.categoryName}</td>
                          <td>
                            <span className={`badge ${h.isSolved ? 'badge-success' : 'badge-danger'}`}>
                              {h.isSolved ? '정답' : '오답'}
                            </span>
                          </td>
                          <td>{h.userAnswer}</td>
                        </tr>
                      ))}
                      {history.length === 0 && (
                        <tr>
                          <td colSpan={4} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                            기록된 이력이 존재하지 않습니다.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* 평가시험 이력 */}
              <div className="card" style={styles.dashboardSectionCard}>
                <h3 style={styles.dashboardSectionTitle}>정기 평가시험 응시 결과</h3>
                <div style={styles.tableScrollFixed}>
                  <table style={styles.table}>
                    <thead style={styles.stickyThead}>
                      <tr>
                        <th>시험명</th>
                        <th>취득 점수</th>
                        <th>상세</th>
                      </tr>
                    </thead>
                    <tbody>
                      {examsHistory.map((ex, i) => (
                        <tr key={i}>
                          <td>{ex.roomName}</td>
                          <td><strong>{ex.score}점</strong></td>
                          <td>
                            <button
                              className="btn btn-secondary"
                              style={{ padding: '0.25rem 0.5rem', fontSize: '0.8rem' }}
                              onClick={() => handleViewExamDetail(ex.roomId)}
                            >
                              리뷰 보기
                            </button>
                          </td>
                        </tr>
                      ))}
                      {examsHistory.length === 0 && (
                        <tr>
                          <td colSpan={3} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                            기록된 시험 이력이 존재하지 않습니다.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* 수동 문제 생성 모달 */}
      {showCreateProblemModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>{editingProblem ? '연습 문제 수정하기' : '새 연습 문제 직접 등록'}</h3>
            <form onSubmit={handleSaveProblem} style={styles.form} noValidate>
              <div className="input-group">
                <label className="input-label">카테고리</label>
                <select
                  className="input-field"
                  value={probCategory}
                  onChange={(e) => {
                    setProbCategory(e.target.value);
                    if (validationErrors.probCategory) {
                      setValidationErrors(prev => ({ ...prev, probCategory: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.probCategory ? '#ef4444' : undefined,
                  }}
                >
                  <option value="">카테고리 선택</option>
                  {categories.map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
                {validationErrors.probCategory && (
                  <span style={styles.errorText}>{validationErrors.probCategory}</span>
                )}
                {categories.length === 0 && (
                  <span style={styles.hintText}>
                    선택 가능한 카테고리가 없습니다. 슈퍼관리자에게 카테고리 생성을 요청해 주세요.
                  </span>
                )}
              </div>

              <div className="input-group">
                <label className="input-label">문제 이름</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="예: 미적분 기초 계산법"
                  value={probName}
                  onChange={(e) => {
                    setProbName(e.target.value);
                    if (validationErrors.probName) {
                      setValidationErrors(prev => ({ ...prev, probName: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.probName ? '#ef4444' : undefined,
                  }}
                />
                {validationErrors.probName && (
                  <span style={styles.errorText}>{validationErrors.probName}</span>
                )}
              </div>

              <div className="input-group">
                <label className="input-label">문제 상세 내용</label>
                <textarea
                  className="input-field"
                  rows={4}
                  placeholder="지문과 물음 내용을 명시해 주세요"
                  value={probContent}
                  onChange={(e) => {
                    setProbContent(e.target.value);
                    if (validationErrors.probContent) {
                      setValidationErrors(prev => ({ ...prev, probContent: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.probContent ? '#ef4444' : undefined,
                    height: '100px',
                  }}
                />
                {validationErrors.probContent && (
                  <span style={styles.errorText}>{validationErrors.probContent}</span>
                )}
              </div>

              <div className="input-group">
                <label className="input-label">정답 입력</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="공백 없이 해답 값을 작성하세요"
                  value={probAnswer}
                  onChange={(e) => {
                    setProbAnswer(e.target.value);
                    if (validationErrors.probAnswer) {
                      setValidationErrors(prev => ({ ...prev, probAnswer: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.probAnswer ? '#ef4444' : undefined,
                  }}
                />
                {validationErrors.probAnswer && (
                  <span style={styles.errorText}>{validationErrors.probAnswer}</span>
                )}
              </div>

              <div className="input-group">
                <label className="input-label">풀이 해설</label>
                <textarea
                  className="input-field"
                  rows={3}
                  placeholder="오답 확인용 풀이 설명을 작성하세요"
                  value={probExplanation}
                  onChange={(e) => {
                    setProbExplanation(e.target.value);
                    if (validationErrors.probExplanation) {
                      setValidationErrors(prev => ({ ...prev, probExplanation: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.probExplanation ? '#ef4444' : undefined,
                    height: '80px',
                  }}
                />
                {validationErrors.probExplanation && (
                  <span style={styles.errorText}>{validationErrors.probExplanation}</span>
                )}
              </div>

              <div style={styles.modalActions}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => {
                    setShowCreateProblemModal(false);
                    setEditingProblem(null);
                    resetProblemForm();
                  }}
                >
                  취소
                </button>
                <button type="submit" className="btn btn-primary">
                  문제 등록 완료
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* AI 문제 자동 생성 모달 */}
      {showCreateAIProblemModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>AI 자동 문제 출제기</h3>
            <form onSubmit={handleGenerateAIProblems} style={styles.form} noValidate>
              <div className="input-group">
                <label className="input-label">타겟 카테고리</label>
                <select
                  className="input-field"
                  value={aiCategory}
                  onChange={(e) => {
                    setAiCategory(e.target.value);
                    if (validationErrors.aiCategory) {
                      setValidationErrors(prev => ({ ...prev, aiCategory: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.aiCategory ? '#ef4444' : undefined,
                  }}
                >
                  <option value="">출제 카테고리 선택</option>
                  {categories.map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
                {validationErrors.aiCategory && (
                  <span style={styles.errorText}>{validationErrors.aiCategory}</span>
                )}
                {categories.length === 0 && (
                  <span style={styles.hintText}>
                    선택 가능한 카테고리가 없습니다. 슈퍼관리자에게 카테고리 생성을 요청해 주세요.
                  </span>
                )}
              </div>

              <div className="input-group">
                <label className="input-label">참고 학습자료 (자료 제공)</label>
                <textarea
                  className="input-field"
                  rows={5}
                  placeholder="AI가 분석하여 출제할 요약 노트, 문서 텍스트 등을 복사 붙여넣기 해주세요"
                  value={aiRefData}
                  onChange={(e) => {
                    setAiRefData(e.target.value);
                    if (validationErrors.aiRefData) {
                      setValidationErrors(prev => ({ ...prev, aiRefData: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.aiRefData ? '#ef4444' : undefined,
                    height: '140px',
                  }}
                />
                {validationErrors.aiRefData && (
                  <span style={styles.errorText}>{validationErrors.aiRefData}</span>
                )}
              </div>

              <div className="input-group">
                <label className="input-label">출제 문항 수</label>
                <select
                  className="input-field"
                  value={aiCount}
                  onChange={(e) => setAiCount(Number(e.target.value))}
                >
                  <option value={1}>1개 문제</option>
                  <option value={3}>3개 문제</option>
                  <option value={5}>5개 문제</option>
                </select>
              </div>

              <div style={styles.modalActions}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => {
                    setShowCreateAIProblemModal(false);
                    setAiRefData('');
                    setAiCategory('');
                    setValidationErrors({});
                  }}
                  disabled={loading}
                >
                  취소
                </button>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading ? 'AI 출제 분석 중...' : 'AI 출제 생성'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 문제 풀기 모달 */}
      {showSolveModal && solvingProblem && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>
              연습 문제 풀기
            </h3>
            <div style={styles.solvingCard}>
              <span className="badge badge-info">{solvingProblem.categoryName || '미분류'}</span>
              <h4 style={{ margin: '0.5rem 0', color: '#1d2939' }}>{solvingProblem.name}</h4>
              <p style={{ color: '#475467', lineHeight: 1.5, background: '#f9fafb', padding: '1rem', borderRadius: '8px' }}>
                {solvingProblem.content}
              </p>
            </div>

            <form onSubmit={handleSolveProblem} style={styles.form} noValidate>
              {!solveResult ? (
                <>
                  <div className="input-group">
                    <label className="input-label">정답 입력</label>
                    <input
                      type="text"
                      className="input-field"
                      placeholder="정답을 기입하세요."
                      value={solveUserAnswer}
                      onChange={(e) => {
                        setSolveUserAnswer(e.target.value);
                        if (validationErrors.solveUserAnswer) {
                          setValidationErrors({});
                        }
                      }}
                      style={{
                        borderColor: validationErrors.solveUserAnswer ? '#ef4444' : undefined,
                      }}
                    />
                    {validationErrors.solveUserAnswer && (
                      <span style={styles.errorText}>{validationErrors.solveUserAnswer}</span>
                    )}
                  </div>

                  <div style={styles.modalActions}>
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => setShowSolveModal(false)}
                    >
                      닫기
                    </button>
                    <button type="submit" className="btn btn-primary">
                      정답 제출
                    </button>
                  </div>
                </>
              ) : (
                <div style={styles.resultBox}>
                  <div
                    style={{
                      ...styles.resultTitle,
                      color: solveResult.correct ? '#10b981' : '#ef4444',
                    }}
                  >
                    {solveResult.correct ? '정답입니다' : '오답입니다'}
                  </div>
                  <p><strong>공식 정답:</strong> {solveResult.answer}</p>
                  <p style={{ marginTop: '0.5rem' }}><strong>상세 설명:</strong> {solveResult.exp}</p>

                  <div style={styles.modalActions}>
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => {
                        setShowSolveModal(false);
                        setSolveResult(null);
                      }}
                    >
                      확인 후 닫기
                    </button>
                  </div>
                </div>
              )}
            </form>
          </div>
        </div>
      )}

      {/* 평가 시험방 개설 모달 (2단계 마법사) */}
      {showCreateRoomModal && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '640px' }}>
            <div style={styles.wizardHeader}>
              <h3 style={styles.modalTitle}>정기 평가 시험방 개설</h3>
              <div style={styles.wizardSteps}>
                <span style={{ ...styles.wizardStepDot, ...(roomWizardStep >= 1 ? styles.wizardStepDotActive : {}) }}>1</span>
                <span style={styles.wizardStepLine}></span>
                <span style={{ ...styles.wizardStepDot, ...(roomWizardStep >= 2 ? styles.wizardStepDotActive : {}) }}>2</span>
              </div>
            </div>
            <p style={styles.wizardStepLabel}>
              {roomWizardStep === 1 ? '1단계 · 기본 정보 및 응시 대상자' : '2단계 · 문제 출제 (수동 + AI 혼합 가능)'}
            </p>
            <form onSubmit={handleCreateRoom} style={styles.form} noValidate>
              {roomWizardStep === 1 && (
              <>
              <div className="input-group">
                <label className="input-label">시험 이름</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="예: 2026 1학기 수학 중간고사"
                  value={roomName}
                  onChange={(e) => {
                    setRoomName(e.target.value);
                    if (validationErrors.roomName) {
                      setValidationErrors(prev => ({ ...prev, roomName: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.roomName ? '#ef4444' : undefined,
                  }}
                />
                {validationErrors.roomName && (
                  <span style={styles.errorText}>{validationErrors.roomName}</span>
                )}
              </div>

              <div className="input-group">
                <label className="input-label">시험 설명</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="범위 및 응시 규칙"
                  value={roomDesc}
                  onChange={(e) => setRoomDesc(e.target.value)}
                />
              </div>

              {/* 시험 일정 설정 */}
              <div className="input-group">
                <label className="input-label">시험 일정</label>
                <div style={styles.scheduleCard}>
                  <div style={styles.scheduleField}>
                    <span style={{ ...styles.scheduleDot, backgroundColor: 'var(--success)' }}></span>
                    <div style={styles.scheduleFieldBody}>
                      <span style={styles.scheduleFieldLabel}>시작</span>
                      <input
                        type="datetime-local"
                        style={{
                          ...styles.scheduleInput,
                          color: validationErrors.roomStart ? 'var(--danger)' : 'var(--text-main)',
                        }}
                        value={roomStart}
                        onChange={(e) => {
                          setRoomStart(e.target.value);
                          if (validationErrors.roomStart) {
                            setValidationErrors(prev => ({ ...prev, roomStart: '' }));
                          }
                        }}
                      />
                    </div>
                  </div>
                  <span style={styles.scheduleArrow}>→</span>
                  <div style={styles.scheduleField}>
                    <span style={{ ...styles.scheduleDot, backgroundColor: 'var(--danger)' }}></span>
                    <div style={styles.scheduleFieldBody}>
                      <span style={styles.scheduleFieldLabel}>종료</span>
                      <input
                        type="datetime-local"
                        style={{
                          ...styles.scheduleInput,
                          color: validationErrors.roomEnd ? 'var(--danger)' : 'var(--text-main)',
                        }}
                        value={roomEnd}
                        onChange={(e) => {
                          setRoomEnd(e.target.value);
                          if (validationErrors.roomEnd) {
                            setValidationErrors(prev => ({ ...prev, roomEnd: '' }));
                          }
                        }}
                      />
                    </div>
                  </div>
                  {examDurationLabel && (
                    <span style={styles.scheduleDuration}>⏱ {examDurationLabel}</span>
                  )}
                </div>
                {(validationErrors.roomStart || validationErrors.roomEnd) && (
                  <span style={styles.errorText}>{validationErrors.roomStart || validationErrors.roomEnd}</span>
                )}
              </div>

              {/* 응시 대상자 다중 선택 */}
              <div className="input-group">
                <div style={styles.examineeHeader}>
                  <label className="input-label" style={{ marginBottom: 0 }}>응시 대상자 지정</label>
                  <span style={styles.examineeCount}>{selectedMembers.length}명 선택됨</span>
                </div>

                <div style={styles.examineeToolbar}>
                  <input
                    type="text"
                    className="input-field"
                    placeholder="이름 또는 이메일로 검색"
                    style={styles.examineeSearchInput}
                    value={memberSearchQuery}
                    onChange={(e) => setMemberSearchQuery(e.target.value)}
                  />
                  <button
                    type="button"
                    className="btn btn-secondary"
                    style={styles.examineeToggleAllBtn}
                    onClick={() => {
                      const filteredIds = filteredMembers.map(u => u.id || '');
                      if (allFilteredSelected) {
                        setSelectedMembers(prev => prev.filter(id => !filteredIds.includes(id)));
                      } else {
                        setSelectedMembers(prev => Array.from(new Set([...prev, ...filteredIds])));
                      }
                      if (validationErrors.selectedMembers) {
                        setValidationErrors(prev => ({ ...prev, selectedMembers: '' }));
                      }
                    }}
                  >
                    {allFilteredSelected ? '전체 해제' : '전체 선택'}
                  </button>
                </div>

                <div style={styles.examineeList}>
                  {filteredMembers.map(u => {
                    const uid = u.id || '';
                    const selected = selectedMembers.includes(uid);
                    return (
                      <div
                        key={uid}
                        onClick={() => {
                          setSelectedMembers(prev => selected ? prev.filter(id => id !== uid) : [...prev, uid]);
                          if (validationErrors.selectedMembers) {
                            setValidationErrors(prev => ({ ...prev, selectedMembers: '' }));
                          }
                        }}
                        style={{
                          ...styles.examineeItem,
                          ...(selected ? styles.examineeItemSelected : {}),
                        }}
                      >
                        <img
                          src={u.profileImageUrl || '/basic.png'}
                          alt={u.name || '?'}
                          style={{
                            width: '32px',
                            height: '32px',
                            borderRadius: '50%',
                            objectFit: 'cover',
                            flexShrink: 0,
                            border: selected ? '2px solid var(--primary)' : '1px solid var(--border-color)',
                          }}
                          onError={handleAvatarError}
                        />
                        <div style={styles.examineeInfo}>
                          <span style={styles.examineeName}>{u.name}</span>
                          <span style={styles.examineeEmail}>{u.email}</span>
                        </div>
                        <span style={{ ...styles.examineeCheck, opacity: selected ? 1 : 0 }}>✓</span>
                      </div>
                    );
                  })}
                  {filteredMembers.length === 0 && (
                    <p style={styles.examineeEmptyText}>
                      {selectableMembers.length === 0 ? '공간에 다른 유저가 없습니다.' : '검색 결과가 없습니다.'}
                    </p>
                  )}
                </div>

                {validationErrors.selectedMembers && (
                  <span style={styles.errorText}>{validationErrors.selectedMembers}</span>
                )}
              </div>
              </>
              )}

              {roomWizardStep === 2 && (
              <>
              {/* 출제 문제 등록 (수동 입력 + AI 자동 출제 혼합 가능) */}
              <div className="input-group">
                <div style={styles.examineeHeader}>
                  <label className="input-label" style={{ marginBottom: 0 }}>시험 출제 문제 목록</label>
                  <span style={styles.examineeCount}>{examProblemsList.length}문항</span>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {examProblemsList.map((item, idx) => (
                    <div key={idx} className="card" style={{ padding: '0.75rem', border: '1px solid var(--border-color)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
                        <p style={{ fontWeight: 600, fontSize: '0.8rem', color: 'var(--primary)' }}>
                          문항 {idx + 1}
                        </p>
                        {examProblemsList.length > 1 && (
                          <button
                            type="button"
                            onClick={() => setExamProblemsList(prev => prev.filter((_, i) => i !== idx))}
                            style={styles.problemRemoveBtn}
                            aria-label="문항 삭제"
                          >
                            ✕
                          </button>
                        )}
                      </div>
                      <select
                        className="input-field"
                        style={{ padding: '0.4rem', fontSize: '0.85rem', marginBottom: '0.25rem' }}
                        value={item.categoryId}
                        onChange={(e) => {
                          const newList = [...examProblemsList];
                          newList[idx].categoryId = e.target.value;
                          setExamProblemsList(newList);
                        }}
                      >
                        <option value="">카테고리 선택</option>
                        {categories.map(c => (
                          <option key={c.id} value={c.id}>{c.name}</option>
                        ))}
                      </select>
                      <input
                        type="text"
                        placeholder="문제명"
                        className="input-field"
                        style={{ padding: '0.4rem', fontSize: '0.85rem', marginBottom: '0.25rem' }}
                        value={item.name}
                        onChange={(e) => {
                          const newList = [...examProblemsList];
                          newList[idx].name = e.target.value;
                          setExamProblemsList(newList);
                        }}
                      />
                      <input
                        type="text"
                        placeholder="내용"
                        className="input-field"
                        style={{ padding: '0.4rem', fontSize: '0.85rem', marginBottom: '0.25rem' }}
                        value={item.content}
                        onChange={(e) => {
                          const newList = [...examProblemsList];
                          newList[idx].content = e.target.value;
                          setExamProblemsList(newList);
                        }}
                      />
                      <div style={{ display: 'flex', gap: '0.25rem' }}>
                        <input
                          type="text"
                          placeholder="정답"
                          className="input-field"
                          style={{ padding: '0.4rem', fontSize: '0.85rem', flex: 1 }}
                          value={item.correctAnswer}
                          onChange={(e) => {
                            const newList = [...examProblemsList];
                            newList[idx].correctAnswer = e.target.value;
                            setExamProblemsList(newList);
                          }}
                        />
                        <input
                          type="text"
                          placeholder="해설"
                          className="input-field"
                          style={{ padding: '0.4rem', fontSize: '0.85rem', flex: 1 }}
                          value={item.explanation}
                          onChange={(e) => {
                            const newList = [...examProblemsList];
                            newList[idx].explanation = e.target.value;
                            setExamProblemsList(newList);
                          }}
                        />
                      </div>
                    </div>
                  ))}
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button
                      type="button"
                      className="btn btn-secondary"
                      style={{ padding: '0.5rem 0.75rem', fontSize: '0.8rem', flex: 1 }}
                      onClick={() =>
                        setExamProblemsList(prev => [
                          ...prev,
                          { categoryId: '', problemOrder: prev.length + 1, name: '', content: '', explanation: '', correctAnswer: '' },
                        ])
                      }
                    >
                      + 문제 직접 추가
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary"
                      style={{ padding: '0.5rem 0.75rem', fontSize: '0.8rem', flex: 1, color: 'var(--primary)', borderColor: 'var(--primary-border)' }}
                      onClick={() => setShowRoomAiModal(true)}
                    >
                      ✨ AI 자동 출제
                    </button>
                  </div>
                </div>
              </div>
              </>
              )}

              <div style={styles.modalActions}>
                {roomWizardStep === 2 && (
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setRoomWizardStep(1)}
                  >
                    이전
                  </button>
                )}
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => {
                    setShowCreateRoomModal(false);
                    resetRoomForm();
                  }}
                >
                  취소
                </button>
                <button type="submit" className="btn btn-primary">
                  {roomWizardStep === 1 ? '다음' : '개설 및 등록 완료'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 2단계 내 AI 자동 출제 모달 (시험방 문제) */}
      {showRoomAiModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>AI 자동 문제 출제기</h3>
            <form onSubmit={handleGenerateRoomAiProblems} style={styles.form} noValidate>
              <div className="input-group">
                <label className="input-label">타겟 카테고리</label>
                <select
                  className="input-field"
                  value={roomAiCategory}
                  onChange={(e) => {
                    setRoomAiCategory(e.target.value);
                    if (validationErrors.roomAiCategory) {
                      setValidationErrors(prev => ({ ...prev, roomAiCategory: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.roomAiCategory ? '#ef4444' : undefined,
                  }}
                >
                  <option value="">출제 카테고리 선택</option>
                  {categories.map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
                {validationErrors.roomAiCategory && (
                  <span style={styles.errorText}>{validationErrors.roomAiCategory}</span>
                )}
                {categories.length === 0 && (
                  <span style={styles.hintText}>
                    선택 가능한 카테고리가 없습니다. 슈퍼관리자에게 카테고리 생성을 요청해 주세요.
                  </span>
                )}
              </div>

              <div className="input-group">
                <label className="input-label">참고 학습자료 (자료 제공)</label>
                <textarea
                  className="input-field"
                  rows={5}
                  placeholder="AI가 분석하여 출제할 요약 노트, 문서 텍스트 등을 복사 붙여넣기 해주세요"
                  value={roomAiRefData}
                  onChange={(e) => {
                    setRoomAiRefData(e.target.value);
                    if (validationErrors.roomAiRefData) {
                      setValidationErrors(prev => ({ ...prev, roomAiRefData: '' }));
                    }
                  }}
                  style={{
                    borderColor: validationErrors.roomAiRefData ? '#ef4444' : undefined,
                    height: '140px',
                  }}
                />
                {validationErrors.roomAiRefData && (
                  <span style={styles.errorText}>{validationErrors.roomAiRefData}</span>
                )}
              </div>

              <div className="input-group">
                <label className="input-label">출제 문항 수</label>
                <select
                  className="input-field"
                  value={roomAiCount}
                  onChange={(e) => setRoomAiCount(Number(e.target.value))}
                >
                  <option value={1}>1개 문제</option>
                  <option value={3}>3개 문제</option>
                  <option value={5}>5개 문제</option>
                </select>
              </div>

              <p style={styles.hintText}>
                생성된 문제는 바로 저장되지 않고 목록에 미리보기로 추가됩니다. 내용을 검토/수정한 뒤 최종 개설해 주세요.
              </p>

              <div style={styles.modalActions}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => {
                    setShowRoomAiModal(false);
                    setRoomAiRefData('');
                    setRoomAiCategory('');
                  }}
                  disabled={roomAiLoading}
                >
                  취소
                </button>
                <button type="submit" className="btn btn-primary" disabled={roomAiLoading}>
                  {roomAiLoading ? 'AI 출제 분석 중...' : 'AI 출제 생성'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 리포트 결과 모달 */}
      {showReportModal && reportData && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 style={styles.modalTitle}>시험 평가 리포트</h3>
            <p style={styles.modalDesc}>시험명: {reportData.roomName}</p>

            <div style={styles.tableScroll}>
              <table style={styles.table}>
                <thead>
                  <tr>
                    <th>학생명</th>
                    <th>상태</th>
                    <th>점수</th>
                  </tr>
                </thead>
                <tbody>
                  {reportData.takerGrades.map((grade, i) => (
                    <tr key={i}>
                      <td>
                        <div style={styles.nameWithAvatar}>
                          <img src={grade.profileImageUrl || '/basic.png'} alt={grade.name} style={styles.avatarImg} onError={handleAvatarError} />
                          <span>{grade.name}</span>
                        </div>
                      </td>
                      <td>
                        <span className={`badge ${grade.isAttended ? 'badge-success' : 'badge-danger'}`}>
                          {grade.isAttended ? '응시 완료' : '결시'}
                        </span>
                      </td>
                      <td><strong>{grade.isAttended ? `${grade.score}점` : '-'}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div style={styles.modalActions}>
              <button className="btn btn-secondary" onClick={() => setShowReportModal(false)}>
                닫기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 채점 검토 모달 - AI 채점 실행 + 답안별 수동 채점 확인 후 최종 확정 */}
      {showGradingModal && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '900px' }}>
            <h3 style={styles.modalTitle}>채점하기</h3>
            {gradingData && <p style={styles.modalDesc}>시험명: {gradingData.roomName}</p>}

            {gradingData?.isAiGradingInProgress && (
              <p style={{ ...styles.hintText, color: 'var(--primary)' }}>
                AI 채점이 진행 중입니다. 잠시 후 "새로고침"을 눌러 결과를 확인해 주세요.
              </p>
            )}

            <p style={styles.hintText}>
              정오 표시가 없는(미채점) 답안은 확정 시 제출 답안과 모범 정답의 글자가 완전히 같은 경우에만 자동으로 정답 처리됩니다.
              주관식 등 표현이 다를 수 있는 답안은 AI 채점을 실행하거나 아래에서 직접 정답/오답을 지정해 주세요.
            </p>

            {gradingLoading ? (
              <p style={styles.hintText}>채점 데이터를 불러오는 중...</p>
            ) : (
              <div style={{ ...styles.tableScrollFixed, maxHeight: '420px' }}>
                <table style={styles.table}>
                  <thead style={styles.stickyThead}>
                    <tr>
                      <th style={{ width: '50px' }}>번호</th>
                      <th>응시자</th>
                      <th>제출 답안</th>
                      <th>모범 정답</th>
                      <th style={{ width: '150px' }}>정오 판정</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(gradingData?.answers || []).map(item => (
                      <tr key={item.answerId}>
                        <td>{item.problemOrder}</td>
                        <td>
                          <div style={styles.nameWithAvatar}>
                            <img src={item.userProfileImageUrl || '/basic.png'} alt={item.userName} style={styles.avatarImg} />
                            <span>{item.userName}</span>
                          </div>
                        </td>
                        <td>{item.userAnswer || '-'}</td>
                        <td>{item.correctAnswer}</td>
                        <td>
                          <div style={{ display: 'flex', gap: '0.25rem' }}>
                            <button
                              type="button"
                              className={item.isCorrect === true ? 'btn btn-primary' : 'btn btn-secondary'}
                              style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem', flex: 1 }}
                              onClick={() => handleManualGrade(item.answerId, true)}
                            >
                              정답
                            </button>
                            <button
                              type="button"
                              className={item.isCorrect === false ? 'btn btn-danger' : 'btn btn-secondary'}
                              style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem', flex: 1 }}
                              onClick={() => handleManualGrade(item.answerId, false)}
                            >
                              오답
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                    {gradingData && gradingData.answers.length === 0 && (
                      <tr>
                        <td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                          제출된 답안이 존재하지 않습니다.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            )}

            <div style={styles.modalActions}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => gradingRoomId && loadGradingData(gradingRoomId)}
                disabled={gradingLoading}
              >
                새로고침
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleAiGradeInModal}
                disabled={gradingData?.isAiGradingInProgress}
              >
                AI 채점 실행
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setShowGradingModal(false);
                  setGradingData(null);
                  setGradingRoomId(null);
                }}
              >
                닫기
              </button>
              <button type="button" className="btn btn-primary" onClick={handleFinalizeFromGrading}>
                채점 확정
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 마이페이지 모의고사 상세 리뷰 모달 */}
      {viewingExamDetail && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '650px', maxHeight: '80vh', overflowY: 'auto' }}>
            <h3 style={styles.modalTitle}>평가 시험 결과 리뷰</h3>
            <div style={{ background: '#f5f3ff', padding: '1rem', borderRadius: '12px', border: '1px solid #c7d2fe' }}>
              <p><strong>시험명:</strong> {viewingExamDetail.roomName}</p>
              <p><strong>내 점수:</strong> <span style={{ color: 'var(--secondary)', fontSize: '1.25rem', fontWeight: 800 }}>{viewingExamDetail.score}점</span></p>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem' }}>
              {viewingExamDetail.problems.map((prob, idx) => (
                <div key={prob.problemId} className="card" style={{ padding: '1rem', border: '1px solid #eaecf0' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                    <span style={{ fontWeight: 700, color: 'var(--text-main)' }}>{idx + 1}. {prob.name}</span>
                    <span className={`badge ${prob.isCorrect ? 'badge-success' : 'badge-danger'}`}>
                      {prob.isCorrect ? '정답' : '오답'}
                    </span>
                  </div>
                  <p style={{ background: '#f9fafb', padding: '0.75rem', borderRadius: '8px', fontSize: '0.9rem', color: '#475467' }}>
                    {prob.content}
                  </p>
                  <div style={{ fontSize: '0.85rem', marginTop: '0.5rem', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                    <p><strong>내 제출 답안:</strong> {prob.userAnswer || '(미제출)'}</p>
                    <p><strong>공식 정답:</strong> {prob.correctAnswer}</p>
                    <p style={{ color: '#4b5563', fontStyle: 'italic', marginTop: '0.25rem' }}>
                      <strong>풀이 해설:</strong> {prob.explanation}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            <div style={styles.modalActions}>
              <button className="btn btn-secondary" onClick={() => setViewingExamDetail(null)}>
                리뷰 닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
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
  sidebarTitle: {
    fontSize: '1.75rem',
    fontWeight: 800,
    color: '#1d2939',
    letterSpacing: '-1px',
    textAlign: 'center',
    margin: '0',
  },
  spaceBadge: {
    backgroundColor: '#f5f3ff',
    color: '#7c3aed',
    padding: '0.5rem 0.75rem',
    borderRadius: '8px',
    border: '1px solid #c7d2fe',
    fontSize: '0.8rem',
    fontWeight: 600,
    textAlign: 'center',
    marginTop: '0.25rem',
  },
  sidebarMenu: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
    marginTop: '1.5rem',
    flex: 1,
  },
  menuBtn: {
    padding: '0.8rem 1rem',
    borderRadius: '8px',
    fontSize: '0.9rem',
    fontWeight: 600,
    color: '#475467',
    border: 'none',
    backgroundColor: 'transparent',
    cursor: 'pointer',
    textAlign: 'left',
    transition: 'all 0.2s',
  },
  menuActive: {
    backgroundColor: '#f0f2ff',
    color: '#6366f1',
  },
  backBtn: {
    width: '100%',
    padding: '0.75rem',
  },
  headerRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '2rem',
  },
  filterCard: {
    padding: '1rem',
    marginBottom: '1.5rem',
    display: 'flex',
    gap: '1rem',
  },
  problemGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(min(260px, 100%), 1fr))',
    gap: '1.5rem',
  },
  problemCard: {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between',
    minHeight: '220px',
  },
  badgeUnsolved: {
    display: 'inline-flex',
    alignItems: 'center',
    padding: '0.25rem 0.75rem',
    borderRadius: '9999px',
    fontSize: '0.75rem',
    fontWeight: 600,
    backgroundColor: '#f2f4f7',
    color: '#475467',
    border: '1px solid #e4e7ec',
  },
  problemTitleText: {
    fontSize: '1.15rem',
    fontWeight: 700,
    color: '#1d2939',
    marginBottom: '0.5rem',
  },
  problemContentText: {
    fontSize: '0.9rem',
    color: '#475467',
    lineHeight: 1.5,
  },
  problemCardActions: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: '1.5rem',
    paddingTop: '1rem',
    borderTop: '1px solid #eaecf0',
  },
  cardBtn: {
    padding: '0.5rem 1rem',
    fontSize: '0.825rem',
  },
  smallIconBtn: {
    padding: '0.5rem 0.75rem',
    fontSize: '0.8rem',
  },
  examGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(min(280px, 100%), 1fr))',
    gap: '1.5rem',
  },
  examCard: {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between',
    minHeight: '220px',
  },
  examTitleText: {
    fontSize: '1.15rem',
    fontWeight: 700,
    color: '#1d2939',
    marginBottom: '0.5rem',
  },
  examDescText: {
    fontSize: '0.875rem',
    color: '#475467',
    marginBottom: '1rem',
  },
  examMetaRow: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.25rem',
  },
  examCardActions: {
    marginTop: '1.5rem',
    paddingTop: '1rem',
    borderTop: '1px solid #eaecf0',
  },
  summaryGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gap: '1.5rem',
    marginBottom: '2rem',
  },
  summaryCard: {
    padding: '1.5rem',
    textAlign: 'center',
  },
  summaryLabel: {
    fontSize: '0.825rem',
    color: '#98a2b3',
    fontWeight: 600,
    display: 'block',
    marginBottom: '0.5rem',
  },
  summaryVal: {
    fontSize: '2rem',
    fontWeight: 800,
    color: '#6366f1',
    margin: 0,
  },
  dashboardSplit: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '1.5rem',
  },
  dashboardSectionCard: {
    padding: '1.5rem',
    minHeight: '400px',
    display: 'flex',
    flexDirection: 'column',
  },
  dashboardSectionTitle: {
    fontSize: '1.1rem',
    marginBottom: '1rem',
    borderBottom: '2px solid #eaecf0',
    paddingBottom: '0.5rem',
  },
  tableScroll: {
    overflowX: 'auto',
    flex: 1,
  },
  tableScrollFixed: {
    overflowX: 'auto',
    overflowY: 'auto',
    maxHeight: '560px',
    flex: 1,
  },
  stickyThead: {
    position: 'sticky',
    top: 0,
    backgroundColor: 'var(--card-bg)',
    zIndex: 1,
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    textAlign: 'left',
    fontSize: '0.875rem',
  },
  errorText: {
    color: '#ef4444',
    fontSize: '0.75rem',
    marginTop: '0.25rem',
    textAlign: 'left',
    display: 'block',
  },
  hintText: {
    color: '#6b7280',
    fontSize: '0.75rem',
    marginTop: '0.25rem',
    textAlign: 'left',
    display: 'block',
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
    marginTop: '1rem',
  },
  solvingCard: {
    padding: '1.25rem',
    backgroundColor: '#ffffff',
    borderRadius: '12px',
    border: '1px solid #eaecf0',
  },
  resultBox: {
    padding: '1.5rem',
    backgroundColor: '#f9fafb',
    borderRadius: '12px',
    border: '1px solid #eaecf0',
    textAlign: 'center',
  },
  resultTitle: {
    fontSize: '1.5rem',
    fontWeight: 800,
    marginBottom: '1rem',
  },
  scheduleCard: {
    display: 'flex',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '0.75rem',
    padding: '1rem 1.25rem',
    borderRadius: 'var(--border-radius-md)',
    border: '1px solid var(--border-color)',
    backgroundColor: '#ffffff',
  },
  scheduleField: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.625rem',
    flex: 1,
    minWidth: '180px',
  },
  scheduleDot: {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    flexShrink: 0,
  },
  scheduleFieldBody: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.15rem',
    flex: 1,
  },
  scheduleFieldLabel: {
    fontSize: '0.7rem',
    fontWeight: 700,
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.03em',
  },
  scheduleInput: {
    border: 'none',
    outline: 'none',
    padding: 0,
    fontSize: '0.9rem',
    fontWeight: 600,
    fontFamily: 'inherit',
    backgroundColor: 'transparent',
    width: '100%',
  },
  scheduleArrow: {
    color: 'var(--text-muted)',
    fontSize: '1rem',
  },
  scheduleDuration: {
    fontSize: '0.75rem',
    fontWeight: 700,
    color: 'var(--primary)',
    backgroundColor: 'var(--primary-light)',
    border: '1px solid var(--primary-border)',
    borderRadius: '999px',
    padding: '0.25rem 0.75rem',
    whiteSpace: 'nowrap',
  },
  examineeHeader: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: '0.5rem',
  },
  examineeCount: {
    fontSize: '0.8rem',
    fontWeight: 700,
    color: 'var(--primary)',
  },
  examineeToolbar: {
    display: 'flex',
    gap: '0.5rem',
    marginBottom: '0.625rem',
  },
  examineeSearchInput: {
    flex: 1,
    padding: '0.6rem 0.9rem',
    fontSize: '0.85rem',
  },
  examineeToggleAllBtn: {
    padding: '0.5rem 0.9rem',
    fontSize: '0.8rem',
    whiteSpace: 'nowrap',
  },
  examineeList: {
    maxHeight: '220px',
    overflowY: 'auto',
    border: '1px solid var(--border-color)',
    borderRadius: 'var(--border-radius-md)',
    padding: '0.5rem',
    backgroundColor: '#ffffff',
    display: 'flex',
    flexDirection: 'column',
    gap: '0.375rem',
  },
  examineeItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    padding: '0.5rem 0.625rem',
    borderRadius: 'var(--border-radius-sm)',
    cursor: 'pointer',
    transition: 'var(--transition)',
    border: '1px solid transparent',
  },
  examineeItemSelected: {
    backgroundColor: 'var(--primary-light)',
    borderColor: 'var(--primary-border)',
  },
  avatarImg: {
    width: '28px',
    height: '28px',
    borderRadius: '50%',
    objectFit: 'cover',
    border: '1px solid var(--border-color)',
    flexShrink: 0,
  },
  nameWithAvatar: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
  },
  examineeInfo: {
    display: 'flex',
    flexDirection: 'column',
    flex: 1,
    minWidth: 0,
  },
  examineeName: {
    fontSize: '0.85rem',
    fontWeight: 600,
    color: 'var(--text-main)',
  },
  examineeEmail: {
    fontSize: '0.75rem',
    color: 'var(--text-muted)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  examineeCheck: {
    color: 'var(--primary)',
    fontWeight: 800,
    fontSize: '1rem',
    flexShrink: 0,
  },
  examineeEmptyText: {
    fontSize: '0.8rem',
    color: 'var(--text-muted)',
    textAlign: 'center',
    padding: '0.75rem 0',
  },
  wizardHeader: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  wizardSteps: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.375rem',
  },
  wizardStepDot: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '24px',
    height: '24px',
    borderRadius: '50%',
    backgroundColor: 'var(--bg-color)',
    color: 'var(--text-muted)',
    fontSize: '0.75rem',
    fontWeight: 700,
  },
  wizardStepDotActive: {
    backgroundColor: 'var(--primary)',
    color: '#ffffff',
  },
  wizardStepLine: {
    width: '24px',
    height: '2px',
    backgroundColor: 'var(--border-color)',
  },
  wizardStepLabel: {
    fontSize: '0.8rem',
    fontWeight: 600,
    color: 'var(--primary)',
    marginTop: '-0.75rem',
  },
  problemRemoveBtn: {
    border: 'none',
    background: 'none',
    color: 'var(--text-muted)',
    cursor: 'pointer',
    fontSize: '0.8rem',
    padding: '0.1rem 0.3rem',
  },
  examAppContainer: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '100vh',
    backgroundColor: '#f9fafb',
    padding: '2rem',
  },
  examWaitCard: {
    width: '100%',
    maxWidth: '540px',
    padding: '2.5rem',
    textAlign: 'center',
  },
  examWaitTitle: {
    fontSize: '1.75rem',
    color: 'var(--primary)',
    marginBottom: '1.5rem',
  },
  examWaitInfoBox: {
    background: '#f9fafb',
    padding: '1.25rem',
    borderRadius: '12px',
    border: '1px solid #eaecf0',
    textAlign: 'left',
    marginBottom: '2rem',
    fontSize: '0.9rem',
    lineHeight: 1.6,
  },
  examWaitActions: {
    display: 'flex',
    gap: '1rem',
    justifyContent: 'center',
  },
  examSolveContainer: {
    display: 'flex',
    flexDirection: 'column',
    minHeight: '100vh',
    backgroundColor: '#f9fafb',
  },
  examHeader: {
    backgroundColor: '#ffffff',
    padding: '1.5rem 3rem 1rem',
    borderBottom: '1px solid #eaecf0',
    position: 'sticky',
    top: 0,
    zIndex: 100,
  },
  examHeaderTitle: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    fontSize: '1.25rem',
    fontWeight: 700,
    marginBottom: '0.75rem',
  },
  badgeTimer: {
    background: '#fef2f2',
    color: '#ef4444',
    border: '1px solid rgba(239, 68, 68, 0.2)',
    padding: '0.35rem 0.85rem',
    borderRadius: '999px',
    fontSize: '0.9rem',
    fontWeight: 700,
  },
  progressBarBg: {
    width: '100%',
    height: '6px',
    backgroundColor: '#eaecf0',
    borderRadius: '999px',
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: '#6366f1',
    transition: 'width 0.3s ease',
  },
  examSolveBody: {
    display: 'flex',
    flex: 1,
    padding: '2.5rem 3rem',
    gap: '2rem',
    alignItems: 'flex-start',
  },
  examProblemCard: {
    flex: 1,
    padding: '2.5rem',
    minHeight: '420px',
  },
  badgeNum: {
    backgroundColor: '#f5f3ff',
    color: '#7c3aed',
    padding: '0.35rem 0.75rem',
    borderRadius: '999px',
    fontSize: '0.8rem',
    fontWeight: 700,
    display: 'inline-block',
    marginBottom: '1rem',
  },
  examProblemName: {
    fontSize: '1.5rem',
    fontWeight: 800,
    color: '#1d2939',
    marginBottom: '1rem',
  },
  examProblemText: {
    fontSize: '1.05rem',
    color: '#475467',
    lineHeight: 1.6,
  },
  omrSidebar: {
    width: '280px',
    padding: '1.5rem',
  },
  omrTitle: {
    fontSize: '1.05rem',
    marginBottom: '1rem',
    borderBottom: '2px solid #eaecf0',
    paddingBottom: '0.5rem',
  },
  omrGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(5, 1fr)',
    gap: '0.5rem',
  },
  omrBtn: {
    padding: '0.6rem 0',
    border: '1px solid #eaecf0',
    backgroundColor: '#ffffff',
    color: '#98a2b3',
    borderRadius: '6px',
    cursor: 'pointer',
    fontWeight: 700,
    fontSize: '0.9rem',
    transition: 'all 0.2s',
  },
  omrBtnActive: {
    borderColor: '#6366f1',
    color: '#6366f1',
    boxShadow: '0 0 0 3px rgba(99, 102, 241, 0.15)',
  },
  omrBtnFilled: {
    backgroundColor: '#f0f2ff',
    color: '#6366f1',
    borderColor: '#c7d2fe',
  },
  examFooter: {
    backgroundColor: '#ffffff',
    padding: '1.5rem 3rem',
    borderTop: '1px solid #eaecf0',
    display: 'flex',
    justifyContent: 'space-between',
    position: 'sticky',
    bottom: 0,
    zIndex: 100,
  },
};

// 스타일 오버라이드
if (typeof document !== 'undefined') {
  const customTableStyle = document.createElement('style');
  customTableStyle.innerHTML = `
    table th, table td {
      padding: 0.85rem 1rem;
      border-bottom: 1px solid #eaecf0;
    }
    table th {
      background-color: #f9fafb;
      color: #475467;
      font-weight: 700;
    }
  `;
  document.head.appendChild(customTableStyle);
}
