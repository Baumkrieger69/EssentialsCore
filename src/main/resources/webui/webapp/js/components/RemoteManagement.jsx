import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import '../css/RemoteManagement.css';

const RemoteManagement = () => {
  const [servers, setServers] = useState([]);
  const [loading, setLoading] = useState(true);
  const { token } = useAuth();

  useEffect(() => {
    fetchServers();
  }, []);

  const fetchServers = async () => {
    try {
      const response = await fetch('/api/servers', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      const data = await response.json();
      setServers(data);
      setLoading(false);
    } catch (error) {
      console.error('Fehler beim Laden der Server:', error);
      setLoading(false);
    }
  };

  const handleServerAction = async (serverId, action) => {
    try {
      await fetch(`/api/servers/${serverId}/${action}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      await fetchServers();
    } catch (error) {
      console.error(`Fehler bei der Server-Aktion ${action}:`, error);
    }
  };

  if (loading) {
    return <div>Lade Server-Informationen...</div>;
  }

  return (
    <div className="remote-management">
      <h2>Remote Server-Verwaltung</h2>
      <div className="server-grid">
        {servers.map(server => (
          <div key={server.id} className="server-card">
            <h3>{server.name}</h3>
            <div className="server-status">
              <span className={`status-indicator ${server.status.toLowerCase()}`}>
                {server.status}
              </span>
            </div>
            <div className="server-info">
              <p>IP: {server.ip}</p>
              <p>Spieler: {server.players.online}/{server.players.max}</p>
              <p>Version: {server.version}</p>
            </div>
            <div className="server-actions">
              <button 
                onClick={() => handleServerAction(server.id, 'restart')}
                className="action-button restart">
                Neustart
              </button>
              <button 
                onClick={() => handleServerAction(server.id, 'stop')}
                className="action-button stop">
                Stop
              </button>
              <button 
                onClick={() => handleServerAction(server.id, 'console')}
                className="action-button console">
                Konsole
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default RemoteManagement;
