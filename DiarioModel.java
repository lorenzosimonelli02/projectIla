import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory per gli ingredienti che garantisce la coerenza nella creazione e
 * semplifica la gestione dei prezzi
 */
class IngredienteFactory {
    private static final Map<String, Double> prezziCache = new HashMap<>();
    
    /**
     * Crea un nuovo ingrediente con prezzo se disponibile
     */
    public static Ingrediente crea(String nome, double quantita, String unita) {
        Ingrediente ingrediente = new Ingrediente(nome, quantita, unita);
        
        // Applica prezzo dalla cache se disponibile
        Double prezzo = prezziCache.get(nome);
        if (prezzo != null) {
            ingrediente.setPrezzo(prezzo);
        }
        
        return ingrediente;
    }
    
    /**
     * Registra un prezzo nella cache
     */
    public static void registraPrezzo(String nomeIngrediente, double prezzo) {
        prezziCache.put(nomeIngrediente, prezzo);
    }
    
    /**
     * Ottiene il prezzo di un ingrediente
     */
    public static double getPrezzo(String nomeIngrediente) {
        return prezziCache.getOrDefault(nomeIngrediente, 0.0);
    }
    
    /**
     * Pulisce la cache dei prezzi
     */
    public static void reset() {
        prezziCache.clear();
    }
}

/**
 * Classe che rappresenta un ingrediente con nome, quantità e unità di misura
 */
class Ingrediente {
    private final String nome;
    private final double quantita;
    private final String unita;
    private double prezzo = 0.0; // prezzo unitario

    public Ingrediente(String nome, double quantita, String unita) {
        this.nome = nome;
        this.quantita = quantita;
        this.unita = unita;
    }

    public String getNome() {
        return nome;
    }

    public double getQuantita() {
        return quantita;
    }

    public String getUnita() {
        return unita;
    }
    
    public double getPrezzo() {
        return prezzo;
    }
    
    public void setPrezzo(double prezzo) {
        this.prezzo = prezzo;
    }
    
    /**
     * Crea una copia dell'ingrediente
     */
    public Ingrediente copia() {
        Ingrediente copia = new Ingrediente(this.nome, this.quantita, this.unita);
        copia.setPrezzo(this.prezzo);
        return copia;
    }
    
    /**
     * Crea un nuovo ingrediente con quantità modificata ma stessi valori per gli altri campi
     */
    public Ingrediente conQuantita(double nuovaQuantita) {
        Ingrediente nuovo = new Ingrediente(this.nome, nuovaQuantita, this.unita);
        nuovo.setPrezzo(this.prezzo);
        return nuovo;
    }
    
    /**
     * Calcola costo totale dell'ingrediente
     */
    public double getCostoTotale() {
        return quantita * prezzo;
    }
    
    /**
     * Crea una chiave univoca per l'ingrediente basata su nome e unità
     */
    public String getChiave() {
        return nome + "_" + unita;
    }
    
    @Override
    public String toString() {
        return String.format("%s: %.2f %s", nome, quantita, unita);
    }
}

/**
 * Classe che rappresenta una ricetta con nome e lista di ingredienti
 */
class Ricetta {
    private final String nome;
    private final List<Ingrediente> ingredienti;

    public Ricetta(String nome) {
        this.nome = nome;
        this.ingredienti = new ArrayList<>();
    }

    public String getNome() {
        return nome;
    }

    public List<Ingrediente> getIngredienti() {
        return Collections.unmodifiableList(ingredienti);
    }

    public void aggiungiIngrediente(Ingrediente ingrediente) {
        ingredienti.add(ingrediente);
    }
    
    /**
     * Calcola il costo totale della ricetta
     */
    public double getCostoTotale() {
        return ingredienti.stream()
               .mapToDouble(Ingrediente::getCostoTotale)
               .sum();
    }

    @Override
    public String toString() {
        return nome;
    }
}

/**
 * Enum per rappresentare i pasti della giornata
 */
enum TipoPasto {
    COLAZIONE("Colazione", 2),
    PRANZO("Pranzo", 3),
    CENA("Cena", 3);
    
    private final String nome;
    private final int maxRicette;
    
