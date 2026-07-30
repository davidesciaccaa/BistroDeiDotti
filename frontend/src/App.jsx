import { useEffect, useState, useRef, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { fetchMenuSections } from './api/barApi.js';
import { Hero } from './components/Hero.jsx';
import { MenuItemCard } from './components/MenuItemCard.jsx';

const fallbackMenuSections = [
  {
    id: 'antipasti',
    title: 'Antipasti',
    description: 'Selezione di antipasti freschi di mare e di terra.',
    items: [
      { id: 'crudo_di_mare', name: 'Crudo di mare', subtitle: 'Su richiesta', description: '', notes: [], price: 25 },
      { id: 'antipasto_di_terra', name: 'Antipasto di terra', subtitle: 'Per 2 persone', description: '', notes: [], price: 19 },
      { id: 'mare_e_monti', name: 'Mare e monti', subtitle: '', description: '', notes: [], price: 20 },
      { id: 'veli_crudo_mojito', name: 'Veli di crudo al mojito', subtitle: '', description: '', notes: [], price: 16 }
    ]
  },
  {
    id: 'sfizi',
    title: 'Sfizi',
    description: 'Piccoli assaggi e stuzzichini da condividere.',
    items: [
      { id: 'salmone_affumicato', name: 'Salmone affumicato', subtitle: '', description: '', notes: [], price: 10 },
      { id: 'bresaola_manzo', name: 'Bresaola di manzo', subtitle: '', description: '', notes: [], price: 10 },
      { id: 'prosciutto_crudo', name: 'Prosciutto crudo', subtitle: '', description: '', notes: [], price: 15 },
      { id: 'crudo_iberico', name: 'Crudo iberico', subtitle: '', description: '', notes: [], price: null },
      { id: 'patatine_fritte', name: 'Patatine fritte', subtitle: '', description: '', notes: [], price: 5 },
      { id: 'verdure_grigliate', name: 'Verdure grigliate', subtitle: '', description: '', notes: [], price: 8 },
      { id: 'polpette', name: 'Polpette', subtitle: '', description: '', notes: [], price: 10 },
      { id: 'pettoline', name: 'Pettoline', subtitle: '', description: '', notes: [], price: 6 },
      { id: 'carpaccio_tonno_rosa', name: 'Carpaccio di Tonno Rosa', subtitle: '', description: '', notes: [], price: 15 }
    ]
  },
  {
    id: 'insalatone',
    title: 'Insalatone',
    description: 'Insalate fresche e nutrienti.',
    items: [
      { id: 'insalatona_verde', name: 'Insalatona mista verde', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'insalatona_proteica', name: 'Insalatona proteica', subtitle: '', description: 'Straccetti di pollo, pomodoro, grana e rucola', notes: [], price: 14 },
      { id: 'insalatona_gamberoni', name: 'Insalatona con Gamberoni', subtitle: '', description: '', notes: [], price: 15 },
      { id: 'insalatona_mare', name: 'Insalatona di Mare', subtitle: '', description: '', notes: [], price: 15 }
    ]
  },
  {
    id: 'primi-terra',
    title: 'Primi di terra',
    description: 'Paste fresche e gnocchi con i sapori della terra.',
    items: [
      { id: 'tagliatelle_porcini', name: 'Tagliatelle ai funghi porcini', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'orecchiette_pomodoro', name: 'Orecchiette al pomodoro fresco', subtitle: '', description: '', notes: [], price: 12 },
      { id: 'gnocchi_pomodoro', name: 'Gnocchi al pomodoro fresco', subtitle: '', description: '', notes: [], price: 12 },
      { id: 'tagliolini_salsiccia', name: 'Tagliolini con crema di porro e salsiccia norcina', subtitle: '', description: '', notes: [], price: 16 },
      { id: 'orecchiette_cime', name: 'Orecchiette con cime di rape', subtitle: '', description: 'Senatore Cappelli, acciughe del Cantabrico, stracciatella', notes: [], price: 15 }
    ]
  },
  {
    id: 'secondi-terra',
    title: 'Secondi di terra',
    description: 'Carni selezionate e preparazioni della tradizione.',
    items: [
      { id: 'filetto_tartufo', name: 'Filetto di manzo in crema di tartufo o porcini', subtitle: 'Con contorno', description: '', notes: [], price: 24 },
      { id: 'entrecote_scottona', name: 'Entrec\u00f4te di Scottona', subtitle: 'Con contorno, circa 250g', description: '', notes: [], price: 24 },
      { id: 'tagliata_pollo', name: 'Tagliata di pollo con grana, rucola e pomodoro', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'costine_messicana', name: 'Costine alla messicana', subtitle: '', description: '', notes: [], price: 22 },
      { id: 'tartare_manzo', name: 'Tartare di manzo', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'carpaccio_manzo', name: 'Carpaccio di Manzo', subtitle: '', description: '', notes: [], price: 14 }
    ]
  },
  {
    id: 'primi-mare',
    title: 'Primi di mare',
    description: 'Paste fresche con i frutti del mare.',
    items: [
      { id: 'tagliatelle_seppia', name: 'Tagliatelle con seppia', subtitle: '', description: '', notes: [], price: 19 },
      { id: 'tagliolini_gambero', name: 'Tagliolini aglio olio e tartare di gambero gobbetto', subtitle: '', description: '', notes: [], price: 16 },
      { id: 'tortelloni_astice', name: 'Tortelloni all\u2019astice e granchio reale con crema di crostacei', subtitle: '', description: '', notes: [], price: 21 },
      { id: 'spaghetti_ostriche', name: 'Spaghetti alle ostriche', subtitle: '', description: '', notes: [], price: 20 },
      { id: 'gnocchi_cozze', name: 'Gnocchi alle cozze', subtitle: '', description: '', notes: [], price: 15 },
      { id: 'panino_polpo', name: 'Panino con tentacolo di polpo', subtitle: '', description: '', notes: [], price: 12 },
      { id: 'tagliolini_astice', name: 'Tagliolini con 1/2 astice al pomodoro fresco', subtitle: '', description: '', notes: [], price: 28 },
      { id: 'pasta_vongole', name: 'Pasta e vongole', subtitle: '', description: '', notes: [], price: 18 }
    ]
  },
  {
    id: 'secondi-mare',
    title: 'Secondi di mare',
    description: 'Pesce e frutti di mare freschi, preparati con cura.',
    items: [
      { id: 'gamberoni_griglia', name: 'Gamberoni alla griglia', subtitle: 'Con contorno', description: '', notes: [], price: 17 },
      { id: 'filetto_pesce_spada', name: 'Filetto di pesce spada', subtitle: 'Con contorno', description: '', notes: [], price: 16 },
      { id: 'frittura_mista', name: 'Frittura mista', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'tartare_tonno', name: 'Tartare di tonno', subtitle: 'Con contorno', description: '', notes: [], price: 18 },
      { id: 'polpo_burratina', name: 'Tentacolo di polpo su burratina affumicata e crema di piselli verdi', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'scottata_tonno', name: 'Scottata di Tonno', subtitle: '', description: '', notes: [], price: 21 }
    ]
  },
  {
    id: 'dolci',
    title: 'Dolci',
    description: 'Dessert artigianali per concludere in dolcezza.',
    items: [
      { id: 'cremoso_caffe', name: 'Cremoso al caff\u00e8', subtitle: '', description: '', notes: [], price: 6 },
      { id: 'cremoso_frutti_bosco', name: 'Cremoso ai frutti di bosco', subtitle: '', description: '', notes: [], price: 6 },
      { id: 'cremoso_pistacchio', name: 'Cremoso al pistacchio', subtitle: '', description: '', notes: [], price: 6 }
    ]
  },
  {
    id: 'bevande',
    title: 'Bevande',
    description: 'Analcolici e soft drink.',
    items: [
      { id: 'acqua', name: 'Acqua naturale / frizzante', subtitle: '50 cl', description: '', notes: [], price: 2.5 },
      { id: 'calice_vino', name: 'Calice di vino', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'calice_prosecco', name: 'Calice di prosecco', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'calice_franciacorta', name: 'Calice franciacorta', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'calice_champagne', name: 'Calice champagne', subtitle: '', description: '', notes: [], price: 10 },
      { id: 'coca_cola', name: 'Coca Cola / Coca Cola Zero / Fanta', subtitle: '', description: '', notes: [], price: 3 },
      { id: 'succhi_frutta', name: 'Succhi di frutta', subtitle: '', description: '', notes: [], price: 3.5 },
      { id: 'chinotto', name: 'Chinotto', subtitle: '', description: '', notes: [], price: 3 },
      { id: 'schweppes_lemon', name: 'Schweppes lemon', subtitle: '', description: '', notes: [], price: 3 },
      { id: 'the_pesca_limone', name: 'The alla pesca / limone', subtitle: '', description: '', notes: [], price: 3 },
      { id: 'san_bitter', name: 'San Bitter bianco / rosso', subtitle: '', description: '', notes: [], price: 3 },
      { id: 'cocktail_san_pellegrino', name: 'Cocktail San Pellegrino', subtitle: '', description: '', notes: [], price: 3 },
      { id: 'crodino', name: 'Crodino', subtitle: '', description: '', notes: [], price: 3 },
      { id: 'acqua_tonica', name: 'Acqua tonica', subtitle: '', description: '', notes: [], price: 3 },
      { id: 'red_bull', name: 'Red Bull', subtitle: '', description: '', notes: [], price: 4 }
    ]
  },
  {
    id: 'gin',
    title: 'Gin',
    description: 'Selezione di gin premium da tutto il mondo.',
    items: [
      { id: 'tanqueray_london', name: 'Tanqueray London Dry', subtitle: 'vol 43', description: '', notes: [], price: 6 },
      { id: 'tanqueray_sevilla', name: 'Tanqueray Sevilla', subtitle: 'vol 41,3', description: '', notes: [], price: 7 },
      { id: 'tanqueray_nten', name: 'Tanqueray N\u00b0Ten', subtitle: 'vol 47,3', description: '', notes: [], price: 8 },
      { id: 'ki_no_tou', name: 'Ki No Tou Kioto', subtitle: 'vol 47,4', description: '', notes: [], price: 12 },
      { id: 'bagur', name: 'Bagur', subtitle: 'vol 43', description: '', notes: [], price: 8 },
      { id: 'amuerte_coca', name: 'Amuerte Coca Gin', subtitle: 'vol 43', description: '', notes: [], price: 12 },
      { id: 'etsu_gin', name: 'Etsu Gin', subtitle: 'vol 43,3', description: '', notes: [], price: 8 },
      { id: 'roku_gin', name: 'Roku Gin', subtitle: 'vol 43', description: '', notes: [], price: 6 },
      { id: 'cubical_gin', name: 'Cubical Gin', subtitle: 'vol 40', description: '', notes: [], price: 7 },
      { id: 'bobbys', name: "Bobby's", subtitle: 'vol 42', description: '', notes: [], price: 8 },
      { id: 'nordes_gin', name: 'Nord\u00e9s Gin', subtitle: 'vol 40', description: '', notes: [], price: 8 },
      { id: 'black_tomato', name: 'Black Tomato', subtitle: 'vol 42', description: '', notes: [], price: 8 },
      { id: 'hendrix', name: "Hendrick's", subtitle: 'vol 44', description: '', notes: [], price: 7 },
      { id: 'hendrix_neptunia', name: "Hendrick's Neptunia", subtitle: 'vol 43,4', description: '', notes: [], price: 8 },
      { id: 'tassoni_gin', name: 'Tassoni Gin', subtitle: 'vol 41,5', description: '', notes: [], price: 7 },
      { id: 'gin_mare', name: 'Gin Mare', subtitle: 'vol 42,7', description: '', notes: [], price: 8 },
      { id: 'malfi_rosa', name: 'Malfi Rosa', subtitle: 'vol 41', description: '', notes: [], price: 7 },
      { id: 'malfi', name: 'Malfi', subtitle: 'vol 41', description: '', notes: [], price: 7 },
      { id: 'irish_gin', name: 'Irish Gin', subtitle: 'vol 43', description: '', notes: [], price: 8 },
      { id: 'professore', name: 'Professore', subtitle: 'vol 45', description: '', notes: [], price: 8 },
      { id: 'cittadinelle', name: 'Cittadinelle', subtitle: 'vol 44', description: '', notes: [], price: 7 },
      { id: 'ambrosia_dry', name: 'Ambrosia Dry', subtitle: 'vol 40', description: '', notes: [], price: 7 },
      { id: 'ambrosia_sicily', name: 'Ambrosia Sicily Edition', subtitle: 'vol 40', description: '', notes: [], price: 7 },
      { id: 'bulldog_gin', name: 'Bulldog Gin', subtitle: 'vol 40', description: '', notes: [], price: 7 },
      { id: 'adamvs', name: 'Adamvs', subtitle: 'vol 44', description: '', notes: [], price: 8 },
      { id: 'dolce_vita_gin', name: 'Dolce Vita', subtitle: 'vol 40', description: '', notes: [], price: 8 },
      { id: 'bombay', name: 'Bombay', subtitle: 'vol 40', description: '', notes: [], price: 7 }
    ]
  },
  {
    id: 'cocktails',
    title: 'Cocktails',
    description: 'I classici e le nostre proposte della casa.',
    items: [
      { id: 'aperol_spritz', name: 'Aperol Spritz', subtitle: '', description: '', notes: [], price: 5 },
      { id: 'campari_spritz', name: 'Campari Spritz', subtitle: '', description: '', notes: [], price: 6 },
      { id: 'campari_prosecco', name: 'Campari e Prosecco', subtitle: '', description: '', notes: [], price: 6 },
      { id: 'caipirinha', name: 'Caipirinha', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'caipiroska', name: 'Caipiroska', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'gin_tonic', name: 'Gin Tonic', subtitle: '', description: '', notes: [], price: 6 },
      { id: 'hugo', name: 'Hugo', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'moscow_mule', name: 'Moscow Mule', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'london_mule', name: 'London Mule', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'long_island', name: 'Long Island Ice Tea', subtitle: '', description: '', notes: [], price: 8 },
      { id: 'japan_ice_tea', name: 'Japan Ice Tea', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'mojito_scuro', name: 'Mojito Scuro', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'negroni', name: 'Negroni', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'negroni_sbagliato', name: 'Negroni Sbagliato', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'sex_on_the_beach', name: 'Sex on the Beach', subtitle: '', description: '', notes: [], price: 8 },
      { id: 'margherita', name: 'Margherita', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'cocktail_martini', name: 'Cocktail Martini', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'espresso_martini', name: 'Espresso Martini', subtitle: '', description: '', notes: [], price: 7 },
      { id: 'cosmopolitan', name: 'Cosmopolitan', subtitle: '', description: '', notes: [], price: 8 },
      { id: 'quattro_bianchi', name: 'Quattro Bianchi', subtitle: 'Alla fragola o al limone', description: '', notes: [], price: 10 }
    ]
  },
  {
    id: 'amari',
    title: 'Amari',
    description: 'Selezione di amari italiani per concludere la serata.',
    items: [
      { id: 'ramazzotti', name: 'Ramazzotti', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'cynar', name: 'Cynar', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'montenegro', name: 'Montenegro', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'fernet_branca', name: 'Fernet Branca / Menta', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'averna', name: 'Averna', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'petrus', name: 'Petrus', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'jagermeister', name: 'Jagermeister', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'jefferson', name: 'Jefferson', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'unicum', name: 'Unicum', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'lucano', name: 'Lucano', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'amaro_del_capo', name: 'Amaro del Capo', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'sambuca', name: 'Sambuca', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'caffe_borghetti', name: 'Caff\u00e8 Borghetti', subtitle: '', description: '', notes: [], price: 4 },
      { id: 'vena_caffe', name: 'Vena Caff\u00e8', subtitle: '', description: '', notes: [], price: 4 }
    ]
  },
  {
    id: 'distillati',
    title: 'Distillati',
    description: 'Whisky, Rum e altri pregiati distillati.',
    items: [
      { id: 'vecchia_romagna', name: 'Vecchia Romagna', subtitle: '', description: '', notes: [], price: 5 },
      { id: 'cointreau', name: 'Cointreau', subtitle: '', description: '', notes: [], price: 5 },
      { id: 'jack_daniels', name: "Jack Daniel's", subtitle: '', description: '', notes: [], price: 6 },
      { id: 'jack_daniels_honey', name: "Jack Daniel's Honey", subtitle: '', description: '', notes: [], price: 6 },
      { id: 'oban', name: 'Oban', subtitle: '', description: '', notes: [], price: 9 },
      { id: 'laphroaig', name: 'Laphroaig', subtitle: '', description: '', notes: [], price: 12 },
      { id: 'lagavulin', name: 'Lagavulin', subtitle: '', description: '', notes: [], price: 12 },
      { id: 'sambuca_dist', name: 'Sambuca', subtitle: '', description: '', notes: [], price: 5 },
      { id: 'martini', name: 'Martini Bianco / Rosso / Dry', subtitle: '', description: '', notes: [], price: 5 },
      { id: 'bacardi', name: 'Bacardi', subtitle: 'Rum', description: '', notes: [], price: 5 },
      { id: 'don_papa', name: 'Don Papa', subtitle: 'Rum', description: '', notes: [], price: 8 },
      { id: 'zacapa_23', name: 'Zacapa 23', subtitle: 'Rum', description: '', notes: [], price: 12 },
      { id: 'shot_2cl', name: 'Shot con distillati base', subtitle: '2 cl', description: '', notes: [], price: 3 }
    ]
  },
  {
    id: 'vini-bianchi',
    title: 'Vini bianchi',
    description: 'Selezione di vini bianchi pugliesi e nazionali.',
    items: [
      { id: 'calavento_igp', name: 'Calavento IGP Salento', subtitle: '', description: '', notes: [], price: 21 },
      { id: 'luna_igp', name: 'Luna IGP Salento', subtitle: '', description: '', notes: [], price: 21 },
      { id: 'leverano_bianco', name: 'Leverano Vecchia Torre', subtitle: '', description: '', notes: [], price: 16 },
      { id: 'muller_thurgau', name: 'Muller Thurgau', subtitle: '', description: '', notes: [], price: 21 },
      { id: 'gewurztraminer', name: 'Gewurztraminer', subtitle: '', description: '', notes: [], price: 21 },
      { id: 'falanghina', name: 'Falanghina', subtitle: '', description: '', notes: [], price: 16 },
      { id: 'trebbiano_abbruzzo', name: "Trebbiano d'Abruzzo", subtitle: '', description: '', notes: [], price: 16 },
      { id: 'fiano', name: 'Fiano', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'verdeca_due_trulli', name: 'Verdeca Due Trulli', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'chardonnay', name: 'Chardonnay', subtitle: '', description: '', notes: [], price: 18 }
    ]
  },
  {
    id: 'vini-rosati',
    title: 'Vini rosati',
    description: 'Rosati freschi e fruttati della tradizione pugliese.',
    items: [
      { id: 'leverano_rosato', name: 'Leverano DOP Vecchia Torre', subtitle: '', description: '', notes: [], price: 16 },
      { id: 'negroamaro_rosato', name: 'Negroamaro Vecchia Torre', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'primitivo_rosato_1932', name: 'Primitivo Rosato 1932', subtitle: '', description: '', notes: [], price: 19 },
      { id: 'no_negroamaro', name: 'N-O Negroamaro Susumaniello', subtitle: '', description: '', notes: [], price: 21 },
      { id: 'susumaniello_rosato', name: 'Susumaniello Due Trulli', subtitle: '', description: '', notes: [], price: 21 }
    ]
  },
  {
    id: 'vini-rossi',
    title: 'Vini rossi',
    description: 'Rossi corposi e strutturati, pugliesi e nazionali.',
    items: [
      { id: 'primitivo_vt', name: 'Primitivo Vecchia Torre', subtitle: '', description: '', notes: [], price: 16 },
      { id: 'primitivo_dt', name: 'Primitivo Due Trulli', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'primitivo_vigniaioli', name: 'Primitivo Vigniaioli 68 IGP', subtitle: '', description: '', notes: [], price: 28 },
      { id: 'primitivo_manduria', name: 'Primitivo di Manduria 1932', subtitle: '', description: '', notes: [], price: 21 },
      { id: 'negroamaro_vt', name: 'Negroamaro Vecchia Torre', subtitle: '', description: '', notes: [], price: 16 },
      { id: 'negroamaro_dt', name: 'Negroamaro Due Trulli', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'negroamaro_manorossa', name: 'Negroamaro Manorossa', subtitle: '', description: '', notes: [], price: 60 },
      { id: 'negroamaro_susumaniello', name: 'Negroamaro-Susumaniello', subtitle: '', description: '', notes: [], price: 34 },
      { id: 'susumaniello_vigna14', name: 'Susumaniello Vigna 14 IGP', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'nerotavola_sicilia', name: 'Nerotavola Sicilia DOC', subtitle: '', description: '', notes: [], price: 28 },
      { id: 'nero_di_troia', name: 'Nero di Troia', subtitle: '', description: '', notes: [], price: 16 },
      { id: 'aglianico', name: 'Aglianico', subtitle: '', description: '', notes: [], price: 18 },
      { id: 'cabernet_veneto', name: 'Cabernet Veneto', subtitle: '', description: '', notes: [], price: 21 },
      { id: 'ripasso_negrar', name: 'Ripasso Negrar', subtitle: '', description: '', notes: [], price: 26 },
      { id: 'chianti_classico', name: 'Chianti Classico', subtitle: '', description: '', notes: [], price: 20 },
      { id: 'brunello', name: 'Brunello', subtitle: '', description: '', notes: [], price: 40 },
      { id: 'amarone', name: 'Amarone', subtitle: '', description: '', notes: [], price: 40 }
    ]
  },
  {
    id: 'vini-bio',
    title: 'Vini bio',
    description: 'Selezione di vini biologici bianchi, rosati e rossi.',
    items: [
      { id: 'trebbiano_bio', name: "Trebbiano d'Abruzzo bio vegano", subtitle: 'Bianco', description: '', notes: [], price: 19 },
      { id: 'passerina_bio', name: 'Passerina bio vegano', subtitle: 'Bianco', description: '', notes: [], price: 19 },
      { id: 'pecorino_bio', name: 'Pecorino bio', subtitle: 'Bianco', description: '', notes: [], price: 19 },
      { id: 'castel_monte_bio_bianco', name: 'Castel del Monte bio', subtitle: 'Bianco', description: '', notes: [], price: 19 },
      { id: 'vitalba_bio', name: 'Vitalba bio', subtitle: 'Bianco', description: '', notes: [], price: 19 },
      { id: 'dharma_bio', name: 'Dharma bio', subtitle: 'Bianco', description: '', notes: [], price: 19 },
      { id: 'novebolle_doc', name: 'Novebolle D.O.C.', subtitle: 'Spumante', description: '', notes: [], price: 19 },
      { id: 'castel_monte_bio_rosato', name: 'Castel del Monte bio', subtitle: 'Rosato', description: '', notes: [], price: 19 },
      { id: 'castel_monte_bio_rosso', name: 'Castel del Monte bio', subtitle: 'Rosso', description: '', notes: [], price: 19 }
    ]
  },
  {
    id: 'prosecco',
    title: 'Prosecco e Champagne',
    description: 'Bollicine per ogni occasione.',
    items: [
      { id: 'monticano', name: 'Monticano', subtitle: 'Prosecco', description: '', notes: [], price: 21 },
      { id: 'fragolino', name: 'Fragolino', subtitle: 'Prosecco', description: '', notes: [], price: 18 },
      { id: 'asti', name: 'Asti', subtitle: 'Prosecco', description: '', notes: [], price: 15 },
      { id: 'franciacorta', name: 'Franciacorta', subtitle: 'Spumante', description: '', notes: [], price: 48 },
      { id: 'moet_chandon', name: 'Mo\u00ebt & Chandon', subtitle: 'Champagne', description: '', notes: [], price: 70 },
      { id: 'veuve_clicquot', name: 'Veuve Clicquot', subtitle: 'Champagne', description: '', notes: [], price: 65 }
    ]
  }
];

function App() {
  const { t } = useTranslation();
  const [menuSections, setMenuSections] = useState(fallbackMenuSections);
  const [isUsingFallback, setIsUsingFallback] = useState(false);

  // Scroll Indicators Logic
  const navRef = useRef(null);
  const [showLeftIndicator, setShowLeftIndicator] = useState(false);
  const [showRightIndicator, setShowRightIndicator] = useState(false);

  const checkScroll = useCallback(() => {
    if (navRef.current) {
      const { scrollLeft, scrollWidth, clientWidth } = navRef.current;
      setShowLeftIndicator(scrollLeft > 10);
      setShowRightIndicator(scrollLeft < scrollWidth - clientWidth - 10);
    }
  }, []);

  const scrollCategoryNav = (direction) => {
    if (navRef.current) {
      navRef.current.scrollBy({
        left: direction === 'left' ? -120 : 120,
        behavior: 'smooth',
      });
    }
  };

  useEffect(() => {
    const nav = navRef.current;
    if (nav) {
      checkScroll();
      nav.addEventListener('scroll', checkScroll);
      window.addEventListener('resize', checkScroll);
      
      // Also check when content might have rendered
      const timeout = setTimeout(checkScroll, 500);

      return () => {
        nav.removeEventListener('scroll', checkScroll);
        window.removeEventListener('resize', checkScroll);
        clearTimeout(timeout);
      };
    }
  }, [checkScroll, menuSections]);

  useEffect(() => {
    let isMounted = true;

    async function loadHomeData() {
      try {
        const sections = await fetchMenuSections();

        if (!isMounted) {
          return;
        }

        setMenuSections(sections);
        setIsUsingFallback(false);
      } catch {
        if (!isMounted) {
          return;
        }

        setMenuSections(fallbackMenuSections);
        setIsUsingFallback(true);
      }
    }

    loadHomeData();

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <main>
      <Hero />

      <section className="section section--menu" id="menu" aria-labelledby="menu-title">
        <div className="section__heading">
          <p className="section__eyebrow">{t('nav.menu', { defaultValue: 'Menu' })}</p>
          <h2 id="menu-title">{t('menu.title')}</h2>
          {isUsingFallback && (
            <p className="section__note">{t('menu.fallback_note')}</p>
          )}
        </div>

        {/* Minimal Category Navigation */}
        <div className={`category-nav-wrapper ${showLeftIndicator ? 'has-left-scroll' : ''} ${showRightIndicator ? 'has-right-scroll' : ''}`}>
          {showLeftIndicator && (
            <button
              className="scroll-hint scroll-hint--left"
              onClick={() => scrollCategoryNav('left')}
              aria-label={t('menu.aria.scroll_left')}
            >
              ‹
            </button>
          )}
          <nav className="category-nav" ref={navRef}>
            {menuSections.map((section) => (
              <a 
                key={`nav-${section.id}`} 
                href={`#${section.id}`} 
                className="category-link"
              >
                {t(`menu.sections.${section.id}.title`, { defaultValue: section.title })}
              </a>
            ))}
          </nav>
          {showRightIndicator && (
            <button
              className="scroll-hint scroll-hint--right"
              onClick={() => scrollCategoryNav('right')}
              aria-label={t('menu.aria.scroll_right')}
            >
              ›
            </button>
          )}
        </div>

        <div className="menu-section-list">
          {menuSections.map((section) => (
            <section className="menu-section" id={section.id} key={section.id}>
              <div className="menu-section__header-editorial">
                <p className="section__eyebrow">{t(`menu.sections.${section.id}.title`, { defaultValue: section.title })}</p>
                <h3>{t(`menu.sections.${section.id}.title`, { defaultValue: section.title })}</h3>
                {section.description && (
                  <p className="menu-item-editorial__description" style={{ marginTop: '4px' }}>
                    {t(`menu.sections.${section.id}.description`, { defaultValue: section.description })}
                  </p>
                )}
              </div>

              <div className="menu-list-editorial">
                {section.items.map((item) => (
                  <MenuItemCard key={`${section.id}-${item.name}`} item={item} />
                ))}
              </div>
            </section>
          ))}
        </div>
      </section>

      <div className="experience-wrapper">
        <section className="section section--experience" id="esperienza">
          <div className="experience-panel">
            <p className="section__eyebrow">{t('experience.eyebrow')}</p>
            <h1>{t('experience.title')}</h1>

            <p>
              {t('experience.text')}
            </p>
          </div>
          <div className="hours-panel" id="contatti">
            <p className="section__eyebrow">{t('contacts.eyebrow')}</p>
            <h1>{t('contacts.title')}</h1>
            <p>{t('contacts.hours')}</p>
            <div className="hours-panel__links">
              <a href={`tel:${t('contacts.phone')}`}>{t('contacts.phone')}</a>
              <a href="mailto:ilbistrodeidotti19@gmail.com">ilbistrodeidotti19@gmail.com</a>
            </div>
          </div>
        </section>

        <footer className="site-footer">
          <a 
            href="https://www.linkedin.com/in/davide-sciacca-a6627728a/" 
            target="_blank" 
            rel="noopener noreferrer"
            className="footer-credit"
          >
            {t('footer.credit')}
          </a>
        </footer>
      </div>
    </main>
  );
}

export default App;
