import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { openStackApi, OpenStackDeployResponse } from '../services/api';

const OpenStackDashboard: React.FC = () => {
  const [deployments, setDeployments] = useState<Record<string, OpenStackDeployResponse>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDeployments();
    // 30초마다 상태 업데이트
    const interval = setInterval(fetchDeployments, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchDeployments = async () => {
    try {
      setLoading(true);
      const deploymentData = await openStackApi.getAllDeployments();
      setDeployments(deploymentData);
      setError(null);
    } catch (err) {
      setError('배포 목록을 가져오는데 실패했습니다.');
      console.error('Error fetching deployments:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDestroy = async (deploymentId: string) => {
    if (!window.confirm('정말로 이 VM을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await openStackApi.destroyVm(deploymentId);
      alert('VM 삭제 요청이 전송되었습니다.');
      fetchDeployments(); // 목록 새로고침
    } catch (err) {
      alert('VM 삭제 요청에 실패했습니다.');
      console.error('Error destroying VM:', err);
    }
  };

  const handleCancel = async (deploymentId: string) => {
    if (!window.confirm('정말로 이 배포를 취소하시겠습니까?')) {
      return;
    }

    try {
      await openStackApi.cancelDeployment(deploymentId);
      alert('배포가 취소되었습니다.');
      fetchDeployments(); // 목록 새로고침
    } catch (err) {
      alert('배포 취소에 실패했습니다.');
      console.error('Error cancelling deployment:', err);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'FAILED': return 'danger';
      case 'IN_PROGRESS': return 'warning';
      case 'PENDING': return 'info';
      case 'CANCELLED': return 'secondary';
      default: return 'secondary';
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

  const deploymentList = Object.values(deployments);
  const stats = {
    total: deploymentList.length,
    completed: deploymentList.filter(d => d.status === 'COMPLETED').length,
    failed: deploymentList.filter(d => d.status === 'FAILED').length,
    inProgress: deploymentList.filter(d => d.status === 'IN_PROGRESS').length,
    pending: deploymentList.filter(d => d.status === 'PENDING').length
  };

  if (loading) {
    return (
      <div className="openstack-dashboard">
        <h1>OpenStack 대시보드</h1>
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="openstack-dashboard">
        <h1>OpenStack 대시보드</h1>
        <div className="error">{error}</div>
        <button onClick={fetchDeployments}>다시 시도</button>
      </div>
    );
  }

  return (
    <div className="openstack-dashboard">
      <div className="dashboard-header">
        <h1>OpenStack 대시보드</h1>
        <Link to="/deploy" className="btn btn-primary">
          새 VM 배포
        </Link>
      </div>

      {/* 통계 카드 */}
      <div className="stats-grid">
        <div className="stat-card">
          <h3>총 배포</h3>
          <div className="stat-number">{stats.total}</div>
        </div>
        <div className="stat-card success">
          <h3>완료</h3>
          <div className="stat-number">{stats.completed}</div>
        </div>
        <div className="stat-card warning">
          <h3>진행중</h3>
          <div className="stat-number">{stats.inProgress}</div>
        </div>
        <div className="stat-card info">
          <h3>대기중</h3>
          <div className="stat-number">{stats.pending}</div>
        </div>
        <div className="stat-card danger">
          <h3>실패</h3>
          <div className="stat-number">{stats.failed}</div>
        </div>
      </div>

      {/* 배포 목록 */}
      {deploymentList.length === 0 ? (
        <div className="empty-state">
          <p>배포된 VM이 없습니다.</p>
          <Link to="/deploy" className="btn btn-primary">
            첫 번째 VM을 배포해보세요
          </Link>
        </div>
      ) : (
        <div className="deployments-table">
          <h2>VM 배포 목록</h2>
          <table>
            <thead>
              <tr>
                <th>배포 ID</th>
                <th>VM 이름</th>
                <th>상태</th>
                <th>상태 메시지</th>
                <th>생성일</th>
                <th>완료일</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              {deploymentList.map((deployment) => (
                <tr key={deployment.deploymentId}>
                  <td>
                    <code>{deployment.deploymentId}</code>
                  </td>
                  <td>{deployment.vmName}</td>
                  <td>
                    <span className={`status-badge ${getStatusColor(deployment.status)}`}>
                      {deployment.status}
                    </span>
                  </td>
                  <td>{deployment.statusMessage}</td>
                  <td>{formatDate(deployment.createdAt)}</td>
                  <td>
                    {deployment.completedAt ? formatDate(deployment.completedAt) : '-'}
                  </td>
                  <td>
                    <div className="action-buttons">
                      {deployment.status === 'COMPLETED' && (
                        <button 
                          onClick={() => handleDestroy(deployment.deploymentId)}
                          className="btn btn-sm btn-danger"
                        >
                          삭제
                        </button>
                      )}
                      {(deployment.status === 'PENDING' || deployment.status === 'IN_PROGRESS') && (
                        <button 
                          onClick={() => handleCancel(deployment.deploymentId)}
                          className="btn btn-sm btn-secondary"
                        >
                          취소
                        </button>
                      )}
                      {deployment.vmInfo && (
                        <div className="vm-info">
                          {deployment.vmInfo.publicIp && (
                            <small>Public IP: {deployment.vmInfo.publicIp}</small>
                          )}
                          {deployment.vmInfo.privateIp && (
                            <small>Private IP: {deployment.vmInfo.privateIp}</small>
                          )}
                        </div>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="dashboard-actions">
        <button onClick={fetchDeployments} className="btn btn-secondary">
          새로고침
        </button>
      </div>
    </div>
  );
};

export default OpenStackDashboard; 