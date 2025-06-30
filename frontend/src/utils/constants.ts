// API 관련 상수
export const API_ENDPOINTS = {
  BOARDS: '/boards',
  OPENSTACK: '/openstack',
  SYSTEM: '/integrated/system-status',
} as const;

// VM 상태 상수
export const VM_STATUS = {
  ACTIVE: 'ACTIVE',
  BUILD: 'BUILD',
  ERROR: 'ERROR',
  SHUTOFF: 'SHUTOFF',
  SUSPENDED: 'SUSPENDED',
  PAUSED: 'PAUSED',
} as const;

// 배포 상태 상수
export const DEPLOYMENT_STATUS = {
  PENDING: 'PENDING',
  DEPLOYING: 'DEPLOYING',
  DEPLOYED: 'DEPLOYED',
  FAILED: 'FAILED',
  DESTROYED: 'DESTROYED',
} as const;

// 우선순위 상수
export const PRIORITY = {
  HIGH: 'high',
  MEDIUM: 'medium',
  LOW: 'low',
} as const;

// 테마 상수
export const THEME = {
  LIGHT: 'light',
  DARK: 'dark',
} as const;

// 알림 타입 상수
export const NOTIFICATION_TYPE = {
  SUCCESS: 'success',
  ERROR: 'error',
  WARNING: 'warning',
  INFO: 'info',
} as const;

// 페이지네이션 기본값
export const PAGINATION_DEFAULTS = {
  PAGE_SIZE: 10,
  PAGE_SIZES: [5, 10, 20, 50],
} as const;

// 로컬 스토리지 키
export const STORAGE_KEYS = {
  AUTH_TOKEN: 'authToken',
  USER_PREFERENCES: 'userPreferences',
  THEME: 'theme',
} as const; 