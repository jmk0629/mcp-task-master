import React from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useBoard, useDeleteBoard } from '../hooks/useBoards';
import { Button, Card, CardHeader, CardBody } from '../components/ui';
import { formatDate } from '../utils/helpers';

export const BoardDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const boardId = parseInt(id || '0', 10);
  
  const { data: board, isLoading, error } = useBoard(boardId);
  const deleteBoard = useDeleteBoard();

  const handleDelete = async () => {
    if (window.confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
      await deleteBoard.mutateAsync(boardId);
      navigate('/boards');
    }
  };

  if (isLoading) {
    return (
      <div className="page">
        <div className="loading-container">
          <div className="loading-spinner" />
          <p>게시글을 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (error || !board) {
    return (
      <div className="page">
        <Card variant="outlined">
          <CardBody>
            <div className="error-container">
              <p className="text-error">게시글을 불러오는데 실패했습니다.</p>
              <p className="text-secondary text-sm">
                {error instanceof Error ? error.message : '게시글을 찾을 수 없습니다.'}
              </p>
              <Link to="/boards">
                <Button variant="outline" className="mt-md">목록으로 돌아가기</Button>
              </Link>
            </div>
          </CardBody>
        </Card>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <div className="page-title-section">
          <h1 className="page-title">{board.title}</h1>
          <div className="board-meta">
            <span className="board-author">작성자: {board.author}</span>
            <span className="board-date">작성일: {formatDate(board.createdAt)}</span>
            {board.updatedAt !== board.createdAt && (
              <span className="board-updated">수정일: {formatDate(board.updatedAt)}</span>
            )}
          </div>
        </div>
        <div className="page-actions">
          <Link to="/boards">
            <Button variant="outline" size="sm">목록</Button>
          </Link>
          <Link to={`/boards/${board.id}/edit`}>
            <Button variant="outline" size="sm">수정</Button>
          </Link>
          <Button 
            variant="error" 
            size="sm" 
            onClick={handleDelete}
            loading={deleteBoard.isPending}
          >
            삭제
          </Button>
        </div>
      </div>

      <Card>
        <CardBody>
          <div className="board-content">
            <div className="content-body">
              {board.content.split('\n').map((line: string, index: number) => (
                <p key={index}>{line}</p>
              ))}
            </div>
          </div>
        </CardBody>
      </Card>

      {board.vmRequest && (
        <Card className="mt-lg">
          <CardHeader>
            <h3>연결된 VM 요청</h3>
          </CardHeader>
          <CardBody>
            <div className="vm-request-info">
              <div className="vm-info-grid">
                <div className="vm-info-item">
                  <label>VM 이름:</label>
                  <span>{board.vmRequest.name}</span>
                </div>
                <div className="vm-info-item">
                  <label>Flavor:</label>
                  <span>{board.vmRequest.flavor}</span>
                </div>
                <div className="vm-info-item">
                  <label>이미지:</label>
                  <span>{board.vmRequest.image}</span>
                </div>
                <div className="vm-info-item">
                  <label>네트워크:</label>
                  <span>{board.vmRequest.network}</span>
                </div>
                <div className="vm-info-item">
                  <label>보안 그룹:</label>
                  <span>{board.vmRequest.securityGroups.join(', ')}</span>
                </div>
                {board.vmRequest.keyPair && (
                  <div className="vm-info-item">
                    <label>키 페어:</label>
                    <span>{board.vmRequest.keyPair}</span>
                  </div>
                )}
              </div>
              {board.vmRequest.userData && (
                <div className="vm-description">
                  <label>사용자 데이터:</label>
                  <pre>{board.vmRequest.userData}</pre>
                </div>
              )}
            </div>
          </CardBody>
        </Card>
      )}
    </div>
  );
}; 