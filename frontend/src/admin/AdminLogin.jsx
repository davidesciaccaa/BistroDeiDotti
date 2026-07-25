import { useState } from 'react';
import { adminLogin } from '../api/adminApi.js';

export function AdminLogin({ onAuthenticated }) {
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    if (isSubmitting || !password) {
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      await adminLogin(password);
      setPassword('');
      onAuthenticated();
    } catch (loginError) {
      setError(loginError.message);
      setIsSubmitting(false);
    }
  }

  return (
    <main className="admin-shell admin-shell--centered">
      <form className="admin-login" onSubmit={handleSubmit}>
        <p className="admin-eyebrow">Il Bistr&ograve; dei Dotti</p>
        <h1 className="admin-login__title">Gestione prezzi</h1>
        <p className="admin-login__hint">Area riservata al proprietario.</p>

        <label className="admin-field">
          <span className="admin-field__label">Password</span>
          <input
            className="admin-field__input"
            type="password"
            name="admin-password"
            autoComplete="current-password"
            autoFocus
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            disabled={isSubmitting}
          />
        </label>

        {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}

        <button className="admin-button admin-button--primary" type="submit" disabled={isSubmitting || !password}>
          {isSubmitting ? 'Accesso…' : 'Accedi'}
        </button>
      </form>
    </main>
  );
}
