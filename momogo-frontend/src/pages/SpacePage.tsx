import React, { useEffect, useState, useRef } from 'react';
import type { UserResponse } from '../types/user';
import type { SpaceResponse } from '../types/space';
import { request } from '../services/api';

interface SpacePageProps {
  user: UserResponse;
  space: SpaceResponse;
  onBack: () => void;
  showToast: (message: string, type?: 'success' | 'error' | 'info') => void;
}

interface CategoryResponse {
  id: string;
  name: string;
}

interface ProblemResponse {
  id: string;
  name: string;
  content: string;
  correctAnswer: string;
  explanation: string;
  category: CategoryResponse | null;
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
  userScores: {
    userId: string;
    userName: string;
    score: number;
    attended: boolean;
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

interface RoomProblemResponse {
  id: string;
  roomId: string;
  problemOrder: number;
  name: string;
  content: string;
}

export const SpacePage: React.FC<SpacePageProps> = ({ user, space, onBack, showToast }) => {
  const [activeTab, setActiveTab] = useState<'problems' | 'exams' | 'dashboard'>('problems');

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

  // 시험방 생성 폼 상태
  const [roomName, setRoomName] = useState('');
  const [roomDesc, setRoomDesc] = useState('');
  const [roomStart, setRoomStart] = useState('');
  const [roomEnd, setRoomEnd] = useState('');
  const [selectedMembers, setSelectedMembers] = useState<string[]>([]);
  const [examProblemsList, setExamProblemsList] = useState<{
    problemOrder: number;
    name: string;
    content: string;
    explanation: string;
    correctAnswer: string;
  }[]>([{ problemOrder: 1, name: '', content: '', explanation: '', correctAnswer: '' }]);

  const [allUsersList, setAllUsersList] = useState<UserResponse[]>([]);

  // 2-1. 실시간 대기실 / 시험 모드 관련 상태
  const [currentExamRoom, setCurrentExamRoom] = useState<RoomResponse | null>(null);
  const [examMode, setExamMode] = useState<'none' | 'waiting' | 'testing'>('none');
  const [waitingUsers, setWaitingUsers] = useState<{ userId: string; name: string }[]>([]);
  const [examProblems, setExamProblems] = useState<RoomProblemResponse[]>([]);
  const [currentProblemIndex, setCurrentProblemIndex] = useState(0);
  const [examAnswers, setExamAnswers] = useState<Record<string, string>>({});
  const [timeLeft, setTimeLeft] = useState(0);

  // 2-2. 성적표 / 통계 모달
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportData, setReportData] = useState<RoomReportResponse | null>(null);

  // 3. 마이페이지 대시보드 관련 상태
  const [summary, setSummary] = useState<SingleSummaryResponse | null>(null);
  const [history, setHistory] = useState<SingleHistoryResponse[]>([]);
  const [examsHistory, setExamsHistory] = useState<ExamListItemResponse[]>([]);
  const [viewingExamDetail, setViewingExamDetail] = useState<ExamDetailResponse | null>(null);

  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const timerRef = useRef<any>(null);

  useEffect(() => {
    loadCategories();
    loadProblems();
    loadRooms();
    loadDashboardData();
    loadMembersForInvitation();
  }, [selectedCategory, activeTab]);

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
      const data = await request<RoomResponse[]>(`/api/spaces/${space.id}/rooms/list`, {
        method: 'GET',
      }).catch(async () => {
        // 백엔드에 전용 맵핑 리스트가 없을 시 전체 목록으로 조회 필터링 fallback
        return request<RoomResponse[]>(`/api/spaces/${space.id}/rooms`, { method: 'GET' });
      });
      if (data) setRooms(data);
    } catch {
      // 룸 리스트 불러오기 임시 하드코딩 mock 구조
      setRooms([]);
    }
  };

  // 대시보드 로딩
  const loadDashboardData = async () => {
    try {
      const summaryData = await request<SingleSummaryResponse>('/api/dashboards/single/summary', { method: 'GET' });
      const historyData = await request<SingleHistoryResponse[]>('/api/dashboards/single/history', { method: 'GET' });
      const examHistoryData = await request<ExamListItemResponse[]>('/api/dashboards/exams', { method: 'GET' });

      if (summaryData) setSummary(summaryData);
      if (historyData) setHistory(historyData);
      if (examHistoryData) setExamsHistory(examHistoryData);
    } catch (err: any) {
      console.error(err);
    }
  };

  // 멤버 가입 리스트 조회 (초대용)
  const loadMembersForInvitation = async () => {
    try {
      const usersData = await request<any>('/api/users', { method: 'GET', params: { size: '100' } });
      if (usersData && usersData.values) {
        setAllUsersList(usersData.values);
      }
    } catch (err: any) {
      console.error(err);
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
      await request<any>(`/api/spaces/${space.id}/problems/${solvingProblem?.id}/solve`, {
        method: 'POST',
        body: JSON.stringify({ userAnswer: solveUserAnswer }),
      });

      const isCorrect = solveUserAnswer.trim() === solvingProblem?.correctAnswer.trim();

      // localStorage에 풀이 이력 저장 (오답 이력 및 통계용)
      const singleHistory = JSON.parse(localStorage.getItem('momogo_single_history') || '[]');
      const newHistoryItem = {
        problemId: solvingProblem?.id,
        problemName: solvingProblem?.name,
        categoryName: solvingProblem?.category?.name || '미분류',
        isSolved: isCorrect,
        userAnswer: solveUserAnswer,
        correctAnswer: solvingProblem?.correctAnswer,
        solvedAt: new Date().toISOString()
      };
      // 중복 풀이 기록이 있을 시 이전 기록 제거하고 최신 풀이로 갱신
      const filteredHistory = singleHistory.filter((h: any) => h.problemId !== solvingProblem?.id);
      localStorage.setItem('momogo_single_history', JSON.stringify([newHistoryItem, ...filteredHistory]));

      setSolveResult({
        correct: isCorrect,
        answer: solvingProblem?.correctAnswer || '',
        exp: solvingProblem?.explanation || '',
      });
      loadDashboardData();
    } catch (err: any) {
      showToast(err.message || '답안 제출에 실패했습니다.', 'error');
    }
  };

  // 평가 시험방 생성 처리
  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors: Record<string, string> = {};
    if (!roomName.trim()) errors.roomName = '시험 이름을 입력해 주세요.';
    if (!roomStart) errors.roomStart = '시작 일시를 입력해 주세요.';
    if (!roomEnd) errors.roomEnd = '종료 일시를 입력해 주세요.';
    if (selectedMembers.length === 0) errors.selectedMembers = '최소 한 명의 응시자를 선택해 주세요.';

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
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

      const newRoom = await request<any>(`/api/spaces/${space.id}/rooms`, {
        method: 'POST',
        body: JSON.stringify(payload),
      });

      // 개설된 룸의 문제를 로컬 스토리지에 백업해둡니다. (사용자 오프라인 채점용)
      if (newRoom && newRoom.id) {
        localStorage.setItem(`momogo_created_room_problems_${newRoom.id}`, JSON.stringify(
          examProblemsList.map((p, i) => ({
            id: `rp-${newRoom.id}-${i}`,
            problemOrder: i + 1,
            name: p.name || `문제 ${i + 1}`,
            content: p.content,
            correctAnswer: p.correctAnswer,
            explanation: p.explanation
          }))
        ));
      }

      showToast('정기 평가시험이 개설되었습니다.', 'success');
      setShowCreateRoomModal(false);
      resetRoomForm();
      loadRooms();
    } catch (err: any) {
      showToast(err.message || '시험방 개설에 실패했습니다.', 'error');
    }
  };

  // 실시간 대기실 입장 처리
  const handleEnterWaitRoom = async (room: RoomResponse) => {
    setCurrentExamRoom(room);
    setExamMode('waiting');
    setWaitingUsers([{ userId: user.id || 'me', name: user.name }]);

    // 웹소켓/STOMP 연결 시뮬레이션 및 실시간 유저 업데이트
    const names = ['김철수', '이영희', '박민수', '최현우'];
    let counter = 0;
    const interval = setInterval(() => {
      if (counter < names.length) {
        setWaitingUsers(prev => [...prev, { userId: `id-${counter}`, name: names[counter] }]);
        counter++;
      } else {
        clearInterval(interval);
      }
    }, 2000);

    return () => clearInterval(interval);
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

      // 제출 답안을 로컬 스토리지에 임시 저장 (채점 시 사용)
      localStorage.setItem(`momogo_temp_exam_submission_${currentExamRoom.id}`, JSON.stringify(examAnswers));

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

  // 채점 최종 확정
  const handleFinalizeGrade = async (roomId: string) => {
    try {
      await request(`/api/rooms/${roomId}/finalize-grade`, { method: 'POST' });
      showToast('최종 채점 마감이 확정되었습니다. 이제 리포트를 열람할 수 있습니다.', 'success');

      // 로컬 스토리지에 저장된 유저 제출 답안과 출제 문제의 정답을 비교하여 채점 결과 생성
      const tempSubmission = JSON.parse(localStorage.getItem(`momogo_temp_exam_submission_${roomId}`) || '{}');
      const createdProblems = JSON.parse(localStorage.getItem(`momogo_created_room_problems_${roomId}`) || '[]');

      if (createdProblems.length > 0) {
        // 백업된 제출 내역이 없는 경우 가상의 답안 생성 (데모 편의성 확보)
        const defaultSubmission: Record<string, string> = {};
        createdProblems.forEach((p: any) => {
          defaultSubmission[p.id] = Math.random() > 0.2 ? p.correctAnswer : '오답';
        });
        const userAnswers = Object.keys(tempSubmission).length > 0 ? tempSubmission : defaultSubmission;

        let correctCount = 0;
        const detailedProblems = createdProblems.map((rp: any) => {
          const userAns = userAnswers[rp.id] || '';
          const isCorrect = userAns.trim().toLowerCase() === rp.correctAnswer?.trim().toLowerCase();
          if (isCorrect) correctCount++;
          return {
            problemId: rp.id,
            problemOrder: rp.problemOrder,
            name: rp.name,
            content: rp.content,
            userAnswer: userAns,
            correctAnswer: rp.correctAnswer,
            explanation: rp.explanation || '해설 정보가 존재하지 않습니다.',
            isCorrect
          };
        });

        const score = Math.round((correctCount / createdProblems.length) * 100);

        // 1. 리스트 아이템 추가
        const examsHistory = JSON.parse(localStorage.getItem('momogo_exams_history') || '[]');
        const targetRoom = rooms.find(r => r.id === roomId);
        const newItem = {
          roomId,
          roomName: targetRoom?.name || '평가 시험',
          description: targetRoom?.description || '',
          score,
          totalProblems: createdProblems.length,
          testStartAt: targetRoom?.testStartAt || new Date().toISOString(),
          testEndAt: targetRoom?.testEndAt || new Date().toISOString(),
          isEnded: true
        };
        const filteredList = examsHistory.filter((item: any) => item.roomId !== roomId);
        localStorage.setItem('momogo_exams_history', JSON.stringify([newItem, ...filteredList]));

        // 2. 상세 내역 저장
        const examDetails = JSON.parse(localStorage.getItem('momogo_exam_details') || '{}');
        examDetails[roomId] = {
          roomId,
          roomName: targetRoom?.name || '평가 시험',
          description: targetRoom?.description || '',
          score,
          problems: detailedProblems
        };
        localStorage.setItem('momogo_exam_details', JSON.stringify(examDetails));
      }

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

  // 리포트 PDF 다운로드
  const handleDownloadPdf = (roomId: string) => {
    window.open(`http://localhost:8080/api/rooms/${roomId}/report/download`, '_blank');
    showToast('PDF 다운로드를 시도합니다.', 'info');
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
    setWaitingUsers([]);
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
    setRoomName('');
    setRoomDesc('');
    setRoomStart('');
    setRoomEnd('');
    setSelectedMembers([]);
    setExamProblemsList([{ problemOrder: 1, name: '', content: '', explanation: '', correctAnswer: '' }]);
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

          <h3 style={styles.examSubTitle}>현재 접속 유저 목록</h3>
          <div style={styles.participantList}>
            {waitingUsers.map((u, i) => (
              <div key={i} style={styles.participantTag}>
                <span style={styles.statusDot}></span> {u.name}
              </div>
            ))}
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

  return (
    <div className="app-container">
      {/* 사이드바 메뉴 */}
      <aside className="sidebar-layout">
        <h2 style={styles.sidebarTitle}>MoMoGo</h2>
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
        <button className="btn btn-secondary" style={styles.backBtn} onClick={onBack}>
          메인 페이지 이동
        </button>
      </aside>

      {/* 본문 레이아웃 */}
      <main className="main-layout">
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
                  <div key={prob.id} className="card" style={styles.problemCard}>
                    <div>
                      <span className="badge badge-info" style={{ marginBottom: '0.5rem' }}>
                        {prob.category?.name || '미분류'}
                      </span>
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
                            onClick={() => {
                              setEditingProblem(prob);
                              setProbName(prob.name);
                              setProbContent(prob.content);
                              setProbAnswer(prob.correctAnswer);
                              setProbExplanation(prob.explanation);
                              setProbCategory(prob.category?.id || '');
                              setShowCreateProblemModal(true);
                            }}
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
                            <>
                              <button
                                className="btn btn-secondary"
                                style={{ flex: 1 }}
                                onClick={() => handleAiGrade(room.id)}
                              >
                                AI 채점
                              </button>
                              <button
                                className="btn btn-primary"
                                style={{ flex: 1 }}
                                onClick={() => handleFinalizeGrade(room.id)}
                              >
                                채점 확정
                              </button>
                            </>
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
              <span style={styles.demoBadge}>데모 데이터 (백엔드 미연동)</span>
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
                  <h3 style={styles.summaryVal}>{summary.averageCorrectRate}%</h3>
                </div>
                <div className="card" style={styles.summaryCard}>
                  <span style={styles.summaryLabel}>평균 오답률</span>
                  <h3 style={styles.summaryVal}>{100 - summary.averageCorrectRate}%</h3>
                </div>
              </div>
            )}

            {/* 공간 대시보드: 랭킹 표시판 (규칙 3.6 공간 대시보드 반영) */}
            <div className="card" style={{ marginTop: '0.5rem', marginBottom: '2rem', padding: '1.5rem' }}>
              <h3 style={{ ...styles.dashboardSectionTitle, borderBottom: '2px solid var(--primary-border)', paddingBottom: '0.5rem', marginBottom: '1rem' }}>
                공간 랭킹 대시보드
                <span style={styles.demoBadge}>데모 데이터 (백엔드 미연동)</span>
              </h3>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-sub)', marginBottom: '1.25rem' }}>
                공간 내부에 등록되어 있는 문제를 푼 유저들의 실시간 랭킹입니다.
              </p>
              <div style={styles.tableScroll}>
                <table style={styles.table}>
                  <thead>
                    <tr>
                      <th style={{ width: '80px' }}>순위</th>
                      <th>이름 (이메일)</th>
                      <th>해결한 문제 수</th>
                      <th>정답률</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr style={{ backgroundColor: '#fffbeb' }}>
                      <td><strong>1등 🥇</strong></td>
                      <td>김철수 (chulsoo@naver.com)</td>
                      <td>15개</td>
                      <td>92.5%</td>
                    </tr>
                    <tr>
                      <td><strong>2등 🥈</strong></td>
                      <td>이영희 (younghee@gmail.com)</td>
                      <td>12개</td>
                      <td>85.0%</td>
                    </tr>
                    <tr style={{ backgroundColor: '#f0f2ff', borderLeft: '4px solid var(--primary)' }}>
                      <td><strong>3등 🥉 (나)</strong></td>
                      <td>{user.name} ({user.email})</td>
                      <td>{history.filter(h => h.isSolved).length}개</td>
                      <td>{summary?.averageCorrectRate || 0}%</td>
                    </tr>
                    <tr>
                      <td>4등</td>
                      <td>박민수 (minsoo@daum.net)</td>
                      <td>3개</td>
                      <td>66.6%</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div style={styles.dashboardSplit}>
              {/* 싱글모드 풀이 이력 */}
              <div className="card" style={styles.dashboardSectionCard}>
                <h3 style={styles.dashboardSectionTitle}>싱글모드 오답 학습 이력</h3>
                <div style={styles.tableScroll}>
                  <table style={styles.table}>
                    <thead>
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
                <div style={styles.tableScroll}>
                  <table style={styles.table}>
                    <thead>
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
              <span style={styles.demoBadge}>제출 결과 데모 처리 (백엔드 미연동)</span>
            </h3>
            <div style={styles.solvingCard}>
              <span className="badge badge-info">{solvingProblem.category?.name || '미분류'}</span>
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
                      placeholder="단답형 해답 값을 기입하세요"
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

      {/* 평가 시험방 개설 모달 */}
      {showCreateRoomModal && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '600px' }}>
            <h3 style={styles.modalTitle}>정기 평가 시험방 개설</h3>
            <form onSubmit={handleCreateRoom} style={styles.form} noValidate>
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

              <div style={{ display: 'flex', gap: '1rem' }}>
                <div className="input-group" style={{ flex: 1 }}>
                  <label className="input-label">시험 시작 일시</label>
                  <input
                    type="datetime-local"
                    className="input-field"
                    value={roomStart}
                    onChange={(e) => {
                      setRoomStart(e.target.value);
                      if (validationErrors.roomStart) {
                        setValidationErrors(prev => ({ ...prev, roomStart: '' }));
                      }
                    }}
                    style={{
                      borderColor: validationErrors.roomStart ? '#ef4444' : undefined,
                    }}
                  />
                  {validationErrors.roomStart && (
                    <span style={styles.errorText}>{validationErrors.roomStart}</span>
                  )}
                </div>
                <div className="input-group" style={{ flex: 1 }}>
                  <label className="input-label">시험 종료 일시</label>
                  <input
                    type="datetime-local"
                    className="input-field"
                    value={roomEnd}
                    onChange={(e) => {
                      setRoomEnd(e.target.value);
                      if (validationErrors.roomEnd) {
                        setValidationErrors(prev => ({ ...prev, roomEnd: '' }));
                      }
                    }}
                    style={{
                      borderColor: validationErrors.roomEnd ? '#ef4444' : undefined,
                    }}
                  />
                  {validationErrors.roomEnd && (
                    <span style={styles.errorText}>{validationErrors.roomEnd}</span>
                  )}
                </div>
              </div>

              {/* 응시 대상자 다중 선택 */}
              <div className="input-group">
                <label className="input-label">응시 대상자 지정</label>
                <div style={styles.scrollMultiSelect}>
                  {allUsersList.map(u => (
                    <label key={u.id} style={styles.checkboxRow}>
                      <input
                        type="checkbox"
                        checked={selectedMembers.includes(u.id || '')}
                        onChange={(e) => {
                          const uid = u.id || '';
                          if (e.target.checked) {
                            setSelectedMembers(prev => [...prev, uid]);
                          } else {
                            setSelectedMembers(prev => prev.filter(id => id !== uid));
                          }
                          if (validationErrors.selectedMembers) {
                            setValidationErrors(prev => ({ ...prev, selectedMembers: '' }));
                          }
                        }}
                      />
                      <span>{u.name} ({u.email})</span>
                    </label>
                  ))}
                </div>
                {validationErrors.selectedMembers && (
                  <span style={styles.errorText}>{validationErrors.selectedMembers}</span>
                )}
              </div>

              {/* 출제 문제 등록 */}
              <div className="input-group">
                <label className="input-label">시험 출제 문제 목록</label>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {examProblemsList.map((item, idx) => (
                    <div key={idx} className="card" style={{ padding: '0.75rem', border: '1px solid #eaecf0' }}>
                      <p style={{ fontWeight: 600, fontSize: '0.8rem', color: 'var(--primary)' }}>
                        문항 {idx + 1}
                      </p>
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
                  <button
                    type="button"
                    className="btn btn-secondary"
                    style={{ padding: '0.4rem', fontSize: '0.8rem' }}
                    onClick={() =>
                      setExamProblemsList(prev => [
                        ...prev,
                        { problemOrder: prev.length + 1, name: '', content: '', explanation: '', correctAnswer: '' },
                      ])
                    }
                  >
                    문제 추가하기
                  </button>
                </div>
              </div>

              <div style={styles.modalActions}>
                <button type="button" className="btn btn-secondary" onClick={resetRoomForm}>
                  취소
                </button>
                <button type="submit" className="btn btn-primary">
                  개설 및 등록 완료
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
                  {reportData.userScores.map((score, i) => (
                    <tr key={i}>
                      <td>{score.userName}</td>
                      <td>
                        <span className={`badge ${score.attended ? 'badge-success' : 'badge-danger'}`}>
                          {score.attended ? '응시 완료' : '결시'}
                        </span>
                      </td>
                      <td><strong>{score.attended ? `${score.score}점` : '-'}</strong></td>
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
    gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
    gap: '1.5rem',
  },
  problemCard: {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between',
    minHeight: '220px',
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
    gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
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
  scrollMultiSelect: {
    maxHeight: '120px',
    overflowY: 'auto',
    border: '1px solid #eaecf0',
    padding: '0.5rem',
    borderRadius: '8px',
    backgroundColor: '#ffffff',
  },
  checkboxRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.25rem 0',
    fontSize: '0.875rem',
    color: '#475467',
    cursor: 'pointer',
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
  examSubTitle: {
    fontSize: '1.1rem',
    textAlign: 'left',
    marginBottom: '0.75rem',
  },
  participantList: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '0.5rem',
    marginBottom: '2.5rem',
  },
  participantTag: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '0.375rem',
    background: '#ffffff',
    border: '1px solid #eaecf0',
    padding: '0.4rem 0.8rem',
    borderRadius: '999px',
    fontSize: '0.825rem',
    fontWeight: 600,
    color: '#475467',
  },
  statusDot: {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    background: '#10b981',
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
