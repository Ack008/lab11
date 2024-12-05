package it.unibo.oop.reactivegui03;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Third experiment with reactive gui.
 */
@SuppressWarnings("PMD.AvoidPrintStackTrace")
public final class AnotherConcurrentGUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final double WIDTH_PERC = 0.2;
    private static final double HEIGHT_PERC = 0.1;
    private final JLabel display = new JLabel();
    private final JButton stop = new JButton("stop");
    private final JButton up = new JButton("up");
    private final JButton down = new JButton("down");
    private final Agent agent = new Agent();
    private final PerformActionDelayedAgent stopCounting = 
        new PerformActionDelayedAgent(10 * 1000, AnotherConcurrentGUI.this::stopCounting);

    /**
     * Builds a new CGUI.
     */
    public AnotherConcurrentGUI() {
        super();
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setSize((int) (screenSize.getWidth() * WIDTH_PERC), (int) (screenSize.getHeight() * HEIGHT_PERC));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        final JPanel panel = new JPanel();
        panel.add(display);
        panel.add(down);
        panel.add(up);
        panel.add(stop);
        this.getContentPane().add(panel);
        this.setVisible(true);
        /*
         * Create the counter agent and start it. This is actually not so good:
         * thread management should be left to
         * java.util.concurrent.ExecutorService
         */
        new Thread(agent).start();
        new Thread(stopCounting).start();
        /*
         * Register a listener that stops it
         */
        stop.addActionListener((e) -> this.stopCounting());
        up.addActionListener(e -> agent.changeToCounterIncreasing());
        down.addActionListener(e -> agent.changeToCounterDecreasing());
    }

    private void stopCounting(){
        SwingUtilities.invokeLater(() -> {
            agent.stopCounting(); 
            up.setEnabled(false);
            down.setEnabled(false);
            stop.setEnabled(false);
        });
    }
    private class PerformActionDelayedAgent implements Runnable {
        private final long milliseconds;
        private final Runnable action;
        /**
         * 
         * @param millisecondi milliseconds to wait
         * @param action action to perform after the specified amount of milliseconds
         */
        public PerformActionDelayedAgent(final long milliseconds, final Runnable action){
            this.milliseconds = milliseconds;
            this.action = action;
        }

        @Override
        public void run(){
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            action.run();
        }
    }
    /*
     * The counter agent is implemented as a nested class. This makes it
     * invisible outside and encapsulated.
     */
    private class Agent implements Runnable {
        /*
         * Stop is volatile to ensure visibility. Look at:
         * 
         * http://archive.is/9PU5N - Sections 17.3 and 17.4
         * 
         * For more details on how to use volatile:
         * 
         * http://archive.is/4lsKW
         * 
         */

        private volatile boolean stop;
        private int counter;
        private boolean increasing = true;
        @Override
        public void run() {
            while (!this.stop) {
                try {
                    // The EDT doesn't access `counter` anymore, it doesn't need to be volatile 
                    final var nextText = Integer.toString(this.counter);
                    SwingUtilities.invokeAndWait(() -> AnotherConcurrentGUI.this.display.setText(nextText));
                    counter = increasing? counter + 1 : counter - 1;
                    Thread.sleep(100);
                } catch (InvocationTargetException | InterruptedException ex) {
                    /*
                     * This is just a stack trace print, in a real program there
                     * should be some logging and decent error reporting
                     */
                    ex.printStackTrace();
                }
            }
        }
        /**
         * A method to change the state of the counter to increasing.
         */
        public void changeToCounterIncreasing(){
            changeCounterState(true);
        }
        /**
         * A method to change the state of the counter to increasing.
         */
        public void changeToCounterDecreasing(){
            changeCounterState(false);
        }

        private void changeCounterState(final boolean value) {
            this.increasing = value;
        }
        /**
         * External command to stop counting.
         */
        public void stopCounting() {
            this.stop = true;
        }
    }
}
