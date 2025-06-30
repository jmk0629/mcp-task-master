import React, { useState, useEffect } from 'react';
import { integratedApi, SystemStatus } from '../services/api';

const Dashboard: React.FC = () => {
  const [systemStatus, setSystemStatus] = useState<SystemStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchSystemStatus();
    // 30초마다 상태 업데이트
    const interval = setInterval(fetchSystemStatus, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchSystemStatus = async () => {
    try {
      setLoading(true);
      const status = await integratedApi.getSystemStatus();
      setSystemStatus(status);
      setError(null);
    } catch (err) {
      setError('시스템 상태를 가져오는데 실패했습니다.');
      console.error('Error fetching system status:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatUptime = (uptime: number) => {
    const seconds = Math.floor(uptime / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) return `${days}일 ${hours % 24}시간`;
    if (hours > 0) return `${hours}시간 ${minutes % 60}분`;
    if (minutes > 0) return `${minutes}분 ${seconds % 60}초`;
    return `${seconds}초`;
  };

  if (loading) {
    return (
      <div className="dashboard">
        <h1>시스템 대시보드</h1>
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard">
        <h1>시스템 대시보드</h1>
        <div className="error">{error}</div>
        <button onClick={fetchSystemStatus}>다시 시도</button>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <h1>시스템 대시보드</h1>
      
      {systemStatus && (
        <div className="dashboard-grid">
          {/* 시스템 상태 */}
          <div className="status-card">
            <h2>시스템 상태</h2>
            <div className={`status-indicator ${systemStatus.system.status.toLowerCase()}`}>
              {systemStatus.system.status}
            </div>
            <p>업타임: {formatUptime(systemStatus.system.uptime)}</p>
          </div>

          {/* 서비스 상태 */}
          <div className="status-card">
            <h2>서비스 상태</h2>
            <div className="service-status">
              <div className="service-item">
                <span>게시판 서비스:</span>
                <span className={`status ${systemStatus.system.services.board.toLowerCase()}`}>
                  {systemStatus.system.services.board}
                </span>
              </div>
              <div className="service-item">
                <span>OpenStack 서비스:</span>
                <span className={`status ${systemStatus.system.services.openstack.toLowerCase()}`}>
                  {systemStatus.system.services.openstack}
                </span>
              </div>
            </div>
          </div>

          {/* 게시판 통계 */}
          <div className="status-card">
            <h2>게시판 통계</h2>
            <div className="stats">
              <div className="stat-item">
                <span className="stat-number">{systemStatus.boards.total}</span>
                <span className="stat-label">총 게시글</span>
              </div>
            </div>
          </div>

          {/* 배포 통계 */}
          <div className="status-card">
            <h2>VM 배포 통계</h2>
            <div className="stats">
              <div className="stat-item">
                <span className="stat-number">{systemStatus.deployments.total}</span>
                <span className="stat-label">총 배포</span>
              </div>
              <div className="deployment-status">
                {Object.entries(systemStatus.deployments.byStatus).map(([status, count]) => (
                  <div key={status} className="deployment-stat">
                    <span className={`status-badge ${status.toLowerCase()}`}>{status}</span>
                    <span>{count}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="dashboard-actions">
        <button onClick={fetchSystemStatus} className="refresh-btn">
          새로고침
        </button>
      </div>
    </div>
  );
};

export default Dashboard; 