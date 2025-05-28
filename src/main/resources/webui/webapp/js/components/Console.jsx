import React, { useEffect, useRef } from 'react';
import { useWebSocket } from '../contexts/WebSocketContext';
import { Button } from './ui/Button';

const Console = () => {
  const { consoleOutput, sendCommand } = useWebSocket();
  const consoleEndRef = useRef(null);
  const [command, setCommand] = useState('');

  useEffect(() => {
    // Auto-scroll to bottom when new console output arrives
    consoleEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [consoleOutput]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (command.trim()) {
      sendCommand(command);
      setCommand('');
    }
  };

  return (
    <div className="console">
      <h1>Server Console</h1>
      
      <div className="console-output">
        {consoleOutput.map((line, index) => (
          <div key={index} className={`console-line ${line.type}`}>
            <span className="timestamp">{line.timestamp}</span>
            <span className="content">{line.content}</span>
          </div>
        ))}
        <div ref={consoleEndRef} />
      </div>

      <form onSubmit={handleSubmit} className="console-input">
        <input
          type="text"
          value={command}
          onChange={(e) => setCommand(e.target.value)}
          placeholder="Enter command..."
          className="form-control"
        />
        <Button type="submit">Send</Button>
      </form>
    </div>
  );
};

export default Console;
