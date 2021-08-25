package com.shpp.p2p.cs.adavydenko.assignment15;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * This class contains methods that are auxiliary to the archiving
 * and unarchiving operations and both those operations use them.
 */
public class AuxiliaryMethods {

    /**
     * Converts
     * Remember, you need to cast argument to byte each time
     * you are trying to convert.
     *
     * @param anyByte is any byte provided to this method.
     * @return a corresponding integer value to the byte
     * provided. E.g. int 4 corresponds byte 4 and int 255
     * corresponds byte 127.
     */
    protected int convertByteToInt(byte anyByte) {
        int byteEncodeCapacity = 256; // Number of values that can be encoded using one byte.
        int correspondingInt;
        if (anyByte >= 0) {
            correspondingInt = anyByte;
        } else {
            correspondingInt = anyByte + byteEncodeCapacity;
        }
        return correspondingInt;
    }

    /**
     * Provides the file size in the format convenient for reading.
     *
     * @param fileSize is the size of the file in bytes.
     * @return a string with a number of bytes, kilobytes etc depending
     * on how many bytes does the file actually have.
     */
    protected String getFileSize(long fileSize) {
        final String[] BYTES_CAPTION = {"bytes", "Kb", "Mb", "Gb", "Tb", "Pt"};
        final int NUM_OF_BYTES = 1024; // Number of bytes in one kilobyte.
        if (fileSize == 1) {
            return "1 byte";
        } else {
            for (int i = BYTES_CAPTION.length - 1; i >= 0; i--) {
                if (fileSize > Math.pow(NUM_OF_BYTES, i)) {
                    return String.format("%.2f", fileSize / Math.pow(NUM_OF_BYTES, i)) + " " + BYTES_CAPTION[i];
                }
            }
        }
        return null;
    }

    /**
     * Checks whether the file submitted is empty or does not exist.
     * If so, terminates the program and prints a message to console.
     * If not, shows both files and says what kind of operation is being carried out.
     *
     * @param file                 is the object managing the information about the
     *                             inputFile.
     * @param isArchivingOperation says whether the program shall archive the
     *                             input file (if true) or unarchive it (if false).
     * @param inFile               is the input file that shall ne either archived
     *                             or unarchived.
     * @param outFile              is the output file that shall be created by the
     *                             program as a result of the operation.
     */
    protected void checkForEmptyFile(File file, boolean isArchivingOperation, String inFile, String outFile) {
        if (file.length() == 0) {
            System.out.println("The file you specified does not exist or is empty. Process terminated");
            System.exit(-1);
        } else {
            if (isArchivingOperation) {
                System.out.println("Archiving " + inFile + " file to " + outFile);
            } else {
                System.out.println("Unarchiving " + inFile + " file to " + outFile);
            }
        }
    }

    /**
     * Prints to console the size of the unarchived / archived file and its size
     * compared to the archived / unarchived one.
     * <p>
     * 100 stands for 100 %.
     *
     * @param outFile              is the name and the location of the resulting file.
     * @param inFileSize           is the length of the
     * @param isArchivingOperation is the type of the operation. Can be either
     *                             archiving or unarchiving.
     */
    protected void displayOutFileSizeAndEffectiveness(String outFile, long inFileSize, boolean isArchivingOperation) {
        File file = new File(outFile);
        String effectivenessPercentage;
        if (inFileSize == 0) {
            effectivenessPercentage = "100"; // if the inFile is empty, the effectiveness is always 100
        } else {
            effectivenessPercentage = String.format("%.2f", file.length() * 100.0 / inFileSize);
        }
        String operationType, sourceFile, resultFile;

        if (isArchivingOperation) {
            operationType = "archiving";
            sourceFile = "unarchived";
            resultFile = "archived";
        } else {
            operationType = "unarchiving";
            sourceFile = "archived";
            resultFile = "unarchived";
        }
        System.out.println("- File size after " + operationType + ": " + getFileSize(file.length()) + "\nThe "
                + resultFile + " file is " + effectivenessPercentage + "% of the " + sourceFile + " file");
    }

    /**
     * Processes an empty file if user wants to archive / unarchive it.
     * Namely, prints corresponding message to console and creates an
     * empty outFile.
     *
     * @param OUT_FILE             is the name and location of the file to be created
     * @param isArchivingOperation is a boolean flag to show whether this operation
     *                             archives or unarchives the inFile
     */
    protected void processEmptyFile(String OUT_FILE, boolean isArchivingOperation) {
        String message;
        if (isArchivingOperation) {
            message = "Archived file is empty";
        } else {
            message = "Unarchived file is empty";
        }
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(OUT_FILE));
            System.out.println(message);
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates an empty par-archive / unarchived file and exits the program
     * if the file provided to the program is empty.
     *
     * @param inFile     is the file provided by user that shall either archived of unarchived.
     * @param inFileSize is the size of the source file (the file provided by user)
     * @param outFile    is the name and location of the file to be created
     * @return true if the file is empty. False if otherwise.
     */
    protected boolean processEmptyFileIfApplicable(File inFile, String outFile, long inFileSize, boolean isArchivingOperation) {
        if (!inFile.exists()) { // if the file does not exist
            System.out.println("The file you specified does not exist");
            System.exit(-1);
        } else if (inFile.length() == 0) { // if the inFile is empty
            System.out.println("Your file has less than 2 unique bytes. No Huffman encoding for you bro");
            processEmptyFile(outFile, isArchivingOperation);
            displayOutFileSizeAndEffectiveness(outFile, inFileSize, isArchivingOperation);
            return true;
        }
        return false;
    }

    /**
     * Creates a copy of an ArrayList.
     *
     * @param sourceArray is the array which contents shall be copied.
     * @return the copy of the source array, byt with a different link.
     */
    protected ArrayList<Byte> getArrayCopy(ArrayList<Byte> sourceArray) {
        ArrayList<Byte> copyArray = new ArrayList<>();
        copyArray.addAll(sourceArray);
        return copyArray;
    }
}
