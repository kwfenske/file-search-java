/*
  File Search #1 - Search for Files that Contain a Given String
  Written by: Keith Fenske, http://kwfenske.github.io/
  Saturday, 29 September 2007
  Java class name: FileSearch1
  Copyright (c) 2007 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 application to find files that contain (or don't contain)
  a given string.  The string may be in plain text or it may be a Java regular
  expression.  Such a trivial search should be part of the operating system,
  and in fact, once was.  As bigger and more impressive features were added to
  Windows, it lost the ability to search files for arbitrary bytes of text.
  Windows 98/ME/2000 could find words buried in files with unknown formats;
  Windows XP/Vista/7 will search only supported file types.

  A regular expression is a way of specifying relationships between elements of
  a complex pattern.  You don't need to understand regular expressions to use
  this program.  Please refer to the following on-line sources for information
  about Java regular expressions and for the related topic of character set
  encodings:

      Java Class: Pattern
      http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Pattern.html

      Alan Wood's Unicode Resources
      http://www.alanwood.net/unicode/

  You can safely ignore the option for regular expressions, as they are turned
  off by default.  An attempt is made to break input files into lines, before
  applying a search pattern.  However, you should not rely on this for files
  with very long lines (thousands of characters) or with large amounts of
  binary data.  Compressed data is not expanded for ZIP files or other archive
  types.

  Apache License or GNU General Public License
  --------------------------------------------
  FileSearch1 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options or file and folder names.  If no
  file or folder names are given on the command line, then this program runs as
  a graphical or "GUI" application with the usual dialog boxes and windows.
  See the "-?" option for a help summary:

      java  FileSearch1  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.  If file or folder names are given on the command
  line, then this program runs as a console application without a graphical
  interface.  A generated report is written on standard output, and may be
  redirected with the ">" or "1>" operators.  (Standard error may be redirected
  with the "2>" operator.)  An example command line is:

      java  FileSearch1  -s  "fluffy snakes"  d:\documents  >report.txt

  The console application will return an exit status equal to the number of
  files reported, whether found or not found.  See the -m option on the command
  line.  The graphical interface can be very slow when the output text area
  gets too big, which will happen if thousands of files are reported.

  Restrictions and Limitations
  ----------------------------
  The speed of this program depends upon the speed of your computer's hardware
  (of course) and the complexity of the search string.  When searching for
  plain ASCII text or Unicode characters from 0x20 to 0x7E, the "(raw data
  bytes)" encoding is about 40% faster than the local system's "(default
  encoding)".  Uppercase and lowercase letters are normally considered to be
  equal; selecting the "case" option (-c) avoids this comparison and almost
  doubles the speed.  Even an old Intel Pentium 4 processor at 3.0 GHz should
  be able to scan large files at 15 megabytes per second (MB/s) as raw data
  bytes with the "case" option enabled.  The "nulls" option (-n) to ignore
  <NUL> and <DEL> control characters is not for performance, but is useful when
  plain text may appear as 7-bit ASCII or 16-bit Unicode.  Don't use the "(raw
  data bytes)" encoding with 8-bit characters (or higher) unless you fully
  understand character sets.

  Suggestions for New Features
  ----------------------------
  (1) Allow searches in hexadecimal, without forcing hex strings to be written
      as regular expressions using \\uhhhh or \\xhh constructs.  Parsing would
      require some knowledge of the bit size for a chosen character set, and
      may require options for byte order and alignment.  KF, 2008-10-22.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support

public class FileSearch1
{
  /* constants */

  static final long BIG_FILE_SIZE = 3 * 1024 * 1024; // "big" means over 3 MB
  static final int BUFFER_SIZE = 0x10000; // input buffer size in bytes (64 KB)
  static final int BYTE_MASK = 0x000000FF; // gets low-order byte from integer
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2007 by Keith Fenske.  Apache License or GNU GPL.";
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final String EMPTY_STATUS = " "; // message when no status to display
  static final String[] FONT_SIZES = {"10", "12", "14", "16", "18", "20", "24",
    "30"};                        // point sizes for text in output text area
  static final String LOCAL_ENCODING = "(default encoding)";
                                  // our special name for local character set
  static final int MATCH_WINDOW = 50; // display window around successful match
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Search for Files that Contain a Given String - by: Keith Fenske";
  static final String RAW_ENCODING = "(raw data bytes)";
                                  // our special name for no data encoding
  static final String[] REPORT_CHOICES = {"found, show summary",
    "found, path only", "found, name only", "not found, summary",
    "not found, path", "not found, name", "all files, summary"};
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 700; // 0.700 seconds between status updates

  /* class variables */

  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static JCheckBox caseCheckbox;  // graphical option for <caseFlag>
  static boolean caseFlag;        // true if uppercase/lowercase is significant
  static boolean consoleFlag;     // true if running as a console application
  static boolean debugFlag;       // true if we show debug information
  static JComboBox encodeDialog;  // graphical option for <encodeName>
  static String encodeName;       // name of assumed character set encoding
  static JButton exitButton;      // "Exit" button for ending this application
  static int failCount;           // number of files that don't match search
  static JFileChooser fileChooser; // asks for input and output file names
  static int folderCount;         // number of folders found
  static String fontName;         // font name for text in output text area
  static JComboBox fontNameDialog; // graphical option for <fontName>
  static int fontSize;            // point size for text in output text area
  static JComboBox fontSizeDialog; // graphical option for <fontSize>
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static boolean hiddenFlag;      // true if we process hidden files or folders
  static JFrame mainFrame;        // this application's window if GUI
  static int matchCount;          // number of files that match search string
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JCheckBox nullCheckbox;  // graphical option for <nullFlag>
  static boolean nullFlag;        // true if we ignore <NUL> and <DEL> chars
  static JButton openButton;      // "Open" button for files or folders
  static File[] openFileList;     // list of files selected by user
  static Thread openFilesThread;  // separate thread for doOpenButton() method
  static JTextArea outputText;    // generated report if running as GUI
  static JCheckBox recurseCheckbox; // graphical option for <recurseFlag>
  static boolean recurseFlag;     // true if we process folders and subfolders
  static JCheckBox regexCheckbox; // graphical option for <regexFlag>
  static boolean regexFlag;       // true if search is Java regular expression
  static JComboBox reportDialog;  // graphical option for <reportIndex>
  static int reportIndex;         // user's selection from <REPORT_CHOICES>
  static JButton saveButton;      // "Save" button for writing output text
  static JTextField searchDialog; // graphical option for <searchString>
  static Pattern searchPattern;   // compiled regular expression for searching
  static String searchString;     // Unicode text or expression to search for
  static JLabel statusDialog;     // status message during extended processing
  static String statusPending;    // will become <statusDialog> after delay
  static javax.swing.Timer statusTimer; // timer for updating status message

