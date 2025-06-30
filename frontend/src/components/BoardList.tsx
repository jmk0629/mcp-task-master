import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { boardApi, Board } from '../services/api';

const BoardList: React.FC = () => {
  const [boards, setBoards] = useState<Board[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchBoards();
  }, []);

  const fetchBoards = async () => {
    try {
      setLoading(true);
      const boardList = await boardApi.getAllBoards();
      setBoards(boardList);
      setError(null);
    } catch (err) {
      setError('게시글을 가져오는데 실패했습니다.');
      console.error('Error fetching boards:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
      try {
        await boardApi.deleteBoard(id);
        setBoards(boards.filter(board => board.id !== id));
      } catch (err) {
        alert('게시글 삭제에 실패했습니다.');
        console.error('Error deleting board:', err);
      }
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
      <div className="board-list">
        <h1>게시판</h1>
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="board-list">
        <h1>게시판</h1>
        <div className="error">{error}</div>
        <button onClick={fetchBoards}>다시 시도</button>
      </div>
    );
  }

  return (
    <div className="board-list">
      <div className="board-header">
        <h1>게시판</h1>
        <Link to="/boards/new" className="btn btn-primary">
          새 게시글 작성
        </Link>
      </div>

      {boards.length === 0 ? (
        <div className="empty-state">
          <p>게시글이 없습니다.</p>
          <Link to="/boards/new" className="btn btn-primary">
            첫 번째 게시글을 작성해보세요
          </Link>
        </div>
      ) : (
        <div className="board-table">
          <table>
            <thead>
              <tr>
                <th>번호</th>
                <th>제목</th>
                <th>작성자</th>
                <th>조회수</th>
                <th>작성일</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              {boards.map((board) => (
                <tr key={board.id}>
                  <td>{board.id}</td>
                  <td>
                    <Link to={`/boards/${board.id}`} className="board-title">
                      {board.title}
                    </Link>
                  </td>
                                  <td>{board.author}</td>
                  <td>{formatDate(board.createdAt)}</td>
                  <td>
                    <div className="action-buttons">
                      <Link 
                        to={`/boards/${board.id}/edit`} 
                        className="btn btn-sm btn-secondary"
                      >
                        수정
                      </Link>
                      <button 
                        onClick={() => handleDelete(board.id)}
                        className="btn btn-sm btn-danger"
                      >
                        삭제
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default BoardList; 