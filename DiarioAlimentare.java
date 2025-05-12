import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class DiarioAlimentare {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            DiarioGUI gui = new DiarioGUI();
            gui.setVisible(true);
        });
    }
}

/**
 * Classe che rappresenta un ingrediente con nome, quantità e unità di misura
 */
class Ingrediente {
    private String nome;
    private double quantita;
    private String unita;
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
    
    public Ingrediente copia() {
        Ingrediente copia = new Ingrediente(this.nome, this.quantita, this.unita);
        copia.setPrezzo(this.prezzo);
        return copia;
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
    private String nome;
    private List<Ingrediente> ingredienti;

    public Ricetta(String nome) {
        this.nome = nome;
        this.ingredienti = new ArrayList<>();
    }

    public String getNome() {
        return nome;
    }

    public List<Ingrediente> getIngredienti() {
        return ingredienti;
    }

    public void aggiungiIngrediente(Ingrediente ingrediente) {
        ingredienti.add(ingrediente);
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
}

/**
 * Classe per gestire le ricette e la lista della spesa
 */
class GestoreRicette {
    private List<Ricetta> ricettario;
    private Map<String, Double> prezziIngredienti;

    public GestoreRicette() {
        ricettario = new ArrayList<>();
        prezziIngredienti = new HashMap<>();
    }

    public List<Ricetta> getRicettario() {
        return ricettario;
    }

    /**
     * Carica le ricette da un file di testo
     */
    public void caricaRicette(String nomeFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(nomeFile));
        String line;
        Ricetta ricettaCorrente = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (!line.startsWith("-")) {
                // Nuova ricetta
                ricettaCorrente = new Ricetta(line);
                ricettario.add(ricettaCorrente);
            } else if (ricettaCorrente != null) {
                // Ingrediente della ricetta corrente
                line = line.substring(1).trim();
                String[] parti = line.split(",");
                if (parti.length >= 3) {
                    String nomeIngrediente = parti[0].trim();
                    double quantita = Double.parseDouble(parti[1].trim());
                    String unita = parti[2].trim();
                    ricettaCorrente.aggiungiIngrediente(new Ingrediente(nomeIngrediente, quantita, unita));
                }
            }
        }
        reader.close();
    }

    /**
     * Carica i prezzi degli ingredienti da un file di testo
     */
    public void caricaPrezzi(String nomeFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(nomeFile));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parti = line.split(",");
            if (parti.length >= 2) {
                String nomeIngrediente = parti[0].trim();
                double prezzo = Double.parseDouble(parti[1].trim());
                prezziIngredienti.put(nomeIngrediente, prezzo);
            }
        }
        reader.close();
        
        // Aggiorna i prezzi negli ingredienti del ricettario
        for (Ricetta ricetta : ricettario) {
            for (Ingrediente ingrediente : ricetta.getIngredienti()) {
                if (prezziIngredienti.containsKey(ingrediente.getNome())) {
                    ingrediente.setPrezzo(prezziIngredienti.get(ingrediente.getNome()));
                }
            }
        }
    }

    /**
     * Genera una lista della spesa aggregata dalle ricette selezionate
     */
    public Map<String, Ingrediente> generaListaSpesa(Map<String, Map<TipoPasto, List<Ricetta>>> pianificazione) {
        Map<String, Ingrediente> listaSpesa = new HashMap<>();

        // Per ogni giorno della settimana
        for (Map.Entry<String, Map<TipoPasto, List<Ricetta>>> entryGiorno : pianificazione.entrySet()) {
            Map<TipoPasto, List<Ricetta>> pastiGiorno = entryGiorno.getValue();
            
            // Per ogni pasto del giorno
            for (Map.Entry<TipoPasto, List<Ricetta>> entryPasto : pastiGiorno.entrySet()) {
                List<Ricetta> ricettePasto = entryPasto.getValue();
                
                // Per ogni ricetta del pasto
                for (Ricetta ricetta : ricettePasto) {
                    // Per ogni ingrediente della ricetta
                    for (Ingrediente ingrediente : ricetta.getIngredienti()) {
                        String chiave = ingrediente.getNome() + "_" + ingrediente.getUnita();
                        
                        if (listaSpesa.containsKey(chiave)) {
                            // Se l'ingrediente è già presente, aggiorna la quantità
                            Ingrediente ingredienteEsistente = listaSpesa.get(chiave);
                            double nuovaQuantita = ingredienteEsistente.getQuantita() + ingrediente.getQuantita();
                            Ingrediente nuovoIngrediente = new Ingrediente(ingrediente.getNome(), nuovaQuantita, ingrediente.getUnita());
                            nuovoIngrediente.setPrezzo(ingrediente.getPrezzo());
                            listaSpesa.put(chiave, nuovoIngrediente);
                        } else {
                            // Altrimenti, aggiungi l'ingrediente alla lista
                            listaSpesa.put(chiave, ingrediente.copia());
                        }
                    }
                }
            }
        }

        return listaSpesa;
    }

    /**
     * Calcola il costo totale della lista della spesa
     */
    public double calcolaCostoTotale(Map<String, Ingrediente> listaSpesa) {
        double costoTotale = 0.0;

        for (Ingrediente ingrediente : listaSpesa.values()) {
            costoTotale += ingrediente.getQuantita() * ingrediente.getPrezzo();
        }

        return costoTotale;
    }
}