/*
  main() method

  If we are running as a GUI application, set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Font buttonFont;              // font for buttons, labels, status, etc
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    buttonFont = null;            // by default, don't use customized font
    cancelFlag = false;           // don't cancel unless user complains
    caseFlag = false;             // by default, uppercase lowercase are equal
    consoleFlag = false;          // assume no files or folders on command line
    debugFlag = false;            // by default, don't show debug information
    encodeName = LOCAL_ENCODING;  // default name for character set encoding
    failCount = folderCount = matchCount = 0; // no files or folders found yet
    fontName = "Verdana";         // preferred font name for output text area
    fontSize = 16;                // default point size for output text area
    hiddenFlag = true;            // by default, process hidden files, folders
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    nullFlag = false;             // by default, keep <NUL> and <DEL> chars
    recurseFlag = false;          // by default, don't process subfolders
    regexFlag = false;            // by default, search is plain Unicode text
    reportIndex = 0;              // by default, report only successful matches
    searchPattern = null;         // by default, there is no compiled search
    searchString = "";            // by default, we don't have a search string
    statusPending = EMPTY_STATUS; // begin with no text for <statusDialog>
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    /* Check command-line parameters for options.  Anything we don't recognize
    as an option is assumed to be a file or folder name. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
//      || word.equals("-h") || (mswinFlag && word.equals("/h")) // see: hidden
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(0);           // exit application after printing help
      }

      else if (word.equals("-c") || (mswinFlag && word.equals("/c"))
        || word.equals("-c1") || (mswinFlag && word.equals("/c1")))
      {
        caseFlag = true;          // uppercase and lowercase are different
      }
      else if (word.equals("-c0") || (mswinFlag && word.equals("/c0")))
        caseFlag = false;         // uppercase and lowercase are equal

      else if (word.equals("-d") || (mswinFlag && word.equals("/d")))
      {
        debugFlag = true;         // show debug information
        System.err.println("main args.length = " + args.length);
        for (int k = 0; k < args.length; k ++)
          System.err.println("main args[" + k + "] = <" + args[k] + ">");
      }

      else if (word.startsWith("-e") || (mswinFlag && word.startsWith("/e")))
      {
        encodeName = args[i].substring(2); // accept any string from user
      }

      else if (word.startsWith("-f") || (mswinFlag && word.startsWith("/f")))
      {
        searchString = args[i].substring(2); // accept any string from user
      }

      else if (word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-h1") || (mswinFlag && word.equals("/h1")))
      {
        hiddenFlag = true;        // process hidden files and folders
      }
      else if (word.equals("-h0") || (mswinFlag && word.equals("/h0")))
        hiddenFlag = false;       // ignore hidden files or subfolders

      else if (word.startsWith("-m") || (mswinFlag && word.startsWith("/m")))
      {
        /* This option is followed by an index into <REPORT_CHOICES> for the
        message level.  We don't assign any meaning to the index here. */

        try                       // try to parse remainder as unsigned integer
        {
          reportIndex = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          reportIndex = -1;       // set result to an illegal value
        }
        if ((reportIndex < 0) || (reportIndex >= REPORT_CHOICES.length))
        {
          System.err.println("Message option must be from -m0 to -m"
            + (REPORT_CHOICES.length - 1) + " not: " + args[i]);
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
      }

      else if (word.equals("-n") || (mswinFlag && word.equals("/n"))
        || word.equals("-n1") || (mswinFlag && word.equals("/n1")))
      {
        nullFlag = true;          // ignore <NUL> and <DEL> control characters
      }
      else if (word.equals("-n0") || (mswinFlag && word.equals("/n0")))
        nullFlag = false;         // keep <NUL> and <DEL> chars as file text

      else if (word.equals("-r") || (mswinFlag && word.equals("/r"))
        || word.equals("-r1") || (mswinFlag && word.equals("/r1")))
      {
        regexFlag = true;         // search string is a Java regular expression
      }
      else if (word.equals("-r0") || (mswinFlag && word.equals("/r0")))
        regexFlag = false;        // search string is plain Unicode text

      else if (word.equals("-s") || (mswinFlag && word.equals("/s"))
        || word.equals("-s1") || (mswinFlag && word.equals("/s1")))
      {
        recurseFlag = true;       // start doing subfolders
      }
      else if (word.equals("-s0") || (mswinFlag && word.equals("/s0")))
        recurseFlag = false;      // stop doing subfolders

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size that will be used for
        buttons, dialogs, labels, etc. */

        int size = -1;            // default value for font point size
        try                       // try to parse remainder as unsigned integer
        {
          size = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          size = -1;              // set result to an illegal value
        }
        if ((size < 10) || (size > 99))
        {
          System.err.println("Dialog font size must be from 10 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
        buttonFont = new Font(SYSTEM_FONT, Font.PLAIN, size); // for big sizes
//      buttonFont = new Font(SYSTEM_FONT, Font.BOLD, size); // for small sizes
        fontSize = size;          // use same point size for output text font
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\s*\\(\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          windowLeft = Integer.parseInt(matcher.group(1));
          windowTop = Integer.parseInt(matcher.group(2));
          windowWidth = Integer.parseInt(matcher.group(3));
          windowHeight = Integer.parseInt(matcher.group(4));
        }
        else                      // bad syntax or too many digits
        {
          System.err.println("Invalid window position or size: " + args[i]);
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // true if we maximize our main window

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(-1);          // exit application after printing help
      }

      else
      {
        /* Parameter does not look like an option.  The first non-option is a
        search string.  Second and later non-options are file or folder names.
        Even through there is no graphical interface, the <cancelFlag> can
        still be set if the search string is an invalid regular expression. */

        if (searchString.length() == 0)
        {
          searchString = args[i]; // accept anything for a search string
        }
        else
        {
          consoleFlag = true;     // don't allow GUI methods to be called
          processFileOrFolder(new File(args[i]));
          if (cancelFlag)         // if some fatal error was reported
          {
            showHelp();           // show help summary
            System.exit(-1);      // exit application after printing help
          }
        }
      }
    }

    /* If running as a console application, print a summary of what we found.
    Exit to the system with an integer status that has the number of files
    successfully matched or not matched. */

    if (consoleFlag)              // was at least one file/folder given?
    {
      putError("Matched " + prettyPlural(matchCount, "file")
        + " and didn't match " + prettyPlural(failCount, "file") + " in "
        + prettyPlural(folderCount, "folder") + ".");
      System.exit(((reportIndex >= 3) && (reportIndex <= 5)) ? failCount
        : matchCount);            // exit from application with status
    }

    /* There were no file or folder names on the command line.  Open the
    graphical user interface (GUI).  We don't need to be inside an if-then-else
    construct here because the console application called System.exit() above.
    The standard Java interface style is the most reliable, but you can switch
    to something closer to the local system, if you want. */

//  try
//  {
//    UIManager.setLookAndFeel(
//      UIManager.getCrossPlatformLookAndFeelClassName());
////    UIManager.getSystemLookAndFeelClassName());
//  }
//  catch (Exception ulafe)
//  {
//    System.err.println("Unsupported Java look-and-feel: " + ulafe);
//  }

    /* Initialize shared graphical objects. */

    action = new FileSearch1User(); // create our shared action listener
    fileChooser = new JFileChooser(); // create our shared file chooser
    statusTimer = new javax.swing.Timer(TIMER_DELAY, action);
                                  // update status message on clock ticks only

    /* If our preferred font is not available for the output text area, then
    use the boring default font for the local system. */

    if (fontName.equals((new Font(fontName, Font.PLAIN, fontSize)).getFamily())
      == false)                   // create font, read back created name
    {
      fontName = SYSTEM_FONT;     // must replace with standard system font
    }

    /* Create the graphical interface as a series of little panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel01, panel02, etc). */

    /* Create a vertical box to stack buttons and options. */

    JPanel panel01 = new JPanel();
    panel01.setLayout(new BoxLayout(panel01, BoxLayout.Y_AXIS));

    /* Create a horizontal panel for the action buttons. */

    JPanel panel11 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    openButton = new JButton("Open File/Folder...");
    openButton.addActionListener(action);
    if (buttonFont != null) openButton.setFont(buttonFont);
    openButton.setMnemonic(KeyEvent.VK_O);
    openButton.setToolTipText("Start finding/opening files.");
    panel11.add(openButton);
    panel11.add(Box.createHorizontalStrut(50));

    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(action);
    cancelButton.setEnabled(false);
    if (buttonFont != null) cancelButton.setFont(buttonFont);
    cancelButton.setMnemonic(KeyEvent.VK_C);
    cancelButton.setToolTipText("Stop finding/opening files.");
    panel11.add(cancelButton);
    panel11.add(Box.createHorizontalStrut(50));

    saveButton = new JButton("Save Output...");
    saveButton.addActionListener(action);
    if (buttonFont != null) saveButton.setFont(buttonFont);
    saveButton.setMnemonic(KeyEvent.VK_S);
    saveButton.setToolTipText("Copy output text to a file.");
    panel11.add(saveButton);
    panel11.add(Box.createHorizontalStrut(50));

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel11.add(exitButton);

    panel01.add(panel11);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Create a horizontal panel for the search string and encoding. */

    JPanel panel21 = new JPanel(new BorderLayout(10, 0));

    JLabel label22 = new JLabel("Search:");
    if (buttonFont != null) label22.setFont(buttonFont);
    panel21.add(label22, BorderLayout.WEST);

    searchDialog = new JTextField(searchString, 20);
    if (buttonFont != null) searchDialog.setFont(buttonFont);
    searchDialog.setMargin(new Insets(1, 3, 2, 3)); // top, left, bottom, right
//  searchDialog.addActionListener(action); // do last so don't fire early
    panel21.add(searchDialog, BorderLayout.CENTER);

    encodeDialog = new JComboBox();
    encodeDialog.addItem(LOCAL_ENCODING); // start with our special names
    encodeDialog.addItem(RAW_ENCODING);
    Object[] list23 = java.nio.charset.Charset.availableCharsets().keySet()
      .toArray();                 // get character set names from local system
    for (i = 0; i < list23.length; i ++)
      encodeDialog.addItem((String) list23[i]); // insert each encoding name
    encodeDialog.setEditable(true); // allow user to enter alternate names
    if (buttonFont != null) encodeDialog.setFont(buttonFont);
    encodeDialog.setSelectedItem(encodeName); // selected item is our default
    encodeDialog.setToolTipText(
      "Select name of character set encoding for reading files.");
    encodeDialog.addActionListener(action); // do last so don't fire early
    panel21.add(encodeDialog, BorderLayout.EAST);

    panel01.add(panel21);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Create a horizontal panel for search options. */

    JPanel panel31 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    JLabel label32 = new JLabel("Options:");
    if (buttonFont != null) label32.setFont(buttonFont);
    panel31.add(label32);
    panel31.add(Box.createHorizontalStrut(10));

    caseCheckbox = new JCheckBox("exact case", caseFlag);
    if (buttonFont != null) caseCheckbox.setFont(buttonFont);
    caseCheckbox.setToolTipText(
      "Select if uppercase and lowercase are different.");
    caseCheckbox.addActionListener(action); // do last so don't fire early
    panel31.add(caseCheckbox);
    panel31.add(Box.createHorizontalStrut(10));

    nullCheckbox = new JCheckBox("ignore nulls", nullFlag);
    if (buttonFont != null) nullCheckbox.setFont(buttonFont);
    nullCheckbox.setToolTipText(
      "Select to ignore <NUL> and <DEL> characters.");
    nullCheckbox.addActionListener(action); // do last so don't fire early
    panel31.add(nullCheckbox);
    panel31.add(Box.createHorizontalStrut(10));

    regexCheckbox = new JCheckBox("regular expression", regexFlag);
    if (buttonFont != null) regexCheckbox.setFont(buttonFont);
    regexCheckbox.setToolTipText(
      "Select if search string is a Java regular expression.");
    regexCheckbox.addActionListener(action); // do last so don't fire early
    panel31.add(regexCheckbox);
    panel31.add(Box.createHorizontalStrut(10));

    recurseCheckbox = new JCheckBox("search subfolders", recurseFlag);
    if (buttonFont != null) recurseCheckbox.setFont(buttonFont);
    recurseCheckbox.setToolTipText("Select to search folders and subfolders.");
    recurseCheckbox.addActionListener(action); // do last so don't fire early
    panel31.add(recurseCheckbox);

    panel01.add(panel31);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Create a horizontal panel for report options. */

    JPanel panel41 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    JLabel label42 = new JLabel("Display:");
    if (buttonFont != null) label42.setFont(buttonFont);
    panel41.add(label42);
    panel41.add(Box.createHorizontalStrut(10));

    reportDialog = new JComboBox(REPORT_CHOICES);
    reportDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) reportDialog.setFont(buttonFont);
    reportDialog.setSelectedIndex(reportIndex); // select default level
    reportDialog.setToolTipText("Select which files to report.");
    reportDialog.addActionListener(action); // do last so don't fire early
    panel41.add(reportDialog);
    panel41.add(Box.createHorizontalStrut(40));

    JLabel label43 = new JLabel("Font:");
    if (buttonFont != null) label43.setFont(buttonFont);
    panel41.add(label43);
    panel41.add(Box.createHorizontalStrut(10));

    fontNameDialog = new JComboBox(GraphicsEnvironment
      .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    fontNameDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontNameDialog.setFont(buttonFont);
    fontNameDialog.setSelectedItem(fontName); // select default font name
    fontNameDialog.setToolTipText("Font name for output text.");
    fontNameDialog.addActionListener(action); // do last so don't fire early
    panel41.add(fontNameDialog);
    panel41.add(Box.createHorizontalStrut(10));

    TreeSet sizelist = new TreeSet(); // collect font sizes 10 to 99 in order
    word = String.valueOf(fontSize); // convert number to a string we can use
    sizelist.add(word);           // add default or user's chosen font size
    for (i = 0; i < FONT_SIZES.length; i ++) // add our preferred size list
      sizelist.add(FONT_SIZES[i]); // assume sizes are all two digits (10-99)
    fontSizeDialog = new JComboBox(sizelist.toArray()); // give user nice list
    fontSizeDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontSizeDialog.setFont(buttonFont);
    fontSizeDialog.setSelectedItem(word); // selected item is our default size
    fontSizeDialog.setToolTipText("Point size for output text.");
    fontSizeDialog.addActionListener(action); // do last so don't fire early
    panel41.add(fontSizeDialog);

    panel01.add(panel41);

    /* Put above boxed options in a panel that is centered horizontally.  Use
    FlowLayout's horizontal gap to add padding on the left and right sides. */

    JPanel panel51 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    panel51.add(panel01);

    /* Use another BorderLayout for precise control over the margins. */

    JPanel panel52 = new JPanel(new BorderLayout(0, 0));
    panel52.add(Box.createVerticalStrut(11), BorderLayout.NORTH);
    panel52.add(Box.createHorizontalStrut(5), BorderLayout.WEST);
    panel52.add(panel51, BorderLayout.CENTER);
    panel52.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
    panel52.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

    /* Create a scrolling text area for the generated output. */

    outputText = new JTextArea(20, 40);
    outputText.setEditable(false); // user can't change this text area
    outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    outputText.setLineWrap(false); // don't wrap text lines
    outputText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right
    outputText.setText(
      "\nSearch for files that contain or don't contain a given string."
      + "\n\nChoose your options; then open files or folders to search."
      + "\n\nCopyright (c) 2007 by Keith Fenske.  By using this program, you"
      + "\nagree to terms and conditions of the Apache License and/or GNU"
      + "\nGeneral Public License.\n\n");

    /* Create an entire panel just for the status message.  We do this so that
    we have some control over the margins.  Put the status text in the middle
    of a BorderLayout so that it expands with the window size. */

    JPanel panel53 = new JPanel(new BorderLayout(0, 0));
    statusDialog = new JLabel(EMPTY_STATUS, JLabel.LEFT);
    if (buttonFont != null) statusDialog.setFont(buttonFont);
    statusDialog.setToolTipText(
      "Running status as files are processed by the Open button.");
    panel53.add(Box.createVerticalStrut(4), BorderLayout.NORTH);
    panel53.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    panel53.add(statusDialog, BorderLayout.CENTER);
    panel53.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
    panel53.add(Box.createVerticalStrut(3), BorderLayout.SOUTH);

    /* Create the main window frame for this application.  Stack buttons and
    options above the text area.  Keep text in the center so that it expands
    horizontally and vertically.  Put status message at the bottom, which also
    expands. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel54 = mainFrame.getContentPane(); // where content meets frame
    panel54.setLayout(new BorderLayout(0, 0));
    panel54.add(panel52, BorderLayout.NORTH); // buttons and options
    panel54.add(new JScrollPane(outputText), BorderLayout.CENTER); // text area
    panel54.add(panel53, BorderLayout.SOUTH); // status message

    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    if ((windowHeight < MIN_FRAME) || (windowWidth < MIN_FRAME))
      mainFrame.pack();           // do component layout with minimum size
    else                          // the user has given us a window size
      mainFrame.setSize(windowWidth, windowHeight); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainFrame.validate();         // recheck application window layout
    mainFrame.setVisible(true);   // and then show application window

    /* Let the graphical interface run the application now. */

    searchDialog.requestFocusInWindow(); // sometimes works, sometimes doesn't

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  doCancelButton() method

  This method is called while we are opening files or folders if the user wants
  to end the processing early, perhaps because it is taking too long.  We must
  cleanly terminate any secondary threads.  Leave whatever output has already
  been generated in the output text area.
*/
  static void doCancelButton()
  {
    cancelFlag = true;            // tell other threads that all work stops now
    putError("Cancelled by user."); // print message and scroll
  }


/*
  doOpenButton() method

  Allow the user to select one or more files or folders for processing.
*/
  static void doOpenButton()
  {
    /* The only option that must be supplied by the user is a search string. */

    searchString = searchDialog.getText(); // plain text or regular expression
    if (searchString.length() == 0)
    {
      JOptionPane.showMessageDialog(mainFrame,
        "Please enter a search string before opening files or folders.");
      return;                     // end the Open button early
    }

    /* Ask the user for input files or folders. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Open Files or Folders...");
    fileChooser.setFileHidingEnabled(! hiddenFlag); // may show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setMultiSelectionEnabled(true); // allow more than one file
    if (fileChooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    openFileList = sortFileList(fileChooser.getSelectedFiles());
                                  // get list of files selected by user

    /* We have a list of files or folders.  Disable the "Open" button until we
    are done, and enable a "Cancel" button in case our secondary thread runs
    for a long time and the user panics. */

    cancelButton.setEnabled(true); // enable button to cancel this processing
    cancelFlag = false;           // but don't cancel unless user complains
    failCount = folderCount = matchCount = 0; // no files or folders found yet
    openButton.setEnabled(false); // suspend "Open" button until we are done
    outputText.setText("");       // clear output text area
    searchPattern = null;         // we haven't compiled the search string yet
    setStatusMessage(EMPTY_STATUS); // clear status message at bottom of window
    statusTimer.start();          // start updating the status message

    openFilesThread = new Thread(new FileSearch1User(), "doOpenRunner");
    openFilesThread.setPriority(Thread.MIN_PRIORITY);
                                  // use low priority for heavy-duty workers
    openFilesThread.start();      // run separate thread to open files, report

  } // end of doOpenButton() method


/*
  doOpenRunner() method

  This method is called inside a separate thread by the runnable interface of
  our "user" class to process the user's selected files in the context of the
  "main" class.  By doing all the heavy-duty work in a separate thread, we
  won't stall the main thread that runs the graphical interface, and we allow
  the user to cancel the processing if it takes too long.
*/
  static void doOpenRunner()
  {
    int i;                        // index variable

    /* Loop once for each file name selected.  Don't assume that these are all
    valid file names. */

    for (i = 0; i < openFileList.length; i ++)
    {
      if (cancelFlag) break;      // exit from <for> loop if user cancelled
      processFileOrFolder(openFileList[i]); // process this file or folder
    }

    /* Print a summary and scroll the output, even if we were cancelled. */

    putError("Matched " + prettyPlural(matchCount, "file")
      + " and didn't match " + prettyPlural(failCount, "file") + " in "
      + prettyPlural(folderCount, "folder") + ".");

    /* We are done.  Turn off the "Cancel" button and allow the user to click
    the "Start" button again. */

    cancelButton.setEnabled(false); // disable "Cancel" button
    openButton.setEnabled(true);  // enable "Open" button
    statusTimer.stop();           // stop updating status on timer ticks
    setStatusMessage(EMPTY_STATUS); // and clear any previous status message

  } // end of doOpenRunner() method


/*
  doSaveButton() method

  Ask the user for an output file name, create or replace that file, and copy
  the contents of our output text area to that file.  The output file will be
  in the default character set for the system, so if there are special Unicode
  characters in the displayed text (Arabic, Chinese, Eastern European, etc),
  then you are better off copying and pasting the output text directly into a
  Unicode-aware application like Microsoft Word.
*/
  static void doSaveButton()
  {
    FileWriter output;            // output file stream
    File userFile;                // file chosen by the user

    /* Ask the user for an output file name. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Save Output as Text File...");
    fileChooser.setFileHidingEnabled(true); // don't show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    userFile = fileChooser.getSelectedFile();

    /* See if we can write to the user's chosen file. */

    if (userFile.isDirectory())   // can't write to directories or folders
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a directory or folder.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isHidden()) // won't write to hidden (protected) files
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a hidden or protected file.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isFile() == false) // if file doesn't exist
    {
      /* Maybe we can create a new file by this name.  Do nothing here. */
    }
    else if (userFile.canWrite() == false) // file exists, but is read-only
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is locked or write protected.\nCan't write to this file."));
      return;
    }
    else if (JOptionPane.showConfirmDialog(mainFrame, (userFile.getName()
      + " already exists.\nDo you want to replace this with a new file?"))
      != JOptionPane.YES_OPTION)
    {
      return;                     // user cancelled file replacement dialog
    }

    /* Write lines to output file. */

    try                           // catch file I/O errors
    {
      output = new FileWriter(userFile); // try to open output file
      outputText.write(output);   // couldn't be much easier for writing!
      output.close();             // try to close output file
    }
    catch (IOException ioe)
    {
      putError("Can't write to text file: " + ioe.getMessage());
    }
  } // end of doSaveButton() method


