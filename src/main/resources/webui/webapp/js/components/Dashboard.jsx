import React from 'react';
import { useWebSocket } from '../contexts/WebSocketContext';
import { Card } from './ui/Card';
import { Table } from './ui/Table';
import { Chart } from './ui/Chart';

const Dashboard = () => {
  const { serverStats, playerStats } = useWebSocket();

  return (
    <div className="dashboard">
      <h1>Server Dashboard</h1>
      
      <div className="stats-grid">
        <Card title="Performance">
          <Chart
            data={serverStats.performance}
            type="line"
            options={{
              labels: ['TPS', 'Memory Usage', 'CPU Load'],
              responsive: true
            }}
          />
        </Card>

        <Card title="Online Players">
          <Table
            data={playerStats.online}
            columns={[
              { header: 'Name', field: 'name' },
              { header: 'World', field: 'world' },
              { header: 'Ping', field: 'ping' }
            ]}
          />
        </Card>

        <Card title="Active Modules">
          <Table
            data={serverStats.modules}
            columns={[
              { header: 'Name', field: 'name' },
              { header: 'Status', field: 'status' },
              { header: 'Memory', field: 'memory' }
            ]}
          />
        </Card>

        <Card title="Recent Events">
          <div className="event-log">
            {serverStats.recentEvents.map((event, index) => (
              <div key={index} className="event-item">
                <span className="event-time">{event.time}</span>
                <span className={`event-type event-type-${event.type}`}>
                  {event.type}
                </span>
                <span className="event-message">{event.message}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
};

export default Dashboard;
