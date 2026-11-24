import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

//Simplified, scrollable Hall of Fame view
public final class HallOfFameUI extends JDialog {
    //This dialog reads saved victories and keeps database work off the Swing UI thread
    private static final int LIMIT = 50;
    private static final int SPRITE_SIZE = 96;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm");

    private final Database database;
    private final String highlightedRun;
    private final JPanel entries = new JPanel();
    private final JLabel totalLabel = new JLabel("Total victories: —", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel("Loading victories…");
    private final JButton refreshButton = new JButton("Refresh");
    private final Map<Integer, ImageIcon> spriteCache = new HashMap<>(); //Map for grabbing ImageIcons
    private SwingWorker<PageData, Void> loader;

    private record PageData(List<Database.HallOfFameEntry> victories, long wins) {}

    public HallOfFameUI(Window owner, Database database, String highlightedRun) {
        //highlightedRun identifies the victory that was just saved, if this dialog followed a win
        super(owner, "Hall of Fame", ModalityType.APPLICATION_MODAL); //Prevents user interaction from other windows open
        this.database = database;
        this.highlightedRun = highlightedRun;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1020, 680);
        setMinimumSize(new Dimension(860, 540));
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(16, 18, 16, 18));
        setContentPane(root);

        //HEADER
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("HALL OF FAME", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(Color.ORANGE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel(
                highlightedRun == null
                        ? "Teams that defeated all four Elite Four opponents."
                        : "Congratulations! Your winning team has entered the Hall of Fame.",
                SwingConstants.CENTER
        );
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(6));
        header.add(totalLabel);
        root.add(header, BorderLayout.NORTH);

        //Scrollable list of victory cards
        entries.setLayout(new BoxLayout(entries, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(entries);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        root.add(scrollPane, BorderLayout.CENTER);

        //FOOTER
        JPanel footer = new JPanel(new BorderLayout(6, 6));
        footer.add(statusLabel, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton closeButton = new JButton("Back");
        refreshButton.addActionListener(e -> loadVictories());
        closeButton.addActionListener(e -> dispose());
        buttons.add(refreshButton);
        buttons.add(closeButton);
        footer.add(buttons, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() { //Listener for window events, makes it so if the window is closed, cancel all loading to save memory/CPU usage 
            @Override
            public void windowClosed(WindowEvent event) {
                if (loader != null) loader.cancel(true);
            }
        });

        loadVictories();
    }

    private void loadVictories() {
        //Refresh clears the current cards and starts a new background query
        refreshButton.setEnabled(false);
        statusLabel.setText("Loading victories…");
        entries.removeAll();
        entries.revalidate();
        entries.repaint();

        loader = new SwingWorker<>() {
            @Override
            protected PageData doInBackground() throws Exception {
                return new PageData(database.getHallOfFame(0, LIMIT), database.getWinCount());
            }

            @Override
            protected void done() {
                //Only the UI thread updates labels, cards, and retry status after the query completes
                if (!isDisplayable() || isCancelled()) return;
                refreshButton.setEnabled(true);
                try {
                    PageData data = get();
                    totalLabel.setText("Total gauntlet victories: " + data.wins());

                    if (data.victories().isEmpty()) {
                        JLabel empty = new JLabel("No victories yet. Beat the gauntlet to get on the board!");
                        empty.setBorder(new EmptyBorder(12, 12, 12, 12));
                        entries.add(empty);
                        statusLabel.setText("0 victories recorded.");
                    } else {
                        for (Database.HallOfFameEntry entry : data.victories()) {
                            entries.add(createVictoryCard(entry));
                            entries.add(Box.createVerticalStrut(10));
                        }
                        statusLabel.setText("Showing " + data.victories().size() + " recent victory runs.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Loading interrupted.");
                } catch (CancellationException ignored) {
                    return;
                } catch (ExecutionException e) {
                    totalLabel.setText("Total gauntlet victories: unavailable");
                    statusLabel.setText("Could not load Hall of Fame. Check database connection and click Refresh.");
                    JTextArea err = new JTextArea(String.valueOf(e.getCause().getMessage()));
                    err.setEditable(false);
                    err.setLineWrap(true);
                    err.setWrapStyleWord(true);
                    entries.add(err);
                }
                entries.revalidate();
                entries.repaint();
            }
        };
        loader.execute();
    }

    private JPanel createVictoryCard(Database.HallOfFameEntry victory) {
        //One card represents one database victory and displays its six saved members in slot order
        boolean isHighlighted = victory.runId().equals(highlightedRun);
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        isHighlighted ? new Color(200, 140, 20) : Color.LIGHT_GRAY,
                        isHighlighted ? 3 : 1
                ),
                new EmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 195));

        String prefix = isHighlighted ? "NEW CHAMPION  •  " : "CHAMPIONS  •  ";
        JLabel title = new JLabel(prefix + DATE.format(victory.wonAt()));
        title.setFont(new Font("Arial", Font.BOLD, 14));
        card.add(title, BorderLayout.NORTH);

        JPanel teamRow = new JPanel(new GridLayout(1, 6, 8, 0));
        teamRow.setOpaque(false);

        for (Database.TeamMember member : victory.team()) {
            JPanel slot = new JPanel(new BorderLayout(4, 4));
            slot.setOpaque(false);

            JLabel sprite = new JLabel("", SwingConstants.CENTER);
            Dimension boxSize = new Dimension(SPRITE_SIZE, SPRITE_SIZE);
            sprite.setPreferredSize(boxSize);
            sprite.setMinimumSize(boxSize);
            sprite.setMaximumSize(boxSize);

            ImageIcon icon = spriteCache.computeIfAbsent(member.pokemonId(), this::loadFixedSprite);
            if (icon != null) {
                sprite.setIcon(icon);
            } else {
                sprite.setText("#" + member.pokemonId());
            }

            JLabel label = new JLabel(member.slot() + ". " + member.name(), SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 12));

            slot.add(sprite, BorderLayout.CENTER);
            slot.add(label, BorderLayout.SOUTH);
            teamRow.add(slot);
        }

        card.add(teamRow, BorderLayout.CENTER);
        return card;
    }

    private ImageIcon loadFixedSprite(int id) {
        //Hall of Fame sprites use the saved Pokémon ID, with a classpath fallback for packaged builds.
        String fileName = "char" + id + ".png";
        Path localPath = Path.of(System.getProperty("pokemon.images", "images"), fileName);
        ImageIcon raw = null;

        if (Files.isRegularFile(localPath)) {
            raw = new ImageIcon(localPath.toString());
        } else {
            URL resource = HallOfFameUI.class.getResource("/images/" + fileName);
            if (resource != null) {
                raw = new ImageIcon(resource);
            }
        }

        if (raw == null || raw.getIconWidth() <= 0) {
            return null;
        }

        Image scaled = raw.getImage().getScaledInstance(SPRITE_SIZE, SPRITE_SIZE, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