/*
  formatMatchWindow() method

  Format and return a small portion of an input line that surrounds the
  starting and ending positions of a matched pattern.
*/
  static String formatMatchWindow(
    StringBuffer text,            // input text from caller's string buffer
    int start,                    // index of first matching character
    int end)                      // index plus one of last matching character
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int i;                        // index variable
    int left, right;              // our starting and ending(+1) positions

    /* Try to center left side, or stop at start of string. */

    left = Math.max(0, (start - ((MATCH_WINDOW + start - end) / 2)));

    /* Try to use maximum window for right side, or stop at end of string. */

    right = Math.min((left + MATCH_WINDOW), text.length());

    /* Try to use maximum window for left side, or stop at start of string. */

    left = Math.max(0, (right - MATCH_WINDOW));

    /* Extract the characters, substituting anything that's not printable. */

    buffer = new StringBuffer();  // allocate empty string buffer for result
    for (i = left; i < right; i ++)
    {
      ch = text.charAt(i);        // get one character from caller's string
      if (Character.isISOControl(ch) || (Character.isDefined(ch) == false))
        buffer.append('.');       // replace with the ever-present period
      else
        buffer.append(ch);        // copy character unchanged to result
    }
    return(buffer.toString());    // give caller our converted string

  } // end of formatMatchWindow() method


