import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.net.URL;
import java.util.Random;

public class Pokemon extends JFrame {
    //This window is the application entry point and owns the team selection logic and UI.
    private final java.util.List<Integer> userTeam = new java.util.ArrayList<>();
    public static Random rng = new Random(); //for random number rolls
    private static final int SLOTS = 12;
    
    private int addedCount = 0;
    private JLabel titleLabel;
    private JLabel subLabel;
    private JButton readyButton;
    private SlotPanel[] slots;

    public Pokemon() {
        //Builds the selection window
        setTitle("Simplekémon");
        setSize(1280, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        //HEADER PANEL
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        //TITLE
        titleLabel = new JLabel("TEAM SELECTION", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.RED);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        //SUBTITLE
        subLabel = new JLabel("(Order matters)", JLabel.CENTER);
        subLabel.setFont(new Font("Arial", Font.BOLD, 12));
        subLabel.setForeground(Color.RED);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(2)); // Small gap between title and subtitle
        headerPanel.add(subLabel);

        add(headerPanel, BorderLayout.NORTH);

        //GRID(SLOTS) 3X4
        JPanel gridPanel = new JPanel(new GridLayout(3, 4, 15, 15));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        slots = new SlotPanel[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            slots[i] = new SlotPanel(i + 1);
            gridPanel.add(slots[i]);
        }
        add(gridPanel, BorderLayout.CENTER);

        //READY BUTTON
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        readyButton = new JButton("READY (0 / 6)");
        readyButton.setFont(new Font("Arial", Font.BOLD, 16));
        readyButton.setBackground(Color.ORANGE);
        readyButton.setForeground(Color.BLACK);
        readyButton.setFocusPainted(false);
        readyButton.setEnabled(false);
        readyButton.setPreferredSize(new Dimension(160, 45));
        
        readyButton.addActionListener(e -> {
            if (userTeam.size() != 6) return; //Checks that userTeam contains exactly 6 Pokémon IDs
            java.util.List<Arceus.PokemonData> catalog = Arceus.create(); //Creates a new Arceus.PokemonData list called catalog
            java.util.List<Arceus.PokemonData> team = new java.util.ArrayList<>();//Creates a new Arceus.PokemonData list called team
            for (int id : userTeam) { //For every id of userTeam, VVV
                team.add(catalog.stream().filter(p -> p.id == id)//Search in catalog(Arceus Pokemon List) for the same ID, then add that pokemon to team
                        .findFirst().orElseThrow());//Grab first match, or throw exception if there is no matching ID
            }
            BattleLogic logic = new BattleLogic(team, ArceusEnemy.createGauntlet());//Pass team and createGauntlet() to logic 
            new BattleUI(logic, new Database()).setVisible(true);//Go to next scene(BattleUI) passing logic and new instance of database
            dispose();
        });
        
        bottomPanel.add(readyButton);
        
        //HALL OF FAME BUTTON
        JButton hallOfFameButton = new JButton("Hall of Fame");
        hallOfFameButton.addActionListener(e ->
                new HallOfFameUI(this, new Database(), null).setVisible(true));
        
        bottomPanel.add(hallOfFameButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    //PANEL
    private class SlotPanel extends JPanel {
        // Each card owns its Add/Remove state while the outer window owns the selection count.
        private int slotId;
        private boolean isAdded = false;
        private JLabel imageLabel;
        private JLabel descLabel;
        private JButton addButton;
        private JButton removeButton;
        private final String[] characterDescriptions = {
            "Venusaur<br><br>A Grass/Poison type Pokémon. The plant absorbs solar energy. It stays on the move to seek sunlight.", //char1
            "Charizard<br><br>A Fire/Flying type Pokémon. Spits fire that is hot enough to melt boulders. Causes forest fires unintentionally.", //char2
            "Blastoise<br><br>A Water type Pokémon. Brutal with pressurized water jets on its shell. They are used for high speed tackles.", //char3
            "Gyarados<br><br>A Water/Flying type Pokémon. Huge and vicious, it is capable of destroying entire cities in a rage.", //char4
            "Snorlax<br><br>A Normal type Pokémon. Very lazy. Just eats and sleeps. As its rotund bulk builds, it becomes steadily more slothful.", //char5
            "Dragonite<br><br>A Dragon/Flying type Pokémon. An extremely rare POKéMON. Its intelligence is said to match that of humans.", //char6
            "Gengar<br><br>A Ghost/Poison type Pokémon. Under a full moon, it likes to mimic the shadows of people and laugh at their fright.", //char7
            "Machamp<br><br>A Fighting type Pokémon. it throws very powerful punches that can send the victim clear over the horizon.", //char8
            "Raichu<br><br>An Electric type Pokémon. Its long tail serves as a ground to protect itself from its own high voltage power.", //char9
            "Golem<br><br>A Rock/Ground type Pokémon. Its boulder-like body is extremely hard. It can easily withstand blasts without damage.", //char10
            "Lapras<br><br>A Water/Ice type Pokémon. It that has been overhunted almost to extinction. It can ferry people across the water.", //char11
            "Jynx<br><br>An Ice/Psychic type Pokémon. It wiggles its hips as it walks. It can cause people to dance in unison with it."//char12
        };

        public SlotPanel(int id) {
            this.slotId = id;
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));
            
            setBackground(Color.WHITE);

            //IMAGE
            imageLabel = new JLabel("", JLabel.CENTER);
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            imageLabel.setVerticalAlignment(JLabel.CENTER);
            imageLabel.setPreferredSize(new Dimension(128, 128));
            imageLabel.setBackground(new Color(235, 235, 235));
            imageLabel.setOpaque(true);
            imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));

