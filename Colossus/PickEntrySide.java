import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class PickEntrySide allows picking which side of a MasterBoard hex
 * to enter.
 * @version $Id$
 * @author David Ripton
 */

public final class PickEntrySide extends HexMap implements ActionListener,
    WindowListener
{
    private static JButton button5;  // left
    private static JButton button3;  // bottom
    private static JButton button1;  // right
    private static boolean laidOut;
    private JDialog dialog;
    private static int entrySide;


    private PickEntrySide(JFrame parentFrame, String masterHexLabel,
        boolean left, boolean bottom, boolean right)
    {
        super(masterHexLabel);
        dialog = new JDialog(parentFrame, "Pick entry side", true);
        laidOut = false;
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(null);

        if (left)
        {
            button5 = new JButton("Left");
            button5.setMnemonic(KeyEvent.VK_L);
            contentPane.add(button5);
            button5.addActionListener(this);
        }

        if (bottom)
        {
            button3 = new JButton("Bottom");
            button3.setMnemonic(KeyEvent.VK_B);
            contentPane.add(button3);
            button3.addActionListener(this);
        }

        if (right)
        {
            button1 = new JButton("Right");
            button1.setMnemonic(KeyEvent.VK_R);
            contentPane.add(button1);
            button1.addActionListener(this);
        }

        dialog.addWindowListener(this);

        setSize(getPreferredSize());
        contentPane.add(this);
        dialog.pack();

        dialog.setSize(getPreferredSize());
        dialog.setResizable(false);
        dialog.setBackground(Color.white);
        dialog.setVisible(true);
    }


    public static int pickEntrySide(JFrame parentFrame, String masterHexLabel,
        boolean left, boolean bottom, boolean right)
    {
        entrySide = -1;
        new PickEntrySide(parentFrame, masterHexLabel, left, bottom, right);
        return entrySide;
    }


    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Abort if called too early.
        Rectangle rectClip = g.getClipBounds();
        if (rectClip == null)
        {
            return;
        }

        int scale = 2 * Scale.get();
        Dimension d = getSize();

        if (!laidOut)
        {
            int cx = 6 * scale;
            int cy = 3 * scale;

            if (button5 != null)
            {
                button5.setBounds(cx + 1 * scale, cy + 1 * scale,
                    d.width / 7, d.height / 16);
            }
            if (button3 != null)
            {
                button3.setBounds(cx + 1 * scale, cy + 21 * scale,
                    d.width / 7, d.height / 16);
            }
            if (button1 != null)
            {
                button1.setBounds(cx + 19 * scale, cy + 11 * scale,
                    d.width / 7, d.height / 16);
            }

            laidOut = true;
        }

        if (button1 != null)
        {
            button1.repaint();
        }
        if (button3 != null)
        {
            button3.repaint();
        }
        if (button5 != null)
        {
            button5.repaint();
        }
    }


    // Set hex's entry side to side, and then exit the dialog.  If side
    // is -1, then do not set an entry side, which will abort the move.
    private void cleanup(int side)
    {
        entrySide = side;
        dialog.dispose();
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Left"))
        {
            cleanup(5);
        }

        else if (e.getActionCommand().equals("Right"))
        {
            cleanup(1);
        }

        else if (e.getActionCommand().equals("Bottom"))
        {
            cleanup(3);
        }
    }


    public void windowClosing(WindowEvent e)
    {
        // Abort the move.
        cleanup(-1);
    }
}
