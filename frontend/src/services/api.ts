import axios from 'axios';
import { Board, BoardCreateRequest, BoardUpdateRequest } from '../types';

// Re-export types for convenience
export type { Board, BoardCreateRequest, BoardUpdateRequest } from '../types';

// API 기본 설정
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

// Axios 인스턴스 생성
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터
apiClient.interceptors.request.use(
  (config) => {
    // 인증 토큰이 있다면 헤더에 추가
    const token = localStorage.getItem('authToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // 401 에러 시 로그아웃 처리
    if (error.response?.status === 401) {
      localStorage.removeItem('authToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 타입 정의 - types/index.ts에서 import하도록 변경
export interface OpenStackDeployRequest {
  vmName: string;
  instanceType: string;
  imageId: string;
  networkId: string;
  securityGroup: string;
  keyPair: string;
  diskSize: number;
  description?: string;
  requestedBy: string;
}

export interface OpenStackDeployResponse {
  deploymentId: string;
  instanceId?: string;
  vmName: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  statusMessage: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
  vmInfo?: {
    instanceId: string;
    instanceType: string;
    imageId: string;
    keyPair: string;
    diskSize: number;
    publicIp?: string;
    privateIp?: string;
    status: string;
  };
  error?: {
    errorCode: string;
    errorMessage: string;
    detailMessage?: string;
    occurredAt: string;
  };
}

export interface SystemStatus {
  system: {
    status: string;
    uptime: number;
    services: {
      openstack: string;
      board: string;
    };
  };
  boards: {
    total: number;
    byStatus: Record<string, number>;
  };
  deployments: {
    total: number;
    byStatus: Record<string, number>;
  };
}

// Board API
export const boardService = {
  // 모든 게시글 조회
  getAllBoards: async (): Promise<Board[]> => {
    const response = await apiClient.get<Board[]>('/boards');
    return response.data;
  },

  // 게시글 상세 조회
  getBoard: async (id: number): Promise<Board> => {
    const response = await apiClient.get<Board>(`/boards/${id}`);
    return response.data;
  },

  // 게시글 생성
  createBoard: async (board: BoardCreateRequest): Promise<Board> => {
    const response = await apiClient.post<Board>('/boards', board);
    return response.data;
  },

  // 게시글 수정
  updateBoard: async (id: number, board: BoardUpdateRequest): Promise<Board> => {
    const response = await apiClient.put<Board>(`/boards/${id}`, board);
    return response.data;
  },

  // 게시글 삭제
  deleteBoard: async (id: number): Promise<void> => {
    await apiClient.delete(`/boards/${id}`);
  },
};

// 기존 boardApi는 호환성을 위해 유지
export const boardApi = boardService;

// OpenStack API
export const openStackApi = {
  // VM 배포
  deployVm: async (request: OpenStackDeployRequest): Promise<OpenStackDeployResponse> => {
    const response = await apiClient.post<OpenStackDeployResponse>('/openstack/deploy', request);
    return response.data;
  },

  // 배포 상태 조회
  getDeploymentStatus: async (deploymentId: string): Promise<OpenStackDeployResponse> => {
    const response = await apiClient.get<OpenStackDeployResponse>(`/openstack/deployments/${deploymentId}`);
    return response.data;
  },

  // 모든 배포 조회
  getAllDeployments: async (): Promise<Record<string, OpenStackDeployResponse>> => {
    const response = await apiClient.get<Record<string, OpenStackDeployResponse>>('/openstack/deployments');
    return response.data;
  },

  // VM 삭제
  destroyVm: async (deploymentId: string): Promise<OpenStackDeployResponse> => {
    const response = await apiClient.delete<OpenStackDeployResponse>(`/openstack/deployments/${deploymentId}`);
    return response.data;
  },

  // 배포 취소
  cancelDeployment: async (deploymentId: string): Promise<OpenStackDeployResponse> => {
    const response = await apiClient.post<OpenStackDeployResponse>(`/openstack/deployments/${deploymentId}/cancel`);
    return response.data;
  },

  // 헬스 체크
  healthCheck: async (): Promise<any> => {
    const response = await apiClient.get<any>('/openstack/health');
    return response.data;
  },
};

// 통합 API
export const integratedApi = {
  // 시스템 상태 조회
  getSystemStatus: async (): Promise<SystemStatus> => {
    const response = await apiClient.get<SystemStatus>('/integrated/system-status');
    return response.data;
  },
};

export default apiClient; 