/**
 * Classe per l'interfaccia grafica del diario alimentare
 */
class DiarioGUI extends JFrame {
    private static final String[] GIORNI = {"Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica"};
    private GestoreRicette gestore;
    private Map<String, Map<TipoPasto, List<Ricetta>>> pianificazione;
    private Map<String, Map<TipoPasto, DefaultListModel<Ricetta>>> modelliGiorni;
    private Map<String, Map<TipoPasto, JList<Ricetta>>> listePianificazione;
    private JTextArea areaListaSpesa;
    private JLabel labelCostoTotale;
    private JPanel pannelloDettagliRicetta;
    private JTextArea dettagliRicetta;
    
    public DiarioGUI() {
        gestore = new GestoreRicette();
        pianificazione = new HashMap<>();
        listePianificazione = new HashMap<>();
        modelliGiorni = new HashMap<>();
        
        // Inizializza la pianificazione con liste vuote
        for (String giorno : GIORNI) {
            pianificazione.put(giorno, new HashMap<>());
            modelliGiorni.put(giorno, new HashMap<>());
            listePianificazione.put(giorno, new HashMap<>());
            
            // Inizializza per ogni tipo di pasto
            for (TipoPasto tipoPasto : TipoPasto.values()) {
                pianificazione.get(giorno).put(tipoPasto, new ArrayList<>());
                modelliGiorni.get(giorno).put(tipoPasto, new DefaultListModel<>());
            }
        }
        
        // Configurazione della finestra
        setTitle("Diario Alimentare Settimanale");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);
        
        // Carica dati
        try {
            gestore.caricaRicette("ricettario.txt");
            gestore.caricaPrezzi("volantino.txt");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Errore nel caricamento dei file: " + e.getMessage(), 
                                         "Errore", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        
        // Inizializza i componenti dell'interfaccia
        initComponents();
    }
    
    private void initComponents() {
        // Look and feel più moderno
        try {
            // Usa FlatLaf attraverso UIManager se disponibile
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Button.borderWidth", 1);
            UIManager.put("Button.innerFocusWidth", 1);
            
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            
            // Aggiorna il font predefinito
            Font defaultFont = new Font("Segoe UI", Font.PLAIN, 12);
            UIManager.put("Label.font", defaultFont);
            UIManager.put("Button.font", defaultFont);
            UIManager.put("TextField.font", defaultFont);
            UIManager.put("ComboBox.font", defaultFont);
            UIManager.put("List.font", defaultFont);
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        // Layout principale
        setLayout(new BorderLayout(15, 15));
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(250, 250, 252)); // Sfondo leggermente off-white
        
        // Pannello per la pianificazione settimanale
        JPanel pianificazionePanel = createPianificazionePanel();
        
        // Pannello per i dettagli della ricetta
        pannelloDettagliRicetta = createPannelloDettagliRicetta();
        pannelloDettagliRicetta.setVisible(false); // Nascosto inizialmente
        
        // Pannello per la lista della spesa
        JPanel listaSpesaPanel = createListaSpesaPanel();
        
        // Layout dei pannelli
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setOpaque(false);
        topPanel.add(pianificazionePanel, BorderLayout.CENTER);
        topPanel.add(pannelloDettagliRicetta, BorderLayout.EAST);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, listaSpesaPanel);
        splitPane.setResizeWeight(0.7);
        splitPane.setDividerSize(8);
        splitPane.setBorder(null);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createPannelloDettagliRicetta() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(new Color(250, 250, 252));
        panel.setPreferredSize(new Dimension(320, 0));
        
