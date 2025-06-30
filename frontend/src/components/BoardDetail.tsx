import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { boardApi, Board } from '../services/api';

const BoardDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [board, setBoard] = useState<Board | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      fetchBoard(parseInt(id));
    }
  }, [id]);

  const fetchBoard = async (boardId: number) => {
    try {
      setLoading(true);
      const boardData = await boardApi.getBoard(boardId);
      setBoard(boardData);
      setError(null);
    } catch (err) {
      setError('게시글을 가져오는데 실패했습니다.');
      console.error('Error fetching board:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!board || !window.confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await boardApi.deleteBoard(board.id);
      alert('게시글이 삭제되었습니다.');
      navigate('/boards');
    } catch (err) {
      alert('게시글 삭제에 실패했습니다.');
      console.error('Error deleting board:', err);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="board-detail">
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  if (error || !board) {
    return (
      <div className="board-detail">
        <div className="error">{error || '게시글을 찾을 수 없습니다.'}</div>
        <Link to="/boards" className="btn btn-secondary">
          목록으로 돌아가기
        </Link>
      </div>
    );
  }

  return (
    <div className="board-detail">
      <div className="board-header">
        <div className="board-meta">
          <h1>{board.title}</h1>
          <div className="meta-info">
            <span className="author">작성자: {board.author}</span>
            <span className="date">작성일: {formatDate(board.createdAt)}</span>
            {board.updatedAt !== board.createdAt && (
              <span className="updated">수정일: {formatDate(board.updatedAt)}</span>
            )}
          </div>
        </div>
        
        <div className="action-buttons">
          <Link to={`/boards/${board.id}/edit`} className="btn btn-secondary">
            수정
          </Link>
          <button onClick={handleDelete} className="btn btn-danger">
            삭제
          </button>
        </div>
      </div>

      <div className="board-content">
        <div className="content-body">
          {board.content.split('\n').map((line, index) => (
            <p key={index}>{line}</p>
          ))}
        </div>
      </div>

      <div className="board-footer">
        <Link to="/boards" className="btn btn-secondary">
          목록으로 돌아가기
        </Link>
      </div>
    </div>
  );
};

export default BoardDetail; 