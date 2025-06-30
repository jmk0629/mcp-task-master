import React from 'react';
import { useNavigate } from 'react-router-dom';
import { BoardList } from '../features/boards/BoardList';

export const BoardsPage: React.FC = () => {
  const navigate = useNavigate();

  const handleCreateClick = () => {
    navigate('/boards/new');
  };

  return (
    <div className="page">
      <BoardList onCreateClick={handleCreateClick} />
    </div>
  );
}; 