/*
  makeRegularPlain() method

  Convert plain text into an equivalent regular expression.  This allows us to
  search for plain text with the same algorithm as regular expressions -- and
  isn't much slower.
*/
  static String makeRegularPlain(String text)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int i;                        // index variable
    int length;                   // size of input string in characters

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = text.length();       // get size of caller's string in characters
    for (i = 0; i < length; i ++) // do all characters in caller's string
    {
      ch = text.charAt(i);        // get one character from caller's string
      if ((ch == '$') || (ch == '(') || (ch == ')') || (ch == '*')
        || (ch == '+') || (ch == '.') || (ch == '?') || (ch == '[')
        || (ch == '\\') || (ch == ']') || (ch == '^') || (ch == '{')
        || (ch == '|') || (ch == '}'))
      {
        buffer.append('\\');      // escape this special character
      }
      buffer.append(ch);          // and append the original character
    }
    return(buffer.toString());    // give caller our converted string

  } // end of makeRegularPlain() method


/*
  prettyPlural() method

  Return a string that formats a number and appends a lowercase "s" to a word
  if the number is plural (not one).  Also provide a more general method that
  accepts both a singular word and a plural word.
*/
  static String prettyPlural(
    long number,                  // number to be formatted
    String singular)              // singular word
  {
    return(prettyPlural(number, singular, (singular + "s")));
  }

  static String prettyPlural(
    long number,                  // number to be formatted
    String singular,              // singular word
    String plural)                // plural word
  {
    final String[] names = {"zero", "one", "two"};
                                  // names for small counting numbers
    String result;                // our converted result

    if ((number >= 0) && (number < names.length))
      result = names[(int) number]; // use names for small counting numbers
    else
      result = formatComma.format(number); // format number with digit grouping

    if (number == 1)              // is the number singular or plural?
      result += " " + singular;   // append singular word
    else
      result += " " + plural;     // append plural word

    return(result);               // give caller our converted string

  } // end of prettyPlural() method


