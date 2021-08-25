package com.shpp.p2p.cs.adavydenko.assignment15;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * The program extracts the first two bytes from the archive and determines the
 * length of the tree structure in bits. The next (third) byte determines the
 * number of meaningful bits in the last byte of the archive. The next bulk of
 * bytes stands for the encoded structure of the tree. The program knows how many
 * bytes are allocated to this structure after analyzing the first two bytes of
 * the archive. After analyzing the structure, the program knows how many unique
 * bytes are in the archive and extracts them all into a separate array. The program
 * then extracts the remaining bytes and decodes them.
 */
public class Unarchiver {

    /**
     * The name and the location of the archived file
     * the program shall to unarchive.
     */
    private final String IN_FILE;

    /**
     * The name and the location of the file the
     * program shall write the decoded bytes from
     * the archived file to.
     */
    private final String OUT_FILE;

    /**
     * An instance of a class containing auxiliary methods helping other
     * classes to conduct operations (like converting byte to int etc).
     */
    private final AuxiliaryMethods AUX_METHODS;

    /**
     * Size of the file to be unarchived in bytes.
     */
    private long inFileSize = 0;

    /**
     * The size of the buffer used to store bytes from the inFile
     */
    private final int BUFFER_SIZE = 32 * 1024;

    /**
     * The size of the tree in bits.
     */
    private short treeSizeInBits;

    /**
     * Number of meaningful bits in the last encoded byte of the archive.
     * In other words the number of bits in that byte that shall be decoded.
     */
    private int numOfMeaningfulBitsInLastByte;

    /**
     * Buffer to store decoded bytes from the archive and to write them later
     * to an (unarchived) output file
     */
    private final byte[] OUT_BUFFER = new byte[BUFFER_SIZE];

    /**
     * Indicates that this extracted buffer from the archive is the last one.
     * This boolean flag is needed to detect tha last buffer so that the
     * program knows how many bits shall it decode in the last byte
     * of the last buffer.
     */
    private boolean isLastExtractedBuffer = false;

    /**
     * The number of bytes already written to the OUT_BUFFER.
     * If it equals to the size of the OUT_BUFFER, it means
     * the OUT_BUFFER is full and shall be written to the
     * outFile (unarchived file).
     */
    private int bytesAlreadyInOutBuffer = 0;

    /**
     * Bits that are extracted from the inFile. The program looks for exactly the same
     * sequences of bits in the BYTE_CODES HashMap.
     */
    private final ArrayList<Byte> EXTRACTED_BITS = new ArrayList<>();

    /**
     * The tree that is formed of all unique inFile`s bytes.
     */
    private Node tree;

    /**
     * An ArrayDeque with all bits representing the tree structure.
     * Is used to build a tree, that was used to created this archive.
     */
    private ArrayDeque<Byte> queueFromTreeStructureBits = new ArrayDeque<>();

    /**
     * The number of leaves that are already placed to the tree plus 1.
     * If the leafIndex equals 10, this mean there are already 11 leaves
     * in the constructed tree.
     */
    private int leafIndex = 0;

    /**
     * Stack to store nodes while creating a tree based on bits describing this tree structure.
     */
    private final Stack<Node> NODE_STACK = new Stack<>();

    /**
     * Index of the first byte in the archive that has encoded bits in it.
     */
    private int indexOfFirstByteWithEncodedBits;

    /**
     * Array with all unique bytes from archive.
     */
    private byte[] uniqueBytesFromArchive;

    /**
     * The index of the first byte in the archive that stands for
     * a unique byte encoded in the archive.
     */
    private int indexOfFirstUniqueEncodedByte;

    /**
     * Number of the unique bytes encoded in the archive.
     */
    private int numOfUniqueBytesInArchive = 0;

    /**
     * An array with separate bits describing the tree structure.
     */
    private byte[] treeStructureBits;

    /**
     * Boolean flag saying that the first tree element was not
     * visited yet. This variable helps the algorithm not to stop
     * on the very beginning when the stack is still empty and there
     * is the only node in the tree.
     */
    private boolean firstNodeVisited = false;

    /**
     * An array with all visited nodes. Is used while the program
     * recursively visits all tree nodes. Nodes are put to this
     * array for the program to know that these nodes shall not be
     * visited again.
     */
    private final ArrayList<Node> VISITED_NODES = new ArrayList<>();