    TipoPasto(String nome, int maxRicette) {
        this.nome = nome;
        this.maxRicette = maxRicette;
    }
    
    public String getNome() {
        return nome;
    }
    
    public int getMaxRicette() {
        return maxRicette;
    }
    
    /**
     * Ottiene il nome del file associato a questo tipo di pasto
     */
    public String getNomeFile() {
        return nome.toLowerCase() + ".txt";
    }
}

/**
 * Rappresenta una giornata alimentare con colazione, pranzo e cena
 */
class PianificazioneGiornaliera {
    private final Map<TipoPasto, List<Ricetta>> pasti;
    
    public PianificazioneGiornaliera() {
        // Utilizzo di EnumMap per ottimizzare le performance con le enum
        pasti = new EnumMap<>(TipoPasto.class);
        
        // Inizializza con liste vuote per ogni tipo di pasto
        for (TipoPasto tipo : TipoPasto.values()) {
            pasti.put(tipo, new ArrayList<>());
        }
    }
    
    /**
     * Ottiene la lista di ricette per un determinato pasto
     */
    public List<Ricetta> getRicette(TipoPasto tipo) {
        return pasti.get(tipo);
    }
    
    /**
     * Aggiunge una ricetta a un pasto se non si è raggiunto il limite
     * @return true se la ricetta è stata aggiunta, false altrimenti
     */
    public boolean aggiungiRicetta(TipoPasto tipo, Ricetta ricetta) {
        List<Ricetta> ricette = pasti.get(tipo);
        
        // Controlla se abbiamo raggiunto il limite di ricette per questo pasto
        if (ricette.size() < tipo.getMaxRicette()) {
            ricette.add(ricetta);
            return true;
        }
        
        return false;
    }
    
    /**
     * Rimuove una ricetta da un pasto
     * @return true se la ricetta è stata rimossa, false se non esisteva
     */
    public boolean rimuoviRicetta(TipoPasto tipo, Ricetta ricetta) {
        return pasti.get(tipo).remove(ricetta);
    }
    
    /**
     * Ottiene tutti gli ingredienti di tutte le ricette in questa giornata
     */
    public List<Ingrediente> getTuttiIngredienti() {
        return pasti.values().stream()
                .flatMap(List::stream)
                .flatMap(ricetta -> ricetta.getIngredienti().stream())
                .collect(Collectors.toList());
    }
}

/**
 * Classe per gestire le ricette e la lista della spesa
 */
class GestoreRicette {
    // Mappa che associa ogni tipo di pasto alla sua lista di ricette
    private final Map<TipoPasto, List<Ricetta>> ricettariPerTipo;
    
    // Costanti per il caricamento dei file
    private static final String DELIMITATORE_CSV = ",";
    private static final String INDICATORE_INGREDIENTE = "-";

    public GestoreRicette() {
        // Utilizziamo EnumMap per ottimizzare l'uso di enum come chiavi
        ricettariPerTipo = new EnumMap<>(TipoPasto.class);
        
        // Inizializza liste vuote per ogni tipo di pasto
        for (TipoPasto tipo : TipoPasto.values()) {
            ricettariPerTipo.put(tipo, new ArrayList<>());
        }
    }

    /**
     * Ottiene il ricettario per un determinato tipo di pasto
     */
    public List<Ricetta> getRicettario(TipoPasto tipo) {
        return Collections.unmodifiableList(ricettariPerTipo.get(tipo));
    }
    
    /**
     * Ottiene la lista completa di tutte le ricette
     */
    public List<Ricetta> getTutteLeRicette() {
        return ricettariPerTipo.values().stream()
               .flatMap(List::stream)
               .collect(Collectors.toList());
    }

