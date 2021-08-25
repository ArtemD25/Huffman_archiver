package com.shpp.p2p.cs.adavydenko.assignment15;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Gets the name and location of two files: the one to be archived and
 * the one to be created as an archive.  Analyzes the frequency of occurrence
 * of different bytes in the file. Based on each byte, a node object is
 * created, which stores information about the byte itself and the frequency
 * of its occurrence in the input file. All these nodes are added to the
 * priority queue. Based on the nodes that are in the priority queue,
 * the program builds a tree. Depending on the location of the nodes with
 * bytes (leaves) in this tree, each leaf-node (which stores information
 * about a specific byte) receives a new code. The program writes the following
 * information in the archive:
 * --- the number of bits needed to describe the structure of the tree (2 bytes)
 * --- the number of bits in the last byte of the archive, which contain information
 * about encoded bytes from the original file (1 byte)
 * --- tree structure (maximum 64 bytes)
 * --- all unique bits in the input file in the order in which they occur in the
 * tree when traversing the tree (always first the left leaf, than the right,
 * maximum 256 bytes)
 * --- the source file itself encoded using the new codes
 */
public class Archiver {

    /**
     * Name and location of the input file to be archived.
     */
    private final String IN_FILE;

    /**
     * Name and location of the output file, that will be a result
     * of the IN_FILE archiving.
     */
    private final String OUT_FILE;

    /**
     * HashMap with all unique bytes from the inFile as a key
     * and the number of times those bytes appear in the inFile
     * as a value;
     */
    private final HashMap<Byte, Integer> BYTE_FREQUENCY = new HashMap<>();

    /**
     * A priority queue used to store nodes. Each node stands for a unique
     * byte in the source file. Each node data on the byte it represents /
     * and how many times does this byte occur in the source file. Node
     * objects are sorted in the priority queue according to the frequency
     * of their occurrence in the source file.
     */
    private final PriorityQueue<Node> PRIORITY_QUEUE = new PriorityQueue<>();

    /**
     * The tree that is formed of all unique inFile`s bytes.
     */
    private Node tree;

    /**
     * An instance of a class containing auxiliary methods helping other
     * classes to conduct operations (like converting byte to int etc).
     */
    private final AuxiliaryMethods AUX_METHODS;

    /**
     * An array with all visited nodes. Is used while the program
     * recursively visits all tree nodes. Nodes are put to this
     * array for the program to know that these nodes shall not be
     * visited again.
     */
    private final ArrayList<Node> VISITED_NODES = new ArrayList<>();

    /**
     * Is the stack with nodes that shall be visited. This stack
     * is used to visit all nodes of the tree by mean of recursion.
     */
    private final Stack<Node> NODE_STACK = new Stack<>();

    /**
     * The size of the tree. This value is written to the archive always
     * as first two bytes.
     */
    private short treeSize;

    /**
     * Boolean flag saying that the first tree element was not
     * visited yet. This variable helps the algorithm not to stop
     * on the very beginning when the stack is still empty and there
     * is the only node in the tree.
     */
    private boolean firstNodeVisited = false;

    /**
     * An array describing the structure of the tree. The program visits
     * all tree nodes: goes from the top to the most left child of
     * all left children. If there no more left children, the program
     * goes back and looks for the nearest yet not visited right child.
     * When it found such node, the program tries to find more left children
     * and if did not find any - looks again for right ones and so on until
     * thw whole tree is not visited.
     */
    private final ArrayList<Byte> TREE_STRUCTURE = new ArrayList<>();

    /**
     * An array describing which leaf in the tree stands for which byte.
     * The order of zeros in the TREE_STRUCTURE array corresponds to the
     * order of bytes in the ENCODED_BYTES array. E.g. if the tree structure
     * is 10100 and the encoded bytes stand for EFD, it means that the first
     * zero in the TREE_STRUCTURE stands for "E", the second one - for "F" and
     * the third one - for "D". All ones in the TREE_STRUCTURE stand for regular
     * nodes (not leaves) and do not encode any bytes.
     */
    private final ArrayList<Byte> ENCODED_BYTES = new ArrayList<>();

    /**
     * Is a copy of the already filled ENCODED_BYTES. It is created in order to
     * write all these bytes to archive since one can not write an ArrayList to it.
     */
    private byte[] encodedBytesArray;

