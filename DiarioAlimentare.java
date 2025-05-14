import javax.swing.*;
import java.awt.Font;
import java.io.IOException;

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