/*
  processFileOrFolder() method

  The caller gives us a Java File object that may be a file, a folder, or just
  random garbage.  Search all files.  Get folder contents and process each file
  found, doing subfolders only if the <recurseFlag> is true.
*/
  static void processFileOrFolder(File givenFile)
  {
    File[] contents;              // contents if <givenFile> is a folder
    int i;                        // index variable
    File next;                    // next File object from <contents>

    if (cancelFlag) return;       // stop if user hit the panic button

    /* Decide what kind of File object this is, or if it's even real!  The code
    when we find a subfolder mimics the overall structure of this method, with
    the exception of hidden files or folders.  We always process files given to
    us by the user, whether hidden or not. */

    if (givenFile.isDirectory())  // is this "file" actually a folder?
    {
      folderCount ++;             // found one more folder, contents unknown
      setStatusMessage("Folder " + givenFile.getPath());
      contents = sortFileList(givenFile.listFiles()); // no filter, but sorted
      for (i = 0; i < contents.length; i ++) // for each file in order
      {
        if (cancelFlag) return;   // stop if user hit the panic button
        next = contents[i];       // get next File object from <contents>
        if ((hiddenFlag == false) && next.isHidden()) // hidden file or folder?
        {
          if (reportIndex == 6)   // are we reporting all files?
            putOutput("Ignoring hidden " + next.getPath());
        }
        else if (next.isDirectory()) // a subfolder inside caller's folder?
        {
          if (recurseFlag)        // do subfolders only if option selected
            processFileOrFolder(next); // call ourself to handle subfolders
          else if (reportIndex == 6) // are we reporting all files?
            putOutput("Ignoring subfolder " + next.getPath());
        }
        else if (next.isFile())   // we do want to look at normal files
        {
          processUnknownFile(next); // figure out what to do with this file
        }
        else if (reportIndex == 6) // file directory has an invalid entry
        {
          putOutput("Ignoring unknown " + next.getPath());
        }
      }
    }
    else if (givenFile.isFile())  // we do want to look at normal files
    {
      processUnknownFile(givenFile); // figure out what to do with this file
    }
    else                          // user gave bad file or folder name
    {
      putError("Not a file or folder: " + givenFile.getPath());
    }
  } // end of processFileOrFolder() method


