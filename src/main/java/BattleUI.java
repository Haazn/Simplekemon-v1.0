import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

//Swing presentation. BattleLogic owns all combat rules and Pokémon HP
public final class BattleUI extends JFrame {
    //This class translates button clicks into BattleLogic calls and renders the result.
    private static final boolean HEAL_BETWEEN_STAGES = true; //Set false if you want damage to carry over between gauntlet opponents.

    private static final List<String> ELITE_FOUR = List.of(
            "Lorelei", "Bruno", "Agatha", "Lance"
    );

    private static final List<String> DIALOGUE = List.of(
            "Lorelei: \"I am Lorelei of the Elite Four! No one can best me when it comes to icy Pokémon! "
                    + "Freezing moves are powerful! Your Pokémon will be at my mercy when they are frozen solid!\"",
            "Bruno: \"I am Bruno of the Elite Four! Through rigorous training, people and Pokémon can become "
                    + "stronger without limit! We will grind you down with our superior power! Hoo hah!\"",
            "Agatha: \"I am Agatha of the Elite Four! Oak's taken a lot of interest in you, child! "
                    + "Let's see what you can do, child!\"",
            "Lance: \"I am Lance of the Elite Four! Dragon Pokémon are mythical creatures. "
                    + "They're hard to catch and raise, but their powers are superior!\""
    );

    private final BattleLogic battle;
    private final Database database;
    private final JLabel heading = new JLabel("", SwingConstants.CENTER);
    private final Fighter player = new Fighter("YOUR POKÉMON");
    private final Fighter enemy = new Fighter("OPPONENT");
    private final JButton move1 = new JButton();
    private final JButton move2 = new JButton();
    private final JButton next = new JButton("Next opponent — heal team");
    private final JButton save = new JButton("Retry saving victory");
    private final JPanel teamButtons = new JPanel(new GridLayout(2, 3, 8, 8));
    private final JTextArea log = new JTextArea(7, 60);
    private final JLabel saveStatus = new JLabel("Wins are recorded after all four opponents.");
    private final Map<String, ImageIcon> imageCache = new HashMap<>();
    private boolean saving, saved;

