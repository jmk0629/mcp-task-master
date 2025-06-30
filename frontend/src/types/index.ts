// 게시판 관련 타입
export interface Board {
  id: number;
  title: string;
  content: string;
  author: string;
  createdAt: string;
  updatedAt: string;
  vmRequest?: VMRequest;
}

export interface BoardCreateRequest {
  title: string;
  content: string;
  author: string;
  vmRequest?: VMRequest;
}

export interface BoardUpdateRequest {
  title?: string;
  content?: string;
  vmRequest?: VMRequest;
}

// OpenStack VM 관련 타입
export interface VMRequest {
  name: string;
  flavor: string;
  image: string;
  network: string;
  securityGroups: string[];
  keyPair?: string;
  userData?: string;
}

export interface VMStatus {
  id: string;
  name: string;
  status: 'ACTIVE' | 'BUILD' | 'ERROR' | 'SHUTOFF' | 'SUSPENDED' | 'PAUSED';
  flavor: string;
  image: string;
  network: string;
  ipAddress?: string;
  createdAt: string;
  updatedAt: string;
}

export interface VMDeployment {
  id: string;
  boardId: number;
  vmId?: string;
  status: 'PENDING' | 'DEPLOYING' | 'DEPLOYED' | 'FAILED' | 'DESTROYED';
  terraformTemplate: string;
  deploymentLog?: string;
  createdAt: string;
  updatedAt: string;
}

// UI 관련 타입
export interface Theme {
  colors: {
    primary: string;
    secondary: string;
    success: string;
    warning: string;
    error: string;
    background: string;
    surface: string;
    text: {
      primary: string;
      secondary: string;
    };
  };
  spacing: {
    xs: string;
    sm: string;
    md: string;
    lg: string;
    xl: string;
  };
  borderRadius: {
    sm: string;
    md: string;
    lg: string;
  };
}

export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
}

export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  size: number;
} 