/*
  processUnknownFile() method

  The caller gives us a Java File object that is known to be a file, not a
  directory.  Read until we either find the search string or reach the end of
  the file.
*/
  static void processUnknownFile(File givenFile)
  {
    int ch;                       // one input character, or -1 for end-of-file
    long charRead;                // number of characters read (not bytes)
    BufferedReader charStream;    // input stream for decoded characters
    long charTold;                // we've told user about this many characters
    boolean done;                 // true when we are done reading from file
    String fileName;              // name for caller's file, fetched once only
//  long fileSize;                // size of caller's file in bytes (not chars)
    boolean found;                // true if the search pattern was found
    StringBuffer lineBuffer;      // buffer for assembling one line of text
    int lineCount;                // number of characters used in <lineBuffer>
    long lineNumber;              // current line number in file, assuming text
    boolean lineReady;            // true when line buffer is complete or full
    Matcher matcher;              // pattern matcher for <searchPattern>
    byte[] rawBuffer;             // buffer when reading raw 8-bit bytes
    int rawCount;                 // number of bytes used in <rawBuffer>
    int rawNext;                  // index of next byte "read" in <rawBuffer>
    FileInputStream rawStream;    // input stream for raw 8-bit bytes
    boolean wasCr;                // true if last character was carriage return

    /* Get some basic information about the caller's file.  Fetching the name
    and the size won't cause any errors (exceptions). */

    if (cancelFlag) return;       // stop if user hit the panic button
    fileName = givenFile.getPath(); // use full path name as the file name
//  fileSize = givenFile.length(); // get total file size in bytes (not chars)

    /* Compile the search string as a regular expression, if we haven't already
    done so.  Regular expressions may be slower to compile, but once compiled,
    they can be quickly reused from file to file. */

    if (searchPattern == null)    // if we haven't already compiled expression
    {
      String express = regexFlag ? searchString
        : makeRegularPlain(searchString); // use given regex or convert plain
      int flags = caseFlag ? 0 : (Pattern.CASE_INSENSITIVE
        | Pattern.UNICODE_CASE);  // are uppercase and lowercase different?
      if (debugFlag)              // does user want debug information?
        System.err.println("processUnknownFile express = <" + express
          + ">, flags = " + flags);
      try                         // attempt to compile regular expression
      {
        searchPattern = Pattern.compile(express, flags);
      }
      catch (PatternSyntaxException pse) // if expression syntax is invalid
      {
        cancelFlag = true;        // stop looking at files or folders
        searchPattern = null;     // invalidate anything created above
        if (consoleFlag)          // are we running in console mode?
          putError("Invalid regular expression: " + express);
        else
          JOptionPane.showMessageDialog(mainFrame,
            ("Search string has poor syntax as a regular expression:\n"
            + express));
        return;                   // return early from this method
      }
    }

    /* Tell GUI users which file we are about to open and read.  This status
    will be updated later for really big files. */

    setStatusMessage("Reading " + fileName);

    /* Try to open the data file for reading. */

    try                           // catch specific and general I/O errors
    {
      charStream = null;          // assume there will be no character stream
      rawBuffer = null;           // just to keep compiler happy
      rawCount = rawNext = 0;     // keep compiler happy: mark raw buffer empty
      rawStream = new FileInputStream(givenFile); // but always need raw bytes
      if (encodeName.equals(LOCAL_ENCODING)) // use local system's encoding?
      {
        charStream = new BufferedReader(new InputStreamReader(rawStream));
      }
      else if (encodeName.equals(RAW_ENCODING)) // use raw bytes as characters?
      {
        rawBuffer = new byte[BUFFER_SIZE]; // allocate space for reading
      }
      else                        // must be some named character set encoding
      {
        charStream = new BufferedReader(new InputStreamReader(rawStream,
          encodeName));
      }

      /* Basic loop is to assemble one "line" of text, breaking at the standard
      newline characters (DOS CR/LF, UNIX NL), or when the buffer gets full. */

      charRead = charTold = 0;    // we haven't read any characters yet
      done = false;               // true when we are done reading from file
      found = false;              // assume that search pattern won't be found
      lineBuffer = new StringBuffer(BUFFER_SIZE); // allocate empty line buffer
      lineNumber = 1;             // first line is number one, assuming text
      wasCr = false;              // no last character, not DOS carriage return
      while (done == false)
      {
        if (cancelFlag) break;    // exit early; this could be a very big file

        /* Update the running status, if this is a big file.  It would be good
        to tell the user how many megabytes or what percent we have finished,
        but the only information we have is the total size of the file in bytes
        and the number of characters we've read so far.  There is no guaranteed
        relationship between input bytes and input characters.  While calls to
        the setStatusMessage() method have no effect for console applications,
        we still avoid the repeated formatting of this message. */

        if ((consoleFlag == false) && ((charRead - charTold) > BIG_FILE_SIZE))
        {
          charTold = charRead;    // remember what we last told the user
          setStatusMessage("Reading " + fileName + " - "
            + formatComma.format(charRead) + " characters");
        }

        /* Fill the line buffer by adding one byte or character at a time.  */

        lineBuffer.setLength(0);  // empty anything already in line buffer
        lineCount = 0;            // mark that we have nothing in line buffer
        lineReady = false;        // line buffer is not full, not complete
        do                        // repeat until we have a line in buffer
        {
          /* Get the next input byte/character, or -1 for end-of-file. */

          if (charStream == null) // are we reading raw 8-bit bytes?
          {
            if (rawNext >= rawCount) // should we fill the buffer again?
            {
              rawCount = rawStream.read(rawBuffer); // fill some or all buffer
              rawNext = 0;        // assume we will use first byte in buffer
              if (rawCount <= 0)  // if nothing read, reached end-of-file
                ch = -1;          // mark this as the end of the file
              else
                ch = rawBuffer[rawNext ++] & BYTE_MASK; // get first byte
            }
            else
              ch = rawBuffer[rawNext ++] & BYTE_MASK; // get next byte
          }
          else
            ch = charStream.read(); // get one input character

          /* Decide if this input character is text, marks the end of the line,
          or marks the end of the file. */

          if (ch < 0)             // negative is for end-of-file
            done = lineReady = true; // don't read anything more after this
          else if (nullFlag && ((ch == 0x00) || (ch == 0x7F)))
            charRead ++;          // count as characters, but otherwise ignore
          else if (ch == '\n')    // is this a UNIX newline or DOS line feed?
          {
            charRead ++;          // count number of characters read
            lineReady = ! wasCr;  // bare NL or LF means end of line
            wasCr = false;        // clear any previous CR status
          }
          else if (ch == '\r')    // is this a DOS carriage return?
          {
            charRead ++;          // count number of characters read
            lineReady = true;     // bare CR or start CR/LF means end of line
            wasCr = true;         // remember this CR in case next is LF
          }
          else                    // must be text character, or other control
          {
            charRead ++;          // count number of characters read
            lineBuffer.append((char) ch); // put text character into buffer
            lineCount ++;         // remember how many characters are in buffer
            wasCr = false;        // clear any previous CR status
          }
        } while ((lineReady == false) && (lineCount < BUFFER_SIZE));

        /* We now have a line of text, or the end of the file, or both.  This
        may be just be a buffer full of garbage for binary files. */

        if (debugFlag && (lineCount >= BUFFER_SIZE)) // show debug information?
        {
          System.err.println("processUnknownFile lineCount = " + lineCount
            + " (full) at charRead = " + charRead + " for givenFile = <"
            + givenFile.getPath() + ">");
        }
        if ((done == false) || (lineCount > 0)) // is there anything to scan?
        {
          matcher = searchPattern.matcher(lineBuffer); // attempt to match
          if (matcher.find())     // if the search pattern is found
          {
            done = found = true;  // don't read anything more after this
            matchCount ++;        // one more file matches search pattern
            if ((reportIndex == 0) || (reportIndex == 6)) // match summary?
            {
              putOutput("Match found for " + fileName + " at line "
                + formatComma.format(lineNumber) + ": "
                + formatMatchWindow(lineBuffer, matcher.start(),
                matcher.end()));
            }
            else if (reportIndex == 1) // show match path only?
              putOutput(givenFile.getPath());
            else if (reportIndex == 2) // show match name only?
              putOutput(givenFile.getName());
          }
          else
            lineNumber ++;        // increment count for next line number
        }
      } // end of <while> read loop

      /* Close the input file.  We may only need to close the raw byte stream,
      but be nice and close the character stream if we opened it that way. */

      if (charStream != null)     // did we open high-level character stream?
        charStream.close();       // yes, first close the character stream

      rawStream.close();          // then always close low-level byte stream

      /* If we didn't find what we were looking for, we should still count this
      file as a failure, and report it as requested by the user's options. */

      if ((cancelFlag == false) && (found == false)) // only need to do failure
      {
        failCount ++;             // one more file doesn't match pattern
        if ((reportIndex == 3) || (reportIndex == 6)) // show failure summary?
          putOutput("Failed to match " + fileName);
        else if (reportIndex == 4) // show failure path only?
          putOutput(givenFile.getPath());
        else if (reportIndex == 5) // show failure name only?
          putOutput(givenFile.getName());
      }
    }

    /* Catch any file I/O errors, here or in called methods. */

    catch (UnsupportedEncodingException uee) // instance of IOException
    {
      cancelFlag = true;          // stop looking at files or folders
      putError("Can't read with character set encoding <" + encodeName + ">.");
    }
    catch (IOException ioe)       // all other I/O errors
    {
      putError("Can't read from file: " + ioe.getMessage());
    }
  } // end of processUnknownFile() method


