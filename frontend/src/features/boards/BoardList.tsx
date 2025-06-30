import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useBoards, useDeleteBoard } from '../../hooks/useBoards';
import { Button, Card, CardBody, Input } from '../../components/ui';
import { formatDate, formatRelativeTime, truncateText } from '../../utils/helpers';
import { Board } from '../../types';
import './BoardList.css';

interface BoardListProps {
  onCreateClick?: () => void;
}

export const BoardList: React.FC<BoardListProps> = ({ onCreateClick }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);
  
  const { data: boards = [], isLoading, error } = useBoards();
  const deleteBoard = useDeleteBoard();

  // 검색 필터링
  const filteredBoards = boards.filter(board =>
    board.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    board.content.toLowerCase().includes(searchTerm.toLowerCase()) ||
    board.author.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleDelete = async (id: number) => {
    if (deleteConfirm === id) {
      await deleteBoard.mutateAsync(id);
      setDeleteConfirm(null);
    } else {
      setDeleteConfirm(id);
    }
  };

  if (isLoading) {
    return (
      <div className="board-list-loading">
        <div className="loading-spinner" />
        <p>게시글을 불러오는 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <Card variant="outlined">
        <CardBody>
          <div className="board-list-error">
            <p className="text-error">게시글을 불러오는데 실패했습니다.</p>
            <p className="text-secondary text-sm">
              {error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'}
            </p>
          </div>
        </CardBody>
      </Card>
    );
  }

  return (
    <div className="board-list">
      {/* 헤더 */}
      <div className="board-list-header">
        <div className="board-list-title">
          <h2>게시판</h2>
          <span className="board-count">{filteredBoards.length}개의 게시글</span>
        </div>
        <div className="board-list-actions">
          <Input
            placeholder="제목, 내용, 작성자로 검색..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            size="sm"
            className="search-input"
          />
          <Button variant="primary" onClick={onCreateClick}>
            새 게시글 작성
          </Button>
        </div>
      </div>

      {/* 게시글 목록 */}
      {filteredBoards.length === 0 ? (
        <Card>
          <CardBody>
            <div className="board-list-empty">
              {searchTerm ? (
                <>
                  <p>검색 결과가 없습니다.</p>
                  <p className="text-secondary text-sm">
                    다른 검색어를 시도해보세요.
                  </p>
                </>
              ) : (
                <>
                  <p>아직 작성된 게시글이 없습니다.</p>
                  <p className="text-secondary text-sm">
                    첫 번째 게시글을 작성해보세요!
                  </p>
                  <Button variant="primary" onClick={onCreateClick} className="mt-md">
                    게시글 작성하기
                  </Button>
                </>
              )}
            </div>
          </CardBody>
        </Card>
      ) : (
        <div className="board-list-items">
          {filteredBoards.map((board) => (
            <BoardListItem
              key={board.id}
              board={board}
              onDelete={() => handleDelete(board.id)}
              deleteConfirm={deleteConfirm === board.id}
              onCancelDelete={() => setDeleteConfirm(null)}
              isDeleting={deleteBoard.isPending}
            />
          ))}
        </div>
      )}
    </div>
  );
};

interface BoardListItemProps {
  board: Board;
  onDelete: () => void;
  deleteConfirm: boolean;
  onCancelDelete: () => void;
  isDeleting: boolean;
}

const BoardListItem: React.FC<BoardListItemProps> = ({
  board,
  onDelete,
  deleteConfirm,
  onCancelDelete,
  isDeleting,
}) => {
  return (
    <Card variant="outlined" hoverable className="board-list-item">
      <CardBody>
        <div className="board-item-content">
          <div className="board-item-main">
            <Link to={`/boards/${board.id}`} className="board-item-title">
              {board.title}
            </Link>
            <p className="board-item-preview">
              {truncateText(board.content, 150)}
            </p>
            <div className="board-item-meta">
              <span className="board-item-author">{board.author}</span>
              <span className="board-item-date" title={formatDate(board.createdAt)}>
                {formatRelativeTime(board.createdAt)}
              </span>
              {board.vmRequest && (
                <span className="board-item-vm-badge">VM 연동</span>
              )}
            </div>
          </div>
          
          <div className="board-item-actions">
            <Link to={`/boards/${board.id}/edit`}>
              <Button variant="outline" size="sm">수정</Button>
            </Link>
            
            {deleteConfirm ? (
              <div className="delete-confirm">
                <Button
                  variant="error"
                  size="sm"
                  onClick={onDelete}
                  loading={isDeleting}
                >
                  삭제 확인
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={onCancelDelete}
                  disabled={isDeleting}
                >
                  취소
                </Button>
              </div>
            ) : (
              <Button
                variant="outline"
                size="sm"
                onClick={onDelete}
                disabled={isDeleting}
              >
                삭제
              </Button>
            )}
          </div>
        </div>
      </CardBody>
    </Card>
  );
}; 