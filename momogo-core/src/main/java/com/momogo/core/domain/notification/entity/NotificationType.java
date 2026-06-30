package com.momogo.core.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
  ROLE_CHANGE("권한 변경"),
  NEW_EXAM("새로운 시험 생성"),
  REPORT_CREATED("리포트 생성"),
  RANKING_UPDATE("랭킹 업데이트"),
  NEW_USER_JOINED("새로운 유저 합류");

  private final String description;
}