        // Titolo del pannello
        JLabel labelTitolo = new JLabel("Dettagli Ricetta");
        labelTitolo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        labelTitolo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(0, 0, 10, 0)
        ));
        panel.add(labelTitolo, BorderLayout.NORTH);
        
        // Area testo con stile moderno
        dettagliRicetta = new JTextArea();
        dettagliRicetta.setEditable(false);
        dettagliRicetta.setLineWrap(true);
        dettagliRicetta.setWrapStyleWord(true);
        dettagliRicetta.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dettagliRicetta.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        dettagliRicetta.setBackground(new Color(255, 255, 255));
        
        JScrollPane scrollPane = new JScrollPane(dettagliRicetta);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 20), 1));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Pulsante chiudi moderno
        JButton chiudiButton = new JButton("Chiudi");
        chiudiButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chiudiButton.setFocusPainted(false);
        chiudiButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        chiudiButton.setBackground(new Color(240, 240, 240));
        
        chiudiButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                chiudiButton.setBackground(new Color(220, 220, 220));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                chiudiButton.setBackground(new Color(240, 240, 240));
            }
        });
        
        chiudiButton.addActionListener(e -> pannelloDettagliRicetta.setVisible(false));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        buttonPanel.add(chiudiButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createPianificazionePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(2, 2, 2, 2),
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200))
                ),
                "Pianificazione Settimanale"
            )
        ));
        panel.setBackground(new Color(250, 250, 252));
        
        // Crea un pannello centrale con layout a griglia di 7 giorni
        JPanel mainGrid = new JPanel(new GridLayout(1, 7, 12, 0));
        mainGrid.setOpaque(false);
        
        // Colori per i giorni - Palette più moderna e sofisticata
        Color[] coloreGiorni = {
            new Color(79, 143, 230),    // Lunedì - blu
            new Color(94, 168, 230),    // Martedì - azzurro
            new Color(240, 151, 114),   // Mercoledì - pesca
            new Color(248, 177, 87),    // Giovedì - arancione chiaro
            new Color(134, 206, 137),   // Venerdì - verde
            new Color(236, 128, 141),   // Sabato - rosa
            new Color(171, 138, 235)    // Domenica - viola
        };
        
        // Crea ComboBox con tutte le ricette
        Vector<Ricetta> ricetteVector = new Vector<>(gestore.getRicettario());
        
        // Per ogni giorno della settimana
        for (int i = 0; i < GIORNI.length; i++) {
            String giorno = GIORNI[i];
            Color coloreGiorno = coloreGiorni[i];
            
            // Crea un pannello per il giorno con bordo più evidente e arrotondato
            JPanel giornoPanel = new JPanel(new BorderLayout(5, 5));
            giornoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(coloreGiorno, 2),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)
            ));
            giornoPanel.setBackground(new Color(250, 250, 252));
            
            // Intestazione del giorno più moderna
            JPanel headerGiorno = new JPanel(new BorderLayout());
            headerGiorno.setBackground(coloreGiorno);
            headerGiorno.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
            
            JLabel labelGiorno = new JLabel(giorno, JLabel.CENTER);
            labelGiorno.setFont(new Font("Segoe UI", Font.BOLD, 16));
            labelGiorno.setForeground(Color.WHITE);
            headerGiorno.add(labelGiorno, BorderLayout.CENTER);
            
            giornoPanel.add(headerGiorno, BorderLayout.NORTH);
            
            // Pannello centrale con i tre tipi di pasto
            JPanel pastiPanel = new JPanel(new GridLayout(3, 1, 0, 8)); // Aumento spazio tra i pasti
            pastiPanel.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));
            pastiPanel.setOpaque(false);
            
            // Crea versioni più chiare del colore del giorno per ogni pasto
            // Utilizzo fattori di intensità per creare sfumature dello stesso colore
            Color coloreColazione = adjustColorBrightness(coloreGiorno, 1.3f); // Più chiaro
            Color colorePranzo = adjustColorBrightness(coloreGiorno, 1.1f);    // Medio chiaro
            Color coloreCena = adjustColorBrightness(coloreGiorno, 0.9f);      // Leggermente scuro
            
            // Per ogni tipo di pasto
            int indicePasto = 0;
            for (TipoPasto tipoPasto : TipoPasto.values()) {
                Color colorePasto;
                switch (indicePasto) {
                    case 0: colorePasto = coloreColazione; break;
                    case 1: colorePasto = colorePranzo; break;
                    default: colorePasto = coloreCena; break;
                }
                
                JPanel pastoPanel = createPastoPanel(giorno, tipoPasto, coloreGiorno, colorePasto, ricetteVector);
                pastiPanel.add(pastoPanel);
                indicePasto++;
            }
            
            giornoPanel.add(pastiPanel, BorderLayout.CENTER);
            mainGrid.add(giornoPanel);
        }
        
        // Aggiunge la griglia principale al pannello con padding
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        wrapperPanel.setOpaque(false);
        wrapperPanel.add(mainGrid, BorderLayout.CENTER);
        panel.add(wrapperPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Metodo di utilità per regolare la luminosità di un colore
     * @param color Colore base
     * @param factor Fattore di luminosità (>1 più chiaro, <1 più scuro)
     * @return Colore regolato
     */
    private Color adjustColorBrightness(Color color, float factor) {
        int r = Math.min(255, Math.max(0, Math.round(color.getRed() * factor)));
        int g = Math.min(255, Math.max(0, Math.round(color.getGreen() * factor)));
        int b = Math.min(255, Math.max(0, Math.round(color.getBlue() * factor)));
        return new Color(r, g, b);
    }
    
    private JPanel createPastoPanel(String giorno, TipoPasto tipoPasto, Color bordoColore, Color sfondoColore, Vector<Ricetta> ricetteVector) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(sfondoColore);
        // Shadow effect con bordo sottile
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 0, 0, 25), 1),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        
        // Pannello titolo più elegante
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(sfondoColore);
        titlePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 0, 0, 40)));
        
        // Icone per i diversi pasti
        String iconaPasto = "◯";
        if (tipoPasto == TipoPasto.COLAZIONE) iconaPasto = "☕";
        else if (tipoPasto == TipoPasto.PRANZO) iconaPasto = "🍴";
        else if (tipoPasto == TipoPasto.CENA) iconaPasto = "🍽️";
        
        JLabel labelPasto = new JLabel(iconaPasto + " " + tipoPasto.getNome(), JLabel.LEFT);
        labelPasto.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelPasto.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 0));
        titlePanel.add(labelPasto, BorderLayout.CENTER);
        
        // Etichetta con numero massimo ricette
        JLabel labelMax = new JLabel("max " + tipoPasto.getMaxRicette(), JLabel.RIGHT);
        labelMax.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        labelMax.setForeground(new Color(80, 80, 80));
        labelMax.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 5));
        titlePanel.add(labelMax, BorderLayout.EAST);
        
        panel.add(titlePanel, BorderLayout.NORTH);
        
        // Contenuto principale con sfondo colorato
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setBackground(sfondoColore);
        
        // Lista delle ricette con stile migliorato
        DefaultListModel<Ricetta> modelPasto = modelliGiorni.get(giorno).get(tipoPasto);
        JList<Ricetta> listaPasto = new JList<>(modelPasto);
        listePianificazione.get(giorno).put(tipoPasto, listaPasto);
        
        // Renderer personalizzato per celle più eleganti
        listaPasto.setCellRenderer(new RicettaCellRenderer());
        listaPasto.setFixedCellHeight(22); // Celle leggermente più alte per leggibilità
        listaPasto.setBackground(new Color(255, 255, 255, 230)); // Sfondo bianco semi-trasparente
        listaPasto.setSelectionBackground(adjustColorBrightness(bordoColore, 1.2f));
        listaPasto.setSelectionForeground(Color.BLACK);
        
        // Mostra dettagli ricetta con doppio click
        listaPasto.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    mostraDettagliRicetta(listaPasto.getSelectedValue());
                }
            }
        });
        
        // Scrollpane con stile migliorato
        JScrollPane scrollPasto = new JScrollPane(listaPasto);
        scrollPasto.setPreferredSize(new Dimension(0, 70)); // Altezza ridotta
        scrollPasto.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 20), 1));
        scrollPasto.setBackground(sfondoColore);
        contentPanel.add(scrollPasto, BorderLayout.CENTER);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Pannello controlli più moderno
        JPanel controlPanel = new JPanel(new BorderLayout(0, 0));
        controlPanel.setBackground(sfondoColore);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        
        // ComboBox con stile migliorato
        JComboBox<Ricetta> comboRicette = new JComboBox<>(ricetteVector);
        comboRicette.setRenderer(new RicettaCellRenderer());
        comboRicette.setBackground(Color.WHITE);
        comboRicette.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1));
        
        // Pannello pulsanti più compatto ed elegante
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        buttonPanel.setOpaque(false);
        
        // Stile moderno per i pulsanti
        JButton infoButton = new JButton();
        styleButton(infoButton, UIManager.getIcon("OptionPane.informationIcon"));
        infoButton.setToolTipText("Mostra dettagli ricetta");
        
        JButton aggiungiButton = new JButton();
        styleButton(aggiungiButton, UIManager.getIcon("Tree.leafIcon"));
        aggiungiButton.setToolTipText("Aggiungi ricetta");
        
        JButton rimuoviButton = new JButton();
        styleButton(rimuoviButton, UIManager.getIcon("Tree.closedIcon"));
        rimuoviButton.setToolTipText("Rimuovi ricetta selezionata");
        
        buttonPanel.add(infoButton);
        buttonPanel.add(aggiungiButton);
        buttonPanel.add(rimuoviButton);
        
        JPanel inputPanel = new JPanel(new BorderLayout(2, 0));
        inputPanel.setOpaque(false);
        inputPanel.add(comboRicette, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        
        controlPanel.add(inputPanel, BorderLayout.CENTER);
        
        // Azioni pulsanti con effetto hover
        infoButton.addActionListener(e -> {
            Ricetta ricettaSelezionata = (Ricetta) comboRicette.getSelectedItem();
            if (ricettaSelezionata != null) {
                mostraDettagliRicetta(ricettaSelezionata);
            }
        });
        
        aggiungiButton.addActionListener(e -> {
            Ricetta ricettaSelezionata = (Ricetta) comboRicette.getSelectedItem();
            if (ricettaSelezionata != null) {
                // Controlla se abbiamo raggiunto il limite di ricette per questo pasto
                if (modelPasto.size() < tipoPasto.getMaxRicette()) {
                    modelPasto.addElement(ricettaSelezionata);
                    pianificazione.get(giorno).get(tipoPasto).add(ricettaSelezionata);
                } else {
                    mostraMessaggioModerno(
                        "Hai raggiunto il limite di " + tipoPasto.getMaxRicette() + 
                        " ricette per " + tipoPasto.getNome().toLowerCase() + ".",
                        "Limite Raggiunto", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        
        rimuoviButton.addActionListener(e -> {
            int index = listaPasto.getSelectedIndex();
            if (index != -1) {
                Ricetta ricettaRimossa = modelPasto.remove(index);
                pianificazione.get(giorno).get(tipoPasto).remove(ricettaRimossa);
            } else {
                mostraMessaggioModerno(
                    "Seleziona prima una ricetta da rimuovere", 
                    "Avviso", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Applica stile moderno a un pulsante
     */
    private void styleButton(JButton button, Icon icon) {
        button.setIcon(icon);
        button.setPreferredSize(new Dimension(26, 26));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        button.setBackground(Color.WHITE);
        
        // Effetto hover
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(240, 240, 240));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);
            }
        });
    }
    
    /**
     * Mostra un messaggio di dialogo con stile moderno
     */
    private void mostraMessaggioModerno(String messaggio, String titolo, int tipoMessaggio) {
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.PLAIN, 12));
        UIManager.put("OptionPane.background", new Color(250, 250, 252));
        UIManager.put("Panel.background", new Color(250, 250, 252));
        
        JOptionPane.showMessageDialog(this, messaggio, titolo, tipoMessaggio);
    }
    }
    
    private void mostraDettagliRicetta(Ricetta ricetta) {
        if (ricetta != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Ricetta: ").append(ricetta.getNome()).append("\n\n");
            sb.append("Ingredienti:\n");
            
            double costoTotale = 0.0;
            
            for (Ingrediente ingrediente : ricetta.getIngredienti()) {
                double costoIngrediente = ingrediente.getQuantita() * ingrediente.getPrezzo();
                costoTotale += costoIngrediente;
                sb.append("• ").append(ingrediente.toString());
                if (ingrediente.getPrezzo() > 0) {
                    sb.append(String.format(" (€ %.2f)", costoIngrediente));
                }
                sb.append("\n");
            }
            
            if (costoTotale > 0) {
                sb.append("\nCosto totale: € ").append(String.format("%.2f", costoTotale));
            }
            
            dettagliRicetta.setText(sb.toString());
            pannelloDettagliRicetta.setVisible(true);
        }
    }
    
    // Renderer personalizzato per le celle delle ricette
    private class RicettaCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Ricetta) {
                Ricetta ricetta = (Ricetta) value;
                label.setText(ricetta.getNome());
                label.setIcon(UIManager.getIcon("FileView.fileIcon"));
                label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
                
                // Imposta colore di sfondo se non è selezionato
                if (!isSelected) {
                    label.setBackground(Color.WHITE);
                }
            }
            
            return label;
        }
    }
    
    private JPanel createListaSpesaPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(2, 2, 2, 2),
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200))
                ),
                "Lista della Spesa"
            )
        ));
        panel.setBackground(new Color(250, 250, 252));
        
        // Area di testo con uno stile più moderno
        areaListaSpesa = new JTextArea();
        areaListaSpesa.setEditable(false);
        areaListaSpesa.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        areaListaSpesa.setLineWrap(true);
        areaListaSpesa.setWrapStyleWord(true);
        areaListaSpesa.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        areaListaSpesa.setBackground(Color.WHITE);
        
        JScrollPane scrollLista = new JScrollPane(areaListaSpesa);
        scrollLista.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 20), 1));
        scrollLista.setBackground(new Color(250, 250, 252));
        
        // Pannello inferiore più elegante
        JPanel bottomPanel = new JPanel(new BorderLayout(15, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 5, 5, 5));
        bottomPanel.setBackground(new Color(250, 250, 252));
        
        // Pulsante moderno
        JButton generaButton = new JButton("Genera Lista della Spesa");
        generaButton.setIcon(UIManager.getIcon("FileView.fileIcon"));
        generaButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        generaButton.setFocusPainted(false);
        generaButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        generaButton.setBackground(new Color(240, 240, 240));
        
        generaButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                generaButton.setBackground(new Color(220, 220, 220));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                generaButton.setBackground(new Color(240, 240, 240));
            }
        });
        
        // Stile moderno per il costo totale
        labelCostoTotale = new JLabel("Costo Totale: € 0.00");
        labelCostoTotale.setFont(new Font("Segoe UI", Font.BOLD, 16));
        labelCostoTotale.setForeground(new Color(50, 120, 200)); // Blu moderno
        labelCostoTotale.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(50, 120, 200, 100)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        
        generaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generaListaSpesa();
            }
        });
        
        bottomPanel.add(generaButton, BorderLayout.WEST);
        bottomPanel.add(labelCostoTotale, BorderLayout.EAST);
        
        panel.add(scrollLista, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void generaListaSpesa() {
        Map<String, Ingrediente> listaSpesa = gestore.generaListaSpesa(pianificazione);
        double costoTotale = gestore.calcolaCostoTotale(listaSpesa);
        
        StringBuilder sb = new StringBuilder();
        sb.append("📋 LISTA DELLA SPESA SETTIMANALE\n");
        sb.append("══════════════════════════════\n\n");
        
        // Categorie per raggruppare gli ingredienti
        Map<String, String> categorie = new HashMap<>();
        categorie.put("pomodoro", "🍅 Verdure");
        categorie.put("carota", "🍅 Verdure");
        categorie.put("cipolla", "🍅 Verdure");
        categorie.put("insalata", "🍅 Verdure");
        categorie.put("zucchina", "🍅 Verdure");
        categorie.put("patata", "🍅 Verdure");
        
        categorie.put("pane", "🍞 Panetteria");
        categorie.put("pasta", "🍞 Panetteria");
        categorie.put("pizza", "🍞 Panetteria");
        
        categorie.put("carne", "🥩 Carne");
        categorie.put("pollo", "🥩 Carne");
        categorie.put("manzo", "🥩 Carne");
        categorie.put("maiale", "🥩 Carne");
        
        categorie.put("pesce", "🐟 Pesce");
        categorie.put("tonno", "🐟 Pesce");
        categorie.put("salmone", "🐟 Pesce");
        
        categorie.put("latte", "🥛 Latticini");
        categorie.put("formaggio", "🥛 Latticini");
        categorie.put("yogurt", "🥛 Latticini");
        categorie.put("burro", "🥛 Latticini");
        
        categorie.put("mela", "🍎 Frutta");
        categorie.put("banana", "🍎 Frutta");
        categorie.put("arancia", "🍎 Frutta");
        categorie.put("pera", "🍎 Frutta");
        
        categorie.put("olio", "🧂 Condimenti");
        categorie.put("sale", "🧂 Condimenti");
        categorie.put("pepe", "🧂 Condimenti");
        categorie.put("zucchero", "🧂 Condimenti");
        
        // Raggruppa gli ingredienti per categoria
        Map<String, Map<String, List<Ingrediente>>> ingredientiPerCategoria = new TreeMap<>();
        
        for (Ingrediente ingrediente : listaSpesa.values()) {
            String nomeIngrediente = ingrediente.getNome().toLowerCase();
            String categoria = "🛒 Altri Prodotti";
            
            // Cerca la categoria appropriata
            for (Map.Entry<String, String> entry : categorie.entrySet()) {
                if (nomeIngrediente.contains(entry.getKey())) {
                    categoria = entry.getValue();
                    break;
                }
            }
            
            // Inizializza la mappa se necessario
            if (!ingredientiPerCategoria.containsKey(categoria)) {
                ingredientiPerCategoria.put(categoria, new TreeMap<>());
            }
            
            // Inizializza la lista se necessario
            Map<String, List<Ingrediente>> mappaCategoria = ingredientiPerCategoria.get(categoria);
            if (!mappaCategoria.containsKey(ingrediente.getNome())) {
                mappaCategoria.put(ingrediente.getNome(), new ArrayList<>());
            }
            
            // Aggiungi l'ingrediente alla lista
            mappaCategoria.get(ingrediente.getNome()).add(ingrediente);
        }
        
        // Stampa gli ingredienti raggruppati per categoria con stile moderno
        for (Map.Entry<String, Map<String, List<Ingrediente>>> entryCategoria : ingredientiPerCategoria.entrySet()) {
            sb.append(entryCategoria.getKey()).append("\n");
            sb.append("──────────────────────\n");
            
            for (Map.Entry<String, List<Ingrediente>> entryIngrediente : entryCategoria.getValue().entrySet()) {
                sb.append(entryIngrediente.getKey()).append(":\n");
                
                for (Ingrediente ingrediente : entryIngrediente.getValue()) {
                    double costoIngrediente = ingrediente.getQuantita() * ingrediente.getPrezzo();
                    sb.append(String.format("  • %.2f %s (€ %.2f/unità): € %.2f\n", 
                            ingrediente.getQuantita(), 
                            ingrediente.getUnita(), 
                            ingrediente.getPrezzo(),
                            costoIngrediente));
                }
                sb.append("\n");
            }
        }
        
        sb.append("══════════════════════════════\n");
        sb.append(String.format("💰 COSTO TOTALE: € %.2f", costoTotale));
        
        // Aggiorna l'interfaccia
        areaListaSpesa.setText(sb.toString());
        labelCostoTotale.setText(String.format("Costo Totale: € %.2f", costoTotale));
        
        // Mostra un messaggio di conferma moderno
        mostraMessaggioModerno(
            String.format("Lista della spesa generata con successo!\nCosto totale: € %.2f", costoTotale), 
            "Lista Generata", JOptionPane.INFORMATION_MESSAGE);
    }
}
