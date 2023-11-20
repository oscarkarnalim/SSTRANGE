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
import java.io.FileOutputStream;
import java.io.PrintStream;
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

import sstrange.evaluation.WebEvalControlled;
import sstrange.evaluation.WebEvalStdAsmt;
import sstrange.matchgenerator.ComparisonPairTuple;

public class MainFrame extends JFrame {
	private String helpURL = "https://github.com/oscarkarnalim/sstrange";

	public static String pairTemplatePath = "pair_html_template.html";
	public static String pairWebTemplatePath = "pair_html_template_web.html";
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
	private JButton proceedBtn;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) throws Exception {
		// if args has arguments, execute as a library
		if (args.length > 0) {
			// just for code tracing in estrange
			String prefixPath = "mcu" + File.separator;
			File file = new File(prefixPath + "err_sstrange.txt");
			FileOutputStream fos = new FileOutputStream(file, true);
			PrintStream ps = new PrintStream(fos);
			System.setErr(ps);

			executeConsole(args);
			ps.close();
		} else {
			// if empty, show the UI
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
		progLangField.setModel(new DefaultComboBoxModel(
				new String[] { "Java", "Python", "C#", "Dart", "Web (HTML, JS, CSS and PHP)" }));
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

		JLabel lblMinSimThreshold = new JLabel("Min. similarity threshold :");
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

		JLabel lblMinMatchingLength = new JLabel("Min. matching length :");
		lblMinMatchingLength.setHorizontalAlignment(SwingConstants.LEFT);
		lblMinMatchingLength.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblMinMatchingLength.setBounds(20, 320, 178, 30);
		contentPane.add(lblMinMatchingLength);

		JLabel lblReportedSimilarities = new JLabel("Similarity measurement :");
		lblReportedSimilarities.setHorizontalAlignment(SwingConstants.LEFT);
		lblReportedSimilarities.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblReportedSimilarities.setBounds(20, 470, 178, 30);
		contentPane.add(lblReportedSimilarities);

		proceedBtn = new JButton("Proceed");
		proceedBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				submit();
			}
		});
		proceedBtn.setFont(new Font("Times New Roman", Font.PLAIN, 14));
		proceedBtn.setBounds(20, 520, 120, 30);
		contentPane.add(proceedBtn);

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
		lblCommonCode.setBounds(20, 420, 120, 30);
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

		JLabel lblMaximumReportedProgram = new JLabel("Max. reported submission pairs :");
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
		similarityMeasurement.setModel(new DefaultComboBoxModel(
				new String[] { "MinHash", "Super-Bit", "Jaccard", "Cosine", "RKRGST", "Sensitive MinHash",
						"Sensitive Super-Bit", "Sensitive Jaccard", "Sensitive Cosine", "Sensitive RKRGST" }));
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

		// if not java and python but use template code, display an error message
		if (templateDirPath.length() > 0 && !(progLangField.getSelectedItem().toString().equals("Java")
				|| progLangField.getSelectedItem().toString().equals("Python")))
			errorMessage += "The template code removal currently works on Java or Python only\n";

		if (commonCodeField.getSelectedIndex() == 1 && !(progLangField.getSelectedItem().toString().equals("Java")
				|| progLangField.getSelectedItem().toString().equals("Python")))
			errorMessage += "The common code removal currently works on Java or Python only\n";

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
			// only used for lsh algorithms
			int numClusters = 2;
			int numStages = 1;

			String similarityType = similarityMeasurement.getSelectedItem().toString();
			if (similarityType.equals("MinHash") || similarityType.equals("Super-Bit")) {
				// set num of cluster
				Object[] options = new Object[] { "2", "3", "4", "5", "6", "7", "8", "9", "10" };
				Object selectedCluster = JOptionPane.showInputDialog(this, "Num of clusters:", "Num of clusters",
						JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
				if (selectedCluster == null)
					return;
				numClusters = Integer.parseInt(selectedCluster.toString());

				// set num of stages
				options = new Object[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" };
				Object selectedStages = JOptionPane.showInputDialog(this, "Num of stages:", "Num of stages",
						JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
				if (selectedStages == null)
					return;
				numStages = Integer.parseInt(selectedStages.toString());
			}

			// notification to wait
			JOptionPane.showMessageDialog(this, "Please wait till another pop-up appears!");
			// disable proceed button
			proceedBtn.setEnabled(false);
			proceedBtn.setText("Processing...");
			revalidate();
			repaint();

			String rawSimilarityMeasurementType = similarityMeasurement.getSelectedItem().toString();
			String similarityMeasurementType = "";
			if (rawSimilarityMeasurementType.toLowerCase().endsWith("MinHash".toLowerCase()))
				similarityMeasurementType = "MinHash";
			else if (rawSimilarityMeasurementType.toLowerCase().endsWith("Super-Bit".toLowerCase()))
				similarityMeasurementType = "Super-Bit";
			else if (rawSimilarityMeasurementType.toLowerCase().endsWith("Jaccard".toLowerCase()))
				similarityMeasurementType = "Jaccard";
			else if (rawSimilarityMeasurementType.toLowerCase().endsWith("Cosine".toLowerCase()))
				similarityMeasurementType = "Cosine";
			else if (rawSimilarityMeasurementType.toLowerCase().endsWith("RKRGST".toLowerCase()))
				similarityMeasurementType = "RKRGST";

			boolean isSensitive = false;
			if (rawSimilarityMeasurementType.toLowerCase().contains("sensitive"))
				isSensitive = true;

			// start processing
			String resultPath = process(assignmentPath, submissionTypeField.getSelectedItem().toString(),
					progLangField.getSelectedItem().toString(), humanLang, simThreshold, maxPairs, minMatchLength,
					templateDirPath, commonCodeField.getSelectedIndex() == 0 ? true : false, similarityMeasurementType,
					numClusters, numStages, isSensitive);

			if (resultPath != null) {
				// set the clipboard
				StringSelection stringSelection = new StringSelection(resultPath);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);

				// notify user that the process has been completed
				JOptionPane.showMessageDialog(this,
						"The comparison process is completed\nThe report link has been copied to the clipboard\nSimply paste it in a web browser");
			} else {
				// notify user that errors occurred
				JOptionPane.showMessageDialog(this,
						"Error(s) occurred; kindly check the parameters and the input files");
			}

			// enable proceed button
			proceedBtn.setEnabled(true);
			proceedBtn.setText("Proceed");
			revalidate();
			repaint();
		}

	}

	private static void executeConsole(String[] args) {

		// arguments should be 10 or 12
		if (args.length != 11 && args.length != 13) {
			System.out.println("Error: the arguments should be either 11 or 13\n");
			return;
		}

		// to store error message
		String errorMessage = "";

		String assignmentPath = "";
		String path = args[0];
		// check whether the path is not valid
		File f = new File(path);
		if (f.exists() == false)
			errorMessage += "1st argument: the assignment path does not exist\n";
		else
			assignmentPath = path;

		String submissionType = "";
		String rawSubmissionType = args[1];
		if (rawSubmissionType.equals("file"))
			submissionType = "Single file";
		else if (rawSubmissionType.equals("dir"))
			submissionType = "Multiple files in a directory";
		else if (rawSubmissionType.equals("zip"))
			submissionType = "Multiple files in a zip";
		else
			errorMessage += "2nd argument: submission type should be either \"file\", \"dir\", or \"zip\"\n";

		String progLang = "";
		String rawProgLang = args[2];
		if (rawProgLang.equalsIgnoreCase("java"))
			progLang = "Java";
		else if (rawProgLang.equalsIgnoreCase("py"))
			progLang = "Python";
		else if (rawProgLang.equalsIgnoreCase("web"))
			progLang = "Web (HTML, JS, CSS and PHP)";
		else if (rawProgLang.equalsIgnoreCase("dart"))
			progLang = "Dart";
		else if (rawProgLang.equalsIgnoreCase("csharp"))
			progLang = "C#";
		else
			errorMessage += "3rd argument: the programming language should be either \"java\", \"py\", \"csharp\", \"web\", or \"dart\"\n";

		String humanLang = "";
		String rawHumanLang = args[3];
		if (rawHumanLang.equalsIgnoreCase("en"))
			humanLang = "en";
		else if (rawHumanLang.equalsIgnoreCase("id"))
			humanLang = "id";
		else
			errorMessage += "4th argument: the human language should be either \"en\" or \"id\"\n";

		int simThreshold = 0;
		String text = args[4];
		try {
			simThreshold = Integer.parseInt(text);
			if (simThreshold < 0 || simThreshold > 100)
				// if not in range
				errorMessage += "5th argument: the similarity threshold should be from 0 to 100 inclusive\n";
		} catch (Exception e) {
			// if not parsed correctly, the input is not integer
			errorMessage += "5th argument: the similarity threshold should be an integer\n";
		}

		int minMatchLength = 0;
		text = args[5];
		try {
			minMatchLength = Integer.parseInt(text);
			if (minMatchLength < 2)
				// if not in range
				errorMessage += "6th argument: the minimum matching length should be at least 2\n";
		} catch (Exception e) {
			// if not parsed correctly, the input is not integer
			errorMessage += "6th argument: the minimum matching length should be an integer\n";
		}

		int maxPairs = 0;
		text = args[6];
		try {
			maxPairs = Integer.parseInt(text);
			if (maxPairs < 1)
				// if not in range
				errorMessage += "7th argument: the maximum program pairs reported should be at least 1\n";
		} catch (Exception e) {
			// if not parsed correctly, the input is not integer
			errorMessage += "7th argument: the maximum program pairs reported should be an integer\n";
		}

		String templateDirPath = "";
		path = args[7];
		if (path.equals("none") == false) {
			// if not java nor python, ignore template directory path
			if (progLang != "Java" && progLang != "Python") {
				errorMessage += "8th argument: the template directory path should be \"none\"; it is only supported for Java and Python\n";
			} else {
				// check whether the path is not valid
				f = new File(path);
				if (f.exists() == false)
					errorMessage += "8th argument: the template directory path does not exist\n";
				else
					templateDirPath = path;
			}
		}

		// if not java and python but use template code, display an error message
		if (templateDirPath.length() > 0 && !(progLang.equals("Java") || progLang.equals("Python")))
			errorMessage += "8th argument: the template code removal currently works on Java or Python only\n";

		boolean isCommonCodeAllowed = false;
		text = args[8];
		try {
			isCommonCodeAllowed = Boolean.parseBoolean(text);
		} catch (Exception e) {
			// if not parsed correctly, the input is not boolean
			errorMessage += "9th argument: the common code status should be either \"true\" or \"false\"\n";
		}

		if (isCommonCodeAllowed == false && !(progLang.equals("Java") || progLang.equals("Python")))
			errorMessage += "9th argument: the common code removal currently works on Java or Python only\n";

		String similarityMeasurementType = "";
		String rawSimilarityMeasurementType = args[9];
		if (rawSimilarityMeasurementType.toLowerCase().endsWith("MinHash".toLowerCase()))
			similarityMeasurementType = "MinHash";
		else if (rawSimilarityMeasurementType.toLowerCase().endsWith("Super-Bit".toLowerCase()))
			similarityMeasurementType = "Super-Bit";
		else if (rawSimilarityMeasurementType.toLowerCase().endsWith("Jaccard".toLowerCase()))
			similarityMeasurementType = "Jaccard";
		else if (rawSimilarityMeasurementType.toLowerCase().endsWith("Cosine".toLowerCase()))
			similarityMeasurementType = "Cosine";
		else if (rawSimilarityMeasurementType.toLowerCase().endsWith("RKRGST".toLowerCase()))
			similarityMeasurementType = "RKRGST";
		else
			errorMessage += "10th argument: similarity measurement type should be either \"SimHash\", \"Super-Bit\", \"Jaccard\",  \"Cosine\", or \"RKRGST\"\n";

		boolean isSensitive = false;
		if (rawSimilarityMeasurementType.toLowerCase().contains("sensitive"))
			isSensitive = true;

		String resourcePath = args[10];
		// check whether the path is not valid
		f = new File(resourcePath);
		if (f.exists() == false)
			errorMessage += "11th argument: the assignment path does not exist\n";

		pairTemplatePath = resourcePath + File.separator + pairTemplatePath;
		pairWebTemplatePath = resourcePath + File.separator + pairWebTemplatePath;
		indexTemplatePath = resourcePath + File.separator + indexTemplatePath;
		additional_dir_path = resourcePath + File.separator + additional_dir_path;
		javaAdditionalKeywords = resourcePath + File.separator + javaAdditionalKeywords;
		pyAdditionalKeywords = resourcePath + File.separator + pyAdditionalKeywords;

		int numClusters = 2;
		int numStages = 1;

		if (similarityMeasurementType.equals("MinHash") || similarityMeasurementType.equals("Super-Bit")) {
			text = args[11];
			try {
				numClusters = Integer.parseInt(text);
				if (numClusters < 2)
					// if not in range
					errorMessage += "12th argument: the number of clusters should be at least 2\n";
			} catch (Exception e) {
				// if not parsed correctly, the input is not integer
				errorMessage += "12th argument: the number of clusters should be an integer\n";
			}

			text = args[12];
			try {
				numStages = Integer.parseInt(text);
				if (numStages < 1)
					// if not in range
					errorMessage += "13th argument: the number of stages should be at least 1\n";
			} catch (Exception e) {
				// if not parsed correctly, the input is not integer
				errorMessage += "13th argument: the number of stages should be an integer\n";
			}
		}

		if (errorMessage.length() > 0) {
			System.out.println("Error(s)\n" + errorMessage);
		} else {
			// notification to wait
			System.out.println("Please wait...");
			// start processing
			String resultPath = process(assignmentPath, submissionType, progLang, humanLang, simThreshold, maxPairs,
					minMatchLength, templateDirPath, isCommonCodeAllowed, similarityMeasurementType, numClusters,
					numStages, isSensitive);

			if (resultPath != null) {
				// notify user that the process has been completed
				System.out.println("The comparison process is completed\nThe report link is " + resultPath);
			}
		}

	}

	public static String process(String assignmentPath, String submissionType, String progLang, String humanLang,
			int simThreshold, int maxPairs, int minMatchingLength, String templateDirPath, boolean isCommonCodeAllowed,
			String similarityMeasurement, int numClusters, int numStages, boolean isSensitive) {

		ArrayList<File> filesToBeDeleted = new ArrayList<File>();

		File assignmentFile = new File(assignmentPath);
		String assignmentParentDirPath = assignmentFile.getParentFile().getAbsolutePath();
		String assignmentName = assignmentFile.getName();

		// rename programming language to its file extensions
		if (progLang.equals("Java") || progLang.equals("java")) {
			progLang = "java";
		} else if (progLang.equals("Python") || progLang.equals("py")) {
			progLang = "py";
		} else if (progLang.equals("Web (HTML, JS, CSS and PHP)") || progLang.equals("html")) {
			progLang = "html";
		} else if (progLang.equals("Dart") || progLang.equals("dart")) {
			progLang = "dart";
		} else if (progLang.equals("C#") || progLang.equals("csharp")) {
			progLang = "cs";
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
		if (submissionType.startsWith("Multiple files"))
			isMultipleFiles = true;

		// to store result
		ArrayList<ComparisonPairTuple> result;

		long before = System.nanoTime();
		// do the comparison
		if (progLang.equalsIgnoreCase("html"))
			// exclusive for web as it handles js and css also
			result = FastComparerWeb.doSyntacticComparison(assignmentPath, humanLang, simThreshold, minMatchingLength,
					maxPairs, similarityMeasurement, assignmentFile, assignmentParentDirPath, assignmentName,
					isMultipleFiles, isCommonCodeAllowed, filesToBeDeleted, numClusters, numStages, isSensitive);
		else
			result = FastComparer.doSyntacticComparison(assignmentPath, progLang, humanLang, simThreshold,
					minMatchingLength, maxPairs, templateDirPath, similarityMeasurement, assignmentFile,
					assignmentParentDirPath, assignmentName, isMultipleFiles, isCommonCodeAllowed, filesToBeDeleted,
					numClusters, numStages, isSensitive);

		// start deleting all files
		for (File f : filesToBeDeleted)
			FileManipulator.deleteAllTemporaryFiles(f);
		
		// System.out.println("procesing time:\t" + (System.nanoTime() - before));

		return assignmentParentDirPath + File.separator + "[out] " + assignmentName + File.separator + "index.html";

	}
}