/*
  putError() method

  Similar to putOutput() except write on standard error if running as a console
  application.  See putOutput() for details.  This method is more for tracing
  execution than for generating a report.  Routines that can only be called
  from within GUI applications should always call putOutput().
*/
  static void putError(String text)
  {
    if (consoleFlag)              // are we running as a console application?
      System.err.println(text);   // console output goes onto standard error
    else
    {
      outputText.append(text + "\n"); // graphical output goes into text area
      outputText.select(999999999, 999999999); // force scroll to end of text
    }
  }


/*
  putOutput() method

  Append a complete line of text to the end of the output text area.  We add a
  newline character at the end of the line, not the caller.  By forcing all
  output to go through this same method, one complete line at a time, the
  generated output is cleaner and can be redirected.

  The output text area is forced to scroll to the end, after the text line is
  written, by selecting character positions that are much too large (and which
  are allowed by the definition of the JTextComponent.select() method).  This
  is easier and faster than manipulating the scroll bars directly.  However, it
  does cancel any selection that the user might have made, for example, to copy
  text from the output area.
*/
  static void putOutput(String text)
  {
    putOutput(text, false);       // by default, do not scroll output lines
  }

  static void putOutput(String text, boolean scroll)
  {
    if (consoleFlag)              // are we running as a console application?
      System.out.println(text);   // console output goes onto standard output
    else
    {
      outputText.append(text + "\n"); // graphical output goes into text area
      if (scroll)                 // does caller want us to scroll?
        outputText.select(999999999, 999999999); // force scroll to end of text
    }
  }


