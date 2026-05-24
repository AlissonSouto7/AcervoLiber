import { useMemo, useState } from 'react';
import {
  AuditOutlined,
  BookOutlined,
  CarryOutOutlined,
  DashboardOutlined,
  HistoryOutlined,
  IdcardOutlined,
  LogoutOutlined,
  MenuOutlined,
  ReadOutlined,
  SettingOutlined,
  SwapOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Button, Drawer, Dropdown, Grid, Layout, Menu, Typography, type MenuProps } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { queryClient } from '../App';
import { logout } from '../api/auth';
import { useAuthStore } from '../auth/authStore';
import type { Role } from '../types/api';

const { Header, Sider, Content } = Layout;

// Itens da equipe (ADMIN e BIBLIOTECARIO)
const ITENS_STAFF = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
  { key: '/livros', icon: <BookOutlined />, label: 'Livros' },
  { key: '/alunos', icon: <TeamOutlined />, label: 'Alunos' },
  { key: '/emprestimos', icon: <SwapOutlined />, label: 'Empréstimos' },
  { key: '/reservas', icon: <CarryOutOutlined />, label: 'Reservas' },
  { key: '/historico', icon: <HistoryOutlined />, label: 'Histórico' },
];

// Itens visiveis apenas para ADMIN
const ITENS_ADMIN = [
  { key: '/usuarios', icon: <IdcardOutlined />, label: 'Usuários' },
  { key: '/auditoria', icon: <AuditOutlined />, label: 'Auditoria' },
];

// Itens do aluno
const ITENS_ALUNO = [
  { key: '/catalogo', icon: <ReadOutlined />, label: 'Catálogo' },
  { key: '/minhas-reservas', icon: <CarryOutOutlined />, label: 'Minhas reservas' },
];

/** Itens de navegacao conforme o perfil. */
function itensPorPerfil(role: Role | undefined) {
  if (role === 'ALUNO') return ITENS_ALUNO;
  if (role === 'ADMIN') return [...ITENS_STAFF, ...ITENS_ADMIN];
  return ITENS_STAFF;
}

const ROTAS_NAVEGAVEIS = [...ITENS_STAFF, ...ITENS_ADMIN, ...ITENS_ALUNO].map((i) => i.key);

/**
 * Layout principal — sidebar fixa no desktop, gaveta no mobile.
 */
export function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.lg;
  const [drawerAberto, setDrawerAberto] = useState(false);

  const usuario = useAuthStore((s) => s.usuario);
  const refreshToken = useAuthStore((s) => s.refreshToken);
  const limparSessao = useAuthStore((s) => s.limparSessao);

  const selecionado = useMemo(() => {
    const rota = ROTAS_NAVEGAVEIS.find((r) => location.pathname.startsWith(r));
    return rota ? [rota] : [];
  }, [location.pathname]);

  async function sair() {
    try {
      if (refreshToken) {
        await logout(refreshToken);
      }
    } catch {
      // Encerra a sessao local mesmo se o servidor falhar.
    }
    limparSessao();
    // Limpa o cache de queries — senao um proximo usuario logando no mesmo
    // navegador veria dados em cache do usuario anterior (vazamento de PII).
    queryClient.clear();
    navigate('/login');
  }

  // Itens de navegacao conforme o perfil
  const itensNavegacao = useMemo(() => itensPorPerfil(usuario?.role), [usuario?.role]);

  function aoClicarMenu(key: string) {
    if (key === 'sair') {
      sair();
      return;
    }
    navigate(key);
    setDrawerAberto(false);
  }

  const logo = (
    <div style={{ color: '#fff', fontWeight: 600, fontSize: 18, padding: 16 }}>
      AcervoLiber
    </div>
  );

  // Conteudo da barra lateral: logo + navegacao no topo; "Sair" fixo embaixo
  // (marginTop:auto empurra o menu de sair para o rodape da coluna flex).
  const conteudoMenu = (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
      {logo}
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={selecionado}
        items={itensNavegacao}
        onClick={({ key }) => aoClicarMenu(key)}
      />
      <Menu
        theme="dark"
        mode="inline"
        selectable={false}
        style={{ marginTop: 'auto' }}
        items={[{ key: 'sair', icon: <LogoutOutlined />, label: 'Sair', danger: true }]}
        onClick={() => sair()}
      />
    </div>
  );

  const menuUsuario: MenuProps['items'] = [
    {
      key: 'configuracoes',
      icon: <SettingOutlined />,
      label: 'Configurações',
      onClick: () => navigate('/configuracoes'),
    },
    { type: 'divider' },
    {
      key: 'sair',
      icon: <LogoutOutlined />,
      label: 'Sair',
      danger: true,
      onClick: sair,
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }} hasSider>
      {!isMobile && (
        <Sider
          width={220}
          style={{ height: '100vh', position: 'sticky', top: 0, overflowY: 'auto' }}
        >
          {conteudoMenu}
        </Sider>
      )}

      <Layout>
        <Header
          style={{
            background: '#fff',
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            padding: '0 16px',
            position: 'sticky',
            top: 0,
            zIndex: 10,
            boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
          }}
        >
          {isMobile && (
            <Button
              type="text"
              icon={<MenuOutlined />}
              onClick={() => setDrawerAberto(true)}
              aria-label="Abrir menu"
            />
          )}
          {isMobile && <Typography.Text strong>AcervoLiber</Typography.Text>}
          <div style={{ flex: 1 }} />
          <Dropdown menu={{ items: menuUsuario }} trigger={['click']}>
            <Button type="text" icon={<UserOutlined />}>
              {!isMobile && (usuario?.nome ?? '')}
            </Button>
          </Dropdown>
        </Header>

        <Content style={{ margin: isMobile ? 12 : 24 }}>
          <Outlet />
        </Content>
      </Layout>

      {/* Menu em gaveta — usado apenas no mobile */}
      <Drawer
        placement="left"
        open={drawerAberto}
        onClose={() => setDrawerAberto(false)}
        width={240}
        closable={false}
        styles={{ body: { padding: 0, background: '#001529', display: 'flex' } }}
      >
        {conteudoMenu}
      </Drawer>
    </Layout>
  );
}
