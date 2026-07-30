import { useTranslation } from 'react-i18next';

const languages = [
  { code: 'it', label: 'ITA', name: 'Italiano', flag: '🇮🇹' },
  { code: 'en', label: 'ENG', name: 'English', flag: '🇬🇧' },
  { code: 'de', label: 'DEU', name: 'Deutsch', flag: '🇩🇪' }
];

export function LanguageSwitcher() {
  const { t, i18n } = useTranslation();

  return (
    <div className="language-switcher">
      {languages.map((lang) => (
        <button
          key={lang.code}
          onClick={() => i18n.changeLanguage(lang.code)}
          className={`lang-button ${i18n.language === lang.code ? 'active' : ''}`}
          aria-label={t('language.switch_to', { language: lang.name })}
        >
          <span className="lang-flag">{lang.flag}</span>
          <span className="lang-label">{lang.label}</span>
        </button>
      ))}
    </div>
  );
}
