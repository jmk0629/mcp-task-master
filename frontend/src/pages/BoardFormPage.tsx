import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useBoard, useCreateBoard, useUpdateBoard } from '../hooks/useBoards';
import { Button, Card, CardHeader, CardBody, Input } from '../components/ui';
import { BoardCreateRequest, BoardUpdateRequest } from '../types';

export const BoardFormPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = id !== 'new';
  const boardId = isEdit ? parseInt(id || '0', 10) : 0;

  const [formData, setFormData] = useState<BoardCreateRequest>({
    title: '',
    content: '',
    author: '',
  });

  const { data: board, isLoading: boardLoading } = useBoard(boardId);
  const createBoard = useCreateBoard();
  const updateBoard = useUpdateBoard();

  useEffect(() => {
    if (isEdit && board) {
      setFormData({
        title: board.title,
        content: board.content,
        author: board.author,
        vmRequest: board.vmRequest,
      });
    }
  }, [isEdit, board]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      if (isEdit) {
        const updateData: BoardUpdateRequest = {
          title: formData.title,
          content: formData.content,
          vmRequest: formData.vmRequest,
        };
        await updateBoard.mutateAsync({ id: boardId, board: updateData });
        navigate(`/boards/${boardId}`);
      } else {
        const newBoard = await createBoard.mutateAsync(formData);
        navigate(`/boards/${newBoard.id}`);
      }
    } catch (error) {
      console.error('게시글 저장 실패:', error);
    }
  };

  const isLoading = boardLoading || createBoard.isPending || updateBoard.isPending;

  if (isEdit && boardLoading) {
    return (
      <div className="page">
        <div className="loading-container">
          <div className="loading-spinner" />
          <p>게시글을 불러오는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">
          {isEdit ? '게시글 수정' : '새 게시글 작성'}
        </h1>
      </div>

      <Card>
        <CardHeader>
          <h2>{isEdit ? '게시글 수정' : '게시글 작성'}</h2>
        </CardHeader>
        <CardBody>
          <form onSubmit={handleSubmit} className="board-form">
            <div className="form-group">
              <Input
                label="제목"
                name="title"
                value={formData.title}
                onChange={handleChange}
                placeholder="게시글 제목을 입력하세요"
                required
                disabled={isLoading}
              />
            </div>

            <div className="form-group">
              <Input
                label="작성자"
                name="author"
                value={formData.author}
                onChange={handleChange}
                placeholder="작성자명을 입력하세요"
                required
                disabled={isEdit || isLoading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="content" className="form-label">내용</label>
              <textarea
                id="content"
                name="content"
                value={formData.content}
                onChange={handleChange}
                placeholder="게시글 내용을 입력하세요"
                required
                disabled={isLoading}
                className="form-textarea"
                rows={10}
              />
            </div>

            <div className="form-actions">
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate(isEdit ? `/boards/${boardId}` : '/boards')}
                disabled={isLoading}
              >
                취소
              </Button>
              <Button
                type="submit"
                variant="primary"
                loading={isLoading}
              >
                {isEdit ? '수정' : '작성'}
              </Button>
            </div>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}; 