import { useEffect, useState } from 'react';
import { clearStoredToken, fetchAdminSession, getStoredToken } from '../api/adminApi.js';
import { AdminLogin } from './AdminLogin.jsx';
import { AdminMenuEditor } from './AdminMenuEditor.jsx';
import './admin.css';

export function AdminApp() {
  const [authState, setAuthState] = useState(() => (getStoredToken() ? 'checking' : 'anonymous'));

  useEffect(() => {
    const previousTitle = document.title;
    document.title = "Gestione menù · Il Bistrò dei Dotti";

    // The panel is unlisted; keep it out of search results too.
    const robots = document.createElement('meta');
    robots.name = 'robots';
    robots.content = 'noindex, nofollow';
    document.head.appendChild(robots);

    return () => {
      document.title = previousTitle;
      robots.remove();
    };
  }, []);

  // A token in sessionStorage survives a reload, so check it before showing the login form.
  useEffect(() => {
    if (authState !== 'checking') {
      return undefined;
    }

    let cancelled = false;
    fetchAdminSession()
      .then(() => {
        if (!cancelled) {
          setAuthState('authenticated');
        }
      })
      .catch(() => {
        if (!cancelled) {
          clearStoredToken();
          setAuthState('anonymous');
        }
      });

    return () => {
      cancelled = true;
    };
  }, [authState]);

  if (authState === 'checking') {
    return (
      <main className="admin-shell admin-shell--centered">
        <p className="admin-boot">Verifica della sessione…</p>
      </main>
    );
  }

  if (authState === 'anonymous') {
    return <AdminLogin onAuthenticated={() => setAuthState('authenticated')} />;
  }

  return <AdminMenuEditor onSignedOut={() => setAuthState('anonymous')} />;
}