    /**
     * HashMap with all unique bytes of the inFile and their codes presented
     * as array of bytes.
     */
    private final HashMap<Byte, ArrayList<Byte>> BYTE_CODES = new HashMap<>();

    /**
     * The number of minimum needed bits to encode one byte
     */
    private int minNumOfBitsToEncodeByte = 0;

    /**
     * Gets the name and the location of an archive and the archive that shall
     * be unarchived as well as the link to the AuxiliaryMethods that provides
     * the Unarchiver class with possibility to access the auxiliary methods in
     * the AuxiliaryMethods class.
     *
     * @param files      an array with archived file name and location
     *                   with index 0 and the future unarchived file
     *                   name and location with index 1.
     * @param auxMethods an auxiliary class that performs functions common
     *                   to both archiver and unarchiver.
     */
    public Unarchiver(String[] files, AuxiliaryMethods auxMethods) {
        this.IN_FILE = files[0];
        this.OUT_FILE = files[1];
        this.AUX_METHODS = auxMethods;
    }

    /**
     * Gets the archived file, checks it for being an empty file,
     * defines its size and unarchives its content.
     */
    protected void unarchiveFile() {
        File inFile = new File(IN_FILE);

        if (AUX_METHODS.processEmptyFileIfApplicable(inFile, OUT_FILE, inFileSize, false)) {
            return;
        }

        defineArchivedFileSize(inFile);
        processFile(inFile);
        AUX_METHODS.displayOutFileSizeAndEffectiveness(OUT_FILE, inFileSize, false);
    }

