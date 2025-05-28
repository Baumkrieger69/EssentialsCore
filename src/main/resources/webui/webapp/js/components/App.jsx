import React from 'react';
import { Switch, Route } from 'react-router-dom';
import Sidebar from './Sidebar';
import Dashboard from './Dashboard';
import Console from './Console';
import Players from './Players';
import Modules from './Modules';
import Settings from './Settings';
import { useAuth } from '../contexts/AuthContext';

const App = () => {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Login />;
  }

  return (
    <div className="layout">
      <Sidebar />
      <main className="main-content">
        <Switch>
          <Route exact path="/" component={Dashboard} />
          <Route path="/console" component={Console} />
          <Route path="/players" component={Players} />
          <Route path="/modules" component={Modules} />
          <Route path="/settings" component={Settings} />
        </Switch>
      </main>
    </div>
  );
};

export default App;