    /**
     * The bits in these byte explain the structure of the tree that is encoded in
     * the archive. Each 1 stands for a node with children. Each 0 stands for a leaf
     * (a node without any children).
     */
    private byte[] treeStructureBytes;

    /**
     * HashMap with all unique bytes of the inFile and their codes presented
     * as array of bytes.
     */
    private final HashMap<Byte, ArrayList<Byte>> BYTE_CODES = new HashMap<>();

    /**
     * The size of the buffer used by the program
     */
    private final int BUFFER_SIZE = 32 * 1024;

    /**
     * An array with bytes that shall be written to an archive.
     */
    private final byte[] BYTES_TO_WRITE_TO_ARCHIVE = new byte[BUFFER_SIZE];

    /**
     * The number of bytes in the BYTES_TO_WRITE_TO_ARCHIVE that shall
     * be written to the archive file.
     */
    private int indexToWriteNewByte = 0;

    /**
     * Bytes where the program will write encoded bits sequences to. When full,
     * this byte will be written to the BYTES_TO_WRITE_TO_ARCHIVE array
     */
    private byte byteToWriteBitsTo = 0;

    /**
     * The number of bits (starting from the right) in this byte that are already
     * used. This means that the program already wrote some bits to them and they
     * are not empty.
     */
    private int alreadyWrittenBitsInThisByte = 0;

    /**
     * Offset is the position in the encoded byte to start writing encoded bits.
     * E.g. if there is encoded byte 11100000 where "111" in the beginning are some
     * encoded bits and one shall add another encoded bits (011 for example), than the
     * offset shall be 3 in order to write 011 to this archive byte and to get 11101100 in result.
     * <p>
     * Offset always takes into account
     * --- the number of bits in the new byte
     * --- the number of bits that already have been written tpo the archive
     */
    private int offset;

    /**
     * The total number of bits needed to encode the inFile`s each particular byte.
     * If the program encodes byte "a" with 010 sequence, the numOfBits equals 3.
     * If the program encodes byte "g" with 1100 sequence, the numIfBits equals 4.
     */
    private long numOfBits = 0;

    /**
     * Number of steps to be done to get to this particular leaf if starting from the
     * root node. In other words the number of bits needed to encode this particular byte
     */
    private int numOfSteps = 0;

    /**
     * Gets the name and the location of the file that shall be converted to
     * an archive and the archive that shall be created as well as the link to
     * the AuxiliaryMethods that provides the Archiver class with possibility
     * to access the auxiliary methods in the AuxiliaryMethods class.
     *
     * @param files      an array with source file name and location
     *                   with index 0 and the future archived file
     *                   name and location with index 1.
     * @param auxMethods an auxiliary class that performs functions common
     *                   to both archiver and unarchiver.
     */
    public Archiver(String[] files, AuxiliaryMethods auxMethods) {
        this.IN_FILE = files[0];
        this.OUT_FILE = files[1];
        this.AUX_METHODS = auxMethods;
    }

    /**
     * Archives the inFile by analyzing all unique bytes and the number of times they
     * appear in the inFile, building a tree to get new codes for those bytes and
     * writing encoded bytes to archive along with a portion of service information.
     */
    protected void archiveFile() {
        File inFile = new File(IN_FILE);
        final long IN_FILE_SIZE = inFile.length(); // Defines the size of the input file in bytes
        System.out.println("- File size before archiving: " + AUX_METHODS.getFileSize(IN_FILE_SIZE));
        if (AUX_METHODS.processEmptyFileIfApplicable(inFile, OUT_FILE, IN_FILE_SIZE, true)) {
            return;
        }
        countByteFrequency(inFile);
        formPriorityQueue();
        buildTree();
        formTreeStructureAndEncodedBytesArray();
        createByteArray();
        treeStructureBytes = writeTreeStructure();
        countBitsOfEncodedBytes();
        fillHashMapWithCodes();
        writeArchive();
        AUX_METHODS.displayOutFileSizeAndEffectiveness(OUT_FILE, IN_FILE_SIZE, true);
    }

