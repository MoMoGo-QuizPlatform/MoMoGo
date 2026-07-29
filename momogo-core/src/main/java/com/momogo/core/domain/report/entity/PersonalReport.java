package com.momogo.core.domain.report.entity;

import com.momogo.core.common.base.BaseCreatedTimeEntity;
import com.momogo.core.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 조회 때마다 풀이 이력(TBL_USER_PROBLEM) 전체를 집계하면 비싸므로,
 * 새벽 배치가 미리 계산해서 여기에 저장해두고 대시보드는 이 행만 읽는다.
 *
 * 동일 유저의 동일 종류(일간/주간) 리포트가 같은 기준일에 중복 생성되는 것은
 * DB 유니크 제약(UQ_PERSONAL_REPORT_USER_TYPE_DATE)이 최종 방어
 */
@Entity
@Table(name = "TBL_PERSONAL_REPORT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PersonalReport extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

     @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Builder.Default
    @Column(name = "attempted_count")
    private Integer attemptedCount = 0;

    @Builder.Default
    @Column(name = "solved_count")
    private Integer solvedCount = 0;
}
