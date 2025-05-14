import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DiarioAlimentare {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                configureUIDefaults(); // Estratto in un metodo separato
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Caricamento dati prima di creare la GUI
            GestoreRicette gestore = new GestoreRicette();
            try {
                gestore.caricaRicette("ricettario.txt");
                gestore.caricaPrezzi("volantino.txt");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, 
                    "Errore nel caricamento dei file: " + e.getMessage(), 
                    "Errore", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                // In caso di errore critico, usciamo dall'applicazione
                System.exit(1);
            }
            
            DiarioGUI gui = new DiarioGUI(gestore);
            gui.setVisible(true);
        });
    }
    
    // Configurazione centralizzata dello stile UI
    private static void configureUIDefaults() {
        try {
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Button.borderWidth", 1);
            UIManager.put("Button.innerFocusWidth", 1);
            
            // Cerca Nimbus look and feel
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            
            // Aggiorna i font predefiniti
            Font defaultFont = new Font("Segoe UI", Font.PLAIN, 12);
            UIDefaults defaults = UIManager.getLookAndFeelDefaults();
            String[] components = {"Label", "Button", "TextField", "ComboBox", "List"};
            for (String component : components) {
                defaults.put(component + ".font", defaultFont);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    
    public Ingrediente copia() {
        Ingrediente copia = new Ingrediente(this.nome, this.quantita, this.unita);
        copia.setPrezzo(this.prezzo);
        return copia;
    }
    
    // Calcola costo totale dell'ingrediente
    public double getCostoTotale() {
        return quantita * prezzo;
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
    
    // Calcola il costo totale della ricetta
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
}

/**
 * Classe per gestire le ricette e la lista della spesa
 */
class GestoreRicette {
    private final List<Ricetta> ricettario;
    private final Map<String, Double> prezziIngredienti;
    
    // Costanti per il caricamento dei file
    private static final String DELIMITATORE_CSV = ",";
    private static final String INDICATORE_INGREDIENTE = "-";

    public GestoreRicette() {
        ricettario = new ArrayList<>();
        prezziIngredienti = new HashMap<>();
    }

    public List<Ricetta> getRicettario() {
        return Collections.unmodifiableList(ricettario);
    }

    /**
     * Carica le ricette da un file di testo
     */
    public void caricaRicette(String nomeFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(nomeFile))) {
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
                    line = line.substring(1).trim();
                    String[] parti = line.split(DELIMITATORE_CSV);
                    if (parti.length >= 3) {
                        try {
                            String nomeIngrediente = parti[0].trim();
                            double quantita = Double.parseDouble(parti[1].trim());
                            String unita = parti[2].trim();
                            ricettaCorrente.aggiungiIngrediente(new Ingrediente(nomeIngrediente, quantita, unita));
                        } catch (NumberFormatException e) {
                            System.err.println("Errore di formato nei dati dell'ingrediente: " + line);
                        }
                    }
                }
            }
        }
    }

    /**
     * Carica i prezzi degli ingredienti da un file di testo
     */
    public void caricaPrezzi(String nomeFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(nomeFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parti = line.split(DELIMITATORE_CSV);
                if (parti.length >= 2) {
                    try {
                        String nomeIngrediente = parti[0].trim();
                        double prezzo = Double.parseDouble(parti[1].trim());
                        prezziIngredienti.put(nomeIngrediente, prezzo);
                    } catch (NumberFormatException e) {
                        System.err.println("Errore di formato nel prezzo: " + line);
                    }
                }
            }
        }
        
        // Aggiorna i prezzi negli ingredienti del ricettario
        for (Ricetta ricetta : ricettario) {
            for (Ingrediente ingrediente : ricetta.getIngredienti()) {
                Double prezzo = prezziIngredienti.get(ingrediente.getNome());
                if (prezzo != null) {
                    ingrediente.setPrezzo(prezzo);
                }
            }
        }
    }

    /**
     * Genera una lista della spesa aggregata dalle ricette selezionate
     */
    public Map<String, Ingrediente> generaListaSpesa(Map<String, Map<TipoPasto, List<Ricetta>>> pianificazione) {
        Map<String, Ingrediente> listaSpesa = new HashMap<>();

        pianificazione.values().stream()
            .flatMap(pastiGiorno -> pastiGiorno.values().stream())
            .flatMap(List::stream)
            .flatMap(ricetta -> ricetta.getIngredienti().stream())
            .forEach(ingrediente -> {
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
            });

        return listaSpesa;
    }

    /**
     * Calcola il costo totale della lista della spesa
     */
    public double calcolaCostoTotale(Map<String, Ingrediente> listaSpesa) {
        return listaSpesa.values().stream()
               .mapToDouble(Ingrediente::getCostoTotale)
               .sum();
    }
}

