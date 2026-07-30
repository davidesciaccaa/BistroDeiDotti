package com.angolodivino.menu;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import com.angolodivino.admin.MenuItemRequest;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    /** Prices are free text ("5 € / 22 €", "2,50 €", "-", ""), but never markup. */
    private static final Pattern PRICE_PATTERN = Pattern.compile("^$|^[0-9 ,./€-]{1,32}$");

    private final MenuOverridesStore overridesStore;

    public MenuService(MenuOverridesStore overridesStore) {
        this.overridesStore = overridesStore;
    }

    /**
     * The public menu: hardcoded content with the prices from the overrides file applied on top.
     * The file is re-read here so price edits take effect without a restart.
     */
    public List<MenuSectionResponse> findMenuSections() {
        List<MenuSectionResponse> saved = overridesStore.readSections();
        return saved.isEmpty() ? applyPriceOverrides(defaultMenuSections(), overridesStore.readPrices()) : saved;
    }

    public List<MenuSectionResponse> createItem(MenuItemRequest request) {
        List<MenuSectionResponse> sections = new java.util.ArrayList<>(findMenuSections());
        String id = slug(request.name()) + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        boolean found = false;
        for (int i = 0; i < sections.size(); i++) {
            MenuSectionResponse section = sections.get(i);
            if (!section.id().equals(request.sectionId())) continue;
            List<MenuItemResponse> items = new java.util.ArrayList<>(section.items());
            items.add(toItem(id, request));
            sections.set(i, new MenuSectionResponse(section.id(), section.title(), section.description(), items));
            found = true;
            break;
        }
        if (!found) throw new IllegalArgumentException("Categoria sconosciuta: " + request.sectionId());
        overridesStore.writeSections(sections);
        return sections;
    }

    public List<MenuSectionResponse> updateItem(String id, MenuItemRequest request) {
        List<MenuSectionResponse> sections = removeItem(findMenuSections(), id, false);
        return insertExisting(sections, toItem(id, request), request.sectionId());
    }

    public List<MenuSectionResponse> deleteItem(String id) {
        List<MenuSectionResponse> sections = removeItem(findMenuSections(), id, true);
        overridesStore.writeSections(sections);
        return sections;
    }

    private List<MenuSectionResponse> insertExisting(List<MenuSectionResponse> sections, MenuItemResponse item, String sectionId) {
        List<MenuSectionResponse> copy = new java.util.ArrayList<>(sections);
        for (int i = 0; i < copy.size(); i++) if (copy.get(i).id().equals(sectionId)) {
            MenuSectionResponse section = copy.get(i); List<MenuItemResponse> items = new java.util.ArrayList<>(section.items()); items.add(item);
            copy.set(i, new MenuSectionResponse(section.id(), section.title(), section.description(), items)); overridesStore.writeSections(copy); return copy;
        }
        throw new IllegalArgumentException("Categoria sconosciuta: " + sectionId);
    }

    private static List<MenuSectionResponse> removeItem(List<MenuSectionResponse> sections, String id, boolean failIfMissing) {
        List<MenuSectionResponse> copy = new java.util.ArrayList<>(); boolean removed = false;
        for (MenuSectionResponse section : sections) { List<MenuItemResponse> items = section.items().stream().filter(item -> !item.id().equals(id)).toList(); removed |= items.size() != section.items().size(); copy.add(new MenuSectionResponse(section.id(), section.title(), section.description(), items)); }
        if (!removed && failIfMissing) throw new IllegalArgumentException("Piatto sconosciuto: " + id); return copy;
    }
    private static MenuItemResponse toItem(String id, MenuItemRequest r) { return new MenuItemResponse(id, r.name().trim(), nullToEmpty(r.subtitle()), nullToEmpty(r.description()), r.notes() == null ? List.of() : r.notes().stream().filter(n -> !n.isBlank()).map(String::trim).toList(), r.price()); }
    private static String nullToEmpty(String value) { return value == null ? "" : value.trim(); }
    private static String slug(String value) { return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""); }

    public List<MenuSectionResponse> defaultMenuSections() {
        return List.of(

                // ── ANTIPASTI ──────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "antipasti",
                        "Antipasti",
                        "Selezione di antipasti freschi di mare e di terra.",
                        List.of(
                                new MenuItemResponse("crudo_di_mare", "Crudo di mare", "Su richiesta", "", List.of(), "25 €"),
                                new MenuItemResponse("antipasto_di_terra", "Antipasto di terra", "Per 2 persone", "", List.of(), "19 €"),
                                new MenuItemResponse("mare_e_monti", "Mare e monti", "", "", List.of(), "20 €"),
                                new MenuItemResponse("veli_crudo_mojito", "Veli di crudo al mojito", "", "", List.of(), "16 €")
                        )
                ),

                // ── SFIZI ──────────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "sfizi",
                        "Sfizi",
                        "Piccoli assaggi e stuzzichini da condividere.",
                        List.of(
                                new MenuItemResponse("salmone_affumicato", "Salmone affumicato", "", "", List.of(), "10 €"),
                                new MenuItemResponse("bresaola_manzo", "Bresaola di manzo", "", "", List.of(), "10 €"),
                                new MenuItemResponse("prosciutto_crudo", "Prosciutto crudo", "", "", List.of(), "15 €"),
                                new MenuItemResponse("crudo_iberico", "Crudo iberico", "", "", List.of(), ""),
                                new MenuItemResponse("patatine_fritte", "Patatine fritte", "", "", List.of(), "5 €"),
                                new MenuItemResponse("verdure_grigliate", "Verdure grigliate", "", "", List.of(), "8 €"),
                                new MenuItemResponse("polpette", "Polpette", "", "", List.of(), "10 €"),
                                new MenuItemResponse("pettoline", "Pettoline", "", "", List.of(), "6 €"),
                                new MenuItemResponse("carpaccio_tonno_rosa", "Carpaccio di Tonno Rosa", "", "", List.of(), "15 €")
                        )
                ),

                // ── INSALATONE ─────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "insalatone",
                        "Insalatone",
                        "Insalate fresche e nutrienti.",
                        List.of(
                                new MenuItemResponse("insalatona_verde", "Insalatona mista verde", "", "", List.of(), "7 €"),
                                new MenuItemResponse("insalatona_proteica", "Insalatona proteica", "", "Straccetti di pollo, pomodoro, grana e rucola", List.of(), "14 €"),
                                new MenuItemResponse("insalatona_gamberoni", "Insalatona con Gamberoni", "", "", List.of(), "15 €"),
                                new MenuItemResponse("insalatona_mare", "Insalatona di Mare", "", "", List.of(), "15 €")
                        )
                ),

                // ── PRIMI DI TERRA ─────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "primi-terra",
                        "Primi di terra",
                        "Paste fresche e gnocchi con i sapori della terra.",
                        List.of(
                                new MenuItemResponse("tagliatelle_porcini", "Tagliatelle ai funghi porcini", "", "", List.of(), "18 €"),
                                new MenuItemResponse("orecchiette_pomodoro", "Orecchiette al pomodoro fresco", "", "", List.of(), "12 €"),
                                new MenuItemResponse("gnocchi_pomodoro", "Gnocchi al pomodoro fresco", "", "", List.of(), "12 €"),
                                new MenuItemResponse("tagliolini_salsiccia", "Tagliolini con crema di porro e salsiccia norcina", "", "", List.of(), "16 €"),
                                new MenuItemResponse("orecchiette_cime", "Orecchiette con cime di rape", "", "Senatore Cappelli, acciughe del Cantabrico, stracciatella", List.of(), "15 €")
                        )
                ),

                // ── SECONDI DI TERRA ───────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "secondi-terra",
                        "Secondi di terra",
                        "Carni selezionate e preparazioni della tradizione.",
                        List.of(
                                new MenuItemResponse("filetto_tartufo", "Filetto di manzo in crema di tartufo o porcini", "Con contorno", "", List.of(), "24 €"),
                                new MenuItemResponse("entrecote_scottona", "Entrecôte di Scottona", "Con contorno, circa 250g", "", List.of(), "24 €"),
                                new MenuItemResponse("tagliata_pollo", "Tagliata di pollo con grana, rucola e pomodoro", "", "", List.of(), "18 €"),
                                new MenuItemResponse("costine_messicana", "Costine alla messicana", "", "", List.of(), "22 €"),
                                new MenuItemResponse("tartare_manzo", "Tartare di manzo", "", "", List.of(), "18 €"),
                                new MenuItemResponse("carpaccio_manzo", "Carpaccio di Manzo", "", "", List.of(), "14 €")
                        )
                ),

                // ── PRIMI DI MARE ──────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "primi-mare",
                        "Primi di mare",
                        "Paste fresche con i frutti del mare.",
                        List.of(
                                new MenuItemResponse("tagliatelle_seppia", "Tagliatelle con seppia", "", "", List.of(), "19 €"),
                                new MenuItemResponse("tagliolini_gambero", "Tagliolini aglio olio e tartare di gambero gobbetto", "", "", List.of(), "16 €"),
                                new MenuItemResponse("tortelloni_astice", "Tortelloni all'astice e granchio reale con crema di crostacei", "", "", List.of(), "21 €"),
                                new MenuItemResponse("spaghetti_ostriche", "Spaghetti alle ostriche", "", "", List.of(), "20 €"),
                                new MenuItemResponse("gnocchi_cozze", "Gnocchi alle cozze", "", "", List.of(), "15 €"),
                                new MenuItemResponse("panino_polpo", "Panino con tentacolo di polpo", "", "", List.of(), "12 €"),
                                new MenuItemResponse("tagliolini_astice", "Tagliolini con 1/2 astice al pomodoro fresco", "", "", List.of(), "28 €"),
                                new MenuItemResponse("pasta_vongole", "Pasta e vongole", "", "", List.of(), "18 €")
                        )
                ),

                // ── SECONDI DI MARE ────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "secondi-mare",
                        "Secondi di mare",
                        "Pesce e frutti di mare freschi, preparati con cura.",
                        List.of(
                                new MenuItemResponse("gamberoni_griglia", "Gamberoni alla griglia", "Con contorno", "", List.of(), "17 €"),
                                new MenuItemResponse("filetto_pesce_spada", "Filetto di pesce spada", "Con contorno", "", List.of(), "16 €"),
                                new MenuItemResponse("frittura_mista", "Frittura mista", "", "", List.of(), "18 €"),
                                new MenuItemResponse("tartare_tonno", "Tartare di tonno", "Con contorno", "", List.of(), "18 €"),
                                new MenuItemResponse("polpo_burratina", "Tentacolo di polpo su burratina affumicata e crema di piselli verdi", "", "", List.of(), "18 €"),
                                new MenuItemResponse("scottata_tonno", "Scottata di Tonno", "", "", List.of(), "21 €")
                        )
                ),

                // ── DOLCI ──────────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "dolci",
                        "Dolci",
                        "Dessert artigianali per concludere in dolcezza.",
                        List.of(
                                new MenuItemResponse("cremoso_caffe", "Cremoso al caffè", "", "", List.of(), "6 €"),
                                new MenuItemResponse("cremoso_frutti_bosco", "Cremoso ai frutti di bosco", "", "", List.of(), "6 €"),
                                new MenuItemResponse("cremoso_pistacchio", "Cremoso al pistacchio", "", "", List.of(), "6 €")
                        )
                ),

                // ── BEVANDE ────────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "bevande",
                        "Bevande",
                        "Analcolici e soft drink.",
                        List.of(
                                new MenuItemResponse("acqua", "Acqua naturale / frizzante", "50 cl", "", List.of(), "2,5 €"),
                                new MenuItemResponse("calice_vino", "Calice di vino", "", "", List.of(), "4 €"),
                                new MenuItemResponse("calice_prosecco", "Calice di prosecco", "", "", List.of(), "4 €"),
                                new MenuItemResponse("calice_franciacorta", "Calice franciacorta", "", "", List.of(), "7 €"),
                                new MenuItemResponse("calice_champagne", "Calice champagne", "", "", List.of(), "10 €"),
                                new MenuItemResponse("coca_cola", "Coca Cola / Coca Cola Zero / Fanta", "", "", List.of(), "3 €"),
                                new MenuItemResponse("succhi_frutta", "Succhi di frutta", "", "", List.of(), "3,5 €"),
                                new MenuItemResponse("chinotto", "Chinotto", "", "", List.of(), "3 €"),
                                new MenuItemResponse("schweppes_lemon", "Schweppes lemon", "", "", List.of(), "3 €"),
                                new MenuItemResponse("the_pesca_limone", "The alla pesca / limone", "", "", List.of(), "3 €"),
                                new MenuItemResponse("san_bitter", "San Bitter bianco / rosso", "", "", List.of(), "3 €"),
                                new MenuItemResponse("cocktail_san_pellegrino", "Cocktail San Pellegrino", "", "", List.of(), "3 €"),
                                new MenuItemResponse("crodino", "Crodino", "", "", List.of(), "3 €"),
                                new MenuItemResponse("acqua_tonica", "Acqua tonica", "", "", List.of(), "3 €"),
                                new MenuItemResponse("red_bull", "Red Bull", "", "", List.of(), "4 €")
                        )
                ),

                // ── GIN ────────────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "gin",
                        "Gin",
                        "Selezione di gin premium da tutto il mondo.",
                        List.of(
                                new MenuItemResponse("tanqueray_london", "Tanqueray London Dry", "vol 43", "", List.of(), "6 €"),
                                new MenuItemResponse("tanqueray_sevilla", "Tanqueray Sevilla", "vol 41,3", "", List.of(), "7 €"),
                                new MenuItemResponse("tanqueray_nten", "Tanqueray N°Ten", "vol 47,3", "", List.of(), "8 €"),
                                new MenuItemResponse("ki_no_tou", "Ki No Tou Kioto", "vol 47,4", "", List.of(), "12 €"),
                                new MenuItemResponse("bagur", "Bagur", "vol 43", "", List.of(), "8 €"),
                                new MenuItemResponse("amuerte_coca", "Amuerte Coca Gin", "vol 43", "", List.of(), "12 €"),
                                new MenuItemResponse("etsu_gin", "Etsu Gin", "vol 43,3", "", List.of(), "8 €"),
                                new MenuItemResponse("roku_gin", "Roku Gin", "vol 43", "", List.of(), "6 €"),
                                new MenuItemResponse("cubical_gin", "Cubical Gin", "vol 40", "", List.of(), "7 €"),
                                new MenuItemResponse("bobbys", "Bobby's", "vol 42", "", List.of(), "8 €"),
                                new MenuItemResponse("nordes_gin", "Nordés Gin", "vol 40", "", List.of(), "8 €"),
                                new MenuItemResponse("black_tomato", "Black Tomato", "vol 42", "", List.of(), "8 €"),
                                new MenuItemResponse("hendrix", "Hendrick's", "vol 44", "", List.of(), "7 €"),
                                new MenuItemResponse("hendrix_neptunia", "Hendrick's Neptunia", "vol 43,4", "", List.of(), "8 €"),
                                new MenuItemResponse("tassoni_gin", "Tassoni Gin", "vol 41,5", "", List.of(), "7 €"),
                                new MenuItemResponse("gin_mare", "Gin Mare", "vol 42,7", "", List.of(), "8 €"),
                                new MenuItemResponse("malfi_rosa", "Malfi Rosa", "vol 41", "", List.of(), "7 €"),
                                new MenuItemResponse("malfi", "Malfi", "vol 41", "", List.of(), "7 €"),
                                new MenuItemResponse("irish_gin", "Irish Gin", "vol 43", "", List.of(), "8 €"),
                                new MenuItemResponse("professore", "Professore", "vol 45", "", List.of(), "8 €"),
                                new MenuItemResponse("cittadinelle", "Cittadinelle", "vol 44", "", List.of(), "7 €"),
                                new MenuItemResponse("ambrosia_dry", "Ambrosia Dry", "vol 40", "", List.of(), "7 €"),
                                new MenuItemResponse("ambrosia_sicily", "Ambrosia Sicily Edition", "vol 40", "", List.of(), "7 €"),
                                new MenuItemResponse("bulldog_gin", "Bulldog Gin", "vol 40", "", List.of(), "7 €"),
                                new MenuItemResponse("adamvs", "Adamvs", "vol 44", "", List.of(), "8 €"),
                                new MenuItemResponse("dolce_vita_gin", "Dolce Vita", "vol 40", "", List.of(), "8 €"),
                                new MenuItemResponse("bombay", "Bombay", "vol 40", "", List.of(), "7 €")
                        )
                ),

                // ── COCKTAILS ──────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "cocktails",
                        "Cocktails",
                        "I classici e le nostre proposte della casa.",
                        List.of(
                                new MenuItemResponse("aperol_spritz", "Aperol Spritz", "", "", List.of(), "5 €"),
                                new MenuItemResponse("campari_spritz", "Campari Spritz", "", "", List.of(), "6 €"),
                                new MenuItemResponse("campari_prosecco", "Campari e Prosecco", "", "", List.of(), "6 €"),
                                new MenuItemResponse("caipirinha", "Caipirinha", "", "", List.of(), "7 €"),
                                new MenuItemResponse("caipiroska", "Caipiroska", "", "", List.of(), "7 €"),
                                new MenuItemResponse("gin_tonic", "Gin Tonic", "", "", List.of(), "6 €"),
                                new MenuItemResponse("hugo", "Hugo", "", "", List.of(), "7 €"),
                                new MenuItemResponse("moscow_mule", "Moscow Mule", "", "", List.of(), "7 €"),
                                new MenuItemResponse("london_mule", "London Mule", "", "", List.of(), "7 €"),
                                new MenuItemResponse("long_island", "Long Island Ice Tea", "", "", List.of(), "8 €"),
                                new MenuItemResponse("japan_ice_tea", "Japan Ice Tea", "", "", List.of(), "7 €"),
                                new MenuItemResponse("mojito_scuro", "Mojito Scuro", "", "", List.of(), "7 €"),
                                new MenuItemResponse("negroni", "Negroni", "", "", List.of(), "7 €"),
                                new MenuItemResponse("negroni_sbagliato", "Negroni Sbagliato", "", "", List.of(), "7 €"),
                                new MenuItemResponse("sex_on_the_beach", "Sex on the Beach", "", "", List.of(), "8 €"),
                                new MenuItemResponse("margherita", "Margherita", "", "", List.of(), "7 €"),
                                new MenuItemResponse("cocktail_martini", "Cocktail Martini", "", "", List.of(), "7 €"),
                                new MenuItemResponse("espresso_martini", "Espresso Martini", "", "", List.of(), "7 €"),
                                new MenuItemResponse("cosmopolitan", "Cosmopolitan", "", "", List.of(), "8 €"),
                                new MenuItemResponse("quattro_bianchi", "Quattro Bianchi", "Alla fragola o al limone", "", List.of(), "10 €")
                        )
                ),

                // ── AMARI ──────────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "amari",
                        "Amari",
                        "Selezione di amari italiani per concludere la serata.",
                        List.of(
                                new MenuItemResponse("ramazzotti", "Ramazzotti", "", "", List.of(), "4 €"),
                                new MenuItemResponse("cynar", "Cynar", "", "", List.of(), "4 €"),
                                new MenuItemResponse("montenegro", "Montenegro", "", "", List.of(), "4 €"),
                                new MenuItemResponse("fernet_branca", "Fernet Branca / Menta", "", "", List.of(), "4 €"),
                                new MenuItemResponse("averna", "Averna", "", "", List.of(), "4 €"),
                                new MenuItemResponse("petrus", "Petrus", "", "", List.of(), "4 €"),
                                new MenuItemResponse("jagermeister", "Jagermeister", "", "", List.of(), "4 €"),
                                new MenuItemResponse("jefferson", "Jefferson", "", "", List.of(), "4 €"),
                                new MenuItemResponse("unicum", "Unicum", "", "", List.of(), "4 €"),
                                new MenuItemResponse("lucano", "Lucano", "", "", List.of(), "4 €"),
                                new MenuItemResponse("amaro_del_capo", "Amaro del Capo", "", "", List.of(), "4 €"),
                                new MenuItemResponse("sambuca", "Sambuca", "", "", List.of(), "4 €"),
                                new MenuItemResponse("caffe_borghetti", "Caffè Borghetti", "", "", List.of(), "4 €"),
                                new MenuItemResponse("vena_caffe", "Vena Caffè", "", "", List.of(), "4 €")
                        )
                ),

                // ── DISTILLATI ─────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "distillati",
                        "Distillati",
                        "Whisky, Rum e altri pregiati distillati.",
                        List.of(
                                new MenuItemResponse("vecchia_romagna", "Vecchia Romagna", "", "", List.of(), "5 €"),
                                new MenuItemResponse("cointreau", "Cointreau", "", "", List.of(), "5 €"),
                                new MenuItemResponse("jack_daniels", "Jack Daniel's", "", "", List.of(), "6 €"),
                                new MenuItemResponse("jack_daniels_honey", "Jack Daniel's Honey", "", "", List.of(), "6 €"),
                                new MenuItemResponse("oban", "Oban", "", "", List.of(), "9 €"),
                                new MenuItemResponse("laphroaig", "Laphroaig", "", "", List.of(), "12 €"),
                                new MenuItemResponse("lagavulin", "Lagavulin", "", "", List.of(), "12 €"),
                                new MenuItemResponse("sambuca_dist", "Sambuca", "", "", List.of(), "5 €"),
                                new MenuItemResponse("martini", "Martini Bianco / Rosso / Dry", "", "", List.of(), "5 €"),
                                new MenuItemResponse("bacardi", "Bacardi", "Rum", "", List.of(), "5 €"),
                                new MenuItemResponse("don_papa", "Don Papa", "Rum", "", List.of(), "8 €"),
                                new MenuItemResponse("zacapa_23", "Zacapa 23", "Rum", "", List.of(), "12 €"),
                                new MenuItemResponse("shot_2cl", "Shot con distillati base", "2 cl", "", List.of(), "3 €")
                        )
                ),

                // ── VINI BIANCHI ───────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "vini-bianchi",
                        "Vini bianchi",
                        "Selezione di vini bianchi pugliesi e nazionali.",
                        List.of(
                                new MenuItemResponse("calavento_igp", "Calavento IGP Salento", "", "", List.of(), "21 €"),
                                new MenuItemResponse("luna_igp", "Luna IGP Salento", "", "", List.of(), "21 €"),
                                new MenuItemResponse("leverano_bianco", "Leverano Vecchia Torre", "", "", List.of(), "16 €"),
                                new MenuItemResponse("muller_thurgau", "Muller Thurgau", "", "", List.of(), "21 €"),
                                new MenuItemResponse("gewurztraminer", "Gewurztraminer", "", "", List.of(), "21 €"),
                                new MenuItemResponse("falanghina", "Falanghina", "", "", List.of(), "16 €"),
                                new MenuItemResponse("trebbiano_abbruzzo", "Trebbiano d'Abruzzo", "", "", List.of(), "16 €"),
                                new MenuItemResponse("fiano", "Fiano", "", "", List.of(), "18 €"),
                                new MenuItemResponse("verdeca_due_trulli", "Verdeca Due Trulli", "", "", List.of(), "18 €"),
                                new MenuItemResponse("chardonnay", "Chardonnay", "", "", List.of(), "18 €")
                        )
                ),

                // ── VINI ROSATI ────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "vini-rosati",
                        "Vini rosati",
                        "Rosati freschi e fruttati della tradizione pugliese.",
                        List.of(
                                new MenuItemResponse("leverano_rosato", "Leverano DOP Vecchia Torre", "", "", List.of(), "16 €"),
                                new MenuItemResponse("negroamaro_rosato", "Negroamaro Vecchia Torre", "", "", List.of(), "18 €"),
                                new MenuItemResponse("primitivo_rosato_1932", "Primitivo Rosato 1932", "", "", List.of(), "19 €"),
                                new MenuItemResponse("no_negroamaro", "N-O Negroamaro Susumaniello", "", "", List.of(), "21 €"),
                                new MenuItemResponse("susumaniello_rosato", "Susumaniello Due Trulli", "", "", List.of(), "21 €")
                        )
                ),

                // ── VINI ROSSI ─────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "vini-rossi",
                        "Vini rossi",
                        "Rossi corposi e strutturati, pugliesi e nazionali.",
                        List.of(
                                new MenuItemResponse("primitivo_vt", "Primitivo Vecchia Torre", "", "", List.of(), "16 €"),
                                new MenuItemResponse("primitivo_dt", "Primitivo Due Trulli", "", "", List.of(), "18 €"),
                                new MenuItemResponse("primitivo_vigniaioli", "Primitivo Vigniaioli 68 IGP", "", "", List.of(), "28 €"),
                                new MenuItemResponse("primitivo_manduria", "Primitivo di Manduria 1932", "", "", List.of(), "21 €"),
                                new MenuItemResponse("negroamaro_vt", "Negroamaro Vecchia Torre", "", "", List.of(), "16 €"),
                                new MenuItemResponse("negroamaro_dt", "Negroamaro Due Trulli", "", "", List.of(), "18 €"),
                                new MenuItemResponse("negroamaro_manorossa", "Negroamaro Manorossa", "", "", List.of(), "60 €"),
                                new MenuItemResponse("negroamaro_susumaniello", "Negroamaro-Susumaniello", "", "", List.of(), "34 €"),
                                new MenuItemResponse("susumaniello_vigna14", "Susumaniello Vigna 14 IGP", "", "", List.of(), "18 €"),
                                new MenuItemResponse("nerotavola_sicilia", "Nerotavola Sicilia DOC", "", "", List.of(), "28 €"),
                                new MenuItemResponse("nero_di_troia", "Nero di Troia", "", "", List.of(), "16 €"),
                                new MenuItemResponse("aglianico", "Aglianico", "", "", List.of(), "18 €"),
                                new MenuItemResponse("cabernet_veneto", "Cabernet Veneto", "", "", List.of(), "21 €"),
                                new MenuItemResponse("ripasso_negrar", "Ripasso Negrar", "", "", List.of(), "26 €"),
                                new MenuItemResponse("chianti_classico", "Chianti Classico", "", "", List.of(), "20 €"),
                                new MenuItemResponse("brunello", "Brunello", "", "", List.of(), "40 €"),
                                new MenuItemResponse("amarone", "Amarone", "", "", List.of(), "40 €")
                        )
                ),

                // ── VINI BIO ───────────────────────────────────────────────────────────────
                new MenuSectionResponse(
                        "vini-bio",
                        "Vini bio",
                        "Selezione di vini biologici bianchi, rosati e rossi.",
                        List.of(
                                new MenuItemResponse("trebbiano_bio", "Trebbiano d'Abruzzo bio vegano", "Bianco", "", List.of(), "19 €"),
                                new MenuItemResponse("passerina_bio", "Passerina bio vegano", "Bianco", "", List.of(), "19 €"),
                                new MenuItemResponse("pecorino_bio", "Pecorino bio", "Bianco", "", List.of(), "19 €"),
                                new MenuItemResponse("castel_monte_bio_bianco", "Castel del Monte bio", "Bianco", "", List.of(), "19 €"),
                                new MenuItemResponse("vitalba_bio", "Vitalba bio", "Bianco", "", List.of(), "19 €"),
                                new MenuItemResponse("dharma_bio", "Dharma bio", "Bianco", "", List.of(), "19 €"),
                                new MenuItemResponse("novebolle_doc", "Novebolle D.O.C.", "Spumante", "", List.of(), "19 €"),
                                new MenuItemResponse("castel_monte_bio_rosato", "Castel del Monte bio", "Rosato", "", List.of(), "19 €"),
                                new MenuItemResponse("castel_monte_bio_rosso", "Castel del Monte bio", "Rosso", "", List.of(), "19 €")
                        )
                ),

                // ── PROSECCO E CHAMPAGNE ───────────────────────────────────────────────────
                new MenuSectionResponse(
                        "prosecco",
                        "Prosecco e Champagne",
                        "Bollicine per ogni occasione.",
                        List.of(
                                new MenuItemResponse("monticano", "Monticano", "Prosecco", "", List.of(), "21 €"),
                                new MenuItemResponse("fragolino", "Fragolino", "Prosecco", "", List.of(), "18 €"),
                                new MenuItemResponse("asti", "Asti", "Prosecco", "", List.of(), "15 €"),
                                new MenuItemResponse("franciacorta", "Franciacorta", "Spumante", "", List.of(), "48 €"),
                                new MenuItemResponse("moet_chandon", "Moët & Chandon", "Champagne", "", List.of(), "70 €"),
                                new MenuItemResponse("veuve_clicquot", "Veuve Clicquot", "Champagne", "", List.of(), "65 €")
                        )
                )
        );
    }

    public List<MenuItemResponse> findSignatureDrinks() {
        return findMenuSections().stream()
                .filter(section -> "cocktails".equals(section.id()))
                .findFirst()
                .map(MenuSectionResponse::items)
                .orElseGet(List::of);
    }

    /**
     * Merges the requested prices into the overrides file and returns the resulting menu.
     * Prices equal to the hardcoded one are dropped, so the file only ever holds real changes
     * and an item reverts to its default simply by being set back to it.
     *
     * @throws IllegalArgumentException if an item id is unknown or a price is malformed
     */
    public List<MenuSectionResponse> updatePrices(Map<String, String> requestedPrices) {
        Map<String, String> defaults = defaultPrices();
        Set<String> unknownIds = new TreeSet<>();
        Set<String> invalidPrices = new TreeSet<>();

        Map<String, String> sanitized = new LinkedHashMap<>();
        requestedPrices.forEach((id, price) -> {
            if (id == null || !defaults.containsKey(id)) {
                unknownIds.add(String.valueOf(id));
                return;
            }
            String trimmed = price == null ? "" : price.trim();
            if (!PRICE_PATTERN.matcher(trimmed).matches()) {
                invalidPrices.add(id);
                return;
            }
            sanitized.put(id, trimmed);
        });

        if (!unknownIds.isEmpty()) {
            throw new IllegalArgumentException("Voci di menù sconosciute: " + String.join(", ", unknownIds));
        }
        if (!invalidPrices.isEmpty()) {
            throw new IllegalArgumentException("Prezzi non validi per: " + String.join(", ", invalidPrices));
        }

        Map<String, String> overrides = new LinkedHashMap<>(overridesStore.readPrices());
        overrides.putAll(sanitized);
        overrides.entrySet().removeIf(entry -> !defaults.containsKey(entry.getKey())
                || entry.getValue().equals(defaults.get(entry.getKey())));

        overridesStore.writePrices(overrides);
        return applyPriceOverrides(defaultMenuSections(), overrides);
    }

    /** Hardcoded prices keyed by item id; item ids are unique across sections. */
    public Map<String, String> defaultPrices() {
        return defaultMenuSections().stream()
                .flatMap(section -> section.items().stream())
                .collect(Collectors.toMap(MenuItemResponse::id, item -> item.price() == null ? "" : item.price().toPlainString(), (first, second) -> first,
                        LinkedHashMap::new));
    }

    private static List<MenuSectionResponse> applyPriceOverrides(List<MenuSectionResponse> sections,
            Map<String, String> prices) {
        if (prices.isEmpty()) {
            return sections;
        }

        return sections.stream()
                .map(section -> new MenuSectionResponse(
                        section.id(),
                        section.title(),
                        section.description(),
                        section.items().stream().map(item -> {
                            String price = prices.get(item.id());
                            return price == null || price.equals(item.price() == null ? "" : item.price().toPlainString())
                                    ? item
                                    : new MenuItemResponse(item.id(), item.name(), item.subtitle(),
                                            item.description(), item.notes(), price);
                        }).toList()))
                .toList();
    }
}
