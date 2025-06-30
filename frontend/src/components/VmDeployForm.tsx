import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { openStackApi, OpenStackDeployRequest } from '../services/api';

const VmDeployForm: React.FC = () => {
  const navigate = useNavigate();
  
  const [formData, setFormData] = useState<OpenStackDeployRequest>({
    vmName: '',
    instanceType: 'm1.small',
    imageId: 'ubuntu-20.04',
    networkId: 'default-network',
    securityGroup: 'default',
    keyPair: 'default-keypair',
    diskSize: 20,
    description: '',
    requestedBy: ''
  });
  
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'number' ? parseInt(value) || 0 : value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.vmName.trim() || !formData.requestedBy.trim()) {
      alert('VM 이름과 요청자는 필수 입력 항목입니다.');
      return;
    }

    try {
      setLoading(true);
      const response = await openStackApi.deployVm(formData);
      alert(`VM 배포 요청이 성공적으로 전송되었습니다.\n배포 ID: ${response.deploymentId}`);
      navigate('/openstack');
    } catch (err) {
      alert('VM 배포 요청에 실패했습니다.');
      console.error('Error deploying VM:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate('/openstack');
  };

  return (
    <div className="vm-deploy-form">
      <h1>새 VM 배포</h1>
      
      <form onSubmit={handleSubmit}>
        <div className="form-row">
          <div className="form-group">
            <label htmlFor="vmName">VM 이름 *</label>
            <input
              type="text"
              id="vmName"
              name="vmName"
              value={formData.vmName}
              onChange={handleChange}
              placeholder="예: web-server-01"
              required
              pattern="^[a-zA-Z0-9-_]+$"
              title="영문, 숫자, 하이픈, 언더스코어만 사용 가능합니다"
              maxLength={50}
            />
          </div>

          <div className="form-group">
            <label htmlFor="requestedBy">요청자 *</label>
            <input
              type="text"
              id="requestedBy"
              name="requestedBy"
              value={formData.requestedBy}
              onChange={handleChange}
              placeholder="요청자명을 입력하세요"
              required
              maxLength={100}
            />
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="instanceType">인스턴스 타입</label>
            <select
              id="instanceType"
              name="instanceType"
              value={formData.instanceType}
              onChange={handleChange}
            >
              <option value="m1.tiny">m1.tiny (1 vCPU, 512MB RAM)</option>
              <option value="m1.small">m1.small (1 vCPU, 2GB RAM)</option>
              <option value="m1.medium">m1.medium (2 vCPU, 4GB RAM)</option>
              <option value="m1.large">m1.large (4 vCPU, 8GB RAM)</option>
              <option value="m1.xlarge">m1.xlarge (8 vCPU, 16GB RAM)</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="diskSize">디스크 크기 (GB)</label>
            <input
              type="number"
              id="diskSize"
              name="diskSize"
              value={formData.diskSize}
              onChange={handleChange}
              min="10"
              max="1000"
              required
            />
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="imageId">이미지</label>
            <select
              id="imageId"
              name="imageId"
              value={formData.imageId}
              onChange={handleChange}
            >
              <option value="ubuntu-20.04">Ubuntu 20.04 LTS</option>
              <option value="ubuntu-22.04">Ubuntu 22.04 LTS</option>
              <option value="centos-7">CentOS 7</option>
              <option value="centos-8">CentOS 8</option>
              <option value="debian-11">Debian 11</option>
              <option value="windows-server-2019">Windows Server 2019</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="networkId">네트워크</label>
            <select
              id="networkId"
              name="networkId"
              value={formData.networkId}
              onChange={handleChange}
            >
              <option value="default-network">기본 네트워크</option>
              <option value="public-network">퍼블릭 네트워크</option>
              <option value="private-network">프라이빗 네트워크</option>
              <option value="dmz-network">DMZ 네트워크</option>
            </select>
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="securityGroup">보안 그룹</label>
            <select
              id="securityGroup"
              name="securityGroup"
              value={formData.securityGroup}
              onChange={handleChange}
            >
              <option value="default">기본 보안 그룹</option>
              <option value="web-server">웹 서버 (HTTP/HTTPS)</option>
              <option value="database">데이터베이스 (MySQL/PostgreSQL)</option>
              <option value="ssh-only">SSH 전용</option>
              <option value="custom">사용자 정의</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="keyPair">키페어</label>
            <select
              id="keyPair"
              name="keyPair"
              value={formData.keyPair}
              onChange={handleChange}
            >
              <option value="default-keypair">기본 키페어</option>
              <option value="admin-keypair">관리자 키페어</option>
              <option value="dev-keypair">개발자 키페어</option>
              <option value="prod-keypair">운영 키페어</option>
            </select>
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="description">설명</label>
          <textarea
            id="description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            placeholder="VM의 용도나 특별한 설정에 대한 설명을 입력하세요"
            rows={4}
            maxLength={500}
          />
        </div>

        <div className="form-actions">
          <button 
            type="submit" 
            className="btn btn-primary"
            disabled={loading}
          >
            {loading ? '배포 요청 중...' : 'VM 배포 요청'}
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

      <div className="deploy-info">
        <h3>배포 정보</h3>
        <ul>
          <li>VM 배포는 일반적으로 5-10분 정도 소요됩니다.</li>
          <li>배포 상태는 OpenStack 대시보드에서 실시간으로 확인할 수 있습니다.</li>
          <li>배포 완료 후 SSH 접속 정보가 제공됩니다.</li>
          <li>문제가 발생하면 시스템 관리자에게 문의하세요.</li>
        </ul>
      </div>
    </div>
  );
};

export default VmDeployForm; 