// Classe utility per i componenti UI riutilizzabili
class UIUtils {
    // Colori applicazione
    public static final Color BACKGROUND_COLOR = new Color(250, 250, 252);
    public static final Color BORDER_COLOR = new Color(0, 0, 0, 30);
    public static final Color HIGHLIGHT_COLOR = new Color(50, 120, 200);
    
    // Metodo per creare icone semplici invece di simboli Unicode
    public static Icon createSimpleIcon(String type) {
        return new ImageIcon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (type.equals("info")) {
                    // Icona info: cerchio blu con 'i'
                    g2d.setColor(new Color(70, 130, 200));
                    g2d.fillOval(x, y, 20, 20);
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    g2d.drawString("i", x + 8, y + 15);
                } 
                else if (type.equals("add")) {
                    // Icona aggiungi: cerchio verde con '+'
                    g2d.setColor(new Color(80, 170, 120));
                    g2d.fillOval(x, y, 20, 20);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2.5f));
                    g2d.drawLine(x + 5, y + 10, x + 15, y + 10);  // orizzontale
                    g2d.drawLine(x + 10, y + 5, x + 10, y + 15);  // verticale
                }
                else if (type.equals("remove")) {
                    // Icona rimuovi: cerchio rosso con '-'
                    g2d.setColor(new Color(220, 90, 90));
                    g2d.fillOval(x, y, 20, 20);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2.5f));
                    g2d.drawLine(x + 5, y + 10, x + 15, y + 10);  // orizzontale
                }
                
                g2d.dispose();
            }
            
            @Override
            public int getIconWidth() {
                return 20;
            }
            
            @Override
            public int getIconHeight() {
                return 20;
            }
        };
    }
    
    private UIUtils() {} // Costruttore privato per class utility
    
    /**
     * Regola la luminosità di un colore
     * @param color Colore base
     * @param factor Fattore di luminosità (>1 più chiaro, <1 più scuro)
     * @return Colore regolato
     */
    public static Color adjustColorBrightness(Color color, float factor) {
        int r = Math.min(255, Math.max(0, Math.round(color.getRed() * factor)));
        int g = Math.min(255, Math.max(0, Math.round(color.getGreen() * factor)));
        int b = Math.min(255, Math.max(0, Math.round(color.getBlue() * factor)));
        return new Color(r, g, b);
    }
    
    /**
     * Applica stile moderno a un pulsante con icona, senza sfondo e senza bordi
     */
    public static void styleButton(JButton button, Icon icon) {
        button.setIcon(icon);
        button.setText(null);  // Nessun testo
        button.setPreferredSize(new Dimension(24, 24));
        button.setFocusPainted(false);
        button.setBorderPainted(false);  // Nessun bordo visibile
        button.setContentAreaFilled(false);  // Nessun riempimento
        button.setOpaque(false);  // Trasparente
        button.setMargin(new Insets(0, 0, 0, 0));  // Nessun margine
        
        // Effetto hover
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                // Effetto di scala (ingrandimento) all'hover
                button.setIcon(createHoverIcon(icon));
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(MouseEvent evt) {
                button.setIcon(icon);
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }
    
    /**
     * Crea un'icona leggermente più grande per l'effetto hover
     */
    private static Icon createHoverIcon(Icon originalIcon) {
        return new ImageIcon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Centra l'icona originale ma la disegna leggermente più grande
                originalIcon.paintIcon(c, g2d, x-1, y-1);
                
                g2d.dispose();
            }
            
            @Override
            public int getIconWidth() {
                return originalIcon.getIconWidth() + 2;
            }
            
            @Override
            public int getIconHeight() {
                return originalIcon.getIconHeight() + 2;
            }
        };
    }
    
    /**
     * Mostra un messaggio di dialogo con stile moderno
     */
    public static void mostraMessaggioModerno(Component parent, String messaggio, String titolo, int tipoMessaggio) {
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.PLAIN, 12));
        UIManager.put("OptionPane.background", BACKGROUND_COLOR);
        UIManager.put("Panel.background", BACKGROUND_COLOR);
        
        JOptionPane.showMessageDialog(parent, messaggio, titolo, tipoMessaggio);
    }
    
    /**
     * Crea un bordo composto comune per pannelli
     */
    public static javax.swing.border.Border createPanelBorder(String titolo) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(2, 2, 2, 2),
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200))
                ),
                titolo
            )
        );
    }
}

