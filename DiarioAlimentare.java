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
    public Map<String, Ingrediente> generaListaSpesa(Map<String, List<Ricetta>> pianificazione) {
        Map<String, Ingrediente> listaSpesa = new HashMap<>();

        // Per ogni giorno della settimana
        for (Map.Entry<String, List<Ricetta>> entry : pianificazione.entrySet()) {
            List<Ricetta> ricetteGiorno = entry.getValue();
            
            // Per ogni ricetta del giorno
            for (Ricetta ricetta : ricetteGiorno) {
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
    private Map<String, List<Ricetta>> pianificazione;
    private Map<String, DefaultListModel<Ricetta>> modelliGiorni;
    private Map<String, JList<Ricetta>> listePianificazione;
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
            pianificazione.put(giorno, new ArrayList<>());
            modelliGiorni.put(giorno, new DefaultListModel<>());
        }
        
        // Configurazione della finestra
        setTitle("Diario Alimentare Settimanale");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
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
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        // Layout principale
        setLayout(new BorderLayout(10, 10));
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Pannello per la pianificazione settimanale
        JPanel pianificazionePanel = createPianificazionePanel();
        
        // Pannello per i dettagli della ricetta
        pannelloDettagliRicetta = createPannelloDettagliRicetta();
        pannelloDettagliRicetta.setVisible(false); // Nascosto inizialmente
        
        // Pannello per la lista della spesa
        JPanel listaSpesaPanel = createListaSpesaPanel();
        
        // Layout dei pannelli
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(pianificazionePanel, BorderLayout.CENTER);
        topPanel.add(pannelloDettagliRicetta, BorderLayout.EAST);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, listaSpesaPanel);
        splitPane.setResizeWeight(0.6);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createPannelloDettagliRicetta() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Dettagli Ricetta"));
        panel.setPreferredSize(new Dimension(300, 0));
        
        dettagliRicetta = new JTextArea();
        dettagliRicetta.setEditable(false);
        dettagliRicetta.setLineWrap(true);
        dettagliRicetta.setWrapStyleWord(true);
        dettagliRicetta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JScrollPane scrollPane = new JScrollPane(dettagliRicetta);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JButton chiudiButton = new JButton("Chiudi");
        chiudiButton.addActionListener(e -> pannelloDettagliRicetta.setVisible(false));
        
        panel.add(chiudiButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createPianificazionePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Pianificazione Settimanale"));
        
        // Pannello superiore con intestazione
        JPanel headerPanel = new JPanel(new GridLayout(1, 7, 5, 0));
        
        // Aggiunge le intestazioni dei giorni con colori diversi
        Color[] coloreGiorni = {
            new Color(102, 178, 255),  // Lunedì - azzurro 
            new Color(153, 204, 255),  // Martedì - azzurro chiaro
            new Color(255, 204, 102),  // Mercoledì - arancione chiaro
            new Color(255, 178, 102),  // Giovedì - arancione
            new Color(178, 255, 102),  // Venerdì - verde chiaro
            new Color(255, 153, 153),  // Sabato - rosa
            new Color(204, 153, 255)   // Domenica - viola
        };
        
        for (int i = 0; i < GIORNI.length; i++) {
            JPanel headerGiorno = new JPanel();
            headerGiorno.setBackground(coloreGiorni[i]);
            JLabel labelGiorno = new JLabel(GIORNI[i], JLabel.CENTER);
            labelGiorno.setFont(new Font("SansSerif", Font.BOLD, 14));
            headerGiorno.add(labelGiorno);
            headerPanel.add(headerGiorno);
        }
        
        // Crea panello principale con layout a griglia
        JPanel giorni = new JPanel(new GridLayout(1, 7, 5, 0));
        
        // Crea ComboBox con tutte le ricette
        Vector<Ricetta> ricetteVector = new Vector<>(gestore.getRicettario());
        
        for (int i = 0; i < GIORNI.length; i++) {
            String giorno = GIORNI[i];
            JPanel giornoPanel = new JPanel(new BorderLayout(5, 5));
            giornoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(coloreGiorni[i], 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            
            // Lista delle ricette del giorno con celle più compatte
            DefaultListModel<Ricetta> modelGiorno = modelliGiorni.get(giorno);
            JList<Ricetta> listaGiorno = new JList<>(modelGiorno);
            listePianificazione.put(giorno, listaGiorno);
            
            // Renderer personalizzato per celle più compatte
            listaGiorno.setCellRenderer(new RicettaCellRenderer());
            // Imposta altezza delle celle a 18px invece di default (25px)
            listaGiorno.setFixedCellHeight(18);
            
            listaGiorno.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        mostraDettagliRicetta(listaGiorno.getSelectedValue());
                    }
                }
            });
            
            JScrollPane scrollGiorno = new JScrollPane(listaGiorno);
            // Ridurre l'altezza preferita per la lista
            scrollGiorno.setPreferredSize(new Dimension(0, 160));
            
            // Riga di pulsanti più compatta
            JPanel controlPanel = new JPanel(new BorderLayout(0, 0));
            controlPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            
            JComboBox<Ricetta> comboRicette = new JComboBox<>(ricetteVector);
            comboRicette.setRenderer(new RicettaCellRenderer());
            
            // Pannello di pulsanti più compatto
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            
            JButton infoButton = new JButton();
            infoButton.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
            infoButton.setToolTipText("Mostra dettagli ricetta");
            infoButton.setPreferredSize(new Dimension(24, 24));
            infoButton.addActionListener(e -> {
                Ricetta ricettaSelezionata = (Ricetta) comboRicette.getSelectedItem();
                if (ricettaSelezionata != null) {
                    mostraDettagliRicetta(ricettaSelezionata);
                }
            });
            
            JButton aggiungiButton = new JButton();
            aggiungiButton.setIcon(UIManager.getIcon("Tree.leafIcon"));
            aggiungiButton.setToolTipText("Aggiungi ricetta");
            aggiungiButton.setPreferredSize(new Dimension(24, 24));
            
            JButton rimuoviButton = new JButton();
            rimuoviButton.setIcon(UIManager.getIcon("Tree.closedIcon"));
            rimuoviButton.setToolTipText("Rimuovi ricetta selezionata");
            rimuoviButton.setPreferredSize(new Dimension(24, 24));
            
            buttonPanel.add(infoButton);
            buttonPanel.add(aggiungiButton);
            buttonPanel.add(rimuoviButton);
            
            JPanel inputPanel = new JPanel(new BorderLayout(2, 0));
            inputPanel.add(comboRicette, BorderLayout.CENTER);
            inputPanel.add(buttonPanel, BorderLayout.EAST);
            
            controlPanel.add(inputPanel, BorderLayout.CENTER);
            
            aggiungiButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Ricetta ricettaSelezionata = (Ricetta) comboRicette.getSelectedItem();
                    if (ricettaSelezionata != null) {
                        modelGiorno.addElement(ricettaSelezionata);
                        pianificazione.get(giorno).add(ricettaSelezionata);
                    }
                }
            });
            
            rimuoviButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int index = listaGiorno.getSelectedIndex();
                    if (index != -1) {
                        Ricetta ricettaRimossa = modelGiorno.remove(index);
                        pianificazione.get(giorno).remove(ricettaRimossa);
                    } else {
                        JOptionPane.showMessageDialog(DiarioGUI.this, "Seleziona prima una ricetta da rimuovere", 
                                                     "Avviso", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });
            
            giornoPanel.add(scrollGiorno, BorderLayout.CENTER);
            giornoPanel.add(controlPanel, BorderLayout.SOUTH);
            
            giorni.add(giornoPanel);
        }
        
        // Aggiungi intestazione e pannello principale
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(giorni, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void mostraDettagliRicetta(Ricetta ricetta) {
        if (ricetta != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Ricetta: ").append(ricetta.getNome()).append("\n\n");
            sb.append("Ingredienti:\n");
            for (Ingrediente ingrediente : ricetta.getIngredienti()) {
                sb.append("• ").append(ingrediente.toString()).append("\n");
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
            }
            
            return label;
        }
    }
    
    private JPanel createListaSpesaPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Lista della Spesa"));
        
        // Area di testo con un font più leggibile
        areaListaSpesa = new JTextArea();
        areaListaSpesa.setEditable(false);
        areaListaSpesa.setFont(new Font("SansSerif", Font.PLAIN, 14));
        areaListaSpesa.setLineWrap(true);
        areaListaSpesa.setWrapStyleWord(true);
        
        JScrollPane scrollLista = new JScrollPane(areaListaSpesa);
        scrollLista.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Pannello inferiore più elegante
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
        
        JButton generaButton = new JButton("Genera Lista della Spesa");
        generaButton.setIcon(UIManager.getIcon("FileView.fileIcon"));
        generaButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        // Stile moderno per il costo totale
        labelCostoTotale = new JLabel("Costo Totale: € 0.00");
        labelCostoTotale.setFont(new Font("SansSerif", Font.BOLD, 16));
        labelCostoTotale.setForeground(new Color(0, 102, 204)); // Blu
        labelCostoTotale.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
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
        
        // Stampa gli ingredienti raggruppati per categoria
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
        
        // Mostra un messaggio di conferma
        JOptionPane.showMessageDialog(this, 
                String.format("Lista della spesa generata con successo!\nCosto totale: € %.2f", costoTotale), 
                "Lista Generata", JOptionPane.INFORMATION_MESSAGE);
    }
}