package com.shpp.p2p.cs.adavydenko.assignment15;

/**
 * This class manages all other classes in order to archive or unarchive a file.
 * First it processes user command line arguments, defines what kind of operation
 * is to be carried out, provides Archiver or Unarchiver object with input and output
 * files and carries out archiving / unarchiving operations. This is also followed
 * by printing messages to console about the files` sizes before and after, the time
 * it took to carry out this operation etc.
 * <p>
 * Following concepts were taken from external resources:
 * --- FileOutput- / FileInputStream
 * https://javarush.ru/groups/posts/2020-vvod-vihvod-v-java-klassih-fileinputstream-fileoutputstream-bufferedinputstream
 * --- Rounding of doubles using regex
 * https://javarush.ru/groups/posts/2773-okruglenie-chisel-v-java
 * --- Priority queue
 * https://docs.oracle.com/javase/7/docs/api/java/util/PriorityQueue.html
 * --- Binary trees
 * https://javarush.ru/groups/posts/3111-strukturih-dannihkh-dvoichnoe-derevo-v-java
 * --- Priority queue and objects comparison
 * https://www.freecodecamp.org/news/priority-queue-implementation-in-java/
 * --- Converting short numeric to byte array
 * http://www.java2s.com/Tutorials/Java/Data_Type/Array_Convert/Convert_short_to_byte_array_in_Java.htm
 * --- How to compare arraylists
 * https://howtodoinjava.com/java/collections/arraylist/compare-two-arraylists/
 */
public class Assignment15Part1 {

    /**
     * Number of seconds in a minute.
     */
    private static final double SEC_IN_MIN = 60;

    /**
     * One second.
     */
    private static final double ONE_SEC = 1;

    /**
     * Number of seconds in a minute.
     */
    private static final int MILLISEC_IN_SEC = 1000;

    /**
     * Saves the time when the program started functioning, processes
     * the command line arguments, provides them for further archiving /
     * unarchiving and prints to console the time it took the program
     * to archive / unarchive a file.
     *
     * @param args are command line arguments provided by user.
     */
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        try {
            ArgumentsHandler argHandler = new ArgumentsHandler();
            AuxiliaryMethods auxMethods = new AuxiliaryMethods();
            String[] filesNames = argHandler.decideOperationType(args); // inputFile (index 0), outputFile (index 1)
            launchFileProcessing(argHandler.archiveOperation, filesNames, auxMethods);
            displayOperationTime(startTime);
        } catch (Exception e) {
            System.out.println("You have an error in your arguments");
            e.printStackTrace();
        }
    }

    /**
     * Launches file archiving / unarchiving based on the results provided
     * by the ArgumentHandler.
     *
     * @param isArchiveOperation says whether the program shall archive the
     *                           input file (if true) or unarchive it (if false).
     * @param filesNames         is a string array containing an input file as its first
     *                           element and an output file as its second element.
     * @param auxMethods         an auxiliary class that performs functions common
     *                           to both archiver and unarchiver.
     */
    private static void launchFileProcessing(boolean isArchiveOperation, String[] filesNames,
                                             AuxiliaryMethods auxMethods) {
        if (isArchiveOperation) {
            System.out.println("Archiving " + filesNames[0] + " to " + filesNames[1]);
            Archiver archiver = new Archiver(filesNames, auxMethods);
            archiver.archiveFile();
        } else {
            System.out.println("Unarchiving " + filesNames[0] + " to " + filesNames[1]);
            Unarchiver unarchiver = new Unarchiver(filesNames, auxMethods);
            unarchiver.unarchiveFile();
        }
    }

    /**
     * Calculates how mane milliseconds / seconds / minutes and
     * second did it take the program to archive / unarchive a file.
     *
     * @param startTime is the time when the program started functioning.
     */
    private static void displayOperationTime(long startTime) {
        long endTime = System.currentTimeMillis();
        String timePhrase = "This operation took ";
        String time;
        long timeDiffInSeconds = (endTime - startTime) / MILLISEC_IN_SEC;
        if (timeDiffInSeconds >= SEC_IN_MIN) {
            time = ((int) Math.floor(timeDiffInSeconds / SEC_IN_MIN))
                    + " minute(s) " + ((int) timeDiffInSeconds - (Math.floor(timeDiffInSeconds / SEC_IN_MIN)
                    * SEC_IN_MIN)) + " second(s)";
        } else if (timeDiffInSeconds >= ONE_SEC) {
            time = (endTime - startTime) / MILLISEC_IN_SEC + " second(s)";
        } else {
            time = (endTime - startTime) + " millisecond(s)";
        }
        System.out.println(timePhrase + time);
    }
}