// Renderer personalizzato per le celle delle ricette nel ComboBox
class RicettaCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, 
                                                 int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        if (value instanceof Ricetta) {
            Ricetta ricetta = (Ricetta) value;
            label.setText(ricetta.getNome());
            
            // Nessuna icona, per assicurarsi che ci sia spazio per il testo
            label.setIcon(null);
            
            // Padding aumentato per maggiore leggibilità
            label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            // Font più grande e chiaro
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            
            // Migliora contrasto e visibilità
            if (!isSelected) {
                label.setBackground(Color.WHITE);
                label.setForeground(new Color(0, 0, 0));
            } else {
                label.setBackground(new Color(70, 130, 180));
                label.setForeground(Color.WHITE);
            }
        }
        
        // Assicurati che il layout del label sia orizzontale
        label.setHorizontalAlignment(JLabel.LEFT);
        label.setHorizontalTextPosition(JLabel.RIGHT);
        
        // Dimensione minima garantita
        label.setPreferredSize(new Dimension(300, 30));
        
        return label;
    }
}

/**
 * Classe per l'interfaccia grafica del diario alimentare
 */
class DiarioGUI extends JFrame {
    private static final String[] GIORNI = {"Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica"};
    private final GestoreRicette gestore;
    private final Map<String, Map<TipoPasto, List<Ricetta>>> pianificazione;
    private final Map<String, Map<TipoPasto, DefaultListModel<Ricetta>>> modelliGiorni;
    private final Map<String, Map<TipoPasto, JList<Ricetta>>> listePianificazione;
    private JTextArea areaListaSpesa;
    private JLabel labelCostoTotale;
    private JPanel pannelloDettagliRicetta;
    private JTextArea dettagliRicetta;
    
    // Cache per le categorie degli ingredienti
    private final Map<String, String> categorieIngredienti;
    
    // Colori per i giorni - Palette più moderna e sofisticata
    private final Color[] coloreGiorni = {
        new Color(79, 143, 230),    // Lunedì - blu
        new Color(94, 168, 230),    // Martedì - azzurro
        new Color(240, 151, 114),   // Mercoledì - pesca
        new Color(248, 177, 87),    // Giovedì - arancione chiaro
        new Color(134, 206, 137),   // Venerdì - verde
        new Color(236, 128, 141),   // Sabato - rosa
        new Color(171, 138, 235)    // Domenica - viola
    };
    
    public DiarioGUI(GestoreRicette gestore) {
        this.gestore = gestore;
        pianificazione = new HashMap<>();
        listePianificazione = new HashMap<>();
        modelliGiorni = new HashMap<>();
        categorieIngredienti = inizializzaCategorieIngredienti();
        
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
        
        // Inizializza i componenti dell'interfaccia
        initComponents();
    }
    
    // Inizializza le categorie degli ingredienti
    private Map<String, String> inizializzaCategorieIngredienti() {
        Map<String, String> categorie = new HashMap<>();
        
        // Verdure
        String[] verdure = {"pomodoro", "carota", "cipolla", "insalata", "zucchina", "patata"};
        for (String v : verdure) {
            categorie.put(v, "🍅 Verdure");
        }
        
        // Panetteria
        String[] panetteria = {"pane", "pasta", "pizza"};
        for (String p : panetteria) {
            categorie.put(p, "🍞 Panetteria");
        }
        
        // Carne
        String[] carni = {"carne", "pollo", "manzo", "maiale"};
        for (String c : carni) {
            categorie.put(c, "🥩 Carne");
        }
        
        // Pesce
        String[] pesci = {"pesce", "tonno", "salmone"};
        for (String p : pesci) {
            categorie.put(p, "🐟 Pesce");
        }
        
        // Latticini
        String[] latticini = {"latte", "formaggio", "yogurt", "burro"};
        for (String l : latticini) {
            categorie.put(l, "🥛 Latticini");
        }
        
        // Frutta
        String[] frutta = {"mela", "banana", "arancia", "pera"};
        for (String f : frutta) {
            categorie.put(f, "🍎 Frutta");
        }
        
        // Condimenti
        String[] condimenti = {"olio", "sale", "pepe", "zucchero"};
        for (String c : condimenti) {
            categorie.put(c, "🧂 Condimenti");
        }
        
        return categorie;
    }
    
