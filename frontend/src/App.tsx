import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { QueryProvider } from './contexts/QueryProvider';
import { applyTheme } from './styles/theme';
import './styles/global.css';
import './App.css';

// Components
import { Button, Card, CardHeader, CardBody, Input } from './components/ui';

// Pages
import { BoardsPage } from './pages/BoardsPage';
import { BoardDetailPage } from './pages/BoardDetailPage';
import { BoardFormPage } from './pages/BoardFormPage';

function App() {
  useEffect(() => {
    applyTheme();
  }, []);

  return (
    <QueryProvider>
      <Router>
        <div className="app">
          <header className="app-header">
            <div className="container">
              <h1 className="app-title">BoardStack</h1>
              <nav className="app-nav">
                <Button variant="outline" size="sm">게시판</Button>
                <Button variant="outline" size="sm">VM 대시보드</Button>
              </nav>
            </div>
          </header>

          <main className="app-main">
            <div className="container">
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/boards" element={<BoardsPage />} />
                <Route path="/boards/new" element={<BoardFormPage />} />
                <Route path="/boards/:id" element={<BoardDetailPage />} />
                <Route path="/boards/:id/edit" element={<BoardFormPage />} />
                <Route path="/dashboard" element={<DashboardPage />} />
              </Routes>
            </div>
          </main>

          <footer className="app-footer">
            <div className="container">
              <p className="text-secondary text-sm">
                © 2024 BoardStack. OpenStack VM 관리 시스템
              </p>
            </div>
          </footer>
        </div>
      </Router>
    </QueryProvider>
  );
}

// Temporary page components for demonstration
const HomePage: React.FC = () => (
  <div className="page">
    <div className="page-header">
      <h2 className="page-title">환영합니다!</h2>
      <p className="page-description">
        게시판과 OpenStack VM을 통합 관리하는 시스템입니다.
      </p>
    </div>
    
    <div className="grid grid-cols-1 md:grid-cols-2 gap-lg">
      <Card variant="elevated" hoverable>
        <CardHeader>
          <h3 className="text-xl font-semibold">게시판 관리</h3>
        </CardHeader>
        <CardBody>
          <p className="text-secondary mb-md">
            게시글을 작성하고 관리하세요. 각 게시글에 VM 배포 요청을 연결할 수 있습니다.
          </p>
          <Button variant="primary" fullWidth>게시판 보기</Button>
        </CardBody>
      </Card>

      <Card variant="elevated" hoverable>
        <CardHeader>
          <h3 className="text-xl font-semibold">VM 대시보드</h3>
        </CardHeader>
        <CardBody>
          <p className="text-secondary mb-md">
            OpenStack VM의 상태를 실시간으로 모니터링하고 관리하세요.
          </p>
          <Button variant="primary" fullWidth>대시보드 보기</Button>
        </CardBody>
      </Card>
    </div>
  </div>
);

const DashboardPage: React.FC = () => (
  <div className="page">
    <div className="page-header">
      <h2 className="page-title">VM 대시보드</h2>
      <Button variant="primary">새 VM 배포</Button>
    </div>
    
    <Card>
      <CardBody>
        <p className="text-secondary">VM 대시보드가 곧 구현됩니다...</p>
      </CardBody>
    </Card>
  </div>
);

export default App;
