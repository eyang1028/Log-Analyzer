import java.lang.Object; //imported lang

import java.util.*; //imported util

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader; //imported io

import java.awt.*;
import java.awt.event.*;
import java.awt.BorderLayout; //imported awt

import javax.swing.*;
import javax.swing.text.Highlighter;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.BadLocationException;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.SwingConstants; //imported swing

class marker { //separate class for marker
	private int mType;
	private int mIndex;
	private double mTimeStamp;

	marker(int i, double t, int type) { //constructor
		mType = type;
		mIndex = i;
		mTimeStamp = t;
	}

	public int getIndex() { //get index of marker
		return mIndex;
	}

	public double getTimeStamp() { //get timestamp of marker
		return mTimeStamp;
	}

	public int getType() { //get type of marker
		return mType;
	}
}

class LogAnalyzer implements ActionListener { //class with actionlistener
	private int curPos;
	private JTextArea textArea;
	private JScrollPane jScrollTextArea;
	private JFrame jfrm;
	private GridBagConstraints c;
	private JFileChooser chooser;
	private InputStream fstream;
	private JLabel intervalLabel;
	private ArrayList<marker> allMatches;
	private JTextField interval;
	private JButton jbSelectFile, jbSearchErr, jbSearchWrn, jbPrevious, jbNext;

	final static Color HILIT_COLOR = Color.LIGHT_GRAY;
	final static Color ERROR_COLOR = Color.RED;
	final static Color WARNING_COLOR = Color.YELLOW;

	final Highlighter hilitr;

	LogAnalyzer() { //constructor for LogAnalyzer
		curPos = 0;
		allMatches = new ArrayList<marker>(); //array of markers found

		jbSelectFile = new JButton("Open"); //jbuttons here
		jbSearchErr = new JButton("Error");
		jbSearchWrn = new JButton("Warning");
		jbPrevious = new JButton("Previous");
		jbNext = new JButton("Next");
		jbSelectFile.addActionListener(this);
		jbSearchErr.addActionListener(this);
		jbSearchWrn.addActionListener(this);
		jbPrevious.addActionListener(this);
		jbNext.addActionListener(this);

		intervalLabel = new JLabel("Interval from previous marker: "); //jlabel here
		intervalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		interval = new JTextField();
		interval.setEditable(false);

		textArea = new JTextArea(); //jtextarea here
		textArea.setColumns(80);
		textArea.setLineWrap(true);
		textArea.setRows(20);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		hilitr = new DefaultHighlighter();
		textArea.setHighlighter(hilitr);
		jScrollTextArea = new JScrollPane(textArea);

		jfrm = new JFrame("Log Analyzer"); //jframe here
		jfrm.setSize(1200, 800);
		jfrm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Container pane = jfrm.getContentPane(); //container here
		pane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.HORIZONTAL; //code for positioning components
		c.weightx = 0.5;
		c.weighty = -1.0;
		c.gridx = 0;
		c.gridy = 0;
		pane.add(jbSelectFile, c);

		c.gridx = 1;
		pane.add(jbSearchErr, c);

		c.gridx = 2;
		pane.add(jbSearchWrn, c);

		c.gridx = 3;
		pane.add(jbPrevious, c);

		c.gridx = 4;
		pane.add(jbNext, c);

		c.gridwidth = 1;
		c.weighty = 0.0;
		c.gridx = 0;
		c.gridy = 1;
		pane.add(intervalLabel, c);

		c.gridwidth = 4;
		c.gridx = 1;
		pane.add(interval, c);

		c.gridwidth = 5;
		c.ipady = 760;
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 2;
		pane.add(jScrollTextArea, c);

		jfrm.setVisible(true);
	}

