import { useTranslation } from 'react-i18next';
import { useRef, useState, useEffect, useCallback } from 'react';
import heroImage from '../assets/lounge-still-life.jpeg';
import { LanguageSwitcher } from './LanguageSwitcher.jsx';

export function Hero() {
  const { t } = useTranslation();

  // Scroll Indicators Logic for Hero Nav
  const navRef = useRef(null);
  const [showLeftIndicator, setShowLeftIndicator] = useState(false);
  const [showRightIndicator, setShowRightIndicator] = useState(false);

  const scrollNav = (direction) => {
    if (navRef.current) {
      navRef.current.scrollBy({
        left: direction === 'left' ? -100 : 100,
        behavior: 'smooth',
      });
    }
  };

  const checkScroll = useCallback(() => {
    if (navRef.current) {
      const { scrollLeft, scrollWidth, clientWidth } = navRef.current;

      console.log({
        scrollLeft,
        scrollWidth,
        clientWidth,
        overflow: scrollWidth > clientWidth
      });

      setShowLeftIndicator(scrollLeft > 10);
      setShowRightIndicator(scrollWidth > clientWidth && scrollLeft < scrollWidth - clientWidth - 10);
    }
  }, []);

  useEffect(() => {
    const nav = navRef.current;
    if (nav) {
      checkScroll();
      nav.addEventListener('scroll', checkScroll);
      window.addEventListener('resize', checkScroll);

      const timeout = setTimeout(checkScroll, 500);

      return () => {
        nav.removeEventListener('scroll', checkScroll);
        window.removeEventListener('resize', checkScroll);
        clearTimeout(timeout);
      };
    }
  }, [checkScroll]);

  return (
    <section className="hero" style={{ '--hero-image': `url(${heroImage})` }}>
      <header className="site-header" aria-label="Intestazione principale">
        <a className="brand" href="#top" aria-label="Il Bistrò dei Dotti">
          Il Bistr&ograve; dei <span className="brand-v">Dotti</span>
        </a>
        <div className="header-actions">
          <div className={`site-nav-wrapper ${showLeftIndicator ? 'has-left-scroll' : ''} ${showRightIndicator ? 'has-right-scroll' : ''}`}>
            {showLeftIndicator && (
              <button
              className="nav-scroll-hint nav-scroll-hint--left"
              onClick={() => scrollNav('left')}
              aria-label="Scorri menu a sinistra"
            >
              <span className="nav-arrow">‹</span>
            </button>)}

            <nav className="site-nav" aria-label="Navigazione principale" ref={navRef}>
              <a href="#antipasti">{t('nav.antipasti')}</a>
              <a href="#sfizi">{t('nav.sfizi')}</a>
              <a href="#insalatone">{t('nav.insalatone')}</a>
              <a href="#primi-terra">{t('nav.primi_terra')}</a>
              <a href="#secondi-terra">{t('nav.secondi_terra')}</a>
              <a href="#primi-mare">{t('nav.primi_mare')}</a>
              <a href="#secondi-mare">{t('nav.secondi_mare')}</a>
              <a href="#dolci">{t('nav.dolci')}</a>
              <a href="#bevande">{t('nav.bevande')}</a>
              <a href="#gin">{t('nav.gin')}</a>
              <a href="#cocktails">{t('nav.cocktails')}</a>
              <a href="#amari">{t('nav.amari')}</a>
              <a href="#distillati">{t('nav.distillati')}</a>
              <a href="#vini-bianchi">{t('nav.vini')}</a>
              <a href="#contatti">{t('nav.contatti')}</a>
            </nav>
              {showRightIndicator && (
                <button
                  className="nav-scroll-hint nav-scroll-hint--right"
                  onClick={() => scrollNav('right')}
                  aria-label="Scorri menu a destra"
                >
                  <span className="nav-arrow">›</span>
                </button>
              )}
          </div>
          <LanguageSwitcher />
        </div>
      </header>

      <div className="hero__content" id="top">
        <p className="hero__eyebrow">{t('hero.eyebrow')}</p>
        <h1 className="hero__title">Il Bistr&ograve; dei <span className="brand-v">Dotti</span></h1>
        <p className="hero__copy">
          {t('hero.copy')}
        </p>
        <div className="hero__actions" aria-label="Azioni principali">
          <a className="button button--primary" href="#antipasti">
            {t('hero.button')}
          </a>
        </div>
      </div>

      <a href="#menu" className="hero__scroll-down" aria-label={t('hero.scroll_down', { defaultValue: 'Scorri per scoprire il menu' })}>
        <div className="mouse-icon">
          <div className="wheel"></div>
        </div>
        <div className="scroll-arrow"></div>
      </a>
    </section>
  );
}
