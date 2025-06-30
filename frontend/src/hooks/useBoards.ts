import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { boardService } from '../services/api';
import { Board, BoardCreateRequest, BoardUpdateRequest } from '../types';
import { useAppStore } from '../stores/useAppStore';

// 쿼리 키
export const BOARD_QUERY_KEYS = {
  all: ['boards'] as const,
  lists: () => [...BOARD_QUERY_KEYS.all, 'list'] as const,
  list: (filters: string) => [...BOARD_QUERY_KEYS.lists(), { filters }] as const,
  details: () => [...BOARD_QUERY_KEYS.all, 'detail'] as const,
  detail: (id: number) => [...BOARD_QUERY_KEYS.details(), id] as const,
};

// 게시글 목록 조회
export const useBoards = () => {
  return useQuery({
    queryKey: BOARD_QUERY_KEYS.lists(),
    queryFn: boardService.getAllBoards,
    staleTime: 5 * 60 * 1000, // 5분
  });
};

// 게시글 상세 조회
export const useBoard = (id: number) => {
  return useQuery({
    queryKey: BOARD_QUERY_KEYS.detail(id),
    queryFn: () => boardService.getBoard(id),
    enabled: !!id,
  });
};

// 게시글 생성
export const useCreateBoard = () => {
  const queryClient = useQueryClient();
  const addNotification = useAppStore((state) => state.addNotification);

  return useMutation({
    mutationFn: (board: BoardCreateRequest) => boardService.createBoard(board),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: BOARD_QUERY_KEYS.lists() });
      addNotification({
        type: 'success',
        message: '게시글이 성공적으로 작성되었습니다.',
      });
    },
    onError: (error: Error) => {
      addNotification({
        type: 'error',
        message: `게시글 작성에 실패했습니다: ${error.message}`,
      });
    },
  });
};

// 게시글 수정
export const useUpdateBoard = () => {
  const queryClient = useQueryClient();
  const addNotification = useAppStore((state) => state.addNotification);

  return useMutation({
    mutationFn: ({ id, board }: { id: number; board: BoardUpdateRequest }) =>
      boardService.updateBoard(id, board),
    onSuccess: (data: Board) => {
      queryClient.invalidateQueries({ queryKey: BOARD_QUERY_KEYS.lists() });
      queryClient.invalidateQueries({ queryKey: BOARD_QUERY_KEYS.detail(data.id) });
      addNotification({
        type: 'success',
        message: '게시글이 성공적으로 수정되었습니다.',
      });
    },
    onError: (error: Error) => {
      addNotification({
        type: 'error',
        message: `게시글 수정에 실패했습니다: ${error.message}`,
      });
    },
  });
};

// 게시글 삭제
export const useDeleteBoard = () => {
  const queryClient = useQueryClient();
  const addNotification = useAppStore((state) => state.addNotification);

  return useMutation({
    mutationFn: (id: number) => boardService.deleteBoard(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: BOARD_QUERY_KEYS.lists() });
      addNotification({
        type: 'success',
        message: '게시글이 성공적으로 삭제되었습니다.',
      });
    },
    onError: (error: Error) => {
      addNotification({
        type: 'error',
        message: `게시글 삭제에 실패했습니다: ${error.message}`,
      });
    },
  });
}; 