    /**
     * Creates objects that read and write bytes, reads all bytes and closes
     * the above mentioned objects.
     *
     * @param inFile is the file provided by user that shall unarchived.
     */
    private void processFile(File inFile) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inFile));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(OUT_FILE));
            readBytes(inFile, bis, bos);
            bis.close();
            bos.close();
        } catch (Exception e) {
            System.exit(-1);
        }
    }

    /**
     * Reads all bytes from the archive file, decodes them and calculates
     * variables based on the information gathered.
     *
     * @param inFile is the file provided by user that shall unarchived.
     * @param bis    is an object reading bytes from an archive.
     * @param bos    is an object writing bytes to an output File.
     */
    private void readBytes(File inFile, BufferedInputStream bis, BufferedOutputStream bos) {
        try {
            int numOfBuffersNeededToExtract = (int) Math.ceil(inFile.length() / (double) BUFFER_SIZE);
            byte[] inBuffer = new byte[BUFFER_SIZE]; // Buffer with bytes from the source file that shall be archived
            int len;
            int timesBufferExtracted = 0; // The number of times the program filled buffer and provided those data further
            while ((len = bis.read(inBuffer)) > 0) {
                timesBufferExtracted++;
                if (timesBufferExtracted == numOfBuffersNeededToExtract) { // If it is the last buffer to be extracted
                    isLastExtractedBuffer = true;
                }
                if (timesBufferExtracted == 1) { // If it is the first extracted buffer from the archive
                    prepareForArchiveDecoding(inBuffer);
                }
                decodeBytes(inBuffer, len, bos);
            }

            if (bytesAlreadyInOutBuffer > 0) { // If there are some bytes in the OUT_BUFFER left that are not written to the archive yet
                bos.write(OUT_BUFFER, 0, bytesAlreadyInOutBuffer);
            }
            bis.close();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Makes all preparations before actually decoding the archived file.
     * Namely gets the number of bits describing tree structure, the number
     * of meaningful bits in the last archive`s byte, the tree structure itself
     * and all unique bytes.
     *
     * @param inBuffer is a buffer with bytes from the source file that shall be archived.
     */
    private void prepareForArchiveDecoding(byte[] inBuffer) {
        getTreeSizeInBits(inBuffer);
        getNumOfMeaningfulBitsInLastByte(inBuffer);
        getTreeStructure(inBuffer);
        countUniqueBytesInArchive();
        getUniqueBytesFromArchive(inBuffer);
        buildTree();
        fillHashMapWithCodes();
        getMinNumOfBitsToEncodeByte();
    }

    /**
     * Counts the number of minimum needed bits to encode one byte
     */
    private void getMinNumOfBitsToEncodeByte() {
        boolean firstByteChecked = false;
        for (Map.Entry<Byte, ArrayList<Byte>> entry : BYTE_CODES.entrySet()) {
            if (!firstByteChecked) {
                minNumOfBitsToEncodeByte = entry.getValue().size();
                firstByteChecked = true;
            }
            if (entry.getValue().size() < minNumOfBitsToEncodeByte) {

                minNumOfBitsToEncodeByte = entry.getValue().size();
            }
        }
    }

    /**
     * Loads a buffer of encoded bytes from the archive file. Iterates through each
     * of those bytes, extracts separate bits from those bytes and looks in the
     * BYTE_CODES hashmap whether there is any byte with such code (consisting of
     * separate bits taken from an encoded byte).
     *
     * @param inBuffer is an array of bytes extracted from the archived file.
     * @param len      is the number of bytes written to the buffer (inBuffer)
     * @param bos      is an object writing bytes to an output File.
     */
    private void decodeBytes(byte[] inBuffer, int len, BufferedOutputStream bos) {
        int offset;

        for (int i = indexOfFirstByteWithEncodedBits; i < len; i++) { // Iterates through the inBuffer
            byte currentEncodedByte = inBuffer[i]; // An encoded byte from the archived file
            int iterations = getIterations(len, i); // How many bits shall the program extract from the currentEncodedByte

            for (int j = 0; j < iterations; j++) { // Iterates through a byte taken from the inBuffer
                offset = Byte.SIZE - (1 + j);
                byte mask = (byte) (1 << offset); // mask to get needed bit out of a byte
                byte extractedBit = (byte) (((currentEncodedByte & mask) >> offset) & 1);
                EXTRACTED_BITS.add(extractedBit); // Extracts a bit from a byte, adds it to the ArrayList EXTRACTED_BITS

                Byte result = getByteBasedOnCode(EXTRACTED_BITS); // Looks for such array in BYTE_CODES arrays
                writeByteToBufferIfDecoded(result, bos);
            }
        }
        indexOfFirstByteWithEncodedBits = 0; // resets this value for all following buffers for them to be read starting from their first element
    }

    /**
     * Writes decoded byte (if decoded it) to buffer in order to write this
     * buffer later to the unarchived file.
     *
     * @param result is the result of decoding a byte. If not decoded, result
     *               equals null. If decoded, it equals to some byte value.
     * @param bos    is an object writing bytes to an output File.
     */
    private void writeByteToBufferIfDecoded(Byte result, BufferedOutputStream bos) {
        if (result != null) { // if returns not null - saves the value returned to OUT_BUFFER
            OUT_BUFFER[bytesAlreadyInOutBuffer] = result;
            bytesAlreadyInOutBuffer++;
            writeBytesArrayToUnarchivedIfApplicable(bos);
            EXTRACTED_BITS.clear(); // clear the array if already found the encoded byte
        } // if returns null - extracts new bit and looks for such bits sequence again in the decodeBytes method
    }

    /**
     * Counts the number of iterations for a particular encoded byte from the archive.
     * This method is used to no to encode those bits from the last archive`s byte,
     * which are meaningless and were not used to encode information.
     *
     * @param len is the number of bytes written to the buffer.
     * @param i   is the index of a byte from the buffer that is being decoded.
     * @return the number of iterations.
     */
    private int getIterations(int len, int i) {
        int iterations;
        if (i == (len - 1) && isLastExtractedBuffer) { // If it is the last byte in the last extracted buffer
            iterations = numOfMeaningfulBitsInLastByte;
        } else {
            iterations = Byte.SIZE;
        }
        return iterations;
    }

    /**
     * If the outBuffer is full (full with decoded bytes) the program writes it to the
     * unarchived file.
     *
     * @param bos is an object writing bytes to an output File.
     */
    private void writeBytesArrayToUnarchivedIfApplicable(BufferedOutputStream bos) {
        if (bytesAlreadyInOutBuffer == OUT_BUFFER.length) {
            try {
                bos.write(OUT_BUFFER, 0, bytesAlreadyInOutBuffer);
                bytesAlreadyInOutBuffer = 0; // resets to zero the number of bytes written to OUT_BUFFER
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets an array with bits and compares it with all all codes (all arrays with bits)
     * in the hashmap. If found one, returns the byte value of such corresponding array.
     *
     * @param encodedArray is an array of 1 and 0 that all together might or might not
     *                     stand for a code for a byte.
     * @return byte value if there is any byte in the BYTE_CODES hashmap with such code.
     * If there is not such any, returns null.
     */
    private Byte getByteBasedOnCode(ArrayList<Byte> encodedArray) {
        if (encodedArray.size() < minNumOfBitsToEncodeByte) {
            return null;
        }
        for (Map.Entry<Byte, ArrayList<Byte>> entry : BYTE_CODES.entrySet()) {
            if (encodedArray.equals(entry.getValue()))
                return entry.getKey();
        }
        return null;
    }

    /**
     * Converts an array to an ArrayDeque.
     *
     * @param array is any byte[] array provided to this method.
     * @return an ArrayDequeue with the same content as the byte[] array provided.
     */
    private ArrayDeque<Byte> getArrayDeque(byte[] array) {
        ArrayDeque<Byte> outputArray = new ArrayDeque<>();
        for (byte currentByte : array) {
            outputArray.add(currentByte);
        }
        return outputArray;
    }

    /**
     * Builds a tree using the treeStructureBits array. Each "1" stands for a node
     * without children and each "0" stands for a leaf.
     */
    private void buildTree() {
        // An ArrayDeque with all bits representing the tree structure
        queueFromTreeStructureBits = getArrayDeque(treeStructureBits);

        byte treeStructureBit = queueFromTreeStructureBits.pollFirst(); // A bit we took from the queueFromUniqueBytes, an array describing the tree structure
        Node newNode;

        if (treeStructureBit == 1) { // If there are 2 or more nodes in the tree
            newNode = new Node(0);
            tree = newNode;
            NODE_STACK.push(tree);
            createNodes();
        } else { // If the tree consists of only one node
            newNode = new Node(uniqueBytesFromArchive[leafIndex], 0);
            tree = newNode;
        }
    }

    /**
     * Takes each bit (1 or 0) form the queueFromTreeStructureBits and builds
     * a tree. "1" stands for a regular node with two children, "0" stands
     * for a leaf.
     */
    private void createNodes() {
        while (queueFromTreeStructureBits.size() > 0) { // While there is at least one bit left that encodes the tree structure left...
            byte treeStructureBit = queueFromTreeStructureBits.pollFirst(); // A bit we took from the queueFromUniqueBytes, an array describing the tree structure
            Node newNode = createLeafOrRegularNode(treeStructureBit); // create new node and fill it with data

            if (NODE_STACK.peek().getLeftChild() == null) { // If the the top most node in the stack does not have any left child
                NODE_STACK.peek().setLeftChild(newNode);
            } else if (NODE_STACK.peek().getRightChild() == null) { // If the top most node in the stack does not have any right child
                NODE_STACK.peek().setRightChild(newNode);
                NODE_STACK.pop(); // if this node has both left and right children, it has to be deleted from the stack
            }

            if (treeStructureBit == 1) { // if it is a regular node (not a leaf), add it to the stack
                NODE_STACK.push(newNode);
            }
        }
    }

    /**
     * Creates a leaf of a regular node with both children depending
     * on the treeStructureBit. If "1", this will be a node, if "0",
     * this will be a leaf.
     *
     * @param treeStructureBit a bit extracted from the queueFromTreeStructureBits
     *                         ArrayDeque
     * @return a new created node
     */
    private Node createLeafOrRegularNode(byte treeStructureBit) {
        Node newNode;
        if (treeStructureBit == 1) {
            newNode = new Node(0);
        } else { // if treeStructureBit == 0
            newNode = new Node(uniqueBytesFromArchive[leafIndex], 0);
            leafIndex++;
        }
        return newNode;
    }

    /**
     * Gets all unique bytes that are encoded in the archive and
     * saves them to an array (uniqueBytesFromArchive)
     *
     * @param buffer is a first buffer of bytes extracted from the archive
     */
    private void getUniqueBytesFromArchive(byte[] buffer) {
        indexOfFirstByteWithEncodedBits = indexOfFirstUniqueEncodedByte + numOfUniqueBytesInArchive;
        uniqueBytesFromArchive = new byte[numOfUniqueBytesInArchive];

        for (int i = indexOfFirstUniqueEncodedByte; i < indexOfFirstUniqueEncodedByte + numOfUniqueBytesInArchive; i++) {
            uniqueBytesFromArchive[i - indexOfFirstUniqueEncodedByte] = buffer[i];
        }
    }

    /**
     * Counts the number of unique bytes in the archive.
     */
    private void countUniqueBytesInArchive() {
        for (byte treeStructureBit : treeStructureBits) {
            if (treeStructureBit == 0) {
                numOfUniqueBytesInArchive++;
            }
        }
    }

    /**
     * Gets bytes from the archive that explain the tree structure. After that
     * the method splits all these bytes in separate bites and writes to an array.
     * <p>
     * The iteration starts with index 3 since bytes with index 0 and 1 stand for
     * the size of the tree in bits and the byte with index 2 stands for the number
     * of meaningful bits in the last archive`s byte.
     *
     * @param buffer is a first buffer of bytes extracted from the archive
     */
    private void getTreeStructure(byte[] buffer) {
        // The number of bytes oh had to use to encode the tree structure in teh archive
        int bytesStandingForTreeStructure = (int) Math.ceil(treeSizeInBits / (double) Byte.SIZE);
        // An array with all bytes describing the tree structure
        byte[] treeStructureBytes = new byte[bytesStandingForTreeStructure];
        // The index of the first byte in the archive that stands for a unique byte encoded in the archive
        indexOfFirstUniqueEncodedByte = bytesStandingForTreeStructure + 3;

        for (int i = 3; i < bytesStandingForTreeStructure + 3; i++) {
            treeStructureBytes[i - 3] = buffer[i];
        }
        divideBytesIntoSeparateBits(treeStructureBytes);
    }

    /**
     * Converts all bytes standing for tree structure and extracts separate bits
     * and only those bits standing for the actual tree size (according to the
     * treeSizeInBits value).
     *
     * @param treeStructureBytes is an array with all bytes describing the tree structure
     */
    private void divideBytesIntoSeparateBits(byte[] treeStructureBytes) {
        treeStructureBits = new byte[treeSizeInBits];
        String result = ""; // String where the program writes all bytes from the treeStructureBytes as separate bytes

        for (byte treeStructureByte : treeStructureBytes) {
            result += byteToString(treeStructureByte);
        }

        char[] separateChars = result.toCharArray();
        createTreeStructureBitsArray(separateChars);
    }

    /**
     * Adds 1 or 0 to the treeStructureBits array depending on the
     * char value extracted from the char[] separateChars array.
     *
     * @param separateChars is an array of chars (1 or 0) that where
     *                      created by extracting the bytes from the
     *                      archive standing for tree structure.
     */
    private void createTreeStructureBitsArray(char[] separateChars) {
        final int ASCII_1 = 49; // ascii code for 1
        final int ASCII_0 = 48; // ascii code for 0
        char char1 = (char) ASCII_1;
        char char0 = (char) ASCII_0;

        for (int i = 0; i < treeStructureBits.length; i++) {
            Character character = separateChars[i];
            if (character.equals(char1)) {
                treeStructureBits[i] = 1;
            } else if (character.equals(char0)) {
                treeStructureBits[i] = 0;
            }
        }
    }

    /**
     * Converts byte to String
     *
     * @param anyByte is any byte provided to this method.
     * @return a byte in a form of a String.
     */
    private String byteToString(byte anyByte) {
        return String.format("%8s", Integer.toBinaryString(anyByte & 0xFF)).replace(' ', '0');
    }

    /**
     * Gets the number of meaningful bits in the last byte. In other words
     * the number of bits in the last archive byte that shall be decoded.
     * <p>
     * "2" stands for the index of a byte in the archive that stands for the
     * number of meaningful bits in the last byte
     *
     * @param buffer is the first buffer of bytes extracted from the archive
     */
    private void getNumOfMeaningfulBitsInLastByte(byte[] buffer) {
        numOfMeaningfulBitsInLastByte = buffer[2];
    }

    /**
     * Extracts first two bytes from the archive, converts them to
     * a numeric (of a short type). This numeric stands for the tree
     * size in bits.
     * <p>
     * Numeric 2 is used since 2 bytes are used to describe the tree
     * size in bits
     * Numeric 0 and 1 are used since the very first byte and the second
     * byte in the archive encode information on the tree size.
     *
     * @param buffer is the first buffer of bytes extracted from the archive
     */
    private void getTreeSizeInBits(byte[] buffer) {
        byte[] treeSizeInBitsArray = new byte[2];
        treeSizeInBitsArray[0] = buffer[0];
        treeSizeInBitsArray[1] = buffer[1];
        ByteBuffer buffer2 = ByteBuffer.wrap(treeSizeInBitsArray);
        treeSizeInBits = buffer2.getShort();
    }

    /**
     * Defines the size of the archived file in bytes and prints
     * it to console.
     *
     * @param file is the object managing the information about the
     *             archived inputFile.
     */
    private void defineArchivedFileSize(File file) {
        inFileSize = file.length();
        System.out.println("- File size before unarchiving: " + AUX_METHODS.getFileSize(inFileSize));
    }

    /**
     * Visits all tree nodes in order to collect all unique bytes
     * and their new codes. Bytes as a key and their codes as a values
     * (in form of an array) are put to a hashmap.
     */
    private void fillHashMapWithCodes() {
        ArrayList<Byte> newByteCode2 = new ArrayList<>();
        resetVariables();

        if (uniqueBytesFromArchive.length == 1) {
            newByteCode2.add((byte) 0); // if there is only one unique byte, the code for it will be 0
            BYTE_CODES.put(tree.value, AUX_METHODS.getArrayCopy(newByteCode2));
        } else { // if there are 2 or more unique bytes
            collectNewCodes(tree, newByteCode2);
        }
    }

    /**
     * Visits every tree node and saves bytes and their new codes to a hashmap.
     *
     * @param anyNode      is any tree node
     * @param newByteCode2 is an array of "1" and "0" used to encode a byte while archiving
     */
    private void collectNewCodes(Node anyNode, ArrayList<Byte> newByteCode2) {
        Node leftChild = anyNode.getLeftChild();
        Node rightChild = anyNode.getRightChild();

        while ((NODE_STACK.size() != 0 || anyNodeLeftToVisit(leftChild, rightChild)) || !firstNodeVisited) {
            firstNodeVisited = true;
            createHashMapItem(anyNode, newByteCode2);

            if (!VISITED_NODES.contains(anyNode)) {
                VISITED_NODES.add(anyNode);
            }

            if (leftChild != null && !VISITED_NODES.contains(leftChild)) {
                newByteCode2.add((byte) 0);
                NODE_STACK.push(anyNode);
                collectNewCodes(leftChild, newByteCode2);
            } else if (rightChild != null && !VISITED_NODES.contains(rightChild)) {
                newByteCode2.add((byte) 1);
                NODE_STACK.push(anyNode);
                collectNewCodes(rightChild, newByteCode2);
            } else {
                if (NODE_STACK.size() != 0) {
                    newByteCode2.remove(newByteCode2.size() - 1);
                    Node someNode = NODE_STACK.peek();
                    NODE_STACK.pop();
                    collectNewCodes(someNode, newByteCode2);
                }
            }
        }
    }

    /**
     * If the current node is a leaf, the method adds it to the hashmap
     * containing all unique bytes and the codes used to encode them
     * while archiving.
     *
     * @param anyNode      is any node of the tree.
     * @param newByteCode2 is an array of "1" and "0" used to encode a
     *                     byte while archiving
     */
    private void createHashMapItem(Node anyNode, ArrayList<Byte> newByteCode2) {
        if (isLeaf(anyNode)) {
            BYTE_CODES.put(anyNode.value, AUX_METHODS.getArrayCopy(newByteCode2));
        }
    }

    /**
     * Says whether a particular node is a leaf (has no children) or is a
     * regular node (has both children).
     *
     * @param anyNode is any node provided to the method.
     * @return true is the provided node is a leaf. False if otherwise.
     */
    private boolean isLeaf(Node anyNode) {
        return anyNode.getLeftChild() == null && anyNode.getRightChild() == null;
    }

    /**
     * Says whether there are any nodes from the point of view of the current node
     * that the program still can visit. The algorithm checks whether a particular
     * child-node of the current node exists and whether the program has not visited
     * it yet.
     *
     * @param leftChild  is the left child-node of the current node.
     * @param rightChild is the right child-node of the current node.
     * @return true if there is at least one node from the two provided that can be
     * visited by the program.
     */
    private boolean anyNodeLeftToVisit(Node leftChild, Node rightChild) {
        return !((leftChild == null || VISITED_NODES.contains(leftChild))
                && (rightChild == null || VISITED_NODES.contains(rightChild)));
    }

    /**
     * Resets three variables: sets the firstNodeVisited to false
     * and clears the NODE_STACK stack and the VISITED_NODES ArrayList
     * as well. This is needed because otherwise several method will
     * not provide a correct result if these three variables are
     * filled with some values from the previous operation.
     */
    private void resetVariables() {
        firstNodeVisited = false;
        NODE_STACK.clear();
        VISITED_NODES.clear();
    }
}
