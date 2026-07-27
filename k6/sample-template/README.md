# 🚀 팀원용 k6 부하 테스트 템플릿 사용 가이드

이 디렉토리는 팀원들이 각자 맡은 도메인의 API를 부하 테스트할 때 복사해서 사용할 수 있는 **샘플 템플릿 파일 모음**입니다.

---

### 📂 복사 및 사용 방법 (3 Step)

1. **템플릿 폴더 복사**:
   - `k6/sample-template/sample-api-test.js` 파일 또는 폴더 전체를 복사합니다.
   - `k6/` 하위에 **본인의 도메인 이름으로 폴더를 생성**하고 거기에 붙여넣습니다.
   - 예시: `k6/space-manage/load-test.js`, `k6/user-auth/load-test.js`

2. **상대 경로 확인**:
   - 상위 공통 설정(`config.js`)을 참조하는 import 경로가 `../config.js`인지 확인합니다.

3. **3가지만 본인 API에 맞게 수정**:
   - ① **API 엔드포인트 URL**: `${BASE_URL}/api/본인-도메인-경로`
   - ② **HTTP 메서드 및 Payload**: `http.get()` 또는 `http.post(url, payload, params)`
   - ③ **성공 체크 조건**: `check(res, { '200 OK': (r) => r.status === 200 })`

---

### 🏃‍♂️ 실행 방법

프로젝트 루트 디렉토리에서 아래 명령어로 실행합니다.

```bash
# 본인 도메인 폴더로 실행
k6 run k6/본인-도메인-폴더/본인-테스트-파일.js
```