    private void initComponents() {
        // Layout principale
        setLayout(new BorderLayout(15, 15));
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        
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
            BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(UIUtils.BACKGROUND_COLOR);
        panel.setPreferredSize(new Dimension(320, 0));
        
        // Titolo del pannello
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(245, 246, 250));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel labelTitolo = new JLabel("Dettagli Ricetta");
        labelTitolo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        labelTitolo.setForeground(new Color(50, 80, 120));
        titlePanel.add(labelTitolo, BorderLayout.CENTER);
        
        panel.add(titlePanel, BorderLayout.NORTH);
        
        // Area testo con stile moderno
        dettagliRicetta = new JTextArea();
        dettagliRicetta.setEditable(false);
        dettagliRicetta.setLineWrap(true);
        dettagliRicetta.setWrapStyleWord(true);
        dettagliRicetta.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dettagliRicetta.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        dettagliRicetta.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(dettagliRicetta);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 20), 1, true));
        scrollPane.setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Pulsante chiudi moderno
        JButton chiudiButton = new JButton("Chiudi");
        chiudiButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chiudiButton.setFocusPainted(false);
        chiudiButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 190, 210), 1, true),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        chiudiButton.setBackground(new Color(250, 250, 255));
        
        chiudiButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                chiudiButton.setBackground(new Color(230, 240, 255));
                chiudiButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(120, 150, 200), 1, true),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
                ));
            }
            
            @Override
            public void mouseExited(MouseEvent evt) {
                chiudiButton.setBackground(new Color(250, 250, 255));
                chiudiButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 190, 210), 1, true),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
                ));
            }
        });
        
        chiudiButton.addActionListener(e -> pannelloDettagliRicetta.setVisible(false));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        buttonPanel.add(chiudiButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createPianificazionePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(UIUtils.createPanelBorder("Pianificazione Settimanale"));
        panel.setBackground(UIUtils.BACKGROUND_COLOR);
        
        // Crea un pannello centrale con layout a griglia di 7 giorni
        JPanel mainGrid = new JPanel(new GridLayout(1, 7, 15, 0));
        mainGrid.setOpaque(false);
        
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
            giornoPanel.setBackground(UIUtils.BACKGROUND_COLOR);
            
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
            
            // Per ogni tipo di pasto
            int indicePasto = 0;
            for (TipoPasto tipoPasto : TipoPasto.values()) {
                // Crea diverse tonalità del colore base
                float fattoreLuminosita;
                switch (indicePasto) {
                    case 0: fattoreLuminosita = 1.3f; break; // Colazione - più chiaro
                    case 1: fattoreLuminosita = 1.1f; break; // Pranzo - medio chiaro
                    default: fattoreLuminosita = 0.9f; break; // Cena - leggermente scuro
                }
                
                Color colorePasto = UIUtils.adjustColorBrightness(coloreGiorno, fattoreLuminosita);
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
    
    private JPanel createPastoPanel(String giorno, TipoPasto tipoPasto, Color bordoColore, Color sfondoColore, Vector<Ricetta> ricette) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(sfondoColore);
        // Shadow effect con bordo sottile e arrotondato
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 0, 0, 25), 1, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // Pannello titolo più elegante
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(sfondoColore);
        titlePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 0, 0, 40)));
        
        // Usa l'icona dall'enum - Senza label se l'icona è vuota
        String labelText = tipoPasto.getNome();
        JLabel labelPasto = new JLabel(labelText, JLabel.LEFT);
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
        listaPasto.setCellRenderer(new RicettaCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                                                     int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                // Garantisce che il testo si adatti al componente
                label.setMinimumSize(new Dimension(150, 25));
                return label;
            }
        });
        listaPasto.setFixedCellHeight(25); // Celle leggermente più alte per leggibilità
        listaPasto.setBackground(new Color(255, 255, 255, 230)); // Sfondo bianco semi-trasparente
        listaPasto.setSelectionBackground(UIUtils.adjustColorBrightness(bordoColore, 1.2f));
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
        
        // Usa direttamente la variabile ricette passata come parametro
        JComboBox<Ricetta> comboRicette = new JComboBox<>(ricette);
        
        // Applica un renderer estremamente semplice
        comboRicette.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                                                         int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if (value instanceof Ricetta) {
                    label.setText(((Ricetta) value).getNome());
                }
                return label;
            }
        });
        
        // Dimensioni aumentate per visualizzare meglio i nomi delle ricette
        comboRicette.setPreferredSize(new Dimension(300, 28));
        comboRicette.setMinimumSize(new Dimension(250, 28));
        comboRicette.setMaximumRowCount(12);
        
        // Configurazione ottimizzata del ComboBox
        comboRicette.setRenderer(new RicettaCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                                                     int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                // Imposta una dimensione preferita più ampia per le celle del dropdown
                if (index >= 0) {
                    label.setPreferredSize(new Dimension(350, 30));
                }
                return label;
            }
        });
        comboRicette.setBackground(Color.WHITE);
        comboRicette.setBorder(BorderFactory.createLineBorder(new Color(180, 190, 210), 1, true));
        comboRicette.setMaximumRowCount(15); // Mostra più elementi nel dropdown
        
        // Pannello pulsanti più elegante - senza sfondo
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);  // Rende il pannello dei pulsanti trasparente
        
        // Stile moderno per i pulsanti con icone personalizzate
        JButton infoButton = new JButton();
        UIUtils.styleButton(infoButton, UIUtils.createSimpleIcon("info"));
        infoButton.setToolTipText("Mostra dettagli ricetta");
        
        JButton aggiungiButton = new JButton();
        UIUtils.styleButton(aggiungiButton, UIUtils.createSimpleIcon("add"));
        aggiungiButton.setToolTipText("Aggiungi ricetta");
        
        JButton rimuoviButton = new JButton();
        UIUtils.styleButton(rimuoviButton, UIUtils.createSimpleIcon("remove"));
        rimuoviButton.setToolTipText("Rimuovi ricetta selezionata");
        
        buttonPanel.add(infoButton);
        buttonPanel.add(aggiungiButton);
        buttonPanel.add(rimuoviButton);
        
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setOpaque(false);
        
        // Assicura che il ComboBox abbia lo spazio necessario
        JPanel comboWrapper = new JPanel(new BorderLayout());
        comboWrapper.setOpaque(false);
        comboWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        comboWrapper.add(comboRicette, BorderLayout.CENTER);
        
        inputPanel.add(comboWrapper, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        
        controlPanel.add(inputPanel, BorderLayout.CENTER);
        
        // Azioni pulsanti
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
            // La riga "generaListaSpesa();" è stata rimossa
        } else {
            UIUtils.mostraMessaggioModerno(
                this,
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
        // La riga "generaListaSpesa();" è stata rimossa
    } else {
        UIUtils.mostraMessaggioModerno(
            this,
            "Seleziona prima una ricetta da rimuovere", 
            "Avviso", JOptionPane.INFORMATION_MESSAGE);
    }
});
        
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void mostraDettagliRicetta(Ricetta ricetta) {
        if (ricetta != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Ricetta: ").append(ricetta.getNome()).append("\n\n");
            sb.append("Ingredienti:\n");
            
            double costoTotale = 0.0;
            
            for (Ingrediente ingrediente : ricetta.getIngredienti()) {
                double costoIngrediente = ingrediente.getCostoTotale();
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
    
    private JPanel createListaSpesaPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(UIUtils.createPanelBorder("Lista della Spesa"));
        panel.setBackground(UIUtils.BACKGROUND_COLOR);
        
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
        scrollLista.setBackground(UIUtils.BACKGROUND_COLOR);
        
        // Pannello inferiore più elegante
        JPanel bottomPanel = new JPanel(new BorderLayout(15, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 5, 5, 5));
        bottomPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        
        // Pulsante moderno e arrotondato con effetto grafico - pulsante senza bordi
        JButton generaButton = new JButton("Genera Lista della Spesa");
        generaButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        generaButton.setFocusPainted(false);
        generaButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(120, 160, 210, 160), 1, true),
            BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        generaButton.setBackground(new Color(230, 240, 255));
        generaButton.setForeground(new Color(50, 90, 160));
        
        // Aggiunge un'icona personalizzata
        generaButton.setIconTextGap(10);
        generaButton.setIcon(new ImageIcon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Disegna un carrello stilizzato
                g2d.setColor(new Color(80, 130, 190));
                int[] xPoints = {x, x+16, x+14, x+4};
                int[] yPoints = {y+16, y+16, y+6, y+6};
                g2d.fillPolygon(xPoints, yPoints, 4);
                
                g2d.fillOval(x+5, y+17, 4, 4);
                g2d.fillOval(x+13, y+17, 4, 4);
                
                g2d.dispose();
            }
            
            @Override
            public int getIconWidth() {
                return 20;
            }
            
            @Override
            public int getIconHeight() {
                return 22;
            }
        });
        
        generaButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                generaButton.setBackground(new Color(180, 210, 250));
                generaButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(80, 130, 190), 1, true),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }
            
            @Override
            public void mouseExited(MouseEvent evt) {
                generaButton.setBackground(new Color(230, 240, 255));
                generaButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(120, 160, 210), 1, true),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }
        });
        
        // Stile moderno per il costo totale
        labelCostoTotale = new JLabel("Costo Totale: € 0.00");
        labelCostoTotale.setFont(new Font("Segoe UI", Font.BOLD, 16));
        labelCostoTotale.setForeground(UIUtils.HIGHLIGHT_COLOR);
        labelCostoTotale.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(50, 120, 200, 100)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        
        generaButton.addActionListener(e -> generaListaSpesa());
        
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
        
        // Raggruppa gli ingredienti per categoria
        Map<String, Map<String, List<Ingrediente>>> ingredientiPerCategoria = 
            raggruppaIngredientiPerCategoria(listaSpesa);
        
        // Stampa gli ingredienti raggruppati per categoria con stile moderno
        for (Map.Entry<String, Map<String, List<Ingrediente>>> entryCategoria : ingredientiPerCategoria.entrySet()) {
            sb.append(entryCategoria.getKey()).append("\n");
            sb.append("──────────────────────\n");
            
            for (Map.Entry<String, List<Ingrediente>> entryIngrediente : entryCategoria.getValue().entrySet()) {
                sb.append(entryIngrediente.getKey()).append(":\n");
                
                for (Ingrediente ingrediente : entryIngrediente.getValue()) {
                    double costoIngrediente = ingrediente.getCostoTotale();
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
        UIUtils.mostraMessaggioModerno(
            this,
            String.format("Lista della spesa generata con successo!\nCosto totale: € %.2f", costoTotale), 
            "Lista Generata", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private Map<String, Map<String, List<Ingrediente>>> raggruppaIngredientiPerCategoria(Map<String, Ingrediente> listaSpesa) {
        Map<String, Map<String, List<Ingrediente>>> ingredientiPerCategoria = new TreeMap<>();
        
        for (Ingrediente ingrediente : listaSpesa.values()) {
            String nomeIngrediente = ingrediente.getNome().toLowerCase();
            String categoria = "🛒 Altri Prodotti";
            
            // Cerca la categoria appropriata
            for (Map.Entry<String, String> entry : categorieIngredienti.entrySet()) {
                if (nomeIngrediente.contains(entry.getKey())) {
                    categoria = entry.getValue();
                    break;
                }
            }
            
            // Inizializza le mappe se necessario usando computeIfAbsent
            Map<String, List<Ingrediente>> mappaCategoria = ingredientiPerCategoria
                .computeIfAbsent(categoria, k -> new TreeMap<>());
            
            List<Ingrediente> listaIngredienti = mappaCategoria
                .computeIfAbsent(ingrediente.getNome(), k -> new ArrayList<>());
            
            // Aggiungi l'ingrediente alla lista
            listaIngredienti.add(ingrediente);
        }
        
        return ingredientiPerCategoria;
    }
}
