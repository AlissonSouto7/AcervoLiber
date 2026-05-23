import React from 'react';
import ReactDOM from 'react-dom/client';
// Patch de compatibilidade do Ant Design 5 com React 19 (silencia o aviso e
// garante o funcionamento de Modal/message/notification).
import '@ant-design/v5-patch-for-react-19';
import 'antd/dist/reset.css';
import App from './App';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