/*
  setStatusMessage() method

  Set the text for the status message if we are running as a GUI application.
  This gives the user some indication of our progress if processing is slow.
  If the update timer is running, then this message will not appear until the
  timer kicks in.  This prevents the status from being updated too often, and
  hence being unreadable.
*/
  static void setStatusMessage(String text)
  {
    statusPending = text;         // save caller's message for later
    if (consoleFlag)              // are we running as a console application?
    {
      /* Do nothing: console doesn't show running status messages. */
    }
    else if (statusTimer.isRunning()) // are we updating on a timed basis?
    {
      /* Do nothing: wait for timer to kick in and update GUI text. */
    }
    else
      statusDialog.setText(text); // show the status message now
  }


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("  java  FileSearch1  [options]  search_string  files_or_folders");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -c0 = uppercase and lowercase are equal (default)");
    System.err.println("  -c1 = -c = uppercase and lowercase are different");
    System.err.println("  -d = show debug information (may be verbose)");
    System.err.println("  -e\"name\" = name of character set encoding for reading files");
    System.err.println("  -f\"string\" = use when search string looks like an option");
    System.err.println("  -h0 = ignore hidden files or folders except given by user");
    System.err.println("  -h1 = -h = process hidden files and folders (default)");
    System.err.println("  -m0 = report files that contain the search string (default)");
    System.err.println("  -m1 = report file name and path only if string found");
    System.err.println("  -m2 = report file name only (no path) if string found");
    System.err.println("  -m3 = report files that don't contain the search string");
    System.err.println("  -m4 = report file name and path only if string not found");
    System.err.println("  -m5 = report file name only (no path) if string not found");
    System.err.println("  -m6 = report all files as to whether string is found");
    System.err.println("  -n0 = keep <NUL> and <DEL> characters as file text (default)");
    System.err.println("  -n1 = -n = ignore <NUL> and <DEL> control characters");
    System.err.println("  -r0 = search string is plain Unicode text (default)");
    System.err.println("  -r1 = -r = search string is a Java regular expression");
    System.err.println("  -s0 = do only given files or folders, no subfolders (default)");
    System.err.println("  -s1 = -s = process files, folders, and subfolders");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println();
    System.err.println("Output may be redirected with the \">\" operator.  If no file or folder names");
    System.err.println("are given on the command line, then a graphical interface will open.  Java");
    System.err.println("may interpret special characters in command-line options or parameters.");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method


/*
  sortFileList() method

  When we ask for a list of files or subfolders in a directory, the list is not
  likely to be in our preferred order.  Java does not guarantee any particular
  order, and the observed order is whatever is supplied by the underlying file
  system (which can be very jumbled for FAT16/FAT32).  We would like the file
  names to be sorted, and since we recurse on subfolders, we also want the
  subfolders to appear in order.

  The caller's parameter may be <null> and this may happen if the caller asks
  File.listFiles() for the contents of a protected system directory.  All calls
  to listFiles() in this program are wrapped inside a call to us, so we replace
  a null parameter with an empty array as our result.
*/
  static File[] sortFileList(File[] input)
  {
    String fileName;              // file name without the path
    int i;                        // index variable
    TreeMap list;                 // our list of files
    File[] result;                // our result
    StringBuffer sortKey;         // created sorting key for each file

    if (input == null)            // were we given a null pointer?
      result = new File[0];       // yes, replace with an empty array
    else if (input.length < 2)    // don't sort lists with zero or one element
      result = input;             // just copy input array as result array
    else
    {
      /* First, create a sorted list with our choice of index keys and the File
      objects as data.  Names are sorted as files or folders, then in lowercase
      to ignore differences in uppercase versus lowercase, then in the original
      form for systems where case is distinct. */

      list = new TreeMap();       // create empty sorted list with keys
      sortKey = new StringBuffer(); // allocate empty string buffer for keys
      for (i = 0; i < input.length; i ++)
      {
        sortKey.setLength(0);     // empty any previous contents of buffer
        if (input[i].isDirectory()) // is this "file" actually a folder?
          sortKey.append("2 ");   // yes, put subfolders after files
        else                      // must be a file or an unknown object
          sortKey.append("1 ");   // put files before subfolders

        fileName = input[i].getName(); // get the file name without the path
        sortKey.append(fileName.toLowerCase()); // start by ignoring case
        sortKey.append(" ");      // separate lowercase from original case
        sortKey.append(fileName); // then sort file name on original case
        list.put(sortKey.toString(), input[i]); // put file into sorted list
      }

      /* Second, now that the TreeMap object has done all the hard work of
      sorting, pull the File objects from the list in order as determined by
      the sort keys that we created. */

      result = (File[]) list.values().toArray(new File[0]);
    }
    return(result);               // give caller whatever we could find

  } // end of sortFileList() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main FileSearch1 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == cancelButton)   // "Cancel" button
    {
      doCancelButton();           // stop opening files or folders
    }
    else if (source == caseCheckbox) // if uppercase differs from lowercase
    {
      caseFlag = caseCheckbox.isSelected();
    }
    else if (source == encodeDialog) // name of assumed character set encoding
    {
      /* Accept anything for a character set name, because availableCharsets()
      can be incomplete.  For example, MacRoman is optional on Java 1.4 for
      Windows and is missing from the official list even if installed.  Illegal
      names will generate an error message when they are first used. */

      encodeName = (String) encodeDialog.getSelectedItem();
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == fontNameDialog) // font name for output text area
    {
      /* We can safely assume that the font name is valid, because we obtained
      the names from getAvailableFontFamilyNames(), and the user can't edit
      this dialog field. */

      fontName = (String) fontNameDialog.getSelectedItem();
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == fontSizeDialog) // point size for output text area
    {
      /* We can safely parse the point size as an integer, because we supply
      the only choices allowed, and the user can't edit this dialog field. */

      fontSize = Integer.parseInt((String) fontSizeDialog.getSelectedItem());
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == nullCheckbox) // if <NUL> and <DEL> chars are ignored
    {
      nullFlag = nullCheckbox.isSelected();
    }
    else if (source == openButton) // "Open" button for files or folders
    {
      doOpenButton();             // open files or folders for processing
    }
    else if (source == recurseCheckbox) // recursion for folders, subfolders
    {
      recurseFlag = recurseCheckbox.isSelected();
    }
    else if (source == regexCheckbox) // if search is Java regular expression
    {
      regexFlag = regexCheckbox.isSelected();
    }
    else if (source == reportDialog) // select which files to report
    {
      reportIndex = reportDialog.getSelectedIndex();
    }
    else if (source == saveButton) // "Save Output" button
    {
      doSaveButton();             // write output text area to a file
    }
    else if (source == statusTimer) // update timer for status message text
    {
      if (statusPending.equals(statusDialog.getText()) == false)
        statusDialog.setText(statusPending); // new message, update the display
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method

} // end of FileSearch1 class

// ------------------------------------------------------------------------- //

/*
  FileSearch1User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class FileSearch1User implements ActionListener, Runnable
{
  /* empty constructor */

  public FileSearch1User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    FileSearch1.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    FileSearch1.doOpenRunner();
  }

} // end of FileSearch1User class

/* Copyright (c) 2007 by Keith Fenske.  Apache License or GNU GPL. */
