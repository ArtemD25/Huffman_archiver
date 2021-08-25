package com.shpp.p2p.cs.adavydenko.assignment15;

/**
 * This class takes user arguments and defines whether it shall be
 * an archiving or an unarchiving operation, what are the input
 * and output files and what is their location.
 */
public class ArgumentsHandler {

    /**
     * A flag explicitly stating that shall be an archiving operation.
     */
    private static final String ARCHIVE_FLAG = "-a";

    /**
     * A flag explicitly stating that shall be an unarchiving operation.
     */
    private static final String UNARCHIVE_FLAG = "-u";

    /**
     * File extension for archived files.
     */
    private static final String P2P_ARCHIVED_TYPE = "par";

    /**
     * File extension for files whose original extension is unknown.
     */
    private static final String UNKNOWN_UNARCHIVED_TYPE = "uar";

    /**
     * A boolean flag stating for the main class that this shall
     * be an archiving operation.
     */
    protected boolean archiveOperation = false;

    /**
     * Name and location of the input file.
     */
    protected String inFile = "";

    /**
     * Name and location of the output file.
     */
    protected String outFile = "";

    /**
     * Takes user command line arguments, processes them and
     * returns back an array with input file having index 0
     * and the output file having index 1.
     *
     * @param args user command line arguments.
     * @return array with input file having index 0 and the
     * output file having index 1.
     */
    protected String[] decideOperationType(String[] args) {
        String DEFAULT_IN_FILE = "test.txt"; // Default name for the inFile if the user did not provide any
        String DEFAULT_OUT_FILE = "test.txt.par"; // Default name for the outFile if the user did not provide any inFile

        if (args.length == 3) {
            analyzeThreeArguments(args);
        } else if (args.length == 2) {
            analyzeTwoArguments(args);
        } else if (args.length == 1) {
            analyzeOneArgument(args);
        } else if (args.length == 0) {
            archiveOperation = true;
            inFile = DEFAULT_IN_FILE;
            outFile = DEFAULT_OUT_FILE;
        } else {
            System.out.println("You entered incorrect number of arguments");
            System.exit(-1);
        }
        return new String[]{inFile, outFile};
    }

    /**
     * Analyzes user command line arguments if there were three of them provided.
     *
     * @param args user command line arguments.
     */
    private void analyzeThreeArguments(String[] args) {
        inFile = args[1];
        outFile = args[2];
        if (args[0].equalsIgnoreCase(ARCHIVE_FLAG)) {
            archiveOperation = true;
        } else if (args[0].equalsIgnoreCase(UNARCHIVE_FLAG)) {
            archiveOperation = false;
        } else {
            System.out.println("You entered incorrect flag");
            System.exit(-1);
        }
    }

    /**
     * Analyzes user command line arguments if there were two of them provided.
     * If it is archiving operation and the output file gas no extension, the
     * program will make it ".par". If it is an unarchiving operation and the
     * output file has no extension, the program will make it ".uar".
     *
     * @param args user command line arguments.
     */
    private void analyzeTwoArguments(String[] args) {
        inFile = args[0];
        if (getFileType(args[0]).equalsIgnoreCase(P2P_ARCHIVED_TYPE)) {
            if (getFileType(args[1]).equals("no file type")) {
                outFile = args[1] + "." + UNKNOWN_UNARCHIVED_TYPE;
            } else {
                outFile = args[1];
            }
        } else {
            if (getFileType(args[1]).equals("no file type")) {
                outFile = args[1] + "." + P2P_ARCHIVED_TYPE;
            } else {
                outFile = args[1];
            }
            archiveOperation = true;
        }
    }

    /**
     * Analyzes user command line arguments if there was only one of them provided.
     *
     * @param args user command line arguments.
     */
    private void analyzeOneArgument(String[] args) {
        String inputFileType = getFileType(args[0]);
        inFile = args[0];

        // If it is not a par-file or if the file has no extension
        if (inputFileType.equals("no file type") || !inputFileType.equalsIgnoreCase(P2P_ARCHIVED_TYPE)) {
            outFile = args[0] + "." + P2P_ARCHIVED_TYPE;
            archiveOperation = true;
        } else { // If the input file is a par-file
            if (inputFileFormatIsKnown(args[0])) {
                outFile = getNewFileName(args[0], "");
            } else {
                outFile = getNewFileName(args[0], UNKNOWN_UNARCHIVED_TYPE);
            }
        }
    }

    /**
     * Says whether the program can define the extension of the input file.
     *
     * @param arg is a user command line argument provided to the program.
     * @return true if a file`s format is known and false if not.
     */
    private boolean inputFileFormatIsKnown(String arg) {
        // The length of ".par"
        int parExtensionLength = 4;
        String cutInputFile = arg.substring(0, arg.length() - parExtensionLength);
        return !getFileType(cutInputFile).equals("no file type");
    }

    /**
     * Creates new file name in cases a user provided only one command
     * line argument. If this argument has not extension at all, the
     * program will archive this file by default.
     *
     * @param arg         is user provided command line argument as a string.
     * @param newFileType is a file extension that the new file shall have.
     * @return the file submitted as command line argument with new file
     * extension provided as second parameter.
     */
    private String getNewFileName(String arg, String newFileType) {
        int pointIndex = 0;
        for (int i = arg.length() - 1; i > 0; i--) {
            if (arg.charAt(i) == '.') {
                pointIndex = i;
                break;
            }
        }
        if (newFileType.equals("")) {
            return arg.substring(0, pointIndex);
        }
        return arg.substring(0, pointIndex) + "." + newFileType;
    }

    /**
     * Gets the file type of a file. E.g. for "document.txt"
     * the method will return string with "txt" value. If a
     * file does not have any extension, the program returns
     * "no file type".
     *
     * @param inputFile is a random file`s name as a string.
     * @return provided file`s format or a phrase "no file type"
     * if the file has no extension.
     */
    private String getFileType(String inputFile) {
        // A string with file`s extension
        StringBuilder fileType = new StringBuilder();

        if (inputFile.contains(".")) {
            for (int i = inputFile.length() - 1; i > 0; i--) {
                if (inputFile.charAt(i) != '.') {
                    fileType.append(inputFile.charAt(i));
                } else {
                    fileType = fileType.reverse();
                    return fileType.toString();
                }
            }
        }
        return "no file type";
    }
}