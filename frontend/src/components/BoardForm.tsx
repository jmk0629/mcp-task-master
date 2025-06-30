import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { boardApi, Board, BoardCreateRequest } from '../services/api';

const BoardForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [formData, setFormData] = useState<BoardCreateRequest>({
    title: '',
    content: '',
    author: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isEdit && id) {
      fetchBoard(parseInt(id));
    }
  }, [isEdit, id]);

  const fetchBoard = async (boardId: number) => {
    try {
      setLoading(true);
      const board = await boardApi.getBoard(boardId);
      setFormData({
        title: board.title,
        content: board.content,
        author: board.author
      });
      setError(null);
    } catch (err) {
      setError('게시글을 가져오는데 실패했습니다.');
      console.error('Error fetching board:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.title.trim() || !formData.content.trim() || !formData.author.trim()) {
      alert('모든 필드를 입력해주세요.');
      return;
    }

    try {
      setLoading(true);
      
      if (isEdit && id) {
        await boardApi.updateBoard(parseInt(id), formData);
        alert('게시글이 수정되었습니다.');
        navigate(`/boards/${id}`);
      } else {
        const newBoard = await boardApi.createBoard(formData);
        alert('게시글이 작성되었습니다.');
        navigate(`/boards/${newBoard.id}`);
      }
    } catch (err) {
      alert(isEdit ? '게시글 수정에 실패했습니다.' : '게시글 작성에 실패했습니다.');
      console.error('Error saving board:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    if (isEdit && id) {
      navigate(`/boards/${id}`);
    } else {
      navigate('/boards');
    }
  };

  if (loading && isEdit) {
    return (
      <div className="board-form">
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="board-form">
        <div className="error">{error}</div>
        <button onClick={handleCancel} className="btn btn-secondary">
          돌아가기
        </button>
      </div>
    );
  }

  return (
    <div className="board-form">
      <h1>{isEdit ? '게시글 수정' : '새 게시글 작성'}</h1>
      
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="title">제목</label>
          <input
            type="text"
            id="title"
            name="title"
            value={formData.title}
            onChange={handleChange}
            placeholder="게시글 제목을 입력하세요"
            required
            maxLength={200}
          />
        </div>

        <div className="form-group">
          <label htmlFor="author">작성자</label>
          <input
            type="text"
            id="author"
            name="author"
            value={formData.author}
            onChange={handleChange}
            placeholder="작성자명을 입력하세요"
            required
            maxLength={50}
            disabled={isEdit} // 수정 시에는 작성자 변경 불가
          />
        </div>

        <div className="form-group">
          <label htmlFor="content">내용</label>
          <textarea
            id="content"
            name="content"
            value={formData.content}
            onChange={handleChange}
            placeholder="게시글 내용을 입력하세요"
            required
            rows={15}
          />
        </div>

        <div className="form-actions">
          <button 
            type="submit" 
            className="btn btn-primary"
            disabled={loading}
          >
            {loading ? '저장 중...' : (isEdit ? '수정' : '작성')}
          </button>
          <button 
            type="button" 
            onClick={handleCancel}
            className="btn btn-secondary"
            disabled={loading}
          >
            취소
          </button>
        </div>
      </form>
    </div>
  );
};

export default BoardForm; 