    public BattleUI(BattleLogic battle, Database database) {
        //The UI receives already-created domain and persistence objects instead of creating rules here.
        super("Pokémon Gauntlet");
        this.battle = battle;
        this.database = database;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() { //Returns to selection on close
            @Override public void windowClosing(WindowEvent event) { returnToSelection(); }
        });
        setSize(1160, 760);
        setMinimumSize(new Dimension(980, 700));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(14, 18, 14, 18));
        setContentPane(root);

        heading.setFont(new Font("SansSerif", Font.BOLD, 23));
        root.add(heading, BorderLayout.NORTH);
        JPanel arena = new JPanel(new GridLayout(1, 2, 18, 0));
        arena.add(player);
        arena.add(enemy);
        root.add(arena, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        actions.add(move1);
        actions.add(move2);
        actions.add(next);
        actions.add(save);
        JButton menu = new JButton("Return to selection");
        actions.add(menu);
        bottom.add(actions);
        teamButtons.setBorder(BorderFactory.createTitledBorder("Switch Pokémon — opponent attacks after switching"));
        bottom.add(teamButtons);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createTitledBorder("Battle log"));
        bottom.add(scroll);
        bottom.add(saveStatus);
        root.add(bottom, BorderLayout.SOUTH);

        move1.addActionListener(e -> act(() -> battle.attack(1)));
        move2.addActionListener(e -> act(() -> battle.attack(2)));
        next.addActionListener(e -> act(() -> battle.nextStage(HEAL_BETWEEN_STAGES)));
        save.addActionListener(e -> saveVictory());
        menu.addActionListener(e -> returnToSelection());

        append("Stage 1 begins.\n"
                + getOpponentDialogue(1) + "\n"
                + getOpponentName(1) + " sent out " + battle.getEnemyActive().name + ".\n");
        refresh();
    }

    private String getOpponentName(int stageNumber) {
        int index = stageNumber - 1;
        if (index >= 0 && index < ELITE_FOUR.size()) {
            return ELITE_FOUR.get(index);
        }
        return "Opponent " + stageNumber;
    }

    private String getOpponentDialogue(int stageNumber) {
        int index = stageNumber - 1;
        if (index >= 0 && index < DIALOGUE.size()) {
            return DIALOGUE.get(index);
        }
        return getOpponentName(stageNumber) + ": \"Let's battle!\"";
    }

    private void act(java.util.function.Supplier<String> action) {
        //All move, switch, and next-stage buttons pass through this error-handling boundary.
        try {
            String result = action.get();
            append(result);
            if (battle.getStatus() == BattleLogic.Status.FIGHTING
                    && result.startsWith("Stage " + battle.getStageNumber() + " begins.")) {
                append(getOpponentDialogue(battle.getStageNumber()) + "\n");
            }
            refresh();
            if (battle.getStatus() == BattleLogic.Status.WON && !saved && !saving) saveVictory();
            if (battle.getStatus() == BattleLogic.Status.LOST) {
                JOptionPane.showMessageDialog(this, "Your team was defeated. Choose a new team and try again.");
                returnToSelection();
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Battle", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void refresh() {
        //Re-render controls from the current domain state after every action.
        BattleLogic.Status status = battle.getStatus();
        int stageNum = battle.getStageNumber();
        String opponentName = getOpponentName(stageNum);

        setTitle("Elite Four: " + opponentName);
        heading.setText("OPPONENT  •  " + opponentName.toUpperCase()
                + " (" + stageNum + " / " + battle.getStageCount() + ")  •  "
                + status.toString().replace('_', ' '));
        enemy.setTitle("OPPONENT — " + opponentName.toUpperCase());

        render(player, battle.getPlayerActive(), true);
        render(enemy, battle.getEnemyActive(), false);
        Arceus.PokemonData active = battle.getPlayerActive();
        move1.setText(active.move1Name + " (" + active.move1Power + ")");
        move2.setText(active.move2Name + " (" + active.move2Power + ")");
        move1.setEnabled(status == BattleLogic.Status.FIGHTING);
        move2.setEnabled(status == BattleLogic.Status.FIGHTING);
        next.setVisible(status == BattleLogic.Status.STAGE_WON);
        next.setText(HEAL_BETWEEN_STAGES ? "Next opponent — heal team" : "Next opponent");
        save.setVisible(status == BattleLogic.Status.WON && !saved);
        save.setEnabled(!saving);
        teamButtons.removeAll();
        for (int i = 0; i < battle.getPlayerTeam().size(); i++) {
            Arceus.PokemonData p = battle.getPlayerTeam().get(i);
            JButton button = new JButton(p.name + "  " + p.currenthp + "/" + p.hp);
            button.setEnabled(status == BattleLogic.Status.FIGHTING && p != active && p.currenthp > 0);
            final int target = i;
            button.addActionListener(e -> act(() -> battle.switchPokemon(target)));
            teamButtons.add(button);
        }
        revalidate();
        repaint();
    }

    private void render(Fighter view, Arceus.PokemonData p, boolean back) {
        //Convert model values into HP, stats, names, and the appropriate sprite.
        view.name.setText(p.name + "  •  " + p.type);
        view.hp.setMaximum(p.hp);
        view.hp.setValue(p.currenthp);
        view.hp.setString("HP: " + p.currenthp + " / " + p.hp);
        view.hp.setForeground(p.currenthp > p.hp / 3 ? new Color(40, 155, 80) : Color.RED);
        view.stats.setText("ATK " + p.atk + "     DEF " + p.def + "     SPD " + p.speed + "     IV ×" + p.IV);
        String key = (back ? "back_" : "") + "char" + p.id;
        ImageIcon icon = imageCache.computeIfAbsent(key, k -> {
            ImageIcon loaded = back ? loadImage("back_char" + p.id + ".png") : null;
            if (loaded == null) loaded = loadImage("char" + p.id + ".png");
            return loaded;
        });
        view.sprite.setIcon(icon);
        view.sprite.setText(icon == null ? p.name + " — image unavailable" : "");
    }

    private ImageIcon loadImage(String name) {
        //Support both project-root images during development and classpath resources in a build.
        Path image = Path.of(System.getProperty("pokemon.images", "images"), name);
        ImageIcon icon = null;
        if (Files.isRegularFile(image)) icon = new ImageIcon(image.toString());
        else {
            URL resource = BattleUI.class.getResource("/images/" + name);
            if (resource != null) icon = new ImageIcon(resource);
        }
        if (icon == null || icon.getIconWidth() <= 0) return null;
        double scale = Math.min(240.0 / icon.getIconWidth(), 190.0 / icon.getIconHeight());
        return new ImageIcon(icon.getImage().getScaledInstance(
                Math.max(1, (int) (icon.getIconWidth() * scale)),
                Math.max(1, (int) (icon.getIconHeight() * scale)), Image.SCALE_SMOOTH));
    }

    private void append(String message) {
        log.append(message);
        log.setCaretPosition(log.getDocument().getLength());
    }

    //Network/database work runs away from Swing's UI thread.
    private void saveVictory() {
        //Saving runs in SwingWorker so JDBC work cannot freeze the Swing event-dispatch thread.
        if (saved || saving || battle.getStatus() != BattleLogic.Status.WON) return;
        saving = true;
        saveStatus.setText("Saving your winning team…");
        refresh();
        new SwingWorker<Long, Void>() {
            @Override protected Long doInBackground() throws Exception {
                return database.recordVictory(battle);
            }
            @Override protected void done() {
                //done() returns to the UI thread and presents success, retry, or error feedback.
                saving = false;
                try {
                    long wins = get();
                    saved = true;
                    saveStatus.setText("Victory saved. Total gauntlet wins: " + wins);
                    JOptionPane.showMessageDialog(BattleUI.this,
                            "Gauntlet cleared! Your team was saved. Total wins: " + wins);
                    new HallOfFameUI(BattleUI.this, database, battle.getRunId()).setVisible(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    saveStatus.setText("Save interrupted. Use Retry saving victory.");
                } catch (ExecutionException e) {
                    saveStatus.setText("Victory not confirmed saved. Check MySQL/configuration and retry.");
                    append("Database: " + e.getCause().getMessage() + "\n");
                }
                refresh();
                if (saved) returnToSelection();
            }
        }.execute();
    }

    private void returnToSelection() {
        // Unsaved or active runs require confirmation so the player cannot lose a result accidentally.
        if (saving) {
            JOptionPane.showMessageDialog(this, "Wait for the save attempt to finish.");
            return;
        }
        boolean unfinished = battle.getStatus() == BattleLogic.Status.FIGHTING
                || battle.getStatus() == BattleLogic.Status.STAGE_WON;
        if (unfinished || (battle.getStatus() == BattleLogic.Status.WON && !saved)) {
            String message = unfinished ? "Abandon this run and return to selection?"
                    : "This victory is not confirmed saved. Return without retrying?";
            if (JOptionPane.showConfirmDialog(this, message, "Return to selection",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        }
        new Pokemon().setVisible(true);
        dispose();
    }

    private static final class Fighter extends JPanel {
        final JLabel name = new JLabel("", SwingConstants.CENTER);
        final JLabel sprite = new JLabel("", SwingConstants.CENTER);
        final JProgressBar hp = new JProgressBar();
        final JLabel stats = new JLabel("", SwingConstants.CENTER);

        Fighter(String title) {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createTitledBorder(title));
            name.setFont(new Font("SansSerif", Font.BOLD, 19));
            add(name, BorderLayout.NORTH);
            sprite.setPreferredSize(new Dimension(260, 200));
            add(sprite, BorderLayout.CENTER);
            JPanel info = new JPanel(new GridLayout(2, 1, 4, 4));
            hp.setStringPainted(true);
            info.add(hp);
            info.add(stats);
            add(info, BorderLayout.SOUTH);
        }

        void setTitle(String title) {
            setBorder(BorderFactory.createTitledBorder(title));
        }
    }
}