    /**
     * Writes bytes to archive. Namely, two first bytes stand for the tree size.
     * One byte after that stand for the number of meaningful bits in the last
     * encoded byte of the archive. Following bytes stand for the tree structure.
     * The bytes after that are the sequence of unique bytes from the inFile.
     * All bytes after that stand for encoded inFile bytes. The method creates
     * the last group of bytes and writes them to the archive.
     */
    private void writeArchive() {
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(OUT_FILE));
            bos.write(convertToByteArray(treeSize)); // Writes the size of the tree to archive
            bos.write(getNumOfMeaningfulBitsInLastByte()); // Writes the number of meaningful bits in the last encoded byte of the archive
            bos.write(treeStructureBytes); // Writes bytes describing tree structure
            bos.write(encodedBytesArray); // Writes all unique bytes from the inFile
            readAndEncodeInFileBytes(bos); // Encodes inFile bytes and writes them to archive
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads bytes from the inFile, gets new codes for them and writes
     * these codes to the archive.
     *
     * @param bos is an object writing bytes to an output File.
     */
    private void readAndEncodeInFileBytes(BufferedOutputStream bos) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(IN_FILE));
            byte[] buffer = new byte[BUFFER_SIZE]; // Buffer with bytes from the source file that shall be archived
            int len; // The number of bytes extracted from the source file
            while ((len = bis.read(buffer)) > 0) {
                encodeBytes(buffer, len, bos);
            }
            writeLastBuffer(bos);
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the byte with encoded bits in it to the buffer (if applicable)
     * and writes the last buffer to the archive (if applicable).
     *
     * @param bos is an object writing bytes to an output File.
     */
    private void writeLastBuffer(BufferedOutputStream bos) {
        try {
            if (indexToWriteNewByte == 0 && alreadyWrittenBitsInThisByte > 0) { // if buffer is empty, but the alreadyWrittenBitsInThisByte is not empty
                flushByteToBuffer();
                bos.write(BYTES_TO_WRITE_TO_ARCHIVE, 0, 1);
            } else if (indexToWriteNewByte > 0 && alreadyWrittenBitsInThisByte > 0) { // Both buffer and the alreadyWrittenBitsInThisByte are not empty
                flushByteToBuffer();
                bos.write(BYTES_TO_WRITE_TO_ARCHIVE, 0, indexToWriteNewByte);
            } else { // if buffer is not empty, but the alreadyWrittenBitsInThisByte is empty. Buffer and alreadyWrittenBitsInThisByte can not be empty at the same time
                bos.write(BYTES_TO_WRITE_TO_ARCHIVE, 0, indexToWriteNewByte);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts byte after byte from tha buffer. Gets new code for each byte
     * in a form of an array of bits. Writes these bits to a byte that will be
     * later (when it contains already 8 bits) put to the buffer, and the buffer
     * will be flushed to the archive when it is already full.
     *
     * @param buffer is an array of bytes from the inFile.
     * @param len    is the number of bytes written to the buffer.
     * @param bos    is an object writing bytes to an output File.
     */
    private void encodeBytes(byte[] buffer, int len, BufferedOutputStream bos) {

        for (int i = 0; i < len; i++) {
            byte currentByteToBeEncoded = buffer[i]; // Gets byte to be encoded from the buffer
            ArrayDeque<Byte> newByteCode = getArrayDequeue(BYTE_CODES.get(currentByteToBeEncoded)); // Gets new code for this byte in a form of an array of bits

            int queueLength = newByteCode.size();
            for (int j = 0; j < queueLength; j++) { // Gets each bit from the newByteCode array
                offset = Byte.SIZE - (1 + alreadyWrittenBitsInThisByte);

                if (offset > 0) {
                    byteToWriteBitsTo |= (byte) (newByteCode.pollFirst() << offset);
                    alreadyWrittenBitsInThisByte++;
                } else if (offset == 0) {
                    byteToWriteBitsTo |= (byte) (newByteCode.pollFirst() << offset);
                    flushByteAndReset(bos);
                    if (newByteCode.size() != 0) { // if there are still any bits not written yet
                        writeRestOfBits(newByteCode, bos);
                        j = queueLength; // resets the j to the queueLength
                    }
                }
            }
        }
    }

    /**
     * Writes to an archive-byte all those bits that remained after the
     * program wrote previous byte.
     *
     * @param newByteCode is the new code for a particular byte in a form
     *                    of an array of bits.
     * @param bos         is an object writing bytes to an output File.
     */
    private void writeRestOfBits(ArrayDeque<Byte> newByteCode, BufferedOutputStream bos) {
        if (newByteCode.size() < Byte.SIZE) { // if there are less than 8 bits left
            int queueLength = newByteCode.size();
            iterateAndWriteBitsToByte(queueLength, newByteCode);
        } else if (newByteCode.size() == Byte.SIZE) { // if there are exactly 8 bits left
            iterateAndWriteBitsToByte(Byte.SIZE, newByteCode);
            flushByteAndReset(bos);
        } else { // If there are more than 8 bits left to write to a byte
            int iterations = (int) Math.floor(newByteCode.size() / (double) Byte.SIZE);
            for (int n = 0; n < iterations; n++) {
                iterateAndWriteBitsToByte(Byte.SIZE, newByteCode);
                flushByteAndReset(bos);
            }
            int queueLength = newByteCode.size();
            iterateAndWriteBitsToByte(queueLength, newByteCode);
        }
    }

    /**
     * Flushes byte that is filled with encoded bits to buffer,
     * resets two variables (alreadyWrittenBitsInThisByte and
     * byteToWriteBitsTo) and writes buffer to archive if it
     * is applicable.
     *
     * @param bos is an object writing bytes to an output File.
     */
    private void flushByteAndReset(BufferedOutputStream bos) {
        flushByteToBuffer();
        alreadyWrittenBitsInThisByte = 0;
        byteToWriteBitsTo = 0;
        writeBytesArrayToArchiveIfApplicable(bos);
    }

    /**
     * Iterates through the array with bits the provided number of times (len)
     * and writes bits to the byte that will be later written to the buffer.
     *
     * @param length      is the number of bits left in the array.
     * @param newByteCode is the new code for a particular byte in a form
     *                    of an array of bits.
     */
    private void iterateAndWriteBitsToByte(int length, ArrayDeque<Byte> newByteCode) {
        for (int j = 0; j < length; j++) {
            offset = Byte.SIZE - (1 + alreadyWrittenBitsInThisByte);
            byteToWriteBitsTo |= (byte) (newByteCode.pollFirst() << offset);
            alreadyWrittenBitsInThisByte++;
        }
    }

    /**
     * Writes byte to buffer if it is already filled with encoded bits.
     */
    private void flushByteToBuffer() {
        BYTES_TO_WRITE_TO_ARCHIVE[indexToWriteNewByte] = byteToWriteBitsTo;
        indexToWriteNewByte++;
    }

    /**
     * If the program already filled the buffer with bytes that shall be
     * written to an archive, this buffer shall be flushed.
     *
     * @param bos is an object writing bytes to an output File.
     */
    private void writeBytesArrayToArchiveIfApplicable(BufferedOutputStream bos) {
        if (indexToWriteNewByte == BYTES_TO_WRITE_TO_ARCHIVE.length) {
            try {
                bos.write(BYTES_TO_WRITE_TO_ARCHIVE, 0, indexToWriteNewByte);
                indexToWriteNewByte = 0; // reset the number of bytes written to array
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Converts the TREE_STRUCTURE array of ones and zeros to bytes.
     * E.g. if the array consists of 1, 0, 1, 1, 1, 0, 0, 1, 0, 0 -
     * than it shall be written to two bytes. Those bytes shall look
     * like 10111001 00000000. But only first 10 bits (starting from
     * the left) have information about the tree.
     */
    private byte[] writeTreeStructure() {
        int arraySize = (int) Math.ceil(TREE_STRUCTURE.size() / (double) Byte.SIZE); // The number of bytes the program shall use to encode the tree structure
        byte[] treeStructureBytes = new byte[arraySize]; // The array where the program will write bytes to (bytes which explain tree structure)
        byte byteToWriteBitsTo = 0; // The byte where the program will write bits to. Always equals zero at the start
        int bitsWrittenOverall = 0; // The overall number of bits from the TREE_STRUCTURE array written to bytes
        byte bitsWrittenToCurrentByte = 0; // The number of bits written to current byte
        int arrayIndex = 0; // The index that the byte that will be written to the treeStructureBytes array shall have

        for (int i = 0; i < TREE_STRUCTURE.size(); i++) {
            byte currentBit = TREE_STRUCTURE.get(i); // Gets bit after bit (0 or 1) from the TREE_STRUCTURE array
            byte offset = (byte) (Byte.SIZE - 1 - bitsWrittenToCurrentByte); // Is the number of times the bit shall be moved to left
            currentBit = (byte) (currentBit << offset); // Changes the location of this bit in the byte so that this bit is in the right place when we write it in byteToWriteBitsTo
            byteToWriteBitsTo = (byte) (byteToWriteBitsTo | currentBit); // Writes a bit from TREE_STRUCTURE (1 or 0) to the byte in which we encode the tree structure
            bitsWrittenToCurrentByte++;
            bitsWrittenOverall++;

            if (bitsWrittenOverall == TREE_STRUCTURE.size()) { // Writes this byte to array
                treeStructureBytes[arrayIndex] = byteToWriteBitsTo;
                return treeStructureBytes;
            } else if (bitsWrittenToCurrentByte == Byte.SIZE) {
                treeStructureBytes[arrayIndex] = byteToWriteBitsTo;
                byteToWriteBitsTo = 0;
                bitsWrittenToCurrentByte = 0;
                arrayIndex++;
            }
        }
        System.out.println("No array returned!");
        return null;
    }

    /**
     * Builds a tree consisting of separate nodes. Each leaf (a node
     * without any children) stands for a particular byte from the inFile.
     * Each node has a value (a particular byte it is representing or a null)
     * and byteFrequency (the number of times this particular byte appeared in
     * the inFile.
     * <p>
     * Each leaf has its unique value and its not necessarily unique byteFrequency.
     * Each node with children has a value variable even to zero and a byteFrequency
     * variable equal to the sum of byteFrequencies of its children.
     */
    private void buildTree() {
        if (PRIORITY_QUEUE.size() != 1) { // if the tree has more than 1 node
            while (PRIORITY_QUEUE.size() != 1) {
                Node node1 = PRIORITY_QUEUE.poll();
                Node node2 = PRIORITY_QUEUE.poll();
                Node jointNode = new Node(node1.byteFrequency + node2.byteFrequency);
                jointNode.setLeftChild(node1);
                jointNode.setRightChild(node2);
                PRIORITY_QUEUE.add(jointNode);
            }
        }
        tree = PRIORITY_QUEUE.poll();
    }

    /**
     * Recursively visits all tree nodes in order to form
     * tree structure and the array of all unique bytes from
     * the source file in a right sequence.
     */
    private void formTreeStructureAndEncodedBytesArray() {
        resetVariables();
        recursiveVisitTree(tree);
        treeSize = (short) VISITED_NODES.size();

    }

    /**
     * Recursively visits all tree nodes to write tree structure
     * and the sequence on unique bytes from the source file in
     * aright order.
     *
     * @param anyNode is any node of the tree.
     */
    private void recursiveVisitTree(Node anyNode) {
        Node leftChild = anyNode.getLeftChild();
        Node rightChild = anyNode.getRightChild();

        while ((NODE_STACK.size() != 0 || anyNodeLeftToVisit(leftChild, rightChild)) || !firstNodeVisited) {
            firstNodeVisited = true;
            saveTreeStructureAndUniqueBytes(anyNode);

            if (leftChild != null && !VISITED_NODES.contains(leftChild)) {
                NODE_STACK.push(anyNode);
                recursiveVisitTree(leftChild);
            } else if (rightChild != null && !VISITED_NODES.contains(rightChild)) {
                NODE_STACK.push(anyNode);
                recursiveVisitTree(rightChild);
            } else {
                if (NODE_STACK.size() != 0) {
                    Node someNode = NODE_STACK.peek();
                    NODE_STACK.pop();
                    recursiveVisitTree(someNode);
                }
            }
        }
    }

    /**
     * Saves tree structure to an arraylist and the unique byte a
     * particular node represents if this node is a leaf.
     *
     * @param anyNode is any node of the tree.
     */
    private void saveTreeStructureAndUniqueBytes(Node anyNode) {
        if (!VISITED_NODES.contains(anyNode)) {
            VISITED_NODES.add(anyNode);
            if (isLeaf(anyNode)) {
                TREE_STRUCTURE.add((byte) 0);
                ENCODED_BYTES.add(anyNode.value);
            } else {
                TREE_STRUCTURE.add((byte) 1);
            }
        }
    }

    /**
     * Counts the number of bits needed to encode all inFile`s bytes.
     * The program uses this number to calculate the number of encoded
     * bits in the last byte of the archive. If there is only one unique
     * byte in the inFile, the program does not count and gives 1.
     */
    private void countBitsOfEncodedBytes() {
        resetVariables();
        if (BYTE_FREQUENCY.size() == 1) {
            numOfBits = 1; // Get 1 if there is only 1 unique byte in the inFile
        } else { // If there is more than 1 unique byte in the inFile, one shall count the number of bits
            countBits(tree);
        }
    }

    /**
     * Counts the number of bits needed to encode all inFile`s bytes.
     * The program uses this number to calculate the number of encoded
     * bits in the last byte of the archive.
     *
     * @param anyNode is any node of the tree.
     */
    private void countBits(Node anyNode) {
        Node leftChild = anyNode.getLeftChild();
        Node rightChild = anyNode.getRightChild();

        while ((NODE_STACK.size() != 0 || anyNodeLeftToVisit(leftChild, rightChild)) || !firstNodeVisited) {
            firstNodeVisited = true;
            if (isLeaf(anyNode) && !VISITED_NODES.contains(anyNode)) {
                numOfBits += ((long) numOfSteps * BYTE_FREQUENCY.get(anyNode.value));
            }

            markNodeAsVisited(anyNode);

            if (leftChild != null && !VISITED_NODES.contains(leftChild)) {
                numOfSteps += 1;
                NODE_STACK.push(anyNode);
                countBits(leftChild);
            } else if (rightChild != null && !VISITED_NODES.contains(rightChild)) {
                numOfSteps += 1;
                NODE_STACK.push(anyNode);
                countBits(rightChild);
            } else {
                if (NODE_STACK.size() != 0) {
                    numOfSteps -= 1;
                    Node someNode = NODE_STACK.peek();
                    NODE_STACK.pop();
                    countBits(someNode);
                }
            }
        }
    }

    /**
     * Calculates the number of meaningful bits in the last byte in the archive.
     * In other words the number of encoded bits in the last byte.
     *
     * @return the number of meaningful bits in the last byte in the archive.
     */
    private byte getNumOfMeaningfulBitsInLastByte() {
        long fullBytesNeeded = (long) Math.ceil((double) numOfBits / Byte.SIZE);
        long numOfMeaningfulBitsInLastByte = Byte.SIZE - (fullBytesNeeded * Byte.SIZE - numOfBits);
        return (byte) numOfMeaningfulBitsInLastByte;
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
     * Takes the hashmap with all inFile`s unique bytes and the number
     * of their appearance in the inFile and transforms it to a priority
     * queue. The queue consists of Nodes, each node has a value (the unique
     * byte it stands for) and the frequency - the number of time this byte
     * appears in the inFile.
     */
    private void formPriorityQueue() {
        for (Map.Entry<Byte, Integer> item : BYTE_FREQUENCY.entrySet()) {
            Node newByte = new Node(item.getKey(), item.getValue());
            PRIORITY_QUEUE.add(newByte);
        }
    }

    /**
     * Reads arrays of bytes from the inFile and than analyzes those arrays
     * instantaneously. After the program analyzed the received array, it
     * reads another array from the inFile and repeats its previous steps.
     *
     * @param inFile is the name and the location of the file that shall be archived.
     */
    private void countByteFrequency(File inFile) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inFile));
            byte[] buffer = new byte[BUFFER_SIZE];
            int len; // The number of bytes extracted from the file
            while ((len = bis.read(buffer)) > 0) {
                analyzeBuff(buffer, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Iterates through each byte in the buffer and counts the number of
     * unique bytes in the inFile as well as the number of all those bytes
     * in the inFile.
     *
     * @param buffer is a local buffer to store streams of bytes from the
     *               inFile. This buffer is used to make the program to
     *               process the inFile faster compared to one byte reading.
     * @param len    stands for the number of bytes in the buffer that shall
     *               be read. This variable is used since not all items in the
     *               buffer shall be used, some of them could have no meaning.
     */
    private void analyzeBuff(byte[] buffer, int len) {
        final int BYTE_ENCODING_CAPACITY = 256;
        for (int i = 0; i < len; i++) {
            if (BYTE_FREQUENCY.size() < BYTE_ENCODING_CAPACITY) { // If still not all 256 possible bytes are already in the array
                if (BYTE_FREQUENCY.containsKey(buffer[i])) { // If the array has such byte
                    BYTE_FREQUENCY.put(buffer[i], BYTE_FREQUENCY.get(buffer[i]) + 1);
                } else { // if the array does not have such byte
                    BYTE_FREQUENCY.put(buffer[i], 1);
                }
            } else { // If all 256 possible bytes are already in the array
                BYTE_FREQUENCY.put(buffer[i], BYTE_FREQUENCY.get(buffer[i]) + 1);
            }
        }
    }

    /**
     * Converts short numeric to the form of two bytes.
     * <p>
     * "2" is used since this number of bytes is needed to
     * write a short numeric
     *
     * @param shortValue is any numeric in form of a short
     * @return an array with two bytes which if read together
     * are equal to the short numeric provided to this method.
     */
    private static byte[] convertToByteArray(short shortValue) {
        byte[] bytes = new byte[2];
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.putShort(shortValue);
        return buffer.array();
    }

    /**
     * Converts ArrayList ENCODED_BYTES to byte[] encodedByteArray
     * in order to be able to write it to the archive.
     */
    private void createByteArray() {
        encodedBytesArray = new byte[ENCODED_BYTES.size()];
        for (int i = 0; i < ENCODED_BYTES.size(); i++) {
            encodedBytesArray[i] = ENCODED_BYTES.get(i);
        }
    }

    /**
     * Fills hashmap with bytes as a key and bite`s new code as values.
     * New codes are stored as arrays of 1 and 0. If there is only one unique
     * byte in the inFile, the program gets 0 as a new code for it and does
     * not collect new codes.
     */
    private void fillHashMapWithCodes() {
        ArrayList<Byte> newByteCode = new ArrayList<>();
        resetVariables();

        if (BYTE_FREQUENCY.size() == 1) {
            newByteCode.add((byte) 0); // if there is only 1 unique byte, its code will be 0
            System.out.println("Your file has less than 2 unique bytes. No Huffman encoding for you bro");
            BYTE_CODES.put(tree.value, AUX_METHODS.getArrayCopy(newByteCode));
        } else { // if there is more than 1 unique byte
            collectNewCodes(tree, newByteCode);
        }
    }

    /**
     * Fills hashmap with bytes as a key and bite`s new code as values.
     * New codes are stored as arrays of 1 and 0.
     *
     * @param anyNode     is any node from the tree.
     * @param newByteCode is the new code for a particular byte in a form
     *                    of an array of bits.
     */
    private void collectNewCodes(Node anyNode, ArrayList<Byte> newByteCode) {
        Node leftChild = anyNode.getLeftChild();
        Node rightChild = anyNode.getRightChild();

        while ((NODE_STACK.size() != 0 || anyNodeLeftToVisit(leftChild, rightChild)) || !firstNodeVisited) {
            firstNodeVisited = true;
            if (isLeaf(anyNode)) {
                BYTE_CODES.put(anyNode.value, AUX_METHODS.getArrayCopy(newByteCode));
            }

            markNodeAsVisited(anyNode);
            if (leftChild != null && !VISITED_NODES.contains(leftChild)) {
                newByteCode.add((byte) 0);
                NODE_STACK.push(anyNode);
                collectNewCodes(leftChild, newByteCode);
            } else if (rightChild != null && !VISITED_NODES.contains(rightChild)) {
                newByteCode.add((byte) 1);
                NODE_STACK.push(anyNode);
                collectNewCodes(rightChild, newByteCode);
            } else {
                if (NODE_STACK.size() != 0) {
                    newByteCode.remove(newByteCode.size() - 1);
                    Node someNode = NODE_STACK.peek();
                    NODE_STACK.pop();
                    collectNewCodes(someNode, newByteCode);
                }
            }
        }
    }

    /**
     * Marks the node provided as visited node while the program
     * recursively visits all tree nodes.
     *
     * @param anyNode is any node from the tree.
     */
    private void markNodeAsVisited(Node anyNode) {
        if (!VISITED_NODES.contains(anyNode)) {
            VISITED_NODES.add(anyNode);
        }
    }

    /**
     * Converts an ArrayList to an ArrayDeque.
     *
     * @param array is any ArrayList provided to this method.
     * @return an ArrayDequeue with the same content as the ArrayList provided.
     */
    private ArrayDeque<Byte> getArrayDequeue(ArrayList<Byte> array) {
        ArrayDeque<Byte> outputArray = new ArrayDeque<>();
        for (int i = 0; i < array.size(); i++) {
            outputArray.add(array.get(i));
        }
        return outputArray;
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
