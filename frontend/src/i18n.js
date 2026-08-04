import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import itTranslations from './locales/it.json';
import enTranslations from './locales/en.json';
import deTranslations from './locales/de.json';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      it: { translation: itTranslations },
      en: { translation: enTranslations },
      de: { translation: deTranslations }
    },
    supportedLngs: ['it', 'en', 'de'],
    load: 'languageOnly',
    fallbackLng: 'it',
    interpolation: {
      escapeValue: false
    }
  });

export default i18n;