    /**
     * Carica le ricette da un file di testo specifico per un tipo di pasto
     */
    public void caricaRicettePerTipo(TipoPasto tipo, String nomeFile) throws IOException {
        List<Ricetta> ricettario = ricettariPerTipo.get(tipo);
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(nomeFile))) {
            String line;
            Ricetta ricettaCorrente = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (!line.startsWith(INDICATORE_INGREDIENTE)) {
                    // Nuova ricetta
                    ricettaCorrente = new Ricetta(line);
                    ricettario.add(ricettaCorrente);
                } else if (ricettaCorrente != null) {
                    // Ingrediente della ricetta corrente
                    processaRigaIngrediente(line.substring(1).trim(), ricettaCorrente);
                }
            }
        }
    }
    
    /**
     * Metodo di utilità per caricare le ricette per tutti i tipi di pasto
     */
    public void caricaTutteLeRicette() throws IOException {
        for (TipoPasto tipo : TipoPasto.values()) {
            caricaRicettePerTipo(tipo, tipo.getNomeFile());
        }
    }
    
    /**
     * Processa una riga ingrediente e la aggiunge alla ricetta
     */
    private void processaRigaIngrediente(String riga, Ricetta ricetta) {
        String[] parti = riga.split(DELIMITATORE_CSV);
        if (parti.length < 3) return;
        
        try {
            String nomeIngrediente = parti[0].trim();
            double quantita = Double.parseDouble(parti[1].trim());
            String unita = parti[2].trim();
            
            // Usa il factory method per creare l'ingrediente
            Ingrediente ingrediente = IngredienteFactory.crea(nomeIngrediente, quantita, unita);
            ricetta.aggiungiIngrediente(ingrediente);
        } catch (NumberFormatException e) {
            System.err.println("Errore di formato nei dati dell'ingrediente: " + riga);
        }
    }

    /**
     * Carica i prezzi degli ingredienti da un file di testo
     */
    public void caricaPrezzi(String nomeFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(nomeFile))) {
            reader.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> line.split(DELIMITATORE_CSV))
                .filter(parti -> parti.length >= 2)
                .forEach(parti -> {
                    try {
                        String nomeIngrediente = parti[0].trim();
                        double prezzo = Double.parseDouble(parti[1].trim());
                        IngredienteFactory.registraPrezzo(nomeIngrediente, prezzo);
                    } catch (NumberFormatException e) {
                        System.err.println("Errore di formato nel prezzo: " + String.join(",", parti));
                    }
                });
        }
        
        // Aggiorna i prezzi negli ingredienti di tutte le ricette
        ricettariPerTipo.values().stream()
            .flatMap(List::stream)
            .forEach(ricetta -> {
                for (Ingrediente ingrediente : ricetta.getIngredienti()) {
                    double prezzo = IngredienteFactory.getPrezzo(ingrediente.getNome());
                    ingrediente.setPrezzo(prezzo);
                }
            });
    }

    /**
     * Genera una lista della spesa aggregata dalle ricette selezionate in modo più efficiente
     */
    public Map<String, Ingrediente> generaListaSpesa(Map<String, Map<TipoPasto, List<Ricetta>>> pianificazione) {
        Map<String, Ingrediente> listaSpesa = new HashMap<>();

        pianificazione.values().stream()
            .flatMap(pastiGiorno -> pastiGiorno.values().stream())
            .flatMap(List::stream)
            .flatMap(ricetta -> ricetta.getIngredienti().stream())
            .forEach(ingrediente -> {
                String chiave = ingrediente.getChiave();
                
                // Usa computeIfPresent e computeIfAbsent per un codice più pulito
                listaSpesa.compute(chiave, (k, existing) -> {
                    if (existing == null) {
                        // Se l'ingrediente non esiste ancora, crea una copia
                        return ingrediente.copia();
                    } else {
                        // Altrimenti, crea un nuovo ingrediente con la quantità aggiornata
                        return existing.conQuantita(existing.getQuantita() + ingrediente.getQuantita());
                    }
                });
            });

        return listaSpesa;
    }

    /**
     * Calcola il costo totale della lista della spesa in modo più efficiente
     */
    public double calcolaCostoTotale(Map<String, Ingrediente> listaSpesa) {
        return listaSpesa.values().stream()
               .mapToDouble(Ingrediente::getCostoTotale)
               .sum();
    }
    
    /**
     * Ottiene una ricetta per nome o null se non esiste
     */
    public Optional<Ricetta> getRicettaPerNome(String nome) {
        return ricettariPerTipo.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getNome().equalsIgnoreCase(nome))
                .findFirst();
    }
}