            try {
                String imagePath = "images/char" + slotId + ".png";
                ImageIcon icon = new ImageIcon(imagePath);

                if (icon.getIconWidth() != -1) {
                    Image scaledImage = icon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaledImage));
                    imageLabel.setText(null);
                } else {
                    imageLabel.setText("IMG " + slotId);
                    imageLabel.setFont(new Font("Arial", Font.BOLD, 11));
                }
            } catch (Exception ex) {
                imageLabel.setText("IMG " + slotId);
                imageLabel.setFont(new Font("Arial", Font.BOLD, 11));
            }
            add(imageLabel, BorderLayout.WEST);
            

            //RIGHT PANEL FOR DESCRIPTION AND BUTTONS
            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
            rightPanel.setOpaque(false);

            //DESCRIPTION
            String descriptionText = "<html>" + characterDescriptions[slotId - 1] + "</html>";
            descLabel = new JLabel(descriptionText);
            descLabel.setFont(new Font("Arial", Font.BOLD, 10));
            descLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 2),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)
            ));
            descLabel.setBackground(new Color(235, 235, 235));
            descLabel.setOpaque(true);

            rightPanel.add(descLabel);
            rightPanel.add(Box.createVerticalStrut(6));

            //ADD AND REMOVE BUTTONS
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            buttonPanel.setOpaque(false);
            buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            addButton = new JButton("Add");
            addButton.setForeground(Color.BLUE);
            addButton.setMargin(new Insets(2, 6, 2, 6));
            removeButton = new JButton("Remove");
            removeButton.setForeground(Color.BLUE);
            removeButton.setEnabled(false);
            removeButton.setMargin(new Insets(2, 6, 2, 6));

            buttonPanel.add(addButton);
            buttonPanel.add(removeButton);
            rightPanel.add(buttonPanel);

            add(rightPanel, BorderLayout.CENTER);

            addButton.addActionListener(e -> {
                // Adding records the click order, which becomes the battle order.
                if (!isAdded && addedCount < 6) {
                    isAdded = true;
                    addedCount++;
                    userTeam.add(slotId);
                    imageLabel.setBackground(Color.cyan);
                    addButton.setEnabled(false);
                    removeButton.setEnabled(true);
                    updateCount();
                }
            });

            removeButton.addActionListener(e -> {
                // Removing an item restores that card to an available state.
                if (isAdded) {
                    isAdded = false;
                    addedCount--;
                    userTeam.remove(Integer.valueOf(slotId));
                    imageLabel.setBackground(new Color(235, 235, 235));
                    addButton.setEnabled(true);
                    removeButton.setEnabled(false);
                    updateCount();
                }
            });
        }
    }

    //READY BUTTON
    private void updateCount() {
        // The Ready button is enabled only when the business rule of six is satisfied.
        readyButton.setEnabled(false);
        readyButton.setText("READY (" + addedCount + " / 6)");
        if (addedCount == 6)
        {
            readyButton.setEnabled(true);
        }
    }

    public static void main(String[] args) {
        // Swing components must be created on the event-dispatch thread.
        SwingUtilities.invokeLater(() -> {
            new Pokemon().setVisible(true);
        });
    }
}
