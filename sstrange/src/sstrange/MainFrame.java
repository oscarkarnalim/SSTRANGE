package sstrange;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class MainFrame extends JFrame {
	private String helpURL = "https://github.com/oscarkarnalim/sstrange";

	public static String pairTemplatePath = "pair_html_template.html";
	public static String indexTemplatePath = "core_html_template.html";
	public static String javaAdditionalKeywords = "java_keywords.txt";
	public static String pyAdditionalKeywords = "python_keywords.txt";
	public static String additional_dir_path = "strange_html_layout_additional_files";

	private JPanel contentPane;
	private JTextField assignmentPathField;
	private JTextField simThresholdField;
	private JTextField templateDirPathField;
	private JTextField minMatchLengthField;
	private JComboBox submissionTypeField;
	private JComboBox commonCodeField;
	private JComboBox similarityMeasurement;
	private JComboBox progLangField;
	private JComboBox humanLangField;
	private JTextField maxPairsField;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainFrame frame = new MainFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public MainFrame() {
		setIconImage(Toolkit.getDefaultToolkit().getImage("strange_html_layout_additional_files\\icon.png"));
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}

		setResizable(false);
		setTitle("SSTRANGE");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 580, 600);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel lblNewLabel = new JLabel("Assessment path :");
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblNewLabel.setBounds(20, 20, 142, 30);
		contentPane.add(lblNewLabel);

		assignmentPathField = new JTextField();
		assignmentPathField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				fileChooser("Select your assignment root directory", assignmentPathField);
			}
		});
		assignmentPathField.setEditable(false);
		assignmentPathField.setBackground(SystemColor.controlHighlight);
		assignmentPathField.setHorizontalAlignment(SwingConstants.LEFT);
		assignmentPathField.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		assignmentPathField.setBounds(250, 21, 220, 30);
		contentPane.add(assignmentPathField);
		assignmentPathField.setColumns(200);

		JButton btnNewButton = new JButton("...");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fileChooser("Select your assignment root directory", assignmentPathField);
			}
		});
		btnNewButton.setBounds(469, 20, 30, 32);
		contentPane.add(btnNewButton);

		JLabel lblSubmissionType = new JLabel("Submission type :");
		lblSubmissionType.setHorizontalAlignment(SwingConstants.LEFT);
		lblSubmissionType.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblSubmissionType.setBounds(20, 70, 142, 30);
		contentPane.add(lblSubmissionType);

		submissionTypeField = new JComboBox();
		submissionTypeField.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		submissionTypeField.setModel(new DefaultComboBoxModel(
				new String[] { "Single file", "Multiple files in a directory", "Multiple files in a zip" }));
		submissionTypeField.setBounds(250, 70, 249, 30);
		contentPane.add(submissionTypeField);

		progLangField = new JComboBox();
		progLangField.setModel(
				new DefaultComboBoxModel(new String[] { "Java", "Python", "C", "C++", "C#", "Scheme", "Text" }));
		progLangField.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		progLangField.setBounds(250, 120, 249, 30);
		contentPane.add(progLangField);

		JLabel lblProgrammingLanguage = new JLabel("Submission language :");
		lblProgrammingLanguage.setHorizontalAlignment(SwingConstants.LEFT);
		lblProgrammingLanguage.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblProgrammingLanguage.setBounds(20, 120, 142, 30);
		contentPane.add(lblProgrammingLanguage);

		humanLangField = new JComboBox();
		humanLangField.setModel(new DefaultComboBoxModel(new String[] { "English", "Indonesian" }));
		humanLangField.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		humanLangField.setBounds(250, 170, 249, 30);
		contentPane.add(humanLangField);

		JLabel lblHumanLanguage = new JLabel("Explanation language :");
		lblHumanLanguage.setHorizontalAlignment(SwingConstants.LEFT);
		lblHumanLanguage.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblHumanLanguage.setBounds(20, 170, 142, 30);
		contentPane.add(lblHumanLanguage);

		simThresholdField = new JTextField();
		simThresholdField.setText("75");
		simThresholdField.setHorizontalAlignment(SwingConstants.LEFT);
		simThresholdField.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		simThresholdField.setColumns(10);
		simThresholdField.setBounds(250, 221, 56, 30);
		contentPane.add(simThresholdField);

		JLabel lblMinSimThreshold = new JLabel("Minimum similarity threshold :");
		lblMinSimThreshold.setHorizontalAlignment(SwingConstants.LEFT);
		lblMinSimThreshold.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblMinSimThreshold.setBounds(20, 220, 209, 30);
		contentPane.add(lblMinSimThreshold);

		JLabel lblMinSimThreshold_1 = new JLabel("%");
		lblMinSimThreshold_1.setHorizontalAlignment(SwingConstants.LEFT);
		lblMinSimThreshold_1.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblMinSimThreshold_1.setBounds(316, 220, 130, 30);
		contentPane.add(lblMinSimThreshold_1);

		templateDirPathField = new JTextField();
		templateDirPathField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				fileChooser("Select your template code directory", templateDirPathField);
			}
		});
		templateDirPathField.setEditable(false);
		templateDirPathField.setBackground(SystemColor.controlHighlight);
		templateDirPathField.setHorizontalAlignment(SwingConstants.LEFT);
		templateDirPathField.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		templateDirPathField.setColumns(200);
		templateDirPathField.setBounds(250, 370, 220, 30);
		contentPane.add(templateDirPathField);

		JButton btnNewButton_1 = new JButton("...");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fileChooser("Select your template code directory", templateDirPathField);
			}
		});
		btnNewButton_1.setBounds(470, 369, 30, 32);
		contentPane.add(btnNewButton_1);

		JLabel lblTemplateCodePath = new JLabel("Template directory path :");
		lblTemplateCodePath.setHorizontalAlignment(SwingConstants.LEFT);
		lblTemplateCodePath.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblTemplateCodePath.setBounds(20, 370, 178, 30);
		contentPane.add(lblTemplateCodePath);

		minMatchLengthField = new JTextField();
		minMatchLengthField.setText("20");
		minMatchLengthField.setHorizontalAlignment(SwingConstants.LEFT);
		minMatchLengthField.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		minMatchLengthField.setColumns(10);
		minMatchLengthField.setBounds(250, 321, 56, 30);
		contentPane.add(minMatchLengthField);

		JLabel lblMinMatchingLength = new JLabel("Minimum matching length :");
		lblMinMatchingLength.setHorizontalAlignment(SwingConstants.LEFT);
		lblMinMatchingLength.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblMinMatchingLength.setBounds(20, 320, 178, 30);
		contentPane.add(lblMinMatchingLength);

		JLabel lblReportedSimilarities = new JLabel("Similarity measurement :");
		lblReportedSimilarities.setHorizontalAlignment(SwingConstants.LEFT);
		lblReportedSimilarities.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblReportedSimilarities.setBounds(20, 470, 178, 30);
		contentPane.add(lblReportedSimilarities);

		JButton btnNewButton_2 = new JButton("Proceed");
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				submit();
			}
		});
		btnNewButton_2.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		btnNewButton_2.setBounds(20, 520, 120, 30);
		contentPane.add(btnNewButton_2);

		JButton btnNewButton_2_1 = new JButton("Refresh");
		btnNewButton_2_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		btnNewButton_2_1.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		btnNewButton_2_1.setBounds(220, 520, 120, 30);
		contentPane.add(btnNewButton_2_1);

		commonCodeField = new JComboBox();
		commonCodeField.setModel(new DefaultComboBoxModel(new String[] { "Allow", "Disallow" }));
		commonCodeField.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		commonCodeField.setBounds(250, 420, 249, 30);
		contentPane.add(commonCodeField);

		JLabel lblCommonCode = new JLabel("Common content :");
		lblCommonCode.setHorizontalAlignment(SwingConstants.LEFT);
		lblCommonCode.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblCommonCode.setBounds(20, 420, 112, 30);
		contentPane.add(lblCommonCode);

		JButton btnNewButton_2_1_1 = new JButton("Help & About");
		btnNewButton_2_1_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openURL();
			}
		});
		btnNewButton_2_1_1.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		btnNewButton_2_1_1.setBounds(420, 520, 120, 30);
		contentPane.add(btnNewButton_2_1_1);

		JLabel lblMaximumReportedProgram = new JLabel("Maximum reported submission pairs :");
		lblMaximumReportedProgram.setHorizontalAlignment(SwingConstants.LEFT);
		lblMaximumReportedProgram.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblMaximumReportedProgram.setBounds(20, 270, 231, 30);
		contentPane.add(lblMaximumReportedProgram);

		maxPairsField = new JTextField();
		maxPairsField.setText("10");
		maxPairsField.setHorizontalAlignment(SwingConstants.LEFT);
		maxPairsField.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		maxPairsField.setColumns(10);
		maxPairsField.setBounds(250, 270, 56, 30);
		contentPane.add(maxPairsField);

		similarityMeasurement = new JComboBox();
		similarityMeasurement.setModel(
				new DefaultComboBoxModel(new String[] {"MinHash", "Super-Bit", "Jaccard", "Cosine", "RKRGST"}));
		similarityMeasurement.setSelectedIndex(0);
		similarityMeasurement.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		similarityMeasurement.setBounds(250, 470, 249, 30);
		contentPane.add(similarityMeasurement);
	}

	private void fileChooser(String title, JTextField targetTextfield) {
		// open filechooser to select a particular directory
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle(title);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			targetTextfield.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	private void refresh() {
		// refresh all fields
		assignmentPathField.setText("");
		simThresholdField.setText("75");
		maxPairsField.setText("10");
		templateDirPathField.setText("");
		minMatchLengthField.setText("20");
		submissionTypeField.setSelectedIndex(0);
		progLangField.setSelectedIndex(0);
		humanLangField.setSelectedIndex(0);
		commonCodeField.setSelectedIndex(0);
	}

	private void openURL() {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(new URI(helpURL));
			}
		} catch (Exception ee) {
			JOptionPane.showMessageDialog(this, "The URL cannot be opened! Please go to " + helpURL + "!", "Error(s)",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void submit() {
		// to store error message
		String errorMessage = "";

		String assignmentPath = "";
		String path = assignmentPathField.getText();
		if (path.length() == 0)
			// check whether the path field is empty
			errorMessage += "Please choose the assignment path\n";
		else {
			// check whether the path is not valid
			File f = new File(path);
			if (f.exists() == false)
				errorMessage += "The assignment path does not exist\n";
			else
				assignmentPath = path;
		}

		String templateDirPath = "";
		path = templateDirPathField.getText();
		if (path.length() > 0) {
			// check whether the path is not valid
			File f = new File(path);
			if (f.exists() == false)
				errorMessage += "The template directory path does not exist\n";
			else
				templateDirPath = path;
		}

		int simThreshold = 0;
		String text = simThresholdField.getText();
		try {
			simThreshold = Integer.parseInt(text);
			if (simThreshold < 0 || simThreshold > 100)
				// if not in range
				errorMessage += "The similarity threshold should be from 0 to 100 inclusive\n";
		} catch (Exception e) {
			// if not parsed correctly, the input is not integer
			errorMessage += "The similarity threshold should be an integer\n";
		}

		int minMatchLength = 0;
		text = minMatchLengthField.getText();
		try {
			minMatchLength = Integer.parseInt(text);
			if (minMatchLength < 2)
				// if not in range
				errorMessage += "The minimum matching length should not be lower than 2\n";
		} catch (Exception e) {
			// if not parsed correctly, the input is not integer
			errorMessage += "The minimum matching length should be an integer\n";
		}

		int maxPairs = 0;
		text = maxPairsField.getText();
		try {
			maxPairs = Integer.parseInt(text);
			if (maxPairs < 1)
				// if not in range
				errorMessage += "The maximum program pairs reported should not be lower than 1\n";
		} catch (Exception e) {
			// if not parsed correctly, the input is not integer
			errorMessage += "The maximum program pairs reported should be an integer\n";
		}

		String humanLang = "en";
		if (humanLangField.getSelectedIndex() == 1)
			humanLang = "id";

		if (errorMessage.length() > 0) {
			JOptionPane.showMessageDialog(this, errorMessage, "Error(s)", JOptionPane.ERROR_MESSAGE);
		} else {
			// notification to wait
			JOptionPane.showMessageDialog(this, "Please wait till another pop-up appears!");
			// start processing
			String resultPath = process(assignmentPath, submissionTypeField.getSelectedItem().toString(),
					progLangField.getSelectedItem().toString(), humanLang, simThreshold, maxPairs, minMatchLength,
					templateDirPath, commonCodeField.getSelectedIndex() == 0 ? true : false,
					similarityMeasurement.getSelectedItem().toString());

			if (resultPath != null) {
				// set the clipboard
				StringSelection stringSelection = new StringSelection(resultPath);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);

				// notify user that the process has been completed
				JOptionPane.showMessageDialog(this,
						"The comparison process is completed\nThe report link has been copied to the clipboard\nSimply paste it in a web browser");
			}

		}

	}

	public String process(String assignmentPath, String submissionType, String progLang, String humanLang,
			int simThreshold, int maxPairs, int minMatchingLength, String templateDirPath, boolean isCommonCodeAllowed,
			String similarityMeasurement) {

		ArrayList<File> filesToBeDeleted = new ArrayList<File>();

		File assignmentFile = new File(assignmentPath);
		String assignmentParentDirPath = assignmentFile.getParentFile().getAbsolutePath();
		String assignmentName = assignmentFile.getName();
		String additionalKeywordsPath = null;

		// if the programming language is either java or python, set it to STRANGE's
		// format
		if (progLang.equals("Java")) {
			progLang = "java";
			additionalKeywordsPath = "java_keywords.txt";
		} else if (progLang.equals("Python")) {
			progLang = "py";
			additionalKeywordsPath = "python_keywords.txt";
		}

		// if zip, unzip first
		if (submissionType.equals("Multiple files in a zip")) {
			// update the assignment path so that it refers to a new directory with all
			// submissions unzipped
			assignmentPath = ZipManipulator.extractAllZips(assignmentParentDirPath, assignmentName);
			// mark new assignment path to be deleted after the whole process
			filesToBeDeleted.add(new File(assignmentPath));
		}

		boolean isMultipleFiles = false;
		if ((progLang.equals("java") || progLang.equals("py")) && submissionType.startsWith("Multiple files"))
			isMultipleFiles = true;

		// do the comparison
		FastComparer.doSyntacticComparison(assignmentPath, progLang, humanLang, simThreshold, minMatchingLength,
				maxPairs, templateDirPath, similarityMeasurement, assignmentFile, assignmentParentDirPath,
				assignmentName, isMultipleFiles, isCommonCodeAllowed, additionalKeywordsPath, filesToBeDeleted);

		// start deleting all files
		for (File f : filesToBeDeleted)
			FileManipulator.deleteAllTemporaryFiles(f);

		return assignmentParentDirPath + File.separator + "[out] " + assignmentName + File.separator + "index.html";

	}
}