	public void search(String s, int type) { //method for searching for errors and warnings
		int index, end, i, j, idxList;
		String ts;
		double t;

		if (s.length() <= 0) //check length of highlighted part
			return;

		Highlighter.HighlightPainter painter;
		if (type == 1)
			painter = new DefaultHighlighter.DefaultHighlightPainter(ERROR_COLOR);
		else if (type == 2)
			painter = new DefaultHighlighter.DefaultHighlightPainter(WARNING_COLOR);
		else
			painter = new DefaultHighlighter.DefaultHighlightPainter(HILIT_COLOR);

		String content = textArea.getText();
		end = index = content.indexOf(s, 0);
		while (index >= 0) {
			try {
				end = index + s.length();
				hilitr.addHighlight(index, end, painter);
				textArea.setCaretPosition(end); //set highlighted length
			} catch (BadLocationException e) {
				e.printStackTrace(); //catch error
			}
			for (i = index - 1; i >= 0; i--) {
				if (content.charAt(i) == '[' && content.charAt(i - 1) == '\n') {
					for (j = i + 1; j < index; j++ ) {
						if (content.charAt(j) == ']') {
							break;
						}
					}
					if (j == index)
						break;
					ts = content.substring(i + 1, j);
					t = Double.valueOf(ts);
					marker oneMatch = new marker(index, t, type);
					if (allMatches.isEmpty())
						allMatches.add(oneMatch);
					else {
						Iterator<marker> itr = allMatches.iterator();
						idxList = 0;
						while (itr.hasNext()) {
							marker temp = itr.next();
							if (oneMatch.getTimeStamp() <= temp.getTimeStamp()) {
								allMatches.add(idxList, oneMatch);
								break;
							} else {
								idxList++;
								if (idxList == allMatches.size()) {
									allMatches.add(oneMatch);
									break;
								}
							}
						}
					}
					break;
				}
			}
			index = content.indexOf(s, end);
		}
	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("Open")) { //open a file when user clicks "Open"
			chooser = new JFileChooser();
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Log Files", "txt", "log");
			chooser.setFileFilter(filter);
			int returnVal = chooser.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				try {
					fstream = new FileInputStream(chooser.getSelectedFile().getName());
					try {
						textArea.read(new InputStreamReader(fstream), null);
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					return;
				}
				allMatches.clear();
				hilitr.removeAllHighlights();
				curPos = 0;
			}
		} else if (ae.getActionCommand().equals("Error")) { //search for an error
			String s = "Err";
			search(s, 1);
			s = "error";
			search(s, 1);
			s = "ERR";
			search(s, 1);
			s = "BUG:";
			search(s, 1);
			s = "Oops";
			search(s, 1);
		} else if (ae.getActionCommand().equals("Warning")) { //search for a warning
			String s = "WARNING";
			search(s, 2);
			s = "warning";
			search(s, 2);
			s = "Warn";
			search(s, 2);
		} else if (ae.getActionCommand().equals("Previous")) { //move to previous highlight
			if (allMatches.isEmpty())
				return;
			if (curPos > 0)
				--curPos;
			marker curMarker = allMatches.get(curPos);
			textArea.setCaretPosition(curMarker.getIndex());
			if (curPos > 0) {
				marker prevMarker = allMatches.get(curPos - 1);
				double delta = curMarker.getTimeStamp() - prevMarker.getTimeStamp();
				interval.setText(String.valueOf(delta));
			} else {
				interval.setText(String.valueOf(curMarker.getTimeStamp()));
			}
		} else if (ae.getActionCommand().equals("Next")) { //move to next highlight
			if (allMatches.isEmpty())
				return;
			if (curPos < allMatches.size() - 1)
				++curPos;
			marker curMarker = allMatches.get(curPos);
			textArea.setCaretPosition(curMarker.getIndex());
			if (curPos > 0) {
				marker prevMarker = allMatches.get(curPos - 1);
				double delta = curMarker.getTimeStamp() - prevMarker.getTimeStamp();
				interval.setText(String.valueOf(delta));
			} else {
				interval.setText(String.valueOf(curMarker.getTimeStamp()));
			}
		}
	}

	public static void main(String args[]) { //main method down here
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new LogAnalyzer();
			}
		});
	}
}
